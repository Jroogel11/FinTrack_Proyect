# 🏦 FinTrack — Personal Finance Microservices Platform

A production-ready microservices backend built with **Java 21**, **Spring Boot** and **Apache Kafka**, simulating a real-world banking platform. Designed to demonstrate enterprise-level architecture patterns used in fintech companies.

---

## 🏗️ Architecture

Client
└── API Gateway (8080) — JWT validation, routing
├── Auth Service (8081) — Authentication & JWT generation
├── Account Service (8082) — Account management + Kafka Consumer
└── Transaction Service (8083) — Transactions + Kafka Producer
│
Apache Kafka (9092)
│
Account Service ← updates balance asynchronously

Each microservice has its **own independent PostgreSQL database** — following the database-per-service pattern.

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.4 |
| API Gateway | Spring Cloud Gateway |
| Security | Spring Security + JWT (JJWT) |
| Messaging | Apache Kafka + ZooKeeper |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 16 |
| Containerization | Docker + Docker Compose |
| Testing | JUnit 5 + Mockito + AssertJ |
| Build Tool | Maven |

---

## 📦 Services

| Service | Port | Database | Responsibility |
|---------|------|----------|----------------|
| API Gateway | 8080 | — | JWT validation, request routing |
| Auth Service | 8081 | fintrack_auth (5432) | User registration, login, JWT generation |
| Account Service | 8082 | fintrack_accounts (5433) | Account CRUD, balance updates via Kafka |
| Transaction Service | 8083 | fintrack_transactions (5434) | Transaction recording, Kafka events |

---

## 🚀 Getting Started

### Prerequisites

- Docker Desktop installed and running
- Git

### Run the entire system with one command

```bash
git clone https://github.com/your-username/FinTrack.git
cd FinTrack
docker-compose up --build
```

Wait until you see all four services started:
fintrack-auth-service        | Started AuthServiceApplication
fintrack-account-service     | Started AccountServiceApplication
fintrack-transaction-service | Started TransactionServiceApplication
fintrack-gateway-service     | Started GatewayServiceApplication

The system is ready at `http://localhost:8080`

---

## 📡 API Endpoints

All requests go through the **API Gateway on port 8080**.

### Authentication (public — no JWT required)
POST /api/v1/auth/register    Register a new user
POST /api/v1/auth/login       Login and get JWT tokens
POST /api/v1/auth/refresh     Refresh access token
GET  /api/v1/auth/me          Get current user info (JWT required)
POST /api/v1/auth/logout      Logout (JWT required)

### Accounts (JWT required)
POST   /api/v1/accounts          Create a new account
GET    /api/v1/accounts          List all accounts for current user
GET    /api/v1/accounts/{id}     Get account by ID
PUT    /api/v1/accounts/{id}     Update account name
DELETE /api/v1/accounts/{id}     Deactivate account (soft delete)

### Transactions (JWT required)
POST /api/v1/transactions                    Create a transaction
GET  /api/v1/transactions?page=0&size=20     List user transactions (paginated)
GET  /api/v1/transactions/account/{id}       List transactions for a specific account
GET  /api/v1/transactions/{id}               Get transaction by ID

### Example — Register and create an account

```bash
# 1. Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Jose","lastName":"Garcia","email":"jose@gmail.com","password":"password123"}'

# 2. Create account (use the accessToken from step 1)
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Main Account","type":"CHECKING","initialBalance":1000.00,"currency":"EUR"}'

# 3. Create a transaction
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"accountId":1,"type":"EXPENSE","amount":50.00,"description":"Grocery shopping","category":"Food"}'
```

---

## ⚡ Kafka Event Flow

When a transaction is created, the balance is updated **asynchronously**:

Client → POST /api/v1/transactions
Transaction Service saves to DB
Transaction Service publishes event to Kafka topic: fintrack.transactions
Client receives 201 Created immediately
Account Service consumes the event asynchronously
Account Service updates the account balance in its own DB
Account Service publishes BALANCE_UPDATED to Kafka topic: fintrack.accounts


This decoupled architecture ensures **resilience** — if Account Service is temporarily down, Kafka retains the events and delivers them when it recovers.

---

## 🔐 Security

- **JWT Authentication** — access tokens (15 min) + refresh tokens (7 days)
- **Token Rotation** — refresh tokens are revoked after use
- **Single validation point** — JWT is validated once at the Gateway
- **User isolation** — all data queries include userId to prevent IDOR attacks
- **Soft delete** — accounts are never physically deleted (regulatory requirement)
- **BCrypt** — passwords are always hashed, never stored in plain text

---

## 🧪 Running Tests

```bash
# Auth Service — 16 tests
cd auth_service
.\mvnw.cmd test

# Account Service — 10 tests
cd account_service
.\mvnw.cmd test
```

**Test coverage includes:**
- JWT token generation, validation and expiration
- User registration with email uniqueness validation
- Login with bad credentials handling
- Refresh token rotation
- Account CRUD with user isolation
- Soft delete verification

---

## 📁 Project Structure
FinTrack/
├── docker-compose.yml          # Starts all 9 containers
├── auth_service/               # Spring Boot 3.4 — port 8081
│   ├── Dockerfile
│   └── src/
├── account_service/            # Spring Boot 3.4 — port 8082
│   ├── Dockerfile
│   └── src/
├── transaction_service/        # Spring Boot 3.4 — port 8083
│   ├── Dockerfile
│   └── src/
└── gateway_service/            # Spring Cloud Gateway — port 8080
├── Dockerfile
└── src/

---

## 🌍 Why this project?

This project was built to demonstrate patterns used in **banking and fintech companies**:

- Microservices architecture with independent databases
- Asynchronous communication with Apache Kafka
- JWT security with refresh token rotation
- Docker containerization for consistent deployments
- Unit testing with JUnit 5 + Mockito
- RESTful API design with proper HTTP status codes

---

## 📄 License

MIT License