package com.example.homerepairs.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homerepairs.R;
import com.google.android.material.card.MaterialCardView;

import java.util.Calendar;
import java.util.List;
import java.util.ArrayList;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private Calendar calendar;
    private Calendar today;
    private List<Integer> days;
    private List<Integer> lowCapacityDays; // Days with limited availability
    private int selectedDay = -1;
    private int selectedPosition = -1;
    private int hoverDay = -1;
    private int hoverPosition = -1;
    private OnDateClickListener onDateClickListener;

    public interface OnDateClickListener {
        void onDateClick(int day, String formattedDate);
    }

    public CalendarAdapter(Calendar calendar, OnDateClickListener listener) {
        this.calendar = (Calendar) calendar.clone();
        this.today = Calendar.getInstance();
        this.today.set(Calendar.HOUR_OF_DAY, 0);
        this.today.set(Calendar.MINUTE, 0);
        this.today.set(Calendar.SECOND, 0);
        this.today.set(Calendar.MILLISECOND, 0);
        this.onDateClickListener = listener;
        this.lowCapacityDays = new ArrayList<>();
        // Example: Day 23 has low capacity (you can extend this to use provider data)
        this.lowCapacityDays.add(23);
        generateDays();
    }

    public void setLowCapacityDays(List<Integer> lowCapacityDays) {
        this.lowCapacityDays = lowCapacityDays != null ? new ArrayList<>(lowCapacityDays) : new ArrayList<>();
        notifyDataSetChanged();
    }

    private void generateDays() {
        days = new ArrayList<>();
        
        // Add day headers
        String[] dayHeaders = {"S", "M", "T", "W", "T", "F", "S"};
        for (int i = 0; i < 7; i++) {
            days.add(-1); // Use -1 to indicate header
        }
        
        // Set calendar to first day of month
        Calendar tempCalendar = (Calendar) calendar.clone();
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1);
        
        // Get first day of week (1 = Sunday, 2 = Monday, etc.)
        int firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK);
        int offset = firstDayOfWeek - 1; // Convert to 0-based (0 = Sunday)
        
        // Add empty cells for days before the first day of month
        for (int i = 0; i < offset; i++) {
            days.add(-2); // Use -2 to indicate empty cell
        }
        
        // Get number of days in month
        int daysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        // Add days of month
        for (int i = 1; i <= daysInMonth; i++) {
            days.add(i);
        }
        
        // Fill remaining cells to make complete weeks
        int remaining = 42 - days.size(); // 6 rows * 7 days = 42
        for (int i = 0; i < remaining; i++) {
            days.add(-2);
        }
    }

    public void updateCalendar(Calendar newCalendar) {
        this.calendar = (Calendar) newCalendar.clone();
        selectedDay = -1;
        selectedPosition = -1;
        hoverDay = -1;
        hoverPosition = -1;
        generateDays();
        notifyDataSetChanged();
    }

    private boolean isLowCapacity(int day) {
        return lowCapacityDays.contains(day);
    }

    public void setSelectedDay(int day) {
        int previousPosition = selectedPosition;
        selectedDay = day;
        selectedPosition = -1;
        
        // Find position of selected day
        for (int i = 0; i < days.size(); i++) {
            if (days.get(i) == day) {
                selectedPosition = i;
                break;
            }
        }
        
        if (previousPosition != -1 && previousPosition < days.size()) {
            notifyItemChanged(previousPosition);
        }
        if (selectedPosition != -1) {
            notifyItemChanged(selectedPosition);
        }
    }

    public int getSelectedDay() {
        return selectedDay;
    }

    private boolean isPastDate(int day) {
        Calendar dateToCheck = (Calendar) calendar.clone();
        dateToCheck.set(Calendar.DAY_OF_MONTH, day);
        dateToCheck.set(Calendar.HOUR_OF_DAY, 0);
        dateToCheck.set(Calendar.MINUTE, 0);
        dateToCheck.set(Calendar.SECOND, 0);
        dateToCheck.set(Calendar.MILLISECOND, 0);
        return dateToCheck.before(today);
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        int day = days.get(position);
        
        if (day == -1) {
            // Header row
            String[] headers = {"S", "M", "T", "W", "T", "F", "S"};
            holder.tvDay.setText(headers[position]);
            holder.tvDay.setTextSize(12);
            holder.cardDay.setCardBackgroundColor(0x00000000);
            holder.cardDay.setCardElevation(0);
            holder.cardDay.setClickable(false);
            holder.tvDay.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
        } else if (day == -2) {
            // Empty cell
            holder.tvDay.setText("");
            holder.cardDay.setCardBackgroundColor(0x00000000);
            holder.cardDay.setCardElevation(0);
            holder.cardDay.setClickable(false);
        } else {
            // Day cell
            holder.tvDay.setText(String.valueOf(day));
            holder.tvDay.setTextSize(14);
            holder.cardDay.setClickable(true);
            
            boolean isPast = isPastDate(day);
            boolean isSelected = (position == selectedPosition && day == selectedDay);
            boolean isHover = (position == hoverPosition && day == hoverDay && !isSelected);
            boolean isLowCapacity = isLowCapacity(day) && !isSelected && !isPast;
            
            if (isSelected) {
                // Selected date - dark blue background, white text (highest priority)
                holder.cardDay.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.calendar_selected_blue));
                holder.tvDay.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_white));
            } else if (isHover) {
                // Hover/secondary selection - light blue background, dark text
                holder.cardDay.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.calendar_hover_blue));
                holder.tvDay.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
            } else if (isLowCapacity) {
                // Low capacity date - light blue background, dark blue text
                holder.cardDay.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.calendar_hover_blue));
                holder.tvDay.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.calendar_selected_blue));
            } else if (isPast) {
                // Past date - faded appearance
                holder.cardDay.setCardBackgroundColor(0x00000000);
                holder.tvDay.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_tertiary));
            } else {
                // Normal available date
                holder.cardDay.setCardBackgroundColor(0x00000000);
                holder.tvDay.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
            }
            
            holder.cardDay.setOnClickListener(v -> {
                if (!isPast) {
                    int previousPosition = selectedPosition;
                    selectedDay = day;
                    selectedPosition = position;
                    hoverDay = -1;
                    hoverPosition = -1;
                    
                    notifyItemChanged(position);
                    if (previousPosition != -1 && previousPosition < days.size()) {
                        notifyItemChanged(previousPosition);
                    }
                    
                    if (onDateClickListener != null) {
                        String[] months = {"January", "February", "March", "April", "May", "June",
                                "July", "August", "September", "October", "November", "December"};
                        int month = calendar.get(Calendar.MONTH);
                        int year = calendar.get(Calendar.YEAR);
                        String formattedDate = months[month] + " " + day + ", " + year;
                        onDateClickListener.onDateClick(day, formattedDate);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return days != null ? days.size() : 0;
    }

    static class CalendarViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay;
        MaterialCardView cardDay;

        CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvDay);
            cardDay = itemView.findViewById(R.id.cardDay);
        }
    }
}
