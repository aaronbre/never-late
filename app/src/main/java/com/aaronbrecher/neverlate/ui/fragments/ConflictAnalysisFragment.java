package com.aaronbrecher.neverlate.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.adapters.ConflictsListAdapter;
import com.aaronbrecher.neverlate.database.EventCompatibilityRepository;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.interfaces.NavigationControl;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import javax.inject.Inject;

public class ConflictAnalysisFragment extends Fragment {
    private RecyclerView mRecyclerView;
    private ConflictsListAdapter mAdapter;
    private InterstitialAd mInterstitialAd;
    private NavigationControl mNavController;

    @Inject
    EventsRepository mEventsRepository;
    @Inject
    EventCompatibilityRepository mCompatibilityRepository;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        NeverLateApp.getApp().getAppComponent().inject(this);
        try {
            mNavController = (NavigationControl) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    " must implement the ListItemClickListener interface");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_compatability, container, false);
        getActivity().setTitle(R.string.anaylize_title);
        mRecyclerView = rootView.findViewById(R.id.compatibility_list_rv);
        mAdapter = new ConflictsListAdapter(null, null, getActivity());
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mEventsRepository.queryAllCurrentTrackedEvents().observe(this, events -> mAdapter.setEvents(events));
        mCompatibilityRepository.queryCompatibility().observe(this, list ->{
            if(list == null || list.size() < 1){
                mNavController.navigateToDestination(R.id.conflictEmptyFragment);
                return;
            }else {
                mAdapter.setEventCompatibilities(list);
            }
        } );
        mInterstitialAd = new InterstitialAd(getActivity());
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        showAd();
    }

    @Override
    public void onDestroyView() {
        mEventsRepository.queryAllCurrentTrackedEvents().removeObservers(this);
        mCompatibilityRepository.queryCompatibility().removeObservers(this);
        mInterstitialAd = null;
        super.onDestroyView();
    }

    /**
     * Method to display an interstitial ad to the user. Currently will show an ad once for
     * each new event opened (if user closes app will restart the count). Possibly change this
     * to show only once on each app lifecycle
     */
    private void showAd() {
        mInterstitialAd = new InterstitialAd(getContext());
        mInterstitialAd.setAdUnitId(getString(R.string.ad_mob_interstitial_ad_unit));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                if(mInterstitialAd != null){
                    mInterstitialAd.show();
                }
            }
        });
    }
}
