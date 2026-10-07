package com.courtstory.app;

import android.widget.*;
import org.json.JSONObject;
import static com.courtstory.app.Domain.*;

/** A resumable, native three-step setup. All choices remain editable afterward. */
final class Onboarding {
    static void start(MainActivity a){a.welcome();}
    static void start(MainActivity a,String role){JSONObject p=a.newRecord("players");put(p,"role",role);put(p,"trackingMode","Standard");a.editorReturn=a::welcome;show(a,p,0);}
    static void show(MainActivity a,JSONObject p,int step){
        a.current=()->show(a,p,step);a.page(new String[]{"Choose your sport",ProfileSetup.coach(p)?"Your coaching profile":"Your player profile",ProfileSetup.coach(p)?"Shape your coaching":"Shape your game"}[step],(ProfileSetup.coach(p)?"Coach":"Player")+" setup • Step "+(step+1)+" of 3");
        a.draft=p;a.draftTable="onboarding";a.setupStep=step;a.editing=true;
        if(step==0){
            a.note("Choose your first racket sport. Add more sports and switch between Player and Coach modes at any time in Settings.");
            RadioGroup group=new RadioGroup(a);for(String sport:ProfileSetup.SPORTS){RadioButton option=new RadioButton(a);option.setText(sport);option.setTextColor(a.ink);option.setTextSize(17);option.setMinHeight(a.dp(48));option.setMinimumHeight(a.dp(48));option.setPadding(a.dp(10),a.dp(8),a.dp(10),a.dp(8));option.setId(android.view.View.generateViewId());group.addView(option);if(p.optString("sport","Tennis").equals(sport))option.setChecked(true);option.setOnCheckedChangeListener((button,checked)->{if(checked){if(!p.optString("sport").equals(sport))for(String key:SportProfiles.SPORT_PREFERENCES)p.remove(key);put(p,"sport",sport);}});}a.body.addView(group,a.lp());a.field(p,"customSport","Custom court sport name (only for Custom)",false);
        }else if(step==1){
            a.field(p,"name","Name",false);a.field(p,"preferredName","Preferred name (optional)",false);
            if(ProfileSetup.coach(p)){a.choice(p,"coachLevel","Coaching level",split("Assistant coach|Club coach|Qualified coach|Senior coach|Performance coach|High-performance coach|Head coach|Independent coach|Custom"));a.field(p,"customCoachLevel","Other coaching level (optional)",false);a.field(p,"qualifications","Qualifications and awarding bodies",false);a.field(p,"coachingOrganisation","Club or coaching organisation",false);a.field(p,"coachingExperienceYears","Years of coaching experience",true);a.choice(p,"coachingAudience","Players you coach",split("All levels|Beginners|Club competitors|Performance athletes|Children and young people|Adults|Inclusive and adaptive sport|Custom"));a.optional("My access preferences (optional)",()->PlayerCategory.controls(a,p,false));}
            else PlayerCategory.controls(a,p,false);
        }else{
            a.hero(ProfileSetup.sport(p),ProfileSetup.coach(p)?"Your coaching workspace is ready to build around your players.":"A personal record of your time on court, from the first point onward.",()->{});
            // This is a summary, not a dead action: expose the panel as readable text.
            android.view.View panel=a.body.getChildAt(a.body.getChildCount()-1);panel.setClickable(false);panel.setFocusable(false);panel.setAccessibilityDelegate(new android.view.View.AccessibilityDelegate(){@Override public void onInitializeAccessibilityNodeInfo(android.view.View host,android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName(TextView.class.getName());}});
            a.button("Detail level: "+p.optString("trackingMode","Basic"),()->new android.app.AlertDialog.Builder(a).setTitle("Detail level").setSingleChoiceItems(split("Basic|Standard|Power"),java.util.Arrays.asList("Basic","Standard","Power").indexOf(p.optString("trackingMode","Basic")),(dialog,index)->{put(p,"trackingMode",split("Basic|Standard|Power")[index]);dialog.dismiss();show(a,p,step);}).setNegativeButton("Cancel",null).show());a.note(TrackingMode.description());
            boolean guided=!TrackingMode.normalise(p.optString("trackingMode","Basic")).equals("Basic");
            if(ProfileSetup.coach(p)){a.field(p,"coachingSpecialisms","Coaching specialisms",false);if(guided)a.field(p,"coachingFocus","Current coaching priorities",false);a.field(p,"qualificationRenewal","Qualification renewal notes (optional)",false);a.field(p,"safeguardingTraining","Safeguarding training (optional)",false);a.optional("My access preferences (optional)",()->PlayerCategory.controls(a,p,true));}
            else {PlayerCategory.controls(a,p,true);a.field(p,"club","Club (optional)",false);if(guided)a.field(p,"primaryGoal","What would you like to achieve?",false);}
            if(!CustomScore.custom(ProfileSetup.sport(p)))a.choice(p,"preferredMatchType","Usual match type",split("Singles|Doubles"));
            if(!CustomScore.custom(ProfileSetup.sport(p))){if(!p.has("defaultMatchFormat"))put(p,"defaultMatchFormat",ProfileSetup.sport(p).equals("Tennis")?"1 set":"Best of 3");if(!SportRules.racketPoints(p))a.choice(p,"defaultMatchFormat","Usual match format",split("1 set|Best of 3|Best of 5|Custom"));}
            a.note("Your sport, role and preferences stay editable in Settings.");
            a.note(TermsPolicy.SAFETY);a.button("Read terms and safe use",()->TermsPolicy.showSetup(a,p,step));
            a.toggle(p,"androidTermsAgreed","I agree to the terms and safe-use guidance",false);
        }
        a.primary(step==2?"Enter my court":"Continue",()->{
            if(step==0&&p.optString("sport").equals("Custom")&&p.optString("customSport").trim().isEmpty()){a.error("Please name your court sport, or choose one from the list.");return;}
            if(step==1&&p.optString("name").trim().isEmpty()){a.error("Please enter your name.");return;}
            if(step<2){show(a,p,step+1);return;}
            if(!TermsPolicy.accepted(p)){a.error("Please review and agree to the terms and safe-use guidance before entering your court.");return;}put(p,"acceptedTermsVersion",TermsPolicy.VERSION);put(p,"termsAcceptedAt",now());
            put(p,"playerMode",p.optString("playerType").equals("Visually impaired")?"Blind or visually impaired tennis":"Standard tennis");JSONObject completed=copy(p);completed.remove("androidTermsAgreed");a.commit("players",completed);if(find(a.store.table("players"),p.optString("id"))!=null){put(a.store.data,"selectedPlayerID",p.optString("id"));if(a.save())a.home();}
        });
        if(step>0)a.button("Back",()->show(a,p,step-1));else a.button("Back to welcome",()->{a.editing=false;a.draft=null;a.welcome();});
    }
}
