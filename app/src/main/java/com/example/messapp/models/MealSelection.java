package com.example.messapp.models;

public class MealSelection {
    private String userId;
    private String date;
    private String lunchStatus;
    private String dinnerStatus;

    public MealSelection() {
        // Required no-argument constructor for Firebase
    }

    public MealSelection(String userId, String date, String lunchStatus, String dinnerStatus) {
        this.userId = userId;
        this.date = date;
        this.lunchStatus = lunchStatus;
        this.dinnerStatus = dinnerStatus;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getLunchStatus() {
        return lunchStatus;
    }

    public void setLunchStatus(String lunchStatus) {
        this.lunchStatus = lunchStatus;
    }

    public String getDinnerStatus() {
        return dinnerStatus;
    }

    public void setDinnerStatus(String dinnerStatus) {
        this.dinnerStatus = dinnerStatus;
    }
}
