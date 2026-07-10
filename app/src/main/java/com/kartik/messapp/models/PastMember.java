package com.kartik.messapp.models;

public class PastMember {
    private String userId;
    private String messId;
    private String name;
    private String phone;
    private String email;
    private long leftAt;

    public PastMember() {
    }

    public PastMember(String userId, String messId, String name, String phone, String email, long leftAt) {
        this.userId = userId;
        this.messId = messId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.leftAt = leftAt;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getMessId() { return messId; }
    public void setMessId(String messId) { this.messId = messId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public long getLeftAt() { return leftAt; }
    public void setLeftAt(long leftAt) { this.leftAt = leftAt; }
}
