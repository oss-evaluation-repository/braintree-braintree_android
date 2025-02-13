package com.braintreepayments.api;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * PayPalDataCollector is used to collect PayPal specific device information to aid in fraud detection and prevention.
 */
public class PayPalDataCollector {

    private static final String CORRELATION_ID_KEY = "correlation_id";

    private final MagnesInternalClient magnesInternalClient;
    private final UUIDHelper uuidHelper;
    private final BraintreeClient braintreeClient;

    public PayPalDataCollector(@NonNull BraintreeClient braintreeClient) {
        this(braintreeClient, new MagnesInternalClient(), new UUIDHelper());
    }

    @VisibleForTesting
    PayPalDataCollector(BraintreeClient braintreeClient, MagnesInternalClient magnesInternalClient, UUIDHelper uuidHelper) {
        this.braintreeClient = braintreeClient;
        this.magnesInternalClient = magnesInternalClient;
        this.uuidHelper = uuidHelper;
    }

    String getPayPalInstallationGUID(Context context) {
        return uuidHelper.getInstallationGUID(context);
    }

    /**
     * Gets a Client Metadata ID at the time of payment activity. Once a user initiates a PayPal payment
     * from their device, PayPal uses the Client Metadata ID to verify that the payment is
     * originating from a valid, user-consented device and application. This helps reduce fraud and
     * decrease declines. This method MUST be called prior to initiating a pre-consented payment (a
     * "future payment") from a mobile device. Pass the result to your server, to include in the
     * payment request sent to PayPal. Do not otherwise cache or store this value.
     *
     * @param context                Android Context
     * @param configuration           The merchant configuration
     * @param hasUserLocationConsent is an optional parameter that informs the SDK
     *                               if your application has obtained consent from the user to collect location data in compliance with
     *                               <a href="https://support.google.com/googleplay/android-developer/answer/10144311#personal-sensitive">Google Play Developer Program policies</a>
     *                               This flag enables PayPal to collect necessary information required for Fraud Detection and Risk Management.
     * @see <a href="https://support.google.com/googleplay/android-developer/answer/10144311#personal-sensitive">User Data policies for the Google Play Developer Program </a>
     * @see <a href="https://support.google.com/googleplay/android-developer/answer/9799150?hl=en#Prominent%20in-app%20disclosure">Examples of prominent in-app disclosures</a>
     */
    @MainThread
    String getClientMetadataId(
        Context context,
        Configuration configuration,
        boolean hasUserLocationConsent
    ) {
        PayPalDataCollectorInternalRequest request = new PayPalDataCollectorInternalRequest(hasUserLocationConsent)
            .setApplicationGuid(getPayPalInstallationGUID(context));

        return getClientMetadataId(context, request, configuration);
    }

    /**
     * Gets a Client Metadata ID at the time of payment activity. Once a user initiates a PayPal payment
     * from their device, PayPal uses the Client Metadata ID to verify that the payment is
     * originating from a valid, user-consented device and application. This helps reduce fraud and
     * decrease declines. This method MUST be called prior to initiating a pre-consented payment (a
     * "future payment") from a mobile device. Pass the result to your server, to include in the
     * payment request sent to PayPal. Do not otherwise cache or store this value.
     *
     * @param context       Android Context.
     * @param request       configures what data to collect.
     * @param configuration the merchant configuration
     */
    @MainThread
    String getClientMetadataId(Context context, PayPalDataCollectorInternalRequest request, Configuration configuration) {
        return magnesInternalClient.getClientMetadataId(context, configuration, request);
    }

    /**
     * Deprecated. Use {@link PayPalDataCollector#collectDeviceData(Context, PayPalDataCollectorRequest, PayPalDataCollectorCallback)}
     *
     * Collects device data based on your merchant configuration.
     * <p>
     * We recommend that you call this method as early as possible, e.g. at app launch. If that's too early,
     * call it at the beginning of customer checkout.
     * <p>
     * Use the return value on your server, e.g. with `Transaction.sale`.
     *
     * @param context  Android Context
     * @param callback {@link PayPalDataCollectorCallback}
     */
    @Deprecated
    public void collectDeviceData(
        @NonNull final Context context,
        @NonNull final PayPalDataCollectorCallback callback
    ) {
        PayPalDataCollectorRequest request = new PayPalDataCollectorRequest(false, null);
        collectDeviceData(context, request, callback);
    }

    /**
     * Deprecated. Use {@link PayPalDataCollector#collectDeviceData(Context, PayPalDataCollectorRequest, PayPalDataCollectorCallback)}
     *
     * Collects device data for PayPal APIs.
     * <p>
     * We recommend that you call this method as early as possible, e.g. at app launch. If that's too early,
     * call it at the beginning of customer checkout.
     * <p>
     * Use the return value on your server, e.g. with `Transaction.sale`.
     *
     * @param context           Android Context
     * @param riskCorrelationId Optional client metadata id
     * @param callback          {@link PayPalDataCollectorCallback}
     */
    @Deprecated
    public void collectDeviceData(
        @NonNull final Context context,
        @Nullable final String riskCorrelationId,
        @NonNull final PayPalDataCollectorCallback callback
    ) {
        PayPalDataCollectorRequest request = new PayPalDataCollectorRequest(false, riskCorrelationId);
        collectDeviceData(context, request, callback);
    }

    /**
     * Collects device data for PayPal APIs.
     * <p>
     * We recommend that you call this method as early as possible, e.g. at app launch. If that's too early,
     * call it at the beginning of customer checkout.
     * <p>
     * Use the return value on your server, e.g. with `Transaction.sale`.
     *
     * @param context                    Android Context
     * @param payPalDataCollectorRequest params for the data collection request
     * @param callback                   {@link PayPalDataCollectorCallback}
     */
    public void collectDeviceData(
        @NonNull final Context context,
        @NonNull final PayPalDataCollectorRequest payPalDataCollectorRequest,
        @NonNull final PayPalDataCollectorCallback callback
    ) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration != null) {
                    final JSONObject deviceData = new JSONObject();
                    try {
                        PayPalDataCollectorInternalRequest request = new PayPalDataCollectorInternalRequest(
                            payPalDataCollectorRequest.getHasUserLocationConsent()
                        ).setApplicationGuid(getPayPalInstallationGUID(context));

                        if (payPalDataCollectorRequest.getRiskCorrelationId() != null) {
                            request.setRiskCorrelationId(payPalDataCollectorRequest.getRiskCorrelationId());
                        }

                        String correlationId =
                            magnesInternalClient.getClientMetadataId(context, configuration, request);
                        if (!TextUtils.isEmpty(correlationId)) {
                            deviceData.put(CORRELATION_ID_KEY, correlationId);
                        }
                    } catch (JSONException ignored) {
                    }
                    callback.onResult(deviceData.toString(), null);

                } else {
                    callback.onResult(null, error);
                }
            }
        });
    }
}
