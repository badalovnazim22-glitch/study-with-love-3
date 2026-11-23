package com.studywithlove.app;
import java.util.TimeZone;
/** Uses the device's current local time zone consistently across the app. */
public final class AppTime {
    private AppTime(){}
    public static TimeZone zone(){return TimeZone.getDefault();}
}
