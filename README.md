# Transaction Service (Mock)

This project is a mock `transaction-service`, created to serve as a monitoring target for the **Observability Core** system. It simulates a microservice responsible for handling financial transactions, providing a key endpoint for health and performance checks.

This service is part of a larger collection of mock services found in the [mock-services-sample](https://github.com/your-username/mock-services-sample) repository.

## Table of Contents
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Running the Service](#running-the-service)
    - [Using Docker (Recommended)](#using-docker-recommended)
    - [Locally with Maven](#locally-with-maven)
- [API Endpoints](#api-endpoints)
- [CI/CD Pipeline](#cicd-pipeline)

## Features

*   Exposes a standard Spring Boot Actuator health endpoint at `/actuator/health`.
*   Includes a simulated processing delay to make performance monitoring more realistic and meaningful.
*   Containerized with Docker for consistent and isolated deployments.
*   Automated build and publishing pipeline to GitHub Container Registry (GHCR).

## Tech Stack

*   **Language:** Java 21
*   **Framework:** Spring Boot 3.x
*   **Build Tool:** Apache Maven
*   **Containerization:** Docker

## Prerequisites

*   Java 21 JDK
*   Apache Maven 3.9+
*   Docker Desktop

## Running the Service

The service can be run via Docker or directly with Maven. Docker is the recommended method for consistency.

### Using Docker (Recommended)

1.  **Build the Docker image:**
    From the root of the `transaction-service` directory, run:
    ```bash
    docker build -t transaction-service .
    ```

2.  **Run the Docker container:**
    This service is typically mapped to port `9002`.
    ```bash
    docker run -p 9002:8080 transaction-service
    ```
    *The service will be accessible on your local machine at `http://localhost:9002`.*

Alternatively, you can pull the latest pre-built image from GitHub Container Registry:
```bash
docker pull ghcr.io/your-username/transaction-service:latest
docker run -p 9002:8080 ghcr.io/your-username/transaction-service:latest
```
*(Remember to replace `your-username` with your actual GitHub username)*

### Locally with Maven

You can also run the application directly from the source code.

1.  **Navigate to the project directory:**
    ```bash
    cd /path/to/mock-services-sample/transaction-service
    ```

2.  **Run the application:**
    ```bash
    mvn spring-boot:run
    ```
    *The service will be accessible on your local machine at `http://localhost:8080`.*


## API Endpoints

The primary endpoint used for monitoring is:

*   **Health Check**
    *   **URL:** `/actuator/health`
    *   **Method:** `GET`
    *   **Success Response (200 OK):**
        ```json
        {
          "status": "UP"
        }
        ```
*   **Simulated Transaction Processing**
    *   **URL:** `/transactions`
    *   **Method:** `POST`
    *   **Behavior:** This endpoint can be used to simulate work. It includes an artificial delay to test the performance monitoring capabilities of the main observability system.

## CI/CD Pipeline

This project is configured with a GitHub Actions workflow located at `.github/workflows/publish-to-ghcr.yml` within the root of the `mock-services-sample` repository.

The pipeline automates the following process on every push to the `main` branch:
1.  **Builds** the Java application using Maven.
2.  **Tests** the application.
3.  **Builds** a new Docker image.
4.  **Publishes** the image to GitHub Container Registry (GHCR) with the `latest` tag.