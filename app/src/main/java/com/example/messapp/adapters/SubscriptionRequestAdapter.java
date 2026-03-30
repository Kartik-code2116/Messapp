package com.example.messapp.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.messapp.databinding.ItemStudentRequestBinding;
import com.example.messapp.models.SubscriptionRequest;
import java.util.ArrayList;
import java.util.List;

public class SubscriptionRequestAdapter extends RecyclerView.Adapter<SubscriptionRequestAdapter.ViewHolder> {

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

    public void setRequests(List<SubscriptionRequest> requests) {
        this.requests = requests;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStudentRequestBinding binding = ItemStudentRequestBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent,
                false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SubscriptionRequest request = requests.get(position);
        holder.binding.textStudentName.setText(request.getStudentName());
        holder.binding.textStudentEmail.setText(request.getStudentEmail());

        holder.binding.textInfo.setVisibility(android.view.View.VISIBLE);
        String info = "Status: " + request.getStatus();
        if (request.getType() != null && !request.getType().equals("BOTH")) {
            info += " | Type: " + request.getType();
        }
        holder.binding.textInfo.setText(info);
        if ("PENDING".equals(request.getStatus())) {
            holder.binding.textInfo.setTextColor(android.graphics.Color.parseColor("#FFA000")); // Amber
        }

        holder.binding.btnConfirm.setText("Confirm");
        holder.binding.btnConfirm.setOnClickListener(v -> {
            if (confirmListener != null)
                confirmListener.onConfirm(request);
        });

        holder.binding.btnDelete.setVisibility(android.view.View.VISIBLE);
        holder.binding.btnDelete.setText("Delete");
        holder.binding.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null)
                deleteListener.onDelete(request);
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
