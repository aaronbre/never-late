package com.aaronbrecher.neverlate.ui.activities;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
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

import com.aaronbrecher.neverlate.AppExecutors;
import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.Utils.BackgroundUtils;
import com.aaronbrecher.neverlate.Utils.SystemUtils;
import com.aaronbrecher.neverlate.interfaces.ListItemClickListener;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.retrofitmodels.Version;
import com.aaronbrecher.neverlate.network.AppApiService;
import com.aaronbrecher.neverlate.network.AppApiUtils;
import com.aaronbrecher.neverlate.ui.controllers.MainActivityController;
import com.aaronbrecher.neverlate.ui.fragments.EventListFragment;
import com.aaronbrecher.neverlate.viewmodels.MainActivityViewModel;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;

import javax.inject.Inject;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.aaronbrecher.neverlate.Constants.PERMISSIONS_REQUEST_CODE;

public class MainActivity extends AppCompatActivity implements ListItemClickListener {
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
    private DrawerLayout mDrawerLayout;
    private NavController mNavController;
    private MainActivityController mController;
    private boolean shouldShowAllEvents = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_Drawer);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((NeverLateApp) getApplication())
                .getAppComponent()
                .inject(this);
        FloatingActionButton fab = findViewById(R.id.event_list_fab);
        mHostFragment = findViewById(R.id.nav_host_fragment);
        mLoadingIcon = findViewById(R.id.loading_icon);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        mViewModel = ViewModelProviders.of(this, mViewModelFactory)
                .get(MainActivityViewModel.class);
        mNavController = Navigation.findNavController(mHostFragment);
        mController = new MainActivityController(this);

        setUpDrawerDestinations();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);


        mController.setUpNotificationChannel();
        mController.checkIfUpdateNeeded();
        checkForCalendarApp(fab);

        if (savedInstanceState != null && savedInstanceState.containsKey(SHOW_ALL_EVENTS_KEY)) {
            shouldShowAllEvents = savedInstanceState.getBoolean(SHOW_ALL_EVENTS_KEY, false);
            mViewModel.setShouldShowAllEvents(shouldShowAllEvents);
        } else {
            shouldShowAllEvents = false;
            mViewModel.setShouldShowAllEvents(false);
        }
        if (!SystemUtils.hasPermissions(this)) {
            SystemUtils.requestCalendarAndLocationPermissions(this, findViewById(R.id.main_container));
        } else {
            mController.createRecurringCalendarCheck();
            mController.setUpActivityMonitoring();
        }
        mController.checkLocationSettings();

        mViewModel.getAllCurrentEvents().observe(this, events -> {
            hideLoadingIcon();
            if (events == null || events.size() < 1) {
                mNavController.navigate(R.id.noEventsFragment);
            } else {
                mNavController.navigate(R.id.eventListFragment);
            }
        });

        fab.setOnClickListener(v -> {
            Intent calIntent = new Intent(Intent.ACTION_INSERT);
            calIntent.setData(CalendarContract.Events.CONTENT_URI);
            startActivity(calIntent);
        });

        MobileAds.initialize(this, getString(R.string.admob_id));
        getFinishedLoading().observe(this, finishedLoading -> {
            if (finishedLoading != null && finishedLoading) hideLoadingIcon();
        });
    }

    public void loadNoEventsFragment() {
        mNavController.navigate(R.id.noEventsFragment);
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
                EventListFragment fragment = (EventListFragment) fragmentManager.findFragmentByTag(Constants.EVENT_LIST_TAG);
                if (fragment == null) return false;
                fragment.filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                EventListFragment fragment = (EventListFragment) fragmentManager.findFragmentByTag(Constants.EVENT_LIST_TAG);
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
                    mNavController.navigate(R.id.eventListFragment);
                    toggleOptionsMenu(true);
                    mDrawerLayout.closeDrawers();
                    return true;
                case R.id.drawer_settings:
                    mNavController.navigate(R.id.settingsFragment);
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
                return true;
            case R.id.main_activity_menu_show_location_only:
                shouldShowAllEvents = false;
                mViewModel.setShouldShowAllEvents(false);
                return true;
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

    @Override
    public void onListItemClick(Event event) {
        Intent intent = new Intent(this, EventDetailActivity.class);
        intent.putExtra(Constants.EVENT_DETAIL_INTENT_EXTRA, Event.convertEventToJson(event));
        startActivity(intent);

    }

    private void hideLoadingIcon() {
        mLoadingIcon.setVisibility(View.GONE);
        mHostFragment.setVisibility(View.VISIBLE);
    }

    private void showLoadingIcon() {
        mHostFragment.setVisibility(View.GONE);
        mLoadingIcon.setVisibility(View.VISIBLE);
    }

    private void checkForCalendarApp(FloatingActionButton fab) {
        Intent calIntent = new Intent(Intent.ACTION_INSERT);
        calIntent.setData(CalendarContract.Events.CONTENT_URI);
        if (getPackageManager().queryIntentActivities(calIntent, 0).isEmpty()) {
            fab.hide();
            mNavController.navigate(R.id.noCalendarFragment);
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
}
