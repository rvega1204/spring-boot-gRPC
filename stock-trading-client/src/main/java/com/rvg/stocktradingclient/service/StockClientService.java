package com.rvg.stocktradingclient.service;

import com.rvg.grpc.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/**
 * Client service for gRPC-based stock trading operations.
 * Communicates with StockTradingService using both blocking and async stubs.
 *
 * - Blocking stub: Used for unary calls requiring an immediate response.
 * - Async stub: Used for streaming calls (server, client, and bidirectional).
 */
@Service
public class StockClientService {

    /** Blocking stub for synchronous unary gRPC calls. */
    @GrpcClient("stockService")
    private StockTradingServiceGrpc.StockTradingServiceBlockingStub stockTradingServiceBlockingStub;

    /** Async stub for non-blocking streaming gRPC calls. */
    @GrpcClient("stockService")
    private StockTradingServiceGrpc.StockTradingServiceStub stockTradingServiceStub;

    /**
     * Unary RPC: Fetches the current price for a given stock symbol.
     *
     * @param stockSymbol The stock ticker symbol (e.g., "AAPL")
     * @return StockResponse with symbol, price, and timestamp
     */
    public StockResponse getStockPrice(String stockSymbol) {
        StockRequest stockRequest = StockRequest.newBuilder()
                .setStockSymbol(stockSymbol)
                .build();

        return stockTradingServiceBlockingStub.getStockPrice(stockRequest);
    }

    /**
     * Server-streaming RPC: Subscribes to real-time price updates for a stock.
     * Server pushes multiple StockResponse messages until stream completes.
     *
     * @param stockSymbol The stock ticker symbol to subscribe to
     */
    public void subscribeStockPrice(String stockSymbol) {
        StockRequest stockRequest = StockRequest.newBuilder()
                .setStockSymbol(stockSymbol)
                .build();

        stockTradingServiceStub.subscribeStockPrice(stockRequest, new StreamObserver<StockResponse>() {

            /** Prints each incoming price update from the server. */
            @Override
            public void onNext(StockResponse stockResponse) {
                System.out.println("Stock Symbol: " + stockResponse.getStockSymbol());
                System.out.println("Price: " + stockResponse.getPrice());
                System.out.println("Timestamp: " + stockResponse.getTimestamp());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error receiving stock price updates: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Completed receiving stock price updates.");
            }
        });
    }

    /**
     * Client-streaming RPC: Sends multiple stock orders to the server in a single stream.
     * Server processes all orders and responds with a single OrderSummary on completion.
     * If any order fails to send, signals the server with onError.
     */
    public void placeBulkOrders() {

        // Response observer receives the final OrderSummary from the server
        StreamObserver<OrderSummary> responseObserver = new StreamObserver<OrderSummary>() {

            /** Prints the order summary received after all orders are processed. */
            @Override
            public void onNext(OrderSummary orderSummary) {
                System.out.println("Received order summary from server:");
                System.out.println("Total Orders: " + orderSummary.getTotalOrders());
                System.out.println("Total Amount: " + orderSummary.getTotalAmount());
                System.out.println("Success Count: " + orderSummary.getSuccessCount());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error receiving order summary: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Completed receiving order summary.");
            }
        };

        // Open the client-streaming channel
        StreamObserver<StockOrder> requestObserver = stockTradingServiceStub.bulkStockOrder(responseObserver);
        try {
            // Send each order individually through the stream
            requestObserver.onNext(
                    StockOrder.newBuilder()
                            .setOrderId("1")
                            .setStockSymbol("AAPL")
                            .setOrderType("BUY")
                            .setPrice(150.5)
                            .setQuantity(10)
                            .build()
            );

            requestObserver.onNext(
                    StockOrder.newBuilder()
                            .setOrderId("2")
                            .setStockSymbol("GOOGL")
                            .setOrderType("SELL")
                            .setPrice(2500.5)
                            .setQuantity(7)
                            .build()
            );

            requestObserver.onNext(
                    StockOrder.newBuilder()
                            .setOrderId("3")
                            .setStockSymbol("TSLA")
                            .setOrderType("BUY")
                            .setPrice(300.0)
                            .setQuantity(5)
                            .build()
            );

            // Signal server that all orders have been sent
            requestObserver.onCompleted();

        } catch (Exception ex) {
            // Propagate error to server to cancel the stream
            requestObserver.onError(ex);
        }
    }

    /**
     * Bidirectional streaming RPC: Opens a live trading session.
     * Client sends 10 stock orders (one per second) and receives a TradeStatus
     * response per order from the server.
     *
     * @throws InterruptedException if the thread is interrupted during sleep between orders
     */
    public void startTrading() throws InterruptedException {

        // Open bidirectional stream; server responds with TradeStatus per order
        StreamObserver<StockOrder> requestObserver = stockTradingServiceStub.liveTrading(new StreamObserver<TradeStatus>() {

            /** Prints the trade status response for each order. */
            @Override
            public void onNext(TradeStatus tradeStatus) {
                System.out.println("Server response: " + tradeStatus);
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error in live trading: " + throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Live trading completed.");
            }
        });

        // Send 10 orders at 1-second intervals to simulate live trading
        for (int i = 1; i <= 10; i++) {
            StockOrder order = StockOrder.newBuilder()
                    .setOrderId("Order-" + i)
                    .setStockSymbol("AAPL")
                    .setOrderType("BUY")
                    .setPrice(150.0 + i)
                    .setQuantity(10 * i)
                    .build();

            requestObserver.onNext(order);
            Thread.sleep(1000);
        }

        // Signal server that no more orders will be sent
        requestObserver.onCompleted();
    }
}