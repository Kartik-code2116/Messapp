package com.example.messapp.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messapp.databinding.ItemStudentRequestBinding;
import com.example.messapp.models.SubscriptionRequest;
import com.bumptech.glide.Glide;
import com.example.messapp.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SubscriptionRequestAdapter
        extends RecyclerView.Adapter<SubscriptionRequestAdapter.ViewHolder> {

    private List<SubscriptionRequest> requests = new ArrayList<>();
    private OnConfirmClickListener confirmListener;
    private OnDeleteClickListener deleteListener;

    public interface OnConfirmClickListener {
        void onConfirm(SubscriptionRequest request);
    }

    public interface OnDeleteClickListener {
        void onDelete(SubscriptionRequest request);
    }

    public void setOnConfirmClickListener(OnConfirmClickListener listener) {
        this.confirmListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }

    public void setRequests(List<SubscriptionRequest> newRequests) {
        final List<SubscriptionRequest> oldList = this.requests;
        final List<SubscriptionRequest> newList =
                newRequests != null ? newRequests : new ArrayList<>();

        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new RequestDiffCallback(oldList, newList));

        this.requests = newList;
        result.dispatchUpdatesTo(this);
    }

    private static class RequestDiffCallback extends DiffUtil.Callback {
        private final List<SubscriptionRequest> oldList;
        private final List<SubscriptionRequest> newList;

        public RequestDiffCallback(List<SubscriptionRequest> oldList, List<SubscriptionRequest> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override public int getOldListSize() { return oldList.size(); }
        @Override public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            String oldId = oldList.get(oldPos).getId();
            String newId = newList.get(newPos).getId();
            return Objects.equals(oldId, newId);
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            SubscriptionRequest o = oldList.get(oldPos);
            SubscriptionRequest n = newList.get(newPos);
            return Objects.equals(o.getStatus(), n.getStatus())
                    && Objects.equals(o.getStudentName(), n.getStudentName())
                    && Objects.equals(o.getType(), n.getType());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStudentRequestBinding binding = ItemStudentRequestBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SubscriptionRequest request = requests.get(position);

        holder.binding.textStudentName.setText(request.getStudentName());
        holder.binding.textStudentEmail.setText(request.getStudentEmail());

        holder.binding.imgStudent.setImageResource(R.drawable.ic_student_profile);
        if (request.getStudentId() != null) {
            final String studentId = request.getStudentId();
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
        String info = "Status: " + request.getStatus();
        if (request.getType() != null && !request.getType().equals("BOTH")) {
            info += " | Type: " + request.getType();
        }
        holder.binding.textInfo.setText(info);
        if ("PENDING".equals(request.getStatus())) {
            holder.binding.textInfo.setTextColor(android.graphics.Color.parseColor("#FFA000"));
        }

        holder.binding.btnConfirm.setText("Confirm");
        holder.binding.btnConfirm.setOnClickListener(v -> {
            if (confirmListener != null) confirmListener.onConfirm(request);
        });

        holder.binding.btnDelete.setVisibility(android.view.View.VISIBLE);
        holder.binding.btnDelete.setText("Delete");
        holder.binding.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDelete(request);
        });
    }

    @Override
    public int getItemCount() { return requests.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemStudentRequestBinding binding;
        public ViewHolder(ItemStudentRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
