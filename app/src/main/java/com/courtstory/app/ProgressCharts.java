package com.courtstory.app;

import android.graphics.*;
import android.view.View;
import android.widget.LinearLayout;
import org.json.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import static com.courtstory.app.Domain.*;

/** Visual charts are paired with complete native text data for TalkBack and magnification. */
final class ProgressCharts {
    static final class Week {String label;int minutes,sessions;Week(String label){this.label=label;}}
    static List<Week> workload(Store store,JSONObject player,String sport){ZoneId zone=ZoneId.systemDefault();LocalDate monday=LocalDate.now(zone).with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(5);List<Week> weeks=new ArrayList<>();DateTimeFormatter format=DateTimeFormatter.ofPattern("d MMM",Locale.UK);for(int i=0;i<6;i++)weeks.add(new Week(monday.plusWeeks(i).format(format)));for(JSONObject session:rows(store.table("trainingSessions"))){if(!CoachInsights.includes(session,player.optString("id"))||!session.optString("sport","Tennis").equals(sport)||!status("trainingSessions",session).equals("Completed"))continue;long when=millis(session,"actualStart")>0?millis(session,"actualStart"):millis(session,"date");if(when>System.currentTimeMillis())continue;LocalDate date=Instant.ofEpochMilli(when).atZone(zone).toLocalDate();long offset=java.time.temporal.ChronoUnit.DAYS.between(monday,date);if(offset<0||offset>=42)continue;Week week=weeks.get((int)(offset/7));week.minutes+=duration(session);week.sessions++;}return weeks;}
    static void show(MainActivity a,JSONObject player,String sport,CoachInsights.Metrics metrics){
        a.heading("Training rhythm");a.note("Recorded training minutes by week • Last six weeks");List<Week> weeks=workload(a.store,player,sport);int[] values=new int[weeks.size()];String[] labels=new String[weeks.size()];int total=0;for(int i=0;i<weeks.size();i++){values[i]=weeks.get(i).minutes;labels[i]=weeks.get(i).label;total+=values[i];}
        if(total==0)a.note("No completed training in this six-week period. Your chart will build as sessions are recorded.");else {chart(a,labels,values);a.note("Six-week total: "+total+" minutes. The current week is still in progress.");}
        for(Week week:weeks)a.note("Week of "+week.label+": "+week.minutes+" minutes across "+week.sessions+" sessions.");
        a.heading("Match outcomes");if(metrics.matches==0){a.note("No completed matches in the selected report period.");return;}chart(a,new String[]{"Wins","Losses","Draws","Retired"},new int[]{metrics.wins,metrics.losses,metrics.draws,metrics.retired});a.note(metrics.wins+" wins, "+metrics.losses+" losses, "+metrics.draws+" draws, "+metrics.retired+" retired. Win rate among recorded results: "+metrics.winRate()+". Unknown results are excluded from the percentage.");
    }
    private static void chart(MainActivity a,String[] labels,int[] values){Bars chart=new Bars(a,labels,values);chart.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);a.body.addView(chart,new LinearLayout.LayoutParams(-1,a.dp(190)));}
    private static final class Bars extends View {
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final MainActivity activity;final String[] labels;final int[] values;
        Bars(MainActivity a,String[] labels,int[] values){super(a);activity=a;this.labels=labels;this.values=values;}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float d=getResources().getDisplayMetrics().density;float top=28*d,bottom=getHeight()-30*d,left=10*d,right=getWidth()-10*d;int max=1;for(int value:values)max=Math.max(max,value);paint.setColor(activity.card);c.drawRoundRect(0,0,getWidth(),getHeight(),14*d,14*d,paint);paint.setColor(activity.muted);paint.setStrokeWidth(d);c.drawLine(left,bottom,right,bottom,paint);float cell=(right-left)/values.length;paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));paint.setTextSize(11*d);for(int i=0;i<values.length;i++){float center=left+cell*(i+.5f),barTop=bottom-(bottom-top)*values[i]/max;paint.setColor(i==values.length-1?activity.accent:activity.green);if(values[i]>0){c.drawRoundRect(center-cell*.27f,barTop,center+cell*.27f,bottom,4*d,4*d,paint);paint.setColor(activity.ink);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f*d);c.drawRoundRect(center-cell*.27f,barTop,center+cell*.27f,bottom,4*d,4*d,paint);paint.setStyle(Paint.Style.FILL);}paint.setColor(activity.ink);c.drawText(Integer.toString(values[i]),center,Math.max(18*d,barTop-7*d),paint);c.drawText(labels[i],center,bottom+19*d,paint);}}
    }
}
