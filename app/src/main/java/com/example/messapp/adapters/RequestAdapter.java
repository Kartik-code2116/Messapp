package com.example.messapp.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.messapp.databinding.ItemStudentRequestBinding;
import com.example.messapp.models.MealRequest;
import java.util.ArrayList;
import java.util.List;

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
        holder.binding.textStudentName.setText("User: " + request.getUserId());
        holder.binding.textStudentEmail.setText("Type: " + request.getMealType());
        holder.binding.textInfo.setVisibility(android.view.View.GONE);
        holder.binding.btnConfirm.setText("Confirm");
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
