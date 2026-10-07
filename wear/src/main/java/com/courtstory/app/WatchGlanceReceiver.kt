package com.courtstory.app
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester

/** Push requests are bounded to Google's recommended five-minute average cadence. */
class WatchGlanceReceiver:BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent) {
        val preferences=context.getSharedPreferences("watch-glances",0)
        val now=System.currentTimeMillis()
        if(!intent.getBooleanExtra("privacyChanged",false)&&now-preferences.getLong("lastUpdate",0) in 0 until 300000)return
        preferences.edit().putLong("lastUpdate",now).apply()
        for(type in arrayOf(CurrentCourtComplication::class.java,NextCourtComplication::class.java,WeekCourtComplication::class.java,LatestCourtComplication::class.java,TrainingCourtComplication::class.java)){
            try{ComplicationDataSourceUpdateRequester.create(context,ComponentName(context,type)).requestUpdateAll()}catch(ignored:RuntimeException){/* The periodic provider request remains available. */}
        }
    }
}
