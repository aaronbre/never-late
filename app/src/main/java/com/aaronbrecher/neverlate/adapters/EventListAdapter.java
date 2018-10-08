package com.aaronbrecher.neverlate.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.Utils.LocationUtils;
import com.aaronbrecher.neverlate.databinding.EventsListItemBinding;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.interfaces.ListItemClickListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.PendingResult;
import com.google.maps.model.DirectionsResult;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.EventViewHolder> implements Filterable {
    private static final String TAG = EventListAdapter.class.getSimpleName();
    private List<Event> mEvents;
    private List<Event> mFilteredEvents;
    private ListItemClickListener mClickListener;
    private Location mLocation;
    private Context mContext;
    private Handler mHandler;

    /**
     * Constructor for the adapter
     *
     * @param events        The list of Events to be used - may be null and set later with swapLists
     * @param location      The current Location of the user - may be null and set later with setLocation
     * @param clickListener Interface to handle clicks will be used also as context - MUST BE A VALID CONTEXT!!
     */
    public EventListAdapter(List<Event> events, Location location, ListItemClickListener clickListener) {
        mEvents = events;
        mFilteredEvents = events;
        mLocation = location;
        mClickListener = clickListener;
        mContext = (Context) mClickListener;
        mHandler = new Handler(mContext.getMainLooper());
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
        if (event.getDistance() != null) holder.binding.eventDistance.setText(getHumanReadableDistance(event.getDistance()));
        if (event.getTimeTo() != null) holder.binding.eventTimeTo.setText(DirectionsUtils.readableTravelTime(event.getTimeTo()));
    }

    private String getHumanReadableDistance(Long distance){
        //TODO add a shared prefs to miles or km and fix this accordingly
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean useMetric = false;
        if(sharedPreferences.contains(Constants.UNIT_SYSTEM_PREFS_KEY)){
            useMetric = sharedPreferences.getBoolean(Constants.UNIT_SYSTEM_PREFS_KEY, false);
        }
        float km = distance.floatValue()/1000;
        DecimalFormat df = new DecimalFormat("#.#");
        if(useMetric){
            return df.format(km) + " KM";
        }else {
            double miles = LocationUtils.kmToMiles(km);
            return df.format(miles) + " MILES";
        }
    }

    @Override
    public int getItemCount() {
        if (mFilteredEvents == null) return 0;
        return mFilteredEvents.size();
    }


    public void swapLists(List<Event> events) {
        this.mEvents = events;
        this.mFilteredEvents = mEvents;
        notifyDataSetChanged();
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
