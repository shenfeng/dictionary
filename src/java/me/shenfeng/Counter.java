package me.shenfeng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Counter<V> {
	private Map<V, Integer> counter = new HashMap<V, Integer>();

	public Counter() {

	}

	public void put(V k) {
		Integer c = counter.get(k);
		if (c == null)
			c = 0;
		counter.put(k, c + 1);
	}

	public ArrayList<Entry<V, Integer>> sortByValue() {
		ArrayList<Entry<V, Integer>> list = new ArrayList<Map.Entry<V, Integer>>(
				counter.entrySet());
		Collections.sort(list, new Comparator<Entry<V, Integer>>() {
			public int compare(Entry<V, Integer> o1, Entry<V, Integer> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});
		return list;
	}

	public void dump() {
		ArrayList<Entry<V, Integer>> list = sortByValue();
		for (Entry<V, Integer> entry : list) {
			System.out.println(entry);
		}
	}



}
