package com.ndurance.mobileapp.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.ndurance.mobileapp.R;
import com.ndurance.mobileapp.activity.CartActivity;
import com.ndurance.mobileapp.model.dto.CartItem;
import com.ndurance.mobileapp.utils.TokenManager;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
    private List<CartItem> cartItems;
    private Context context;
    private TokenManager tokenManager;
    private final OkHttpClient client = new OkHttpClient();

    public CartAdapter(Context context, List<CartItem> cartItems) {
        this.context = context;
        this.cartItems = cartItems;
        this.tokenManager = new TokenManager(context);
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        holder.tvProductName.setText(item.getName());
        holder.tvProductPrice.setText("$" + ( item.getPrice() * item.getQuantity()));
        // Load image (use Glide or Picasso)
        Glide.with(context).load("http://10.0.2.2:8080/product-service/products/images/" + item.getImages().get(0)).into(holder.ivProductImage);

        //holder.btnRemove.setOnClickListener(v -> onRemoveClickListener.onRemoveClick(item));

        holder.btnRemove.setOnClickListener(v -> {
            if (context instanceof CartActivity) {
                ((CartActivity) context).removeFromCart(item.getUser(), item.getCartId());
            }
        });

        holder.btnDecreaseQuantity.setOnClickListener(v-> qu2(item, false, true, holder));
        holder.btnIncreaseQuantity.setOnClickListener(v-> qu2(item, true, false, holder));
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));
    }

    public void qu2(CartItem cart, boolean in, boolean de, CartViewHolder holder) {
        String url = "http://10.0.2.2:8080/cart-service/cart/qu/" + cart.getCartId() + "/" + tokenManager.getUserId() + "?in=" + in + "&de=" + de;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + tokenManager.getJwtToken())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                ((CartActivity) context).runOnUiThread(() ->
                        Toast.makeText(context, "Failed to update quantity. Please try again.", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    ((CartActivity) context).runOnUiThread(() -> {
                        try {
                            // Parse the current quantity and price
                            int currentQuantity = Integer.parseInt(holder.tvQuantity.getText().toString());
                            String itemPriceText = holder.tvProductPrice.getText().toString(); // e.g., "$231"
                            int itemPrice = Integer.parseInt(itemPriceText.replace("$", "").trim());

                            // Update the quantity logic
                            int updatedQuantity = currentQuantity;
                            if (in) updatedQuantity++; // Increment quantity
                            else if (de && currentQuantity > 1) updatedQuantity--; // Decrement but not below 1
                            else if (de && currentQuantity == 1) {
                                // Remove the item if quantity reaches zero
                                ((CartActivity) context).removeFromCart(cart.getUser(), cart.getCartId());
                                return;
                            }

                            // Update item quantity in the UI
                            holder.tvQuantity.setText(String.valueOf(updatedQuantity));

                            // Update the item's price in the UI
                            int updatedItemPrice = updatedQuantity * cart.getPrice();
                            holder.tvProductPrice.setText("$" + updatedItemPrice);

                            // Update the total price dynamically
                            String totalText = ((CartActivity) context).tvTotal.getText().toString(); // e.g., "Total: $231"
                            String cleanedTotal = totalText.replace("Total: ", "").replace("$", "").trim();
                            int currentTotal = Integer.parseInt(cleanedTotal);

                            // Calculate the new total price
                            int oldItemPrice = currentQuantity * cart.getPrice();
                            int priceDifference = updatedItemPrice - oldItemPrice;
                            int newCartTotal = currentTotal + priceDifference;

                            // Update the total cart price in the UI
                            ((CartActivity) context).tvTotal.setText("Total: $" + newCartTotal);

                        } catch (Exception e) {
                            Toast.makeText(context, "Error updating cart. Please refresh.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    ((CartActivity) context).runOnUiThread(() ->
                            Toast.makeText(context, "Failed to update quantity. Server error.", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }
    public void qu(CartItem cart, boolean in, boolean de, CartViewHolder holder) {
        String url = "http://10.0.2.2:8080/cart-service/cart/qu/" + cart.getCartId() + "/" + tokenManager.getUserId() + "?in=" + in + "&de=" + de;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + tokenManager.getJwtToken())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle network failure (optional)
                ((CartActivity) context).runOnUiThread(() -> {
                    Toast.makeText(context, "Failed to update quantity. Please try again.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    ((CartActivity) context).runOnUiThread(() -> {
                        // Parse the server's response for updated quantity and price (if available)
                        try {
                            // Update the quantity in the UI
                            int currentQuantity = Integer.parseInt(holder.tvQuantity.getText().toString());

                            if (in) {
                                currentQuantity++;
                            } else if (de && currentQuantity > 1) {
                                currentQuantity--;
                            } else if (de) {
                                // Remove the item from the cart if quantity reaches 0
                                ((CartActivity) context).removeFromCart(cart.getUser(), cart.getCartId());
                                return;
                            }

                            holder.tvQuantity.setText(String.valueOf(currentQuantity));

                            // Update the price in the UI
                            int totalPrice = currentQuantity * cart.getPrice();
                            holder.tvProductPrice.setText("$" + totalPrice);

                        } catch (NumberFormatException e) {
                            // Handle invalid data from server or UI
                            Toast.makeText(context, "Error updating quantity. Please refresh the cart.", Toast.LENGTH_SHORT).show();
                        }
                    });

                } else {
                    // Handle unsuccessful response
                    ((CartActivity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "Failed to update quantity. Server error.", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }
    public static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProductImage;
        TextView tvProductName, tvProductPrice, btnDecreaseQuantity, btnIncreaseQuantity, tvQuantity;
        Button btnRemove;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);

            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            btnRemove = itemView.findViewById(R.id.btnRemove);

            btnDecreaseQuantity = itemView.findViewById(R.id.btnDecreaseQuantity);
            btnIncreaseQuantity = itemView.findViewById(R.id.btnIncreaseQuantity);
        }
    }
}
