package com.courtstory.app

import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import androidx.health.services.client.*
import androidx.health.services.client.data.*
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File
import java.time.Instant
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max

/** Health Services owns the sensor lifecycle; a foreground service keeps its callback alive. */
class WatchWorkoutService : Service(), ExerciseUpdateCallback {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val actionLock = Mutex()
    private val client by lazy { HealthServices.getClient(this).exerciseClient }
    private var state = JSONObject()
    private var finishing = false
    private var announcedState = ""
    private fun file() = File(filesDir, "watch-workout.json")
    private fun persist() {
        WatchTransport.write(file(), state)
        val key=state.optString("phase")+":"+state.optBoolean("error")+":"+state.optString("message")
        if(key!=announcedState){announcedState=key;sendBroadcast(Intent("com.courtstory.app.WORKOUT_STATE").setPackage(packageName).putExtra("record",state.optString("record")))}
    }
    private suspend fun <T> ListenableFuture<T>.result(): T = suspendCancellableCoroutine { continuation ->
        addListener({ try { continuation.resume(get()) } catch (e: Exception) { continuation.resumeWithException(e) } }, mainExecutor)
    }
    override fun onBind(intent: Intent?) = null
    override fun onCreate() { super.onCreate(); state = try { WatchTransport.read(file()) ?: JSONObject() } catch(e:Exception) { JSONObject() }; client.setUpdateCallback(this) }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("court_workout", "Court Story workout", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 17, Intent(this, WatchActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try { startForeground(17, Notification.Builder(this, "court_workout").setSmallIcon(R.drawable.ic_court_monochrome).setContentTitle("Court Story training").setContentText("Workout recording controls on your watch").setOngoing(true).setContentIntent(open).build()) }
        catch(e:RuntimeException){state.put("phase","Failed").put("error",true).put("message","Workout permission is unavailable. You can finish or record training without sensors.");try{persist()}catch(ignored:Exception){};stopSelf();return START_NOT_STICKY}
        scope.launch {
            actionLock.withLock {
            try {
                when(intent?.action) {
                    "start" -> start(intent.getStringExtra("record") ?: error("Missing training record"))
                    "pause" -> client.pauseExerciseAsync().result()
                    "resume" -> client.resumeExerciseAsync().result()
                    "finish" -> { if(!state.optBoolean("committed"))client.endExerciseAsync().result() }
                    else -> {
                        val info=client.getCurrentExerciseInfoAsync().result()
                        if(info.exerciseTrackedStatus != ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS) finishRecord("Workout recording was interrupted. Saved available measurements.")
                    }
                }
            } catch(e: Exception) { if(e is CancellationException)throw e;state.put("message", "Workout action could not complete. Retry, or record training without sensors.");state.put("error",true);if(state.optString("phase")=="Preparing")state.put("phase","Failed");persist();if(state.optString("phase")!="Active"&&state.optString("phase")!="Paused"){stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()} }
            }
        }
        return START_STICKY
    }
    private suspend fun start(id:String) {
        val info=client.getCurrentExerciseInfoAsync().result()
        if(info.exerciseTrackedStatus==ExerciseTrackedStatus.OTHER_APP_IN_PROGRESS) throw IllegalStateException("Another app is recording a workout")
        if(info.exerciseTrackedStatus==ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS){if(state.optString("record")!=id)throw IllegalStateException("Finish the current Court Story workout first");return}
        val store=Store(this);val record=Domain.find(store.table("trainingSessions"),id) ?: error("Training record missing")
        if(record.optString("playerID")!=store.player()?.optString("id"))throw IllegalStateException("Only your own training can use your wrist sensors")
        state=JSONObject().put("record",id).put("phase","Preparing").put("startedAt",Domain.now());persist()
        val capabilities=client.getCapabilitiesAsync().result()
        val requested=when(record.optString("sport","Tennis")){"Tennis"->ExerciseType.TENNIS;"Badminton"->ExerciseType.BADMINTON;"Squash"->ExerciseType.SQUASH;"Table tennis"->ExerciseType.TABLE_TENNIS;else->ExerciseType.WORKOUT}
        val exercise=if(requested in capabilities.supportedExerciseTypes)requested else ExerciseType.WORKOUT
        if(exercise !in capabilities.supportedExerciseTypes)throw IllegalStateException("This watch does not support this workout type")
        val supported=capabilities.getExerciseTypeCapabilities(exercise).supportedDataTypes
        val types=mutableSetOf<DataType<*,*>>(DataType.CALORIES_TOTAL,DataType.DISTANCE_TOTAL,DataType.STEPS_TOTAL)
        val permission=if(Build.VERSION.SDK_INT>=36)"android.permission.health.READ_HEART_RATE" else "android.permission.BODY_SENSORS"
        if(checkSelfPermission(permission)==PackageManager.PERMISSION_GRANTED)types.add(DataType.HEART_RATE_BPM)
        types.retainAll(supported)
        try {
            client.startExerciseAsync(ExerciseConfig(exerciseType=exercise,dataTypes=types,isAutoPauseAndResumeEnabled=false,isGpsEnabled=false)).result()
            val fresh=Store(this);val current=Domain.copy(Domain.find(fresh.table("trainingSessions"),id) ?: error("Training record missing"))
            current.put("actualStart",state.optString("startedAt"));current.remove("actualFinish");current.put("androidScheduled",false);current.put("androidWearWorkout",true);fresh.saveRecord("trainingSessions",current)
            state.put("phase","Active").put("error",false).put("message","Workout recording started");persist()
        } catch(e:Exception){state.put("phase","Failed");persist();try{client.endExerciseAsync().result()}catch(ignored:Exception){};throw e}
    }
    override fun onExerciseUpdateReceived(update:ExerciseUpdate) {
        if(state.optString("record").isEmpty()||state.optString("phase")=="Failed")return
        val metrics=update.latestMetrics
        metrics.getData(DataType.CALORIES_TOTAL)?.let { state.put("activeEnergyKcal",it.total) }
        metrics.getData(DataType.DISTANCE_TOTAL)?.let { state.put("distanceMeters",it.total) }
        metrics.getData(DataType.STEPS_TOTAL)?.let { state.put("stepCount",it.total) }
        for(sample in metrics.getData(DataType.HEART_RATE_BPM)){
            val time=sample.timeDurationFromBoot.toMillis();val value=sample.value
            if(time>state.optLong("lastHeartSample",-1)&&value.isFinite()&&value>0){state.put("lastHeartSample",time);state.put("heartSum",state.optDouble("heartSum",0.0)+value);state.put("heartCount",state.optInt("heartCount")+1);state.put("peakHeartRate",max(state.optDouble("peakHeartRate",0.0),value));state.put("averageHeartRate",state.optDouble("heartSum")/state.optInt("heartCount"))}
        }
        update.activeDurationCheckpoint?.let { val running = !update.exerciseStateInfo.state.isPaused && !update.exerciseStateInfo.state.isEnded; val extra = if(running) max(0,java.time.Duration.between(it.time,Instant.now()).seconds) else 0; state.put("durationSeconds",it.activeDuration.seconds+extra) }
        val exerciseState=update.exerciseStateInfo.state
        state.put("phase",if(exerciseState.isPaused)"Paused" else if(exerciseState.isEnded)"Ended" else "Active")
        try{persist();if(exerciseState.isEnded)finishRecord("Training finished. Available workout measurements saved.")}catch(e:Exception){state.put("message","Workout measurements could not be saved. Keep the app installed and retry.")}
    }
    private fun finishRecord(message:String){
        if(finishing||state.optBoolean("committed")||state.optString("record").isEmpty())return;finishing=true
        try{val store=Store(this);val source=Domain.find(store.table("trainingSessions"),state.optString("record"));if(source!=null){val record=Domain.copy(source);val workout=JSONObject().put("source","Wear OS Health Services").put("durationSeconds",state.optLong("durationSeconds",max(1,(System.currentTimeMillis()-Domain.millis(state,"startedAt"))/1000)))
            for(key in arrayOf("averageHeartRate","peakHeartRate","activeEnergyKcal","distanceMeters","stepCount"))if(state.has(key))workout.put(key,state.get(key))
            record.put("workout",workout).put("actualFinish",Domain.now()).put("androidScheduled",false).put("durationSource","recorded");store.saveRecord("trainingSessions",record)}
            state.put("phase","Ended").put("committed",true).put("message",message);persist();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()
        }finally{finishing=false}
    }
    override fun onLapSummaryReceived(lapSummary:ExerciseLapSummary) {}
    override fun onAvailabilityChanged(dataType:DataType<*,*>,availability:Availability) { state.put("sensorAvailability",availability.toString());try{persist()}catch(ignored:Exception){} }
    override fun onRegistered() {}
    override fun onRegistrationFailed(throwable:Throwable) { state.put("message","Watch sensor service is unavailable. Training without sensors remains available.");try{persist()}catch(ignored:Exception){} }
    override fun onDestroy(){client.clearUpdateCallbackAsync(this);scope.cancel();super.onDestroy()}
}
