package com.studywithlove.app;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Единый источник состояния Pomodoro. Activity, foreground service и Quick Settings
 * читают один wall-clock deadline из timer_runtime.
 */
public final class FocusTimerController {
    private static final String PREFS = "timer_runtime";
    private static final Object LOCK = new Object();
    private FocusTimerController() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static long endTime(Context context) {
        return prefs(context).getLong("timer_service_end_wall", 0L);
    }

    public static long duration(Context context) { return prefs(context).getLong("timer_service_duration", 0L); }

    public static boolean isBreak(Context context) { return prefs(context).getBoolean("timer_service_break", false); }

    public static long remaining(Context context) {
        return Math.max(0L, endTime(context) - System.currentTimeMillis());
    }

    public static boolean isRunning(Context context) {
        return remaining(context) > 0L;
    }

    /** Returns the absolute end timestamp, or 0 when another segment is already active. */
    public static long startSegment(Context context, long durationMs, boolean isBreak) {
        synchronized (LOCK) {
            SharedPreferences p = prefs(context);
            long current = p.getLong("timer_service_end_wall", 0L);
            if (current > System.currentTimeMillis()) return 0L;
            long duration = Math.max(1000L, durationMs);
            long end = System.currentTimeMillis() + duration;
            p.edit().putLong("timer_service_end_wall", end)
                    .putLong("timer_service_duration", duration)
                    .putBoolean("timer_service_break", isBreak).apply();
            return end;
        }
    }

    public static void clear(Context context) {
        synchronized (LOCK) { clearLocked(prefs(context)); }
    }

    public static void clearIfMatches(Context context, long expectedEnd) {
        synchronized (LOCK) {
            SharedPreferences p=prefs(context);
            if(expectedEnd<=0L || p.getLong("timer_service_end_wall",0L)==expectedEnd) clearLocked(p);
        }
    }

    private static void clearLocked(SharedPreferences p) {
        p.edit().remove("timer_service_end_wall").remove("timer_service_duration")
                .remove("timer_service_break").apply();
    }

    public static long remainingUntil(long endWallTime) {
        return Math.max(0L, endWallTime - System.currentTimeMillis());
    }
}
