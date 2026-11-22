package com.studywithlove.app;

import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.widget.RemoteViews;
import android.app.PendingIntent;
import android.content.Intent;
import java.text.SimpleDateFormat;
import java.util.*;

public class StudyWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids)update(c,m,id);}
    public static void updateAll(Context c){AppWidgetManager m=AppWidgetManager.getInstance(c);int[] ids=m.getAppWidgetIds(new android.content.ComponentName(c,StudyWidgetProvider.class));for(int id:ids)update(c,m,id);}
    private static void update(Context c,AppWidgetManager m,int id){
        android.content.SharedPreferences p=c.getSharedPreferences("study_progress",Context.MODE_PRIVATE);
        RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.study_widget);
        v.setTextViewText(R.id.widget_streak,p.getInt("current_streak",0)+" дней подряд");
        String date=p.getString("exam_date","");String title=p.getString("exam_title","До экзамена");
        try{Set<String> ids=new TreeSet<>(p.getStringSet("exam_goal_ids",new TreeSet<String>()));String x=p.getString("exam_main_goal_id","");if(x.isEmpty()&&!ids.isEmpty())x=ids.iterator().next();if(!x.isEmpty()){date=p.getString("exam_goal_date_"+x,date);title=p.getString("exam_goal_title_"+x,title);}}catch(Exception ignored){}
        String left="Добавь дату экзамена";try{SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.US);f.setTimeZone(TimeZone.getTimeZone("Asia/Baku"));Calendar target=Calendar.getInstance(TimeZone.getTimeZone("Asia/Baku"));target.setTime(f.parse(date));Calendar today=Calendar.getInstance(TimeZone.getTimeZone("Asia/Baku"));today.set(Calendar.HOUR_OF_DAY,0);today.set(Calendar.MINUTE,0);today.set(Calendar.SECOND,0);today.set(Calendar.MILLISECOND,0);target.set(Calendar.HOUR_OF_DAY,0);target.set(Calendar.MINUTE,0);target.set(Calendar.SECOND,0);target.set(Calendar.MILLISECOND,0);long days=(target.getTimeInMillis()-today.getTimeInMillis())/86400000L;left=days>=0?"До "+title+": "+days+" дн.":"Экзамен прошёл";}catch(Exception ignored){}
        v.setTextViewText(R.id.widget_exam,left);
        Intent open=new Intent(c,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(c,930,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);v.setOnClickPendingIntent(R.id.widget_root,pi);m.updateAppWidget(id,v);
    }
}
