package com.naijameals.driver;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

public class RegisterStep3Fragment extends Fragment {
    private String selectedDeliveryType = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_register_step3, container, false);

        MaterialButton btnCar = v.findViewById(R.id.btnCar);
        MaterialButton btnMotorcycle = v.findViewById(R.id.btnMotorcycle);
        MaterialButton btnBicycle = v.findViewById(R.id.btnBicycle);
        MaterialButton btnPrevious = v.findViewById(R.id.btnPrevious);
        MaterialButton btnRegister = v.findViewById(R.id.btnRegister);

        // Restore selected delivery type (when user navigates back from step 3)
        Bundle args = getArguments();
        if (args != null && args.getString("deliveryType") != null) {
            selectedDeliveryType = args.getString("deliveryType", "");
        }

        int primaryColor = ContextCompat.getColor(requireContext(), R.color.login_primary);
        int whiteColor = ContextCompat.getColor(requireContext(), R.color.white);
        int inactiveColor = ContextCompat.getColor(requireContext(), R.color.gray_light);
        int inactiveTextColor = ContextCompat.getColor(requireContext(), R.color.gray_dark);
        android.content.res.ColorStateList primaryTint = android.content.res.ColorStateList.valueOf(primaryColor);
        android.content.res.ColorStateList inactiveTint = android.content.res.ColorStateList.valueOf(inactiveColor);

        View.OnClickListener typeListener = v1 -> {
            MaterialButton btn = (MaterialButton) v1;
            int id = btn.getId();
            if (id == R.id.btnCar) selectedDeliveryType = "Car";
            else if (id == R.id.btnMotorcycle) selectedDeliveryType = "Motorcycle";
            else if (id == R.id.btnBicycle) selectedDeliveryType = "Bicycle";
            btnCar.setBackgroundTintList(inactiveTint);
            btnMotorcycle.setBackgroundTintList(inactiveTint);
            btnBicycle.setBackgroundTintList(inactiveTint);
            btnCar.setTextColor(inactiveTextColor);
            btnMotorcycle.setTextColor(inactiveTextColor);
            btnBicycle.setTextColor(inactiveTextColor);
            btn.setBackgroundTintList(primaryTint);
            btn.setTextColor(whiteColor);
        };

        btnCar.setOnClickListener(typeListener);
        btnMotorcycle.setOnClickListener(typeListener);
        btnBicycle.setOnClickListener(typeListener);

        // Apply saved selection visually (buttons show emoji, we store API value)
        if ("Car".equals(selectedDeliveryType)) {
            btnCar.setBackgroundTintList(primaryTint);
            btnCar.setTextColor(whiteColor);
        } else if ("Motorcycle".equals(selectedDeliveryType)) {
            btnMotorcycle.setBackgroundTintList(primaryTint);
            btnMotorcycle.setTextColor(whiteColor);
        } else if ("Bicycle".equals(selectedDeliveryType)) {
            btnBicycle.setBackgroundTintList(primaryTint);
            btnBicycle.setTextColor(whiteColor);
        }

        btnPrevious.setOnClickListener(v1 -> ((RegisterActivity) requireActivity()).onStep3Previous());
        btnRegister.setOnClickListener(v1 -> ((RegisterActivity) requireActivity()).onStep3Register(selectedDeliveryType));

        return v;
    }
}
