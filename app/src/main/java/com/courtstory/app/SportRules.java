package com.courtstory.app;
import org.json.*;
import static com.courtstory.app.Domain.*;

/** Explicit per-match rules; custom sports require their rules to be chosen. */
public final class SportRules {
    public static boolean racketPoints(JSONObject m){String s=m.optString("sport","Tennis");return !(s.equals("Tennis")||s.equals("Padel")||s.equals("Beach tennis")||s.equals("Platform tennis"));}
    public static void defaults(JSONObject m){
        String s=m.optString("sport","Tennis");
        missing(m,"scoringStyle",racketPoints(m)?s.equals("Pickleball")?"Side-out":"Rally points":"Tennis games");
        missing(m,"pointsTarget",s.equals("Badminton")||s.equals("Racketlon")?21:11);
        missing(m,"pointsCap",s.equals("Badminton")?30:0);missing(m,"pointsWinBy",2);
        missing(m,"gamesToWin",s.equals("Table tennis")||s.equals("Squash")||s.equals("Racquetball")?3:2);
        missing(m,"suddenDeathDeuce",s.equals("Beach tennis"));
        missing(m,"padelDeuceRule","Star point");missing(m,"decidingMatchTiebreak",s.equals("Beach tennis"));
        missing(m,"matchFormat",s.equals("Tennis")?"1 set":"Best of 3");
    }
    static void missing(JSONObject m,String key,Object value){if(!m.has(key))put(m,key,value);}
    public static boolean pointsStyle(JSONObject m){defaults(m);return !m.optString("scoringStyle").equals("Tennis games");}
    static boolean twoServers(JSONObject m){return m.optString("matchType").equals("Doubles")&&(m.optString("sport").equals("Pickleball")||m.optString("sport").equals("Racquetball"));}
    public static void rally(Scoring e,boolean player){
        JSONObject m=e.match;defaults(m);if(m.optString("sport").equals("Racketlon")){RacketlonScoring.point(e,player);return;}e.checkpoint();
        boolean sideOut=m.optString("scoringStyle").equals("Side-out"),serving=e.state.optBoolean("playerServing",true);
        missing(e.state,"gameFirstServer",serving);int server=e.state.optInt("serverNumber",twoServers(m)?2:1);
        if(!sideOut||player==serving){String k=player?"playerPoints":"opponentPoints";e.n(k,e.n(k)+1);}
        if(sideOut||m.optString("sport").equals("Racquetball")){
            if(player!=serving){if(twoServers(m)&&server==1)put(e.state,"serverNumber",2);else{put(e.state,"playerServing",!serving);put(e.state,"serverNumber",1);}}
        }else if(m.optString("sport").equals("Table tennis")){
            int total=e.n("playerPoints")+e.n("opponentPoints"),target=m.optInt("pointsTarget",11);
            if(total%2==0||e.n("playerPoints")>=target-1&&e.n("opponentPoints")>=target-1)put(e.state,"playerServing",!serving);
        }else put(e.state,"playerServing",player);
        int a=e.n("playerPoints"),b=e.n("opponentPoints"),target=m.optInt("pointsTarget",11),margin=m.optInt("pointsWinBy",2),cap=m.optInt("pointsCap",0);
        if(Math.max(a,b)>=target&&(Math.abs(a-b)>=margin||cap>0&&Math.max(a,b)>=cap)){
            array(e.state,"completedSetScores").put(a+"-"+b);String k=a>b?"playerSets":"opponentSets";e.n(k,e.n(k)+1);
            e.n("playerPoints",0);e.n("opponentPoints",0);
            boolean next=m.optString("sport").equals("Table tennis")?!e.state.optBoolean("gameFirstServer",true):a>b;
            put(e.state,"playerServing",next);put(e.state,"gameFirstServer",next);put(e.state,"serverNumber",twoServers(m)?2:1);
            put(e.state,"isMatchComplete",Math.max(e.n("playerSets"),e.n("opponentSets"))>=m.optInt("gamesToWin",2));
        }e.persist();
    }
    static boolean validRecordedGame(JSONObject m,int a,int b,int index){
        int high=Math.max(a,b),low=Math.min(a,b),diff=high-low;
        if(pointsStyle(m)){int target=m.optInt("pointsTarget"),margin=m.optInt("pointsWinBy"),cap=m.optInt("pointsCap");return (cap==0||high<=cap)&&high>=target&&(diff>=margin||cap>0&&high==cap)&&(high==target||diff==margin||cap>0&&high==cap&&diff<margin);}
        Scoring scorer=new Scoring(m);
        if(scorer.setsNeeded()>1&&m.optBoolean("decidingMatchTiebreak")&&index==2*(scorer.setsNeeded()-1))return high>=10&&(high==10?diff>=2:diff==2);
        if(high==6)return low<=4;if(high==7)return low==5||low==6&&!m.optString("tieBreakRule").equals("No automatic tie-break");
        return m.optString("tieBreakRule").equals("No automatic tie-break")&&high>7&&diff==2;
    }
    static String validate(JSONObject match){
        defaults(match);if(!pointsStyle(match))return null;
        int target=match.optInt("pointsTarget"),margin=match.optInt("pointsWinBy"),cap=match.optInt("pointsCap"),games=match.optInt("gamesToWin");
        if(target<1||target>99)return "Game targets must be between 1 and 99 points.";
        if(margin<1||margin>10)return "Winning margins must be between 1 and 10 points.";
        if(cap<0||cap>199||cap>0&&cap<target)return "The point cap must be zero for no cap, or at least the game target and no more than 199.";
        if(games<1||games>9)return "Choose between 1 and 9 games needed to win.";
        return null;
    }
    public static String[] surfaces(JSONObject r){if(CustomScore.custom(r))return split("Not specified|Indoor court|Outdoor court|Wooden sports floor|Synthetic sports floor|Other");return switch(r.optString("sport","Tennis")){case "Badminton","Squash","Racquetball"->split("Not specified|Wooden sports floor|Rubber sports floor|Synthetic sports floor|Other");case "Table tennis"->split("Not specified|Indoor table|Outdoor table|Other");case "Beach tennis"->split("Sand|Other");default->split("Not specified|Hard court|Clay|Grass|Carpet|Artificial grass|Artificial clay|Wooden sports floor|Rubber sports floor|Other");};}
    static String[] customTraining(){return split("Skills practice|Team practice|One-to-one coaching|Group coaching|Scoring practice|Defensive drills|Movement and positioning|Communication|Fitness and conditioning|Match practice|Other");}
    public static String[] focus(JSONObject r){if(CustomScore.custom(r))return split("Not specified|Scoring|Defending|Accuracy|Positioning|Movement|Communication|Teamwork|Tactics|Fitness|Other");return switch(r.optString("sport","Tennis")){case "Badminton"->split("Not specified|Serving|Returning|Clears|Drops|Smashes|Net play|Footwork|Defence|Tactics|Fitness|Other");case "Pickleball"->split("Not specified|Serving|Returning|Dinking|Third-shot drops|Drives|Volleys|Resets|Transition play|Positioning|Tactics|Fitness|Other");case "Padel"->split("Not specified|Serving|Returning|Wall play|Lobs|Bandeja|Vibora|Volleys|Smashes|Court positioning|Tactics|Fitness|Other");case "Squash","Racquetball"->split("Not specified|Serving|Returning|Drives|Drops|Boasts|Lobs|Court positioning|Movement|Tactics|Fitness|Other");case "Table tennis"->split("Not specified|Serving|Receiving|Forehand topspin|Backhand topspin|Pushes|Blocks|Spin variation|Footwork|Tactics|Fitness|Other");default->split("Not specified|Serving|Returning|Forehand|Backhand|Volleys|Movement and positioning|Rally consistency|Tactics|Match confidence|Fitness|Other");};}
    public static String rulesLabel(JSONObject r){return switch(r.optString("sport","Tennis")){
        case "Racquetball" -> "IRF 2026–2028: rally scoring, best of five games to 11, win by two.";
        case "Badminton" -> "BWF current format: best of three to 21, win by two, capped at 30. The 15-point format takes effect on 4 January 2027.";
        case "Table tennis" -> "ITTF: games to 11, win by two. Best of five default; match length is customisable.";
        case "Squash" -> "World Squash: rally scoring to 11, win by two; best of five default.";
        case "Pickleball" -> "USA Pickleball side-out format: games to 11, win by two; best of three default. Only the serving side scores.";
        case "Padel" -> "FIP 2026: Star Point default. Advantage and golden-point formats are also available.";
        case "Beach tennis" -> "ITF 2026: no-ad games, seven-point set tie-breaks; ten-point match tie-break at one set all.";
        case "Platform tennis" -> "APTA: singles uses no-ad; standard doubles uses advantage. Select No-ad for events using that format. Seven-point set tie-breaks; best of three default.";
        case "Racketlon" -> "FIR: table tennis, badminton, squash, then tennis. Four games to 21, win by two; total points decide the match. A tied total leads to one Gummiarm point.";
        default -> "ITF: advantage games, six-game sets and seven-point tie-breaks at six all. Select your match length below.";
    };}
    public static void form(MainActivity a,JSONObject r){
        defaults(r);a.heading(r.optString("sport","Tennis")+" scoring");a.note("Reference format: "+rulesLabel(r));a.note("Saved scoring system: "+r.optString("scoringStyle"));
        a.note("Selected settings are saved with this match. Local competition formats can be customised below.");
        if(pointsStyle(r)){
            boolean aggregate=r.optString("sport").equals("Racketlon");
            a.note("Game target: "+r.optInt("pointsTarget")+" • Win by "+r.optInt("pointsWinBy")+(aggregate?" • Four-discipline aggregate":" • "+r.optInt("gamesToWin")+" games to win"));
            a.optional("Customise game rules",()->{a.field(r,"pointsTarget","Points to win a game",true);a.field(r,"pointsWinBy","Winning margin",true);if(!aggregate){a.field(r,"pointsCap","Point cap (0 for no cap)",true);a.field(r,"gamesToWin","Games needed to win the match",true);}if(r.optString("sport").equals("Pickleball"))a.note("Doubles begins with one server on the first serving side, then two servers per side.");});
        }else if(r.optString("sport").equals("Platform tennis"))a.choice(r,"platformDeuceRule","Deuce format",split("Automatic|Advantage|No-ad"));else if(r.optString("sport").equals("Padel"))a.choice(r,"padelDeuceRule","Deuce format",split("Star point|Advantage|Golden point"));
    }
}
