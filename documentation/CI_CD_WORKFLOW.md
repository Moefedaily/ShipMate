# CI/CD Workflow Documentation

This document provides a detailed explanation of the Continuous Integration and Continuous Deployment (CI/CD) pipelines for the ShipMate project, powered by GitHub Actions.

---

## Overview

The project uses two main workflows located in `.github/workflows/`:
1.  **Backend CI/CD (`backend.yml`)**: Handles Java/Maven build, testing, Dockerization, and deployment.
2.  **Frontend CI/CD (`frontend.yml`)**: Handles Node.js/Angular build, testing, Dockerization, and deployment.

---

## 1. Backend CI/CD Pipeline

### Triggers
- **Push** to `main` or `dev` branches when files in `backend/**` or the workflow itself change.
- **Pull Request** to `main` or `dev` branches for the same paths.

### Jobs

#### A. Build & Test (`build`)
1.  **Environment:** Runs on `ubuntu-latest`.
2.  **Services:** Starts a **PostgreSQL 14** container for integration tests.
3.  **Setup:**
    - Checks out the code.
    - Sets up **Java 21** (Temurin distribution) with Maven caching.
4.  **Execution:** Runs `mvn clean verify` in the `backend` directory.
5.  **Dockerization (only on push):**
    - Logs into **GitHub Container Registry (GHCR)**.
    - Builds a Docker image tagged with the Git SHA and `prod`.
    - Pushes both tags to GHCR (`ghcr.io/moefedaily/shipmate-backend`).

#### B. Deploy (`deploy`)
1.  **Condition:** Only runs on `push` to the `main` branch and after the `build` job succeeds.
2.  **Strategy:** **Immutable Deployment** via SSH.
3.  **Steps:**
    - **Connect:** SSH into the production server.
    - **Backup:** Performs a `pg_dump` of the database before any changes.
    - **Save State:** Records the currently running image tag for potential rollback.
    - **Update:**
        - Pulls the new Docker image using the Git SHA.
        - Updates `docker-compose.prod.yml` with the new image tag.
        - Restarts the `backend` container.
    - **Health Check:** Polls the `/actuator/health` endpoint.
    - **Rollback:** If health checks fail after 20 retries, it automatically reverts to the previous image.
    - **Cleanup:** Keeps only the last 3 Docker images and SQL backups to save space.

---

## 2. Frontend CI/CD Pipeline

### Triggers
- **Push** or **Pull Request** to `main` or `dev` branches when files in `frontend/**` or the workflow change.

### Jobs

#### A. Build & Test (`build`)
1.  **Environment:** Runs on `ubuntu-latest`.
2.  **Setup:**
    - Checks out the code.
    - Sets up **Node.js 20** with npm caching.
3.  **Execution:**
    - `npm ci` to install dependencies.
    - `npm run build -- --configuration production` to generate optimized assets.
4.  **Dockerization (only on push):**
    - Builds a Docker image using `Dockerfile.prod`.
    - Tags with Git SHA and `prod`.
    - Pushes to GHCR (`ghcr.io/moefedaily/shipmate-frontend`).

#### B. Deploy (`deploy`)
1.  **Condition:** Only runs on `push` to the `main` branch.
2.  **Strategy:** Similar to the backend, it uses SSH and an immutable strategy.
3.  **Steps:**
    - **Update:**
        - Pulls the new frontend image.
        - Updates `docker-compose.prod.yml`.
        - Restarts the `web` container.
    - **Health Check:** Attempts to `curl` the `index.html` page.
    - **Rollback:** Reverts to the previous image if the site is unreachable.
    - **Cleanup:** Prunes old images and layers.

---

## Secrets Required

The following secrets must be configured in the GitHub repository settings:

| Secret Name | Description |
| :--- | :--- |
| `SERVER_IP` | Production server IP address |
| `SERVER_USER` | SSH username (e.g., `root` or `deploy`) |
| `SSH_PRIVATE_KEY` | Private key for SSH access |
| `GITHUB_TOKEN` | Automatically provided by GitHub for GHCR access |

---

## Deployment Logic (Scripted)

The deployment scripts in both workflows follow a "Safe Update" pattern:
1.  **Pull** first to ensure minimal downtime.
2.  **Update Compose** file to maintain a source of truth for the current version.
3.  **Up** the specific service.
4.  **Verify** health.
5.  **Rollback** if verification fails.
