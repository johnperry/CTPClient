/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package client;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import org.apache.log4j.*;
import org.rsna.util.FileUtil;

public class CTPClient extends JFrame implements ActionListener {

    static final String windowTitle = "CTP Client";
    static final String panelTitle = "CTP Transmission Utility";
	static final Color bgColor = new Color(0xc6d8f9);

	String destinationURL;

    ColorPane cp;
    StatusPane status;
    volatile boolean sending = false;

    InputText destinationField;
    FieldValue directoryPath;
    FieldButton browseButton;
    FieldButton startButton;

    JFileChooser chooser = null;
    File dir = null;

    File daScriptFile;
    File daLUTFile;
    File configFile;

    Properties config;

    String browsePrompt = "Select a directory containing data to transmit.";

    public static void main(String[] args) {
		Logger.getRootLogger().addAppender(
				new ConsoleAppender(
					new PatternLayout("%d{HH:mm:ss} %-5p [%c{1}] %m%n")));
		Logger.getRootLogger().setLevel(Level.ERROR);
        new CTPClient(args);
    }

    public CTPClient(String[] args) {
		super();
		setTitle(windowTitle);
		JPanel panel = new JPanel(new BorderLayout());
		getContentPane().add(panel, BorderLayout.CENTER);

		//Set the SSL params
		FileUtil.getFile( new File("keystore"), "/keystore" );
		System.setProperty("javax.net.ssl.keyStore", "keystore");
		System.setProperty("javax.net.ssl.keyStorePassword", "ctpstore");

		//Get the anonymizer script and the lookup table
		daScriptFile = FileUtil.getFile( new File("DA.script"), "/DA.script" );
		daLUTFile = FileUtil.getFile( new File("LUT.properties"), "/LUT.properties" );

		//Get the properties
		configFile = FileUtil.getFile( new File("config.properties"), "/config.properties" );
		config = getProperties(configFile);
		destinationURL = config.getProperty("url");

		//Make the UI components
		destinationField = new InputText(destinationURL);
		browseButton = new FieldButton("Browse");
		browseButton.addActionListener(this);
		startButton = new FieldButton("Start");
		startButton.setEnabled(false);
		startButton.addActionListener(this);
		directoryPath = new FieldValue(browsePrompt);

		//Make the header panel
		JPanel header = new JPanel();
		header.setBackground(bgColor);
		header.add(new TitleLabel(panelTitle));
		header.setBorder(BorderFactory.createEmptyBorder(10,5,10,5));
		panel.add(header, BorderLayout.NORTH);

		//Make a panel for the input fields and the progress display
		JPanel main = new JPanel();
		main.setLayout(new BorderLayout());

		//Put the input fields in a RowLayout panel
		JPanel ui = new JPanel();
		ui.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
		ui.setBackground(bgColor);
		RowLayout layout = new RowLayout();
		ui.setLayout(layout);

		ui.add(new FieldLabel("Destination URL:"));
		ui.add(destinationField);
		ui.add(RowLayout.crlf());

		ui.add(browseButton);
		ui.add(directoryPath);
		ui.add(RowLayout.crlf());

		//Now we need a FlowLayout panel to center the ui panel
		JPanel flow = new JPanel();
		flow.add(ui);
		flow.setBackground(bgColor);

		//Put the flow panel in the north panel of the main panel
		main.add(flow, BorderLayout.NORTH);

		//Put a ColorPane in a scroll pane and put that in the center of the main panel
		cp = new ColorPane();
		JScrollPane sp = new JScrollPane();
		sp.setViewportView(cp);
		main.add(sp, BorderLayout.CENTER);

		//Put the start button in the south panel of the main panel
		JPanel startPanel = new JPanel();
		startPanel.add(startButton);
		startPanel.setBorder(BorderFactory.createEmptyBorder(3,0,3,0));
		startPanel.setBackground(bgColor);
		main.add(startPanel, BorderLayout.SOUTH);

		//Now put the main panel in the center of the frame
		panel.add(main, BorderLayout.CENTER);

		//Make a footer bar to display status.
		status = StatusPane.getInstance(" ", bgColor);
		panel.add(status, BorderLayout.SOUTH);

        //Catch close requests and check before closing if we are busy sending
        addWindowListener(new WindowCloser(this));

        pack();
        centerFrame();
        setVisible(true);
	}

