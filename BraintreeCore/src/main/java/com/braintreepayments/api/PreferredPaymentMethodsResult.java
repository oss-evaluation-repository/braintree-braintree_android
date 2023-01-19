package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Contains information about which payment methods are preferred on the device.
 * This class is currently in beta and may change in future releases.
 * @hide
 */
public class PreferredPaymentMethodsResult {
    private boolean payPalPreferred;
    private boolean venmoPreferred;

    PreferredPaymentMethodsResult() {}

    static PreferredPaymentMethodsResult fromJSON(String responseBody, boolean venmoInstalled) {
        boolean payPalPreferred = false;

        try {
            JSONObject response = new JSONObject(responseBody);
            JSONObject preferredPaymentMethods = getObjectAtKeyPath(response, "data.preferredPaymentMethods");
            if (preferredPaymentMethods != null) {
                payPalPreferred = preferredPaymentMethods.getBoolean("paypalPreferred");
            }
        } catch (JSONException ignored) {
            // do nothing
        }
        return new PreferredPaymentMethodsResult()
                .isPayPalPreferred(payPalPreferred)
                .isVenmoPreferred(venmoInstalled);
    }

    public PreferredPaymentMethodsResult isPayPalPreferred(boolean payPalPreferred) {
        this.payPalPreferred = payPalPreferred;
        return this;
    }

    public PreferredPaymentMethodsResult isVenmoPreferred(boolean venmoPreferred) {
        this.venmoPreferred = venmoPreferred;
        return this;
    }

    /**
     *
     * @return True if PayPal is a preferred payment method. False otherwise.
     */
    public boolean isPayPalPreferred() {
        return payPalPreferred;
    }

    /**
     *
     * @return True if Venmo app is installed. False otherwise.
     */
    public boolean isVenmoPreferred() {
        return venmoPreferred;
    }

    private static JSONObject getObjectAtKeyPath(JSONObject obj, String keyPath) throws JSONException {
        String[] keys = keyPath.split("\\.");
        JSONObject result = obj;
        for (String key : keys) {
            result = result.getJSONObject(key);
        }
        return result;
    }
}
