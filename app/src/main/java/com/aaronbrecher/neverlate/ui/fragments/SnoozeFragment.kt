package com.aaronbrecher.neverlate.ui.fragments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TimePicker
import android.widget.Toast

import com.aaronbrecher.neverlate.Constants
import com.aaronbrecher.neverlate.NeverLateApp
import com.aaronbrecher.neverlate.R
import com.aaronbrecher.neverlate.database.Converters
import com.aaronbrecher.neverlate.databinding.FragmentSnoozeBinding
import com.aaronbrecher.neverlate.viewmodels.MainActivityViewModel

import org.threeten.bp.format.DateTimeFormatter

import java.util.Calendar

import javax.inject.Inject

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders

class SnoozeFragment : Fragment(), DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {
    @Inject
    internal lateinit var mSharedPreferences: SharedPreferences

    @Inject
    internal lateinit var mViewModelFactory: ViewModelProvider.Factory

    private lateinit var mBinding: FragmentSnoozeBinding
    private lateinit var mViewModel: MainActivityViewModel
    private var mCalendar: Calendar = Calendar.getInstance()
    private var year: Int = 0
    private var month: Int = 0
    private var day: Int = 0
    private var mOnlySnoozeNotifications: Boolean = false
    private lateinit var mCreateSnoozeListener: View.OnClickListener
    private lateinit var mCancelSnoozeListener: View.OnClickListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        NeverLateApp.app.appComponent.inject(this)
        mViewModel = ViewModelProviders.of(activity!!, mViewModelFactory).get(MainActivityViewModel::class.java)
        mCreateSnoozeListener = View.OnClickListener {
            // Use the current date as the default date in the picker
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)
            // Create a new instance of DatePickerDialog and return it
            val dialog = DatePickerDialog(context, this, year, month, day)
            dialog.show()
        }
        mCancelSnoozeListener = View.OnClickListener{
            mSharedPreferences.edit().putLong(Constants.SNOOZE_PREFS_KEY, Constants.ROOM_INVALID_LONG_VALUE)
                    .putBoolean(Constants.SNOOZE_ONLY_NOTIFICATIONS_PREFS_KEY, false).apply()
            mViewModel.rescheduleAllJobs()
            mBinding.snoozeInfo.visibility = View.GONE
            mBinding.snoozeButton.text = getString(R.string.snooze_set_button)
            mBinding.snoozeButton.setOnClickListener(mCreateSnoozeListener)
            mBinding.snoozeOptions.visibility = View.VISIBLE
            mBinding.snoozeOptionsLabel.visibility = View.VISIBLE
        }
    }

    @Nullable
    override fun onCreateView(@NonNull inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View {
        mBinding = FragmentSnoozeBinding.inflate(inflater, container, false)
        activity?.setTitle(R.string.snooze_title)
        val snoozeEnd = mSharedPreferences.getLong(Constants.SNOOZE_PREFS_KEY, Constants.ROOM_INVALID_LONG_VALUE)
        if (snoozeEnd != Constants.ROOM_INVALID_LONG_VALUE) {
            showSnoozeInfo(snoozeEnd)
            mBinding.snoozeOptions.visibility = View.GONE
            mBinding.snoozeOptionsLabel.visibility = View.GONE
        } else {
            mBinding.snoozeInfo.visibility = View.GONE
            mBinding.snoozeButton.setOnClickListener(mCreateSnoozeListener)
        }
        mBinding.setOnSnoozeTypeChange { _, isChecked ->
            mOnlySnoozeNotifications = isChecked
            mSharedPreferences.edit().putBoolean(Constants.SNOOZE_ONLY_NOTIFICATIONS_PREFS_KEY, isChecked).apply()
        }
        mBinding.snoozeEditButton.setOnClickListener {
            val unixTime = mSharedPreferences.getLong(Constants.SNOOZE_PREFS_KEY, System.currentTimeMillis())
            mCalendar.timeInMillis = unixTime
            val dialog = DatePickerDialog(activity!!, this, mCalendar.get(Calendar.YEAR),
                    mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH))
            dialog.show()
        }
        return mBinding.root
    }


    override fun onDateSet(view: DatePicker, year: Int, month: Int, dayOfMonth: Int) {
        this.year = year
        this.month = month
        day = dayOfMonth
        val dialog = TimePickerDialog(activity, this, mCalendar.get(Calendar.HOUR_OF_DAY), mCalendar.get(Calendar.MINUTE) + 15, false)
        dialog.show()
    }

    override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {

        mCalendar.set(year, month, day, hourOfDay, minute)
        val unixDate = mCalendar.timeInMillis
        if (unixDate < System.currentTimeMillis()) {
            Toast.makeText(activity, R.string.snooze_end_error_toast, Toast.LENGTH_SHORT).show()
            return
        }
        if (!mOnlySnoozeNotifications) {
            snooze(unixDate)
        }
        showSnoozeInfo(unixDate)
        mSharedPreferences.edit().putLong(Constants.SNOOZE_PREFS_KEY, unixDate).apply()
    }

    private fun showSnoozeInfo(snoozeEnd: Long) {
        mBinding.snoozeInfo.visibility = View.VISIBLE
        mBinding.snoozeButton.setText(R.string.cancel_snooze_button)
        val formatter = DateTimeFormatter.ofPattern("EEE MMM d  h:mm a")
        mBinding.snoozeTime.text = formatter.format(Converters.dateTimeFromUnix(snoozeEnd))
        mBinding.snoozeButton.setOnClickListener(mCancelSnoozeListener)
        val snoozeType = if (mOnlySnoozeNotifications) getString(R.string.snooze_type_main, getString(R.string.snooze_type_notifications))
        else getString(R.string.snooze_type_main, getString(R.string.snooze_type_all))
        mBinding.snoozeTypeText.text = snoozeType
        mBinding.snoozeOptionsLabel.visibility = View.GONE
        mBinding.snoozeOptions.visibility = View.GONE
    }

    private fun snooze(unixDate: Long) {
        mViewModel.setSnoozeForTime(unixDate)
    }
}
