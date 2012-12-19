/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package client;

import java.io.File;
import java.io.FileFilter;

class DirectoriesOnlyFilter implements FileFilter {

	public DirectoriesOnlyFilter() { };

	public boolean accept(File file) {
		return file.isDirectory();
	}

}

