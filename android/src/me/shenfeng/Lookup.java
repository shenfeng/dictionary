package me.shenfeng;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;

public class Lookup {

    private File getTheFile() throws IOException {
        File cached = new File(dict.getCacheDir(), "dbdata");
        if (!cached.exists()) {
            InputStream is = dict.getResources().openRawResource(R.raw.dbdata);

            FileOutputStream fos = new FileOutputStream(cached);
            byte[] buffer = new byte[1024 * 80];
            int read = 0;
            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            is.close();
            fos.close();
        }
        return cached;
    }

    private final Dictionary dict;
    private final MappedByteBuffer buffer;
    private final int word_count;
    private final int[] index_data;

    private static final int GZIP_MAGIC = 0x8b1f;

    static byte gzip_header[] = { (byte) GZIP_MAGIC, // Magic number (short)
            (byte) (GZIP_MAGIC >> 8), // Magic number (short)
            8, // Compression method (CM) Deflater.DEFLATED
            0, // Flags (FLG)
            0, // Modification time MTIME (int)
            0, // Modification time MTIME (int)
            0, // Modification time MTIME (int)
            0, // Modification time MTIME (int)
            0, // Extra flags (XFLG)
            0 // Operating system (OS)
    };

    static int unsigned(byte b) {
        if (b >= 0)
            return b;
        else {
            return 256 + b;
        }
    }

    static int unsignedShort(short s) {
        if (s < 0) {
            return s + 256 * 256;
        }
        return s;
    }

    public Lookup(Dictionary dic) throws IOException {
        this.dict = dic;
        File file = getTheFile();
        long fileSize = file.length();
        buffer = new RandomAccessFile(file, "r").getChannel().map(MapMode.READ_ONLY, 0,
                fileSize);
        word_count = unsignedShort(buffer.getShort());
        index_data = new int[word_count];

        int index = 2; // ignore first 2 bytes: word cout
        int word_index = 0, next_count = 0;
        while (index < fileSize) {
            index_data[word_index++] = index;
            while (buffer.get(index++) != 0) {
                // skip word, word is terminated by \0
            }
            next_count = unsignedShort(buffer.getShort(index));
            if (next_count > 0xe000) { // first bit: is gzipped
                next_count -= 0xe000;
            }
            index += next_count + 2;
        }
    }

    private int strcmp(String src, int index) {
        int end = index;
        while (buffer.get(end++) != 0) {

        }
        byte[] bytes = new byte[end - index - 1];
        buffer.position(index);
        buffer.get(bytes, 0, end - index - 1);
        String s = new String(bytes);
        return src.compareTo(s);
    }

    private String getStr(int index) throws IOException {
        int data_size = unsignedShort(buffer.getShort(index));
        byte[] data;
        buffer.position(index + 2);
        if (data_size > 0xe000) { // first bit: is not gzipped
            data_size -= 0xe000;
            data = new byte[data_size];
            buffer.get(data, 0, data_size);
        } else {
            data = Arrays.copyOf(gzip_header, data_size + gzip_header.length);
            buffer.get(data, gzip_header.length, data_size);
            GZIPInputStream is = new GZIPInputStream(new ByteArrayInputStream(data));
            data = new byte[data_size * 6];
            byte[] tmp = new byte[4096];
            int read = 0, idx = 0;
            while ((read = is.read(tmp)) != -1) {
                System.arraycopy(tmp, 0, data, idx, read);
                idx += read;
            }
            data_size = idx;
        }
        return new String(data, 0, data_size);
    }

    @JavascriptInterface
    public String abc() {
        return "abc";
    }

    @JavascriptInterface
    public String look(String word) {
        byte[] target = word.getBytes();
        int low = 0, high = word_count - 1;
        while (low <= high) {
            int mid = (low + high) >> 1;
            int cmp = strcmp(word, index_data[mid]);
            // printf("%s, %s, %d, mid %d\n", target, data + index_data[mid],
            // cmp, mid);
            if (cmp > 0) {
                low = mid + 1;
            } else if (cmp < 0) {
                high = mid - 1;
            } else {
                int offset = index_data[mid] + target.length + 1;
                try {
                    InputMethodManager imm = (InputMethodManager) dict
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(dict.mWebView.getWindowToken(), 0);
                    return getStr(offset);
                } catch (IOException e) {
                    return "";
                }
            }
        }
        return "";
    }

    public String toString() {
        return "abc";
    }
}
