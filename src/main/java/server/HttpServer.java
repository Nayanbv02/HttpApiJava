package server;

import java.io.*;
import java.net.*;

public class HttpServer {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8070)) {
            System.out.println("Server is listening on port 8070");
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new RequestHandler(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}