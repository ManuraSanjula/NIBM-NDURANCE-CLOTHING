package com.ndurance.mobileapp.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ndurance.mobileapp.R;
import com.ndurance.mobileapp.adapter.ImagePreviewAdapter;
import com.ndurance.mobileapp.utils.FileUtils;
import com.ndurance.mobileapp.utils.TokenManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddProductActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_PICK = 1001;
    private TokenManager tokenManager = new TokenManager(this);

    private EditText etName, etDescription, etPrice;
    private Spinner spType;
    private Button btnChooseImages, btnSubmit;
    private RecyclerView rvImagesPreview;
    private String productId;
    private List<Uri> localImageUris; // Images added from device
    private List<String> serverImageUrls; // Images already on the server
    private ImagePreviewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        // Initialize views
        etName = findViewById(R.id.etName);
        etDescription = findViewById(R.id.etDescription);
        etPrice = findViewById(R.id.etPrice);
        spType = findViewById(R.id.category_spinner);
        btnChooseImages = findViewById(R.id.btn_choose_images);
        btnSubmit = findViewById(R.id.btnSubmit);
        rvImagesPreview = findViewById(R.id.rv_images_preview);

        adapter = new ImagePreviewAdapter(
                this,
                localImageUris,
                serverImageUrls,
                this::onImageDelete // Pass the delete listener
        );

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                new String[]{"All", "HATS", "OUTERWEAR", "FOOTWEAR", "WOMENS", "MENS"});
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(categoryAdapter);

        // Initialize data structures
        localImageUris = new ArrayList<>();
        serverImageUrls = new ArrayList<>();

        // Setup RecyclerView
        adapter = new ImagePreviewAdapter(this, localImageUris, serverImageUrls, this::onImageDelete);
        rvImagesPreview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvImagesPreview.setAdapter(adapter);

        // Check if we are updating an existing product
        productId = getIntent().getStringExtra("PRODUCT_ID");
        if (productId != null) {
            fetchProductDetails(productId);
        }

        // Handle image selection
        btnChooseImages.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, REQUEST_IMAGE_PICK);
        });

        // Handle form submission
        btnSubmit.setOnClickListener(v -> submitProduct());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK) {
            if (data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        localImageUris.add(imageUri);
                    }
                } else if (data.getData() != null) {
                    Uri imageUri = data.getData();
                    localImageUris.add(imageUri);
                }
                adapter.notifyDataSetChanged();
            }
        } else {
            Toast.makeText(this, "No images selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchProductDetails(String productId) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url("http://10.0.2.2:8080/product-service/products/" + productId)
                        .get()
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JSONObject product = new JSONObject(responseBody);

                    runOnUiThread(() -> {
                        try {
                            // Populate fields with product details
                            etName.setText(product.getString("name"));
                            etDescription.setText(product.getString("description"));
                            etPrice.setText(product.getString("price"));
                            spType.setSelection(getSpinnerIndex(spType, product.getString("type")));

                            // Populate server image URLs
                            JSONArray images = product.getJSONArray("images");
                            for (int i = 0; i < images.length(); i++) {
                                serverImageUrls.add(images.getString(i));
                            }
                            adapter.notifyDataSetChanged();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Failed to fetch product details", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error fetching product details", Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            }
        }).start();
    }

    private void submitProduct() {
        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String price = etPrice.getText().toString().trim();
        String type = spType.getSelectedItem().toString();

        // Validate fields
        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (description.isEmpty()) {
            Toast.makeText(this, "Description is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (price.isEmpty()) {
            Toast.makeText(this, "Price is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (localImageUris.isEmpty() && getIntent().getStringExtra("PRODUCT_ID") == null) {
            Toast.makeText(this, "At least one image is required", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {

                productId = getIntent().getStringExtra("PRODUCT_ID");

                if (productId != null) {
                    updateProduct();
                }else{
                    addProduct(name, description, price, type, localImageUris);
                }

            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Failed to upload product", Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            }
        }).start();
    }

    private void addProduct(String name, String description, String price, String type, List<Uri> imageUris) throws IOException {
        String jwtToken = tokenManager.getJwtToken();

        OkHttpClient client = new OkHttpClient();

        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);

        builder.addFormDataPart("name", name);
        builder.addFormDataPart("description", description);
        builder.addFormDataPart("price", price);
        builder.addFormDataPart("type", type);

        for (Uri uri : imageUris) {
            File file = new File(FileUtils.getPathFromUri(this, uri));
            builder.addFormDataPart("images", file.getName(), RequestBody.create(file, MediaType.parse("image/*")));
        }

        RequestBody requestBody = builder.build();
        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/product-service/products") // Replace with your actual server URL
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + jwtToken) // Replace with actual JWT token
                .build();

        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            runOnUiThread(() -> Toast.makeText(this, "Product uploaded successfully", Toast.LENGTH_SHORT).show());
        } else {
            runOnUiThread(() -> Toast.makeText(this, "Error: " + response.message(), Toast.LENGTH_SHORT).show());
        }
    }

    private void updateProduct() {

        String jwtToken = tokenManager.getJwtToken();

        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String price = etPrice.getText().toString().trim();
        String type = spType.getSelectedItem().toString();

        if (name.isEmpty() || description.isEmpty() || price.isEmpty() || (localImageUris.isEmpty() && serverImageUrls.isEmpty())) {
            Toast.makeText(this, "All fields and at least one image are required", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);

                // Add fields
                builder.addFormDataPart("name", name);
                builder.addFormDataPart("description", description);
                builder.addFormDataPart("price", price);
                builder.addFormDataPart("type", type);

                // Add new images
                for (Uri uri : localImageUris) {
                    File file = new File(FileUtils.getPathFromUri(this, uri));
                    builder.addFormDataPart("images", file.getName(), RequestBody.create(file, MediaType.parse("image/*")));
                }

                // Add server image URLs
                for (String url : serverImageUrls) {
                    builder.addFormDataPart("serverImages", url);
                }

                RequestBody requestBody = builder.build();
                Request request = new Request.Builder()
                        .url("http://10.0.2.2:8080/product-service/products/" + productId)
                        .put(requestBody)
                        .addHeader("Authorization", "Bearer " + jwtToken) // Replace with actual JWT token
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Product updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Failed to update product", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Error updating product", Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            }
        }).start();
    }

    private void onImageDelete(Uri localUri, String serverUrl) {
        if (localUri != null) {
            localImageUris.remove(localUri);
        }
        if (serverUrl != null) {
            serverImageUrls.remove(serverUrl);
        }
        adapter.notifyDataSetChanged();
    }

    private int getSpinnerIndex(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value)) {
                return i;
            }
        }
        return 0;
    }
}