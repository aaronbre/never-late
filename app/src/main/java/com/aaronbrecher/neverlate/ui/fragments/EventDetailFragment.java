package com.aaronbrecher.neverlate.ui.fragments;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.Utils.GeofenceUtils;
import com.aaronbrecher.neverlate.Utils.LocationUtils;
import com.aaronbrecher.neverlate.databinding.EventDetailFragmentBinding;
import com.aaronbrecher.neverlate.dependencyinjection.AppComponent;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.GeofenceModel;
import com.aaronbrecher.neverlate.viewmodels.BaseViewModel;
import com.aaronbrecher.neverlate.viewmodels.DetailActivityViewModel;
import com.aaronbrecher.neverlate.viewmodels.MainActivityViewModel;
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
    private BaseViewModel mViewModel;
    private EventDetailFragmentBinding mBinding;
    private SupportMapFragment mMapFragment;
    private Event mEvent;
    private Marker mEventMarker;
    private Marker mLocationMarker;

    private Observer<Event> mEventObserver = new Observer<Event>() {
        @Override
        public void onChanged(@Nullable Event event) {
            mBinding.setEvent(event);
            mMapFragment.getMapAsync(EventDetailFragment.this);
            mEvent = event;
            String timeToLeave = DirectionsUtils.getTimeToLeaveHumanReadable(mEvent.getTimeTo(),
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
        mBinding = EventDetailFragmentBinding.inflate(inflater, container, false);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM d  h:mm a");
        mBinding.setFormatter(formatter);
        mMapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.event_detail_map);
        mViewModel.getEvent().observe(this, mEventObserver);
        return mBinding.getRoot();
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mViewModel.getGeofenceForKey(mEvent.getId()).observe(this, new Observer<GeofenceModel>() {
            @Override
            public void onChanged(@Nullable GeofenceModel geofenceModel) {
                addGeofenceToMap(googleMap);
            }
        });
    }

    private void addGeofenceToMap(GoogleMap googleMap) {
        //TODO NOT Working fix this
        if (mEventMarker != null) mEventMarker.remove();
        if (mLocationMarker != null) mLocationMarker.remove();
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        LatLng latLng = mEvent.getLocationLatlng();
        LatLng locLatLng = null;
        String location = mSharedPreferences.getString(Constants.USER_LOCATION_PREFS_KEY, "");
        if (!location.equals("")) {
            Location loc = LocationUtils.locationJsonToLocation(location);
            locLatLng = new LatLng(loc.getLatitude(), loc.getLongitude());
        }
        if (latLng != null) {
            mEventMarker = googleMap.addMarker(new MarkerOptions().position(latLng)
                    .title(mEvent.getTitle()));
            builder.include(mEventMarker.getPosition());
        }
        if (locLatLng != null) {
            mLocationMarker = googleMap.addMarker(new MarkerOptions().position(locLatLng)
                    .title("Your Location"));
            builder.include(mLocationMarker.getPosition());
        }
        if(latLng != null && locLatLng != null){

        }
        LatLngBounds bounds = builder.build();
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 200);
        googleMap.moveCamera(cu);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mEvent != null) {
            mViewModel.getGeofenceForKey(mEvent.getId()).removeObservers(this);
        }
    }
}
