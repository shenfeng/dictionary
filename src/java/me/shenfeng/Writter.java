package me.shenfeng;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.Deflater;

import org.apache.commons.io.IOUtils;

import clojure.lang.IFn;

public class Writter {

	public static void addToMap(Map<String, Object> map, DictItem item) {
		String word = item.word.toLowerCase();
		if (word.equals(item.word)) {
			item.word = null;// just the same
		}
		Object object = map.get(word);

		if (object instanceof DictItem) {
			List<Object> items = new ArrayList<Object>();
			items.add(object);
			items.add(item);
			map.put(word, items);

		} else if (object instanceof List) {
			List<Object> items = (List<Object>) object;
			items.add(item);
		} else {
			map.put(word, item);
		}
	}

	public static void splitToDisk(IFn toJsonStr) throws FileNotFoundException,
			IOException {

		File dir = new File("/home/feng/www.ldoceonline.com/dictionary");
		FileOutputStream fs = new FileOutputStream("/tmp/dbdata");
		Map<String, Object> items = new TreeMap<String, Object>();
		File[] files = dir.listFiles();
		int count = 0;
		for (File f : files) {
			if (f.isFile()) {
				count++;
				String html = IOUtils.toString(new FileInputStream(f));
				DictItem item = Extrator.extract(html);
				addToMap(items, item);
			}
		}
		System.out.println("size: " + items.size() + " files: " + count);
		for (Map.Entry<String, Object> entry : items.entrySet()) {
			String word = entry.getKey();
			Object item = entry.getValue();
			String json = (String) toJsonStr.invoke(item);
			fs.write(word.getBytes());
			fs.write(0); // NULL terminate String
			byte[] bytes = Zipper.zip2(json, Deflater.BEST_COMPRESSION);
			fs.write(getBytes(bytes.length));
			fs.write(bytes);
		}
		fs.close();
	}

	public static int getUnsigned(byte b) {
		if (b >= 0)
			return b;
		else {
			return 256 + b;
		}
	}

	public static byte[] getBytes(int i) {
		byte[] buffer = new byte[2];
		buffer[0] = (byte) (i >> 8);
		buffer[1] = (byte) (i & 0x00ff);
		return buffer;
	}

	public static int getShort(byte[] bytes, int offset) {
		int hi = getUnsigned(bytes[0 + offset]);
		int low = getUnsigned(bytes[1 + offset]);
		return (hi << 8) + low;
	}

	public static void main(String[] args) throws IOException {
		RandomAccessFile ra = new RandomAccessFile("/tmp/dbdata", "r");
		byte bytes[] = new byte[(int) ra.length()];
		ra.readFully(bytes);
		int index = 0;
		byte buffer[] = new byte[1024 * 128];
		int b = 0;
		while (index < bytes.length) {
			int i = 0;
			while (true) {
				b = bytes[index++];
				if (b != 0) {
					buffer[i++] = (byte) b;
				} else {
					break;
				}
			}
			String word = new String(buffer, 0, i);
			System.out.println(word);

			int size = getShort(bytes, index);
			// System.out.println(size);

			index += 2;
			String str = Zipper.unzip(bytes, index, size);
			System.out.println(str);
			index += size;
		}
	}
}
