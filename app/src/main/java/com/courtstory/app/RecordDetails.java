package com.courtstory.app;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.widget.*;
import org.json.*;
import java.util.*;
import static com.courtstory.app.Domain.*;

/** Readable record cards keep storage flags out of the person's story. */
final class RecordDetails {
    static final Set<String> INTERNAL=new HashSet<>(Arrays.asList("id","liveScore","revision","modifiedAt","stableShareID","needsDetails","positionChoice","durationSource","hasStartTime","hasExplicitStatus","playerMode","hasSessionDetails","hasExpectedDuration","date","endDate","status","finalResult","playerName"));
    static void show(MainActivity a,String table,JSONObject record){LinearLayout details=a.column();details.setPadding(a.dp(16),a.dp(10),a.dp(16),a.dp(10));details.setBackground(CourtDesign.surface(a.card,android.graphics.Color.luminance(a.paper)<.3?a.muted:android.graphics.Color.rgb(217,225,216),a.dp(16),0x22216348));
        for(Iterator<String> keys=record.keys();keys.hasNext();){String key=keys.next();Object raw=record.opt(key);if(INTERNAL.contains(key)||key.endsWith("ID")||key.endsWith("IDs")||key.startsWith("android")||raw instanceof JSONObject||raw instanceof JSONArray||raw==JSONObject.NULL||String.valueOf(raw).trim().isEmpty()||!TrackingMode.field(a.store,key))continue;
            if(!table.equals("players")&&(key.equals("role")||key.equals("playerType")))continue;
            if(!table.equals("matches")&&key.equals("matchFormat"))continue;
            if(table.equals("players")&&CustomScore.custom(ProfileSetup.sport(record))&&(key.equals("defaultMatchFormat")||key.equals("preferredMatchType")))continue;
            if(table.equals("matches")&&CustomScore.custom(record)&&(key.startsWith("custom")||Arrays.asList("matchFormat","matchType","sightLevel","allowedBounces","suddenDeathDeuce","yourSetsWon","opponentSetsWon").contains(key)))continue;
            String value=raw instanceof Boolean?((Boolean)raw?"Yes":"No"):String.valueOf(raw);
            if(key.toLowerCase(Locale.ROOT).contains("date")||key.equals("actualStart")||key.equals("actualFinish"))value=date(record,key,key.startsWith("actual")||record.optBoolean("hasStartTime"));
            if(key.equals("durationMinutes")||key.equals("expectedDurationMinutes"))value+=" minutes";
            if(key.equals("pointsCap")&&record.optInt(key)==0)value="No cap";
            if(key.equals("sport"))value=ProfileSetup.sport(record);
            String label=a.pretty(key);SpannableString text=new SpannableString(label+": "+value);text.setSpan(new StyleSpan(Typeface.BOLD),0,label.length()+1,0);TextView row=a.text("",16,false);row.setText(text);row.setPadding(0,a.dp(8),0,a.dp(8));details.addView(row);
        }
        if(details.getChildCount()>0){a.heading("Record details");a.body.addView(details,a.lp());}
        JSONObject practice=record.optJSONObject("practiceResult");if(practice!=null&&practice.length()>0){a.heading("Practice result");for(String[] field:new String[][]{{"kind","Practice type"},{"opponentName","Opponent"},{"partnerName","Partner"},{"opponent2Name","Second opponent"},{"result","Result"},{"playerGames","Your games"},{"opponentGames","Opponent games"}})if(practice.has(field[0])&&!practice.optString(field[0]).trim().isEmpty())a.note(field[1]+": "+practice.optString(field[0]));}
        JSONObject conditions=record.optJSONObject("environment");if(conditions!=null&&conditions.length()>0&&TrackingMode.guided(a.store)){a.heading("Conditions");for(String[] field:new String[][]{{"setting","Court setting"},{"lighting","Lighting"},{"noise","Sound and background noise"}})if(!conditions.optString(field[0]).trim().isEmpty())a.note(field[1]+": "+conditions.optString(field[0]));JSONArray weather=conditions.optJSONArray("weather");if(weather!=null&&weather.length()>0){List<String> values=new ArrayList<>();for(int i=0;i<weather.length();i++)values.add(weather.optString(i));a.note("Weather: "+String.join(", ",values));}}

    }
}
