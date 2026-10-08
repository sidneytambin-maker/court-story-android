package com.courtstory.app;
import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;
import static com.courtstory.app.Domain.*;
public class ProfileHistoryTest {
    JSONObject library(){JSONObject d=newLibrary(),p=obj();put(p,"id",id());put(p,"name","Historical athlete");table(d,"players").put(p);put(d,"selectedPlayerID",p.optString("id"));JSONObject match=obj();put(match,"id",id());put(match,"playerID",p.optString("id"));put(match,"status","Completed");table(d,"matches").put(match);return d;}
    void remove(JSONObject d){JSONObject p=table(d,"players").optJSONObject(0);ProfileHistory.retain(d,p);array(d,"deletedRecordIDs").put(p.optString("id"));table(d,"players").remove(0);d.remove("selectedPlayerID");}
    @Test public void explicitRetainedOwnerIsValidButMissingOwnerIsNot(){JSONObject d=library();remove(d);assertNull(validate(d,false));assertFalse(ProfileHistory.empty(d));put(d,ProfileHistory.KEY,new JSONArray());assertEquals("An activity refers to a missing player.",validate(d,false));}
    @Test public void retainedProfileCannotAlsoBeActive(){JSONObject d=library();ProfileHistory.retain(d,table(d,"players").optJSONObject(0));assertNotNull(validate(d,false));}
    @Test public void remoteRemovalKeepsHistoryAndRepairsSelection() throws Exception {JSONObject base=library(),remote=copy(base);remove(remote);JSONObject merged=WatchMerge.merge(base,remote,base);assertEquals(0,table(merged,"players").length());assertEquals(1,table(merged,"matches").length());assertTrue(ProfileHistory.has(merged));assertFalse(merged.has("selectedPlayerID"));assertNull(validate(merged,false));}
    @Test public void historicalLibraryIsNotOverwrittenByDifferentIncomingLibrary(){JSONObject local=library();remove(local);try{WatchMerge.merge(local,library(),null);fail("Different library must remain separate");}catch(java.io.IOException expected){assertTrue(ProfileHistory.has(local));}}
    @Test public void changedProfileBlocksIncomingDeletion() throws Exception {JSONObject base=library(),local=copy(base),remote=copy(base);put(table(local,"players").optJSONObject(0),"notes","Unsynchronised feedback");remove(remote);JSONObject result=WatchMerge.merge(local,remote,base);assertEquals(1,table(result,"players").length());assertEquals(1,array(result,"androidWatchConflicts").length());assertFalse(ProfileHistory.has(result));assertNull(validate(result,false));}
}
