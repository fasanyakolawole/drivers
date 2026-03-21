package com.naijameals.driver;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.naijameals.driver.data.AuthRepository;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private AuthRepository authRepo;
    private int currentStep = 1;
    private static final int TOTAL_STEPS = 3;

    private String name, email, password, passwordConfirmation;
    private String mobile, houseAddress, postcode, deliveryType;

    private TextView tvStepIndicator;
    private View progressDot1, progressDot2, progressDot3;
    private View progressLine1, progressLine2;
    private View progressRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authRepo = new AuthRepository(this);

        tvStepIndicator = findViewById(R.id.tvStepIndicator);
        progressDot1 = findViewById(R.id.progressDot1);
        progressDot2 = findViewById(R.id.progressDot2);
        progressDot3 = findViewById(R.id.progressDot3);
        progressLine1 = findViewById(R.id.progressLine1);
        progressLine2 = findViewById(R.id.progressLine2);
        progressRegister = findViewById(R.id.progressRegister);

        showStep(1);

        findViewById(R.id.tvSignIn).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void showStep(int step) {
        currentStep = step;
        Fragment fragment;
        Bundle args = new Bundle();
        args.putInt("step", step);

        if (step == 1) {
            fragment = new RegisterStep1Fragment();
            args.putString("name", name);
            args.putString("email", email);
            args.putString("password", password);
            args.putString("passwordConfirmation", passwordConfirmation);
        } else if (step == 2) {
            fragment = new RegisterStep2Fragment();
            args.putString("mobile", mobile);
            args.putString("houseAddress", houseAddress);
            args.putString("postcode", postcode);
        } else {
            fragment = new RegisterStep3Fragment();
            args.putString("deliveryType", deliveryType);
        }

        fragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentRegister, fragment)
                .commit();

        updateProgressIndicator();
    }

    private void updateProgressIndicator() {
        try {
            if (tvStepIndicator != null) {
                tvStepIndicator.setText(getString(R.string.step_of, currentStep, TOTAL_STEPS));
            }
            int primaryColor = ContextCompat.getColor(this, R.color.login_primary);
            int inactiveColor = ContextCompat.getColor(this, R.color.gray_border);

            if (progressDot1 != null) progressDot1.setBackgroundColor(currentStep >= 1 ? primaryColor : inactiveColor);
            if (progressDot2 != null) progressDot2.setBackgroundColor(currentStep >= 2 ? primaryColor : inactiveColor);
            if (progressDot3 != null) progressDot3.setBackgroundColor(currentStep >= 3 ? primaryColor : inactiveColor);
            if (progressLine1 != null) progressLine1.setBackgroundColor(currentStep > 1 ? primaryColor : inactiveColor);
            if (progressLine2 != null) progressLine2.setBackgroundColor(currentStep > 2 ? primaryColor : inactiveColor);
        } catch (Exception ignored) {}
    }

    public void onStep1Next(String name, String email, String password, String passwordConfirmation) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.passwordConfirmation = passwordConfirmation;
        showStep(2);
    }

    public void onStep2Next(String mobile, String houseAddress, String postcode) {
        this.mobile = mobile;
        this.houseAddress = houseAddress;
        this.postcode = postcode;
        showStep(3);
    }

    public void onStep2Previous() {
        showStep(1);
    }

    public void onStep3Previous() {
        showStep(2);
    }

    public void onStep3Register(String deliveryType) {
        this.deliveryType = deliveryType;

        if (name == null || name.trim().isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (email == null || email.trim().isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password == null || password.length() < 8) {
            Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(passwordConfirmation)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading (matches React ActivityIndicator)
        if (progressRegister != null) progressRegister.setVisibility(View.VISIBLE);

        Map<String, Object> data = new HashMap<>();
        data.put("name", name.trim());
        data.put("email", email.trim().toLowerCase());
        data.put("password", password);
        data.put("password_confirmation", passwordConfirmation);
        if (mobile != null && !mobile.trim().isEmpty()) data.put("mobile", mobile.trim());
        if (houseAddress != null && !houseAddress.trim().isEmpty()) data.put("house_address", houseAddress.trim());
        if (postcode != null && !postcode.trim().isEmpty()) data.put("postcode", postcode.trim());
        if (deliveryType != null && !deliveryType.isEmpty()) data.put("delivery_type", deliveryType);

        WeakReference<RegisterActivity> ref = new WeakReference<>(this);
        authRepo.register(data, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(com.naijameals.driver.api.models.User user, String token) {
                runOnUiThread(() -> {
                    RegisterActivity a = ref.get();
                    if (a == null || a.isDestroyed()) return;
                    if (a.progressRegister != null) a.progressRegister.setVisibility(View.GONE);
                    new AlertDialog.Builder(a)
                            .setTitle("Success")
                            .setMessage("Registration successful! Please wait for admin confirmation.")
                            .setPositiveButton("OK", (d, w) -> {
                                a.startActivity(new Intent(a, MainActivity.class));
                                a.finish();
                            })
                            .show();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    RegisterActivity a = ref.get();
                    if (a == null || a.isDestroyed()) return;
                    if (a.progressRegister != null) a.progressRegister.setVisibility(View.GONE);
                    new AlertDialog.Builder(a)
                            .setTitle("Registration Failed")
                            .setMessage(message != null ? message : "Please try again")
                            .setPositiveButton("OK", null)
                            .show();
                });
            }
        });
    }
}
