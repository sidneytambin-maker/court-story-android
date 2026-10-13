package com.courtstory.app;
import android.content.*;
import android.app.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.json.JSONObject;
final class ConnectPush {
 static void configure(Context context,JSONObject config) {
  if(config.optString("firebase_app_id").isEmpty()) return;
  if(FirebaseApp.getApps(context).isEmpty()) FirebaseApp.initializeApp(context,new FirebaseOptions.Builder().setApplicationId(config.optString("firebase_app_id")).setApiKey(config.optString("firebase_api_key")).setProjectId(config.optString("firebase_project_id")).setGcmSenderId(config.optString("firebase_sender_id")).build());
 }
 static void enable(Context context) {
  if(FirebaseApp.getApps(context).isEmpty()) {ConnectClient.get(context).status="Push service configuration needs attention. Confirmations remain in Connect.";return;}
  FirebaseMessaging.getInstance().setAutoInitEnabled(true);
  FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token->ConnectClient.get(context).registerPush(token));
 }
 static void disable(Context context) {
  context.getSharedPreferences("connect",0).edit().remove("pendingNotifications").apply();
  if(!FirebaseApp.getApps(context).isEmpty()){FirebaseMessaging.getInstance().setAutoInitEnabled(false);FirebaseMessaging.getInstance().deleteToken();}
  android.app.NotificationManager manager=(android.app.NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
  for(android.service.notification.StatusBarNotification n:manager.getActiveNotifications())if("connect".equals(n.getTag()))manager.cancel("connect",n.getId());
 }
 static void deliverPending(Context context,ConnectClient c){
  android.content.SharedPreferences prefs=context.getSharedPreferences("connect",0);java.util.Set<String> pending;
  synchronized(ConnectPush.class){pending=new java.util.HashSet<>(prefs.getStringSet("pendingNotifications",java.util.Collections.emptySet()));prefs.edit().remove("pendingNotifications").commit();}
  for(String id:pending)deliver(context,c,id);
 }
 static void deliver(Context context,ConnectClient c,String id){
  try{
   if(!c.signedIn())return;if(!c.notificationSettings.optBoolean("push_enabled"))return;
   JSONObject found=null;for(JSONObject n:Domain.rows(c.notifications))if(id.equals(n.optString("id")))found=n;
   if(found==null)return;
   String kind=found.optString("kind");String preference=kind.equals("coach_feedback")?"coach_feedback":kind.equals("connection_request")?"connection_requests":"sharing_confirmations";
   if(!c.notificationSettings.optBoolean(preference,true))return;
   if(kind.equals("sharing_started")){boolean active=false;for(JSONObject connection:Domain.rows(c.connections))if(connection.optString("id").equals(found.optString("connection_id"))&&connection.optString("state").equals("active")&&Domain.array(connection,"categories").length()>0)active=true;if(!active)return;}
   NotificationManager manager=context.getSystemService(NotificationManager.class);if(!manager.areNotificationsEnabled())return;
   manager.createNotificationChannel(new NotificationChannel("court_story_connect","Court Story sharing",NotificationManager.IMPORTANCE_DEFAULT));
   Intent intent=new Intent(context,MainActivity.class).putExtra("connect_notification",id).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
   PendingIntent open=PendingIntent.getActivity(context,id.hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   manager.notify("connect",id.hashCode(),new Notification.Builder(context,"court_story_connect").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Court Story Connect").setContentText(found.optString("message")).setStyle(new Notification.BigTextStyle().bigText(found.optString("message"))).setVisibility(Notification.VISIBILITY_PRIVATE).setContentIntent(open).setAutoCancel(true).build());
  }catch(Exception ignored){/* In-app confirmation persists; do not show unverified content. */}
 }
}
