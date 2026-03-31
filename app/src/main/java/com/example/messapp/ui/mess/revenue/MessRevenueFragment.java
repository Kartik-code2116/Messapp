package com.example.messapp.ui.mess.revenue;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import android.app.AlertDialog;
import android.text.InputType;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.messapp.databinding.FragmentMessRevenueBinding;
import com.example.messapp.models.Student;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;
import java.util.Calendar;
import android.app.DatePickerDialog;
import android.text.Editable;
import android.text.TextWatcher;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

public class MessRevenueFragment extends Fragment {

    private FragmentMessRevenueBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentMessId;
    private double currentMessMonthlyPrice = 0.0;
    private List<Student> studentsList = new ArrayList<>();
    private java.util.Map<String, Student> studentMap = new java.util.HashMap<>();
    private List<com.example.messapp.models.Transaction> allTransactionsList = new ArrayList<>();
    private List<com.example.messapp.models.Transaction> displayedTransactionsList = new ArrayList<>();
    private TransactionsAdapter transactionsAdapter;
    private long filterDateStart = 0;
    private long filterDateEnd = 0;
    private String currentSearchQuery = "";
    private ListenerRegistration studentsListener;
    private ListenerRegistration transactionsListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMessRevenueBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        binding.progressBar.setVisibility(View.GONE); // Default hidden

        setupRecyclerView();
        setupSearchAndFilter();
        fetchMessOwnerData();

