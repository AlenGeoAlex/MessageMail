package me.alenalex.messageforwarder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;

import me.alenalex.messageforwarder.constants.SharedPrefConstants;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final int SMS_PERMISSION_CODE = 101;

    private EditText etApiUrl, etPassword;
    private Button btnSave;
    private ProgressBar progressBar;
    private TextView tvError;
    private OkHttpClient client;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etApiUrl = findViewById(R.id.etApiUrl);
        etPassword = findViewById(R.id.etPassword);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);
        tvError = findViewById(R.id.tvError);
        this.client = new OkHttpClient();
        checkAndRequestSmsPermission();
        btnSave.setOnClickListener(listener -> {
            validateAndSaveSettings();
        });
    }

    private void checkAndRequestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS}, SMS_PERMISSION_CODE);
        }
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

        // --- 2. Create the validation request ---
        // Let's assume your API expects a JSON with the secret key for validation
        String json = "{\"action\":\"validate\", \"secret\":\"" + secretKey + "\"}";
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(apiUrl)
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
                        // --- 4. Handle SUCCESS (HTTP 200-299) ---
                        Toast.makeText(MainActivity.this, "Validation Successful!", Toast.LENGTH_SHORT).show();
                        saveToPreferences(apiUrl, secretKey);
                        // Optional: close the activity after success
                        // finish();
                    } else {
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

    private void saveToPreferences(String apiUrl, String secretKey) {
        SharedPreferences sharedPreferences = getSharedPreferences(SharedPrefConstants.SHARED_PREF, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SharedPrefConstants.API_URL, apiUrl);
        editor.putString(SharedPrefConstants.SECRET_KEY, secretKey);
        editor.apply();
        Toast.makeText(this, "Settings Saved!", Toast.LENGTH_LONG).show();
    }
}
