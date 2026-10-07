package com.courtstory.app

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.util.function.Consumer

/** Uses Google's provider on Android 9–13 and the system service on Android 14+. */
object HealthBridge {
    private val permissions = setOf(HealthPermission.getWritePermission(ExerciseSessionRecord::class))
    private const val provider = "com.google.android.apps.healthdata"

    @JvmStatic fun belongsToCurrentProfile(a: MainActivity, session: JSONObject): Boolean =
        a.store.player()?.optString("id") == session.optString("playerID")

    internal fun available(a: MainActivity, error: Consumer<String>): Boolean {
        when (HealthConnectClient.getSdkStatus(a)) {
            HealthConnectClient.SDK_AVAILABLE -> return true
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                AlertDialog.Builder(a).setTitle("Set up Health Connect")
                    .setMessage("Install or update Google Health Connect to save workouts. Your Court Story training records stay available without it.")
                    .setNegativeButton("Not now", null)
                    .setPositiveButton("Open Google Play") { _, _ ->
                        try { a.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$provider"))) }
                        catch (_: Exception) {
                            try { a.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$provider"))) }
                            catch (_: Exception) { error.accept("Google Play could not be opened. Install Health Connect from Google Play, then return here.") }
                        }
                    }.show()
            }
            else -> error.accept("Health Connect is unavailable on this device or user profile. Your workout remains saved in Court Story.")
        }
        return false
    }

    @JvmStatic fun permissions(a: MainActivity, error: Consumer<String>) {
        if (!available(a, error)) return
        a.lifecycleScope.launch {
            try {
                val client = HealthConnectClient.getOrCreate(a)
                if (!client.permissionController.getGrantedPermissions().containsAll(permissions)) {
                    a.pendingHealthAction = null
                    a.pendingHealthRecord = null
                    a.healthPermissionLauncher.launch(permissions)
                } else {
                    val action = if (Build.VERSION.SDK_INT >= 34) "android.health.connect.action.HEALTH_HOME_SETTINGS" else "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"
                    a.startActivity(Intent(action))
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { error.accept("Health Connect could not open its permissions. Your Court Story records are unchanged.") }
        }
    }

    @JvmStatic fun permissionResult(a: MainActivity, granted: Set<String>) {
        if (a.pendingHealthAction == "import") {
            a.pendingHealthAction = null
            val recordId = a.pendingHealthRecord
            a.pendingHealthRecord = null
            val session = recordId?.let { Domain.find(a.store.table("trainingSessions"), it) }
            if (session != null && granted.contains(HealthPermission.getReadPermission(ExerciseSessionRecord::class))) HealthImport.choose(a, session)
            else a.announce("Workout reading was not granted. Your training record is unchanged.")
            return
        }
        val recordId = a.pendingHealthRecord
        a.pendingHealthRecord = null
        if (!granted.containsAll(permissions)) {
            a.announce("Health permission was not granted. Your workout is still saved in Court Story.")
            return
        }
        val session = recordId?.let { Domain.find(a.store.table("trainingSessions"), it) }
        if (session != null) save(a, session, a::announce, a::error)
        else a.announce("Health Connect permission granted. You can save your completed workouts from their summaries.")
    }

    @JvmStatic fun record(session: JSONObject): ExerciseSessionRecord {
        require(session.optString("id").isNotBlank()) { "Save the training session first." }
        require(Domain.status("trainingSessions", session) == "Completed") { "Finish the training session before saving a workout." }
        val duration = Domain.duration(session).toLong() * 60_000L
        require(duration > 0) { "Enter a workout duration greater than zero." }
        val end = Domain.millis(session, "actualFinish").takeIf { it > 0 }
            ?: (Domain.millis(session, "date") + duration)
        val start = Domain.millis(session, "actualStart").takeIf { it > 0 } ?: (end - duration)
        require(start > 0 && end > start) { "A workout needs a finish time after its start." }
        require(end <= System.currentTimeMillis()) { "The workout finish time is in the future. Check the date and duration first." }
        val startTime = Instant.ofEpochMilli(start)
        val endTime = Instant.ofEpochMilli(end)
        val zone = ZoneId.systemDefault().rules
        return ExerciseSessionRecord(
            startTime = startTime, startZoneOffset = zone.getOffset(startTime),
            endTime = endTime, endZoneOffset = zone.getOffset(endTime),
            exerciseType = when (session.optString("sport", "Tennis")) {
                "Tennis" -> ExerciseSessionRecord.EXERCISE_TYPE_TENNIS
                "Badminton" -> ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON
                "Squash" -> ExerciseSessionRecord.EXERCISE_TYPE_SQUASH
                "Racquetball" -> ExerciseSessionRecord.EXERCISE_TYPE_RACQUETBALL
                "Table tennis" -> ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS
                else -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            },
            title = "Court Story · ${session.optString("sport", "Tennis")} · ${session.optString("trainingType", "Training")}",
            metadata = Metadata.manualEntry(
                clientRecordId = "court-story-${session.optString("id")}",
                clientRecordVersion = session.optLong("revision", 1).coerceAtLeast(1)
            )
        )
    }

    @JvmStatic fun save(a: MainActivity, session: JSONObject, success: Consumer<String>, error: Consumer<String>) {
        if (!belongsToCurrentProfile(a, session)) {
            error.accept("A coached player’s workout cannot be saved to your personal Health Connect profile.")
            return
        }
        val record = try { record(session) } catch (e: IllegalArgumentException) { error.accept(e.message ?: "Check the workout date and duration."); return }
        if (!available(a, error)) return
        a.lifecycleScope.launch {
            try {
                val client = HealthConnectClient.getOrCreate(a)
                if (!client.permissionController.getGrantedPermissions().containsAll(permissions)) {
                    a.pendingHealthAction = null
                    a.pendingHealthRecord = session.optString("id")
                    a.healthPermissionLauncher.launch(permissions)
                    return@launch
                }
                client.insertRecords(listOf(record))
                success.accept("Workout saved to Health Connect")
            } catch (e: CancellationException) { throw e }
            catch (_: SecurityException) { error.accept("Health permission changed. Reconnect Health Connect in Settings and try again. Your training record is safe.") }
            catch (_: Exception) { error.accept("Health Connect could not save this workout. Try again when it is available. Your Court Story training record is safe.") }
        }
    }
}

