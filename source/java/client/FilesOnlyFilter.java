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

	boolean dcmOnly = false;

	public FilesOnlyFilter() { this(false); }

	public FilesOnlyFilter(boolean dcmOnly) {
		this.dcmOnly = dcmOnly;
	}

	public boolean accept(File file) {
		if (!file.isFile()) return false;
		if (!dcmOnly) return true;
		String name = file.getName().toLowerCase();
		boolean dcm = name.endsWith(".dcm");
		dcm |= name.startsWith("img");
		dcm |= name.startsWith("image");
		dcm |= name.matches("[0-9\\.]+");
		dcm &= !name.endsWith(".jpg");
		dcm &= !name.endsWith(".jpeg");
		dcm &= !name.endsWith(".png");
		return dcm;
	}

}

