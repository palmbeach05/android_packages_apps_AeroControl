package com.aero.control.helpers;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import com.aero.control.R;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/* JADX INFO: loaded from: classes.dex */
public class Util {
    public static CharSequence getRandomLoadingText(Context context) {
        ArrayList<Integer> randomData = new ArrayList<>();
        randomData.add(Integer.valueOf(R.string.random_programming_flux));
        randomData.add(Integer.valueOf(R.string.random_nsa_loading));
        randomData.add(Integer.valueOf(R.string.random_data_somewhere));
        randomData.add(Integer.valueOf(R.string.random_shovelling_coal));
        randomData.add(Integer.valueOf(R.string.random_testing_patience));
        randomData.add(Integer.valueOf(R.string.random_prepare_awesomeness));
        randomData.add(Integer.valueOf(R.string.random_working_you_know));
        return context.getText(randomData.get(new Random().nextInt(randomData.size())).intValue());
    }

    public static String getFormatedTimeString(long milliseconds) {
        if (milliseconds < 60000) {
            return String.format("%02d secs", Long.valueOf(TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds))));
        }
        if (milliseconds < 3600000) {
            return String.format("%02d min %02d secs", Long.valueOf(TimeUnit.MILLISECONDS.toMinutes(milliseconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds))), Long.valueOf(TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds))));
        }
        return String.format("%02d h %02d min %02d secs", Long.valueOf(TimeUnit.MILLISECONDS.toHours(milliseconds)), Long.valueOf(TimeUnit.MILLISECONDS.toMinutes(milliseconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds))), Long.valueOf(TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds))));
    }

    public static String getLastSysValue(String syspath) {
        if (syspath == null) {
            return null;
        }
        String[] values = syspath.split("/");
        return values[values.length - 1];
    }

    @TargetApi(21)
    public static List<UsageStats> getUsageStatsList(Context context) {
        UsageStatsManager usm = getUsageStatsManager(context);
        Calendar calendar = Calendar.getInstance();
        long endTime = calendar.getTimeInMillis();
        calendar.add(1, -1);
        long startTime = calendar.getTimeInMillis();
        List<UsageStats> usageStatsList = usm.queryUsageStats(0, startTime, endTime);
        return usageStatsList;
    }

    @TargetApi(21)
    private static UsageStatsManager getUsageStatsManager(Context context) {
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService("usagestats");
        return usm;
    }

    public static void showUsageStatDialog(final Context context) {
        if (Build.VERSION.SDK_INT > 19 && getUsageStatsList(context).isEmpty()) {
            AlertDialog dialog = new AlertDialog.Builder(context).setTitle(R.string.warning).setIcon(R.drawable.warning).setMessage(R.string.pref_lollipop_usage_warning).setCancelable(false).setPositiveButton(R.string.aero_continue, new DialogInterface.OnClickListener() { // from class: com.aero.control.helpers.Util.1
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialog2, int which) {
                    Intent intent = new Intent("android.settings.USAGE_ACCESS_SETTINGS");
                    context.startActivity(intent);
                }
            }).create();
            dialog.show();
        }
    }
}
