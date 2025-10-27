package org.example.repositories;

import jakarta.persistence.EntityManager;
import org.example.entities.Rent;

import java.util.List;
import java.util.Optional;

public class RentRepository implements CrudRepository<Rent, Long> {

    private EntityManager entityManager;

    public RentRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public <S extends Rent> S save(S rent) {
        if (rent.getId() == null) {
            entityManager.persist(rent);
        } else {
            rent = entityManager.merge(rent);
        }
        return rent;
    }

    @Override
    public <S extends Rent> List<S> saveAll(List<S> rents) {
        for (Rent rent : rents) {
            if (rent.getId() == null) {
                entityManager.persist(rent);
            } else {
                entityManager.merge(rent);
            }
        }
        return rents;
    }

    @Override
    public Optional<Rent> findById(Long id) {
        Rent rent = entityManager.find(Rent.class, id);
        return rent != null ? Optional.of(rent) : Optional.empty();
    }

    @Override
    public List<Rent> findAll() {
        return entityManager.createQuery("SELECT r FROM Rent r", Rent.class).getResultList();
    }

    @Override
    public List<Rent> findAllById(List<Long> ids) {
        return entityManager.createQuery("SELECT r FROM Rent r WHERE r.id IN :ids", Rent.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    @Override
    public long count() {
        return entityManager.createQuery("SELECT COUNT(r) FROM Rent r", Long.class).getSingleResult();
    }

    @Override
    public void delete(Rent rent) {
        if (entityManager.contains(rent)) {
            entityManager.remove(rent);
        } else {
            Rent managedRent = entityManager.merge(rent);
            entityManager.remove(managedRent);
        }
    }

    @Override
    public void deleteById(Long id) {
        Rent rent = entityManager.find(Rent.class, id);
        if (rent != null) {
            entityManager.remove(rent);
        }
    }

    @Override
    public void deleteAll(List<? extends Rent> rents) {
        for (Rent rent : rents) {
            if (entityManager.contains(rent)) {
                entityManager.remove(rent);
            } else {
                Rent managedRent = entityManager.merge(rent);
                entityManager.remove(managedRent);
            }
        }
    }

    @Override
    public void deleteAllById(List<Long> ids) {
        for (Long id : ids) {
            Rent rent = entityManager.find(Rent.class, id);
            if (rent != null) {
                entityManager.remove(rent);
            }
        }
    }

    @Override
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM Rent").executeUpdate();
    }

    public boolean existsById(Long id) {
        if (id == null) {
            return false;
        }
        return entityManager.createQuery("SELECT COUNT(r) FROM Rent r WHERE r.id = :id", Long.class).
                setParameter("id", id).getSingleResult() > 0;
    }
}
