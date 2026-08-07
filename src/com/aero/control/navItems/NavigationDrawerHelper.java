package com.aero.control.navItems;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.aero.control.R;
import com.aero.control.helpers.ThemeHelper;
import com.ikimuhendis.ldrawer.ActionBarDrawerToggle;
import com.ikimuhendis.ldrawer.DrawerArrowDrawable;

import java.util.ArrayList;

/**
 * Reusable navigation-drawer component shared by AeroActivity, PrefsActivity and
 * AboutActivity. It wires up the R.id.left_drawer ListView with the NavBarItems
 * adapter, and configures the DrawerArrowDrawable/ActionBarDrawerToggle pair so the
 * action-bar menu icon opens or closes the drawer over the activity's current content.
 */
public class NavigationDrawerHelper {

    public interface OnDrawerItemSelectedListener {
        void onDrawerItemSelected(NavBarItems.PreferenceItem item, int position);
    }

    private final DrawerLayout mDrawerLayout;
    private final ActionBarDrawerToggle mDrawerToggle;
    private final ItemAdapter mAdapter;

    public NavigationDrawerHelper(final Activity activity, final OnDrawerItemSelectedListener listener) {
        this.mDrawerLayout = (DrawerLayout) activity.findViewById(R.id.drawer_layout);
        ListView drawerList = (ListView) activity.findViewById(R.id.left_drawer);

        this.mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        NavBarItems content = new NavBarItems(activity);
        this.mAdapter = new ItemAdapter(activity, content.ITEMS, ThemeHelper.getTheme(activity));
        drawerList.setAdapter((ListAdapter) this.mAdapter);
        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                NavBarItems.PreferenceItem item = NavigationDrawerHelper.this.mAdapter.getItem(position);
                if (item != null && listener != null) {
                    listener.onDrawerItemSelected(item, position);
                }
            }
        });

        activity.getActionBar().setDisplayHomeAsUpEnabled(true);
        activity.getActionBar().setHomeButtonEnabled(true);

        DrawerArrowDrawable drawerArrow = new DrawerArrowDrawable(activity) {
            @Override
            public boolean isLayoutRtl() {
                return false;
            }
        };

        this.mDrawerToggle = new ActionBarDrawerToggle(activity, this.mDrawerLayout, drawerArrow, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                activity.invalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                activity.invalidateOptionsMenu();
            }
        };
        this.mDrawerLayout.setDrawerListener(this.mDrawerToggle);
    }

    public void syncState() {
        this.mDrawerToggle.syncState();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        this.mDrawerToggle.onConfigurationChanged(newConfig);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return this.mDrawerToggle.onOptionsItemSelected(item);
    }

    public void closeDrawers() {
        this.mDrawerLayout.closeDrawers();
    }

    public NavBarItems.PreferenceItem getItem(int position) {
        return this.mAdapter.getItem(position);
    }

    public int getItemCount() {
        return this.mAdapter.getCount();
    }

    private static final class ItemAdapter extends ArrayAdapter<NavBarItems.PreferenceItem> {
        private static final Typeface FONT = Typeface.create("sans-serif-condensed", 0);
        private final ArrayList<NavBarItems.PreferenceItem> items;
        private final String currentTheme;

        ItemAdapter(Context context, ArrayList<NavBarItems.PreferenceItem> objects, String currentTheme) {
            super(context, R.layout.adapter_item, objects);
            this.items = objects;
            this.currentTheme = currentTheme;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = LayoutInflater.from(getContext()).inflate(R.layout.adapter_item, null);
            }
            NavBarItems.PreferenceItem item = this.items.get(position);
            if (item != null) {
                ImageView icon = (ImageView) v.findViewById(R.id.icon);
                TextView text = (TextView) v.findViewById(R.id.text);
                text.setTypeface(FONT);
                if (icon != null) {
                    icon.setImageResource(item.drawable);
                }
                if (text != null) {
                    text.setText(getContext().getString(item.content));
                    text.setTextColor(getContext().getResources().getColorStateList(
                            ThemeHelper.THEME_DARK.equals(this.currentTheme) ? R.drawable.textview_drawer_dark : R.drawable.textview_drawer));
                }
            }
            return v;
        }
    }
}