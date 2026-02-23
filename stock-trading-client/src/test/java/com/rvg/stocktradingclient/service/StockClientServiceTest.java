package com.rvg.stocktradingclient.service;

import com.rvg.grpc.*;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockClientServiceTest {

    @Mock
    private StockTradingServiceGrpc.StockTradingServiceBlockingStub blockingStub;

    @Mock
    private StockTradingServiceGrpc.StockTradingServiceStub asyncStub;

    private StockClientService service;

    @BeforeEach
    void setUp() {
        service = new StockClientService();
        injectField(service, "stockTradingServiceBlockingStub", blockingStub);
        injectField(service, "stockTradingServiceStub", asyncStub);
    }

    // ─── getStockPrice ───────────────────────────────────────────────────────────

    @Test
    void getStockPrice_shouldReturnResponseFromBlockingStub() {
        // Given
        StockResponse expected = StockResponse.newBuilder()
                .setStockSymbol("AAPL")
                .setPrice(150.5)
                .build();
        when(blockingStub.getStockPrice(any(StockRequest.class))).thenReturn(expected);

        // When
        StockResponse result = service.getStockPrice("AAPL");

        // Then
        assertThat(result.getStockSymbol()).isEqualTo("AAPL");
        assertThat(result.getPrice()).isEqualTo(150.5);
    }

    @Test
    void getStockPrice_shouldCallBlockingStubWithCorrectSymbol() {
        // Given
        when(blockingStub.getStockPrice(any())).thenReturn(StockResponse.getDefaultInstance());

        // When
        service.getStockPrice("GOOGL");

        // Then
        ArgumentCaptor<StockRequest> captor = ArgumentCaptor.forClass(StockRequest.class);
        verify(blockingStub).getStockPrice(captor.capture());
        assertThat(captor.getValue().getStockSymbol()).isEqualTo("GOOGL");
    }

    // ─── subscribeStockPrice ─────────────────────────────────────────────────────

    @Test
    void subscribeStockPrice_shouldCallAsyncStubWithCorrectSymbol() {
        // When
        service.subscribeStockPrice("AAPL");

        // Then
        ArgumentCaptor<StockRequest> captor = ArgumentCaptor.forClass(StockRequest.class);
        verify(asyncStub).subscribeStockPrice(captor.capture(), any(StreamObserver.class));
        assertThat(captor.getValue().getStockSymbol()).isEqualTo("AAPL");
    }

    @Test
    void subscribeStockPrice_onError_shouldNotThrow() {
        // When
        service.subscribeStockPrice("AAPL");

        // Simulate error
        ArgumentCaptor<StreamObserver> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(asyncStub).subscribeStockPrice(any(), captor.capture());

        // Then - no exception thrown
        captor.getValue().onError(new RuntimeException("gRPC error"));
    }

    @Test
    void subscribeStockPrice_onCompleted_shouldNotThrow() {
        // When
        service.subscribeStockPrice("AAPL");

        // Simulate completion
        ArgumentCaptor<StreamObserver> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(asyncStub).subscribeStockPrice(any(), captor.capture());

        // Then - no exception thrown
        captor.getValue().onCompleted();
    }

    // ─── placeBulkOrders ─────────────────────────────────────────────────────────

    @Test
    void placeBulkOrders_shouldSendExactlyThreeOrders() {
        // Given
        StreamObserver<StockOrder> requestObserver = mock(StreamObserver.class);
        when(asyncStub.bulkStockOrder(any())).thenReturn(requestObserver);

        // When
        service.placeBulkOrders();

        // Then
        verify(requestObserver, times(3)).onNext(any(StockOrder.class));
    }

    @Test
    void placeBulkOrders_shouldCompleteStreamAfterAllOrders() {
        // Given
        StreamObserver<StockOrder> requestObserver = mock(StreamObserver.class);
        when(asyncStub.bulkStockOrder(any())).thenReturn(requestObserver);

        // When
        service.placeBulkOrders();

        // Then
        verify(requestObserver).onCompleted();
        verify(requestObserver, never()).onError(any());
    }

    @Test
    void placeBulkOrders_shouldSendCorrectSymbols() {
        // Given
        StreamObserver<StockOrder> requestObserver = mock(StreamObserver.class);
        when(asyncStub.bulkStockOrder(any())).thenReturn(requestObserver);

        // When
        service.placeBulkOrders();

        // Then
        ArgumentCaptor<StockOrder> captor = ArgumentCaptor.forClass(StockOrder.class);
        verify(requestObserver, times(3)).onNext(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(StockOrder::getStockSymbol)
                .containsExactly("AAPL", "GOOGL", "TSLA");
    }

    // ─── startTrading ────────────────────────────────────────────────────────────

    @Test
    void startTrading_shouldCallLiveTradingOnStub() throws InterruptedException {
        // Given
        StreamObserver<StockOrder> requestObserver = mock(StreamObserver.class);
        when(asyncStub.liveTrading(any())).thenReturn(requestObserver);

        // When - NOTE: this test takes ~10s due to Thread.sleep in the loop
        service.startTrading();

        // Then
        verify(asyncStub).liveTrading(any(StreamObserver.class));
        verify(requestObserver, times(10)).onNext(any(StockOrder.class));
        verify(requestObserver).onCompleted();
    }

    // ─── Helper ──────────────────────────────────────────────────────────────────

    private void injectField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject: " + fieldName, e);
        }
    }
}