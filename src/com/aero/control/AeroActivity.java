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
    private static final String SELECTED_ITEM_ID = "SelectedItemId";
    private static final String EXTRA_OPEN_NAVIGATION_DRAWER = "com.aero.control.OPEN_NAVIGATION_DRAWER";
    public Stack<Fragment> mFragmentStack;
    public static JobManager mJobManager;
    public static PerAppServiceHelper perAppService;
    private ActionBar mActionBar;
    public TextView mActionBarTitle;
    public int mActionBarTitleID;
    private ItemAdapter mAdapter;
    private AeroFragment mAeroFragment;
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
    private ProfileFragment mProfileFragment;
    private StatisticsFragment mStatisticsFragment;
    private TestSuiteFragment mTestSuiteFragment;
    private CharSequence mTitle;
    private UpdaterFragment mUpdaterFragement;
    private String mCurrentTheme;
    private Runnable mPendingSwitch;
    private boolean mClosePending = false;
    private int mSelectedItemPosition = 0;
    private Runnable mClearClosePending;
    private static final int CLOSE_CONFIRMATION_TIMEOUT_MS = 3500;
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
        // Always initialize a fresh stack for this Activity instance
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
            // Restore full fragment stack from saved resource IDs
            int[] stackResourceIds = savedInstanceState.getIntArray("FRAGMENT_STACK_IDS");
            reconnectRestoredFragments(stackResourceIds);
            // Restore using stable resource ID if available, otherwise fall back to position
            int savedItemId = savedInstanceState.getInt(SELECTED_ITEM_ID, -1);

            // Check if the current fragment in content_frame matches the saved selection
            Fragment currentFragment = getFragmentManager().findFragmentById(R.id.content_frame);
            Fragment expectedFragment = (savedItemId != -1) ? getFragmentByResourceId(savedItemId) : null;

            // Enable replacement when restored content doesn't match saved selection
            // Exception: AppDetail is handled via back stack, not drawer selection
            boolean needsReplacement = (currentFragment != expectedFragment) && !hasAppDetailBackStackEntry();

            if (savedItemId != -1) {
                selectItemByResourceId(savedItemId, needsReplacement);
            } else {
                selectItem(savedInstanceState.getInt(SELECTED_ITEM), needsReplacement);
            }
        }
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null && extras.getString("NOTIFY_STRING").equals("APPMONITOR")) {
                selectItemByResourceId(R.string.slider_app_monitor);
            }
        } else {
            if (savedInstanceState.getSerializable("NOTIFY_STRING") != null && savedInstanceState.getSerializable("NOTIFY_STRING").equals("APPMONITOR")) {
                selectItemByResourceId(R.string.slider_app_monitor);
            }
        }
        handleOpenNavigationDrawerRequest();
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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleOpenNavigationDrawerRequest();
    }

    private void handleOpenNavigationDrawerRequest() {
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra(EXTRA_OPEN_NAVIGATION_DRAWER, false)) {
            intent.removeExtra(EXTRA_OPEN_NAVIGATION_DRAWER);
            if (this.mDrawerLayout != null) {
                this.mDrawerLayout.openDrawer(GravityCompat.START);
            }
        }
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
        return super.onOptionsItemSelected(item);
    }

    private class DrawerItemClickListener implements AdapterView.OnItemClickListener {
        private DrawerItemClickListener() {
        }

        @Override // android.widget.AdapterView.OnItemClickListener
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            NavBarItems.PreferenceItem item = AeroActivity.this.mAdapter.getItem(position);
            if (item != null && item.content == R.string.aero_settings) {
                AeroActivity.this.launchSettings();
                return;
            }
            AeroActivity.this.selectItem(position);
        }
    }

    private void launchSettings() {
        if (this.mDrawerLayout != null) {
            this.mDrawerLayout.closeDrawers();
        }
        Intent trIntent = new Intent("android.intent.action.PREFS");
        trIntent.setClass(this, PrefsActivity.class);
        trIntent.setFlags(268435456);
        startActivity(trIntent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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

    private void reconnectRestoredFragments() {
        reconnectRestoredFragments(null);
    }

    private void reconnectRestoredFragments(int[] stackResourceIds) {
        // Reconnect any fragment restored by FragmentManager to activity fields
        android.app.FragmentManager fm = getFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.content_frame);

        if (fragment instanceof AeroFragment) {
            this.mAeroFragment = (AeroFragment) fragment;
        } else if (fragment instanceof CPUFragment) {
            this.mCPUFragement = (CPUFragment) fragment;
        } else if (fragment instanceof StatisticsFragment) {
            this.mStatisticsFragment = (StatisticsFragment) fragment;
        } else if (fragment instanceof GPUFragment) {
            this.mGPUFragement = (GPUFragment) fragment;
        } else if (fragment instanceof MemoryFragment) {
            this.mMemoryFragment = (MemoryFragment) fragment;
        } else if (fragment instanceof MiscSettingsFragment) {
            this.mMiscSettingsFragment = (MiscSettingsFragment) fragment;
        } else if (fragment instanceof DefyPartsFragment) {
            this.mDefyPartsFragment = (DefyPartsFragment) fragment;
        } else if (fragment instanceof UpdaterFragment) {
            this.mUpdaterFragement = (UpdaterFragment) fragment;
        } else if (fragment instanceof ProfileFragment) {
            this.mProfileFragment = (ProfileFragment) fragment;
        } else if (fragment instanceof AppMonitorFragment) {
            this.mAppStatisticsFragment = (AppMonitorFragment) fragment;
        } else if (fragment instanceof TestSuiteFragment) {
            this.mTestSuiteFragment = (TestSuiteFragment) fragment;
        }

        // Rebuild mFragmentStack from saved resource IDs, creating missing fragments
        if (stackResourceIds != null && stackResourceIds.length > 0) {
            for (int resourceId : stackResourceIds) {
                Fragment stackFragment = getFragmentByResourceId(resourceId, true);
                if (stackFragment != null) {
                    mFragmentStack.push(stackFragment);
                }
            }
        } else if (fragment != null) {
            // Fallback: just push the current fragment if no stack was saved
            mFragmentStack.push(fragment);
        }
    }

    private Fragment getFragmentByResourceId(int resourceId) {
        return getFragmentByResourceId(resourceId, false);
    }

    private Fragment getFragmentByResourceId(int resourceId, boolean createIfMissing) {
        // Map resource ID to fragment instance, optionally creating if missing
        if (resourceId == R.string.slider_overview) {
            if (createIfMissing && this.mAeroFragment == null) {
                this.mAeroFragment = new AeroFragment();
            }
            return this.mAeroFragment;
        } else if (resourceId == R.string.slider_cpu_settings) {
            if (createIfMissing && this.mCPUFragement == null) {
                this.mCPUFragement = new CPUFragment();
            }
            return this.mCPUFragement;
        } else if (resourceId == R.string.slider_statistics) {
            if (createIfMissing && this.mStatisticsFragment == null) {
                this.mStatisticsFragment = new StatisticsFragment();
            }
            return this.mStatisticsFragment;
        } else if (resourceId == R.string.slider_gpu_settings) {
            if (createIfMissing && this.mGPUFragement == null) {
                this.mGPUFragement = new GPUFragment();
            }
            return this.mGPUFragement;
        } else if (resourceId == R.string.slider_memory_settings) {
            if (createIfMissing && this.mMemoryFragment == null) {
                this.mMemoryFragment = new MemoryFragment();
            }
            return this.mMemoryFragment;
        } else if (resourceId == R.string.slider_misc_settings) {
            if (createIfMissing && this.mMiscSettingsFragment == null) {
                this.mMiscSettingsFragment = new MiscSettingsFragment();
            }
            return this.mMiscSettingsFragment;
        } else if (resourceId == R.string.slider_defy_parts) {
            if (createIfMissing && this.mDefyPartsFragment == null) {
                this.mDefyPartsFragment = new DefyPartsFragment();
            }
            return this.mDefyPartsFragment;
        } else if (resourceId == R.string.slider_backup_restore) {
            if (createIfMissing && this.mUpdaterFragement == null) {
                this.mUpdaterFragement = new UpdaterFragment();
            }
            return this.mUpdaterFragement;
        } else if (resourceId == R.string.slider_profile) {
            if (createIfMissing && this.mProfileFragment == null) {
                this.mProfileFragment = new ProfileFragment();
            }
            return this.mProfileFragment;
        } else if (resourceId == R.string.slider_app_monitor) {
            if (createIfMissing && this.mAppStatisticsFragment == null) {
                this.mAppStatisticsFragment = new AppMonitorFragment();
            }
            return this.mAppStatisticsFragment;
        } else if (resourceId == R.string.slider_test_suite_settings) {
            if (createIfMissing && this.mTestSuiteFragment == null) {
                this.mTestSuiteFragment = new TestSuiteFragment();
            }
            return this.mTestSuiteFragment;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void selectItem(int position) {
        selectItem(position, true);
    }

    private void selectItem(int position, boolean replaceFragment) {
        if (this.mDrawerLayout != null) {
            this.mDrawerLayout.closeDrawers();
        }
        Fragment fragment = null;
        Fragment oldFragment = null;

        // Get the item's resource ID to identify it, rather than using position
        NavBarItems.PreferenceItem item = this.mAdapter.getItem(position);
        if (item == null) {
            return;
        }

        this.mSelectedItemPosition = position;

        int itemResourceId = item.content;

        // Map resource ID to fragment
        if (itemResourceId == R.string.slider_overview) {
            oldFragment = this.mAeroFragment;
            if (this.mAeroFragment == null || !this.mAeroFragment.isAdded()) {
                this.mAeroFragment = new AeroFragment();
            }
            fragment = this.mAeroFragment;
        } else if (itemResourceId == R.string.slider_cpu_settings) {
            oldFragment = this.mCPUFragement;
            if (this.mCPUFragement == null || !this.mCPUFragement.isAdded()) {
                this.mCPUFragement = new CPUFragment();
            }
            fragment = this.mCPUFragement;
        } else if (itemResourceId == R.string.slider_statistics) {
            oldFragment = this.mStatisticsFragment;
            if (this.mStatisticsFragment == null || !this.mStatisticsFragment.isAdded()) {
                this.mStatisticsFragment = new StatisticsFragment();
            }
            fragment = this.mStatisticsFragment;
        } else if (itemResourceId == R.string.slider_gpu_settings) {
            oldFragment = this.mGPUFragement;
            if (this.mGPUFragement == null || !this.mGPUFragement.isAdded()) {
                this.mGPUFragement = new GPUFragment();
            }
            fragment = this.mGPUFragement;
        } else if (itemResourceId == R.string.slider_memory_settings) {
            oldFragment = this.mMemoryFragment;
            if (this.mMemoryFragment == null || !this.mMemoryFragment.isAdded()) {
                this.mMemoryFragment = new MemoryFragment();
            }
            fragment = this.mMemoryFragment;
        } else if (itemResourceId == R.string.slider_misc_settings) {
            oldFragment = this.mMiscSettingsFragment;
            if (this.mMiscSettingsFragment == null || !this.mMiscSettingsFragment.isAdded()) {
                this.mMiscSettingsFragment = new MiscSettingsFragment();
            }
            fragment = this.mMiscSettingsFragment;
        } else if (itemResourceId == R.string.slider_defy_parts) {
            oldFragment = this.mDefyPartsFragment;
            if (this.mDefyPartsFragment == null || !this.mDefyPartsFragment.isAdded()) {
                this.mDefyPartsFragment = new DefyPartsFragment();
            }
            fragment = this.mDefyPartsFragment;
        } else if (itemResourceId == R.string.slider_backup_restore) {
            oldFragment = this.mUpdaterFragement;
            if (this.mUpdaterFragement == null || !this.mUpdaterFragement.isAdded()) {
                this.mUpdaterFragement = new UpdaterFragment();
            }
            fragment = this.mUpdaterFragement;
        } else if (itemResourceId == R.string.slider_profile) {
            oldFragment = this.mProfileFragment;
            if (this.mProfileFragment == null || !this.mProfileFragment.isAdded()) {
                this.mProfileFragment = new ProfileFragment();
            }
            fragment = this.mProfileFragment;
        } else if (itemResourceId == R.string.slider_app_monitor) {
            oldFragment = this.mAppStatisticsFragment;
            if (this.mAppStatisticsFragment == null || !this.mAppStatisticsFragment.isAdded()) {
                this.mAppStatisticsFragment = new AppMonitorFragment();
            }
            fragment = this.mAppStatisticsFragment;
        } else if (itemResourceId == R.string.slider_test_suite_settings) {
            oldFragment = this.mTestSuiteFragment;
            if (this.mTestSuiteFragment == null || !this.mTestSuiteFragment.isAdded()) {
                this.mTestSuiteFragment = new TestSuiteFragment();
            }
            fragment = this.mTestSuiteFragment;
        }

        if (fragment != null) {
            // If a new fragment was created to replace an old one, remove the old one from the stack
            if (oldFragment != null && oldFragment != fragment && mFragmentStack.contains(oldFragment)) {
                mFragmentStack.remove(oldFragment);
            }
            if (replaceFragment) {
                switchContent(fragment);
            }
        }
        this.mDrawerList.setItemChecked(position, true);

        setTitle(getString(itemResourceId));
        clearClosePending();
        this.mDrawerLayout.closeDrawer(this.mDrawerList);
    }

    private void selectItemByResourceId(int resourceId) {
        selectItemByResourceId(resourceId, true);
    }

    private void selectItemByResourceId(int resourceId, boolean replaceFragment) {
        // Find the position of the item with this resource ID in the current adapter
        for (int i = 0; i < this.mAdapter.getCount(); i++) {
            NavBarItems.PreferenceItem item = this.mAdapter.getItem(i);
            if (item != null && item.content == resourceId) {
                selectItem(i, replaceFragment);
                return;
            }
        }
    }

    public void setActionBarTitle(String title) {
        setTitle(title);
    }

    public void closeAppDetail() {
        getFragmentManager().popBackStack("AppDetail", android.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        setActionBarTitle(getString(R.string.slider_app_monitor));
    }

    private boolean hasAppDetailBackStackEntry() {
        android.app.FragmentManager fragmentManager = getFragmentManager();
        int backStackEntryCount = fragmentManager.getBackStackEntryCount();
        if (backStackEntryCount > 0) {
            // Check only the top entry
            return "AppDetail".equals(fragmentManager.getBackStackEntryAt(backStackEntryCount - 1).getName());
        }
        return false;
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

    @Override // android.app.Activity
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_ITEM, this.mSelectedItemPosition);
        // Save stable navigation item resource ID for robust restoration
        NavBarItems.PreferenceItem item = this.mAdapter.getItem(this.mSelectedItemPosition);
        if (item != null) {
            outState.putInt(SELECTED_ITEM_ID, item.content);
        }
        // Persist the full fragment stack as stable resource IDs
        int[] stackResourceIds = new int[mFragmentStack.size()];
        for (int i = 0; i < mFragmentStack.size(); i++) {
            Fragment frag = mFragmentStack.get(i);
            int resourceId = getResourceIdForFragment(frag);
            stackResourceIds[i] = resourceId;
        }
        outState.putIntArray("FRAGMENT_STACK_IDS", stackResourceIds);
    }

    @Override // android.app.Activity, android.content.ComponentCallbacks
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        if (hasAppDetailBackStackEntry()) {
            closeAppDetail();
            return;
        }
        if (this.mClosePending) {
            finish();
            return;
        }
        if (mFragmentStack.size() > 1) {
            mFragmentStack.pop();
            Fragment previousFragment = mFragmentStack.peek();
            switchContent(previousFragment, false);
            // Restore title by finding which fragment we're returning to
            String restoredTitle = getTitleForFragment(previousFragment);
            if (restoredTitle != null) {
                setTitle(restoredTitle);
            }
        }
        this.mClosePending = true;
        if (this.mClearClosePending != null) {
            mHandler.removeCallbacks(this.mClearClosePending);
        }
        this.mClearClosePending = new Runnable() { // from class: com.aero.control.AeroActivity.4
            @Override // java.lang.Runnable
            public void run() {
                AeroActivity.this.mClosePending = false;
            }
        };
        mHandler.postDelayed(this.mClearClosePending, CLOSE_CONFIRMATION_TIMEOUT_MS);
        Toast.makeText(this, R.string.back_for_close, 1).show();
    }

    private void clearClosePending() {
        this.mClosePending = false;
        if (this.mClearClosePending != null) {
            mHandler.removeCallbacks(this.mClearClosePending);
        }
    }

    private String getTitleForFragment(Fragment fragment) {
        // Map fragment instances back to their titles
        if (fragment == this.mAeroFragment) {
            return getString(R.string.slider_overview);
        } else if (fragment == this.mCPUFragement) {
            return getString(R.string.slider_cpu_settings);
        } else if (fragment == this.mStatisticsFragment) {
            return getString(R.string.slider_statistics);
        } else if (fragment == this.mGPUFragement) {
            return getString(R.string.slider_gpu_settings);
        } else if (fragment == this.mMemoryFragment) {
            return getString(R.string.slider_memory_settings);
        } else if (fragment == this.mMiscSettingsFragment) {
            return getString(R.string.slider_misc_settings);
        } else if (fragment == this.mDefyPartsFragment) {
            return getString(R.string.slider_defy_parts);
        } else if (fragment == this.mUpdaterFragement) {
            return getString(R.string.slider_backup_restore);
        } else if (fragment == this.mProfileFragment) {
            return getString(R.string.slider_profile);
        } else if (fragment == this.mAppStatisticsFragment) {
            return getString(R.string.slider_app_monitor);
        } else if (fragment == this.mTestSuiteFragment) {
            return getString(R.string.slider_test_suite_settings);
        }
        return null;
    }

    private int getResourceIdForFragment(Fragment fragment) {
        // Map fragment instances to their resource IDs
        if (fragment == this.mAeroFragment) {
            return R.string.slider_overview;
        } else if (fragment == this.mCPUFragement) {
            return R.string.slider_cpu_settings;
        } else if (fragment == this.mStatisticsFragment) {
            return R.string.slider_statistics;
        } else if (fragment == this.mGPUFragement) {
            return R.string.slider_gpu_settings;
        } else if (fragment == this.mMemoryFragment) {
            return R.string.slider_memory_settings;
        } else if (fragment == this.mMiscSettingsFragment) {
            return R.string.slider_misc_settings;
        } else if (fragment == this.mDefyPartsFragment) {
            return R.string.slider_defy_parts;
        } else if (fragment == this.mUpdaterFragement) {
            return R.string.slider_backup_restore;
        } else if (fragment == this.mProfileFragment) {
            return R.string.slider_profile;
        } else if (fragment == this.mAppStatisticsFragment) {
            return R.string.slider_app_monitor;
        } else if (fragment == this.mTestSuiteFragment) {
            return R.string.slider_test_suite_settings;
        }
        return -1;
    }

    public final void switchContent(final Fragment fragment) {
        switchContent(fragment, true);
    }

    private void switchContent(final Fragment fragment, final boolean addToStack) {
        if (this.mPendingSwitch != null) {
            mHandler.removeCallbacks(this.mPendingSwitch);
        }
        this.mPendingSwitch = new Runnable() { // from class: com.aero.control.AeroActivity.3
            @Override // java.lang.Runnable
            public void run() {
                if (AeroActivity.this.isFinishing()) {
                    return;
                }
                try {
                    AeroActivity.this.getFragmentManager().beginTransaction().setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out).replace(R.id.content_frame, fragment).commitAllowingStateLoss();
                    // Only add to stack after the transaction has been committed
                    if (addToStack) {
                        mFragmentStack.push(fragment);
                    }
                } catch (IllegalStateException e) {
                    if (!AeroActivity.this.isFinishing()) {
                        AeroActivity.this.recreate();
                    }
                }
            }
        };
        mHandler.postDelayed(this.mPendingSwitch, genHelper.getDefaultDelay());
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        if (this.mPendingSwitch != null) {
            mHandler.removeCallbacks(this.mPendingSwitch);
        }
        if (this.mClearClosePending != null) {
            mHandler.removeCallbacks(this.mClearClosePending);
        }
        super.onDestroy();
    }
}
