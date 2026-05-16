package com.example.messapp.ui.user.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messapp.R;
import com.example.messapp.models.MealSelection;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HistoryAdapter extends ListAdapter<MealSelection, HistoryAdapter.HistoryViewHolder> {

    private static final SimpleDateFormat INPUT_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat DISPLAY_FORMAT =
            new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault());

    public HistoryAdapter() {
        super(new MealSelectionDiffCallback());
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
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
        private final TextView textHistoryDate;
        private final TextView textLunchStatus;
        private final TextView textDinnerStatus;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textHistoryDate = itemView.findViewById(R.id.text_history_date);
            textLunchStatus = itemView.findViewById(R.id.text_lunch_status);
            textDinnerStatus = itemView.findViewById(R.id.text_dinner_status);
        }

        void bind(MealSelection mealSelection) {
            textHistoryDate.setText(formatDate(mealSelection.getDate()));
            bindMealStatus(textLunchStatus, "Lunch", mealSelection.getLunchStatus());
            bindMealStatus(textDinnerStatus, "Dinner", mealSelection.getDinnerStatus());
        }

        private void bindMealStatus(TextView view, String mealLabel, String status) {
            String displayStatus = status != null ? status : "Not marked";
            view.setText(mealLabel + ": " + displayStatus);

            int colorRes;
            if ("IN".equalsIgnoreCase(displayStatus)) {
                colorRes = R.color.state_success;
            } else if ("OUT".equalsIgnoreCase(displayStatus)) {
                colorRes = R.color.state_error;
            } else {
                colorRes = R.color.text_caption;
            }
            view.setTextColor(ContextCompat.getColor(view.getContext(), colorRes));
        }

        private String formatDate(String rawDate) {
            if (rawDate == null || rawDate.isEmpty()) {
                return "Unknown date";
            }
            try {
                Date date = INPUT_FORMAT.parse(rawDate);
                if (date != null) {
                    return DISPLAY_FORMAT.format(date);
                }
            } catch (ParseException ignored) {
                // fall through
            }
            return rawDate;
        }
    }

    private static class MealSelectionDiffCallback extends DiffUtil.ItemCallback<MealSelection> {
        @Override
        public boolean areItemsTheSame(@NonNull MealSelection oldItem, @NonNull MealSelection newItem) {
            String oldId = safe(oldItem.getUserId()) + safe(oldItem.getDate());
            String newId = safe(newItem.getUserId()) + safe(newItem.getDate());
            return oldId.equals(newId);
        }

        @Override
        public boolean areContentsTheSame(@NonNull MealSelection oldItem, @NonNull MealSelection newItem) {
            return safe(oldItem.getDate()).equals(safe(newItem.getDate()))
                    && safe(oldItem.getLunchStatus()).equals(safe(newItem.getLunchStatus()))
                    && safe(oldItem.getDinnerStatus()).equals(safe(newItem.getDinnerStatus()));
        }

        private static String safe(String value) {
            return value != null ? value : "";
        }
    }
}
