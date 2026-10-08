package com.courtstory.app;
import java.time.*;
/** Positive values are rolling days; negative values identify a calendar season. */
final class ReportPeriod {
    static int previousWeek(long notificationTime){LocalDate monday=Instant.ofEpochMilli(notificationTime).atZone(ZoneId.systemDefault()).toLocalDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);return 1000000+(int)monday.toEpochDay();}
    static LocalDate weekDate(int value){return LocalDate.ofEpochDay(value-1000000L);}
    static String label(int days){return days>=1000000?"Week of "+weekDate(days).format(java.time.format.DateTimeFormatter.ofPattern("d MMM uuuu")):days<0?"Season "+(-days):days==0?"All time":days+" days";}
    static long start(int days){return days>=1000000?weekDate(days).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli():days<0?LocalDate.of(-days,1,1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli():days==0?0:System.currentTimeMillis()-days*86400000L;}
    static long end(int days){return days>=1000000?weekDate(days).plusWeeks(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli():days<0?LocalDate.of(-days+1,1,1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli():Long.MAX_VALUE;}
}
