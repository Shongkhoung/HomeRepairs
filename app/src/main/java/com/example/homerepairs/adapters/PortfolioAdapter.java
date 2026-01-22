package com.example.homerepairs.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homerepairs.R;
import com.example.homerepairs.models.PortfolioItem;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class PortfolioAdapter extends RecyclerView.Adapter<PortfolioAdapter.PortfolioViewHolder> {

    private List<PortfolioItem> portfolioItems;

    public PortfolioAdapter(List<PortfolioItem> portfolioItems) {
        this.portfolioItems = portfolioItems;
    }

    public void updateData(List<PortfolioItem> newItems) {
        this.portfolioItems = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PortfolioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_portfolio, parent, false);
        return new PortfolioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PortfolioViewHolder holder, int position) {
        PortfolioItem item = portfolioItems.get(position);

        // Set title
        if (item.getLabel() != null && !item.getLabel().isEmpty()) {
            holder.tvPortfolioTitle.setText(item.getLabel());
        } else {
            holder.tvPortfolioTitle.setText("Portfolio Item");
        }

        // Set description
        if (item.getDescription() != null && !item.getDescription().isEmpty()) {
            holder.tvPortfolioDescription.setText(item.getDescription());
            holder.tvPortfolioDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvPortfolioDescription.setVisibility(View.GONE);
        }

        // Load image using Glide if available
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            com.bumptech.glide.Glide.with(holder.itemView.getContext())
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.ic_image) // You might need a placeholder drawable
                    .error(R.drawable.ic_alert_circle) // And an error drawable
                    .centerCrop()
                    .into(holder.ivPortfolioIcon);
            // Ensure it looks like an image, not an icon (scale type)
            holder.ivPortfolioIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
            holder.ivPortfolioIcon.setColorFilter(null); // Remove tint
        } else {
            // Set icon based on title/description (Fallback)
            holder.ivPortfolioIcon.setScaleType(ImageView.ScaleType.FIT_CENTER); // Icon scaling
            String title = item.getLabel() != null ? item.getLabel().toLowerCase() : "";
            String description = item.getDescription() != null ? item.getDescription().toLowerCase() : "";

            if (title.contains("kitchen") || title.contains("sink") || description.contains("sink")
                    || description.contains("faucet")) {
                holder.ivPortfolioIcon.setImageResource(R.drawable.hair_washer_sink);
                holder.ivPortfolioIcon
                        .setColorFilter(holder.itemView.getContext().getResources().getColor(R.color.deep_royal_blue));
            } else if (title.contains("bathroom") || title.contains("bathtub") || description.contains("bathroom")
                    || description.contains("bathtub")) {
                holder.ivPortfolioIcon.setImageResource(R.drawable.bath_bathtub);
                holder.ivPortfolioIcon
                        .setColorFilter(holder.itemView.getContext().getResources().getColor(R.color.deep_royal_blue));
            } else if (title.contains("water heater") || title.contains("heater")
                    || description.contains("water heater") || description.contains("heater")) {
                holder.ivPortfolioIcon.setImageResource(R.drawable.hot_water);
                holder.ivPortfolioIcon
                        .setColorFilter(holder.itemView.getContext().getResources().getColor(R.color.warning_red));
            } else {
                // Default icon
                holder.ivPortfolioIcon.setImageResource(R.drawable.ic_briefcase);
                holder.ivPortfolioIcon
                        .setColorFilter(holder.itemView.getContext().getResources().getColor(R.color.deep_royal_blue));
            }
        }
    }

    @Override
    public int getItemCount() {
        return portfolioItems != null ? portfolioItems.size() : 0;
    }

    static class PortfolioViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardPortfolio;
        ImageView ivPortfolioIcon;
        TextView tvPortfolioTitle;
        TextView tvPortfolioDescription;

        PortfolioViewHolder(@NonNull View itemView) {
            super(itemView);
            cardPortfolio = itemView.findViewById(R.id.cardPortfolio);
            ivPortfolioIcon = itemView.findViewById(R.id.ivPortfolioIcon);
            tvPortfolioTitle = itemView.findViewById(R.id.tvPortfolioTitle);
            tvPortfolioDescription = itemView.findViewById(R.id.tvPortfolioDescription);
        }
    }
}
