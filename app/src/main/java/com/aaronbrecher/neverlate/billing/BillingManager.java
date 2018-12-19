package com.aaronbrecher.neverlate.billing;


import android.app.Activity;
import android.support.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.android.billingclient.api.BillingClient.*;

public class BillingManager implements PurchasesUpdatedListener {

    // Default value of mBillingClientResponseCode until BillingManager was not yeat initialized
    public static final int BILLING_MANAGER_NOT_INITIALIZED  = -1;
    private int mBillingClientResponseCode = BILLING_MANAGER_NOT_INITIALIZED;

    private BillingClient mBillingClient;

    private boolean mIsServiceConnected;

    private final Activity mActivity;

    private final BillingUpdatesListener mBillingUpdatesListener;

    private final List<Purchase> mPurchases = new ArrayList<>();

    private Set<String> mTokensToBeConsumed;

    public interface BillingUpdatesListener{
        void onBillingClientSetupFinished();
        void onConsumeFinished(String token, @BillingResponse int result);
        void onPurchasesUpdated(List<Purchase> purchases);
    }

    public BillingManager(Activity activity, BillingUpdatesListener billingUpdatesListener) {
        mActivity = activity;
        mBillingUpdatesListener = billingUpdatesListener;
        mBillingClient = BillingClient.newBuilder(mActivity).setListener(this).build();

        startServiceConnection(()->{
            mBillingUpdatesListener.onBillingClientSetupFinished();
            queryPurchases();
        });
    }

    private void queryPurchases(){
        Runnable queryToExecute = ()->{
            long time = System.currentTimeMillis();

        };
    }

    public void startServiceConnection(Runnable executeOnSuccess){
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(int responseCode) {
                if(responseCode == BillingResponse.OK){
                    mIsServiceConnected = true;
                    if(executeOnSuccess != null){
                        executeOnSuccess.run();
                    }
                }
                mBillingClientResponseCode = responseCode;
            }

            @Override
            public void onBillingServiceDisconnected() {
                mIsServiceConnected = false;
            }
        });
    }

    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {

    }
}