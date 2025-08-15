package my.gpsfix.webserver;

import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class Logger {
    private static final String LOG_FILE = "server.log";
    private static final DateTimeFormatter LOG_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static void initializeLogFile() throws IOException {
        Path logPath = Paths.get(LOG_FILE);
        if (!Files.exists(logPath)) {
            try (BufferedWriter writer = Files.newBufferedWriter(logPath,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {

                writer.write("Timestamp|IP Address|Method|URI|User-Agent|File Name|Status|Details");
                writer.newLine();
            }
        }
    }

    static synchronized void logRequest(HttpExchange exchange) {

        String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
        String method = exchange.getRequestMethod();
        String uri = exchange.getRequestURI().toString();
        String userAgent = exchange.getRequestHeaders().getFirst("User-Agent");
        String fileName = exchange.getRequestHeaders().getFirst("X-File-Name");
        String status = "SUCCESS";
        String details = "";

        try (BufferedWriter writer = Files.newBufferedWriter(
                Paths.get(LOG_FILE),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE)) {

            String timestamp = LocalDateTime.now().format(LOG_DATE_FORMAT);
            String logLine = String.format("%s|%s|%s|%s|%s|%s|%s|%s",
                    timestamp,
                    (ip != null) ? ip : "N/A",
                    (method != null) ? method : "N/A",
                    (uri != null) ? uri : "N/A",
                    (userAgent != null) ? userAgent : "N/A",
                    (fileName != null) ? fileName : "N/A",
                    (status != null) ? status : "N/A",
                    (details != null) ? details : "N/A"
            );

            writer.write(logLine);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Ошибка записи в лог-файл: " + e.getMessage());
        }
    }

    private static synchronized void logRequest(
            String ip, String method, String uri,
            String userAgent, String fileName,
            String status, String details) {

        try (BufferedWriter writer = Files.newBufferedWriter(
                Paths.get(LOG_FILE),
                StandardOpenOption.APPEND, StandardOpenOption.WRITE)) {

            String timestamp = LocalDateTime.now().format(LOG_DATE_FORMAT);
            String logLine = String.format("%s|%s|%s|%s|%s|%s|%s|%s",
                    timestamp,
                    (ip != null) ? ip : "N/A",
                    (method != null) ? method : "N/A",
                    (uri != null) ? uri : "N/A",
                    (userAgent != null) ? userAgent : "N/A",
                    (fileName != null) ? fileName : "N/A",
                    (status != null) ? status : "N/A",
                    (details != null) ? details : "N/A"
            );

            writer.write(logLine);
            writer.newLine();

        } catch (IOException e) {
            System.err.println("Ошибка записи в лог-файл: " + e.getMessage());
        }
    }

    static void generateUsageReport() throws IOException {
        Map<String, Integer> ipStats = new HashMap<>();
        Map<String, Integer> fileStats = new HashMap<>();
        int reqCount = 0;
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(LOG_FILE))) {
            reader.readLine(); // Skip header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                reqCount++;
                if (parts.length > 6 && "SUCCESS".equals(parts[6])) {
                    // Статистика по IP
                    ipStats.merge(parts[1], 1, Integer::sum);
                    // Статистика по типам файлов
                    if (parts[5].endsWith(".fit")) {
                        fileStats.merge("FIT", 1, Integer::sum);
                    } else if (parts[5].endsWith(".gpx")) {
                        fileStats.merge("GPX", 1, Integer::sum);
                    }
                }
            }
        }
        // печать отчета
        System.out.println("==================== Statistics =============================");
        System.out.println("total: " + reqCount + " process requests");
        System.out.println("\nIP Address Statistics:");
        ipStats.forEach((ip, count) -> System.out.println(ip + ": " + count + " process requests"));
        System.out.println("\nFile Statistics:");
        fileStats.forEach((file, count) -> System.out.println(file + ": " + count + " uploads"));
        System.out.println("==============================================================");

    }

}
