package com.courtstory.app;
import static com.courtstory.app.Domain.*;

/** FIR aggregate scoring. An unfinished final discipline retains its actual score. */
final class RacketlonScoring {
    static final String[] DISCIPLINES={"Table tennis","Badminton","Squash","Tennis"};
    static void point(Scoring e,boolean player){
        e.checkpoint();
        if(e.state.optBoolean("gummiarm")){
            put(e.state,"gummiarmPlayed",true);finish(e,player);e.persist();return;
        }
        boolean serving=e.state.optBoolean("playerServing",true);
        SportRules.missing(e.state,"gameFirstServer",serving);
        String k=player?"playerPoints":"opponentPoints";e.n(k,e.n(k)+1);
        k=player?"aggregatePlayerPoints":"aggregateOpponentPoints";e.n(k,e.n(k)+1);
        int a=e.n("playerPoints"),b=e.n("opponentPoints"),target=e.match.optInt("pointsTarget",21),margin=e.match.optInt("pointsWinBy",2),index=e.n("disciplineIndex");
        if((a+b)%2==0||a>=target-1&&b>=target-1)put(e.state,"playerServing",!serving);
        boolean end=Math.max(a,b)>=target&&Math.abs(a-b)>=margin;
        // Maximum possible remaining losing margin: this game's minimum opponent
        // winning total, then one full target per remaining discipline.
        int diff=e.n("aggregatePlayerPoints")-e.n("aggregateOpponentPoints");
        int remaining=3-index;
        boolean clinchA=diff-(Math.max(target,a+margin)-b)-remaining*target>0;
        boolean clinchB=-diff-(Math.max(target,b+margin)-a)-remaining*target>0;
        if(end||clinchA||clinchB){
            array(e.state,"completedSetScores").put(a+"-"+b);
            if(end){k=a>b?"playerSets":"opponentSets";e.n(k,e.n(k)+1);}
            if(clinchA||clinchB){finish(e,clinchA);e.persist();return;}
            if(index==3){
                if(diff==0){put(e.state,"gummiarm",true);}
                else finish(e,diff>0);
            }else{
                e.n("disciplineIndex",index+1);e.n("playerPoints",0);e.n("opponentPoints",0);
                boolean next=!e.state.optBoolean("gameFirstServer",true);put(e.state,"playerServing",next);put(e.state,"gameFirstServer",next);
                if(Math.abs(diff)>(3-index)*target)finish(e,diff>0);
            }
        }e.persist();
    }
    static void finish(Scoring e,boolean player){put(e.state,"racketlonPlayerWon",player);put(e.state,"isMatchComplete",true);}
    static String display(Scoring e){return e.state.optBoolean("gummiarm")?"Gummiarm — one deciding point":DISCIPLINES[Math.min(3,e.n("disciplineIndex"))]+" "+e.n("playerPoints")+" – "+e.n("opponentPoints");}
    static String spoken(Scoring e){String total="Aggregate "+e.n("aggregatePlayerPoints")+" to "+e.n("aggregateOpponentPoints");return e.complete()?e.team(e.winner())+" wins. "+total+(e.state.optBoolean("gummiarmPlayed")?". Gummiarm point won.":"."):display(e)+". "+total+(e.state.optBoolean("gummiarm")?". Toss for serve; one service only.":". Serving: "+e.team(e.state.optBoolean("playerServing",true))+".");}
}
