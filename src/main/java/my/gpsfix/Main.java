package my.gpsfix;

import my.gpsfix.webserver.Server;

import java.io.File;
import java.io.IOException;

public class Main {
    // TODO надо будет переписать архитектуру Processor на фабрику
    // TODO У FITpt и GPXpt - выделить общего родителя и передавать его в FilterStrategy
    // TODO переписать все System.out.println на Logger
    // TODO выделить sendResponse куда нибудь отдельно, может сделать абстрактный класс CommonHandler и унаследовать от него другие обработчики
    // TODO версию хранить в отдельном классе
    public static void main(String[] args) throws IOException {
        System.out.println("Version: 0.2.0");
        if (args.length == 0) {
            Server server = new Server(80);
            server.Start();
        } else if (args.length == 2) {
            String inputFile = args[0];
            String outputFile = args[1];
            // определение типа обработчика от типа файла,
            FileTypeDetector.FileType fileType = FileTypeDetector.detect(new File(inputFile));
            if (fileType == FileTypeDetector.FileType.GPX) {
                GPXProcessor gpxProcessor = new GPXProcessor();
                try {
                    gpxProcessor.load(inputFile);
                    gpxProcessor.process();
                    gpxProcessor.save(outputFile);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if (fileType == FileTypeDetector.FileType.FIT) {
                FITProcessor fitProcessor = new FITProcessor();
                try {
                    fitProcessor.load(inputFile);
                    fitProcessor.process();
                    fitProcessor.save(outputFile);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                System.out.println("file type is not supported");
                return;
            }

        } else {
            System.out.println("Usage: java fixmytrack <input.fit> <output.fit> to fix trackfile");
            System.out.println("   or: java fixmytrack without arguments to start web server");
        }

    }
}