package com.courtstory.app;
import org.json.JSONObject;
import android.widget.*;
import static com.courtstory.app.Domain.*;

final class SessionRatings {
    static boolean included(JSONObject r){return r.optBoolean("hasSessionDetails",r.has("effortLevel")||r.has("confidenceLevel")||r.has("energyLevel")||r.has("painLevel"));}
    static void form(MainActivity a,JSONObject r){
        Switch include=new Switch(a);include.setText("Include body ratings");include.setTextColor(a.ink);include.setTextSize(16);include.setMinHeight(a.dp(56));include.setChecked(included(r));a.body.addView(include,a.lp());
        LinearLayout host=a.body,fields=a.column();host.addView(fields);a.body=fields;
        // Unrecorded remains unknown until the person chooses a rating.
        for(String[] f:new String[][]{{"effortLevel","Effort"},{"confidenceLevel","Confidence"},{"energyLevel","Energy"}})a.choice(r,f[0],f[1],split("Not recorded|Low|Medium|High"));
        a.choice(r,"painLevel","Pain",split("Not recorded|None|Mild|Moderate|High"));a.body=host;
        fields.setVisibility(include.isChecked()?android.view.View.VISIBLE:android.view.View.GONE);
        put(r,"hasSessionDetails",include.isChecked());include.setOnCheckedChangeListener((b,on)->{put(r,"hasSessionDetails",on);fields.setVisibility(on?android.view.View.VISIBLE:android.view.View.GONE);});
    }
}
