package server;

import java.io.*;
import java.net.*;
import java.sql.*;

import utils.DatabaseManager;
import utils.ResponseUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public class RequestHandler implements Runnable {

    private Socket clientSocket;

    public RequestHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                OutputStream out = clientSocket.getOutputStream()) {

            String requestLine = in.readLine();
            if (requestLine == null) {
                return;
            }

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                return;
            }

            String method = parts[0];
            String path = parts[1];

            if (method.equals("GET") && path.equals("/books")) {
                handleGetBooks(out);
            } else if (method.equals("GET") && path.startsWith("/books/")) {
                int id = extractId(path);
                handleGetBookById(out, id);
            } else if (method.equals("POST") && path.equals("/books")) {
                handleCreateBook(out, in);
            } else if (method.equals("PUT") && path.startsWith("/books/")) {
                int id = extractId(path);
                handleUpdateBook(out, in, id);
            } else if (method.equals("DELETE") && path.startsWith("/books/")) {
                int id = extractId(path);
                handleDeleteBook(out, id);
            } else {
                try {
                    ResponseUtils.sendResponse(out, 404, "Not Found", "{}");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int extractId(String path) {
        try {
            return Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void handleGetBooks(OutputStream out) {
        try (Connection conn = DatabaseManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM books")) {

            JSONArray books = new JSONArray();
            while (rs.next()) {
                JSONObject book = new JSONObject();
                book.put("id", rs.getInt("id"));
                book.put("title", rs.getString("title"));
                book.put("author", rs.getString("author"));
                book.put("year", rs.getInt("year"));
                books.put(book);
            }

            try {
                ResponseUtils.sendResponse(out, 200, "OK", books.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                ResponseUtils.sendResponse(out, 500, "Internal Server Error", "{}");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    private void handleGetBookById(OutputStream out, int id) {
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement("SELECT * FROM books WHERE id = ?")) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                JSONObject book = new JSONObject();
                book.put("id", rs.getInt("id"));
                book.put("title", rs.getString("title"));
                book.put("author", rs.getString("author"));
                book.put("year", rs.getInt("year"));

                try {
                    ResponseUtils.sendResponse(out, 200, "OK", book.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    ResponseUtils.sendResponse(out, 404, "Not Found", "{}");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                ResponseUtils.sendResponse(out, 500, "Internal Server Error", "{}");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    private void handleCreateBook(OutputStream out, BufferedReader in) {
        try {
            // Leer el cuerpo de la solicitud sin bloquear la ejecución
            StringBuilder body = new StringBuilder();
            String line;
            while (in.ready() && (line = in.readLine()) != null) {
                body.append(line);
            }

            // Depuración: Imprimir el cuerpo recibido
            String bodyString = body.toString().trim();
            System.out.println("Received JSON: " + bodyString);

            // Verificar que el cuerpo no esté vacío
            if (bodyString.isEmpty()) {
                ResponseUtils.sendResponse(out, 400, "Bad Request", "{\"error\":\"Empty JSON body\"}");
                return;
            }

            // Intentar parsear el JSON
            JSONObject json;
            try {
                json = new JSONObject(bodyString);
            } catch (JSONException e) {
                ResponseUtils.sendResponse(out, 400, "Bad Request", "{\"error\":\"Invalid JSON format\"}");
                return;
            }

            // Validar que el JSON tenga los campos requeridos
            if (!json.has("title") || !json.has("author") || !json.has("year")) {
                ResponseUtils.sendResponse(out, 400, "Bad Request", "{\"error\":\"Missing required fields\"}");
                return;
            }

            String title = json.getString("title");
            String author = json.getString("author");
            int year = json.getInt("year");

            // Insertar el libro en la base de datos
            try (Connection conn = DatabaseManager.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO books (title, author, year) VALUES (?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS)) {

                stmt.setString(1, title);
                stmt.setString(2, author);
                stmt.setInt(3, year);
                stmt.executeUpdate();

                // Obtener el ID generado
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int newId = generatedKeys.getInt(1);
                    json.put("id", newId); // Asignar el ID generado al JSON
                    ResponseUtils.sendResponse(out, 201, "Created", json.toString()); // Enviar la respuesta con el
                                                                                      // libro creado
                } else {
                    ResponseUtils.sendResponse(out, 500, "Internal Server Error",
                            "{\"error\":\"Failed to retrieve generated ID\"}");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            try {
                ResponseUtils.sendResponse(out, 500, "Internal Server Error", "{\"error\":\"IOException occurred\"}");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                ResponseUtils.sendResponse(out, 500, "Internal Server Error",
                        "{\"error\":\"Database error occurred\"}");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    private void handleUpdateBook(OutputStream out, BufferedReader in, int id) {
        try {
            // Leer el cuerpo de la solicitud sin bloquear la ejecución
            StringBuilder body = new StringBuilder();
            String line;
            while (in.ready() && (line = in.readLine()) != null) {
                body.append(line);
            }

            // Depuración: Imprimir el cuerpo recibido
            String bodyString = body.toString().trim();
            System.out.println("Received JSON: " + bodyString);

            // Verificar que el cuerpo no esté vacío
            if (bodyString.isEmpty()) {
                ResponseUtils.sendResponse(out, 400, "Bad Request", "{\"error\":\"Empty JSON body\"}");
                return;
            }

            // Intentar parsear el JSON
            JSONObject json;
            try {
                json = new JSONObject(bodyString);
            } catch (JSONException e) {
                ResponseUtils.sendResponse(out, 400, "Bad Request", "{\"error\":\"Invalid JSON format\"}");
                return;
            }

            // Validar que el JSON tenga los campos requeridos
            if (!json.has("title") || !json.has("author") || !json.has("year")) {
                ResponseUtils.sendResponse(out, 400, "Bad Request", "{\"error\":\"Missing required fields\"}");
                return;
            }

            String title = json.getString("title");
            String author = json.getString("author");
            int year = json.getInt("year");

            // Actualizar el libro en la base de datos
            try (Connection conn = DatabaseManager.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(
                            "UPDATE books SET title = ?, author = ?, year = ? WHERE id = ?")) {

                stmt.setString(1, title);
                stmt.setString(2, author);
                stmt.setInt(3, year);
                stmt.setInt(4, id);

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    json.put("id", id); // Asegúrate de incluir el ID en la respuesta
                    ResponseUtils.sendResponse(out, 200, "OK", json.toString()); // Respuesta con el libro actualizado
                } else {
                    // Si no se encuentra el libro con el ID
                    ResponseUtils.sendResponse(out, 404, "Not Found", "{\"error\":\"Book not found\"}");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                ResponseUtils.sendResponse(out, 500, "Internal Server Error",
                        "{\"error\":\"Database error occurred\"}");
            }
        } catch (IOException e) {
            e.printStackTrace();
            try {
                ResponseUtils.sendResponse(out, 500, "Internal Server Error", "{\"error\":\"IOException occurred\"}");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    private void handleDeleteBook(OutputStream out, int id) {
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement("DELETE FROM books WHERE id = ?")) {

            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                try {
                    ResponseUtils.sendResponse(out, 200, "OK", "{}");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    ResponseUtils.sendResponse(out, 404, "Not Found", "{}");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                ResponseUtils.sendResponse(out, 500, "Internal Server Error", "{}");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}
