package http;

import java.util.Arrays;

public record Request(
        Method method,
        String path,
        Headers headers,
        byte[] body) {

    @Override
    public String toString() {
        String formatted;

        if (body != null) {
            formatted = "%n%s %s%n%n%s%n%s%n".formatted(method.toString(), path, headers.toString(), Arrays.toString(body));
        } else {
            formatted = "%n%s %s%n%n%s%n".formatted(method.toString(), path, headers.toString());
        }

        return "\n"
                + "———————————REQUEST————————————"
                + formatted
                + "——————————————————————————————"
                + "\n";
    }
}
