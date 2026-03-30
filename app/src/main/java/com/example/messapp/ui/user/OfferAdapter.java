package com.example.messapp.ui.user;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messapp.R;
import com.example.messapp.models.Offer;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OfferAdapter extends RecyclerView.Adapter<OfferAdapter.OfferViewHolder> {

    private List<Offer> offerList;
    private boolean showDeleteButton;

    public OfferAdapter(List<Offer> offerList, boolean showDeleteButton) {
        this.offerList = offerList;
        this.showDeleteButton = showDeleteButton;
    }

    public void setOfferList(List<Offer> offerList) {
        this.offerList = offerList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OfferViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_offer, parent, false);
        return new OfferViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OfferViewHolder holder, int position) {
        Offer offer = offerList.get(position);
        holder.textOfferTitle.setText(offer.getTitle());
        holder.textOfferDescription.setText(offer.getDescription());

        Context context = holder.itemView.getContext();
        holder.textOfferDiscount
                .setText(context.getString(R.string.offer_discount_format, offer.getDiscountPercentage()));

        if (offer.getExpiryDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            holder.textOfferExpiry
                    .setText(context.getString(R.string.offer_expiry_format, sdf.format(offer.getExpiryDate())));
        } else {
            holder.textOfferExpiry.setText(context.getString(R.string.offer_expiry_not_available));
        }

        if (showDeleteButton) {
            holder.ivDeleteOffer.setVisibility(View.VISIBLE);
            holder.ivDeleteOffer.setOnClickListener(v -> {
                deleteOffer(offer.getOfferId(), position);
            });
        } else {
            holder.ivDeleteOffer.setVisibility(View.GONE);
        }
    }

    private void deleteOffer(String offerId, int position) {
        FirebaseFirestore.getInstance().collection("offers").document(offerId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    offerList.remove(position);
                    notifyItemRemoved(position);
                });
    }

    @Override
    public int getItemCount() {
        return offerList.size();
    }

    static class OfferViewHolder extends RecyclerView.ViewHolder {
        TextView textOfferTitle;
        TextView textOfferDescription;
        TextView textOfferDiscount;
        TextView textOfferExpiry;
        ImageView ivDeleteOffer;

        public OfferViewHolder(@NonNull View itemView) {
            super(itemView);
            textOfferTitle = itemView.findViewById(R.id.text_offer_title);
            textOfferDescription = itemView.findViewById(R.id.text_offer_description);
            textOfferDiscount = itemView.findViewById(R.id.text_offer_discount);
            textOfferExpiry = itemView.findViewById(R.id.text_offer_expiry);
            ivDeleteOffer = itemView.findViewById(R.id.iv_delete_offer);
        }
    }
}
