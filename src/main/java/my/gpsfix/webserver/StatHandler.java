package my.gpsfix.webserver;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class StatHandler extends CommonHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            System.out.println("Statistic:");
            Logger.generateUsageReport();
            super.sendResponse(exchange, 200, "ok");
        }
    }

}
