package com.aaronbrecher.neverlate.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.aaronbrecher.neverlate.NeverLateApp
import com.aaronbrecher.neverlate.R
import com.aaronbrecher.neverlate.database.EventCompatibilityRepository
import com.aaronbrecher.neverlate.interfaces.NavigationControl

import javax.inject.Inject

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.aaronbrecher.neverlate.models.EventCompatibility

class ConflictEmptyFragment : Fragment() {
    private lateinit var mNavController: NavigationControl
    @Inject
    internal lateinit var mCompatibilityRepository: EventCompatibilityRepository

    override fun onAttach(context: Context) {
        super.onAttach(context)
        NeverLateApp.app.appComponent.inject(this)
        try {
            mNavController = activity as NavigationControl
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString() + " must implement the ListItemClickListener interface")
        }

    }

    @Nullable
    override fun onCreateView(@NonNull inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View {
        mCompatibilityRepository.queryCompatibility().observe(this, Observer {
            if (it != null && it.isNotEmpty()) {
                mNavController.navigateToDestination(R.id.conflictAnalysisFragment)
            }
        })
        return inflater.inflate(R.layout.fragment_conflict_empty, container, false)
    }
}
