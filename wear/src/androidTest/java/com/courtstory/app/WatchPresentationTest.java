package com.courtstory.app;

import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class WatchPresentationTest extends ActivityInstrumentationTestCase2<WatchActivity> {
    WatchActivity a;
    public WatchPresentationTest(){super(WatchActivity.class);}
    @Override protected void setUp() throws Exception {
        super.setUp();a=getActivity();
        for(int i=0;i<100&&a.store==null;i++){Thread.sleep(100);getInstrumentation().waitForIdleSync();}
        assertNotNull("Watch library must load",a.store);
    }
    String text(View v){String result=v instanceof TextView?((TextView)v).getText().toString()+"\n":"";if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)result+=text(((ViewGroup)v).getChildAt(i));return result;}
    void checkButtons(View v){if(v instanceof Button){Button b=(Button)v;assertTrue("Touch target: "+b.getText(),b.getHeight()>=a.dp(48));assertTrue("Button label: "+b.getText(),b.getText().length()>0);assertTrue(b.isImportantForAccessibility());}if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)checkButtons(((ViewGroup)v).getChildAt(i));}
    public void testWelcomeHasClearSetupAndConnection() throws Throwable {
        runTestOnUiThread(()->a.welcome());getInstrumentation().waitForIdleSync();
        runTestOnUiThread(()->{String s=text(a.root);assertTrue(s.contains("Court Story"));assertTrue(s.contains("Set up this watch"));assertTrue(s.contains("Phone connection"));assertTrue(s.contains("Privacy"));checkButtons(a.root);assertNotNull(a.root.getAccessibilityPaneTitle());});
    }
    public void testWatchMenuCanReturnHomeWithoutProfile() throws Throwable {
        runTestOnUiThread(()->a.watchMenu());getInstrumentation().waitForIdleSync();
        runTestOnUiThread(()->{assertTrue(text(a.root).contains("Today"));checkButtons(a.root);a.home();assertNotNull(a.root.getAccessibilityPaneTitle());});
    }
    public void testConnectionStartsWithExplicitControls() throws Throwable {
        runTestOnUiThread(()->WatchConnection.show(a));getInstrumentation().waitForIdleSync();
        runTestOnUiThread(()->{assertTrue(text(a.root).toLowerCase(java.util.Locale.ROOT).contains("connection"));checkButtons(a.root);});
    }
    org.json.JSONObject waitPhase(String phase) throws Exception {
        org.json.JSONObject state=null;
        for(int i=0;i<300;i++){
            state=WatchTransport.read(new java.io.File(a.getFilesDir(),"watch-workout.json"));
            if(state!=null&&phase.equals(state.optString("phase")))return state;
            if(state!=null&&state.optBoolean("error"))fail("Workout error: "+state);
            Thread.sleep(100);
        }
        fail("Expected workout phase "+phase+", received "+state);return null;
    }
    public void testSyntheticWorkoutStartPauseResumeFinish() throws Throwable {
        assertTrue("Sensor test requires the QA package",a.getPackageName().endsWith(".qa"));
        final org.json.JSONObject original=Domain.copy(a.store.data);
        try {
            runTestOnUiThread(()->{
                a.store.data=Domain.newLibrary();org.json.JSONObject player=Domain.obj();
                Domain.put(player,"id",Domain.id());Domain.put(player,"name","Synthetic watch tester");Domain.put(player,"sport","Tennis");
                Domain.table(a.store.data,"players").put(player);Domain.put(a.store.data,"selectedPlayerID",player.optString("id"));
                org.json.JSONObject record=a.newRecord("trainingSessions");Domain.put(record,"id","watch-sensor-test");Domain.put(record,"androidScheduled",true);
                assertTrue(a.saveRecord("trainingSessions",record));a.home();
                WatchTraining.command(a,"start","watch-sensor-test");
            });
            waitPhase("Active");Thread.sleep(5000);
            runTestOnUiThread(()->WatchTraining.command(a,"pause","watch-sensor-test"));waitPhase("Paused");
            runTestOnUiThread(()->WatchTraining.command(a,"resume","watch-sensor-test"));waitPhase("Active");Thread.sleep(5000);
            runTestOnUiThread(()->WatchTraining.command(a,"finish","watch-sensor-test"));waitPhase("Ended");
            for(int i=0;i<100;i++){org.json.JSONObject s=WatchTransport.read(new java.io.File(a.getFilesDir(),"watch-workout.json"));if(s.optBoolean("committed"))break;Thread.sleep(100);}
            Store saved=new Store(a);org.json.JSONObject result=Domain.find(saved.table("trainingSessions"),"watch-sensor-test");
            assertNotNull(result);assertFalse(result.optString("actualFinish").isEmpty());
            assertEquals("Wear OS Health Services",Domain.object(result,"workout").optString("source"));
            assertTrue("Recorded active duration",Domain.object(result,"workout").optLong("durationSeconds")>=5);
        } finally {
            a.stopService(new android.content.Intent(a,WatchWorkoutService.class));
            Store restore=new Store(a);restore.data=original;restore.save();
            runTestOnUiThread(()->{try{a.store=new Store(a);}catch(Exception e){throw new AssertionError(e);}a.home();});
        }
    }
}
