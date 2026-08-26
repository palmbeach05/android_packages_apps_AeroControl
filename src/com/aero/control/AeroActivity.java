package com.aero.control;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.widget.DrawerLayout;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.aero.control.fragments.AeroFragment;
import com.aero.control.fragments.AppMonitorDetailFragment;
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
import com.aero.control.helpers.OrientationHelper;
import com.aero.control.helpers.PerApp.AppMonitor.JobManager;
import com.aero.control.helpers.ThemeHelper;
import com.aero.control.helpers.Util;
import com.aero.control.helpers.shellHelper;
import com.aero.control.navItems.NavBarItems;
import com.aero.control.navItems.NavigationDrawerHelper;
import com.aero.control.service.PerAppService;
import com.aero.control.service.PerAppServiceHelper;
import com.aero.control.settings.PrefsActivity;
import com.aero.control.testsuite.TestSuiteFragment;
import java.util.Stack;

/**
 * Main activity of the AeroControl application. Provides a navigation drawer
 * interface for switching between CPU, GPU, memory, and other system control
 * fragments. Manages fragment lifecycle, theme changes, and integration with
 * the per-app monitoring service.
 */
public final class AeroActivity extends Activity {
    private static final String SELECTED_ITEM = "SelectedItem";
    private static final String SELECTED_ITEM_ID = "SelectedItemId";
    public static final String EXTRA_SELECTED_ITEM_ID = "com.aero.control.SELECTED_ITEM_ID";
    public Stack<Fragment> mFragmentStack;
    public static JobManager mJobManager;
    public static PerAppServiceHelper perAppService;
    private ActionBar mActionBar;
    public TextView mActionBarTitle;
    public int mActionBarTitleID;
    private AeroFragment mAeroFragment;
    private AppMonitorFragment mAppStatisticsFragment;
    private CPUFragment mCPUFragement;
    private DefyPartsFragment mDefyPartsFragment;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private NavigationDrawerHelper mNavigationDrawer;
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
    private Runnable mPendingBackgroundInit;
    // Process-local handoff used to carry a drawer selection made on this activity
    // instance across into the instance created by recreate() (e.g. on rotation),
    // so the tap isn't lost or acted upon by the soon-to-be-destroyed instance.
    private static final int NO_PENDING_DRAWER_ITEM = -1;
    private static int sPendingDrawerItemResourceId = NO_PENDING_DRAWER_ITEM;
    private static boolean sPendingRecreation = false;
    // Resource ID of the drawer item for a switchContent() transaction that
    // selectItem() has posted on mHandler but that hasn't committed yet. Lets
    // onConfigurationChanged() hand the selection off to the recreated
    // instance instead of letting it run on this one. Tracked on all
    // supported API levels, since rotation can race ahead of the posted
    // transaction on any of them.
    private int mPendingDrawerTransactionItemResourceId = NO_PENDING_DRAWER_ITEM;
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
        OrientationHelper.applyOrientation(this);
        // This instance is the recreated activity (if any recreation was in
        // progress); clear the marker now, before the normal restoration below
        // runs, so restoration's own selectItem() calls aren't mistaken for a
        // hand-off from the old, destroyed instance.
        sPendingRecreation = false;
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
        this.mNavigationDrawer = new NavigationDrawerHelper(this, new NavigationDrawerHelper.OnDrawerItemSelectedListener() {
            @Override
            public void onDrawerItemSelected(NavBarItems.PreferenceItem item, int position) {
                if (item.content == R.string.aero_settings) {
                    AeroActivity.this.launchSettings();
                    return;
                }
                AeroActivity.this.selectItem(position);
            }
        });
        this.mNavigationDrawer.syncState();
        int savedItemId = -1;
        if (savedInstanceState == null) {
            selectItem(0);
        } else {
            // Restore full fragment stack from saved resource IDs
            int[] stackResourceIds = savedInstanceState.getIntArray("FRAGMENT_STACK_IDS");
            reconnectRestoredFragments(stackResourceIds);
            // Restore using stable resource ID if available, otherwise fall back to position
            savedItemId = savedInstanceState.getInt(SELECTED_ITEM_ID, -1);

            // Check if the current fragment in content_frame matches the saved selection.
            // Note: Fragment.getView() is not used here to test for a rendered view --
            // it is allowed to be null while Android is still restoring/creating the
            // fragment's view during onCreate(), so treating that as "needs replacement"
            // caused a spurious switchContent() that raced with normal restoration.
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
            if (extras != null && "APPMONITOR".equals(extras.getString("NOTIFY_STRING"))) {
                selectItemByResourceId(R.string.slider_app_monitor);
            }
        } else {
            if (savedInstanceState.getSerializable("NOTIFY_STRING") != null && savedInstanceState.getSerializable("NOTIFY_STRING").equals("APPMONITOR")) {
                selectItemByResourceId(R.string.slider_app_monitor);
            }
        }
        handleSelectedItemRequest();
        // If a drawer item was tapped on the previous instance while a
        // recreation (e.g. rotation) was already underway, apply it now that
        // normal restoration above is done, so it overrides only the stale
        // restored selection instead of racing with it.
        int pendingDrawerItemResourceId = sPendingDrawerItemResourceId;
        sPendingDrawerItemResourceId = NO_PENDING_DRAWER_ITEM;
        if (pendingDrawerItemResourceId != NO_PENDING_DRAWER_ITEM) {
            selectItemByResourceId(pendingDrawerItemResourceId);
        }
        if (savedInstanceState != null) {
            // FragmentManager restoration can re-attach the correct fragment
            // instance for content_frame without ever giving it a rendered
            // view (e.g. StatisticsFragment recreated for rotation into
            // res/layout-land/statistics.xml), leaving the CPU Statistics
            // screen blank even though currentFragment already matched
            // expectedFragment earlier. Unlike checking getView() during
            // onCreate() itself, this schedules a one-time check to run
            // after this activity's layout has completed, so it only reacts
            // to an actually-blank content_frame instead of racing with a
            // still-in-progress normal restoration.
            //
            // Schedule the check for the item that remains selected after
            // the hand-off above, not the restored savedItemId: on the
            // initial rotation, savedItemId is the page that was active
            // before CPU Statistics was tapped mid-rotation, so a stale
            // check for savedItemId would silently skip the CPU Statistics
            // recovery this activity actually needs.
            int recoveryItemId = (pendingDrawerItemResourceId != NO_PENDING_DRAWER_ITEM)
                    ? pendingDrawerItemResourceId : savedItemId;
            scheduleBlankStatisticsContentRecoveryCheck(recoveryItemId);
        }
        // Initialize mJobManager synchronously so restored fragments can access it.
        mJobManager = JobManager.instance(this);
        // Defer heavier service-starting work until after the restored fragment has rendered,
        // so it doesn't add latency to activity recreation (e.g. on rotation).
        if (this.mPendingBackgroundInit != null) {
            mHandler.removeCallbacks(this.mPendingBackgroundInit);
        }
        this.mPendingBackgroundInit = new Runnable() { // from class: com.aero.control.AeroActivity.5
            @Override // java.lang.Runnable
            public void run() {
                if (!AeroActivity.this.isFinishing()) {
                    AeroActivity.this.initBackgroundServices();
                }
            }
        };
        mHandler.post(this.mPendingBackgroundInit);
    }

    private void initBackgroundServices() {
        if (perAppService == null) {
            perAppService = new PerAppServiceHelper(getApplicationContext());
        }
        if (!isServiceUp()) {
            if (perAppService.shouldBeStarted()) {
                Util.showUsageStatDialog(this);
                perAppService.startService();
            }
        } else {
            perAppService.setState(true);
        }
    }

    @Override // android.app.Activity
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleSelectedItemRequest();
    }

    private void handleSelectedItemRequest() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_SELECTED_ITEM_ID)) {
            int selectedItemId = intent.getIntExtra(EXTRA_SELECTED_ITEM_ID, -1);
            intent.removeExtra(EXTRA_SELECTED_ITEM_ID);
            if (selectedItemId != -1) {
                selectItemByResourceId(selectedItemId);
            }
        }
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        OrientationHelper.applyOrientation(this);
        if (!ThemeHelper.getTheme(this).equals(this.mCurrentTheme)) {
            recreate();
            return;
        }
        Bundle extras = getIntent().getExtras();
        if (extras != null && "APPMONITOR".equals(extras.getString("NOTIFY_STRING"))) {
            selectItemByResourceId(R.string.slider_app_monitor);
        }
        getIntent().putExtra("NOTIFY_STRING", new String());
    }

    @Override // android.app.Activity
    public boolean onOptionsItemSelected(MenuItem item) {
        if (this.mNavigationDrawer.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        NavBarItems.PreferenceItem item = this.mNavigationDrawer.getItem(position);
        if (item == null) {
            return;
        }

        if (sPendingRecreation) {
            // This instance is being replaced (e.g. rotation already triggered
            // recreate()). Hand the selection off to the recreated instance
            // instead of building a fragment or switching content here.
            sPendingDrawerItemResourceId = item.content;
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
                // Track which drawer item this transaction is for, so
                // onConfigurationChanged() can hand it off if rotation
                // races ahead of this transaction committing.
                mPendingDrawerTransactionItemResourceId = itemResourceId;
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
        for (int i = 0; i < this.mNavigationDrawer.getItemCount(); i++) {
            NavBarItems.PreferenceItem item = this.mNavigationDrawer.getItem(i);
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
        this.mNavigationDrawer.syncState();
    }

    @Override // android.app.Activity
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_ITEM, this.mSelectedItemPosition);
        // Save stable navigation item resource ID for robust restoration
        NavBarItems.PreferenceItem item = this.mNavigationDrawer.getItem(this.mSelectedItemPosition);
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
        this.mNavigationDrawer.onConfigurationChanged(newConfig);
        if (this.mTitle != null) {
            setTitle(this.mTitle);
        }
        if (currentFragmentRequiresRecreation()) {
            if (this.mPendingDrawerTransactionItemResourceId != NO_PENDING_DRAWER_ITEM) {
                // A drawer transaction was already posted by switchContent()
                // before this configuration change began recreating the
                // activity. Hand its resource ID off through the same mechanism
                // used for selections made after recreation begins, so it
                // isn't lost.
                sPendingDrawerItemResourceId = this.mPendingDrawerTransactionItemResourceId;
                this.mPendingDrawerTransactionItemResourceId = NO_PENDING_DRAWER_ITEM;
            }
            // Cancel any switchContent() transaction still pending on the
            // handler so it can't run against this soon-to-be-destroyed
            // instance.
            if (this.mPendingSwitch != null) {
                mHandler.removeCallbacks(this.mPendingSwitch);
            }
            // Mark that this instance is about to be replaced so a drawer
            // selection tapped on it before it's destroyed is handed off to the
            // recreated instance instead of switching content here.
            sPendingRecreation = true;
            recreate();
        }
    }

    // StatisticsFragment (CPU Statistics), ProfileFragment, and
    // AppMonitorDetailFragment each provide a dedicated landscape layout
    // under res/layout-land/ that can only be inflated by recreating the
    // activity. Standard drawer pages have no such requirement, so they stay
    // in this activity instance across rotation instead of being torn down
    // and recreated.
    private boolean currentFragmentRequiresRecreation() {
        Fragment currentFragment = getFragmentManager().findFragmentById(R.id.content_frame);
        return currentFragment instanceof ProfileFragment
                || currentFragment instanceof StatisticsFragment
                || currentFragment instanceof AppMonitorDetailFragment;
    }

    // Schedules a one-time, post-layout check for a blank content_frame
    // after restoring the CPU Statistics selection. Only CPU Statistics is
    // affected by the res/layout-land/statistics.xml recreation described
    // above, so other saved selections don't need this check.
    private void scheduleBlankStatisticsContentRecoveryCheck(final int savedItemId) {
        if (savedItemId != R.string.slider_statistics) {
            return;
        }
        final View contentFrame = findViewById(R.id.content_frame);
        if (contentFrame == null) {
            return;
        }
        contentFrame.post(new Runnable() { // from class: com.aero.control.AeroActivity.6
            @Override
            public void run() {
                AeroActivity.this.recoverBlankStatisticsContentIfNeeded(savedItemId);
            }
        });
    }

    // Runs after this restored activity's layout has completed (and, if a
    // matching drawer transaction was still in flight, after that
    // transaction has settled too). If content_frame is still blank for the
    // restored CPU Statistics selection, performs a single replacement
    // transaction to force the fragment's view to be created.
    private void recoverBlankStatisticsContentIfNeeded(int savedItemId) {
        if (isFinishing() || hasAppDetailBackStackEntry()) {
            return;
        }
        // This instance is itself about to be recreated again (e.g. another
        // rotation raced in before this check ran); let the next instance's
        // own restoration handle recovery instead of fighting it here.
        if (sPendingDrawerItemResourceId != NO_PENDING_DRAWER_ITEM) {
            return;
        }
        if (this.mPendingDrawerTransactionItemResourceId != NO_PENDING_DRAWER_ITEM) {
            // During initial rotation, the old activity instance can hand
            // off a still-pending switchContent() transaction for this same
            // restored CPU Statistics selection (see onConfigurationChanged
            // and the sPendingDrawerItemResourceId hand-off in onCreate).
            // That transaction hasn't committed yet, so content_frame can't
            // be judged blank or not yet -- wait for it to settle instead of
            // giving up permanently.
            if (this.mPendingDrawerTransactionItemResourceId == savedItemId) {
                scheduleBlankStatisticsContentRecoveryCheck(savedItemId);
            }
            // Otherwise a different, newer drawer selection is pending;
            // don't fight it with a stale replacement for the restored CPU
            // Statistics selection.
            return;
        }
        Fragment currentFragment = getFragmentManager().findFragmentById(R.id.content_frame);
        FrameLayout contentFrame = (FrameLayout) findViewById(R.id.content_frame);
        boolean contentFramePresent = currentFragment != null && currentFragment.getView() != null
                && contentFrame != null && contentFrame.getChildCount() > 0;
        if (contentFramePresent) {
            return;
        }
        selectItemByResourceId(savedItemId, true);
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
                // This transaction is about to run (or be skipped below), so
                // it's no longer pending.
                AeroActivity.this.mPendingDrawerTransactionItemResourceId = NO_PENDING_DRAWER_ITEM;
                if (AeroActivity.this.isFinishing()) {
                    return;
                }
                try {
                    AeroActivity.this.getFragmentManager().beginTransaction().replace(R.id.content_frame, fragment).commitAllowingStateLoss();
                    // Only add to stack after the transaction has been committed
                    // and only if the fragment is not already at the top of the stack
                    if (addToStack && (mFragmentStack.isEmpty() || mFragmentStack.peek() != fragment)) {
                        mFragmentStack.push(fragment);
                    }
                } catch (IllegalStateException e) {
                    if (!AeroActivity.this.isFinishing()) {
                        AeroActivity.this.recreate();
                    }
                }
            }
        };
        mHandler.post(this.mPendingSwitch);
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        if (this.mPendingSwitch != null) {
            mHandler.removeCallbacks(this.mPendingSwitch);
        }
        if (this.mClearClosePending != null) {
            mHandler.removeCallbacks(this.mClearClosePending);
        }
        if (this.mPendingBackgroundInit != null) {
            mHandler.removeCallbacks(this.mPendingBackgroundInit);
        }
        super.onDestroy();
    }
}
