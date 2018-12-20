package com.aaronbrecher.neverlate.billing


import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponse
import com.android.billingclient.api.BillingClient.SkuType
import java.util.*

// Default value of mBillingClientResponseCode until BillingManager was not yeat initialized
private const val BILLING_MANAGER_NOT_INITIALIZED = -1

class BillingManager(private val mActivity: Activity, private val mBillingUpdatesListener: BillingUpdatesListener) : PurchasesUpdatedListener {
    private var mBillingClientResponseCode = BILLING_MANAGER_NOT_INITIALIZED

    private val mBillingClient: BillingClient = BillingClient.newBuilder(mActivity).setListener(this).build()

    private var mIsServiceConnected: Boolean = false

    private val mPurchases = ArrayList<Purchase>()

    private val mTokensToBeConsumed: Set<String>? = null

    init {
        startServiceConnection(Runnable{
            mBillingUpdatesListener.onBillingClientSetupFinished()
        })
    }

    private fun startServiceConnection(executeOnSuccess: Runnable) {
        mBillingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(responseCode: Int) {
                if (responseCode == BillingResponse.OK) {
                    mIsServiceConnected = true
                    executeOnSuccess.run()
                }
                mBillingClientResponseCode = responseCode
            }

            override fun onBillingServiceDisconnected() {
                mIsServiceConnected = false
            }
        })
    }

    /**
     * Query the skus to display to the user and return the value
     * using the listener
     */
    fun querySkuDetailsAsync(@SkuType itemType: String, skuList: List<String>,
                             listener: SkuDetailsResponseListener) {
        // Creating a runnable from the request to use it inside our connection retry policy below
        val queryRequest = Runnable{
            // Query the purchase async
            val params = SkuDetailsParams.newBuilder()
            params.setSkusList(skuList).setType(itemType)
            mBillingClient.querySkuDetailsAsync(params.build(),
                    listener)
        }
        executeServiceRequest(queryRequest)
    }

    fun initiatePurchaseFlow(skuDetails: SkuDetails){
        executeServiceRequest(Runnable {
            mBillingClient.launchBillingFlow(mActivity, BillingFlowParams.newBuilder().setSkuDetails(skuDetails).build())
        })
    }

    /**
     * Main interaction with the Billing client will be done through here,
     * the interaction will be via a Runnable so as to allow for a retry policy
     * if the client was not connected
     */
    private fun executeServiceRequest(runnable: Runnable) {
        if (mIsServiceConnected) {
            runnable.run()
        } else {
            // If billing service was disconnected, we try to reconnect 1 time.
            // (feel free to introduce your retry policy here).
            startServiceConnection(runnable)
        }
    }

    override fun onPurchasesUpdated(responseCode: Int, purchases: List<Purchase>?) {

    }
}

interface BillingUpdatesListener {
    fun onBillingClientSetupFinished()
    fun onPurchasesUpdated(purchases: List<Purchase>)
}