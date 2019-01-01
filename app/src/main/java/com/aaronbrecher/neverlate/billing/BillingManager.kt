package com.aaronbrecher.neverlate.billing


import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.aaronbrecher.neverlate.AppExecutors
import com.aaronbrecher.neverlate.Constants
import com.aaronbrecher.neverlate.models.PurchaseData
import com.aaronbrecher.neverlate.network.createRetrofitService
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponse
import com.android.billingclient.api.BillingClient.SkuType
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.ArrayList

// Default value of mBillingClientResponseCode until BillingManager was not yeat initialized
private const val BILLING_MANAGER_NOT_INITIALIZED = -1
private const val TAG = "Billing Manager"

class BillingManager(private val mContext: Context, private val mBillingUpdatesListener: BillingUpdatesListener?) : PurchasesUpdatedListener, PurchaseHistoryResponseListener{
    private var mBillingClientResponseCode = BILLING_MANAGER_NOT_INITIALIZED

    private val mBillingClient: BillingClient = BillingClient.newBuilder(mContext).setListener(this).build()

    private val mRetrofitService = createRetrofitService()

    private var mIsServiceConnected: Boolean = false

    private val mAppExecutors = AppExecutors()

    private val mPurchases = ArrayList<PurchaseData>()


    init {
        startServiceConnection(Runnable {
            mBillingUpdatesListener?.onBillingClientSetupFinished()
        })
    }

    private fun startServiceConnection(executeOnSuccess: Runnable) {
        mBillingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(responseCode: Int) {
                if (responseCode == BillingResponse.OK) {
                    mIsServiceConnected = true
                    executeOnSuccess.run()
                }else{
                    mBillingUpdatesListener?.onBillingSetupFailed()
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
            if(mContext is Activity){
                mBillingClient.launchBillingFlow(mContext, BillingFlowParams.newBuilder().setSkuDetails(skuDetails).build())
            }
        })
    }

    //TODO change this to do validation in app to find correct purchase, then send data to server
    //for secure validation
    fun verifySub() {
        val purchases = checkSubFromLocalPurchases()
        var isValid = false
        mAppExecutors.networkIO().execute {
            purchases.forEach {
                if(verifyPurchase(it)){
                    mBillingUpdatesListener?.onSubscriptionVerified(true)
                    isValid = true
                    return@forEach
                }
            }
            if(!isValid){
                Handler(Looper.getMainLooper()).post {
                    checkSubFromAsyncPurchases(this)
                }
            }
        }
    }

    // Gets the list of purchase either sync or async and updates via the
    // purchases updated listener
    fun getSubList(doAsync: Boolean){
        if(!doAsync){
            val purchases = checkSubFromLocalPurchases()
            purchases.forEach {
                if(it.sku == BillingConstants.SKU_PREMIUM_MONTHLY || it.sku == BillingConstants.SKU_PREMIUM_YEARLY)
                    mPurchases.add(PurchaseData(it.purchaseToken, it.sku, it.packageName))
            }
        }
        if(mPurchases.size < 1 || doAsync){
            getAsyncPurchases()
        } else{
            mBillingUpdatesListener?.onPurchasesUpdated(mPurchases, false)
        }
    }

    fun getAsyncPurchases(){
        mBillingClient.queryPurchaseHistoryAsync(SkuType.SUBS) { responseCode, purchasesList ->
            if(responseCode == BillingResponse.OK)
                purchasesList.forEach { mPurchases.add(PurchaseData(it.purchaseToken, it.sku, it.packageName))
            }
            mBillingUpdatesListener?.onPurchasesUpdated(mPurchases, true)
        }
    }



    override fun onPurchaseHistoryResponse(responseCode: Int, purchasesList: MutableList<Purchase>?) {
        if(responseCode != BillingResponse.OK){
            mBillingUpdatesListener?.onSubscriptionVerified(false)
        } else {
            mAppExecutors.networkIO().execute{
                var isValid = false
                purchasesList?.forEach {
                    if(verifyPurchase(it)){
                        isValid = true
                        return@forEach
                    }
                }
                mBillingUpdatesListener?.onSubscriptionVerified(isValid)
            }
        }
    }

    fun checkSubFromAsyncPurchases(listener: PurchaseHistoryResponseListener){
        if(mIsServiceConnected) mBillingClient.queryPurchaseHistoryAsync(SkuType.SUBS, listener)
    }

    fun checkSubFromLocalPurchases(): List<Purchase>{
        val purchases = mBillingClient.queryPurchases(SkuType.SUBS)
        return purchases.purchasesList
    }

    private fun verifyPurchase(purchase: Purchase) : Boolean{
        val valid = mRetrofitService.verifyPurchase(purchase.purchaseToken, purchase.sku, purchase.packageName).execute().body()
        return valid ?: false
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
                mBillingUpdatesListener?.onPurchaseVerified(purchase, PurchaseVerification.UNKNOWN)
            }
            override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                val valid = response.isSuccessful && response.body() == true
                mBillingUpdatesListener?.onPurchaseVerified(purchase, if(valid) PurchaseVerification.VALID else PurchaseVerification.INVALID)
            }
        })
    }
}




interface BillingUpdatesListener {
    fun onBillingClientSetupFinished()
    fun onBillingSetupFailed(){/* default method */}
    fun onPurchasesUpdated(purchases: List<PurchaseData>, wasAsync: Boolean){/*default method */}
    fun onPurchaseVerified(purchase: Purchase, valid: PurchaseVerification) {/*default method */}
    fun onSubscriptionVerified(isVerified: Boolean){/* default method */}
}

enum class PurchaseVerification{
    VALID, INVALID, UNKNOWN
}