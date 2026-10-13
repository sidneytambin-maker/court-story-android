package com.courtstory.app;
import android.app.job.*;
import android.content.*;
public final class ConnectSyncJob extends JobService {
 static final int ID=6104;
 static void schedule(Context c){schedule(c,false);}
 static void schedule(Context c,boolean urgent){
  if(c.getPackageManager().hasSystemFeature("android.hardware.type.watch"))return;
  JobInfo.Builder builder=new JobInfo.Builder(ID,new ComponentName(c,ConnectSyncJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true).setBackoffCriteria(30000,JobInfo.BACKOFF_POLICY_EXPONENTIAL);
  if(urgent&&android.os.Build.VERSION.SDK_INT>=31)builder.setExpedited(true);else builder.setMinimumLatency(urgent?0:5000);
  JobScheduler scheduler=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
  if(scheduler.schedule(builder.build())==JobScheduler.RESULT_FAILURE&&urgent)schedule(c,false);
 }
 @Override public boolean onStartJob(JobParameters params){ConnectClient c=ConnectClient.get(this);c.worker.execute(()->{boolean retry=false;try{if(c.signedIn()){c.sync(new Store(this).data);ConnectPush.deliverPending(this,c);}}catch(Exception e){retry=true;c.status="Sync queued for the next connection.";}jobFinished(params,retry);});return true;}
 @Override public boolean onStopJob(JobParameters params){return true;}
}
