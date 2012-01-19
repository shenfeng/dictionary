package me.shenfeng;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
}
