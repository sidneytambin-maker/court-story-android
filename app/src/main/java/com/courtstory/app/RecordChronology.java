package com.courtstory.app;
import org.json.JSONObject;
import java.time.*;
import static com.courtstory.app.Domain.*;
/** Sort by visible date and minute, never a hidden time on an untimed record. */
final class RecordChronology {
    static int compare(JSONObject a,JSONObject b){ZoneId zone=ZoneId.systemDefault();ZonedDateTime first=Instant.ofEpochMilli(millis(a,"date")).atZone(zone),second=Instant.ofEpochMilli(millis(b,"date")).atZone(zone);int day=first.toLocalDate().compareTo(second.toLocalDate());if(day!=0)return day;boolean at=a.optBoolean("hasStartTime"),bt=b.optBoolean("hasStartTime");if(at!=bt)return at?-1:1;if(at){int minute=Integer.compare(first.getHour()*60+first.getMinute(),second.getHour()*60+second.getMinute());if(minute!=0)return minute;}return a.optString("id").compareTo(b.optString("id"));}
}
