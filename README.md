# MKW ORM — Car Rental Practice Project

A car rental management system built to practice **Object-Relational Mapping (ORM)** using **Jakarta Persistence 3.0 (JPA)** with **Hibernate 6** as the persistence provider. The project covers a range of JPA patterns — from entity inheritance and embedded types, through transaction management and optimistic locking, to a service layer with retry logic.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Domain Model](#domain-model)
- [ORM Patterns Demonstrated](#orm-patterns-demonstrated)
- [Project Structure](#project-structure)
- [Database Configuration](#database-configuration)
- [Running the Project](#running-the-project)
- [Test Suite](#test-suite)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 23 |
| Persistence API | Jakarta Persistence 3.2.0 |
| ORM Provider | Hibernate Core 6.6.1.Final |
| Validation | Hibernate Validator 8 + Jakarta EL 4 |
| Database | PostgreSQL 42.7.4 |
| Logging | SLF4J 2 + Log4j2 2.24 |
| Testing | JUnit Jupiter 5 |
| Build | Maven |

---

## Domain Model

### Entities

#### `Vehicle` (abstract, JOINED inheritance base)
The root of the vehicle hierarchy. Stored in the `vehicle` table with a `type` discriminator column.

| Field | Type | Notes |
|---|---|---|
| `id` | Long | PK, identity-generated |
| `plateNumber` | String | Unique, 4–10 chars |
| `perHourPrice` | double | ≥ 0 |
| `isRented` | boolean | Updated on rent start/end |
| `version` | long | Optimistic lock version |

**Subclasses (JOINED strategy — each gets its own table):**

- **`Car`** — adds `engineDisplacement` (double) and `segmentType` (enum: A–E). Table: `car`, FK: `car_id → vehicle.id`.
- **`Bicycle`** — no additional fields. Table: `bicycle`, FK: `bicycle_id → vehicle.id`.

#### `Client`
Represents a customer. Embeds an `Address` value object.

| Field | Type | Notes |
|---|---|---|
| `id` | Long | PK, identity-generated |
| `personalId` | String | Polish PESEL |
| `firstName` / `lastName` | String | @NotNull |
| `address` | Address | @Embeddable, columns inlined into `client` table |
| `maxVehicles` | int | Max concurrent rentals (min 1, default 2) |
| `discount` | double | 0–5%, calculated from `moneySpent` |
| `moneySpent` | double | Cumulative rental spend, drives loyalty discount |
| `currentRents` | List\<Rent\> | @OneToMany, LAZY, cascade PERSIST/MERGE |
| `version` | long | Optimistic lock version |

Discount tiers: 1% at 10k PLN, 2% at 20k, 3% at 30k, 4% at 40k, 5% at 50k+.

#### `Rent`
A rental record linking a `Client` to a `Vehicle` for a time range.

| Field | Type | Notes |
|---|---|---|
| `id` | Long | PK, identity-generated |
| `beginTime` | LocalDateTime | Immutable after creation |
| `endTime` | LocalDateTime | Null until rental ends |
| `vehiclePerHourPrice` | double | Snapshot of price at start (historical accuracy) |
| `totalCost` | double | Computed on `endRental`, discount-adjusted |
| `client` | Client | @ManyToOne, cascade PERSIST |
| `vehicle` | Vehicle | @ManyToOne, cascade PERSIST |
| `archived` | boolean | Set to true when rental completes |

A `@PrePersist` / `@PreUpdate` callback validates that `beginTime < endTime`.

#### `Address` (@Embeddable)
A value object embedded directly into the `client` table.

Fields: `streetName`, `streetNumber`, `city`, `postalCode` (format `XX-YYY`).

---

## ORM Patterns Demonstrated

### 1. JOINED Inheritance Strategy
`Vehicle`, `Car`, and `Bicycle` use `@Inheritance(strategy = JOINED)`. Each subclass has its own table containing only its additional columns, linked to the `vehicle` base table by a shared primary key foreign key. This avoids null columns (TABLE_PER_CLASS) and avoids large joins (SINGLE_TABLE).

### 2. Embeddable / Embedded Types
`Address` is annotated `@Embeddable` and stored directly in the `client` table via `@Embedded`. No join needed; the value type has no identity of its own.

### 3. Optimistic Locking with @Version
`Client` and `Vehicle` carry a `version` field annotated with `@Version`. Hibernate increments this on each update and checks it on write; if another transaction has already modified the row, an `OptimisticLockException` is thrown. The service layer catches this and wraps it in a `ConflictException`.

Lock modes used:
- `OPTIMISTIC` — read-time version check (used in `ClientService`, `VehicleService`).
- `OPTIMISTIC_FORCE_INCREMENT` — forces a version bump even on reads (used in `RentService.endRental` to prevent double-end races).

### 4. Retry Logic on Lock Conflicts
`RentService.rentVehicle` and `endRental` retry the operation up to **3 times** on `OptimisticLockException` before propagating, simulating realistic contention handling without distributed coordination.

### 5. Transaction Management (RESOURCE_LOCAL)
The persistence unit runs in `RESOURCE_LOCAL` mode — no application server. Each service method manually demarcates a transaction:
```java
em.getTransaction().begin();
// ... work ...
em.getTransaction().commit();
```
Rollback is called in the `finally` block if the transaction is still active after an exception.

### 6. Cascading
- `Client.currentRents` → cascade PERSIST/MERGE: persisting a client automatically persists its rents.
- `Rent.client` / `Rent.vehicle` → cascade PERSIST: persisting a rent can persist the associated entities.

### 7. Lazy Fetching
`Client.currentRents` uses `fetch = LAZY`. The collection is not loaded unless accessed, avoiding unnecessary SQL when only the client's scalar data is needed.

### 8. Price Snapshot on Rental Start
`Rent.vehiclePerHourPrice` captures the vehicle's price at the moment of rental creation. This ensures that historical cost calculations remain accurate even if the vehicle's price is later updated.

### 9. Bean Validation
Entities are validated with Jakarta Bean Validation annotations (`@NotNull`, `@Min`, `@Length`). Hibernate Validator is the implementation. Validation mode is set to `CALLBACK` in `persistence.xml` so validation runs automatically on persist/update lifecycle events.

### 10. JPQL Queries
`RentService` uses typed JPQL queries to:
- Count a client's active (non-archived) rentals.
- Fetch all active rentals for a client.
- Retrieve all archived rental records.

---

## Project Structure

```
src/
├── main/
│   ├── java/org/example/
│   │   ├── entities/
│   │   │   ├── Client.java
│   │   │   ├── Vehicle.java          # abstract base
│   │   │   ├── Rent.java
│   │   │   └── vehicles/
│   │   │       ├── Car.java
│   │   │       └── Bicycle.java
│   │   ├── repositories/
│   │   │   ├── CrudRepository.java   # generic interface
│   │   │   ├── Lockable.java         # lockedFindById interface
│   │   │   ├── ClientRepository.java
│   │   │   ├── VehicleRepository.java
│   │   │   └── RentRepository.java
│   │   ├── services/
│   │   │   ├── ClientService.java    # changeMaxVehicles
│   │   │   ├── VehicleService.java   # updateVehiclePrice
│   │   │   └── RentService.java      # rentVehicle, endRental
│   │   ├── exceptions/
│   │   │   ├── RentalException.java  # checked, business logic
│   │   │   └── ConflictException.java # runtime, optimistic lock
│   │   ├── util/
│   │   │   └── Address.java          # @Embeddable
│   │   └── Main.java                 # sample data population
│   └── resources/
│       ├── META-INF/persistence.xml
│       └── log4j.properties
└── test/
    └── java/
        ├── RepositoriesTests/
        │   ├── ClientRepositoryTest.java
        │   ├── VehicleRepositoryTest.java
        │   └── RentRepositoryTest.java
        └── ServicesTests/
            ├── ClientServiceTest.java
            ├── VehicleServiceTest.java
            └── RentServiceTest.java
```

---

## Database Configuration

The persistence unit (`default`) connects to a local PostgreSQL instance:

```
Host:     localhost:5432
Database: postgres
User:     postgres
Password: postgres
```

Schema generation is set to **`drop-and-create`** on every startup — suitable for development/testing. Running `Main.java` populates the database with:

- **20 clients** with Polish names, PESEL numbers, and addresses (5 have high `moneySpent` to trigger discount tiers).
- **10 vehicles** — 7 cars (various segments, 1.0–3.5L engines, 20–100 PLN/h) and 3 bicycles (5–20 PLN/h).
- **15 rentals** — random pairings over the last 30 days, ~60% completed.

SQL statements and parameter bindings are logged to the console at DEBUG/TRACE level via the Log4j2 configuration.

---

## Running the Project

**Prerequisites:** Java 23, Maven, PostgreSQL running locally with the credentials above.

```bash
# Compile
mvn compile

# Populate the database with sample data
mvn exec:java -Dexec.mainClass="org.example.Main"

# Run the full test suite
mvn test
```

---

## Test Suite

Tests are organized into two packages:

### Repository Tests (`RepositoriesTests/`)
Cover the full `CrudRepository` contract for each entity: save, saveAll, findById, findAll, findAllById, count, existsById, delete, deleteById, deleteAll, deleteAllById. Vehicle tests also exercise polymorphic queries returning a mix of `Car` and `Bicycle` instances.

### Service Tests (`ServicesTests/`)
Cover business logic and ORM-specific behavior:

| Test class | Key scenarios |
|---|---|
| `ClientServiceTest` | Max-vehicles update, client-not-found, optimistic lock conflict, sequential updates, property isolation |
| `VehicleServiceTest` | Price update for Car and Bicycle, zero price, decimal precision, concurrent update simulation, price update while rented |
| `RentServiceTest` | Successful rent start and end, vehicle-not-found / client-not-found, vehicle already rented, max-vehicles limit, cost calculation (hours × price), discount tiers, money-spent tracking, archived rental queries |
