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
        StringBuilder response = new StringBuilder();
        StringBuilder responseHeader = new StringBuilder();
        StringBuilder responseBody = new StringBuilder();

        InputStream input = clientSocket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        String requestStatus = reader.readLine();
        String requestHeader = reader.readLine();
        String urlPath = requestStatus.split(" ")[1];
        System.out.println("\nHTTP request received: \n" + requestStatus);


        if (urlPath.equals("/")) {
            // add status to response
            response.append("HTTP/1.1 200 OK\r\n");
        } else if (urlPath.startsWith("/echo")) {
            response.append("HTTP/1.1 200 OK\r\n");
            responseBody.append(urlPath.split("/")[2]);
            responseHeader.append("Content-Type: text/plain\r\n");
            responseHeader.append("Content-Length: ").append(responseBody.length()).append("\r\n");
        } else if (urlPath.startsWith("/user-agent")) {
            while (!requestHeader.startsWith("User-Agent")) {
                requestHeader = reader.readLine();
            }
            responseBody.append(requestHeader.substring(12));
            responseHeader.append("Content-Type: text/plain\r\n");
            responseHeader.append("Content-Length: ").append(responseBody.length()).append("\r\n");
            System.out.println(requestHeader);
        }
        else {
            response.append("HTTP/1.1 404 Not Found\r\n");
        }
        // add header to response
        response.append(responseHeader).append("\r\n");
        // add body to response
        response.append(responseBody).append("\r\n");
        return response.toString();
    }
}
