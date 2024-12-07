package com.ndurance.mobileapp.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ndurance.mobileapp.R;
import com.ndurance.mobileapp.model.dto.Comment;
import com.ndurance.mobileapp.utils.TokenManager;

import java.io.InputStream;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.ViewHolder> {
    private final List<Comment> comments;
    public static TokenManager tokenManager;
    private static Context context;
    public CommentsAdapter(Context context, List<Comment> comments) {
        this.comments = comments;
        this.context = context;
        this.tokenManager = new TokenManager(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.userName.setText(comment.getEmail());
        holder.commentText.setText(comment.getComment());
        Glide.with(holder.userProfilePic.getContext()).load(comment.getPic()).into(holder.userProfilePic);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    private static void fetchUserProfilePicture(String userId, ImageView userProfilePic) {
        new Thread(() -> {
            try {
                String jwtToken = tokenManager.getJwtToken();
                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url("http://10.0.2.2:8080/user-service/users/image/" + userId)
                        .addHeader("Authorization", "Bearer " + jwtToken)
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    InputStream inputStream = response.body().byteStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                    // Update the UI on the main thread
                    if (context instanceof Activity) {
                        ((Activity) context).runOnUiThread(() -> userProfilePic.setImageBitmap(bitmap));
                    }                } else {
                    Log.e("ProfileImage", "Failed to fetch image: " + response.message());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView userName, commentText;
        ImageView userProfilePic;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userName);
            commentText = itemView.findViewById(R.id.commentText);
            userProfilePic = itemView.findViewById(R.id.userProfilePic);
            fetchUserProfilePicture(CommentsAdapter.tokenManager.getUserId(), userProfilePic);
        }
    }
}
