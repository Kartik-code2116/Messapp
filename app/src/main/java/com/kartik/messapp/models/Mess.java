package com.kartik.messapp.models;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Mess {
    public static final String FIELD_MESS_ID = "messId";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_LOCATION = "location";
    public static final String FIELD_CONTACT = "contact";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_STUDENT_COUNT = "studentCount";
    public static final String FIELD_MONTHLY_PRICE = "monthlyPrice";
    public static final String FIELD_AVG_RATING = "avgRating";
    public static final String FIELD_NUM_REVIEWS = "numReviews";

    private String messId;
    private String name;
    private String location;
    private String contact;
    private String description;
    private long studentCount;
    private double monthlyPrice;
    private double avgRating;
    private long numReviews;

    public Mess() {
        // Default constructor required for calls to DataSnapshot.getValue(Mess.class)
    }

    public Mess(String name, String location, String contact, String description, double monthlyPrice) {
        this.name = name;
        this.location = location;
        this.contact = contact;
        this.description = description;
        this.monthlyPrice = monthlyPrice;
    }

    public Mess(String messId, String name, String location, String contact, String description, long studentCount,
            double monthlyPrice, double avgRating, long numReviews) {
        this.messId = messId;
        this.name = name;
        this.location = location;
        this.contact = contact;
        this.description = description;
        this.studentCount = studentCount;
        this.monthlyPrice = monthlyPrice;
        this.avgRating = avgRating;
        this.numReviews = numReviews;
    }

    // Getters and Setters
    public String getMessId() {
        return messId;
    }

    public void setMessId(String messId) {
        this.messId = messId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(long studentCount) {
        this.studentCount = studentCount;
    }

    public double getMonthlyPrice() {
        return monthlyPrice;
    }

    public void setMonthlyPrice(double monthlyPrice) {
        this.monthlyPrice = monthlyPrice;
    }

    public double getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(double avgRating) {
        this.avgRating = avgRating;
    }

    public long getNumReviews() {
        return numReviews;
    }

    public void setNumReviews(long numReviews) {
        this.numReviews = numReviews;
    }
}
