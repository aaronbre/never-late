package com.aaronbrecher.neverlate.ui.fragments;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.Utils.GeofenceUtils;
import com.aaronbrecher.neverlate.databinding.FragmentEventDetailBinding;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.ui.activities.EventDetailActivity;
import com.aaronbrecher.neverlate.viewmodels.BaseViewModel;
import com.aaronbrecher.neverlate.viewmodels.DetailActivityViewModel;
import com.aaronbrecher.neverlate.viewmodels.MainActivityViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;

import org.threeten.bp.format.DateTimeFormatter;

import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

public class EventDetailFragment extends Fragment {

    @Inject
    ViewModelProvider.Factory mViewModelFactory;
    @Inject
    SharedPreferences mSharedPreferences;

    @Inject
    FusedLocationProviderClient mFusedLocationProviderClient;
    private BaseViewModel mViewModel;
    private FragmentEventDetailBinding mBinding;
    private Event mEvent;
    private EventDetailActivity mActivity;
    private EditedEventListener mEditedEventListener;


    private Observer<Event> mEventObserver = new Observer<Event>() {
        @Override
        public void onChanged(@Nullable Event event) {
            mBinding.setEvent(event);
            mEvent = event;
            String timeToLeave = DirectionsUtils.getTimeToLeaveHumanReadable(mEvent.getDrivingTime(),
                    GeofenceUtils.determineRelevantTime(mEvent.getStartTime(), mEvent.getEndTime()));
            String formatted = getString(R.string.event_detail_leave_time, timeToLeave);
            mBinding.eventDetailLeaveTime.setText(formatted);
            String watchingText;
            watchingText = event.isWatching() ? getString(R.string.tracking) : getString(R.string.not_tracking);
            mBinding.eventDetailTracking.setText(watchingText);
            setTransportTextView();
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (EventDetailActivity) getActivity();
        mEditedEventListener = mActivity;
        NeverLateApp.getApp().getAppComponent().inject(this);
        if (getResources().getBoolean(R.bool.is_tablet)) {
            mViewModel = ViewModelProviders.of(getActivity(), mViewModelFactory).get(MainActivityViewModel.class);
        } else {
            mViewModel = ViewModelProviders.of(getActivity(), mViewModelFactory).get(DetailActivityViewModel.class);
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentEventDetailBinding.inflate(inflater, container, false);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM d  h:mm a");
        mBinding.setFormatter(formatter);
        mViewModel.getEvent().observe(this, mEventObserver);
        mBinding.eventDetailChangeTrackingButton.setOnClickListener(mClickListener);
        mBinding.eventDetailChangeTransportButton.setOnClickListener(mClickListener);
        return mBinding.getRoot();
    }

    private void setTransportTextView() {
        int mode = mEvent.getTransportMode();
        String modeString;
        switch (mode) {
            case Constants.TRANSPORT_WALKING:
                modeString = getString(R.string.transport_mode_walking);
                break;
            case Constants.TRANSPORT_PUBLIC:
                modeString = getString(R.string.transport_mode_public);
                break;
            default:
                modeString = getString(R.string.transport_mode_driving);

        }
        mBinding.eventDetailTransportMode.setText(modeString);
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int id = view.getId();
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            if (id == mBinding.eventDetailChangeTrackingButton.getId()) {
                int checked = mBinding.eventDetailTracking.getText().toString().equals(getString(R.string.tracking)) ? 0 : 1;
                AtomicInteger userSelected = new AtomicInteger();
                builder.setTitle(R.string.edit_dialog_tracking_title);
                builder.setSingleChoiceItems(R.array.event_detail_edit_tracking, checked, (dialog, which) -> userSelected.set(which));
                builder.setPositiveButton(R.string.save, (dialog, which) -> {
                    //This will be true for tracking and false for not tracking used to check if there
                    //was a real change from original event
                    boolean selectedTracking = userSelected.get() == 0;
                    String watchingText = userSelected.get() == 0 ? "Tracking" : "Not tracking";
                    mBinding.eventDetailTracking.setText(watchingText);
                    Event editedEvent = mEvent.copy();
                    editedEvent.setWatching(selectedTracking);
                    mEditedEventListener.updateEvent(editedEvent);
                    if (selectedTracking != mEvent.isWatching()) {
                        mActivity.showOptionsMenu();
                    }else {
                        mActivity.hideOptionsMenu();
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);
                builder.create().show();
            } else if (id == mBinding.eventDetailChangeTransportButton.getId()) {
                Toast.makeText(getActivity(), "This feature is not implemented yet, we are working hard to get this up and running", Toast.LENGTH_SHORT).show();
//                TODO this is not yet implemented
//                builder.setTitle("Select driving mode");
//                builder.setItems(R.array.event_detail_edit_transport, (dialog, which) ->{
//                    String transportMode = "";
//                    switch (which + 1){
//                        case Constants.TRANSPORT_DRIVING:
//                            transportMode = getString(R.string.transport_mode_driving);
//                            break;
//                        case Constants.TRANSPORT_WALKING:
//                            transportMode = getString(R.string.transport_mode_walking);
//                            break;
//                        case Constants.TRANSPORT_PUBLIC:
//                            transportMode = getString(R.string.transport_mode_public);
//                            break;
//                    }
//                    mBinding.eventDetailTransportMode.setText(transportMode);
//                    Event editedEvent = mEvent.copy();
//                    editedEvent.setTransportMode(which+1);
//                    mEditedEventListener.updateEvent(editedEvent);
//                    if(mEvent.getTransportMode() == which+1){
//                        mActivity.hideOptionsMenu();
//                    }else {
//                        mActivity.showOptionsMenu();
//                    }
//                });
//                builder.setNegativeButton(R.string.cancel, null);
//                builder.create().show();
            }
        }
    };

    public interface EditedEventListener{
        void updateEvent(Event event);
    }
}
