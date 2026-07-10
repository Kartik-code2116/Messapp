package com.kartik.messapp.ui.mess.students;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.kartik.messapp.R;
import com.kartik.messapp.models.PastMember;
import com.kartik.messapp.utils.ThemeManager;

import java.util.ArrayList;
import java.util.List;

public class PastMembersActivity extends AppCompatActivity {

    public static final String EXTRA_MESS_ID = "extra_mess_id";
    private String currentMessId;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView textEmpty;
    private PastMembersAdapter adapter;
    private FirebaseFirestore db;
    private List<PastMember> pastMembersList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_past_members);

        currentMessId = getIntent().getStringExtra(EXTRA_MESS_ID);
        if (currentMessId == null) {
            Toast.makeText(this, "Mess ID is missing", Toast.LENGTH_SHORT).show();
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

        recyclerView = findViewById(R.id.recycler_past_members);
        progressBar = findViewById(R.id.progress_bar);
        textEmpty = findViewById(R.id.text_empty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PastMembersAdapter(member -> {
            Intent intent = new Intent(PastMembersActivity.this, PastMemberDetailsActivity.class);
            intent.putExtra("USER_ID", member.getUserId());
            intent.putExtra("MESS_ID", member.getMessId());
            intent.putExtra("STUDENT_NAME", member.getName());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        loadPastMembers();
    }

    private void loadPastMembers() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("mess_leavers")
                .whereEqualTo("messId", currentMessId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    pastMembersList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        PastMember member = doc.toObject(PastMember.class);
                        if (member != null) {
                            pastMembersList.add(member);
                        }
                    }
                    
                    // Sort locally to avoid needing a Firestore composite index
                    java.util.Collections.sort(pastMembersList, (m1, m2) -> Long.compare(m2.getLeftAt(), m1.getLeftAt()));
                    
                    adapter.submitList(pastMembersList);
                    if (pastMembersList.isEmpty()) {
                        textEmpty.setVisibility(View.VISIBLE);
                    } else {
                        textEmpty.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load past members: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
