package org.example.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.example.entities.vehicles.Car;

import java.time.LocalDateTime;

@Entity
public class Rent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private final LocalDateTime beginTime;

    @Column
    private LocalDateTime endTime;

    @Column
    private double vehiclePerHourPrice;

    @Column
    private double totalCost;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @NotNull
    private Client client;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @NotNull
    private Vehicle vehicle;

    @Column
    private boolean archived;

    @PrePersist
    @PreUpdate
    private void validateTimeRange() throws IllegalArgumentException {
        if (beginTime != null && endTime != null && !beginTime.isBefore(endTime)) {
            throw new IllegalArgumentException("beginTime must be earlier than endTime.");
        }
    }

    protected Rent() {
        this(new Client(), new Car(), LocalDateTime.parse("2000-01-01T00:00:00"));
    }

    public Rent(Client client, Vehicle vehicle, LocalDateTime beginTime) {
        this.client = client;
        this.vehicle = vehicle;
        this.beginTime = beginTime;
    }

    // Getters
    public Long getId() {
        return id;
    }
    public LocalDateTime getBeginTime() {
        return beginTime;
    }
    public LocalDateTime getEndTime() {
        return endTime;
    }
    public double getVehiclePerHourPrice() {
        return vehiclePerHourPrice;
    }
    public double getTotalCost() {
        return totalCost;
    }
    public Client getClient() {
        return client;
    }
    public Vehicle getVehicle() {
        return vehicle;
    }
    public boolean isArchived() {
        return archived;
    }

    // Setters
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }
    public void setVehiclePerHourPrice(double vehiclePerHourPrice) {
        this.vehiclePerHourPrice = vehiclePerHourPrice;
    }
    public void setArchived() {
        this.archived = true;
    }
}