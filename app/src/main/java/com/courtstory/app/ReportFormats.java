package com.courtstory.app;

import org.json.*;
import java.util.*;
import static com.courtstory.app.Domain.*;

/** Format totals include practice once, even when a session also has a linked match record. */
final class ReportFormats {
    static final class Result {
        int count,wins,losses,draws,retired,unknown,practice;
        void add(String outcome,boolean training){count++;if(training)practice++;switch(outcome){case "Win":wins++;break;case "Loss":losses++;break;case "Draw":draws++;break;case "Retired":retired++;break;default:unknown++;}}
        String text(boolean power){int known=wins+losses+draws+retired;return count+" matches\n"+wins+" wins • "+losses+" losses • "+draws+" draws • "+retired+" retired"+(unknown==0?"":"\n"+unknown+" results not recorded")+(practice==0?"":"\nIncludes "+practice+" practice matches")+(power?"\n"+(known==0?"Win rate not recorded":Math.round(wins*100f/known)+"% win rate across recorded results"):"");}
    }
    static Map<String,Result> build(JSONObject library,JSONObject player,String sport,int days){
        Map<String,Result> totals=new LinkedHashMap<>();boolean custom=CustomScore.custom(sport);totals.put(custom?"Team events":"Singles",new Result());totals.put(custom?"Player events":"Doubles",new Result());
        long start=ReportPeriod.start(days),end=Math.min(System.currentTimeMillis()+1,ReportPeriod.end(days));Set<String> linked=new HashSet<>();String owner=player.optString("id");
        for(JSONObject match:rows(table(library,"matches"))){
            if(!match.optString("sport","Tennis").equals(sport)||!CoachInsights.includes(match,owner))continue;
            if(!match.optString("trainingSessionID").isEmpty())linked.add(match.optString("trainingSessionID"));
            long when=millis(match,"date");if(!status("matches",match).equals("Completed")||when<start||when>=end)continue;
            String kind=custom?(match.optString("customKind","Teams").equals("Players")?"Player events":"Team events"):(match.optString("matchType","Singles").equals("Doubles")?"Doubles":"Singles");totals.get(kind).add(match.optString("result"),!match.optString("trainingSessionID").isEmpty());
        }
        if(!custom)for(JSONObject session:rows(table(library,"trainingSessions"))){
            long when=millis(session,"actualStart")>0?millis(session,"actualStart"):millis(session,"date");
            if(!session.optString("sport","Tennis").equals(sport)||!CoachInsights.includes(session,owner)||!status("trainingSessions",session).equals("Completed")||when<start||when>=end||linked.contains(session.optString("id")))continue;
            JSONObject practice=session.optJSONObject("practiceResult");if(practice!=null)totals.get(practice.optString("kind","Singles").equals("Doubles")?"Doubles":"Singles").add(practice.optString("result"),true);
        }
        return totals;
    }
    static void show(MainActivity a,JSONObject player,String sport,int days){
        a.heading(CustomScore.custom(sport)?"Results by event format":"Results by match format");a.note("Recorded results include practice matches once. Unknown results stay separate.");
        for(Map.Entry<String,Result> entry:build(a.store.data,player,sport,days).entrySet()){a.heading(entry.getKey());a.note(entry.getValue().text(TrackingMode.power(a.store)));}
    }
}
