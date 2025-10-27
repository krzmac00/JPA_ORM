package RepositoriesTests;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import org.example.entities.Rent;
import org.example.entities.Client;
import org.example.entities.vehicles.*;
import org.example.repositories.RentRepository;
import org.junit.jupiter.api.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RentRepositoryTest {
    private static EntityManagerFactory emf;
    private static RentRepository rentRepository;
    private static EntityTransaction transaction;

    @BeforeEach
    void initTest() {
        emf = Persistence.createEntityManagerFactory("default");
        EntityManager entityManager = emf.createEntityManager();
        rentRepository = new RentRepository(entityManager);
        transaction = entityManager.getTransaction();
    }

    @AfterEach
    void endTest() {
        if(emf != null) emf.close();
    }

    @Test
    void saveTest() {
        Client client = new Client("0028965431","John", "Doe");
        Car car = new Car("EL9GM78", 25.5, 1.6, Car.SegmentType.A);
        Rent rent = new Rent(client, car, Timestamp.valueOf(LocalDateTime.now()).toLocalDateTime());

        transaction.begin();
        Rent saved = rentRepository.save(rent);
        transaction.commit();

        assertSame(rent, saved);
        assertSame(client, saved.getClient());
        assertSame(car, saved.getVehicle());
        assertEquals(rent.getId(), saved.getId());
        assertEquals(rent.getBeginTime(), saved.getBeginTime());
        assertFalse( rent.isArchived());
    }

    @Test
    void saveAllFindAllTest() {
        assertEquals(0, rentRepository.count());
        List<Rent> rents = new ArrayList<>();
        Client client1 = new Client("0028965431","John", "Doe");
        Client client2 = new Client("0321234531","Adam", "Smith");
        Car car = new Car("EL9GM78", 25.5, 1.6, Car.SegmentType.A);
        Bicycle bicycle = new Bicycle("7718001336", 12.5);
        Rent rent1 = new Rent(client1, car, Timestamp.valueOf(LocalDateTime.now()).toLocalDateTime());
        Rent rent2 = new Rent(client2, bicycle, Timestamp.valueOf(LocalDateTime.now()).toLocalDateTime());
        rents.add(rent1);
        rents.add(rent2);

        transaction.begin();
        rents = rentRepository.saveAll(rents);
        transaction.commit();
        assertEquals(2, rentRepository.count());

        List<Rent> savedRents = rentRepository.findAll();
        int i = 0;
        for (Rent rent : rents) {
            assertSame(rent.getClient(), savedRents.get(i).getClient());
            assertSame(rent.getVehicle(), savedRents.get(i).getVehicle());
            assertEquals(rent.getId(), savedRents.get(i).getId());
            i++;
        }
    }

    @Test
    void findByIDTest() {
        Client client = new Client("0028965431","John", "Doe");
        Car car = new Car("EL9GM78", 25.5, 1.6, Car.SegmentType.A);
        Rent rent = new Rent(client, car, Timestamp.valueOf(LocalDateTime.now()).toLocalDateTime());

        transaction.begin();
        Rent saved = rentRepository.save(rent);
        transaction.commit();

        if (rentRepository.findById(saved.getId()).isPresent()) {
            Rent found = rentRepository.findById(saved.getId()).get();
            assertSame(rent, found);
        } else {
            fail("Rent not found");
        }
    }

    @Test
    void findAllByIDTest() {
        List<Rent> rents = new ArrayList<>();
        List<Long> rentIds = new ArrayList<>();
        Client client1 = new Client("0028965431","John", "Doe");
        Client client2 = new Client("0321234531","Adam", "Smith");
        Car car = new Car("EL9GM78", 25.5, 1.6, Car.SegmentType.A);
        Bicycle bicycle = new Bicycle("7718001336", 12.5);
        Rent rent1 = new Rent(client1, car, Timestamp.valueOf(LocalDateTime.now()).toLocalDateTime());
        Rent rent2 = new Rent(client2, bicycle, Timestamp.valueOf(LocalDateTime.now()).toLocalDateTime());

        rents.add(rent1);
        transaction.begin();
        rentRepository.save(rent1);
        transaction.commit();
        rentIds.add(rent1.getId());

        rents.add(rent2);
        transaction.begin();
        rentRepository.save(rent2);
        transaction.commit();

        rentIds.add(rent2.getId());
        assertEquals(2, rentRepository.count());
        List<Rent> foundRents = rentRepository.findAllById(rentIds);
        int i = 0;
        for (Rent rent : rents) {
            assertSame(rent.getClient(), foundRents.get(i).getClient());
            assertSame(rent.getVehicle(), foundRents.get(i).getVehicle());
            assertEquals(rent.getId(), foundRents.get(i).getId());
            i++;
        }
    }

    @Test
    void deleteTest() {
        Client client = new Client("0028965431","John", "Doe");
        Car car = new Car("EL9GM78", 25.5, 1.6, Car.SegmentType.A);
        Rent rent = new Rent(client, car, Timestamp.valueOf(LocalDateTime.now()).toLocalDateTime());

        transaction.begin();
        rentRepository.save(rent);
        transaction.commit();
        assertEquals(1, rentRepository.count());

        transaction.begin();
        rentRepository.delete(rent);
        transaction.commit();
        assertEquals(0, rentRepository.count());
    }

    @Test
    void deleteByIdTest() {
        Client client = new Client("0028965431","John", "Doe");
        Car car = new Car("EL9GM78", 25.5, 1.6, Car.SegmentType.A);
        Rent rent = new Rent(client, car, Timestamp.valueOf(LocalDateTime.now()).toLocalDateTime());

        transaction.begin();
        rentRepository.save(rent);
        transaction.commit();
        assertEquals(1, rentRepository.count());

        transaction.begin();
        rentRepository.deleteById(rent.getId());
        transaction.commit();
        assertEquals(0, rentRepository.count());
    }

    @Test
    void deleteAllRentsTest() {
        List<Rent> rents = new ArrayList<>();
        Client client1 = new Client("0028965431","John", "Doe");
        Client client2 = new Client("0321234531","Adam", "Smith");
        Car car = new Car("EL9GM78", 25.5, 1.6, Car.SegmentType.A);
        Bicycle bicycle = new Bicycle("7718001336", 12.5);
        Rent rent1 = new Rent(client1, car, Timestamp.valueOf(LocalDateTime.now()).toLocalDateTime());
        Rent rent2 = new Rent(client2, bicycle, Timestamp.valueOf(LocalDateTime.now()).toLocalDateTime());
        rents.add(rent1);
        rents.add(rent2);

        transaction.begin();
        rentRepository.save(rent1);
        rentRepository.save(rent2);
        transaction.commit();
        assertEquals(2, rentRepository.count());

        transaction.begin();
        rentRepository.deleteAll(rents);
        transaction.commit();
        assertEquals(0, rentRepository.count());
    }

    @Test
    void deleteAllByIdTest() {
        List<Long> ids = new ArrayList<>();
        Client client1 = new Client("0028965431","John", "Doe");
        Client client2 = new Client("0321234531","Adam", "Smith");
        Car car = new Car("EL9GM78", 25.5, 1.6, Car.SegmentType.A);
        Bicycle bicycle = new Bicycle("7718001336", 12.5);
        Rent rent1 = new Rent(client1, car, Timestamp.valueOf(LocalDateTime.now()).toLocalDateTime());
        Rent rent2 = new Rent(client2, bicycle, Timestamp.valueOf(LocalDateTime.now()).toLocalDateTime());

        transaction.begin();
        rentRepository.save(rent1);
        rentRepository.save(rent2);
        transaction.commit();
        ids.add(rent1.getId());
        ids.add(rent2.getId());
        assertEquals(2, rentRepository.count());

        transaction.begin();
        rentRepository.deleteAllById(ids);
        transaction.commit();
        assertEquals(0, rentRepository.count());
    }

    @Test
    void deleteAllTest() {
        Client client1 = new Client("0028965431","John", "Doe");
        Client client2 = new Client("0321234531","Adam", "Smith");
        Car car = new Car("EL9GM78", 25.5, 1.6, Car.SegmentType.A);
        Bicycle bicycle = new Bicycle("7718001336", 12.5);
        Rent rent1 = new Rent(client1, car, Timestamp.valueOf(LocalDateTime.now()).toLocalDateTime());
        Rent rent2 = new Rent(client2, bicycle, Timestamp.valueOf(LocalDateTime.now()).toLocalDateTime());

        transaction.begin();
        rentRepository.save(rent1);
        rentRepository.save(rent2);
        transaction.commit();
        assertEquals(2, rentRepository.count());

        transaction.begin();
        rentRepository.deleteAll();
        transaction.commit();
        assertEquals(0, rentRepository.count());
    }

    @Test
    void existsByIdTest() {
        Client client = new Client("0028965431","John", "Doe");
        Car car = new Car("EL9GM78", 25.5, 1.6, Car.SegmentType.A);
        Rent rent = new Rent(client, car, Timestamp.valueOf(LocalDateTime.now()).toLocalDateTime());

        transaction.begin();
        Rent saved = rentRepository.save(rent);
        transaction.commit();

        assertTrue(rentRepository.existsById(saved.getId()));
    }
}
