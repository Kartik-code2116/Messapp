package com.example.messapp.ui.mess.students;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.messapp.R;
import com.example.messapp.models.Student;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StudentsAdapter extends ListAdapter<Student, StudentsAdapter.StudentViewHolder> {

    public StudentsAdapter() {
        super(new StudentDiffCallback());
    }

    private OnManageClickListener manageListener;

    public interface OnManageClickListener {
        void onManageClick(Student student);

        void onDeleteClick(Student student);

        void onResetClick(Student student);
    }

    public void setOnManageClickListener(OnManageClickListener listener) {
        this.manageListener = listener;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student student = getItem(position);
        if (student != null) {
            holder.bind(student, manageListener);
        }
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView textStudentName;
        TextView textStudentEmail;
        TextView textLunchStatus;
        TextView textDinnerStatus;
        TextView textLunchExpiry;
        TextView textDinnerExpiry;
        TextView textSubscriptionExpiry;
        android.widget.ImageView imgStudent;
        android.view.View containerLunch;
        android.view.View containerDinner;
        android.widget.ImageButton btnRenew;
        android.widget.ImageButton btnDelete;
        com.google.android.material.button.MaterialButton btnResetStatus;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            textStudentName = itemView.findViewById(R.id.text_student_name);
            textStudentEmail = itemView.findViewById(R.id.text_student_email);
            textLunchStatus = itemView.findViewById(R.id.text_lunch_status);
            textDinnerStatus = itemView.findViewById(R.id.text_dinner_status);
            textLunchExpiry = itemView.findViewById(R.id.text_lunch_expiry);
            textDinnerExpiry = itemView.findViewById(R.id.text_dinner_expiry);
            textSubscriptionExpiry = itemView.findViewById(R.id.text_subscription_expiry);
            imgStudent = itemView.findViewById(R.id.img_student);
            containerLunch = itemView.findViewById(R.id.container_lunch);
            containerDinner = itemView.findViewById(R.id.container_dinner);
            btnRenew = itemView.findViewById(R.id.btn_grant_subscription);
            btnDelete = itemView.findViewById(R.id.btn_delete_subscription);
            btnResetStatus = itemView.findViewById(R.id.btn_reset_status);
        }

        public void bind(Student student, OnManageClickListener listener) {
            textStudentName.setText(student.getName() != null ? student.getName() : "Anonymous Student");
            textStudentEmail.setText(student.getEmail());

            // Bind Lunch Status
            String lunch = student.getLunchStatus() != null ? student.getLunchStatus() : "--";
            textLunchStatus.setText(lunch);
            int lunchBg = R.drawable.bg_status_pill_neutral;
            if ("IN".equals(lunch))
                lunchBg = R.drawable.bg_status_pill_success;
            else if ("OUT".equals(lunch))
                lunchBg = R.drawable.bg_status_pill_danger;
            containerLunch.setBackgroundResource(lunchBg);

            // Bind Dinner Status
            String dinner = student.getDinnerStatus() != null ? student.getDinnerStatus() : "--";
            textDinnerStatus.setText(dinner);
            int dinnerBg = R.drawable.bg_status_pill_neutral;
            if ("IN".equals(dinner))
                dinnerBg = R.drawable.bg_status_pill_success;
            else if ("OUT".equals(dinner))
                dinnerBg = R.drawable.bg_status_pill_danger;
            containerDinner.setBackgroundResource(dinnerBg);

            if (student.getProfileImageUrl() != null && !student.getProfileImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(student.getProfileImageUrl())
                        .placeholder(R.drawable.ic_person_black_24dp)
                        .circleCrop()
                        .into(imgStudent);
            } else {
                imgStudent.setImageResource(R.drawable.ic_person_black_24dp);
            }

            if (student.getLunchSubscriptionExpiry() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                String expiryDate = sdf.format(new Date(student.getLunchSubscriptionExpiry()));
                textLunchExpiry.setText("Lunch: " + expiryDate);
            } else {
                textLunchExpiry.setText("Lunch: No Sub");
            }

            if (student.getDinnerSubscriptionExpiry() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                String expiryDate = sdf.format(new Date(student.getDinnerSubscriptionExpiry()));
                textDinnerExpiry.setText("Dinner: " + expiryDate);
            } else {
                textDinnerExpiry.setText("Dinner: No Sub");
            }

            // Keep legacy field hidden logic or updated for compatibility
            if (student.getSubscriptionExpiry() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                String expiryDate = sdf.format(new Date(student.getSubscriptionExpiry()));
                textSubscriptionExpiry.setText("Expires: " + expiryDate);
            } else {
                textSubscriptionExpiry.setText("No Subscription");
            }

            btnRenew.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onManageClick(student);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(student);
                }
            });

            btnResetStatus.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onResetClick(student);
                }
            });
        }
    }

    private static class StudentDiffCallback extends DiffUtil.ItemCallback<Student> {
        @Override
        public boolean areItemsTheSame(@NonNull Student oldItem, @NonNull Student newItem) {
            return oldItem.getUserId() != null && oldItem.getUserId().equals(newItem.getUserId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Student oldItem, @NonNull Student newItem) {
            return oldItem.getName() != null && oldItem.getName().equals(newItem.getName()) &&
                   oldItem.getEmail() != null && oldItem.getEmail().equals(newItem.getEmail()) &&
                   oldItem.getLunchStatus() != null && oldItem.getLunchStatus().equals(newItem.getLunchStatus()) &&
                   oldItem.getDinnerStatus() != null && oldItem.getDinnerStatus().equals(newItem.getDinnerStatus()) &&
                   oldItem.getLunchSubscriptionExpiry() == newItem.getLunchSubscriptionExpiry() &&
                   oldItem.getDinnerSubscriptionExpiry() == newItem.getDinnerSubscriptionExpiry();
        }
    }
}
