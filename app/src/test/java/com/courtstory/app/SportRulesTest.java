package com.courtstory.app;
import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;
import static com.courtstory.app.Domain.*;
public class SportRulesTest {
    JSONObject m(String sport){JSONObject m=obj();put(m,"sport",sport);put(m,"playerName","Alex");put(m,"opponentName","Sam");SportRules.defaults(m);return m;}
    @Test public void pickleballRequiresServe(){JSONObject m=m("Pickleball");Scoring s=new Scoring(m);s.point(false);assertEquals(0,s.state.optInt("opponentPoints"));assertFalse(s.state.optBoolean("playerServing",true));s.point(false);assertEquals(1,s.state.optInt("opponentPoints"));s.undo();assertEquals(0,s.state.optInt("opponentPoints"));}
    @Test public void pickleballDoublesFirstSideHasOneServer(){JSONObject m=m("Pickleball");put(m,"matchType","Doubles");Scoring s=new Scoring(m);s.point(false);assertFalse(s.state.optBoolean("playerServing",true));assertEquals(1,s.state.optInt("serverNumber"));s.point(true);assertFalse(s.state.optBoolean("playerServing",true));assertEquals(2,s.state.optInt("serverNumber"));s.point(true);assertTrue(s.state.optBoolean("playerServing"));}
    @Test public void tableTennisWinByTwo(){JSONObject m=m("Table tennis");put(m,"gamesToWin",1);Scoring s=new Scoring(m);for(int i=0;i<10;i++){s.point(true);s.point(false);}s.point(true);assertFalse(s.complete());s.point(true);assertTrue(s.complete());assertEquals("12-10",m.optString("setScores"));}
    @Test public void badmintonCap(){JSONObject m=m("Badminton");put(m,"gamesToWin",1);Scoring s=new Scoring(m);for(int i=0;i<29;i++){s.point(true);s.point(false);}s.point(true);assertTrue(s.complete());assertEquals("30-29",m.optString("setScores"));}
    @Test public void changingProfileDoesNotChangeExistingMatch(){JSONObject p=obj();put(p,"sport","Tennis");JSONObject m=m(p.optString("sport"));put(p,"sport","Squash");assertFalse(SportRules.pointsStyle(m));}
    @Test public void tableTennisServiceAlternatesByPointsAndAtDeuce(){JSONObject match=m("Table tennis");Scoring score=new Scoring(match);score.point(false);assertTrue(score.state.optBoolean("playerServing",true));score.point(true);assertFalse(score.state.optBoolean("playerServing",true));for(int i=1;i<10;i++){score.point(true);score.point(false);}boolean before=score.state.optBoolean("playerServing",true);score.point(true);assertEquals(!before,score.state.optBoolean("playerServing",true));score.undo();assertEquals(before,score.state.optBoolean("playerServing",true));}
    @Test public void rejectImpossiblePointRules(){JSONObject match=m("Badminton");put(match,"pointsTarget",0);assertNotNull(SportRules.validate(match));put(match,"pointsTarget",21);put(match,"pointsCap",15);assertNotNull(SportRules.validate(match));put(match,"pointsCap",30);assertNull(SportRules.validate(match));put(match,"gamesToWin",-1);assertNotNull(SportRules.validate(match));}
}
