package com.aaronbrecher.neverlate.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.database.Converters;
import com.aaronbrecher.neverlate.databinding.CompatibilityLastListItemBinding;
import com.aaronbrecher.neverlate.databinding.CompatibilityListItemBinding;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.models.EventCompatibility;

import org.threeten.bp.format.DateTimeFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.aaronbrecher.neverlate.models.EventCompatibility.Compatible;

public class ConflictsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int REGULAR_VIEW_HOLDER = 1;
    public static final int LAST_ITEM_VIEW_HOLDER = 0;
    private List<EventCompatibility> mEventCompatibilities;
    private List<Event> mEvents;
    private Context mContext;

    public ConflictsListAdapter(List<EventCompatibility> eventCompatibilities, List<Event> events, Context context) {
        mEventCompatibilities = eventCompatibilities;
        mEvents = events;
        mContext = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        if(viewType == REGULAR_VIEW_HOLDER){
            return new  CompatibilityViewHolder(CompatibilityListItemBinding.inflate(inflater, parent, false));
        } else {
            return new CompatibilityViewHolderLast(CompatibilityLastListItemBinding.inflate(inflater, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        EventCompatibility compatibility = mEventCompatibilities.get(position);
        Event event = getEventForId(compatibility.getStartEvent());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM d  h:mm a");
        long maxTimeAtStartEvent = compatibility.getMaxTimeAtStartEvent() == null ? 0 : compatibility.getMaxTimeAtStartEvent();
        if (event == null) return;
        if(viewHolder.getItemViewType() == REGULAR_VIEW_HOLDER){
            CompatibilityViewHolder holder = (CompatibilityViewHolder) viewHolder;
            holder.binding.setEvent(event);
            holder.binding.setFormatter(formatter);
            String leaveTimeString;
            if(compatibility.getWithinDrivingDistance() != Compatible.TRUE){
                leaveTimeString = "Not applicable";
            } else {
                SimpleDateFormat format = new SimpleDateFormat("h:mm a");
                leaveTimeString  = format.format(new Date(Converters.unixFromDateTime(event.getStartTime()) + maxTimeAtStartEvent));
            }
            holder.binding.listItemLeaveTime.setText(leaveTimeString);
            holder.binding.listItemMaxTime.setText(DirectionsUtils.readableTravelTime(maxTimeAtStartEvent/1000));
            if(compatibility.getWithinDrivingDistance() == Compatible.TRUE){
                holder.binding.listItemConnectionImage.setImageDrawable(mContext.getDrawable(R.drawable.is_compatible));
            }else {
                holder.binding.listItemConnectionImage.setImageDrawable(mContext.getDrawable(R.drawable.ic_not_compatible));
            }
        }
        else if(viewHolder.getItemViewType() == LAST_ITEM_VIEW_HOLDER){
            CompatibilityViewHolderLast holder = (CompatibilityViewHolderLast) viewHolder;
            holder.binding.setEvent(getEventForId(compatibility.getEndEvent()));
            holder.binding.setFormatter(formatter);
            holder.binding.includedItem.setEvent(event);
            holder.binding.includedItem.setFormatter(formatter);
            String leaveTimeString;
            if(compatibility.getWithinDrivingDistance() != Compatible.TRUE){
                leaveTimeString = "Not applicable";
            } else {
                SimpleDateFormat format = new SimpleDateFormat("h:mm a");
                leaveTimeString  = format.format(new Date(Converters.unixFromDateTime(event.getStartTime()) + maxTimeAtStartEvent));
            }
            holder.binding.includedItem.listItemLeaveTime.setText(leaveTimeString);
            holder.binding.includedItem.listItemMaxTime.setText(DirectionsUtils.readableTravelTime( maxTimeAtStartEvent/1000));
            if(compatibility.getWithinDrivingDistance() == Compatible.TRUE){
                holder.binding.listItemConnectionImage.setImageDrawable(mContext.getDrawable(R.drawable.is_compatible));
            }else {
                holder.binding.listItemConnectionImage.setImageDrawable(mContext.getDrawable(R.drawable.ic_not_compatible));
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if(position == getItemCount()-1)
            return LAST_ITEM_VIEW_HOLDER;
        else return REGULAR_VIEW_HOLDER;
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

    class CompatibilityViewHolderLast extends RecyclerView.ViewHolder{
        private CompatibilityLastListItemBinding binding;
        public CompatibilityViewHolderLast(CompatibilityLastListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public void setEventCompatibilities(List<EventCompatibility> eventCompatibilities) {
        mEventCompatibilities = eventCompatibilities;
        notifyDataSetChanged();
    }

    public void setEvents(List<Event> events) {
        mEvents = events;
        notifyDataSetChanged();
    }
}
