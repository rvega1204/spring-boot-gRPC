package com.rvg.stocktradingclient;

import com.rvg.stocktradingclient.service.StockClientService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StockTradingClientApplication implements CommandLineRunner {

     private final StockClientService stockClientService;

    public StockTradingClientApplication(StockClientService stockClientService) {
        this.stockClientService = stockClientService;
    }

    @Override
    public void run(String... args) throws Exception {
        // Example usage of the StockClientService to demonstrate gRPC calls
        String stockSymbol = "GOOGL";
        var stockResponse = stockClientService.getStockPrice(stockSymbol);
        System.out.println("Stock Symbol: " + stockResponse.getStockSymbol());
        System.out.println("Price: " + stockResponse.getPrice());
        System.out.println("Timestamp: " + stockResponse.getTimestamp());

        stockClientService.subscribeStockPrice("AAPL");
        stockClientService.placeBulkOrders();
        stockClientService.startTrading();
    }

    public static void main(String[] args) {
        SpringApplication.run(StockTradingClientApplication.class, args);
    }

}
