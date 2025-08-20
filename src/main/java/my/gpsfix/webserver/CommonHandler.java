package my.gpsfix.webserver;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class CommonHandler implements HttpHandler {

    void sendResponse(HttpExchange exchange, int code, String message) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, message.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(message.getBytes());
        }
    }


    void sendResponse(HttpExchange exchange, int code, Path pathToFile, String extraFileName) throws IOException {
        // Устанавливаем CORS заголовки
        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.set("Access-Control-Allow-Origin", "*");
        responseHeaders.set("Content-Type", "application/octet-stream");
        responseHeaders.set("Content-Disposition", "attachment; filename=\"" + extraFileName + "\"");
        responseHeaders.set("Content-Length", String.valueOf(Files.size(pathToFile)));
        responseHeaders.set("X-File-Name", extraFileName);

        exchange.sendResponseHeaders(code, Files.size(pathToFile));

        // Отправляем обработанный файл
        try (InputStream fis = Files.newInputStream(pathToFile);
             OutputStream os = exchange.getResponseBody()) {

            byte[] buffer2 = new byte[4096];
            int bytesRead2;

            while ((bytesRead2 = fis.read(buffer2)) != -1) {
                os.write(buffer2, 0, bytesRead2);
            }
        }
    }
}
