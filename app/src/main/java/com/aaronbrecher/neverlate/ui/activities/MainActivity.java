package com.aaronbrecher.neverlate.ui.activities;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.aaronbrecher.neverlate.AppExecutors;
import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.Utils.SystemUtils;
import com.aaronbrecher.neverlate.interfaces.NavigationControl;
import com.aaronbrecher.neverlate.ui.controllers.MainActivityController;
import com.aaronbrecher.neverlate.ui.fragments.EventListFragment;
import com.aaronbrecher.neverlate.viewmodels.MainActivityViewModel;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

import javax.inject.Inject;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import static com.aaronbrecher.neverlate.Constants.PERMISSIONS_REQUEST_CODE;

public class MainActivity extends AppCompatActivity implements NavigationControl {
    //TODO with current refactoring need to figure out where to set up ActivityRecoginition
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String SHOW_ALL_EVENTS_KEY = "should-show-all-events";
    private static MutableLiveData<Boolean> finishedLoading;
    private Menu mMenu;

    public static void setFinishedLoading(boolean finished) {
        if (finishedLoading == null) finishedLoading = new MutableLiveData<>();
        finishedLoading.setValue(finished);
    }

    public static MutableLiveData<Boolean> getFinishedLoading() {
        if (finishedLoading == null) finishedLoading = new MutableLiveData<>();
        return finishedLoading;
    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            Log.i(TAG, "onLocationResult: recieved");
            mController.doCalendarUpdate();
            mController.createRecurringCalendarCheck();
            mController.setUpActivityMonitoring();
        }
    };

    @Inject
    ViewModelProvider.Factory mViewModelFactory;
    @Inject
    SharedPreferences mSharedPreferences;
    @Inject
    FusedLocationProviderClient mLocationProviderClient;
    @Inject
    AppExecutors mAppExecutors;

    MainActivityViewModel mViewModel;
    private FrameLayout mHostFragment;
    private ProgressBar mLoadingIcon;
    private FloatingActionButton mFab;
    private DrawerLayout mDrawerLayout;
    private MainActivityController mController;
    private boolean shouldShowAllEvents = false;
    private boolean mNotSnoozed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_Drawer);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((NeverLateApp) getApplication())
                .getAppComponent()
                .inject(this);
        mFab = findViewById(R.id.event_list_fab);
        mHostFragment = findViewById(R.id.nav_host_fragment);
        mLoadingIcon = findViewById(R.id.loading_icon);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        mViewModel = ViewModelProviders.of(this, mViewModelFactory)
                .get(MainActivityViewModel.class);
        NavController navController = Navigation.findNavController(mHostFragment);
        mController = new MainActivityController(this, navController);

        setUpDrawerDestinations();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        //NavigationUI.setupActionBarWithNavController(this, navController);
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);
        mController.setUpNotificationChannel();
        mController.checkIfUpdateNeeded();
        mController.setupRateThisApp();
        checkForCalendarApp();


        shouldShowAllEvents = false;
        mViewModel.setShouldShowAllEvents(false);
        //The app is not snoozed either if there is no unix time in the snooze prefs or even if there is
        //if the snooze is only a notification snooze
        mNotSnoozed = mSharedPreferences.getLong(Constants.SNOOZE_PREFS_KEY, Constants.ROOM_INVALID_LONG_VALUE) == Constants.ROOM_INVALID_LONG_VALUE
                || mSharedPreferences.getBoolean(Constants.SNOOZE_ONLY_NOTIFICATIONS_PREFS_KEY, false);
        if (!SystemUtils.hasPermissions(this)) {
            SystemUtils.requestCalendarAndLocationPermissions(this, findViewById(R.id.main_container));
            //only create calendar check if there is no snooze i.e. the value saved to prefs is -1
        } else if (mNotSnoozed) {
            mController.createRecurringCalendarCheck();
            mController.setUpActivityMonitoring();
        }
        mController.checkLocationSettings();

        mFab.setOnClickListener(v -> {
            Intent calIntent = new Intent(Intent.ACTION_INSERT);
            calIntent.setData(CalendarContract.Events.CONTENT_URI);
            startActivity(calIntent);
        });

        MobileAds.initialize(this, getString(R.string.admob_id));

        getFinishedLoading().observe(this, finishedLoading -> {
            if (finishedLoading != null && finishedLoading) hideLoadingIcon();
        });

        mViewModel.getAllCurrentEvents().observe(this, events -> {
            hideLoadingIcon();
            if (events != null && events.size() >= 1) {
                int currentDestinationId = mController.getCurrentFragment();
                if (currentDestinationId == R.id.noEventsFragment
                        || currentDestinationId == R.id.appSnoozedFragment
                        || currentDestinationId == R.id.noCalendarFragment){
                    mController.navigateToDestination(R.id.eventListFragment);
                }
            }
        });
    }

    @SuppressLint({"MissingPermission", "ApplySharedPref"})
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            //if permissions are granted for the first time, assume data was not loaded into room and
            //do so now...
            if (SystemUtils.verifyPermissions(grantResults)) {
                LocationRequest request = new LocationRequest()
                        .setInterval(10)
                        .setNumUpdates(1)
                        .setExpirationDuration(15000)
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                mLocationProviderClient.requestLocationUpdates(request, mLocationCallback, null);
            } else {
                SystemUtils.requestCalendarAndLocationPermissions(this, findViewById(R.id.main_container));
                // TODO change this to Show image showing error with button to rerequest permissions...
            }
        } else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mMenu = menu;
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.main_activity_menu_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);
        FragmentManager fragmentManager = getSupportFragmentManager();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                EventListFragment fragment = (EventListFragment) fragmentManager.findFragmentById(R.id.eventListFragment);
                if (fragment == null) return false;
                fragment.filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                EventListFragment fragment = (EventListFragment) fragmentManager.findFragmentById(R.id.eventListFragment);
                if (fragment == null) return false;
                fragment.filter(newText);
                return false;
            }
        });
        return true;
    }

    private void setUpDrawerDestinations() {
        NavigationView navView = findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();
            switch (id) {
                case R.id.drawer_home:
                    mController.navigateToDestination(R.id.eventListFragment);
                    toggleOptionsMenu(true);
                    mDrawerLayout.closeDrawers();
                    return true;
                case R.id.drawer_settings:
                    mController.navigateToDestination(R.id.settingsFragment);
                    toggleOptionsMenu(false);
                    mDrawerLayout.closeDrawers();
                    return true;
                case R.id.drawer_analyze:
                    mController.navigateToDestination(R.id.conflictAnalysisFragment);
                    showAnalyzeMenu();
                    mDrawerLayout.closeDrawers();
                    return true;
                case R.id.drawer_subscription:
                    mController.navigateToDestination(R.id.subscriptionFragment);
                    toggleOptionsMenu(false);
                    mDrawerLayout.closeDrawers();
                    return true;
                case R.id.drawer_snooze:
                    mController.navigateToDestination(R.id.snoozeFragment);
                    toggleOptionsMenu(false);
                    mDrawerLayout.closeDrawers();
                    return true;
                case R.id.drawer_privacy:
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Constants.PRIVACY_POLICY_URI);
                    startActivity(intent);
                    return true;
            }
            return false;
        });
    }

    private void toggleOptionsMenu(boolean shouldShow) {
        if (mMenu != null) {
            mMenu.setGroupVisible(R.id.main_activity_menu, shouldShow);
            mMenu.setGroupVisible(R.id.analyze_menu, false);
            if (shouldShow) mFab.show();
            else mFab.hide();
        }
    }

    private void showAnalyzeMenu() {
        if (mMenu != null) {
            toggleOptionsMenu(false);
            mMenu.setGroupVisible(R.id.analyze_menu, true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.main_activity_menu_sync:
                if (mSharedPreferences.getLong(Constants.SNOOZE_PREFS_KEY, Constants.ROOM_INVALID_LONG_VALUE) != Constants.ROOM_INVALID_LONG_VALUE
                        && !mSharedPreferences.getBoolean(Constants.SNOOZE_ONLY_NOTIFICATIONS_PREFS_KEY, false)) {
                    Toast.makeText(this, R.string.refresh_pressed_on_snooze, Toast.LENGTH_SHORT).show();
                    return true;
                }
                if (SystemUtils.isConnected(this)) {
                    showLoadingIcon();
                    mController.doCalendarUpdate();
                } else {
                    showNoConnectionSnackbar();
                }
                break;
            case R.id.main_activity_menu_show_all:
                shouldShowAllEvents = true;
                mViewModel.setShouldShowAllEvents(true);
                mController.navigateToDestination(R.id.eventListFragment);
                return true;
            case R.id.main_activity_menu_show_location_only:
                shouldShowAllEvents = false;
                mViewModel.setShouldShowAllEvents(false);
                return true;
            case R.id.analyze_refresh:
                if (SystemUtils.isConnected(this)) {
                    //Toast.makeText(this, "This feature is currently disabled while we track down the issues involving it", Toast.LENGTH_LONG).show();
                    //Todo when the bug is fixed uncomment
                    showLoadingIcon();
                    mController.analyzeEvents();
                    return true;
                }
        }
        return super.onOptionsItemSelected(item);
    }

    private void showNoConnectionSnackbar() {
        Snackbar.make(mHostFragment, R.string.refresh_error_no_connection, Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(R.string.launch_data_settings_button), v -> {
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setComponent(
                            new ComponentName(
                                    "com.android.settings",
                                    "com.android.settings.Settings$DataUsageSummaryActivity"
                            ));
                    startActivity(intent);
                })
                .show();
    }

    public void hideLoadingIcon() {
        mLoadingIcon.setVisibility(View.GONE);
        mHostFragment.setVisibility(View.VISIBLE);
    }

    private void showLoadingIcon() {
        mHostFragment.setVisibility(View.GONE);
        mLoadingIcon.setVisibility(View.VISIBLE);
    }

    private void checkForCalendarApp() {
        Intent calIntent = new Intent(Intent.ACTION_INSERT);
        calIntent.setData(CalendarContract.Events.CONTENT_URI);
        if (getPackageManager().queryIntentActivities(calIntent, 0).isEmpty()) {
            mFab.hide();
            mController.navigateToDestination(R.id.noCalendarFragment);
        }
    }

    public void showUpdateSnackbar() {
        Snackbar.make(mHostFragment, R.string.version_mismatch_snackbar, Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.version_mismatch_snackbar_update_button), v -> {
                    String appPackageName = getPackageName();
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                    } catch (android.content.ActivityNotFoundException e) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                    }
                }).show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SHOW_ALL_EVENTS_KEY, shouldShowAllEvents);
    }


    @Override
    public void navigateToDestination(int destination) {
        if (destination == R.id.snoozeFragment) mFab.hide();
        mController.navigateToDestination(destination);
    }
}
