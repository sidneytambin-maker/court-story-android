package com.courtstory.app;

import android.view.View;
import android.widget.*;
import org.json.JSONObject;
import java.util.*;
import static com.courtstory.app.Domain.*;

/** A bounded, searchable roster. The same sport context is used for labels and measurements. */
final class CoachRosterUI {
    static final int PAGE_SIZE=25;
    static void show(MainActivity a,boolean progress,String query,int offset){
        String sport=ProfileSetup.sport(a.store.player());
        a.page(progress?"Player progress":"Your players",sport+" • Your coaching roster");
        a.playerScreen(progress?"overview":"roster",null);a.rosterQuery=query;a.rosterOffset=offset;
        if(!progress)a.primary("Add a player",()->{JSONObject p=a.newRecord("players");put(p,"coachProfileID",a.store.player().optString("id"));a.edit("players",p,false);});
        JSONObject filter=obj();put(filter,"query",query);EditText input=a.field(filter,"query","Search name, sport or development goal",false);input.setSingleLine(true);
        TextView count=a.text("",14,false);count.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);a.body.addView(count,a.lp());
        LinearLayout results=a.column();a.body.addView(results);
        List<JSONObject> people=new ArrayList<>();
        for(JSONObject p:rows(a.store.table("players")))if(StoryTimeline.inRoster(p,a.store.player())&&SportProfiles.plays(p,sport)&&!ProfileSetup.coach(SportProfiles.forSport(p,sport)))people.add(p);
        people.sort(Comparator.comparing(p->playerName(p).toLowerCase(Locale.ROOT)));
        Map<String,CoachInsights.Metrics> metrics=CoachMetrics.roster(a.store,people,30,sport);
        Runnable[] render={null};render[0]=()->{
            String term=input.getText().toString().trim().toLowerCase(Locale.ROOT);List<JSONObject> found=new ArrayList<>();
            for(JSONObject p:people){JSONObject scoped=SportProfiles.forSport(p,sport);String text=playerName(p)+" "+sport+(TrackingMode.guided(a.store)?" "+scoped.optString("primaryGoal"):"");if(text.toLowerCase(Locale.ROOT).contains(term))found.add(p);}
            a.rosterQuery=input.getText().toString();a.rosterOffset=found.isEmpty()?0:Math.min(Math.max(0,a.rosterOffset),(found.size()-1)/PAGE_SIZE*PAGE_SIZE);
            final String savedQuery=a.rosterQuery;final int savedOffset=a.rosterOffset;
            a.current=()->show(a,progress,savedQuery,savedOffset);
            results.removeAllViews();LinearLayout host=a.body;a.body=results;
            if(found.isEmpty())count.setText(people.isEmpty()?"Add your first player to build their court story.":"No players match this search.");
            else{
                int end=Math.min(found.size(),a.rosterOffset+PAGE_SIZE);count.setText("Showing "+(a.rosterOffset+1)+"–"+end+" of "+found.size()+" players");
                for(int i=a.rosterOffset;i<end;i++){JSONObject p=found.get(i),scoped=SportProfiles.forSport(p,sport);CoachInsights.Metrics m=metrics.get(p.optString("id"));a.card(playerName(p),sport+" • Last 30 days\n"+m.summary(TrackingMode.power(a.store))+(TrackingMode.guided(a.store)?"\nGoal: "+scoped.optString("primaryGoal","Not set"):""),()->a.go(()->CoachInsights.player(a,p,30)));}
                if(a.rosterOffset>0)a.button("Previous "+PAGE_SIZE+" players",()->{a.rosterOffset-=PAGE_SIZE;render[0].run();a.scroll.smoothScrollTo(0,0);});
                if(end<found.size())a.button("Next "+Math.min(PAGE_SIZE,found.size()-end)+" players",()->{a.rosterOffset=end;render[0].run();a.scroll.smoothScrollTo(0,0);});
            }
            a.body=host;
        };
        render[0].run();input.addTextChangedListener(a.watcher(s->{a.rosterOffset=0;render[0].run();}));
        if(progress){a.button("Coaching achievements",()->Achievements.show(a));a.button("Manage players",()->CoachInsights.roster(a));a.button("My personal progress",a::reports);}
        a.navigation(progress?"Progress":"Players");
    }
}
