package com.courtstory.app

import android.app.AlertDialog
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.*
import java.time.format.DateTimeFormatter

/** User-selected workouts and their source's measurements; never guesses missing sensor values. */
object HealthImport {
    private val exercisePermission = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    private val readPermissions = setOf(exercisePermission,
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class))

    @JvmStatic fun choose(a: MainActivity, session: JSONObject) {
        if (!HealthBridge.belongsToCurrentProfile(a,session)) { a.error("Only attach your own workout to your personal training record.");return }
        if (!HealthBridge.available(a,a::error)) return
        a.lifecycleScope.launch {
            try {
                val client = HealthConnectClient.getOrCreate(a)
                val granted = client.permissionController.getGrantedPermissions()
                if (exercisePermission !in granted) {
                    a.pendingHealthAction = "import"
                    a.pendingHealthRecord = session.optString("id")
                    a.healthPermissionLauncher.launch(readPermissions)
                    return@launch
                }
                val day = Instant.ofEpochMilli(Domain.millis(session,"date")).atZone(ZoneId.systemDefault()).toLocalDate()
                val start = day.atStartOfDay(ZoneId.systemDefault()).toInstant()
                val end = day.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                val workouts = mutableListOf<ExerciseSessionRecord>()
                var page: String? = null
                do {
                    val response = client.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class,
                        timeRangeFilter=TimeRangeFilter.between(start,end),pageToken=page))
                    workouts.addAll(response.records.filter { it.endTime <= Instant.now() })
                    page = response.pageToken
                } while(page != null)
                workouts.sortByDescending { it.startTime }
                if (workouts.isEmpty()) {
                    a.error("No completed workouts are available on $day. Check that your fitness app syncs to Health Connect and that this session has the correct date. Access to older workouts may be restricted by Health Connect.")
                    return@launch
                }
                val format = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
                val labels = workouts.map { "${it.title?.takeIf(String::isNotBlank) ?: "Workout"} · ${format.format(it.startTime)} · ${Duration.between(it.startTime,it.endTime).toMinutes()} minutes" }.toTypedArray()
                AlertDialog.Builder(a).setTitle("Choose your workout · $day")
                    .setItems(labels) { _, index -> preview(a,session,workouts[index]) }
                    .setNegativeButton("Cancel",null).show()
            } catch(e: CancellationException) { throw e }
            catch(_: SecurityException) { a.error("Health Connect reading is not available for this date or permission. Review access in Health Connect and try again.") }
            catch(_: Exception) { a.error("Health Connect could not load your workouts. Your Court Story session is unchanged.") }
        }
    }

    internal suspend fun measurements(client: HealthConnectClient, workout: ExerciseSessionRecord): JSONObject {
        val granted=client.permissionController.getGrantedPermissions()
        val metrics=mutableSetOf<AggregateMetric<*>>()
        if(HealthPermission.getReadPermission(HeartRateRecord::class) in granted) metrics.addAll(setOf(HeartRateRecord.BPM_AVG,HeartRateRecord.BPM_MAX))
        if(HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class) in granted) metrics.add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
        if(HealthPermission.getReadPermission(DistanceRecord::class) in granted) metrics.add(DistanceRecord.DISTANCE_TOTAL)
        if(HealthPermission.getReadPermission(StepsRecord::class) in granted) metrics.add(StepsRecord.COUNT_TOTAL)
        val summary=JSONObject()
        Domain.put(summary,"androidHealthRecordID",workout.metadata.id)
        Domain.put(summary,"source","Health Connect")
        Domain.put(summary,"sourcePackage",workout.metadata.dataOrigin.packageName)
        Domain.put(summary,"durationSeconds",Duration.between(workout.startTime,workout.endTime).seconds)
        if(metrics.isNotEmpty()) {
            val values=client.aggregate(AggregateRequest(metrics,
                TimeRangeFilter.between(workout.startTime,workout.endTime),setOf(workout.metadata.dataOrigin)))
            values[HeartRateRecord.BPM_AVG]?.let { Domain.put(summary,"averageHeartRate",it) }
            values[HeartRateRecord.BPM_MAX]?.let { Domain.put(summary,"peakHeartRate",it) }
            values[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.let { Domain.put(summary,"activeEnergyKcal",it.inKilocalories) }
            values[DistanceRecord.DISTANCE_TOTAL]?.let { Domain.put(summary,"distanceMeters",it.inMeters) }
            values[StepsRecord.COUNT_TOTAL]?.let { Domain.put(summary,"stepCount",it) }
        }
        return summary
    }

    private fun preview(a: MainActivity, original: JSONObject, workout: ExerciseSessionRecord) {
        a.lifecycleScope.launch {
            try {
                val summary=measurements(HealthConnectClient.getOrCreate(a),workout)
                val info=WorkoutSummary.text(summary)+"\n\nAttach these measurements to this session? The workout's start and finish replace its recorded timer. Any manual duration choice is kept. Measurements come only from the selected workout's source app."
                AlertDialog.Builder(a).setTitle("Review your workout").setMessage(info)
                    .setNegativeButton("Cancel",null).setPositiveButton("Attach workout") { _, _ ->
                        val latest=Domain.find(a.store.table("trainingSessions"),original.optString("id"))
                        if(latest==null || !HealthBridge.belongsToCurrentProfile(a,latest)) { a.error("This session is no longer available in your current profile.");return@setPositiveButton }
                        val next=Domain.copy(latest)
                        Domain.put(next,"workout",summary)
                        Domain.put(next,"actualStart",workout.startTime.toString())
                        Domain.put(next,"actualFinish",workout.endTime.toString())
                        Domain.put(next,"androidScheduled",false)
                        if(next.optString("durationSource")!="manual") Domain.put(next,"durationSource","workout")
                        if(a.saveRecord("trainingSessions",next)){a.detail("trainingSessions",next);a.announce("Workout measurements attached")}
                    }.show()
            } catch(e: CancellationException) { throw e }
            catch(_: Exception) { a.error("These workout measurements could not be read. Your Court Story session is unchanged.") }
        }
    }
}
