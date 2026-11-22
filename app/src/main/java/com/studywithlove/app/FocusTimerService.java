package com.studywithlove.app;

import android.app.*;
import android.content.*;
import android.os.*;
import android.media.RingtoneManager;
import android.net.Uri;

public class FocusTimerService extends Service {
    public static final String ACTION_START="focus_start", ACTION_STOP="focus_stop", ACTION_PAUSE="focus_pause";
    private static final String CHANNEL="focus_timer", FINISH_CHANNEL="focus_timer_finish";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private long endAt, segmentDuration;
    private boolean currentBreak;
    private Runnable tick;
    private NotificationManager nm;
    private static final String PREFS="study_progress";
    @Override public void onCreate(){super.onCreate();nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(Build.VERSION.SDK_INT>=26){nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Таймер Study with Love",NotificationManager.IMPORTANCE_LOW));nm.createNotificationChannel(new NotificationChannel(FINISH_CHANNEL,"Окончание фокус-сессии",NotificationManager.IMPORTANCE_HIGH));}}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null&&ACTION_STOP.equals(intent.getAction())){clearSavedTimer();stopForeground(true);stopSelf();return START_NOT_STICKY;}
        if(intent!=null&&ACTION_PAUSE.equals(intent.getAction())){if(!intent.getBooleanExtra("break",false))recordFocus(intent.getLongExtra("elapsed_ms",0));clearSavedTimer();stopForeground(true);stopSelf();return START_NOT_STICKY;}
        if(intent==null){
            android.content.SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);
            long wallEnd=p.getLong("timer_service_end_wall",0);
            if(wallEnd<=0){stopSelf();return START_NOT_STICKY;}
            currentBreak=p.getBoolean("timer_service_break",false);
            segmentDuration=p.getLong("timer_service_duration",60000);
            long remaining=wallEnd-System.currentTimeMillis();
            endAt=SystemClock.elapsedRealtime()+Math.max(0,remaining);
        }else{
            segmentDuration=Math.max(1000,intent.getLongExtra("duration_ms",60000));
            currentBreak=intent.getBooleanExtra("break",false);
            endAt=SystemClock.elapsedRealtime()+segmentDuration;
            getSharedPreferences(PREFS,MODE_PRIVATE).edit()
                    .putLong("timer_service_end_wall",System.currentTimeMillis()+segmentDuration)
                    .putLong("timer_service_duration",segmentDuration)
                    .putBoolean("timer_service_break",currentBreak).apply();
        }
        startForeground(920,build("Таймер работает","Осталось "+format(Math.max(0,endAt-SystemClock.elapsedRealtime())),false));
        if(tick!=null)handler.removeCallbacks(tick);
        tick=new Runnable(){public void run(){long left=endAt-SystemClock.elapsedRealtime();if(left<=0){finish(currentBreak);return;}nm.notify(920,build("Таймер работает","Осталось "+format(left),false));handler.postDelayed(this,1000);}};
        if(endAt<=SystemClock.elapsedRealtime())finish(currentBreak);else handler.post(tick);
        return START_STICKY;
    }
    private void finish(boolean rest){
        handler.removeCallbacks(tick);
        if(!rest)recordFocus(segmentDuration);
        clearSavedTimer();
        try{((android.os.Vibrator)getSystemService(VIBRATOR_SERVICE)).vibrate(Build.VERSION.SDK_INT>=26?VibrationEffect.createWaveform(new long[]{0,350,180,550},-1):new long[]{0,350,180,550},-1);}catch(Exception ignored){}
        Uri sound=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,FINISH_CHANNEL):new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(rest?"Перерыв окончен":"Фокус-сессия завершена")
          .setContentText(rest?"Можно возвращаться к занятиям":"Отличная работа! Запиши маленькую победу в дневник.")
          .setAutoCancel(true).setSound(sound).setVibrate(new long[]{0,350,180,550})
          .setContentIntent(PendingIntent.getActivity(this,921,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        nm.notify(921,b.build());stopForeground(true);stopSelf();
    }
    private void clearSavedTimer(){getSharedPreferences(PREFS,MODE_PRIVATE).edit().remove("timer_service_end_wall").remove("timer_service_duration").remove("timer_service_break").apply();}
    private void recordFocus(long ms){try{android.content.SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);java.text.SimpleDateFormat fmt=new java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US);fmt.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Baku"));String date=fmt.format(new java.util.Date());long total=p.getLong("focus_remainder_ms_"+date,0)+Math.max(0,ms);int add=(int)(total/60000L);p.edit().putLong("focus_remainder_ms_"+date,total%60000L).putInt("focus_minutes_"+date,p.getInt("focus_minutes_"+date,0)+add).putInt("focus_total_minutes",p.getInt("focus_total_minutes",0)+add).apply();StudyWidgetProvider.updateAll(this);}catch(Exception ignored){}}
    private Notification build(String title,String body,boolean alert){Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);return b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(title).setContentText(body).setOngoing(true).setOnlyAlertOnce(true).setContentIntent(PendingIntent.getActivity(this,922,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)).build();}
    private String format(long ms){long s=Math.max(0,ms/1000);return String.format(java.util.Locale.US,"%02d:%02d",s/60,s%60);}
    @Override public void onDestroy(){if(tick!=null)handler.removeCallbacks(tick);super.onDestroy();}
    @Override public android.os.IBinder onBind(Intent i){return null;}
}
