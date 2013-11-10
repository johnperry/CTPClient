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

public class FileName extends JPanel implements Comparable<FileName> {

	File file;
	String patientName = "";
	String patientID = "";
	String siUID = "";
	String studyDate = "";
	String modality = "";
	int seriesNumberInt = 0;
	int acquisitionNumberInt = 0;
	int instanceNumberInt = 0;

	public FileName(File file) {
		super();
		this.file = file;
		setLayout(new RowLayout(0, 0));
		setBackground(Color.white);
		JLabel fileName = new JLabel(file.getName());
		fileName.setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
		fileName.setForeground( Color.BLACK );
		add(fileName);
		add(RowLayout.crlf());
		try {
			DicomObject dob = new DicomObject(file);
			patientName = dob.getPatientName();
			patientID = dob.getPatientID();
			siUID = dob.getStudyInstanceUID();
			modality = dob.getModality();
			studyDate = dob.getStudyDate();
			String seriesNumber = dob.getSeriesNumber();
			String acquisitionNumber = dob.getAcquisitionNumber();
			String instanceNumber = dob.getInstanceNumber();
			seriesNumberInt = StringUtil.getInt(seriesNumber);
			acquisitionNumberInt = StringUtil.getInt(acquisitionNumber);
			instanceNumberInt = StringUtil.getInt(instanceNumber);

			String s = "";
			if (dob.isImage()) {
				s += getText("Series:", seriesNumber, " ");
				s += getText("Acquisition:", acquisitionNumber, " ");
				s += getText("Image:", instanceNumber, "");
			}
			else s += dob.getSOPClassName();
			add(new JLabel(s));
			add(RowLayout.crlf());
		}
		catch (Exception nonDICOM) { }
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
		return fixDate(studyDate);
	}

	public String getModality() {
		return modality;
	}

	private String fixDate(String s) {
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
		return (this.patientID.equals(fn.patientID));
	}

	public boolean isSameStudy(FileName fn) {
		return isSamePatient(fn) && (this.siUID.equals(fn.siUID));
	}

}

