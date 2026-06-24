# 🚀 Mini Payment Gateway + Fraud Detection System

A **production-grade, microservice-based payment gateway** that simulates how platforms like Stripe/Razorpay work internally. Features real-time transaction processing, rule-based fraud detection, event-driven architecture, and a merchant dashboard.

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-brightgreen?style=flat-square)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.7-blue?style=flat-square)
![Redis](https://img.shields.io/badge/Redis-7-red?style=flat-square)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=flat-square)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square)
![React](https://img.shields.io/badge/React-18-61DAFB?style=flat-square)

---

## 📋 Table of Contents

- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [System Modules](#-system-modules)
- [Getting Started](#-getting-started)
- [API Reference](#-api-reference)
- [Fraud Detection Engine](#-fraud-detection-engine)
- [Kafka Event Flow](#-kafka-event-flow)
- [Docker Deployment](#-docker-deployment)

---

## 🏗 Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                    React Dashboard (:3000)                     │
└─────────────────────────┬────────────────────────────────────┘
                          │
┌─────────────────────────▼────────────────────────────────────┐
│              API Gateway (:8080)                              │
│         JWT Auth │ Rate Limiting │ Routing                    │
└──┬──────┬──────┬──────┬──────┬──────┬────────────────────────┘
   │      │      │      │      │      │
   ▼      ▼      ▼      ▼      ▼      ▼
┌─────┐┌─────┐┌─────┐┌─────┐┌─────┐┌─────┐
│Merch││ Pay ││Trans││Fraud││Settl││Notif│
│:8081││:8082││:8083││:8084││:8085││:8086│
└──┬──┘└──┬──┘└──┬──┘└──┬──┘└──┬──┘└──┬──┘
   │      │      │      │      │      │
   ▼      ▼      ▼      ▼      ▼      ▼
┌──────────────────────────────────────────┐
│  MySQL │ Redis │ Apache Kafka │ Eureka   │
└──────────────────────────────────────────┘
```

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 17, Spring Boot 3.5.3, Spring Cloud 2025.0.0 |
| **API Gateway** | Spring Cloud Gateway (reactive) |
| **Service Discovery** | Netflix Eureka |
| **Database** | MySQL 8.0 (database-per-service) |
| **Messaging** | Apache Kafka 3.7 (KRaft mode, no Zookeeper) |
| **Caching & Rate Limiting** | Redis 7 |
| **Authentication** | JWT (JJWT 0.12.6) |
| **Frontend** | React + Vite + TailwindCSS v4 + Recharts |
| **Containerization** | Docker Compose |

---

## 📦 System Modules

### 1. Merchant Service (`:8081`)
- Merchant onboarding & authentication
- API key generation (Stripe-like: `pk_live_xxx`, `sk_live_xxx`)
- JWT-based login
- Merchant dashboard data

### 2. Payment Service (`:8082`) — Core Engine
- Payment order creation
- Transaction processing with simulation
- Idempotency via Redis
- Retry mechanism with exponential backoff
- Publishes events to Kafka

### 3. Transaction Service (`:8083`)
- Transaction history & tracking
- Status: INITIATED → PROCESSING → SUCCESS/FAILED/FRAUD_REVIEW
- Aggregated statistics for dashboard
- Daily volume & payment method distribution

### 4. Fraud Detection Service (`:8084`) ⭐
- **Rule-based scoring engine** with 8 fraud rules
- Risk scoring (0-100) with weighted rules
- Decision thresholds: LOW (0-30) → MEDIUM (31-60) → HIGH (61-80) → CRITICAL (81-100)
- Manual review workflow (approve/reject)
- Redis-backed velocity checks

### 5. Settlement Service (`:8085`)
- T+1 daily settlement batch processing
- Merchant balance tracking
- Platform fee deduction (configurable %)
- Settlement history

### 6. Notification Service (`:8086`)
- Event-driven notifications (Kafka consumers)
- Email/SMS simulation
- Payment confirmations, fraud alerts, settlement notices
- Notification logs

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.9+
- Docker Desktop
- Node.js 18+

### Quick Start

```bash
# 1. Clone the repository
git clone https://github.com/Dipanshu-Kubde/payment-gateway.git
cd payment-gateway

# 2. Build all services
mvn clean package -DskipTests

# 3. Start everything with Docker Compose
docker-compose up --build

# 4. Access the services
# Eureka Dashboard: http://localhost:8761
# API Gateway:      http://localhost:8080
# React Dashboard:  http://localhost:3000
```

### Local Development (without Docker)

```bash
# Start infrastructure
docker-compose up mysql redis kafka -d

# Build & run services individually
mvn clean install
cd merchant-service && mvn spring-boot:run
cd payment-service && mvn spring-boot:run
# ... etc

# Start React dashboard
cd dashboard && npm install && npm run dev
```

---

## 📡 API Reference

### Merchant APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/merchants/register` | Register new merchant |
| POST | `/api/merchants/login` | Login & get JWT token |
| GET | `/api/merchants/{id}` | Get merchant details |
| POST | `/api/merchants/{id}/regenerate-keys` | Regenerate API keys |

### Payment APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/payments/orders` | Create payment order |
| POST | `/api/payments/process` | Process payment |
| GET | `/api/payments/orders/{orderId}` | Get order status |
| POST | `/api/payments/{txnId}/retry` | Retry failed payment |

### Transaction APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/transactions` | List all transactions |
| GET | `/api/transactions/{id}` | Transaction details |
| GET | `/api/transactions/stats` | Aggregated statistics |
| GET | `/api/transactions/volume/daily` | Daily volume chart data |

### Fraud APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/fraud/check/{txnId}` | Get fraud result |
| GET | `/api/fraud/rules` | List active rules |
| GET | `/api/fraud/stats` | Fraud statistics |
| POST | `/api/fraud/review/{txnId}/approve` | Approve flagged txn |
| POST | `/api/fraud/review/{txnId}/reject` | Reject flagged txn |

### Settlement APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/settlements/merchant/{id}` | Merchant settlements |
| GET | `/api/settlements/balance/{id}` | Current balance |
| POST | `/api/settlements/trigger/{id}` | Manual settlement |

---

## 🔍 Fraud Detection Engine

### Rules & Scoring

| Rule | Max Score | Description |
|------|-----------|-------------|
| High Amount | 15 | Transaction > ₹50,000 |
| Velocity Check | 20 | > 5 transactions in 10 minutes |
| Failed Attempts | 15 | > 3 failures in 5 minutes |
| Geo Mismatch | 15 | IP country ≠ card country |
| High-Risk Country | 10 | Origin from sanctioned country |
| Unusual Hour | 5 | Transaction at 2-5 AM |
| First-Time + High Amount | 10 | New customer + > ₹20,000 |
| Suspicious Pattern | 10 | Round amounts, threshold testing |

### Decision Matrix

| Risk Score | Level | Action |
|-----------|-------|--------|
| 0-30 | 🟢 LOW | Auto-approve |
| 31-60 | 🟡 MEDIUM | Flag for review |
| 61-80 | 🟠 HIGH | Block, require approval |
| 81-100 | 🔴 CRITICAL | Auto-block |

---

## 📨 Kafka Event Flow

```
Payment Service ──▶ [payment.initiated] ──▶ Fraud Service, Transaction Service
Fraud Service   ──▶ [fraud.check.result] ──▶ Payment Service
Payment Service ──▶ [payment.processed]  ──▶ Transaction, Notification, Settlement
Settlement Svc  ──▶ [settlement.completed] ──▶ Notification Service
```

---

## 🐳 Docker Deployment

```bash
# Full system (12 containers)
docker-compose up --build -d

# View logs
docker-compose logs -f payment-service
docker-compose logs -f fraud-detection-service

# Stop everything
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### Container Ports

| Service | Port | URL |
|---------|------|-----|
| MySQL | 3306 | - |
| Redis | 6379 | - |
| Kafka | 9092/9094 | - |
| Eureka | 8761 | http://localhost:8761 |
| API Gateway | 8080 | http://localhost:8080 |
| Merchant Service | 8081 | - |
| Payment Service | 8082 | - |
| Transaction Service | 8083 | - |
| Fraud Detection | 8084 | - |
| Settlement Service | 8085 | - |
| Notification Service | 8086 | - |
| Dashboard | 3000 | http://localhost:3000 |

---

## 📄 License

This project is for educational and portfolio purposes.

---

Built with ❤️ by [Dipanshu Kubde](https://github.com/Dipanshu-Kubde)
