package com.courtstory.app;
import org.json.JSONObject;
import java.time.*;
import static com.courtstory.app.Domain.*;

/** Android calendar all-day dates are UTC midnight; timed events retain their real instant. */
final class CalendarPlan {
    final long start,end;final boolean allDay;
    CalendarPlan(long start,long end,boolean allDay){this.start=start;this.end=end;this.allDay=allDay;}
    static CalendarPlan make(String table,JSONObject record,ZoneId zone){
        long date=millis(record,"date");boolean tournament=table.equals("tournaments");boolean allDay=tournament?record.optBoolean("isAllDay",true)||!record.optBoolean("hasStartTime"):!record.optBoolean("hasStartTime");
        if(allDay){LocalDate first=Instant.ofEpochMilli(date).atZone(zone).toLocalDate();LocalDate last=tournament?Instant.ofEpochMilli(Math.max(date,millis(record,"endDate"))).atZone(zone).toLocalDate():first;return new CalendarPlan(first.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),last.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),true);}
        int minutes=table.equals("trainingSessions")?duration(record):record.optBoolean("hasExpectedDuration",record.has("expectedDurationMinutes"))?record.optInt("expectedDurationMinutes",60):60;
        long end=tournament?Math.max(millis(record,"endDate"),date+3600000L):date+Math.max(1,minutes)*60000L;
        return new CalendarPlan(date,end,false);
    }
}
