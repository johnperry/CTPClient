/*---------------------------------------------------------------
*  Copyright 2014 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package client;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

public class StudyList implements ActionListener {

	Hashtable<String,Study> table;
	File directory = null;
	boolean radioMode = false;
	boolean anio = false;

	FileFilter filesOnlyFilter = new FilesOnlyFilter();
	FileFilter directoriesOnlyFilter = new DirectoriesOnlyFilter();

	public StudyList(File directory, boolean radioMode, boolean acceptNonImageObjects) {
		this.directory = directory;
		this.radioMode = radioMode;
		this.anio = acceptNonImageObjects;
		table = new Hashtable<String,Study>();
		addFiles(directory);
		StatusPane.getInstance().setText("Directory: "+directory);
	}

	public void actionPerformed(ActionEvent event) {
		StudyCheckBox cb = (StudyCheckBox)event.getSource();
		Study study = cb.getStudy();
		if ((study != null) && cb.isSelected() && radioMode) {
			for (Study s : table.values()) {
				if (!s.equals(study)) s.setSelected(false);
			}
		}
	}

	public void selectFirstStudy() {
		if (table.size() > 0) {
			deselectAll();
			getStudies()[0].setSelected(true);
		}
	}

	public void deselectAll() {
		for (Study study : table.values()) {
			study.setSelected(false);
		}
	}

	public Study[] getStudies() {
		Study[] studies = new Study[table.size()];
		studies = table.values().toArray(studies);
		Arrays.sort(studies);
		return studies;
	}

	private void addFiles(File dir) {
		File[] files = dir.listFiles(filesOnlyFilter);
		for (File file: files) {
			FileName fileName = new FileName(file);
			if (fileName.isDICOM() && (anio || fileName.isImage())) {
				String siuid = fileName.getStudyInstanceUID();
				Study study = table.get(siuid);
				if (study == null) {
					study = new Study(fileName);
					study.getCheckBox().addActionListener(this);
					table.put(siuid, study);
				}
				else study.add(fileName);
			}
		}
		files = dir.listFiles(directoriesOnlyFilter);
		for (File file : files) addFiles(file);
	}

	public void display(DirectoryPanel dp) {
		for (Study study : getStudies()) {
			study.display(dp);
		}
	}

}
