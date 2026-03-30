package com.example.messapp;

public class Mess {
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
        // Required empty public constructor for Firestore
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
