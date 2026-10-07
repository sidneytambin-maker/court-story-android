package com.courtstory.app;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static com.courtstory.app.Domain.*;
/** Explicit, synthetic-only profiling fixture; never included in the regular regression run. */
public class PerformanceAuditTest extends DeviceFlowTest {
    public void testLargeLibraryStages() throws Exception {
        File fixture=new File(a.getCacheDir(),"QA-large-library.json");JSONObject library=new JSONObject(new String(java.nio.file.Files.readAllBytes(fixture.toPath()),StandardCharsets.UTF_8));
        for(JSONObject player:rows(table(library,"players")))assertTrue(player.optString("name").startsWith("DEMO")||player.optString("name").startsWith("QA"));
        assertEquals(3000,table(library,"matches").length());assertEquals(3000,table(library,"trainingSessions").length());
        ui(()->{a.store.data=library;a.save();});JSONObject timings=obj();JSONArray reads=new JSONArray(),homes=new JSONArray(),lists=new JSONArray(),coached=new JSONArray();
        for(int i=0;i<3;i++){
            long begin=android.os.SystemClock.elapsedRealtime();Store loaded=new Store(a);reads.put(android.os.SystemClock.elapsedRealtime()-begin);
            begin=android.os.SystemClock.elapsedRealtime();ui(()->{a.store=loaded;a.home();});homes.put(android.os.SystemClock.elapsedRealtime()-begin);
            begin=android.os.SystemClock.elapsedRealtime();ui(()->a.list("matches"));lists.put(android.os.SystemClock.elapsedRealtime()-begin);
            begin=android.os.SystemClock.elapsedRealtime();ui(()->{JSONObject subject=null;for(JSONObject p:rows(a.store.table("players")))if(p.optString("name").equals("DEMO Player Power"))subject=p;assertNotNull(subject);CoachWorkspace.records(a,subject,"matches");});coached.put(android.os.SystemClock.elapsedRealtime()-begin);
        }
        put(timings,"readAndValidateMs",reads);put(timings,"coachDashboardMs",homes);put(timings,"personalMatchHistoryMs",lists);put(timings,"coachedMatchHistoryMs",coached);
        java.nio.file.Files.write(new File(a.getCacheDir(),"QA-performance-stages.json").toPath(),timings.toString(2).getBytes(StandardCharsets.UTF_8));
    }
}
