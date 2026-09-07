# Wallet System

A secure digital wallet backend built with **Spring Boot**. The system supports user registration, JWT authentication, wallet top-ups, peer-to-peer transfers, idempotent money operations, transaction history, role-based authorization, and concurrency-safe wallet balance updates.

## Features

- JWT-based authentication and role-based authorization (`USER`, `ADMIN`)
- One wallet per user, automatically created during registration
- Secure password hashing using BCrypt
- Add money to a wallet
- Peer-to-peer wallet transfers
- Idempotent add-money and transfer operations using the `Idempotency-Key` header
- Database-enforced unique idempotency keys
- Concurrency-safe wallet updates using database-level pessimistic locking
- Optimistic locking using JPA `@Version`
- Ordered wallet locking during transfers to reduce deadlock risk
- Atomic money transfers using database transactions
- Persistent transaction history
- Paginated transaction and wallet APIs
- Request payload validation using Jakarta Bean Validation
- Global exception handling with consistent error responses
- H2 file-based database for local persistence
- JaCoCo coverage verification with an 80% minimum coverage gate
- SonarQube integration for code quality analysis

## Tech Stack

- **Java 17**
- **Spring Boot 4**
- **Spring Security**
- **JWT (JJWT)**
- **Spring Data JPA / Hibernate**
- **H2 Database**
- **Maven**
- **Jakarta Bean Validation**
- **JUnit / Spring Boot Test**
- **JaCoCo**
- **SonarQube**

## Project Structure

```text
src/main/java/com/shantanu/wallet/
├── config/          # Security, JWT filter and application configuration
├── controller/      # REST API controllers
├── dto/             # Request and response DTOs
├── entity/          # JPA entities and enums
├── exception/       # Custom exceptions and global exception handler
├── repository/      # Spring Data JPA repositories
└── service/         # Business logic
```

## Prerequisites

- JDK 17 or later
- Maven 3.9+ or the included Maven Wrapper
- Git

## Running Locally

Clone the repository and navigate to the project directory.

### Windows

```bash
.\mvnw.cmd spring-boot:run
```

### Linux / macOS

```bash
./mvnw spring-boot:run
```

The application starts on:

```text
http://localhost:8080
```

## H2 Database

The application uses an H2 file-based database:

```text
JDBC URL: jdbc:h2:file:./data/walletdb
Username: sa
Password:
```

H2 Console:

```text
http://localhost:8080/h2-console
```

The H2 console is provided for local development and demonstration purposes.

---

# API Documentation

All wallet and admin APIs require JWT authentication unless explicitly stated otherwise.

## 1. Register User

```http
POST /auth/register
Content-Type: application/json
```

Request:

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

Creates a new user with the `USER` role and automatically creates one wallet with an initial balance of `0.00`.

---

## 2. Login

```http
POST /auth/login
Content-Type: application/json
```

Request:

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

Response:

```json
{
  "token": "<JWT>"
}
```

Use the returned JWT for protected APIs:

```http
Authorization: Bearer <JWT>
```

---

## 3. Add Money

```http
POST /wallet/add
Authorization: Bearer <JWT>
Idempotency-Key: add-money-001
Content-Type: application/json
```

Request:

```json
{
  "amount": 100.00
}
```

The `Idempotency-Key` must be unique for each new operation.

If the same request is submitted again with the same idempotency key, the previously created transaction is returned instead of processing the operation again.

---

## 4. Transfer Money

```http
POST /wallet/transfer
Authorization: Bearer <JWT>
Idempotency-Key: transfer-001
Content-Type: application/json
```

Request:

```json
{
  "toUserId": 2,
  "amount": 50.00
}
```

The transfer:

1. Identifies the sender from the authenticated JWT.
2. Loads the sender and receiver wallets.
3. Locks both wallet rows.
4. Validates the sender's balance.
5. Debits the sender.
6. Credits the receiver.
7. Creates the transaction record.
8. Commits all changes atomically.

If any step fails, the transaction is rolled back.

---

## 5. View Own Wallet

```http
GET /wallet
Authorization: Bearer <JWT>
```

Returns the authenticated user's wallet and current balance.

The wallet ID is derived from the authenticated user rather than being supplied by the client.

---

## 6. View Own Transaction History

```http
GET /wallet/transactions?page=0&size=10
Authorization: Bearer <JWT>
```

Returns paginated transactions associated with the authenticated user's wallet.

---

# Admin APIs

Admin APIs require a valid JWT belonging to a user with the `ADMIN` role.

## View All Wallets

```http
GET /admin/wallets?page=0&size=20
Authorization: Bearer <ADMIN_JWT>
```

## View All Transactions

```http
GET /admin/transactions?page=0&size=20
Authorization: Bearer <ADMIN_JWT>
```

Regular users receive `403 Forbidden` when attempting to access admin APIs.

---

# Security

The application implements the following security controls:

- Passwords are stored using BCrypt hashing.
- JWT authentication is used for protected APIs.
- JWT tokens are validated on every protected request.
- Authentication is stateless; server-side sessions are not used.
- Wallet ownership is derived from the authenticated user.
- Users cannot access another user's wallet through request parameters.
- `/admin/**` endpoints require `ROLE_ADMIN`.
- All other business endpoints require authentication.
- CSRF protection is disabled because the application is a stateless REST API using JWT authentication through the `Authorization` header rather than browser sessions or authentication cookies.

