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

/**
 * Utility class providing helper methods for formatting time strings, retrieving
 * usage statistics, and displaying random loading messages.
 */
public class Util {
    /**
     * Returns a random loading message to display to the user during long operations.
     *
     * @param context the context to access string resources
     * @return a random loading text from the available options
     */
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

    /**
     * Formats a time duration in milliseconds to a human-readable string.
     *
     * @param milliseconds the duration in milliseconds
     * @return formatted string in the format "HH h MM min SS secs", "MM min SS secs", or "SS secs"
     */
    public static String getFormatedTimeString(long milliseconds) {
        if (milliseconds < 60000) {
            return String.format("%02d secs", Long.valueOf(TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds))));
        }
        if (milliseconds < 3600000) {
            return String.format("%02d min %02d secs", Long.valueOf(TimeUnit.MILLISECONDS.toMinutes(milliseconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds))), Long.valueOf(TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds))));
        }
        return String.format("%02d h %02d min %02d secs", Long.valueOf(TimeUnit.MILLISECONDS.toHours(milliseconds)), Long.valueOf(TimeUnit.MILLISECONDS.toMinutes(milliseconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds))), Long.valueOf(TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds))));
    }

    /**
     * Extracts the last component from a sysfs path string.
     *
     * @param syspath the full sysfs path (e.g., "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
     * @return the last path component (e.g., "scaling_governor"), or null if input is null
     */
    public static String getLastSysValue(String syspath) {
        if (syspath == null) {
            return null;
        }
        String[] values = syspath.split("/");
        return values[values.length - 1];
    }

    /**
     * Retrieves usage statistics for the past year from the Android UsageStatsManager.
     * Requires API level 21 or higher.
     *
     * @param context the context to access the UsageStatsManager
     * @return a list of usage statistics for all apps
     */
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

    /**
     * Retrieves the UsageStatsManager system service.
     *
     * @param context the context to access system services
     * @return the UsageStatsManager instance
     */
    @TargetApi(21)
    private static UsageStatsManager getUsageStatsManager(Context context) {
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService("usagestats");
        return usm;
    }

    /**
     * Displays a dialog prompting the user to grant usage access permission if it
     * hasn't been granted yet. Only shown on Android Lollipop (API 21) and higher
     * when usage statistics are unavailable.
     *
     * @param context the context to display the dialog and launch settings
     */
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
