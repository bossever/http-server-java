import java.net.ServerSocket;
import java.net.Socket;

import http.Client;

public class Main {

    private static final int PORT = 4221;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.printf("Listening on port %d for connections...%n", PORT);
            serverSocket.setReuseAddress(true);
            final Socket socket = serverSocket.accept();
            final Client client = new Client(socket);
            client.run();
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }
}
