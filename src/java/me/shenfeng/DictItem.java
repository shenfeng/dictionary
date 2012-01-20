package me.shenfeng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DictItem {

	public String word;
	public List<ExplainItem> items = new ArrayList<ExplainItem>(8);
	public String wordClass;
	public boolean isFreqSpoken1;
	public boolean isFreqWritten1;

	static final Map<String, String> classes;

	static {
		classes = new HashMap<String, String>();
		classes.put("noun", "n.");
		// classes.put("adverb", "adv.");
		classes.put("verb", "v.");
		classes.put("preposition", "prep.");
		classes.put("adjective", "adj.");
		// classes.put("pronoun", "pron.");
	}

	public List<String> imags = new ArrayList<String>(1);

	public String getW() {
		return word;
	}

	public List<ExplainItem> getL() {
		return items;
	}

	public String getT() {
		if (wordClass.isEmpty()) // do no keep empty type in file
			return null;
		// do them first
		wordClass = wordClass.replace("adverb", "adv."); // verb
		wordClass = wordClass.replace("pronoun", "pron."); // noun
		for (Map.Entry<String, String> e : classes.entrySet()) {
			wordClass = wordClass.replace(e.getKey(), e.getValue());
		}
		return wordClass;
	}

	public boolean isFS() {
		return isFreqSpoken1;
	}

	public boolean isFW() {
		return isFreqWritten1;
	}

	public List<String> getI() {
		if (imags.size() > 0)
			return imags;
		return null;
	}

	public String toString() {
		String s = word + ": " + wordClass;
		if (isFreqSpoken1) {
			s += " S1";
		}
		if (isFreqWritten1) {
			s += " W1";
		}
		s += "\n";
		int i = 1;
		for (ExplainItem item : items) {
			s += " " + i + ", " + item;
			i++;
		}
		return s;
	}
}
