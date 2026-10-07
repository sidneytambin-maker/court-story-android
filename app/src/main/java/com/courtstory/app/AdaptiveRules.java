package com.courtstory.app;
import org.json.JSONObject;import static com.courtstory.app.Domain.*;
/** Optional personal preferences; recorded per subject, never imposed on their roster. */
final class AdaptiveRules {
 static int bounces(String sport,JSONObject person){
  String type=person.optString("playerType","Sighted"),level=person.optString("sightLevel");
  if(type.equals("Wheelchair")&&(sport.equals("Tennis")||sport.equals("Pickleball")||sport.equals("Racquetball")))return 2;
  if(sport.equals("Tennis")&&type.equals("Visually impaired")){if(level.startsWith("B1")||level.startsWith("B2"))return 3;if(level.startsWith("B3"))return 2;if(level.startsWith("B4")||level.startsWith("B5"))return 1;return -1;}
  if(type.equals("Custom")||CustomScore.custom(sport)||sport.equals("Racketlon"))return -1;
  if(sport.equals("Badminton")||sport.equals("Beach tennis"))return 0;
  if(type.equals("Visually impaired")||type.equals("Wheelchair"))return -1;
  return 1;
 }
 static String guidance(String sport,JSONObject person){String type=person.optString("playerType","Sighted");
  if(sport.equals("Tennis")&&type.equals("Wheelchair"))return "Wheelchair tennis: two bounces are allowed; the second may land outside the court. The allowance applies to this player, not automatically to an opponent.";
  if(sport.equals("Pickleball")&&type.equals("Wheelchair"))return "Wheelchair pickleball: two bounces may be used before returning. This is separate from the serve-and-return two-bounce rule. Use the event's wheelchair service and non-volley-zone rules.";
  if(sport.equals("Racquetball")&&type.equals("Wheelchair"))return "IRF wheelchair racquetball permits two floor bounces on serves and rallies. Chair-position and wheel-fault rules also apply.";
  if(sport.equals("Tennis")&&type.equals("Visually impaired"))return "Blind tennis: B1 and B2 allow three bounces, B3 two, and B4 one. Choose your known classification; court, sound-ball and service-call arrangements depend on the event. You may override the allowance.";
  if(sport.equals("Tennis")&&(type.equals("Deaf")||type.equals("Learning disability")))return "Standard tennis scoring and one bounce apply by default. Communication or learning support does not automatically change the rules. Record any agreed training or competition adaptations below.";
  if(type.equals("Sighted"))return "Use this sport's standard rules, or record agreed adjustments below.";
  return "Use the selected sport's scoring. Add the classification, communication, equipment and agreed rule adaptations for this event. Court Story does not infer additional bounce allowances for this combination.";
 }
 static void apply(JSONObject match,JSONObject person){String sport=match.optString("sport","Tennis");put(match,"playerType",person.optString("playerType","Sighted"));int suggested=bounces(sport,person);if(person.has("bounceAllowance"))suggested=person.optInt("bounceAllowance",-1);if(suggested>=0)put(match,"allowedBounces",suggested);else match.remove("allowedBounces");
  if(!sport.equals("Tennis"))match.remove("sightLevel");
  for(String key:new String[]{"ruleAdaptations","communicationPreferences","learningPreferences","adaptivePreferences","accessibilityPreferences","customPlayerType","sportClassification"})if(person.has(key))put(match,key,person.opt(key));else match.remove(key);
 }
 static void matchForm(MainActivity a,JSONObject match){a.optional("Playing adaptations",()->{a.note(guidance(match.optString("sport","Tennis"),match));a.field(match,"allowedBounces","This player's allowed bounces (blank if not specified; 0 for none)",true);a.field(match,"opponentAllowedBounces","Opponent's allowed bounces (optional)",true);a.field(match,"ruleAdaptations","Agreed rules, court or equipment adaptations",false);a.field(match,"communicationPreferences","Communication and visual cue preferences",false);a.field(match,"learningPreferences","Instructions, pacing and learning preferences",false);a.field(match,"accessibilityPreferences","Other access preferences",false);a.note("These settings record your agreement. You award points and referee decisions yourself; the app does not detect bounces or faults.");});}
 static void summary(MainActivity a,JSONObject source,String sport){JSONObject person=SportProfiles.forSport(source,sport);a.optional("Player access and playing preferences",()->{a.note(PlayerCategory.description(person));a.note(guidance(sport,person));int allowance=person.has("bounceAllowance")?person.optInt("bounceAllowance",-1):bounces(sport,person);a.note("Allowed bounces: "+(allowance<0?"Not specified":allowance)+(person.has("bounceAllowance")?" • personal override":" • sport and category default"));for(String key:new String[]{"ruleAdaptations","communicationPreferences","learningPreferences","adaptivePreferences","accessibilityPreferences"})if(!person.optString(key).trim().isEmpty())a.note(a.pretty(key)+": "+person.optString(key));});}
}
