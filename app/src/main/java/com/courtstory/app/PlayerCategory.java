package com.courtstory.app;
import android.widget.*;import android.view.View;import android.app.AlertDialog;import org.json.JSONObject;import java.util.function.Consumer;import static com.courtstory.app.Domain.*;
/** One category presentation for setup and profiles; selection never implies a diagnosis. */
final class PlayerCategory {
 static String type(JSONObject p){return p.optString("playerType","Sighted");}
 static String description(JSONObject p){String result=switch(type(p)){
  case "Visually impaired" -> "Visually impaired player selected. Add your sight classification if known and your preferred communication or equipment adaptations.";
  case "Wheelchair" -> "Wheelchair player selected. Add your division where relevant and your court or equipment preferences.";
  case "Deaf" -> "Deaf player selected. Add your preferred communication methods and visual cues.";
  case "Learning disability" -> "Learning disability selected. Add the instructions, pacing and learning support that work for you.";
  case "Custom" -> p.optString("customPlayerType").trim().isEmpty()?"Custom player category selected. Describe your category and the adaptations you prefer.":"Custom player category: "+p.optString("customPlayerType")+". Add the adaptations you prefer.";
  default -> "Sighted player selected. Standard visual play preferences; you can customise your match rules and preferences.";
 };return ProfileSetup.coach(p)?result.replace("player selected", "coach selected").replace("player category", "coach access profile"):result;}
 static void select(JSONObject p,String type){String previous=type(p);put(p,"playerType",type);put(p,"playerMode",type.equals("Visually impaired")?"Blind or visually impaired tennis":"Standard tennis");if(type.equals("Sighted"))put(p,"sightLevel",MainActivity.CLASSIFICATIONS[5]);else if(type.equals("Visually impaired")&&(!p.has("sightLevel")||p.optString("sightLevel").startsWith("Fully Sighted")))put(p,"sightLevel","Not known");}
 static Consumer<String> controls(MainActivity a,JSONObject p,boolean details){
  if(!p.has("playerType"))put(p,"playerType","Sighted");
  String label=ProfileSetup.coach(p)?"Access profile":"Player type";Button choice=a.button(label+": "+type(p),()->{});TextView description=a.text(description(p),16,false);description.setTextColor(a.muted);description.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);a.body.addView(description,a.lp());
  LinearLayout adaptive=a.column();a.body.addView(adaptive);Runnable render=()->{adaptive.removeAllViews();LinearLayout parent=a.body;a.body=adaptive;if(details)preferences(a,p,()->description.setText(description(p)));a.body=parent;};render.run();
  Consumer<String> changed=value->{select(p,value);choice.setText(label+": "+value);description.setText(description(p));render.run();choice.requestFocus();};
  choice.setOnClickListener(v->new AlertDialog.Builder(a).setTitle(ProfileSetup.coach(p)?"Your access preferences (optional)":"Player type").setSingleChoiceItems(ProfileSetup.TYPES,java.util.Arrays.asList(ProfileSetup.TYPES).indexOf(type(p)),(d,w)->{d.dismiss();changed.accept(ProfileSetup.TYPES[w]);}).setNegativeButton("Cancel",null).show());return changed;
 }
 static void preferences(MainActivity a,JSONObject p,Runnable changed){
  String type=type(p);boolean tennis=ProfileSetup.sport(p).equals("Tennis");
  if(type.equals("Custom")){EditText input=a.field(p,"customPlayerType","Your player type",false);input.addTextChangedListener(a.watcher(value->changed.run()));}
  if(type.equals("Visually impaired")){if(tennis){if(!p.has("sightLevel")||p.optString("sightLevel").startsWith("Fully Sighted"))put(p,"sightLevel","Not known");a.choice(p,"sightLevel","Sight classification",a.CLASSIFICATIONS);}else a.field(p,"sportClassification","Sport classification (optional)",false);}
  if(type.equals("Wheelchair")){if(tennis)a.choice(p,"wheelchairDivision","Wheelchair division",split("Not specified|Open|Quad|Custom"));else a.field(p,"sportClassification","Sport classification or division (optional)",false);a.field(p,"adaptivePreferences","Court and equipment preferences",false);}
  if(type.equals("Deaf"))a.field(p,"communicationPreferences","Communication and visual cue preferences",false);
  if(type.equals("Learning disability"))a.field(p,"learningPreferences","Instructions and learning preferences",false);
  if(!type.equals("Sighted"))a.field(p,"accessibilityPreferences","Communication, equipment or learning preferences",false);
  a.note(AdaptiveRules.guidance(ProfileSetup.sport(p),p));a.field(p,"bounceAllowance","Personal bounce override (optional; 0 for none)",true);a.field(p,"ruleAdaptations","Your preferred rule or training adaptations (optional)",false);a.note("All scoring and adaptation settings remain editable for each match. Your preferences do not change the rules for players you coach.");
 }
}
