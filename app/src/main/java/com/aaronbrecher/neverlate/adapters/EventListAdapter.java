package com.aaronbrecher.neverlate.adapters;

import android.content.Context;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.databinding.EventsListItemBinding;
import com.aaronbrecher.neverlate.interfaces.ListItemClickListener;
import com.aaronbrecher.neverlate.models.Event;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.EventViewHolder> implements Filterable {
    private static final String TAG = EventListAdapter.class.getSimpleName();
    private List<Event> mEvents;
    private List<Event> mFilteredEvents;
    private ListItemClickListener mClickListener;

    public Context getContext() {
        return mContext;
    }

    private Context mContext;

    /**
     * Constructor for the adapter
     *
     * @param events        The list of Events to be used - may be null and set later with swapLists
     * @param clickListener Interface to handle clicks will be used also as context - MUST BE A VALID CONTEXT!!
     */
    public EventListAdapter(List<Event> events,Context context, ListItemClickListener clickListener) {
        mEvents = events;
        mFilteredEvents = events;
        mClickListener = clickListener;
        mContext = context;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        return new EventViewHolder(EventsListItemBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final EventViewHolder holder, final int position) {
        Event event = mFilteredEvents.get(position);
        holder.binding.eventTitle.setText(event.getTitle());
        holder.binding.eventLocation.setText(event.getLocation());
        if (event.getDistance() != Constants.ROOM_INVALID_LONG_VALUE) holder.binding.eventDistance.setText(
                DirectionsUtils.getHumanReadableDistance(mContext,event.getDistance(),  PreferenceManager.getDefaultSharedPreferences(mContext)));
        LocalDateTime time = event.getStartTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
        if (time != null) holder.binding.eventTimeTo.setText(time.format(formatter));
    }

    @Override
    public int getItemCount() {
        if (mFilteredEvents == null) return 0;
        return mFilteredEvents.size();
    }


    public void swapLists(List<Event> events) {
        this.mEvents = new ArrayList<>(events);
        this.mFilteredEvents = mEvents;
        notifyDataSetChanged();
    }

    public void removeAt(int index){
        mFilteredEvents.remove(index);
        notifyItemRemoved(index);
    }

    public void insertAt(int index, Event event){
        try {
            mFilteredEvents.add(index, event);
        }catch (IndexOutOfBoundsException e){
            mFilteredEvents.add(event);
        }
        notifyItemInserted(index);
    }

    public class EventViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private EventsListItemBinding binding;

        public EventViewHolder(EventsListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Event event = mEvents.get(getAdapterPosition());
            mClickListener.onListItemClick(event);
        }
    }

    /**
     * Filter the list based on the searchbar in menu
     * @return
     */
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String query = constraint.toString();
                List<Event> filtered = new ArrayList<>();
                if(query.isEmpty()){
                    filtered = mEvents;
                }else{
                    for(Event event : mEvents){
                        if(event.getTitle().toLowerCase().contains(query.toLowerCase())){
                            filtered.add(event);
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.count = filtered.size();
                results.values = filtered;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mFilteredEvents = (ArrayList<Event>)results.values;
                notifyDataSetChanged();
            }
        };
    }
}
