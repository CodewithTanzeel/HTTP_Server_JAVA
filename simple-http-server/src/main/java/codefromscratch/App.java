package codefromscratch;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class App {
    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("192.168.1.21", 8000), 0);

            // Share Desktop folder
            server.createContext("/files", new FileHandler("C:/Users/FATTANI COMPUTERS/OneDrive/Desktop/learn-agentic-ai"));

            server.setExecutor(null);
            server.start();

            System.out.println("✅ Server running at http://192.168.1.21:8000/files");
        } catch (IOException e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
    }

    static class FileHandler implements HttpHandler {
        private final Path folderPath;

        public FileHandler(String folder) {
            this.folderPath = Paths.get(folder).toAbsolutePath();
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Remove "/files" from the request
            String requestPath = exchange.getRequestURI().getPath().replaceFirst("/files", "");

            // If requestPath is empty, it means root folder
            Path requested = requestPath.isEmpty()
                    ? folderPath
                    : folderPath.resolve(requestPath.substring(1)).normalize();

            String response;

            // Prevent directory traversal
            if (!requested.startsWith(folderPath)) {
                response = "Access Denied!";
                exchange.sendResponseHeaders(403, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            if (Files.isDirectory(requested)) {
                // List directory
                try (Stream<Path> paths = Files.list(requested)) {
                    response = paths.map(p -> {
                                String name = p.getFileName().toString();
                                String href = exchange.getRequestURI().getPath();
                                if (!href.endsWith("/")) href += "/";
                                return "<a href=\"" + href + name + "\">" + name + "</a>";
                            })
                            .collect(Collectors.joining("<br>"));
                }
                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else if (Files.exists(requested)) {
                // Serve file content
                byte[] data = Files.readAllBytes(requested);
                exchange.sendResponseHeaders(200, data.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(data);
                }
            } else {
                // Not found
                response = "404 Not Found";
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    }
}
