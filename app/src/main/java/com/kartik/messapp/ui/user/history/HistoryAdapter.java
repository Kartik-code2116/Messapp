package com.kartik.messapp.ui.user.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.kartik.messapp.R;
import com.kartik.messapp.models.MealSelection;

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
            private final View layoutLunchPill;
            private final View layoutDinnerPill;
            private final android.widget.ImageView iconLunch;
            private final android.widget.ImageView iconDinner;

            HistoryViewHolder(@NonNull View itemView) {
                super(itemView);
                textHistoryDate = itemView.findViewById(R.id.text_history_date);
                textLunchStatus = itemView.findViewById(R.id.text_lunch_status);
                textDinnerStatus = itemView.findViewById(R.id.text_dinner_status);
                layoutLunchPill = itemView.findViewById(R.id.layout_lunch_pill);
                layoutDinnerPill = itemView.findViewById(R.id.layout_dinner_pill);
                iconLunch = itemView.findViewById(R.id.icon_lunch);
                iconDinner = itemView.findViewById(R.id.icon_dinner);
            }

            void bind(MealSelection mealSelection) {
                textHistoryDate.setText(formatDate(mealSelection.getDate()));
                bindMealStatus(textLunchStatus, layoutLunchPill, iconLunch, "Lunch", mealSelection.getLunchStatus());
                bindMealStatus(textDinnerStatus, layoutDinnerPill, iconDinner, "Dinner", mealSelection.getDinnerStatus());
            }

            private void bindMealStatus(TextView textView, View layoutPill, android.widget.ImageView iconView, String mealLabel, String status) {
                String displayStatus = status != null ? status : "Not marked";
                if ("RESET".equalsIgnoreCase(displayStatus)) {
                    displayStatus = "Not marked";
                }
                
                // Format presentation status
                String presentationStatus = displayStatus;
                if ("Auto-IN".equalsIgnoreCase(displayStatus) || "Auto IN".equalsIgnoreCase(displayStatus)) {
                    presentationStatus = "Auto-selected IN";
                }
                
                textView.setText(mealLabel + ": " + presentationStatus);

                int textColorRes;
                int bgColorRes;

                if ("IN".equalsIgnoreCase(displayStatus) 
                        || "Auto-IN".equalsIgnoreCase(displayStatus)
                        || "Auto-selected IN".equalsIgnoreCase(displayStatus)
                        || "Auto IN".equalsIgnoreCase(displayStatus)) {
                    textColorRes = R.color.state_success;
                    bgColorRes = R.color.semantic_success_bg;
                } else if ("OUT".equalsIgnoreCase(displayStatus)) {
                    textColorRes = R.color.state_error;
                    bgColorRes = R.color.ios_danger_light;
                } else {
                    textColorRes = R.color.text_caption;
                    bgColorRes = R.color.neutral_50;
                }

                int colorVal = ContextCompat.getColor(textView.getContext(), textColorRes);
                int bgColorVal = ContextCompat.getColor(textView.getContext(), bgColorRes);
                
                textView.setTextColor(colorVal);
                layoutPill.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgColorVal));
                if (iconView != null) {
                    iconView.setImageTintList(android.content.res.ColorStateList.valueOf(colorVal));
                }
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
