package com.aaronbrecher.neverlate.adapters

import android.content.Context
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable

import com.aaronbrecher.neverlate.Constants
import com.aaronbrecher.neverlate.Utils.DirectionsUtils
import com.aaronbrecher.neverlate.databinding.EventsListItemBinding
import com.aaronbrecher.neverlate.interfaces.ListItemClickListener
import com.aaronbrecher.neverlate.models.Event

import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

import java.util.ArrayList

import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView

/**
 * Constructor for the adapter
 *
 * @param mEvents        The list of Events to be used - may be null and set later with swapLists
 * @param mClickListener Interface to handle clicks will be used also as context - MUST BE A VALID CONTEXT!!
 */
class EventListAdapter (private var mEvents: MutableList<Event>?, val context: Context, private val mClickListener: ListItemClickListener) : RecyclerView.Adapter<EventListAdapter.EventViewHolder>(), Filterable {
    private var mFilteredEvents: MutableList<Event>

    init {
        mFilteredEvents = mEvents ?: ArrayList()
    }

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): EventViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        return EventViewHolder(EventsListItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(@NonNull holder: EventViewHolder, position: Int) {
        val event = mFilteredEvents[position]
        holder.binding.eventTitle.text = event.title
        holder.binding.eventLocation.text = event.location
        if (event.distance != Constants.ROOM_INVALID_LONG_VALUE)
            holder.binding.eventDistance.text = DirectionsUtils.getHumanReadableDistance(context, event.distance?: -1, PreferenceManager.getDefaultSharedPreferences(context))
        val time = event.startTime
        val formatter = DateTimeFormatter.ofPattern("h:mm a")
        if (time != null) holder.binding.eventTimeTo.text = time.format(formatter)
    }

    override fun getItemCount(): Int {
        return mFilteredEvents.size
    }


    fun swapLists(events: List<Event>) {
        this.mEvents = ArrayList(events)
        this.mFilteredEvents = mEvents ?: ArrayList()
        notifyDataSetChanged()
    }

    fun removeAt(index: Int) {
        mFilteredEvents.removeAt(index)
        notifyItemRemoved(index)
    }

    fun insertAt(index: Int, event: Event) {
        try {
            mFilteredEvents.add(index, event)
        } catch (e: IndexOutOfBoundsException) {
            mFilteredEvents.add(event)
        }

        notifyItemInserted(index)
    }

    inner class EventViewHolder(val binding: EventsListItemBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val event = mEvents!![adapterPosition]
            mClickListener.onListItemClick(event)
        }
    }

    /**
     * Filter the list based on the searchbar in menu
     * @return
     */
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence): Filter.FilterResults {
                val query = constraint.toString()
                var filtered: MutableList<Event>? = ArrayList()
                if (query.isEmpty()) {
                    filtered = mEvents
                } else {
                    for (event in mEvents!!) {
                        if (event.title!!.toLowerCase().contains(query.toLowerCase())) {
                            filtered!!.add(event)
                        }
                    }
                }
                val results = Filter.FilterResults()
                results.count = filtered!!.size
                results.values = filtered
                return results
            }

            override fun publishResults(constraint: CharSequence, results: Filter.FilterResults) {
                mFilteredEvents = results.values as ArrayList<Event>
                notifyDataSetChanged()
            }
        }
    }
}
