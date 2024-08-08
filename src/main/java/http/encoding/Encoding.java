package http.encoding;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class Encoding {
    public static byte[] encode(String encoding, byte[] payload) throws IOException {
        if (encoding.equals("gzip")) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(payload);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
            byte[] buffer = new byte[512];
            int bytesRead = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                gzipOutputStream.write(buffer, 0, bytesRead);
            }
            gzipOutputStream.finish();
            byte[] compressedPayload = outputStream.toByteArray();
            System.out.printf("GZIP compression finished, final payload size: %d bytes", compressedPayload.length);
            return compressedPayload;
        }
        return payload;
    }
}
