package my.gpsfix.webserver;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;


public class RootHandler  extends CommonHandler  {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            // корневая страница лежит в ресурсах
            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("index.html")) {
                if (inputStream != null) {
                    // Чтение данных из потока. Например, можно использовать Scanner для чтения построчно:
                    // Scanner scanner = new Scanner(inputStream, "UTF-8");
                    // while (scanner.hasNextLine()) {
                    //     String line = scanner.nextLine();
                    //     // Обработка строки
                    // }
                    // scanner.close();

                    // Или чтение всего файла в массив байт:
                    byte[] bytes = inputStream.readAllBytes();
                    //  String fileContent = new String(bytes, "UTF-8");
                    Headers headers = exchange.getResponseHeaders();
                    headers.set("content-type", "text/html");
                    headers.set("Access-Control-Allow-Origin", "*");
                    exchange.sendResponseHeaders(200, bytes.length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes, 0, bytes.length);
                    } catch (IOException e) {
                        System.out.println("Internal Server Error");
                        sendResponse(exchange, 502, "Internal Server Error");
                    }

                } else {
                    System.out.println("Not Found index page");
                    sendResponse(exchange, 404, "Not Found");
                }
            } catch (IOException e) {
                System.err.println("Ошибка при чтении файла: " + e.getMessage());
                sendResponse(exchange, 502, "Internal Server Error");
            }

            return;
        }

        sendResponse(exchange, 405, "Method Not Allowed");
        System.out.println("Method Not Allowed " + exchange.getRequestMethod());

    }


}
