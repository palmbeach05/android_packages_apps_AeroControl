package com.aero.control.boot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.aero.control.service.PerAppServiceHelper;
import java.io.File;

/**
 * Broadcast receiver that handles the BOOT_COMPLETED system event to restore
 * application settings after device reboot. Starts {@link bootService} to apply
 * saved CPU, GPU, and system settings, optionally displays the reboot analysis
 * activity if enabled, and starts the per-app monitoring service if configured.
 */
public class bootReceiver extends BroadcastReceiver {
    private static final String LAST_KMSG = "/proc/last_kmsg";

    /**
     * Called when the device completes booting. Starts the boot service to restore
     * settings, checks for kernel crash logs to display if reboot analysis is enabled,
     * and starts the per-app monitoring service if previously enabled by the user.
     *
     * @param context the context in which the receiver is running
     * @param intent the intent being received (typically BOOT_COMPLETED)
     */
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Intent i = new Intent(context, (Class<?>) bootService.class);
        context.startService(i);
        File last_kmsg = new File(LAST_KMSG);
        Boolean rebootChecker = Boolean.valueOf(prefs.getBoolean("reboot_checker", false));
        if (rebootChecker.booleanValue() && last_kmsg.exists()) {
            Intent trIntent = new Intent("android.intent.action.BOOT");
            trIntent.setClass(context, RebootActivity.class);
            trIntent.setFlags(268435456);
            context.startActivity(trIntent);
        }
        PerAppServiceHelper perAppService = new PerAppServiceHelper(context);
        if (perAppService.shouldBeStarted()) {
            perAppService.startService();
        }
    }
}
