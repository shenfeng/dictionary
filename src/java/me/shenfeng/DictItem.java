package me.shenfeng;

import java.util.ArrayList;
import java.util.List;

public class DictItem {

    public String word;
    public List<ExplainItem> items = new ArrayList<ExplainItem>();
    public String wordClass;
    public boolean isFreqSpoken1;
    public boolean isFreqWritten1;
    public List<String> imags = new ArrayList<String>();

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

class ExplainItem {
    public String registerlab; // spoken
    public String gram;// countable
    public String meaning;
    public String helpWithMeaning;
    public List<String> examples = new ArrayList<String>();
    public List<GramExa> extas = new ArrayList<GramExa>();

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

class GramExa {
    public String phrase;
    public List<String> examples = new ArrayList<String>();

    public String toString() {
        String s = "   $: " + phrase + "\n";
        for (String e : examples) {
            s += "    >: " + e + "\n";
        }
        return s;
    }
}
