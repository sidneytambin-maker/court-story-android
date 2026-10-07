package com.courtstory.app;

import android.content.Context;
import android.util.AtomicFile;
import com.google.android.gms.wearable.*;
import com.google.android.gms.tasks.Tasks;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import org.json.*;

/** Optional system-managed encrypted transport. Incoming libraries are staged, never applied over an editor. */
final class WatchTransport {
    static final String PATH="/court-story/library/";
    static final ExecutorService IO=Executors.newSingleThreadExecutor();
    static boolean enabled(Context c){return c.getSharedPreferences("watch-connection",0).getBoolean("enabled",false);}
    static File inbox(Context c){return new File(c.getFilesDir(),"watch-inbox.json");}
    static File baseline(Context c){return new File(c.getFilesDir(),"watch-baseline.json");}
    static JSONObject read(File file) throws Exception {if(!file.exists())return null;try(InputStream in=new FileInputStream(file)){return new JSONObject(new String(readBytes(in),StandardCharsets.UTF_8));}}
    static byte[] readBytes(InputStream in) throws IOException {ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>20*1024*1024)throw new IOException("Watch data exceeds the transfer limit.");out.write(buffer,0,n);}return out.toByteArray();}
    static void write(File file,JSONObject data) throws IOException {AtomicFile f=new AtomicFile(file);FileOutputStream stream=null;try{stream=f.startWrite();stream.write(data.toString().getBytes(StandardCharsets.UTF_8));f.finishWrite(stream);}catch(IOException e){if(stream!=null)f.failWrite(stream);throw e;}}
    static void checkIncoming(Context context,java.util.function.Consumer<String> done){Context c=context.getApplicationContext();IO.execute(()->{try{
        if(!enabled(c))throw new IOException("Enable the connection before checking incoming records.");
        DataItemBuffer items=Tasks.await(Wearable.getDataClient(c).getDataItems(),20,TimeUnit.SECONDS);
        try{for(DataItem item:items){String path=item.getUri().getPath();if(path!=null&&path.startsWith(PATH))receive(c,item);}}finally{items.release();}
        done.accept(inbox(c).exists()?"Incoming records are ready to review.":"No incoming records yet. Send your latest records from the other device.");
    }catch(Exception e){done.accept(e instanceof IOException?e.getMessage():"Could not check the connection yet. Your saved records are unchanged.");}});}
    static void disable(Context context,java.util.function.Consumer<String> done){Context c=context.getApplicationContext();c.getSharedPreferences("watch-connection",0).edit().putBoolean("enabled",false).apply();IO.execute(()->{try{
        String node=Tasks.await(Wearable.getNodeClient(c).getLocalNode(),15,TimeUnit.SECONDS).getId();
        android.net.Uri uri=new android.net.Uri.Builder().scheme("wear").authority(node).path(PATH+node).build();
        Tasks.await(Wearable.getDataClient(c).deleteDataItems(uri),20,TimeUnit.SECONDS);
        done.accept("Connection disabled and this device's outgoing transfer removed. Records already received on either device remain there.");
    }catch(Exception e){done.accept("Connection disabled on this device. The pending outgoing transfer could not be removed yet; records already sent may remain on your paired device. Reconnect and disable again to retry removal.");}});}
    static void send(Context context,java.util.function.Consumer<String> done){Context c=context.getApplicationContext();IO.execute(()->{try{if(!enabled(c))throw new IOException("Enable the optional phone/watch connection first.");Store s=new Store(c);if(Domain.array(s.data,"androidWatchConflicts").length()>0)throw new IOException("Review conflicting records before sending another library.");byte[] bytes=s.data.toString().getBytes(StandardCharsets.UTF_8);if(bytes.length>20*1024*1024)throw new IOException("This library is too large for watch transfer.");String node=Tasks.await(Wearable.getNodeClient(c).getLocalNode(),15,TimeUnit.SECONDS).getId();PutDataMapRequest request=PutDataMapRequest.create(PATH+node);request.getDataMap().putAsset("library",Asset.createFromBytes(bytes));request.getDataMap().putLong("sentAt",System.currentTimeMillis());if(!enabled(c))throw new IOException("Connection disabled; no new transfer was sent.");Tasks.await(Wearable.getDataClient(c).putDataItem(request.asPutDataRequest().setUrgent()),20,TimeUnit.SECONDS);done.accept("Library sent through your optional encrypted watch connection. Open the connection screen on the other device to review incoming records.");}catch(Exception e){done.accept("Could not send yet. Check the watch is paired and Google Play services are available. Your records remain saved on this device.");}});}
    static void receive(Context c,DataItem item) throws Exception {if(!enabled(c))return;String localNode=Tasks.await(Wearable.getNodeClient(c).getLocalNode(),15,TimeUnit.SECONDS).getId();if(localNode.equals(item.getUri().getHost()))return;Asset asset=DataMapItem.fromDataItem(item).getDataMap().getAsset("library");if(asset==null)return;DataClient.GetFdForAssetResponse response=Tasks.await(Wearable.getDataClient(c).getFdForAsset(asset),20,TimeUnit.SECONDS);try(InputStream in=response.getInputStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(bytes.size()+n>20*1024*1024)throw new IOException("Incoming library exceeds the watch transfer limit.");bytes.write(buffer,0,n);}JSONObject incoming=new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));String issue=Domain.validate(incoming,false);if(issue!=null)throw new IOException(issue);synchronized(WatchTransport.class){if(!enabled(c))return;JSONObject existing=read(inbox(c));if(existing!=null&&!WatchMerge.same(existing,incoming))throw new IOException("Review the existing incoming library before receiving another.");write(inbox(c),incoming);}}finally{response.release();}}
}
