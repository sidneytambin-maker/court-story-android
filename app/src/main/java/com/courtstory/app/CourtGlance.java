package com.courtstory.app;

import org.json.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import static com.courtstory.app.Domain.*;

/** The five watch-face summaries use the selected person's current sport only. */
public final class CourtGlance {
    public final String title,compact,detail,description,table,recordID,action;
    CourtGlance(String title,String compact,String detail,String description,String table,String recordID,String action){this.title=title;this.compact=compact;this.detail=detail;this.description="Court Story. "+description;this.table=table;this.recordID=recordID;this.action=action;}
    static CourtGlance empty(String title,String compact,String detail,String action){return new CourtGlance(title,compact,detail,detail,"","",action);}
    public CourtGlance withWorkout(JSONObject state){if(!table.equals("trainingSessions")||!recordID.equals(state.optString("record"))||!(state.optString("phase").equals("Active")||state.optString("phase").equals("Paused")))return this;long minutes=Math.max(0,state.optLong("durationSeconds")/60);String text=state.optString("phase")+" training. "+minutes+" recorded active minutes. Tap to open the workout controls.";return new CourtGlance(title,minutes+"m",text,text,table,recordID,action);}
    public static CourtGlance make(JSONObject library,String kind,long now){
        JSONObject player=find(table(library,"players"),library.optString("selectedPlayerID"));if(player==null)player=table(library,"players").optJSONObject(0);
        if(player==null)return empty("Court Story","Set up","Set up your court on the watch or connect your phone.","");
        String sport=ProfileSetup.sport(player),owner=player.optString("id");Map<String,List<JSONObject>> scoped=new LinkedHashMap<>();
        for(String t:new String[]{"matches","trainingSessions","tournaments"}){List<JSONObject> records=new ArrayList<>();for(JSONObject r:rows(table(library,t)))if(owner.equals(r.optString("playerID"))&&sport.equals(r.optString("sport","Tennis")))records.add(r);scoped.put(t,records);}
        if(kind.equals("week"))return week(scoped,sport,now);
        if(kind.equals("startTraining")){
            for(JSONObject r:scoped.get("trainingSessions"))if(active(r))return activity("trainingSessions",r,"current",now);
            List<JSONObject> nearby=new ArrayList<>();for(JSONObject r:scoped.get("trainingSessions"))if(status("trainingSessions",r).equals("Scheduled")&&Math.abs(millis(r,"date")-now)<=3600000)nearby.add(r);
            if(nearby.size()==1)return activity("trainingSessions",nearby.get(0),"startTraining",now);
            return empty("Training","Start","Choose your "+sport+" training session.","training");
        }
        JSONObject selected=null;String selectedTable="";long boundary=kind.equals("next")?Long.MAX_VALUE:Long.MIN_VALUE;
        long today=Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        for(Map.Entry<String,List<JSONObject>> group:scoped.entrySet())for(JSONObject r:group.getValue()){
            String t=group.getKey();long date=millis(r,"date");boolean eligible;
            if(kind.equals("current")){eligible=active(r);date=Math.max(millis(r,"actualStart"),millis(r,"modifiedAt"));}
            else if(kind.equals("next")){boolean scheduled=status(t,r).equals("Scheduled")||status(t,r).equals("Entered");eligible=!active(r)&&scheduled&&(t.equals("tournaments")?Math.max(date,millis(r,"endDate"))>=today:date>=(r.optBoolean("hasStartTime")?now:today));}
            else {date=finished(t,r);eligible=status(t,r).equals("Completed")&&!active(r)&&date>0&&date<=now;}
            if(eligible&&(selected==null||(kind.equals("next")?date<boundary:date>boundary))){selected=r;selectedTable=t;boundary=date;}
        }
        if(selected!=null)return activity(selectedTable,selected,kind,now);
        return kind.equals("current")?empty("Now","Start","No "+sport+" activity is running.","training"):kind.equals("next")?empty("Next","None","No upcoming "+sport+" activity is scheduled.",""):empty("Latest","None","No completed "+sport+" activity is recorded.","recentRecord");
    }
    static long finished(String t,JSONObject r){long end=millis(r,"actualFinish");if(end>0)return end;if(t.equals("tournaments"))return Math.max(millis(r,"date"),millis(r,"endDate"));return millis(r,"date")+(t.equals("trainingSessions")?duration(r)*60000L:0);}
    static CourtGlance week(Map<String,List<JSONObject>> scoped,String sport,long now){
        long start=Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        int sessions=0,minutes=0,matches=0,wins=0;
        for(JSONObject r:scoped.get("trainingSessions")){long began=millis(r,"actualStart");if(began==0)began=millis(r,"date");if(status("trainingSessions",r).equals("Completed")&&!active(r)&&began>=start&&finished("trainingSessions",r)<=now){sessions++;minutes+=duration(r);}}
        for(JSONObject r:scoped.get("matches")){long end=finished("matches",r);if(status("matches",r).equals("Completed")&&end>=start&&end<=now&&r.optString("trainingSessionID").isEmpty()){matches++;if(r.optString("result").equals("Win"))wins++;}}
        String text=sessions+" training sessions, "+minutes+" minutes. "+matches+" matches, "+wins+" wins.";
        return new CourtGlance("Week",minutes+"m",sessions+" sessions · "+matches+" matches","This week, Monday to Sunday. "+sport+". "+text,"","","watchWeek");
    }
    static CourtGlance activity(String t,JSONObject r,String kind,long now){
        String title=kind.equals("current")?"Now":kind.equals("next")?"Next":kind.equals("startTraining")?"Training":"Latest",compact,detail=summary(t,r);
        if(kind.equals("startTraining")){compact="Start";detail="Start scheduled training. "+detail;}
        else if(kind.equals("next")){compact=DateTimeFormatter.ofPattern(r.optBoolean("hasStartTime")?"HH:mm":"d/M").format(Instant.ofEpochMilli(millis(r,"date")).atZone(ZoneId.systemDefault()));}
        else if(t.equals("matches")){
            if(CustomScore.custom(r)){compact="Score";detail=CustomScore.summary(r)+". "+summary(t,r);}
            else if(active(r)){Scoring score=new Scoring(copy(r));compact=SportRules.pointsStyle(r)?score.state.optInt("playerPoints")+"–"+score.state.optInt("opponentPoints"):score.state.optInt("playerGames")+"–"+score.state.optInt("opponentGames");detail=score.spoken();}
            else compact=r.optString("result","Saved");
        }else if(t.equals("trainingSessions")){compact=active(r)?r.optBoolean("androidWearWorkout")?"Live":Math.max(0,(now-millis(r,"actualStart"))/60000)+"m":duration(r)+"m";if(active(r))detail=title("trainingSessions",r)+" in progress. Open for the current duration and workout status.";}
        else compact=active(r)?"Live":"Done";
        return new CourtGlance(title,compact,detail,r.optString("sport","Tennis")+". "+detail,t,r.optString("id"),"");
    }
}
