package ServicesTests;

import jakarta.persistence.*;
import org.example.entities.Vehicle;
import org.example.entities.vehicles.Bicycle;
import org.example.entities.vehicles.Car;
import org.example.exceptions.RentalException;
import org.example.repositories.VehicleRepository;
import org.example.services.VehicleService;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class VehicleServiceTest {
    private static EntityManagerFactory emf;
    private static VehicleService vehicleService;
    private static VehicleRepository vehicleRepository;

    @BeforeAll
    static void setup() {
        emf = Persistence.createEntityManagerFactory("default");
    }

    @AfterAll
    static void tearDown() {
        if (emf != null) {
            emf.close();
        }
    }

    @BeforeEach
    void initTest() {
        EntityManager entityManager = emf.createEntityManager();
        vehicleRepository = new VehicleRepository(entityManager);
        vehicleService = new VehicleService(emf, vehicleRepository);
        entityManager.close();
    }

    @Test
    void updateVehiclePrice_ShouldUpdateCarPrice() throws RentalException {
        // Given
        EntityManager em = emf.createEntityManager();
        vehicleRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Car car = new Car("EL9GM78", 25.5, 1.6, Car.SegmentType.A);
        car = vehicleRepository.save(car);
        transaction.commit();

        Long vehicleId = car.getId();
        double initialPrice = car.getPerHourPrice();
        double newPrice = 35.0;

        em.close();

        // When
        vehicleService.updateVehiclePrice(vehicleId, newPrice);

        // Then
        em = emf.createEntityManager();
        vehicleRepository.setEntityManager(em);
        Vehicle updatedVehicle = vehicleRepository.findById(vehicleId).orElse(null);

        assertNotNull(updatedVehicle);
        assertEquals(newPrice, updatedVehicle.getPerHourPrice());
        assertNotEquals(initialPrice, updatedVehicle.getPerHourPrice());

        em.close();
    }

    @Test
    void updateVehiclePrice_ShouldUpdateBicyclePrice() throws RentalException {
        // Given
        EntityManager em = emf.createEntityManager();
        vehicleRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Bicycle bicycle = new Bicycle("BK30011LD", 12.5);
        bicycle = vehicleRepository.save(bicycle);
        transaction.commit();

        Long vehicleId = bicycle.getId();
        double newPrice = 15.0;

        em.close();

        // When
        vehicleService.updateVehiclePrice(vehicleId, newPrice);

        // Then
        em = emf.createEntityManager();
        vehicleRepository.setEntityManager(em);
        Vehicle updatedVehicle = vehicleRepository.findById(vehicleId).orElse(null);

        assertNotNull(updatedVehicle);
        assertEquals(newPrice, updatedVehicle.getPerHourPrice());
        assertTrue(updatedVehicle instanceof Bicycle);

        em.close();
    }

    @Test
    void updateVehiclePrice_ShouldThrowException_WhenVehicleNotFound() {
        // Given
        Long nonExistentVehicleId = 999999L;
        double newPrice = 50.0;

        // When & Then
        assertThrows(RentalException.class, () -> {
            vehicleService.updateVehiclePrice(nonExistentVehicleId, newPrice);
        });
    }

    @Test
    void updateVehiclePrice_ShouldHandleZeroPrice() throws RentalException {
        // Given
        EntityManager em = emf.createEntityManager();
        vehicleRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Car car = new Car("SK294TJ", 25.5, 1.6, Car.SegmentType.B);
        car = vehicleRepository.save(car);
        transaction.commit();

        Long vehicleId = car.getId();
        em.close();

        // When
        vehicleService.updateVehiclePrice(vehicleId, 0.0);

        // Then
        em = emf.createEntityManager();
        vehicleRepository.setEntityManager(em);
        Vehicle updatedVehicle = vehicleRepository.findById(vehicleId).orElse(null);

        assertNotNull(updatedVehicle);
        assertEquals(0.0, updatedVehicle.getPerHourPrice());

        em.close();
    }

    @Test
    void updateVehiclePrice_ShouldHandleMultipleUpdatesSequentially() throws RentalException {
        // Given
        EntityManager em = emf.createEntityManager();
        vehicleRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Car car = new Car("KR456MN", 30.0, 1.8, Car.SegmentType.C);
        car = vehicleRepository.save(car);
        transaction.commit();

        Long vehicleId = car.getId();
        em.close();

        // When
        vehicleService.updateVehiclePrice(vehicleId, 35.0);
        vehicleService.updateVehiclePrice(vehicleId, 40.0);
        vehicleService.updateVehiclePrice(vehicleId, 45.0);

        // Then
        em = emf.createEntityManager();
        vehicleRepository.setEntityManager(em);
        Vehicle updatedVehicle = vehicleRepository.findById(vehicleId).orElse(null);

        assertNotNull(updatedVehicle);
        assertEquals(45.0, updatedVehicle.getPerHourPrice());

        em.close();
    }

    @Test
    void updateVehiclePrice_ShouldNotAffectOtherVehicleProperties() throws RentalException {
        // Given
        EntityManager em = emf.createEntityManager();
        vehicleRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Car car = new Car("PO123AB", 28.0, 2.0, Car.SegmentType.D);
        car = vehicleRepository.save(car);
        transaction.commit();

        Long vehicleId = car.getId();
        String originalPlate = car.getPlateNumber();
        double originalEngineDisplacement = car.getEngineDisplacement();
        Car.SegmentType originalSegment = car.getSegmentType();
        boolean originalRentedStatus = car.isRented();

        em.close();

        // When
        vehicleService.updateVehiclePrice(vehicleId, 55.0);

        // Then
        em = emf.createEntityManager();
        vehicleRepository.setEntityManager(em);
        Car updatedCar = (Car) vehicleRepository.findById(vehicleId).orElse(null);

        assertNotNull(updatedCar);
        assertEquals(55.0, updatedCar.getPerHourPrice());
        assertEquals(originalPlate, updatedCar.getPlateNumber());
        assertEquals(originalEngineDisplacement, updatedCar.getEngineDisplacement());
        assertEquals(originalSegment, updatedCar.getSegmentType());
        assertEquals(originalRentedStatus, updatedCar.isRented());

        em.close();
    }

    @Test
    void updateVehiclePrice_ShouldWorkWithRentedVehicle() throws RentalException {
        // Given
        EntityManager em = emf.createEntityManager();
        vehicleRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Car car = new Car("GD987ZX", 32.0, 1.9, Car.SegmentType.B);
        car.setRented(true);
        car = vehicleRepository.save(car);
        transaction.commit();

        Long vehicleId = car.getId();
        double newPrice = 38.0;

        em.close();

        // When
        vehicleService.updateVehiclePrice(vehicleId, newPrice);

        // Then
        em = emf.createEntityManager();
        vehicleRepository.setEntityManager(em);
        Vehicle updatedVehicle = vehicleRepository.findById(vehicleId).orElse(null);

        assertNotNull(updatedVehicle);
        assertEquals(newPrice, updatedVehicle.getPerHourPrice());
        assertTrue(updatedVehicle.isRented());

        em.close();
    }

    @Test
    void updateVehiclePrice_ShouldHandleConcurrentUpdates() throws RentalException {
        // Given
        EntityManager em = emf.createEntityManager();
        vehicleRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Car car = new Car("LU654QW", 27.0, 1.7, Car.SegmentType.A);
        car = vehicleRepository.save(car);
        transaction.commit();

        Long vehicleId = car.getId();
        em.close();

        // When - Simulate concurrent update by manually modifying vehicle
        EntityManager em2 = emf.createEntityManager();
        EntityTransaction transaction2 = em2.getTransaction();
        transaction2.begin();
        Vehicle vehicleToModify = em2.find(Vehicle.class, vehicleId);
        vehicleToModify.setPerHourPrice(100.0); // Modify directly
        em2.flush();
        transaction2.commit();
        em2.close();

        assertDoesNotThrow(() -> {
            vehicleService.updateVehiclePrice(vehicleId, 75.0);
        });

        // Then - Verify final state
        EntityManager em3 = emf.createEntityManager();
        vehicleRepository.setEntityManager(em3);
        Vehicle finalVehicle = vehicleRepository.findById(vehicleId).orElse(null);
        assertNotNull(finalVehicle);
        assertEquals(75.0, finalVehicle.getPerHourPrice());
        em3.close();
    }

    @Test
    void updateVehiclePrice_ShouldHandleDecimalPrecision() throws RentalException {
        // Given
        EntityManager em = emf.createEntityManager();
        vehicleRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Bicycle bicycle = new Bicycle("BL33002WR", 10.0);
        bicycle = vehicleRepository.save(bicycle);
        transaction.commit();

        Long vehicleId = bicycle.getId();
        double precisePrice = 12.345;

        em.close();

        // When
        vehicleService.updateVehiclePrice(vehicleId, precisePrice);

        // Then
        em = emf.createEntityManager();
        vehicleRepository.setEntityManager(em);
        Vehicle updatedVehicle = vehicleRepository.findById(vehicleId).orElse(null);

        assertNotNull(updatedVehicle);
        assertEquals(precisePrice, updatedVehicle.getPerHourPrice(), 0.001);

        em.close();
    }
}