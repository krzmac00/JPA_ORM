package RepositoriesTests;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import org.example.entities.Client;
import org.example.repositories.ClientRepository;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ClientRepositoryTest {
    private static EntityManagerFactory emf;
    private static ClientRepository clientRepository;
    private static EntityTransaction transaction;

    @BeforeEach
    void initTest() {
        emf = Persistence.createEntityManagerFactory("default");
        EntityManager entityManager = emf.createEntityManager();
        clientRepository = new ClientRepository(entityManager);
        transaction = entityManager.getTransaction();
    }

    @AfterEach
    void endTest() {
        if(emf != null) emf.close();
    }

    @Test
    void saveTest() {
        Client client = new Client("0028965431","John", "Doe");

        transaction.begin();
        Client saved = clientRepository.save(client);
        transaction.commit();

        assertSame(client, saved);
        assertEquals(client.getId(), saved.getId());
        assertEquals(client.getPersonalId(), saved.getPersonalId());
        assertEquals(client.getFirstName(), saved.getFirstName());
        assertEquals(client.getLastName(), saved.getLastName());
    }

    @Test
    void saveAllFindAllTest() {
        assertEquals(0, clientRepository.count());
        List<Client> clients = new ArrayList<>();
        Client client1 = new Client("0028965431","John", "Doe");
        Client client2 = new Client("0321234531","Adam", "Smith");
        Client client3 = new Client("9902113203","Robert", "Meyers");
        clients.add(client1);
        clients.add(client2);
        clients.add(client3);

        transaction.begin();
        clients = clientRepository.saveAll(clients);
        transaction.commit();
        assertEquals(3, clientRepository.count());

        transaction.begin();
        List<Client> savedClients = clientRepository.findAll();
        transaction.commit();

        int i = 0;
        for (Client client : clients) {
            assertEquals(client.getId(), savedClients.get(i).getId());
            assertEquals(client.getPersonalId(), savedClients.get(i).getPersonalId());
            assertEquals(client.getFirstName(), savedClients.get(i).getFirstName());
            assertEquals(client.getLastName(), savedClients.get(i).getLastName());
            i++;
        }
    }

    @Test
    void findByIDTest() {
        Client client = new Client("0028965431","John", "Doe");

        transaction.begin();
        Client saved = clientRepository.save(client);
        transaction.commit();

        if (clientRepository.findById(saved.getId()).isPresent()) {
            Client found = clientRepository.findById(saved.getId()).get();
            assertSame(client, found);
        } else {
            fail("Client not found");
        }
    }

    @Test
    void findAllByIDTest() {
        List<Client> clients = new ArrayList<>();
        List<Long> clientsIds = new ArrayList<>();
        Client client1 = new Client("0028965431","John", "Doe");
        Client client2 = new Client("0321234531","Adam", "Smith");
        Client client3 = new Client("9902113203","Robert", "Meyers");
        clients.add(client1);
        clients.add(client2);
        clients.add(client3);

        transaction.begin();
        clientRepository.save(client1);
        clientRepository.save(client2);
        clientRepository.save(client3);
        transaction.commit();

        clientsIds.add(client1.getId());
        clientsIds.add(client2.getId());
        clientsIds.add(client3.getId());

        assertEquals(3, clientRepository.count());
        List<Client> foundClients = clientRepository.findAllById(clientsIds);
        int i = 0;
        for (Client client : foundClients) {
            assertEquals(client.getId(), clients.get(i).getId());
            assertEquals(client.getPersonalId(), clients.get(i).getPersonalId());
            assertEquals(client.getFirstName(), clients.get(i).getFirstName());
            assertEquals(client.getLastName(), clients.get(i).getLastName());
            i++;
        }
    }

    @Test
    void deleteTest() {
        Client client1 = new Client("0028965431","John", "Doe");

        transaction.begin();
        clientRepository.save(client1);
        transaction.commit();
        assertEquals(1, clientRepository.count());

        transaction.begin();
        clientRepository.delete(client1);
        transaction.commit();

        assertEquals(0, clientRepository.count());
    }

    @Test
    void deleteByIdTest() {
        Client client1 = new Client("0028965431","John", "Doe");

        transaction.begin();
        clientRepository.save(client1);
        transaction.commit();
        assertEquals(1, clientRepository.count());

        transaction.begin();
        clientRepository.deleteById(client1.getId());
        transaction.commit();
        assertEquals(0, clientRepository.count());
    }

    @Test
    void deleteAllClientsTest() {
        List<Client> clients = new ArrayList<>();
        Client client1 = new Client("0028965431","John", "Doe");
        Client client2 = new Client("0321234531","Adam", "Smith");
        Client client3 = new Client("9902113203","Robert", "Meyers");
        clients.add(client1);
        clients.add(client2);
        clients.add(client3);

        transaction.begin();
        clientRepository.save(client1);
        clientRepository.save(client2);
        clientRepository.save(client3);
        transaction.commit();
        assertEquals(3, clientRepository.count());

        transaction.begin();
        clientRepository.deleteAll(clients);
        transaction.commit();
        assertEquals(0, clientRepository.count());
    }

    @Test
    void deleteAllByIdTest() {
        List<Long> ids = new ArrayList<>();
        Client client1 = new Client("0028965431","John", "Doe");
        Client client2 = new Client("0321234531","Adam", "Smith");
        Client client3 = new Client("9902113203","Robert", "Meyers");

        transaction.begin();
        clientRepository.save(client1);
        clientRepository.save(client2);
        clientRepository.save(client3);
        transaction.commit();

        ids.add(client1.getId());
        ids.add(client2.getId());
        ids.add(client3.getId());
        assertEquals(3, clientRepository.count());

        transaction.begin();
        clientRepository.deleteAllById(ids);
        transaction.commit();
        assertEquals(0, clientRepository.count());
    }

    @Test
    void deleteAllTest() {
        Client client1 = new Client("0028965431","John", "Doe");
        Client client2 = new Client("0321234531","Adam", "Smith");
        Client client3 = new Client("9902113203","Robert", "Meyers");

        transaction.begin();
        clientRepository.save(client1);
        clientRepository.save(client2);
        clientRepository.save(client3);
        transaction.commit();
        assertEquals(3, clientRepository.count());

        transaction.begin();
        clientRepository.deleteAll();
        transaction.commit();
        assertEquals(0, clientRepository.count());
    }

    @Test
    void existsByIdTest() {
        Client client = new Client("0028965431","John", "Doe");

        transaction.begin();
        Client saved = clientRepository.save(client);
        transaction.commit();

        assertTrue(clientRepository.existsById(saved.getId()));
    }
}
