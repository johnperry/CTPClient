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

public class StudyName extends JLabel {

	FileName fileName;

	public StudyName(FileName fileName) {
		super( fileName.getPatientName()
				+ " [" + fileName.getPatientID() + "] "
				+ fileName.getDate()
				+ " " + fileName.getModality() );
		this.fileName = fileName;
		setFont( new Font( "Monospaced", Font.BOLD, 16 ) );
		setForeground( Color.blue );
	}

}

