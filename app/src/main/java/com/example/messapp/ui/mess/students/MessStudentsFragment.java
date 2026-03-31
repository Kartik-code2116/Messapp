package com.example.messapp.ui.mess.students;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.messapp.R;
import com.example.messapp.databinding.FragmentMessStudentsBinding;
import com.example.messapp.models.Student;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import android.widget.RadioGroup;
import android.widget.RadioButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MessStudentsFragment extends Fragment {

    private FragmentMessStudentsBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private List<Student> allStudentsList = new ArrayList<>();
    private List<Student> filteredStudentsList = new ArrayList<>();
    private String currentMessId;
    private double currentMessMonthlyPrice = 0.0;
    private ListenerRegistration studentsListener;
    private ListenerRegistration mealSelectionsListener;
    private StudentsAdapter studentsAdapter;

    private String currentFilterType = "All";
    private String currentSearchQuery = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMessStudentsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        fetchMessOwnerData();
        setupRecyclerView();
        setupFilters();
        setupSearch();

        return root;
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                applyCurrentFilters();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });
    }

    private void setupRecyclerView() {
        studentsAdapter = new StudentsAdapter();
        studentsAdapter.setOnManageClickListener(new StudentsAdapter.OnManageClickListener() {
            @Override
            public void onManageClick(Student student) {
                showManageSubscriptionDialog(student);
            }

            @Override
            public void onDeleteClick(Student student) {
                showDeleteConfirmationDialog(student);
            }

            @Override
            public void onResetClick(Student student) {
                resetStudentMealPermission(student);
            }
        });
        binding.recyclerViewStudents
                .setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
        binding.recyclerViewStudents.setAdapter(studentsAdapter);
    }

    private void showDeleteConfirmationDialog(Student student) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_freeze_delete, null);

        TextInputEditText etFreezeDays = dialogView.findViewById(R.id.et_freeze_days);
        View btnFreeze = dialogView.findViewById(R.id.btn_freeze_account);
        View btnDelete = dialogView.findViewById(R.id.btn_delete_permanently);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .create();

        btnFreeze.setOnClickListener(v -> {
            String daysStr = etFreezeDays.getText().toString().trim();
            if (TextUtils.isEmpty(daysStr)) {
                Toast.makeText(getContext(), "Please enter days to freeze", Toast.LENGTH_SHORT).show();
                return;
            }
            int days = Integer.parseInt(daysStr);
            freezeAccount(student, days);
            dialog.dismiss();
        });

        btnDelete.setOnClickListener(v -> {
            // Show double confirmation for permanent delete? Or just proceed safely since
            // they clicked specific button.
            // Let's add a quick alert for double safety inside this click or just proceed.
            // Given the distinct red button, proceeding is acceptable, but let's be safe.
            new AlertDialog.Builder(getContext())
                    .setTitle("Confirm Permanent Delete")
                    .setMessage("Are you absolutely sure?")
                    .setPositiveButton("Yes, Delete", (d, w) -> {
                        deleteStudent(student);
                        dialog.dismiss();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        dialog.show();
    }

    private void freezeAccount(Student student, int days) {
        if (binding == null)
            return;
        binding.progressBar.setVisibility(View.VISIBLE);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, days);
        long frozenUntil = calendar.getTimeInMillis();

        Map<String, Object> updates = new HashMap<>();
        updates.put("isFrozen", true);
        updates.put("frozenUntil", frozenUntil);

        db.collection("users").document(student.getUserId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (binding == null)
                        return;
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Account frozen for " + days + " days", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (binding == null)
                        return;
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error freezing account: " + e.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                });
    }

    private void deleteStudent(Student student) {
        if (binding == null)
            return;
        binding.progressBar.setVisibility(View.VISIBLE);

        db.collection("users").document(student.getUserId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (binding == null)
                        return;
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Student deleted successfully", Toast.LENGTH_SHORT).show();
                    // The realtime listener will automatically update the list
                })
                .addOnFailureListener(e -> {
                    if (binding == null)
                        return;
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error deleting student: " + e.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                });
    }

    private void resetStudentMealPermission(Student student) {
        if (binding == null)
            return;

        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String selectionDocId = currentMessId + "_" + todayDate + "_" + student.getUserId();

        new AlertDialog.Builder(getContext())
                .setTitle("Reset Meal Status")
                .setMessage("Allow " + (student.getName() != null ? student.getName() : "this student")
                        + " to re-enter their meal status for today?")
                .setPositiveButton("Reset", (dialog, which) -> {
                    binding.progressBar.setVisibility(View.VISIBLE);
                    db.collection("meal_selections").document(selectionDocId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                if (binding == null)
                                    return;
                                binding.progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Status reset successfully!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                if (binding == null)
                                    return;
                                binding.progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupFilters() {
        binding.btnBack.setOnClickListener(v -> {
            if (getActivity() != null)
                getActivity().onBackPressed();
        });

        binding.btnRefresh.setOnClickListener(v -> {
            binding.etSearch.setText("");
            currentSearchQuery = "";
            currentFilterType = "All";
            updateFilterUI();
            fetchMessOwnerData(); // This will re-trigger the data fetch
            Toast.makeText(getContext(), "Refreshing data...", Toast.LENGTH_SHORT).show();
        });

        binding.btnFilterAll.setOnClickListener(v -> {
            binding.etSearch.setText("");
            currentSearchQuery = "";
            currentFilterType = "All";
            updateFilterUI();
            applyCurrentFilters();
        });
        binding.btnFilterActiveToday.setOnClickListener(v -> {
            currentFilterType = "Active Today";
            updateFilterUI();
            applyCurrentFilters();
        });
        binding.btnFilterExpiredToday.setOnClickListener(v -> {
            currentFilterType = "Expired Today";
            updateFilterUI();
            applyCurrentFilters();
        });

        binding.btnFilterInToday.setOnClickListener(v -> {
            currentFilterType = "IN Today";
            updateFilterUI();
            applyCurrentFilters();
        });

        binding.btnFilterOutToday.setOnClickListener(v -> {
            currentFilterType = "OUT Today";
            updateFilterUI();
            applyCurrentFilters();
        });
    }

    private void updateFilterUI() {
        // Reset all to unselected
        binding.btnFilterAll.setBackgroundResource(R.drawable.bg_chip_unselected);
        binding.btnFilterAll.setTextColor(getResources().getColor(R.color.ios_grey_400));
        binding.btnFilterActiveToday.setBackgroundResource(R.drawable.bg_chip_unselected);
        binding.btnFilterActiveToday.setTextColor(getResources().getColor(R.color.ios_grey_400));
        binding.btnFilterExpiredToday.setBackgroundResource(R.drawable.bg_chip_unselected);
        binding.btnFilterExpiredToday.setTextColor(getResources().getColor(R.color.ios_grey_400));

        binding.btnFilterInTodayCard.setCardBackgroundColor(getResources().getColor(R.color.ios_surface));
        binding.btnFilterInTodayCard.setStrokeColor(getResources().getColor(R.color.divider));
        // text in today color
        ((android.widget.TextView) binding.btnFilterInToday.getChildAt(1))
                .setTextColor(getResources().getColor(R.color.ios_grey_400));

        binding.btnFilterOutTodayCard.setCardBackgroundColor(getResources().getColor(R.color.ios_surface));
        binding.btnFilterOutTodayCard.setStrokeColor(getResources().getColor(R.color.divider));
        ((android.widget.TextView) binding.btnFilterOutToday.getChildAt(0))
                .setTextColor(getResources().getColor(R.color.ios_grey_400));

        // Highlight selected
        switch (currentFilterType) {
            case "All":
                binding.btnFilterAll.setBackgroundResource(R.drawable.bg_chip_selected);
                binding.btnFilterAll.setTextColor(getResources().getColor(R.color.white));
                break;
            case "Active Today":
                binding.btnFilterActiveToday.setBackgroundResource(R.drawable.bg_chip_selected);
                binding.btnFilterActiveToday.setTextColor(getResources().getColor(R.color.white));
                break;
            case "Expired Today":
                binding.btnFilterExpiredToday.setBackgroundResource(R.drawable.bg_chip_selected);
                binding.btnFilterExpiredToday.setTextColor(getResources().getColor(R.color.white));
                break;
            case "IN Today":
                binding.btnFilterInTodayCard.setCardBackgroundColor(getResources().getColor(R.color.ios_success_light));
                binding.btnFilterInTodayCard.setStrokeColor(getResources().getColor(R.color.ios_success));
                ((android.widget.TextView) binding.btnFilterInToday.getChildAt(1))
                        .setTextColor(getResources().getColor(R.color.ios_success));
                break;
            case "OUT Today":
                binding.btnFilterOutTodayCard.setCardBackgroundColor(getResources().getColor(R.color.ios_danger_light));
                binding.btnFilterOutTodayCard.setStrokeColor(getResources().getColor(R.color.ios_danger));
                ((android.widget.TextView) binding.btnFilterOutToday.getChildAt(0))
                        .setTextColor(getResources().getColor(R.color.ios_danger));
                break;
        }
    }

    private void applyCurrentFilters() {
        filterStudents(currentFilterType);
    }

    private void filterStudents(String filterType) {
        if (binding == null)
            return;
        filteredStudentsList.clear();
        long currentTimeMillis = System.currentTimeMillis();

        for (Student student : allStudentsList) {
            // Check subscription status - use specific lunch/dinner expiry if available,
            // otherwise general
            long lunchExp = student.getLunchSubscriptionExpiry() > 0 ? student.getLunchSubscriptionExpiry()
                    : (student.getSubscriptionExpiry() > 0 ? student.getSubscriptionExpiry() : 0);
            long dinnerExp = student.getDinnerSubscriptionExpiry() > 0 ? student.getDinnerSubscriptionExpiry()
                    : (student.getSubscriptionExpiry() > 0 ? student.getSubscriptionExpiry() : 0);

            boolean isLunchSubscribed = lunchExp > currentTimeMillis;
            boolean isDinnerSubscribed = dinnerExp > currentTimeMillis;
            boolean isSubscribed = isLunchSubscribed || isDinnerSubscribed; // At least one active

            String lunchStatus = (student.getLunchStatus() != null) ? student.getLunchStatus() : "";
            String dinnerStatus = (student.getDinnerStatus() != null) ? student.getDinnerStatus() : "";
            String name = (student.getName() != null) ? student.getName().toLowerCase() : "";
            String email = (student.getEmail() != null) ? student.getEmail().toLowerCase() : "";

            boolean matchesSearch = TextUtils.isEmpty(currentSearchQuery) ||
                    name.contains(currentSearchQuery) ||
                    email.contains(currentSearchQuery);

            if (!matchesSearch)
                continue;

            if (filterType.equals("All")) {
                filteredStudentsList.add(student);
            } else if (filterType.equals("Active Today") && isSubscribed) {
                filteredStudentsList.add(student);
            } else if (filterType.equals("Expired Today") && !isSubscribed) {
                filteredStudentsList.add(student);
            } else if (filterType.equals("IN Today")) {
                // Check if marked IN for lunch or dinner (and has active subscription for that
                // meal)
                boolean lunchIn = "IN".equals(lunchStatus) && isLunchSubscribed;
                boolean dinnerIn = "IN".equals(dinnerStatus) && isDinnerSubscribed;
                if (lunchIn || dinnerIn) {
                    filteredStudentsList.add(student);
                }
            } else if (filterType.equals("OUT Today")) {
                // Check if marked OUT for lunch or dinner (and has active subscription for that
                // meal)
                boolean lunchOut = "OUT".equals(lunchStatus) && isLunchSubscribed;
                boolean dinnerOut = "OUT".equals(dinnerStatus) && isDinnerSubscribed;
                if (lunchOut || dinnerOut) {
                    filteredStudentsList.add(student);
                }
            }
        }
        if (studentsAdapter != null) {
            studentsAdapter.submitList(new ArrayList<>(filteredStudentsList));
        }
    }

    private void showManageSubscriptionDialog(Student student) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_manage_subscription, null);

        android.widget.TextView textCurrentExpiry = dialogView.findViewById(R.id.text_current_expiry);
        RadioGroup radioGroupMode = dialogView.findViewById(R.id.radio_group_mode);
        RadioButton radioExtend = dialogView.findViewById(R.id.radio_extend);
        TextInputEditText etDays = dialogView.findViewById(R.id.et_days);
        // Ah, I see I used et_days/et_amount in XML but in Java I might have had
        // different IDs before? No, they were et_days/et_amount.
        TextInputEditText etAmount = dialogView.findViewById(R.id.et_amount);
        RadioGroup radioGroupMealType = dialogView.findViewById(R.id.radio_group_meal_type);

        long lunchExpiry = student.getLunchSubscriptionExpiry();
        long dinnerExpiry = student.getDinnerSubscriptionExpiry();
        String lunchText = (lunchExpiry > 0)
                ? new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(lunchExpiry))
                : "Not Active";
        String dinnerText = (dinnerExpiry > 0)
                ? new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(dinnerExpiry))
                : "Not Active";

        textCurrentExpiry.setText("Lunch: " + lunchText + "\nDinner: " + dinnerText);

        new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String daysStr = etDays.getText().toString().trim();
                    String amountStr = etAmount.getText().toString().trim();

                    if (TextUtils.isEmpty(daysStr) || TextUtils.isEmpty(amountStr)) {
                        Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int days = Integer.parseInt(daysStr);
                    double amount = Double.parseDouble(amountStr);
                    boolean isExtend = radioExtend.isChecked();

                    String mealType = "BOTH";
                    int selectedId = radioGroupMealType.getCheckedRadioButtonId();
                    if (selectedId == R.id.radio_lunch)
                        mealType = "LUNCH";
                    else if (selectedId == R.id.radio_dinner)
                        mealType = "DINNER";

                    manageSubscription(student, amount, days, isExtend, mealType);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void manageSubscription(Student student, double amount, int days, boolean isExtend, String mealType) {
        if (binding == null)
            return;
        binding.progressBar.setVisibility(View.VISIBLE);

        long newLunchExpiry = student.getLunchSubscriptionExpiry();
        long newDinnerExpiry = student.getDinnerSubscriptionExpiry();
        long now = System.currentTimeMillis();

        if (mealType.equals("LUNCH") || mealType.equals("BOTH")) {
            long base = (newLunchExpiry > now) ? newLunchExpiry : now;
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(base);
            cal.add(Calendar.DAY_OF_YEAR, isExtend ? days : -days);
            newLunchExpiry = cal.getTimeInMillis();
        }

        if (mealType.equals("DINNER") || mealType.equals("BOTH")) {
            long base = (newDinnerExpiry > now) ? newDinnerExpiry : now;
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(base);
            cal.add(Calendar.DAY_OF_YEAR, isExtend ? days : -days);
            newDinnerExpiry = cal.getTimeInMillis();
        }

        // Also update legacy field for safety (use the max of two)
        long overallExpiry = Math.max(newLunchExpiry, newDinnerExpiry);
        // If reduced below now, it's expired.

        if (currentMessId == null) {
            Toast.makeText(getContext(), "Error: Mess ID not found.", Toast.LENGTH_SHORT).show();
            binding.progressBar.setVisibility(View.GONE);
            return;
        }

        String transId = db.collection("transactions").document().getId();
        // Negative days for record if reducing? Or just keep transaction positive as a
        // record of change?
        // Usually transaction amount tracks money. If cutting days and refunding,
        // amount should be negative.
        // If just cutting days (penalty), amount might be 0.
        // We will store exactly what user entered for amount.

        // However, for days granted log, if reducing, we might want to log negative
        // days.
        int loggedDays = isExtend ? days : -days;

        com.example.messapp.models.Transaction transaction = new com.example.messapp.models.Transaction(
                transId,
                currentMessId,
                student.getUserId(),
                amount,
                loggedDays,
                System.currentTimeMillis(),
                mealType);

        com.google.firebase.firestore.WriteBatch batch = db.batch();
        batch.update(db.collection("users").document(student.getUserId()),
                "subscriptionExpiry", overallExpiry,
                "lunchSubscriptionExpiry", newLunchExpiry,
                "dinnerSubscriptionExpiry", newDinnerExpiry);
        batch.set(db.collection("transactions").document(transId), transaction);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    if (binding == null)
                        return;
                    binding.progressBar.setVisibility(View.GONE);
                    String action = isExtend ? "extended" : "reduced";
                    Toast.makeText(getContext(), "Subscription " + action + " by " + days + " days.",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (binding == null)
                        return;
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Deprecated or unused regular grant method can be kept or removed.
    // Keeping for safety if used elsewhere, but setupRecyclerView uses the new one.

    private void fetchMessOwnerData() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Sign in to view students", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();
        // Since we don't know the collection name for mess owners, we'll check the
        // 'users' collection with role ADMIN or OWNER if it exists.
        // Assuming current logic from previous steps:
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null)
                        return;
                    if (documentSnapshot.exists()) {
                        currentMessId = documentSnapshot.getString("messId");
                        if (currentMessId != null) {
                            fetchMessDetails(currentMessId);
                            setupRealtimeStudentsListener(currentMessId);
                        }
                    }
                });
    }

    private void removeListeners() {
        if (studentsListener != null) {
            studentsListener.remove();
            studentsListener = null;
        }
        if (mealSelectionsListener != null) {
            mealSelectionsListener.remove();
            mealSelectionsListener = null;
        }
    }

    private void fetchMessDetails(String messId) {
        db.collection("messes").document(messId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null)
                        return;
                    if (documentSnapshot.exists()) {
                        Double price = documentSnapshot.getDouble("monthlyPrice");
                        if (price != null) {
                            currentMessMonthlyPrice = price;
                        }
                    }
                });
    }

    private void setupRealtimeStudentsListener(String messId) {
        removeListeners();
        studentsListener = db.collection("users")
                .whereEqualTo("role", "USER")
                .whereEqualTo("messId", messId)
                .addSnapshotListener((snapshots, e) -> {
                    if (binding == null)
                        return;
                    if (e != null) {
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshots != null) {
                        allStudentsList.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Student student = doc.toObject(Student.class);
                            if (student != null) {
                                student.setUserId(doc.getId());
                                allStudentsList.add(student);
                            }
                        }
                        applyCurrentFilters();
                        setupRealtimeMealSelectionsListener(messId);
                        updateStudentCount();
                    }
                });
    }

    private void updateStudentCount() {
        if (binding != null && binding.textTotalStudentsCount != null) {
            binding.textTotalStudentsCount.setText(String.valueOf(allStudentsList.size()));
        }
    }

    private void setupRealtimeMealSelectionsListener(String messId) {
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (mealSelectionsListener != null) {
            mealSelectionsListener.remove();
        }

        // Correct collection name: meal_selections
        mealSelectionsListener = db.collection("meal_selections")
                .whereEqualTo("messId", messId)
                .whereEqualTo("date", todayDate)
                .addSnapshotListener((snapshots, e) -> {
                    if (binding == null)
                        return;
                    if (e != null)
                        return;
                    if (snapshots != null) {
                        Map<String, String[]> statusMap = new HashMap<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            String userId = doc.getString("userId");
                            String lunch = doc.getString("lunch");
                            String dinner = doc.getString("dinner");

                            statusMap.put(userId, new String[] { lunch, dinner });
                        }
                        updateStudentsWithStatus(statusMap);
                    }
                });
    }

    private void updateStudentsWithStatus(Map<String, String[]> statusMap) {
        for (int i = 0; i < allStudentsList.size(); i++) {
            Student student = allStudentsList.get(i);
            String[] statuses = statusMap.get(student.getUserId());

            String newLunch = (statuses != null) ? statuses[0] : null;
            String newDinner = (statuses != null) ? statuses[1] : null;

            // Check if status actually changed to avoid unnecessary updates
            // (But more importantly, if it DID change, we MUST create a NEW object)
            String oldLunch = student.getLunchStatus();
            String oldDinner = student.getDinnerStatus();

            boolean changed = false;
            if (newLunch == null) {
                if (oldLunch != null)
                    changed = true;
            } else {
                if (!newLunch.equals(oldLunch))
                    changed = true;
            }

            if (newDinner == null) {
                if (oldDinner != null)
                    changed = true;
            } else {
                if (!newDinner.equals(oldDinner))
                    changed = true;
            }

            if (changed) {
                // Create a COPY, update it, and replace in the list
                Student newStudent = student.copy();
                newStudent.setLunchStatus(newLunch);
                newStudent.setDinnerStatus(newDinner);
                allStudentsList.set(i, newStudent);
            }
        }
        applyCurrentFilters(); // Refresh the visible list with statuses
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (studentsListener != null)
            studentsListener.remove();
        if (mealSelectionsListener != null)
            mealSelectionsListener.remove();
        binding = null;
    }
}
