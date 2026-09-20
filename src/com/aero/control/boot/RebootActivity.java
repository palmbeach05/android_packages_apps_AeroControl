package com.aero.control.boot;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import com.aero.control.R;

/**
 * Activity that displays a dialog notifying the user about a system reboot,
 * typically shown when kernel crash logs are detected. Provides an option
 * to disable future reboot notifications.
 */
public class RebootActivity extends Activity {
    /**
     * Creates and displays the reboot notification dialog when the activity is created.
     *
     * @param savedInstanceState the saved instance state
     */
    @Override // android.app.Activity
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.reboot_screen, (ViewGroup) null);
        TextView aboutText = (TextView) layout.findViewById(R.id.aboutScreen);
        final CheckBox checkBox = (CheckBox) layout.findViewById(R.id.reboot_checkbox);
        checkBox.setText(R.string.dont_show_again);
        builder.setCancelable(false);
        builder.setIcon(R.drawable.warning);
        builder.setTitle(R.string.reboot_notifier_header);
        aboutText.setText(getText(R.string.reboot_notifier));
        aboutText.setTextSize(14.0f);
        builder.setView(layout).setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() { // from class: com.aero.control.boot.RebootActivity.1
            /**
             * Handles dialog dismissal and optionally disables future reboot notifications.
             *
             * @param dialog the dialog interface
             * @param id the button identifier
             */
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                if (checkBox.isChecked()) {
                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(RebootActivity.this.getApplicationContext());
                    pref.edit().putBoolean("reboot_checker", false).apply();
                }
                RebootActivity.this.finish();
            }
        });
        builder.show();
    }
}
