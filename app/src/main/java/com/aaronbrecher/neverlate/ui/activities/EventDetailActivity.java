package com.aaronbrecher.neverlate.ui.activities;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.SharedPreferences;
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

public class EventDetailActivity extends AppCompatActivity implements OnMapReadyCallback {
    @Inject
    ViewModelProvider.Factory mViewModelFactory;
    @Inject
    SharedPreferences mSharedPreferences;
    private DetailActivityViewModel mViewModel;
    private Event mEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.event_detail_map);
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng latLng = LocationUtils.latlngFromAddress(this, mEvent.getLocation());
        float milesPerMinute = mSharedPreferences.getFloat(Constants.MILES_PER_MINUTE_PREFS_KEY, .5f);
        long relevantTime = MapUtils.determineRelevantTime(mEvent.getStartTime(), mEvent.getEndTime());
        CircleOptions circleOptions = new CircleOptions()
                .center(latLng)
                .radius(MapUtils.getFenceRadius(relevantTime ,milesPerMinute));
        googleMap.addMarker(new MarkerOptions().position(latLng)
                .title(mEvent.getTitle()));
        googleMap.addCircle(circleOptions);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,10.0f));
    }
}
