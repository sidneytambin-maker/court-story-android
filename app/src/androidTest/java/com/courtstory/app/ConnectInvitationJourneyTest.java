package com.courtstory.app;
import java.net.*;
import java.io.*;
import java.security.cert.Certificate;
import javax.net.ssl.HttpsURLConnection;
import org.json.*;
import static com.courtstory.app.Domain.*;

/** Exercises the real client and native screens against an isolated, strict HTTP fixture. */
public class ConnectInvitationJourneyTest extends DeviceFlowTest {
 static volatile Fixture fixture;
 static boolean installed;
 static final String CODE="a".repeat(64);
 static final String ACTOR="00000000-0000-4000-8000-000000000011";
 static final String CONNECTION="00000000-0000-4000-8000-000000000012";
 static final class Fixture {
  volatile int responses,uploads;volatile String response="";boolean expired;
  synchronized String result(String path,String payload) throws Exception {
   if(path.equals("/auth/v1/signup"))return "{\"access_token\":\"synthetic\",\"refresh_token\":\"synthetic\",\"user\":{\"id\":\""+ACTOR+"\"}}";
   if(path.equals("/rest/v1/rpc/cs_review_invitation")){if(expired)throw new IOException("Expired invitation fixture");return "{\"name\":\"Synthetic sender\",\"role\":\"player\",\"sport\":\"Tennis\"}";}
   if(path.equals("/rest/v1/rpc/cs_respond_invitation")){
    JSONObject b=new JSONObject(payload);if(!CODE.equals(b.optString("p_code")))throw new IOException("Wrong invitation");
    response=b.optString("p_response");if(!response.equals("accept")&&!response.equals("decline"))throw new IOException("Wrong response");responses++;
    return "{\"response\":\""+response+"\",\"connection_id\":\""+CONNECTION+"\"}";
   }
   if(path.equals("/rest/v1/rpc/cs_register_profile"))return "null";
   if(path.equals("/rest/v1/rpc/cs_put_record")||path.equals("/rest/v1/rpc/cs_set_permission")){uploads++;throw new IOException("Invitation must not grant sharing or upload records");}
   if(path.equals("/rest/v1/rpc/cs_visible_manifest")||path.startsWith("/rest/v1/cs_"))return "[]";
   throw new IOException("Unexpected fixture request: "+path);
  }
 }
 static final class Response extends HttpsURLConnection {
  final ByteArrayOutputStream body=new ByteArrayOutputStream();byte[] reply;
  Response(URL url){super(url);}
  public OutputStream getOutputStream(){return body;}
  public int getResponseCode()throws IOException {try{if(fixture==null)throw new IOException("Fixture not active");reply=fixture.result(url.getPath(),body.toString("UTF-8")).getBytes(java.nio.charset.StandardCharsets.UTF_8);return 200;}catch(Exception e){throw new IOException(e);}}
  public InputStream getInputStream(){return new ByteArrayInputStream(reply);}
  public void connect(){} public void disconnect(){} public boolean usingProxy(){return false;}
  public String getCipherSuite(){return "fixture";} public Certificate[] getLocalCertificates(){return null;} public Certificate[] getServerCertificates(){return null;}
 }
 @Override protected void setUp()throws Exception {
  if(!installed){URL.setURLStreamHandlerFactory(protocol->protocol.equals("https")?new URLStreamHandler(){protected URLConnection openConnection(URL url){return new Response(url);}}:null);installed=true;}
  fixture=new Fixture();super.setUp();
  ConnectClient c=ConnectClient.get(a);
  c.worker.submit(()->{}).get(10,java.util.concurrent.TimeUnit.SECONDS);
  java.lang.reflect.Field session=ConnectClient.class.getDeclaredField("session");session.setAccessible(true);session.set(c,null);c.status="Ready for synthetic invitation test.";
  a.getSharedPreferences("connect",0).edit().clear().commit();
 }
 @Override protected void tearDown()throws Exception {
  ConnectClient c=ConnectClient.get(a);c.worker.submit(()->{}).get(10,java.util.concurrent.TimeUnit.SECONDS);
  java.lang.reflect.Field session=ConnectClient.class.getDeclaredField("session");session.setAccessible(true);session.set(c,null);
  new android.util.AtomicFile(new File(a.getNoBackupFilesDir(),"connect-session.enc")).delete();
  new android.util.AtomicFile(new File(a.getNoBackupFilesDir(),"connect-"+ACTOR+".json")).delete();
  a.getSharedPreferences("connect",0).edit().clear().commit();fixture=null;super.tearDown();
 }
 void open(String action){ui(()->a.handleIntent(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("https://court-story-connect.firebaseapp.com/connect/invite#"+CODE+"/"+action))));}
 void awaitResult(String text)throws Exception {long until=android.os.SystemClock.uptimeMillis()+15000;while(android.os.SystemClock.uptimeMillis()<until){getInstrumentation().waitForIdleSync();if(ConnectClient.get(a).status.startsWith(text))return;Thread.sleep(30);}fail("Expected "+text+" but got "+ConnectClient.get(a).status);}
 public void testConfirmationViewOpensOnlyTheNamedPlayer()throws Exception {
  seed();String player=id(),other=id();JSONObject connection=obj(),preview=obj();
  put(connection,"id",CONNECTION);put(connection,"player_id",player);put(connection,"player_name","Morgan Lee");put(connection,"coach_name","Alex");put(connection,"sport","Badminton");put(connection,"state","active");put(connection,"categories",new JSONArray().put("match").put("training"));put(connection,"history","all");put(connection,"future_sharing",true);put(connection,"capability","feedback");
  put(preview,"role","player");put(preview,"name","Morgan Lee");put(preview,"sport","Badminton");
  ConnectClient c=ConnectClient.get(a);c.connections=new JSONArray().put(connection);
  JSONObject first=ConnectFlowTest.record(player,"Morgan match"),second=ConnectFlowTest.record(other,"Other player match");put(first,"sport","Badminton");put(second,"sport","Badminton");c.records=new JSONArray().put(first).put(second);
  ui(()->{ConnectUI.confirmation(a,preview,"accept",CONNECTION);assertTrue(findText(a.root,"You agreed to receive Morgan Lee’s Badminton data."));assertNotNull(findButton(a.root,"View player’s data"));audit(a.root);});
  ui(()->{try{android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(a.root.getWidth(),a.root.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);a.root.draw(new android.graphics.Canvas(bitmap));try(FileOutputStream out=new FileOutputStream(new File(a.getExternalFilesDir(null),"invitation-confirmation.png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}catch(Exception e){throw new AssertionError(e);}});
  ui(()->{findButton(a.root,"View player’s data").performClick();assertNotNull(findButton(a.root,"Morgan match - Badminton - 2020-01-01T00:00:00Z"));assertNull(findButton(a.root,"Other player match - Badminton - 2020-01-01T00:00:00Z"));});
 }
 public void testFirstUseAcceptUsesOneResponseTap()throws Exception {
  seed();ui(()->{a.handleIntent(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("https://court-story-connect.web.app/connect/invite#"+CODE)));assertNotNull(findButton(a.root,"Accept invitation"));assertNotNull(findButton(a.root,"Decline invitation"));assertNull(findButton(a.root,"Continue to invitation"));findButton(a.root,"Accept invitation").performClick();});
  awaitResult("Invitation accepted.");assertEquals("accept",fixture.response);assertEquals(1,fixture.responses);assertEquals(0,fixture.uploads);
 }
 public void testWebAcceptNeedsNoAdditionalTapAndNeverUploads()throws Exception {
  seed();String before=a.store.data.toString();open("accept");awaitResult("Invitation accepted.");
  assertEquals("accept",fixture.response);assertEquals(1,fixture.responses);assertEquals(0,fixture.uploads);assertEquals(before,a.store.data.toString());
  ui(()->{assertNull(findButton(a.root,"Accept invitation"));assertNull(findButton(a.root,"Enable Connect"));assertTrue(findText(a.root,"Invitation accepted"));assertTrue(findText(a.root,"You agreed to receive Synthetic sender’s Tennis data."));assertNull(findButton(a.root,"View player’s data"));});
  assertEquals("",a.getSharedPreferences("connect",0).getString("pending",""));
 }
 public void testWebDeclineNeedsNoAdditionalTapAndNeverUploads()throws Exception {
  seed();String before=a.store.data.toString();open("decline");awaitResult("Invitation declined.");
  assertEquals("decline",fixture.response);assertEquals(1,fixture.responses);assertEquals(0,fixture.uploads);assertEquals(before,a.store.data.toString());
  assertEquals("",a.getSharedPreferences("connect",0).getString("pending",""));
 }
 public void testInvalidInvitationCannotRespond()throws Exception {
  seed();fixture.expired=true;open("accept");awaitResult("java.io.IOException: Expired invitation fixture");
  assertEquals(0,fixture.responses);assertEquals(0,fixture.uploads);assertEquals(CODE+"/accept",a.getSharedPreferences("connect",0).getString("pending",""));
 }
 public void testDuplicateTapsOnSameScreenSubmitOnce()throws Exception {
  java.util.concurrent.CountDownLatch entered=new java.util.concurrent.CountDownLatch(1),release=new java.util.concurrent.CountDownLatch(1);
  java.util.concurrent.atomic.AtomicInteger calls=new java.util.concurrent.atomic.AtomicInteger();
  ui(()->{ConnectUI.run(a,()->{calls.incrementAndGet();entered.countDown();release.await();},()->{});ConnectUI.run(a,calls::incrementAndGet,()->{});});
  assertTrue(entered.await(5,java.util.concurrent.TimeUnit.SECONDS));release.countDown();ConnectClient.get(a).worker.submit(()->{}).get(5,java.util.concurrent.TimeUnit.SECONDS);assertEquals(1,calls.get());
 }
}
