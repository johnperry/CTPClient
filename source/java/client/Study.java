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
import org.rsna.ui.RowLayout;

public class Study implements ActionListener {

	LinkedList<FileName> list = null;
	StudyCheckBox cb = null;
	StudyName studyName = null;

	public Study(FileName fileName) {
		list = new LinkedList<FileName>();
		cb = new StudyCheckBox();
		cb.addActionListener(this);
		studyName = new StudyName(fileName);
		add(fileName);
	}

	public void add(FileName fileName) {
		list.add(fileName);
		fileName.getCheckBox().addActionListener(this);
	}

	public void actionPerformed(ActionEvent event) {
		JCheckBox cbox = (JCheckBox)event.getSource();
		if (cb.equals(cbox)) {
			if (cb.isSelected()) selectAll();
			else deselectAll();
		}
		else {
			if (!cb.isSelected()) cbox.setSelected(false);
		}
	}

	public void selectAll() {
		for (FileName fn : list) {
			fn.setSelected(true);
		}
	}

	public void deselectAll() {
		for (FileName fn : list) {
			fn.setSelected(false);
		}
	}

	public StudyCheckBox getCheckBox() {
		return cb;
	}

	public boolean isSelected() {
		return cb.isSelected();
	}

	public void setSelected(boolean selected) {
		cb.setSelected(selected);
		if (selected) selectAll();
		else deselectAll();
	}

	public String getName() {
		return studyName.getText();
	}

	public FileName[] getFileNames() {
		FileName[] names = new FileName[list.size()];
		return list.toArray(names);
	}

	public void display(DirectoryPanel dp) {
		cb.setStudy(this);
		dp.add(cb);
		dp.add(studyName, RowLayout.span(4));
		dp.add(RowLayout.crlf());
		for (FileName fn : list) {
			fn.display(dp);
		}
		dp.add(Box.createVerticalStrut(10));
		dp.add(RowLayout.crlf());
	}

}
