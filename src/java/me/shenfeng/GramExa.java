package me.shenfeng;

import java.util.ArrayList;
import java.util.List;

public class GramExa {
	public String phrase;
	public List<String> examples = new ArrayList<String>();

	public String getP() {
		return phrase;
	}

	public List<String> getE() {
		return examples;
	}

	public String toString() {
		String s = "   $: " + phrase + "\n";
		for (String e : examples) {
			s += "    >: " + e + "\n";
		}
		return s;
	}
}