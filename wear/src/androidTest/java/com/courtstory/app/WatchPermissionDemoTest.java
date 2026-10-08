package com.courtstory.app;

/** Reviewer evidence only: real app controls with fictional emulator records. */
public class WatchPermissionDemoTest extends WatchPresentationTest {
    void pressVisible(String label) throws Throwable {
        runTestOnUiThread(()->{
            android.widget.Button target=button(a.root,label);
            assertNotNull("Visible control: "+label,target);
            android.graphics.Rect rect=new android.graphics.Rect();
            target.getDrawingRect(rect);a.body.offsetDescendantRectToMyCoords(target,rect);
            a.scroll.scrollTo(0,Math.max(0,rect.top-a.dp(48)));
        });
        Thread.sleep(2000);
        runTestOnUiThread(()->button(a.root,label).performClick());
        getInstrumentation().waitForIdleSync();Thread.sleep(2000);
    }
    public void testForegroundWorkoutReviewerDemo() throws Throwable {
        assertTrue(a.getPackageName().endsWith(".qa"));
        org.json.JSONObject original=Domain.copy(a.store.data);
        final String id="00000000-0000-4000-8000-000000000036";
        try {
            runTestOnUiThread(()->{
                a.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                profile("Player");org.json.JSONObject r=a.newRecord("trainingSessions");
                Domain.put(r,"id",id);Domain.put(r,"focus","Serve practice");
                Domain.put(r,"date",java.time.Instant.now().plusSeconds(600).toString());
                Domain.put(r,"androidScheduled",true);Domain.put(r,"durationMinutes",30);
                assertTrue(a.saveRecord("trainingSessions",r));a.home();
            });
            Thread.sleep(3000);
            String title=Domain.title("trainingSessions",Domain.find(a.store.table("trainingSessions"),id));
            pressVisible("Open "+title);
            pressVisible("Start with watch measurements");waitPhase("Active");
            Thread.sleep(3000);
            // Show the ongoing workout continuing after the app leaves the foreground.
            getInstrumentation().getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME);
            Thread.sleep(4000);
            assertEquals("Active",WatchTraining.state(a).optString("phase"));
            getInstrumentation().getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
            Thread.sleep(4000);
            runTestOnUiThread(()->a.startActivity(new android.content.Intent(a,WatchActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT).putExtra("table","trainingSessions").putExtra("id",id)));
            Thread.sleep(2000);
            runTestOnUiThread(()->{try{a.store=new Store(a);}catch(Exception e){throw new AssertionError(e);}WatchTraining.show(a,Domain.find(a.store.table("trainingSessions"),id));});
            pressVisible("Pause workout");waitPhase("Paused");
            pressVisible("Resume workout");waitPhase("Active");
            pressVisible("Finish workout");
            android.view.accessibility.AccessibilityNodeInfo root=getInstrumentation().getUiAutomation().getRootInActiveWindow();
            java.util.List<android.view.accessibility.AccessibilityNodeInfo> confirm=root.findAccessibilityNodeInfosByText("Confirm");
            assertFalse("Finish confirmation must be visible",confirm.isEmpty());
            assertTrue(confirm.get(0).performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK));
            waitPhase("Ended");Thread.sleep(4000);
            assertTrue("Workout must be saved",WatchTraining.state(a).optBoolean("committed"));
            assertFalse(WatchTraining.state(a).optBoolean("error"));
        } finally {
            a.stopService(new android.content.Intent(a,WatchWorkoutService.class));
            Store restored=new Store(a);restored.data=original;restored.save();
        }
    }
}
