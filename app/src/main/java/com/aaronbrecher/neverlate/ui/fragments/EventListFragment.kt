package com.aaronbrecher.neverlate.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.aaronbrecher.neverlate.AwarenessFencesCreator
import com.aaronbrecher.neverlate.Constants
import com.aaronbrecher.neverlate.NeverLateApp
import com.aaronbrecher.neverlate.R
import com.aaronbrecher.neverlate.utils.BackgroundUtils
import com.aaronbrecher.neverlate.adapters.EventListAdapter
import com.aaronbrecher.neverlate.adapters.EventListSwipeToDeleteCallback
import com.aaronbrecher.neverlate.databinding.FragmentMainActivityListBinding
import com.aaronbrecher.neverlate.interfaces.ListItemClickListener
import com.aaronbrecher.neverlate.interfaces.NavigationControl
import com.aaronbrecher.neverlate.interfaces.SwipeToDeleteListener
import com.aaronbrecher.neverlate.models.Event
import com.aaronbrecher.neverlate.ui.activities.EventDetailActivity
import com.aaronbrecher.neverlate.ui.activities.MainActivity
import com.aaronbrecher.neverlate.viewmodels.MainActivityViewModel
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.GooglePlayDriver
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.material.snackbar.Snackbar

import javax.inject.Inject

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager

//TODO when the list becomes empty need to close the fragment and load the no-events-fragment, probably needs to be done in mainActivity via a interface
class EventListFragment : Fragment(), SwipeToDeleteListener, ListItemClickListener {
    @Inject
    internal lateinit var mViewModelFactory: ViewModelProvider.Factory
    @Inject
    internal lateinit var mLocationProviderClient: FusedLocationProviderClient
    @Inject
    internal lateinit var mSharedPreferences: SharedPreferences

    private lateinit var mViewModel: MainActivityViewModel
    private lateinit var mListAdapter: EventListAdapter
    private lateinit var mBinding: FragmentMainActivityListBinding
    private lateinit var mRootView: View
    private lateinit var mNavController: NavigationControl
    private var mEventList: List<Event> = ArrayList()
    private lateinit var mItemTouchHelper: ItemTouchHelper
    private var mAppIsSnoozed: Boolean = false

    private val eventsObserver = Observer<List<Event>> { events ->
        MainActivity.setFinishedLoading(true)
        if (mAppIsSnoozed) {
            mNavController.navigateToDestination(R.id.appSnoozedFragment)
        } else if (events == null || events.isEmpty()) {
            mNavController.navigateToDestination(R.id.noEventsFragment)
        } else {
            mEventList = events
            mListAdapter.swapLists(events)
        }
    }

    private val showAllEventsObserver = Observer<Boolean> { shouldShow ->
        if (shouldShow != null && shouldShow) {
            mViewModel.allCurrentEvents.removeObserver(eventsObserver)
            mViewModel.allEvents.observe(this@EventListFragment, eventsObserver)
            activity?.setTitle(R.string.list_title_all)
            mItemTouchHelper.attachToRecyclerView(null)
        } else {
            mViewModel.allEvents.removeObserver(eventsObserver)
            mViewModel.allCurrentEvents.observe(this@EventListFragment, eventsObserver)
            activity?.setTitle(R.string.list_title)
            mItemTouchHelper.attachToRecyclerView(mBinding.eventListRv)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mNavController = activity as NavigationControl
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString() + " must implement the ListItemClickListener interface")
        }

        val appComponent = NeverLateApp.app.appComponent
        appComponent.inject(this)
        mViewModel = ViewModelProviders.of(activity!!, mViewModelFactory).get(MainActivityViewModel::class.java)
        mAppIsSnoozed = mSharedPreferences.getLong(Constants.SNOOZE_PREFS_KEY, Constants.ROOM_INVALID_LONG_VALUE) != Constants.ROOM_INVALID_LONG_VALUE
                && !mSharedPreferences.getBoolean(Constants.SNOOZE_ONLY_NOTIFICATIONS_PREFS_KEY, false)
        mListAdapter = EventListAdapter(null, context, this)
        mViewModel.shouldShowAllEvents.observe(this, showAllEventsObserver)
    }

    @Nullable
    override fun onCreateView(@NonNull inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View? {
        mBinding = FragmentMainActivityListBinding.inflate(inflater, container, false)
        mBinding.eventListRv.adapter = mListAdapter
        val layoutManager = LinearLayoutManager(context)
        mBinding.eventListRv.layoutManager = layoutManager
        val dividerItemDecoration = DividerItemDecoration(context, layoutManager.orientation)
        mBinding.eventListRv.addItemDecoration(dividerItemDecoration)
        setupDeleteTouchHelper()
        mRootView = mBinding.root
        return mRootView
    }

    private fun setupDeleteTouchHelper() {
        val swipeCallback = EventListSwipeToDeleteCallback(mListAdapter, this)
        mItemTouchHelper = ItemTouchHelper(swipeCallback)
    }

    // unsubscribe observer here so as not to have multiple observers
    override fun onDestroyView() {
        super.onDestroyView()
        mViewModel.allCurrentEvents.removeObserver(eventsObserver)
    }

    fun filter(query: CharSequence) {
        mListAdapter.filter.filter(query)
    }

    override fun deleteListItem(index: Int) {
        showUndoSnackbar(index)
    }

    private fun showUndoSnackbar(index: Int) {
        val snackbar = Snackbar.make(this.mRootView, R.string.undo_delete_snackbar,
                Snackbar.LENGTH_LONG)
        snackbar.setAction(R.string.undo_delete_snackbar_action_undo) { undoDelete(index) }
        snackbar.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                if (event == Snackbar.Callback.DISMISS_EVENT_TIMEOUT || event == Snackbar.Callback.DISMISS_EVENT_SWIPE) {
                    removeEvent(index)
                } else
                    super.onDismissed(transientBottomBar, event)
            }
        })
        snackbar.show()
    }

    private fun removeEvent(index: Int) {
        if (index >= mEventList.size) return
        val event = mEventList[index]
        val creator = AwarenessFencesCreator.Builder(null).build()
        creator.removeFences(event)
        event.watching = false
        mViewModel.updateEvent(event)
        val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))
        dispatcher.mustSchedule(BackgroundUtils.anaylzeSchedule(dispatcher))
    }

    /**
     * using Bang as can only be called if list is not null
     */
    private fun undoDelete(index: Int) {
        mListAdapter.insertAt(index, mEventList[index])
    }

    override fun onListItemClick(event: Any) {
        if (event is Event) {
            val intent = Intent(context, EventDetailActivity::class.java)
            intent.putExtra(Constants.EVENT_DETAIL_INTENT_EXTRA, Event.convertEventToJson(event))
            startActivity(intent)
        }
    }


}
