package org.example.services;

import jakarta.persistence.*;
import org.example.entities.Client;
import org.example.entities.Rent;
import org.example.entities.Vehicle;
import org.example.exceptions.RentalException;
import org.example.repositories.ClientRepository;
import org.example.repositories.RentRepository;
import org.example.repositories.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class RentService {

    private static final Logger logger = LoggerFactory.getLogger(RentService.class);
    private static final int maxRetries = 3;
    private final EntityManagerFactory entityManagerFactory;

    private final ClientRepository clientRepository;
    private final VehicleRepository vehicleRepository;
    private final RentRepository rentRepository;

    public RentService(EntityManagerFactory entityManagerFactory, ClientRepository clientRepository, VehicleRepository vehicleRepository, RentRepository rentRepository) {
        this.entityManagerFactory = entityManagerFactory;
        this.clientRepository = clientRepository;
        this.vehicleRepository = vehicleRepository;
        this.rentRepository = rentRepository;
    }


    public Rent rentVehicle(Long clientId, Long vehicleId, LocalDateTime beginTime) throws RentalException {
        int attempt = 0;
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction entityTransaction = null;

        clientRepository.setEntityManager(entityManager);
        vehicleRepository.setEntityManager(entityManager);
        rentRepository.setEntityManager(entityManager);

        while (attempt < maxRetries) {
            try(entityManager) {
                attempt++;
                entityTransaction = entityManager.getTransaction();
                entityTransaction.begin();
                logger.info("Rental attempt: {} for client: {} and vehicle: {}", attempt, clientId, vehicleId);

                Client client = clientRepository.findById(clientId).orElse(null);

                if (client == null) {
                    throw new RentalException("Client not found with ID: " + clientId);
                }

                Vehicle vehicle = vehicleRepository.findById(vehicleId).orElse(null);

                if (vehicle == null) {
                    throw new RentalException("Vehicle not found with ID: " + vehicleId);
                }

                validateRentalConditions(client, vehicle, entityManager);

                Rent rent = new Rent(client, vehicle, beginTime);
                vehicle.setRented(true);
                client.addRent(rent);
                rent.setVehiclePerHourPrice(vehicle.getPerHourPrice());

                rent = rentRepository.save(rent);
                vehicleRepository.save(vehicle);
                clientRepository.save(client);

                entityManager.flush();
                entityTransaction.commit();

                logger.info("Successfully rented vehicle ID: {} to client ID: {}", vehicleId, clientId);
                return rent;

            } catch (OptimisticLockException e) {
                if (attempt >= maxRetries) {
                    logger.warn("Failed to rent vehicle after {} attempts due to concurrent modifications", maxRetries);
                    throw new RentalException("Unable to complete rental due to concurrent " +
                            "modifications. Please try again.", e);
                }
                logger.info("Optimistic lock exception on attempt {}. Retrying...", attempt);
                entityManager.clear();
            } catch (Exception e) {
                if (entityTransaction != null && entityTransaction.isActive()) {
                    entityTransaction.rollback();
                }
                logger.error("Unexpected error while renting vehicle ID: {} to client ID: {} - {}",
                        vehicleId, clientId, e.getMessage());
                throw e;
            }
        }
        throw new RentalException("Failed to complete rental after maximum retries");
    }

    public Rent endRental(Long rentId, LocalDateTime endTime) throws RentalException {
        int attempt = 0;
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction entityTransaction = null;

        clientRepository.setEntityManager(entityManager);
        vehicleRepository.setEntityManager(entityManager);
        rentRepository.setEntityManager(entityManager);

        while (attempt < maxRetries) {
            try(entityManager) {
                attempt++;
                entityTransaction = entityManager.getTransaction();
                entityTransaction.begin();

                Optional<Rent> rentOpt = rentRepository.findById(rentId);
                if (rentOpt.isEmpty()) {
                    throw new RentalException("Rental not found with ID: " + rentId);
                }

                Rent rent = rentOpt.get();

                if (rent.isArchived()) {
                    throw new RentalException("Rental is already archived");
                }

                if (rent.getEndTime() != null) {
                    throw new RentalException("Rental has already ended");
                }

                Vehicle vehicle = entityManager.find(Vehicle.class,
                        rent.getVehicle().getId(), LockModeType.OPTIMISTIC_FORCE_INCREMENT);

                Client client = entityManager.find(Client.class,
                        rent.getClient().getId(), LockModeType.OPTIMISTIC_FORCE_INCREMENT);

                double totalCost = calculateRentalCost(rent, endTime, client.getDiscount());

                rent = entityManager.find(Rent.class, rentId);
                rent.setEndTime(endTime);
                rent.setTotalCost(totalCost);
                rent.setArchived();

                vehicle.setRented(false);
                client.addMoneySpent(totalCost);

                rentRepository.save(rent);
                vehicleRepository.save(vehicle);
                clientRepository.save(client);

                entityManager.flush();
                entityTransaction.commit();

                logger.info("Successfully ended rental ID: {}", rentId);
                return rent;

            } catch (OptimisticLockException e) {
                if (attempt >= maxRetries) {
                    throw new RentalException("Unable to end rental due to concurrent " +
                            "modifications. Please try again.", e);
                }
                entityManager.clear();
            } catch (Exception e) {
                if (entityTransaction != null && entityTransaction.isActive()) {
                    entityTransaction.rollback();
                }
                logger.error("Unexpected error while ending rental ID: {} - {}", rentId, e.getMessage());
                throw e;
            }
        }
        throw new RentalException("Failed to end rental after maximum retries");
    }

    public List<Rent> getActiveRentalsForClient(Long clientId, EntityManager entityManager) {
        return entityManager.createQuery(
                        "SELECT r FROM Rent r WHERE r.client.id = :clientId AND r.archived = false",
                        Rent.class)
                .setParameter("clientId", clientId)
                .getResultList();
    }

    private int countActiveRentals(Long clientId, EntityManager entityManager) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(r) FROM Rent r WHERE r.client.id = :clientId " +
                                "AND r.archived = false AND r.endTime IS NULL",
                        Long.class)
                .setParameter("clientId", clientId)
                .getSingleResult();
        return count.intValue();
    }

    private void validateRentalConditions(Client client, Vehicle vehicle, EntityManager entityManager)
            throws RentalException {

        if (vehicle.isRented()) {
            throw new RentalException("Vehicle with plate number " +
                    vehicle.getPlateNumber() + " is already rented");
        }

        int activeRentals = countActiveRentals(client.getId(), entityManager);

        if (activeRentals >= client.getMaxVehicles()) {
            throw new RentalException("Client " + client.getFirstName() + " " +
                    client.getLastName() + " has reached the maximum number of rentals (" +
                    client.getMaxVehicles() + ")");
        }
    }

    private double calculateRentalCost(Rent rent, LocalDateTime endTime, double discount) {
        if (endTime.isBefore(rent.getBeginTime())) {
            throw new IllegalArgumentException("End time cannot be before begin time");
        }

        long hours = java.time.Duration.between(rent.getBeginTime(), endTime).toHours();
        if (hours == 0) {
            hours = 1; // Minimum 1 hour charge
        }

        double perHourPrice = rent.getVehiclePerHourPrice();
        double totalCost = perHourPrice * hours;
        double discountAmount = totalCost * discount;

        return totalCost - discountAmount;
    }

    public boolean isVehicleAvailable(Long vehicleId) {
        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
        return vehicleOpt.map(vehicle -> !vehicle.isRented()).orElse(false);
    }

    public List<Rent> getArchivedRentals(EntityManager entityManager) {
        return entityManager.createQuery(
                        "SELECT r FROM Rent r WHERE r.archived = true",
                        Rent.class)
                .getResultList();
    }

}