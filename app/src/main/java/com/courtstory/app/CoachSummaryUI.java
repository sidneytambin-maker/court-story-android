package com.courtstory.app;
import android.view.Gravity;
import android.widget.EditText;
import org.json.JSONObject;
import static com.courtstory.app.Domain.*;
/** The editable sharing preview remains a draft until the user shares it. */
final class CoachSummaryUI {
    static void show(MainActivity a,int days,JSONObject restored){
        if(!TrackingMode.power(a.store)){a.reports();return;}
        JSONObject options=restored==null?obj():restored;put(options,"days",days);
        a.current=()->show(a,days,options);a.page("Coach summary","Preview your own words before sharing.");
        a.draft=options;a.draftTable="coachSummary";a.editing=true;a.editorReturn=()->a.reportsRange(days);
        a.toggle(options,"includeResults","Match results",true);a.toggle(options,"includeTraining","Training totals",true);a.toggle(options,"includeFocus","Training focus",false);a.toggle(options,"includeGoals","Goals and match review",false);
        a.toggle(options,"includeRecords","Include individual activity records",false);a.toggle(options,"includeNotes","Include reflections and notes",false);a.toggle(options,"includeWellness","Include wellness details",false);
        EditText preview=a.field(options,"summaryText","Editable coach summary",false);preview.setTextSize(16);preview.setGravity(Gravity.TOP);preview.setMaxLines(12);preview.setMinLines(10);
        Runnable build=()->{String text=build(a,days,options);put(options,"summaryText",text);put(options,"generatedText",text);preview.setText(text);};
        if(restored!=null&&restored.has("generatedText"))preview.setText(options.optString("summaryText"));else build.run();
        a.button("Refresh preview with selected details",()->{if(!options.optString("summaryText").equals(options.optString("generatedText")))a.confirm("Replace your edited preview?","This rebuilds the summary from your records. Your edited wording will be replaced.",build);else build.run();});
        a.button("Share this preview",()->{if(preview.getText().toString().trim().isEmpty()){a.error("Write or generate a summary before sharing.");return;}a.share(preview.getText().toString());});
    }
    static String build(MainActivity a,int days,JSONObject options){
        StringBuilder b=new StringBuilder("Court Story — "+playerName(a.store.player())+"\n"+ProfileSetup.sport(a.store.player())+" · "+ReportPeriod.label(days)+"\n");
        long cut=ReportPeriod.start(days),end=Math.min(System.currentTimeMillis()+1,ReportPeriod.end(days));
        if(options.optBoolean("includeResults",true))for(java.util.Map.Entry<String,ReportFormats.Result> e:ReportFormats.build(a.store.data,a.store.player(),ProfileSetup.sport(a.store.player()),days).entrySet())b.append("\n").append(e.getKey()).append(": ").append(e.getValue().text(false)).append("\n");
        int sessions=0;long seconds=0;java.util.Map<String,Integer> focus=new java.util.LinkedHashMap<>();
        for(JSONObject r:a.mine("trainingSessions")){long when=millis(r,"actualStart")>0?millis(r,"actualStart"):millis(r,"date");if(when<cut||when>=end||!status("trainingSessions",r).equals("Completed"))continue;sessions++;seconds+=durationSeconds(r);TrainingProgress.count(focus,r);}
        if(options.optBoolean("includeTraining",true))b.append("\nTraining: ").append(sessions).append(" sessions, ").append(durationText(seconds)).append(".\n");
        if(options.optBoolean("includeFocus")){b.append("\nTraining focus\n");if(focus.isEmpty())b.append("No training focus recorded.\n");for(java.util.Map.Entry<String,Integer> e:focus.entrySet())b.append(e.getKey()).append(": ").append(e.getValue()).append(" sessions\n");}
        if(options.optBoolean("includeGoals")){b.append("\nGoals and match review\n");for(String key:new String[]{"primaryGoal","coachingFocus"})if(!a.store.player().optString(key).trim().isEmpty())b.append(a.store.player().optString(key)).append("\n");JSONObject latest=TrainingProgress.latestReview(a.mine("matches"));if(latest!=null)b.append(TrainingProgress.review(latest)).append("\n");}
        for(String t:new String[]{"trainingSessions","matches","tournaments"})for(JSONObject r:a.mine(t))if(millis(r,"date")>=cut&&millis(r,"date")<end){if(options.optBoolean("includeRecords"))b.append("\n").append(summary(t,r)).append("\n");if(options.optBoolean("includeNotes"))b.append(r.optString("notes")).append(" ").append(r.optString("sessionOutcome")).append(" ").append(r.optString("matchStory")).append("\n");if(options.optBoolean("includeWellness")&&t.equals("trainingSessions")&&SessionRatings.included(r))b.append("Energy: ").append(r.optString("energyLevel","Not recorded")).append(". Pain: ").append(r.optString("painLevel","Not recorded")).append("\n");}
        return b.toString();
    }
}

