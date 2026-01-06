package com.example.homerepairs.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.homerepairs.R;
import com.example.homerepairs.models.Booking;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingCardAdapter extends RecyclerView.Adapter<BookingCardAdapter.BookingViewHolder> {
    private List<Booking> bookings;
    private OnBookingActionListener listener;

    public interface OnBookingActionListener {
        void onBookAgain(Booking booking);

        void onReview(Booking booking);

        void onCallNow(Booking booking);

        void onTrack(Booking booking);

        void onViewDetails(Booking booking);

        void onDelete(Booking booking);
    }

    public BookingCardAdapter(List<Booking> bookings, OnBookingActionListener listener) {
        this.bookings = bookings;
        this.listener = listener;
    }

    public void updateBookings(List<Booking> newBookings) {
        this.bookings = newBookings;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking_card, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookings.get(position);
        holder.bind(booking);
    }

    @Override
    public int getItemCount() {
        return bookings != null ? bookings.size() : 0;
    }

    class BookingViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cardServiceIcon;
        private ImageView ivServiceIcon;
        private TextView tvServiceName;
        private TextView tvProviderName;
        private Chip chipStatus;
        private TextView tvTimeAgo;
        private TextView tvRating;
        private TextView tvTotalCost;
        private TextView tvDuration;
        private MaterialButton btnPrimaryAction;
        private MaterialButton btnSecondaryAction;

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            cardServiceIcon = itemView.findViewById(R.id.cardServiceIcon);
            ivServiceIcon = itemView.findViewById(R.id.ivServiceIcon);
            tvServiceName = itemView.findViewById(R.id.tvServiceName);
            tvProviderName = itemView.findViewById(R.id.tvProviderName);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            tvTimeAgo = itemView.findViewById(R.id.tvTimeAgo);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvTotalCost = itemView.findViewById(R.id.tvTotalCost);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            btnPrimaryAction = itemView.findViewById(R.id.btnPrimaryAction);
            btnSecondaryAction = itemView.findViewById(R.id.btnSecondaryAction);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onViewDetails(bookings.get(getAdapterPosition()));
                }
            });
        }

        void bind(Booking booking) {
            // Set service name
            String serviceName = booking.getServiceName() != null ? booking.getServiceName()
                    : (booking.getServiceCategory() != null
                            ? booking.getServiceCategory()
                                    + itemView.getContext().getString(R.string.default_service_category_suffix)
                            : "Service");
            tvServiceName.setText(serviceName);

            // Set provider name
            tvProviderName.setText(booking.getProviderName() != null ? booking.getProviderName()
                    : itemView.getContext().getString(R.string.default_provider_name));

            // Set status badge
            String status = booking.getStatus() != null ? booking.getStatus()
                    : itemView.getContext().getString(R.string.status_pending);
            chipStatus.setText(status);

            // Set status color based on status
            String statusLower = status.toLowerCase();
            if (statusLower.equals("completed")) {
                chipStatus.setChipBackgroundColorResource(R.color.success);
            } else if (statusLower.equals("in progress")) {
                chipStatus.setChipBackgroundColorResource(R.color.warning);
            } else if (statusLower.equals("confirmed") || statusLower.equals("scheduled")) {
                chipStatus.setChipBackgroundColorResource(R.color.primary_blue);
            } else if (statusLower.equals("cancelled")) {
                chipStatus.setChipBackgroundColorResource(R.color.error);
            } else {
                chipStatus.setChipBackgroundColorResource(R.color.text_secondary);
            }

            // Set time ago
            if (booking.getCreatedAt() != null) {
                // For scheduled bookings, show "Just now" if created recently
                if (statusLower.equals("scheduled")) {
                    long now = System.currentTimeMillis();
                    long diff = now - booking.getCreatedAt().getTime();
                    if (diff < 60000) { // Less than 1 minute
                        tvTimeAgo.setText("Just now");
                    } else {
                        tvTimeAgo.setText(getTimeAgo(booking.getCreatedAt(), itemView.getContext()));
                    }
                } else {
                    tvTimeAgo.setText(getTimeAgo(booking.getCreatedAt(), itemView.getContext()));
                }
            } else {
                tvTimeAgo.setText(
                        statusLower.equals("scheduled") ? itemView.getContext().getString(R.string.time_just_now)
                                : itemView.getContext().getString(R.string.time_recently));
            }

            // Set rating (default or from booking if available)
            tvRating.setText("4.9"); // Default rating, can be enhanced with actual rating data

            // Set total cost (default, can be enhanced with actual cost data)
            tvTotalCost.setText(com.example.homerepairs.utils.CurrencyHelper.formatPrice(itemView.getContext(), 50.0)); // Default
                                                                                                                        // placeholder
                                                                                                                        // price

            // Set duration (default, can be enhanced with actual duration data)
            tvDuration.setText(itemView.getContext().getString(R.string.default_duration));

            // Set service icon based on category
            int iconResId = getServiceIcon(booking.getServiceCategory());
            ivServiceIcon.setImageResource(iconResId);

            // Set action buttons based on status
            setupActionButtons(booking, status);

            // Set click listeners
            btnPrimaryAction.setOnClickListener(v -> {
                if (listener != null) {
                    if (status.equalsIgnoreCase("completed")) {
                        listener.onBookAgain(booking);
                    } else if (status.equalsIgnoreCase("in progress")) {
                        listener.onTrack(booking); // Track/Call
                    } else {
                        // Pending/Scheduled/Others -> View Details
                        listener.onViewDetails(booking);
                    }
                }
            });

            btnSecondaryAction.setOnClickListener(v -> {
                if (listener != null) {
                    if (status.equalsIgnoreCase("completed")) {
                        listener.onReview(booking);
                    } else if (status.equalsIgnoreCase("in progress")) {
                        listener.onCallNow(booking);
                    } else {
                        // Pending/Scheduled -> Cancel
                        listener.onDelete(booking);
                    }
                }
            });
        }

        private void setupActionButtons(Booking booking, String status) {
            if (status.equalsIgnoreCase("completed")) {
                // Primary: Book Again (Deep Blue)
                btnPrimaryAction.setText(itemView.getContext().getString(R.string.btn_book_again));
                btnPrimaryAction.setIconResource(R.drawable.ic_calendar);
                btnPrimaryAction.setBackgroundTintList(
                        ContextCompat.getColorStateList(itemView.getContext(), R.color.deep_royal_blue));
                btnPrimaryAction.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
                btnPrimaryAction.setIconTint(ContextCompat.getColorStateList(itemView.getContext(), R.color.white));

                // Secondary: Review (Gray)
                btnSecondaryAction.setText(itemView.getContext().getString(R.string.btn_review));
                btnSecondaryAction.setIconResource(R.drawable.message);
                btnSecondaryAction.setVisibility(View.VISIBLE);

            } else if (status.equalsIgnoreCase("in progress")) {
                // Primary: Track Job (Deep Blue)
                btnPrimaryAction.setText(itemView.getContext().getString(R.string.btn_track_job));
                btnPrimaryAction.setIconResource(R.drawable.ic_map_pin);
                btnPrimaryAction.setBackgroundTintList(
                        ContextCompat.getColorStateList(itemView.getContext(), R.color.deep_royal_blue));

                // Secondary: Call Provider (Gray)
                btnSecondaryAction.setText(itemView.getContext().getString(R.string.btn_call_now));
                btnSecondaryAction.setIconResource(R.drawable.ic_phone);
                btnSecondaryAction.setVisibility(View.VISIBLE);

            } else {
                // Pending, Scheduled, etc.
                // Primary: View Details (Deep Blue)
                btnPrimaryAction.setText(itemView.getContext().getString(R.string.btn_view_details));
                btnPrimaryAction.setIconResource(R.drawable.ic_document);
                btnPrimaryAction.setBackgroundTintList(
                        ContextCompat.getColorStateList(itemView.getContext(), R.color.deep_royal_blue));
                btnPrimaryAction.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
                btnPrimaryAction.setIconTint(ContextCompat.getColorStateList(itemView.getContext(), R.color.white));

                // Secondary: Cancel (Gray/White)
                btnSecondaryAction.setText(itemView.getContext().getString(R.string.btn_cancel));
                btnSecondaryAction.setIconResource(R.drawable.ic_arrow_back); // Using arrow back as a placeholder for
                                                                              // simple icon or none
                // Ideally use specific cancel icon or generic 'x' if available, but arrow back
                // is what was in code
                // Let's check if we have a close or delete icon
                btnSecondaryAction.setIconResource(R.drawable.ic_close); // Using close/cancel icon
                btnSecondaryAction.setBackgroundTintList(
                        ContextCompat.getColorStateList(itemView.getContext(), R.color.background_gray)); // Light gray
                btnSecondaryAction.setVisibility(View.VISIBLE);
            }
        }
    }

    private int getServiceIcon(String category) {
        if (category == null)
            return R.drawable.handyman;

        switch (category.toLowerCase()) {
            case "plumbing":
                return R.drawable.plumbing;
            case "electrical":
            case "electrical work":
                return R.drawable.electrical;
            case "hvac":
                return R.drawable.hvac;
            case "cleaning":
                return R.drawable.cleaning;
            case "carpentry":
                return R.drawable.carpentry;
            case "painting":
                return R.drawable.painting;
            case "roofing":
                return R.drawable.roofing;
            case "landscaping":
                return R.drawable.landscaping;
            case "appliance repair":
                return R.drawable.appliance_repair;
            default:
                return R.drawable.handyman;
        }
    }

    private String getTimeAgo(Date date, android.content.Context context) {
        long now = System.currentTimeMillis();
        long diff = now - date.getTime();

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return context.getString(R.string.time_days_ago, days);
        } else if (hours > 0) {
            return context.getString(R.string.time_hours_ago, hours);
        } else if (minutes > 0) {
            return context.getString(R.string.time_minutes_ago, minutes);
        } else {
            return context.getString(R.string.time_just_now);
        }
    }
}
