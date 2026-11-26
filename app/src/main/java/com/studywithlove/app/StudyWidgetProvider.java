package com.studywithlove.app;

import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.widget.RemoteViews;
import android.app.PendingIntent;
import android.content.Intent;
import java.text.SimpleDateFormat;
import java.util.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class StudyWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids)update(c,m,id);}
    @Override public void onReceive(Context c,Intent intent){super.onReceive(c,intent);if(Intent.ACTION_DATE_CHANGED.equals(intent.getAction())||Intent.ACTION_TIME_CHANGED.equals(intent.getAction())||Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction()))updateAll(c);}
    public static void updateAll(Context c){AppWidgetManager m=AppWidgetManager.getInstance(c);int[] ids=m.getAppWidgetIds(new android.content.ComponentName(c,StudyWidgetProvider.class));for(int id:ids)update(c,m,id);}
    private static int streak(android.content.SharedPreferences p){
        String last=p.getString("last_study_date","");
        if(last.isEmpty())return 0;
        try{LocalDate today=LocalDate.now(java.time.ZoneId.of(AppTime.zone().getID()));long gap=ChronoUnit.DAYS.between(LocalDate.parse(last),today);if(gap==0||gap==1||(gap==2&&!weekKey().equals(p.getString("streak_freeze_week",""))))return p.getInt("current_streak",0);}catch(Exception ignored){}return 0;
    }
    private static String dayWord(int n,String lang){if("en".equals(lang))return n==1?"day":"days";if("az".equals(lang))return "gün";int x=Math.abs(n)%10,y=Math.abs(n)%100;if(x==1&&y!=11)return "день";if(x>=2&&x<=4&&(y<12||y>14))return "дня";return "дней";}
    private static String tr(String lang,String ru,String en,String az){return "en".equals(lang)?en:("az".equals(lang)?az:ru);}
    private static String weekKey(){Calendar c=Calendar.getInstance(AppTime.zone());c.setFirstDayOfWeek(Calendar.MONDAY);c.setMinimalDaysInFirstWeek(4);int week=c.get(Calendar.WEEK_OF_YEAR),year=c.get(Calendar.YEAR),month=c.get(Calendar.MONTH);if(week==1&&month==Calendar.DECEMBER)year++;else if(week>=52&&month==Calendar.JANUARY)year--;return year+"-"+week;}
    private static void update(Context c,AppWidgetManager m,int id){
        android.content.SharedPreferences p=c.getSharedPreferences("study_progress",Context.MODE_PRIVATE);
        RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.study_widget);
        String lang=p.getString("app_language","ru");
        v.setTextViewText(R.id.widget_streak,streak(p)+" "+dayWord(streak(p),lang)+("en".equals(lang)?" in a row":"az".equals(lang)?" ardıcıl":" подряд"));
        String date=p.getString("exam_date","");String title=p.getString("exam_title",tr(lang,"До экзамена","Until exam","İmtahana qədər"));
        try{Set<String> ids=new TreeSet<>(p.getStringSet("exam_goal_ids",new TreeSet<String>()));String x=p.getString("exam_main_goal_id","");if(x.isEmpty()&&!ids.isEmpty())x=ids.iterator().next();if(!x.isEmpty()){date=p.getString("exam_goal_date_"+x,date);title=p.getString("exam_goal_title_"+x,title);}}catch(Exception ignored){}
        String left=tr(lang,"Добавь дату экзамена","Add an exam date","İmtahan tarixi əlavə et");try{long days=ChronoUnit.DAYS.between(LocalDate.now(AppTime.zone().toZoneId()),LocalDate.parse(date));left=days>=0?tr(lang,"До ","Until ","")+" "+title+": "+days+" "+tr(lang,"дн.","days","gün"):tr(lang,"Экзамен прошёл","Exam has passed","İmtahan keçib");}catch(Exception ignored){}
        v.setTextViewText(R.id.widget_exam,left);
        Intent open=new Intent(c,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(c,930,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);v.setOnClickPendingIntent(R.id.widget_root,pi);Intent focus=new Intent(c,MainActivity.class).setAction("com.studywithlove.app.SHORTCUT_FOCUS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);PendingIntent focusPi=PendingIntent.getActivity(c,931,focus,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);v.setOnClickPendingIntent(R.id.widget_focus,focusPi);m.updateAppWidget(id,v);
    }
}
