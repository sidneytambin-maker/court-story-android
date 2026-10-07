package com.courtstory.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Complications expose only the summary a wearer explicitly selects in their watch face. */
object CourtComplications {
    @JvmStatic fun render(context:Context,kind:String,type:ComplicationType,glance:CourtGlance,preview:Boolean):ComplicationData? {
        val shown=if(!preview&&!context.getSharedPreferences("watch-glances",0).getBoolean("showDetails",false))CourtGlance.empty(glance.title,"Open","Open Court Story for this summary.","")else glance
        val description=PlainComplicationText.Builder(shown.description+" Tap to open the latest saved record.").build()
        val image=MonochromaticImage.Builder(Icon.createWithResource(context,R.drawable.ic_court_complication)).build()
        val intent=Intent(context,WatchActivity::class.java).setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse("courtstory://watch-glance/$kind/${glance.recordID}"))
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra("table",glance.table).putExtra("id",glance.recordID).putExtra("courtAction",glance.action)
        val tap=if(preview)null else PendingIntent.getActivity(context,(kind+glance.recordID).hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val title=if(glance.title.length>7)"Court" else glance.title
        return when(type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(PlainComplicationText.Builder(shown.compact.take(7)).build(),description)
                .setTitle(PlainComplicationText.Builder(title).build()).setMonochromaticImage(image).setTapAction(tap)
                .build()
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(PlainComplicationText.Builder(shown.detail).build(),description)
                .setTitle(PlainComplicationText.Builder("Court Story · ${glance.title}").build()).setMonochromaticImage(image).setTapAction(tap)
                .build()
            ComplicationType.MONOCHROMATIC_IMAGE -> MonochromaticImageComplicationData.Builder(image,description).setTapAction(tap)
                .build()
            else -> null
        }
    }
}

abstract class CourtComplicationService(private val kind:String):SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request:ComplicationRequest):ComplicationData? = withContext(Dispatchers.IO) {
        if(getSystemService(android.app.KeyguardManager::class.java).isDeviceLocked)return@withContext NoDataComplicationData()
        val glance=try{CourtGlance.make(Store(this@CourtComplicationService).data,kind,System.currentTimeMillis()).withWorkout(WatchTransport.read(java.io.File(filesDir,"watch-workout.json"))?:org.json.JSONObject())}
            catch(error:Exception){CourtGlance.empty("Court Story","Open","Open Court Story to check your saved library.","")}
        CourtComplications.render(this@CourtComplicationService,kind,request.complicationType,glance,false)
    }
    override fun getPreviewData(type:ComplicationType):ComplicationData? {
        val glance=when(kind){
            "current" -> CourtGlance.empty("Now","2–1","Your current court activity","")
            "next" -> CourtGlance.empty("Next","14:00","Your next scheduled court activity","")
            "week" -> CourtGlance.empty("Week","90m","2 training sessions · 1 match","")
            "latest" -> CourtGlance.empty("Latest","Win","Your most recently completed activity","")
            else -> CourtGlance.empty("Training","Start","Choose or resume your training session","")
        }
        return CourtComplications.render(this,kind,type,glance,true)
    }
}
class CurrentCourtComplication:CourtComplicationService("current")
class NextCourtComplication:CourtComplicationService("next")
class WeekCourtComplication:CourtComplicationService("week")
class LatestCourtComplication:CourtComplicationService("latest")
class TrainingCourtComplication:CourtComplicationService("startTraining")
