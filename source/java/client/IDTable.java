/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package client;

import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import org.rsna.util.FileUtil;
import org.rsna.util.StringUtil;

public class IDTable {

	Hashtable<String,PatientInfo> table;

	public IDTable() {
		table = new Hashtable<String,PatientInfo>();
	}

	public void put(String phiPatientName, String phiPatientID, String anonPatientID) {
		table.put(phiPatientID, new PatientInfo(phiPatientName, phiPatientID, anonPatientID));
	}

	public void save(Component parent) {
		//Offer to save the idTable if it isn't empty
		if (table.size() > 0) {
			File dir = new File(System.getProperty("user.home"));
			dir = new File(dir, "Documents");
			dir.mkdirs();
			File file = new File(dir, "ID-Table.csv");
			JFileChooser chooser = new JFileChooser(dir);
			chooser.setSelectedFile(file);
			chooser.setDialogTitle("Select a file to save the list of patients transmitted");
			if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
				StringBuffer sb = new StringBuffer();
				file = chooser.getSelectedFile();
				if (file != null) {
					if (file.exists()) {
						sb.append(FileUtil.getText(file));
					}
					else {
						sb.append("PatientName,PatientID,SubjectID,Transmitted\n");
					}
					for (String key : table.keySet()) {
						PatientInfo info = table.get(key);
						sb.append("\"=\"\"" + info.phiPatientName + "\"\"\",");
						sb.append("\"=\"\"" + info.phiPatientID + "\"\"\",");
						sb.append("\"=\"\"" + info.anonPatientID + "\"\"\",");
						sb.append("\"=\"\"" + info.datetime + "\"\"\"");
						sb.append("\n");
					}
				}
				FileUtil.setText(file, sb.toString());
			}
		}
	}

	class PatientInfo {
		String phiPatientName;
		String phiPatientID;
		String anonPatientID;
		String datetime;

		public PatientInfo(String phiPatientName, String phiPatientID, String anonPatientID) {
			this.phiPatientName = phiPatientName.replaceAll(",", " ");
			this.phiPatientID = phiPatientID;
			this.anonPatientID = anonPatientID;
			this.datetime = StringUtil.getDateTime(" @");
		}
	}

}

