/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package client;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

public class StudyCheckBox extends JCheckBox implements ActionListener {

	Container dp;

	public StudyCheckBox(Container dp) {
		super();
		this.dp = dp;
		setSelected(false);
		addActionListener(this);
		setBackground(Color.white);
	}

	public void actionPerformed(ActionEvent event) {
		setCheckBoxes();
	}

	public void setCheckBoxes() {

		this.setSelected(true);

		Component[] components = dp.getComponents();
		for (int k=0; k<components.length; k++) {
			Component c = components[k];
			if (c instanceof FileCheckBox) {
				((FileCheckBox)c).setSelected(false);
			}
		}

		for (int k=0; k<components.length; k++) {
			if (components[k].equals(this)) {
				for (int i=k+1; i<components.length; i++) {
					Component c = components[i];
					if (c instanceof StudyCheckBox) return;
					if (c instanceof FileCheckBox) {
						((FileCheckBox)c).setSelected(true);
					}
				}
				return;
			}
		}
		return;
	}
}


