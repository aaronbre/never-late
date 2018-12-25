package com.aaronbrecher.neverlate.ui.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aaronbrecher.neverlate.AwarenessFencesCreator;
import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.Utils.BackgroundUtils;
import com.aaronbrecher.neverlate.adapters.EventListAdapter;
import com.aaronbrecher.neverlate.adapters.EventListSwipeToDeleteCallback;
import com.aaronbrecher.neverlate.databinding.FragmentMainActivityListBinding;
import com.aaronbrecher.neverlate.dependencyinjection.AppComponent;
import com.aaronbrecher.neverlate.interfaces.ListItemClickListener;
import com.aaronbrecher.neverlate.interfaces.NavigationControl;
import com.aaronbrecher.neverlate.interfaces.SwipeToDeleteListener;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.ui.activities.EventDetailActivity;
import com.aaronbrecher.neverlate.ui.activities.MainActivity;
import com.aaronbrecher.neverlate.viewmodels.MainActivityViewModel;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;

//TODO when the list becomes empty need to close the fragment and load the no-events-fragment, probably needs to be done in mainActivity via a interface
public class EventListFragment extends Fragment implements SwipeToDeleteListener, ListItemClickListener {
    @Inject
    ViewModelProvider.Factory mViewModelFactory;
    @Inject
    FusedLocationProviderClient mLocationProviderClient;
    @Inject
    SharedPreferences mSharedPreferences;

    private MainActivityViewModel mViewModel;
    private EventListAdapter mListAdapter;
    private FragmentMainActivityListBinding mBinding;
    private View mRootView;
    private NavigationControl mNavController;
    private List<Event> mEventList;
    private ItemTouchHelper mItemTouchHelper;
    private boolean mAppIsSnoozed;

    @SuppressLint("MissingPermission")
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mNavController = (NavigationControl) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    " must implement the ListItemClickListener interface");
        }
        AppComponent appComponent = NeverLateApp.getApp().getAppComponent();
        appComponent.inject(this);
        mViewModel = ViewModelProviders.of(getActivity(), mViewModelFactory).get(MainActivityViewModel.class);
        mAppIsSnoozed = mSharedPreferences.getLong(Constants.SNOOZE_PREFS_KEY, Constants.ROOM_INVALID_LONG_VALUE) != Constants.ROOM_INVALID_LONG_VALUE
                && !mSharedPreferences.getBoolean(Constants.SNOOZE_ONLY_NOTIFICATIONS_PREFS_KEY, false);
        mListAdapter = new EventListAdapter(null, getContext(), this);
        mViewModel.getShouldShowAllEvents().observe(this, showAllEventsObserver);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentMainActivityListBinding.inflate(inflater, container, false);
        mBinding.eventListRv.setAdapter(mListAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mBinding.eventListRv.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getContext(), layoutManager.getOrientation());
        mBinding.eventListRv.addItemDecoration(dividerItemDecoration);
        setupDeleteTouchHelper();
        mRootView = mBinding.getRoot();
        return mRootView;
    }

    private void setupDeleteTouchHelper() {
        EventListSwipeToDeleteCallback swipeCallback = new EventListSwipeToDeleteCallback(mListAdapter, this);
        mItemTouchHelper = new ItemTouchHelper(swipeCallback);
    }

    // unsubscribe observer here so as not to have multiple observers
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mViewModel.getAllCurrentEvents().removeObserver(eventsObserver);
    }

    private final Observer<List<Event>> eventsObserver = new Observer<List<Event>>() {
        @Override
        public void onChanged(@Nullable List<Event> events) {
            MainActivity.setFinishedLoading(true);
            if (mAppIsSnoozed) {
                mNavController.navigateToDestination(R.id.appSnoozedFragment);
            } else if (events == null || events.size() == 0) {
                mNavController.navigateToDestination(R.id.noEventsFragment);
            } else {
                mEventList = events;
                mListAdapter.swapLists(events);
            }
        }
    };

    private final Observer<Boolean> showAllEventsObserver = new Observer<Boolean>() {
        @Override
        public void onChanged(@Nullable Boolean shouldShow) {
            if (shouldShow != null && shouldShow) {
                mViewModel.getAllCurrentEvents().removeObserver(eventsObserver);
                mViewModel.getAllEvents().observe(EventListFragment.this, eventsObserver);
                getActivity().setTitle(R.string.list_title_all);
                mItemTouchHelper.attachToRecyclerView(null);
            } else {
                mViewModel.getAllEvents().removeObserver(eventsObserver);
                mViewModel.getAllCurrentEvents().observe(EventListFragment.this, eventsObserver);
                getActivity().setTitle(R.string.list_title);
                mItemTouchHelper.attachToRecyclerView(mBinding.eventListRv);
            }
        }
    };

    public void filter(CharSequence query) {
        mListAdapter.getFilter().filter(query);
    }

    @Override
    public void deleteListItem(int index) {
        showUndoSnackbar(index);
    }

    private void showUndoSnackbar(int index) {
        Snackbar snackbar = Snackbar.make(this.mRootView, R.string.undo_delete_snackbar,
                Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.undo_delete_snackbar_action_undo, v -> undoDelete(index));
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                if (event == Snackbar.Callback.DISMISS_EVENT_TIMEOUT || event == Snackbar.Callback.DISMISS_EVENT_SWIPE) {
                    removeEvent(index);
                } else super.onDismissed(transientBottomBar, event);
            }
        });
        snackbar.show();
    }

    private void removeEvent(int index) {
        Event event = mEventList.get(index);
        AwarenessFencesCreator creator = new AwarenessFencesCreator.Builder(null).build();
        creator.removeFences(event);
        event.setWatching(false);
        mViewModel.updateEvent(event);
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(getContext()));
        dispatcher.mustSchedule(BackgroundUtils.anaylzeSchedule(dispatcher));
    }

    private void undoDelete(int index) {
        mListAdapter.insertAt(index, mEventList.get(index));
    }

    @Override
    public void onListItemClick(Object event) {
        if(event instanceof Event){
            Intent intent = new Intent(getContext(), EventDetailActivity.class);
            intent.putExtra(Constants.EVENT_DETAIL_INTENT_EXTRA, Event.convertEventToJson((Event) event));
            startActivity(intent);
        }
    }


}
