package com.courtstory.app;
import org.json.JSONObject;
import java.util.Locale;
final class WorkoutSummary {
    static String text(JSONObject workout){StringBuilder b=new StringBuilder("Workout: "+Math.round(workout.optDouble("durationSeconds")/60)+" minutes");for(String[] metric:new String[][]{{"averageHeartRate","Average heart rate"," beats per minute"},{"peakHeartRate","Peak heart rate"," beats per minute"},{"activeEnergyKcal","Active energy"," kcal"},{"distanceMeters","Distance"," metres"},{"stepCount","Steps",""}}){b.append("\n").append(metric[1]).append(": ");if(workout.has(metric[0])&&!workout.isNull(metric[0])&&Double.isFinite(workout.optDouble(metric[0])))b.append(String.format(Locale.getDefault(),"%.0f",workout.optDouble(metric[0]))).append(metric[2]);else b.append("Not recorded");}return b.toString();}
    static void show(MainActivity a,JSONObject session){JSONObject workout=session.optJSONObject("workout");if(workout==null)return;a.heading("Workout summary");a.note(text(workout));a.note("Source: "+workout.optString("source","Recorded workout"));}
}
