package com.aero.control.boot;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.aero.control.helpers.settingsHelper;

/**
 * Background service that applies saved user settings when the device boots.
 * This service is started by {@link bootReceiver} after a system reboot to
 * restore CPU, GPU, memory and other system configuration parameters.
 */
/* JADX INFO: loaded from: classes.dex */
public class bootService extends Service {
    private static final settingsHelper settings = new settingsHelper();

    /**
     * Returns null because this service does not support binding.
     *
     * @param intent the intent used to bind to this service
     * @return always null
     */
    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Applies all saved settings from shared preferences when the service starts.
     *
     * @param intent the intent that started this service
     * @param flags additional data about this start request
     * @param startId a unique integer representing this specific request to start
     * @return START_NOT_STICKY to indicate the service should not be restarted if killed
     */
    @Override // android.app.Service
    public int onStartCommand(Intent intent, int flags, int startId) {
        settings.setSettings(getBaseContext(), null, true);
        return 2;
    }
}