	public void actionPerformed(ActionEvent event) {
		if (event.getSource().equals(browseButton)) {
			dir = getDirectory();
			if (dir != null) {
				directoryPath.setText(dir.getAbsolutePath());
				listFiles(dir);
				startButton.setEnabled(true);
			}
			else startButton.setEnabled(false);
		}
		else if (event.getSource().equals(startButton)) {
			startButton.setEnabled(false);
			browseButton.setEnabled(false);
			cp.clear();
			sending = true;
			(new SenderThread(dir, destinationField.getText(), cp, daScriptFile, daLUTFile, this)).start();
		}
	}

	public void transmissionComplete() {
		sending = false;
		Runnable enable = new Runnable() {
			public void run() {
				browseButton.setEnabled(true);
				directoryPath.setText(browsePrompt);
			}
		};
		SwingUtilities.invokeLater(enable);
	}

	private Properties getProperties(File file) {
		Properties props = new Properties();
		FileInputStream stream = null;
		try {
			stream = new FileInputStream(file);
			props.load(stream);
		}
		catch (Exception ignore) { }
		FileUtil.close(stream);
		return props;
	}

	private File getDirectory() {
		if (chooser == null) {
			File here = new File(System.getProperty("user.dir"));
			chooser = new JFileChooser(here);
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		}
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			File dir = chooser.getSelectedFile();
			if ((dir != null) && dir.exists()) return dir;
		}
		return null;
	}

	private void listFiles(File dir) {
		cp.clear();
		File[] files = dir.listFiles(new FilesOnlyFilter());
		for (File file : files) {
			cp.println(Color.black, file.getName() + " [" + file.length() + "]");
		}
		StatusPane.getInstance().setText( files.length + " files selected" );
	}

    class WindowCloser extends WindowAdapter {
		private Component parent;
		public WindowCloser(JFrame parent) {
			this.parent = parent;
			parent.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		}
		public void windowClosing(WindowEvent evt) {
			if (sending) {
				int response = JOptionPane.showConfirmDialog(
								parent,
								"Files are still being transmitted.\nAre you sure you want to stop the program?",
								"Are you sure?",
								JOptionPane.YES_NO_OPTION);
				if (response == JOptionPane.YES_OPTION) System.exit(0);
			}
			else System.exit(0);
		}
    }

    private void centerFrame() {
        Toolkit t = getToolkit();
        Dimension scr = t.getScreenSize ();
        setSize(scr.width/2, scr.height/2);
        setLocation (new Point ((scr.width-getSize().width)/2,
                                (scr.height-getSize().height)/2));
    }

	//UI components
	class TitleLabel extends JLabel {
		public TitleLabel(String s) {
			super(s);
			setFont( new Font( "SansSerif", Font.BOLD, 24 ) );
			setForeground( Color.BLUE );
		}
	}

	class InputText extends JTextField {
		public InputText(String s) {
			super(s);
			setFont( new Font("Monospaced",Font.PLAIN,12) );
			Dimension size = getPreferredSize();
			size.width = 400;
			setPreferredSize(size);
		}
	}

	class FieldLabel extends JLabel {
		public FieldLabel(String s) {
			super(s);
			setFont( new Font( "SansSerif", Font.BOLD, 12 ) );
			setForeground( Color.BLUE );
		}
	}

	class FieldValue extends JLabel {
		public FieldValue(String s) {
			super(s);
			setFont( new Font( "Monospaced", Font.BOLD, 12 ) );
			setForeground( Color.BLACK );
		}
	}

	class FieldButton extends JButton {
		public FieldButton(String s) {
			super(s);
			setFont( new Font( "SansSerif", Font.BOLD, 12 ) );
			setForeground( Color.BLUE );
			setAlignmentX(0.5f);
		}
	}

}
