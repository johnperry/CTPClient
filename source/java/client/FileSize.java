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

public class FileSize extends JLabel {

	public FileSize(File file) {
		super();
		setText( String.format("%,d", file.length()) );
		setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
		setAlignmentX(1.0f);
		setAlignmentY(0.0f);
	}
}

