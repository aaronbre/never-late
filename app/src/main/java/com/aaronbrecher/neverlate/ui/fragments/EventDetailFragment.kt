package com.aaronbrecher.neverlate.ui.fragments

import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.aaronbrecher.neverlate.Constants
import com.aaronbrecher.neverlate.NeverLateApp
import com.aaronbrecher.neverlate.R
import com.aaronbrecher.neverlate.Utils.DirectionsUtils
import com.aaronbrecher.neverlate.Utils.GeofenceUtils
import com.aaronbrecher.neverlate.databinding.FragmentEventDetailBinding
import com.aaronbrecher.neverlate.models.Event
import com.aaronbrecher.neverlate.ui.activities.EventDetailActivity
import com.aaronbrecher.neverlate.viewmodels.DetailActivityViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import org.threeten.bp.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

class EventDetailFragment : Fragment() {

    @Inject
    internal lateinit var mViewModelFactory: ViewModelProvider.Factory
    @Inject
    internal lateinit var mSharedPreferences: SharedPreferences

    @Inject
    internal lateinit var mFusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var mViewModel: DetailActivityViewModel
    private lateinit var mBinding: FragmentEventDetailBinding
    private var mEvent: Event? = null
    private lateinit var mActivity: EventDetailActivity
    private lateinit var mEditedEventListener: EditedEventListener


    private val mClickListener = View.OnClickListener { view ->
        val id = view.id
        val builder = AlertDialog.Builder(mActivity)
        if (id == mBinding.eventDetailChangeTrackingButton.id) {
            val checked = if (mBinding.eventDetailTracking.text.toString() == getString(R.string.tracking)) 0 else 1
            val userSelected = AtomicInteger()
            builder.setTitle(R.string.edit_dialog_tracking_title)
            builder.setSingleChoiceItems(R.array.event_detail_edit_tracking, checked) { _, which -> userSelected.set(which) }
            builder.setPositiveButton(R.string.save) { _, _ ->
                //This will be true for tracking and false for not tracking used to check if there
                //was a real change from original event
                val selectedTracking = userSelected.get() == 0
                val watchingText = if (userSelected.get() == 0) "Tracking" else "Not tracking"
                mBinding.eventDetailTracking.text = watchingText
                val editedEvent = mEvent!!.copy()
                editedEvent.watching = selectedTracking
                mEditedEventListener.updateEvent(editedEvent)
                if (selectedTracking != mEvent!!.watching) {
                    mActivity.showOptionsMenu()
                } else {
                    mActivity.hideOptionsMenu()
                }
            }
            builder.setNegativeButton(R.string.cancel, null)
            builder.create().show()
        } else if (id == mBinding.eventDetailChangeTransportButton.id) {
            //Toast.makeText(activity, "This feature is not implemented yet, we are working hard to get this up and running", Toast.LENGTH_SHORT).show()
            builder.setTitle("Select driving mode")
            builder.setItems(R.array.event_detail_edit_transport) { _, which ->
                val transportMode = when(which){
                    Constants.TRANSPORT_DRIVING -> getString(R.string.transport_mode_driving)
                    Constants.TRANSPORT_PUBLIC -> getString(R.string.transport_mode_public)
                    else -> ""
                }
                mBinding.eventDetailTransportMode.text = transportMode
                if(mEvent != null){
                    val editedEvent = mEvent?.copy() ?: Event()
                    editedEvent.transportMode = which
                    mEditedEventListener.updateEvent(editedEvent)
                    if(mEvent?.transportMode == which) mActivity.hideOptionsMenu()
                    else mActivity.showOptionsMenu()
                }
            }
            builder.setNegativeButton(R.string.cancel, null)
            builder.create().show()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        NeverLateApp.app.appComponent.inject(this)
        mActivity = activity as EventDetailActivity
        mEditedEventListener = mActivity
        activity?.let {  mViewModel = ViewModelProviders.of(it, mViewModelFactory).get(DetailActivityViewModel::class.java) }
    }

    @Nullable
    override fun onCreateView(@NonNull inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View {
        mBinding = FragmentEventDetailBinding.inflate(inflater, container, false)
        val formatter = DateTimeFormatter.ofPattern("EEE MMM d  h:mm a")
        mBinding.formatter = formatter
        mViewModel.mEvent.observe(this, Observer {
            Log.i("Observer", "called")
            it?.let {
                mBinding.event = it
                mEvent = it
                val timeToLeave = DirectionsUtils.getTimeToLeaveHumanReadable(it.drivingTime,
                        GeofenceUtils.determineRelevantTime(it.startTime, it.endTime))
                val formatted = getString(R.string.event_detail_leave_time, timeToLeave)
                mBinding.eventDetailLeaveTime.text = formatted
                val watchingText = if (it.watching!!) getString(R.string.tracking) else getString(R.string.not_tracking)
                mBinding.eventDetailTracking.text = watchingText
                setTransportTextView()
            }
         })
        mBinding.eventDetailChangeTrackingButton.setOnClickListener(mClickListener)
        mBinding.eventDetailChangeTransportButton.setOnClickListener(mClickListener)
        return mBinding.root
    }

    private fun setTransportTextView() {
        val mode = mEvent?.transportMode!!
        val modeString: String
        modeString = when (mode) {
            Constants.TRANSPORT_PUBLIC -> getString(R.string.transport_mode_public)
            else -> getString(R.string.transport_mode_driving)
        }
        mBinding.eventDetailTransportMode.text = modeString
    }

    interface EditedEventListener {
        fun updateEvent(event: Event)
    }
}
