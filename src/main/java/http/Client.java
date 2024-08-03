package http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client implements Runnable {
  private static final AtomicInteger ID = new AtomicInteger();
  
  private final int id;
  private final Socket socket;
  private final String WORKING_DIRECTORY;

  public static final String HTTP_1_1 = "HTTP/1.1";
  public static final Pattern ECHO_PATTERN = Pattern.compile("/echo/(.*)");
  public static final Pattern FILES_PATTERN = Pattern.compile("/files/(.*)");
  public static final String CRLF = "\r\n";

  private static final byte[] HTTP_1_1_BYTES = HTTP_1_1.getBytes();
  private static final byte[] CRLF_BYTES = CRLF.getBytes();
  private static final byte SPACE_BYTE = ' ';
  private static final byte[] COLON_SPACE_BYTE = { ':', ' ' };

  public Client(Socket socket, String WORKING_DIRECTORY) throws Exception {
    this.id = ID.incrementAndGet();
    this.socket = socket;
    this.WORKING_DIRECTORY = WORKING_DIRECTORY;
  }

  @Override
  public void run() {
    System.out.printf("Accepted client connection %d%n", id);

    try (socket) {
      final BufferedInputStream bufferedInputStream = new BufferedInputStream(socket.getInputStream());
      final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());

      final Request request = parse(bufferedInputStream);
      final Response response = handle(request);

      send(response, bufferedOutputStream);
    } catch (IOException e) {
      System.out.printf("Connection %d returned an error %s%n", id, e.getMessage());
      e.printStackTrace();
    }
  }

  private Request parse(BufferedInputStream inputStream) {
    // GET /user-agent HTTP/1.1\r\n
    // Host: localhost:4221\r\n
    // User-Agent: foobar/1.2.3\r\n // Read this value
    // Accept: */\*\r\n
    // \r\n
    //
    // Request body (empty)
    final Scanner scanner = new Scanner(inputStream);
    final Headers headers = new Headers();
    final Method method = Method.valueOf(scanner.next());
    final String path = scanner.next();

    if (!path.startsWith("/")) {
      throw new IllegalStateException("Path does not start with forward-slash: %s".formatted(path));
    }
    String version = scanner.next();

    if (!HTTP_1_1.equals(version)) {
      throw new IllegalStateException("Unsupported HTTP version: %s".formatted(version));
    }

    List<Byte> bodyBytesList = new ArrayList<>();
    String line;
    line = scanner.nextLine();

    while (!(line = scanner.nextLine()).isEmpty()) {
      String[] header = line.split(":");

      if (header.length < 2) {
        throw new IllegalStateException("Header is missing value: %s".formatted(line));
      }
      else if (header.length == 2) {
        headers.set(header[0], header[1].strip());
      }
      else {
        headers.set(header[0], String.join(":", Arrays.copyOfRange(header, 1, header.length)).strip());
      }
    }
    final Request request;

    if (method.equals(Method.POST)) {
      while (scanner.hasNextByte()) {
        bodyBytesList.add(scanner.nextByte());
      }
      byte[] body = new byte[bodyBytesList.size()];

      for (int i = 0; i < bodyBytesList.size(); i++) {
        body[i] = bodyBytesList.get(i);
      }
      scanner.close();
      request = new Request(method, path, headers, body);

    } else {
      request = new Request(method, path, headers, null);
    }
    System.out.printf(request.toString());
    return request;
  }

  private Response handle(Request request) throws IOException {
    Response response = switch (request.method()) {
      case GET -> handleGet(request);
      case POST -> handlePost(request);
    };
    System.out.println(response);
    return response;
  }

  private Response handleGet(Request request) throws IOException {
    if (request.path().equals("/")) {
      return Response.status(Status.OK);
    }

    if (request.path().equals("/user-agent")) {
      String userAgent = request.headers().getUserAgent();
      return Response.plainText(userAgent);
    }

    Matcher match = ECHO_PATTERN.matcher(request.path());
    if (match.find()) {
      final String message = match.group(1);
      return Response.plainText(message);
    }

    match = FILES_PATTERN.matcher(request.path());
    if (match.find()) {
      final String filename = match.group(1);
      return Response.file(new File(WORKING_DIRECTORY, filename));
    }

    return Response.status(Status.NOT_FOUND);
  }

  private Response handlePost(Request request) {
    // TODO replace with actual implementation
    return Response.status(Status.OK);
  }

  private void send(Response response, BufferedOutputStream outputStream) throws IOException {
    // HTTP/1.1 200 OK\r\n
    //
    // Content-Type: text/plain\r\n
    // Content-Length: 12\r\n
    // \r\n
    //
    // Response body
    // foobar/1.2.3
    outputStream.write(HTTP_1_1_BYTES);
    outputStream.write(SPACE_BYTE);

    outputStream.write(response.status().toString().getBytes());
    outputStream.write(CRLF_BYTES);

    for (final Entry<String, String> entry : response.headers().entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();

      if (key.equals(Headers.CONTENT_LENGTH)) {
        continue;
      }

      outputStream.write(key.getBytes());
      outputStream.write(COLON_SPACE_BYTE);
      outputStream.write(value.getBytes());
      outputStream.write(CRLF_BYTES);
    }

    final byte[] body = response.body();

    if (body != null) {
      outputStream.write(Headers.CONTENT_LENGTH.getBytes());
      outputStream.write(COLON_SPACE_BYTE);
      outputStream.write(String.valueOf(body.length).getBytes());
      outputStream.write(CRLF_BYTES);
    }

    outputStream.write(CRLF_BYTES);

    if (body != null) {
      outputStream.write(body);
    }

    outputStream.flush();
  }
}
