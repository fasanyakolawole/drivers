package com.naijameals.driver;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterStep2Fragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_register_step2, container, false);

        TextInputEditText etMobile = v.findViewById(R.id.etMobile);
        TextInputEditText etHouseAddress = v.findViewById(R.id.etHouseAddress);
        TextInputEditText etPostcode = v.findViewById(R.id.etPostcode);
        MaterialButton btnPrevious = v.findViewById(R.id.btnPrevious);
        MaterialButton btnNext = v.findViewById(R.id.btnNext);

        // Prefill from saved state (when user navigates back from step 3)
        Bundle args = getArguments();
        if (args != null) {
            if (args.getString("mobile") != null) etMobile.setText(args.getString("mobile"));
            if (args.getString("houseAddress") != null) etHouseAddress.setText(args.getString("houseAddress"));
            if (args.getString("postcode") != null) etPostcode.setText(args.getString("postcode"));
        }

        btnPrevious.setOnClickListener(v1 -> ((RegisterActivity) requireActivity()).onStep2Previous());
        btnNext.setOnClickListener(v1 -> {
            String mobile = etMobile.getText() != null ? etMobile.getText().toString() : "";
            String houseAddress = etHouseAddress.getText() != null ? etHouseAddress.getText().toString() : "";
            String postcode = etPostcode.getText() != null ? etPostcode.getText().toString() : "";
            ((RegisterActivity) requireActivity()).onStep2Next(mobile, houseAddress, postcode);
        });

        return v;
    }
}
