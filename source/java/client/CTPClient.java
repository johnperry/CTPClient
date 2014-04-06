/*---------------------------------------------------------------
*  Copyright 2014 by the Radiological Society of North America
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
import org.rsna.ctp.stdstages.dicom.SimpleDicomStorageSCP;
import org.rsna.server.HttpResponse;
import org.rsna.util.BrowserUtil;
import org.rsna.util.FileUtil;
import org.rsna.util.HttpUtil;
import org.rsna.util.IPUtil;
import org.rsna.util.StringUtil;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Document;

public class CTPClient extends JFrame implements ActionListener, ComponentListener {

    static final String title = "CTP Client";
	static final Color bgColor = new Color(0xc6d8f9);

    JScrollPane sp;
    DirectoryPanel dp;
    StatusPane status;
    DialogPanel dialog = null;
    volatile boolean sending = false;

    InputText httpURLField = null;
    FieldButton browseButton = null;
    FieldButton scpButton = null;
    FieldButton dialogButton = null;
    FieldButton helpButton = null;
    FieldButton startButton = null;
    FieldButton showLogButton = null;
    FieldButton instructionsButton = null;
    int instructionsWidth = 425;

    AttachedFrame instructionsFrame = null;

    JFileChooser chooser = null;
    File dir = null;

    boolean dfEnabled;
    boolean dpaEnabled;
    boolean showURL = true;
    boolean showDialogButton = true;
    boolean showBrowseButton = true;

    boolean zip = false;
    boolean setBurnedInAnnotation = false;

    Properties config;

    DAScript daScript;
    File daScriptFile;
    String defaultKeyType = null;

    LookupTable lookupTable;
    File lookupTableFile;

    String dfScript;
    PixelScript dpaPixelScript;

    IDTable idTable = new IDTable();

    int scpPort;
    File scpDirectory = null;
    SimpleDicomStorageSCP scp = null;
    String ipAddressString = "";

    String browsePrompt = "Select a directory containing data to transmit.";
    String helpURL = null;

    File exportDirectory = null;
    boolean renameFiles = false;

    StudyList studyList = null;

    String dicomURL = null;

    public static void main(String[] args) {
		Logger.getRootLogger().addAppender(
				new ConsoleAppender(
					new PatternLayout("%d{HH:mm:ss} %-5p [%c{1}] %m%n")));
		Logger.getRootLogger().setLevel(Level.WARN);
        new CTPClient(args);
    }

    public CTPClient(String[] args) {
		super();

		try { UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() ); }
		catch (Exception ignore) { }

		//Get the configuration from the args.
		config = getConfiguration(args);

		//Set up the SCP directory
		scpPort = StringUtil.getInt( config.getProperty("scpPort"), 0 );
		if (scpPort > 0) {
			try {
				scpDirectory = File.createTempFile("TMP-", "");
				scpDirectory.delete();
				scpDirectory.mkdirs();
			}
			catch (Exception ignoreForNow) { }
		}

		//Get the DICOM export URL
		dicomURL = config.getProperty("dicomURL", "");

		//Set up the exportDirectory
		String expDir = config.getProperty("exportDirectory");
		if (expDir != null) exportDirectory = new File(expDir);
		renameFiles = config.getProperty("renameFiles", "no").equals("yes");

		//Get the button enables
		showURL = !config.getProperty("showURL", "yes").equals("no");
		showBrowseButton = !config.getProperty("showBrowseButton", "yes").equals("no") || (scpPort <= 0);
		showDialogButton = !config.getProperty("showDialogButton", "yes").equals("no");

		setTitle(config.getProperty("windowTitle"));
		JPanel panel = new JPanel(new BorderLayout());
		getContentPane().add(panel, BorderLayout.CENTER);

		//Set the SSL params
		getKeystore();

		//Get the DialogPanel if specified
		dialog = getDialogPanel();

		//Get the anonymizer script
		getDAScript();

		//Get the LookupTable
		 getLookupTable();

		//Get the PixelScript
		dpaPixelScript = getDPAPixelScriptObject();

		//Get the filter script
		dfScript = getDFScriptObject();

		//Get the zip parameter for HTTP export
		zip = config.getProperty("zip", "no").trim().equals("yes");

		//Set the enables
		dfEnabled = config.getProperty("dfEnabled", "no").trim().equals("yes");
		dpaEnabled = config.getProperty("dpaEnabled", "no").trim().equals("yes");
		setBurnedInAnnotation = config.getProperty("setBurnedInAnnotation", "no").trim().equals("yes");

		//Make the UI components
		String httpURL = config.getProperty("httpURL", "").trim();
		httpURLField = new InputText(httpURL);
		browseButton = new FieldButton("Open Local Folder");
		browseButton.setEnabled(true);
		browseButton.addActionListener(this);

		scpButton = new FieldButton("Open DICOM Storage");
		scpButton.setEnabled( (scpPort > 0) );
		scpButton.addActionListener(this);

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

		if (showURL) {
			Box destBox = Box.createHorizontalBox();
			destBox.setBackground(bgColor);
			destBox.add(new FieldLabel("Destination URL:"));
			destBox.add(Box.createHorizontalStrut(5));
			destBox.add(httpURLField);
			vBox.add(destBox);
			vBox.add(Box.createVerticalStrut(10));
		}

		Box buttonBox = Box.createHorizontalBox();
		buttonBox.setBackground(bgColor);

		if ((dialog != null) && showDialogButton) {
			dialogButton = new FieldButton(dialog.getTitle());
			dialogButton.setEnabled(true);
			dialogButton.addActionListener(this);
			buttonBox.add(dialogButton);
			buttonBox.add(Box.createHorizontalStrut(20));
		}
		else dialogButton = new FieldButton("unused");

		if (showBrowseButton) {
			buttonBox.add(browseButton);
			if (scpPort > 0) buttonBox.add(Box.createHorizontalStrut(20));
		}

		if (scpPort > 0) {
			buttonBox.add(scpButton);
		}

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

		//Make the instructionsFrame
		instructionsFrame = new AttachedFrame("Instructions", instructionsWidth, bgColor);

		//Make a footer bar to display status.
		status = StatusPane.getInstance(" ", bgColor);
		showLogButton = new FieldButton("Show Log");
		showLogButton.addActionListener(this);
		status.addRightComponent(showLogButton);
		instructionsButton = new FieldButton("Instructions");
		instructionsButton.addActionListener(this);
		status.addRightComponent(instructionsButton);
		panel.add(status, BorderLayout.SOUTH);

        //Catch close requests and check before closing if we are busy sending
        addWindowListener(new WindowCloser(this));

        pack();
        centerFrame();
        setVisible(true);

		//Start the SCP if so configured.
		if (scpPort > 0) {
			try {
				ipAddressString = IPUtil.getIPAddress() + ":" + scpPort + " [AET: CTP]";
				scp = new SimpleDicomStorageSCP(scpDirectory, scpPort);
				scp.start();
				status.setText("DICOM Storage SCP open on "+ipAddressString+".");
			}
			catch (Exception ex) {
				JOptionPane.showMessageDialog(
					this,
					"Unable to start the\nDICOM Storage SCP on\n"+ipAddressString);
				System.exit(0);
			}
		}

		//Now that everything is set up, set the text of the
		//instructions to make it correspond to the configuration
		//and display the frame
		instructionsFrame.setText(getInstructions());
		instructionsFrame.attachTo(this);
		instructionsFrame.setVisible(true);
		this.requestFocus();
		addComponentListener(this);
	}

	//.Implement the ActionListener interface
	public void actionPerformed(ActionEvent event) {
		boolean radioMode = (dialog != null) && dialog.studyMode();
		boolean anio = getAcceptNonImageObjects();
		Object source = event.getSource();
		if (source.equals(browseButton)) {
			dir = getDirectory();
			if (dir != null) {
				setWaitCursor(true);
				dp.clear();
				dp.setDeleteOnSuccess(false);
				studyList = new StudyList(dir, radioMode, anio);
				studyList.display(dp);
				studyList.selectFirstStudy();
				startButton.setEnabled(true);
				sp.getVerticalScrollBar().setValue(0);
				setWaitCursor(false);
			}
			else startButton.setEnabled(false);
		}
		else if (source.equals(scpButton)) {
			if (scpDirectory != null) {
				setWaitCursor(true);
				dp.clear();
				dp.setDeleteOnSuccess(true);
				studyList = new StudyList(scpDirectory, radioMode, anio);
				studyList.display(dp);
				studyList.selectFirstStudy();
				startButton.setEnabled(true);
				sp.getVerticalScrollBar().setValue(0);
				setWaitCursor(false);
			}
		}
		else if (source.equals(dialogButton)) {
			displayDialog();
		}
		else if (source.equals(startButton)) {
			if (displayDialog()) {
				startButton.setEnabled(false);
				dialogButton.setEnabled(false);
				browseButton.setEnabled(false);
				scpButton.setEnabled(false);
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
		else if (source.equals(showLogButton)) {
			showLog();
		}
		else if (source.equals(instructionsButton)) {
			instructionsFrame.attachTo(this);
			instructionsFrame.setVisible(true);
			this.requestFocus();
		}
	}

    private void setWaitCursor(boolean on) {
        if (on) setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        else setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

	private void showLog() {
		String text = Log.getInstance().getText().trim();
		if (text.equals("")) text = "The log is empty.";
		JOptionPane.showMessageDialog(this, text, "Log", JOptionPane.PLAIN_MESSAGE);
	}

	//Implement the ComponentListener interface
	public void componentHidden(ComponentEvent e) { }
	public void componentMoved(ComponentEvent e) { setInstructionsPosition(); }
	public void componentResized(ComponentEvent e) { setInstructionsPosition(); }
	public void componentShown(ComponentEvent e) { }
	private void setInstructionsPosition() {
		if (instructionsFrame.isVisible()) {
			instructionsFrame.attachTo(this);
		}
	}

	public String getInstructions() {
		StringBuffer sb = new StringBuffer();
		sb.append("<center><h1>Instructions</h1></center><hr/>\n");
		if ((scp != null)) {
			sb.append("<h2>To process and export images received from a PACS or workstation:</h2>\n");
			sb.append("<ol>");
			sb.append("<li>Send images to <b>"+ ipAddressString+"</b>\n");
			sb.append("<li>Click the <b>Open DICOM Storage</b> button.\n");
			sb.append("<li>Check the boxes of the images to be processed.\n");
			sb.append("<li>Click the <b>Start</b> button.\n");
			if (dialog != null) {
				sb.append("<li>Fill in the fields in the dialog.\n");
				sb.append("<li>Click <b>OK</b> on the dialog.\n");
			}
			sb.append("</ol>");
		}
		if (showBrowseButton) {
			if (sb.length() > 0) sb.append("\n");
			sb.append("<h2>To process and export images stored on this computer:</h2>\n");
			sb.append("<ol>");
			sb.append("<li>Click the <b>Open Local Folder</b> button\n");
			sb.append("<li>Navigate to a folder containing images.\n");
			sb.append("<li>Click <b>OK</b> on the file dialog.\n");
			sb.append("<li>Check the boxes of the images to be processed.\n");
			sb.append("<li>Click the <b>Start</b> button.\n");
			if (dialog != null) {
				sb.append("<li>Fill in the fields in the dialog.\n");
				sb.append("<li>Click <b>OK</b> on the dialog.\n");
			}
			sb.append("</ol>");
		}
		return sb.toString();
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
				//Set the field values in the configuration
				dialog.setProperties(config);
				return true;
			}
			else return false;
		}
		return true;
	}

	public void transmissionComplete() {
		sending = false;
		final JFrame parent = this;
		Runnable enable = new Runnable() {
			public void run() {
				scpButton.setEnabled(true);
				browseButton.setEnabled(true);
				dialogButton.setEnabled(true);
				startButton.setEnabled(true);
				int result = JOptionPane.showOptionDialog(
					parent,
					"The selected images have been processed\n\n"
					+"If you want to process more images, click YES.\n"
					+"If you want to exit the program, click NO.\n\n",
					"Processing Complete",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null, //icon
					null, //options
					null); //initialValue
				if (result == JOptionPane.NO_OPTION) {
					WindowEvent wev = new WindowEvent(parent, WindowEvent.WINDOW_CLOSING);
					Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
				}
			}
		};
		SwingUtilities.invokeLater(enable);
	}

	public String getHttpURL() {
		return httpURLField.getText().trim();
	}

	public String getDicomURL() {
		return dicomURL;
	}

	public File getExportDirectory() {
		return exportDirectory;
	}

	public boolean getRenameFiles() {
		return renameFiles;
	}

	public DirectoryPanel getDirectoryPanel() {
		return dp;
	}

	public StudyList getStudyList() {
		return studyList;
	}

	public Properties getDAScriptProps() {
		daScript = DAScript.getInstance(daScriptFile);
		Properties daScriptProps = daScript.toProperties();
		for (String configProp : config.stringPropertyNames()) {
			if (configProp.startsWith("@")) {
				String value = config.getProperty(configProp);
				String key = "param." + configProp.substring(1);
				daScriptProps.setProperty(key, value);
			}
		}
		return daScriptProps;
	}

	public Properties getDALUTProps() {
		Properties daLUTProps = LookupTable.getProperties(lookupTableFile, defaultKeyType);
		if (daLUTProps != null) {
			for (String configProp : config.stringPropertyNames()) {
				if (configProp.startsWith("$")) {
					String value = config.getProperty(configProp);
					String key = configProp.substring(1);
					daLUTProps.setProperty(key, value);
				}
			}
		}
		return daLUTProps;
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

	public boolean getSetBurnedInAnnotation() {
		return setBurnedInAnnotation;
	}

	public boolean getZip() {
		return zip;
	}

	public boolean getAcceptNonImageObjects() {
		//Require an explicit acceptance of non-image objects
		String anio = config.getProperty("acceptNonImageObjects", "");
		return anio.trim().equals("yes");
	}

	private Properties getConfiguration(String[] args) {
		Properties props = new Properties();

		//Put in the default titles
		props.setProperty("windowTitle", title);
		props.setProperty("panelTitle", title);

		try {
			//Get the config file from the jar
			File configFile = File.createTempFile("CONFIG-", ".properties");
			configFile.delete();
			FileUtil.getFile( configFile, "/config.properties" );
			loadProperties(props, configFile);

			//Overwrite any props from the local defaults, if present
			File defProps = new File("config.default");
			loadProperties(props, defProps);

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
			Log.getInstance().append("Unable to load the config properties\n");
		}
		return props;
	}

	private void loadProperties(Properties props, File file) {
		if (file.exists()) {
			FileInputStream stream = null;
			try {
				stream = new FileInputStream(file);
				props.load(stream);
			}
			catch (Exception ignore) { }
			FileUtil.close(stream);
		}
	}

	private DialogPanel getDialogPanel() {
		if (config.getProperty("dialogEnabled", "no").equals("yes")) {
			try {
				String dialogName = config.getProperty("dialogName", "DIALOG.xml");
				File dialogFile = getTextFile(dialogName, "/DIALOG.xml");
				//Now parse the file
				Document doc = XmlUtil.getDocument(dialogFile);
				return new DialogPanel(doc, config);
			}
			catch (Exception unable) {
				StringWriter sw = new StringWriter();
				unable.printStackTrace(new PrintWriter(sw));
				JOptionPane.showMessageDialog(this, "Exception in getDialogPanel:\n"+sw.toString());
			}
		}
		return null;
	}

	private void getDAScript() {
		daScript = null;
		String daScriptName = config.getProperty("daScriptName", "DA.script");
		daScriptFile =  getTextFile(daScriptName, "/DA.script");
		if (daScriptFile != null) {
			daScript = DAScript.getInstance(daScriptFile);
			defaultKeyType = daScript.getDefaultKeyType();
		}
	}

	private void getLookupTable() {
		lookupTable = null;
		String daLUTName = config.getProperty("daLUTName");
		lookupTableFile = getTextFile(daLUTName, "/LUT.properties");
		lookupTable = LookupTable.getInstance(lookupTableFile, defaultKeyType);
	}

	private String getDFScriptObject() {
		String filterScript = null;
		if (config.getProperty("dfEnabled", "no").equals("yes")) {
			String dfName = config.getProperty("dfName", "DF.script");
			File dfFile = getTextFile(dfName, "/DF.script");
			if (dfFile != null) filterScript = FileUtil.getText(dfFile);
			else Log.getInstance().append("Unable to obtain the DicomFilter script\n");
		}
		return filterScript;
	}

	private PixelScript getDPAPixelScriptObject() {
		PixelScript pixelScript = null;
		if (config.getProperty("dpaEnabled", "no").equals("yes")) {
			String daLUTName = config.getProperty("dpaName", "DPA.script");
			File dpaFile = getTextFile(daLUTName, "/DPA.script");
			if (dpaFile != null) pixelScript = new PixelScript(dpaFile);
			else Log.getInstance().append("Unable to obtain the DicomPixelAnonymizer script\n");
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
			Log.getInstance().append("Unable to install the keystore\n");
		}
	}

	private File getTextFile(String name, String resource) {
		if (name == null) return null;
		String protocol = config.getProperty("protocol");
		String host = config.getProperty("host");
		String application = config.getProperty("application");
		if ((protocol != null) && (host != null) && (application != null) && (name != null)) {
			String url = protocol + "://" + host + "/" + application + "/" + name;
			BufferedReader reader = null;
			try {
				//Connect to the server
				HttpURLConnection conn = HttpUtil.getConnection(url);
				conn.setRequestMethod("GET");
				conn.setDoOutput(false);
				conn.connect();

				//Get the response
				if (conn.getResponseCode() == HttpResponse.ok) {
					reader = new BufferedReader( new InputStreamReader( conn.getInputStream(), FileUtil.utf8 ) );
					StringWriter buffer = new StringWriter();
					char[] cbuf = new char[1024];
					int n;
					while ( (n = reader.read(cbuf,0,1024)) != -1 ) buffer.write(cbuf,0,n);
					reader.close();
					File file = File.createTempFile("CTPClient-",name);
					if ( FileUtil.setText(file, buffer.toString())) return file;
				}
			}
			catch (Exception unable) { FileUtil.close(reader); }
		}
		else if (name != null) {
			//The file is not available on the server, try to get it locally.
			File localFile = new File(name);
			if (localFile.exists()) return localFile;
		}
		if (resource != null) {
			//The file is not available locally; use the resource as a last resort.
			try {
				File file = File.createTempFile("CTPClient-",name);
				file.delete();
				return FileUtil.getFile(file, resource);
			}
			catch (Exception unable) { }
		}
		return null;
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

			//Stop the SCP if it's running.
			if (scp != null) scp.stop();

			//Offer to save the idTable if it isn't empty
			idTable.save(parent);

			//Offer to save the log if it isn't empty
			Log.getInstance().save(parent);

			System.exit(0);
		}
    }

    private void centerFrame() {
        Toolkit t = getToolkit();
        Dimension scr = t.getScreenSize ();
        int thisWidth = 2*(scr.width - instructionsWidth)/3;
        int thisHeight = scr.height/2;
        int totalWidth = thisWidth + instructionsWidth;
        setSize(thisWidth, thisHeight);
        setLocation (new Point ((scr.width -totalWidth)/2,
                                (scr.height - thisHeight)/2));
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