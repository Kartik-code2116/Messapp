package com.example.messapp.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messapp.R;
import com.example.messapp.databinding.FragmentUserHistoryBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.messapp.models.MealRequest;

import java.util.ArrayList;
import java.util.List;

public class UserHistoryFragment extends Fragment {

    private FragmentUserHistoryBinding binding;
    private FirebaseFirestore db;
    private HistoryAdapter adapter;
    private String userId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUserHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        adapter = new HistoryAdapter();
        binding.recyclerViewHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewHistory.setAdapter(adapter);

        loadHistory();
    }

    private void loadHistory() {
        // Fetch all meal selections for this user
        db.collection("meal_selections")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<HistoryItem> items = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String date = doc.getString("date");
                        String lunch = doc.getString("lunch");
                        String dinner = doc.getString("dinner");

                        if ("IN".equals(lunch) || "IN".equals(dinner)) {
                            items.add(new HistoryItem(date, lunch, dinner));
                        }
                    }
                    adapter.setItems(items);
                });
    }

    // Inner Model
    public static class HistoryItem {
        String date;
        String lunchStatus;
        String dinnerStatus;

        public HistoryItem(String date, String lunchStatus, String dinnerStatus) {
            this.date = date;
            this.lunchStatus = lunchStatus;
            this.dinnerStatus = dinnerStatus;
        }
    }

    // Inner Adapter
    public static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<HistoryItem> items = new ArrayList<>();

        public void setItems(List<HistoryItem> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HistoryItem item = items.get(position);
            holder.textDate.setText(item.date);

            holder.textLunch.setText("Lunch: " + (item.lunchStatus != null ? item.lunchStatus : "-"));
            holder.textDinner.setText("Dinner: " + (item.dinnerStatus != null ? item.dinnerStatus : "-"));

            if ("IN".equals(item.lunchStatus))
                holder.textLunch.setTextColor(Color.parseColor("#4CAF50"));
            if ("IN".equals(item.dinnerStatus))
                holder.textDinner.setTextColor(Color.parseColor("#4CAF50"));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textDate, textLunch, textDinner;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                textDate = itemView.findViewById(R.id.text_history_date);
                textLunch = itemView.findViewById(R.id.text_lunch_status);
                textDinner = itemView.findViewById(R.id.text_dinner_status);
            }
        }
    }
}
