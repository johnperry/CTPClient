/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package client;

import java.awt.*;
import java.io.*;
import javax.swing.*;
import org.rsna.ui.RowLayout;

public class DirectoryPanel extends JPanel {

	RowLayout layout;
	boolean deleteOnSuccess = false;

	public DirectoryPanel() {
		super();
		layout = new RowLayout();
		setLayout(layout);
		setBackground(Color.WHITE);
		setBorder(BorderFactory.createEmptyBorder());
		setAlignmentX(0.0f);
	}

	public void clear() {
		removeAll();
		invalidate();
		validate();
		repaint();
	}

	public void setDeleteOnSuccess(boolean delete) {
		deleteOnSuccess = delete;
	}

	public boolean getDeleteOnSuccess() {
		return deleteOnSuccess;
	}

	public void setRowVisible(Component startingComponent, boolean visibility) {
		layout.setRowVisible(this, startingComponent, visibility);
	}

}
