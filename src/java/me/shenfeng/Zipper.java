package me.shenfeng;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Zipper {

    public static byte[] zip(String str) throws IOException {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(bs);
        gzip.write(str.getBytes());
        gzip.close();
        return bs.toByteArray();
    }

    public static byte[] zip2(String str, int level) throws IOException {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        GZIPOutputStreamEx gzip = new GZIPOutputStreamEx(bs, 512, level);
        gzip.write(str.getBytes());
        gzip.close();
        return bs.toByteArray();
    }

    public static String unzip(byte[] bytes, int offset, final int length) throws IOException {
        byte[] header = Extrator.header;
        byte buffer[] = new byte[length + header.length];
        for (int i = 0; i < header.length; i++) {
            buffer[i] = header[i];
        }

        for (int i = 0; i < length; ++i) {
            buffer[i + header.length] = bytes[i + offset];
        }

        byte[] result = new byte[1024 * 100];

        GZIPInputStream gi = new GZIPInputStream(new ByteArrayInputStream(buffer));
        int b = 0;
        int index = 0;
        while ((b = gi.read()) != -1) {
            result[index++] = (byte) b;
        }
        return new String(result, 0, index);
    }
}
