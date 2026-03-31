package com.example.messapp.ui.mess.requests;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messapp.R;
import com.example.messapp.adapters.RequestAdapter;
import com.example.messapp.models.MealRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import com.example.messapp.adapters.SubscriptionRequestAdapter;
import com.example.messapp.models.SubscriptionRequest;
import com.example.messapp.ui.mess.dashboard.MessDashboardFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.text.TextUtils;
import android.widget.ProgressBar;

public class MessRequestsFragment extends Fragment {

    private RecyclerView recyclerViewMealRequests;
    private RequestAdapter mealRequestAdapter;
    private List<MealRequest> mealRequestList = new ArrayList<>();

    private RecyclerView recyclerViewSubRequests;
    private SubscriptionRequestAdapter subRequestAdapter;
    private List<SubscriptionRequest> subRequestList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentMessId;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mess_requests, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        progressBar = view.findViewById(R.id.progressBar);
        if (progressBar == null) {
            // If progressBar is not in layout, we might need a frame layout or just skip
        }

        setupMealRequests(view);
        setupSubscriptionRequests(view);

        fetchMessIdAndLoadData();

        return view;
    }

    private void setupMealRequests(View view) {
        recyclerViewMealRequests = view.findViewById(R.id.recycler_view_requests);
        recyclerViewMealRequests.setLayoutManager(new LinearLayoutManager(getContext()));
        mealRequestAdapter = new RequestAdapter();
        recyclerViewMealRequests.setAdapter(mealRequestAdapter);

        mealRequestAdapter.setOnConfirmClickListener(request -> {
            db.collection("meal_requests").document(request.getId()).delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Request confirmed and removed", Toast.LENGTH_SHORT).show();
                        fetchMealRequests();
                    })
                    .addOnFailureListener(
                            e -> Toast.makeText(getContext(), "Error confirming request", Toast.LENGTH_SHORT).show());
        });
    }

    private void setupSubscriptionRequests(View view) {
        recyclerViewSubRequests = view.findViewById(R.id.recycler_pending);
        recyclerViewSubRequests.setLayoutManager(new LinearLayoutManager(getContext()));
        subRequestAdapter = new SubscriptionRequestAdapter();
        recyclerViewSubRequests.setAdapter(subRequestAdapter);

        subRequestAdapter.setOnConfirmClickListener(this::showGrantSubscriptionDialog);
        subRequestAdapter.setOnDeleteClickListener(this::deleteSubscriptionRequest);
    }

    private void fetchMessIdAndLoadData() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Sign in to view requests", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentMessId = documentSnapshot.getString("messId");
                        if (currentMessId != null) {
                            fetchMealRequests();
                            fetchSubscriptionRequests();
                        }
                    }
                });
    }

    private void fetchSubscriptionRequests() {
        db.collection("subscriptionRequests")
                .whereEqualTo("messId", currentMessId)
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    subRequestList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        SubscriptionRequest req = doc.toObject(SubscriptionRequest.class);
                        if (req != null)
                            subRequestList.add(req);
                    }
                    subRequestAdapter.setRequests(subRequestList);
                });
    }

    private void showGrantSubscriptionDialog(SubscriptionRequest request) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_grant_subscription, null);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
        TextInputEditText etDays = dialogView.findViewById(R.id.etDays);
        android.widget.RadioGroup radioGroupMealType = dialogView.findViewById(R.id.radio_group_meal_type);

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setPositiveButton("Grant", (dialog, which) -> {
                    String amountStr = etAmount.getText().toString().trim();
                    String daysStr = etDays.getText().toString().trim();

                    if (TextUtils.isEmpty(amountStr) || TextUtils.isEmpty(daysStr)) {
                        Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String mealType = "BOTH";
                    int selectedId = radioGroupMealType.getCheckedRadioButtonId();
                    if (selectedId == R.id.radio_lunch)
                        mealType = "LUNCH";
                    else if (selectedId == R.id.radio_dinner)
                        mealType = "DINNER";

                    grantSubscription(request, Double.parseDouble(amountStr), Integer.parseInt(daysStr), mealType);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void grantSubscription(SubscriptionRequest request, double amount, int days, String mealType) {
        if (progressBar != null)
            progressBar.setVisibility(View.VISIBLE);

        db.collection("users").document(request.getStudentId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    long lunchExpiry = System.currentTimeMillis();
                    long dinnerExpiry = System.currentTimeMillis();
                    long generalExpiry = System.currentTimeMillis();

                    if (documentSnapshot.exists()) {
                        Long existingL = documentSnapshot.getLong("lunchSubscriptionExpiry");
                        Long existingD = documentSnapshot.getLong("dinnerSubscriptionExpiry");
                        Long existingG = documentSnapshot.getLong("subscriptionExpiry");

                        if (existingL != null && existingL > lunchExpiry)
                            lunchExpiry = existingL;
                        if (existingD != null && existingD > dinnerExpiry)
                            dinnerExpiry = existingD;
                        if (existingG != null && existingG > generalExpiry)
                            generalExpiry = existingG;
                    }

                    if (mealType.equals("LUNCH") || mealType.equals("BOTH")) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(lunchExpiry);
                        cal.add(Calendar.DAY_OF_YEAR, days);
                        lunchExpiry = cal.getTimeInMillis();
                    }
                    if (mealType.equals("DINNER") || mealType.equals("BOTH")) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(dinnerExpiry);
                        cal.add(Calendar.DAY_OF_YEAR, days);
                        dinnerExpiry = cal.getTimeInMillis();
                    }

                    generalExpiry = Math.max(lunchExpiry, dinnerExpiry);

                    Map<String, Object> userUpdate = new HashMap<>();
                    userUpdate.put("subscriptionExpiry", generalExpiry);
                    userUpdate.put("lunchSubscriptionExpiry", lunchExpiry);
                    userUpdate.put("dinnerSubscriptionExpiry", dinnerExpiry);

                    // Create Transaction
                    String transId = db.collection("transactions").document().getId();
                    com.example.messapp.models.Transaction transaction = new com.example.messapp.models.Transaction(
                            transId,
                            currentMessId,
                            request.getStudentId(),
                            amount,
                            days,
                            System.currentTimeMillis(),
                            mealType);

                    // Use batch write for atomicity
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    batch.update(db.collection("users").document(request.getStudentId()), userUpdate);
                    batch.update(db.collection("subscriptionRequests").document(request.getId()), "status", "GRANTED");
                    batch.set(db.collection("transactions").document(transId), transaction);

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                if (progressBar != null)
                                    progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Subscription granted successfully!",
                                        Toast.LENGTH_SHORT).show();
                                fetchSubscriptionRequests();
                            })
                            .addOnFailureListener(e -> {
                                if (progressBar != null)
                                    progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Failed to grant: " + e.getMessage(), Toast.LENGTH_SHORT)
                                        .show();
                            });
                });
    }

    private void deleteSubscriptionRequest(SubscriptionRequest request) {
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Delete Request")
                .setMessage("Are you sure you want to delete this subscription request?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (progressBar != null)
                        progressBar.setVisibility(View.VISIBLE);
                    db.collection("subscriptionRequests").document(request.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                if (progressBar != null)
                                    progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Request deleted successfully!", Toast.LENGTH_SHORT)
                                        .show();
                                fetchSubscriptionRequests();
                            })
                            .addOnFailureListener(e -> {
                                if (progressBar != null)
                                    progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT)
                                        .show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void fetchMealRequests() {
        db.collection("meal_requests").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        mealRequestList.clear();
                        for (DocumentSnapshot document : task.getResult()) {
                            MealRequest request = new MealRequest(
                                    document.getId(),
                                    document.getString("userId"),
                                    document.getString("mealType"));
                            mealRequestList.add(request);
                        }
                        mealRequestAdapter.setRequests(mealRequestList);
                    } else {
                        Toast.makeText(getContext(), "Error getting requests", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
