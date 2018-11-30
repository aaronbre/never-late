package com.aaronbrecher.neverlate.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.aaronbrecher.neverlate.ui.fragments.EventDetailFragment;
import com.aaronbrecher.neverlate.ui.fragments.EventDetailMapFragment;

public class EventDetailPagerAdapter extends FragmentPagerAdapter {
    private int numTabs;

    public EventDetailPagerAdapter(FragmentManager fm, int numTabs) {
        super(fm);
        this.numTabs = numTabs;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new EventDetailFragment();
            case 1:
                return new EventDetailMapFragment();
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return numTabs;
    }
}
