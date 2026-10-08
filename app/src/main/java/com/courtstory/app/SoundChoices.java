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
}
