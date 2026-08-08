package com.aero.control;

import android.app.AlertDialog;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.aero.control.helpers.Android.CirclePageIndicator;
import com.aero.control.helpers.OrientationHelper;
import com.aero.control.helpers.ZoomOutPageTransformer;
import com.aero.control.helpers.rootHelper;
import com.aero.control.sliderFragments.IntroductionFragment;
import com.aero.control.sliderFragments.PerAppFragment;
import com.aero.control.sliderFragments.SetOnBootFragment;
import com.aero.control.sliderFragments.TutorialFragment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class SplashScreen extends FragmentActivity {
    public static final String FIRSTRUN_AERO = "firstrun_aero";
    private static int NUM_PAGES = 4;
    private static final rootHelper rootCheck = new rootHelper();
    private CirclePageIndicator mCircleIndicator;
    private List<Fragment> mFragments = new ArrayList();
    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;
    public Button mSkip;

    @Override // android.support.v4.app.FragmentActivity, android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        OrientationHelper.applyOrientation(this);
        this.mFragments.clear();
        this.mFragments.add(new IntroductionFragment());
        this.mFragments.add(new PerAppFragment());
        this.mFragments.add(new SetOnBootFragment());
        this.mFragments.add(new TutorialFragment());
        ContextWrapper cw = new ContextWrapper(getBaseContext());
        File firstrun_aero = new File(cw.getFilesDir() + "/" + FIRSTRUN_AERO);
        if (!rootCheck.isDeviceRooted()) {
            showRootDialog();
        }
        if (firstrun_aero.exists()) {
            Intent i = new Intent(this, (Class<?>) AeroActivity.class);
            startActivity(i);
            finish();
        }
        this.mPager = (ViewPager) findViewById(R.id.pager);
        this.mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        this.mPager.setPageTransformer(true, new ZoomOutPageTransformer());
        this.mPager.setAdapter(this.mPagerAdapter);
        this.mCircleIndicator = (CirclePageIndicator) findViewById(R.id.indicator);
        this.mCircleIndicator.setViewPager(this.mPager);
        this.mSkip = (Button) findViewById(R.id.splash_button);
        initDefaultSkip();
    }

    @Override // android.support.v4.app.FragmentActivity, android.app.Activity
    protected void onResume() {
        super.onResume();
        OrientationHelper.applyOrientation(this);
    }

    public void initDefaultSkip() {
        this.mSkip.setText(R.string.skip_splash);
        this.mSkip.setOnClickListener(new View.OnClickListener() { // from class: com.aero.control.SplashScreen.1
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                try {
                    FileOutputStream fos = SplashScreen.this.openFileOutput(FIRSTRUN_AERO, 0);
                    fos.write("1".getBytes());
                    fos.close();
                } catch (IOException e) {
                    Log.e("Aero", "Could not save file(s). ", e);
                } catch (NullPointerException e2) {
                    Log.e("Aero", "OpenFileOutput probably was initialized on a null-object.", e2);
                }
                Intent i = new Intent(SplashScreen.this, AeroActivity.class);
                SplashScreen.this.startActivity(i);
                SplashScreen.this.finish();
            }
        });
    }

    public final void showRootDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.about_screen, (ViewGroup) null);
        TextView aboutText = (TextView) layout.findViewById(R.id.aboutScreen);
        builder.setTitle(R.string.not_rooted);
        builder.setIcon(R.drawable.warning);
        aboutText.setText(getText(R.string.root_required));
        builder.setCancelable(false);
        builder.setView(layout).setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() { // from class: com.aero.control.SplashScreen.2
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog, int id) {
                SplashScreen.this.finish();
            }
        });
        builder.show();
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override // android.support.v4.app.FragmentStatePagerAdapter
        public Fragment getItem(int position) {
            Fragment fragment = (Fragment) SplashScreen.this.mFragments.get(position);
            return fragment;
        }

        @Override // android.support.v4.view.PagerAdapter
        public int getCount() {
            return SplashScreen.NUM_PAGES;
        }
    }
}
