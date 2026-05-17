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
import java.util.Objects;

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
        TextView textOneTimeExpiry;
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
            textOneTimeExpiry = itemView.findViewById(R.id.text_one_time_expiry);
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

            String lunch = student.getLunchStatus() != null ? student.getLunchStatus() : "--";
            String dinner = student.getDinnerStatus() != null ? student.getDinnerStatus() : "--";

            if ("ONE_TIME".equals(student.getSubscriptionType())) {
                if ("IN".equals(lunch)) {
                    dinner = "LOCKED";
                } else if ("IN".equals(dinner)) {
                    lunch = "LOCKED";
                } else {
                    // No explicit selection yet, check auto-select
                    String autoSelect = student.getOneTimeAutoSelect();
                    boolean isReset = "RESET".equals(lunch) || "RESET".equals(dinner);
                    if (!isReset) {
                        if ("LUNCH".equals(autoSelect)) {
                            lunch = "IN (Auto)";
                            dinner = "LOCKED";
                        } else if ("DINNER".equals(autoSelect)) {
                            dinner = "IN (Auto)";
                            lunch = "LOCKED";
                        }
                    } else {
                        if ("RESET".equals(lunch)) lunch = "--";
                        if ("RESET".equals(dinner)) dinner = "--";
                    }
                }
            } else {
                if ("RESET".equals(lunch)) lunch = "--";
                if ("RESET".equals(dinner)) dinner = "--";
            }

            textLunchStatus.setText(lunch);
            int lunchBg = R.drawable.bg_status_pill_neutral;
            if ("IN".equals(lunch) || "IN (Auto)".equals(lunch))
                lunchBg = R.drawable.bg_status_pill_success;
            else if ("OUT".equals(lunch))
                lunchBg = R.drawable.bg_status_pill_danger;
            containerLunch.setBackgroundResource(lunchBg);

            textDinnerStatus.setText(dinner);
            int dinnerBg = R.drawable.bg_status_pill_neutral;
            if ("IN".equals(dinner) || "IN (Auto)".equals(dinner))
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

            if ("ONE_TIME".equals(student.getSubscriptionType())) {
                textLunchExpiry.setVisibility(View.GONE);
                textDinnerExpiry.setVisibility(View.GONE);
                if (textOneTimeExpiry != null) textOneTimeExpiry.setVisibility(View.VISIBLE);
                
                if (student.getOneTimeMealExpiry() > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    String expiryDate = sdf.format(new Date(student.getOneTimeMealExpiry()));
                    if (textOneTimeExpiry != null) textOneTimeExpiry.setText("One Time: " + expiryDate);
                } else {
                    if (textOneTimeExpiry != null) textOneTimeExpiry.setText("One Time: No Sub");
                }
            } else {
                textLunchExpiry.setVisibility(View.VISIBLE);
                textDinnerExpiry.setVisibility(View.VISIBLE);
                if (textOneTimeExpiry != null) textOneTimeExpiry.setVisibility(View.GONE);

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
            }

            // Show days remaining for quick admin visibility
            long now = System.currentTimeMillis();
            long lunchExp = student.getLunchSubscriptionExpiry() > 0
                    ? student.getLunchSubscriptionExpiry() : student.getSubscriptionExpiry();
            long dinnerExp = student.getDinnerSubscriptionExpiry() > 0
                    ? student.getDinnerSubscriptionExpiry() : student.getSubscriptionExpiry();
            long oneTimeExp = student.getOneTimeMealExpiry() > 0
                    ? student.getOneTimeMealExpiry() : student.getSubscriptionExpiry();
                    
            long maxExp;
            if ("ONE_TIME".equals(student.getSubscriptionType())) {
                maxExp = oneTimeExp;
            } else {
                maxExp = Math.max(lunchExp, dinnerExp);
            }
            if (maxExp > now) {
                long daysLeft = (maxExp - now) / (1000 * 60 * 60 * 24);
                textSubscriptionExpiry.setText(daysLeft + "d remaining");
            } else if (maxExp > 0) {
                textSubscriptionExpiry.setText("Expired");
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
            // BUG FIX: use Objects.equals() to avoid NullPointerException when any field is null
            return Objects.equals(oldItem.getName(), newItem.getName()) &&
                   Objects.equals(oldItem.getEmail(), newItem.getEmail()) &&
                   Objects.equals(oldItem.getLunchStatus(), newItem.getLunchStatus()) &&
                   Objects.equals(oldItem.getDinnerStatus(), newItem.getDinnerStatus()) &&
                   Objects.equals(oldItem.getSubscriptionType(), newItem.getSubscriptionType()) &&
                   oldItem.getLunchSubscriptionExpiry() == newItem.getLunchSubscriptionExpiry() &&
                   oldItem.getDinnerSubscriptionExpiry() == newItem.getDinnerSubscriptionExpiry() &&
                   oldItem.getOneTimeMealExpiry() == newItem.getOneTimeMealExpiry();
        }
    }
}
