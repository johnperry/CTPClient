/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package client;

import java.io.File;
import java.io.FileFilter;

class FilesOnlyFilter implements FileFilter {

	public FilesOnlyFilter() { };

	public boolean accept(File file) {
		return file.isFile();
	}

}

