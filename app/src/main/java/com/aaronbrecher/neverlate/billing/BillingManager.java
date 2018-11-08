package com.aaronbrecher.neverlate.billing;

import android.support.annotation.Nullable;

import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;

import java.util.List;

public class BillingManager implements PurchasesUpdatedListener {
    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {

    }
}
