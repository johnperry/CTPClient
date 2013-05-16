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

public class FileName extends JPanel {

	File file;
	JLabel fileName;

	public FileName(File file) {
		super();
		this.file = file;
		setLayout(new RowLayout(0, 0));
		setBackground(Color.white);
		fileName = new JLabel(file.getName());
		fileName.setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
		fileName.setForeground( Color.BLACK );
		add(fileName);
		add(RowLayout.crlf());
		try {
			DicomObject dob = new DicomObject(file);
			String patientName = dob.getPatientName();
			String patientID = dob.getPatientID();
			String modality = dob.getModality();
			String studyDate = dob.getStudyDate();
			String seriesNumber = dob.getSeriesNumber();
			String acquisitionNumber = dob.getAcquisitionNumber();
			String instanceNumber = dob.getInstanceNumber();
			add(new JLabel(patientName + " [" + patientID + "]"));
			add(RowLayout.crlf());
			if (dob.isImage()) {
				String s =  getText("", fixDate(studyDate), " ") + getText("", modality, ": ");
				s += getText("Series:", seriesNumber, " ");
				s += getText("Acquisition:", acquisitionNumber, " ");
				s += getText("Image:", instanceNumber, "");
				add(new JLabel(s));
				add(RowLayout.crlf());
			}
		}
		catch (Exception nonDICOM) { }
	}

	public File getFile() {
		return file;
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
}

