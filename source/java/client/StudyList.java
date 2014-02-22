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

	LinkedList<Study> list;
	File directory = null;
	boolean radioMode = false;
	boolean anio = false;

	FileFilter filesOnlyFilter = new FilesOnlyFilter();
	FileFilter directoriesOnlyFilter = new DirectoriesOnlyFilter();

	public StudyList(File directory, boolean radioMode, boolean acceptNonImageObjects) {
		this.directory = directory;
		this.radioMode = radioMode;
		this.anio = acceptNonImageObjects;
		list = new LinkedList<Study>();
		listFiles(directory);
		StatusPane.getInstance().setText("Directory: "+directory);
	}

	public void add(Study study) {
		list.add(study);
		study.getCheckBox().addActionListener(this);
	}

	public void actionPerformed(ActionEvent event) {
		StudyCheckBox cb = (StudyCheckBox)event.getSource();
		Study study = cb.getStudy();
		if ((study != null) && cb.isSelected() && radioMode) {
			for (Study s : list) {
				if (!s.equals(study)) s.setSelected(false);
			}
		}
	}

	public void selectFirstStudy() {
		if (list.size() > 0) {
			deselectAll();
			list.getFirst().setSelected(true);
		}
	}

	public void deselectAll() {
		for (Study study : list) {
			study.setSelected(false);
		}
	}

	public Study[] getStudies() {
		Study[] studies = new Study[list.size()];
		return list.toArray(studies);
	}

	private void listFiles(File dir) {
		File[] files = dir.listFiles(filesOnlyFilter);
		if (files.length > 0) {
			FileName[] fileNames = new FileName[files.length];
			for (int i=0; i<files.length; i++) fileNames[i] = new FileName(files[i]);
			Arrays.sort(fileNames);
			Study study = null;
			FileName last = null;
			for (FileName fileName : fileNames) {
				if (fileName.isDICOM() && (anio || fileName.isImage())) {
					if ((last == null) || !fileName.isSameStudy(last)) {
						study = new Study(fileName);
						last = fileName;
						add(study);
					}
					else study.add(fileName);
				}
			}
		}
		files = dir.listFiles(directoriesOnlyFilter);
		for (File file : files) listFiles(file);
	}

	public void display(DirectoryPanel dp) {
		for (Study study : list) {
			study.display(dp);
		}
	}

}
