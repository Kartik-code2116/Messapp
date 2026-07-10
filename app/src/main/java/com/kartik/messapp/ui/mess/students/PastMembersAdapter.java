package com.kartik.messapp.ui.mess.students;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kartik.messapp.R;
import com.kartik.messapp.models.PastMember;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PastMembersAdapter extends RecyclerView.Adapter<PastMembersAdapter.ViewHolder> {

    private List<PastMember> pastMembers = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(PastMember member);
    }

    public PastMembersAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<PastMember> list) {
        this.pastMembers = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_past_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PastMember member = pastMembers.get(position);
        holder.bind(member, listener);
    }

    @Override
    public int getItemCount() {
        return pastMembers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textName, textEmail, textPhone, textLeftAt;

        ViewHolder(View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_member_name);
            textEmail = itemView.findViewById(R.id.text_member_email);
            textPhone = itemView.findViewById(R.id.text_member_phone);
            textLeftAt = itemView.findViewById(R.id.text_left_at);
        }

        void bind(PastMember member, OnItemClickListener listener) {
            textName.setText(member.getName() != null ? member.getName() : "Unknown");
            textEmail.setText(member.getEmail() != null ? member.getEmail() : "No email");
            textPhone.setText(member.getPhone() != null && !member.getPhone().isEmpty() ? member.getPhone() : "No phone");

            if (member.getLeftAt() > 0) {
                String dateStr = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date(member.getLeftAt()));
                textLeftAt.setText("Left on " + dateStr);
            } else {
                textLeftAt.setText("Left recently");
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(member);
                }
            });
        }
    }
}
