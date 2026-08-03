package com.aero.control;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.aero.control.fragments.AeroFragment;
import com.aero.control.fragments.AppMonitorFragment;
import com.aero.control.fragments.CPUFragment;
import com.aero.control.fragments.DefyPartsFragment;
import com.aero.control.fragments.GPUFragment;
import com.aero.control.fragments.MemoryFragment;
import com.aero.control.fragments.MiscSettingsFragment;
import com.aero.control.fragments.ProfileFragment;
import com.aero.control.fragments.StatisticsFragment;
import com.aero.control.fragments.UpdaterFragment;
import com.aero.control.helpers.GenericHelper;
import com.aero.control.helpers.PerApp.AppMonitor.JobManager;
import com.aero.control.helpers.ThemeHelper;
import com.aero.control.helpers.Util;
import com.aero.control.helpers.shellHelper;
import com.aero.control.navItems.NavBarItems;
import com.aero.control.service.PerAppService;
import com.aero.control.service.PerAppServiceHelper;
import com.aero.control.settings.PrefsActivity;
import com.aero.control.testsuite.TestSuiteFragment;
import com.ikimuhendis.ldrawer.ActionBarDrawerToggle;
import com.ikimuhendis.ldrawer.DrawerArrowDrawable;
import java.util.ArrayList;
import java.util.Stack;

/* JADX INFO: loaded from: classes.dex */
public final class AeroActivity extends Activity {
    private static final String SELECTED_ITEM = "SelectedItem";
    public static Stack<Fragment> mFragmentStack;
    public static JobManager mJobManager;
    public static PerAppServiceHelper perAppService;
    private ActionBar mActionBar;
    public TextView mActionBarTitle;
    public int mActionBarTitleID;
    private ItemAdapter mAdapter;
    private AeroFragment mAeroFragment;
    private String[] mAeroTitle;
    private AppMonitorFragment mAppStatisticsFragment;
    private CPUFragment mCPUFragement;
    private DefyPartsFragment mDefyPartsFragment;
    private DrawerArrowDrawable mDrawerArrow;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private GPUFragment mGPUFragement;
    private MemoryFragment mMemoryFragment;
    private MiscSettingsFragment mMiscSettingsFragment;
    private int mPreviousTitle;
    private ProfileFragment mProfileFragment;
    private StatisticsFragment mStatisticsFragment;
    private TestSuiteFragment mTestSuiteFragment;
    private CharSequence mTitle;
    private UpdaterFragment mUpdaterFragement;
    private String mCurrentTheme;
    private static int mBackCounter = 0;
    public static final Handler mHandler = new Handler(Looper.getMainLooper());
    public static final Typeface font = Typeface.create("sans-serif-condensed", 0);
    public static final shellHelper shell = shellHelper.instance();
    public static GenericHelper genHelper = new GenericHelper();

