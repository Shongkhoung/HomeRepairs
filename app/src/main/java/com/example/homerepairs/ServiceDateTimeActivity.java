package com.example.homerepairs;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.homerepairs.databinding.ActivityServiceDateTimeBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ServiceDateTimeActivity extends BaseActivity {

    private ActivityServiceDateTimeBinding binding;
    private String viewMode = "month";
    private int selectedDay = -1;
    private String selectedTime = "10:00 AM";
    private boolean isFlexible = false;
    private Calendar calendar;
    private SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
    private SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());

    private final String[] days = { "S", "M", "T", "W", "T", "F", "S" };
    private final String[] times = { "8:00 AM", "9:00 AM", "10:00 AM", "11:00 AM", "12:00 PM", "1:00 PM", "2:00 PM",
            "3:00 PM", "4:00 PM" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityServiceDateTimeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        calendar = Calendar.getInstance();
        setupInitialSelection();
        setupListeners();
        updateView();
        setupFlexible();
    }

    private void setupInitialSelection() {
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        selectedDay = tomorrow.get(Calendar.DAY_OF_MONTH);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnPrevMonth.setOnClickListener(v -> {
            calendar.add(viewMode.equals("month") ? Calendar.MONTH : Calendar.WEEK_OF_YEAR, -1);
            updateView();
        });
        binding.btnNextMonth.setOnClickListener(v -> {
            calendar.add(viewMode.equals("month") ? Calendar.MONTH : Calendar.WEEK_OF_YEAR, 1);
            updateView();
        });
        binding.btnWeek.setOnClickListener(v -> {
            viewMode = "week";
            updateView();
        });
        binding.btnMonth.setOnClickListener(v -> {
            viewMode = "month";
            updateView();
        });
        binding.btnContinue.setOnClickListener(v -> proceed());
    }

    private void updateView() {
        updateToggle();
        if (viewMode.equals("month"))
            setupMonthView();
        else
            setupWeekView();
        setupTimeSlots();
    }

    private void updateToggle() {
        boolean isWeek = viewMode.equals("week");
        binding.btnWeek
                .setBackgroundResource(isWeek ? R.drawable.view_toggle_selected : R.drawable.view_toggle_unselected);
        binding.btnWeek.setTextColor(ContextCompat.getColor(this, isWeek ? R.color.white : R.color.text_secondary));
        binding.btnMonth
                .setBackgroundResource(!isWeek ? R.drawable.view_toggle_selected : R.drawable.view_toggle_unselected);
        binding.btnMonth.setTextColor(ContextCompat.getColor(this, !isWeek ? R.color.white : R.color.text_secondary));
        binding.llCalendarGrid.setVisibility(isWeek ? View.GONE : View.VISIBLE);
        binding.llWeekView.setVisibility(isWeek ? View.VISIBLE : View.GONE);
    }

    private void setupMonthView() {
        binding.tvMonthYear.setText(monthYearFormat.format(calendar.getTime()));
        binding.llCalendarGrid.removeAllViews();

        LinearLayout header = new LinearLayout(this);
        header.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        for (String d : days) {
            TextView tv = new TextView(this);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            tv.setText(d);
            tv.setGravity(Gravity.CENTER);
            tv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tv.setPadding(0, 0, 0, 16);
            header.addView(tv);
        }
        binding.llCalendarGrid.addView(header);

        Calendar m = (Calendar) calendar.clone();
        m.set(Calendar.DAY_OF_MONTH, 1);
        int first = m.get(Calendar.DAY_OF_WEEK) - 1;
        int count = m.getActualMaximum(Calendar.DAY_OF_MONTH);
        Calendar now = Calendar.getInstance();

        int day = 1;
        for (int w = 0; w < 6; w++) {
            LinearLayout row = new LinearLayout(this);
            row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
            for (int d = 0; d < 7; d++) {
                Button b = new Button(this);
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
                p.setMargins(4, 4, 4, 4);
                b.setLayoutParams(p);
                b.setMinHeight(0);
                b.setPadding(8, 16, 8, 16);

                if ((w == 0 && d < first) || day > count)
                    b.setVisibility(View.INVISIBLE);
                else {
                    final int dId = day;
                    b.setText(String.valueOf(day));
                    boolean isPast = now.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)
                            && now.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)
                            && day < now.get(Calendar.DAY_OF_MONTH);
                    if (isPast) {
                        b.setEnabled(false);
                        b.setTextColor(ContextCompat.getColor(this, R.color.text_hint));
                        b.setBackgroundResource(R.drawable.calendar_day_disabled);
                    } else if (selectedDay == day) {
                        b.setBackgroundResource(R.drawable.calendar_day_selected);
                        b.setTextColor(ContextCompat.getColor(this, R.color.white));
                    } else {
                        b.setBackgroundResource(R.drawable.calendar_day_unselected);
                        b.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                    }
                    b.setOnClickListener(v -> {
                        selectedDay = dId;
                        setupMonthView();
                    });
                    day++;
                }
                row.addView(b);
            }
            binding.llCalendarGrid.addView(row);
            if (day > count)
                break;
        }
    }

    private void setupWeekView() {
        binding.tvMonthYear.setText("This Week");
        binding.llWeekView.removeAllViews();
        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

        Calendar w = (Calendar) calendar.clone();
        w.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        for (int i = 0; i < 7; i++) {
            Calendar d = (Calendar) w.clone();
            d.add(Calendar.DAY_OF_MONTH, i);
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            box.setGravity(Gravity.CENTER);
            box.setPadding(8, 16, 8, 16);

            boolean sel = selectedDay == d.get(Calendar.DAY_OF_MONTH)
                    && calendar.get(Calendar.MONTH) == d.get(Calendar.MONTH);
            box.setBackgroundResource(sel ? R.drawable.calendar_day_selected : R.drawable.calendar_day_unselected);

            TextView dn = new TextView(this);
            dn.setText(dayFormat.format(d.getTime()));
            dn.setTextColor(ContextCompat.getColor(this, sel ? R.color.white : R.color.text_secondary));
            dn.setGravity(Gravity.CENTER);
            TextView num = new TextView(this);
            num.setText(String.valueOf(d.get(Calendar.DAY_OF_MONTH)));
            num.setTextSize(18);
            num.setTextColor(ContextCompat.getColor(this, sel ? R.color.white : R.color.text_primary));
            num.setGravity(Gravity.CENTER);
            TextView mon = new TextView(this);
            mon.setText(monthFormat.format(d.getTime()));
            mon.setTextColor(ContextCompat.getColor(this, sel ? R.color.white : R.color.text_secondary));
            mon.setGravity(Gravity.CENTER);

            box.addView(dn);
            box.addView(num);
            box.addView(mon);
            final int dayVal = d.get(Calendar.DAY_OF_MONTH);
            final int monVal = d.get(Calendar.MONTH);
            box.setOnClickListener(v -> {
                selectedDay = dayVal;
                calendar.set(Calendar.MONTH, monVal);
                setupWeekView();
            });
            row.addView(box);
        }
        binding.llWeekView.addView(row);
    }

    private void setupTimeSlots() {
        binding.llAvailableTimes.removeAllViews();
        for (int i = 0; i < times.length; i++) {
            if (i % 3 == 0) {
                LinearLayout row = new LinearLayout(this);
                row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
                ((LinearLayout.LayoutParams) row.getLayoutParams()).setMargins(0, 0, 0, 12);
                binding.llAvailableTimes.addView(row);
            }
            LinearLayout row = (LinearLayout) binding.llAvailableTimes.getChildAt(i / 3);
            Button b = new Button(this);
            b.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            ((LinearLayout.LayoutParams) b.getLayoutParams()).setMargins(6, 0, 6, 0);
            b.setText(times[i]);
            b.setPadding(12, 12, 12, 12);
            b.setMinHeight(0);
            boolean sel = selectedTime.equals(times[i]);
            b.setBackgroundResource(sel ? R.drawable.time_slot_selected : R.drawable.time_slot_unselected);
            b.setTextColor(ContextCompat.getColor(this, sel ? R.color.white : R.color.text_secondary));
            final String t = times[i];
            b.setOnClickListener(v -> {
                selectedTime = t;
                setupTimeSlots();
            });
            row.addView(b);
        }
    }

    private void setupFlexible() {
        binding.cardFlexibleTiming.setOnClickListener(v -> {
            isFlexible = !isFlexible;
            binding.toggleSwitch
                    .setBackgroundResource(isFlexible ? R.drawable.toggle_switch_on : R.drawable.toggle_switch_off);
            binding.toggleCircle.animate()
                    .translationX(isFlexible ? 28 * getResources().getDisplayMetrics().density : 0).setDuration(200)
                    .start();
        });
    }

    private void proceed() {
        if (selectedDay == -1) {
            Toast.makeText(this, "Select date", Toast.LENGTH_SHORT).show();
            return;
        }
        Calendar sel = (Calendar) calendar.clone();
        sel.set(Calendar.DAY_OF_MONTH, selectedDay);
        String dateStr = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(sel.getTime());

        if (getIntent().getBooleanExtra("return_result", false)) {
            Intent res = new Intent();
            res.putExtra("serviceDate", dateStr);
            res.putExtra("serviceTime", selectedTime);
            res.putExtra("isFlexible", isFlexible);
            setResult(RESULT_OK, res);
            finish();
            return;
        }

        Intent next = new Intent(this, ReviewConfirmActivity.class);
        next.putExtras(getIntent());
        next.putExtra("serviceDate", dateStr);
        next.putExtra("serviceTime", selectedTime);
        next.putExtra("isFlexible", isFlexible);
        next.putExtra("location", getIntent().getStringExtra("propertyLocation"));
        startActivity(next);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}
