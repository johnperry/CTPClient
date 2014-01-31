/*---------------------------------------------------------------
*  Copyright 2013 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package client;

import java.awt.*;
import java.io.File;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import org.rsna.util.FileUtil;

public class AttachedFrame extends JFrame {

	Component parent;
	int width;
	JEditorPane editorPane;

	public AttachedFrame(String title, int width, Color bgColor) {
		super(title);
		this.width = width;
		setBackground(bgColor);
		JScrollPane jsp = new JScrollPane();
		getContentPane().add(jsp,BorderLayout.CENTER);
		editorPane = new JEditorPane();
		editorPane.setContentType("text/html");
		editorPane.setEditable(false);
		editorPane.setBackground(bgColor);
		jsp.setViewportView(editorPane);

		EditorKit kit = editorPane.getEditorKit();
		if (kit instanceof HTMLEditorKit) {
			HTMLEditorKit htmlKit = (HTMLEditorKit)kit;
			StyleSheet sheet = htmlKit.getStyleSheet();
			sheet.addRule("body {font-family: arial; font-size:16;}");
			htmlKit.setStyleSheet(sheet);
		}

	}

	public void setText(File file) {
		String text = FileUtil.getText(file);
		editorPane.setText(text);
		editorPane.setCaretPosition(0);
	}

	public void setText(String text) {
		editorPane.setText(text);
		editorPane.setCaretPosition(0);
	}

	public void attachTo() {
		attachTo(parent);
	}

	public void attachTo(Component component) {
		Dimension componentSize = component.getSize();
		setSize(width, componentSize.height);
		Point componentLocation = component.getLocation();
		int x = componentLocation.x + componentSize.width;
		setLocation(new Point(x, componentLocation.y));
		validate();
	}
}
