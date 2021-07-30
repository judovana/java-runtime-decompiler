package org.jrd.backend.data;

import java.util.ArrayList;
import java.util.List;

public class ArchiveManagerOptions {

	private static class ArchiveManagerOptionsHolder {
		private static final ArchiveManagerOptions INSTANCE = new ArchiveManagerOptions();
	}

	public static ArchiveManagerOptions getInstance() {
		return ArchiveManagerOptionsHolder.INSTANCE;
	}

	private final List<String> extensions = new ArrayList<>();
	private static final List<String> defaults = List.of(".zip", ".jar", ".war", ".ear");

	public void addExtension(String s) {
		if (s.startsWith(".")) {
			extensions.add(s);
		} else {
			extensions.add("." + s);
		}
	}

	public void clearExtensions() {
		extensions.clear();
	}

	public List<String> getExtensions() {
		if (extensions.isEmpty()) {
			return defaults;
		}
		return extensions;
	}
}
