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
    Button button(View v,String label){if(v instanceof Button&&((Button)v).getText().toString().equals(label))return (Button)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){Button result=button(((ViewGroup)v).getChildAt(i),label);if(result!=null)return result;}return null;}
    void profile(String role){a.store.data=Domain.newLibrary();org.json.JSONObject p=Domain.obj();Domain.put(p,"id",Domain.id());Domain.put(p,"name","Synthetic watch "+role);Domain.put(p,"role",role);Domain.put(p,"sport","Tennis");Domain.put(p,"trackingMode","Basic");Domain.table(a.store.data,"players").put(p);Domain.put(a.store.data,"selectedPlayerID",p.optString("id"));assertTrue(a.save());}
    public void testPreferencesOpenThroughNormalMenuAndSave() throws Throwable {
        org.json.JSONObject original=Domain.copy(a.store.data);
        try{runTestOnUiThread(()->{profile("Player");a.settings();assertTrue(text(a.root).contains("Watch preferences"));Button save=button(a.root,"Save preferences");assertNotNull(save);save.performClick();assertEquals("Today",a.screenName);assertFalse(a.editing);});}
        finally{Store restore=new Store(a);restore.data=original;restore.save();}
    }
    public void testCoachHasRosterAndOwnGameRoutes() throws Throwable {
        org.json.JSONObject original=Domain.copy(a.store.data);
        try{runTestOnUiThread(()->{profile("Coach");a.home();assertNotNull(button(a.root,"Your players"));assertTrue(text(a.root).contains("My own game"));assertNotNull(button(a.root,"Track"));a.watchMenu();assertNotNull(button(a.root,"Your players"));});}
        finally{Store restore=new Store(a);restore.data=original;restore.save();}
    }
    public void testGoogleAccessibilityChecksAcrossRolesAndTrackingModes() throws Throwable {
        org.json.JSONObject original=Domain.copy(a.store.data);
        try{
            for(String role:new String[]{"Player","Coach"})for(String tier:new String[]{"Basic","Standard","Power"}){
                runTestOnUiThread(()->{profile(role);Domain.put(a.store.player(),"trackingMode",tier);assertTrue(a.save());});
                for(Runnable screen:new Runnable[]{a::home,a::track,a::watchMenu,a::settings,a::reports,()->Achievements.show(a)}){
                    java.util.concurrent.CountDownLatch drawn=new java.util.concurrent.CountDownLatch(1);
                    runTestOnUiThread(()->{screen.run();View target=a.root;target.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){target.getViewTreeObserver().removeOnPreDrawListener(this);drawn.countDown();return true;}});target.invalidate();});
                    assertTrue("Screen must be laid out",drawn.await(10,java.util.concurrent.TimeUnit.SECONDS));
                    final java.util.concurrent.atomic.AtomicReference<com.google.android.apps.common.testing.accessibility.framework.uielement.AccessibilityHierarchyAndroid> hierarchy=new java.util.concurrent.atomic.AtomicReference<>();
                    if(android.os.Build.VERSION.SDK_INT>=34){android.view.accessibility.AccessibilityNodeInfo node=getInstrumentation().getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).getRootInActiveWindow();assertNotNull(node);assertEquals(a.getPackageName(),String.valueOf(node.getPackageName()));hierarchy.set(com.google.android.apps.common.testing.accessibility.framework.uielement.AccessibilityHierarchyAndroid.newBuilder(node,a).build());}
                    else runTestOnUiThread(()->hierarchy.set(com.google.android.apps.common.testing.accessibility.framework.uielement.AccessibilityHierarchyAndroid.newBuilder(a.root).build()));
                    java.util.List<String> errors=new java.util.ArrayList<>();
                    for(com.google.android.apps.common.testing.accessibility.framework.AccessibilityHierarchyCheck check:com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset.getAccessibilityHierarchyChecksForPreset(com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset.LATEST))
                        for(com.google.android.apps.common.testing.accessibility.framework.AccessibilityHierarchyCheckResult result:check.runCheckOnHierarchy(hierarchy.get()))
                            if(result.getType()==com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType.ERROR)errors.add(check.getClass().getSimpleName()+": "+result.getMessage(java.util.Locale.UK));
                    assertTrue(role+" / "+tier+" / "+a.screenName+": "+errors,errors.isEmpty());
                    android.graphics.Bitmap screenshot=getInstrumentation().getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).takeScreenshot();
                    assertNotNull("Capture the rendered watch screen",screenshot);
                    String filename="watch-"+role+"-"+tier+"-"+a.screenName.replaceAll("[^A-Za-z0-9]+","-")+".png";
                    try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(a.getExternalFilesDir(null),filename))){screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{screenshot.recycle();}
                }
            }
        }finally{Store restore=new Store(a);restore.data=original;restore.save();}
    }
    public void testSyntheticWorkoutStartPauseResumeFinish() throws Throwable {
        assertTrue("Sensor test requires the QA package",a.getPackageName().endsWith(".qa"));
        final org.json.JSONObject original=Domain.copy(a.store.data);
        try {
            runTestOnUiThread(()->{
                a.store.data=Domain.newLibrary();org.json.JSONObject player=Domain.obj();
                Domain.put(player,"id",Domain.id());Domain.put(player,"name","Synthetic watch tester");Domain.put(player,"sport","Tennis");
                Domain.table(a.store.data,"players").put(player);Domain.put(a.store.data,"selectedPlayerID",player.optString("id"));
                org.json.JSONObject record=a.newRecord("trainingSessions");Domain.put(record,"id","00000000-0000-4000-8000-000000000032");Domain.put(record,"androidScheduled",true);
                assertTrue(a.saveRecord("trainingSessions",record));a.home();
                WatchTraining.command(a,"start","00000000-0000-4000-8000-000000000032");
            });
            waitPhase("Active");Thread.sleep(5000);
            runTestOnUiThread(()->WatchTraining.command(a,"pause","00000000-0000-4000-8000-000000000032"));waitPhase("Paused");long paused=WatchTraining.state(a).optLong("durationSeconds");Thread.sleep(2000);assertEquals("Paused duration must not advance",paused,WatchTraining.state(a).optLong("durationSeconds"));
            runTestOnUiThread(()->WatchTraining.command(a,"resume","00000000-0000-4000-8000-000000000032"));waitPhase("Active");Thread.sleep(5000);
            runTestOnUiThread(()->WatchTraining.command(a,"finish","00000000-0000-4000-8000-000000000032"));waitPhase("Ended");
            for(int i=0;i<100;i++){org.json.JSONObject s=WatchTransport.read(new java.io.File(a.getFilesDir(),"watch-workout.json"));if(s.optBoolean("committed"))break;Thread.sleep(100);}
            Store saved=new Store(a);org.json.JSONObject result=Domain.find(saved.table("trainingSessions"),"00000000-0000-4000-8000-000000000032");
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
