package com.aaronbrecher.neverlate.ui.activities

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast

import com.aaronbrecher.neverlate.AppExecutors
import com.aaronbrecher.neverlate.Constants
import com.aaronbrecher.neverlate.NeverLateApp
import com.aaronbrecher.neverlate.R
import com.aaronbrecher.neverlate.Utils.SystemUtils
import com.aaronbrecher.neverlate.interfaces.NavigationControl
import com.aaronbrecher.neverlate.ui.controllers.MainActivityController
import com.aaronbrecher.neverlate.ui.fragments.EventListFragment
import com.aaronbrecher.neverlate.viewmodels.MainActivityViewModel
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar

import javax.inject.Inject

import androidx.annotation.NonNull
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.Navigation

import com.aaronbrecher.neverlate.Constants.PERMISSIONS_REQUEST_CODE

class MainActivity : AppCompatActivity(), NavigationControl {
    private lateinit var mMenu: Menu

    private var mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)
            Log.i(TAG, "onLocationResult: recieved")
            mController.doCalendarUpdate()
            mController.createRecurringCalendarCheck()
            mController.setUpActivityMonitoring()
        }
    }

    @Inject
    internal lateinit var mViewModelFactory: ViewModelProvider.Factory
    @Inject
    internal lateinit var mSharedPreferences: SharedPreferences
    @Inject
    internal lateinit var mLocationProviderClient: FusedLocationProviderClient
    @Inject
    internal lateinit var mAppExecutors: AppExecutors

    private lateinit var mViewModel: MainActivityViewModel
    private lateinit var mHostFragment: FrameLayout
    private lateinit var mLoadingIcon: ProgressBar
    private lateinit var mFab: FloatingActionButton
    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var mController: MainActivityController
    private var shouldShowAllEvents = false
    private lateinit var homeDrawable: DrawerArrowDrawable
    private var mNotSnoozed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_Drawer)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        (application as NeverLateApp)
                .appComponent
                .inject(this)
        mFab = findViewById(R.id.event_list_fab)
        mHostFragment = findViewById(R.id.nav_host_fragment)
        mLoadingIcon = findViewById(R.id.loading_icon)
        mDrawerLayout = findViewById(R.id.drawer_layout)
        mViewModel = ViewModelProviders.of(this, mViewModelFactory)
                .get(MainActivityViewModel::class.java)
        val navController = Navigation.findNavController(mHostFragment)
        mController = MainActivityController(this, navController)

        setUpDrawerDestinations()
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val actionbar = supportActionBar
        actionbar?.setDisplayHomeAsUpEnabled(true)
        homeDrawable = DrawerArrowDrawable(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            homeDrawable.color = getColor(R.color.material_light_white)
        }
        actionbar?.setHomeAsUpIndicator(homeDrawable)
        //        NavigationUI.setupActionBarWithNavController(this, navController, mDrawerLayout);
        //        NavigationUI.setupWithNavController((NavigationView) findViewById(R.id.nav_view), navController);
        mController.setUpNotificationChannel()
        mController.checkIfUpdateNeeded()
        mController.setupRateThisApp()
        checkForCalendarApp()


        shouldShowAllEvents = false
        mViewModel.setShouldShowAllEvents(false)
        //The app is not snoozed either if there is no unix time in the snooze prefs or even if there is
        //if the snooze is only a notification snooze
        mNotSnoozed = mSharedPreferences.getLong(Constants.SNOOZE_PREFS_KEY, Constants.ROOM_INVALID_LONG_VALUE) == Constants.ROOM_INVALID_LONG_VALUE
                || mSharedPreferences.getBoolean(Constants.SNOOZE_ONLY_NOTIFICATIONS_PREFS_KEY, false)
        if (!SystemUtils.hasPermissions(this)) {
            SystemUtils.requestCalendarAndLocationPermissions(this, findViewById(R.id.main_container))
            //only create calendar check if there is no snooze i.e. the value saved to prefs is -1
        } else if (mNotSnoozed) {
            mController.createRecurringCalendarCheck()
            mController.setUpActivityMonitoring()
        }
        mController.checkLocationSettings()

        mFab.setOnClickListener {
            val calIntent = Intent(Intent.ACTION_INSERT)
            calIntent.data = CalendarContract.Events.CONTENT_URI
            startActivity(calIntent)
        }

        MobileAds.initialize(this, getString(R.string.admob_id))

        getFinishedLoading().observe(this, Observer { finishedLoading -> if (finishedLoading != null && finishedLoading) hideLoadingIcon() })

        mViewModel.allCurrentEvents.observe(this, Observer { events ->
            hideLoadingIcon()
            if (events != null && events.isNotEmpty()) {
                mController.navigateToDestination(R.id.eventListFragment)
            }
        })
    }

    @SuppressLint("MissingPermission", "ApplySharedPref")
    override fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String>, @NonNull grantResults: IntArray) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            //if permissions are granted for the first time, assume data was not loaded into room and
            //do so now...
            if (SystemUtils.verifyPermissions(grantResults)) {
                val request = LocationRequest()
                        .setInterval(10)
                        .setNumUpdates(1)
                        .setExpirationDuration(15000)
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                mLocationProviderClient.requestLocationUpdates(request, mLocationCallback, null)
            } else {
                SystemUtils.requestCalendarAndLocationPermissions(this, findViewById(R.id.main_container))
                // TODO change this to Show image showing error with button to rerequest permissions...
            }
        } else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        mMenu = menu
        menuInflater.inflate(R.menu.main_activity_menu, menu)
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu.findItem(R.id.main_activity_menu_search).actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.maxWidth = Integer.MAX_VALUE
        val fragmentManager = supportFragmentManager
        //TODO search is not working figure out why
