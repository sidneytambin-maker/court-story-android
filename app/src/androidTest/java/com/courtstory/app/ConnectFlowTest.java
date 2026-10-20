package com.courtstory.app;
import android.view.*;
import java.util.*;
import com.google.android.apps.common.testing.accessibility.framework.*;
import com.google.android.apps.common.testing.accessibility.framework.uielement.AccessibilityHierarchyAndroid;
import android.widget.*;
import org.json.*;
import static com.courtstory.app.Domain.*;
/** Isolated QA navigation tests: synthetic records, no backend credentials or network mutations. */
public final class ConnectFlowTest extends DeviceFlowTest {
 public void testBrowserIntentPreservesFullInvitation() throws Exception {
  String code="a".repeat(64);
  String value="intent://court-story-connect.web.app/connect/invite#"+code+"#Intent;scheme=https;package=com.courtstory.app;S.browser_fallback_url=https%3A%2F%2Fcourt-story-connect.firebaseapp.com%2Fconnect%2Finvite%23"+code+";end";
  android.content.Intent intent=android.content.Intent.parseUri(value,android.content.Intent.URI_INTENT_SCHEME);
  assertEquals("com.courtstory.app",intent.getPackage());
  assertEquals(code,ConnectProtocol.code(intent.getDataString(),"court-story-connect.web.app"));
 }
 public void testBrowserButtonsPreserveResponseInAndroidIntent() throws Exception {
  String code="b".repeat(64);
  for(String action:new String[]{"accept","decline"}){
   String value="intent://court-story-connect.web.app/connect/invite#"+code+"/"+action+"#Intent;scheme=https;package=com.courtstory.app;end";
   android.content.Intent intent=android.content.Intent.parseUri(value,android.content.Intent.URI_INTENT_SCHEME);
   assertEquals("com.courtstory.app",intent.getPackage());
   assertEquals(code,ConnectProtocol.code(intent.getDataString(),"court-story-connect.web.app"));
   assertEquals(action,ConnectProtocol.response(intent.getDataString(),"court-story-connect.web.app"));
  }
 }
 public void testIncomingInvitationGoesStraightToReviewWithoutUpload(){
  seed();String before=a.store.data.toString();String code="b".repeat(64);
  ui(()->{a.handleIntent(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("https://court-story-connect.web.app/connect/invite#"+code)));assertNotNull(findButton(a.root,"Accept invitation"));assertNotNull(findButton(a.root,"Decline invitation"));assertNull(findButton(a.root,"Continue to invitation"));assertFalse(findText(a.root,"Upload consent on this device"));audit(a.root);assertEquals(code,a.getSharedPreferences("connect",0).getString("pending",""));findButton(a.root,"Not now").performClick();});assertEquals(before,a.store.data.toString());
 }
 public void testBothInvitationRolesHaveClearDecisions(){seed();for(String role:new String[]{"player","coach"}){JSONObject preview=obj();put(preview,"name","Synthetic sender");put(preview,"role",role);put(preview,"sport","Tennis");ui(()->{ConnectUI.invitationDecision(a,"c".repeat(64),preview);assertNotNull(findButton(a.root,"Accept invitation"));assertNotNull(findButton(a.root,"Decline invitation"));assertNotNull(findButton(a.root,"Not now"));audit(a.root);});}}

 public void testDeviceConnectionKeepsTrackingLocal(){seed();String before=a.store.data.toString();ui(()->{ConnectUI.show(a);audit(a.root);assertNotNull(findButton(a.root,"Enable Connect"));assertNotNull(findButton(a.root,"Back to local tracking"));});assertEquals(before,a.store.data.toString());}
 public void testShareAllSelectsHistoryAndFuture(){seed();JSONObject connection=obj();put(connection,"id",id());put(connection,"player_account","");put(connection,"sport","Tennis");put(connection,"history","future");ui(()->{ConnectUI.permission(a,connection);Button all=findButton(a.root,"Share all sporting data, including history");assertNotNull(all);all.performClick();assertTrue(findText(a.root,"History: all history and future"));assertEquals(7,countChecked(a.root));assertNotNull(findButton(a.root,"Approve and start sharing"));audit(a.root);});}
 public void testCoachDashboardSeparatesPlayers(){seed();ConnectClient c=ConnectClient.get(a);String first=id(),second=id();JSONObject one=record(first,"First player's historical match"),two=record(second,"Second player's private-to-this-view match");c.records=new JSONArray().put(one).put(two);ui(()->{ConnectUI.playerDashboard(a,first,"First player","All approved sports","All categories");assertNotNull(findButton(a.root,"First player's historical match - Tennis - 2020-01-01T00:00:00Z"));assertNull(findButton(a.root,"Second player's private-to-this-view match - Tennis - 2020-01-01T00:00:00Z"));audit(a.root);});}
 public void testNotificationControlsRemainIndependent(){seed();ConnectClient c=ConnectClient.get(a);c.notificationSettings=obj();put(c.notificationSettings,"push_enabled",false);put(c.notificationSettings,"connection_requests",true);put(c.notificationSettings,"sharing_confirmations",true);put(c.notificationSettings,"coach_feedback",false);ui(()->{ConnectUI.notifications(a);assertEquals(2,countChecked(a.root));assertNotNull(findButton(a.root,"Open phone notification settings"));audit(a.root);});}
 static JSONObject record(String player,String title){JSONObject r=obj(),b=obj();put(r,"id",id());put(r,"player_id",player);put(r,"sport","Tennis");put(r,"category","match");put(r,"occurred_at","2020-01-01T00:00:00Z");put(b,"title",title);put(r,"body",b);return r;}
 static int countChecked(View v){int result=v instanceof CheckBox&&((CheckBox)v).isChecked()?1:0;if(v instanceof ViewGroup)for(int n=0;n<((ViewGroup)v).getChildCount();n++)result+=countChecked(((ViewGroup)v).getChildAt(n));return result;}
 public void testConnectGoogleAccessibilityChecks(){seed();JSONObject connection=obj();put(connection,"id",id());put(connection,"player_account","");put(connection,"sport","Tennis");List<Runnable> screens=Arrays.asList(()->ConnectUI.show(a),()->ConnectUI.permission(a,connection),()->ConnectUI.notifications(a),()->ConnectUI.privacy(a),()->ConnectUI.playerDashboard(a,id(),"Synthetic player","All approved sports","All categories"));int index=0;for(Runnable screen:screens){ui(screen);AccessibilityHierarchyAndroid hierarchy=accessibilityHierarchy();List<String> errors=new ArrayList<>();for(AccessibilityHierarchyCheck check:AccessibilityCheckPreset.getAccessibilityHierarchyChecksForPreset(AccessibilityCheckPreset.LATEST))for(AccessibilityHierarchyCheckResult result:check.runCheckOnHierarchy(hierarchy))if(result.getType()==AccessibilityCheckResult.AccessibilityCheckResultType.ERROR)errors.add(check.getClass().getSimpleName()+": "+result.getMessage(Locale.UK));assertTrue("Connect accessibility screen "+index+++": "+errors,errors.isEmpty());}}
 public void testDurablePushJobCanBeScheduled(){seed();ui(()->ConnectSyncJob.schedule(a,true));}
 public void testFeedbackQueueAndConflictPrivacyPersist() throws Exception {
  seed();java.lang.reflect.Constructor<ConnectClient> constructor=ConnectClient.class.getDeclaredConstructor(android.content.Context.class);constructor.setAccessible(true);ConnectClient c=constructor.newInstance(a.getApplicationContext());
  java.lang.reflect.Field session=ConnectClient.class.getDeclaredField("session"),state=ConnectClient.class.getDeclaredField("state");session.setAccessible(true);state.setAccessible(true);
  String account=id(),rid=id();JSONObject login=obj(),user=obj(),local=obj();put(user,"id",account);put(login,"user",user);session.set(c,login);state.set(c,local);
  try{
   JSONObject message=obj();put(message,"p_id",id());put(message,"p_text","Synthetic offline feedback");c.queueFeedback(message);assertEquals(1,c.pendingFeedback().length());
   java.lang.reflect.Method load=ConnectClient.class.getDeclaredMethod("loadState");load.setAccessible(true);state.set(c,obj());load.invoke(c);assertEquals("Synthetic offline feedback",c.pendingFeedback().getJSONObject(0).optString("p_text"));c.discardFeedback(message.optString("p_id"));assertEquals(0,c.pendingFeedback().length());
   local=(JSONObject)state.get(c);JSONObject queued=obj(),remote=obj();put(queued,"p_id",rid);put(queued,"p_expected",1);put(queued,"p_private",false);put(queued,"p_body",obj());put(queued,"local_hash","previous");put(queued,"p_operation",id());put(local,"queue",new JSONArray().put(queued));put(remote,"id",rid);put(remote,"owner_id",account);put(remote,"revision",2);put(remote,"private",true);c.records=new JSONArray().put(remote);
   assertEquals(1,c.syncConflicts().size());c.resolveConflict(rid,true);assertTrue("Private setting retained",c.isPrivate(rid));assertTrue("Queued record stays private",array((JSONObject)state.get(c),"queue").getJSONObject(0).optBoolean("p_private"));assertEquals(2,array((JSONObject)state.get(c),"queue").getJSONObject(0).optInt("p_expected"));
   JSONObject boundState=(JSONObject)state.get(c);put(boundState,"uploadPlayers",new JSONArray().put(id()));put(boundState,"libraryID",id());JSONObject changedLibrary=obj();put(changedLibrary,"libraryID",id());String before=boundState.toString();try{c.sync(changedLibrary);fail("A different local library must require renewed consent");}catch(java.io.IOException expected){assertTrue(expected.getMessage().contains("local library changed"));}assertEquals(before,((JSONObject)state.get(c)).toString());
   put(remote,"revision",3);c.resolveConflict(rid,false);assertEquals(0,array((JSONObject)state.get(c),"queue").length());assertTrue("Private setting retained",c.isPrivate(rid));
   ui(()->{JSONObject saved=a.newRecord("matches");put(saved,"opponentName","Synthetic opponent");assertTrue(a.saveRecord("matches",saved));});
   JSONObject actual=new Store(a).data;JSONObject savedMatch=Domain.rows(Domain.table(actual,"matches")).get(0);String matchID=savedMatch.optString("id").toLowerCase(Locale.ROOT),playerID=savedMatch.optString("playerID").toLowerCase(Locale.ROOT);JSONObject metadata=obj(),uploaded=obj();put(metadata,"p_id",matchID);put(metadata,"p_player",playerID);put(uploaded,matchID,metadata);boundState=(JSONObject)state.get(c);put(boundState,"uploaded",uploaded);put(boundState,"uploadPlayers",new JSONArray().put(playerID));put(boundState,"libraryID",actual.optString("libraryID").toLowerCase(Locale.ROOT));JSONObject stale=obj();put(stale,"libraryID",actual.optString("libraryID"));java.lang.reflect.Field config=ConnectClient.class.getDeclaredField("config");config.setAccessible(true);config.set(c,obj());try{c.sync(stale);fail("Network configuration intentionally absent");}catch(java.io.IOException expected){assertTrue(expected.getMessage().contains("configuration is unavailable"));}assertEquals("An older screen cannot create deletion tombstones",0,array((JSONObject)state.get(c),"queue").length());
   c.signOut();assertFalse("Offline sign-out clears the local session",c.signedIn());
  } finally {c.worker.shutdownNow();new android.util.AtomicFile(new java.io.File(a.getNoBackupFilesDir(),"connect-"+account+".json")).delete();}
 }
}
