package com.kartik.messapp.ui.mess.students;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.kartik.messapp.R;
import com.kartik.messapp.models.Student;
import com.kartik.messapp.models.Transaction;
import com.kartik.messapp.ui.mess.revenue.TransactionsAdapter;
import com.kartik.messapp.utils.ThemeManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PastMemberDetailsActivity extends AppCompatActivity {

    private String userId;
    private String messId;
    private String studentName;

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView textEmpty;
    private TransactionsAdapter adapter;
    private FirebaseFirestore db;
    private List<Transaction> transactionsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_past_member_details);

        userId = getIntent().getStringExtra("USER_ID");
        messId = getIntent().getStringExtra("MESS_ID");
        studentName = getIntent().getStringExtra("STUDENT_NAME");

        if (userId == null || messId == null) {
            Toast.makeText(this, "Missing data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView textName = findViewById(R.id.text_name);
        textName.setText(studentName != null ? studentName : "Unknown");

        recyclerView = findViewById(R.id.recycler_transactions);
        progressBar = findViewById(R.id.progress_bar);
        textEmpty = findViewById(R.id.text_empty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create a dummy student map so the adapter can show the correct name
        Map<String, Student> map = new HashMap<>();
        Student s = new Student();
        s.setUserId(userId);
        s.setName(studentName);
        map.put(userId, s);

        adapter = new TransactionsAdapter(transactionsList, map);
        recyclerView.setAdapter(adapter);

        loadTransactions();
    }

    private void loadTransactions() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("transactions")
                .whereEqualTo("messId", messId)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    transactionsList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Transaction t = doc.toObject(Transaction.class);
                        if (t != null) {
                            transactionsList.add(t);
                        }
                    }
                    
                    // Sort locally to avoid needing a Firestore composite index
                    java.util.Collections.sort(transactionsList, (t1, t2) -> Long.compare(t2.getTimestamp(), t1.getTimestamp()));
                    
                    adapter.updateData(transactionsList);
                    if (transactionsList.isEmpty()) {
                        textEmpty.setVisibility(View.VISIBLE);
                    } else {
                        textEmpty.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load transactions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
