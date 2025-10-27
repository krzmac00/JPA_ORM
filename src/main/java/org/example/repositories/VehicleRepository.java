package org.example.repositories;

import jakarta.persistence.LockModeType;
import org.example.entities.Vehicle;
import jakarta.persistence.EntityManager;


import java.util.List;
import java.util.Optional;

public class VehicleRepository implements CrudRepository<Vehicle, Long>, Lockable<Vehicle, Long> {

    private EntityManager entityManager;

    public VehicleRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public <S extends Vehicle> S save(S vehicle) {
        if (vehicle.getId() == null) {
            entityManager.persist(vehicle);
        } else {
            vehicle = entityManager.merge(vehicle);
        }
        return vehicle;
    }

    @Override
    public <S extends Vehicle> List<S> saveAll(List<S> vehicles) {
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getId() == null) {
                entityManager.persist(vehicle);
            } else {
                entityManager.merge(vehicle);
            }
        }
        return vehicles;
    }

    @Override
    public Optional<Vehicle> findById(Long id) {
        Vehicle vehicle = entityManager.find(Vehicle.class, id);
        return vehicle != null ? Optional.of(vehicle) : Optional.empty();
    }

    @Override
    public Optional<Vehicle> lockedFindById(Long id, LockModeType lockModeType) {
        Vehicle vehicle = entityManager.find(Vehicle.class, id, lockModeType);
        return vehicle != null ? Optional.of(vehicle) : Optional.empty();
    }

    @Override
    public List<Vehicle> findAll() {
        return entityManager.createQuery("SELECT v FROM Vehicle v", Vehicle.class).getResultList();
    }

    @Override
    public List<Vehicle> findAllById(List<Long> ids) {
        return entityManager.createQuery("SELECT v FROM Vehicle v WHERE v.id IN :ids", Vehicle.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    @Override
    public long count() {
        return entityManager.createQuery("SELECT COUNT(v) FROM Vehicle v", Long.class).getSingleResult();
    }

    @Override
    public void deleteById(Long id) {
        Vehicle vehicle = entityManager.find(Vehicle.class, id);
        if (vehicle != null) {
            entityManager.remove(vehicle);
        }
    }

    @Override
    public void delete(Vehicle vehicle) {
        if (entityManager.contains(vehicle)) {
            entityManager.remove(vehicle);
        } else {
            Vehicle managedVehicle = entityManager.merge(vehicle);
            entityManager.remove(managedVehicle);
        }
    }

    @Override
    public void deleteAll(List<? extends Vehicle> vehicles) {
        for (Vehicle vehicle : vehicles) {
            if (entityManager.contains(vehicle)) {
                entityManager.remove(vehicle);
            } else {
                Vehicle managedVehicle = entityManager.merge(vehicle);
                entityManager.remove(managedVehicle);
            }
        }
    }

    @Override
    public void deleteAllById(List<Long> ids) {
        for (Long id : ids) {
            Vehicle vehicle = entityManager.find(Vehicle.class, id);
            if (vehicle != null) {
                entityManager.remove(vehicle);
            }
        }
    }

    @Override
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM Vehicle").executeUpdate();
    }

    public boolean existsById(Long id) {
        if (id == null) {
            return false;
        }
        return entityManager.createQuery("SELECT COUNT(v) FROM Vehicle v WHERE v.id = :id", Long.class).setParameter("id", id).getSingleResult() > 0;
    }
}
