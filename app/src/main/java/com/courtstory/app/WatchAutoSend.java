package com.courtstory.app;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.concurrent.*;

/** Optional saved-change delivery. A durable marker survives process death; edits are coalesced. */
final class WatchAutoSend {
    private static final ScheduledExecutorService TIMER=Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> scheduled;
    static boolean enabled(Context c){return WatchTransport.enabled(c)&&c.getSharedPreferences("watch-connection",0).getBoolean("autoSend",false);}
    static void changed(Context context){Context c=context.getApplicationContext();if(!enabled(c))return;c.getSharedPreferences("watch-connection",0).edit().putString("pendingSave",java.util.UUID.randomUUID().toString()).apply();retry(c);}
    static synchronized void retry(Context context){Context c=context.getApplicationContext();if(!enabled(c)||c.getSharedPreferences("watch-connection",0).getString("pendingSave","").isEmpty())return;if(scheduled!=null)scheduled.cancel(false);scheduled=TIMER.schedule(()->{
        if(!enabled(c))return;SharedPreferences prefs=c.getSharedPreferences("watch-connection",0);String marker=prefs.getString("pendingSave","");
        WatchTransport.send(c,message->{if(!enabled(c))return;SharedPreferences.Editor editor=prefs.edit().putString("outgoingStatus",message);if(message.startsWith("Library sent")&&marker.equals(prefs.getString("pendingSave","")))editor.remove("pendingSave").putLong("lastSentAt",System.currentTimeMillis());editor.apply();});
    },3,TimeUnit.SECONDS);}
}
