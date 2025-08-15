package my.gpsfix.webserver;

import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;

public class Server {
    private HttpServer server;
    private int port;


    public Server(int port) throws IOException {
        this.port = port;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        // Обработчики для различных эндпоинтов
        server.createContext("/", new RootHandler());
        server.createContext("/upload", new UploadHandler());
        server.createContext("/api/v1/fixme", new UploadHandler());
        server.createContext("/stat", new StatHandler());
        //server.createContext("/ping", new PingHandler());
        Logger.initializeLogFile();

    }

    public void Start() {
        if (this.server != null) {
            this.server.start();
            System.out.println("Server started at port:" + this.port);
        }
    }
}

