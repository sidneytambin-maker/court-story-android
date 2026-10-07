package com.courtstory.app

import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.*
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Real provider round-trip on the disposable emulator; never writes health data on a person's phone. */
class HealthIntegrationTest : DeviceFlowTest() {
    fun testProviderInsertUpdateReadAndDelete() {
        assertTrue("Health provider test is restricted to the disposable emulator", Build.FINGERPRINT.contains("generic") || Build.FINGERPRINT.contains("sdk_gphone"))
        assertTrue(Build.VERSION.SDK_INT >= 34)
        seed()
        val session = AtomicReference<JSONObject>()
        ui {
            val s = a.newRecord("trainingSessions")
            Domain.put(s, "androidScheduled", false)
            Domain.put(s, "date", Instant.now().minusSeconds(7200).toString())
            Domain.put(s, "durationMinutes", 20)
            Domain.put(s, "trainingType", "DEMO Health Connect integration")
            assertTrue(a.saveRecord("trainingSessions", s))
            session.set(Domain.copy(Domain.find(a.store.table("trainingSessions"),s.optString("id"))))
        }
        val client = HealthConnectClient.getOrCreate(a)
        val clientId = "court-story-${session.get().optString("id")}";
        try {
            val start=HealthBridge.record(session.get()).startTime
            val end=start.plusSeconds(1200)
            runBlocking {
                client.insertRecords(listOf(
                    HeartRateRecord(start,null,end,null,listOf(HeartRateRecord.Sample(start.plusSeconds(60),100),HeartRateRecord.Sample(start.plusSeconds(120),140)),Metadata.manualEntry(clientRecordId="$clientId-heart")),
                    ActiveCaloriesBurnedRecord(start,null,end,null,Energy.kilocalories(100.0),Metadata.manualEntry(clientRecordId="$clientId-energy")),
                    DistanceRecord(start,null,end,null,Length.meters(500.0),Metadata.manualEntry(clientRecordId="$clientId-distance")),
                    StepsRecord(start,null,end,null,1000,Metadata.manualEntry(clientRecordId="$clientId-steps"))
                ))
            }
            for (duration in listOf(20, 35)) {
                ui {
                    Domain.put(session.get(),"durationMinutes",duration)
                    assertTrue(a.saveRecord("trainingSessions",session.get()))
                    session.set(Domain.copy(Domain.find(a.store.table("trainingSessions"),session.get().optString("id"))))
                }
                val done = CountDownLatch(1)
                val error = AtomicReference<String>()
                ui { HealthBridge.save(a, session.get(), { done.countDown() }, { error.set(it);done.countDown() }) }
                assertTrue("Health provider responded",done.await(20,TimeUnit.SECONDS))
                assertNull(error.get())
                runBlocking {
                    val records = client.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class,TimeRangeFilter.between(Instant.now().minusSeconds(86400),Instant.now())))
                        .records.filter { it.metadata.clientRecordId == clientId }
                    assertEquals("Repeated export updates one workout",1,records.size)
                    assertEquals(duration.toLong()*60,java.time.Duration.between(records[0].startTime,records[0].endTime).seconds)
                    val measurements=HealthImport.measurements(client,records[0])
                    assertEquals(120,measurements.optInt("averageHeartRate"))
                    assertEquals(140,measurements.optInt("peakHeartRate"))
                    val rawEnergy=client.readRecords(ReadRecordsRequest(ActiveCaloriesBurnedRecord::class,TimeRangeFilter.between(start,end))).records
                    assertEquals("$measurements; raw energy=${rawEnergy.map { it.energy.inKilocalories }}",100.0,measurements.optDouble("activeEnergyKcal"),0.01)
                    assertEquals(500,measurements.optInt("distanceMeters"))
                    assertEquals(1000,measurements.optInt("stepCount"))
                }
            }
        } finally {
            runBlocking {
                client.deleteRecords(ExerciseSessionRecord::class,emptyList(),listOf(clientId))
                client.deleteRecords(HeartRateRecord::class,emptyList(),listOf("$clientId-heart"))
                client.deleteRecords(ActiveCaloriesBurnedRecord::class,emptyList(),listOf("$clientId-energy"))
                client.deleteRecords(DistanceRecord::class,emptyList(),listOf("$clientId-distance"))
                client.deleteRecords(StepsRecord::class,emptyList(),listOf("$clientId-steps"))
            }
        }
    }
}
