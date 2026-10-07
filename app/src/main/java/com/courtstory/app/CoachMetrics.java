package com.courtstory.app;

import org.json.*;
import java.util.*;
import static com.courtstory.app.Domain.*;

/** Aggregate a roster in one pass, counting a participant only once per session. */
final class CoachMetrics {
    static Map<String,CoachInsights.Metrics> roster(Store store,List<JSONObject> people,int days,String sport){
        Map<String,CoachInsights.Metrics> all=new HashMap<>();
        for(JSONObject p:people){CoachInsights.Metrics m=new CoachInsights.Metrics();m.sport=sport;all.put(p.optString("id"),m);}
        long now=System.currentTimeMillis(),cutoff=days==0?0:now-days*86400000L;
        for(JSONObject r:rows(store.table("matches"))){
            long when=millis(r,"date");if(!r.optString("sport","Tennis").equals(sport)||when<cutoff||when>now||!status("matches",r).equals("Completed")||!r.optString("trainingSessionID").isEmpty())continue;
            for(CoachInsights.Metrics m:subjects(all,r)){
                m.matches++;switch(r.optString("result")){case "Win":m.wins++;break;case "Loss":m.losses++;break;case "Draw":m.draws++;break;case "Retired":m.retired++;break;}
                m.setsFor+=r.optInt("yourSetsWon");m.setsAgainst+=r.optInt("opponentSetsWon");String surface=r.optString("courtSurface","Not specified");m.surfaces.put(surface,m.surfaces.getOrDefault(surface,0)+1);
                boolean measured=false;
                if(r.has("aces")&&!r.isNull("aces")){m.acesRecorded++;m.aces+=r.optInt("aces");measured=true;}
                if(r.has("doubleFaults")&&!r.isNull("doubleFaults")){m.faultsRecorded++;m.faults+=r.optInt("doubleFaults");measured=true;}
                if(r.has("winners")&&!r.isNull("winners")){m.winnersRecorded++;m.winners+=r.optInt("winners");measured=true;}
                if(r.has("unforcedErrors")&&!r.isNull("unforcedErrors")){m.errorsRecorded++;m.errors+=r.optInt("unforcedErrors");measured=true;}
                if(measured)m.statisticsRecorded++;
            }
        }
        for(JSONObject r:rows(store.table("trainingSessions"))){
            long when=millis(r,"actualStart")>0?millis(r,"actualStart"):millis(r,"date");if(!r.optString("sport","Tennis").equals(sport)||when<cutoff)continue;
            for(CoachInsights.Metrics m:subjects(all,r)){
                if(!status("trainingSessions",r).equals("Completed")){m.planned++;continue;}if(when>now)continue;
                m.sessions++;m.minutes+=duration(r);if(!r.optString("sessionOutcome").trim().isEmpty()||!r.optString("notes").trim().isEmpty())m.reflections++;
                String focus=r.optString("focus","Not specified");m.focus.put(focus,m.focus.getOrDefault(focus,0)+1);
            }
        }
        return all;
    }
    private static List<CoachInsights.Metrics> subjects(Map<String,CoachInsights.Metrics> all,JSONObject record){
        Set<String> ids=new HashSet<>();ids.add(record.optString("playerID"));JSONObject context=record.optJSONObject("context");JSONArray participants=context==null?null:context.optJSONArray("participantIDs");
        if(participants!=null)for(int i=0;i<participants.length();i++)ids.add(participants.optString(i));
        List<CoachInsights.Metrics> result=new ArrayList<>();for(String id:ids)if(all.containsKey(id))result.add(all.get(id));return result;
    }
}
