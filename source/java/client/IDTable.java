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

public class IDTable extends Hashtable<String,String> {

	public IDTable() {
		super();
	}

	public void save(Component parent) {
		//Offer to save the idTable if it isn't empty
		if (size() > 0) {
			JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle("Select a file to save the list of patients transmitted");
			if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
				StringBuffer sb = new StringBuffer();
				File file = chooser.getSelectedFile();
				if (file != null) {
					if (file.exists()) {
						sb.append(FileUtil.getText(file));
					}
					else {
						sb.append("PatientID,SubjectID\n");
					}
					for (String key : keySet()) {
						String value = get(key);
						sb.append("=\"" + key + "\",=\"" + value + "\"\n");
					}
				}
				FileUtil.setText(file, sb.toString());
			}
		}
	}

}

