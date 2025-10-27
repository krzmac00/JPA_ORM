package org.example.util;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
@Access(AccessType.FIELD)
public class Address implements Serializable {
    private String streetName;
    private String streetNumber;
    private String city;
    private String postalCode;

    public Address() {
        this.streetName = "";
        this.streetNumber = "";
        this.city = "";
        this.postalCode = "";
    }

    public Address(String streetName, String streetNumber, String city, String postalCode) {
        this.streetName = streetName;
        this.streetNumber = streetNumber;
        this.city = city;
        this.postalCode = postalCode;
    }

    // Getters
    public String getStreetName() {
        return streetName;
    }

    public String getStreetNumber() {
        return streetNumber;
    }

    public String getCity() {
        return city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    // Setters
    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public void setStreetNumber(String streetNumber) {
        this.streetNumber = streetNumber;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }
}
