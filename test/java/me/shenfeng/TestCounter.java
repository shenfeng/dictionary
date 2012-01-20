package me.shenfeng;

import org.junit.Test;

public class TestCounter {

	@Test
	public void testCounter() {
		Counter<String> counter = new Counter<String>();
		counter.put("aaa");
		counter.put("bbb");
		counter.put("bbb");
		counter.put("bbb");
		counter.put("aaa");
		counter.dump();
	}
}
