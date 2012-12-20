/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package client;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

public class DirectoryCheckBox extends JCheckBox implements ActionListener {

	Container dp;

	public DirectoryCheckBox(Container dp) {
		super();
		this.dp = dp;
		setSelected(false);
		addActionListener(this);
	}

	public void actionPerformed(ActionEvent event) {
		DirectoryCheckBox source = (DirectoryCheckBox)event.getSource();
		boolean selected = source.isSelected();
		Component[] components = dp.getComponents();
		for (int k=0; k<components.length; k++) {
			if (components[k].equals(source)) {
				for (int i=k+1; i<components.length; i++) {
					Component c = components[i];
					if (c instanceof DirectoryCheckBox) return;
					if (c instanceof FileCheckBox) {
						((FileCheckBox)c).setSelected(selected);
					}
				}
				return;
			}
		}
		return;
	}
}

