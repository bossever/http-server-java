package http;

import java.io.*;

public record Response(
    Status status,
    Headers headers,
    byte[] body) {

  public static Response status(Status status) {
    return new Response(
        status,
        new Headers(),
        new byte[0]);
  }

  public static Response plainText(String body) {
    final byte[] bytes = body.getBytes();

    return new Response(
        Status.OK,
        (new Headers()).set(Headers.CONTENT_TYPE, "text/plain"),
        bytes);
  }

  public static Response file(File file) throws IOException {
    try (InputStream inputStream = new FileInputStream(file)) {
      byte[] data = inputStream.readAllBytes();

      return new Response(
              Status.OK,
              (new Headers().set(Headers.CONTENT_TYPE, "application/octet-stream").set(Headers.CONTENT_LENGTH, String.valueOf(data.length))),
              data
      );
    } catch (FileNotFoundException exception) {
      System.out.println(exception.getMessage());
      return status(Status.NOT_FOUND);
    }
  }

  @Override
  public String toString() {
    String formatted = "%n%s %s%n%s%n%s%n".formatted(Client.HTTP_1_1, status.toString(), headers.toString(), new String(body));

    return "\n"
            + "———————————RESPONSE———————————"
            + formatted
            + "——————————————————————————————"
            + "\n";
  }
}
