# Stock Trading Client

A Spring Boot application that acts as a gRPC client to interact with a Stock Trading Service. It demonstrates various gRPC communication patterns including Unary, Server Streaming, Client Streaming, and Bidirectional Streaming.

## 🚀 Features

- **Real-time Stock Prices**: Get current prices for specific stock symbols (Unary RPC).
- **Price Subscription**: Subscribe to a stream of live stock price updates (Server Streaming).
- **Bulk Ordering**: Submit multiple stock orders in a single stream and receive a summary (Client Streaming).
- **Live Trading**: Real-time bidirectional trading where orders are sent and status updates are received (Bidirectional Streaming).
- **Web Interface**: A user-friendly UI built with Thymeleaf to interact with the gRPC service.

## 🛠️ Technologies Used

- **Java 21**
- **Spring Boot 3.4.1**
- **gRPC** (io.grpc)
- **Protobuf** (Protocol Buffers)
- **Thymeleaf** (Template Engine)
- **Maven** (Build Tool)

## 📋 Prerequisites

- **Java Development Kit (JDK) 21** or higher.
- **Apache Maven** installed.
- **Stock Trading gRPC Server**: A running instance of the gRPC server that this client connects to. The default address is `static://127.0.0.1:9090`.

## ⚙️ Configuration

The client configuration can be found in `src/main/resources/application.yml`:

```yaml
grpc:
  client:
    stockService:
      address: "static://127.0.0.1:9090"
      negotiation-type: plaintext
```

## 🏗️ Getting Started

### 1. Build the Project

Run the following command to compile the project and generate the gRPC code from the proto files:

```bash
mvn clean install
```

### 2. Run the Application

Start the Spring Boot application:

```bash
mvn spring-boot:run
```

The application will be available at `http://localhost:8080`.

## 🧪 Testing

The project includes comprehensive tests for both the service and controller layers, utilizing `spring-grpc-test` and Mockito.

### Running Tests

To execute all tests, run:

```bash
mvn test
```

### Key Test Classes:

- **`StockClientServiceTest`**: Unit tests for the gRPC client service logic, mocking the gRPC stubs.
- **`StockStreamingControllerTest`**: Integration tests for the web controller, verifying the interaction between the UI and the gRPC service.

## 📁 Project Structure

- `src/main/proto`: Contains `stock_trading.proto` defining the gRPC service and messages.
- `src/main/java`:
  - `controller`: Web controllers handling UI requests.
  - `service`: Implementation of the gRPC client logic.
- `src/main/resources`:
  - `templates`: Thymeleaf HTML templates (e.g., `index.html`).
  - `application.yml`: Application configuration.

## 📜 License

This project is for educational purposes. Ricardo Vega 2026
