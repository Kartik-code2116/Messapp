package com.example.messapp.ui.mess.requests;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messapp.R;
import com.example.messapp.adapters.RequestAdapter;
import com.example.messapp.adapters.SubscriptionRequestAdapter;
import com.example.messapp.managers.MessNotificationManager;
import com.example.messapp.models.MealRequest;
import com.example.messapp.models.SubscriptionRequest;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private View cardEmptyState;
    private View layoutPendingSubs;
    private View layoutMealRequests;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mess_requests, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        progressBar = view.findViewById(R.id.progressBar);
        cardEmptyState = view.findViewById(R.id.card_empty_state);
        layoutPendingSubs = view.findViewById(R.id.layout_pending_subs);
        layoutMealRequests = view.findViewById(R.id.layout_meal_requests);

        setupMealRequests(view);
        setupSubscriptionRequests(view);
        fetchMessIdAndLoadData();
        
        androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipe_refresh_requests);
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                fetchMessIdAndLoadData();
                swipeRefresh.setRefreshing(false);
            });
        }

        return view;
    }

    private void setupMealRequests(View view) {
        recyclerViewMealRequests = view.findViewById(R.id.recycler_view_requests);
        recyclerViewMealRequests.setLayoutManager(new LinearLayoutManager(getContext()));
        mealRequestAdapter = new RequestAdapter();
        recyclerViewMealRequests.setAdapter(mealRequestAdapter);

        mealRequestAdapter.setOnConfirmClickListener(request -> {
            confirmExtraMealRequest(request);
        });
    }

    private void confirmExtraMealRequest(MealRequest request) {
        if (request.getUserId() == null || request.getId() == null) {
            Toast.makeText(getContext(), "Invalid request data", Toast.LENGTH_SHORT).show();
            return;
        }
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        com.google.firebase.firestore.DocumentReference userRef = db.collection("users").document(request.getUserId());
        String date = request.getDate() != null ? request.getDate() : new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        String mealDocId = currentMessId + "_" + date + "_" + request.getUserId();
        com.google.firebase.firestore.DocumentReference mealRef = db.collection("meal_selections").document(mealDocId);
        com.google.firebase.firestore.DocumentReference reqRef = db.collection("subscriptionRequests").document(request.getId());

        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot userSnapshot = transaction.get(userRef);
            if (userSnapshot.exists()) {
                Long oneTimeExpiry = userSnapshot.getLong("oneTimeMealExpiry");
                if (oneTimeExpiry != null) {
                    transaction.update(userRef, "oneTimeMealExpiry", oneTimeExpiry - (24 * 60 * 60 * 1000L));
                    Long generalExpiry = userSnapshot.getLong("subscriptionExpiry");
                    if (generalExpiry != null && generalExpiry.equals(oneTimeExpiry)) {
                         transaction.update(userRef, "subscriptionExpiry", generalExpiry - (24 * 60 * 60 * 1000L));
                    }
                }
            }

            Map<String, Object> mealUpdate = new HashMap<>();
            mealUpdate.put("adminAllowedBoth", true);
            String statusKey = "LUNCH".equals(request.getMealType()) ? "lunch" : "dinner";
            mealUpdate.put(statusKey, "IN");
            mealUpdate.put("userId", request.getUserId());
            mealUpdate.put("messId", currentMessId);
            mealUpdate.put("date", date);
            mealUpdate.put("timestamp", System.currentTimeMillis());
            transaction.set(mealRef, mealUpdate, com.google.firebase.firestore.SetOptions.merge());

            transaction.delete(reqRef);
            return null;
        }).addOnSuccessListener(aVoid -> {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Extra meal allowed and 1 day deducted.", Toast.LENGTH_SHORT).show();
            fetchSubscriptionRequests();
        }).addOnFailureListener(e -> {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Error confirming request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        db.collection("users").document(mAuth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentMessId = doc.getString("messId");
                        if (currentMessId != null) {
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
                    mealRequestList.clear();
                    String todayDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        SubscriptionRequest req = doc.toObject(SubscriptionRequest.class);
                        if (req != null) {
                            if (req.getType() != null && req.getType().startsWith("EXTRA_")) {
                                String mealType = req.getType().replace("EXTRA_", "");
                                MealRequest mealReq = new MealRequest(
                                    req.getId(), req.getStudentId(), req.getStudentName(),
                                    req.getMessId(), todayDate, mealType
                                );
                                mealRequestList.add(mealReq);
                            } else {
                                subRequestList.add(req);
                            }
                        }
                    }
                    subRequestAdapter.setRequests(subRequestList);
                    mealRequestAdapter.setRequests(mealRequestList);
                    updateUIState();
                });
    }

    private void showGrantSubscriptionDialog(SubscriptionRequest request) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_grant_subscription, null);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
        TextInputEditText etDays   = dialogView.findViewById(R.id.etDays);
        android.widget.RadioGroup radioGroupMealType = dialogView.findViewById(R.id.radio_group_meal_type);
        android.widget.TextView textOneTimeInfo = dialogView.findViewById(R.id.text_one_time_info);

        radioGroupMealType.setOnCheckedChangeListener((group, checkedId) -> {
            textOneTimeInfo.setVisibility(checkedId == R.id.radio_one_time ? View.VISIBLE : View.GONE);
        });

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setPositiveButton("Grant", (dialog, which) -> {
                    String amountStr = etAmount.getText().toString().trim();
                    String daysStr   = etDays.getText().toString().trim();
                    if (TextUtils.isEmpty(amountStr) || TextUtils.isEmpty(daysStr)) {
                        Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String mealType = "ONE_TIME";
                    int selectedId = radioGroupMealType.getCheckedRadioButtonId();
                    if (selectedId == R.id.radio_lunch)        mealType = "LUNCH";
                    else if (selectedId == R.id.radio_dinner)  mealType = "DINNER";
                    else if (selectedId == R.id.radio_both)    mealType = "BOTH";
                    grantSubscription(request, Double.parseDouble(amountStr), Integer.parseInt(daysStr), mealType);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void grantSubscription(SubscriptionRequest request, double amount, int days, String mealType) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        db.collection("users").document(request.getStudentId()).get()
                .addOnSuccessListener(doc -> {
                    long lunchExpiry   = System.currentTimeMillis();
                    long dinnerExpiry  = System.currentTimeMillis();
                    long oneTimeExpiry = System.currentTimeMillis();
                    long generalExpiry = System.currentTimeMillis();

                    if (doc.exists()) {
                        Long existingL = doc.getLong("lunchSubscriptionExpiry");
                        Long existingD = doc.getLong("dinnerSubscriptionExpiry");
                        Long existingO = doc.getLong("oneTimeMealExpiry");
                        Long existingG = doc.getLong("subscriptionExpiry");
                        if (existingL != null && existingL > lunchExpiry)   lunchExpiry   = existingL;
                        if (existingD != null && existingD > dinnerExpiry)  dinnerExpiry  = existingD;
                        if (existingO != null && existingO > oneTimeExpiry) oneTimeExpiry = existingO;
                        if (existingG != null && existingG > generalExpiry) generalExpiry = existingG;
                    }

                    if (mealType.equals("ONE_TIME")) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(oneTimeExpiry);
                        cal.add(Calendar.DAY_OF_YEAR, days);
                        oneTimeExpiry = cal.getTimeInMillis();
                        lunchExpiry = 0;
                        dinnerExpiry = 0;
                    } else {
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
                        oneTimeExpiry = 0;
                    }
                    generalExpiry = Math.max(oneTimeExpiry, Math.max(lunchExpiry, dinnerExpiry));

                    Map<String, Object> userUpdate = new HashMap<>();
                    userUpdate.put("subscriptionExpiry",       generalExpiry);
                    userUpdate.put("lunchSubscriptionExpiry",  lunchExpiry);
                    userUpdate.put("dinnerSubscriptionExpiry", dinnerExpiry);
                    userUpdate.put("oneTimeMealExpiry",        oneTimeExpiry);
                    userUpdate.put("subscriptionType",         mealType);

                    String transId = db.collection("transactions").document().getId();
                    com.example.messapp.models.Transaction transaction =
                            new com.example.messapp.models.Transaction(
                                    transId, currentMessId, request.getStudentId(),
                                    amount, days, System.currentTimeMillis(), mealType);

                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    batch.update(db.collection("users").document(request.getStudentId()), userUpdate);
                    batch.update(db.collection("subscriptionRequests").document(request.getId()), "status", "GRANTED");
                    batch.set(db.collection("transactions").document(transId), transaction);
                    final long notificationExpiry = generalExpiry;

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                                String senderId = mAuth.getCurrentUser() != null
                                        ? mAuth.getCurrentUser().getUid() : "";
                                MessNotificationManager.sendSubscriptionGranted(
                                        currentMessId,
                                        senderId,
                                        request.getStudentId(),
                                        mealType,
                                        days,
                                        notificationExpiry);
                                Toast.makeText(getContext(), "Subscription granted!", Toast.LENGTH_SHORT).show();
                                fetchSubscriptionRequests();
                            })
                            .addOnFailureListener(e -> {
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void deleteSubscriptionRequest(SubscriptionRequest request) {
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Delete Request")
                .setMessage("Delete this subscription request?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                    db.collection("subscriptionRequests").document(request.getId()).delete()
                            .addOnSuccessListener(aVoid -> {
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Request deleted.", Toast.LENGTH_SHORT).show();
                                fetchSubscriptionRequests();
                            })
                            .addOnFailureListener(e -> {
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateUIState() {
        boolean hasSubs = subRequestList != null && !subRequestList.isEmpty();
        boolean hasMeals = mealRequestList != null && !mealRequestList.isEmpty();

        if (layoutPendingSubs != null) {
            layoutPendingSubs.setVisibility(hasSubs ? View.VISIBLE : View.GONE);
        }
        if (layoutMealRequests != null) {
            layoutMealRequests.setVisibility(hasMeals ? View.VISIBLE : View.GONE);
        }
        if (cardEmptyState != null) {
            cardEmptyState.setVisibility((!hasSubs && !hasMeals) ? View.VISIBLE : View.GONE);
        }
    }
}
