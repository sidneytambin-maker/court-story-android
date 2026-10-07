package com.courtstory.app;
import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;
import static com.courtstory.app.Domain.*;

public class DomainTest {
    JSONObject match(String format){JSONObject m=obj();put(m,"matchFormat",format);put(m,"playerName","Alex");put(m,"opponentName","Sam");put(m,"suddenDeathDeuce",false);return m;}
    void game(Scoring s,boolean a){for(int i=0;i<4;i++)s.point(a);}
    @Test public void straightSetAndUndoWinningPoint(){JSONObject m=match("1 set");Scoring s=new Scoring(m);for(int i=0;i<6;i++)game(s,true);assertTrue(s.complete());assertEquals("6-0",m.optString("setScores"));assertEquals("Completed",m.optString("status"));assertTrue(s.undo());assertFalse(s.complete());assertEquals(5,s.state.optInt("playerGames"));assertEquals(3,s.state.optInt("playerPoints"));assertFalse(m.has("actualFinish"));}
    @Test public void advantageRequiresTwoPoints(){Scoring s=new Scoring(match("1 set"));for(int i=0;i<3;i++){s.point(true);s.point(false);}assertEquals("Deuce",s.points());s.point(true);assertEquals(0,s.state.optInt("playerGames"));s.point(false);assertEquals("Deuce",s.points());s.point(false);s.point(false);assertEquals(1,s.state.optInt("opponentGames"));}
    @Test public void suddenDeath(){JSONObject m=match("1 set");put(m,"suddenDeathDeuce",true);Scoring s=new Scoring(m);for(int i=0;i<3;i++){s.point(true);s.point(false);}s.point(true);assertEquals(1,s.state.optInt("playerGames"));}
    @Test public void tiebreakAtSixAll(){Scoring s=new Scoring(match("1 set"));for(int i=0;i<6;i++){game(s,true);game(s,false);}for(int i=0;i<6;i++){s.point(true);s.point(false);}s.point(true);assertFalse(s.complete());s.point(true);assertTrue(s.complete());assertEquals("7-6",s.match.optString("setScores"));}
    @Test public void advantageSetDoesNotEndAtSevenSix(){JSONObject m=match("1 set");put(m,"tieBreakRule","No automatic tie-break");Scoring s=new Scoring(m);for(int i=0;i<6;i++){game(s,true);game(s,false);}game(s,true);assertFalse(s.complete());game(s,true);assertTrue(s.complete());assertEquals("8-6",m.optString("setScores"));}
    @Test public void bestOfThreeNeedsTwoSets(){Scoring s=new Scoring(match("Best of 3"));for(int i=0;i<6;i++)game(s,true);assertFalse(s.complete());for(int i=0;i<6;i++)game(s,false);assertFalse(s.complete());for(int i=0;i<6;i++)game(s,true);assertTrue(s.complete());assertEquals(2,s.state.optInt("playerSets"));}
    @Test public void resumePreservesUndo(){JSONObject m=match("1 set");Scoring s=new Scoring(m);s.point(true);Scoring resumed=new Scoring(copy(m));assertEquals(1,resumed.state.optInt("playerPoints"));assertTrue(resumed.undo());assertEquals(0,resumed.state.optInt("playerPoints"));}
    @Test public void manualDurationWins(){JSONObject r=obj();put(r,"actualStart","2026-10-06T10:00:00Z");put(r,"actualFinish","2026-10-06T11:00:00Z");put(r,"durationMinutes",75);put(r,"durationSource","manual");assertEquals(75,duration(r));put(r,"durationSource","recorded");assertEquals(60,duration(r));}
    @Test public void rejectsMissingAndDuplicateIds(){JSONObject d=newLibrary();JSONObject p=obj();put(p,"id",id());put(p,"name","Test");table(d,"players").put(p);assertNull(validate(d,true));table(d,"players").put(copy(p));assertEquals("Duplicate record identifier.",validate(d,true));}
    @Test public void rejectsBrokenRelationships(){JSONObject d=newLibrary();JSONObject m=obj();put(m,"id",id());put(m,"playerID",id());table(d,"matches").put(m);assertEquals("An activity refers to a missing player.",validate(d,true));}
    @Test public void unknownFieldsSurvive(){JSONObject d=newLibrary();put(d,"futureExtension","preserve me");assertEquals("preserve me",copy(d).optString("futureExtension"));}
    @Test public void oldIosDate(){JSONObject d=obj();put(d,"date",0);assertEquals(978307200000L,millis(d,"date"));}
    @Test public void dateVariantsAndMissingValues(){JSONObject d=obj();assertEquals(0,millis(d,"date"));put(d,"date",JSONObject.NULL);assertEquals(0,millis(d,"date"));put(d,"date","");assertEquals(0,millis(d,"date"));put(d,"date","2026-10-07");assertEquals(java.time.LocalDate.of(2026,10,7).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),millis(d,"date"));put(d,"date","2026-10-07T10:30:00+01:00");assertEquals(java.time.Instant.parse("2026-10-07T09:30:00Z").toEpochMilli(),millis(d,"date"));put(d,"date","not a date");assertEquals(0,millis(d,"date"));}
}
