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
import java.net.HttpURLConnection;
import java.util.*;
import javax.swing.*;
import org.apache.log4j.*;
import org.rsna.ctp.stdstages.anonymizer.dicom.DAScript;
import org.rsna.ctp.stdstages.anonymizer.LookupTable;
import org.rsna.server.HttpResponse;
import org.rsna.util.HttpUtil;
import org.rsna.util.FileUtil;
import org.rsna.util.StringUtil;

public class CTPClient extends JFrame implements ActionListener {

    static final String title = "CTP Client";
	static final Color bgColor = new Color(0xc6d8f9);

	String destinationURL;

    DirectoryPanel dp;
    //ColorPane cp;
    StatusPane status;
    volatile boolean sending = false;

    InputText destinationField;
    FieldValue directoryPath;
    FieldButton browseButton;
    FieldButton startButton;

    JFileChooser chooser = null;
    File dir = null;

    DAScript daScript;
    Properties daScriptProps;
    Properties daLUTProps;
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

		//Get the properties, including the default title and the args
		config = getProperties(args);

		destinationURL = config.getProperty("url");

		setTitle(config.getProperty("windowTitle"));
		JPanel panel = new JPanel(new BorderLayout());
		getContentPane().add(panel, BorderLayout.CENTER);

		//Set the SSL params
		FileUtil.getFile( new File("keystore"), "/keystore" );
		System.setProperty("javax.net.ssl.keyStore", "keystore");
		System.setProperty("javax.net.ssl.keyStorePassword", "ctpstore");

		//Get the anonymizer script and set the overridden params
		daScriptProps = getDAScriptProps();

		//Get the LookupTable
		daLUTProps = getDALUTProps();

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
		header.add(new TitleLabel(config.getProperty("panelTitle")));
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

		if (!config.getProperty("showURL", "yes").equals("no")) {
			ui.add(new FieldLabel("Destination URL:"));
			ui.add(destinationField);
			ui.add(RowLayout.crlf());
		}

		ui.add(browseButton);
		ui.add(directoryPath);
		ui.add(RowLayout.crlf());

		//Now we need a FlowLayout panel to center the ui panel
		JPanel flow = new JPanel();
		flow.add(ui);
		flow.setBackground(bgColor);

		//Put the flow panel in the north panel of the main panel
		main.add(flow, BorderLayout.NORTH);

