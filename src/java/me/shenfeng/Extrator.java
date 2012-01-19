package me.shenfeng;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Extrator {

	final static int GZIP_MAGIC = 0x8b1f;

	private final static byte[] header = { (byte) GZIP_MAGIC, // Magic number
																// (short)
			(byte) (GZIP_MAGIC >> 8), // Magic number (short)
			Deflater.DEFLATED, // Compression method (CM)
			0, // Flags (FLG)
			0, // Modification time MTIME (int)
			0, // Modification time MTIME (int)
			0, // Modification time MTIME (int)
			0, // Modification time MTIME (int)
			0, // Extra flags (XFLG)
			0 // Operating system (OS)
	};

	private static String getString(Element doc, String query) {
		Elements select = doc.select(query);
		if (select.size() > 0) {
			return select.get(0).text();
		}
		throw new RuntimeException(query + " returns 0 element");
	}

	private static String maybeGetString(Element doc, String query) {
		Elements select = doc.select(query);
		if (select.size() > 0) {
			return select.get(0).text();
		}
		return null;
	}

	private static List<String> getStrings(Element doc, String query) {
		List<String> result = new ArrayList<String>();
		Elements select = doc.select(query);
		for (Element e : select) {
			result.add(e.text());
		}
		return result;
	}

	public static ByteArrayInputStream e(String str) throws IOException {
		DictItem r = extract(str);
		byte[] zipped = Zipper.zip2(r.toString(), 9);
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		bs.write(header);
		bs.write(zipped);
		ByteArrayInputStream bi = new ByteArrayInputStream(bs.toByteArray());
		return bi;
	}

	public static DictItem extract(String src) {

		DictItem item = new DictItem();

		Document content = Jsoup.parse(src);
		item.word = getString(content, ".headwordSelected");
		item.wordClass = getString(content, ".wordclassSelected");
		item.isFreqSpoken1 = maybeGetString(content, ".freqS") != null;
		item.isFreqWritten1 = maybeGetString(content, ".freqW") != null;

		Elements imgs = content.select(".preview");
		for (Element img : imgs) {
			String imgSrc = img.attr("src");
			String[] split = imgSrc.split("/");
			String s = split[3];
			item.imags.add(s.substring(2, s.length() - 4));
		}

		Elements senses = content.select(".Sense");
		for (Element sense : senses) {
			if (sense.className().contains("Sense")) {
				// System.out.println(sense);
				// System.out.println();

				ExplainItem i = new ExplainItem();
				String gram = maybeGetString(sense, ".GRAM");
				if (gram != null) {
					if (gram.startsWith("[")) {
						gram = gram.substring(1);
					}
					if (gram.endsWith("]")) {
						gram = gram.substring(0, gram.length() - 1);
					}
				}
				i.gram = gram;
				i.meaning = maybeGetString(sense, "ftdef");
				i.helpWithMeaning = maybeGetString(sense, ".SIGNPOST");
				i.examples = getStrings(sense, "> ftexa .EXAMPLE");
				i.registerlab = maybeGetString(sense, ".REGISTERLAB");
				for (Element extraElement : sense.select(".GramExa")) {
					GramExa extra = new GramExa();
					extra.phrase = maybeGetString(extraElement, "strong");
					extra.examples = getStrings(extraElement, ".EXAMPLE");
					i.extas.add(extra);
				}

				item.items.add(i);
			}
		}
		return item;
	}
}
