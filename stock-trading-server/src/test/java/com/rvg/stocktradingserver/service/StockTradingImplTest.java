package com.rvg.stocktradingserver.service;

import com.rvg.grpc.*;
import com.rvg.stocktradingserver.entity.Stock;
import com.rvg.stocktradingserver.repository.StockRepository;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StockTradingImplTest {

    private StockRepository stockRepository;
    private StockTradingImpl stockTradingImpl;

    @BeforeEach
    void setUp() {
        stockRepository = mock(StockRepository.class);
        stockTradingImpl = new StockTradingImpl(stockRepository);
    }

    @Test
    void getStockPrice_shouldReturnPriceFromRepository() {
        // Arrange
        String symbol = "AAPL";
        Stock stock = new Stock();
        stock.setStockSymbol(symbol);
        stock.setPrice(150.5);
        stock.setLastUpdated(LocalDateTime.parse("2024-01-01T10:00:00"));

        when(stockRepository.findByStockSymbol(symbol)).thenReturn(stock);

        StreamObserver<StockResponse> responseObserver = mock(StreamObserver.class);
        StockRequest request = StockRequest.newBuilder()
                .setStockSymbol(symbol)
                .build();

        // Act
        stockTradingImpl.getStockPrice(request, responseObserver);

        // Assert
        ArgumentCaptor<StockResponse> captor = ArgumentCaptor.forClass(StockResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        StockResponse response = captor.getValue();
        assertThat(response.getStockSymbol()).isEqualTo(symbol);
        assertThat(response.getPrice()).isEqualTo(150.5);
        assertThat(response.getTimestamp()).isEqualTo("2024-01-01T10:00");
    }

    @Test
    void subscribeStockPrice_shouldEmitMultiplePricesAndComplete() {
        // Arrange
        String symbol = "AAPL";
        StreamObserver<StockResponse> responseObserver = mock(StreamObserver.class);
        StockRequest request = StockRequest.newBuilder()
                .setStockSymbol(symbol)
                .build();

        // Act
        stockTradingImpl.subscribeStockPrice(request, responseObserver);

        // Assert
        ArgumentCaptor<StockResponse> captor = ArgumentCaptor.forClass(StockResponse.class);
        verify(responseObserver, atLeastOnce()).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        // Simple sanity check: all messages have the same symbol
        assertThat(captor.getAllValues())
                .isNotEmpty()
                .allMatch(r -> r.getStockSymbol().equals(symbol));
    }

    @Test
    void bulkStockOrder_shouldAggregateOrdersAndReturnSummary() {
        // Arrange
        StreamObserver<OrderSummary> responseObserver = mock(StreamObserver.class);
        StreamObserver<StockOrder> requestObserver =
                stockTradingImpl.bulkStockOrder(responseObserver);

        StockOrder order1 = StockOrder.newBuilder()
                .setOrderId("1")
                .setStockSymbol("AAPL")
                .setPrice(100.0)
                .setQuantity(2)
                .build();

        StockOrder order2 = StockOrder.newBuilder()
                .setOrderId("2")
                .setStockSymbol("GOOGL")
                .setPrice(200.0)
                .setQuantity(3)
                .build();

        // Act
        requestObserver.onNext(order1);
        requestObserver.onNext(order2);
        requestObserver.onCompleted();

        // Assert
        ArgumentCaptor<OrderSummary> captor = ArgumentCaptor.forClass(OrderSummary.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        OrderSummary summary = captor.getValue();
        assertThat(summary.getTotalOrders()).isEqualTo(2);
        assertThat(summary.getSuccessCount()).isEqualTo(2);
        // 2 * 100 + 3 * 200 = 800
        assertThat(summary.getTotalAmount()).isEqualTo(800.0);
    }

    @Test
    void liveTrading_shouldReturnExecutedWhenQuantityIsPositive() {
        // Arrange
        StreamObserver<TradeStatus> responseObserver = mock(StreamObserver.class);
        StreamObserver<StockOrder> requestObserver =
                stockTradingImpl.liveTrading(responseObserver);

        StockOrder order = StockOrder.newBuilder()
                .setOrderId("1")
                .setStockSymbol("AAPL")
                .setQuantity(5)
                .setPrice(100.0)
                .build();

        // Act
        requestObserver.onNext(order);
        requestObserver.onCompleted();

        // Assert
        ArgumentCaptor<TradeStatus> captor = ArgumentCaptor.forClass(TradeStatus.class);
        verify(responseObserver, atLeastOnce()).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        TradeStatus status = captor.getValue();
        assertThat(status.getOrderId()).isEqualTo("1");
        assertThat(status.getStatus()).isEqualTo("EXECUTED");
        assertThat(status.getMessage()).contains("executed successfully");
    }

    @Test
    void liveTrading_shouldReturnFailedWhenQuantityIsZeroOrNegative() {
        // Arrange
        StreamObserver<TradeStatus> responseObserver = mock(StreamObserver.class);
        StreamObserver<StockOrder> requestObserver =
                stockTradingImpl.liveTrading(responseObserver);

        StockOrder order = StockOrder.newBuilder()
                .setOrderId("2")
                .setStockSymbol("AAPL")
                .setQuantity(0)
                .setPrice(100.0)
                .build();

        // Act
        requestObserver.onNext(order);
        requestObserver.onCompleted();

        // Assert
        ArgumentCaptor<TradeStatus> captor = ArgumentCaptor.forClass(TradeStatus.class);
        verify(responseObserver, atLeastOnce()).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        TradeStatus status = captor.getValue();
        assertThat(status.getOrderId()).isEqualTo("2");
        assertThat(status.getStatus()).isEqualTo("FAILED");
        assertThat(status.getMessage()).contains("Quantity must be greater than zero");
    }
}
