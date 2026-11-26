package com.studywithlove.app;

import android.app.NotificationChannel;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String CHANNEL = "study_reminders";
    private static String tr(String lang,String ru,String en,String az){return "en".equals(lang)?en:("az".equals(lang)?az:ru);}
    @Override public void onReceive(Context context, Intent intent) {
        String message = intent.getStringExtra("message");
        String action=intent.getAction()==null?"":intent.getAction();
        boolean studyReminder=action.startsWith("study_reminder_");
        int notificationId=intent.getIntExtra("id",1);
        android.content.SharedPreferences prefs=context.getSharedPreferences("study_progress",Context.MODE_PRIVATE);
        boolean retry=intent.getBooleanExtra("repeat_retry",false);
        long scheduledAt=intent.getLongExtra("scheduled_at",System.currentTimeMillis());
        if(retry&&prefs.getLong("reminder_opened_at_"+notificationId,0L)>=scheduledAt)return;
        if(studyReminder&&!retry){if(MainActivity.activityVisible)prefs.edit().putLong("reminder_opened_at_"+notificationId,System.currentTimeMillis()).apply();else prefs.edit().putInt("pending_reminder_id",notificationId).putLong("pending_reminder_at",scheduledAt).apply();}
        if(studyReminder&&!retry){
            int repeat=prefs.getInt("reminder_repeat_minutes",0);
            if(repeat>0){Intent again=new Intent(context,ReminderReceiver.class).setAction("study_reminder_retry_"+notificationId).putExtra("id",notificationId).putExtra("message",message).putExtra("repeat_retry",true).putExtra("scheduled_at",scheduledAt);PendingIntent retryPi=PendingIntent.getBroadcast(context,notificationId+200000,again,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);AlarmManager alarms=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);long at=System.currentTimeMillis()+repeat*60000L;if(Build.VERSION.SDK_INT>=23)alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,retryPi);else alarms.set(AlarmManager.RTC_WAKEUP,at,retryPi);}
            String tone=prefs.getString("reminder_tone","soft");String lang=prefs.getString("app_language","ru");message="strict".equals(tone)?tr(lang,"Пора открыть приложение и выполнить запланированное занятие.","Open the app and complete your planned study session.","Açıq tətbiqi və planlaşdırdığın dərs məşğələsini yerinə yetir."): "neutral".equals(tone)?tr(lang,"Напоминание об учёбе: можно начать запланированное занятие.","Study reminder: you can start your planned session.","Dərs xatırlatması: planlaşdırdığın məşğələyə başlaya bilərsən."):tr(lang,"Небольшое напоминание 🌸 Если удобно, удели время учёбе.","A gentle reminder 🌸 If it works for you, spend a little time studying.","Kiçik xatırlatma 🌸 Uyğundursa, bir az dərsə vaxt ayır.");
        }
        if(MainActivity.showForegroundReminder(message)) { MainActivity.scheduleReminders(context); return; }
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(new NotificationChannel(CHANNEL, tr(prefs.getString("app_language","ru"),"Напоминания об учёбе","Study reminders","Dərs xatırlatmaları"), NotificationManager.IMPORTANCE_DEFAULT));
        }
        Intent open = new Intent(context, MainActivity.class);if(studyReminder)open.putExtra("opened_reminder_id",notificationId);
        PendingIntent pi = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new android.app.Notification.Builder(context, CHANNEL)
                : new android.app.Notification.Builder(context);
        builder.setSmallIcon(R.drawable.ic_notification).setContentTitle("Study with Love 🌸")
                .setContentText(message == null ? tr(prefs.getString("app_language","ru"),"Пора немного позаниматься!","Time for a little study session!","Bir az dərs vaxtıdır!") : message)
                .setAutoCancel(true).setContentIntent(pi);
        manager.notify(notificationId, builder.build());
        MainActivity.scheduleReminders(context);
    }
}