//        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
//            override fun onQueryTextSubmit(query: String): Boolean {
//                val fragment = fragmentManager.findFragmentById(R.id.eventListFragment) as EventListFragment
//                fragment.filter(query)
//                return false
//            }
//
//            override fun onQueryTextChange(newText: String): Boolean {
//                val fragment = fragmentManager.findFragmentById(R.id.eventListFragment) as EventListFragment
//                fragment.filter(newText)
//                return false
//            }
//        })
        return true
    }

    private fun setUpDrawerDestinations() {
        val navView = findViewById<NavigationView>(R.id.nav_view)
        navView.setNavigationItemSelectedListener { menuItem ->
            val id = menuItem.itemId
            when (id) {
                R.id.drawer_home -> {
                    mController.navigateToDestination(R.id.eventListFragment)
                    toggleOptionsMenu(true)
                    mDrawerLayout.closeDrawers()
                    true
                }
                R.id.drawer_settings -> {
                    mController.navigateToDestination(R.id.settingsFragment)
                    toggleOptionsMenu(false)
                    mDrawerLayout.closeDrawers()
                    true
                }
                R.id.drawer_analyze -> {
                    mController.navigateToDestination(R.id.conflictAnalysisFragment)
                    showAnalyzeMenu()
                    mDrawerLayout.closeDrawers()
                    true
                }
                R.id.drawer_subscription -> {
                    mController.navigateToDestination(R.id.subscriptionFragment)
                    toggleOptionsMenu(false)
                    mDrawerLayout.closeDrawers()
                    true
                }
                R.id.drawer_snooze -> {
                    mController.navigateToDestination(R.id.snoozeFragment)
                    toggleOptionsMenu(false)
                    mDrawerLayout.closeDrawers()
                    true
                }
                R.id.drawer_privacy -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Constants.PRIVACY_POLICY_URI
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleOptionsMenu(shouldShow: Boolean) {
        mMenu.setGroupVisible(R.id.main_activity_menu, shouldShow)
        mMenu.setGroupVisible(R.id.analyze_menu, false)
        if (shouldShow)
            mFab.show()
        else
            mFab.hide()
    }

    private fun showAnalyzeMenu() {
        toggleOptionsMenu(false)
        mMenu.setGroupVisible(R.id.analyze_menu, true)
    }

    override fun onBackPressed() {
        if(mController.currentFragment.let { it == R.id.eventListFragment || it == R.id.noEventsFragment }){
            super.onBackPressed()
        }
        mController.backToHome()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            android.R.id.home -> {
                if (mController.currentFragment == R.id.purchaseSubscriptionFragment) {
                    mController.navigateUp()
                    setHomeAsUpIcon(false)
                } else {
                    if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                        mDrawerLayout.closeDrawer(GravityCompat.START)
                    } else {
                        mDrawerLayout.openDrawer(GravityCompat.START)
                    }
                }
                return true
            }
            R.id.main_activity_menu_sync -> {
                if (mSharedPreferences.getLong(Constants.SNOOZE_PREFS_KEY, Constants.ROOM_INVALID_LONG_VALUE) != Constants.ROOM_INVALID_LONG_VALUE
                        && !mSharedPreferences!!.getBoolean(Constants.SNOOZE_ONLY_NOTIFICATIONS_PREFS_KEY, false)) {
                    Toast.makeText(this, R.string.refresh_pressed_on_snooze, Toast.LENGTH_SHORT).show()
                    return true
                }
                if (SystemUtils.isConnected(this)) {
                    showLoadingIcon()
                    mController.doCalendarUpdate()
                } else {
                    showNoConnectionSnackbar()
                }
            }
            R.id.main_activity_menu_show_all -> {
                shouldShowAllEvents = true
                mViewModel.setShouldShowAllEvents(true)
                mController.navigateToDestination(R.id.eventListFragment)
                return true
            }
            R.id.main_activity_menu_show_location_only -> {
                shouldShowAllEvents = false
                mViewModel.setShouldShowAllEvents(false)
                return true
            }
            R.id.analyze_refresh -> if (SystemUtils.isConnected(this)) {
                //Toast.makeText(this, "This feature is currently disabled while we track down the issues involving it", Toast.LENGTH_LONG).show();
                //Todo when the bug is fixed uncomment
                showLoadingIcon()
                mController.analyzeEvents()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showNoConnectionSnackbar() {
        Snackbar.make(mHostFragment, R.string.refresh_error_no_connection, Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(R.string.launch_data_settings_button)) {
                    val intent = Intent(Intent.ACTION_MAIN)
                    intent.component = ComponentName(
                            "com.android.settings",
                            "com.android.settings.Settings\$DataUsageSummaryActivity"
                    )
                    startActivity(intent)
                }
                .show()
    }

    fun hideLoadingIcon() {
        mLoadingIcon.visibility = View.GONE
        mHostFragment.visibility = View.VISIBLE
    }

    private fun showLoadingIcon() {
        mHostFragment.visibility = View.GONE
        mLoadingIcon.visibility = View.VISIBLE
    }

    private fun checkForCalendarApp() {
        val calIntent = Intent(Intent.ACTION_INSERT)
        calIntent.data = CalendarContract.Events.CONTENT_URI
        if (packageManager.queryIntentActivities(calIntent, 0).isEmpty()) {
            mFab.hide()
            mController.navigateToDestination(R.id.noCalendarFragment)
        }
    }

    fun showUpdateSnackbar() {
        Snackbar.make(mHostFragment, R.string.version_mismatch_snackbar, Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.version_mismatch_snackbar_update_button)) {
                    val appPackageName = packageName
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
                    } catch (e: android.content.ActivityNotFoundException) {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
                    }
                }.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(SHOW_ALL_EVENTS_KEY, shouldShowAllEvents)
    }


    override fun navigateToDestination(destination: Int) {
        if (destination == R.id.snoozeFragment) mFab.hide()
        mController.navigateToDestination(destination)
    }

    fun setHomeAsUpIcon(setToUp: Boolean) {
        val anim = if (setToUp) ValueAnimator.ofFloat(0f, 1f) else ValueAnimator.ofFloat(1f, 0f)
        anim.addUpdateListener { valueAnimator ->
            val slideOffset = valueAnimator.animatedValue as Float
            homeDrawable.progress = slideOffset
        }
        anim.interpolator = DecelerateInterpolator()
        anim.duration = 400
        anim.start()
    }

    companion object {
        //TODO with current refactoring need to figure out where to set up ActivityRecoginition
        private val TAG = MainActivity::class.java.simpleName
        private val SHOW_ALL_EVENTS_KEY = "should-show-all-events"
        private var finishedLoading: MutableLiveData<Boolean>? = null

        fun setFinishedLoading(finished: Boolean) {
            if (finishedLoading == null) finishedLoading = MutableLiveData()
            finishedLoading!!.value = finished
        }

        fun getFinishedLoading(): MutableLiveData<Boolean> {
            if (finishedLoading == null) finishedLoading = MutableLiveData()
            return finishedLoading!!
        }
    }
}
