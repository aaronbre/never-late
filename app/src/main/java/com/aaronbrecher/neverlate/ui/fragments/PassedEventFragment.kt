package com.aaronbrecher.neverlate.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.aaronbrecher.neverlate.R

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment

class PassedEventFragment : Fragment() {
    @Nullable
    override fun onCreateView(@NonNull inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View {
        activity?.setTitle(R.string.event_passed_title)
        return inflater.inflate(R.layout.fragment_passed_event, container, false)
    }
}
