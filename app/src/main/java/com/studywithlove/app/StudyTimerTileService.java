package com.studywithlove.app;

import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

@android.annotation.TargetApi(24)
public class StudyTimerTileService extends TileService {
    @Override public void onStartListening(){super.onStartListening();Tile t=getQsTile();if(t!=null){t.setLabel("Фокус 25 мин");t.setState(Tile.STATE_INACTIVE);t.updateTile();}}
    @Override public void onClick(){super.onClick();try{Intent i=new Intent(this,FocusTimerService.class).setAction(FocusTimerService.ACTION_START).putExtra("duration_ms",25L*60L*1000L).putExtra("break",false);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);Tile t=getQsTile();if(t!=null){t.setState(Tile.STATE_ACTIVE);t.updateTile();}}catch(Exception ignored){}}
}