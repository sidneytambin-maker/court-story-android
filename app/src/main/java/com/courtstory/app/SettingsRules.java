package com.courtstory.app;
import org.json.JSONObject;
final class SettingsRules {
    static String validate(JSONObject settings){
        int season=settings.optInt("defaultSeason",java.time.Year.now().getValue());
        if(season<2000||season>2100)return "Choose a season from 2000 to 2100.";
        int lead=settings.optInt("reminderLeadMinutes",60),delay=settings.optInt("postSessionDelayMinutes",120);
        if(lead<0||lead>10080)return "Reminder lead time must be between 0 minutes and 7 days (10080 minutes).";
        if(delay<1||delay>10080)return "Reflection delay must be between 1 minute and 7 days (10080 minutes).";
        return null;
    }
}
