package com.studywithlove.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class WeeklyBackupReceiver extends BroadcastReceiver {
    private static final int REQUEST=947;
    public static void schedule(Context context){
        SharedPreferences p=context.getSharedPreferences("study_progress",Context.MODE_PRIVATE);
        Intent intent=new Intent(context,WeeklyBackupReceiver.class);
        PendingIntent pi=PendingIntent.getBroadcast(context,REQUEST,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if(!p.getBoolean("weekly_backup_enabled",false)||p.getString("weekly_backup_tree_uri","").isEmpty()){am.cancel(pi);pi.cancel();return;}
        Calendar next=Calendar.getInstance(AppTime.zone());next.set(Calendar.DAY_OF_WEEK,Calendar.SUNDAY);next.set(Calendar.HOUR_OF_DAY,20);next.set(Calendar.MINUTE,0);next.set(Calendar.SECOND,0);next.set(Calendar.MILLISECOND,0);if(next.getTimeInMillis()<=System.currentTimeMillis())next.add(Calendar.WEEK_OF_YEAR,1);
        if(Build.VERSION.SDK_INT>=23){if(Build.VERSION.SDK_INT<31||am.canScheduleExactAlarms())am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),pi);else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),pi);}else am.setExact(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),pi);
    }
    @Override public void onReceive(Context context,Intent intent){
        SharedPreferences p=context.getSharedPreferences("study_progress",Context.MODE_PRIVATE);
        if(!p.getBoolean("weekly_backup_enabled",false)){schedule(context);return;}
        String raw=p.getString("weekly_backup_tree_uri","");
        try{
            Uri tree=Uri.parse(raw);String treeId=DocumentsContract.getTreeDocumentId(tree);
            Uri parent=DocumentsContract.buildDocumentUriUsingTree(tree,treeId);
            String stamp=new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm",Locale.US).format(new Date());
            Uri file=DocumentsContract.createDocument(context.getContentResolver(),parent,"application/json","Study_with_Love_backup_"+stamp+".json");
            if(file==null)throw new Exception("Cannot create backup file");
            org.json.JSONObject root=new org.json.JSONObject(),data=new org.json.JSONObject(),photos=new org.json.JSONObject();
            root.put("app","Study with Love");root.put("version",2);
            Map<String,?> all=p.getAll();
            for(Map.Entry<String,?> entry:all.entrySet()){
                String key=entry.getKey();if(key.startsWith("timer_service_")||key.startsWith("topic_photo_"))continue;
                Object value=entry.getValue();
                if(value instanceof Set){org.json.JSONArray arr=new org.json.JSONArray();for(Object item:(Set<?>)value)arr.put(String.valueOf(item));data.put(key,arr);}
                else data.put(key,value);
            }
            Set<String> suffixes=new TreeSet<>();
            for(String key:all.keySet()){if(key.startsWith("topic_photo_file_"))suffixes.add(key.substring(17));else if(key.startsWith("topic_photo_"))suffixes.add(key.substring(12));}
            for(String suffix:suffixes){
                String path=p.getString("topic_photo_file_"+suffix,""),uri=p.getString("topic_photo_"+suffix,"");InputStream in=null;
                try{
                    if(!path.isEmpty())in=new FileInputStream(path);else if(!uri.isEmpty())in=context.getContentResolver().openInputStream(Uri.parse(uri));
                    if(in==null)continue;ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);photos.put(suffix,android.util.Base64.encodeToString(bytes.toByteArray(),android.util.Base64.NO_WRAP));
                }finally{if(in!=null)try{in.close();}catch(Exception ignored){}}
            }
            root.put("data",data);root.put("topic_photos",photos);
            try(OutputStream out=context.getContentResolver().openOutputStream(file,"wt")){if(out==null)throw new Exception("Cannot open backup file");out.write(root.toString(2).getBytes("UTF-8"));out.flush();}
            p.edit().putLong("weekly_backup_last_success",System.currentTimeMillis()).remove("weekly_backup_last_error").apply();
        }catch(Exception e){p.edit().putString("weekly_backup_last_error",e.getMessage()==null?"Ошибка резервной копии":e.getMessage()).apply();}
        schedule(context);
    }
}
