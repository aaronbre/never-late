package com.aaronbrecher.neverlate.ui.activities;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.Utils.LocationUtils;
import com.aaronbrecher.neverlate.Utils.MapUtils;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.GeofenceModel;
import com.aaronbrecher.neverlate.ui.fragments.EventDetailFragment;
import com.aaronbrecher.neverlate.viewmodels.DetailActivityViewModel;
import com.aaronbrecher.neverlate.viewmodels.MainActivityViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import javax.inject.Inject;

public class EventDetailActivity extends AppCompatActivity{
    @Inject
    ViewModelProvider.Factory mViewModelFactory;
    @Inject
    SharedPreferences mSharedPreferences;
    private DetailActivityViewModel mViewModel;
    private Event mEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //TODO when opening the detail view force an update on the geofence(i.e. update just this fence)
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);
        ((NeverLateApp) getApplication())
                .getAppComponent()
                .inject(this);
        mViewModel = ViewModelProviders.of(this, mViewModelFactory).get(DetailActivityViewModel.class);
        Intent intent = getIntent();

        if (!intent.hasExtra(Constants.EVENT_DETAIL_INTENT_EXTRA)) {
            Toast.makeText(this, "Unable to load event please try again", Toast.LENGTH_LONG).show();
            finish();
        }
        mEvent = intent.getParcelableExtra(Constants.EVENT_DETAIL_INTENT_EXTRA);
        setTitle(mEvent.getTitle());
        mViewModel.setEvent(mEvent);
        EventDetailFragment eventDetailFragment = new EventDetailFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .add(R.id.event_detail_fragment_container, eventDetailFragment, Constants.EVENT_DETAIL_FRAGMENT_TAG)
                .commit();
    }
}
