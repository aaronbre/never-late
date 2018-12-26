package com.aaronbrecher.neverlate.billing


import android.app.Activity
import android.util.Log
import com.aaronbrecher.neverlate.network.createRetrofitService
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponse
import com.android.billingclient.api.BillingClient.SkuType
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

// Default value of mBillingClientResponseCode until BillingManager was not yeat initialized
private const val BILLING_MANAGER_NOT_INITIALIZED = -1
private const val TAG = "Billing Manager"

class BillingManager(private val mActivity: Activity, private val mBillingUpdatesListener: BillingUpdatesListener) : PurchasesUpdatedListener {
    private var mBillingClientResponseCode = BILLING_MANAGER_NOT_INITIALIZED

    private val mBillingClient: BillingClient = BillingClient.newBuilder(mActivity).setListener(this).build()

    private var mIsServiceConnected: Boolean = false

    private val mPurchases = ArrayList<Purchase>()


    init {
        startServiceConnection(Runnable {
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
        val queryRequest = Runnable {
            // Query the purchase async
            val params = SkuDetailsParams.newBuilder()
            params.setSkusList(skuList).setType(itemType)
            mBillingClient.querySkuDetailsAsync(params.build(),
                    listener)
        }
        executeServiceRequest(queryRequest)
    }

    fun initiatePurchaseFlow(skuDetails: SkuDetails) {
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
        when (responseCode) {
            BillingResponse.OK -> purchases?.let { purchaseList ->
                purchaseList.forEach {
                    handlePurchase(it)
                }
            }
            BillingResponse.USER_CANCELED -> Log.i(TAG, "onPurchasesUpdated() - user cancelled the purchase flow - skipping")
            else -> Log.w(TAG, "onPurchasesUpdated() got unknown resultCode: $responseCode")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        val networkService = createRetrofitService()
        networkService.verifyPurchase(purchase.purchaseToken, purchase.sku, purchase.packageName).enqueue(object : Callback<Boolean>{
            override fun onFailure(call: Call<Boolean>, t: Throwable) {
                mBillingUpdatesListener.onPurchaseVerified(purchase, PurchaseVerification.UNKNOWN)
            }
            override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                val valid = response.isSuccessful && response.body() == true
                mBillingUpdatesListener.onPurchaseVerified(purchase, if(valid) PurchaseVerification.VALID else PurchaseVerification.INVALID)
            }
        })
    }
}




interface BillingUpdatesListener {
    fun onBillingClientSetupFinished()
    fun onPurchasesUpdated(purchases: List<Purchase>)
    fun onPurchaseVerified(purchase: Purchase, valid: PurchaseVerification)
}

enum class PurchaseVerification{
    VALID, INVALID, UNKNOWN
}