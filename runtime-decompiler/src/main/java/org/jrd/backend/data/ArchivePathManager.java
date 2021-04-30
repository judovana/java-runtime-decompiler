package org.jrd.backend.data;

import java.util.ArrayList;

public class ArchivePathManager {
	private static  ArchivePathManager singleton = null;

	public static ArchivePathManager getInstance() {
		if (singleton == null) {
			singleton = new ArchivePathManager();
		}
		return singleton;
	}

	private String clazz = "";
	private boolean found = false;
	private boolean extracted = false;
	private ArrayList<String> currentPath = new ArrayList<>();

	public void clear() {
		clazz = "";
		found = false;
		extracted = false;
		currentPath = new ArrayList<>();
	}

	public boolean wasFound() {
		return found;
	}

	public boolean isExtracted() {
		return extracted;
	}

	public void setExtracted() {
		extracted = true;
	}

	public void setFound() {
		found = true;
	}

	public void addPathPart(String str) {
		currentPath.add(str);
	}

	public int getPathSize() {
		return currentPath.size();
	}

	public String get(int i) {
		return currentPath.get(i);
	}

	public void removePathPart(String str) {
		currentPath.remove(str);
	}

	public String getCurrentClazz() {
		return clazz;
	}

	public void setClazz(String str) {
		clazz = str;
	}


}
