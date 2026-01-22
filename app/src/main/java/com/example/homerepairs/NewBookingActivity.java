package com.example.homerepairs;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.homerepairs.databinding.ActivityNewBookingBinding;
import com.example.homerepairs.models.ServiceCategory;
import com.example.homerepairs.utils.AuthHelper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class NewBookingActivity extends BaseActivity {

    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;
    private static final int REQUEST_PERMISSIONS = 100;
    private static final int REQUEST_ADDRESS = 200;

    private ActivityNewBookingBinding binding;
    private String selectedDate = "";
    private String selectedTime = "";
    private Calendar customCalendar = Calendar.getInstance();

    private List<Uri> selectedPhotos = new ArrayList<>();
    private List<Bitmap> selectedPhotoBitmaps = new ArrayList<>();
    private int currentPhotoIndex = 0;

    private String currentServiceCategory;
    private String currentServiceName;
    private String currentProviderId;
    private String currentProviderName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewBookingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializePhotos();
        populateServiceInfo();
        setupListeners();
    }

    private void initializePhotos() {
        selectedPhotoBitmaps = new ArrayList<>();
        for (int i = 0; i < 3; i++)
            selectedPhotoBitmaps.add(null);
    }

    private void populateServiceInfo() {
        currentServiceCategory = getIntent().getStringExtra("service_category");
        currentServiceName = getIntent().getStringExtra("service_name");
        currentProviderId = getIntent().getStringExtra("provider_id");
        currentProviderName = getIntent().getStringExtra("provider_name");

        boolean hasInfo = currentServiceName != null && !currentServiceName.isEmpty();
        if (hasInfo) {
            binding.tvServiceName.setText(currentServiceName);
            binding.tvServiceCategory.setText(currentServiceCategory != null ? currentServiceCategory : "General");
            binding.ivServiceDropdown.setVisibility(View.GONE);
        } else {
            binding.tvServiceName.setText("Select Service");
            binding.tvServiceCategory.setText("Tap to choose service type");
            binding.ivServiceDropdown.setVisibility(View.VISIBLE);
        }

        binding.cardService.setOnClickListener(v -> {
            ServiceSelectionBottomSheet bottomSheet = new ServiceSelectionBottomSheet();
            bottomSheet.setListener(this::updateServiceSelection);
            bottomSheet.show(getSupportFragmentManager(), "ServiceSelection");
        });
    }

    private void updateServiceSelection(ServiceCategory service) {
        if (service == null)
            return;
        currentServiceCategory = service.getName();
        currentServiceName = service.getName();
        binding.tvServiceName.setText(currentServiceName);
        binding.tvServiceCategory.setText("Service by Professional");

        // Reset provider info when service changes
        currentProviderId = null;
        currentProviderName = null;

        if (service.getIconResId() != 0) {
            binding.ivServiceIcon.setImageResource(service.getIconResId());
            binding.ivServiceIcon.setColorFilter(ContextCompat.getColor(this, R.color.white));
        }

        // Ask user if they want to select a specific provider
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Provider")
                .setMessage("Would you like to choose a specific provider for " + service.getName() + "?")
                .setPositiveButton("Choose Provider", (d, w) -> {
                    Intent intent = new Intent(this, PlumbingActivity.class);
                    intent.putExtra("category_name", service.getName());
                    intent.putExtra("ACTION_PICK_PROVIDER", true);
                    startActivityForResult(intent, 300); // REQUEST_PROVIDER_PICK
                })
                .setNegativeButton("Auto-Assign", (d, w) -> {
                    // Keep generic
                    binding.tvServiceCategory.setText("Service by Professional");
                })
                .show();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        setupDateButtons();
        setupTimeButtons();
        setupPhotoButtons();
        binding.llLocationContainer.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddressesActivity.class);
            intent.putExtra("ACTION_PICK", true);
            startActivityForResult(intent, REQUEST_ADDRESS);
        });
        binding.btnCancel.setOnClickListener(v -> finish());
        binding.btnNext.setOnClickListener(v -> validateAndProceed());
    }

    private void setupDateButtons() {
        View.OnClickListener listener = v -> {
            resetDateButtons();
            Button b = (Button) v;
            selectedDate = b.getText().toString();
            b.setBackgroundResource(R.drawable.bg_choice_selected);
            b.setTextColor(ContextCompat.getColor(this, R.color.white));
            binding.btnDateSchedule.setText("Schedule");
        };
        binding.btnDateToday.setOnClickListener(listener);
        binding.btnDateTomorrow.setOnClickListener(listener);

        binding.btnDateSchedule.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, m, d) -> {
                customCalendar.set(y, m, d);
                resetDateButtons();
                binding.btnDateSchedule.setBackgroundResource(R.drawable.bg_choice_selected);
                binding.btnDateSchedule.setTextColor(ContextCompat.getColor(this, R.color.white));
                String dateStr = new SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                        .format(customCalendar.getTime());
                binding.btnDateSchedule.setText(dateStr);
                selectedDate = "Custom";
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void resetDateButtons() {
        int gray = ContextCompat.getColor(this, R.color.gray_700);
        int unselected = R.drawable.bg_choice_unselected;
        binding.btnDateSchedule.setBackgroundResource(unselected);
        binding.btnDateSchedule.setTextColor(gray);
        binding.btnDateToday.setBackgroundResource(unselected);
        binding.btnDateToday.setTextColor(gray);
        binding.btnDateTomorrow.setBackgroundResource(unselected);
        binding.btnDateTomorrow.setTextColor(gray);
    }

    private void setupTimeButtons() {
        View.OnClickListener listener = v -> {
            resetTimeButtons();
            Button b = (Button) v;
            selectedTime = b.getText().toString();
            binding.btnTimeCustom.setText("Pick Time"); // Reset custom text if another is picked
            b.setBackgroundResource(R.drawable.bg_choice_selected);
            b.setTextColor(ContextCompat.getColor(this, R.color.white));
        };
        binding.btnTimeMorning.setOnClickListener(listener);
        binding.btnTimeAfternoon.setOnClickListener(listener);

        binding.btnTimeCustom.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, (view, h, m) -> {
                resetTimeButtons();
                binding.btnTimeCustom.setBackgroundResource(R.drawable.bg_choice_selected);
                binding.btnTimeCustom.setTextColor(ContextCompat.getColor(this, R.color.white));
                String amPm = h >= 12 ? "PM" : "AM";
                int hour12 = h > 12 ? h - 12 : (h == 0 ? 12 : h);
                String timeStr = String.format(Locale.getDefault(), "%d:%02d %s", hour12, m, amPm);
                binding.btnTimeCustom.setText(timeStr);
                selectedTime = timeStr;
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        });
    }

    private void resetTimeButtons() {
        int gray = ContextCompat.getColor(this, R.color.gray_700);
        int unselected = R.drawable.bg_choice_unselected;
        binding.btnTimeMorning.setBackgroundResource(unselected);
        binding.btnTimeMorning.setTextColor(gray);
        binding.btnTimeAfternoon.setBackgroundResource(unselected);
        binding.btnTimeAfternoon.setTextColor(gray);
        binding.btnTimeCustom.setBackgroundResource(unselected);
        binding.btnTimeCustom.setTextColor(gray);
    }

    private void setupPhotoButtons() {
        binding.cardPhoto1.setOnClickListener(v -> {
            currentPhotoIndex = 0;
            showPhotoOptions();
        });
        binding.cardPhoto2.setOnClickListener(v -> {
            currentPhotoIndex = 1;
            showPhotoOptions();
        });
        binding.cardPhoto3.setOnClickListener(v -> {
            currentPhotoIndex = 2;
            showPhotoOptions();
        });
    }

    private void showPhotoOptions() {
        String[] options = { "Take Photo", "Gallery", "Remove" };
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Image")
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        if (checkPerm(Manifest.permission.CAMERA))
                            openCamera();
                        else
                            reqPerm(Manifest.permission.CAMERA);
                    } else if (which == 1) {
                        String p = android.os.Build.VERSION.SDK_INT >= 33 ? Manifest.permission.READ_MEDIA_IMAGES
                                : Manifest.permission.READ_EXTERNAL_STORAGE;
                        if (checkPerm(p))
                            openGallery();
                        else
                            reqPerm(p);
                    } else if (which == 2)
                        removePhoto(currentPhotoIndex);
                }).show();
    }

    private boolean checkPerm(String p) {
        return ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED;
    }

    private void reqPerm(String p) {
        ActivityCompat.requestPermissions(this, new String[] { p }, REQUEST_PERMISSIONS);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(intent, REQUEST_CAMERA);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_CAMERA) {
                Bitmap b = (Bitmap) data.getExtras().get("data");
                if (b != null) {
                    setPhoto(currentPhotoIndex, b);
                    selectedPhotoBitmaps.set(currentPhotoIndex, b);
                }
            } else if (requestCode == REQUEST_GALLERY) {
                Uri uri = data.getData();
                if (uri != null) {
                    try {
                        Bitmap b = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        setPhoto(currentPhotoIndex, b);
                        selectedPhotos.add(uri);
                        selectedPhotoBitmaps.set(currentPhotoIndex, b);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else if (requestCode == REQUEST_ADDRESS) {
                binding.tvLocationName.setText(data.getStringExtra("address_name"));
                binding.tvLocationAddress.setText(data.getStringExtra("address_full"));
            } else if (requestCode == 300) { // REQUEST_PROVIDER_PICK
                currentProviderId = data.getStringExtra("provider_id");
                currentProviderName = data.getStringExtra("provider_name");
                String cat = data.getStringExtra("service_category");
                if (cat != null)
                    currentServiceCategory = cat;

                binding.tvServiceCategory.setText(getString(R.string.service_by, currentProviderName));
            }
        }
    }

    private void setPhoto(int index, Bitmap bitmap) {
        ImageView iv = null;
        ImageView add = null;
        if (index == 0) {
            iv = binding.ivPhoto1;
            add = binding.ivAddPhoto1;
        } else if (index == 1) {
            iv = binding.ivPhoto2;
            add = binding.ivAddPhoto2;
        } else if (index == 2) {
            iv = binding.ivPhoto3;
            add = binding.ivAddPhoto3;
        }
        if (iv != null) {
            iv.setImageBitmap(bitmap);
            iv.setVisibility(View.VISIBLE);
            add.setVisibility(View.GONE);
        }
    }

    private void removePhoto(int index) {
        ImageView iv = null;
        ImageView add = null;
        if (index == 0) {
            iv = binding.ivPhoto1;
            add = binding.ivAddPhoto1;
        } else if (index == 1) {
            iv = binding.ivPhoto2;
            add = binding.ivAddPhoto2;
        } else if (index == 2) {
            iv = binding.ivPhoto3;
            add = binding.ivAddPhoto3;
        }
        if (iv != null) {
            iv.setVisibility(View.GONE);
            add.setVisibility(View.VISIBLE);
            if (index < selectedPhotos.size())
                selectedPhotos.remove(index);
            selectedPhotoBitmaps.set(index, null);
        }
    }

    private void validateAndProceed() {
        String desc = binding.etIssueDescription.getText().toString().trim();
        if (desc.isEmpty()) {
            Toast.makeText(this, "Describe the issue", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedDate.isEmpty() || selectedTime.isEmpty()) {
            Toast.makeText(this, "Select date and time", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = AuthHelper.getCurrentUserId(this);
        if (uid == null) {
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar cal = Calendar.getInstance();
        if (selectedDate.equalsIgnoreCase("Tomorrow"))
            cal.add(Calendar.DAY_OF_YEAR, 1);
        else if (selectedDate.equals("Custom"))
            cal = customCalendar;
        String formattedDate = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(cal.getTime());

        Intent intent = new Intent(this, ReviewConfirmActivity.class);
        intent.putExtra("userId", uid);
        intent.putExtra("providerId", currentProviderId);
        intent.putExtra("providerName", currentProviderName);
        intent.putExtra("serviceCategory", currentServiceCategory != null ? currentServiceCategory : "General");
        intent.putExtra("serviceName", currentServiceName != null ? currentServiceName : "Service Request");
        intent.putExtra("issueDescription", desc);
        intent.putExtra("serviceDate", formattedDate);
        intent.putExtra("serviceTime", selectedTime);
        intent.putExtra("location",
                binding.tvLocationName.getText().toString() + ", " + binding.tvLocationAddress.getText().toString());
        intent.putExtra("propertyName", binding.tvLocationName.getText().toString());

        ArrayList<String> uris = new ArrayList<>();
        for (Uri uri : selectedPhotos)
            uris.add(uri.toString());
        intent.putStringArrayListExtra("photoUris", uris);

        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}
