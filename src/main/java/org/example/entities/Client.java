package org.example.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.example.util.Address;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "client")
@Access(AccessType.FIELD)
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String personalId;

    @Column
    @NotNull
    private String firstName;

    @Column
    @NotNull
    private String lastName;

    @Embedded
    private Address address;

    @Column
    @Min(value = 1, message = "Minimal maxVehicles value cant be less than 1")
    private int maxVehicles = 2;

    @Column
    private double discount = 0.0;

    @Column
    private double moneySpent = 0.0;

    @OneToMany(mappedBy = "client", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Rent> currentRents = new ArrayList<Rent>();

    @Version
    private long version;

    public Client() {
        this.personalId = "";
        this.firstName = "";
        this.lastName = "";
        this.address = new Address();
    }

    public Client(String personalId, String firstName, String lastName) {
        this.personalId = personalId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = new Address();
    }

    public Client(String personalId, String firstName, String lastName, Address address) {
        this.personalId = personalId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
    }

    // Getters
    public Long getId() {
        return id;
    }
    public String getPersonalId() {
        return personalId;
    }
    public String getFirstName() {
        return firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public Address getAddress() {
        return address;
    }
    public int getMaxVehicles() {
        return maxVehicles;
    }
    public double getDiscount() {
        return discount;
    }
    public double getMoneySpent() {
        return moneySpent;
    }
    public long getVersion() {
        return version;
    }
    public List<Rent> getCurrentRents() {
        return Collections.unmodifiableList(currentRents);
    }

    // Setters
    public void setPersonalId(String personalId) {
        this.personalId = personalId;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public void setAddress(Address address) {
        this.address = address;
    }
    public void setMaxVehicles(int maxVehicles) {
        this.maxVehicles = maxVehicles;
    }

    public void addMoneySpent(double value) {
        this.moneySpent += value;

        if (moneySpent >= 50000.0) {
            discount = 0.05;
        } else if (moneySpent >= 40000.0) {
            discount = 0.04;
        } else if (moneySpent >= 30000.0) {
            discount = 0.03;
        } else if (moneySpent >= 20000.0) {
            discount = 0.02;
        } else if (moneySpent >= 10000.0) {
            discount = 0.01;
        }
    }

    public void addRent(Rent rent) {
        currentRents.add(rent);
    }
}