		//Put a DirectoryPanel in a scroll pane and put that in the center of the main panel
		dp = new DirectoryPanel();
		JScrollPane sp = new JScrollPane();
		sp.setViewportView(dp);
		sp.getVerticalScrollBar().setBlockIncrement(100);
		sp.getVerticalScrollBar().setUnitIncrement(20);
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
				dp.clear();
				listFiles(dir);
				startButton.setEnabled(true);
			}
			else startButton.setEnabled(false);
		}
		else if (event.getSource().equals(startButton)) {
			startButton.setEnabled(false);
			browseButton.setEnabled(false);
			sending = true;
			//(new SenderThread(dir, destinationField.getText(), dp, daScriptProps, daLUTProps, this)).start();
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

	private Properties getProperties(String[] args) {
		Properties props = new Properties();
		try {
			File configFile = File.createTempFile("CONFIG-", ".properties");
			configFile.delete();
			FileUtil.getFile( configFile, "/config.properties" );

			FileInputStream stream = null;
			try {
				stream = new FileInputStream(configFile);
				props.load(stream);
			}
			catch (Exception ignore) { }
			FileUtil.close(stream);

			//Put in the default title
			props.setProperty("windowTitle", title);
			props.setProperty("panelTitle", title);

			//Add in the args
			for (String arg : args) {
				if (arg.length() >= 2) {
					arg = StringUtil.removeEnclosingQuotes(arg);
					int k = arg.indexOf("=");
					if (k > 0) {
						String name = arg.substring(0,k).trim();
						String value = arg.substring(k+1).trim();
						props.setProperty(name, value);
					}
				}
			}
		}
		catch (Exception noProps) { }
		return props;
	}

	private Properties getDAScriptProps() {
		Properties daScriptProps = new Properties();
		try {
			File daScriptFile = File.createTempFile("DA-", ".script");
			daScriptFile.delete();
			String daScriptName = config.getProperty("daScriptName");
			if (!getTextFileFromServer(daScriptFile, daScriptName)) {
				FileUtil.getFile( daScriptFile, "/DA.script" );
			}
			daScript = DAScript.getInstance(daScriptFile);
			daScriptProps = daScript.toProperties();
			for (String configProp : config.stringPropertyNames()) {
				if (configProp.startsWith("@")) {
					String value = config.getProperty(configProp);
					String key = "param." + configProp.substring(1);
					daScriptProps.setProperty(key, value);
				}
			}
		}
		catch (Exception unable) { }
		return daScriptProps;
	}

	private Properties getDALUTProps() {
		Properties daLUTProps = new Properties();
		try {
			File daLUTFile = File.createTempFile("DA-", ".lut");
			daLUTFile.delete();
			String daLUTName = config.getProperty("daLUTName");
			if (!getTextFileFromServer(daLUTFile, daLUTName)) {
				FileUtil.getFile( daLUTFile, "/LUT.properties" );
			}
			daLUTProps = LookupTable.getProperties(daLUTFile);
			for (String configProp : config.stringPropertyNames()) {
				if (configProp.startsWith("$")) {
					String value = config.getProperty(configProp);
					String key = configProp.substring(1);
					daLUTProps.setProperty(key, value);
				}
			}
		}
		catch (Exception unable) { }
		return daLUTProps;
	}

	private boolean getTextFileFromServer(File file, String nameOnServer) {
		String protocol = config.getProperty("protocol");
		String host = config.getProperty("host");
		String application = config.getProperty("application");
		if ((protocol != null) && (host != null) && (application != null) && (nameOnServer != null)) {
			String url = protocol + "://" + host + "/" + application + "/" + nameOnServer;
			try {
				//Connect to the server
				HttpURLConnection conn = HttpUtil.getConnection(url);
				conn.setRequestMethod("GET");
				conn.setDoOutput(false);
				conn.connect();

				//Get the response
				if (conn.getResponseCode() == HttpResponse.ok) {
					BufferedReader reader =
						new BufferedReader(
							new InputStreamReader( conn.getInputStream(), FileUtil.utf8 ) );
					StringWriter buffer = new StringWriter();
					char[] cbuf = new char[1024];
					int n;
					while ( (n = reader.read(cbuf,0,1024)) != -1 ) buffer.write(cbuf,0,n);
					reader.close();
					FileUtil.setText(file, buffer.toString());
					return true;
				}
			}
			catch (Exception unable) { }
		}
		return false;
	}

	private File getDirectory() {
		if (chooser == null) {
			File here = new File(System.getProperty("user.dir"));
			chooser = new JFileChooser(here);
			chooser.setDialogTitle("Navigate to a directory containing images and click Open");
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		}
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			File dir = chooser.getSelectedFile();
			if ((dir != null) && dir.exists()) return dir;
		}
		return null;
	}

	private void listFiles(File dir) {
		dp.add(new DirectoryCheckBox());
		dp.add(new DirectoryName(dir), RowLayout.span(4));
		dp.add(RowLayout.crlf());
		File[] files = dir.listFiles(new FilesOnlyFilter());
		for (File file : files) {
			FileCheckBox cb = (FileCheckBox)dp.add(new FileCheckBox());
			String name = file.getName().toLowerCase();
			boolean dcm = name.endsWith(".dcm");
			dcm |= name.startsWith("img");
			dcm |= name.startsWith("image");
			dcm |= name.matches("[0-9\\.]+");
			dcm &= !name.endsWith(".jpg");
			dcm &= !name.endsWith(".jpeg");
			dcm &= !name.endsWith(".png");
			cb.setSelected(dcm);

			dp.add(new FileName(file));
			dp.add(new FileSize(file));
			dp.add(new StatusText());
			dp.add(RowLayout.crlf());
		}
		dp.add(Box.createVerticalStrut(10));
		dp.add(RowLayout.crlf());
		files = dir.listFiles(new DirectoriesOnlyFilter());
		for (File file : files) listFiles(file);
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

	class DirectoryCheckBox extends JCheckBox implements ActionListener {
		public DirectoryCheckBox() {
			super();
			setSelected(true);
			addActionListener(this);
		}
		public void actionPerformed(ActionEvent event) {
			DirectoryCheckBox source = (DirectoryCheckBox)event.getSource();
			boolean selected = source.isSelected();
			Component[] components = dp.getComponents();
			for (int k=0; k<components.length; k++) {
				if (components[k].equals(source)) {
					for (int i=k+1; i<components.length; i++) {
						Component c = components[i];
						if (c instanceof DirectoryCheckBox) return;
						if (c instanceof FileCheckBox) {
							((FileCheckBox)c).setSelected(selected);
						}
					}
					return;
				}
			}
			return;
		}
	}

	class DirectoryName extends JLabel {
		File dir;
		public DirectoryName(File dir) {
			super(dir.getAbsolutePath());
			this.dir = dir;
			setFont( new Font( "Monospaced", Font.BOLD, 18 ) );
			setForeground( Color.BLACK );
		}
		public File getDirectory() {
			return dir;
		}
	}

	class FileCheckBox extends JCheckBox {
		public FileCheckBox() {
			super();
			setBorder(BorderFactory.createEmptyBorder(0,20,0,0));
			setSelected(true);
		}
	}

	class FileName extends JLabel {
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

	class FileSize extends JLabel {
		public FileSize(File file) {
			super();
			setText( String.format("%,d", file.length()) );
			setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
			setAlignmentX(1.0f);
		}
	}

	class StatusText extends JLabel {
		public StatusText() {
			super();
			setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
			setForeground( Color.BLACK );
		}
	}

	class DirectoryPanel extends JPanel {
		public DirectoryPanel() {
			super();
			setLayout(new RowLayout());
			setBackground(Color.WHITE);
		}
		public void clear() {
			removeAll();
		}
	}

}
