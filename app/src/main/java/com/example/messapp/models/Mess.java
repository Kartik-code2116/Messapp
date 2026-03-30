package com.example.messapp.models;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Mess {
    public static final String FIELD_NAME = "name";
    public static final String FIELD_LOCATION = "location";
    public static final String FIELD_CONTACT = "contact";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_MONTHLY_PRICE = "monthlyPrice";

    private String name;
    private String location;
    private String contact;
    private String description;
    private double monthlyPrice;

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

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String getContact() {
        return contact;
    }

    public String getDescription() {
        return description;
    }

    public double getMonthlyPrice() {
        return monthlyPrice;
    }
}
