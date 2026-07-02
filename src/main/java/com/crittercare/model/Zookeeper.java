package com.crittercare.model;

public class Zookeeper {

    private final int id;
    private String name;

    public Zookeeper(int id, String name) {
        this.id   = id;
        this.name = name;
    }

    public int    getId()   { return id; }
    public String getName() { return name; }
    public void   setName(String name) { this.name = name; }

    public String getInitials() {
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
        }
        return name.isBlank() ? "?" : name.substring(0, Math.min(2, name.length())).toUpperCase();
    }
}
