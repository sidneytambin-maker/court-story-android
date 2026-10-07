package com.courtstory.app;



import android.widget.*;

import java.util.*;

import org.json.*;

import static com.courtstory.app.Domain.*;



/** Observed performance only. Missing statistics are never treated as measured zeros. */

final class CoachInsights {

    static boolean includes(JSONObject r,String player){if(r.optString("playerID").equals(player))return true;JSONObject context=r.optJSONObject("context");if(context==null)return false;JSONArray ids=context.optJSONArray("participantIDs");if(ids!=null)for(int i=0;i<ids.length();i++)if(ids.optString(i).equals(player))return true;return false;}

    static final class Metrics {

        String sport; int matches,wins,losses,draws,retired,sessions,minutes,planned,reflections,setsFor,setsAgainst;

        int aces,faults,winners,errors,statisticsRecorded,acesRecorded,faultsRecorded,winnersRecorded,errorsRecorded;

        final Map<String,Integer> focus=new LinkedHashMap<>(),surfaces=new LinkedHashMap<>();

        String winRate(){int known=wins+losses+draws+retired;return known==0?"Not recorded":Math.round(wins*100f/known)+"%";}

        String summary(){return summary(true);} String summary(boolean power){return matches+" competitive matches"+(power?" • "+(winRate().equals("Not recorded")?"Win rate not recorded":winRate()+" wins"):" • "+wins+" wins, "+losses+" losses")+"\n"+sessions+" sessions • "+minutes/60+"h "+minutes%60+"m training";}

    }

    static Metrics metrics(Store store,JSONObject p,int days){return metrics(store,p,days,ProfileSetup.sport(p));}
    static Metrics metrics(Store store,JSONObject p,int days,String sport){return CoachMetrics.roster(store,Collections.singletonList(p),days,sport).get(p.optString("id"));}

    static String stat(String name,int total,int recorded){return name+": "+(recorded==0?"Not recorded":total+" across "+recorded+(recorded==1?" match":" matches"));}
    static void roster(MainActivity a){CoachRosterUI.show(a,false,"",0);}

    static void player(MainActivity a,JSONObject p,int days){

        a.current=()->player(a,p,days);String sport=ProfileSetup.sport(a.store.player());JSONObject scoped=SportProfiles.forSport(p,sport);

        a.page(playerName(p),sport+" • Player development • "+TrackingMode.current(a.store));a.playerScreen("player",p);a.screenDays=days;

        a.button("Period: "+(days==0?"All time":days+" days"),()->new android.app.AlertDialog.Builder(a).setTitle("Report period").setItems(split("30 days|90 days|All time"),(d,w)->player(a,p,new int[]{30,90,0}[w])).setNegativeButton("Cancel",null).show());

        Metrics m=metrics(a.store,p,days,sport);a.heading("Performance overview");a.note(m.summary(TrackingMode.power(a.store)));

        a.primary("Record for this player",()->CoachWorkspace.capturePage(a,p));

        a.button("Player records",()->CoachWorkspace.sectionsPage(a,p));
        CoachWorkspace.practicePriority(a,p,sport);

        a.heading("Match performance");a.note(m.wins+" wins • "+m.losses+" losses • "+m.draws+" draws • "+m.retired+" retired"+(CustomScore.custom(sport)?"\nNumeric score events":"\nSets or games won: "+m.setsFor+" • lost: "+m.setsAgainst));

        ReportFormats.show(a,p,sport,days);if(TrackingMode.power(a.store))ProgressCharts.show(a,p,sport,m);

        a.heading("Training workload");a.note(m.sessions+" completed sessions\n"+m.minutes+" recorded minutes\n"+m.planned+" planned or active sessions");

        if(TrackingMode.guided(a.store)){a.note("Reflections recorded: "+m.reflections+" of "+m.sessions);for(Map.Entry<String,Integer> entry:m.focus.entrySet())a.note(entry.getKey()+": "+entry.getValue()+" sessions");}

        if(TrackingMode.power(a.store)&&!CustomScore.custom(sport)){a.heading("Recorded match statistics");if(m.statisticsRecorded==0)a.note("No advanced statistics recorded yet. Unrecorded figures are not estimates.");else a.note("Totals across "+m.statisticsRecorded+" matches with statistics:\n"+stat("Aces",m.aces,m.acesRecorded)+"\n"+stat("Double faults",m.faults,m.faultsRecorded)+"\n"+stat("Winners",m.winners,m.winnersRecorded)+"\n"+stat("Unforced errors",m.errors,m.errorsRecorded));}

        if(TrackingMode.guided(a.store)){

            if(!m.surfaces.isEmpty()){a.heading("Surface experience");for(Map.Entry<String,Integer> entry:m.surfaces.entrySet())a.note(entry.getKey()+": "+entry.getValue()+" completed matches");}

            a.button("Player photos, videos and feedback",()->MediaLibrary.show(a,p));a.heading("Development plan");a.note("Goal: "+scoped.optString("primaryGoal","Not set")+"\n"+scoped.optString("developmentNotes","Add a plan with your player’s priorities and next steps."));

            a.button("Development plan and session history",()->ProfileSetup.athlete(a,p));

        }

        a.primary("Plan this player's next session",()->{JSONObject session=CoachWorkspace.record(a,p,"trainingSessions");put(session,"trainingType","One-to-one coaching");a.edit("trainingSessions",session,true);});

        if(TrackingMode.power(a.store))a.button("Preview player report",()->report(a,p,days,m));a.navigation("Players");

    }

    static void report(MainActivity a,JSONObject p,int days,Metrics m){if(!TrackingMode.power(a.store)){player(a,p,days);return;}JSONObject scoped=SportProfiles.forSport(p,m.sport);JSONObject preview=obj();put(preview,"days",days);put(preview,"text",playerName(p)+" — "+m.sport+"\nPeriod: "+(days==0?"All time":days+" days")+"\n\n"+m.summary()+"\n"+m.wins+" wins, "+m.losses+" losses, "+m.draws+" draws, "+m.retired+" retired.\nGoal: "+scoped.optString("primaryGoal","Not set")+"\nNext steps: "+scoped.optString("developmentNotes","Not recorded"));reportDraft(a,p,preview);}
    static void reportDraft(MainActivity a,JSONObject p,JSONObject preview){
        if(p==null){a.home();return;}int days=preview.optInt("days",30);
        a.page("Player report","Review and edit before sharing.");a.draft=preview;a.draftTable="playerReport";a.mediaPlayerID=p.optString("id");a.editing=true;a.editorReturn=()->player(a,p,days);
        EditText text=a.field(preview,"text","Editable report",false);text.setMaxLines(Integer.MAX_VALUE);text.setMinLines(8);a.primary("Share reviewed report",()->a.share(preview.optString("text")));a.button("Back to player",()->player(a,p,days));}

}

