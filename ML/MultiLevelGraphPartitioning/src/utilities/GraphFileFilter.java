package utilities;

import java.io.File;
import java.io.FileFilter;

public class GraphFileFilter implements FileFilter {

	public boolean accept(File file) {
		if (file.isDirectory()) {
			return false;
		} else {
			String path = file.getAbsolutePath().toLowerCase();
			if (path.endsWith(".graph")) {
				return true;
			}
		}
		return false;
	}

}
