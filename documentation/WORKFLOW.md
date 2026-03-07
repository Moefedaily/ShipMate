# ShipMate Project Workflow

This document outlines the standard development, testing, and deployment workflows for the ShipMate project.

## 1. Development Workflow

### Prerequisites
Before starting, ensure you have the required versions of Java, Maven, Node.js, and Docker installed as specified in the `README.md`.

### Environment Setup
1.  **Environment Variables:** Copy `.env.sample` to `.env` and fill in the required values.
    ```bash
    cp .env.sample .env
    ```
2.  **Database:** Start the database using Docker.
    ```bash
    # From the root directory
    docker-compose up -d db
    ```

### Backend Development (Spring Boot)
- **Run Application:**
  ```bash
  cd backend
  mvn spring-boot:run
  # OR using Makefile
  make run
  ```
- **Database Migrations:** Flyway is used for database migrations.
  ```bash
  cd backend
  mvn flyway:migrate
  # OR using Makefile
  make db-migrate
  ```
- **Code Formatting:**
  ```bash
  cd backend
  mvn spring-javaformat:apply
  # OR using Makefile
  make format
  ```

### Frontend Development (Angular)
- **Install Dependencies:**
  ```bash
  cd frontend
  npm install
  ```
- **Run Application:**
  ```bash
  cd frontend
  npm start
  ```
- **Accessing the App:** The frontend is usually available at `http://localhost:4200`.

---

## 2. Testing Workflow

### Backend Testing
- **Run All Tests:**
  ```bash
  cd backend
  mvn test
  # OR using Makefile
  make test
  ```
- **Run Specific Test:**
  ```bash
  cd backend
  mvn test -Dtest=ClassName
  # OR using Makefile
  make test-one TEST=ClassName
  ```

### Frontend Testing
- **Run Tests:**
  ```bash
  cd frontend
  npm test
  ```

---

## 3. Build Workflow

### Backend Build
- **Package JAR:**
  ```bash
  cd backend
  mvn clean package -DskipTests
  # OR using Makefile
  make build
  ```

### Frontend Build
- **Build Production Assets:**
  ```bash
  cd frontend
  npm run build
  ```

---

## 4. Docker & Orchestration

### Running the Full Stack
To run the entire system (DB, Backend, Frontend) in Docker:
```bash
# From the root directory
docker-compose up -d
```

### Stopping the System
```bash
docker-compose down
```

---

## 5. CI/CD Workflow (GitHub Actions)

The project uses GitHub Actions for automated building and testing. For a detailed breakdown of the pipelines, triggers, and deployment strategies, see:

👉 **[Detailed CI/CD Workflow Documentation](./CI_CD_WORKFLOW.md)**

- **Backend CI:** `.github/workflows/backend.yml`
- **Frontend CI:** `.github/workflows/frontend.yml`

---

## 6. Git Workflow

1.  **Branching:** Create a new branch for each feature or bug fix.
    ```bash
    git checkout -b feature/your-feature-name
    ```
2.  **Committing:** Use descriptive commit messages.
3.  **Pull Requests:** Submit a pull request to the `main` or `develop` branch for review.
4.  **Formatting:** Ensure code is formatted correctly (e.g., `make format` for backend) before pushing.
