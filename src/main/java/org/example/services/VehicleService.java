package org.example.services;

import jakarta.persistence.*;
import org.example.entities.Vehicle;
import org.example.exceptions.ConflictException;
import org.example.exceptions.RentalException;
import org.example.repositories.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class VehicleService {
    private static final Logger logger = LoggerFactory.getLogger(VehicleService.class);
    private final EntityManagerFactory entityManagerFactory;
    private final VehicleRepository vehicleRepository;

    public VehicleService(EntityManagerFactory entityManagerFactory, VehicleRepository vehicleRepository) {
        this.entityManagerFactory = entityManagerFactory;
        this.vehicleRepository = vehicleRepository;
    }

    public void updateVehiclePrice(Long vehicleId, double newPrice) throws RentalException {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction entityTransaction = null;

        vehicleRepository.setEntityManager(entityManager);

        try (entityManager) {
            entityTransaction = entityManager.getTransaction();
            entityTransaction.begin();

            Vehicle vehicle = vehicleRepository.lockedFindById(vehicleId, LockModeType.OPTIMISTIC)
                    .orElseThrow(() -> new RentalException("Vehicle not found with ID: " + vehicleId));

            vehicle.setPerHourPrice(newPrice);
            vehicleRepository.save(vehicle);

            entityTransaction.commit();
            logger.info("Updated price for vehicle ID: {} to {}", vehicleId, newPrice);

        } catch (OptimisticLockException e) {
            if (entityTransaction != null && entityTransaction.isActive()) {
                entityTransaction.rollback();
            }
            logger.warn("Optimistic lock conflict while updating vehicle price for vehicle ID: {}", vehicleId);
            throw new ConflictException("Client was busy with a rent; please retry");
        } catch (Exception e) {
            if (entityTransaction != null && entityTransaction.isActive()) {
                entityTransaction.rollback();
            }
            logger.error("Unexpected error while changing vehicle price for vehicle ID: {} - {}", vehicleId, e.getMessage());
            throw e;
        }
    }
}
