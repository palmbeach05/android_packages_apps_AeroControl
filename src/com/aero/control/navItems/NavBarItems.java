package com.aero.control.navItems;

import android.content.Context;
import android.os.Build;
import com.aero.control.R;
import java.util.ArrayList;

/**
 * Builds and manages the list of navigation drawer items displayed in the main activity.
 * Dynamically includes device-specific items based on the device model.
 */
public class NavBarItems {
    public ArrayList<PreferenceItem> ITEMS = new ArrayList<>();
    private Context mContext;

    /**
     * Creates a new navigation bar items builder.
     *
     * @param context the context used to access resources and device information
     */
    public NavBarItems(Context context) {
        this.mContext = context;
        listItems();
    }

    /**
     * Populates the navigation drawer with items. Includes device-specific items for
     * certain Motorola models.
     */
    public void listItems() {
        addItem(new PreferenceItem(R.string.slider_overview, R.drawable.overview));
        addItem(new PreferenceItem(R.string.slider_cpu_settings, R.drawable.cpu));
        addItem(new PreferenceItem(R.string.slider_statistics, R.drawable.clock));
        addItem(new PreferenceItem(R.string.slider_gpu_settings, R.drawable.gpu));
        addItem(new PreferenceItem(R.string.slider_memory_settings, R.drawable.memory));
        addItem(new PreferenceItem(R.string.slider_misc_settings, R.drawable.mixer));
        if (Build.MODEL.equals("MB525") || Build.MODEL.equals("MB526") || Build.MODEL.equals("DROIDX")) {
            addItem(new PreferenceItem(R.string.slider_defy_parts, R.drawable.gear));
        }
        addItem(new PreferenceItem(R.string.slider_backup_restore, R.drawable.update));
        addItem(new PreferenceItem(R.string.slider_profile, R.drawable.profile));
        addItem(new PreferenceItem(R.string.slider_app_monitor, R.drawable.appmonitor));
        addItem(new PreferenceItem(R.string.slider_test_suite_settings, R.drawable.dashboard));
        addItem(new PreferenceItem(R.string.aero_settings, R.drawable.ic_action_settings));
    }

    /**
     * Represents a single navigation drawer item with a label and icon.
     */
    public static class PreferenceItem {
        public int content;
        public int drawable;

        /**
         * Creates a navigation drawer item.
         *
         * @param content the string resource ID for the item label
         * @param drawable the drawable resource ID for the item icon
         */
        public PreferenceItem(int content, int drawable) {
            this.content = content;
            this.drawable = drawable;
        }
    }

    private void addItem(PreferenceItem item) {
        this.ITEMS.add(item);
    }
}
