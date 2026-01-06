package com.example.homerepairs;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class NotificationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(
                        getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        setupViews();
    }

    private void setupViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Helper to setup toggles
        setupToggle(findViewById(R.id.layoutPush), R.drawable.ic_check_circle, getString(R.string.push_notifications),
                getString(R.string.push_desc), true);
        setupToggle(findViewById(R.id.layoutEmail), R.drawable.ic_email, getString(R.string.email_notifications),
                getString(R.string.email_desc), true);
        setupToggle(findViewById(R.id.layoutSms), R.drawable.ic_phone, getString(R.string.sms_notifications),
                getString(R.string.sms_desc),
                false);
        setupToggle(findViewById(R.id.layoutOrderUpdates), R.drawable.ic_list, getString(R.string.order_updates),
                getString(R.string.order_desc),
                true);
        setupToggle(findViewById(R.id.layoutPromotions), R.drawable.ic_notification, getString(R.string.promotions),
                getString(R.string.promo_desc), false);
    }

    private void setupToggle(View layout, int iconRes, String title, String description, boolean initialChecked) {
        if (layout == null)
            return;

        ImageView ivIcon = layout.findViewById(R.id.ivIcon);
        if (ivIcon != null)
            ivIcon.setImageResource(iconRes);

        TextView tvTitle = layout.findViewById(R.id.tvTitle);
        if (tvTitle != null)
            tvTitle.setText(title);

        TextView tvDescription = layout.findViewById(R.id.tvDescription);
        if (tvDescription != null)
            tvDescription.setText(description);

        SwitchMaterial switchToggle = layout.findViewById(R.id.switchToggle);
        if (switchToggle != null)
            switchToggle.setChecked(initialChecked);
    }
}
