package com.aaronbrecher.neverlate.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.aaronbrecher.neverlate.R

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment

class NoCalendarFragment : Fragment() {
    @Nullable
    override fun onCreateView(@NonNull inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View {
        val rootView = inflater.inflate(R.layout.fragment_no_calendar, container, false)
        activity?.setTitle(R.string.no_calendar_title)
        return rootView
    }
}
