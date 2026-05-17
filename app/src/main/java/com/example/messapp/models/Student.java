package com.example.messapp.models;

public class Student {
    private String userId;
    private String email;
    private String name;
    private String lunchStatus;
    private String dinnerStatus;
    private long subscriptionExpiry;
    private long lunchSubscriptionExpiry;
    private long dinnerSubscriptionExpiry;
    private String phone;
    private String gender;
    private String dietaryPreference; // e.g., "Veg", "Non-Veg"
    private String profileImageUrl;
    // "LUNCH", "DINNER", "BOTH", "ONE_TIME" — set by admin when granting subscription
    private String subscriptionType;
    // For ONE_TIME subscriptions: shared expiry used for both lunch and dinner slots
    private long oneTimeMealExpiry;
    // For ONE_TIME subscriptions: auto-select preference ("LUNCH", "DINNER", "NONE")
    private String oneTimeAutoSelect;

    public Student() {
        // Required no-argument constructor for Firebase
    }

    public Student(String userId, String email, long subscriptionExpiry) {
        this.userId = userId;
        this.email = email;
        this.subscriptionExpiry = subscriptionExpiry;
    }

    public Student(String userId, String email, String name, String lunchStatus, String dinnerStatus,
            long subscriptionExpiry) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.lunchStatus = lunchStatus;
        this.dinnerStatus = dinnerStatus;
        this.subscriptionExpiry = subscriptionExpiry;
    }

    public Student(String userId, String email, String name, String lunchStatus, String dinnerStatus,
            long subscriptionExpiry, long lunchSubscriptionExpiry, long dinnerSubscriptionExpiry, String phone,
            String gender, String dietaryPreference, String profileImageUrl) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.lunchStatus = lunchStatus;
        this.dinnerStatus = dinnerStatus;
        this.subscriptionExpiry = subscriptionExpiry;
        this.lunchSubscriptionExpiry = lunchSubscriptionExpiry;
        this.dinnerSubscriptionExpiry = dinnerSubscriptionExpiry;
        this.phone = phone;
        this.gender = gender;
        this.dietaryPreference = dietaryPreference;
        this.profileImageUrl = profileImageUrl;
    }

    public String getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(String subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public long getOneTimeMealExpiry() {
        return oneTimeMealExpiry;
    }

    public void setOneTimeMealExpiry(long oneTimeMealExpiry) {
        this.oneTimeMealExpiry = oneTimeMealExpiry;
    }
    
    public String getOneTimeAutoSelect() {
        return oneTimeAutoSelect;
    }
    
    public void setOneTimeAutoSelect(String oneTimeAutoSelect) {
        this.oneTimeAutoSelect = oneTimeAutoSelect;
    }

    public boolean isOneTimeSubscription() {
        return "ONE_TIME".equals(subscriptionType);
    }

    public long getSubscriptionExpiry() {
        return subscriptionExpiry;
    }

    public void setSubscriptionExpiry(long subscriptionExpiry) {
        this.subscriptionExpiry = subscriptionExpiry;
    }

    public long getLunchSubscriptionExpiry() {
        return lunchSubscriptionExpiry;
    }

    public void setLunchSubscriptionExpiry(long lunchSubscriptionExpiry) {
        this.lunchSubscriptionExpiry = lunchSubscriptionExpiry;
    }

    public long getDinnerSubscriptionExpiry() {
        return dinnerSubscriptionExpiry;
    }

    public void setDinnerSubscriptionExpiry(long dinnerSubscriptionExpiry) {
        this.dinnerSubscriptionExpiry = dinnerSubscriptionExpiry;
    }

    public String getId() {
        return userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDietaryPreference() {
        return dietaryPreference;
    }

    public void setDietaryPreference(String dietaryPreference) {
        this.dietaryPreference = dietaryPreference;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public Student copy() {
        Student s = new Student(
                userId,
                email,
                name,
                lunchStatus,
                dinnerStatus,
                subscriptionExpiry,
                lunchSubscriptionExpiry,
                dinnerSubscriptionExpiry,
                phone,
                gender,
                dietaryPreference,
                profileImageUrl);
        s.subscriptionType = subscriptionType;
        s.oneTimeMealExpiry = oneTimeMealExpiry;
        s.oneTimeAutoSelect = oneTimeAutoSelect;
        return s;
    }
}