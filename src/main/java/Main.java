import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(4221);) {
            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);
            Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
            System.out.println("Accepted new connection");

            StringBuilder response = buildResponse(clientSocket);
            System.out.println("Server responded with: " + response.toString());

            clientSocket.getOutputStream().write(response.toString().getBytes());
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static StringBuilder buildResponse(Socket clientSocket) throws IOException {
        InputStream input = clientSocket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        String urlPath = reader.readLine().split(" ")[1];

        StringBuilder response = new StringBuilder();

        if (urlPath.equals("/")) {
            // add status to response
            response.append("HTTP/1.1 200 OK\r\n");
        } else {
            response.append("HTTP/1.1 404 Not Found\r\n");
        }
        // add header to response
        response.append("\r\n");
        // add body to response
        response.append("");
        return response;
    }
}
