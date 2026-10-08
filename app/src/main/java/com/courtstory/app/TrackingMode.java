package com.courtstory.app;
import org.json.JSONObject;
import java.util.*;
import static com.courtstory.app.Domain.*;
/** Presentation tiers are cumulative; lowering a tier never changes stored activity data. */
final class TrackingMode {
    static String normalise(String value){return value.equals("Power")||value.equals("Standard")?value:"Basic";}
    static String current(Store store){JSONObject p=store.player();return normalise(p!=null&&p.has("trackingMode")?p.optString("trackingMode"):store.settings().optString("trackingMode","Basic"));}
    static boolean guided(Store store){return !current(store).equals("Basic");}
    static boolean power(Store store){return current(store).equals("Power");}
    static final Set<String> GUIDED_SECTIONS=new HashSet<>(Arrays.asList("Links and conditions","Reflection","Focus and reflection","Coaching plan","Goals and results","Goals and reflection","Linked practice matches","More profile details"));
    static final Set<String> POWER_SECTIONS=new HashSet<>(Arrays.asList("Advanced match statistics","Wellness"));
    static boolean section(Store store,String name){return (!GUIDED_SECTIONS.contains(name)||guided(store))&&(!POWER_SECTIONS.contains(name)||power(store));}
    static final Set<String> GUIDED_FIELDS=new HashSet<>(Arrays.asList("goal","primaryGoal","preferredSurface","developmentNotes","nextReviewDate","coachingFocus","sessionObjectives","plannedDrills","equipmentPlan","adaptations","successMeasures","coachReview","opponentStyle","pressureMoment","matchStory","nextPracticeFocus","matchStrengths","matchNeedsWork","notes","focus","additionalFocus","surface","courtSurface","effortLevel","confidenceLevel","sessionOutcome","matchConditions","expectedDurationMinutes","coachingSessionGoals"));
    static final Set<String> POWER_FIELDS=new HashSet<>(Arrays.asList("aces","doubleFaults","winners","unforcedErrors","energyLevel","painLevel","effortLevel","confidenceLevel"));
    static boolean field(Store store,String key){return (!GUIDED_FIELDS.contains(key)||guided(store))&&(!POWER_FIELDS.contains(key)||power(store));}
    static String description(){return "Basic: core records, live scoring and results. Standard: adds goals, plans, reflections and media feedback. Power: adds statistics, wellness, trends and measured drills. Changing mode keeps all saved data.";}
}
