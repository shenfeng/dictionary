package me.shenfeng;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

public class TestExtract {

	public DictItem get(String word) throws FileNotFoundException, IOException {
		String html = IOUtils
				.toString(new FileInputStream("test/java/" + word));
		return Extrator.extract(html);
	}

	public DictItem get(File file) throws FileNotFoundException, IOException {
		String html = IOUtils.toString(new FileInputStream(file));
		return Extrator.extract(html);
	}

	@Test
	public void testCurrent2() throws FileNotFoundException, IOException {
		DictItem current = get("current_2");
		Assert.assertEquals("class noun", "noun", current.wordClass);
		Assert.assertEquals("word", "current", current.word);
		Assert.assertEquals("explanation count is 4", 4, current.items.size());
	}

	@Test
	public void testWent() throws Exception {
		DictItem current = get(new File(
				"/home/feng/www.ldoceonline.com/dictionary/went"));

		System.out.println(current);
		System.out.println(current.toString().length());
	}

	@Test
	public void testPage1() throws FileNotFoundException, IOException {
		DictItem current = get("page_1");
		Assert.assertEquals("class noun", "noun", current.wordClass);
		Assert.assertEquals("word", "page", current.word);
		Assert.assertEquals("explanation count is 7", 7, current.items.size());
		Assert.assertTrue(current.isFreqSpoken1);
		Assert.assertTrue(current.isFreqWritten1);
	}

	@Test
	public void testEmail() throws FileNotFoundException, IOException {
		DictItem current = get("email_1");
		System.out.println(current);
		Assert.assertEquals("class noun", "noun", current.wordClass);
		Assert.assertEquals("word", "email", current.word);
		Assert.assertEquals("explanation count is 2", 2, current.items.size());
		Assert.assertFalse(current.isFreqSpoken1);
		Assert.assertFalse(current.isFreqWritten1);
	}

	@Test
	public void testGive1() throws FileNotFoundException, IOException {
		DictItem current = get("give_1");
		Assert.assertEquals("class noun", "verb", current.wordClass);
		Assert.assertEquals("word", "give", current.word);
		Assert.assertEquals("explanation count is 74", 74, current.items.size());
		Assert.assertTrue(current.isFreqSpoken1);
		Assert.assertTrue(current.isFreqWritten1);
	}

	@Test
	public void testEgg1() throws FileNotFoundException, IOException {
		DictItem current = get("egg_1");
		Assert.assertEquals("class noun", "noun", current.wordClass);
		Assert.assertEquals("word", "egg", current.word);
		Assert.assertEquals("explanation count is 8", 8, current.items.size());
		Assert.assertTrue(current.isFreqSpoken1);
		Assert.assertTrue(current.isFreqWritten1);
		Assert.assertEquals("img count 1", 1, current.imags.size());
	}

	@Test
	public void testFact() throws FileNotFoundException, IOException {
		DictItem current = get("fact");
		Assert.assertEquals("class noun", "noun", current.wordClass);
		Assert.assertEquals("word", "fact", current.word);
		Assert.assertEquals("explanation count is 8", 9, current.items.size());
		Assert.assertTrue(current.isFreqSpoken1);
		Assert.assertTrue(current.isFreqWritten1);
		boolean hasSpoken = false;
		for (ExplainItem i : current.items) {
			if (i.registerlab != null) {
				hasSpoken = true;
			}
		}

		Assert.assertTrue(hasSpoken);
	}

	@Test
	public void testBytes() {
		for (int i = 0; i < 255; i++) {
			byte b = (byte) i;
			Assert.assertTrue(i + "", i == Writter.getUnsigned(b));
		}

		for (int i = 32768; i < 65535; i++) {

			byte[] bytes = Writter.getBytes(i);
			Assert.assertTrue(i + "", Writter.getShort(bytes, 0) == i);
		}

	}

	@Test
	public void testCommercially() throws FileNotFoundException, IOException {
		DictItem current = get("commercially");
		Assert.assertEquals("class noun", "adverb", current.wordClass);
		Assert.assertEquals("word", "commercially", current.word);
		Assert.assertEquals("explanation count is 3", 3, current.items.size());
		Assert.assertFalse(current.isFreqSpoken1);
		Assert.assertFalse(current.isFreqWritten1);
	}

	@Test
	public void testShatke1() throws FileNotFoundException, IOException {
		DictItem current = get("shake_1");
		System.out.println(current);
		Assert.assertEquals("class noun", "verb", current.wordClass);
		Assert.assertEquals("word", "turn", current.word);
		Assert.assertEquals("explanation count is 86", 86, current.items.size());
		Assert.assertTrue(current.isFreqSpoken1);
		Assert.assertTrue(current.isFreqWritten1);
	}

	@Test
	public void testTurn1() throws FileNotFoundException, IOException {
		DictItem current = get("turn_1");
		System.out.println(current);
		Assert.assertEquals("class noun", "verb", current.wordClass);
		Assert.assertEquals("word", "turn", current.word);
		Assert.assertEquals("explanation count is 86", 86, current.items.size());
		Assert.assertTrue(current.isFreqSpoken1);
		Assert.assertTrue(current.isFreqWritten1);
	}

	@Test
	public void testParseAll() throws FileNotFoundException, IOException {
		File dir = new File("/home/feng/www.ldoceonline.com/dictionary");
		File[] files = dir.listFiles();
		int length = 0;
		int count = 0;
		int ziped = 0;

		Counter<String> types = new Counter<String>();
		Counter<String> grammer = new Counter<String>();
		// Map<String, Integer> types = new HashMap<String, Integer>();
		// Set<String> types = new HashSet<String>();
		int[] levels = new int[10];
		for (File f : files) {
			if (f.isFile()) {
				try {
					DictItem item = get(f);
					types.put(item.getT());
					count++;
					for (ExplainItem i : item.getL()) {
						grammer.put(i.getG());
					}

					// for (int i : levels) {
					//
					// }
					// String s = item.toString();
					// ziped += Zipper.zip(s).length;

					for (int i = 0; i < levels.length; ++i) {
						// levels[i] += Zipper.zip2(s, i).length;
					}

					// length += s.length();
					// if (s.length() < 400) {
					// System.out.println(s.length() + "\t" + f);
					// }
					if (item.imags.size() > 1) {
						// System.out.println(f + "\t" + item.imags);
					}
				} catch (RuntimeException e) {
					System.out.println(f);
					e.printStackTrace();
				}
			}
		}
		System.out.println("length " + length + " count: " + count + " avg: "
				+ length / count);
		System.out.println(ziped + "\t" + ziped / count);
		for (int i = 0; i < levels.length; i++) {
			System.out.println(i + "\t" + levels[i]);
		}

		// System.out.println("types------------------------------");
		// types.dump();
		// System.out.println("grama-===============================");
		// grammer.dump();

	}
}
