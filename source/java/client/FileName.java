/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package client;

import java.awt.*;
import java.io.*;
import javax.swing.*;
import org.rsna.ctp.objects.DicomObject;
import org.rsna.ui.RowLayout;
import org.rsna.util.StringUtil;

public class FileName implements Comparable<FileName> {

	File file;
	FileSize fileSize;
	String patientName = "";
	String patientID = "";
	String siUID = "";
	String studyDate = "";
	String modality = "";
	int seriesNumberInt = 0;
	int acquisitionNumberInt = 0;
	int instanceNumberInt = 0;
	boolean isDICOM = false;
	boolean isImage = false;
	FileCheckBox cb = null;
	StatusText statusText = null;
	String description = "";

	public FileName(File file) {
		this.file = file;
		cb = new FileCheckBox();
		statusText = new StatusText();
		fileSize = new FileSize(file);
		try {
			DicomObject dob = new DicomObject(file);
			isDICOM = true;
			isImage = dob.isImage();
			patientName = fixNull(dob.getPatientName());
			patientID = fixNull(dob.getPatientID());
			siUID = fixNull(dob.getStudyInstanceUID());
			modality = fixNull(dob.getModality());
			studyDate = fixDate(dob.getStudyDate());
			String seriesNumber = fixNull(dob.getSeriesNumber());
			String acquisitionNumber = fixNull(dob.getAcquisitionNumber());
			String instanceNumber = fixNull(dob.getInstanceNumber());
			seriesNumberInt = StringUtil.getInt(seriesNumber);
			acquisitionNumberInt = StringUtil.getInt(acquisitionNumber);
			instanceNumberInt = StringUtil.getInt(instanceNumber);
			if (isImage) {
				description += getText("Series:", seriesNumber, " ");
				description += getText("Acquisition:", acquisitionNumber, " ");
				description += getText("Image:", instanceNumber, "");
			}
			else description += fixNull(dob.getSOPClassName());
		}
		catch (Exception nonDICOM) { }
	}

	private String fixNull(String s) {
		return (s == null) ? "" : s;
	}

	public File getFile() {
		return file;
	}

	public String getPatientName() {
		return patientName;
	}

	public String getPatientID() {
		return patientID;
	}

	public String getDate() {
		return studyDate;
	}

	public String getModality() {
		return modality;
	}

	private String fixDate(String s) {
		if (s == null) s = "";
		if (s.length() == 8) {
			s = s.substring(0,4) + "." + s.substring(4,6) + "." + s.substring(6);
		}
		return s;
	}

	private String getText(String prefix, String s, String suffix) {
		if (s.length() != 0) s = prefix + s + suffix;
		return s;
	}

	public int compareTo(FileName fn) {
		if (fn == null)  return 0;
		int c;
		if ( (c = this.patientID.compareTo(fn.patientID)) != 0 ) return c;
		if ( (c = this.studyDate.compareTo(fn.studyDate)) != 0 ) return c;
		if ( (c = this.siUID.compareTo(fn.siUID)) != 0 ) return c;
		if ( (c = this.seriesNumberInt - fn.seriesNumberInt) != 0 ) return c;
		if ( (c = this.acquisitionNumberInt - fn.acquisitionNumberInt) != 0 ) return c;
		if ( (c = this.instanceNumberInt - fn.instanceNumberInt) != 0 ) return c;
		return 0;
 	}

 	public boolean isSamePatient(FileName fn) {
		if (fn == null) return false;
		return (this.patientID.equals(fn.patientID));
	}

	public boolean isSameStudy(FileName fn) {
		if (fn == null) return false;
		return isSamePatient(fn) && (this.siUID.equals(fn.siUID));
	}

	public boolean isDICOM() {
		return isDICOM;
	}

	public boolean isImage() {
		return isImage;
	}

	public void setSelected(boolean selected) {
		cb.setSelected(selected);
	}

	public boolean isSelected() {
		return cb.isSelected();
	}

	public StatusText getStatusText() {
		return statusText;
	}

	public FileCheckBox getCheckBox() {
		return cb;
	}

	public void display(DirectoryPanel dp) {
		JPanel panel = new JPanel();
		panel.setLayout(new RowLayout(0, 0));
		panel.setBackground(Color.white);
		JLabel name = new JLabel(file.getName());
		name.setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
		name.setForeground( Color.BLACK );
		panel.add(name);
		panel.add(RowLayout.crlf());
		panel.add(new JLabel(description));
		panel.add(RowLayout.crlf());

		dp.add(cb);
		dp.add(panel);
		dp.add(Box.createHorizontalStrut(20));
		dp.add(fileSize);
		dp.add(statusText);
		dp.add(RowLayout.crlf());
	}

}
