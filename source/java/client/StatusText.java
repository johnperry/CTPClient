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

public class StatusText extends JLabel {

	public StatusText() {
		super();
		setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
		setForeground( Color.BLACK );
	}

	public void setText(Color c, String s) {
		if (SwingUtilities.isEventDispatchThread()) {
			setForeground(c);
			super.setText(s);
		}
		else {
			final Color cc = c;
			final String ss = s;
			Runnable r = new Runnable() {
				public void run() {
					setForeground(cc);
					setText(ss);
					invalidate();
					validate();
				}
			};
			SwingUtilities.invokeLater(r);
		}
	}

	public void setText(String s) {
		setText(Color.black, s);
	}

}

