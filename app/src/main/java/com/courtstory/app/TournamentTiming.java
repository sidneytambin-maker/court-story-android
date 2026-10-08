package com.courtstory.app;

import android.os.SystemClock;
import android.view.View;
import android.widget.Chronometer;
import org.json.JSONObject;
import static com.courtstory.app.Domain.*;

/** Tournament elapsed time survives leaving the screen and restarting either device. */
final class TournamentTiming {
    static void show(MainActivity a,JSONObject r){
        long start=millis(r,"actualStart"),finish=millis(r,"actualFinish");
        if(start>0&&finish==0){
            Chronometer timer=new Chronometer(a);timer.setTextSize(16);timer.setTextColor(a.ink);
            timer.setBase(SystemClock.elapsedRealtime()-Math.max(0,System.currentTimeMillis()-start));timer.setFormat("Elapsed: %s");
            timer.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_NONE);
            timer.setOnChronometerTickListener(v->v.setContentDescription("Tournament elapsed time: "+durationText(Math.max(0,SystemClock.elapsedRealtime()-v.getBase())/1000)));
            a.body.addView(timer,a.lp());timer.start();
        }else if(start>0&&finish>=start)a.note("Tournament duration: "+durationText((finish-start)/1000));
        if(!active(r)&&!status("tournaments",r).equals("Completed")&&!status("tournaments",r).equals("Withdrawn"))
            a.button("Start tournament timing",()->start(a,r));
    }
    static void start(MainActivity a,JSONObject record){
        JSONObject current=find(a.store.table("tournaments"),record.optString("id"));
        if(current==null){a.error("This tournament is no longer available.");return;}
        if(status("tournaments",current).equals("Completed")||status("tournaments",current).equals("Withdrawn")){a.detail("tournaments",current);return;}
        for(JSONObject other:rows(a.store.table("tournaments")))if(!other.optString("id").equals(current.optString("id"))&&other.optString("playerID").equals(current.optString("playerID"))&&active(other)){a.error("Finish this player’s active tournament before timing another.");return;}
        JSONObject next=copy(current);if(millis(next,"actualStart")==0)put(next,"actualStart",now());next.remove("actualFinish");put(next,"finalResult","In progress");put(next,"hasExplicitStatus",true);
        if(a.saveRecord("tournaments",next)){a.detail("tournaments",next);a.announce("Tournament timing started");}
    }
    static void finish(MainActivity a,JSONObject record){
        JSONObject current=find(a.store.table("tournaments"),record.optString("id"));if(current==null){a.error("This tournament is no longer available.");return;}
        JSONObject next=copy(current);put(next,"finalResult","Completed");put(next,"hasExplicitStatus",true);put(next,"actualFinish",java.time.Instant.ofEpochMilli(Math.max(System.currentTimeMillis(),millis(next,"actualStart"))).toString());
        if(a.saveRecord("tournaments",next)){a.detail("tournaments",next);a.announce("Tournament completed");}
    }
}
