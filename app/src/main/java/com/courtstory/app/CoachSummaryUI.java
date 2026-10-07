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
        a.toggle(options,"includeNotes","Include reflections and notes",false);a.toggle(options,"includeWellness","Include wellness details",false);
        EditText preview=a.field(options,"summaryText","Editable coach summary",false);preview.setTextSize(16);preview.setGravity(Gravity.TOP);preview.setMaxLines(12);preview.setMinLines(10);
        Runnable build=()->{StringBuilder b=new StringBuilder("Court Story — "+playerName(a.store.player())+"\n");long cut=ReportPeriod.start(days);for(String t:new String[]{"trainingSessions","matches","tournaments"})for(JSONObject r:a.mine(t))if(millis(r,"date")>=cut&&millis(r,"date")<ReportPeriod.end(days)){b.append("\n").append(summary(t,r)).append("\n");if(options.optBoolean("includeNotes"))b.append(r.optString("notes")).append(" ").append(r.optString("sessionOutcome")).append(" ").append(r.optString("matchStory")).append("\n");if(options.optBoolean("includeWellness")&&t.equals("trainingSessions"))b.append("Energy: ").append(r.optString("energyLevel","Not recorded")).append(". Pain: ").append(r.optString("painLevel","Not recorded")).append("\n");}put(options,"summaryText",b.toString());put(options,"generatedText",b.toString());preview.setText(b.toString());};
        if(restored!=null&&restored.has("generatedText"))preview.setText(options.optString("summaryText"));else build.run();
        a.button("Refresh preview with selected details",()->{if(!options.optString("summaryText").equals(options.optString("generatedText")))a.confirm("Replace your edited preview?","This rebuilds the summary from your records. Your edited wording will be replaced.",build);else build.run();});
        a.button("Share this preview",()->a.share(preview.getText().toString()));
    }
}

