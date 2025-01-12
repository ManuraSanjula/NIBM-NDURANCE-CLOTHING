package com.ndurance.paymentservice.Controller;

import com.stripe.Stripe;
import com.stripe.model.Customer;
import com.stripe.model.EphemeralKey;
import com.stripe.model.PaymentIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.EphemeralKeyCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class PaymentController {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @PostMapping("/payment-sheet")
    public Map<String, String> createPaymentSheet() {
        Stripe.apiKey = stripeApiKey;

        try {
            CustomerCreateParams customerParams = CustomerCreateParams.builder().build();
            Customer customer = Customer.create(customerParams);

            EphemeralKeyCreateParams ephemeralKeyParams = EphemeralKeyCreateParams.builder()
                    .setStripeVersion("2024-12-18.acacia")
                    .setCustomer(customer.getId())
                    .build();
            EphemeralKey ephemeralKey = EphemeralKey.create(ephemeralKeyParams);

           PaymentIntentCreateParams paymentIntentParams = PaymentIntentCreateParams.builder()
                    .setAmount(1099L)
                    .setCurrency("eur")
                    .setCustomer(customer.getId())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build())
                    .build();
            PaymentIntent paymentIntent = PaymentIntent.create(paymentIntentParams);

            // Prepare the response
            Map<String, String> responseData = new HashMap<>();
            responseData.put("paymentIntent", paymentIntent.getClientSecret());
            responseData.put("ephemeralKey", ephemeralKey.getSecret());
            responseData.put("customer", customer.getId());
            responseData.put("publishableKey", "sk_test_51QgKi407vBfsilMXPQfc2gmftOBqnmXdMM2HFCrch1D7guIuNB28q4sW8FKIzHvi0RVRHTKgL6WxTlO14D4ZaEVh00Grqjoycz");

            return responseData;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error creating payment sheet: " + e.getMessage());
        }
    }
}