    @Override // android.app.Activity
    public void onCreate(Bundle savedInstanceState) {
        this.mCurrentTheme = ThemeHelper.getTheme(this);
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (getResources().getBoolean(R.bool.portrait_only)) {
            setRequestedOrientation(1);
        }
        mJobManager = JobManager.instance(this);
        int actionBarHeight = 0;
        if (getActionBar() != null) {
            getActionBar().setIcon(android.R.color.transparent);
        }
        mFragmentStack = new Stack<>();
        if (Build.VERSION.SDK_INT >= 19 && !ViewConfiguration.get(getBaseContext()).hasPermanentMenuKey()) {
            Window win = getWindow();
            WindowManager.LayoutParams winParams = win.getAttributes();
            winParams.flags |= 134217728;
            win.setAttributes(winParams);
            TypedValue tv = new TypedValue();
            if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
            }
        }
        if (!isServiceUp()) {
            perAppService = new PerAppServiceHelper(this);
            if (perAppService.shouldBeStarted()) {
                Util.showUsageStatDialog(this);
                perAppService.startService();
            }
        }
        if (Build.VERSION.SDK_INT >= 21) {
            this.mActionBar = getActionBar();
        } else {
            this.mActionBarTitleID = getResources().getIdentifier("action_bar_title", "id", "android");
            this.mActionBarTitle = (TextView) findViewById(this.mActionBarTitleID);
            this.mActionBarTitle.setTypeface(font);
        }
        this.mTitle = getTitle();
        this.mAeroTitle = getResources().getStringArray(R.array.aero_array);
        this.mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        this.mDrawerList = (ListView) findViewById(R.id.left_drawer);
        if (actionBarHeight != 0) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) this.mDrawerLayout.getLayoutParams();
            params.setMargins(0, ((int) TypedValue.applyDimension(1, 24.0f, getResources().getDisplayMetrics())) + actionBarHeight, 0, 0);
            this.mDrawerLayout.setLayoutParams(params);
        }
        this.mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        NavBarItems content = new NavBarItems(this);
        this.mAdapter = new ItemAdapter(this, R.layout.activity_main, content.ITEMS);
        this.mDrawerList.setAdapter((ListAdapter) this.mAdapter);
        this.mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        this.mDrawerArrow = new DrawerArrowDrawable(this) { // from class: com.aero.control.AeroActivity.1
            @Override // com.ikimuhendis.ldrawer.DrawerArrowDrawable
            public boolean isLayoutRtl() {
                return false;
            }
        };
        this.mDrawerToggle = new ActionBarDrawerToggle(this, this.mDrawerLayout, this.mDrawerArrow, R.string.drawer_open, R.string.drawer_close) { // from class: com.aero.control.AeroActivity.2
            @Override // com.ikimuhendis.ldrawer.ActionBarDrawerToggle, android.support.v4.app.ActionBarDrawerToggle, android.support.v4.widget.DrawerLayout.DrawerListener
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                AeroActivity.this.invalidateOptionsMenu();
            }

            @Override // com.ikimuhendis.ldrawer.ActionBarDrawerToggle, android.support.v4.app.ActionBarDrawerToggle, android.support.v4.widget.DrawerLayout.DrawerListener
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                AeroActivity.this.invalidateOptionsMenu();
            }
        };
        this.mDrawerLayout.setDrawerListener(this.mDrawerToggle);
        this.mDrawerToggle.syncState();
        if (savedInstanceState == null) {
            selectItem(0);
        } else {
            selectItem(savedInstanceState.getInt(SELECTED_ITEM));
        }
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null && extras.getString("NOTIFY_STRING").equals("APPMONITOR")) {
                selectItemByResourceId(R.string.slider_app_monitor);
                return;
            }
            return;
        }
        if (savedInstanceState.getSerializable("NOTIFY_STRING") != null && savedInstanceState.getSerializable("NOTIFY_STRING").equals("APPMONITOR")) {
            selectItemByResourceId(R.string.slider_app_monitor);
        }
    }

    private final class ItemAdapter extends ArrayAdapter<NavBarItems.PreferenceItem> {
        private ArrayList<NavBarItems.PreferenceItem> items;

        public ItemAdapter(Context context, int textViewResourceId, ArrayList<NavBarItems.PreferenceItem> objects) {
            super(context, textViewResourceId, objects);
            this.items = objects;
        }

        @Override // android.widget.ArrayAdapter, android.widget.Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = AeroActivity.this.getLayoutInflater();
                v = vi.inflate(R.layout.adapter_item, (ViewGroup) null);
            }
            NavBarItems.PreferenceItem item = this.items.get(position);
            if (item != null) {
                ImageView icon = (ImageView) v.findViewById(R.id.icon);
                TextView text = (TextView) v.findViewById(R.id.text);
                text.setTypeface(AeroActivity.font);
                if (icon != null) {
                    icon.clearColorFilter();
                    icon.setImageResource(item.drawable);
                }
                if (text != null) {
                    text.setText(AeroActivity.this.getString(item.content));
                    text.setTextColor(AeroActivity.this.getResources().getColorStateList(ThemeHelper.THEME_DARK.equals(AeroActivity.this.mCurrentTheme) ? R.drawable.textview_drawer_dark : R.drawable.textview_drawer));
                }
            }
            return v;
        }
    }

    @Override // android.app.Activity
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override // android.app.Activity
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        if (!ThemeHelper.getTheme(this).equals(this.mCurrentTheme)) {
            recreate();
            return;
        }
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getString("NOTIFY_STRING").equals("APPMONITOR")) {
            selectItemByResourceId(R.string.slider_app_monitor);
        }
        getIntent().putExtra("NOTIFY_STRING", new String());
    }

    @Override // android.app.Activity
    public boolean onOptionsItemSelected(MenuItem item) {
        if (this.mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.aero_settings /* 2131099748 */:
                Intent trIntent = new Intent("android.intent.action.PREFS");
                trIntent.setClass(this, PrefsActivity.class);
                trIntent.setFlags(268435456);
                startActivity(trIntent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private class DrawerItemClickListener implements AdapterView.OnItemClickListener {
        private DrawerItemClickListener() {
        }

        @Override // android.widget.AdapterView.OnItemClickListener
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            AeroActivity.this.selectItem(position);
        }
    }

    private boolean isServiceUp() {
        ActivityManager manager = (ActivityManager) getSystemService("activity");
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (PerAppService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void selectItem(int position) {
        if (this.mDrawerLayout != null) {
            this.mDrawerLayout.closeDrawers();
        }
        Fragment fragment = null;

        // Get the item's resource ID to identify it, rather than using position
        NavBarItems.PreferenceItem item = this.mAdapter.getItem(position);
        if (item == null) {
            return;
        }

        int itemResourceId = item.content;

        // Map resource ID to fragment
        if (itemResourceId == R.string.slider_overview) {
            if (this.mAeroFragment == null) {
                this.mAeroFragment = new AeroFragment();
            }
            fragment = this.mAeroFragment;
        } else if (itemResourceId == R.string.slider_cpu_settings) {
            if (this.mCPUFragement == null) {
                this.mCPUFragement = new CPUFragment();
            }
            fragment = this.mCPUFragement;
        } else if (itemResourceId == R.string.slider_statistics) {
            if (this.mStatisticsFragment == null) {
                this.mStatisticsFragment = new StatisticsFragment();
            }
            fragment = this.mStatisticsFragment;
        } else if (itemResourceId == R.string.slider_gpu_settings) {
            if (this.mGPUFragement == null) {
                this.mGPUFragement = new GPUFragment();
            }
            fragment = this.mGPUFragement;
        } else if (itemResourceId == R.string.slider_memory_settings) {
            if (this.mMemoryFragment == null) {
                this.mMemoryFragment = new MemoryFragment();
            }
            fragment = this.mMemoryFragment;
        } else if (itemResourceId == R.string.slider_misc_settings) {
            if (this.mMiscSettingsFragment == null) {
                this.mMiscSettingsFragment = new MiscSettingsFragment();
            }
            fragment = this.mMiscSettingsFragment;
        } else if (itemResourceId == R.string.slider_defy_parts) {
            if (this.mDefyPartsFragment == null) {
                this.mDefyPartsFragment = new DefyPartsFragment();
            }
            fragment = this.mDefyPartsFragment;
        } else if (itemResourceId == R.string.slider_backup_restore) {
            if (this.mUpdaterFragement == null) {
                this.mUpdaterFragement = new UpdaterFragment();
            }
            fragment = this.mUpdaterFragement;
        } else if (itemResourceId == R.string.slider_profile) {
            if (this.mProfileFragment == null) {
                this.mProfileFragment = new ProfileFragment();
            }
            fragment = this.mProfileFragment;
        } else if (itemResourceId == R.string.slider_app_monitor) {
            if (this.mAppStatisticsFragment == null) {
                this.mAppStatisticsFragment = new AppMonitorFragment();
            }
            fragment = this.mAppStatisticsFragment;
        } else if (itemResourceId == R.string.slider_test_suite_settings) {
            if (this.mTestSuiteFragment == null) {
                this.mTestSuiteFragment = new TestSuiteFragment();
            }
            fragment = this.mTestSuiteFragment;
        }

        if (fragment != null) {
            switchContent(fragment);
        }
        this.mDrawerList.setItemChecked(position, true);

        // Find the position in the title array for this item
        int titleIndex = findTitleIndex(itemResourceId);
        this.mPreviousTitle = titleIndex;
        setTitle(this.mAeroTitle[titleIndex]);
        mBackCounter = 0;
        this.mDrawerLayout.closeDrawer(this.mDrawerList);
    }

    private int findTitleIndex(int resourceId) {
        // The aero_array in arrays.xml contains all items including defy_parts
        // We need to find the index matching this resource ID
        String[] titleArray = this.mAeroTitle;
        String itemName = getString(resourceId);
        for (int i = 0; i < titleArray.length; i++) {
            if (titleArray[i].equals(itemName)) {
                return i;
            }
        }
        return 0; // Default to overview if not found
    }

    private void selectItemByResourceId(int resourceId) {
        // Find the position of the item with this resource ID in the current adapter
        for (int i = 0; i < this.mAdapter.getCount(); i++) {
            NavBarItems.PreferenceItem item = this.mAdapter.getItem(i);
            if (item != null && item.content == resourceId) {
                selectItem(i);
                return;
            }
        }
    }

    public void setActionBarTitle(String title) {
        setTitle(title);
    }

    @Override // android.app.Activity
    public final void setTitle(CharSequence title) {
        this.mTitle = title;
        if (Build.VERSION.SDK_INT >= 21) {
            if (this.mActionBar != null) {
                this.mActionBar.setTitle(this.mTitle);
            }
        } else if (this.mActionBarTitle != null) {
            this.mActionBarTitle.setText(this.mTitle);
        }
    }

    @Override // android.app.Activity
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        this.mDrawerToggle.syncState();
    }

    @Override // android.app.Activity, android.content.ComponentCallbacks
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        if (mFragmentStack.size() > 1) {
            switchContent(mFragmentStack.lastElement());
            setTitle(this.mAeroTitle[this.mPreviousTitle]);
        }
        mBackCounter++;
        if (mBackCounter == 1) {
            Toast.makeText(this, R.string.back_for_close, 1).show();
        }
        if (mBackCounter == 2) {
            finish();
        }
    }

    public static void resetBackCounter() {
        mBackCounter = 0;
    }

    public final void switchContent(final Fragment fragment) {
        mHandler.postDelayed(new Runnable() { // from class: com.aero.control.AeroActivity.3
            @Override // java.lang.Runnable
            public void run() {
                try {
                    AeroActivity.this.getFragmentManager().beginTransaction().setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out).replace(R.id.content_frame, fragment).commitAllowingStateLoss();
                } catch (IllegalStateException e) {
                    AeroActivity.this.recreate();
                }
            }
        }, genHelper.getDefaultDelay());
        mFragmentStack.push(fragment);
    }
}
