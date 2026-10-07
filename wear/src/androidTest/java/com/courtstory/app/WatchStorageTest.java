package com.courtstory.app;

import android.test.InstrumentationTestCase;
import android.content.Context;
import android.content.ContextWrapper;
import java.io.File;
import java.io.IOException;
import org.json.JSONObject;
import static com.courtstory.app.Domain.*;

/** Uses an isolated directory: never replaces the watch owner's library. */
public class WatchStorageTest extends InstrumentationTestCase {
    Context isolated;
    @Override protected void setUp() throws Exception {
        super.setUp();
        final File dir=new File(getInstrumentation().getTargetContext().getCacheDir(),"watch-storage-"+System.nanoTime());
        assertTrue(dir.mkdirs());
        isolated=new ContextWrapper(getInstrumentation().getTargetContext()) {
            @Override public Context getApplicationContext(){return this;}
            @Override public File getFilesDir(){return dir;}
        };
    }
    public void testStaleEditorCannotEraseBackgroundSave() throws Exception {
        Store first=new Store(isolated);first.save();Store stale=new Store(isolated);
        put(first.settings(),"milestonesEnabled",false);first.save();
        put(stale.settings(),"milestonesEnabled",true);
        try{stale.save();fail("Stale editor overwrote a newer save");}catch(IOException expected){assertTrue(expected.getMessage().contains("Refresh"));}
        assertFalse(new Store(isolated).settings().optBoolean("milestonesEnabled",true));
    }
    public void testFreshStoreCanSaveAgain() throws Exception {
        Store first=new Store(isolated);first.save();put(first.settings(),"milestonesEnabled",false);first.save();
        Store refreshed=new Store(isolated);put(refreshed.settings(),"milestonesEnabled",true);refreshed.save();
        assertTrue(new Store(isolated).settings().optBoolean("milestonesEnabled"));
    }
    public void testWatchStateRoundTripAndMissingFile() throws Exception {
        File file=new File(isolated.getFilesDir(),"state.json");assertNull(WatchTransport.read(file));
        JSONObject state=obj();put(state,"phase","Failed");put(state,"message","Training without sensors remains available.");
        WatchTransport.write(file,state);assertTrue(WatchMerge.same(state,WatchTransport.read(file)));
    }
    public void testTransportReadsUtf8OnOlderAndroid() throws Exception {
        byte[] expected="Court Story · entraînement".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(java.util.Arrays.equals(expected,WatchTransport.readBytes(new java.io.ByteArrayInputStream(expected))));
    }
}
