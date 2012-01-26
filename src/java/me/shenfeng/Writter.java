package me.shenfeng;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
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
		write(items, toJsonStr);
		TreeSet<String> allWords = new TreeSet<String>(items.keySet());
		BufferedWriter writer = new BufferedWriter(new FileWriter(
				"/tmp/allwords"));
		for (String word : allWords) {
			writer.write(word);
			writer.write("\n");
		}
		writer.close();
	}

	public static void write(Map<String, Object> items, IFn toJsonStr)
			throws IOException {
		FileOutputStream fs = new FileOutputStream("/tmp/dbdata");
		int count = items.size();
		fs.write(getBytes(count)); // how many words

		for (Map.Entry<String, Object> entry : items.entrySet()) {
			String word = entry.getKey();
			String json = (String) toJsonStr.invoke(entry.getValue());
			fs.write(word.getBytes());
			fs.write(0); // NULL terminate String@
			byte[] bytes = json.getBytes();
			byte[] gzipped = Zipper.zip2(json, Deflater.BEST_COMPRESSION);
			if (bytes.length < gzipped.length) {
				fs.write(getBytes(bytes.length + 0xe000));
				// fs.write(0); // save unziped
				fs.write(bytes);
			} else {
				fs.write(getBytes(gzipped.length));
				// fs.write(1); // saved zipped
				fs.write(gzipped);
			}
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
		int index = 2;
		byte buffer[] = new byte[1024 * 128];
		int b = 0;
		List<String> list = new ArrayList<String>(50000);
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
			list.add(word);

			if (word.equals("aa")) {
				int size = getShort(bytes, index);
				System.out.println("offset is " + index);

				byte[] header = Extrator.header;
				byte buffer2[] = new byte[size + header.length];
				for (int j = 0; j < header.length; j++) {
					buffer2[j] = header[j];
				}

				for (int j = 0; j < size; ++j) {
					buffer2[j + header.length] = bytes[j + index + 2];
				}
				for (int j = 0; j < buffer2.length; j++) {
					if (j % 16 == 0) {
						System.out.println();
					}
					int by = getUnsigned(buffer2[j]);

					System.out.print(Integer.toHexString(by) + " ");
				}
				System.out.println("======");

			}

			// System.out.println(word);

			int size = getShort(bytes, index);
			System.out.println(word + "\t" + size + "\t" + index);

			index += 2;
			String str = Zipper.unzip(bytes, index, size);
			System.out.println(str);
			index += size;
		}
		int i = 0;
		for (String str : list) {
			System.out.printf("%17s", str);
			if (++i % 8 == 0) {
				System.out.println();
			}
		}
	}
}
