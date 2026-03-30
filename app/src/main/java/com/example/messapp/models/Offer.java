package com.example.messapp.models;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

@IgnoreExtraProperties
public class Offer {
    private String offerId;
    private String messId;
    private String title;
    private String description;
    private double discountPercentage;
    @ServerTimestamp
    private Date expiryDate;
    private boolean active;
    private long usageCount;

    public Offer() {
        // Public no-argument constructor required for Firestore
    }

    public Offer(String offerId, String messId, String title, String description, double discountPercentage,
            Date expiryDate) {
        this.offerId = offerId;
        this.messId = messId;
        this.title = title;
        this.description = description;
        this.discountPercentage = discountPercentage;
        this.expiryDate = expiryDate;
    }

    // Getters
    public String getOfferId() {
        return offerId;
    }

    public String getMessId() {
        return messId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public double getDiscountPercentage() {
        return discountPercentage;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public boolean isActive() {
        return active;
    }

    public long getUsageCount() {
        return usageCount;
    }

    // Setters
    public void setOfferId(String offerId) {
        this.offerId = offerId;
    }

    public void setMessId(String messId) {
        this.messId = messId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDiscountPercentage(double discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setUsageCount(long usageCount) {
        this.usageCount = usageCount;
    }
}
