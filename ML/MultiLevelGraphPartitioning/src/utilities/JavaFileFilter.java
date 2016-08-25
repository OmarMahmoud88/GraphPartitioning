package utilities;

import java.io.File;
import java.io.FileFilter;

public class JavaFileFilter implements FileFilter {

	@Override
	public boolean accept(File file) {
		if (file.isDirectory()) {
			return false;
		} else {
			String path = file.getAbsolutePath().toLowerCase();
			if (path.endsWith(".java")) {
				return true;
			}
		}
		return false;
	}

}
