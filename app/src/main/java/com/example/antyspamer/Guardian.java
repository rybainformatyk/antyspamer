package com.example.antyspamer;

public class Guardian {
    private int id;
    private String name;
    private String phone;

    public Guardian(int id, String name, String phone) {
        this.id = id;
        this.name = name;
        this.phone = phone;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
}
