package org.jrd.backend.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArchiveManagerOptions {

	private static class ArchiveManagerOptionsHolder {
		private static final ArchiveManagerOptions INSTANCE = new ArchiveManagerOptions();
	}

	public static ArchiveManagerOptions getInstance() {
		return ArchiveManagerOptionsHolder.INSTANCE;
	}

	private final List<String> extensions = new ArrayList<>();
	private final List<String> defaults = Arrays.asList(".zip", ".jar", ".war", ".ear");
	private boolean defaultsOn = true;

	public void defaultsOn(boolean on) {
		defaultsOn = on;
	}

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
		if (defaultsOn) {
			List<String> ret = new ArrayList<>();
			ret.addAll(extensions);
			ret.addAll(defaults);
			return ret;
		}
		return extensions;
	}
}
