package com.aaronbrecher.neverlate.ui.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.database.Converters;
import com.aaronbrecher.neverlate.databinding.FragmentSnoozeBinding;
import com.aaronbrecher.neverlate.viewmodels.MainActivityViewModel;

import org.threeten.bp.format.DateTimeFormatter;

import java.util.Calendar;

import javax.inject.Inject;

public class SnoozeFragment extends Fragment implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {
    @Inject
    SharedPreferences mSharedPreferences;

    @Inject
    ViewModelProvider.Factory mViewModelFactory;

    private FragmentSnoozeBinding mBinding;
    private MainActivityViewModel mViewModel;
    private Calendar mCalendar;
    private int year;
    private int month;
    private int day;
    private boolean mOnlySnoozeNotifications;
    private View.OnClickListener mCreateSnoozeListener;
    private View.OnClickListener mCancelSnoozeListener;

    public Calendar getCalendar() {
        if (mCalendar == null) mCalendar = Calendar.getInstance();
        return mCalendar;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        NeverLateApp.getApp().getAppComponent().inject(this);
        mViewModel = ViewModelProviders.of(getActivity(), mViewModelFactory).get(MainActivityViewModel.class);
        mCreateSnoozeListener = v -> {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            // Create a new instance of DatePickerDialog and return it
            DatePickerDialog dialog = new DatePickerDialog(getActivity(), this, year, month, day);
            dialog.show();
        };
        mCancelSnoozeListener = v -> {
            mSharedPreferences.edit().putLong(Constants.SNOOZE_PREFS_KEY, Constants.ROOM_INVALID_LONG_VALUE)
                    .putBoolean(Constants.SNOOZE_ONLY_NOTIFICATIONS_PREFS_KEY, false).apply();
            mViewModel.rescheduleAllJobs();
            mBinding.snoozeInfo.setVisibility(View.GONE);
            mBinding.snoozeButton.setText(getString(R.string.snooze_set_button));
            mBinding.snoozeButton.setOnClickListener(mCreateSnoozeListener);
            mBinding.snoozeOptions.setVisibility(View.VISIBLE);
            mBinding.snoozeOptionsLabel.setVisibility(View.VISIBLE);
        };
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentSnoozeBinding.inflate(inflater, container, false);
        long snoozeEnd = mSharedPreferences.getLong(Constants.SNOOZE_PREFS_KEY, Constants.ROOM_INVALID_LONG_VALUE);
        if (snoozeEnd != Constants.ROOM_INVALID_LONG_VALUE) {
            showSnoozeInfo(snoozeEnd);
            mBinding.snoozeOptions.setVisibility(View.GONE);
            mBinding.snoozeOptionsLabel.setVisibility(View.GONE);
        } else {
            mBinding.snoozeInfo.setVisibility(View.GONE);
            mBinding.snoozeButton.setOnClickListener(mCreateSnoozeListener);
        }
        mBinding.setOnSnoozeTypeChange((buttonView, isChecked) ->{
            mOnlySnoozeNotifications = isChecked;
            mSharedPreferences.edit().putBoolean(Constants.SNOOZE_ONLY_NOTIFICATIONS_PREFS_KEY, isChecked).apply();
        });
        mBinding.snoozeEditButton.setOnClickListener(v -> {
            long unixTime = mSharedPreferences.getLong(Constants.SNOOZE_PREFS_KEY, System.currentTimeMillis());
            this.getCalendar().setTimeInMillis(unixTime);
            DatePickerDialog dialog = new DatePickerDialog(getActivity(), this, mCalendar.get(Calendar.YEAR),
                    mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });
        return mBinding.getRoot();
    }


    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        this.year = year;
        this.month = month;
        day = dayOfMonth;
        Calendar c = this.getCalendar();
        TimePickerDialog dialog = new TimePickerDialog(getActivity(), this, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE) + 15, false);
        dialog.show();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Calendar calendar = getCalendar();
        calendar.set(year, month, day, hourOfDay, minute);
        long unixDate = calendar.getTimeInMillis();
        if (unixDate < System.currentTimeMillis()) {
            Toast.makeText(getActivity(), R.string.snooze_end_error_toast, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!mOnlySnoozeNotifications) {
            snooze(unixDate);
        }
        showSnoozeInfo(unixDate);
        mSharedPreferences.edit().putLong(Constants.SNOOZE_PREFS_KEY, unixDate).apply();
    }

    private void showSnoozeInfo(long snoozeEnd) {
        mBinding.snoozeInfo.setVisibility(View.VISIBLE);
        mBinding.snoozeButton.setText(R.string.cancel_snooze_button);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM d  h:mm a");
        mBinding.snoozeTime.setText(formatter.format(Converters.dateTimeFromUnix(snoozeEnd)));
        mBinding.snoozeButton.setOnClickListener(mCancelSnoozeListener);
        String snoozeType = mOnlySnoozeNotifications ? getString(R.string.snooze_type_main, getString(R.string.snooze_type_notifications))
                : getString(R.string.snooze_type_main, getString(R.string.snooze_type_all));
        mBinding.snoozeTypeText.setText(snoozeType);
        mBinding.snoozeOptionsLabel.setVisibility(View.GONE);
        mBinding.snoozeOptions.setVisibility(View.GONE);
    }

    private void snooze(long unixDate) {
        mViewModel.setSnoozeForTime(unixDate);
    }
}
