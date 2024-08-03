import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import http.Client;

public class Main {

    private static final int PORT = 4221;
    private static final String DIRECTORY_ARG = "--directory";
    private static String WORKING_DIRECTORY = ".";

    public static void main(String[] args) {
        if (args.length > 1 && args[0].equals(DIRECTORY_ARG)) {
            WORKING_DIRECTORY = args[1];
            System.out.printf("File upload directory set to %s%n", WORKING_DIRECTORY);
        }

        try (
                ServerSocket serverSocket = new ServerSocket(PORT);
                ExecutorService executorService = Executors.newCachedThreadPool();
        ) {
            System.out.printf("Listening on port %d for connections...%n", PORT);
            serverSocket.setReuseAddress(true);

            while (true) {
                final Socket socket = serverSocket.accept();
                final Client client = new Client(socket, WORKING_DIRECTORY);
                executorService.submit(client);
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }
}
