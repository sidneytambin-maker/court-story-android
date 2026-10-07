package com.courtstory.app;
import org.json.*;
import java.util.*;
import static com.courtstory.app.Domain.*;
/** Numeric events support any number of named sides, independently of tennis scoring. */
public final class CustomScore {
    public static boolean custom(JSONObject event){return custom(event.optString("sport","Tennis"));}
    public static boolean custom(String sport){return !Arrays.asList("Tennis","Padel","Pickleball","Badminton","Squash","Table tennis","Racquetball","Racketlon","Beach tennis","Platform tennis").contains(sport);}
    public static void defaults(JSONObject event){
        put(event,"scoringStyle","Numeric totals");
        if(!event.has("customTeams")){JSONArray sides=new JSONArray();for(int i=1;i<=2;i++){JSONObject side=obj();put(side,"id",id());put(side,"name","Team "+i);put(side,"score",0);sides.put(side);}put(event,"customTeams",sides);}
        if(!event.has("customKind"))put(event,"customKind","Teams");if(!event.has("customUnit"))put(event,"customUnit","Points");
        if(!event.has("customWinningRule"))put(event,"customWinningRule","Target score");if(!event.has("customTarget"))put(event,"customTarget",10);
        if(!event.has("customWinBy"))put(event,"customWinBy",1);if(!event.has("customPeriods"))put(event,"customPeriods",1);if(!event.has("customPeriodMinutes"))put(event,"customPeriodMinutes",10);
        if(!event.has("customPeriod"))put(event,"customPeriod",1);if(!event.has("customOwnerSide"))put(event,"customOwnerSide",0);
    }
    public static String validate(JSONObject event){defaults(event);JSONArray sides=array(event,"customTeams");if(sides.length()<2)return "Add at least two players or teams.";Set<String> names=new HashSet<>();for(JSONObject side:rows(sides)){String name=side.optString("name").trim();if(name.isEmpty())return "Give every player or team a name.";if(!names.add(name.toLowerCase(Locale.ROOT)))return "Use different names so each score is clear.";if(side.optInt("score",-1)<0)return "Scores must be zero or higher.";}
        if(event.optInt("customTarget")<1||event.optInt("customWinBy")<1)return "Score targets and winning margins must be positive.";
        if(event.optInt("customPeriods")<1||event.optInt("customPeriods")>99||event.optInt("customPeriodMinutes")<1||event.optInt("customPeriodMinutes")>240)return "Choose 1 to 99 periods, each lasting 1 to 240 minutes.";
        if(event.optInt("customOwnerSide")<0||event.optInt("customOwnerSide")>=sides.length())return "Choose which side this profile represents.";return null;
    }
    static final String[] STATE={"customTeams","customPeriod","customClockRemainingMs","customClockStartedAt","status","result","actualStart","actualFinish","customScoreSummary"};
    static void checkpoint(JSONObject event){JSONObject saved=obj();for(String key:STATE)if(event.has(key))put(saved,key,event.opt(key));array(event,"customHistory").put(copy(saved));}
    public static boolean undo(JSONObject event){JSONArray history=array(event,"customHistory");if(history.length()==0)return false;JSONObject saved=history.optJSONObject(history.length()-1);for(String key:STATE){event.remove(key);if(saved.has(key))put(event,key,saved.opt(key));}history.remove(history.length()-1);return true;}
    public static boolean add(JSONObject event,int index,int delta){defaults(event);if(event.optString("status").equals("Completed"))return false;JSONObject side=array(event,"customTeams").optJSONObject(index);if(side==null||side.optInt("score")+delta<0||side.optInt("score")+((long)delta)>Integer.MAX_VALUE)return false;checkpoint(event);put(side,"score",side.optInt("score")+delta);put(event,"status","In progress");if(!event.has("actualStart"))put(event,"actualStart",now());put(event,"customScoreSummary",summary(event));if(winner(event)>=0)complete(event);return true;}
    public static int winner(JSONObject event){JSONArray sides=array(event,"customTeams");int best=-1,top=-1,second=-1;for(int i=0;i<sides.length();i++){int score=sides.optJSONObject(i).optInt("score");if(score>top){second=top;top=score;best=i;}else second=Math.max(second,score);}if(top==second)return -1;String rule=event.optString("customWinningRule");if(rule.equals("Manual finish"))return -1;if(rule.equals("Lead by"))return top-second>=event.optInt("customTarget",10)?best:-1;return top>=event.optInt("customTarget",10)&&top-second>=event.optInt("customWinBy",1)?best:-1;}
    public static void finish(JSONObject event){checkpoint(event);complete(event);}
    static void complete(JSONObject event){JSONArray sides=array(event,"customTeams");int top=-1,count=0,best=-1;for(int i=0;i<sides.length();i++){int score=sides.optJSONObject(i).optInt("score");if(score>top){top=score;best=i;count=1;}else if(score==top)count++;}put(event,"result",count>1?"Draw":best==event.optInt("customOwnerSide")?"Win":"Loss");put(event,"status","Completed");put(event,"actualFinish",now());put(event,"customScoreSummary",summary(event));pause(event);}
    public static String summary(JSONObject event){List<String> scores=new ArrayList<>();for(JSONObject side:rows(array(event,"customTeams")))scores.add(side.optString("name")+": "+side.optInt("score"));return String.join(" • ",scores);}
    public static long remaining(JSONObject event){long left=event.optLong("customClockRemainingMs",event.optInt("customPeriodMinutes",10)*60000L);long started=event.optLong("customClockStartedAt",0);return Math.max(0,left-(started>0?Math.max(0,System.currentTimeMillis()-started):0));}
    public static void pause(JSONObject event){put(event,"customClockRemainingMs",remaining(event));event.remove("customClockStartedAt");}
    public static void start(JSONObject event){if(event.optString("status").equals("Completed")||remaining(event)==0||event.has("customClockStartedAt"))return;put(event,"customClockRemainingMs",remaining(event));put(event,"customClockStartedAt",System.currentTimeMillis());put(event,"status","In progress");if(!event.has("actualStart"))put(event,"actualStart",now());}
}
