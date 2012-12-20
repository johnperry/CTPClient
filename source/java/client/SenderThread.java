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
import java.util.*;
import org.rsna.server.*;
import org.rsna.util.*;
import org.rsna.ctp.objects.*;
import org.rsna.ctp.stdstages.anonymizer.dicom.DICOMAnonymizer;
import org.rsna.ctp.stdstages.anonymizer.AnonymizerStatus;

public class SenderThread extends Thread {

	String urlString;
	DirectoryPanel dp;
	CTPClient parent;
	Properties daScriptProps;
	Properties daLUTProps;
	IDTable idTable;
	Log log;

    public SenderThread (CTPClient parent) {
		super("SenderThread");
		this.urlString = parent.getDestinationURL();
		this.dp = parent.getDirectoryPanel();
		this.daScriptProps = parent.getScriptProps();
		this.daLUTProps = parent.getLUTProps();
		this.idTable = parent.getIDTable();
		this.log = parent.getLog();
		this.parent = parent;
	}

	public void run() {
		StatusPane statusPane = StatusPane.getInstance();
		Component[] components = dp.getComponents();
		LinkedList<FileCheckBox> cbs = new LinkedList<FileCheckBox>();
		for (int i=0; i<components.length; i++) {
			if (components[i] instanceof FileCheckBox) {
				FileCheckBox cb = (FileCheckBox)components[i];
				if (cb.isSelected()) cbs.add(cb);
			}
		}

		int fileNumber = 0;
		int nFiles = cbs.size();
		int successes = 0;
		for (FileCheckBox cb : cbs) {
			File file = cb.getFileNameObject().getFile();
			StatusText fileStatus = cb.getStatusTextObject();

			statusPane.setText( "Sending "+ (++fileNumber) + "/" + nFiles + " (" + file.getName() + ")");

			try {
				//Set what kind of object it is
				FileObject fob = FileObject.getInstance(file);
				if (fob instanceof DicomObject) {

					//Get the PHI PatientID for the IDTable
					String phiPatientID = ((DicomObject)fob).getPatientID();

					//Anonymize the object
					File temp = new File("temp");
					AnonymizerStatus result =
						DICOMAnonymizer.anonymize(file, //input file
												  temp, //output file
												  daScriptProps,
												  daLUTProps,
												  null, //no IntegerTable in this application
												  false, //keep transfer syntax
												  false); //do not rename to SOPInstanceUID
					if (result.isOK() || result.isSKIP()) {
						try {
							DicomObject dob = new DicomObject(temp);
							String anonPatientID = dob.getPatientID();
							idTable.put(phiPatientID, anonPatientID);
						}
						catch (Exception ignore) { }
						export(temp);
						successes++;
						fileStatus.setText(Color.black, "[OK]");
						temp.delete();
					}
					else {
						fileStatus.setText(Color.black, "["+result.toString()+"]");
					}
				}
				else {
					fileStatus.setText(Color.blue, "[NON-DICOM OBJECT]");
				}
			}

			catch (Exception ex) {
				fileStatus.setText(Color.red, "[FAILED]");
				StringWriter sw = new StringWriter();
				ex.printStackTrace(new PrintWriter(sw));
				log.append(sw.toString());
			}
		}
		statusPane.setText( "Processsing complete: "+fileNumber+" files processed; "+successes+" files successfully transmitted");
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
