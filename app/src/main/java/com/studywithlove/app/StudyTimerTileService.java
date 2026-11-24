package com.studywithlove.app;

import android.content.Intent;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

@android.annotation.TargetApi(24)
public class StudyTimerTileService extends TileService {
    private final android.os.Handler tileHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable tileTicker;
    private long remaining(){return FocusTimerController.remaining(this);}
    @Override public void onStartListening(){
        super.onStartListening();
        updateTileLabel();
        if(tileTicker!=null)tileHandler.removeCallbacks(tileTicker);
        tileTicker=new Runnable(){@Override public void run(){updateTileLabel();if(remaining()>0)tileHandler.postDelayed(this,1000L);}};
        tileHandler.postDelayed(tileTicker,1000L);
    }
    @Override public void onStopListening(){
        if(tileTicker!=null)tileHandler.removeCallbacks(tileTicker);
        tileTicker=null;
        super.onStopListening();
    }
    private void updateTileLabel(){
        Tile tile=getQsTile();if(tile==null)return;
        long left=remaining();
        if(left>0){tile.setLabel("Фокус · "+format(left));tile.setState(Tile.STATE_ACTIVE);}
        else{int minutes=Math.max(1,getSharedPreferences("study_progress",MODE_PRIVATE).getInt("selected_study_minutes",25));tile.setLabel("Фокус · "+minutes+" мин");tile.setState(Tile.STATE_INACTIVE);}
        tile.updateTile();
    }
    @SuppressLint("StartActivityAndCollapseDeprecated")
    @Override public void onClick(){
        super.onClick();
        try{
            long left=remaining();
            if(left<=0){
                SharedPreferences prefs=getSharedPreferences("study_progress",MODE_PRIVATE);
                int minutes=Math.max(1,Math.min(600,prefs.getInt("selected_study_minutes",25)));
                Intent service=new Intent(this,FocusTimerService.class).setAction(FocusTimerService.ACTION_START)
                        .putExtra("duration_ms",minutes*60000L).putExtra("break",false);
                if(Build.VERSION.SDK_INT>=26)startForegroundService(service);else startService(service);
            }
            Intent open=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            open.putExtra("open_pomodoro_from_tile",true);
            if(Build.VERSION.SDK_INT>=34)startActivityAndCollapse(android.app.PendingIntent.getActivity(this,925,open,android.app.PendingIntent.FLAG_UPDATE_CURRENT|android.app.PendingIntent.FLAG_IMMUTABLE));
            else startActivityAndCollapse(open);
            Tile tile=getQsTile();
            if(tile!=null){tile.setState(Tile.STATE_ACTIVE);tile.setLabel("Фокус · "+format(left>0?left:getSharedPreferences("study_progress",MODE_PRIVATE).getInt("selected_study_minutes",25)*60000L));tile.updateTile();}
        }catch(Exception ignored){}
    }
    private String format(long ms){long sec=Math.max(0,ms/1000);return String.format(java.util.Locale.US,"%02d:%02d",sec/60,sec%60);}
}
