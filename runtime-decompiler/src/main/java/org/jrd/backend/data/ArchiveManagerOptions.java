package org.jrd.backend.data;

import java.util.ArrayList;

public class ArchiveManagerOptions {
	private static ArrayList<String> extensions = new ArrayList<>();

	public static void addExtension(String s) {
		if (s.startsWith(".")) {
			extensions.add(s);
		} else {
			extensions.add("." + s);
		}
	}

	public static void clearExtensions() {
		extensions.clear();
	}

	public static ArrayList<String> getExtensions() {
		return extensions;
	}
}
