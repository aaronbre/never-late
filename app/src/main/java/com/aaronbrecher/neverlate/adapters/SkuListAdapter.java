package com.aaronbrecher.neverlate.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aaronbrecher.neverlate.databinding.SkuListItemBinding;
import com.aaronbrecher.neverlate.interfaces.ListItemClickListener;
import com.android.billingclient.api.SkuDetails;

import java.util.List;

public class SkuListAdapter extends RecyclerView.Adapter<SkuListAdapter.SkuViewHolder> {

    private List<SkuDetails> mSkuList;
    private ListItemClickListener mClickListener;

    public SkuListAdapter(List<SkuDetails> skuList, ListItemClickListener clickListener) {
        mSkuList = skuList;
        mClickListener = clickListener;
    }

    @NonNull
    @Override
    public SkuViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        return new SkuViewHolder(SkuListItemBinding.inflate(inflater, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SkuViewHolder skuViewHolder, int position) {
        SkuDetails details = mSkuList.get(position);
        skuViewHolder.mBinding.setSkuDetails(details);
        String title = details.getTitle();
        if(title.contains("(")){
            title = title.substring(0, title.indexOf("("));
        }

        skuViewHolder.mBinding.skuListItemName.setText(title);
    }

    @Override
    public int getItemCount() {
        return mSkuList == null ? 0 : mSkuList.size();
    }

    class SkuViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        SkuListItemBinding mBinding;
        public SkuViewHolder(SkuListItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            mBinding.skuListItemPurchaseButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            SkuDetails details = mSkuList.get(getAdapterPosition());
            mClickListener.onListItemClick(details);
        }
    }

    public void swapLists(List<SkuDetails> skuList){
        this.mSkuList = skuList;
        notifyDataSetChanged();
    }
}
