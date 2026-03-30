package com.example.messapp.models;

public class SubscriptionRequest {
    private String id;
    private String studentId;
    private String studentName;
    private String studentEmail;
    private String messId;
    private long timestamp;
    private String status; // PENDING, GRANTED
    private String type; // LUNCH, DINNER, BOTH

    public SubscriptionRequest() {
        // Required for Firebase
    }

    public SubscriptionRequest(String id, String studentId, String studentName, String studentEmail, String messId,
            long timestamp, String status, String type) {
        this.id = id;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.messId = messId;
        this.timestamp = timestamp;
        this.status = status;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getMessId() {
        return messId;
    }

    public void setMessId(String messId) {
        this.messId = messId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
