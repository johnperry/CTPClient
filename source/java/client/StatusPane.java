/*---------------------------------------------------------------
*  Copyright 2013 by the Radiological Society of North America
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
	boolean hasRightComponents = false;

    public static StatusPane getInstance() {
		return pane;
	}

	public static StatusPane getInstance(String s, Color c) {
		if (pane == null) pane = new StatusPane(s, c);
		return pane;
	}

	protected StatusPane(String s, Color c) {
		super();
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		Border lowered = BorderFactory.createLoweredBevelBorder();
		Border empty = BorderFactory.createEmptyBorder(1,1,1,1);
		Border compound = BorderFactory.createCompoundBorder(lowered, empty);
		setBorder(compound);
		setBackground(c);
		status = new JLabel(s);
		add(status);
		add(Box.createHorizontalGlue());
	}

	public void setText(String s) {
		if (SwingUtilities.isEventDispatchThread()) {
			status.setText(s);
		}
		else {
			final String ss = s;
			Runnable display = new Runnable() {
				public void run() {
					status.setText(ss);
				}
			};
			SwingUtilities.invokeLater(display);
		}
	}

	public void addRightComponent(Component component) {
		if (hasRightComponents) add(Box.createHorizontalStrut(5));
		add(component);
		hasRightComponents = true;
	}
}
