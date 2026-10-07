package com.courtstory.app;

import android.app.AlertDialog;
import android.widget.*;
import org.json.*;
import java.util.*;
import static com.courtstory.app.Domain.*;

/** One person, several sports, and a reversible current workspace mode. */
final class SportProfiles {
    static List<String> sports(JSONObject p){LinkedHashSet<String> values=new LinkedHashSet<>();if(!ProfileSetup.sport(p).trim().isEmpty())values.add(ProfileSetup.sport(p));JSONArray saved=p.optJSONArray("sports");if(saved!=null)for(int i=0;i<saved.length();i++)if(!saved.optString(i).trim().isEmpty()&&!saved.optString(i).equals("Custom"))values.add(saved.optString(i));return new ArrayList<>(values);}
    static JSONObject forSport(JSONObject person,String sport){JSONObject view=copy(person);select(view,sport);return view;}
    static boolean plays(JSONObject p,String sport){return sports(p).contains(sport);}
    static final String[] SPORT_PREFERENCES={"defaultMatchFormat","preferredMatchType","preferredSurface","bounceAllowance","sportClassification","wheelchairDivision","ruleAdaptations","primaryGoal","coachingFocus","developmentNotes","nextReviewDate","playingStyle"};
    static void select(JSONObject p,String sport){
        sport=sport.trim();for(String known:ProfileSetup.SPORTS)if(known.equalsIgnoreCase(sport)){sport=known;break;}for(String existing:sports(p))if(existing.equalsIgnoreCase(sport)){sport=existing;break;}
        String previous=ProfileSetup.sport(p);
        if(!previous.equals(sport)){
            JSONObject preferences=object(p,"sportPreferences"),saved=obj();
            for(String key:SPORT_PREFERENCES)if(p.has(key))put(saved,key,p.opt(key));
            put(preferences,previous,saved);if(!object(p,"sportModes").has(previous))put(object(p,"sportModes"),previous,p.optString("role","Player"));
            JSONObject destination=preferences.optJSONObject(sport);
            for(String key:SPORT_PREFERENCES){p.remove(key);if(destination!=null&&destination.has(key))put(p,key,destination.opt(key));}
        }
        JSONArray all=new JSONArray(sports(p));if(sport.equals("Custom")){put(p,"sports",all);put(p,"sport","Custom");put(p,"customSport","");return;}boolean exists=false;for(int i=0;i<all.length();i++)exists|=all.optString(i).equals(sport);if(!exists)all.put(sport);put(p,"sports",all);if(Arrays.asList(ProfileSetup.SPORTS).contains(sport)&&!sport.equals("Custom")){put(p,"sport",sport);}else{put(p,"sport","Custom");put(p,"customSport",sport);}JSONObject modes=p.optJSONObject("sportModes");if(modes!=null&&modes.has(sport))put(p,"role",modes.optString(sport));}
    static void controls(MainActivity a){JSONObject p=a.store.player();if(p==null)return;LinearLayout host=a.body,row=new LinearLayout(a);boolean stacked=a.getResources().getConfiguration().fontScale>=1.5f;row.setOrientation(stacked?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);host.addView(row,a.lp());a.body=row;Button sport=a.button("Sport: "+ProfileSetup.sport(p),()->choose(a));Button mode=a.button("Role: "+(ProfileSetup.coach(p)?"Coach":"Player"),()->mode(a));for(Button button:new Button[]{sport,mode}){button.setTextSize(13);LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(stacked?-1:0,-2,stacked?0:1);layout.setMargins(a.dp(2),a.dp(2),a.dp(2),a.dp(2));button.setLayoutParams(layout);}a.body=host;}    static void choose(MainActivity a){JSONObject p=a.store.player();if(p==null){Onboarding.start(a);return;}List<String> sports=sports(p);sports.add("Add another sport");new AlertDialog.Builder(a).setTitle("Your sports").setSingleChoiceItems(sports.toArray(new String[0]),0,(d,w)->{d.dismiss();if(w==sports.size()-1){add(a);return;}JSONObject next=copy(p);select(next,sports.get(w));if(a.saveRecord("players",next)){a.home();a.announce(sports.get(w)+" selected");}}).setNegativeButton("Cancel",null).show();}
    static void add(MainActivity a){new AlertDialog.Builder(a).setTitle("Add a racket sport").setItems(ProfileSetup.SPORTS,(d,w)->{String sport=ProfileSetup.SPORTS[w];if(sport.equals("Custom")){EditText input=new EditText(a);input.setHint("Court sport name");input.setContentDescription("Court sport name");input.setMinHeight(a.dp(48));AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Your court sport").setView(input).setPositiveButton("Add",null).setNegativeButton("Cancel",null).create();dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String name=input.getText().toString().trim();if(name.isEmpty()||name.equalsIgnoreCase("Custom")){input.setError("Enter the name of your court sport");return;}addAndSelect(a,name);dialog.dismiss();}));dialog.show();}else addAndSelect(a,sport);}).setNegativeButton("Cancel",null).show();}
    static void addAndSelect(MainActivity a,String sport){JSONObject p=copy(a.store.player());select(p,sport);if(a.saveRecord("players",p)){a.home();a.announce(sport+" added. Your other sports and records are retained.");}}
    static void mode(MainActivity a){JSONObject p=a.store.player();new AlertDialog.Builder(a).setTitle("Choose your mode for "+ProfileSetup.sport(p)).setSingleChoiceItems(new String[]{"Player — my own performance","Coach — my players and my own game"},ProfileSetup.coach(p)?1:0,(d,w)->{JSONObject next=copy(p);put(next,"role",w==0?"Player":"Coach");put(object(next,"sportModes"),ProfileSetup.sport(next),next.optString("role"));d.dismiss();if(a.saveRecord("players",next)){a.home();a.announce(w==0?"Player mode":"Coach mode");}}).setNegativeButton("Cancel",null).show();}
}
