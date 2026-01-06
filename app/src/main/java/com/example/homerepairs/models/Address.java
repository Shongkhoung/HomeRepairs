package com.example.homerepairs.models;

public class Address {
    private String id;
    private String name; // e.g. Home, Work
    private String streetAddress;
    private String city;
    private String zipCode;

    // Required empty constructor for Firestore
    public Address() {
    }

    public Address(String name, String streetAddress, String city, String zipCode) {
        this.name = name;
        this.streetAddress = streetAddress;
        this.city = city;
        this.zipCode = zipCode;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }
}
