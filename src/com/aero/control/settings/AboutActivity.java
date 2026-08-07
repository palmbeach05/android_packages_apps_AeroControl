package com.aero.control.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.aero.control.R;
import com.aero.control.helpers.ThemeHelper;
import com.ikimuhendis.ldrawer.DrawerArrowDrawable;

import java.lang.reflect.Method;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setTitle(R.string.about);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(false);

        DrawerArrowDrawable upIndicator = new DrawerArrowDrawable(this) {
            @Override
            public boolean isLayoutRtl() {
                return false;
            }
        };
        upIndicator.setProgress(1.0f);
        setActionBarUpIndicator(upIndicator);

        TextView appName = (TextView) findViewById(R.id.about_app_name);
        TextView versionValue = (TextView) findViewById(R.id.about_version_value);
        TextView buildValue = (TextView) findViewById(R.id.about_build_value);
        TextView aboutText = (TextView) findViewById(R.id.about_text);

        appName.setText(R.string.app_name);
        aboutText.setText(R.string.about_dialog);

        try {
            PackageManager pm = getPackageManager();
            String versionName = pm.getPackageInfo(getPackageName(), 0).versionName;
            int versionCode = pm.getPackageInfo(getPackageName(), 0).versionCode;
            versionValue.setText(versionName);
            buildValue.setText(String.valueOf(versionCode));
        } catch (PackageManager.NameNotFoundException e) {
        }

        findViewById(R.id.about_xda).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openExternalUri("https://forum.xda-developers.com/showthread.php?t=2483827");
            }
        });

        findViewById(R.id.about_github).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openExternalUri("https://github.com/Blechd0se/android_packages_apps_AeroControl");
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
                openExternalUri("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=46VQEKBETN36U");
            }
        });
    }

    private void setActionBarUpIndicator(Drawable indicator) {
        try {
            Method setHomeAsUpIndicator = ActionBar.class.getDeclaredMethod(
                    "setHomeAsUpIndicator", Drawable.class);
            setHomeAsUpIndicator.invoke(getActionBar(), indicator);
            return;
        } catch (Exception e) {
            Log.e(AboutActivity.class.getName(), "setActionBarUpIndicator error", e);
        }

        View home = findViewById(android.R.id.home);
        if (home == null) {
            return;
        }

        ViewGroup parent = (ViewGroup) home.getParent();
        if (parent.getChildCount() != 2) {
            return;
        }

        View first = parent.getChildAt(0);
        View second = parent.getChildAt(1);
        View up = first.getId() == android.R.id.home ? second : first;

        if (up instanceof ImageView) {
            ((ImageView) up).setImageDrawable(indicator);
        }
    }

    private void openExternalUri(String uriString) {
        Uri uri = Uri.parse(uriString);
        Intent intent = new Intent("android.intent.action.VIEW", uri);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, R.string.no_activity_for_link, Toast.LENGTH_SHORT).show();
        }
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