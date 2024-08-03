package http;

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
