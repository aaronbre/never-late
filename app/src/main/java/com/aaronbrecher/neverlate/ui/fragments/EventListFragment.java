package com.aaronbrecher.neverlate.ui.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.Utils.LocationUtils;
import com.aaronbrecher.neverlate.Utils.PermissionUtils;
import com.aaronbrecher.neverlate.adapters.EventListAdapter;
import com.aaronbrecher.neverlate.databinding.MainActivityListFragmentBinding;
import com.aaronbrecher.neverlate.dependencyinjection.AppComponent;
import com.aaronbrecher.neverlate.interfaces.ListItemClickListener;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.ui.activities.MainActivity;
import com.aaronbrecher.neverlate.viewmodels.MainActivityViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.PendingResult;
import com.google.maps.model.DirectionsResult;

import java.util.List;

import javax.inject.Inject;

public class EventListFragment extends Fragment {

    ListItemClickListener mListItemClickListener;

    @Inject
    ViewModelProvider.Factory mViewModelFactory;
    @Inject
    FusedLocationProviderClient mLocationProviderClient;

    private MainActivityViewModel mViewModel;
    private EventListAdapter mListAdapter;
    private MainActivityListFragmentBinding mBinding;
    private MainActivity mActivity;
    private Location mLocation;

    @SuppressLint("MissingPermission")
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListItemClickListener = (ListItemClickListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    " must implement the ListItemClickListener interface");
        }
        AppComponent appComponent = ((NeverLateApp) getActivity().getApplication()).getAppComponent();
        appComponent.inject(this);
        mViewModel = ViewModelProviders.of(getActivity(), mViewModelFactory).get(MainActivityViewModel.class);
        mActivity = (MainActivity) getActivity();
        mListAdapter = new EventListAdapter(null, null, mActivity);
        mViewModel.getEventsWithLocation().observe(this, eventsObserver);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = MainActivityListFragmentBinding.inflate(inflater, container,false);
        mBinding.eventListRv.setAdapter(mListAdapter);
        mBinding.eventListRv.setLayoutManager(new LinearLayoutManager(mActivity));
        return mBinding.getRoot();
    }

    // unsubscribe observer here so as not to have multiple observers
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mViewModel.getAllCurrentEvents().removeObserver(eventsObserver);
    }

    private final Observer<List<Event>> eventsObserver = new Observer<List<Event>>() {
        @Override
        public void onChanged(@Nullable List<Event> events) {
            mListAdapter.swapLists(events);
        }
    };

}
