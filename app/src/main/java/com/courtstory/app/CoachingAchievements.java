package com.courtstory.app;

import org.json.*;
import java.util.*;
import static com.courtstory.app.Domain.*;

/** Awards reflect recorded coaching, never inferred from roster membership or names. */
final class CoachingAchievements {
    static void add(Store store, Map<String,Integer> counts) {
        JSONObject coach=store.player();String coachId=coach.optString("id"),sport=ProfileSetup.sport(coach);
        Set<String> sessions=new HashSet<>(),athletes=new HashSet<>(),eligible=new HashSet<>();
        for(JSONObject p:rows(store.table("players")))if(!p.optString("id").equals(coachId)&&SportProfiles.plays(p,sport))eligible.add(p.optString("id"));
        long now=System.currentTimeMillis();
        for(JSONObject r:rows(store.table("trainingSessions"))){
            long when=millis(r,"actualFinish")>0?millis(r,"actualFinish"):millis(r,"date");
            if(!r.optString("enteredByCoachID").equals(coachId)||!r.optString("sport","Tennis").equals(sport)||!status("trainingSessions",r).equals("Completed")||when<=0||when>now||millis(r,"date")>now||duration(r)<=0||r.optString("id").isEmpty())continue;
            Set<String> participants=new HashSet<>();participants.add(r.optString("playerID"));JSONObject context=r.optJSONObject("context");JSONArray others=context==null?null:context.optJSONArray("participantIDs");
            if(others!=null)for(int i=0;i<others.length();i++)participants.add(others.optString(i));participants.retainAll(eligible);
            if(!participants.isEmpty()){sessions.add(r.optString("id"));athletes.addAll(participants);}
        }
        counts.put("coachedSessions",sessions.size());counts.put("coachedPlayers",athletes.size());
    }
}
