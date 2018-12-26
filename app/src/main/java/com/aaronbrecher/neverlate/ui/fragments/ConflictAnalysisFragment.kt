package com.aaronbrecher.neverlate.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aaronbrecher.neverlate.NeverLateApp
import com.aaronbrecher.neverlate.R
import com.aaronbrecher.neverlate.adapters.ConflictsListAdapter
import com.aaronbrecher.neverlate.database.EventCompatibilityRepository
import com.aaronbrecher.neverlate.database.EventsRepository
import com.aaronbrecher.neverlate.interfaces.NavigationControl
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import javax.inject.Inject

class ConflictAnalysisFragment : Fragment() {
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mAdapter: ConflictsListAdapter
    private var mInterstitialAd: InterstitialAd? = null
    private lateinit var mNavController: NavigationControl

    @Inject
    lateinit var mEventsRepository: EventsRepository
    @Inject
    lateinit var mCompatibilityRepository: EventCompatibilityRepository

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
        val rootView = inflater.inflate(R.layout.fragment_compatability, container, false)
        activity?.setTitle(R.string.anaylize_title)
        mRecyclerView = rootView.findViewById(R.id.compatibility_list_rv)
        mAdapter = ConflictsListAdapter(null, null, context!!)
        mRecyclerView.adapter = mAdapter
        mRecyclerView.layoutManager = LinearLayoutManager(activity)
        mEventsRepository.queryAllCurrentTrackedEvents().observe(this, Observer { list -> list?.let { mAdapter.setEvents(it) } })

        mCompatibilityRepository.queryCompatibility().observe(this, Observer{ list ->
            list?.let {
                if(it.isEmpty()) mNavController.navigateToDestination(R.id.conflictEmptyFragment)
                else mAdapter.setEventCompatibilities(it)
            }?: mNavController.navigateToDestination(R.id.conflictEmptyFragment)
        })
        mInterstitialAd = InterstitialAd(activity)
        return rootView
    }

    override fun onViewCreated(@NonNull view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showAd()
    }

    override fun onDestroyView() {
        mEventsRepository.queryAllCurrentTrackedEvents().removeObservers(this)
        mCompatibilityRepository.queryCompatibility().removeObservers(this)
        mInterstitialAd = null
        super.onDestroyView()
    }

    /**
     * Method to display an interstitial ad to the user. Currently will show an ad once for
     * each new event opened (if user closes app will restart the count). Possibly change this
     * to show only once on each app lifecycle
     */
    private fun showAd() {
        mInterstitialAd = InterstitialAd(context)
        mInterstitialAd?.let {
            it.adUnitId = getString(R.string.ad_mob_interstitial_ad_unit)
            it.loadAd(AdRequest.Builder().build())
            it.adListener =  object : AdListener() {
                override fun onAdLoaded() {
                    if (mInterstitialAd != null) {
                        mInterstitialAd!!.show()
                    }
                }
            }
        }
    }
}
