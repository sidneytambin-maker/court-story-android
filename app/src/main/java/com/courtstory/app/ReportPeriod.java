package com.courtstory.app;
import java.time.*;
/** Positive values are rolling days; negative values identify a calendar season. */
final class ReportPeriod {
    static String label(int days){return days<0?"Season "+(-days):days==0?"All time":days+" days";}
    static long start(int days){return days<0?LocalDate.of(-days,1,1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli():days==0?0:System.currentTimeMillis()-days*86400000L;}
    static long end(int days){return days<0?LocalDate.of(-days+1,1,1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli():Long.MAX_VALUE;}
}
