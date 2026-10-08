package com.courtstory.app;
import android.widget.*;
import org.json.*;
import static com.courtstory.app.Domain.*;

/** Uses the normal draft so set entry survives recreation and Cancel never changes saved data. */
final class RecordedScoreUI {
    static void form(MainActivity a,JSONObject match){
        if(!match.optBoolean("androidSetEditor")){
            a.field(match,"setScores",SportRules.pointsStyle(match)?"Game scores":"Set scores",false);
            a.button("Enter individual sets or games",()->{try{JSONObject snapshot=copy(match);String issue=RecordedScores.apply(snapshot);if(issue!=null)throw new IllegalArgumentException(issue);put(match,"androidSetDraft",array(snapshot,"recordedSets"));put(match,"androidSetEditor",true);a.edit("matches",match,true);}catch(IllegalArgumentException e){a.error(e.getMessage());}});return;
        }
        JSONArray sets=array(match,"androidSetDraft");
        if(sets.length()==0)sets.put(obj());
        boolean points=SportRules.pointsStyle(match);
        for(int i=0;i<sets.length();i++){JSONObject set=sets.optJSONObject(i);int number=i+1;a.heading((points?"Game ":"Set ")+number);a.field(set,"yourGames",(points?"Game ":"Set ")+number+", your "+(points?"points":"games"),true);a.field(set,"opponentGames",(points?"Game ":"Set ")+number+", opponent "+(points?"points":"games"),true);if(!points){a.toggle(set,"hasTiebreak","Set "+number+", tie-break played",false);a.field(set,"yourTiebreak","Set "+number+", your tie-break points (optional)",true);a.field(set,"opponentTiebreak","Set "+number+", opponent tie-break points (optional)",true);}}
        if(sets.length()<RecordedScores.maximum(match))a.button(points?"Add game":"Add set",()->{sets.put(obj());a.edit("matches",match,true);});
        a.button(points?"Remove last game":"Remove last set",()->{sets.remove(sets.length()-1);a.edit("matches",match,true);});
        a.note("Leave unused rows empty. Tie-break points belong to their set. Choose Retired above for an unfinished final set.");
    }
    static String prepare(JSONObject match){
        if(!match.optBoolean("androidSetEditor"))return null;JSONArray entered=new JSONArray();boolean gap=false;
        for(JSONObject row:rows(array(match,"androidSetDraft"))){boolean any=row.has("yourGames")||row.has("opponentGames")||row.has("yourTiebreak")||row.has("opponentTiebreak");if(!any){gap=true;continue;}if(gap)return "Complete or remove the empty set before the next score.";for(String key:new String[]{"yourGames","opponentGames"})if(!row.has(key)||row.optInt(key,-1)<0||row.optInt(key)>999999)return "Enter both scores as non-negative whole numbers.";if(row.optBoolean("hasTiebreak"))for(String key:new String[]{"yourTiebreak","opponentTiebreak"})if(!row.has(key)||row.optInt(key,-1)<0||row.optInt(key)>999999)return "Enter both tie-break point scores or turn off tie-break played.";entered.put(copy(row));}
        put(match,"setScores",RecordedScores.format(entered));put(match,"recordedSets",entered);return null;
    }
}
