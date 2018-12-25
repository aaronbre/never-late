package com.aaronbrecher.neverlate.ui.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.Utils.SystemUtils;
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

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

public class EventDetailMapFragment extends Fragment implements OnMapReadyCallback {

    @Inject
    ViewModelProvider.Factory mViewModelFactory;
    @Inject
    SharedPreferences mSharedPreferences;
    @Inject
    FusedLocationProviderClient mFusedLocationProviderClient;

    private BaseViewModel mViewModel;
    private Marker mEventMarker;
    private LatLng mUserLocationLatLng = null;
    private SupportMapFragment mMapFragment;
    private Event mEvent;

    private Observer<Event> mEventObserver = new Observer<Event>() {
        @Override
        public void onChanged(@Nullable Event event) {
            mMapFragment.getMapAsync(EventDetailMapFragment.this);
            mEvent = event;
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
        View rootView = inflater.inflate(R.layout.fragment_event_detail_map, container, false);
        mMapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.event_detail_map);
        mViewModel.getEvent().observe(this, mEventObserver);
        return rootView;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(final GoogleMap googleMap) {
        if (SystemUtils.hasLocationPermissions(getActivity())) {
            mFusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null)
                    mUserLocationLatLng = new LatLng(location.getLatitude(), location.getLongitude());
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
        if (!mEvent.getLocation().isEmpty()) {
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
        } else if (mUserLocationLatLng != null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mUserLocationLatLng, 10));
        }
    }
}
