package com.ndurance.mobileapp.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ndurance.mobileapp.R;

import java.util.List;

public class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ViewHolder> {
    private final Context context;
    private final List<Uri> localImageUris; // Local images selected from the device
    private final List<String> serverImageUrls; // Images already uploaded to the server
    private final ImageDeleteListener imageDeleteListener;

    // Interface to handle image deletion
    public interface ImageDeleteListener {
        void onImageDelete(Uri localUri, String serverUrl);
    }

    public ImagePreviewAdapter(Context context, List<Uri> localImageUris, List<String> serverImageUrls, ImageDeleteListener imageDeleteListener) {
        this.context = context;
        this.localImageUris = localImageUris;
        this.serverImageUrls = serverImageUrls;
        this.imageDeleteListener = imageDeleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image_preview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Determine if the image is local or from the server
        if (position < localImageUris.size()) {
            // Display local image
            Uri localUri = localImageUris.get(position);
            Glide.with(context)
                    .load(localUri)
                    .into(holder.imageView);

            // Handle image delete
            holder.imageView.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Delete Image")
                        .setMessage("Do you want to delete this image?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            imageDeleteListener.onImageDelete(localUri, null);
                        })
                        .setNegativeButton("No", null)
                        .show();
            });
        } else {
            // Display server image
            String serverUrl = "http://10.0.2.2:8080/product-service/products/images/" + serverImageUrls.get(position - localImageUris.size());
            Glide.with(context)
                    .load(serverUrl)
                    .into(holder.imageView);

            // Handle image delete
            holder.imageView.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Delete Image")
                        .setMessage("Do you want to delete this image?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            imageDeleteListener.onImageDelete(null, serverUrl);
                        })
                        .setNegativeButton("No", null)
                        .show();
            });
        }
    }

    @Override
    public int getItemCount() {
        return localImageUris.size() + serverImageUrls.size();
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_preview);
        }
    }
}