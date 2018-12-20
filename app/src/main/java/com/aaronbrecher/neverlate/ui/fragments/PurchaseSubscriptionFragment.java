package com.aaronbrecher.neverlate.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.adapters.SkuListAdapter;
import com.aaronbrecher.neverlate.adapters.VerticalSpaceItemDecoration;
import com.aaronbrecher.neverlate.interfaces.ListItemClickListener;
import com.aaronbrecher.neverlate.ui.activities.MainActivity;
import com.aaronbrecher.neverlate.ui.controllers.BillingViewController;
import com.android.billingclient.api.SkuDetails;

public class PurchaseSubscriptionFragment extends Fragment implements ListItemClickListener {
    private BillingViewController mViewController;
    private SkuListAdapter mSkuListAdapter;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mViewController = new BillingViewController(getActivity());
        //TODO find a better way to do this
        if(getActivity() instanceof MainActivity){
            ((MainActivity) getActivity()).setHomeAsUpIcon(true);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_purchase_subscription, container, false);
        RecyclerView skuListView = rootView.findViewById(R.id.sku_list_rv);
        mSkuListAdapter = new SkuListAdapter(null, this);
        skuListView.setAdapter(mSkuListAdapter);
        skuListView.setLayoutManager(new LinearLayoutManager(getContext()));
        skuListView.addItemDecoration(new VerticalSpaceItemDecoration(36));
        mViewController.querySkus();
        mViewController.getSkuDetailList().observe(this, skuDetails -> mSkuListAdapter.swapLists(skuDetails));
        return rootView;
    }

    @Override
    public void onListItemClick(Object skuDetails) {
        if(skuDetails instanceof SkuDetails){
            mViewController.initiatePurchase((SkuDetails) skuDetails);
        }
    }
}
