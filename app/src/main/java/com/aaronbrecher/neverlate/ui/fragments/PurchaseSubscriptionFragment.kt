package com.aaronbrecher.neverlate.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.aaronbrecher.neverlate.R
import com.aaronbrecher.neverlate.adapters.SkuListAdapter
import com.aaronbrecher.neverlate.adapters.VerticalSpaceItemDecoration
import com.aaronbrecher.neverlate.interfaces.ListItemClickListener
import com.aaronbrecher.neverlate.ui.activities.MainActivity
import com.aaronbrecher.neverlate.ui.controllers.BillingViewController
import com.android.billingclient.api.SkuDetails

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PurchaseSubscriptionFragment : Fragment(), ListItemClickListener {
    private lateinit var mViewController: BillingViewController
    private lateinit var mSkuListAdapter: SkuListAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mViewController = BillingViewController(activity!!)
        //TODO find a better way to do this
        if (activity is MainActivity) {
            (activity as MainActivity).setHomeAsUpIcon(true)
        }
    }

    @Nullable
    override fun onCreateView(@NonNull inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View {
        val rootView = inflater.inflate(R.layout.fragment_purchase_subscription, container, false)
        val skuListView = rootView.findViewById<RecyclerView>(R.id.sku_list_rv)
        mSkuListAdapter = SkuListAdapter(null, this)
        skuListView.adapter = mSkuListAdapter
        skuListView.layoutManager = LinearLayoutManager(context)
        skuListView.addItemDecoration(VerticalSpaceItemDecoration(36))
        mViewController.querySkus()
        mViewController.skuDetailList.observe(this, Observer { skuList -> skuList?.let { mSkuListAdapter.swapLists(it) } })
        return rootView
    }

    override fun onListItemClick(skuDetails: Any) {
        if (skuDetails is SkuDetails) {
            mViewController.initiatePurchase(skuDetails)
        }
    }
}
