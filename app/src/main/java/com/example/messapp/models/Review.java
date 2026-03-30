package com.example.messapp.models;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

@IgnoreExtraProperties
public class Review {
    private String reviewId;
    private String messId;
    private String userId;
    private String userName;
    private float rating;
    private String comment;
    @ServerTimestamp
    private Date timestamp;
    private int likes;

    public Review() {
        // Public no-argument constructor required for Firestore
    }

    public Review(String reviewId, String messId, String userId, String userName, float rating, String comment,
            Date timestamp) {
        this.reviewId = reviewId;
        this.messId = messId;
        this.userId = userId;
        this.userName = userName;
        this.rating = rating;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    // Getters
    public String getReviewId() {
        return reviewId;
    }

    public String getMessId() {
        return messId;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public float getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public int getLikes() {
        return likes;
    }

    // Setters
    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    public void setMessId(String messId) {
        this.messId = messId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }
}
