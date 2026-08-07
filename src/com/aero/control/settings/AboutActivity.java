package com.aero.control.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aero.control.R;
import com.aero.control.helpers.ThemeHelper;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setTitle(R.string.about);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        TextView appName = (TextView) findViewById(R.id.about_app_name);
        TextView versionText = (TextView) findViewById(R.id.about_version);
        TextView aboutText = (TextView) findViewById(R.id.about_text);

        appName.setText(R.string.app_name);
        aboutText.setText(R.string.about_dialog);

        try {
            PackageManager pm = getPackageManager();
            String versionName = pm.getPackageInfo(getPackageName(), 0).versionName;
            int versionCode = pm.getPackageInfo(getPackageName(), 0).versionCode;
            versionText.setText("Version: " + versionName + "\nBuild: " + versionCode);
        } catch (PackageManager.NameNotFoundException e) {
        }

        findViewById(R.id.about_xda).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("http://forum.xda-developers.com/showthread.php?t=2483827");
                startActivity(new Intent("android.intent.action.VIEW", uri));
            }
        });

        findViewById(R.id.about_github).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("https://github.com/Blechd0se/android_packages_apps_AeroControl");
                startActivity(new Intent("android.intent.action.VIEW", uri));
            }
        });

        findViewById(R.id.about_legal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLegalDialog();
            }
        });

        findViewById(R.id.about_donate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=46VQEKBETN36U");
                startActivity(new Intent("android.intent.action.VIEW", uri));
            }
        });
    }

    private void showLegalDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.about_screen, (ViewGroup) null);
        TextView legalText = (TextView) layout.findViewById(R.id.aboutScreen);
        builder.setTitle(R.string.legal);
        builder.setIcon(getResources().getDrawable(R.drawable.email));
        legalText.setText(getText(R.string.legal_dialog));
        legalText.setTextSize(13.0f);
        builder.setView(layout).setPositiveButton(R.string.send_email, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent emailIntent = new Intent("android.intent.action.SENDTO", Uri.fromParts("mailto", "alex.christ@hotmail.de", null));
                try {
                    emailIntent.putExtra("android.intent.extra.SUBJECT", "[AeroControl] Got something for you (" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName + ")");
                } catch (PackageManager.NameNotFoundException e) {
                    emailIntent.putExtra("android.intent.extra.SUBJECT", "[AeroControl] Got something for you");
                }
                startActivity(Intent.createChooser(emailIntent, getText(R.string.send_email)));
            }
        });
        builder.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}