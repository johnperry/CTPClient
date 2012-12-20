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

public class DirectoryPanel extends JPanel {

	public DirectoryPanel() {
		super();
		setLayout(new RowLayout());
		setBackground(Color.WHITE);
	}
	
	public void clear() {
		removeAll();
	}
}
