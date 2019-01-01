package com.aaronbrecher.neverlate.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup

import com.aaronbrecher.neverlate.R
import com.aaronbrecher.neverlate.Utils.DirectionsUtils
import com.aaronbrecher.neverlate.database.Converters
import com.aaronbrecher.neverlate.databinding.CompatibilityLastListItemBinding
import com.aaronbrecher.neverlate.databinding.CompatibilityListItemBinding
import com.aaronbrecher.neverlate.models.Event
import com.aaronbrecher.neverlate.models.EventCompatibility

import org.threeten.bp.format.DateTimeFormatter

import java.text.SimpleDateFormat
import java.util.Date

import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView

import com.aaronbrecher.neverlate.models.EventCompatibility.Compatible

private const val REGULAR_VIEW_HOLDER = 1
private const val LAST_ITEM_VIEW_HOLDER = 0

class ConflictsListAdapter(private var mEventCompatibilities: List<EventCompatibility>?, private var mEvents: List<Event>?, private val mContext: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        return if (viewType == REGULAR_VIEW_HOLDER) {
            CompatibilityViewHolder(CompatibilityListItemBinding.inflate(inflater, parent, false))
        } else {
            CompatibilityViewHolderLast(CompatibilityLastListItemBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(@NonNull viewHolder: RecyclerView.ViewHolder, position: Int) {
        val (_, startEvent, endEvent, withinDrivingDistance, maxTimeAtStartEvent1) = mEventCompatibilities!![position]
        val event = getEventForId(startEvent)
        val formatter = DateTimeFormatter.ofPattern("EEE MMM d  h:mm a")
        val maxTimeAtStartEvent = maxTimeAtStartEvent1 ?: 0
        if (event == null) return
        if (viewHolder.itemViewType == REGULAR_VIEW_HOLDER) {
            val holder = viewHolder as CompatibilityViewHolder
            holder.binding.event = event
            holder.binding.formatter = formatter
            val leaveTimeString: String
            leaveTimeString = if (withinDrivingDistance !== Compatible.TRUE) {
                "Not applicable"
            } else {
                val format = SimpleDateFormat("h:mm a")
                format.format(Date(Converters.unixFromDateTime(event.startTime)!! + maxTimeAtStartEvent))
            }
            holder.binding.listItemLeaveTime.text = leaveTimeString
            holder.binding.listItemMaxTime.text = DirectionsUtils.readableTravelTime(maxTimeAtStartEvent / 1000)
            if (withinDrivingDistance === Compatible.TRUE) {
                holder.binding.listItemConnectionImage.setImageDrawable(mContext.getDrawable(R.drawable.is_compatible))
            } else {
                holder.binding.listItemConnectionImage.setImageDrawable(mContext.getDrawable(R.drawable.ic_not_compatible))
            }
        } else if (viewHolder.itemViewType == LAST_ITEM_VIEW_HOLDER) {
            val holder = viewHolder as CompatibilityViewHolderLast
            holder.binding.event = getEventForId(endEvent)
            holder.binding.formatter = formatter
            holder.binding.includedItem.event = event
            holder.binding.includedItem.formatter = formatter
            val leaveTimeString: String
            if (withinDrivingDistance !== Compatible.TRUE) {
                leaveTimeString = "Not applicable"
            } else {
                val format = SimpleDateFormat("h:mm a")
                leaveTimeString = format.format(Date(Converters.unixFromDateTime(event.startTime)!! + maxTimeAtStartEvent))
            }
            holder.binding.includedItem.listItemLeaveTime.text = leaveTimeString
            holder.binding.includedItem.listItemMaxTime.text = DirectionsUtils.readableTravelTime(maxTimeAtStartEvent / 1000)
            if (withinDrivingDistance === Compatible.TRUE) {
                holder.binding.listItemConnectionImage.setImageDrawable(mContext.getDrawable(R.drawable.is_compatible))
            } else {
                holder.binding.listItemConnectionImage.setImageDrawable(mContext.getDrawable(R.drawable.ic_not_compatible))
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == itemCount - 1)
            LAST_ITEM_VIEW_HOLDER
        else
            REGULAR_VIEW_HOLDER
    }

    private fun getEventForId(startEvent: Int?): Event? {
        if (mEvents == null) return null
        for (event in mEvents!!) {
            if (event.id == startEvent) return event
        }
        return null
    }

    override fun getItemCount(): Int {
        return if (mEventCompatibilities == null) 0 else mEventCompatibilities!!.size
    }

    internal inner class CompatibilityViewHolder(val binding: CompatibilityListItemBinding) : RecyclerView.ViewHolder(binding.root)

    internal inner class CompatibilityViewHolderLast(val binding: CompatibilityLastListItemBinding) : RecyclerView.ViewHolder(binding.root)

    fun setEventCompatibilities(eventCompatibilities: List<EventCompatibility>) {
        mEventCompatibilities = eventCompatibilities
        notifyDataSetChanged()
    }

    fun setEvents(events: List<Event>) {
        mEvents = events
        notifyDataSetChanged()
    }
}
