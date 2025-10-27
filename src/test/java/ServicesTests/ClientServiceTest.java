package ServicesTests;

import jakarta.persistence.*;
import org.example.entities.Client;
import org.example.repositories.ClientRepository;
import org.example.services.ClientService;
import org.example.util.Address;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class ClientServiceTest {
    private static EntityManagerFactory emf;
    private static ClientService clientService;
    private static ClientRepository clientRepository;

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
        clientService = new ClientService(emf, clientRepository);
        entityManager.close();
    }

    @Test
    void changeMaxVehicles_ShouldUpdateClientMaxVehicles() {
        // Given
        EntityManager em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Client client = new Client("0028965431", "John", "Doe");
        client = clientRepository.save(client);
        transaction.commit();

        Long clientId = client.getId();
        int initialMaxVehicles = client.getMaxVehicles();
        int newMaxVehicles = 5;

        em.close();

        // When
        clientService.changeMaxVehicles(clientId, newMaxVehicles);

        // Then
        em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        Client updatedClient = clientRepository.findById(clientId).orElse(null);

        assertNotNull(updatedClient);
        assertEquals(newMaxVehicles, updatedClient.getMaxVehicles());
        assertNotEquals(initialMaxVehicles, updatedClient.getMaxVehicles());

        em.close();
    }

    @Test
    void changeMaxVehicles_ShouldThrowException_WhenClientNotFound() {
        // Given
        Long nonExistentClientId = 999999L;
        int newMaxVehicles = 5;

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            clientService.changeMaxVehicles(nonExistentClientId, newMaxVehicles);
        });
    }

    @Test
    void changeMaxVehicles_ShouldThrowConflictException_WhenConcurrentModificationOccurs() throws InterruptedException {
        // Given
        EntityManager em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Client client = new Client("0321234531", "Adam", "Smith");
        client = clientRepository.save(client);
        transaction.commit();

        Long clientId = client.getId();
        long initialVersion = client.getVersion();
        em.close();

        // When - First transaction starts and loads the client
        EntityManager em1 = emf.createEntityManager();
        EntityTransaction transaction1 = em1.getTransaction();
        transaction1.begin();
        Client client1 = em1.find(Client.class, clientId, LockModeType.OPTIMISTIC);
        long version1 = client1.getVersion();
        assertEquals(initialVersion, version1);

        // Second transaction completes successfully (simulating concurrent modification)
        EntityManager em2 = emf.createEntityManager();
        EntityTransaction transaction2 = em2.getTransaction();
        transaction2.begin();
        Client client2 = em2.find(Client.class, clientId, LockModeType.OPTIMISTIC);
        client2.setMaxVehicles(10);
        transaction2.commit();
        em2.close();

        // Try to modify and commit first transaction - should fail with OptimisticLockException
        client1.setMaxVehicles(7);

        // Then - First transaction should fail during commit
        OptimisticLockException exception = assertThrows(OptimisticLockException.class, () -> {
            em1.flush(); // This will trigger the optimistic lock check
            transaction1.commit();
        });

        if (transaction1.isActive()) {
            transaction1.rollback();
        }
        em1.close();

        // Verify that second transaction's changes persisted
        EntityManager em3 = emf.createEntityManager();
        clientRepository.setEntityManager(em3);
        Client finalClient = clientRepository.findById(clientId).orElse(null);
        assertNotNull(finalClient);
        assertEquals(10, finalClient.getMaxVehicles());
        assertTrue(finalClient.getVersion() > initialVersion);
        em3.close();
    }

    @Test
    void changeMaxVehicles_ShouldThrowConflictException_OnOptimisticLockFailure() throws InterruptedException {
        // Given
        EntityManager em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Client client = new Client("1231231231", "Conflict", "Test");
        client = clientRepository.save(client);
        transaction.commit();

        Long clientId = client.getId();
        em.close();

        // When - Create a competing modification that will cause conflict
        Thread concurrentThread = new Thread(() -> {
            try {
                Thread.sleep(100); // Small delay to ensure main thread starts first
                try (EntityManager emConcurrent = emf.createEntityManager()) {
                    EntityTransaction txConcurrent = emConcurrent.getTransaction();
                    txConcurrent.begin();
                    Client clientConcurrent = emConcurrent.find(Client.class, clientId);
                    clientConcurrent.setMaxVehicles(99);
                    emConcurrent.flush();
                    txConcurrent.commit();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Start the service call
        try (EntityManager emMain = emf.createEntityManager()) {
            ClientRepository mainRepo = new ClientRepository(emMain);
            EntityTransaction txMain = emMain.getTransaction();

            txMain.begin();
            Client clientMain = mainRepo.lockedFindById(clientId, LockModeType.OPTIMISTIC).orElseThrow();

            // Start concurrent modification
            concurrentThread.start();
            concurrentThread.join(); // Wait for concurrent modification to complete

            // Now try to modify and commit - should detect version conflict
            clientMain.setMaxVehicles(50);

            // Then - Should throw OptimisticLockException on flush/commit
            assertThrows(OptimisticLockException.class, () -> {
                emMain.flush();
                txMain.commit();
            });

            if (txMain.isActive()) {
                txMain.rollback();
            }
        }

        // Verify the concurrent modification won
        try (EntityManager emVerify = emf.createEntityManager()) {
            clientRepository.setEntityManager(emVerify);
            Client finalClient = clientRepository.findById(clientId).orElse(null);
            assertNotNull(finalClient);
            assertEquals(99, finalClient.getMaxVehicles());
        }
    }

    @Test
    void changeMaxVehicles_ServiceShouldHandleOptimisticLockCorrectly() throws InterruptedException {
        // Given
        EntityManager em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Client client = new Client("9879879879", "Service", "Test");
        client = clientRepository.save(client);
        transaction.commit();

        Long clientId = client.getId();
        em.close();

        // When - Service successfully updates when no conflict
        assertDoesNotThrow(() -> {
            clientService.changeMaxVehicles(clientId, 5);
        });

        // Then - Verify the change was applied
        try (EntityManager emVerify = emf.createEntityManager()) {
            clientRepository.setEntityManager(emVerify);
            Client updatedClient = clientRepository.findById(clientId).orElse(null);
            assertNotNull(updatedClient);
            assertEquals(5, updatedClient.getMaxVehicles());
        }

        // When - Multiple rapid updates from service (tests retry logic)
        assertDoesNotThrow(() -> {
            clientService.changeMaxVehicles(clientId, 10);
            clientService.changeMaxVehicles(clientId, 15);
            clientService.changeMaxVehicles(clientId, 20);
        });

        // Then - Final value should be the last update
        try (EntityManager emFinal = emf.createEntityManager()) {
            clientRepository.setEntityManager(emFinal);
            Client finalClient = clientRepository.findById(clientId).orElse(null);
            assertNotNull(finalClient);
            assertEquals(20, finalClient.getMaxVehicles());
        }
    }

    @Test
    void changeMaxVehicles_ShouldHandleMultipleUpdatesSequentially() {
        // Given
        EntityManager em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Client client = new Client("9902113203", "Robert", "Meyers");
        client = clientRepository.save(client);
        transaction.commit();

        Long clientId = client.getId();
        em.close();

        // When
        clientService.changeMaxVehicles(clientId, 3);
        clientService.changeMaxVehicles(clientId, 4);
        clientService.changeMaxVehicles(clientId, 1);

        // Then
        em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        Client updatedClient = clientRepository.findById(clientId).orElse(null);

        assertNotNull(updatedClient);
        assertEquals(1, updatedClient.getMaxVehicles());

        em.close();
    }

    @Test
    void changeMaxVehicles_ShouldWorkWithMinimalValue() {
        // Given
        EntityManager em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Client client = new Client("1234567890", "Test", "User");
        client = clientRepository.save(client);
        transaction.commit();

        Long clientId = client.getId();
        em.close();

        // When - Set to minimum allowed value (1)
        clientService.changeMaxVehicles(clientId, 1);

        // Then
        em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        Client updatedClient = clientRepository.findById(clientId).orElse(null);

        assertNotNull(updatedClient);
        assertEquals(1, updatedClient.getMaxVehicles());

        em.close();
    }

    @Test
    void changeMaxVehicles_ShouldWorkWithHighValue() {
        // Given
        EntityManager em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Client client = new Client("9876543210", "Premium", "Customer",
                new Address("Main St", "100", "Warsaw", "00-001"));
        client = clientRepository.save(client);
        transaction.commit();

        Long clientId = client.getId();
        em.close();

        // When - Set to high value
        clientService.changeMaxVehicles(clientId, 100);

        // Then
        em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        Client updatedClient = clientRepository.findById(clientId).orElse(null);

        assertNotNull(updatedClient);
        assertEquals(100, updatedClient.getMaxVehicles());

        em.close();
    }

    @Test
    void changeMaxVehicles_ShouldNotAffectOtherClientProperties() {
        // Given
        EntityManager em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        EntityTransaction transaction = em.getTransaction();

        transaction.begin();
        Client client = new Client("5555555555", "Jane", "Doe",
                new Address("Test St", "42", "Lodz", "90-001"));
        client.addMoneySpent(15000);
        client = clientRepository.save(client);
        transaction.commit();

        Long clientId = client.getId();
        String originalFirstName = client.getFirstName();
        String originalLastName = client.getLastName();
        double originalDiscount = client.getDiscount();
        double originalMoneySpent = client.getMoneySpent();

        em.close();

        // When
        clientService.changeMaxVehicles(clientId, 8);

        // Then
        em = emf.createEntityManager();
        clientRepository.setEntityManager(em);
        Client updatedClient = clientRepository.findById(clientId).orElse(null);

        assertNotNull(updatedClient);
        assertEquals(8, updatedClient.getMaxVehicles());
        assertEquals(originalFirstName, updatedClient.getFirstName());
        assertEquals(originalLastName, updatedClient.getLastName());
        assertEquals(originalDiscount, updatedClient.getDiscount());
        assertEquals(originalMoneySpent, updatedClient.getMoneySpent());

        em.close();
    }
}