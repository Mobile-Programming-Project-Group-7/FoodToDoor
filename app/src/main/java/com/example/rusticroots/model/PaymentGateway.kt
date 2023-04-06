package com.example.rusticroots.model

import android.content.Context
import com.example.rusticroots.model.data.Constants
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode

/** Contains helper static methods for dealing with the Payments API.*/
object PaymentGateway {
    val CENTS = BigDecimal(100)
    /**********************************************************
     * Create a Google Pay API base request object with properties used in all requests.
     * @return Google Pay API base request object.
     */
    private val baseRequest = JSONObject().apply {
        put("apiVersion", 2)
        put("apiVersionMinor", 0)
    }

    private val allowedCardNetworks = JSONArray(Constants.SUPPORTED_NETWORKS)
    private val allowedCardAuthMethods = JSONArray(Constants.SUPPORTED_METHODS)

    /**********************************************************
     * Gateway Integration: Identify your gateway and your app's gateway merchant identifier.
     * The Google Pay API response will return an encrypted payment method capable of being charged
     * by a supported gateway after payer authorization.
     *
     * @return Payment data tokenization for the CARD payment method.
     */
    private fun gatewayTokenizationSpecification(): JSONObject {
        return JSONObject().apply {
            put("type", "PAYMENT_GATEWAY")
            put("parameters", JSONObject(Constants.PAYMENT_GATEWAY_TOKENIZATION_PARAMETERS))
        }
    }

    /**********************************************************
     * Describe your app's support for the CARD payment method.
     * @return A CARD PaymentMethod object describing accepted cards.
     */
    private fun baseCardPaymentMethod(): JSONObject {
        return JSONObject().apply {

            val parameters = JSONObject().apply {
                put("allowedAuthMethods", allowedCardAuthMethods)
                put("allowedCardNetworks", allowedCardNetworks)
                put("billingAddressRequired", true)
                put("billingAddressParameters", JSONObject(mapOf("format" to "FULL")))
            }

            put("type", "CARD")
            put("parameters", parameters)
        }
    }
    /**********************************************************
     * Describe the expected returned payment data for the CARD payment method
     * @return A CARD PaymentMethod describing accepted cards and optional fields.
     */
    private fun cardPaymentMethod(): JSONObject {
        val cardPaymentMethod = baseCardPaymentMethod()
        cardPaymentMethod.put("tokenizationSpecification", gatewayTokenizationSpecification())

        return cardPaymentMethod
    }

    /**********************************************************
     * Creates an instance of PaymentsClient
     * @param context from the caller activity.
     */
    fun createPaymentsClient(context: Context): PaymentsClient {
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(Constants.PAYMENTS_ENVIRONMENT)
            .build()

        return Wallet.getPaymentsClient(context, walletOptions)
    }

    /**********************************************************
     * An object describing accepted forms of payment by your app, used to determine a viewer's
     * readiness to pay.
     * @return API version and payment methods supported by the app.
     */
    fun isReadyToPayRequest(): JSONObject? {
        return try {
            baseRequest.apply {
                put("allowedPaymentMethods", JSONArray().put(baseCardPaymentMethod()))
            }

        } catch (e: JSONException) {
            null
        }
    }

    /**********************************************************
     * Information about the merchant requesting payment information
     */
    private val merchantInfo: JSONObject =
        JSONObject().apply {
            put("merchantName", Constants.MERCHANT_NAME)
            put("merchantId", Constants.MERCHANT_ID)
        }

    /**********************************************************
     * Provide Google Pay API with a payment amount, currency, and amount status.
     * @return information about the requested payment.
     */
    @Throws(JSONException::class)
    private fun getTransactionInfo(price: String): JSONObject {
        return JSONObject().apply {
            put("totalPrice", price)
            put("totalPriceStatus", "FINAL")
            put("countryCode", Constants.COUNTRY_CODE)
            put("currencyCode", Constants.CURRENCY_CODE)
        }
    }

    /**********************************************************
     * An object describing information requested in a Google Pay payment sheet
     *
     * @return Payment data expected by your app.
     * See [PaymentDataRequest](https://developers.google.com/pay/api/android/reference/object.PaymentDataRequest)
     */
    fun getPaymentDataRequest(priceCents: Long): JSONObject {
        return baseRequest.apply {
            put("allowedPaymentMethods", JSONArray().put(cardPaymentMethod()))
            put("transactionInfo", getTransactionInfo(priceCents.centsToString()))
            put("merchantInfo", merchantInfo)
        }
    }
}

/** Converts cents to a string format accepted by [PaymentGateway.getPaymentDataRequest].*/
fun Long.centsToString() = BigDecimal(this)
    .divide(PaymentGateway.CENTS)
    .setScale(2, RoundingMode.HALF_EVEN)
    .toString()
