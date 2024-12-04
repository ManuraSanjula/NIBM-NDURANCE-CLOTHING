package com.ndurance.mobileapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.ArrayList;
import java.util.List;
import com.ndurance.mobileapp.R;
import com.ndurance.mobileapp.adapter.ProductAdapter;
import com.ndurance.mobileapp.model.dto.Product;
import com.ndurance.mobileapp.model.response.ProductResponse;
import com.ndurance.mobileapp.service.ProductService;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private ImageView carIcon, order_icon;
    private ImageView profile_icon;
    private TextView errorMessage;
    private EditText searchField, minPriceField, maxPriceField;
    private Spinner categorySpinner;
    private List<Product> originalProductList;
    private List<Product> filteredProductList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        originalProductList = new ArrayList<>();
        filteredProductList = new ArrayList<>();

        recyclerView = findViewById(R.id.product_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        carIcon = findViewById(R.id.cart_icon);
        order_icon = findViewById(R.id.order_icon);
        profile_icon = findViewById(R.id.profile_icon);
        errorMessage = findViewById(R.id.error_message);

        searchField = findViewById(R.id.search_field);
        minPriceField = findViewById(R.id.min_price);
        maxPriceField = findViewById(R.id.max_price);
        categorySpinner = findViewById(R.id.category_spinner);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                new String[]{"All", "HATS", "OUTERWEAR", "FOOTWEAR", "WOMENS", "MENS"});
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        carIcon.setOnClickListener(view -> {
            Intent intent = new Intent(HomeActivity.this, CartActivity.class);
            startActivity(intent);
        });

        order_icon.setOnClickListener(view -> {
            Intent intent = new Intent(HomeActivity.this, ActivityOrder.class);
            startActivity(intent);
        });

        profile_icon.setOnClickListener(view -> {
            Intent intent = new Intent(HomeActivity.this, UserActivity.class);
            startActivity(intent);
        });

        fetchProducts(0, 20);
        setupSearchAndFilterListeners();

    }

    private void setupSearchAndFilterListeners() {
        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProducts();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        minPriceField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProducts();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        maxPriceField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProducts();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterProducts();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void filterProducts() {
        // Check if originalProductList has been populated
        if (originalProductList == null || originalProductList.isEmpty()) {
            Log.d("DEBUG", "No products available for filtering.");
            return;
        }

        String searchQuery = searchField.getText().toString().toLowerCase();
        String selectedCategory = categorySpinner.getSelectedItem().toString();
        String minPriceText = minPriceField.getText().toString();
        String maxPriceText = maxPriceField.getText().toString();

        double minPrice = minPriceText.isEmpty() ? 0 : Double.parseDouble(minPriceText);
        double maxPrice = maxPriceText.isEmpty() ? Double.MAX_VALUE : Double.parseDouble(maxPriceText);

        filteredProductList.clear();

        for (Product product : originalProductList) {
            boolean matchesSearch = product.getName() != null && product.getName().toLowerCase().contains(searchQuery);
            boolean matchesCategory = selectedCategory.equals("All") ||
                    (product.getType() != null && product.getType().toString().equalsIgnoreCase(selectedCategory));
            boolean matchesPrice = product.getPrice() >= minPrice && product.getPrice() <= maxPrice;

            if (matchesSearch && matchesCategory && matchesPrice) {
                filteredProductList.add(product);
            }
        }

        adapter.updateData(filteredProductList);

        Log.d("DEBUG", "Filtered products count: " + filteredProductList.size());
    }

    private void fetchProducts(int page, int size) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/product-service/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ProductService productService = retrofit.create(ProductService.class);

        productService.getProducts(page, size).enqueue(new Callback<ProductResponse>() {
            @Override
            public void onResponse(Call<ProductResponse> call, Response<ProductResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Fetch all products and initialize lists
                    originalProductList = response.body().getContent();
                    filteredProductList = new ArrayList<>(originalProductList);

                    originalProductList.forEach(product -> {
                        if (product.getImages() != null && !product.getImages().isEmpty()) {
                            product.setImageURL("http://10.0.2.2:8080/product-service/products/images/" + product.getImages().get(0));
                        } else {
                            product.setImageURL("http://10.0.2.2:8080/product-service/default_image.png");
                        }
                    });

                    adapter = new ProductAdapter(HomeActivity.this, filteredProductList);
                    recyclerView.setAdapter(adapter);

                    Log.d("DEBUG", "Products fetched successfully: " + originalProductList.size());
                } else {
                    errorMessage.setVisibility(View.VISIBLE);
                    Toast.makeText(HomeActivity.this, "Failed to fetch products", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ProductResponse> call, Throwable t) {
                errorMessage.setVisibility(View.VISIBLE);
                Toast.makeText(HomeActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}