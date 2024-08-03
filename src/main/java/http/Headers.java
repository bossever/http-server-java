package http;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Headers {
  private final Map<String, String> storage;

  public static final String CONTENT_TYPE = "Content-Type";
  public static final String CONTENT_LENGTH = "Content-Length";
  public static final String USER_AGENT = "User-Agent";

  public Headers() {
    this.storage = new HashMap<String, String>();
  }

  public Headers(Headers headers) {
    this();
    this.storage.putAll(headers.storage);
  }

  public Headers set(String headerName, String headerValue) {
    storage.put(headerName, headerValue);
    return this;
  }

  public int getContentLength() {
    return Integer.parseInt(storage.getOrDefault(CONTENT_LENGTH, "0"));
  }

  public String getUserAgent() {
    return storage.get(USER_AGENT);
  }

  public Set<Map.Entry<String, String>> entrySet() {
    return storage.entrySet();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    for (Map.Entry<String, String> entry : entrySet()) {
      String headerName = entry.getKey();
      String headerValue = entry.getValue();

      sb.append(headerName).append(": ").append(headerValue).append(System.lineSeparator());
    }
    return sb.toString();
  }

}
