package com.example.messapp.managers;

import com.example.messapp.models.Menu;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MenuManager handles menu management for mess owners including:
 * - Creating and updating menus for each day
 * - Managing meal items
 * - Retrieving menus for users
 * - Updating menu availability
 */
public class MenuManager {
    private static final String COLLECTION_MENUS = "menus";
    private static final String FIELD_MENU_ID = "menuId";
    private static final String FIELD_MESS_ID = "messId";
    private static final String FIELD_DAY_OF_WEEK = "dayOfWeek";
    private static final String FIELD_MEALS = "meals";
    private static final String FIELD_ITEMS = "items";
    private static final String FIELD_AVAILABLE = "available";

    private final FirebaseFirestore db;

    public interface MenuCallback {
        void onSuccess(Menu menu);
        void onFailure(String errorMessage);
    }

    public interface MenuListCallback {
        void onSuccess(List<Menu> menus);
        void onFailure(String errorMessage);
    }

    public interface UpdateCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public MenuManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    private String getMenuDocumentId(String messId, String dayOfWeek) {
        return "MENU" + dayOfWeek + "_" + messId;
    }

    /**
     * Create or update menu for a specific day
     */
    public void createOrUpdateMenu(String messId, String dayOfWeek, List<String> meals, List<String> items, boolean available, UpdateCallback callback) {
        String menuId = getMenuDocumentId(messId, dayOfWeek);

        Map<String, Object> menuData = new HashMap<>();
        menuData.put(FIELD_MENU_ID, menuId);
        menuData.put(FIELD_MESS_ID, messId);
        menuData.put(FIELD_DAY_OF_WEEK, dayOfWeek);
        menuData.put(FIELD_MEALS, meals);
        menuData.put(FIELD_ITEMS, items);
        menuData.put(FIELD_AVAILABLE, available);

        db.collection(COLLECTION_MENUS).document(menuId).set(menuData)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get menu for a specific day and mess
     */
    public void getMenuForDay(String messId, String dayOfWeek, MenuCallback callback) {
        String menuId = getMenuDocumentId(messId, dayOfWeek);

        db.collection(COLLECTION_MENUS).document(menuId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Menu menu = documentSnapshot.toObject(Menu.class);
                        if (menu != null) {
                            callback.onSuccess(menu);
                        } else {
                            callback.onFailure("Failed to parse menu data");
                        }
                    } else {
                        callback.onFailure("Menu not found for this day");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get all menus for a mess
     */
    public void getWeeklyMenu(String messId, MenuListCallback callback) {
        db.collection(COLLECTION_MENUS)
                .whereEqualTo(FIELD_MESS_ID, messId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Menu> menus = querySnapshot.toObjects(Menu.class);
                    callback.onSuccess(menus);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Update menu availability
     */
    public void updateMenuAvailability(String messId, String dayOfWeek, boolean available, UpdateCallback callback) {
        String menuId = getMenuDocumentId(messId, dayOfWeek);

        db.collection(COLLECTION_MENUS).document(menuId).update(FIELD_AVAILABLE, available)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Delete menu for a day
     */
    public void deleteMenu(String messId, String dayOfWeek, UpdateCallback callback) {
        String menuId = getMenuDocumentId(messId, dayOfWeek);

        db.collection(COLLECTION_MENUS).document(menuId).delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Add meal items to menu
     */
    public void addMealItems(String messId, String dayOfWeek, List<String> newItems, UpdateCallback callback) {
        String menuId = getMenuDocumentId(messId, dayOfWeek);

        db.collection(COLLECTION_MENUS).document(menuId).update(FIELD_ITEMS, FieldValue.arrayUnion(newItems.toArray()))
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
