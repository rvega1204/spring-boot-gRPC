# Stock Trading Server (gRPC)

A Spring Boot application that implements a gRPC server for real-time stock trading and price consultation. This project demonstrates various gRPC communication patterns, including Unary, Server-Streaming, Client-Streaming, and Bidirectional Streaming.

## 🚀 Features

- **Stock Price Consultation**: Retrieve the current price of a stock by its symbol (Unary RPC).
- **Real-time Price Subscription**: Subscribe to a stream of live stock price updates (Server-Streaming RPC).
- **Bulk Order Processing**: Submit multiple stock orders in a single request and receive a summary (Client-Streaming RPC).
- **Live Trading**: Interactive live trading with immediate status responses for each order (Bidirectional Streaming RPC).

## 🛠️ Technologies Used

- **Java 21**: The core programming language.
- **Spring Boot 4.0.3**: Framework for building the application.
- **gRPC**: For high-performance, contract-first communication.
- **Spring gRPC**: Integration between Spring Boot and gRPC.
- **Protobuf (Protocol Buffers)**: For efficient data serialization and service definition.
- **MySQL**: Relational database for storing stock data.
- **Maven**: Project management and build automation.

## 📁 Project Structure

```bash
stock-trading-server/
├── src/
│   ├── main/
│   │   ├── java/                # Java source code
│   │   │   └── com.rvg.stocktradingserver
│   │   │       ├── entity       # JPA Entities (Stock)
│   │   │       ├── repository   # JPA Repositories (StockRepository)
│   │   │       └── service      # gRPC Service Implementations (StockTradingImpl)
│   │   ├── proto/               # gRPC Service Definitions (.proto files)
│   │   │   └── stock_trading.proto
│   │   └── resources/
│   │       └── application.properties # Application configuration (DB, gRPC)
│   └── test/                    # Unit and Integration tests
├── pom.xml                      # Maven dependencies and plugins
└── README.md                    # Project documentation
```

## ⚙️ Setup and Configuration

### Prerequisites

- JDK 21 or higher.
- Maven 3.x.
- MySQL Server.

### Database Setup

Create a database named `your_database_name` (or as configured in `application.properties`):

```sql
CREATE DATABASE your_database_name;
```

Update your `src/main/resources/application.properties` with your database credentials:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/your_database_name?useSSL=false&serverTimezone=UTC
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### Building the Project

Compile the Protobuf files and build the project using Maven:

```bash
mvn clean install
```

### Running the Application

Run the Spring Boot application:

```bash
mvn spring-boot:run
```

The gRPC server will start and listen for incoming connections on the default port (usually 9090).

## 📡 gRPC API Reference

The gRPC service is defined in `src/main/proto/stock_trading.proto`.

| RPC Method            | Type             | Description                                       |
| :-------------------- | :--------------- | :------------------------------------------------ |
| `GetStockPrice`       | Unary            | Returns the current price for a stock symbol.     |
| `SubscribeStockPrice` | Server Streaming | Streams constant price updates for a symbol.      |
| `BulkStockOrder`      | Client Streaming | Aggregates multiple orders into a single summary. |
| `LiveTrading`         | Bidirectional    | Interactive order placement with status replies.  |

## 📚 gRPC Resources and Documentation

To learn more about gRPC and how it integrates with Spring Boot, check out the following resources:

- **[gRPC Documentation](https://grpc.io/docs/)**: The official gRPC documentation.
- **[Introduction to gRPC](https://grpc.io/docs/what-is-grpc/introduction/)**: Core concepts and architecture.
- **[gRPC Java Basics](https://grpc.io/docs/languages/java/basics/)**: Guide for implementing gRPC in Java.
- **[Protocol Buffers Language Guide](https://protobuf.dev/programming-guides/proto3/)**: Detailed explanation of `.proto` file syntax.
- **[Spring gRPC Documentation](https://docs.spring.io/spring-grpc/docs/current/reference/html/)**: Official documentation for the Spring gRPC integration used in this project.
- **[gRPC Communication Patterns](https://grpc.io/docs/what-is-grpc/core-concepts/)**: Understanding Unary vs. Streaming RPCs.

## 🧪 Testing

The project includes unit tests for the gRPC service implementation.

To run the tests:

```bash
mvn test
```

### Test Coverage Highlights

- **`StockTradingImplTest`**:
  - Verifies `GetStockPrice` returns correct price from repository.
  - Verifies `SubscribeStockPrice` emits multiple price updates.
  - Verifies `BulkStockOrder` correctly calculates total order volume and count.
  - Verifies `LiveTrading` handles successful orders and validation errors (e.g., negative quantity).

  ### License

  This project is licensed under the MIT License - Ricardo Vega 2026
