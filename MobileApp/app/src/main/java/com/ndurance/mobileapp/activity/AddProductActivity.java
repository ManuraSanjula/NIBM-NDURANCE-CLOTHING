package com.ndurance.mobileapp.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
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
    private EditText etName, etDescription, etPrice;
    private Spinner categorySpinner;
    private Button btnChooseImages, btnSubmit;
    private RecyclerView rvImagesPreview;
    private List<Uri> imageUris;
    private ImagePreviewAdapter adapter;
    private TokenManager tokenManager = new TokenManager(this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        etName = findViewById(R.id.etName);
        etDescription = findViewById(R.id.etDescription);
        etPrice = findViewById(R.id.etPrice);

        btnChooseImages = findViewById(R.id.btn_choose_images);
        rvImagesPreview = findViewById(R.id.rv_images_preview);
        categorySpinner = findViewById(R.id.category_spinner);
        btnSubmit = findViewById(R.id.btnSubmit);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                new String[]{"All", "HATS", "OUTERWEAR", "FOOTWEAR", "WOMENS", "MENS"});
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        if (rvImagesPreview == null) {
            Log.e("RecyclerViewError", "RecyclerView is null! Check the layout file or ID.");
            return;
        }

        imageUris = new ArrayList<>();
        adapter = new ImagePreviewAdapter(imageUris);

        rvImagesPreview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvImagesPreview.setAdapter(adapter);

        btnChooseImages.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, REQUEST_IMAGE_PICK);
        });

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
                        imageUris.add(imageUri);
                    }
                } else if (data.getData() != null) {
                    Uri imageUri = data.getData();
                    imageUris.add(imageUri);
                }
                adapter.notifyDataSetChanged();
            }
        } else {
            Toast.makeText(this, "No images selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitProduct() {
        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String price = etPrice.getText().toString().trim();
        String type = categorySpinner.getSelectedItem().toString();

        if (name.isEmpty() || description.isEmpty() || price.isEmpty() || type.isEmpty() || imageUris.isEmpty()) {
            Toast.makeText(this, "Please fill all fields and select at least one image", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            try {
                addProduct(name, description, price, type, imageUris);
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Failed to upload product", Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            }
        }).start();
    }

    private void addProduct(String name, String description, String price, String type, List<Uri> imageUris) throws IOException {
        OkHttpClient client = new OkHttpClient();

        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);

        // Add text fields
        builder.addFormDataPart("name", name);
        builder.addFormDataPart("description", description);
        builder.addFormDataPart("price", price);
        builder.addFormDataPart("type", type);

        // Add image files
        for (Uri uri : imageUris) {
            File file = new File(FileUtils.getPathFromUri(this, uri));
            builder.addFormDataPart("images", file.getName(), RequestBody.create(file, MediaType.parse("image/*")));
        }

        String jwtToken = tokenManager.getJwtToken();

        RequestBody requestBody = builder.build();
        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/product-service/products")
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + jwtToken)
                .build();

        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            runOnUiThread(() -> Toast.makeText(this, "Product uploaded successfully", Toast.LENGTH_SHORT).show());
        } else {
            runOnUiThread(() -> Toast.makeText(this, "Error: " + response.message(), Toast.LENGTH_SHORT).show());
        }
    }


}