package com.example.messapp.managers;

import com.example.messapp.models.Menu;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MenuManager handles menu management for mess owners.
 *
 * FIX #7 — Document ID format was "MENU{dayOfWeek}_{messId}" which never matched
 * the format "{messId}_{date}" used in UserHomeFragment.loadMenu().  Both now use
 * the same format:  "{messId}_{date}"  where date is "yyyy-MM-dd".
 *
 * If you previously stored menus with the old format (MEMONday_MESSXXX), those
 * documents will need to be migrated or re-saved using this updated manager.
 */
public class MenuManager {
    private static final String COLLECTION_MENUS = "menus";
    private static final String FIELD_MESS_ID    = "messId";
    private static final String FIELD_DATE       = "date";
    private static final String FIELD_LUNCH      = "lunch";
    private static final String FIELD_DINNER     = "dinner";
    private static final String FIELD_ITEMS      = "items";
    private static final String FIELD_AVAILABLE  = "available";

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

    /** Shared document ID builder — must be used everywhere to guarantee consistency. */
    private String getMenuDocumentId(String messId, String date) {
        // FIX #7: old format was "MENU{dayOfWeek}_{messId}" — now "{messId}_{date}"
        return messId + "_" + date;
    }

    /** Convenience: get today's date string in the format used by document IDs. */
    public static String todayDateString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    /** Create or update the menu for a specific date. */
    public void createOrUpdateMenu(String messId, String date, String lunch, String dinner,
                                   List<String> items, boolean available, UpdateCallback callback) {
        String menuId = getMenuDocumentId(messId, date);

        Map<String, Object> menuData = new HashMap<>();
        menuData.put(FIELD_MESS_ID,   messId);
        menuData.put(FIELD_DATE,      date);
        menuData.put(FIELD_LUNCH,     lunch);
        menuData.put(FIELD_DINNER,    dinner);
        menuData.put(FIELD_ITEMS,     items);
        menuData.put(FIELD_AVAILABLE, available);

        db.collection(COLLECTION_MENUS).document(menuId).set(menuData)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /** Get the menu for a specific date and mess. */
    public void getMenuForDate(String messId, String date, MenuCallback callback) {
        String menuId = getMenuDocumentId(messId, date);

        db.collection(COLLECTION_MENUS).document(menuId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Menu menu = doc.toObject(Menu.class);
                        if (menu != null) callback.onSuccess(menu);
                        else callback.onFailure("Failed to parse menu data");
                    } else {
                        callback.onFailure("Menu not found for this date");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /** Get all menus stored for a mess. */
    public void getAllMenus(String messId, MenuListCallback callback) {
        db.collection(COLLECTION_MENUS)
                .whereEqualTo(FIELD_MESS_ID, messId)
                .get()
                .addOnSuccessListener(query -> callback.onSuccess(query.toObjects(Menu.class)))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /** Update the availability flag for a specific date's menu. */
    public void updateMenuAvailability(String messId, String date, boolean available, UpdateCallback callback) {
        String menuId = getMenuDocumentId(messId, date);
        db.collection(COLLECTION_MENUS).document(menuId).update(FIELD_AVAILABLE, available)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /** Delete the menu for a specific date. */
    public void deleteMenu(String messId, String date, UpdateCallback callback) {
        String menuId = getMenuDocumentId(messId, date);
        db.collection(COLLECTION_MENUS).document(menuId).delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /** Append extra items to an existing menu's item list. */
    public void addMealItems(String messId, String date, List<String> newItems, UpdateCallback callback) {
        String menuId = getMenuDocumentId(messId, date);
        db.collection(COLLECTION_MENUS).document(menuId)
                .update(FIELD_ITEMS, FieldValue.arrayUnion(newItems.toArray()))
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
