package com.studywithlove.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) { MainActivity.scheduleReminders(context); MainActivity.rescheduleFutureLetterReminders(context); WeeklyReportReceiver.schedule(context); StudyWidgetProvider.updateAll(context); android.content.SharedPreferences p=context.getSharedPreferences("study_progress",Context.MODE_PRIVATE); if(p.getLong("timer_service_end_wall",0)>0){Intent timer=new Intent(context,FocusTimerService.class);if(android.os.Build.VERSION.SDK_INT>=26)context.startForegroundService(timer);else context.startService(timer);} }
    }
}
