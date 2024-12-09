package com.ndurance.mobileapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
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

        holder.btnDecreaseQuantity.setOnClickListener(v-> qu(item, false, true, holder));
        holder.btnIncreaseQuantity.setOnClickListener(v-> qu(item, true, false, holder));
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));
    }

    public void qu(CartItem cart, boolean in, boolean de, CartViewHolder holder){
        String url = "http://10.0.2.2:8080/cart-service/cart/qu/" + cart.getCartId() + "/" + tokenManager.getUserId() + "?in=" + in +"&de=" + de;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + tokenManager.getJwtToken())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (response.isSuccessful()) {
                    if(in){

                        String quantityText = holder.tvQuantity.getText().toString();
                        int quantity = Integer.parseInt(quantityText);
                        quantity++;
                        String updatedQuantityText = String.valueOf(quantity);
                        holder.tvQuantity.setText(updatedQuantityText);

                        int price = ( cart.getPrice() * cart.getQuantity() ) + cart.getPrice();
                        holder.tvProductPrice.setText("$" + price);

                    } else if(de){

                        String quantityText = holder.tvQuantity.getText().toString();
                        int quantity = Integer.parseInt(quantityText);
                        quantity--;
                        String updatedQuantityText = String.valueOf(quantity);
                        holder.tvQuantity.setText(updatedQuantityText);

                        int price = ( cart.getPrice() * cart.getQuantity() ) - cart.getPrice();
                        holder.tvProductPrice.setText("$" + price);

                    }
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
