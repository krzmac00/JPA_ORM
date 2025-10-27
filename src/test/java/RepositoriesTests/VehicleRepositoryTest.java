package RepositoriesTests;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import org.example.entities.Vehicle;
import org.example.entities.vehicles.Bicycle;
import org.example.entities.vehicles.Car;
import org.example.repositories.VehicleRepository;
import org.junit.jupiter.api.*;


import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class VehicleRepositoryTest {
    private static EntityManagerFactory emf;
    private static VehicleRepository vehicleRepository;
    private static EntityTransaction transaction;

    @BeforeEach
    void initTest() {
        emf = Persistence.createEntityManagerFactory("default");
        EntityManager entityManager = emf.createEntityManager();
        vehicleRepository = new VehicleRepository(entityManager);
        transaction = entityManager.getTransaction();
    }

    @AfterEach
    void endTest() {
        if(emf != null) emf.close();
    }

    @Test
    void saveTest() {
        Car car = new Car("EL9GM78", 25.5, 1.6, Car.SegmentType.A);

        transaction.begin();
        Car saved = vehicleRepository.save(car);
        transaction.commit();

        assertSame(car, saved);
        assertEquals(car.getId(), saved.getId());
        assertEquals(car.getPlateNumber(), saved.getPlateNumber());
        assertEquals(car.getPerHourPrice(), saved.getPerHourPrice());
        assertEquals(car.getEngineDisplacement(), saved.getEngineDisplacement());
        assertEquals(car.getSegmentType(), saved.getSegmentType());
    }

    @Test
    void saveAllFindAllTest() {
        assertEquals(0, vehicleRepository.count());
        List<Vehicle> vehicles = new ArrayList<>();
        Car car1 = new Car("EL9GM78", 25.5, 1.6, Car.SegmentType.A);
        Car car2 = new Car("SK294TJ", 25.5, 1.6, Car.SegmentType.A);
        Bicycle bicycle = new Bicycle("7718001336", 12.5);

        vehicles.add(car1);
        vehicles.add(car2);
        vehicles.add(bicycle);
        assertEquals(3, vehicles.size());

        transaction.begin();
        vehicleRepository.saveAll(vehicles);
        transaction.commit();
        assertEquals(3, vehicleRepository.count());

        List<Vehicle> savedVehicles = vehicleRepository.findAll();
        int i = 0;
        for (Vehicle vehicle : vehicles) {
            assertEquals(vehicle.getId(), savedVehicles.get(i).getId());
            assertEquals(vehicle.getPlateNumber(), savedVehicles.get(i).getPlateNumber());
            assertEquals(vehicle.getPerHourPrice(), savedVehicles.get(i).getPerHourPrice());
            assertEquals(vehicle.isRented(), savedVehicles.get(i).isRented());
            i++;
        }
        assertEquals(Bicycle.class, savedVehicles.get(2).getClass());
    }

    @Test
    void findByIDTest() {
        Vehicle car = new Car("EL9GM78", 25.5, 1.6, Car.SegmentType.A);

        transaction.begin();
        Vehicle saved = vehicleRepository.save(car);
        transaction.commit();

        if (vehicleRepository.findById(saved.getId()).isPresent()) {
            Vehicle found = vehicleRepository.findById(saved.getId()).get();
            assertSame(car, found);
        } else {
            fail("Vehicle not found");
        }
    }

    @Test
    void findAllByIDTest() {
        List<Vehicle> vehicles = new ArrayList<>();
        List<Long> vehiclesIds = new ArrayList<>();
        Car car1 = new Car("EL9GM78", 25.5, 1.6, Car.SegmentType.A);
        Car car2 = new Car("SK294TJ", 25.5, 1.6, Car.SegmentType.A);
        Bicycle bicycle = new Bicycle("7718001336", 12.5);
        vehicles.add(car1);
        vehicles.add(car2);
        vehicles.add(bicycle);

        transaction.begin();
        vehicleRepository.save(car1);
        vehicleRepository.save(car2);
        vehicleRepository.save(bicycle);
        transaction.commit();

        vehiclesIds.add(car1.getId());
        vehiclesIds.add(car2.getId());
        vehiclesIds.add(bicycle.getId());
        assertEquals(3, vehicleRepository.count());

        List<Vehicle> foundVehicles = vehicleRepository.findAllById(vehiclesIds);
        int i = 0;
        for (Vehicle vehicle : vehicles) {
            assertEquals(vehicle.getId(), foundVehicles.get(i).getId());
            assertEquals(vehicle.getPlateNumber(), foundVehicles.get(i).getPlateNumber());
            assertEquals(vehicle.getPerHourPrice(), foundVehicles.get(i).getPerHourPrice());
            assertEquals(vehicle.isRented(), foundVehicles.get(i).isRented());
            i++;
        }
    }

    @Test
    void deleteTest() {
        Vehicle vehicle1 = new Car("EL9GM78", 25.5, 1.6, Car.SegmentType.A);

        transaction.begin();
        vehicleRepository.save(vehicle1);
        transaction.commit();
        assertEquals(1, vehicleRepository.count());

        transaction.begin();
        vehicleRepository.delete(vehicle1);
        transaction.commit();
        assertEquals(0, vehicleRepository.count());
    }

    @Test
    void deleteByIdTest() {
        Vehicle vehicle1 = new Car("EL9GM78", 25.5, 1.6, Car.SegmentType.A);

        transaction.begin();
        vehicleRepository.save(vehicle1);
        transaction.commit();
        assertEquals(1, vehicleRepository.count());

        transaction.begin();
        vehicleRepository.deleteById(vehicle1.getId());
        transaction.commit();
        assertEquals(0, vehicleRepository.count());
    }

    @Test
    void deleteAllVehiclesTest() {
        List<Vehicle> vehicles = new ArrayList<>();
        Car car1 = new Car("EL9GM78", 25.5, 1.6, Car.SegmentType.A);
        Car car2 = new Car("SK294TJ", 25.5, 1.6, Car.SegmentType.A);
        Bicycle bicycle = new Bicycle("7718001336", 12.5);
        vehicles.add(car1);
        vehicles.add(car2);
        vehicles.add(bicycle);

        transaction.begin();
        vehicleRepository.saveAll(vehicles);
        transaction.commit();
        assertEquals(3, vehicleRepository.count());

        transaction.begin();
        vehicleRepository.deleteAll(vehicles);
        transaction.commit();
        assertEquals(0, vehicleRepository.count());
    }

    @Test
    void deleteAllByIdTest() {
        List<Long> ids = new ArrayList<>();
        Car car1 = new Car("EL9GM78", 25.5, 1.6, Car.SegmentType.A);
        Car car2 = new Car("SK294TJ", 25.5, 1.6, Car.SegmentType.A);
        Bicycle bicycle = new Bicycle("7718001336", 12.5);

        transaction.begin();
        vehicleRepository.save(car1);
        vehicleRepository.save(car2);
        vehicleRepository.save(bicycle);
        transaction.commit();
        ids.add(car1.getId());
        ids.add(car2.getId());
        ids.add(bicycle.getId());
        assertEquals(3, vehicleRepository.count());

        transaction.begin();
        vehicleRepository.deleteAllById(ids);
        transaction.commit();
        assertEquals(0, vehicleRepository.count());
    }

    @Test
    void deleteAllTest() {
        Car car1 = new Car("EL9GM78", 25.5, 1.6, Car.SegmentType.A);
        Car car2 = new Car("SK294TJ", 25.5, 1.6, Car.SegmentType.A);
        Bicycle bicycle = new Bicycle("7718001336", 12.5);

        transaction.begin();
        vehicleRepository.save(car1);
        vehicleRepository.save(car2);
        vehicleRepository.save(bicycle);
        transaction.commit();
        assertEquals(3, vehicleRepository.count());

        transaction.begin();
        vehicleRepository.deleteAll();
        transaction.commit();
        assertEquals(0, vehicleRepository.count());
    }

    @Test
    void existsByIdTest() {
        Car car = new Car("EL9GM78", 25.5, 1.6, Car.SegmentType.A);

        transaction.begin();
        Car saved = vehicleRepository.save(car);
        transaction.commit();

        assertTrue(vehicleRepository.existsById(saved.getId()));
    }
}
