package my.gpsfix;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

public class FileTypeDetector {

    public enum FileType {
        GPX,
        FIT,
        OTHER
    }

    // Сигнатура FIT:
    private static final byte[] FIT_MAGIC = {'.', 'F', 'I', 'T'};

    // Маркер UTF-8 BOM (для GPX)
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    public static FileType detect(File file) throws IOException {
        try (InputStream is = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(is)) {

            bis.mark(200); // Помечаем начало для reset()
            byte[] header = new byte[200];
            int bytesRead = bis.read(header);
            // Проверка FIT (требуется минимум 12 байт)
            if (bytesRead >= 12 && isFitFile(header)) {
                return FileType.FIT;
            }
            // Сброс потока для повторного чтения
            bis.reset();
            return checkTextBased(bis);
        }
    }

    private static boolean isFitFile(byte[] header) {
        // Проверяем сигнатуру в трех позициях (8-10 или 4-6)
        boolean fitAt8 = checkFitSequence(header, 8);
        boolean fitAt4 = checkFitSequence(header, 4);

        return fitAt8 || fitAt4;
    }

    private static boolean checkFitSequence(byte[] header, int startIndex) {
        for (int i = 0; i < FIT_MAGIC.length; i++) {
            if (header[startIndex + i] != FIT_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    private static FileType checkTextBased(InputStream is) throws IOException {
        byte[] buffer = new byte[200];
        int bytesRead = is.read(buffer);
        if (bytesRead <= 0) return FileType.OTHER;

        // Удаление BOM при наличии
        String content;
        if (bytesRead >= UTF8_BOM.length &&
                buffer[0] == UTF8_BOM[0] &&
                buffer[1] == UTF8_BOM[1] &&
                buffer[2] == UTF8_BOM[2]) {
            content = new String(buffer, 3, bytesRead - 3, StandardCharsets.UTF_8);
        } else {
            content = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
        }

        // Поиск корневого элемента GPX
        String normalizedContent = content.toLowerCase()
                .replaceAll("[\\u0000-\\u001F]", "")
                .replaceFirst("^\\s+", "");

        if (normalizedContent.startsWith("<?xml")) {
            int gpxPos = normalizedContent.indexOf("<gpx");
            if (gpxPos > 0 && gpxPos < 100) { // <gpx в первых 100 символах
                return FileType.GPX;
            }
        } else if (normalizedContent.startsWith("<gpx")) {
            return FileType.GPX;
        }

        return FileType.OTHER;
    }

}