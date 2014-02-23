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
		studyName.addActionListener(this);
		add(fileName);
	}

	public void add(FileName fileName) {
		list.add(fileName);
		fileName.getCheckBox().addActionListener(this);
	}

	public void actionPerformed(ActionEvent event) {
		Object source = event.getSource();
		if (source.equals(cb)) {
			if (cb.isSelected()) selectAll();
			else {
				deselectAll();
				DirectoryPanel dp = getDirectoryPanel();
				if (dp != null) {
					boolean ctrl = (event.getModifiers() & ActionEvent.CTRL_MASK) != 0;
					if (ctrl) dp.setRowVisible(cb, false);
				}
			}

		}
		else if (source.equals(studyName)) {
			cb.doClick();
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

	public void selectAll() {
		DirectoryPanel dp = getDirectoryPanel();
		for (FileName fn : list) {
			fn.setSelected(true);
			if (dp != null) dp.setRowVisible(fn.getCheckBox(), true);
		}
	}

	public void deselectAll() {
		DirectoryPanel dp = getDirectoryPanel();
		for (FileName fn : list) {
			fn.setSelected(false);
			if (dp != null) dp.setRowVisible(fn.getCheckBox(), false);
		}
	}

	private DirectoryPanel getDirectoryPanel() {
		Component container = cb.getParent();
		if ((container != null) && (container instanceof DirectoryPanel)) {
			return (DirectoryPanel)container;
		}
		return null;
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
	}

}
