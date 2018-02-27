package com.example.xujia.posturemonitor.sensornode;

import com.example.xujia.posturemonitor.R;
import android.app.ActionBar;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xujia on 2018-02-20.
 */

public class ViewPagerActivity extends FragmentActivity {

    // private static final String TAG = "ViewPagerActivity";

    // GUI
    protected static ViewPagerActivity mThis = null;
    protected SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    protected int mResourceFragmentPager;
    protected int mResourceIdPager;
    private int mCurrentTab = 0;
    protected Menu optionsMenu;
    private MenuItem refreshItem;
    protected boolean mBusy;

    public ViewPagerActivity() {
        // Log.d(TAG, "construct");
        mThis = this;
        mBusy = false;
        refreshItem = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(mResourceFragmentPager);

        // With v7 we use getSupportActionBar(); to use getActionBar()
        // this line is needed in manifest file: android:theme="@android:style/Theme.Holo.Light"
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        ImageView view = (ImageView) findViewById(android.R.id.home);
        view.setPadding(10, 0, 20, 10);

        // Set up the ViewPager with the sections adapter
        mViewPager = (ViewPager) findViewById(mResourceIdPager);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int n) {
                // Log.d(TAG, "onPageSelected: " + n);
                actionBar.setSelectedNavigationItem(n);
            }
        });
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mSectionsPagerAdapter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Log.d(TAG, "onDestroy");
        mSectionsPagerAdapter = null;
    }

    @Override
    public void onBackPressed() {
        if (mCurrentTab != 0)
            getActionBar().setSelectedNavigationItem(0);
        else
            super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.optionsMenu = menu;
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.opt_about:
                onAbout();
                break;
            case R.id.opt_exit:
                Toast.makeText(this, "Exit...", Toast.LENGTH_SHORT).show();
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void onAbout() {
        final Dialog dialog = new AboutDialog(this);
        dialog.show();
    }

    protected void showBusyIndicator(final boolean busy) {
        if (optionsMenu != null) {
            refreshItem = optionsMenu.findItem(R.id.opt_progress);
            if (refreshItem != null) {
                if (!busy) {
                    refreshItem.setActionView(R.layout.frame_progress);
                } else {
                    refreshItem.setActionView(null);
                }
                refreshItem.setVisible(!busy);
            } else {
                // Log.e(TAG,"Refresh item not expanded");
            }
        } else {
            // Log.e(TAG,"Options not expanded");
        }
        mBusy = busy;
    }

    // Create a tab listener that is called when the user changes tabs
    ActionBar.TabListener tabListener = new ActionBar.TabListener() {
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
            int n = tab.getPosition();
            // Log.d(TAG, "onTabSelected: " + n);
            mCurrentTab = n;
            mViewPager.setCurrentItem(n);
        }

        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
            // int n = tab.getPosition();
            // Log.d(TAG, "onTabUnselected: " + n);
        }

        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
            // int n = tab.getPosition();
            // Log.d(TAG, "onTabReselected: " + n);
        }
    };

    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {
        private List<Fragment> mFragmentList;
        private List<String> mTitles;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            mFragmentList = new ArrayList<Fragment>();
            mTitles = new ArrayList<String>();
        }

        public void addSection(Fragment fragment, String title) {
            final ActionBar actionBar = getActionBar();
            mFragmentList.add(fragment);
            mTitles.add(title);
            actionBar.addTab(actionBar.newTab().setText(title).setTabListener(tabListener));
            notifyDataSetChanged();
            // Log.d(TAG, "Tab: " + title);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mTitles.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position < getCount()) {
                return mTitles.get(position);
            } else {
                return null;
            }
        }
    }

}
