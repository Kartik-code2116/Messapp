package com.example.messapp.ui.mess.revenue;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messapp.R;
import com.example.messapp.models.Student;
import com.example.messapp.models.Transaction;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.TransactionViewHolder> {

    private List<Transaction> transactionList;
    private Map<String, Student> studentMap;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    public TransactionsAdapter(List<Transaction> transactionList, Map<String, Student> studentMap) {
        this.transactionList = transactionList;
        this.studentMap = studentMap != null ? studentMap : new HashMap<>();
    }

    public void updateData(List<Transaction> newList) {
        this.transactionList = newList;
        notifyDataSetChanged();
    }

    public void updateStudentMap(Map<String, Student> newMap) {
        this.studentMap = newMap != null ? newMap : new HashMap<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);

        Student student = studentMap.get(transaction.getUserId());
        if (student != null) {
            holder.textStudentName.setText(student.getName() != null ? student.getName() : "Unknown");
            holder.textStudentEmail.setText(student.getEmail() != null ? student.getEmail() : transaction.getUserId());
        } else {
            holder.textStudentName.setText("Unknown Student");
            holder.textStudentEmail.setText("ID: " + transaction.getUserId());
        }

        holder.textDate.setText(dateFormat.format(new Date(transaction.getTimestamp())));
        holder.textAmount.setText("+ ₹" + String.format(Locale.getDefault(), "%.2f", transaction.getAmount()));
        holder.textDays.setText(transaction.getDaysGranted() + " Days");

        if (holder.textTypeBadge != null) {
            String type = transaction.getSubscriptionType();
            if (type == null)
                type = "BOTH";
            holder.textTypeBadge.setText(type);

            // Subtle color coding for types
            switch (type) {
                case "LUNCH":
                    holder.textTypeBadge.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FEF3C7"))); // Amber
                    holder.textTypeBadge.setTextColor(android.graphics.Color.parseColor("#D97706"));
                    break;
                case "DINNER":
                    holder.textTypeBadge.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#DBEAFE"))); // Blue
                    holder.textTypeBadge.setTextColor(android.graphics.Color.parseColor("#2563EB"));
                    break;
                default:
                    holder.textTypeBadge.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F1F5F9"))); // Slate
                    holder.textTypeBadge.setTextColor(android.graphics.Color.parseColor("#64748B"));
                    break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return transactionList != null ? transactionList.size() : 0;
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView textStudentName, textStudentEmail, textDate, textAmount, textDays, textTypeBadge;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            textStudentName = itemView.findViewById(R.id.text_trans_student_name);
            textStudentEmail = itemView.findViewById(R.id.text_trans_student_email);
            textDate = itemView.findViewById(R.id.text_trans_date);
            textAmount = itemView.findViewById(R.id.text_trans_amount);
            textDays = itemView.findViewById(R.id.text_trans_days);
            textTypeBadge = itemView.findViewById(R.id.text_trans_type_badge);
        }
    }
}
