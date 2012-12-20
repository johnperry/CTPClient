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

public class FileName extends JLabel {

	File file;
	
	public FileName(File file) {
		super(file.getName());
		this.file = file;
		setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
		setForeground( Color.BLACK );
	}
	
	public File getFile() {
		return file;
	}
}

