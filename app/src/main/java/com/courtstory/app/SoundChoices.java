package com.courtstory.app;
import android.app.AlertDialog;
import android.widget.Button;
import org.json.JSONObject;
import static com.courtstory.app.Domain.*;
final class SoundChoices {
    static final String[] KEYS=split("bounce|racketStrike|racketSwing|ballCan|applause");
    static final String[] NAMES=split("Tennis bounce|Racket strike|Racket swoosh|New ball can|Court applause");
    static final String[] FILES=split("tennis_bounce|tennis_racket_strike|tennis_racket_swoosh|tennis_ball_can|tennis_applause");
    static String title(String key){int i=java.util.Arrays.asList(KEYS).indexOf(key);return NAMES[Math.max(0,i)];}
    static void form(MainActivity a,JSONObject sounds){Button selected=a.button("Selected sound: "+title(sounds.optString("selected","bounce")),()->{});selected.setOnClickListener(v->new AlertDialog.Builder(a).setTitle("Select and preview a sound").setSingleChoiceItems(NAMES,Math.max(0,java.util.Arrays.asList(KEYS).indexOf(sounds.optString("selected","bounce"))),(d,i)->{put(sounds,"selected",KEYS[i]);selected.setText("Selected sound: "+NAMES[i]);a.playSound(FILES[i]);d.dismiss();a.announce(NAMES[i]+" selected. Save settings to keep this choice.");}).setNegativeButton("Cancel",null).show());}
    static void credits(MainActivity a){a.optional("Sound credits",()->{a.note("Five independent CC0 recordings, edited for Court Story.");for(String[] credit:new String[][]{{"Bounce: Joseph SARDIN / LaSonotheque","https://lasonotheque.org/balle-de-tennis-rebonds-s0584.html"},{"Racket strike: jacklilley","https://freesound.org/people/jacklilley/sounds/338122/"},{"Racket swoosh: MIKEJONESBONES","https://freesound.org/people/MIKEJONESBONES/sounds/511825/"},{"Ball can: tomschuetz","https://freesound.org/people/tomschuetz/sounds/649763/"},{"Court applause: muse88","https://freesound.org/people/muse88/sounds/490341/"}})a.button(credit[0],()->{try{a.startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(credit[1])));}catch(android.content.ActivityNotFoundException e){a.error("No browser is available to open the sound credit.");}});});}
}
