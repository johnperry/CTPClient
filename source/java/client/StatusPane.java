/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package client;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

public class StatusPane extends JPanel {

	static StatusPane pane = null;
	JLabel status;

    public static StatusPane getInstance() {
		return pane;
	}

	public static StatusPane getInstance(String s, Color c) {
		if (pane == null) pane = new StatusPane(s, c);
		return pane;
	}

	protected StatusPane(String s, Color c) {
		super();
		setLayout(new FlowLayout(FlowLayout.LEFT));
		Border lowered = BorderFactory.createLoweredBevelBorder();
		Border empty = BorderFactory.createEmptyBorder(1,1,1,1);
		Border compound = BorderFactory.createCompoundBorder(lowered, empty);
		setBorder(compound);
		setBackground(c);
		status = new JLabel(s);
		this.add(status);
	}

	public void setText(String s) {
		final String ss = s;
		Runnable display = new Runnable() {
			public void run() {
				status.setText(ss);
			}
		};
		SwingUtilities.invokeLater(display);
	}

}
