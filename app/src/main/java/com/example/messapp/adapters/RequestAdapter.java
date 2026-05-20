package com.example.messapp.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.messapp.databinding.ItemStudentRequestBinding;
import com.example.messapp.models.MealRequest;
import com.bumptech.glide.Glide;
import com.example.messapp.R;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.ViewHolder> {

    private List<MealRequest> requests = new ArrayList<>();
    private OnConfirmClickListener listener;

    public interface OnConfirmClickListener {
        void onConfirm(MealRequest request);
    }

    public void setOnConfirmClickListener(OnConfirmClickListener listener) {
        this.listener = listener;
    }

    public void setRequests(List<MealRequest> requests) {
        this.requests = requests;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStudentRequestBinding binding = ItemStudentRequestBinding.inflate(LayoutInflater.from(parent.getContext()),
                parent,
                false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MealRequest request = requests.get(position);
        holder.binding.textStudentName.setText(request.getStudentName() != null ? request.getStudentName() : "User: " + request.getUserId());
        holder.binding.textStudentEmail.setText("Extra Meal: " + request.getMealType() + " | Date: " + request.getDate());

        holder.binding.imgStudent.setImageResource(R.drawable.ic_student_profile);
        if (request.getUserId() != null) {
            final String studentId = request.getUserId();
            holder.itemView.setTag(studentId);
            FirebaseFirestore.getInstance().collection("users").document(studentId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (Objects.equals(holder.itemView.getTag(), studentId) && documentSnapshot.exists()) {
                            String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                Glide.with(holder.itemView.getContext())
                                        .load(profileImageUrl)
                                        .placeholder(R.drawable.ic_student_profile)
                                        .into(holder.binding.imgStudent);
                            }
                        }
                    });
        }

        holder.binding.textInfo.setVisibility(android.view.View.VISIBLE);
        holder.binding.textInfo.setText("Deducts 1 day from subscription.");
        holder.binding.btnConfirm.setText("Allow Extra");
        holder.binding.btnConfirm.setOnClickListener(v -> {
            if (listener != null)
                listener.onConfirm(request);
        });
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemStudentRequestBinding binding;

        public ViewHolder(ItemStudentRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
