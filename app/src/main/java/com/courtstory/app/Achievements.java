package com.courtstory.app;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.util.*;
import static com.courtstory.app.Domain.*;
public final class Achievements {
    static void preview(MainActivity a){Map<String,Integer> totals=counts(a);a.heading("Milestones");a.note("All-time progress for this profile and sport.");for(String[] entry:new String[][]{{"match","1","Game On"},{"match","10","Ten Match Moments"},{"training","1","First Steps on Court"},{"training","10","Ten Sessions Strong"}}){int count=totals.getOrDefault(entry[0],0),target=Integer.parseInt(entry[1]);a.note((count>=target?"Earned":"Progress: "+count+" of "+target)+" — "+entry[2]);}}
    static void bump(Map<String,Integer> m,String k){m.put(k,m.getOrDefault(k,0)+1);}
    static void result(Map<String,Integer> m,String kind,String result){bump(m,"match");String k=kind.equals("Doubles")?"doubles":"singles";bump(m,k);bump(m,k+result);}
    public static Map<String,Integer> counts(MainActivity a){Map<String,Integer> count=new HashMap<>();Set<String> linked=new HashSet<>();for(JSONObject r:a.mine("matches"))if(r.has("trainingSessionID"))linked.add(r.optString("trainingSessionID"));for(JSONObject r:a.mine("matches"))if(status("matches",r).equals("Completed")&&millis(r,"date")<=System.currentTimeMillis())result(count,CustomScore.custom(r)?(r.optString("customKind","Teams").equals("Players")?"Doubles":"Singles"):r.optString("matchType"),r.optString("result"));for(JSONObject r:a.mine("trainingSessions"))if(status("trainingSessions",r).equals("Completed")&&millis(r,"date")<=System.currentTimeMillis()){bump(count,"training");if(!r.optString("focus").trim().isEmpty()&&!r.optString("focus").equals("Not specified"))bump(count,"focus");if(!r.optString("notes").trim().isEmpty()||!r.optString("sessionOutcome").trim().isEmpty())bump(count,"reflection");if(r.optBoolean("trackedOnWatch")||r.optJSONObject("workout")!=null)bump(count,"watchTraining");JSONObject practice=r.optJSONObject("practiceResult");if(practice!=null&&!linked.contains(r.optString("id")))result(count,practice.optString("kind"),practice.optString("result"));}for(JSONObject r:a.mine("tournaments"))if(!r.optString("name").trim().isEmpty()){bump(count,"tournament");if(status("tournaments",r).equals("Completed"))bump(count,"completedTournament");}CoachingAchievements.add(a.store,count);return count;}
    static String[] labels(String[] original,String sport){
        String[] parts=original.clone();for(int i=1;i<=3;i++)parts[i]=parts[i].replace("tennis",sport.toLowerCase(Locale.UK));
        if(CustomScore.custom(sport)){
            for(int i=1;i<=3;i++)parts[i]=parts[i].replace("singles","team-event").replace("Singles","Team").replace("doubles","individual-event");
            if(parts[0].equals("singles.10"))parts[1]="Team Court Story";
            if(parts[0].equals("doubles.10"))parts[1]="Individual Court Story";
            if(parts[0].equals("doublesWin.1")){parts[1]="Individual Breakthrough";parts[3]="Your first recorded individual-event win. A milestone in your court story.";}
        }
        return parts;
    }
    static List<String[]> catalog(MainActivity a) throws IOException {
        List<String[]> entries=new ArrayList<>();try(BufferedReader in=new BufferedReader(new InputStreamReader(a.getAssets().open("achievements.tsv"),java.nio.charset.StandardCharsets.UTF_8))){String line;while((line=in.readLine())!=null){String[] parts=line.split("\t");if(parts.length!=6)throw new IOException("Invalid achievement catalog");entries.add(parts);}}return entries;
    }
    static Set<String> earned(MainActivity a) throws IOException {Set<String> ids=new HashSet<>();if(a.store.player()==null)return ids;Map<String,Integer> totals=counts(a);for(String[] entry:catalog(a))if(visible(a,entry)&&totals.getOrDefault(entry[4],0)>=Integer.parseInt(entry[5]))ids.add(entry[0]);return ids;}
    static boolean visible(MainActivity a,String[] entry){return (!entry[4].startsWith("coached")||ProfileSetup.coach(a.store.player()))&&(TrackingMode.guided(a.store)||!entry[4].equals("focus")&&!entry[4].equals("reflection"));}
    static String collectionLabel(MainActivity a){try{int count=0;for(String[] entry:catalog(a))if(visible(a,entry))count++;return "All "+count+" milestones";}catch(IOException e){return "Milestones";}}
    static void badgeCard(MainActivity a,String[] entry,int index,Map<String,Integer> counts){
        int current=counts.getOrDefault(entry[4],0),target=Integer.parseInt(entry[5]);boolean earned=current>=target;
        boolean watch=a.getPackageManager().hasSystemFeature("android.hardware.type.watch");LinearLayout row=new LinearLayout(a);if(watch)row.setOrientation(LinearLayout.VERTICAL);row.setGravity(android.view.Gravity.TOP);row.setPadding(a.dp(14),a.dp(14),a.dp(14),a.dp(14));row.setBackground(a.bg(a.card,18));
        LinearLayout.LayoutParams badgeSize=new LinearLayout.LayoutParams(a.dp(watch?48:64),a.dp(watch?48:64));badgeSize.setMarginEnd(a.dp(14));row.addView(new AchievementBadge(a,index,earned),badgeSize);
        LinearLayout words=a.column();row.addView(words,watch?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-2,1));
        TextView title=a.text(entry[1],17,true);title.setAccessibilityHeading(true);words.addView(title);
        TextView state=a.text(earned?"Earned. "+entry[3]:current+" of "+target+". "+entry[2],15,false);state.setTextColor(a.ink);words.addView(state);
        ProgressBar bar=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(target);bar.setProgress(Math.min(current,target));bar.setProgressTintList(android.content.res.ColorStateList.valueOf(a.ink));bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(a.paper));bar.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);words.addView(bar,new LinearLayout.LayoutParams(-1,a.dp(6)));
        a.body.addView(row,a.lp());
    }
    public static void show(MainActivity a){a.current=()->show(a);String sport=ProfileSetup.sport(a.store.player());a.page("Milestones",sport+" • Your collection");Map<String,Integer> counts=counts(a);
        try{List<String[]> all=catalog(a);int earned=0,total=0;for(String[] entry:all)if(visible(a,entry)){total++;if(counts.getOrDefault(entry[4],0)>=Integer.parseInt(entry[5]))earned++;}
            a.heading(earned+" of "+total+" earned");a.note("All-time achievements for this profile and sport. Progress updates from your saved records.");
            if(ProfileSetup.coach(a.store.player())){a.heading("Your coaching story");a.note("Completed sessions you record for other players count here. Your own practice stays in your playing collection.");for(int i=0;i<all.size();i++)if(all.get(i)[4].startsWith("coached"))badgeCard(a,labels(all.get(i),sport),i,counts);}
            a.heading("Your playing story");for(int i=0;i<all.size();i++)if(!all.get(i)[4].startsWith("coached")&&visible(a,all.get(i)))badgeCard(a,labels(all.get(i),sport),i,counts);
        }catch(IOException e){a.error("Could not load milestones.");}a.navigation("Progress");
    }
}
