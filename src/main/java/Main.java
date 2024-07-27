import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);
            Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
            System.out.println("Accepted new connection");

            String response = buildResponse(clientSocket);
            System.out.println("\nServer responded with: \n" + response);

            clientSocket.getOutputStream().write(response.getBytes());
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static String buildResponse(Socket clientSocket) throws IOException {
        StringBuilder header = new StringBuilder();
        String responseBody = "";
        InputStream input = clientSocket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String request = reader.readLine();
        System.out.println("\nHTTP request received: \n" + request);
        String urlPath = request.split(" ")[1];

        StringBuilder response = new StringBuilder();

        if (urlPath.equals("/")) {
            // add status to response
            response.append("HTTP/1.1 200 OK\r\n");
        } else if (urlPath.matches(".*(/echo).*")) {
            response.append("HTTP/1.1 200 OK\r\n");
            responseBody = urlPath.split("/")[2];
            header.append("Content-Type: text/plain\r\n");
            header.append("Content-Length: ").append(responseBody.length()).append("\r\n");
        } else {
            response.append("HTTP/1.1 404 Not Found\r\n");
        }
        // add header to response
        response.append(header).append("\r\n");
        // add body to response
        response.append(responseBody).append("\r\n");
        return response.toString();
    }
}
