

import java.io.File;
import java.io.FileFilter;

public class DirectoryFilter implements FileFilter {

	@Override
	public boolean accept(File file) {
		if (file.isDirectory())
			return true;

		return false;
	}

}
