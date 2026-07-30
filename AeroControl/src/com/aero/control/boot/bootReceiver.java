package com.aero.control.boot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.aero.control.service.PerAppServiceHelper;
import java.io.File;

/* JADX INFO: loaded from: classes.dex */
public class bootReceiver extends BroadcastReceiver {
    private static final String LAST_KMSG = "/proc/last_kmsg";

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
