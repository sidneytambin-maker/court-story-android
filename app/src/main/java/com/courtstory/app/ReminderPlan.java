package com.courtstory.app;

import org.json.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import static com.courtstory.app.Domain.*;

/** Reminder eligibility is checked again at delivery, so edited/deleted records cannot send stale alerts. */
final class ReminderPlan {
    static final class Item {
        final String key,text,table,id;final long when;
        Item(String key,String text,String table,String id,long when){this.key=key;this.text=text;this.table=table;this.id=id;this.when=when;}
    }
    static long nextWeek(ZonedDateTime now){ZonedDateTime next=now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).withHour(9).withMinute(0).withSecond(0).withNano(0);if(!next.isAfter(now))next=next.plusWeeks(1);return next.toInstant().toEpochMilli();}
    static List<Item> records(JSONObject library){
        JSONObject settings=object(library,"settings");List<Item> result=new ArrayList<>();
        long lead=Math.max(0,Math.min(10080,settings.optInt("reminderLeadMinutes",60)))*60000L;
        long delay=Math.max(1,Math.min(10080,settings.optInt("postSessionDelayMinutes",120)))*60000L;
        for(String table:new String[]{"matches","trainingSessions","tournaments"})for(JSONObject record:rows(table(library,table))){
            String id=record.optString("id"),state=status(table,record),toggle=table.equals("matches")?"matchRemindersEnabled":table.equals("trainingSessions")?"trainingRemindersEnabled":"tournamentRemindersEnabled";
            if((table.equals("tournaments")||record.optBoolean("hasStartTime",true))&&settings.optBoolean(toggle)&&(state.equals("Scheduled")||state.equals("Entered")))result.add(new Item(table+":"+id,title(table,record),table,id,millis(record,"date")-lead));
            if(table.equals("trainingSessions")&&settings.optBoolean("postSessionRemindersEnabled")&&record.optBoolean("hasStartTime",true)&&!active(record)&&!state.equals("Cancelled")&&record.optString("notes").trim().isEmpty()&&record.optString("sessionOutcome").trim().isEmpty()){
                long finish=millis(record,"actualFinish");if(finish==0)finish=millis(record,"date")+duration(record)*60000L;
                result.add(new Item("reflect:"+id,"How did your training go?",table,id,finish+delay));
            }
            if(table.equals("matches")&&record.optBoolean("hasStartTime",true)&&settings.optBoolean("matchResultRemindersEnabled")&&!state.equals("Withdrawn")&&(record.optString("result").isEmpty()||record.optString("result").equals("Not recorded"))){
                long finish=millis(record,"actualFinish");if(finish==0)finish=(millis(record,"actualStart")>0?millis(record,"actualStart"):millis(record,"date"))+Math.max(1,record.optBoolean("hasExpectedDuration",record.has("expectedDurationMinutes"))?record.optInt("expectedDurationMinutes",120):120)*60000L;
                result.add(new Item("result:"+id,"Record your match result",table,id,finish+delay));
            }
        }
        return result;
    }
    static Item eligible(JSONObject library,String key,long scheduled,long now){
        if(key==null||scheduled>now||scheduled<=0)return null;
        if(key.equals("weekly"))return object(library,"settings").optBoolean("weeklySummaryEnabled")?new Item(key,"Your court week","reports","",scheduled):null;
        for(Item item:records(library))if(item.key.equals(key)&&item.when==scheduled)return item;
        return null;
    }
    static List<Item> upcoming(JSONObject library,long now){List<Item> queue=new ArrayList<>();for(Item item:records(library))if(item.when>now)queue.add(item);queue.sort(Comparator.comparingLong(item->item.when));return new ArrayList<>(queue.subList(0,Math.min(128,queue.size())));}
}
