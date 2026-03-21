package com.naijameals.driver;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.naijameals.driver.data.AuthRepository;

import java.lang.ref.WeakReference;

public class LoginActivity extends AppCompatActivity {
    private AuthRepository authRepo;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnSignIn;
    private ProgressBar progressLogin;
    private ImageButton btnTogglePassword;
    private boolean isLoading = false;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authRepo = new AuthRepository(this);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        progressLogin = findViewById(R.id.progressLogin);

        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        btnTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            int sel = etPassword.getSelectionStart();
            int end = etPassword.getSelectionEnd();
            etPassword.setInputType(isPasswordVisible
                    ? android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePassword.setImageResource(isPasswordVisible ? R.drawable.ic_visibility : R.drawable.ic_visibility_off);
            etPassword.setSelection(sel, end);
        });

        btnSignIn.setOnClickListener(v -> handleLogin());
        findViewById(R.id.tvRegister).setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void setLoading(boolean loading) {
        isLoading = loading;
        btnSignIn.setEnabled(!loading);
        btnSignIn.setVisibility(loading ? View.GONE : View.VISIBLE);
        progressLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void handleLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isLoading) return;

        setLoading(true);

        WeakReference<LoginActivity> ref = new WeakReference<>(this);
        authRepo.login(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(com.naijameals.driver.api.models.User user, String token) {
                runOnUiThread(() -> {
                    LoginActivity a = ref.get();
                    if (a == null || a.isDestroyed()) return;
                    a.setLoading(false);
                    a.startActivity(new Intent(a, MainActivity.class));
                    a.finish();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    LoginActivity a = ref.get();
                    if (a == null || a.isDestroyed()) return;
                    a.setLoading(false);
                    new AlertDialog.Builder(a)
                            .setTitle("Login Failed")
                            .setMessage(message != null ? message : "Invalid credentials. Please try again.")
                            .setPositiveButton("OK", null)
                            .show();
                });
            }
        });
    }
}
