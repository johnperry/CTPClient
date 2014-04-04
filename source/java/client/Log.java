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

	private static Log logInstance = null;
	private StringBuffer log;

	public static Log getInstance() {
		if (logInstance == null) logInstance = new Log();
		return logInstance;
	}

	protected Log() {
		log = new StringBuffer();
	}

	public void save(Component parent) {
		if (log.length() > 0) {
			File dir = new File(System.getProperty("user.home"));
			dir = new File(dir, "Documents");
			dir.mkdirs();
			File file = new File(dir, "CTPClient.log");
			JFileChooser chooser = new JFileChooser(dir);
			chooser.setSelectedFile(file);
			chooser.setDialogTitle("Select a file to save the error log");
			if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
				StringBuffer sb = new StringBuffer();
				file = chooser.getSelectedFile();
				if (file != null) {
					if (file.exists()) sb.append(FileUtil.getText(file));
				}
				sb.append(log);
				FileUtil.setText(file, sb.toString());
			}
		}
	}

	public void append(String s) {
		if (SwingUtilities.isEventDispatchThread()) {
			log.append(s);
		}
		else {
			final String ss = s;
			Runnable r = new Runnable() {
				public void run() {
					log.append(ss);
				}
			};
			SwingUtilities.invokeLater(r);
		}
	}

	public String getText() {
		return log.toString();
	}

}

