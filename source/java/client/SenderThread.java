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
import org.rsna.ctp.stdstages.anonymizer.dicom.DICOMPixelAnonymizer;
import org.rsna.ctp.stdstages.anonymizer.dicom.PixelScript;
import org.rsna.ctp.stdstages.anonymizer.dicom.Signature;
import org.rsna.ctp.stdstages.anonymizer.dicom.Regions;
import org.rsna.ctp.stdstages.anonymizer.AnonymizerStatus;

public class SenderThread extends Thread {

	String urlString;
	DirectoryPanel dp;
	CTPClient parent;
	Properties daScriptProps;
	Properties daLUTProps;
	IDTable idTable;
	String dfScript;
	PixelScript dpaPixelScript;
	Log log;
	boolean acceptNonImageObjects;
	boolean dfEnabled;
	boolean dpaEnabled;

    public SenderThread (CTPClient parent) {
		super("SenderThread");
		this.urlString = parent.getDestinationURL();
		this.dp = parent.getDirectoryPanel();
		this.daScriptProps = parent.getDAScriptProps();
		this.daLUTProps = parent.getDALUTProps();
		this.idTable = parent.getIDTable();
		this.log = parent.getLog();
		this.acceptNonImageObjects = parent.getAcceptNonImageObjects();
		this.dfScript = parent.getDFScript();
		this.dpaPixelScript = parent.getDPAPixelScript();
		this.dfEnabled = parent.getDFEnabled();
		this.dpaEnabled = parent.getDPAEnabled();
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
					DicomObject dob = (DicomObject) fob;

					//See if we are processing this type of DicomObject
					if (acceptNonImageObjects || dob.isImage()) {

						//Apply the filter if one is available
						if (!dfEnabled || (dfScript == null) || dob.matches(dfScript).getResult()) {

							//Get the PHI PatientID for the IDTable
							String phiPatientID = dob.getPatientID();

							//Anonymize the pixels and the rest of the dataset.
							//This returns a new DicomObject in the temp directory.
							//The original object is left unmodified.
							dob = anonymize(dob, fileStatus);

							//If all went well, update the idTable and export
							if (dob != null) {
								String anonPatientID = dob.getPatientID();
								idTable.put(phiPatientID, anonPatientID);
								export(dob.getFile());
								successes++;
								fileStatus.setText(Color.black, "[OK]");
								dob.getFile().delete();
							}
						}
						else fileStatus.setText(Color.blue, "[REJECTED by DicomFilter]");
					}
					else fileStatus.setText(Color.blue, "[NON-IMAGE DICOM OBJECT]");
				}
				else fileStatus.setText(Color.blue, "[NON-DICOM OBJECT]");
			}

			catch (Exception ex) {
				fileStatus.setText(Color.red, "[FAILED]");
				StringWriter sw = new StringWriter();
				ex.printStackTrace(new PrintWriter(sw));
				log.append(sw.toString());
			}
		}
		statusPane.setText( "Processsing complete: "
								+fileNumber+" files processed; "
									+successes+" files successfully transmitted" );
		parent.transmissionComplete();
	}

	private DicomObject anonymize(DicomObject dob, StatusText fileStatus) {
		try {
			//Copy the file to the temp directory to protect the original
			File temp = File.createTempFile("Anon-",".dcm");
			temp.delete();
			dob.copyTo(temp);

			//Parse it again, so everything points to the right place
			dob = new DicomObject(temp);

			//Anonymize the pixels
			if (dpaEnabled && (dpaPixelScript != null)) {
				Signature signature = dpaPixelScript.getMatchingSignature(dob);
				if (signature != null) {
					Regions regions = signature.regions;
					if ((regions != null) && (regions.size() > 0)) {
						AnonymizerStatus status = DICOMPixelAnonymizer.anonymize(temp, temp, regions);
						if (status.isOK()) {
							try { dob = new DicomObject(temp); }
							catch (Exception unable) {
								fileStatus.setText(Color.red, "[REJECTED by DicomPixelAnonymizer]");
								return null;
							}
						}
						else {
							fileStatus.setText(Color.red, "[REJECTED by DicomPixelAnonymizer]");
							return null;
						}
					}
				}
			}

			//Anonymize the rest of the dataset
			AnonymizerStatus result =
				DICOMAnonymizer.anonymize(temp, //input file
										  temp, //output file
										  daScriptProps,
										  daLUTProps,
										  null, //no IntegerTable in this application
										  false, //keep transfer syntax
										  false); //do not rename to SOPInstanceUID
			if (result.isOK()) {
				try { dob = new DicomObject(temp); }
				catch (Exception unable) {
					fileStatus.setText(Color.red, "[REJECTED by DicomAnonymizer]");
					return null;
				}
			}
			else {
				fileStatus.setText(Color.red, "[REJECTED by DicomAnonymizer]");
				return null;
			}
			return dob;
		}
		catch (Exception failed) {
			fileStatus.setText(Color.red, "[Unknown anonymization failure]");
			return null;
		}
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
