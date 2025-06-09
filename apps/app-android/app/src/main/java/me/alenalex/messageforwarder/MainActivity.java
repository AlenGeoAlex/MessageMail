package me.alenalex.messageforwarder;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
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

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.alenalex.messageforwarder.constants.SharedPrefConstants;
import me.alenalex.messageforwarder.dto.UpdateResponse;
import me.alenalex.messageforwarder.services.NotificationService;
import me.alenalex.messageforwarder.services.api.WebRequestOptions;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity {
    private static final int SMS_PERMISSION_CODE = 101;
    private static final int READ_PHONE_STATE_PERMISSION_CODE = 102;
    private EditText etApiUrl, etPassword;
    private Button btnSave, checkUpdate;
    private ProgressBar progressBar;
    private TextView tvError, tvSimInfoLabel, tvVersion;
    private Spinner spinnerSimSelection;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch switchForwardingEnabled;
    private final AppContainer container = AppContainer.container();
    private final List<Integer> subscriptionIdsList = new ArrayList<>();// To store subscription IDs corresponding to spinner items
    private String versionNumber;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etApiUrl = findViewById(R.id.etApiUrl);
        etPassword = findViewById(R.id.etPassword);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);
        tvError = findViewById(R.id.tvError);
        spinnerSimSelection = findViewById(R.id.spinnerSimSelection);
        tvSimInfoLabel = findViewById(R.id.tvSimInfoLabel);
        switchForwardingEnabled = findViewById(R.id.switchForwardingEnabled);
        tvVersion = findViewById(R.id.tvVersion);
        checkUpdate = findViewById(R.id.btnCheckUpdate);

        spinnerSimSelection.setVisibility(View.VISIBLE);
        tvSimInfoLabel.setVisibility(View.VISIBLE);
        try {
            PackageInfo packageInfo = getApplicationContext().getPackageManager().getPackageInfo(getPackageName(), 0);
            versionNumber = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting package info", e);
            versionNumber = "N/A";
        }

        tvVersion.setText(versionNumber);
        tvVersion.setVisibility(View.VISIBLE);
        checkAndRequestBasePermissions();
        setupForwardingSwitch();
        btnSave.setOnClickListener(listener -> {
            validateAndSaveSettings();
        });

        checkUpdate.setOnClickListener(l -> checkUpdate());

        loadSavedSettings();
        createNotificationChannels();
    }

    private void loadSavedSettings() {
        SharedPreferences sharedPreferences = getSharedPreferences(SharedPrefConstants.SHARED_PREF, MODE_PRIVATE);
        etApiUrl.setText(sharedPreferences.getString(SharedPrefConstants.API_URL, ""));
        etPassword.setText(sharedPreferences.getString(SharedPrefConstants.SECRET_KEY, ""));
    }

    private void checkUpdate(){

        AppContainer.container().updateService().checkUpdate(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Toast.makeText(getApplicationContext(), "Failed to check for update", Toast.LENGTH_LONG).show();
                return;
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    ResponseBody body = response.body();
                    if(body == null)
                        return;

                    String jsonString = body.string();
                    Moshi moshi = AppContainer.container().moshi();
                    JsonAdapter<UpdateResponse> jsonAdapter = moshi.adapter(UpdateResponse.class);

                    UpdateResponse updateResponse = jsonAdapter.fromJson(jsonString);
                    if(updateResponse == null)
                        return;

                    if(versionNumber == null || versionNumber.equals("N/A"))
                        return;

                    if(!versionNumber.equals(updateResponse.latestVersion))
                        Toast.makeText(getApplicationContext(), "Update Available!", Toast.LENGTH_LONG).show();
                }catch (Exception e){
                    Log.e(TAG, "Error parsing JSON", e);
                }
            }
        });
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
            setupSimSelection();
        }
    }

    private void setupSimSelection() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, READ_PHONE_STATE_PERMISSION_CODE);
        } else {
            loadSimInformation();
        }
    }


    private void loadSimInformation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "READ_PHONE_STATE permission not granted.", Toast.LENGTH_SHORT).show();
            return;
        }
        SubscriptionManager subscriptionManager = (SubscriptionManager) getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        List<SubscriptionInfo> activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
        List<String> simDetailsList = new ArrayList<>();
        subscriptionIdsList.clear();

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
        ComponentName receiver = new ComponentName(this, SmsReceiver.class);
        PackageManager pm = getPackageManager();
        int newState = enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        pm.setComponentEnabledSetting(receiver, newState, PackageManager.DONT_KILL_APP);
        Log.d("MainActivity", "SmsReceiver enabled: " + enable);
    }


    @SuppressLint("SetTextI18n")
    private void validateAndSaveSettings() {
        Context applicationContext = getApplicationContext();
        showLoading(true);
        String apiUrl = etApiUrl.getText().toString().trim();
        String secretKey = etPassword.getText().toString().trim();

        if (apiUrl.isEmpty()) {
            showLoading(false);
            tvError.setText("API URL cannot be empty.");
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        HttpUrl url = HttpUrl.parse(apiUrl);
        if(url == null){
            showLoading(false);
            tvError.setText("API URL cannot be invalid.");
            tvError.setVisibility(View.VISIBLE);
            return;
        }


        container.apiService().ping(new WebRequestOptions(url +"/ping", applicationContext), new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
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
                        saveApiAndSecretToPreferences(apiUrl, secretKey);                    }
                    else {
                        String message;
                        try {
                            ResponseBody body = response.body();
                            if(body != null)
                                message = body.string();
                            else message = "N/A";
                        } catch (IOException e) {
                            message = "Failed to read the body";
                        }
                        tvError.setText("Validation failed: Invalid URL or Secret Key (Code: " + response.code() + "). (Body: "+message+")");
                        tvError.setVisibility(View.VISIBLE);
                    }
                });
            }
        } );
    }

    // Helper method to manage UI state
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            tvError.setVisibility(View.GONE);
            btnSave.setEnabled(false);

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(btnSave.getWindowToken(), 0);
        } else {
            progressBar.setVisibility(View.GONE);
            btnSave.setEnabled(true);
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

    @SuppressLint("SetTextI18n")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean allBasePermissionsGranted = true;

        if (requestCode == SMS_PERMISSION_CODE) {
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
                setupSimSelection();
            }

        } else if (requestCode == READ_PHONE_STATE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadSimInformation();
            } else {
                Toast.makeText(this, "Read Phone State permission denied. Cannot list SIMs.", Toast.LENGTH_LONG).show();
                spinnerSimSelection.setVisibility(View.GONE);
                tvSimInfoLabel.setText("SIM info requires Read Phone State permission.");
            }
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence nameSmsForwarder = getString(R.string.notification_channel_name_sms_forwarder);
            String descriptionSmsForwarder = getString(R.string.notification_channel_description_sms_forwarder);
            int importanceSmsForwarder = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel smsForwarderChannel = new NotificationChannel(NotificationService.channelId(), nameSmsForwarder, importanceSmsForwarder);
            smsForwarderChannel.setDescription(descriptionSmsForwarder);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(smsForwarderChannel);
            }
        }
    }
}
