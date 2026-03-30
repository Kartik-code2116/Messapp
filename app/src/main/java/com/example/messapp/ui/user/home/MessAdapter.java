package com.example.messapp.ui.user.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messapp.Mess;
import com.example.messapp.R;

public class MessAdapter extends ListAdapter<Mess, MessAdapter.MessViewHolder> {

    private OnMessClickListener listener;

    public interface OnMessClickListener {
        void onMessClick(Mess mess);
        void onViewDetailsClick(Mess mess);
    }

    public MessAdapter(OnMessClickListener listener) {
        super(new MessDiffCallback());
        this.listener = listener;
    }

    @NonNull
    @Override
    public MessViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mess_card, parent, false);
        return new MessViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessViewHolder holder, int position) {
        Mess mess = getItem(position);
        if (mess != null) {
            holder.bind(mess, listener);
        }
    }

    static class MessViewHolder extends RecyclerView.ViewHolder {
        TextView messName;
        TextView messLocation;
        TextView messStudents;
        Button viewDetailsButton;

        public MessViewHolder(@NonNull View itemView) {
            super(itemView);
            messName = itemView.findViewById(R.id.text_mess_card_name);
            messLocation = itemView.findViewById(R.id.text_mess_card_location);
            messStudents = itemView.findViewById(R.id.text_mess_card_students);
        }

        void bind(Mess mess, OnMessClickListener listener) {
            messName.setText(mess.getName() != null ? mess.getName() : "Unknown Mess");
            messLocation.setText(mess.getLocation() != null ? mess.getLocation() : "Location not available");
            messStudents.setText("Total Students: " + mess.getStudentCount());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMessClick(mess);
                }
            });
            
            viewDetailsButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewDetailsClick(mess);
                }
            });
        }
    }

    private static class MessDiffCallback extends DiffUtil.ItemCallback<Mess> {
        @Override
        public boolean areItemsTheSame(@NonNull Mess oldItem, @NonNull Mess newItem) {
            return oldItem.getMessId() != null && oldItem.getMessId().equals(newItem.getMessId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Mess oldItem, @NonNull Mess newItem) {
            return oldItem.getName() != null && oldItem.getName().equals(newItem.getName()) &&
                   oldItem.getLocation() != null && oldItem.getLocation().equals(newItem.getLocation()) &&
                   oldItem.getStudentCount() == newItem.getStudentCount();
        }
    }
}
