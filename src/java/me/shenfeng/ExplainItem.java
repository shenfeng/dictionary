package me.shenfeng;

import java.util.ArrayList;
import java.util.List;

public class ExplainItem {
	public String registerlab; // spoken
	public String gram;// countable
	public String meaning;
	public String helpWithMeaning;
	public List<String> examples = new ArrayList<String>(4);
	public List<GramExa> extas = new ArrayList<GramExa>(2);

	public String getR() {
		return registerlab;
	}

	public String getG() {
		return gram;
	}

	public String getM() {
		return meaning;
	}

	public String getH() {
		return helpWithMeaning;
	}

	public List<String> getE() {
		return examples;
	}

	public List<GramExa> getX() {
		if (extas.size() > 0) // no need to keep extra info, since a lot do not
								// have it
			return extas;
		return null;
	}

	public String toString() {
		String s = "";
		if (helpWithMeaning != null) {
			s += " [" + helpWithMeaning + "] ";
		}
		s += meaning;
		if (gram != null) {
			s += " " + gram;
		}

		s += "\n";
		for (String e : examples) {
			s += "  >: " + e + "\n";
		}
		for (GramExa extra : extas) {
			s += extra;
		}
		return s;
	}
}
