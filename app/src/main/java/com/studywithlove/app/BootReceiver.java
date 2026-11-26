package com.studywithlove.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        AppUpdater.scheduleAutomaticChecks(context);
        MainActivity.scheduleReminders(context);
        MainActivity.rescheduleFutureLetterReminders(context);
        WeeklyReportReceiver.schedule(context);
        WeeklyBackupReceiver.schedule(context);
        StudyWidgetProvider.updateAll(context);

        SharedPreferences legacy = context.getSharedPreferences("study_progress", Context.MODE_PRIVATE);
        SharedPreferences runtime = context.getSharedPreferences("timer_runtime", Context.MODE_PRIVATE);

        // Migrate timer state saved by older app versions before trying to resume it.
        if (!runtime.contains("timer_service_end_wall") && legacy.contains("timer_service_end_wall")) {
            runtime.edit()
                    .putLong("timer_service_end_wall", legacy.getLong("timer_service_end_wall", 0))
                    .putLong("timer_service_duration", legacy.getLong("timer_service_duration", 60000))
                    .putBoolean("timer_service_break", legacy.getBoolean("timer_service_break", false))
                    .apply();
            legacy.edit()
                    .remove("timer_service_end_wall")
                    .remove("timer_service_duration")
                    .remove("timer_service_break")
                    .apply();
        }

        if (runtime.getLong("timer_service_end_wall", 0) > 0) {
            Intent timer = new Intent(context, FocusTimerService.class);
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(timer);
            } else {
                context.startService(timer);
            }
        }
    }
}
