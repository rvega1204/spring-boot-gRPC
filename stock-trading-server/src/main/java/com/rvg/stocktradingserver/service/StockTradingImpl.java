package com.rvg.stocktradingserver.service;

import com.rvg.grpc.*;
import com.rvg.stocktradingserver.entity.Stock;
import com.rvg.stocktradingserver.repository.StockRepository;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * gRPC service implementation for stock trading operations.
 * Handles unary, server-streaming, client-streaming, and bidirectional streaming RPCs.
 *
 * @author Your Name
 */
@GrpcService
public class StockTradingImpl extends StockTradingServiceGrpc.StockTradingServiceImplBase {

    private final StockRepository stockRepository;

    /**
     * Constructor for dependency injection.
     *
     * @param stockRepository Repository for stock data access
     */
    public StockTradingImpl(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    /**
     * Unary RPC: Get current price for a specific stock symbol.
     *
     * @param request Stock symbol request
     * @param responseObserver Single StockResponse observer
     */
    @Override
    public void getStockPrice(StockRequest request, StreamObserver<StockResponse> responseObserver) {
        String stockSymbol = request.getStockSymbol();
        Stock stockEntity = stockRepository.findByStockSymbol(stockSymbol);

        StockResponse stockResponse = StockResponse.newBuilder()
                .setStockSymbol(stockEntity.getStockSymbol())
                .setPrice(stockEntity.getPrice())
                .setTimestamp(stockEntity.getLastUpdated().toString())
                .build();

        responseObserver.onNext(stockResponse);
        responseObserver.onCompleted();
    }

    /**
     * Server-streaming RPC: Subscribe to real-time stock price updates.
     * Streams 11 random price updates every second for 10 seconds.
     *
     * @param request Stock symbol request
     * @param responseObserver Multiple StockResponse observer
     */
    @Override
    public void subscribeStockPrice(StockRequest request, StreamObserver<StockResponse> responseObserver) {
        String stockSymbol = request.getStockSymbol();
        for (int i = 0; i <= 10; i++) {
            StockResponse stockResponse = StockResponse.newBuilder()
                    .setStockSymbol(stockSymbol)
                    .setPrice(new Random().nextDouble(200.0))
                    .setTimestamp(Instant.now().toString())
                    .build();
            responseObserver.onNext(stockResponse);
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        responseObserver.onCompleted();
    }

    /**
     * Client-streaming RPC: Process multiple bulk stock orders and return summary.
     * Accumulates total orders, amount, and success count.
     *
     * @param responseObserver Single OrderSummary observer
     * @return StreamObserver for multiple StockOrder requests
     */
    @Override
    public StreamObserver<StockOrder> bulkStockOrder(StreamObserver<OrderSummary> responseObserver) {
        return new StreamObserver<StockOrder>() {
            private int totalOrders = 0;
            private double totalAmount = 0.0;
            private int successCount = 0;

            /**
             * Process each incoming stock order.
             * Updates counters and total amount.
             */
            @Override
            public void onNext(StockOrder stockOrder) {
                System.out.println("Received order: " + stockOrder);
                totalOrders++;
                totalAmount += stockOrder.getQuantity() * stockOrder.getPrice();
                successCount++;
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error receiving stock orders: " + t.getMessage());
            }

            /**
             * Finalize bulk operation and send summary.
             */
            @Override
            public void onCompleted() {
                System.out.println("Completed receiving stock orders.");
                OrderSummary orderSummary = OrderSummary.newBuilder()
                        .setTotalOrders(totalOrders)
                        .setTotalAmount(totalAmount)
                        .setSuccessCount(successCount)
                        .build();

                responseObserver.onNext(orderSummary);
                responseObserver.onCompleted();
            }
        };
    }

    /**
     * Bidirectional streaming RPC: Live trading with immediate response per order.
     * Client sends orders continuously, server responds with TradeStatus for each.
     *
     * @param responseObserver Multiple TradeStatus observer
     * @return StreamObserver for continuous StockOrder requests
     */
    @Override
    public StreamObserver<StockOrder> liveTrading(StreamObserver<TradeStatus> responseObserver) {
        return new StreamObserver<StockOrder>() {
            /**
             * Process live trading order and send immediate status response.
             * Validates quantity > 0.
             */
            @Override
            public void onNext(StockOrder stockOrder) {
                System.out.println("Received live order: " + stockOrder);
                String status = "EXECUTED";
                String message = "Order " + stockOrder.getOrderId() +
                        " for " + stockOrder.getStockSymbol() +
                        " executed successfully.";

                if (stockOrder.getQuantity() <= 0) {
                    status = "FAILED";
                    message = "Order " + stockOrder.getOrderId() +
                            " failed: Quantity must be greater than zero.";
                }

                TradeStatus tradeStatus = TradeStatus.newBuilder()
                        .setOrderId(stockOrder.getOrderId())
                        .setStatus(status)
                        .setMessage(message)
                        .setTimestamp(Instant.now().toString())
                        .build();
                responseObserver.onNext(tradeStatus);
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error in live trading: " + t.getMessage());
            }

            /**
             * Complete live trading session.
             */
            @Override
            public void onCompleted() {
                System.out.println("Live trading completed.");
                responseObserver.onCompleted();
            }
        };
    }
}