package me.alenalex.messageforwarder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.alenalex.messageforwarder.constants.SharedPrefConstants;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final int SMS_PERMISSION_CODE = 101;
    private static final int READ_PHONE_STATE_PERMISSION_CODE = 102;
    private static final int POST_NOTIFICATIONS_PERMISSION_CODE = 103;

    public static final String CHANNEL_ID = "SMS_FORWARDER_CHANNEL_ID"; // Must match channel created elsewhere
    public static final int NOTIFICATION_ID_CONFIG_ERROR = 101; // Unique ID for config errors
    public static final int NOTIFICATION_ID_FORWARD_SUCCESS = 102;
    public static final int NOTIFICATION_ID_FORWARD_FAILURE = 103;


    private EditText etApiUrl, etPassword;
    private Button btnSave;
    private ProgressBar progressBar;
    private TextView tvError, tvSimInfoLabel;
    private Spinner spinnerSimSelection;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch switchForwardingEnabled;
    private OkHttpClient client;
    private List<Integer> subscriptionIdsList = new ArrayList<>();// To store subscription IDs corresponding to spinner items

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etApiUrl = findViewById(R.id.etApiUrl);
        etPassword = findViewById(R.id.etPassword);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);
        tvError = findViewById(R.id.tvError);
        spinnerSimSelection = findViewById(R.id.spinnerSimSelection); // Add to your layout
        tvSimInfoLabel = findViewById(R.id.tvSimInfoLabel); // Add to your layout
        switchForwardingEnabled = findViewById(R.id.switchForwardingEnabled); // Add to your layout


        this.client = new OkHttpClient();
        checkAndRequestBasePermissions();
        setupSimSelectionUiVisibility();
        setupForwardingSwitch();

        btnSave.setOnClickListener(listener -> {
            validateAndSaveSettings();
        });
        loadSavedSettings();
        createNotificationChannels();
    }

    private void loadSavedSettings() {
        SharedPreferences sharedPreferences = getSharedPreferences(SharedPrefConstants.SHARED_PREF, MODE_PRIVATE);
        etApiUrl.setText(sharedPreferences.getString(SharedPrefConstants.API_URL, ""));
        // For security, you might not want to re-display the password,
        // or show it as asterisks and only update it if the user types something new.
        // etPassword.setText(sharedPreferences.getString(SharedPrefConstants.SECRET_KEY, ""));
    }

    private void checkAndRequestBasePermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECEIVE_SMS);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+ for Notifications
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), SMS_PERMISSION_CODE); // Use one code, check results
        } else {
            // All base permissions granted, proceed with SIM selection if needed
            setupSimSelection();
        }
    }

    private void setupSimSelectionUiVisibility() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            spinnerSimSelection.setVisibility(View.VISIBLE);
            tvSimInfoLabel.setVisibility(View.VISIBLE);
        } else {
            spinnerSimSelection.setVisibility(View.GONE);
            tvSimInfoLabel.setText("SIM selection requires Android 5.1+");
            tvSimInfoLabel.setVisibility(View.VISIBLE);
        }
    }


    private void setupSimSelection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, READ_PHONE_STATE_PERMISSION_CODE);
            } else {
                loadSimInformation();
            }
        }
    }


    private void loadSimInformation() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            return; // Should be handled by setupSimSelectionUiVisibility
        }

        SubscriptionManager subscriptionManager = (SubscriptionManager) getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // This should ideally not happen if permission flow is correct
            Toast.makeText(this, "READ_PHONE_STATE permission not granted.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<SubscriptionInfo> activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
        List<String> simDetailsList = new ArrayList<>();
        subscriptionIdsList.clear(); // Clear previous list

        if (activeSubscriptionInfoList != null && !activeSubscriptionInfoList.isEmpty()) {
            for (SubscriptionInfo subInfo : activeSubscriptionInfoList) {
                String detail = "SIM " + (subInfo.getSimSlotIndex() + 1) + ": " + subInfo.getDisplayName() + " (" + subInfo.getCarrierName() + ")";
                simDetailsList.add(detail);
                subscriptionIdsList.add(subInfo.getSubscriptionId());
            }
        } else {
            simDetailsList.add("No active SIMs found");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, simDetailsList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSimSelection.setAdapter(adapter);

        if (!subscriptionIdsList.isEmpty()) {
            SharedPreferences sharedPreferences = getSharedPreferences(SharedPrefConstants.SHARED_PREF, MODE_PRIVATE);
            int savedSubId = sharedPreferences.getInt(SharedPrefConstants.PREF_SELECTED_SUB_ID, -1);
            int selectionIndex = subscriptionIdsList.indexOf(savedSubId);
            if (selectionIndex != -1) {
                spinnerSimSelection.setSelection(selectionIndex);
            }

            spinnerSimSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position < subscriptionIdsList.size()) {
                        int selectedSubId = subscriptionIdsList.get(position);
                        saveSelectedSubscriptionId(selectedSubId);
                    } else if (activeSubscriptionInfoList == null || activeSubscriptionInfoList.isEmpty()) {
                        // "No active SIMs found" is selected, clear preference
                        saveSelectedSubscriptionId(-1); // Or a specific indicator for "any" / "none"
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        } else {
            // Handle case where "No active SIMs found" is the only item
            spinnerSimSelection.setEnabled(false);
        }
    }

    private void saveSelectedSubscriptionId(int subId) {
        SharedPreferences sharedPreferences = getSharedPreferences(SharedPrefConstants.SHARED_PREF, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(SharedPrefConstants.PREF_SELECTED_SUB_ID, subId);
        editor.apply();
        if (subId != -1) {
            Toast.makeText(this, "Forwarding SIM preference saved.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupForwardingSwitch() {
        SharedPreferences sharedPreferences = getSharedPreferences(SharedPrefConstants.SHARED_PREF, MODE_PRIVATE);
        boolean isForwardingEnabled = sharedPreferences.getBoolean(SharedPrefConstants.PREF_FORWARDING_ENABLED, true); // Default to true
        switchForwardingEnabled.setChecked(isForwardingEnabled);
        enableSmsReceiver(isForwardingEnabled); // Set initial state

        switchForwardingEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveForwardingEnabledState(isChecked);
            enableSmsReceiver(isChecked);
            if (isChecked) {
                Toast.makeText(MainActivity.this, "SMS Forwarding Enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "SMS Forwarding Disabled", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveForwardingEnabledState(boolean isEnabled) {
        SharedPreferences sharedPreferences = getSharedPreferences(SharedPrefConstants.SHARED_PREF, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(SharedPrefConstants.PREF_FORWARDING_ENABLED, isEnabled);
        editor.apply();
    }

    private void enableSmsReceiver(boolean enable) {
        ComponentName receiver = new ComponentName(this, SmsReceiver.class); // Use your actual receiver class
        PackageManager pm = getPackageManager();
        int newState = enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        pm.setComponentEnabledSetting(receiver, newState, PackageManager.DONT_KILL_APP);
        Log.d("MainActivity", "SmsReceiver enabled: " + enable);
    }


    @SuppressLint("SetTextI18n")
    private void validateAndSaveSettings() {
        showLoading(true);
        String apiUrl = etApiUrl.getText().toString().trim();
        String secretKey = etPassword.getText().toString().trim();

        if (apiUrl.isEmpty()) {
            showLoading(false);
            tvError.setText("API URL cannot be empty.");
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        HttpUrl url = HttpUrl.parse(apiUrl+"/ping");
        if(url == null){
            showLoading(false);
            tvError.setText("API URL cannot be empty.");
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        // --- 2. Create the validation request ---
        // Let's assume your API expects a JSON with the secret key for validation
        String json = "";
        RequestBody body = RequestBody.create(json, MediaType.get("plain/text; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        // --- 3. Execute the request asynchronously ---
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Network error or host unreachable
                runOnUiThread(() -> {
                    showLoading(false);
                    tvError.setText("Validation failed: " + e.getMessage());
                    tvError.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    showLoading(false);
                    if (response.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Validation Successful!", Toast.LENGTH_SHORT).show();
                        saveApiAndSecretToPreferences(apiUrl, secretKey);                    } else {
                        String message;
                        try {
                            message = response.body().string();
                        } catch (IOException e) {
                            message = "Failed to read the body";
                        }
                        tvError.setText("Validation failed: Invalid URL or Secret Key (Code: " + response.code() + "). (Body: "+message+")");
                        tvError.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    // Helper method to manage UI state
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            tvError.setVisibility(View.GONE);
            btnSave.setEnabled(false); // Disable button
            // Hide keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(btnSave.getWindowToken(), 0);
        } else {
            progressBar.setVisibility(View.GONE);
            btnSave.setEnabled(true); // Re-enable button
        }
    }

    private void saveApiAndSecretToPreferences(String apiUrl, String secretKey) {
        SharedPreferences sharedPreferences = getSharedPreferences(SharedPrefConstants.SHARED_PREF, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SharedPrefConstants.API_URL, apiUrl);
        editor.putString(SharedPrefConstants.SECRET_KEY, secretKey);
        editor.apply();
        Toast.makeText(this, "API Settings Saved!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean allBasePermissionsGranted = true;

        if (requestCode == SMS_PERMISSION_CODE) { // Check for the combined request code
            for (int i = 0; i < permissions.length; i++) {
                if (Manifest.permission.RECEIVE_SMS.equals(permissions[i])) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Receive SMS permission is required.", Toast.LENGTH_LONG).show();
                        allBasePermissionsGranted = false;
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        Manifest.permission.POST_NOTIFICATIONS.equals(permissions[i])) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Notification permission denied. You won't see status updates.", Toast.LENGTH_LONG).show();
                        allBasePermissionsGranted = false;
                    }
                }
            }
            if(allBasePermissionsGranted){
                // Now safe to try and load SIM info
                setupSimSelection();
            }

        } else if (requestCode == READ_PHONE_STATE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadSimInformation();
            } else {
                Toast.makeText(this, "Read Phone State permission denied. Cannot list SIMs.", Toast.LENGTH_LONG).show();
                spinnerSimSelection.setVisibility(View.GONE); // Hide SIM selection if permission denied
                tvSimInfoLabel.setText("SIM info requires Read Phone State permission.");
            }
        }
    }

    private void createNotificationChannels() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel for SMS Forwarding Status
            CharSequence nameSmsForwarder = getString(R.string.notification_channel_name_sms_forwarder); // Example: "SMS Forwarding Status"
            String descriptionSmsForwarder = getString(R.string.notification_channel_description_sms_forwarder); // Example: "Notifications about SMS forwarding success or failure"
            int importanceSmsForwarder = NotificationManager.IMPORTANCE_DEFAULT; // Or IMPORTANCE_HIGH for config errors

            NotificationChannel smsForwarderChannel = new NotificationChannel(CHANNEL_ID, nameSmsForwarder, importanceSmsForwarder);
            smsForwarderChannel.setDescription(descriptionSmsForwarder);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(smsForwarderChannel);
            }
        }
    }
}
