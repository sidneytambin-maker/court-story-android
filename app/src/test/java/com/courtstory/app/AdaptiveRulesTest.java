package com.courtstory.app;
import org.junit.Test;import org.json.*;import static org.junit.Assert.*;import static com.courtstory.app.Domain.*;
public class AdaptiveRulesTest {
 JSONObject p(String category){JSONObject p=obj();put(p,"playerType",category);return p;}
 @Test public void wheelchairSportSpecific(){JSONObject p=p("Wheelchair");for(String sport:new String[]{"Tennis","Pickleball","Racquetball"})assertEquals(2,AdaptiveRules.bounces(sport,p));assertEquals(0,AdaptiveRules.bounces("Badminton",p));assertEquals(-1,AdaptiveRules.bounces("Padel",p));}
 @Test public void deafAndLearningDoNotInventExtraBounces(){for(String category:new String[]{"Deaf","Learning disability"}){assertEquals(1,AdaptiveRules.bounces("Tennis",p(category)));assertEquals(0,AdaptiveRules.bounces("Badminton",p(category)));}}
 @Test public void viClassificationAndUnknown(){JSONObject p=p("Visually impaired");assertEquals(-1,AdaptiveRules.bounces("Tennis",p));for(int i=1;i<=4;i++){put(p,"sightLevel","B"+i);assertEquals(i<=2?3:i==3?2:1,AdaptiveRules.bounces("Tennis",p));}assertEquals(-1,AdaptiveRules.bounces("Padel",p));}
 @Test public void explicitZeroOverrideIsPreserved(){JSONObject p=p("Wheelchair"),m=obj();put(m,"sport","Tennis");put(p,"bounceAllowance",0);put(p,"communicationPreferences","Visual start signal");AdaptiveRules.apply(m,p);assertEquals(0,m.optInt("allowedBounces",-1));assertEquals("Visual start signal",m.optString("communicationPreferences"));assertEquals(0,p.optInt("bounceAllowance",-1));}
 @Test public void subjectWithoutPreferencesClearsCoachsValues(){JSONObject match=obj();put(match,"sport","Tennis");JSONObject coach=p("Wheelchair");put(coach,"adaptivePreferences","Step-free access");AdaptiveRules.apply(match,coach);assertTrue(match.has("adaptivePreferences"));AdaptiveRules.apply(match,p("Deaf"));assertEquals(1,match.optInt("allowedBounces"));assertFalse(match.has("adaptivePreferences"));}
}