---

# Idempotency

Money-changing operations require an `Idempotency-Key` header.

The key is stored in the transaction table with a database-level unique constraint.

Example:

```http
Idempotency-Key: transfer-001
```

If the same key is submitted again, the existing transaction is returned and the wallet operation is not processed again.

This protects against duplicate requests caused by:

- Client retries
- Network failures
- Duplicate API calls
- Request replay

The database unique constraint provides an additional safeguard against duplicate transaction records.

---

# Concurrency Strategy

The wallet system is designed to be concurrency-safe using database-level locking and transactional boundaries.

### Pessimistic Locking

Wallet rows are locked using:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
```

This ensures that concurrent operations modifying the same wallet are serialized at the database level.

### Ordered Locking

For transfers involving two wallets, wallet IDs are locked in ascending order.

This provides a consistent lock acquisition order and reduces the possibility of deadlocks when opposite-direction transfers occur concurrently.

### Optimistic Locking

The `Wallet` entity also contains:

```java
@Version
private Long version;
```

This provides an additional safeguard against unexpected concurrent modifications.

### Atomic Transfers

Transfers execute inside a Spring `@Transactional` service method.

The sender debit, receiver credit, and transaction record are committed as one database transaction.

If an operation fails, the complete transaction is rolled back.

### No In-Memory Locks

The application does not use:

- `synchronized`
- In-memory mutexes
- Static locks
- Application-level locking mechanisms

Concurrency control is handled at the database and transaction level.

---

# Transaction Boundaries

The following operations are transactional:

- User registration and wallet creation
- Add-money operations
- Wallet transfers

For a transfer, the following operations occur within the same database transaction:

```text
Lock sender wallet
        ↓
Lock receiver wallet
        ↓
Validate balance
        ↓
Debit sender
        ↓
Credit receiver
        ↓
Create transaction record
        ↓
Commit
```

If an exception occurs before commit, the balance changes and transaction record are rolled back.

---

# Validation & Error Handling

Request payloads are validated using Jakarta Bean Validation.

Examples of validation rules include:

- Email must be valid.
- Required fields cannot be null or blank.
- Transaction amounts must be at least `0.01`.
- Transfer requests must contain a receiver user ID.

The application uses a global exception handler to return consistent error responses.

Example:

```json
{
  "timestamp": "2026-09-06T15:39:53",
  "status": 400,
  "error": "Bad Request",
  "message": "Insufficient balance"
}
```

Common errors include:

- `400 Bad Request` — invalid request or insufficient balance
- `401 Unauthorized` — invalid credentials
- `403 Forbidden` — insufficient permissions
- `404 Not Found` — wallet/user not found
- `409 Conflict` — concurrent or duplicate database operation
- `500 Internal Server Error` — unexpected server error

---

# Testing

Run the test suite:

### Windows

```bash
.\mvnw.cmd test
```

### Full verification with coverage

```bash
.\mvnw.cmd clean verify
```

The project enforces a minimum **80% line coverage** using JaCoCo.

Current test suite covers:

- Authentication
- User registration
- Wallet operations
- Money transfers
- Idempotency
- Insufficient balance
- Self-transfer validation
- Transaction history
- Admin authorization
- Global exception handling
- JWT functionality

JaCoCo report:

```text
target/site/jacoco/index.html
```

---

# SonarQube

The project is configured for SonarQube analysis.

Start SonarQube locally and run:

```bash
.\mvnw.cmd clean verify sonar:sonar -Dsonar.projectKey=wallet-system -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<YOUR_SONAR_TOKEN>
```

The SonarQube token should be provided through the command line or environment variable and **must not be committed to the repository**.

SonarQube configuration is maintained through the Maven project configuration.

Current quality results include:

- Security: **A**
- Reliability: **A**
- Maintainability: **A**
- Coverage: **91.7%**
- Duplications: **0.0%**

---

# Design Decisions & Assumptions

- Each user owns exactly one wallet.
- A wallet is automatically created when a user registers.
- Wallet ownership is determined from the authenticated user.
- Transfer targets are specified using `toUserId`.
- Money values use `BigDecimal` rather than floating-point types.
- Wallet balances use database-level locking for concurrency control.
- Transfers are atomic database transactions.
- Idempotency keys are enforced with a unique database constraint.
- Transaction records are persistent and retained in the database.
- Failed transaction status is modeled in the entity, while the current business flow persists successful operations.
- H2 is used as required for the assignment.
- The seeded administrator account is created by `DataInitializer` for demonstration purposes.

---

# Demo Flow

The recommended demonstration flow is:

```text
Register User 1
      ↓
Login User 1
      ↓
View Wallet
      ↓
Add Money
      ↓
Repeat Add Money with same Idempotency-Key
      ↓
Verify balance was not duplicated
      ↓
Register User 2
      ↓
Login User 2
      ↓
Transfer Money
      ↓
Verify both wallet balances
      ↓
View Transaction History
      ↓
Login as Admin
      ↓
View All Wallets
      ↓
View All Transactions
      ↓
Demonstrate authorization restriction
```

---

# Build & Run

To verify the complete project before submission:

```bash
.\mvnw.cmd clean verify
```

Expected result:

```text
BUILD SUCCESS
```

Then start the application:

```bash
.\mvnw.cmd spring-boot:run
```

---

# License

This project was developed as part of a Java/Spring Boot technical assignment.