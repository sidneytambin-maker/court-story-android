package com.courtstory.app;
import android.content.Context;
import android.content.pm.PackageManager;
import org.json.JSONObject;
import static com.courtstory.app.Domain.*;
/** Permission information is reported only through the explicitly enabled paired-device transfer. */
final class WatchAccess {
    static JSONObject local(Context c){JSONObject value=obj();put(value,"reportedAt",now());put(value,"activityAllowed",c.checkSelfPermission("android.permission.ACTIVITY_RECOGNITION")==PackageManager.PERMISSION_GRANTED);put(value,"heartRateAllowed",c.checkSelfPermission(android.os.Build.VERSION.SDK_INT>=36?"android.permission.health.READ_HEART_RATE":"android.permission.BODY_SENSORS")==PackageManager.PERMISSION_GRANTED);put(value,"sensorsPreferred",c.getSharedPreferences("watch-workout-preferences",0).getBoolean("preferSensors",false));return value;}
    static String summary(JSONObject value){return "Activity measurement: "+(value.optBoolean("activityAllowed")?"allowed":"not allowed")+". Heart rate: "+(value.optBoolean("heartRateAllowed")?"allowed":"not allowed")+". Preferred start: "+(value.optBoolean("sensorsPreferred")?"with watch measurements":"without sensors")+".";}
    static void show(MainActivity a){boolean watch=a.getPackageManager().hasSystemFeature("android.hardware.type.watch");if(watch){a.heading("Watch measurement access");a.note(summary(local(a)));}else{String saved=a.getSharedPreferences("watch-connection",0).getString("watchAccess","");a.heading("Watch measurement access");if(saved.isEmpty())a.note("No watch access report received yet. Send records from your watch to include its current permission status.");else try{JSONObject value=new JSONObject(saved);a.note(summary(value));a.note("Reported: "+date(value,"reportedAt",true)+". Permissions can change after this report.");}catch(Exception e){a.note("Send records from your watch to refresh its access report.");}}}
}
