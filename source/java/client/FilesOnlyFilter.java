/*---------------------------------------------------------------
*  Copyright 2014 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package client;

import java.io.File;
import java.io.FileFilter;
import org.rsna.ctp.objects.DicomObject;

class FilesOnlyFilter implements FileFilter {

	boolean dicomOnly = false;
	boolean imagesOnly = false;

	public FilesOnlyFilter() { this(false, false); }

	public FilesOnlyFilter(boolean dicomOnly, boolean imagesOnly) {
		this.dicomOnly = dicomOnly;
		this.imagesOnly = imagesOnly;
	}

	public boolean accept(File file) {
		if (!file.isFile()) return false;
		if (!dicomOnly) return true;
		try {
			DicomObject dob = new DicomObject(file);
			if (!imagesOnly) return true;
			else return dob.isImage();
		}
		catch (Exception notDicom) { }
		return false;
	}
}

