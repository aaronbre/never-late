package com.aaronbrecher.neverlate.ui.controllers


import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build

import com.aaronbrecher.neverlate.Constants
import com.aaronbrecher.neverlate.R
import com.aaronbrecher.neverlate.Utils.BackgroundUtils
import com.aaronbrecher.neverlate.models.retrofitmodels.Version
import com.aaronbrecher.neverlate.ui.activities.MainActivity
import com.aaronbrecher.neverlate.ui.activities.NeedUpdateActivity
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.GooglePlayDriver
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.firebase.analytics.FirebaseAnalytics
import com.kobakei.ratethisapp.RateThisApp

import androidx.navigation.NavController
import com.aaronbrecher.neverlate.network.createRetrofitService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivityController(private val mActivity: MainActivity, private val mNavController: NavController) {

    private val mFirebaseJobDispatcher = FirebaseJobDispatcher(GooglePlayDriver(mActivity))

    val currentFragment: Int
        get() {
            val destination = mNavController.currentDestination
            return destination?.id ?: -1
        }

    init {
        val firebaseAnalytics = FirebaseAnalytics.getInstance(mActivity)
    }

    fun setUpActivityMonitoring() {
        mFirebaseJobDispatcher.mustSchedule(BackgroundUtils.setUpActivityRecognitionJob(mFirebaseJobDispatcher))
    }

    //This will only set the alarm if it wasn't already set there will be a
    //seperate broadcast reciever to schedule alarm after boot...
    fun createRecurringCalendarCheck() {
        val recurringCheckJob = BackgroundUtils.setUpPeriodicCalendarChecks(mFirebaseJobDispatcher)
        mFirebaseJobDispatcher.mustSchedule(recurringCheckJob)
    }

    fun doCalendarUpdate() {
        mFirebaseJobDispatcher.mustSchedule(BackgroundUtils.oneTimeCalendarUpdate(mFirebaseJobDispatcher))
    }

    fun analyzeEvents() {
        mFirebaseJobDispatcher.mustSchedule(BackgroundUtils.anaylzeSchedule(mFirebaseJobDispatcher))
    }

    fun checkLocationSettings() {
        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(LocationRequest().setExpirationDuration(Constants.TIME_TEN_MINUTES)
                        .setFastestInterval(Constants.TIME_TEN_MINUTES)
                        .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY))
        val settingsClient = LocationServices.getSettingsClient(mActivity)
        val task = settingsClient.checkLocationSettings(builder.build())
        task.addOnSuccessListener {

        }.addOnFailureListener { e ->
            if (e is ResolvableApiException) {
                try {
                    e.startResolutionForResult(mActivity, 1)
                } catch (sendEx: IntentSender.SendIntentException) {
                    sendEx.printStackTrace()
                }

            }
        }
    }

    fun setupRateThisApp() {
        RateThisApp.onCreate(mActivity)
        RateThisApp.showRateDialogIfNeeded(mActivity)
    }

    fun setUpNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = mActivity.getString(R.string.notification_channel_name)
            val description = mActivity.getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val notificationChannel = NotificationChannel(Constants.NOTIFICATION_CHANNEL_ID, channelName, importance)
            notificationChannel.description = description
            val manager = mActivity.getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(notificationChannel)
        }
    }


    fun checkIfUpdateNeeded() {
        try {
            val currentVersion = mActivity.packageManager.getPackageInfo(mActivity.packageName, 0).versionCode
            val service = createRetrofitService()
            service.queryVersionNumber(currentVersion).enqueue(object : Callback<Version> {
                override fun onResponse(call: Call<Version>, response: Response<Version>) {
                    val v = response.body() ?: return
                    val latestVersion = v.version
                    if (currentVersion < latestVersion) {
                        mActivity.showUpdateSnackbar()
                    } else if (mActivity.getString(R.string.version_invalid) == v.message || v.needsUpdate!!) {
                        mFirebaseJobDispatcher.cancel(Constants.FIREBASE_JOB_SERVICE_CHECK_CALENDAR_CHANGED)
                        mFirebaseJobDispatcher.cancel(Constants.FIREBASE_JOB_SERVICE_SETUP_ACTIVITY_RECOG)
                        val intent = Intent(mActivity, NeedUpdateActivity::class.java)
                        mActivity.startActivity(intent)
                    }
                }

                override fun onFailure(call: Call<Version>, t: Throwable) {
                    t.printStackTrace()
                }
            })
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

    }

    fun backToHome() {
        mNavController.popBackStack(R.id.eventListFragment, true)
        mActivity.setTitle(R.string.list_title)
    }

    fun navigateUp() {
        mNavController.navigateUp()
    }

    fun navigateToDestination(id: Int) {
        mActivity.hideLoadingIcon()
        mNavController.navigate(id)

    }

}
