package com.aero.control.boot;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.aero.control.helpers.settingsHelper;

/* JADX INFO: loaded from: classes.dex */
public class bootService extends Service {
    private static final settingsHelper settings = new settingsHelper();

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int flags, int startId) {
        settings.setSettings(getBaseContext(), null, true);
        return 2;
    }
}
