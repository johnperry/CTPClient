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

public class Log {

	StringBuffer log = new StringBuffer();

	public Log() {
		super();
	}

	public void save(Component parent) {
		if (log.length() > 0) {
			JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle("Select a file to save the error log");
			if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
				StringBuffer sb = new StringBuffer();
				File file = chooser.getSelectedFile();
				if (file != null) {
					if (file.exists()) sb.append(FileUtil.getText(file));
				}
				sb.append(log);
				FileUtil.setText(file, sb.toString());
			}
		}
	}

	public void append(String s) {
		log.append(s);
	}

}

