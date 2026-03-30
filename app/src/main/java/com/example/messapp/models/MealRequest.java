package com.example.messapp.models;

public class MealRequest {
    private String id; // Document ID
    private String userId;
    private String mealType; // LUNCH or DINNER

    public MealRequest(String id, String userId, String mealType) {
        this.id = id;
        this.userId = userId;
        this.mealType = mealType;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getMealType() {
        return mealType;
    }
}
