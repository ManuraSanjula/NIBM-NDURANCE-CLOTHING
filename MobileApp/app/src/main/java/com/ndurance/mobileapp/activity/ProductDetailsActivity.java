package com.ndurance.mobileapp.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.gson.Gson;
import com.ndurance.mobileapp.R;
import com.ndurance.mobileapp.adapter.CommentsAdapter;
import com.ndurance.mobileapp.adapter.ImageSliderAdapter;
import com.ndurance.mobileapp.model.Request.CartRequestModel;
import com.ndurance.mobileapp.model.dto.Comment;
import com.ndurance.mobileapp.utils.TokenManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProductDetailsActivity extends AppCompatActivity {
    private static final String TAG = "ProductDetailsActivity";
    private ViewPager2 imageSlider;
    private TextView productName, productPrice, productCategory;
    private Button btnSubmitComment;
    private EditText commentInput;
    private RecyclerView commentsRecyclerView;
    private int currentImageIndex = 0;
    private List<String> imageUrls = new ArrayList<>();
    private List<Comment> commentsList = new ArrayList<>();
    private CommentsAdapter commentsAdapter;
    private final OkHttpClient client = new OkHttpClient();
    private Button btnPrevImage, btnNextImage, btnOrder, btnCart, btnUpdate, btnDelete;
    private TokenManager tokenManager = new TokenManager(this);
    private String productId;
    private String product_name;
    private double product_price;
    private List<String> productImages = new ArrayList<>();
    private SharedPreferences prefs; //--

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);

        String userId = tokenManager.getUserId();
        String jwtToken = tokenManager.getJwtToken();


        if(userId == null || jwtToken == null){
            Intent intent = new Intent(ProductDetailsActivity.this, MainActivity.class);
            startActivity(intent);
        }

        prefs = this.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE); //--
        productId = getIntent().getStringExtra("productId");

        initializeViews(this, productId);

        setupCommentsRecyclerView();

        fetchProductDetails(productId);
        fetchComments(productId);

        btnSubmitComment.setOnClickListener(v -> submitComment(productId));
        btnDelete = findViewById(R.id.btnDelete);

        btnCart.setOnClickListener(v -> {
            if (userId == null || userId.isEmpty()) {
                Toast.makeText(this, "User ID not available!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (productId == null || product_name == null || productImages.isEmpty()) {
                Toast.makeText(this, "Product details are incomplete!", Toast.LENGTH_SHORT).show();
                return;
            }
            addToCart(userId);
        });

        btnDelete.setOnClickListener(v->{
            deleteProduct(productId);
        });
    }

    public void btnOrder(String productId, String userId, double price){
        String token = tokenManager.getJwtToken();

        if (token == null || token.isEmpty()) {
            runOnUiThread(() -> Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show());
            return;
        }

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                JSONObject orderJsonObject = new JSONObject();
                JSONObject productJsonObject = new JSONObject();

                productJsonObject.put(productId, price);

                orderJsonObject.put("user", userId);
                orderJsonObject.put("products", productJsonObject);

                RequestBody body = RequestBody.create(orderJsonObject.toString(), MediaType.parse("application/json"));

                Request request = new Request.Builder()
                        .url("http://10.0.2.2:8080/order-service/orders?addressSame=false&changeAddress=false") // Replace with your base URL localhost:8080/order-service/orders
                        .post(body)
                        .addHeader("Authorization", "Bearer " + token)
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(this, "Product Ordered successfully", Toast.LENGTH_SHORT).show());
                } else {
                    String errorMessage = response.message();
                    runOnUiThread(() -> Toast.makeText(this, "Failed to Order the product: " + errorMessage, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error Order product: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            }
        }).start();
    }
    private void deleteProduct(String productId) {
        String token = tokenManager.getJwtToken();

        if (token == null || token.isEmpty()) {
            runOnUiThread(() -> Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show());
            return;
        }

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url("http://10.0.2.2:8080/product-service/products/" + productId) // Replace with your base URL
                        .delete()
                        .addHeader("Authorization", "Bearer " + token)
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(this, "Product deleted successfully", Toast.LENGTH_SHORT).show());
                } else {
                    String errorMessage = response.message();
                    runOnUiThread(() -> Toast.makeText(this, "Failed to delete product: " + errorMessage, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error deleting product: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            }
        }).start();
    }
    public String getRole() {
        return prefs.getString("ROLE", null); // Corrected key to "ROLE"
    }
    private void initializeViews(Context context, String productId) {
        imageSlider = findViewById(R.id.imageSlider);
        productName = findViewById(R.id.productName);
        productPrice = findViewById(R.id.productPrice);
        productCategory = findViewById(R.id.productCategory);
        btnPrevImage = findViewById(R.id.btnPrevImage);
        btnNextImage = findViewById(R.id.btnNextImage);
        btnOrder = findViewById(R.id.btnOrder);
        btnCart = findViewById(R.id.btnCart);

        btnUpdate = findViewById(R.id.btnUpdate);
        btnDelete = findViewById(R.id.btnDelete);

        if(!getRole().isEmpty() || getRole() != null){
            if(getRole().equals("ROLE_ADMIN")){
                btnUpdate.setVisibility(View.VISIBLE);
                btnDelete.setVisibility(View.VISIBLE);

                btnOrder.setVisibility(View.GONE);
                btnCart.setVisibility(View.GONE);

                btnUpdate.setOnClickListener(v->{
                    Intent intent = new Intent(context, AddProductActivity.class);
                    intent.putExtra("PRODUCT_ID", productId);
                    context.startActivity(intent);
                });
            }
        }

        btnSubmitComment = findViewById(R.id.btnSubmitComment);
        commentInput = findViewById(R.id.commentInput);
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView);

        btnPrevImage.setOnClickListener(v -> navigateImages(-1));
        btnNextImage.setOnClickListener(v -> navigateImages(1));
    }

    private void setupCommentsRecyclerView() {
        commentsAdapter = new CommentsAdapter(this, commentsList);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentsRecyclerView.setAdapter(commentsAdapter);
    }

    private void fetchProductDetails(String productId) {
        String url = "http://10.0.2.2:8080/product-service/products/" + productId;

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ProductDetailsActivity.this, "Failed to load product details", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(ProductDetailsActivity.this, "Error: " + response.message(), Toast.LENGTH_SHORT).show());
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    JSONObject productData = new JSONObject(responseBody);

                    product_name = productData.getString("name");
                    product_price = productData.getDouble("price");

                    String name = productData.getString("name");
                    double price = productData.getDouble("price");
                    String category = productData.getString("type");

                    JSONArray images = productData.getJSONArray("images");
                    imageUrls.clear();
                    for (int i = 0; i < images.length(); i++) {
                        imageUrls.add(images.getString(i));
                        productImages.add(images.getString(i));
                    }

                    runOnUiThread(() -> {
                        productName.setText(name);
                        productPrice.setText("Price: $" + price);
                        productCategory.setText("Category: " + category);

                        ImageSliderAdapter adapter = new ImageSliderAdapter(imageUrls);
                        imageSlider.setAdapter(adapter);
                    });

                    btnOrder.setOnClickListener(v->{
                        btnOrder(productId, tokenManager.getUserId(), product_price);
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse product details", e);
                    runOnUiThread(() -> Toast.makeText(ProductDetailsActivity.this, "Failed to parse product details", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void fetchComments(String productId) {
        String url = "http://10.0.2.2:8080/product-service/products/" + productId + "/comments";

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ProductDetailsActivity.this, "Failed to load comments", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(ProductDetailsActivity.this, "Error: " + response.message(), Toast.LENGTH_SHORT).show());
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    JSONArray commentsArray = new JSONArray(responseBody);

                    commentsList.clear();
                    for (int i = 0; i < commentsArray.length(); i++) {
                        JSONObject commentObj = commentsArray.getJSONObject(i);
                        Comment comment = new Gson().fromJson(commentObj.toString(), Comment.class);
                        commentsList.add(comment);
                    }

                    runOnUiThread(() -> commentsAdapter.notifyDataSetChanged());
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse comments", e);
                    runOnUiThread(() -> Toast.makeText(ProductDetailsActivity.this, "Failed to parse comments", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void navigateImages(int direction) {
        currentImageIndex += direction;
        if (currentImageIndex < 0) currentImageIndex = imageUrls.size() - 1;
        if (currentImageIndex >= imageUrls.size()) currentImageIndex = 0;
        imageSlider.setCurrentItem(currentImageIndex, true);
    }

    private void postComment(String productId, String userId, String token, String newComment) {
        String url = "http://10.0.2.2:8080/product-service/products/comments/" + userId;

        JSONObject commentData = new JSONObject();
        try {
            commentData.put("comment", newComment);
            commentData.put("clothPublicId", productId);
            commentData.put("userPublicId", userId);

        } catch (Exception e) {
            Toast.makeText(this, "Failed to create comment data", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody body = RequestBody.create(commentData.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ProductDetailsActivity.this, "Failed to post comment", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(ProductDetailsActivity.this, "Error: " + response.message(), Toast.LENGTH_SHORT).show());
                    return;
                }

                runOnUiThread(() -> {
                    commentInput.setText("");
                    fetchComments(productId); // Refresh comments
                });
            }
        });
    }

    private void addToCart(String userId) {
        String url = "http://10.0.2.2:8080/cart-service/cart/" + userId;
        String jwtToken = tokenManager.getJwtToken();

        // Create CartRequestModel
        CartRequestModel cartRequest = new CartRequestModel(
                productId,
                (int) product_price, // assuming price is an integer
                1, // default quantity
                product_name,
                productImages
        );

        // Convert CartRequestModel to JSON
        Gson gson = new Gson();
        String jsonBody = gson.toJson(cartRequest);

        // Create RequestBody
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

        // Create POST Request
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + jwtToken)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("Error", e.toString());
                runOnUiThread(() -> Toast.makeText(ProductDetailsActivity.this, "Failed to add to cart", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(ProductDetailsActivity.this, "Error: " + response.message(), Toast.LENGTH_SHORT).show());
                    return;
                }

                runOnUiThread(() -> Toast.makeText(ProductDetailsActivity.this, "Product added to cart successfully!", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void submitComment(String productId) {
        String commentText = commentInput.getText().toString().trim();
        if (commentText.isEmpty()) {
            Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = tokenManager.getUserId();
        String jwtToken = tokenManager.getJwtToken();

        if (userId.isEmpty() || jwtToken.isEmpty()) {
            Toast.makeText(this, "An Error", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            postComment(productId, userId, jwtToken, commentText);

            Comment newComment = new Comment(commentText, null, "User@example.com");
            commentsList.add(0, newComment);
            commentsAdapter.notifyItemInserted(0);
            commentInput.setText("");

            Toast.makeText(this, "Comment added!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}