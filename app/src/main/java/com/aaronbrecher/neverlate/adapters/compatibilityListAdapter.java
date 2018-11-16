package com.aaronbrecher.neverlate.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.aaronbrecher.neverlate.database.Converters;
import com.aaronbrecher.neverlate.databinding.CompatibilityListItemBinding;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.EventCompatibility;

import org.threeten.bp.format.DateTimeFormatter;

import java.text.DateFormat;
import java.util.List;

import static com.aaronbrecher.neverlate.models.EventCompatibility.*;

public class compatibilityListAdapter extends RecyclerView.Adapter<compatibilityListAdapter.CompatibilityViewHolder> {
    private List<EventCompatibility> mEventCompatibilities;
    private List<Event> mEvents;

    public compatibilityListAdapter(List<EventCompatibility> eventCompatibilities, List<Event> events) {
        mEventCompatibilities = eventCompatibilities;
        mEvents = events;
    }

    @NonNull
    @Override
    public CompatibilityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        return new  CompatibilityViewHolder(CompatibilityListItemBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CompatibilityViewHolder viewHolder, int position) {
        EventCompatibility compatibility = mEventCompatibilities.get(position);
        Event event = getEventForId(compatibility.getStartEvent());
        if (event == null) return;
        viewHolder.binding.setEvent(event);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM d  h:mm a");
        viewHolder.binding.setFormatter(formatter);
        CharSequence time = DateUtils.formatSameDayTime(Converters.unixFromDateTime(event.getStartTime()) + compatibility.getMaxTimeAtStartEvent(),
                System.currentTimeMillis(), DateFormat.DEFAULT, DateFormat.DEFAULT);
        viewHolder.binding.listItemLeaveTime.setText(time);
        if(compatibility.getWithinDrivingDistance() == Compatible.TRUE){
            //show the connected image
        }else {
            //show the error image
        }
    }

    private Event getEventForId(Integer startEvent) {
        if(mEvents == null) return null;
        for(Event event : mEvents){
            if(event.getId() == startEvent) return event;
        }
        return null;
    }

    @Override
    public int getItemCount() {
        if(mEventCompatibilities == null) return 0;
        return mEventCompatibilities.size();
    }

    class CompatibilityViewHolder extends RecyclerView.ViewHolder {
        private CompatibilityListItemBinding binding;
        public CompatibilityViewHolder(CompatibilityListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
