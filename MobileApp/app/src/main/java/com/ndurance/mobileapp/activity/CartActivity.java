package com.ndurance.mobileapp.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ndurance.mobileapp.R;
import com.ndurance.mobileapp.adapter.CartAdapter;
import com.ndurance.mobileapp.adapter.SpaceItemDecoration;
import com.ndurance.mobileapp.model.dto.CartItem;
import com.ndurance.mobileapp.service.CartService;
import com.ndurance.mobileapp.utils.TokenManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CartActivity extends AppCompatActivity {

    private TextView tvTotal, tvEmptyCartMessage;
    private Button btnCheckout, btnChangeTheAddress;
    private TokenManager tokenManager = new TokenManager(this);
    private CartService cartService;
    private RecyclerView recyclerView;
    private CartAdapter cartAdapter;
    private List<CartItem> cartItems = new ArrayList<>();
    private ImageView ivUser, order_icon;
    private TextView errorMessage, tvOrderSummary;
    private LinearLayout orderSummaryLayout;
    private boolean isExpanded = false;
    private LinearLayout layoutExpandable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        String userId = tokenManager.getUserId();
        String jwtToken = tokenManager.getJwtToken();

        if(userId == null || jwtToken == null){
            Intent intent = new Intent(CartActivity.this, MainActivity.class);
            startActivity(intent);
        }

        errorMessage = findViewById(R.id.error_message);
        errorMessage.setVisibility(View.GONE);

        recyclerView = findViewById(R.id.recyclerViewCart);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartAdapter = new CartAdapter(this, cartItems);
        recyclerView.setAdapter(cartAdapter);
        orderSummaryLayout = findViewById(R.id.orderSummaryLayout);
        tvOrderSummary = findViewById(R.id.tvOrderSummary);
        layoutExpandable = findViewById(R.id.layoutExpandable);
        btnChangeTheAddress = findViewById(R.id.btnChangeTheAddress);

        int spacing = getResources().getDimensionPixelSize(R.dimen.recycler_item_spacing);
        recyclerView.addItemDecoration(new SpaceItemDecoration(spacing));

        // Initialize UI components
        tvTotal = findViewById(R.id.tvTotal);
        tvEmptyCartMessage = findViewById(R.id.tvEmptyCartMessage); // New message view
        btnCheckout = findViewById(R.id.btnCheckout);

        ivUser = findViewById(R.id.ivUser);
        order_icon = findViewById(R.id.order_icon);

        ivUser.setOnClickListener(view -> {
            Intent intent = new Intent(CartActivity.this, UserActivity.class);
            startActivity(intent);
        });

        order_icon.setOnClickListener(view -> {
            Intent intent = new Intent(CartActivity.this, ActivityOrder.class);
            startActivity(intent);
        });

        btnCheckout.setOnClickListener(btn->{
            checkout();
            cartItems.forEach(cart->{
                removeFromCart(userId, cart.getCartId());
            });
        });

        // Initialize Retrofit
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/") // Replace with your server URL
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        cartService = retrofit.create(CartService.class);

        if (userId != null && !userId.isEmpty() && jwtToken != null && !jwtToken.isEmpty()) {
            fetchCartData(userId, jwtToken);
        }
        fetchUserProfilePicture(userId);
        //tvOrderSummary.setOnClickListener(view -> toggleExpandableSection());

    }

    private void fetchUserProfilePicture(String userId) {
        new Thread(() -> {
            try {
                String jwtToken = tokenManager.getJwtToken();
                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url("http://10.0.2.2:8080/user-service/users/image/" + userId)
                        .addHeader("Authorization", "Bearer " + jwtToken)
                        .build();

                okhttp3.Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    InputStream inputStream = response.body().byteStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                    runOnUiThread(() -> ivUser.setImageBitmap(bitmap));
                } else {
                    Log.e("ProfileImage", "Failed to fetch image: " + response.message());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void toggleExpandableSection() {
        if (isExpanded) {
            layoutExpandable.setVisibility(View.GONE);
            btnCheckout.setVisibility(View.VISIBLE);
            btnChangeTheAddress.setVisibility(View.GONE);
            isExpanded = false;
        } else {
            layoutExpandable.setVisibility(View.VISIBLE);
            btnCheckout.setVisibility(View.GONE);
            btnChangeTheAddress.setVisibility(View.VISIBLE);
            isExpanded = true;
        }
    }

    private void updateCartUI() {
        if (cartItems.isEmpty()) {
            tvEmptyCartMessage.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            tvTotal.setText("Total: $0");
        } else {
            tvEmptyCartMessage.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            calculatePrices(cartItems);
        }
    }

    private void fetchCartData(String userId, String token) {
        token = "Bearer " + token;
        userId = userId.trim();
        if (userId.endsWith("}")) {
            userId = userId.substring(0, userId.length() - 1);
        }
        cartService.fetchCart(userId, token).enqueue(new Callback<List<CartItem>>() {
            @Override
            public void onResponse(Call<List<CartItem>> call, Response<List<CartItem>> response) {
                Log.d("CartActivity", "Response: " + response.toString());
                if (response.isSuccessful() && response.body() != null) {
                    cartItems.clear();
                    cartItems.addAll(response.body());
                    cartAdapter.notifyDataSetChanged();
                    calculatePrices(cartItems);
                    if(cartItems.isEmpty()){
                        showEmptyCartMessage();
                    }
                } else {
                    Log.e("CartActivity", "Error: " + response.message());
                    errorMessage.setVisibility(View.VISIBLE);
                    orderSummaryLayout.setVisibility(View.GONE);
                    Toast.makeText(CartActivity.this, "Failed to load cart data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<CartItem>> call, Throwable t) {
                errorMessage.setVisibility(View.VISIBLE);
                orderSummaryLayout.setVisibility(View.GONE);
                Log.e("CartActivity", "Error fetching cart data", t);
                Toast.makeText(CartActivity.this, "Error fetching cart data", Toast.LENGTH_SHORT).show();
                showEmptyCartMessage();
            }
        });
    }

    private void calculatePrices(List<CartItem> cartItems) {
        int originalPrice = 0;
        for (CartItem item : cartItems) {
            originalPrice += ( item.getPrice() * item.getQuantity());
        }
        tvTotal.setText("Total: $" + originalPrice);
    }

    private void checkout() {
        String userId = tokenManager.getUserId();
        String jwtToken = tokenManager.getJwtToken();

        String baseUrl = "http://10.0.2.2:8080/";

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        CartService cartService = retrofit.create(CartService.class);

        Call<Void> cartServiceCall = cartService.checkout(userId, true, "Bearer " + jwtToken);

        new Thread(() -> {
            try {
                Response<Void> response = cartServiceCall.execute();

                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(getApplicationContext(), "Successfully checked out the cart!", Toast.LENGTH_SHORT).show();
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                            Toast.makeText(getApplicationContext(), "An error occurred: " + errorBody, Toast.LENGTH_LONG).show();
                        } catch (IOException e) {
                            Toast.makeText(getApplicationContext(), "An error occurred while processing the error response.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "An error occurred: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
                e.printStackTrace();
            }
        }).start();
    }

    /*private void checkout() {
        String userId = tokenManager.getUserId();
        String jwtToken = tokenManager.getJwtToken();

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://localhost:8080/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        cartService = retrofit.create(CartService.class);

        Call<Void> cart_service_call = cartService.checkout(userId, true, "Bearer " + jwtToken);
        try {
            Response<Void> response = cart_service_call.execute();

            if(response.isSuccessful()){
                Toast.makeText(getApplicationContext(), "Successfully checked out the cart!", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(getApplicationContext(), "An error occurred while checking out the cart.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "An error occurred: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.d("FUCK @@@@@@@@@@@@@@@@@@@@@@ ---- +++++", e.toString());
        }
    }*/

    private void showEmptyCartMessage() {
        tvEmptyCartMessage.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        // Reset total and hide order summary
        tvTotal.setText("Total: $0");
    }

    public void removeFromCart(String userId, String cartId) {
        String authToken = "Bearer " + tokenManager.getJwtToken(); // Assuming tokenManager handles your JWT tokens

        cartService.removeFromCart(userId, cartId, authToken).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Remove the item from the local list and update the UI
                    CartItem itemToRemove = null;
                    for (CartItem item : cartItems) {
                        if (item.getCartId().equals(cartId)) {
                            itemToRemove = item;
                            break;
                        }
                    }
                    if (itemToRemove != null) {
                        cartItems.remove(itemToRemove);
                        cartAdapter.notifyDataSetChanged();
                        calculatePrices(cartItems);
                        updateCartUI();

                        Toast.makeText(CartActivity.this, "Item removed from cart", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(CartActivity.this, "Failed to remove item", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(CartActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}