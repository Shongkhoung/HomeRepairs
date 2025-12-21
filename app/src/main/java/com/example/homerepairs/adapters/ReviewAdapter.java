package com.example.homerepairs.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homerepairs.ProviderProfileActivity;
import com.example.homerepairs.R;

import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private List<ProviderProfileActivity.Review> reviews;

    public ReviewAdapter(List<ProviderProfileActivity.Review> reviews) {
        this.reviews = reviews;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        ProviderProfileActivity.Review review = reviews.get(position);
        
        holder.tvReviewerName.setText(review.getReviewerName());
        holder.tvTimeAgo.setText(review.getTimeAgo());
        holder.tvComment.setText(review.getComment());
        holder.tvThumbsUp.setText(String.valueOf(review.getThumbsUp()));
        holder.tvThumbsDown.setText(String.valueOf(review.getThumbsDown()));
        
        // Set stars
        int rating = review.getRating();
        for (int i = 0; i < 5; i++) {
            ImageView star = holder.stars[i];
            if (i < rating) {
                star.setImageResource(R.drawable.ic_star);
                star.setColorFilter(holder.itemView.getContext().getResources().getColor(R.color.rating_gold));
            } else {
                star.setImageResource(R.drawable.ic_star);
                star.setColorFilter(holder.itemView.getContext().getResources().getColor(R.color.rating_star_empty));
            }
        }
    }

    @Override
    public int getItemCount() {
        return reviews != null ? reviews.size() : 0;
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile;
        TextView tvReviewerName;
        TextView tvTimeAgo;
        ImageView[] stars = new ImageView[5];
        TextView tvComment;
        TextView tvThumbsUp;
        TextView tvThumbsDown;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvReviewerName = itemView.findViewById(R.id.tvReviewerName);
            tvTimeAgo = itemView.findViewById(R.id.tvTimeAgo);
            stars[0] = itemView.findViewById(R.id.star1);
            stars[1] = itemView.findViewById(R.id.star2);
            stars[2] = itemView.findViewById(R.id.star3);
            stars[3] = itemView.findViewById(R.id.star4);
            stars[4] = itemView.findViewById(R.id.star5);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvThumbsUp = itemView.findViewById(R.id.tvThumbsUp);
            tvThumbsDown = itemView.findViewById(R.id.tvThumbsDown);
        }
    }
}

