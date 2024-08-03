package http;

public enum Status {
  OK(200, "OK"),
  NOT_FOUND(404, "Not Found");

  private final int statusCode;
  private final String statusMsg;

  Status(int statusCode, String statusMsg) {
    this.statusCode = statusCode;
    this.statusMsg = statusMsg;
  }

  @Override
  public String toString() {
    return "%d %s".formatted(statusCode, statusMsg);
  }
}
