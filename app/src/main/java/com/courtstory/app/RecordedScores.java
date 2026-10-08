package com.courtstory.app;

import org.json.*;
import java.util.*;
import java.util.regex.*;
import static com.courtstory.app.Domain.*;

/** One score model for typed results, individual set entry and corrected records. */
final class RecordedScores {
    static final Pattern SCORE=Pattern.compile("\\s*(\\d{1,6})\\s*[-–−]\\s*(\\d{1,6})(?:\\s*\\(\\s*(\\d{1,6})\\s*[-–−]\\s*(\\d{1,6})\\s*\\))?\\s*");
    static int maximum(JSONObject m){return m.optString("sport").equals("Racketlon")?4:SportRules.pointsStyle(m)?Math.max(1,m.optInt("gamesToWin",2))*2-1:new Scoring(copy(m)).setsNeeded()*2-1;}
    static JSONArray parse(String text){
        JSONArray sets=new JSONArray();if(text.trim().isEmpty())return sets;
        for(String part:text.split(",",-1)){Matcher found=SCORE.matcher(part);if(!found.matches())throw new IllegalArgumentException("Enter scores such as 6-4, 7-6 (7-5), separated by commas.");JSONObject set=obj();put(set,"yourGames",Integer.parseInt(found.group(1)));put(set,"opponentGames",Integer.parseInt(found.group(2)));if(found.group(3)!=null){put(set,"hasTiebreak",true);put(set,"yourTiebreak",Integer.parseInt(found.group(3)));put(set,"opponentTiebreak",Integer.parseInt(found.group(4)));}sets.put(set);}return sets;
    }
    static String format(JSONArray sets){List<String> parts=new ArrayList<>();for(JSONObject s:rows(sets)){String part=s.optInt("yourGames")+"-"+s.optInt("opponentGames");if(s.optBoolean("hasTiebreak")&&s.has("yourTiebreak")&&s.has("opponentTiebreak"))part+=" ("+s.optInt("yourTiebreak")+"-"+s.optInt("opponentTiebreak")+")";parts.add(part);}return String.join(", ",parts);}
    static String apply(JSONObject match){
        JSONArray sets;try{sets=parse(match.optString("setScores"));}catch(IllegalArgumentException e){return e.getMessage();}
        if(sets.length()>maximum(match))return "There are too many sets or games for the selected match format.";
        boolean retired=match.optString("result").equals("Retired"),racketlon=match.optString("sport").equals("Racketlon");
        int wins=0,losses=0,totalFor=0,totalAgainst=0;List<String> ties=new ArrayList<>();
        JSONArray old=match.optJSONArray("recordedSets");
        for(int i=0;i<sets.length();i++){
            JSONObject s=sets.optJSONObject(i);int a=s.optInt("yourGames"),b=s.optInt("opponentGames");
            // A plain score edit must not erase previously entered tie-break points for unchanged sets.
            if(!s.has("hasTiebreak")&&old!=null&&old.optJSONObject(i)!=null){JSONObject prior=old.optJSONObject(i);if(prior.optInt("yourGames")==a&&prior.optInt("opponentGames")==b&&prior.optBoolean("hasTiebreak")){for(String key:new String[]{"hasTiebreak","yourTiebreak","opponentTiebreak"})if(prior.has(key))put(s,key,prior.opt(key));}}
            if(s.optBoolean("hasTiebreak")){
                if(SportRules.pointsStyle(match))return "Game scores for this sport do not use tennis set tie-breaks.";
                if(!s.has("yourTiebreak")||!s.has("opponentTiebreak"))return "Set "+(i+1)+": enter both tie-break point scores.";
                int ta=s.optInt("yourTiebreak"),tb=s.optInt("opponentTiebreak");
                if(a==6&&b==6&&ta!=tb){if(ta>tb)a++;else b++;put(s,"yourGames",a);put(s,"opponentGames",b);}
                if(Integer.signum(a-b)!=Integer.signum(ta-tb))return "Set "+(i+1)+": games and tie-break points must show the same winner.";
                if(!retired){int target=match.optString("tieBreakRule").equals("10-point tie-break at 6-6")||match.optString("tieBreakRule").equals("10-point deciding-set tie-break at 6-6")&&i==maximum(match)-1?10:match.optString("tieBreakRule").equals("Manual or custom")?match.optInt("tieBreakTarget",7):7;int high=Math.max(ta,tb),margin=Math.abs(ta-tb);if(high<target||margin<(match.optBoolean("tieBreakWinByTwo",true)?2:1))return "Set "+(i+1)+": the tie-break has not reached its winning target and margin.";}
                ties.add("Set "+(i+1)+": "+ta+"-"+tb);
            }
            if(!retired&&(a==b||!racketlon&&!SportRules.validRecordedGame(copy(match),a,b,i)))return "Set or game "+(i+1)+" does not fit the selected rules. Check the score or customise the rules.";
            if(!retired&&!racketlon&&Math.max(wins,losses)>=(SportRules.pointsStyle(match)?match.optInt("gamesToWin",2):new Scoring(copy(match)).setsNeeded()))return "Remove scores entered after the match was already won.";
            if(a>b)wins++;else if(b>a)losses++;totalFor+=a;totalAgainst+=b;
        }
        put(match,"recordedSets",sets);put(match,"setScores",format(sets));put(match,"yourSetsWon",wins);put(match,"opponentSetsWon",losses);
        if(!ties.isEmpty()||sets.length()==0||match.optBoolean("androidSetEditor")){put(match,"hadTiebreak",!ties.isEmpty());put(match,"tiebreakScore",String.join("; ",ties));}
        if(sets.length()>0&&!retired)put(match,"result",racketlon?(object(match,"liveScore").optBoolean("gummiarmPlayed")?(object(match,"liveScore").optBoolean("racketlonPlayerWon")?"Win":"Loss"):totalFor>totalAgainst?"Win":totalFor<totalAgainst?"Loss":"Not recorded"):wins>losses?"Win":wins<losses?"Loss":"Draw");
        return null;
    }
}
