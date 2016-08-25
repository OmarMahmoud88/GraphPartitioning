package utilities;

import java.io.File;
import java.io.FileFilter;

public class ClassFileFilter implements FileFilter{

	@Override
	public boolean accept(File file) {
		if (file.isDirectory()) {
			return false;
		} else {
			String path = file.getAbsolutePath().toLowerCase();
			if (path.endsWith(".class")) {
				return true;
			}
		}
		return false;
	}
}
