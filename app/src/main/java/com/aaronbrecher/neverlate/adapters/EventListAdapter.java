package com.aaronbrecher.neverlate.adapters;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.Utils.LocationUtils;
import com.aaronbrecher.neverlate.databinding.EventsListItemBinding;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.interfaces.ListItemClickListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.PendingResult;
import com.google.maps.model.DirectionsResult;

import java.util.List;

public class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.EventViewHolder>{
    private static final String TAG = EventListAdapter.class.getSimpleName();
    private List<Event> mEvents;
    private ListItemClickListener mClickListener;
    private Location mLocation;
    private Context mContext;
    private Handler mHandler;

    /**
     * Constructor for the adapter
     * @param events The list of Events to be used - may be null and set later with swapLists
     * @param location The current Location of the user - may be null and set later with setLocation
     * @param clickListener Interface to handle clicks will be used also as context - MUST BE A VALID CONTEXT!!
     */
    public EventListAdapter(List<Event> events, Location location, ListItemClickListener clickListener) {
        mEvents = events;
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
        Event event = mEvents.get(position);
        holder.binding.eventTitle.setText(event.getTitle());
        holder.binding.eventLocation.setText(event.getLocation());
        if(mLocation != null){
            holder.binding.eventDistance.setText(getDistance(event.getLocation()));
            //Following code is used to get the time from current location to the destination
            //TODO currently queries the api every time activity is recreated resulting in many
            //api calls on oreintation change and back press. Need to fix this as each call cost money...
            DirectionsApiRequest apiRequest = DirectionsUtils.getDirectionsApiRequest(
                    LocationUtils.latlngFromAddress(mContext, event.getLocation()),
                    LocationUtils.locationToLatLng(mLocation));
            apiRequest.setCallback(new PendingResult.Callback<DirectionsResult>() {
                @Override
                public void onResult(DirectionsResult result) {
                    Log.i(TAG, "onResult: result success");
                    final String time = result.routes[0].legs[0].duration.humanReadable;
                    //use runnable to do on Ui Thread
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            holder.binding.eventTimeTo.setText(time);
                        }
                    });
                }

                @Override
                public void onFailure(Throwable e) {
                    Log.d(TAG, "onFailure: failed" + e);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        if(mEvents == null) return 0;
        return mEvents.size();
    }

    private String getDistance(String location){
        LatLng latLng = LocationUtils.latlngFromAddress(mContext, location);
        Location destination = LocationUtils.latlngToLocation(latLng);
        return LocationUtils.getDistance(mLocation, destination);
    }

    public void swapLists(List<Event> events){
        this.mEvents = events;
        notifyDataSetChanged();
    }

    public void setLocation(Location location){
        this.mLocation = location;
        notifyDataSetChanged();
    }

    public class EventViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
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
}
