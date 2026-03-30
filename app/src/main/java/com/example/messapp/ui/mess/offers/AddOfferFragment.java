package com.example.messapp.ui.mess.offers;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.messapp.models.Offer;
import com.example.messapp.databinding.FragmentAddOfferBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AddOfferFragment extends Fragment {

    private FragmentAddOfferBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentMessId;
    private Calendar selectedExpiryDateCalendar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddOfferBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        selectedExpiryDateCalendar = Calendar.getInstance(); // Default to today

        fetchMessId();
        displaySelectedDate();

        binding.btnSelectExpiryDate.setOnClickListener(v -> showDatePickerDialog());
        binding.btnSaveOffer.setOnClickListener(v -> saveOffer());

        return root;
    }

    private void fetchMessId() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentMessId = documentSnapshot.getString("messId");
                        if (currentMessId == null) {
                            Toast.makeText(getContext(), "Mess ID not found for this user.", Toast.LENGTH_SHORT).show();
                            // Optionally navigate back or disable functionality
                        }
                    } else {
                        Toast.makeText(getContext(), "Mess owner data not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error fetching mess owner data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void displaySelectedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        binding.textExpiryDate.setText("Expiry Date: " + sdf.format(selectedExpiryDateCalendar.getTime()));
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedExpiryDateCalendar.set(year, month, dayOfMonth);
                    displaySelectedDate();
                },
                selectedExpiryDateCalendar.get(Calendar.YEAR),
                selectedExpiryDateCalendar.get(Calendar.MONTH),
                selectedExpiryDateCalendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void saveOffer() {
        if (currentMessId == null) {
            Toast.makeText(getContext(), "Mess ID not available. Cannot save offer.", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = binding.offerTitleEditText.getText().toString().trim();
        String description = binding.offerDescriptionEditText.getText().toString().trim();
        String discountText = binding.offerDiscountEditText.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description) || TextUtils.isEmpty(discountText)) {
            Toast.makeText(getContext(), "Please fill all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        double discountPercentage;
        try {
            discountPercentage = Double.parseDouble(discountText);
            if (discountPercentage < 0 || discountPercentage > 100) {
                Toast.makeText(getContext(), "Discount percentage must be between 0 and 100.", Toast.LENGTH_SHORT)
                        .show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid discount percentage.", Toast.LENGTH_SHORT).show();
            return;
        }

        Date expiryDate = selectedExpiryDateCalendar.getTime();
        if (expiryDate.before(new Date())) {
            Toast.makeText(getContext(), "Expiry date cannot be in the past.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);

        String offerId = UUID.randomUUID().toString();
        Offer newOffer = new Offer(offerId, currentMessId, title, description, discountPercentage, expiryDate);

        db.collection("offers").document(offerId).set(newOffer)
                .addOnSuccessListener(aVoid -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Offer created successfully!", Toast.LENGTH_SHORT).show();
                    sendOfferNotificationToAllStudents(newOffer);
                    getParentFragmentManager().popBackStack(); // Go back to previous fragment
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error creating offer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendOfferNotificationToAllStudents(Offer offer) {
        String notificationId = UUID.randomUUID().toString();
        Map<String, Object> notification = new HashMap<>();
        notification.put("notificationId", notificationId);
        notification.put("messId", offer.getMessId());
        notification.put("offerId", offer.getOfferId());
        notification.put("title", "New Offer: " + offer.getTitle());
        notification.put("description", offer.getDescription());
        notification.put("timestamp", new Date());
        notification.put("type", "OFFER");

        db.collection("notifications").document(notificationId).set(notification)
                .addOnSuccessListener(aVoid -> {
                    // Notification sent
                })
                .addOnFailureListener(e -> {
                    // Handle notification failure
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
