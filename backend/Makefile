# Makefile for ShipMate Backend

# Load environment variables from .env file
ifneq (,$(wildcard ./.env))
    include .env
    export
endif

# Default goal
.DEFAULT_GOAL := help

# Colors for output
RED := \033[0;31m
GREEN := \033[0;32m
YELLOW := \033[0;33m
NC := \033[0m # No Color

##@ General

.PHONY: help
help: ## Display this help message
	@echo "$(GREEN)ShipMate Backend - Available Commands$(NC)"
	@echo ""
	@awk 'BEGIN {FS = ":.*##"; printf "\nUsage:\n  make $(YELLOW)<target>$(NC)\n"} /^[a-zA-Z_0-9-]+:.*?##/ { printf "  $(YELLOW)%-20s$(NC) %s\n", $$1, $$2 } /^##@/ { printf "\n$(GREEN)%s$(NC)\n", substr($$0, 5) } ' $(MAKEFILE_LIST)

##@ Development

.PHONY: run
run: ## Run the Spring Boot application
	@echo "$(GREEN)Starting Spring Boot application...$(NC)"
	mvn spring-boot:run

.PHONY: dev
dev: ## Run with dev profile
	@echo "$(GREEN)Starting application with dev profile...$(NC)"
	mvn spring-boot:run -Dspring-boot.run.profiles=dev

.PHONY: test
test: ## Run all tests
	@echo "$(GREEN)Running tests...$(NC)"
	mvn test -X

.PHONY: test-unit
test-unit: ## Run only unit tests
	@echo "$(GREEN)Running unit tests...$(NC)"
	mvn test -Dtest="*Test" -X

.PHONY: test-int
test-int: ## Run only integration tests
	@echo "$(GREEN)Running integration tests...$(NC)"
	mvn test -Dtest="*IT" -X

.PHONY: test-one
test-one: ## Run a specific test (usage: make test-one TEST=ClassName)
	@echo "$(GREEN)Running test: $(TEST)$(NC)"
	mvn test -Dtest=$(TEST) -X

.PHONY: clean
clean: ## Clean build artifacts
	@echo "$(GREEN)Cleaning build artifacts...$(NC)"
	mvn clean

.PHONY: build
build: ## Build the project
	@echo "$(GREEN)Building project...$(NC)"
	mvn clean package -DskipTests

.PHONY: compile
compile: ## Compile the project
	@echo "$(GREEN)Compiling project...$(NC)"
	mvn compile

##@ Database

.PHONY: db-migrate
db-migrate: ## Run Flyway migrations
	@echo "$(GREEN)Running Flyway migrations...$(NC)"
	mvn flyway:migrate -Dflyway.url=jdbc:postgresql://$(DB_HOST):$(POSTGRES_PORT)/$(POSTGRES_DB) \
		-Dflyway.user=$(POSTGRES_USER) \
		-Dflyway.password=$(POSTGRES_PASSWORD)

.PHONY: db-repair
db-repair: ## Repair Flyway schema history
	@echo "$(YELLOW)Repairing Flyway schema history...$(NC)"
	mvn flyway:repair -Dflyway.url=jdbc:postgresql://$(DB_HOST):$(POSTGRES_PORT)/$(POSTGRES_DB) \
		-Dflyway.user=$(POSTGRES_USER) \
		-Dflyway.password=$(POSTGRES_PASSWORD)

.PHONY: db-info
db-info: ## Show Flyway migration status
	@echo "$(GREEN)Showing Flyway migration info...$(NC)"
	mvn flyway:info -Dflyway.url=jdbc:postgresql://$(DB_HOST):$(POSTGRES_PORT)/$(POSTGRES_DB) \
		-Dflyway.user=$(POSTGRES_USER) \
		-Dflyway.password=$(POSTGRES_PASSWORD)

.PHONY: db-validate
db-validate: ## Validate Flyway migrations
	@echo "$(GREEN)Validating Flyway migrations...$(NC)"
	mvn flyway:validate -Dflyway.url=jdbc:postgresql://$(DB_HOST):$(POSTGRES_PORT)/$(POSTGRES_DB) \
		-Dflyway.user=$(POSTGRES_USER) \
		-Dflyway.password=$(POSTGRES_PASSWORD)

.PHONY: db-clean
db-clean: ## Clean database (ATTENTION: drops all objects)
	@echo "$(RED)WARNING: This will drop all database objects!$(NC)"
	@echo "$(RED)Press Ctrl+C to cancel, or Enter to continue...$(NC)"
	@read confirm
	mvn flyway:clean -Dflyway.url=jdbc:postgresql://$(DB_HOST):$(POSTGRES_PORT)/$(POSTGRES_DB) \
		-Dflyway.user=$(POSTGRES_USER) \
		-Dflyway.password=$(POSTGRES_PASSWORD) \
		-Dflyway.cleanDisabled=false

.PHONY: db-reset
db-reset: db-clean db-migrate ## Reset database (clean + migrate)
	@echo "$(GREEN)Database reset complete!$(NC)"

.PHONY: db-connect
db-connect: ## Connect to PostgreSQL database
	@echo "$(GREEN)Connecting to database...$(NC)"
	psql -h $(DB_HOST) -p $(POSTGRES_PORT) -U $(POSTGRES_USER) -d $(POSTGRES_DB)

##@ Docker

.PHONY: docker-up
docker-up: ## Start Docker containers
	@echo "$(GREEN)Starting Docker containers...$(NC)"
	docker-compose up -d

.PHONY: docker-up-db
docker-up-db: ## Start Docker containers (including database)
	@echo "$(GREEN)Starting Docker containers...$(NC)"
	docker-compose up -d db

.PHONY: docker-down
docker-down: ## Stop Docker containers
	@echo "$(GREEN)Stopping Docker containers...$(NC)"
	docker-compose down

.PHONY: docker-logs
docker-logs: ## Show Docker logs
	@echo "$(GREEN)Showing Docker logs...$(NC)"
	docker-compose logs -f

.PHONY: docker-ps
docker-ps: ## Show running Docker containers
	@echo "$(GREEN)Running containers:$(NC)"
	docker-compose ps

.PHONY: docker-restart
docker-restart: docker-down docker-up ## Restart Docker containers
	@echo "$(GREEN)Docker containers restarted!$(NC)"

##@ Code Quality

.PHONY: format
format: ## Format code with Spring Java Format
	@echo "$(GREEN)Formatting code...$(NC)"
	mvn spring-javaformat:apply

.PHONY: format-check
format-check: ## Check code formatting
	@echo "$(GREEN)Checking code formatting...$(NC)"
	mvn spring-javaformat:validate

.PHONY: lint
lint: ## Run code quality checks
	@echo "$(GREEN)Running code quality checks...$(NC)"
	mvn verify -DskipTests

##@ Dependencies

.PHONY: deps-update
deps-update: ## Update dependencies
	@echo "$(GREEN)Updating dependencies...$(NC)"
	mvn versions:display-dependency-updates

.PHONY: deps-tree
deps-tree: ## Show dependency tree
	@echo "$(GREEN)Showing dependency tree...$(NC)"
	mvn dependency:tree

##@ Utilities

.PHONY: check-env
check-env: ## Check if .env file exists and is configured
	@echo "$(GREEN)Checking environment configuration...$(NC)"
	@if [ ! -f .env ]; then \
		echo "$(RED)Error: .env file not found!$(NC)"; \
		exit 1; \
	fi
	@echo "$(GREEN)✓ .env file found$(NC)"
	@echo "Database: $(POSTGRES_DB)"
	@echo "User: $(POSTGRES_USER)"
	@echo "Host: $(DB_HOST):$(POSTGRES_PORT)"

.PHONY: logs
logs: ## Tail application logs
	@echo "$(GREEN)Tailing application logs...$(NC)"
	tail -f logs/application.log

.PHONY: install
install: ## Install dependencies
	@echo "$(GREEN)Installing dependencies...$(NC)"
	mvn clean install -DskipTests

.PHONY: package
package: ## Package application as JAR
	@echo "$(GREEN)Packaging application...$(NC)"
	mvn clean package

.PHONY: verify
verify: ## Run all checks and tests
	@echo "$(GREEN)Running verification...$(NC)"
	mvn clean verify