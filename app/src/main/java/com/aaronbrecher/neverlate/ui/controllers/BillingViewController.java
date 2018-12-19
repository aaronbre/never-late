package com.aaronbrecher.neverlate.ui.controllers;

import android.app.Activity;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.Nullable;

import com.aaronbrecher.neverlate.billing.BillingConstants;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.BillingResponse;
import com.android.billingclient.api.BillingClient.SkuType;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.List;

public class BillingViewController implements PurchasesUpdatedListener {
    private Context mContext;
    private BillingClient mBillingClient;
    private List<String> mSkus = BillingConstants.getSkuList();
    private MutableLiveData<List<SkuDetails>> skuDetailList = new MutableLiveData<>();

    public MutableLiveData<List<SkuDetails>> getSkuDetailList() {
        return skuDetailList;
    }

    public BillingViewController(Context context) {
        mContext = context;
        mBillingClient = BillingClient.newBuilder(mContext).setListener(this).build();
    }

    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {

    }

    public void querySkus(){
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(int responseCode) {
                if(responseCode == BillingResponse.OK){
                    SkuDetailsParams params = SkuDetailsParams.newBuilder()
                            .setSkusList(mSkus)
                            .setType(SkuType.SUBS)
                            .build();
                    mBillingClient.querySkuDetailsAsync(params, (skuResponseCode, skuDetailsList) -> {
                        if(skuResponseCode == BillingResponse.OK && skuDetailsList != null){
                            skuDetailList.setValue(skuDetailsList);
                        }
                    });
                }
            }

            @Override
            public void onBillingServiceDisconnected() {

            }
        });
    }

    public int initiatePurchase(SkuDetails details, Activity activity){
        BillingFlowParams params = BillingFlowParams.newBuilder()
                .setSkuDetails(details)
                .build();
        return mBillingClient.launchBillingFlow(activity, params);
    }
}