        return root;
    }

    private void setupRecyclerView() {
        transactionsAdapter = new TransactionsAdapter(displayedTransactionsList, studentMap);
        binding.recyclerViewTransactions
                .setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
        binding.recyclerViewTransactions.setAdapter(transactionsAdapter);

        binding.btnEditPrice.setOnClickListener(v -> showEditPriceDialog());
    }

    private void setupSearchAndFilter() {
        binding.editSearchTransactions.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.btnFilterDate.setOnClickListener(v -> showDatePicker());

        binding.textActiveFilter.setOnClickListener(v -> {
            filterDateStart = 0;
            filterDateEnd = 0;
            binding.textActiveFilter.setVisibility(View.GONE);
            applyFilters();
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth, 0, 0, 0);
                    selectedDate.set(Calendar.MILLISECOND, 0);
                    filterDateStart = selectedDate.getTimeInMillis();

                    selectedDate.set(Calendar.HOUR_OF_DAY, 23);
                    selectedDate.set(Calendar.MINUTE, 59);
                    selectedDate.set(Calendar.SECOND, 59);
                    filterDateEnd = selectedDate.getTimeInMillis();

                    String dateStr = java.text.DateFormat.getDateInstance().format(selectedDate.getTime());
                    binding.textActiveFilter.setText("Date: " + dateStr + " ✕");
                    binding.textActiveFilter.setVisibility(View.VISIBLE);

                    applyFilters();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void applyFilters() {
        displayedTransactionsList.clear();
        for (com.example.messapp.models.Transaction transaction : allTransactionsList) {
            boolean matchesSearch = true;
            boolean matchesDate = true;

            // Search Filter
            if (!currentSearchQuery.isEmpty()) {
                Student student = studentMap.get(transaction.getUserId());
                String name = (student != null && student.getName() != null) ? student.getName().toLowerCase() : "";
                String email = (student != null && student.getEmail() != null) ? student.getEmail().toLowerCase() : "";

                if (!name.contains(currentSearchQuery) && !email.contains(currentSearchQuery)) {
                    matchesSearch = false;
                    // android.util.Log.d("MessRevenue", "No match: " + name + "/" + email);
                }
            }

            // Date Filter
            if (filterDateStart != 0 && filterDateEnd != 0) {
                if (transaction.getTimestamp() < filterDateStart || transaction.getTimestamp() > filterDateEnd) {
                    matchesDate = false;
                }
            }

            if (matchesSearch && matchesDate) {
                displayedTransactionsList.add(transaction);
            }
        }

        if (transactionsAdapter != null) {
            transactionsAdapter.updateData(new ArrayList<>(displayedTransactionsList));
            binding.textTransactionCount.setText(displayedTransactionsList.size() + " total");
        }
    }

    private void showEditPriceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Update Monthly Price");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.valueOf(currentMessMonthlyPrice));
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newPriceStr = input.getText().toString();
            try {
                double newPrice = Double.parseDouble(newPriceStr);
                updateMessPrice(newPrice);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid price", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateMessPrice(double newPrice) {
        if (currentMessId != null) {
            db.collection("messes").document(currentMessId).update("monthlyPrice", newPrice)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Price updated to ₹" + newPrice, Toast.LENGTH_SHORT).show();
                        currentMessMonthlyPrice = newPrice;
                        if (binding.textMessMonthlyPrice != null) {
                            binding.textMessMonthlyPrice
                                    .setText(String.format(Locale.getDefault(), "Monthly Mess Price: ₹%.2f", newPrice));
                        }
                        if (binding.textRevenueBasis != null) {
                            binding.textRevenueBasis.setText(String.format(Locale.getDefault(),
                                    "Based on active subscriptions at ₹%.2f/mo", newPrice));
                        }
                        calculateAndDisplayRevenue();
                    })
                    .addOnFailureListener(e -> Toast
                            .makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(getContext(), "Error: Mess ID not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchMessOwnerData() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Sign in to view revenue", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null)
                        return;
                    if (documentSnapshot.exists()) {
                        currentMessId = documentSnapshot.getString("messId");
                        if (currentMessId != null) {
                            fetchMessDetails(currentMessId);
                            setupRealtimeStudentsListener(currentMessId);
                            setupRealtimeTransactionsListener(currentMessId);
                        }
                    }
                });
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
                            if (binding.textMessMonthlyPrice != null) {
                                binding.textMessMonthlyPrice
                                        .setText(
                                                String.format(Locale.getDefault(), "Monthly Mess Price: ₹%.2f", price));
                            }
                        }
                        calculateAndDisplayRevenue();
                    }
                });
    }

    private void setupRealtimeStudentsListener(String messId) {
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
                        studentsList.clear();
                        studentMap.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Student student = doc.toObject(Student.class);
                            if (student != null) {
                                // Ensure userId is set, fallback to document ID
                                if (student.getUserId() == null || student.getUserId().isEmpty()) {
                                    student.setUserId(doc.getId());
                                }

                                studentsList.add(student);
                                // Use document ID as the map key to ensure consistency
                                studentMap.put(doc.getId(), student);
                            }
                        }
                        if (transactionsAdapter != null) {
                            transactionsAdapter.updateStudentMap(studentMap);
                            applyFilters(); // Re-apply filters as names might have changed
                        }
                        calculateAndDisplayRevenue();
                    }
                });
    }

    private void setupRealtimeTransactionsListener(String messId) {
        transactionsListener = db.collection("transactions")
                .whereEqualTo("messId", messId)
                .addSnapshotListener((snapshots, e) -> {
                    if (binding == null)
                        return;
                    if (e != null) {
                        android.util.Log.e("MessRevenue", "Transactions error", e);
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Error loading revenue: " + e.getMessage(), Toast.LENGTH_SHORT)
                                    .show();
                        }
                        return;
                    }
                    if (snapshots != null) {
                        allTransactionsList.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            com.example.messapp.models.Transaction transaction = doc
                                    .toObject(com.example.messapp.models.Transaction.class);
                            if (transaction != null) {
                                allTransactionsList.add(transaction);
                            }
                        }

                        // Sort in memory: newest first (descending by timestamp)
                        java.util.Collections.sort(allTransactionsList,
                                (t1, t2) -> Long.compare(t2.getTimestamp(), t1.getTimestamp()));

                        fetchMissingStudents();
                    }
                });
    }

    private void fetchMissingStudents() {
        Set<String> missingUserIds = new HashSet<>();
        for (com.example.messapp.models.Transaction trans : allTransactionsList) {
            if (!studentMap.containsKey(trans.getUserId())) {
                missingUserIds.add(trans.getUserId());
            }
        }

        if (missingUserIds.isEmpty()) {
            applyFilters();
            calculateAndDisplayRevenue();
            return;
        }

        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String userId : missingUserIds) {
            if (userId != null && !userId.isEmpty()) {
                tasks.add(db.collection("users").document(userId).get());
            }
        }

        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            if (binding == null)
                return;
            for (Object result : results) {
                DocumentSnapshot doc = (DocumentSnapshot) result;
                if (doc.exists()) {
                    Student student = doc.toObject(Student.class);
                    if (student != null) {
                        student.setUserId(doc.getId()); // Ensure ID is set
                        studentMap.put(doc.getId(), student);
                    }
                }
            }
            if (transactionsAdapter != null) {
                transactionsAdapter.updateStudentMap(studentMap);
                applyFilters();
            }
            calculateAndDisplayRevenue();
        });
    }

    private void calculateAndDisplayRevenue() {
        if (binding == null)
            return;
        int activeStudentsCount = 0;
        long currentTimeMillis = System.currentTimeMillis();
        for (Student student : studentsList) {
            // Check if student has at least one active subscription (lunch, dinner, or
            // general)
            long lunchExp = student.getLunchSubscriptionExpiry() > 0 ? student.getLunchSubscriptionExpiry()
                    : (student.getSubscriptionExpiry() > 0 ? student.getSubscriptionExpiry() : 0);
            long dinnerExp = student.getDinnerSubscriptionExpiry() > 0 ? student.getDinnerSubscriptionExpiry()
                    : (student.getSubscriptionExpiry() > 0 ? student.getSubscriptionExpiry() : 0);

            if (lunchExp > currentTimeMillis || dinnerExp > currentTimeMillis) {
                activeStudentsCount++;
            }
        }

        double expectedRevenue = currentMessMonthlyPrice * activeStudentsCount;
        if (binding.textRevenueBasis != null) {
            binding.textRevenueBasis.setText(String.format(Locale.getDefault(),
                    "Based on %d active subscriptions at ₹%.2f/mo", activeStudentsCount, currentMessMonthlyPrice));
        }

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        long startOfMonth = calendar.getTimeInMillis();

        double actualRevenueMonthly = 0;
        double actualRevenueTotal = 0;

        for (com.example.messapp.models.Transaction trans : allTransactionsList) {
            actualRevenueTotal += trans.getAmount();
            if (trans.getTimestamp() >= startOfMonth) {
                actualRevenueMonthly += trans.getAmount();
            }
        }

        binding.textMonthlyRevenueExpected.setText(String.format(Locale.getDefault(), "₹%.2f", expectedRevenue));
        binding.textMonthlyRevenueActual.setText(String.format(Locale.getDefault(), "₹%.2f", actualRevenueMonthly));
        binding.textTotalRevenueAllTime.setText(String.format(Locale.getDefault(), "₹%.2f", actualRevenueTotal));
        if (binding.textRevenueBasis != null) {
            binding.textRevenueBasis
                    .setText(String.format(Locale.getDefault(), "Based on %d active subscriptions.",
                            activeStudentsCount));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (studentsListener != null) {
            studentsListener.remove();
        }
        if (transactionsListener != null) {
            transactionsListener.remove();
        }
        binding = null;
    }
}
