package com.aaronbrecher.neverlate.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

import com.aaronbrecher.neverlate.R
import com.aaronbrecher.neverlate.interfaces.NavigationControl

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment

class AppSnoozedFragment : Fragment() {
    private lateinit var mButton: Button
    private lateinit var mNavController: NavigationControl



    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mNavController = activity as NavigationControl
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString() + " must implement the ListItemClickListener interface")
        }

    }



    @Nullable
    override fun onCreateView(@NonNull inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View {
        val rootView = inflater.inflate(R.layout.fragment_app_snoozed, container, false)
        activity?.setTitle(R.string.app_snoozed_title)
        mButton = rootView.findViewById(R.id.app_snoozed_cancel_button)
        return rootView
    }

    override fun onViewCreated(@NonNull view: View, @Nullable savedInstanceState: Bundle?) {
        mButton.setOnClickListener { mNavController.navigateToDestination(R.id.snoozeFragment) }
    }
}
