/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package client;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import java.nio.charset.Charset;
import org.rsna.ui.*;
import org.rsna.util.*;
import org.w3c.dom.*;

public class DialogPanel extends JPanel {

	private static final Color headingColor = Color.BLUE;
	private static final Color paragraphColor = Color.BLACK;
	private static final Font headingFont = new Font( "SansSerif", Font.BOLD, 16 );
	private static final Font paragraphFont = new Font( "SansSerif", Font.PLAIN, 12 );
	private static final Font labelFont = new Font( "SansSerif", Font.BOLD, 12 );
	private static final Font fieldFont = new Font( "Monospaced", Font.PLAIN, 12 );
	private static final int fieldWidth = 30;
	private Hashtable<String,JTextField> fields;
	private String title = "";
	private boolean sessionMode = false;

	public DialogPanel(Document doc, Properties config) {
		super();
		setLayout(new RowLayout());
		fields = new Hashtable<String,JTextField>();

		Element root = doc.getDocumentElement();
		title = root.getAttribute("title").trim();
		sessionMode = root.getAttribute("mode").trim().toLowerCase().equals("session");
		Node child = root.getFirstChild();
		while (child != null) {
			if (child instanceof Element) {
				Element el = (Element)child;
				String name = el.getTagName();

				if (name.equals("h")) addH(el);
				else if (name.equals("p")) addP(el);
				else if (name.equals("param")) addParam(el, config);

			}
			child = child.getNextSibling();
		}
		add(Box.createVerticalStrut(5));
		add(RowLayout.crlf());
	}

	public String getTitle() {
		return title;
	}

	public boolean studyMode() {
		return !sessionMode;
	}

	public boolean sessionMode() {
		return sessionMode;
	}

	public void setProperties(Properties config) {
		for (String name : fields.keySet()) {
			String value = fields.get(name).getText();
			config.setProperty(name, value);
		}
	}

	private void addH(Element el) {
		String text = el.getTextContent().trim();
		if (!text.equals("")) {
			add(Box.createVerticalStrut(5));
			add(RowLayout.crlf());
			JLabel h = new JLabel(text);
			h.setFont(headingFont);
			h.setForeground(headingColor);

			String align = el.getAttribute("align").trim();
			if (align.equals("left")) h.setAlignmentX(0.0f);
			else if (align.equals("right")) h.setAlignmentX(1.0f);
			else h.setAlignmentX(0.5f);

			add(h, new Integer(2));
			add(RowLayout.crlf());
			add(Box.createVerticalStrut(5));
			add(RowLayout.crlf());
		}
	}

	private void addP(Element el) {
		String text = el.getTextContent().trim();
		if (!text.equals("")) {
			String align = el.getAttribute("align").trim();
			String[] lines = text.split("\n");
			for (String line : lines) {
				JLabel t = new JLabel(line.trim());
				t.setFont(paragraphFont);
				t.setForeground(paragraphColor);

				if (align.equals("center")) t.setAlignmentX(0.5f);
				else if (align.equals("right")) t.setAlignmentX(1.0f);
				else t.setAlignmentX(0.0f);

				add(t, new Integer(2));
				add(RowLayout.crlf());
			}
		}
	}

	private void addParam(Element el, Properties config) {
		String name = el.getAttribute("name").trim();
		String label = el.getAttribute("label").trim();
		String value = el.getAttribute("value").trim();
		boolean readonly = el.getAttribute("readonly").trim().equals("yes");

		JLabel jl = new JLabel(label);
		jl.setFont(labelFont);
		add(jl);

		if (value.equals("") && !name.equals("")) value = config.getProperty(name, "");
		if (value == null) value = "";

		if (!readonly) {
			JTextField jtf = new JTextField(value, fieldWidth);
			jtf.setFont(fieldFont);
			add(jtf);
			fields.put(name, jtf);
		}
		else {
			JLabel jlb = new JLabel(value);
			jlb.setFont(fieldFont);
			add(jlb);
		}

		add(RowLayout.crlf());
	}



}
