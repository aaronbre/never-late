package com.aaronbrecher.neverlate.billing;

import java.util.Arrays;
import java.util.List;

public class BillingConstants {
    public static final String SKU_PREMIUM_MONTHLY = "premium_sub";
    public static final String SKU_PREMIUM_YEARLY = "premium_sub_yearly";

    public static final String[] SUBSCRIPTION_SKUS = {SKU_PREMIUM_MONTHLY, SKU_PREMIUM_YEARLY};

    /**
     * Returns the list of all SKUs for the billing type specified
     */
    public static List<String> getSkuList() {
        return Arrays.asList(SUBSCRIPTION_SKUS);
    }
}
