package com.courtstory.app;
import android.content.Context;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static com.courtstory.app.Domain.*;

public final class Store {
    private static final Object IO_LOCK=new Object();
    final Context context;
    final AtomicFile file;
    public JSONObject data;
    private byte[] savedBytes;
    public Store(Context c) throws IOException {
        context=c.getApplicationContext();file=new AtomicFile(new File(c.getFilesDir(),"court-story-library.json"));
        synchronized(IO_LOCK){
            if(!file.getBaseFile().exists()&&!new File(file.getBaseFile().getPath()+".bak").exists()){data=newLibrary();return;}
            try{savedBytes=file.readFully();data=new JSONObject(new String(savedBytes,StandardCharsets.UTF_8));String issue=validate(data,false);if(issue!=null)throw new IOException(issue);}
            catch(JSONException e){throw new IOException("Your library could not be read. It has not been replaced.",e);}
        }
    }
    public synchronized void save() throws IOException {
        String issue=validate(data,false);if(issue!=null)throw new IOException(issue);
        synchronized(IO_LOCK){
            byte[] current=file.getBaseFile().exists()||new File(file.getBaseFile().getPath()+".bak").exists()?file.readFully():null;
            if(!java.util.Arrays.equals(savedBytes,current))throw new IOException("Your saved records changed while this screen was open. Refresh the screen before saving again. Your existing records are safe.");
            byte[] next=data.toString().getBytes(StandardCharsets.UTF_8);
            FileOutputStream stream=null;try{stream=file.startWrite();stream.write(next);file.finishWrite(stream);savedBytes=next;}catch(IOException e){if(stream!=null)file.failWrite(stream);throw e;}}
        WatchAutoSend.changed(context);
        try{if(context.getPackageManager().hasSystemFeature("android.hardware.type.watch"))context.sendBroadcast(new android.content.Intent().setClassName(context,"com.courtstory.app.WatchGlanceReceiver"));else CourtWidget.refresh(context);}catch(RuntimeException ignored){/* Widget refresh cannot undo a successful atomic save. */}
    }
    public JSONObject settings(){return object(data,"settings");}
    public JSONArray table(String name){return Domain.table(data,name);}
    public JSONObject player(){JSONObject p=find(table("players"),data.optString("selectedPlayerID"));return p!=null?p:table("players").optJSONObject(0);}
    public void saveRecord(String table,JSONObject record) throws IOException {if(table.equals("trainingSessions")){saveTraining(record);return;}JSONObject old=copy(data);upsert(data,table,copy(record));try{save();}catch(IOException e){data=old;throw e;}}
    void saveTraining(JSONObject record) throws IOException {
        JSONObject old=copy(data),session=copy(record);JSONArray pending=session.optJSONArray("androidPendingPlayers"),links=session.optJSONArray("androidLinkedMatchIDs");session.remove("androidPendingPlayers");session.remove("androidLinkedMatchIDs");
        try {
            if(pending!=null)for(JSONObject player:rows(pending)){if(player.optString("name").trim().isEmpty())throw new IOException("A new participant needs a name.");upsert(data,"players",player);}
            if(links!=null){java.util.Set<String> selected=new java.util.HashSet<>();for(int i=0;i<links.length();i++){String id=links.optString(i);JSONObject match=find(table("matches"),id);if(match==null||!TrainingLinks.eligible(match,session))throw new IOException("A linked match is no longer available for this session’s players and sport.");selected.add(id);}for(JSONObject existing:rows(table("matches"))){boolean wanted=selected.contains(existing.optString("id")),linked=existing.optString("trainingSessionID").equals(session.optString("id"));if(wanted&&!linked||!wanted&&linked){JSONObject match=copy(existing);put(match,"trainingSessionID",wanted?session.optString("id"):null);upsert(data,"matches",match);}}}
            upsert(data,"trainingSessions",session);save();
        } catch(IOException | RuntimeException error){data=old;throw error;}
    }
    public void remove(String table,JSONObject r) throws IOException {JSONObject old=copy(data);String id=r.optString("id");if(table.equals("players"))for(String owned:new String[]{"matches","trainingSessions","tournaments"})for(JSONObject activity:rows(table(owned)))if(activity.optString("playerID").equals(id))throw new IOException("This player owns activity records. Keep the profile to preserve their history.");JSONArray a=table(table);for(int i=a.length()-1;i>=0;i--)if(a.optJSONObject(i).optString("id").equals(id))a.remove(i);array(data,"deletedRecordIDs").put(id);
        if(table.equals("players")){
            if(data.optString("selectedPlayerID").equals(id)){JSONObject next=table("players").optJSONObject(0);put(data,"selectedPlayerID",next==null?null:next.optString("id"));}
            for(JSONObject coach:rows(table("coaches")))if(coach.optString("playerID").equals(id))coach.remove("playerID");
            for(JSONObject person:rows(table("players")))if(person.optString("coachProfileID").equals(id))person.remove("coachProfileID");
            for(JSONObject session:rows(table("trainingSessions"))){JSONObject practice=session.optJSONObject("practiceResult");if(practice!=null)for(String key:new String[]{"partnerID","opponentID","opponent2ID"})if(practice.optString(key).equals(id))practice.remove(key);}
        }
        for(String t:new String[]{"matches","trainingSessions","tournaments","coaches","venues","tournamentTemplates"})for(JSONObject row:rows(table(t))){for(String k:new String[]{"tournamentID","trainingSessionID","venueID","templateID","opponentID","partnerID","opponent2ID"})if(row.optString(k).equals(id))row.remove(k);JSONObject c=row.optJSONObject("context");if(c!=null){for(String k:new String[]{"venueID","tournamentID"})if(c.optString(k).equals(id))c.remove(k);for(String k:new String[]{"coachIDs","participantIDs"}){JSONArray list=array(c,k);for(int i=list.length()-1;i>=0;i--)if(list.optString(i).equals(id))list.remove(i);}}}try{save();}catch(IOException e){data=old;throw e;}}
    public void removeTournament(JSONObject tournament,boolean includeMatches) throws IOException {
        JSONObject before=copy(data);try{if(includeMatches){JSONArray matches=table("matches");for(JSONObject match:rows(matches))if(match.optString("tournamentID").equals(tournament.optString("id"))&&active(match))throw new IOException("Finish linked active matches before deleting this tournament and its matches.");for(int n=matches.length()-1;n>=0;n--){JSONObject match=matches.optJSONObject(n);if(match.optString("tournamentID").equals(tournament.optString("id"))){array(data,"deletedRecordIDs").put(match.optString("id"));matches.remove(n);}}}remove("tournaments",tournament);}catch(IOException|RuntimeException e){data=before;throw e;}
    }
    public void restore(JSONObject imported) throws IOException {String issue=validate(imported,imported.optInt("androidBackupFormat")!=1);if(issue!=null)throw new IOException(issue);if(table("players").length()>0)throw new IOException("Restore into an empty library only. Your records have not been changed.");JSONObject old=data;data=imported;try{save();}catch(IOException e){data=old;throw e;}}
}


