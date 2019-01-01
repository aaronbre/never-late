package com.aaronbrecher.neverlate.adapters

import com.aaronbrecher.neverlate.ui.fragments.EventDetailFragment
import com.aaronbrecher.neverlate.ui.fragments.EventDetailMapFragment

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class EventDetailPagerAdapter(fm: FragmentManager, private val numTabs: Int) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> EventDetailFragment()
            1 -> EventDetailMapFragment()
            else -> EventDetailFragment()
        }
    }

    override fun getCount(): Int {
        return numTabs
    }
}
