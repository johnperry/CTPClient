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
import org.rsna.ctp.stdstages.anonymizer.dicom.PixelScript;
import org.rsna.ctp.stdstages.anonymizer.LookupTable;
import org.rsna.server.HttpResponse;
import org.rsna.util.HttpUtil;
import org.rsna.util.FileUtil;
import org.rsna.util.StringUtil;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Document;

public class CTPClient extends JFrame implements ActionListener {

    static final String title = "CTP Client";
	static final Color bgColor = new Color(0xc6d8f9);

    JScrollPane sp;
    DirectoryPanel dp;
    StatusPane status;
    DialogPanel dialog = null;
    volatile boolean sending = false;

    InputText destinationField = null;
    FieldButton browseButton = null;
    FieldButton dialogButton = null;
    FieldButton helpButton = null;
    FieldButton startButton = null;

    JFileChooser chooser = null;
    File dir = null;

    boolean dfEnabled;
    boolean dpaEnabled;

    Properties config;
    DAScript daScript;
    Properties daScriptProps;
    Properties daLUTProps;
    String dfScript;
    PixelScript dpaPixelScript;

    Log log = new Log();
    IDTable idTable = new IDTable();

    String browsePrompt = "Select a directory containing data to transmit.";
    String helpURL;

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

		setTitle(config.getProperty("windowTitle"));
		JPanel panel = new JPanel(new BorderLayout());
		getContentPane().add(panel, BorderLayout.CENTER);

		//Set the SSL params
		getKeystore();

		//Get the DialogPanel if specified
		dialog = getDialogPanel();

		//Get the anonymizer script and set the overridden params
		daScriptProps = getDAScriptPropsObject();

		//Get the LookupTable and set the overridden params
		daLUTProps = getDALUTPropsObject();

		//Get the PixelScript
		dpaPixelScript = getDPAPixelScriptObject();

		//Get the filter script
		dfScript = getDFScriptObject();

		//Set the enables
		dfEnabled = config.getProperty("dfEnabled", "no").trim().equals("yes");
		dpaEnabled = config.getProperty("dpaEnabled", "no").trim().equals("yes");

		//Make the UI components
		String destinationURL = config.getProperty("url");
		destinationURL = (destinationURL != null) ? destinationURL.trim() : "";
		destinationField = new InputText(destinationURL);
		browseButton = new FieldButton("Select Image Directory");
		browseButton.setEnabled(true);
		browseButton.addActionListener(this);
		helpButton = new FieldButton("Help");
		helpButton.setEnabled(true);
		helpButton.addActionListener(this);
		startButton = new FieldButton("Start");
		startButton.setEnabled(false);
		startButton.addActionListener(this);

		//Make the header panel
		JPanel header = new JPanel();
		header.setBackground(bgColor);
		header.add(new TitleLabel(config.getProperty("panelTitle")));
		header.setBorder(BorderFactory.createEmptyBorder(10,5,10,5));
		panel.add(header, BorderLayout.NORTH);

		//Make a panel for the input fields and the progress display
		JPanel main = new JPanel();
		main.setLayout(new BorderLayout());
		main.setBackground(bgColor);

		//Put the input fields in a vertical Box
		Box vBox = Box.createVerticalBox();
		vBox.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
		vBox.setBackground(bgColor);

		if (!config.getProperty("showURL", "yes").equals("no")) {
			Box destBox = Box.createHorizontalBox();
			destBox.setBackground(bgColor);
			destBox.add(new FieldLabel("Destination URL:"));
			destBox.add(Box.createHorizontalStrut(5));
			destBox.add(destinationField);
			vBox.add(destBox);
			vBox.add(Box.createVerticalStrut(10));
		}

		Box buttonBox = Box.createHorizontalBox();
		buttonBox.setBackground(bgColor);

		if (dialog != null) {
			dialogButton = new FieldButton(dialog.getTitle());
			dialogButton.setEnabled(true);
			dialogButton.addActionListener(this);
			buttonBox.add(dialogButton);
			buttonBox.add(Box.createHorizontalStrut(20));
		}
		else dialogButton = new FieldButton("unused");

