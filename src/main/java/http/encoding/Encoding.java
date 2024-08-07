package http.encoding;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class Encoding {
    public static byte[] encode(String encoding, byte[] body) throws IOException {
        if (encoding.equals("gzip")) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);


        }
    }
}
