package com.aaronbrecher.neverlate.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.aaronbrecher.neverlate.databinding.SkuListItemBinding
import com.aaronbrecher.neverlate.interfaces.ListItemClickListener
import com.android.billingclient.api.SkuDetails

import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView

class SkuListAdapter(private var mSkuList: List<SkuDetails>?, private val mClickListener: ListItemClickListener) : RecyclerView.Adapter<SkuListAdapter.SkuViewHolder>() {

    @NonNull
    override fun onCreateViewHolder(@NonNull viewGroup: ViewGroup, viewType: Int): SkuViewHolder {
        val inflater = LayoutInflater.from(viewGroup.context)
        return SkuViewHolder(SkuListItemBinding.inflate(inflater, viewGroup, false))
    }

    override fun onBindViewHolder(@NonNull skuViewHolder: SkuViewHolder, position: Int) {
        val details = mSkuList!![position]
        skuViewHolder.mBinding.skuDetails = details
        var title = details.title
        if (title.contains("(")) {
            title = title.substring(0, title.indexOf("("))
        }

        skuViewHolder.mBinding.skuListItemName.text = title
    }

    override fun getItemCount(): Int {
        return if (mSkuList == null) 0 else mSkuList!!.size
    }

    inner class SkuViewHolder(var mBinding: SkuListItemBinding) : RecyclerView.ViewHolder(mBinding.root), View.OnClickListener {
        init {
            mBinding.skuListItemPurchaseButton.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val details = mSkuList!![adapterPosition]
            mClickListener.onListItemClick(details)
        }
    }

    fun swapLists(skuList: List<SkuDetails>) {
        this.mSkuList = skuList
        notifyDataSetChanged()
    }
}
