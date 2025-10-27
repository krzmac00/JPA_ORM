package org.example.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.example.entities.Client;

import java.util.List;
import java.util.Optional;


public class ClientRepository implements CrudRepository<Client, Long>, Lockable<Client, Long> {

    private EntityManager entityManager;

    public ClientRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public <S extends Client> S save(S client) {
        if (client.getId() == null) {
            entityManager.persist(client);
        } else {
            client = entityManager.merge(client);
        }
        return client;
    }

    @Override
    public <S extends Client> List<S> saveAll(List<S> clients) {
        for (Client client : clients) {
            if (client.getId() == null) {
                entityManager.persist(client);
            } else {
                entityManager.merge(client);
            }
        }
        return clients;
    }

    @Override
    public Optional<Client> findById(Long id) {
        Client client = entityManager.find(Client.class, id);
        return client != null ? Optional.of(client) : Optional.empty();
    }

    @Override
    public Optional<Client> lockedFindById(Long id, LockModeType lockModeType) {
        Client client = entityManager.find(Client.class, id, lockModeType);
        return client != null ? Optional.of(client) : Optional.empty();
    }

    @Override
    public List<Client> findAll() {
        return entityManager.createQuery("SELECT c FROM Client c", Client.class).getResultList();
    }

    @Override
    public List<Client> findAllById(List<Long> ids) {
        return entityManager.createQuery("SELECT c FROM Client c WHERE c.id IN :ids", Client.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    @Override
    public long count() {
        return entityManager.createQuery("SELECT COUNT(c) FROM Client c", Long.class).getSingleResult();
    }

    @Override
    public void deleteById(Long id) {
        Client client = entityManager.find(Client.class, id);
        if (client != null) {
            entityManager.remove(client);
        }
    }

    @Override
    public void delete(Client client) {
        if (entityManager.contains(client)) {
            entityManager.remove(client);
        } else {
            Client managedClient = entityManager.merge(client);
            entityManager.remove(managedClient);
        }
    }

    @Override
    public void deleteAll(List<? extends Client> entities) {
        for (Client client : entities) {
            if (entityManager.contains(client)) {
                entityManager.remove(client);
            } else {
                Client managedClient = entityManager.merge(client);
                entityManager.remove(managedClient);
            }
        }
    }

    @Override
    public void deleteAllById(List<Long> ids) {
        for (Long id : ids) {
            Client client = entityManager.find(Client.class, id);
            if (client != null) {
                entityManager.remove(client);
            }
        }
    }

    @Override
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM Client").executeUpdate();
    }

    public boolean existsById(Long id) {
        if (id == null) {
            return false;
        }
        return entityManager.createQuery("SELECT COUNT(c) FROM Client c WHERE c.id = :id", Long.class).setParameter("id", id).getSingleResult() > 0;
    }
}