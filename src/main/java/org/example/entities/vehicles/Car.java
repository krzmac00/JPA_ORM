package org.example.entities.vehicles;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.example.entities.Vehicle;

@Entity
@Table(name = "car")
@PrimaryKeyJoinColumn(name="car_id")
@DiscriminatorValue("car")
public class Car extends Vehicle {
    @Column
    @Min(value = 0, message = "Value of engineDisplacement cannot be less than 0")
    private double engineDisplacement = 0.0;

    @Column
    @NotNull
    private SegmentType segmentType = SegmentType.A;

    public Car() {
        super();
    }

    public Car(String plateNumber, double perHourPrice, double engineDisplacement, SegmentType segmentType) {
        super(plateNumber, perHourPrice);
        this.engineDisplacement = engineDisplacement;
        this.segmentType = segmentType;
    }

    //getters
    public double getEngineDisplacement() { return engineDisplacement; }
    public SegmentType getSegmentType() { return segmentType; }

    //setters
    public void setEngineDisplacement(double engineDisplacement) { this.engineDisplacement = engineDisplacement; }
    public void setSegmentType(SegmentType segmentType) { this.segmentType = segmentType; }

    public enum SegmentType {
        A,
        B,
        C,
        D,
        E
    }
}
