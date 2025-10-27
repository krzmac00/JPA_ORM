package org.example.entities.vehicles;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import org.example.entities.Vehicle;

@Entity
@Table(name = "bicycle")
@PrimaryKeyJoinColumn(name="bicycle_id")
@DiscriminatorValue("bicycle")
public class Bicycle extends Vehicle {
    public Bicycle() {
        super();
    }

    public Bicycle(String plateNumber, double perHourPrice) {
        super(plateNumber, perHourPrice);
    }
}
