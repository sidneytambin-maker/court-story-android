package com.courtstory.app;
import android.webkit.*;
import android.view.*;
import android.content.Intent;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.json.*;
/** Renders the exact hosted HTML/JS locally in Android's browser engine, then taps its real controls.
 * This covers rendering and native handoff data, not OS association verification or messaging-app policies. */
public final class ConnectInvitationBrowserTest extends ConnectInvitationJourneyTest {
 void browserJourney(String action,int textZoom)throws Exception {
  seed();CountDownLatch loaded=new CountDownLatch(1);AtomicReference<String> target=new AtomicReference<>();AtomicReference<WebView> view=new AtomicReference<>();
  ui(()->{WebView web=new WebView(a);view.set(web);web.getSettings().setJavaScriptEnabled(true);web.getSettings().setTextZoom(textZoom);
   web.setWebViewClient(new WebViewClient(){
    @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest request){
     String path=request.getUrl().getPath();String file=path.equals("/connect/invite")?"index.html":path.equals("/invite.js")?"invite.js":path.equals("/invite.css")?"invite.css":null;
     try{return new WebResourceResponse(file==null?"text/plain":file.endsWith(".js")?"application/javascript":file.endsWith(".css")?"text/css":"text/html","UTF-8",file==null?new ByteArrayInputStream(new byte[0]):getInstrumentation().getContext().getAssets().open("invitation/"+file));}catch(Exception e){throw new AssertionError(e);}
    }
    @Override public void onPageFinished(WebView v,String url){loaded.countDown();}
    @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest request){
     if(!request.getUrl().getScheme().equals("intent"))return false;
     target.set(request.getUrl().toString());try{a.handleIntent(Intent.parseUri(target.get(),Intent.URI_INTENT_SCHEME));}catch(Exception e){throw new AssertionError(e);}return true;
    }
   });a.setContentView(web);web.loadUrl("https://court-story-connect.web.app/connect/invite#"+CODE);
  });
  assertTrue("Invitation HTML loads",loaded.await(15,TimeUnit.SECONDS));
  CountDownLatch measured=new CountDownLatch(1);AtomicReference<String> geometry=new AtomicReference<>();
  ui(()->view.get().evaluateJavascript("(()=>{const b=document.getElementById('"+action+"');b.scrollIntoView({block:'center'});const r=b.getBoundingClientRect();return JSON.stringify({x:r.left+r.width/2,y:r.top+r.height/2,scale:devicePixelRatio,width:innerWidth,content:document.documentElement.scrollWidth,label:b.textContent,href:b.href});})()",value->{geometry.set(value);measured.countDown();}));
  assertTrue(measured.await(5,TimeUnit.SECONDS));JSONObject rect=new JSONObject((String)new JSONTokener(geometry.get()).nextValue());
  assertEquals(action.equals("accept")?"Accept invitation":"Decline invitation",rect.getString("label"));assertTrue("Page must not require horizontal scrolling",rect.getInt("content")<=rect.getInt("width"));assertTrue(rect.getString("href").contains(CODE+"/"+action));
  int[] origin=new int[2];ui(()->view.get().getLocationOnScreen(origin));float x=origin[0]+(float)(rect.getDouble("x")*rect.getDouble("scale")),y=origin[1]+(float)(rect.getDouble("y")*rect.getDouble("scale"));
  CountDownLatch painted=new CountDownLatch(1);
  ui(()->view.get().postVisualStateCallback(1,new WebView.VisualStateCallback(){@Override public void onComplete(long id){view.get().postOnAnimation(()->view.get().postOnAnimation(painted::countDown));}}));
  assertTrue("Browser commits the rendered invitation",painted.await(10,TimeUnit.SECONDS));
  getInstrumentation().waitForIdleSync();
  android.graphics.Bitmap bitmap=getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull("Composited browser screenshot",bitmap);
  try(FileOutputStream out=new FileOutputStream(new File(a.getExternalFilesDir(null),"invitation-web-"+action+".png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{bitmap.recycle();}

  long now=android.os.SystemClock.uptimeMillis();getInstrumentation().sendPointerSync(MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,x,y,0));getInstrumentation().sendPointerSync(MotionEvent.obtain(now,now+100,MotionEvent.ACTION_UP,x,y,0));
  awaitResult(action.equals("accept")?"Invitation accepted.":"Invitation declined.");assertNotNull("The web control must invoke a package intent",target.get());assertEquals(action,fixture.response);assertEquals(1,fixture.responses);assertEquals(0,fixture.uploads);
  ui(()->view.get().destroy());
 }
 public void testRenderedAcceptButtonOpensNativeConfirmation()throws Exception{browserJourney("accept",100);}
 public void testRenderedDeclineAtLargeTextOpensNativeConfirmation()throws Exception{browserJourney("decline",200);}
}
