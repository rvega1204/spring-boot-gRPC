package com.rvg.stocktradingclient.controller;

import com.rvg.grpc.StockRequest;
import com.rvg.grpc.StockResponse;
import com.rvg.grpc.StockTradingServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import net.devh.boot.grpc.client.inject.GrpcClient;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.protobuf.util.JsonFormat;

/**
 * REST controller for handling stock price streaming via Server-Sent Events (SSE).
 * Integrates with the gRPC StockTradingService to subscribe to real-time stock price updates.
 *
 * @author Ricardo Vega
 */
@RestController
@RequestMapping("/stocks")
public class StockStreamingController {

    @GrpcClient("stockService")
    private StockTradingServiceGrpc.StockTradingServiceStub stockServiceStub;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Endpoint to subscribe to real-time stock price updates for a given stock symbol.
     * Uses Server-Sent Events (SSE) to stream updates to the client.
     *
     * @param symbol Stock symbol to subscribe to
     * @return SseEmitter for streaming stock price updates
     */
    @GetMapping(value = "/subscribe/{symbol}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeStockPrice(@PathVariable String symbol) {
        SseEmitter emitter = new SseEmitter();
        executor.execute(() -> {
            StockRequest request = StockRequest.newBuilder().setStockSymbol(symbol).build();

            stockServiceStub.subscribeStockPrice(request, new StreamObserver<>() {
                @Override
                public void onNext(StockResponse response) {
                    try {
                        String jsonResponse = JsonFormat.printer().print(response);
                        emitter.send(jsonResponse);
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    emitter.completeWithError(t);
                }

                @Override
                public void onCompleted() {
                    emitter.complete();
                }
            });
        });

        return emitter;
    }
}
