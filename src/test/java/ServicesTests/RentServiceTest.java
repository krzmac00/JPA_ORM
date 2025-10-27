package ServicesTests;

import jakarta.persistence.*;
import org.example.entities.Client;
import org.example.entities.Rent;
import org.example.entities.Vehicle;
import org.example.entities.vehicles.Bicycle;
import org.example.entities.vehicles.Car;
import org.example.exceptions.RentalException;
import org.example.repositories.ClientRepository;
import org.example.repositories.RentRepository;
import org.example.repositories.VehicleRepository;
import org.example.services.RentService;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RentServiceTest {
    private static EntityManagerFactory emf;
    private static RentService rentService;
    private static ClientRepository clientRepository;
    private static VehicleRepository vehicleRepository;
    private static RentRepository rentRepository;

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
        clientRepository = new ClientRepository(entityManager);
        vehicleRepository = new VehicleRepository(entityManager);
        rentRepository = new RentRepository(entityManager);
        rentService = new RentService(emf, clientRepository, vehicleRepository, rentRepository);
        entityManager.close();
    }

    @Test
    void rentVehicle_ShouldCreateRentSuccessfully() throws RentalException {
        // Given
        EntityManager em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        vehicleRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Client client = new Client("0028965431", "John", "Doe");
        client = clientRepository.save(client);
        Car car = new Car("EL9GM78", 25.5, 1.6, Car.SegmentType.A);
        car = vehicleRepository.save(car);
        transaction.commit();

        Long clientId = client.getId();
        Long vehicleId = car.getId();
        LocalDateTime beginTime = LocalDateTime.now();

        em.close();

        // When
        Rent rent = rentService.rentVehicle(clientId, vehicleId, beginTime);

        // Then
        assertNotNull(rent);
        assertNotNull(rent.getId());
        assertEquals(clientId, rent.getClient().getId());
        assertEquals(vehicleId, rent.getVehicle().getId());
        assertEquals(beginTime, rent.getBeginTime());
        assertEquals(25.5, rent.getVehiclePerHourPrice());
        assertFalse(rent.isArchived());

        // Verify vehicle is marked as rented
        em = emf.createEntityManager();
        vehicleRepository.setEntityManager(em);
        Vehicle rentedVehicle = vehicleRepository.findById(vehicleId).orElse(null);
        assertNotNull(rentedVehicle);
        assertTrue(rentedVehicle.isRented());

        em.close();
    }

    @Test
    void rentVehicle_ShouldThrowException_WhenClientNotFound() {
        // Given
        EntityManager em = emf.createEntityManager();
        vehicleRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Car car = new Car("SK294TJ", 30.0, 1.8, Car.SegmentType.B);
        car = vehicleRepository.save(car);
        transaction.commit();

        Long nonExistentClientId = 999999L;
        Long vehicleId = car.getId();
        LocalDateTime beginTime = LocalDateTime.now();

        em.close();

        // When & Then
        assertThrows(RentalException.class, () -> {
            rentService.rentVehicle(nonExistentClientId, vehicleId, beginTime);
        });
    }

    @Test
    void rentVehicle_ShouldThrowException_WhenVehicleNotFound() {
        // Given
        EntityManager em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Client client = new Client("0321234531", "Adam", "Smith");
        client = clientRepository.save(client);
        transaction.commit();

        Long clientId = client.getId();
        Long nonExistentVehicleId = 999999L;
        LocalDateTime beginTime = LocalDateTime.now();

        em.close();

        // When & Then
        assertThrows(RentalException.class, () -> {
            rentService.rentVehicle(clientId, nonExistentVehicleId, beginTime);
        });
    }

    @Test
    void rentVehicle_ShouldThrowException_WhenVehicleAlreadyRented() throws RentalException {
        // Given
        EntityManager em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        vehicleRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Client client1 = new Client("1111111111", "First", "Client");
        Client client2 = new Client("2222222222", "Second", "Client");
        client1 = clientRepository.save(client1);
        client2 = clientRepository.save(client2);

        Car car = new Car("WA789KL", 28.0, 1.7, Car.SegmentType.A);
        car = vehicleRepository.save(car);
        transaction.commit();

        Long client1Id = client1.getId();
        Long client2Id = client2.getId();
        Long vehicleId = car.getId();
        LocalDateTime beginTime = LocalDateTime.now();

        em.close();

        // First rental should succeed
        Rent rent1 = rentService.rentVehicle(client1Id, vehicleId, beginTime);
        assertNotNull(rent1);

        // When & Then - Second rental should fail
        assertThrows(RentalException.class, () -> {
            rentService.rentVehicle(client2Id, vehicleId, beginTime);
        });
    }

    @Test
    void rentVehicle_ShouldThrowException_WhenClientExceedsMaxVehicles() throws RentalException {
        // Given
        EntityManager em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        vehicleRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Client client = new Client("3333333333", "Test", "Client");
        client.setMaxVehicles(1);
        client = clientRepository.save(client);

        Car car1 = new Car("KR456MN", 25.0, 1.6, Car.SegmentType.A);
        Car car2 = new Car("PO123AB", 27.0, 1.8, Car.SegmentType.B);
        car1 = vehicleRepository.save(car1);
        car2 = vehicleRepository.save(car2);
        transaction.commit();

        Long clientId = client.getId();
        Long vehicle1Id = car1.getId();
        Long vehicle2Id = car2.getId();
        LocalDateTime beginTime = LocalDateTime.now();

        em.close();

        // Then - First rental should succeed
        Rent rent1 = rentService.rentVehicle(clientId, vehicle1Id, beginTime);
        assertNotNull(rent1);

        // When & Then - Second rental should fail due to max vehicles limit
        assertThrows(RentalException.class, () -> {
            rentService.rentVehicle(clientId, vehicle2Id, beginTime);
        });
    }

    @Test
    void endRental_ShouldCompleteRentalSuccessfully() throws RentalException {
        // Given
        EntityManager em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        vehicleRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Client client = new Client("4444444444", "End", "Rental");
        client = clientRepository.save(client);

        Bicycle bicycle = new Bicycle("BK30011LD", 12.5);
        bicycle = vehicleRepository.save(bicycle);
        transaction.commit();

        Long clientId = client.getId();
        Long vehicleId = bicycle.getId();
        LocalDateTime beginTime = LocalDateTime.now().minusHours(3);
        LocalDateTime endTime = LocalDateTime.now();

        em.close();

        Rent rent = rentService.rentVehicle(clientId, vehicleId, beginTime);
        Long rentId = rent.getId();

        // When
        Rent endedRent = rentService.endRental(rentId, endTime);

        // Then
        assertNotNull(endedRent);
        assertEquals(rentId, endedRent.getId());
        assertNotNull(endedRent.getEndTime());
        assertEquals(endTime, endedRent.getEndTime());
        assertTrue(endedRent.isArchived());
        assertTrue(endedRent.getTotalCost() > 0);

        // Verify vehicle is no longer rented
        em = emf.createEntityManager();
        vehicleRepository.setEntityManager(em);
        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElse(null);
        assertNotNull(vehicle);
        assertFalse(vehicle.isRented());

        em.close();
    }

    @Test
    void endRental_ShouldThrowException_WhenRentNotFound() {
        // Given
        Long nonExistentRentId = 999999L;
        LocalDateTime endTime = LocalDateTime.now();

        // When & Then
        assertThrows(RentalException.class, () -> {
            rentService.endRental(nonExistentRentId, endTime);
        });
    }

    @Test
    void endRental_ShouldThrowException_WhenRentAlreadyArchived() throws RentalException {
        // Given
        EntityManager em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        vehicleRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Client client = new Client("5555555555", "Archive", "Test");
        client = clientRepository.save(client);

        Car car = new Car("GD987ZX", 30.0, 2.0, Car.SegmentType.C);
        car = vehicleRepository.save(car);
        transaction.commit();

        Long clientId = client.getId();
        Long vehicleId = car.getId();
        LocalDateTime beginTime = LocalDateTime.now().minusHours(5);
        LocalDateTime endTime = LocalDateTime.now();

        em.close();

        Rent rent = rentService.rentVehicle(clientId, vehicleId, beginTime);
        Long rentId = rent.getId();

        // End the rental first time
        rentService.endRental(rentId, endTime);

        // When & Then - Try to end it again
        assertThrows(RentalException.class, () -> {
            rentService.endRental(rentId, endTime.plusHours(1));
        });
    }

    @Test
    void endRental_ShouldCalculateCostCorrectly() throws RentalException {
        // Given
        EntityManager em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        vehicleRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Client client = new Client("6666666666", "Cost", "Test");
        client = clientRepository.save(client);

        Car car = new Car("LU654QW", 20.0, 1.5, Car.SegmentType.A); // 20 PLN per hour
        car = vehicleRepository.save(car);
        transaction.commit();

        Long clientId = client.getId();
        Long vehicleId = car.getId();
        LocalDateTime beginTime = LocalDateTime.now().minusHours(5);
        LocalDateTime endTime = LocalDateTime.now();

        em.close();

        Rent rent = rentService.rentVehicle(clientId, vehicleId, beginTime);
        Long rentId = rent.getId();

        // When
        Rent endedRent = rentService.endRental(rentId, endTime);

        // Then - 5 hours at 20 PLN/hour = 100 PLN (no discount)
        assertEquals(100.0, endedRent.getTotalCost(), 0.01);
    }

    @Test
    void endRental_ShouldApplyDiscount_WhenClientHasDiscount() throws RentalException {
        // Given
        EntityManager em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        vehicleRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Client client = new Client("7777777777", "Discount", "Client");
        client.addMoneySpent(15000); // Should give 1% discount
        client = clientRepository.save(client);

        Bicycle bicycle = new Bicycle("BL33002WR", 10.0); // 10 PLN per hour
        bicycle = vehicleRepository.save(bicycle);
        transaction.commit();

        Long clientId = client.getId();
        Long vehicleId = bicycle.getId();
        LocalDateTime beginTime = LocalDateTime.now().minusHours(10);
        LocalDateTime endTime = LocalDateTime.now();

        em.close();

        Rent rent = rentService.rentVehicle(clientId, vehicleId, beginTime);
        Long rentId = rent.getId();

        // When
        Rent endedRent = rentService.endRental(rentId, endTime);

        // Then - 10 hours at 10 PLN/hour = 100 PLN, with 1% discount = 99 PLN
        assertEquals(99.0, endedRent.getTotalCost(), 0.01);

        // Verify client's money spent increased
        em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        Client updatedClient = clientRepository.findById(clientId).orElse(null);
        assertNotNull(updatedClient);
        assertEquals(15099.0, updatedClient.getMoneySpent(), 0.01);

        em.close();
    }

    @Test
    void rentVehicle_ShouldAllowMultipleRentalsWithinLimit() throws RentalException {
        // Given
        EntityManager em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        vehicleRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Client client = new Client("8888888888", "Multi", "Rental");
        client.setMaxVehicles(3);
        client = clientRepository.save(client);

        Car car1 = new Car("MR001AA", 20.0, 1.6, Car.SegmentType.A);
        Car car2 = new Car("MR002BB", 25.0, 1.8, Car.SegmentType.B);
        Car car3 = new Car("MR003CC", 30.0, 2.0, Car.SegmentType.C);
        car1 = vehicleRepository.save(car1);
        car2 = vehicleRepository.save(car2);
        car3 = vehicleRepository.save(car3);
        transaction.commit();

        Long clientId = client.getId();
        LocalDateTime beginTime = LocalDateTime.now();

        em.close();

        // When - Rent three vehicles
        Rent rent1 = rentService.rentVehicle(clientId, car1.getId(), beginTime);
        Rent rent2 = rentService.rentVehicle(clientId, car2.getId(), beginTime);
        Rent rent3 = rentService.rentVehicle(clientId, car3.getId(), beginTime);

        // Then
        assertNotNull(rent1);
        assertNotNull(rent2);
        assertNotNull(rent3);

        // Verify all three vehicles are rented
        em = emf.createEntityManager();
        vehicleRepository.setEntityManager(em);
        assertTrue(vehicleRepository.findById(car1.getId()).get().isRented());
        assertTrue(vehicleRepository.findById(car2.getId()).get().isRented());
        assertTrue(vehicleRepository.findById(car3.getId()).get().isRented());
        em.close();
    }

    @Test
    void isVehicleAvailable_ShouldReturnTrue_WhenVehicleNotRented() {
        // Given
        EntityManager em = emf.createEntityManager();
        vehicleRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Car car = new Car("AV001XY", 25.0, 1.7, Car.SegmentType.A);
        car = vehicleRepository.save(car);
        transaction.commit();

        Long vehicleId = car.getId();

        // When - Check availability with same EntityManager
        boolean available = rentService.isVehicleAvailable(vehicleId);

        // Then
        assertTrue(available);

        em.close();
    }

    @Test
    void isVehicleAvailable_ShouldReturnFalse_WhenVehicleRented() throws RentalException {
        // Given
        EntityManager em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        vehicleRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Client client = new Client("9999999999", "Avail", "Test");
        client = clientRepository.save(client);

        Car car = new Car("AV002YZ", 28.0, 1.9, Car.SegmentType.B);
        car = vehicleRepository.save(car);
        transaction.commit();

        Long clientId = client.getId();
        Long vehicleId = car.getId();
        em.close();

        // Rent the vehicle
        rentService.rentVehicle(clientId, vehicleId, LocalDateTime.now());

        // When - Check availability with fresh EntityManager
        EntityManager em2 = emf.createEntityManager();
        vehicleRepository.setEntityManager(em2);
        boolean available = rentService.isVehicleAvailable(vehicleId);

        // Then
        assertFalse(available);

        em2.close();
    }

    @Test
    void getActiveRentalsForClient_ShouldReturnActiveRentals() throws RentalException {
        // Given
        EntityManager em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        vehicleRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Client client = new Client("1010101010", "Active", "Rentals");
        client.setMaxVehicles(2);
        client = clientRepository.save(client);

        Car car1 = new Car("AR001ZZ", 20.0, 1.6, Car.SegmentType.A);
        Car car2 = new Car("AR002WW", 25.0, 1.8, Car.SegmentType.B);
        car1 = vehicleRepository.save(car1);
        car2 = vehicleRepository.save(car2);
        transaction.commit();

        Long clientId = client.getId();
        em.close();

        // Create two active rentals
        rentService.rentVehicle(clientId, car1.getId(), LocalDateTime.now());
        rentService.rentVehicle(clientId, car2.getId(), LocalDateTime.now());

        // When
        em = emf.createEntityManager();
        List<Rent> activeRentals = rentService.getActiveRentalsForClient(clientId, em);

        // Then
        assertNotNull(activeRentals);
        assertEquals(2, activeRentals.size());

        em.close();
    }

    @Test
    void getArchivedRentals_ShouldReturnOnlyArchivedRentals() throws RentalException {
        // Given
        EntityManager em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        vehicleRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Client client = new Client("1212121212", "Archived", "Test");
        client = clientRepository.save(client);

        Car car = new Car("ARC01VV", 22.0, 1.5, Car.SegmentType.A);
        car = vehicleRepository.save(car);
        transaction.commit();

        Long clientId = client.getId();
        Long vehicleId = car.getId();
        em.close();

        // Create and end a rental
        Rent rent = rentService.rentVehicle(clientId, vehicleId, LocalDateTime.now().minusHours(2));
        rentService.endRental(rent.getId(), LocalDateTime.now());

        // When
        em = emf.createEntityManager();
        List<Rent> archivedRentals = rentService.getArchivedRentals(em);

        // Then
        assertNotNull(archivedRentals);
        assertTrue(archivedRentals.size() > 0);
        assertTrue(archivedRentals.stream().allMatch(Rent::isArchived));

        em.close();
    }

    @Test
    void endRental_ShouldUpdateClientMoneySpent() throws RentalException {
        // Given
        EntityManager em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        vehicleRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Client client = new Client("1313131313", "Money", "Spent");
        double initialMoneySpent = client.getMoneySpent();
        client = clientRepository.save(client);

        Car car = new Car("MS001UU", 50.0, 2.5, Car.SegmentType.E);
        car = vehicleRepository.save(car);
        transaction.commit();

        Long clientId = client.getId();
        Long vehicleId = car.getId();
        em.close();

        // Create rental
        Rent rent = rentService.rentVehicle(clientId, vehicleId, LocalDateTime.now().minusHours(4));

        // When
        Rent endedRent = rentService.endRental(rent.getId(), LocalDateTime.now());
        double rentalCost = endedRent.getTotalCost();

        // Then
        em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        Client updatedClient = clientRepository.findById(clientId).orElse(null);
        assertNotNull(updatedClient);
        assertEquals(initialMoneySpent + rentalCost, updatedClient.getMoneySpent(), 0.01);

        em.close();
    }
}