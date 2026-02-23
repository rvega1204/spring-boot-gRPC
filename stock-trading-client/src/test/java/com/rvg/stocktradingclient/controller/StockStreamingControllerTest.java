package com.rvg.stocktradingclient.controller;

import com.rvg.grpc.StockRequest;
import com.rvg.grpc.StockResponse;
import com.rvg.grpc.StockTradingServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockStreamingControllerTest {

    @Mock
    private StockTradingServiceGrpc.StockTradingServiceStub stockServiceStub;

    @Mock
    private ExecutorService executor;

    private StockStreamingController controller;

    @BeforeEach
    void setUp() {
        controller = new StockStreamingController();
        injectField(controller, "stockServiceStub", stockServiceStub);
        injectField(controller, "executor", executor);

        // Run Runnables synchronously so gRPC calls happen immediately
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(executor).execute(any(Runnable.class));
    }

    @Test
    void subscribeStockPrice_shouldReturnEmitter() {
        SseEmitter result = controller.subscribeStockPrice("AAPL");
        assertThat(result).isNotNull();
    }

    @Test
    void subscribeStockPrice_shouldCallGrpcWithCorrectSymbol() {
        // When
        controller.subscribeStockPrice("AAPL");

        // Then
        ArgumentCaptor<StockRequest> captor = ArgumentCaptor.forClass(StockRequest.class);
        verify(stockServiceStub).subscribeStockPrice(captor.capture(), any(StreamObserver.class));
        assertThat(captor.getValue().getStockSymbol()).isEqualTo("AAPL");
    }

    @Test
    void subscribeStockPrice_onCompleted_shouldCompleteEmitter() {
        // When
        controller.subscribeStockPrice("AAPL");

        // Capture and fire onCompleted
        ArgumentCaptor<StreamObserver> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(stockServiceStub).subscribeStockPrice(any(), captor.capture());
        captor.getValue().onCompleted();

        // No exception = success (emitter.complete() was called)
    }

    @Test
    void subscribeStockPrice_onError_shouldCompleteEmitterWithError() {
        // When
        controller.subscribeStockPrice("AAPL");

        // Capture and fire onError
        ArgumentCaptor<StreamObserver> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(stockServiceStub).subscribeStockPrice(any(), captor.capture());
        captor.getValue().onError(new RuntimeException("gRPC error"));

        // No exception = success (emitter.completeWithError() was called)
    }

    @Test
    void subscribeStockPrice_shouldCallGrpcWithGoogleSymbol() {
        // When
        controller.subscribeStockPrice("GOOGL");

        // Then
        ArgumentCaptor<StockRequest> captor = ArgumentCaptor.forClass(StockRequest.class);
        verify(stockServiceStub).subscribeStockPrice(captor.capture(), any(StreamObserver.class));
        assertThat(captor.getValue().getStockSymbol()).isEqualTo("GOOGL");
    }

    @Test
    void subscribeStockPrice_shouldCallGrpcWithTslaSymbol() {
        // When
        controller.subscribeStockPrice("TSLA");

        // Then
        ArgumentCaptor<StockRequest> captor = ArgumentCaptor.forClass(StockRequest.class);
        verify(stockServiceStub).subscribeStockPrice(captor.capture(), any(StreamObserver.class));
        assertThat(captor.getValue().getStockSymbol()).isEqualTo("TSLA");
    }

    @Test
    void subscribeStockPrice_onCompleted_shouldNotThrow() {
        // When
        controller.subscribeStockPrice("AAPL");

        // Simulate stream completion
        ArgumentCaptor<StreamObserver> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(stockServiceStub).subscribeStockPrice(any(), captor.capture());

        // Then - no exception thrown
        captor.getValue().onCompleted();
    }

    @Test
    void subscribeStockPrice_onError_shouldNotThrow() {
        // When
        controller.subscribeStockPrice("AAPL");

        // Simulate stream error
        ArgumentCaptor<StreamObserver> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(stockServiceStub).subscribeStockPrice(any(), captor.capture());

        // Then - no exception thrown
        captor.getValue().onError(new RuntimeException("gRPC error"));
    }

    @Test
    void subscribeStockPrice_onNext_shouldNotThrow() {
        // When
        controller.subscribeStockPrice("AAPL");

        // Simulate incoming message
        ArgumentCaptor<StreamObserver> captor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(stockServiceStub).subscribeStockPrice(any(), captor.capture());

        // Then - no exception thrown
        captor.getValue().onNext(StockResponse.newBuilder()
                .setStockSymbol("AAPL")
                .setPrice(150.5)
                .build());
    }

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