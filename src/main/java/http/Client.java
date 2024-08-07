package http;

import http.encoding.Encoding;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
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
      final Response compressedResponse = middleware(request, response);

      System.out.println(compressedResponse);
      send(compressedResponse, bufferedOutputStream);
    } catch (IOException e) {
      System.out.printf("Connection %d returned an error %s%n", id, e.getMessage());
      e.printStackTrace();
    }
  }

  private Request parse(BufferedInputStream inputStream) throws IOException {
    // GET /user-agent HTTP/1.1\r\n
    // Host: localhost:4221\r\n
    // User-Agent: foobar/1.2.3\r\n
    // Accept: */\*\r\n
    // \r\n
    //
    // Request body
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
    final Headers headers = new Headers();
    final String[] requestLine = reader.readLine().split(" ");
    final Method method = Method.valueOf(requestLine[0]);
    final String path = requestLine[1];
    final String version = requestLine[2];
    final Request request;

    if (!path.startsWith("/")) {
      throw new IllegalStateException("Path does not start with forward-slash: %s".formatted(path));
    }

    if (!HTTP_1_1.equals(version)) {
      throw new IllegalStateException("Unsupported HTTP version: %s".formatted(version));
    }

    String line;
    reader.readLine(); // Skip the empty line after the request line

    while (!(line = reader.readLine()).isEmpty()) {
      String[] header = line.split(":");

      if (header.length < 2) {
        throw new IllegalStateException("Header is missing value: %s".formatted(line));
      } else if (header.length == 2) {
          headers.set(header[0], header[1].strip());
      } else {
        headers.set(header[0], String.join(":", Arrays.copyOfRange(header, 1, header.length)).strip());
      }
    }

    if (method.equals(Method.POST)) {
      final int contentLength = headers.getContentLength();
      final byte[] body = new byte[contentLength];

      for (int i = 0; i < contentLength; i++) {
        int byteRead = reader.read();
        if (byteRead == -1) {
          throw new IOException("Unexpected end of stream");
        }
        body[i] = (byte) byteRead;
      }
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

  private Response handlePost(Request request) throws IOException {
    Matcher match = FILES_PATTERN.matcher(request.path());
    if (match.find()) {
      final String filename = match.group(1);

      try (OutputStream outputStream = new FileOutputStream(new File(WORKING_DIRECTORY, filename))
      ) {
        outputStream.write(request.body());
        return Response.status(Status.CREATED);
      }
    }
    return Response.status(Status.NOT_FOUND);
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

  private Response middleware(Request request, Response response) throws IOException {
    List<String> encodings = request.headers().getEncodingsStack();

    while (!encodings.isEmpty()) {
      String encoding = encodings.getFirst().toLowerCase();

      if ((Headers.SUPPORTED_ENCODINGS).contains(encoding)) {
        // encode
        byte[] encodedBody = Encoding.encode(encoding, response.body());
        Headers newHeaders = new Headers(response.headers());
        newHeaders.set(Headers.CONTENT_ENCODING, encoding);
        return new Response(response.status(), newHeaders, encodedBody);
      }
      else {
        encodings.removeFirst();
      }
    }
    return response;
  }
}
