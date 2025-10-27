package org.example.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

@Entity
@Table(name = "vehicle")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type")
@Access(AccessType.FIELD)
public abstract class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 10, unique = true)
    @Length(min = 4, max = 10)
    @NotNull
    private String plateNumber;

    @Column
    @Min(value = 0, message = "Per-hour price cannot be less than 0")
    private double perHourPrice;

    @Column
    private boolean isRented = false;

    @Version
    private long version;

    public Vehicle() {
        this.plateNumber = "";
        this.perHourPrice = 0;
    }

    public Vehicle(String plateNumber, double perHourPrice) {
        this.plateNumber = plateNumber;
        this.perHourPrice = perHourPrice;
    }

    // Getters
    public Long getId() {
        return id;
    }
    public String getPlateNumber() {
        return plateNumber;
    }
    public double getPerHourPrice() {
        return perHourPrice;
    }
    public boolean isRented() {
        return isRented;
    }

    // Setters
    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }
    public void setPerHourPrice(double perHourPrice) {
        this.perHourPrice = perHourPrice;
    }
    public void setRented(boolean rented) {
        isRented = rented;
    }
}
