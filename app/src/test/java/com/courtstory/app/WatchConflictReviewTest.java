package com.courtstory.app;
import org.junit.Test;
import org.json.*;
import java.util.*;
import static org.junit.Assert.*;

public class WatchConflictReviewTest {
    @Test public void notesAndNestedCoachingChangesAreVisible() throws Exception {
        JSONObject left=new JSONObject("{\"notes\":\"Improve serve\",\"context\":{\"coachName\":\"Alex\"}}"),right=new JSONObject("{\"notes\":\"Improve return\",\"context\":{\"coachName\":\"Sam\"}}");
        String text=String.join("\n",WatchConflictReview.differences(left,right));assertTrue(text.contains("This device: Improve serve"));assertTrue(text.contains("Incoming: Improve return"));assertTrue(text.contains("Context · Coach Name"));assertTrue(text.contains("Incoming: Sam"));
    }
    @Test public void clearedValueIsExplicit() throws Exception {String text=String.join("\n",WatchConflictReview.differences(new JSONObject("{\"notes\":\"Keep this detail\"}"),new JSONObject()));assertTrue(text.contains("Incoming: Not set"));}
    @Test public void deletionAndUnchangedRecordsAreDistinct() throws Exception {JSONObject record=new JSONObject("{\"notes\":\"One\"}");assertTrue(WatchConflictReview.differences(record,new JSONObject(record.toString())).isEmpty());assertTrue(WatchConflictReview.differences(record,null).get(0).contains("Deleted on the other device"));}
}