		buttonBox.add(browseButton);
		helpURL = config.getProperty("helpURL", "").trim();
		if (!helpURL.equals("")) {
			buttonBox.add(Box.createHorizontalStrut(20));
			buttonBox.add(helpButton);
		}
		vBox.add(buttonBox);

		//Put the vBox in the north panel of the main panel
		main.add(vBox, BorderLayout.NORTH);

		//Put a DirectoryPanel in a scroll pane and put that in the center of the main panel
		dp = new DirectoryPanel();
		sp = new JScrollPane();
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
		Object source = event.getSource();
		if (source.equals(browseButton)) {
			dir = getDirectory();
			if (dir != null) {
				dp.clear();
				listFiles(dir);
				startButton.setEnabled(true);
				invalidate();
				validate();
				sp.getVerticalScrollBar().setValue(0);
			}
			else startButton.setEnabled(false);
		}
		else if (source.equals(dialogButton)) {
			displayDialog();
		}
		else if (source.equals(startButton)) {
			if (displayDialog()) {
				startButton.setEnabled(false);
				dialogButton.setEnabled(false);
				browseButton.setEnabled(false);
				sending = true;
				SenderThread sender = new SenderThread(this);
				sender.start();
			}
		}
		else if (source.equals(helpButton)) {
			String helpURL = config.getProperty("helpURL");
			if (!helpURL.equals("")) {
				BrowserUtil.openURL(helpURL);
			}
		}
	}

	private boolean displayDialog() {
		if (dialog != null) {
			int result = JOptionPane.showOptionDialog(
							this,
							dialog,
							dialog.getTitle(),
							JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.QUESTION_MESSAGE,
							null, //icon
							null, //options
							null); //initialValue
			if (result == JOptionPane.OK_OPTION) {
				dialog.setProperties(config);
				for (String configProp : config.stringPropertyNames()) {
					if (configProp.startsWith("$")) {
						String value = config.getProperty(configProp);
						String key = configProp.substring(1);
						daLUTProps.setProperty(key, value);
					}
					else if (configProp.startsWith("@")) {
						String value = config.getProperty(configProp);
						String key = "param." + configProp.substring(1);
						daScriptProps.setProperty(key, value);
					}
				}
				return true;
			}
			else return false;
		}
		return true;
	}

	public void transmissionComplete() {
		sending = false;
		Runnable enable = new Runnable() {
			public void run() {
				browseButton.setEnabled(true);
				dialogButton.setEnabled(true);
			}
		};
		SwingUtilities.invokeLater(enable);
	}

	public String getDestinationURL() {
		return destinationField.getText();
	}

	public DirectoryPanel getDirectoryPanel() {
		return dp;
	}

	public Properties getDAScriptProps() {
		return daScriptProps;
	}

	public Properties getDALUTProps() {
		return daLUTProps;
	}

	public Log getLog() {
		return log;
	}

	public IDTable getIDTable() {
		return idTable;
	}

	public String getDFScript() {
		return dfScript;
	}

	public PixelScript getDPAPixelScript() {
		return dpaPixelScript;
	}

	public boolean getDFEnabled() {
		return dfEnabled;
	}

	public boolean getDPAEnabled() {
		return dpaEnabled;
	}

	public boolean getAcceptNonImageObjects() {
		//Require an explicit acceptance of non-image objects
		String anio = config.getProperty("acceptNonImageObjects", "");
		return anio.trim().equals("yes");
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
		catch (Exception noProps) {
			log.append("Unable to load the config properties\n");
		}
		return props;
	}

	private DialogPanel getDialogPanel() {
		if (config.getProperty("dialogEnabled", "").equals("yes")) {
			try {
				File dialogFile = File.createTempFile("DIALOG-", ".xml");
				dialogFile.delete();
				String dialogName = config.getProperty("dialogName", "DIALOG.xml");
				if (!getTextFileFromServer(dialogFile, dialogName)) {
					File localFile = new File(dialogName);
					if (localFile.exists()) FileUtil.copy(localFile, dialogFile);
					else FileUtil.getFile( dialogFile, "/DIALOG.xml" );
				}
				//Now parse the file
				Document doc = XmlUtil.getDocument(dialogFile);
				return new DialogPanel(doc, config);
			}
			catch (Exception unable) { }
		}
		return null;
	}

	private Properties getDAScriptPropsObject() {
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
		catch (Exception unable) {
			log.append("Unable to obtain the DicomAnonymizer script\n");
		}
		return daScriptProps;
	}

	private Properties getDALUTPropsObject() {
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
		catch (Exception unable) {
			log.append("Unable to obtain the DicomAnonymizer LUT\n");
		}
		return daLUTProps;
	}

	private String getDFScriptObject() {
		String filterScript = null;
		try {
			File dfFile = File.createTempFile("DF-", ".script");
			dfFile.delete();
			String dfName = config.getProperty("dfName");
			if (!getTextFileFromServer(dfFile, dfName)) {
				dfFile = FileUtil.getFile( dfFile, "/DF.script" );
			}
			if (dfFile != null) filterScript = FileUtil.getText(dfFile);
		}
		catch (Exception unable) {
			log.append("Unable to obtain the DicomFilter script\n");
		}
		return filterScript;
	}

	private PixelScript getDPAPixelScriptObject() {
		PixelScript pixelScript = null;
		try {
			File dpaFile = File.createTempFile("DPA-", ".script");
			dpaFile.delete();
			String daLUTName = config.getProperty("dpaName");
			if (!getTextFileFromServer(dpaFile, daLUTName)) {
				dpaFile = FileUtil.getFile( dpaFile, "/DPA.script" );
			}
			if (dpaFile != null) pixelScript = new PixelScript(dpaFile);
		}
		catch (Exception unable) {
			log.append("Unable to obtain the DicomPixelAnonymizer script\n");
		}
		return pixelScript;
	}

	private void getKeystore() {
		try {
			File keystore = File.createTempFile("CC-", ".keystore");
			keystore.delete();
			FileUtil.getFile( keystore, "/keystore" );
			System.setProperty("javax.net.ssl.keyStore", keystore.getAbsolutePath());
			System.setProperty("javax.net.ssl.keyStorePassword", "ctpstore");
		}
		catch (Exception ex) {
			log.append("Unable to install the keystore\n");
		}
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
		dp.add(new DirectoryCheckBox(dp));
		dp.add(new DirectoryName(dir), RowLayout.span(4));
		dp.add(RowLayout.crlf());
		File[] files = dir.listFiles(new FilesOnlyFilter());
		for (File file : files) {
			String name = file.getName().toLowerCase();
			boolean dcm = name.endsWith(".dcm");
			dcm |= name.startsWith("img");
			dcm |= name.startsWith("image");
			dcm |= name.matches("[0-9\\.]+");
			dcm &= !name.endsWith(".jpg");
			dcm &= !name.endsWith(".jpeg");
			dcm &= !name.endsWith(".png");

			FileName fileName = new FileName(file);
			FileSize fileSize = new FileSize(file);
			StatusText statusText = new StatusText();
			FileCheckBox cb = new FileCheckBox(fileName, statusText);
			cb.setSelected(dcm);

			dp.add(cb);
			dp.add(fileName);
			dp.add(Box.createHorizontalStrut(20));
			dp.add(fileSize);
			dp.add(statusText);
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

			//Make sure we aren't busy
			if (sending) {
				int response = JOptionPane.showConfirmDialog(
								parent,
								"Files are still being transmitted.\nAre you sure you want to stop the program?",
								"Are you sure?",
								JOptionPane.YES_NO_OPTION);
				if (response != JOptionPane.YES_OPTION) return;
			}

			//Offer to save the idTable if it isn't empty
			idTable.save(parent);

			//Offer to save the log if it isn't empty
			log.save(parent);

			//Done
			System.exit(0);
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
			setMaximumSize(size);
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