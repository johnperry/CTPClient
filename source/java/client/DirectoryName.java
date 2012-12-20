/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package client;

import java.awt.*;
import java.io.*;
import javax.swing.*;

public class DirectoryName extends JLabel {

	File dir;

	public DirectoryName(File dir) {
		super(dir.getAbsolutePath());
		this.dir = dir;
		setFont( new Font( "Monospaced", Font.BOLD, 16 ) );
		setForeground( Color.blue );
	}

	public File getDirectory() {
		return dir;
	}
}

