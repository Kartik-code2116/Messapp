package com.example.messapp.ui.user.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messapp.R;
import com.example.messapp.models.MealSelection;

public class HistoryAdapter extends ListAdapter<MealSelection, HistoryAdapter.HistoryViewHolder> {

    public HistoryAdapter() {
        super(new MealSelectionDiffCallback());
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        MealSelection mealSelection = getItem(position);
        if (mealSelection != null) {
            holder.bind(mealSelection);
        }
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView textHistoryDate;
        TextView textLunchStatus;
        TextView textDinnerStatus;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textHistoryDate = itemView.findViewById(R.id.text_history_date);
            textLunchStatus = itemView.findViewById(R.id.text_lunch_status);
            textDinnerStatus = itemView.findViewById(R.id.text_dinner_status);
        }

        public void bind(MealSelection mealSelection) {
            textHistoryDate.setText("Date: " + (mealSelection.getDate() != null ? mealSelection.getDate() : "N/A"));
            textLunchStatus.setText("Lunch: " + (mealSelection.getLunchStatus() != null ? mealSelection.getLunchStatus() : "--"));
            textDinnerStatus.setText("Dinner: " + (mealSelection.getDinnerStatus() != null ? mealSelection.getDinnerStatus() : "--"));
        }
    }

    private static class MealSelectionDiffCallback extends DiffUtil.ItemCallback<MealSelection> {
        @Override
        public boolean areItemsTheSame(@NonNull MealSelection oldItem, @NonNull MealSelection newItem) {
            // Use userId + date as unique identifier
            String oldId = (oldItem.getUserId() != null ? oldItem.getUserId() : "") + 
                          (oldItem.getDate() != null ? oldItem.getDate() : "");
            String newId = (newItem.getUserId() != null ? newItem.getUserId() : "") + 
                          (newItem.getDate() != null ? newItem.getDate() : "");
            return oldId.equals(newId);
        }

        @Override
        public boolean areContentsTheSame(@NonNull MealSelection oldItem, @NonNull MealSelection newItem) {
            return (oldItem.getDate() != null ? oldItem.getDate().equals(newItem.getDate()) : newItem.getDate() == null) &&
                   (oldItem.getLunchStatus() != null ? oldItem.getLunchStatus().equals(newItem.getLunchStatus()) : newItem.getLunchStatus() == null) &&
                   (oldItem.getDinnerStatus() != null ? oldItem.getDinnerStatus().equals(newItem.getDinnerStatus()) : newItem.getDinnerStatus() == null);
        }
    }
}
