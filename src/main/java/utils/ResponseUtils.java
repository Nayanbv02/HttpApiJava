package utils;

import java.io.IOException;
import java.io.OutputStream;

public class ResponseUtils {
    public static void sendResponse(OutputStream out, int statusCode, String statusMessage, String body)
            throws IOException {
        String response = "HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n" +
                "Content-Type: application/json\r\n" +
                "\r\n" + body;
        out.write(response.getBytes());
        out.flush();
    }
}