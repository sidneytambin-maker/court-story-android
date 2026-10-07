package com.courtstory.app;
import android.app.*;
import android.content.*;
import android.os.Build;
import org.json.*;
import java.util.*;
import static com.courtstory.app.Domain.*;

public class ReminderReceiver extends BroadcastReceiver {
    static final String CHANNEL="court_story_reminders";
    private static final java.util.concurrent.ExecutorService WORK=java.util.concurrent.Executors.newSingleThreadExecutor();
    private static final java.util.concurrent.atomic.AtomicLong REQUESTS=new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicBoolean QUEUED=new java.util.concurrent.atomic.AtomicBoolean();
    public static void schedule(Context c,Store ignored){
        Context app=c.getApplicationContext();REQUESTS.incrementAndGet();enqueue(app);
    }
    private static void enqueue(Context app){
        if(!QUEUED.compareAndSet(false,true))return;
        WORK.execute(()->{long done=0;try{do{done=REQUESTS.get();try{scheduleNow(app,new Store(app));app.getSharedPreferences("alarms",0).edit().remove("lastError").apply();}catch(Exception e){app.getSharedPreferences("alarms",0).edit().putString("lastError","Reminders could not be updated. Check Android notification settings.").apply();}}while(done!=REQUESTS.get());}finally{QUEUED.set(false);if(done!=REQUESTS.get())enqueue(app);}});
    }
    static void awaitIdle() throws Exception {WORK.submit(()->{}).get(30,java.util.concurrent.TimeUnit.SECONDS);}
    static void afterBoot(Context c,PendingResult result){Context app=c.getApplicationContext();WORK.execute(()->{try{scheduleNow(app,new Store(app));}catch(Exception e){android.util.Log.w("CourtStory","Reminders could not be restored after restart");}finally{result.finish();}});}
    static void scheduleNow(Context c,Store s){
        createChannel(c,s);AlarmManager alarm=c.getSystemService(AlarmManager.class);
        Set<String> previous=new HashSet<>(c.getSharedPreferences("alarms",0).getStringSet("ids",new HashSet<>()));
        for(String key:previous)alarm.cancel(pending(c,key,"","","",0));
        Set<String> next=new HashSet<>();long now=System.currentTimeMillis();
        for(ReminderPlan.Item item:ReminderPlan.upcoming(s.data,now)){alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,item.when,pending(c,item.key,item.text,item.table,item.id,item.when));next.add(item.key);}
        if(s.settings().optBoolean("weeklySummaryEnabled")){long when=ReminderPlan.nextWeek(java.time.ZonedDateTime.now());alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pending(c,"weekly","Your court week","reports","",when));next.add("weekly");}
        c.getSharedPreferences("alarms",0).edit().putStringSet("ids",next).apply();
    }
        static String channelId(Store s){JSONObject sounds=object(s.settings(),"sounds");return CHANNEL+"_"+sounds.optString("reminders","System sound").hashCode()+"_"+sounds.optString("selected","bounce");}
    static void createChannel(Context c,Store s){JSONObject sounds=object(s.settings(),"sounds");String mode=sounds.optString("reminders","System sound");NotificationChannel ch=new NotificationChannel(channelId(s),"Court Story reminders",NotificationManager.IMPORTANCE_DEFAULT);if(mode.equals("Silent"))ch.setSound(null,null);else if(mode.equals("Selected tennis sound")){int sound=switch(sounds.optString("selected","bounce")){case "racketStrike"->R.raw.tennis_racket_strike;case "racketSwing"->R.raw.tennis_racket_swoosh;case "ballCan"->R.raw.tennis_ball_can;case "applause"->R.raw.tennis_applause;default->R.raw.tennis_bounce;};ch.setSound(android.net.Uri.parse("android.resource://"+c.getPackageName()+"/"+sound),new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION).build());}c.getSystemService(NotificationManager.class).createNotificationChannel(ch);}
    static PendingIntent pending(Context c,String key,String text,String table,String id,long when){Intent i=new Intent(c,ReminderReceiver.class).setAction(key).putExtra("scheduledFor",when);return PendingIntent.getBroadcast(c,key.hashCode(),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    static boolean deliver(Context c,Intent intent) throws Exception {
        Store store=new Store(c);ReminderPlan.Item item=ReminderPlan.eligible(store.data,intent.getAction(),intent.getLongExtra("scheduledFor",0),System.currentTimeMillis());
        if(item==null)return false;
        NotificationManager manager=c.getSystemService(NotificationManager.class);createChannel(c,store);
        if(!manager.areNotificationsEnabled())return false;
        Intent open=openIntent(c).setAction(item.key).putExtra("table",item.table).putExtra("id",item.id);
        PendingIntent tap=PendingIntent.getActivity(c,item.key.hashCode(),open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        String text=item.text;
        if(!item.table.equals("reports")){JSONObject record=find(store.table(item.table),item.id),person=find(store.table("players"),record.optString("playerID"));text+=(person==null?"":" • "+playerName(person))+" • "+record.optString("sport","Tennis");}
        Notification n=new Notification.Builder(c,channelId(store)).setSmallIcon(android.R.drawable.ic_menu_my_calendar).setContentTitle("Court Story").setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setVisibility(Notification.VISIBILITY_PRIVATE).setContentIntent(tap).setAutoCancel(true).build();
        manager.notify(item.key.hashCode(),n);return true;
    }
    static Intent openIntent(Context c){
        if(c.getPackageManager().hasSystemFeature("android.hardware.type.watch"))return new Intent().setClassName(c,"com.courtstory.app.WatchActivity").addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return phoneIntent(c);
    }
    // Phone-only task reuse; the watch branch above opens its launcher directly.
    @android.annotation.SuppressLint("WearRecents")
    private static Intent phoneIntent(Context c){return new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);}
    @Override public void onReceive(Context c,Intent i){
        PendingResult pending=goAsync();Context app=c.getApplicationContext();
        WORK.execute(()->{try{deliver(app,i);scheduleNow(app,new Store(app));}catch(Exception e){android.util.Log.w("CourtStory","Reminder delivery unavailable");}finally{pending.finish();}});
    }
}
