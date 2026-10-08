package com.courtstory.app;
import android.content.Context;
import com.google.android.gms.wearable.*;
public final class WatchSyncService extends WearableListenerService {
    @Override public void onCapabilityChanged(CapabilityInfo capability){if(capability.getName().equals("court_story_library")&&!capability.getNodes().isEmpty())WatchAutoSend.retry(this);}
    @Override public void onDataChanged(DataEventBuffer events){
        for(DataEvent event:events)
            if(event.getType()==DataEvent.TYPE_CHANGED&&event.getDataItem().getUri().getPath()!=null&&event.getDataItem().getUri().getPath().startsWith(WatchTransport.PATH))
                receiveItem(this,event.getDataItem());
    }
    static void receiveItem(Context context,DataItem item){
        try{
            if(WatchTransport.receive(context,item))
                context.getSharedPreferences("watch-connection",0).edit().putString("status","Incoming library ready to review").apply();
        }catch(Exception e){
            context.getSharedPreferences("watch-connection",0).edit().putString("status",e.getMessage()==null?"Watch transfer needs review":e.getMessage()).apply();
        }
    }
}
