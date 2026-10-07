package com.courtstory.app;

import org.json.*;
import java.util.*;
import static com.courtstory.app.Domain.*;

/** Deterministic tennis scorer. Every mutation, including tie-breaks, is undoable and persisted. */
public final class Scoring {
    public JSONObject state;
    public JSONArray history;
    public final JSONObject match;
    public Scoring(JSONObject match){this.match=match;state=copy(object(match,"liveScore"));history=array(match,"androidScoreHistory");}
    int n(String key){return state.optInt(key);}
    void n(String key,int value){put(state,key,value);}
    public boolean complete(){return state.optBoolean("isMatchComplete");}
    public boolean tie(){return state.optBoolean("isTiebreak");}
    void checkpoint(){history.put(copy(state));}
    void persist(){put(match,"liveScore",state);put(match,"androidScoreHistory",history);put(match,"yourSetsWon",n("playerSets"));put(match,"opponentSetsWon",n("opponentSets"));JSONArray sets=array(state,"completedSetScores");List<String>s=new ArrayList<>();for(int i=0;i<sets.length();i++)s.add(sets.optString(i));put(match,"setScores",String.join(", ",s));if(complete()){put(match,"status","Completed");put(match,"actualFinish",now());put(match,"result",state.optString("manualResult",winner()?"Win":"Loss"));}else {put(match,"status","In progress");match.remove("actualFinish");put(match,"result","Not recorded");}}
    public void finishEarly(String result){if(complete())return;checkpoint();put(state,"manualResult",result);put(state,"isMatchComplete",true);persist();}
    boolean noAd(){if(match.optString("sport").equals("Platform tennis")){String rule=match.optString("platformDeuceRule","Automatic");return rule.equals("No-ad")||rule.equals("Automatic")&&match.optString("matchType","Singles").equals("Singles");}return match.optBoolean("suddenDeathDeuce",false);}
    boolean winner(){return match.optString("sport").equals("Racketlon")?state.optBoolean("racketlonPlayerWon"):n("playerSets")>n("opponentSets");}
    int setsNeeded(){String f=match.optString("matchFormat","1 set");return f.equals("Best of 5")?3:f.equals("Best of 3")?2:f.equals("Custom")?match.optInt("customSetsToWin",2):1;}
    public void point(boolean player){
        if(complete())return;if(SportRules.pointsStyle(match)){SportRules.rally(this,player);return;}checkpoint();
        if(!tie()&&match.optBoolean("decidingMatchTiebreak")&&setsNeeded()>1&&n("playerSets")==setsNeeded()-1&&n("opponentSets")==setsNeeded()-1&&n("playerGames")==0&&n("opponentGames")==0){put(state,"isTiebreak",true);put(state,"isMatchTiebreak",true);put(state,"tieTarget",10);}
        if(n("playerGames")==6&&n("opponentGames")==6&&!tie()){
            String r=match.optString("tieBreakRule","Standard at 6-6");
            if(r.equals("Standard at 6-6")||r.equals("10-point tie-break at 6-6")||r.equals("Match tie-break")||r.equals("10-point deciding-set tie-break at 6-6")){put(state,"isTiebreak",true);put(state,"tieTarget",r.equals("Standard at 6-6")||r.equals("10-point deciding-set tie-break at 6-6")&&!(n("playerSets")==setsNeeded()-1&&n("opponentSets")==setsNeeded()-1)?7:10);}
        }
        String key=player?"playerPoints":"opponentPoints";n(key,n(key)+1);put(state,"lastPointWinner",player?"player":"opponent");
        int a=n("playerPoints"),b=n("opponentPoints"),margin=Math.abs(a-b);
        if(tie()){
            int target=state.optInt("tieTarget",match.optInt("tieBreakTarget",7));
            if(Math.max(a,b)>=target&&(!match.optBoolean("tieBreakWinByTwo",true)||margin>=2))game(a>b,true);
        }else{
            boolean noAd=noAd(),star=false;
            if(match.optString("sport").equals("Padel")){String rule=match.optString("padelDeuceRule","Star point");noAd=rule.equals("Golden point");star=rule.equals("Star point")&&Math.min(a,b)>=5;}
            if(Math.max(a,b)>=4&&(margin>=2||(noAd||star)&&margin>=1))game(a>b,false);
        }persist();
    }
    void game(boolean player,boolean wasTie){
        boolean matchTie=state.optBoolean("isMatchTiebreak");int pa=n("playerPoints"),pb=n("opponentPoints");
        String key=player?"playerGames":"opponentGames";n(key,n(key)+1);n("playerPoints",0);n("opponentPoints",0);put(state,"isTiebreak",false);put(state,"isMatchTiebreak",false);
        int a=n("playerGames"),b=n("opponentGames");
        if(matchTie||Math.max(a,b)>=6&&(Math.abs(a-b)>=2||wasTie)){
            array(state,"completedSetScores").put(matchTie?pa+"-"+pb:a+"-"+b);key=player?"playerSets":"opponentSets";n(key,n(key)+1);n("playerGames",0);n("opponentGames",0);
            put(state,"isMatchComplete",Math.max(n("playerSets"),n("opponentSets"))>=setsNeeded());
            if(!complete()&&match.optBoolean("decidingMatchTiebreak")&&n("playerSets")==setsNeeded()-1&&n("opponentSets")==setsNeeded()-1){put(state,"isTiebreak",true);put(state,"isMatchTiebreak",true);put(state,"tieTarget",10);}
        }
    }
    public boolean undo(){if(history.length()==0)return false;state=copy(history.optJSONObject(history.length()-1));history.remove(history.length()-1);persist();return true;}
    public void startTie(){if(tie()||complete()||SportRules.pointsStyle(match))return;checkpoint();n("playerPoints",0);n("opponentPoints",0);put(state,"isTiebreak",true);put(state,"tieTarget",match.optInt("tieBreakTarget",7));persist();}
    public void finishTie(boolean player){if(!tie()||complete())return;checkpoint();game(player,true);persist();}
    public String team(boolean player){String a=match.optString(player?"playerName":"opponentName",player?"Player":"Opponent");String b=match.optString(player?"partnerName":"opponent2Name");return match.optString("matchType").equals("Doubles")&&!b.trim().isEmpty()?a+" and "+b:a;}
    public String points(){if(complete())return "Match complete";int a=n("playerPoints"),b=n("opponentPoints");if(match.optString("sport").equals("Racketlon"))return RacketlonScoring.display(this);if(SportRules.pointsStyle(match))return a+" – "+b;if(tie())return "Tie-break "+a+"–"+b;if(a>=3&&b>=3){if(a==b){if(match.optString("sport").equals("Padel")){String rule=match.optString("padelDeuceRule","Star point");if(rule.equals("Star point")&&a>=5)return "Star point — deciding point";if(rule.equals("Golden point"))return "Golden point — deciding point";}return noAd()?"Sudden-death deuce":"Deuce";}return "Advantage "+team(a>b);}String[] x={"Love","15","30","40"};return x[Math.min(3,a)]+" – "+x[Math.min(3,b)];}
    public String spoken(){if(complete()&&state.has("manualResult"))return "Match finished early. Result: "+state.optString("manualResult")+". "+match.optString("setScores");if(match.optString("sport").equals("Racketlon"))return RacketlonScoring.spoken(this);if(complete())return team(winner())+" wins. "+match.optString("setScores");if(SportRules.pointsStyle(match))return points()+". Games "+n("playerSets")+" to "+n("opponentSets")+((match.optString("scoringStyle").equals("Side-out")||match.optString("sport").equals("Table tennis")||match.optString("sport").equals("Racquetball"))?". Serving: "+team(state.optBoolean("playerServing",true))+(SportRules.twoServers(match)?", server "+state.optInt("serverNumber",2):""):"");return points()+". Games "+n("playerGames")+" to "+n("opponentGames")+". Sets "+n("playerSets")+" to "+n("opponentSets")+". "+team(true)+" against "+team(false)+".";}
}



