package com.studywithlove.app;

import android.app.*;
import android.content.*;
import android.os.*;
import android.media.RingtoneManager;
import android.net.Uri;

public class FocusTimerService extends Service {
    public static final String ACTION_START="focus_start", ACTION_STOP="focus_stop", ACTION_PAUSE="focus_pause";
    private static final String CHANNEL="focus_timer", FINISH_CHANNEL="focus_timer_finish_v2";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private long endAt, segmentDuration;
    private boolean currentBreak;
    private Runnable tick;
    private NotificationManager nm;
    private static final String PREFS="study_progress";
    @Override public void onCreate(){super.onCreate();nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(Build.VERSION.SDK_INT>=26){nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Таймер Study with Love",NotificationManager.IMPORTANCE_LOW));android.content.SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);boolean vibrate=p.getBoolean("haptics_enabled",true);for(boolean vibrate:new boolean[]{true,false}){String finishId=FINISH_CHANNEL+(vibrate?"_v":"_silent");NotificationChannel finish=new NotificationChannel(finishId,"Окончание фокус-сессии",NotificationManager.IMPORTANCE_HIGH);finish.setSound(null,null);finish.enableVibration(vibrate);nm.createNotificationChannel(finish);}}}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null&&ACTION_STOP.equals(intent.getAction())){if(!intent.getBooleanExtra("break",false))recordFocus(intent.getLongExtra("elapsed_ms",0),dateKey(System.currentTimeMillis()));clearSavedTimer();stopForeground(true);stopSelf();return START_NOT_STICKY;}
        if(intent!=null&&ACTION_PAUSE.equals(intent.getAction())){if(!intent.getBooleanExtra("break",false))recordFocus(intent.getLongExtra("elapsed_ms",0),dateKey(System.currentTimeMillis()));clearSavedTimer();stopForeground(true);stopSelf();return START_NOT_STICKY;}
        if(intent==null){
            android.content.SharedPreferences p=getSharedPreferences("timer_runtime",MODE_PRIVATE);
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
            getSharedPreferences("timer_runtime",MODE_PRIVATE).edit()
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
        if(!rest){long finishWall=getSharedPreferences("timer_runtime",MODE_PRIVATE).getLong("timer_service_end_wall",System.currentTimeMillis());String finishDate=dateKey(finishWall);recordFocus(segmentDuration,finishDate);recordStudyDay(finishDate);android.content.SharedPreferences.Editor reward=getSharedPreferences(PREFS,MODE_PRIVATE).edit();reward.putInt("postcard_focus_count",getSharedPreferences(PREFS,MODE_PRIVATE).getInt("postcard_focus_count",0)+1).putInt("flower_petals",getSharedPreferences(PREFS,MODE_PRIVATE).getInt("flower_petals",0)+1).apply();StudyWidgetProvider.updateAll(this);if(!MainActivity.activityVisible)getSharedPreferences(PREFS,MODE_PRIVATE).edit().putBoolean("diary_prefill_timer_win",true).putBoolean("timer_finish_prompt_pending",true).apply();}
        clearSavedTimer();
        android.content.SharedPreferences settings=getSharedPreferences(PREFS,MODE_PRIVATE);if(settings.getBoolean("haptics_enabled",true)&&!MainActivity.activityVisible){try{android.os.Vibrator vibrator=(android.os.Vibrator)getSystemService(VIBRATOR_SERVICE);if(Build.VERSION.SDK_INT>=26)vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0,350,180,550},-1));else vibrator.vibrate(new long[]{0,350,180,550},-1);}catch(Exception ignored){}}
        Uri sound=null;
        String finishChannel=FINISH_CHANNEL+(settings.getBoolean("haptics_enabled",true)?"_v":"_silent");Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,finishChannel):new Notification.Builder(this);
        b.setSmallIcon(R.drawable.ic_notification).setContentTitle(rest?"Перерыв окончен":"Фокус-сессия завершена")
          .setContentText(rest?"Можно возвращаться к занятиям":"Отличная работа! Запиши маленькую победу в дневник.")
          .setAutoCancel(true).setVibrate(settings.getBoolean("haptics_enabled",true)?new long[]{0,350,180,550}:new long[]{0})
          .setContentIntent(PendingIntent.getActivity(this,921,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        if(settings.getBoolean("timer_notification_enabled",true)&&!MainActivity.activityVisible)nm.notify(921,b.build());
        if(settings.getBoolean("timer_sound_enabled",true)&&!MainActivity.activityVisible){try{android.media.ToneGenerator tone=new android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM,settings.getInt("completion_sound_volume",55));tone.startTone(android.media.ToneGenerator.TONE_PROP_BEEP,650);new Handler(Looper.getMainLooper()).postDelayed(tone::release,850);}catch(Exception ignored){}}
        stopForeground(true);stopSelf();
    }
    private void clearSavedTimer(){getSharedPreferences("timer_runtime",MODE_PRIVATE).edit().remove("timer_service_end_wall").remove("timer_service_duration").remove("timer_service_break").apply();}
    private String dateKey(long wall){java.text.SimpleDateFormat fmt=new java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US);fmt.setTimeZone(AppTime.zone());return fmt.format(new java.util.Date(wall));}
    private void recordFocus(long ms,String date){try{android.content.SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);long total=p.getLong("focus_remainder_ms_"+date,0)+Math.max(0,ms);int add=(int)(total/60000L);p.edit().putLong("focus_remainder_ms_"+date,total%60000L).putInt("focus_minutes_"+date,p.getInt("focus_minutes_"+date,0)+add).putInt("focus_total_minutes",p.getInt("focus_total_minutes",0)+add).apply();StudyWidgetProvider.updateAll(this);}catch(Exception ignored){}}
    private void recordStudyDay(String date){
        android.content.SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);String last=p.getString("last_study_date","");java.util.Set<String> days=new java.util.TreeSet<>(p.getStringSet("study_days",new java.util.TreeSet<String>()));days.add(date);
        if(!date.equals(last)){int streak=p.getInt("current_streak",0);String yesterday=shiftDate(date,-1),twoDaysAgo=shiftDate(date,-2);
            if(last.equals(yesterday))streak++;else if(last.equals(twoDaysAgo)&&!weekKey().equals(p.getString("streak_freeze_week",""))){streak++;p.edit().putString("streak_freeze_week",weekKey()).putString("streak_freeze_used_date",date).apply();}else streak=1;
            p.edit().putInt("current_streak",streak).putInt("longest_streak",Math.max(streak,p.getInt("longest_streak",0))).putString("last_study_date",date).putStringSet("study_days",days).apply();
        }else p.edit().putStringSet("study_days",days).apply();
        StudyWidgetProvider.updateAll(this);
    }
    private String shiftDate(String date,int days){try{java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US);f.setTimeZone(AppTime.zone());java.util.Calendar c=java.util.Calendar.getInstance(AppTime.zone());c.setTime(f.parse(date));c.add(java.util.Calendar.DAY_OF_YEAR,days);return f.format(c.getTime());}catch(Exception e){return "";}}
    private String weekKey(){java.util.Calendar c=java.util.Calendar.getInstance(AppTime.zone());c.setFirstDayOfWeek(java.util.Calendar.MONDAY);c.setMinimalDaysInFirstWeek(4);int week=c.get(java.util.Calendar.WEEK_OF_YEAR),year=c.get(java.util.Calendar.YEAR),month=c.get(java.util.Calendar.MONTH);if(week==1&&month==java.util.Calendar.DECEMBER)year++;else if(week>=52&&month==java.util.Calendar.JANUARY)year--;return year+"-"+week;}
    private Notification build(String title,String body,boolean alert){Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);return b.setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(body).setOngoing(true).setOnlyAlertOnce(true).setContentIntent(PendingIntent.getActivity(this,922,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)).build();}
    private String format(long ms){long s=Math.max(0,ms/1000);return String.format(java.util.Locale.US,"%02d:%02d",s/60,s%60);}
    @Override public void onDestroy(){if(tick!=null)handler.removeCallbacks(tick);super.onDestroy();}
    @Override public android.os.IBinder onBind(Intent i){return null;}
}
