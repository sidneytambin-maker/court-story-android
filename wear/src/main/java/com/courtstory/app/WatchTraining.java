package com.courtstory.app;
import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.util.*;
import static com.courtstory.app.Domain.*;

final class WatchTraining {
    static JSONObject state(WatchActivity a){try{JSONObject s=WatchTransport.read(new File(a.getFilesDir(),"watch-workout.json"));return s==null?obj():s;}catch(Exception e){return obj();}}
    static String startIssue(Store store,String id){JSONObject record=find(store.table("trainingSessions"),id);if(record==null)return "Training record missing.";if(!record.optString("playerID").equals(store.player().optString("id")))return "Only your own training can use your wrist sensors.";if(millis(record,"actualFinish")>0||!status("trainingSessions",record).equals("Scheduled"))return "Only a scheduled session can start a new workout.";for(JSONObject other:rows(store.table("trainingSessions")))if(active(other)&&other.optString("playerID").equals(record.optString("playerID")))return "Finish your active training session first.";return null;}
    static void command(WatchActivity a,String action,String id){try{JSONObject saved=state(a);if(action.equals("start")){String phase=saved.optString("phase");if(!saved.optBoolean("committed")&&!saved.optString("record").isEmpty()&&(phase.equals("Ended")||!saved.optString("record").equals(id)&&(phase.equals("Active")||phase.equals("Paused")||phase.equals("Preparing")))){a.error("Finish and save your previous watch workout before starting another.");return;}String issue=startIssue(a.store,id);if(issue!=null){a.error(issue);return;}}Intent i=new Intent(a,WatchWorkoutService.class).setAction(action).putExtra("record",id);a.startForegroundService(i);a.announce("Workout request sent. Status will update when it completes.");}catch(Exception e){a.error("The watch could not start its workout service. Training without sensors remains available.");}}
    static void show(WatchActivity a,JSONObject source){
        JSONObject r=find(a.store.table("trainingSessions"),source.optString("id"));if(r==null){a.home();return;}a.current=()->show(a,r);a.page(title("trainingSessions",r),status("trainingSessions",r));a.screenTable="trainingSessions";a.screenRecord=r.optString("id");a.note(summary("trainingSessions",r));JSONObject state=state(a);boolean thisWorkout=state.optString("record").equals(r.optString("id"));boolean pending=thisWorkout&&state.optString("phase").equals("Ended")&&!state.optBoolean("committed");boolean sensors=thisWorkout&&(state.optString("phase").equals("Active")||state.optString("phase").equals("Paused")||state.optString("phase").equals("Preparing"));
        if(thisWorkout&&!state.optString("message").isEmpty())a.note(state.optString("message"));
        if(pending){a.note("Your measurements are retained on this watch and still need to be saved to the session.");a.primary("Retry saving workout",()->command(a,"finish",r.optString("id")));}
        else if(sensors){a.note("Watch workout: "+state.optString("phase"));if(state.optString("phase").equals("Paused"))a.button("Resume workout",()->command(a,"resume",r.optString("id")));else if(state.optString("phase").equals("Active"))a.button("Pause workout",()->command(a,"pause",r.optString("id")));a.button("Finish workout",()->a.confirm("Finish training?","Save your session and available watch measurements.",()->command(a,"finish",r.optString("id"))));}
        else if(active(r)){a.button("Finish training",()->a.confirm("Finish training?","Save the recorded duration.",()->{JSONObject next=copy(r);put(next,"actualFinish",now());put(next,"androidScheduled",false);put(next,"durationSource","recorded");if(a.saveRecord("trainingSessions",next)){a.sound("completionsEnabled","tennis_applause");show(a,next);}}));}
        else if(status("trainingSessions",r).equals("Scheduled")){
            a.note(TermsPolicy.SAFETY);a.primary("Start without sensors",()->{for(JSONObject existing:a.mine("trainingSessions"))if(active(existing)){a.error("Finish your active session first.");return;}JSONObject next=copy(r);put(next,"actualStart",now());next.remove("actualFinish");put(next,"androidScheduled",false);if(a.saveRecord("trainingSessions",next))show(a,next);});
            if(r.optString("playerID").equals(a.store.player().optString("id"))){
                String heartPermission=Build.VERSION.SDK_INT>=36?"android.permission.health.READ_HEART_RATE":Manifest.permission.BODY_SENSORS;
                if(a.checkSelfPermission(heartPermission)!=PackageManager.PERMISSION_GRANTED)a.button("Allow optional heart rate",()->a.requestPermissions(new String[]{heartPermission},711));
                a.button("Start with watch measurements",()->{if(a.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)!=PackageManager.PERMISSION_GRANTED){a.confirm("Optional watch measurements","Activity recognition enables the workout. Heart rate is optional and can be declined. Start without sensors is always available.",()->a.requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION},710));return;}command(a,"start",r.optString("id"));});
            }
        }
        RecordDetails.show(a,"trainingSessions",r);
        if(!sensors&&!pending){a.button("Edit session",()->a.edit("trainingSessions",r,false));if(!active(r))a.button("Delete record",()->a.delete("trainingSessions",r));}
        WorkoutSummary.show(a,r);a.button("Refresh workout status",()->{try{a.store=new Store(a);JSONObject pendingState=state(a);if(pendingState.optString("record").equals(r.optString("id"))&&!pendingState.optBoolean("committed")&&!pendingState.optString("phase").equals("Failed"))command(a,"recover",r.optString("id"));show(a,r);}catch(Exception e){a.error("Saved session could not be refreshed.");}});a.navigation("Live");
    }
}
