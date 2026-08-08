package com.aero.control.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.aero.control.R;
import com.aero.control.helpers.OrientationHelper;
import com.aero.control.helpers.ThemeHelper;
import com.aero.control.navItems.NavigationDrawerHelper;
import com.ikimuhendis.ldrawer.DrawerArrowDrawable;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        OrientationHelper.applyOrientation(this);
        setContentView(R.layout.activity_about);
        setTitle(R.string.about);
        getActionBar().setDisplayShowHomeEnabled(false);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        DrawerArrowDrawable upIndicator = new DrawerArrowDrawable(this) {
            @Override
            public boolean isLayoutRtl() {
                return false;
            }
        };
        upIndicator.setProgress(1.0f);
        NavigationDrawerHelper.setActionBarUpIndicator(this, upIndicator);

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

        Button githubButton = (Button) findViewById(R.id.about_github);
        tintGithubIcon(githubButton);
        githubButton.setOnClickListener(new View.OnClickListener() {
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

    private void tintGithubIcon(Button githubButton) {
        Drawable[] compoundDrawables = githubButton.getCompoundDrawables();
        Drawable icon = compoundDrawables[0];
        if (icon == null) {
            return;
        }
        TypedArray typedArray = obtainStyledAttributes(new int[]{R.attr.aeroIconTint});
        int tintColor = typedArray.getColor(0, 0);
        typedArray.recycle();

        icon = icon.mutate();
        icon.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);
        githubButton.setCompoundDrawablesWithIntrinsicBounds(icon, compoundDrawables[1], compoundDrawables[2], compoundDrawables[3]);
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
    protected void onResume() {
        super.onResume();
        OrientationHelper.applyOrientation(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}