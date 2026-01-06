package com.example.homerepairs;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ServiceDateTimeActivity extends AppCompatActivity {

    private TextView tvMonthYear;
    private LinearLayout llCalendarGrid;
    private LinearLayout llWeekView;
    private LinearLayout llViewToggle;
    private Button btnWeek;
    private Button btnMonth;
    private LinearLayout llAvailableTimes;
    private MaterialCardView cardFlexibleTiming;
    private Button btnContinue;

    private String viewMode = "month"; // "week" or "month"
    private int selectedDate = -1;
    private String selectedTime = "";
    private boolean isFlexible = false;
    private Calendar currentCalendar;
    private SimpleDateFormat monthYearFormat;
    private SimpleDateFormat dayFormat;
    private SimpleDateFormat dateFormat;

    private String[] daysOfWeek = { "S", "M", "T", "W", "T", "F", "S" };
    private String[] availableTimes = {
            "8:00 AM", "9:00 AM", "10:00 AM",
            "11:00 AM", "12:00 PM", "1:00 PM",
            "2:00 PM", "3:00 PM", "4:00 PM"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_date_time);

        // Set status bar color to white
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(
                        getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        currentCalendar = Calendar.getInstance();
        monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        dateFormat = new SimpleDateFormat("MMM", Locale.getDefault());

        initializeViews();
        setupViewToggle();
        setupCalendar();
        setupAvailableTimes();
        setupFlexibleTiming();
        setupButtons();

        // Set default selections - select tomorrow or next available date
        Calendar today = Calendar.getInstance();
        Calendar tomorrow = (Calendar) today.clone();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);

        // If viewing current month, select tomorrow, otherwise select first day
        if (currentCalendar.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
                currentCalendar.get(Calendar.MONTH) == tomorrow.get(Calendar.MONTH)) {
            selectedDate = tomorrow.get(Calendar.DAY_OF_MONTH);
        } else {
            selectedDate = 1;
        }

        selectedTime = "10:00 AM";
        updateTimeSelection();
    }

    private void initializeViews() {
        tvMonthYear = findViewById(R.id.tvMonthYear);
        llCalendarGrid = findViewById(R.id.llCalendarGrid);
        llWeekView = findViewById(R.id.llWeekView);
        llViewToggle = findViewById(R.id.llViewToggle);
        btnWeek = findViewById(R.id.btnWeek);
        btnMonth = findViewById(R.id.btnMonth);
        llAvailableTimes = findViewById(R.id.llAvailableTimes);
        cardFlexibleTiming = findViewById(R.id.cardFlexibleTiming);
        btnContinue = findViewById(R.id.btnContinue);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        ImageButton btnPrevMonth = findViewById(R.id.btnPrevMonth);
        ImageButton btnNextMonth = findViewById(R.id.btnNextMonth);
        btnPrevMonth.setOnClickListener(v -> {
            if (viewMode.equals("month")) {
                currentCalendar.add(Calendar.MONTH, -1);
                setupCalendar();
            } else {
                currentCalendar.add(Calendar.WEEK_OF_YEAR, -1);
                setupWeekView();
            }
        });
        btnNextMonth.setOnClickListener(v -> {
            if (viewMode.equals("month")) {
                currentCalendar.add(Calendar.MONTH, 1);
                setupCalendar();
            } else {
                currentCalendar.add(Calendar.WEEK_OF_YEAR, 1);
                setupWeekView();
            }
        });
    }

    private void setupViewToggle() {
        btnWeek.setOnClickListener(v -> {
            viewMode = "week";
            updateViewToggle();
            setupWeekView();
        });

        btnMonth.setOnClickListener(v -> {
            viewMode = "month";
            updateViewToggle();
            setupCalendar();
        });

        updateViewToggle();
    }

    private void updateViewToggle() {
        if (viewMode.equals("week")) {
            btnWeek.setBackgroundResource(R.drawable.view_toggle_selected);
            btnWeek.setTextColor(ContextCompat.getColor(this, R.color.white));
            btnMonth.setBackgroundResource(R.drawable.view_toggle_unselected);
            btnMonth.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            llCalendarGrid.setVisibility(View.GONE);
            llWeekView.setVisibility(View.VISIBLE);
        } else {
            btnMonth.setBackgroundResource(R.drawable.view_toggle_selected);
            btnMonth.setTextColor(ContextCompat.getColor(this, R.color.white));
            btnWeek.setBackgroundResource(R.drawable.view_toggle_unselected);
            btnWeek.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            llCalendarGrid.setVisibility(View.VISIBLE);
            llWeekView.setVisibility(View.GONE);
        }
    }

    private void setupCalendar() {
        tvMonthYear.setText(monthYearFormat.format(currentCalendar.getTime()));

        // Clear existing views
        llCalendarGrid.removeAllViews();

        // Add day headers
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        for (String day : daysOfWeek) {
            TextView dayHeader = new TextView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            dayHeader.setLayoutParams(params);
            dayHeader.setText(day);
            dayHeader.setTextSize(14);
            dayHeader.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            dayHeader.setGravity(android.view.Gravity.CENTER);
            dayHeader.setPadding(0, 0, 0, 16);
            headerRow.addView(dayHeader);
        }
        llCalendarGrid.addView(headerRow);

        // Get first day of month and number of days
        Calendar monthStart = (Calendar) currentCalendar.clone();
        monthStart.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = monthStart.get(Calendar.DAY_OF_WEEK);
        int daysInMonth = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Get today's date
        Calendar today = Calendar.getInstance();
        boolean isCurrentMonth = today.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
                today.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH);
        int todayDay = today.get(Calendar.DAY_OF_MONTH);

        // Create calendar grid
        int dayNumber = 1;
        for (int week = 0; week < 6; week++) {
            LinearLayout weekRow = new LinearLayout(this);
            weekRow.setOrientation(LinearLayout.HORIZONTAL);
            weekRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            for (int day = 0; day < 7; day++) {
                Button dayButton = new Button(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                params.setMargins(4, 4, 4, 4);
                dayButton.setLayoutParams(params);
                dayButton.setMinHeight(0);
                dayButton.setPadding(8, 16, 8, 16);

                if (week == 0 && day < firstDayOfWeek - 1) {
                    // Empty cell before first day
                    dayButton.setVisibility(View.INVISIBLE);
                } else if (dayNumber <= daysInMonth) {
                    final int currentDay = dayNumber;
                    dayButton.setText(String.valueOf(dayNumber));

                    // Check if date is in the past or today
                    boolean isPast = isCurrentMonth && currentDay < todayDay;
                    boolean isToday = isCurrentMonth && currentDay == todayDay;

                    if (isPast) {
                        dayButton.setEnabled(false);
                        dayButton.setTextColor(ContextCompat.getColor(this, R.color.text_hint));
                        dayButton.setBackgroundResource(R.drawable.calendar_day_disabled);
                    } else {
                        dayButton.setEnabled(true);
                        if (selectedDate == currentDay) {
                            dayButton.setBackgroundResource(R.drawable.calendar_day_selected);
                            dayButton.setTextColor(ContextCompat.getColor(this, R.color.white));
                        } else {
                            dayButton.setBackgroundResource(R.drawable.calendar_day_unselected);
                            dayButton.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                        }

                        dayButton.setOnClickListener(v -> {
                            selectedDate = currentDay;
                            setupCalendar();
                        });
                    }

                    dayNumber++;
                } else {
                    dayButton.setVisibility(View.INVISIBLE);
                }

                weekRow.addView(dayButton);
            }

            llCalendarGrid.addView(weekRow);
            if (dayNumber > daysInMonth)
                break;
        }
    }

    private void setupWeekView() {
        tvMonthYear.setText("This Week");

        // Clear existing views
        llWeekView.removeAllViews();

        // Create week view
        LinearLayout weekRow = new LinearLayout(this);
        weekRow.setOrientation(LinearLayout.HORIZONTAL);
        weekRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        Calendar weekStart = (Calendar) currentCalendar.clone();
        // Set to Monday of current week
        int dayOfWeek = weekStart.get(Calendar.DAY_OF_WEEK);
        int daysFromMonday = (dayOfWeek == Calendar.SUNDAY) ? 6 : dayOfWeek - Calendar.MONDAY;
        weekStart.add(Calendar.DAY_OF_MONTH, -daysFromMonday);

        for (int i = 0; i < 7; i++) {
            Calendar day = (Calendar) weekStart.clone();
            day.add(Calendar.DAY_OF_MONTH, i);

            LinearLayout dayContainer = new LinearLayout(this);
            dayContainer.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            params.setMargins(4, 4, 4, 4);
            dayContainer.setLayoutParams(params);
            dayContainer.setGravity(android.view.Gravity.CENTER);
            dayContainer.setPadding(8, 16, 8, 16);

            int dayOfMonth = day.get(Calendar.DAY_OF_MONTH);
            int dayMonth = day.get(Calendar.MONTH);
            int dayYear = day.get(Calendar.YEAR);

            // Check if this day matches selected date (considering month/year)
            Calendar selectedCal = (Calendar) currentCalendar.clone();
            selectedCal.set(Calendar.DAY_OF_MONTH, selectedDate);
            boolean isSelected = selectedDate == dayOfMonth &&
                    selectedCal.get(Calendar.MONTH) == dayMonth &&
                    selectedCal.get(Calendar.YEAR) == dayYear;

            dayContainer.setBackgroundResource(
                    isSelected ? R.drawable.calendar_day_selected : R.drawable.calendar_day_unselected);

            TextView dayName = new TextView(this);
            dayName.setText(dayFormat.format(day.getTime()).substring(0, 3));
            dayName.setTextSize(12);
            dayName.setTextColor(isSelected ? ContextCompat.getColor(this, R.color.white)
                    : ContextCompat.getColor(this, R.color.text_secondary));
            dayName.setGravity(android.view.Gravity.CENTER);

            TextView dayNumber = new TextView(this);
            dayNumber.setText(String.valueOf(dayOfMonth));
            dayNumber.setTextSize(18);
            dayNumber.setTypeface(null, android.graphics.Typeface.BOLD);
            dayNumber.setTextColor(isSelected ? ContextCompat.getColor(this, R.color.white)
                    : ContextCompat.getColor(this, R.color.text_primary));
            dayNumber.setGravity(android.view.Gravity.CENTER);

            TextView monthName = new TextView(this);
            monthName.setText(dateFormat.format(day.getTime()));
            monthName.setTextSize(12);
            monthName.setTextColor(isSelected ? ContextCompat.getColor(this, R.color.white)
                    : ContextCompat.getColor(this, R.color.text_secondary));
            monthName.setGravity(android.view.Gravity.CENTER);

            dayContainer.addView(dayName);
            dayContainer.addView(dayNumber);
            dayContainer.addView(monthName);

            final int currentDay = dayOfMonth;
            final int currentMonth = dayMonth;
            final int currentYear = dayYear;
            dayContainer.setOnClickListener(v -> {
                selectedDate = currentDay;
                // Update current calendar to match selected week
                currentCalendar.set(Calendar.YEAR, currentYear);
                currentCalendar.set(Calendar.MONTH, currentMonth);
                currentCalendar.set(Calendar.DAY_OF_MONTH, currentDay);
                setupWeekView();
            });

            weekRow.addView(dayContainer);
        }

        llWeekView.addView(weekRow);
    }

    private void setupAvailableTimes() {
        llAvailableTimes.removeAllViews();

        for (int i = 0; i < availableTimes.length; i++) {
            if (i % 3 == 0) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.setMargins(0, 0, 0, 12);
                row.setLayoutParams(rowParams);
                llAvailableTimes.addView(row);
            }

            LinearLayout parentRow = (LinearLayout) llAvailableTimes.getChildAt(i / 3);
            Button timeButton = new Button(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            params.setMargins(6, 0, 6, 0);
            timeButton.setLayoutParams(params);
            timeButton.setText(availableTimes[i]);
            timeButton.setTextSize(14);
            timeButton.setMinHeight(0);
            timeButton.setPadding(12, 12, 12, 12);

            final String time = availableTimes[i];
            if (selectedTime.equals(time)) {
                timeButton.setBackgroundResource(R.drawable.time_slot_selected);
                timeButton.setTextColor(ContextCompat.getColor(this, R.color.white));
            } else {
                timeButton.setBackgroundResource(R.drawable.time_slot_unselected);
                timeButton.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }

            timeButton.setOnClickListener(v -> {
                selectedTime = time;
                updateTimeSelection();
            });

            parentRow.addView(timeButton);
        }
    }

    private void updateTimeSelection() {
        for (int i = 0; i < llAvailableTimes.getChildCount(); i++) {
            LinearLayout row = (LinearLayout) llAvailableTimes.getChildAt(i);
            for (int j = 0; j < row.getChildCount(); j++) {
                Button button = (Button) row.getChildAt(j);
                String time = button.getText().toString();
                if (selectedTime.equals(time)) {
                    button.setBackgroundResource(R.drawable.time_slot_selected);
                    button.setTextColor(ContextCompat.getColor(this, R.color.white));
                } else {
                    button.setBackgroundResource(R.drawable.time_slot_unselected);
                    button.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                }
            }
        }
    }

    private void setupFlexibleTiming() {
        cardFlexibleTiming.setOnClickListener(v -> {
            isFlexible = !isFlexible;
            updateFlexibleToggle();
        });
        updateFlexibleToggle();
    }

    private void updateFlexibleToggle() {
        View toggleSwitch = findViewById(R.id.toggleSwitch);
        View toggleCircle = findViewById(R.id.toggleCircle);

        if (isFlexible) {
            toggleSwitch.setBackgroundResource(R.drawable.toggle_switch_on);
            toggleCircle.animate()
                    .translationX(convertDpToPx(28))
                    .setDuration(200)
                    .start();
        } else {
            toggleSwitch.setBackgroundResource(R.drawable.toggle_switch_off);
            toggleCircle.animate()
                    .translationX(0)
                    .setDuration(200)
                    .start();
        }
    }

    private int convertDpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void setupButtons() {
        btnContinue.setOnClickListener(v -> {
            if (selectedDate == -1) {
                Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedTime.isEmpty()) {
                Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show();
                return;
            }

            // Pass selected date and time
            Calendar selectedCalendar = (Calendar) currentCalendar.clone();
            selectedCalendar.set(Calendar.DAY_OF_MONTH, selectedDate);

            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
            String serviceDate = dateFormat.format(selectedCalendar.getTime());

            // Check if we should return result
            if (getIntent().getBooleanExtra("return_result", false)) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("serviceDate", serviceDate);
                resultIntent.putExtra("serviceTime", selectedTime);
                resultIntent.putExtra("isFlexible", isFlexible);
                setResult(RESULT_OK, resultIntent);
                finish();
                return;
            }

            // Navigate to ReviewConfirmActivity passing all accumulated data
            Intent intent = new Intent(this, ReviewConfirmActivity.class);

            // 1. Pass newly selected date/time
            intent.putExtra("serviceDate", serviceDate);
            intent.putExtra("serviceTime", selectedTime);
            intent.putExtra("isFlexible", isFlexible);

            // 2. Pass through data from NewBookingActivity
            Intent currentIntent = getIntent();

            // User & Provider Info
            if (currentIntent.hasExtra("userId"))
                intent.putExtra("userId", currentIntent.getStringExtra("userId"));
            if (currentIntent.hasExtra("providerId"))
                intent.putExtra("providerId", currentIntent.getStringExtra("providerId"));
            if (currentIntent.hasExtra("providerName"))
                intent.putExtra("providerName", currentIntent.getStringExtra("providerName"));

            // Service Details
            if (currentIntent.hasExtra("serviceCategory"))
                intent.putExtra("serviceCategory", currentIntent.getStringExtra("serviceCategory"));
            if (currentIntent.hasExtra("serviceName"))
                intent.putExtra("serviceName", currentIntent.getStringExtra("serviceName"));
            if (currentIntent.hasExtra("issueDescription"))
                intent.putExtra("issueDescription", currentIntent.getStringExtra("issueDescription"));
            if (currentIntent.hasExtra("urgency"))
                intent.putExtra("urgency", currentIntent.getStringExtra("urgency"));

            // Location
            if (currentIntent.hasExtra("propertyLocation"))
                intent.putExtra("location", currentIntent.getStringExtra("propertyLocation")); // Note key change to
                                                                                               // "location" for
                                                                                               // ReviewActivity
            if (currentIntent.hasExtra("propertyName"))
                intent.putExtra("propertyName", currentIntent.getStringExtra("propertyName"));

            // Photos
            if (currentIntent.hasExtra("photoUris")) {
                intent.putStringArrayListExtra("photoUris", currentIntent.getStringArrayListExtra("photoUris"));
            }

            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

}
