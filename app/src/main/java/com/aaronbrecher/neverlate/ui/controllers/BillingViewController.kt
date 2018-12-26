package com.aaronbrecher.neverlate.ui.controllers

import android.app.Activity
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.aaronbrecher.neverlate.R
import com.aaronbrecher.neverlate.billing.BillingConstants
import com.aaronbrecher.neverlate.billing.BillingManager
import com.aaronbrecher.neverlate.billing.BillingUpdatesListener
import com.aaronbrecher.neverlate.billing.PurchaseVerification
import com.android.billingclient.api.BillingClient.BillingResponse
import com.android.billingclient.api.BillingClient.SkuType
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsResponseListener

class BillingViewController(private val mActivity: Activity) :  BillingUpdatesListener {

    private val mBillingManager: BillingManager = BillingManager(mActivity, this)
    val skuDetailList = MutableLiveData<List<SkuDetails>?>()

    fun querySkus() {
        mBillingManager.querySkuDetailsAsync(SkuType.SUBS, BillingConstants.getSkuList(), SkuDetailsResponseListener { responseCode, skuDetailsList ->
            if(responseCode == BillingResponse.OK && skuDetailsList != null)
                skuDetailList.value = skuDetailsList
        })
    }

        fun initiatePurchase(details: SkuDetails){
            mBillingManager.initiatePurchaseFlow(details)
        }

    override fun onBillingClientSetupFinished() {
        querySkus()
    }

    override fun onPurchasesUpdated(purchases: List<Purchase>) {

    }

    override fun onPurchaseVerified(purchase: Purchase, valid: PurchaseVerification) {
        when(valid){
            PurchaseVerification.VALID -> Toast.makeText(mActivity, mActivity.getString(R.string.purchase_success_toast, purchase.orderId),
                    Toast.LENGTH_LONG).show()
            PurchaseVerification.INVALID -> Toast.makeText(mActivity, mActivity.getString(R.string.purchase_failed_toast),
                    Toast.LENGTH_LONG).show()
            PurchaseVerification.UNKNOWN -> Toast.makeText(mActivity, mActivity.getString(R.string.purchase_unkown_toast),
                    Toast.LENGTH_LONG).show()
        }
    }
}
