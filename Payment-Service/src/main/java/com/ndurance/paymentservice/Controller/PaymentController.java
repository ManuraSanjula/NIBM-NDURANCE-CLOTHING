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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class PaymentController {

    // Inject Stripe API Key from application.properties or environment variable
    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @PostMapping("/payment-sheet")
    public Map<String, String> createPaymentSheet(@RequestParam String price) {
        Stripe.apiKey = stripeApiKey;
        System.out.println(price);
        try {
            // Create a new Stripe customer
            CustomerCreateParams customerParams = CustomerCreateParams.builder().build();
            Customer customer = Customer.create(customerParams);

            // Create an ephemeral key for the customer
            EphemeralKeyCreateParams ephemeralKeyParams = EphemeralKeyCreateParams.builder()
                    .setStripeVersion("2024-12-18.acacia")
                    .setCustomer(customer.getId())
                    .build();
            EphemeralKey ephemeralKey = EphemeralKey.create(ephemeralKeyParams);

            // Create a PaymentIntent
            PaymentIntentCreateParams paymentIntentParams = PaymentIntentCreateParams.builder()
                    .setAmount((Long.decode(price) * 100)) // Amount in smallest currency unit (e.g., cents)
                    .setCurrency("USD")
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
            responseData.put("publishableKey", "pk_test_51QgKi407vBfsilMX5jGkIrNs9vPlREX1iV67kY4SN9ZU4JBb0KsMfnR0QX9bJ0cdiyaI40G7gLLHVry3xLMf1GMl00ExJk1kJ6");

            return responseData;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error creating payment sheet: " + e.getMessage());
        }
    }
}