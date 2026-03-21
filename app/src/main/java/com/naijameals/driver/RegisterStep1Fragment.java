package com.naijameals.driver;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterStep1Fragment extends Fragment {
    private boolean passwordVisible = false;
    private boolean passwordConfirmVisible = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_register_step1, container, false);

        TextInputEditText etName = v.findViewById(R.id.etName);
        TextInputEditText etEmail = v.findViewById(R.id.etEmail);
        TextInputEditText etPassword = v.findViewById(R.id.etPassword);
        TextInputEditText etPasswordConfirm = v.findViewById(R.id.etPasswordConfirm);
        MaterialButton btnNext = v.findViewById(R.id.btnNext);
        ImageButton btnTogglePassword = v.findViewById(R.id.btnTogglePassword);
        ImageButton btnTogglePasswordConfirm = v.findViewById(R.id.btnTogglePasswordConfirm);

        btnTogglePassword.setOnClickListener(v1 -> {
            passwordVisible = !passwordVisible;
            int sel = etPassword.getSelectionStart();
            int end = etPassword.getSelectionEnd();
            etPassword.setInputType(passwordVisible
                    ? android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePassword.setImageResource(passwordVisible ? R.drawable.ic_visibility : R.drawable.ic_visibility_off);
            etPassword.setSelection(sel, end);
        });
        btnTogglePasswordConfirm.setOnClickListener(v1 -> {
            passwordConfirmVisible = !passwordConfirmVisible;
            int sel = etPasswordConfirm.getSelectionStart();
            int end = etPasswordConfirm.getSelectionEnd();
            etPasswordConfirm.setInputType(passwordConfirmVisible
                    ? android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePasswordConfirm.setImageResource(passwordConfirmVisible ? R.drawable.ic_visibility : R.drawable.ic_visibility_off);
            etPasswordConfirm.setSelection(sel, end);
        });

        // Prefill from saved state (when user navigates back from step 2)
        Bundle args = getArguments();
        if (args != null) {
            if (args.getString("name") != null) etName.setText(args.getString("name"));
            if (args.getString("email") != null) etEmail.setText(args.getString("email"));
            if (args.getString("password") != null) etPassword.setText(args.getString("password"));
            if (args.getString("passwordConfirmation") != null) etPasswordConfirm.setText(args.getString("passwordConfirmation"));
        }

        btnNext.setOnClickListener(v1 -> {
            String name = etName.getText() != null ? etName.getText().toString() : "";
            String email = etEmail.getText() != null ? etEmail.getText().toString() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
            String confirm = etPasswordConfirm.getText() != null ? etPasswordConfirm.getText().toString() : "";

            if (name.trim().isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your name", Toast.LENGTH_SHORT).show();
                return;
            }
            if (email.trim().isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 8) {
                Toast.makeText(requireContext(), "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirm)) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            ((RegisterActivity) requireActivity()).onStep1Next(name, email, password, confirm);
        });

        return v;
    }
}
