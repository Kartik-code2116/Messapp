package com.example.messapp.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;

public class Menu {
    private String menuId;
    private String messId;
    private String dayOfWeek; // "MONDAY", "TUESDAY", etc.
    private List<String> meals; // e.g., ["Breakfast", "Lunch", "Dinner"]
    private List<String> items; // e.g., ["Rice", "Chicken", "Vegetables"]
    private boolean available;
    @ServerTimestamp
    private Date createdAt;
    @ServerTimestamp
    private Date updatedAt;

    public Menu() {
        // Required no-argument constructor for Firebase
    }

    public Menu(String menuId, String messId, String dayOfWeek, List<String> meals, List<String> items, boolean available) {
        this.menuId = menuId;
        this.messId = messId;
        this.dayOfWeek = dayOfWeek;
        this.meals = meals;
        this.items = items;
        this.available = available;
    }

    public String getMenuId() {
        return menuId;
    }

    public void setMenuId(String menuId) {
        this.menuId = menuId;
    }

    public String getMessId() {
        return messId;
    }

    public void setMessId(String messId) {
        this.messId = messId;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public List<String> getMeals() {
        return meals;
    }

    public void setMeals(List<String> meals) {
        this.meals = meals;
    }

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
