package com.example.messapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.messapp.adapters.RequestAdapter;
import com.example.messapp.databinding.FragmentMessDashboardBinding;
import com.example.messapp.models.MealRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MessDashboardFragment extends Fragment {

    private FragmentMessDashboardBinding binding;
    private FirebaseFirestore db;
    private String messId;
    private String todayDate;
    private RequestAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMessDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        adapter = new RequestAdapter();
        binding.recyclerPending.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerPending.setAdapter(adapter);

        adapter.setOnConfirmClickListener(this::confirmRequest);

        loadMessDetails();
    }

    private void loadMessDetails() {
        String ownerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("messes").whereEqualTo("ownerId", ownerId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        messId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        listenToLiveStats();
                        listenToTotalStudents();
                    }
                });
    }

    private void listenToTotalStudents() {
        db.collection("users").whereEqualTo("messId", messId)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        binding.textTotalStudents.setText(String.valueOf(value.size()));
                    }
                });
    }

    private void listenToLiveStats() {
        db.collection("meal_selections")
                .whereEqualTo("messId", messId)
                .whereEqualTo("date", todayDate)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        int lunchIn = 0, lunchOut = 0;
                        int dinnerIn = 0, dinnerOut = 0;
                        List<MealRequest> pendingRequests = new ArrayList<>();

                        for (QueryDocumentSnapshot doc : value) {
                            String l = doc.getString("lunch");
                            String d = doc.getString("dinner");
                            String lApp = doc.getString("lunch_approval");
                            String dApp = doc.getString("dinner_approval");

                            if ("IN".equals(l))
                                lunchIn++;
                            if ("OUT".equals(l))
                                lunchOut++;
                            if ("IN".equals(d))
                                dinnerIn++;
                            if ("OUT".equals(d))
                                dinnerOut++;

                            if ("PENDING".equals(lApp)) {
                                pendingRequests.add(new MealRequest(doc.getId(), doc.getString("userId"), "LUNCH"));
                            }
                            if ("PENDING".equals(dApp)) {
                                pendingRequests.add(new MealRequest(doc.getId(), doc.getString("userId"), "DINNER"));
                            }
                        }

                        binding.textLunchInCount.setText(String.valueOf(lunchIn));
                        binding.textLunchOutCount.setText(String.valueOf(lunchOut));
                        binding.textDinnerInCount.setText(String.valueOf(dinnerIn));
                        binding.textDinnerOutCount.setText(String.valueOf(dinnerOut));

                        adapter.setRequests(pendingRequests);
                    }
                });
    }

    private void confirmRequest(MealRequest request) {
        String field = request.getMealType().equals("LUNCH") ? "lunch_approval" : "dinner_approval";

        db.collection("meal_selections").document(request.getId())
                .update(field, "CONFIRMED")
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Confirmed!", Toast.LENGTH_SHORT).show());
    }
}
