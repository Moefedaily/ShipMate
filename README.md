# Shipment Management Platform

ShipMate is a comprehensive logistics and shipment management platform designed to streamline the process of booking, tracking, and managing shipments. It features a robust Spring Boot backend and a modern Angular frontend, all orchestrated with Docker.

---

## Project Overview

ShipMate provides a complete ecosystem for shipment logistics, including:
- **Shipment Management:** Create, edit, and track shipments.
- **Booking System:** Manage bookings between senders and drivers.
- **Admin Dashboard:** Overview of system activity and user management.
- **Driver/Sender Portals:** Specialized interfaces for different user roles.
- **Earnings & Payments:** Integrated tracking of earnings and payment processing.
- **Insurance:** Integrated shipment insurance management.
- **Real-time Notifications:** WebSocket-based notification system.

---

## Tech Stack

### Backend
- **Framework:** Spring Boot 3 (Java 21)
- **Database:** PostgreSQL 16
- **Migration:** Flyway
- **Security:** Spring Security with JWT
- **API Documentation:** Postman collections included
- **Build Tool:** Maven

### Frontend
- **Framework:** Angular 21
- **Styling:** SCSS / Vanilla CSS
- **State Management:** Service-based state patterns
- **Maps:** Leaflet for geolocation and shipment tracking
- **UI Components:** Angular Material

### Infrastructure & CI/CD
- **Containerization:** Docker & Docker Compose
- **CI/CD:** GitHub Actions (Build, Test, Deploy)
- **Registry:** GitHub Container Registry (GHCR)
- **Web Server:** Nginx (for frontend production)

---

## Project Structure

```text
.
├── backend/            # Spring Boot application
├── frontend/           # Angular application
├── documentation/      # Project workflows and CI/CD docs
├── .github/workflows/  # CI/CD pipeline definitions
├── docker-compose.yml  # Development orchestration
└── README.md           # This file
```

---

##  Documentation

For detailed guides on how to work with this project, please refer to the documentation folder:

-  **[Development Workflow](./documentation/WORKFLOW.md)**: Setup, running, and testing.
-  **[CI/CD Workflow](./documentation/CI_CD_WORKFLOW.md)**: Pipelines, deployments, and rollbacks.

---

##  Quick Start (Development)

1.  **Prerequisites:** Ensure Java 21, Node.js 22, and Docker are installed.
2.  **Environment:** `cp .env.sample .env` and configure your variables.
3.  **Database:** `docker-compose up -d db`
4.  **Backend:** `cd backend && ./mvnw spring-boot:run`
5.  **Frontend:** `cd frontend && npm install && npm start`

---

##  Environment Verification

Run these commands to confirm your local setup:

```bash
java -version
mvn -v
node -v
npm -v
ng version
docker -v
docker compose version
```
