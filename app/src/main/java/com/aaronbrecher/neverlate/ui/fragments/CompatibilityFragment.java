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
import com.aaronbrecher.neverlate.adapters.CompatibilityListAdapter;
import com.aaronbrecher.neverlate.database.EventCompatibilityRepository;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import javax.inject.Inject;

public class CompatibilityFragment extends Fragment {
    private RecyclerView mRecyclerView;
    private CompatibilityListAdapter mAdapter;
    private InterstitialAd mInterstitialAd;

    @Inject
    EventsRepository mEventsRepository;
    @Inject
    EventCompatibilityRepository mCompatibilityRepository;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        NeverLateApp.getApp().getAppComponent().inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_compatability, container, false);
        mRecyclerView = rootView.findViewById(R.id.compatibility_list_rv);
        mAdapter = new CompatibilityListAdapter(null, null, getActivity());
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mEventsRepository.queryAllCurrentTrackedEvents().observe(this, events -> {
            mAdapter.setEvents(events);
        });
        mCompatibilityRepository.queryCompatibility().observe(this, list -> {
            mAdapter.setEventCompatibilities(list);
        });
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
        mInterstitialAd = new InterstitialAd(getActivity());
        mInterstitialAd.setAdUnitId(getString(R.string.ad_mob_interstitial_ad_unit));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                mInterstitialAd.show();
            }
        });
    }
}
