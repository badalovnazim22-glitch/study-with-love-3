package com.studywithlove.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UpdateCheckReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        PendingResult pendingResult=goAsync();
        AppUpdater.checkAndNotify(context.getApplicationContext(),pendingResult::finish);
    }
}
