package de.bodden.tamiflex.playout;
import java.awt.image.RenderedImage;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Set;

public class Filter	implements FilenameFilter {
		private final Set<String> fileNames;
		public Filter(Set<String> fileNames) {
			this.fileNames = fileNames;
		}
		public boolean accept(File dir, String name) {
		return name.toLowerCase().endsWith(".png") && !fileNames.contains(name);
		}
}
