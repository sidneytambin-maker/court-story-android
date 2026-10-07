package com.courtstory.app;
import com.google.android.gms.wearable.*;
public final class WatchSyncService extends WearableListenerService {
    @Override public void onDataChanged(DataEventBuffer events){for(DataEvent event:events)if(event.getType()==DataEvent.TYPE_CHANGED&&event.getDataItem().getUri().getPath()!=null&&event.getDataItem().getUri().getPath().startsWith(WatchTransport.PATH))try{WatchTransport.receive(this,event.getDataItem());getSharedPreferences("watch-connection",0).edit().putString("status","Incoming library ready to review").apply();}catch(Exception e){getSharedPreferences("watch-connection",0).edit().putString("status",e.getMessage()==null?"Watch transfer needs review":e.getMessage()).apply();}}
}
