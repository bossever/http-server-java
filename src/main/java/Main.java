import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(4221);) {
            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);

            Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
            System.out.println("Accepted new connection");
            clientSocket.getOutputStream().write("HTTP/1.1 200 OK\r\n\r\n".getBytes());

            // responding with HTTP status 200
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}
