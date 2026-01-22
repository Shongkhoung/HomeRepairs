package com.example.homerepairs;

import android.os.Bundle;

import com.example.homerepairs.databinding.ActivityNotificationsBinding;
import com.example.homerepairs.databinding.ItemNotificationToggleBinding;

public class NotificationsActivity extends BaseActivity {

        private ActivityNotificationsBinding binding;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                binding = ActivityNotificationsBinding.inflate(getLayoutInflater());
                setContentView(binding.getRoot());

                setupUI();
        }

        private void setupUI() {
                binding.btnBack.setOnClickListener(v -> finish());

                setupToggle(binding.layoutPush, R.drawable.ic_check_circle, getString(R.string.push_notifications),
                                getString(R.string.push_desc), true);
                setupToggle(binding.layoutEmail, R.drawable.ic_email, getString(R.string.email_notifications),
                                getString(R.string.email_desc), true);
                setupToggle(binding.layoutSms, R.drawable.ic_phone, getString(R.string.sms_notifications),
                                getString(R.string.sms_desc), false);
                setupToggle(binding.layoutOrderUpdates, R.drawable.ic_list, getString(R.string.order_updates),
                                getString(R.string.order_desc), true);
                setupToggle(binding.layoutPromotions, R.drawable.ic_notification, getString(R.string.promotions),
                                getString(R.string.promo_desc), false);
        }

        private void setupToggle(ItemNotificationToggleBinding item, int icon, String title, String desc,
                        boolean checked) {
                item.ivIcon.setImageResource(icon);
                item.tvTitle.setText(title);
                item.tvDescription.setText(desc);
                item.switchToggle.setChecked(checked);
        }
}
