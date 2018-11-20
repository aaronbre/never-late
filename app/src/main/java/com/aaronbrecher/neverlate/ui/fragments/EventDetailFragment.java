package com.aaronbrecher.neverlate.ui.fragments;

import android.annotation.SuppressLint;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.Utils.GeofenceUtils;
import com.aaronbrecher.neverlate.Utils.SystemUtils;
import com.aaronbrecher.neverlate.databinding.FragmentEventDetailBinding;
import com.aaronbrecher.neverlate.dependencyinjection.AppComponent;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.viewmodels.BaseViewModel;
import com.aaronbrecher.neverlate.viewmodels.DetailActivityViewModel;
import com.aaronbrecher.neverlate.viewmodels.MainActivityViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.threeten.bp.format.DateTimeFormatter;

import javax.inject.Inject;

public class EventDetailFragment extends Fragment implements OnMapReadyCallback {

    @Inject
    ViewModelProvider.Factory mViewModelFactory;
    @Inject
    SharedPreferences mSharedPreferences;

    @Inject
    FusedLocationProviderClient mFusedLocationProviderClient;
    private BaseViewModel mViewModel;
    private FragmentEventDetailBinding mBinding;
    private SupportMapFragment mMapFragment;
    private Event mEvent;
    private Marker mEventMarker;
    private LatLng mUserLocationLatLng = null;

    private Observer<Event> mEventObserver = new Observer<Event>() {
        @Override
        public void onChanged(@Nullable Event event) {
            mBinding.setEvent(event);
            mMapFragment.getMapAsync(EventDetailFragment.this);
            mEvent = event;
            String timeToLeave = DirectionsUtils.getTimeToLeaveHumanReadable(mEvent.getDrivingTime(),
                    GeofenceUtils.determineRelevantTime(mEvent.getStartTime(), mEvent.getEndTime()));
            String formatted = getString(R.string.event_detail_leave_time, timeToLeave);
            mBinding.eventDetailLeaveTime.setText(formatted);
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        AppComponent appComponent = ((NeverLateApp) getActivity().getApplication()).getAppComponent();
        appComponent.inject(this);
        if (getResources().getBoolean(R.bool.is_tablet)) {
            mViewModel = ViewModelProviders.of(getActivity(), mViewModelFactory).get(MainActivityViewModel.class);
        } else {
            mViewModel = ViewModelProviders.of(getActivity(), mViewModelFactory).get(DetailActivityViewModel.class);
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentEventDetailBinding.inflate(inflater, container, false);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM d  h:mm a");
        mBinding.setFormatter(formatter);
        mMapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.event_detail_map);
        mViewModel.getEvent().observe(this, mEventObserver);
        return mBinding.getRoot();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(final GoogleMap googleMap) {
        if(SystemUtils.hasLocationPermissions(getActivity())){
            mFusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) mUserLocationLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                setUpMap(googleMap);
            });
        } else {
            setUpMap(googleMap);
        }
    }

    @SuppressLint("MissingPermission")
    private void setUpMap(GoogleMap googleMap) {
        if (mEventMarker != null) mEventMarker.remove();
        googleMap.setMyLocationEnabled(true);
        if(!mEvent.getLocation().isEmpty()){
            LatLng latLng = mEvent.getLocationLatlng();
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            if (latLng != null) {
                mEventMarker = googleMap.addMarker(new MarkerOptions().position(latLng)
                        .title(mEvent.getTitle()));
                builder.include(latLng);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));
            }

            if (mUserLocationLatLng != null) {
                builder.include(mUserLocationLatLng);
            }
            googleMap.setOnMapLoadedCallback(() -> {
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(builder.build(), 200);
                googleMap.animateCamera(cu);
            });
        }else if(mUserLocationLatLng != null){
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mUserLocationLatLng,10));
        }
    }
}
