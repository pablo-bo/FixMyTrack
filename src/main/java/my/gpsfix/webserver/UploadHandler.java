package my.gpsfix.webserver;

import com.sun.net.httpserver.Headers;
import my.gpsfix.FITProcessor;
import my.gpsfix.FileTypeDetector;
import my.gpsfix.FileTypeDetector.FileType;
import my.gpsfix.GPXProcessor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

class UploadHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Обработка CORS preflight запроса (OPTIONS)
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            handleOptionsRequest(exchange);
            return;
        }
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            handlePostRequest(exchange);
            return;
        }
        sendResponse(exchange, 405, "Method Not Allowed");
        System.out.println("Method Not Allowed " + exchange.getRequestMethod());
    }

    private void handlePostRequest(HttpExchange exchange) throws IOException {
        // сначала просто сохраним
        String randomUUID = UUID.randomUUID().toString();
        String tempRecvFileName = randomUUID + ".tmp";

        String tempInputFilename;// = "in.tmp";
        String tempOutputFilename;// = "out.tmp";
        try (InputStream is = exchange.getRequestBody();
             FileOutputStream fos = new FileOutputStream(tempRecvFileName)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.close();
            // определим тип файла и установим имена временных файлов
            File tempFile = new File(tempRecvFileName);//tempFile.getCanonicalPath();
            FileType fileType = FileTypeDetector.detect(tempFile);
            String extFile;
            if (fileType == FileType.GPX) {
                extFile = ".gpx";
            } else if (fileType == FileType.FIT) {
                extFile = ".fit";
            } else {
                System.out.println("file type is not supported");
                sendResponse(exchange, 500, "file type is not supported");
                return;
            }
            // Переименуем принятый файл
            tempInputFilename = "in_" + randomUUID + extFile;
            tempOutputFilename = "out_" + randomUUID + extFile;
            if (!tempFile.renameTo(new File(tempInputFilename))) {
                // TODO надо кинуть исключение
                System.out.println("Ошибка переименов");
            }

            String originalFileName = exchange.getRequestHeaders().getFirst("X-File-Name");
            String fixedFileName = "fix_" + originalFileName;
            //проверим тип файла
            if (fileType == FileType.GPX) {
                System.out.println("START process " + tempInputFilename);
                GPXProcessor gpxProcessor = new GPXProcessor();
                gpxProcessor.load(tempInputFilename);
                gpxProcessor.process();
                gpxProcessor.save(tempOutputFilename);
                System.out.println("DONE!");
            } else if (fileType == FileType.FIT) {
                System.out.println("START process " + tempInputFilename);
                FITProcessor fitProcessor = new FITProcessor();
                fitProcessor.load(tempInputFilename);
                fitProcessor.process();
                fitProcessor.save(tempOutputFilename);
                System.out.println("DONE!");
            }
            // Подготавливаем ответ с обработанным файлом
            Path pathTempOutputFile = Path.of(tempOutputFilename);
            Path pathTempInputFile = Path.of(tempInputFilename);

            sendResponse(exchange, 200, pathTempOutputFile, fixedFileName);
            // Удаляем временные файлы
            Files.deleteIfExists(pathTempInputFile);
            Files.deleteIfExists(pathTempOutputFile);

            Logger.logRequest(exchange);

        } catch (IOException e) {
            sendResponse(exchange, 500, "Error uploading file");
        } catch (TransformerException | ParserConfigurationException | SAXException e) {
            sendResponse(exchange, 500, "Error parsing gpx file");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            sendResponse(exchange, 502, "Internal Server Error");
            throw new RuntimeException(e);
        }

    }

    private void handleOptionsRequest(HttpExchange exchange) throws IOException {
        // Устанавливаем CORS заголовки
        Headers headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "POST, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type, X-File-Name");

        exchange.sendResponseHeaders(204, -1); // No Content
        exchange.getResponseBody().close();
    }

    private void sendResponse(HttpExchange exchange, int code, String message) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, message.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(message.getBytes());
        }
    }

    private void sendResponse(HttpExchange exchange, int code, Path pathToFile, String extraFileName) throws IOException {
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