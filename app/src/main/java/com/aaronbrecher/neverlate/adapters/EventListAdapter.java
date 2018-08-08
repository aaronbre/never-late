package com.aaronbrecher.neverlate.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aaronbrecher.neverlate.databinding.EventsListItemBinding;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.interfaces.ListItemClickListener;

import java.util.List;

public class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.EventViewHolder>{
    private List<Event> mEvents;
    private ListItemClickListener mClickListener;

    public EventListAdapter(List<Event> events, ListItemClickListener clickListener) {
        mEvents = events;
        mClickListener = clickListener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        return new EventViewHolder(EventsListItemBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = mEvents.get(position);
        holder.binding.eventTitle.setText(event.getTitle());
        holder.binding.eventLocation.setText(event.getLocation());
        //TODO create a function in LocationUtils to determine current location and distance to
        //and time to location
    }

    @Override
    public int getItemCount() {
        if(mEvents == null) return 0;
        return mEvents.size();
    }

    public void swapLists(List<Event> events){
        this.mEvents = events;
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
