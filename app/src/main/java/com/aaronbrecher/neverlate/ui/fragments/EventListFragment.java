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
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.adapters.EventListAdapter;
import com.aaronbrecher.neverlate.databinding.MainActivityListFragmentBinding;
import com.aaronbrecher.neverlate.dependencyinjection.AppComponent;
import com.aaronbrecher.neverlate.interfaces.ListItemClickListener;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.ui.activities.MainActivity;
import com.aaronbrecher.neverlate.viewmodels.MainActivityViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;

import java.util.List;

import javax.inject.Inject;

//TODO when the list becomes empty need to close the fragment and load the no-events-fragment, probably needs to be done in mainActivity via a interface
public class EventListFragment extends Fragment {

    ListItemClickListener mListItemClickListener;

    @Inject
    ViewModelProvider.Factory mViewModelFactory;
    @Inject
    FusedLocationProviderClient mLocationProviderClient;
    @Inject
    SharedPreferences mSharedPreferences;

    private MainActivityViewModel mViewModel;
    private EventListAdapter mListAdapter;
    private MainActivityListFragmentBinding mBinding;
    private MainActivity mActivity;

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
        AppComponent appComponent = NeverLateApp.getApp().getAppComponent();
        appComponent.inject(this);
        mActivity = (MainActivity) getActivity();
        mViewModel = ViewModelProviders.of(mActivity, mViewModelFactory).get(MainActivityViewModel.class);
        mListAdapter = new EventListAdapter(null, mActivity);
        mViewModel.getShouldShowAllEvents().observe(this, showAllEventsObserver);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = MainActivityListFragmentBinding.inflate(inflater, container,false);
        mBinding.eventListRv.setAdapter(mListAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(mActivity);
        mBinding.eventListRv.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mActivity, layoutManager.getOrientation());
        mBinding.eventListRv.addItemDecoration(dividerItemDecoration);
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
            if(events == null || events.size() == 0){
               mActivity.loadNoEventsFragment();
            }
            else {
                mListAdapter.swapLists(events);
            }
        }
    };

    private final Observer<Boolean> showAllEventsObserver = new Observer<Boolean>() {
        @Override
        public void onChanged(@Nullable Boolean shouldShow) {
            if(shouldShow != null && shouldShow){
                mViewModel.getAllCurrentEvents().removeObserver(eventsObserver);
                mViewModel.getAllEvents().observe(EventListFragment.this, eventsObserver);
            }else {
                mViewModel.getAllEvents().removeObserver(eventsObserver);
                mViewModel.getAllCurrentEvents().observe(EventListFragment.this, eventsObserver);
            }
        }
    };

    public void filter(CharSequence query){
        mListAdapter.getFilter().filter(query);
    }
}
