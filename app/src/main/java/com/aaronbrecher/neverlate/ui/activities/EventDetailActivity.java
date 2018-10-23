package com.aaronbrecher.neverlate.ui.activities;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.CalendarContract;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.Utils.GeofenceUtils;
import com.aaronbrecher.neverlate.backgroundservices.StartJobIntentServiceBroadcastReceiver;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.ui.fragments.EventDetailFragment;
import com.aaronbrecher.neverlate.ui.fragments.PassedEventFragment;
import com.aaronbrecher.neverlate.viewmodels.DetailActivityViewModel;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import java.util.ArrayList;

import javax.inject.Inject;

public class EventDetailActivity extends AppCompatActivity{
    @Inject
    ViewModelProvider.Factory mViewModelFactory;
    @Inject
    SharedPreferences mSharedPreferences;
    private DetailActivityViewModel mViewModel;
    private AdView mAdView;
    private InterstitialAd mInterstitialAd;
    private Event mEvent;
    private Intent mIntent;

    private static ArrayList<Integer> eventsViewed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);
        ((NeverLateApp) getApplication())
                .getAppComponent()
                .inject(this);
        mViewModel = ViewModelProviders.of(this, mViewModelFactory).get(DetailActivityViewModel.class);
        mIntent = getIntent();

        if (!mIntent.hasExtra(Constants.EVENT_DETAIL_INTENT_EXTRA)) {
            Toast.makeText(this, R.string.event_not_found_toast_text, Toast.LENGTH_LONG).show();
            finish();
        }
        //Samsung devices throw error when parsing this line fixes it
        mEvent = Event.convertJsonToEvent(mIntent.getStringExtra(Constants.EVENT_DETAIL_INTENT_EXTRA));
        if(mEvent == null){
            Toast.makeText(this, R.string.event_not_found_toast_text, Toast.LENGTH_LONG).show();
            finish();
        }
        FloatingActionButton fab = findViewById(R.id.detail_edit_fab);
        setTitle(mEvent.getTitle());
        mViewModel.setEvent(mEvent);
        if(GeofenceUtils.eventIsPassedCurrentTime(mEvent.getEndTime())){
            //show message that event has already passed.
            PassedEventFragment passedEventFragment = new PassedEventFragment();
            fab.setVisibility(View.GONE);
            getSupportFragmentManager().beginTransaction().replace(R.id.event_detail_fragment_container, passedEventFragment).commit();
        }
        else {
            EventDetailFragment eventDetailFragment = new EventDetailFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .add(R.id.event_detail_fragment_container, eventDetailFragment, Constants.EVENT_DETAIL_FRAGMENT_TAG)
                    .commit();
        }
        fab.setOnClickListener(v -> {
            int id = mEvent.getId();
            Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(uri);
            startActivity(intent);
        });
        showAd();

    }

    /**
     * Method to display an interstitial ad to the user. Currently will show an ad once for
     * each new event opened (if user closes app will restart the count). Possibly change this
     * to show only once on each app lifecycle
     */
    private void showAd() {
        if(eventsViewed == null) eventsViewed = new ArrayList<>();
        Integer eventId = mEvent.getId();
        if(!eventsViewed.contains(eventId)){
           eventsViewed.add(eventId);
            mInterstitialAd = new InterstitialAd(this);
            mInterstitialAd.setAdUnitId("ca-app-pub-3940256099942544/1033173712");
            mInterstitialAd.loadAd(new AdRequest.Builder().build());

            mInterstitialAd.setAdListener(new AdListener(){
                @Override
                public void onAdLoaded() {
                    mInterstitialAd.show();
                }
            });
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mIntent = intent;
    }
}
