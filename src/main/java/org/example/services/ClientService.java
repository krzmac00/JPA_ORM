package org.example.services;

import jakarta.persistence.*;
import org.example.entities.Client;
import org.example.exceptions.ConflictException;
import org.example.repositories.ClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientService {
    private static final Logger logger = LoggerFactory.getLogger(ClientService.class);
    private final EntityManagerFactory entityManagerFactory;
    private final ClientRepository clientRepository;

    public ClientService(EntityManagerFactory entityManagerFactory, ClientRepository clientRepository) {
        this.entityManagerFactory = entityManagerFactory;
        this.clientRepository = clientRepository;
    }

    public void changeMaxVehicles(long clientId, int newLimit) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction entityTransaction = null;

        clientRepository.setEntityManager(entityManager);

        try (entityManager) {
            entityTransaction = entityManager.getTransaction();
            entityTransaction.begin();
            logger.info("Attempting to change maxVehicles for client ID: {}", clientId);

            Client client = clientRepository.lockedFindById(clientId, LockModeType.OPTIMISTIC)
                    .orElseThrow(() -> new IllegalArgumentException("Client not found with ID: " + clientId));

            client.setMaxVehicles(newLimit);
            entityTransaction.commit();
            logger.info("Client {} maxVehicles changed to {}", clientId, newLimit);
        } catch (OptimisticLockException e) {
            if (entityTransaction != null && entityTransaction.isActive()) {
                entityTransaction.rollback();
            }
            logger.warn("Optimistic lock conflict while changing maxVehicles for client ID: {}", clientId);
            throw new ConflictException("Client was busy with a rent; please retry");
        } catch (Exception e) {
            if (entityTransaction != null && entityTransaction.isActive()) {
                entityTransaction.rollback();
            }
            logger.error("Unexpected error while changing maxVehicles for client ID: {} - {}", clientId, e.getMessage());
            throw e;
        }
    }
}
