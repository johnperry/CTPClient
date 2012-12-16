/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package client;

import java.awt.*;
import java.io.*;
import java.net.*;
import org.rsna.server.*;
import org.rsna.util.*;
import org.rsna.ctp.objects.*;
import org.rsna.ctp.stdstages.anonymizer.dicom.DAScript;
import org.rsna.ctp.stdstages.anonymizer.dicom.DICOMAnonymizer;
import org.rsna.ctp.stdstages.anonymizer.AnonymizerStatus;
import org.rsna.ctp.stdstages.anonymizer.LookupTable;

public class SenderThread extends Thread {

	File dir;
	String urlString;
	ColorPane cp;
	CTPClient parent;
	File daScriptFile;
	File daLUTFile;
	DAScript daScript;

    public SenderThread (File dir, String urlString, ColorPane cp, File daScriptFile, File daLUTFile, CTPClient parent) {
		super("SenderThread");
		this.dir = dir;
		this.urlString = urlString;
		this.cp = cp;
		this.daScriptFile = daScriptFile;
		this.daLUTFile = daLUTFile;
		this.parent = parent;
		this.daScript = DAScript.getInstance(daScriptFile);
	}

	public void run() {
		StatusPane status = StatusPane.getInstance();
		cp.println("Sending directory: "+dir.getAbsolutePath() + "\n");
		File[] files = dir.listFiles(new FilesOnlyFilter());
		for (int i=0; i<files.length; i++) {
			File file = files[i];
			status.setText( "Sending "+ (i+1) + "/" + files.length + " (" + file.getName() + ")");

			try {
				//Set what kind of object it is
				FileObject fob = FileObject.getInstance(file);
				if (fob instanceof DicomObject) {

					//Anonymize the object
					File temp = new File("temp");
					AnonymizerStatus result =
						DICOMAnonymizer.anonymize(file, //input file
												  temp, //output file
												  daScript.toProperties(),
												  LookupTable.getProperties(daLUTFile),
												  null, //no IntegerTable in this application
												  false, //keep transfer syntax
												  false); //do not rename to SOPInstanceUID
					if (result.isOK() || result.isSKIP()) {
						export(temp);
						cp.println(Color.black, file.getName() + " [OK]");
						temp.delete();
					}
					else {
						cp.println(Color.black, file.getName() + " ["+result.toString()+"]");
					}
				}
				else {
					cp.println(Color.blue, file.getName() + " [NON-DICOM OBJECT]");
				}
			}

			catch (Exception ex) {
				cp.println(Color.red, file.getName() + " [FAILED]");
				StringWriter sw = new StringWriter();
				ex.printStackTrace(new PrintWriter(sw));
				cp.println(Color.red, sw.toString());
			}
		}
		StatusPane.getInstance().setText( "Done" );
		parent.transmissionComplete();
	}

	private boolean export(File fileToExport) throws Exception {
		HttpURLConnection conn;
		OutputStream svros;
		//Establish the connection
		conn = HttpUtil.getConnection(new URL(urlString));
		conn.connect();
		svros = conn.getOutputStream();

		//Send the file to the server
		FileUtil.streamFile(fileToExport, svros);

		//Check the response code
		int responseCode = conn.getResponseCode();
		if (responseCode != HttpResponse.ok) return false;

		//Check the response text.
		//Note: this rather odd way of acquiring a success
		//result is for backward compatibility with MIRC.
		String result = FileUtil.getText( conn.getInputStream() );
		return result.equals("OK");
	}
}
