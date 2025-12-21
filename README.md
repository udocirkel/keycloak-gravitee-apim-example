# Keycloak & Gravitee APIM Example

This repository provides a complete example setup for integrating **Keycloak** as an OAuth2 / OpenID Connect identity
provider with **Gravitee APIM** as an API management platform.
It demonstrates how REST APIs can be secured using standardized authentication and authorization flows, token exchange,
and centralized policy enforcement.
The project delivers an end-to-end example of secure API access using a gateway-based architecture.

A key feature of this example is the implementation of the **Phantom Token Pattern**.
This pattern ensures that clients receive opaque access tokens, while internal services work with validated and fully
authorized JWTs.
The example includes all required configurations and extensions in both Keycloak and Gravitee APIM.
It shows how Gravitee validates incoming opaque tokens, exchanges them for JWT access tokens, caches these tokens, and
forwards them to backend services.

---

## 📦 Project Structure

```
├── keycloak/                   # Keycloak component with preconfigured realm and clients 
├── gravitee/                   # Gravitee Gateway & APIM components
├── nginx/                      # Reverse proxy configuration for Gravitee components
├── coffeehouse/                # Example services (OAuth2 resource server)
│   ├── coffee-ingredient-service/  # Spring WebFlux based service (server)
│   ├── coffee-menu-service/        # Spring WebFlux based service (server + web client)
│   └── coffee-order-service/       # Spring Web based service (server) using WebFlux (web client) 
├── http-test/                  # Demo REST API calls
│
├── pom.xml                     # Maven build for extensions and container images
├── setup.md                    # Manual steps for the initial setup of the example
│
├── docker-compose.yml          # Full local environment (Keycloak + Gravitee + services)
├── docker-compose-kc.yml       # Keycloak-only local environment (Keycloak + services)
├── start.sh                    # Start full local environment
├── stop.sh                     # Stop full local environment
├── startkc.sh                  # Start Keycloak-only local environment
└── stopkc.sh                   # Stop Keycloak-only local environment
```

---

## 🚀 Getting Started

### **Prerequisites / Required Software on Your Machine**

To run this project locally, you need the following installed on your **client machine**:

| Software                  | Purpose                                                            |
|---------------------------|--------------------------------------------------------------------|
| **Java JDK 21+**          | Build the demo REST APIs, Keycloak extensions and Gravitee plugins |
| **Maven 3.8+**            | Build the example Java project and Docker images                   |
| **Docker/Docker Compose** | Run containers for Keycloak, Gravitee, and backend services        |

> Any terminal or shell to run bash scripts (`start.sh`, `stop.sh`, etc.)  
> Recommended at least **4GB RAM** free for the full stack.

### **Get project from SCM**

```bash
git clone https://github.com/udocirkel/keycloak-gravitee-apim-example.git
cd keycloak-gravitee-apim-example
```

### **Build project**

```bash
mvn clean verify
```

### **Start full environment**

```bash
./start.sh
```

### **Initialize environment (only on first run)**

Manually execute the steps described in `setup.md`.

### **Stop environment**

```bash
./stop.sh
```

---

## 🔐 Authentication and Authorization Workflow

This example demonstrates the complete flow:

1. Keycloak issues opaque tokens when authenticating client applications.
2. Client applications call a protected API through the Gravitee API Gateway using the opaque token.
3. The Gravitee API Gateway validates the opaque token.
4. The gateway checks the token's Issued For claim to ensure the client is subscribed to the requested API and
   authorized to access it.
5. The gateway retrieves a corresponding access token from the cache, if available.
6. If no cached token exists, the gateway exchanges the opaque token with Keycloak for a fully authorized access token
   scoped for the API, and stores it in the cache.
7. The gateway calls the API backend using the access token.
8. The API backend validates the access token, executes the business logic, and may call downstream APIs through the
   Gravitee API Gateway as required.

---

## 🌐 Access the Services

| Service                                 | URL                                                                                       |
|-----------------------------------------|-------------------------------------------------------------------------------------------|
| **Keycloak Admin Console**              | http://localhost:8080                                                                     |
| **Gravitee API Gateway**                | http://localhost:8082                                                                     |
| **Gravitee Management API**             | http://localhost:8083                                                                     |
| **Gravitee Management Console (nginx)** | http://localhost:8084                                                                     |
| **Gravitee Developer Portal (nginx)**   | http://localhost:8085                                                                     |
| **Coffee Order Service**                | gateway:<br/>http://localhost:8082/coffee-order-api<br/>direct:<br/>http://localhost:8089 |
| **Coffee Menu Service**                 | gateway:<br/>http://localhost:8082/coffee-menu-api<br/>direct:<br/>http://localhost:8088  |
| **Coffee Ingredient Service**           | only direct:<br/>http://localhost:8087                                                    |

---

## 🧪 Test the Integration

The project includes a ready-to-use HTTP test script located at:

```bash
http-test/coffeehouse-check.http
```

This script can be executed with an HTTP client that supports `.http` files, such as the built-in HTTP client in
IntelliJ IDEA or VS Code (REST Client extension).

The test script performs the complete end-to-end flow:

1. Request an opaque access token from Keycloak using the configured demo client.
2. Call the protected API through the Gravitee API Gateway using the opaque token.
3. Verify the Phantom Token Pattern, where Gravitee validates the opaque token, exchanges it for a JWT access token, and
   forwards the request to the API backend.
4. Receive a processed response from the API backend.

Open the file in your editor and execute the requests.

---

## 🤝 Contributing

Contributions and improvements are welcome.

---

## ⚡ Future Improvements

### Distributed Token Exchange Cache

For production use, a shared distributed cache should be employed, e.g., Redis or Hazelcast.

Benefit: Significantly reduces load on Keycloak as not every gateway instance needs to exchange tokens.

Enables horizontal scaling of the API gateway and more stable latencies.

Cache TTL should be equal to or shorter than the token lifetime to ensure security and proper revocation.

---

## 📄 License

This project is licensed under the **Apache License 2.0**.
