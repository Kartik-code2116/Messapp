package com.example.messapp.models;

public class MealRequest {
    private String id; // Document ID
    private String userId;
    private String studentName;
    private String messId;
    private String date;
    private String mealType; // LUNCH or DINNER

    public MealRequest() {
        // Required for Firebase
    }

    public MealRequest(String id, String userId, String studentName, String messId, String date, String mealType) {
        this.id = id;
        this.userId = userId;
        this.studentName = studentName;
        this.messId = messId;
        this.date = date;
        this.mealType = mealType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getMessId() {
        return messId;
    }

    public void setMessId(String messId) {
        this.messId = messId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }
}
