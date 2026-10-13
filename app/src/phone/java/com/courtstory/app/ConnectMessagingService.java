package com.courtstory.app;
import android.app.*;
import android.content.*;
import com.google.firebase.messaging.*;
import org.json.JSONObject;
/** Fetch the account-owned notification before displaying any provider wake-up. */
public final class ConnectMessagingService extends FirebaseMessagingService {
 @Override public void onNewToken(String token){ConnectClient.get(this).registerPush(token);}
 @Override public void onMessageReceived(RemoteMessage message){
  String id=message.getData().get("connect_notification");if(id==null)return;
  android.content.SharedPreferences prefs=getSharedPreferences("connect",0);
  synchronized(ConnectPush.class){java.util.Set<String> pending=new java.util.HashSet<>(prefs.getStringSet("pendingNotifications",java.util.Collections.emptySet()));pending.add(id);prefs.edit().putStringSet("pendingNotifications",pending).commit();}
  ConnectSyncJob.schedule(this,true);

 }
}
