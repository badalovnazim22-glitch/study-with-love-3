package com.studywithlove.app;

import android.app.*;
import android.content.*;
import android.os.*;
import android.media.RingtoneManager;
import android.net.Uri;

public class FocusTimerService extends Service {
    public static final String ACTION_START="focus_start", ACTION_STOP="focus_stop";
    private static final String CHANNEL="focus_timer";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private long endAt;
    private Runnable tick;
    private NotificationManager nm;
    @Override public void onCreate(){super.onCreate();nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Таймер Study with Love",NotificationManager.IMPORTANCE_LOW));}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null||ACTION_STOP.equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
        long duration=Math.max(1000,intent.getLongExtra("duration_ms",60000));
        boolean rest=intent.getBooleanExtra("break",false);
        endAt=SystemClock.elapsedRealtime()+duration;
        startForeground(920,build("Таймер работает","Осталось "+format(duration),false));
        if(tick!=null)handler.removeCallbacks(tick);
        tick=new Runnable(){public void run(){long left=endAt-SystemClock.elapsedRealtime();if(left<=0){finish(rest);return;}nm.notify(920,build("Таймер работает","Осталось "+format(left),false));handler.postDelayed(this,1000);}};
        handler.post(tick);return START_STICKY;
    }
    private void finish(boolean rest){
        handler.removeCallbacks(tick);
        try{((android.os.Vibrator)getSystemService(VIBRATOR_SERVICE)).vibrate(Build.VERSION.SDK_INT>=26?VibrationEffect.createWaveform(new long[]{0,350,180,550},-1):new long[]{0,350,180,550},-1);}catch(Exception ignored){}
        Uri sound=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(rest?"Перерыв окончен":"Фокус-сессия завершена")
          .setContentText(rest?"Можно возвращаться к занятиям":"Отличная работа! Запиши маленькую победу в дневник.")
          .setAutoCancel(true).setSound(sound).setVibrate(new long[]{0,350,180,550})
          .setContentIntent(PendingIntent.getActivity(this,921,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        nm.notify(921,b.build());stopForeground(true);stopSelf();
    }
    private Notification build(String title,String body,boolean alert){Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);return b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(title).setContentText(body).setOngoing(true).setOnlyAlertOnce(true).setContentIntent(PendingIntent.getActivity(this,922,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)).build();}
    private String format(long ms){long s=Math.max(0,ms/1000);return String.format(java.util.Locale.US,"%02d:%02d",s/60,s%60);}
    @Override public void onDestroy(){if(tick!=null)handler.removeCallbacks(tick);super.onDestroy();}
    @Override public android.os.IBinder onBind(Intent i){return null;}
}
