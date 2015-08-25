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
import org.rsna.ctp.objects.*;
import org.rsna.ctp.pipeline.Status;
import org.rsna.ctp.stdstages.anonymizer.AnonymizerStatus;
import org.rsna.ctp.stdstages.anonymizer.dicom.DICOMAnonymizer;
import org.rsna.ctp.stdstages.anonymizer.dicom.DICOMDecompressor;
import org.rsna.ctp.stdstages.anonymizer.dicom.DICOMPixelAnonymizer;
import org.rsna.ctp.stdstages.anonymizer.dicom.PixelScript;
import org.rsna.ctp.stdstages.anonymizer.dicom.Regions;
import org.rsna.ctp.stdstages.anonymizer.dicom.Signature;
import org.rsna.ctp.stdstages.anonymizer.IntegerTable;
import org.rsna.ctp.stdstages.dicom.DicomStorageSCU;
import org.rsna.server.*;
import org.rsna.util.*;

public class SenderThread extends Thread {

	StudyList studyList;
	String httpURLString;
	String dicomURLString;
	File exportDirectory = null;
	boolean renameFiles = false;
	DirectoryPanel dp;
	CTPClient parent;
	Properties daScriptProps;
	Properties daLUTProps;
	IDTable idTable;
	String dfScript;
	PixelScript dpaPixelScript;
	boolean acceptNonImageObjects;
	boolean dfEnabled;
	boolean dpaEnabled;
	boolean setBurnedInAnnotation = false;
	boolean zip = false;
	DicomStorageSCU scu = null;
	IntegerTable integerTable = null;

	static final int retryCount = 5;
	static final int readTimeout = 5000;
	static final String JPEGBaseline = "1.2.840.10008.1.2.4.50";
	static final long maxUnchunked = 20 * 1024 * 1024;

    public SenderThread (CTPClient parent) {
		super("SenderThread");
		this.studyList = parent.getStudyList();
		this.httpURLString = parent.getHttpURL();
		this.dicomURLString = parent.getDicomURL();
		this.dp = parent.getDirectoryPanel();
		this.daScriptProps = parent.getDAScriptProps();
		this.daLUTProps = parent.getDALUTProps();
		this.idTable = parent.getIDTable();
		this.acceptNonImageObjects = parent.getAcceptNonImageObjects();
		this.dfScript = parent.getDFScript();
		this.dpaPixelScript = parent.getDPAPixelScript();
		this.dfEnabled = parent.getDFEnabled();
		this.dpaEnabled = parent.getDPAEnabled();
		this.setBurnedInAnnotation = parent.getSetBurnedInAnnotation();
		this.zip = parent.getZip();
		this.exportDirectory = parent.getExportDirectory();
		this.renameFiles = parent.getRenameFiles();
		this.parent = parent;
	}

	public void run() {
		StatusPane statusPane = StatusPane.getInstance();

		LinkedList<FileName> fileNames = new LinkedList<FileName>();
		Study[] studies = studyList.getStudies();
		for (Study study : studies) {
			if (study.isSelected()) {
				FileName[] names = study.getFileNames();
				for (FileName name : names) {
					if (name.isSelected()) fileNames.add(name);
				}
			}
		}

		if ((dicomURLString != null) && !dicomURLString.equals("") && (fileNames.size() > 0)) {
			scu = new DicomStorageSCU(dicomURLString, 0, false, 0, 0, 0, 0);
		}

		try { integerTable = new IntegerTable(new File(System.getProperty("user.dir"))); }
		catch (Exception noIntegerTable) { }

		int fileNumber = 0;
		int nFiles = fileNames.size();
		int successes = 0;
		for (FileName fn : fileNames) {
			File file = fn.getFile();
			StatusText fileStatus = fn.getStatusText();

			statusPane.setText( "Sending "+ (++fileNumber) + "/" + nFiles + " (" + file.getName() + ")");

			try {
				//See what kind of object it is
				FileObject fob = FileObject.getInstance(file);
				if (fob instanceof DicomObject) {
					DicomObject dob = (DicomObject) fob;

					//See if we are processing this type of DicomObject
					if (acceptNonImageObjects || dob.isImage()) {

						//Apply the filter if one is available
						if (!dfEnabled || (dfScript == null) || dob.matches(dfScript)) {

							//Get the PHI PatientID for the IDTable
							String phiPatientName = dob.getPatientName();
							String phiPatientID = dob.getPatientID();

							//Anonymize the pixels and the rest of the dataset.
							//This returns a new DicomObject in the temp directory.
							//The original object is left unmodified.
							dob = anonymize(dob, fileStatus);

							//If all went well, update the idTable and export
							if (dob != null) {
								String anonPatientID = dob.getPatientID();
								idTable.put(phiPatientName, phiPatientID, anonPatientID);
								String status = "";

								//Copy the file to the export directory, if so configured
								boolean fileExportOK = true;
								if (exportDirectory != null) {
									fileExportOK = directoryExport(dob);
									if (!fileExportOK) status = append(status, "File");
								}

								//Do the HTTP export, if so configured
								boolean httpExportOK = true;
								if ((httpURLString != null) && !httpURLString.equals("")) {
									httpExportOK =  httpExport(dob.getFile());
									if (!httpExportOK) status = append(status, "HTTP");
								}

								//Do the DICOM export, if so configured
								boolean dicomExportOK = true;
								if (scu != null) {
									dicomExportOK =  dicomExport(dob);
									if (!dicomExportOK) status = append(status, "DICOM");
								}

								//Count the complete successes
								boolean ok = fileExportOK && httpExportOK && dicomExportOK;
								if (ok) {
									successes++;
									fn.setSelected(false);
								}
								status = ok ? "OK" : "FAILED: "+status;
								fileStatus.setText(Color.black, "["+status+"]");
								dob.getFile().delete();

								//If we are configured to delete from the original directory, do it.
								if (ok && dp.getDeleteOnSuccess()) file.delete();
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
				Log.getInstance().append("exportDirectory: "+exportDirectory
										+"\nhttpURL:"+httpURLString
										+"\ndicomURL:"+dicomURLString
										+"\n"+sw.toString());
			}
		}
		if (scu != null) scu.close();
		if (integerTable != null) integerTable.close();
		String resultText = "Processsing complete: ";
		resultText += fileNumber+" file"+plural(fileNumber)+" processed";
		if (fileNumber > 0) resultText += "; "+successes+" file"+plural(successes)+" successfully exported";
		statusPane.setText(resultText);
		parent.transmissionComplete();
	}

	String plural(int n) {
		return (n != 1) ? "s" : "";
	}

	String append(String status, String text) {
		if (status.length() > 0) status += ";";
		status += text;
		return status;
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
						boolean isJPEGBaseline = dob.hasTransferSyntaxUID(JPEGBaseline);
						if (dob.isEncapsulated() && !isJPEGBaseline) DICOMDecompressor.decompress(temp, temp);
						AnonymizerStatus status = DICOMPixelAnonymizer.anonymize(temp, temp, regions, setBurnedInAnnotation, false);
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
			if (daScriptProps == null) {
				fileStatus.setText(Color.red, "[ABORTED (daScript)]");
				return null;
			}
			AnonymizerStatus result =
				DICOMAnonymizer.anonymize(temp, //input file
										  temp, //output file
										  daScriptProps,
										  daLUTProps,
										  integerTable,
										  false, //keep transfer syntax
										  false); //do not rename to SOPInstanceUID
			if (result.isOK()) {
				try { dob = new DicomObject(temp); }
				catch (Exception unable) {
					fileStatus.setText(Color.red, "[REJECTED by DicomAnonymizer (parse)]");
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

	private boolean httpExport(File fileToExport) {
		Log log = Log.getInstance();
		for (int k=0; k<retryCount; k++) {
			String msg = "";
			try {
				HttpURLConnection conn;
				OutputStream svros;
				//Establish the connection
				conn = HttpUtil.getConnection(new URL(httpURLString));
				conn.setReadTimeout(readTimeout);
				if (fileToExport.length() > maxUnchunked) conn.setChunkedStreamingMode(0);
				conn.connect();
				svros = conn.getOutputStream();

				//Send the file to the server
				if (!zip) FileUtil.streamFile(fileToExport, svros);
				else FileUtil.zipStreamFile(fileToExport, svros);

				//Check the response code
				int responseCode = conn.getResponseCode();
				if (responseCode != HttpResponse.ok) {
					conn.disconnect();
					return false;
				}

				//Check the response text.
				//Note: this rather odd way of acquiring a success
				//result is for backward compatibility with MIRC.
				String result = FileUtil.getTextOrException( conn.getInputStream(), FileUtil.utf8, false );
				conn.disconnect();
				if (result.equals("OK")) return true;
			}
			catch (Exception ex) { msg = ex.getMessage(); }
			log.append("HttpExport: "+httpURLString+" [failed on try "+(k+1)+"]");
			if (!msg.equals("")) log.append("..."+msg);
			snooze();
		}
		return false;
	}

	private boolean dicomExport(DicomObject dob) {
		for (int k=0; k<retryCount; k++) {
			Status status = scu.send(dob.getFile()); //Use File because the stream was not open.
			if (status.equals(Status.OK)) return true;
			Log.getInstance().append("DicomExport: [failed on try "+(k+1)+"]");
			snooze();
		}
		return false;
	}

	private boolean directoryExport(DicomObject dob) {
		File dir = exportDirectory;
		String name = dob.getSOPInstanceUID();

		if (renameFiles) {
			String patientID = dob.getPatientID();
			String study = DigestUtil.hash(dob.getStudyInstanceUID(), 6);
			String series = dob.getSeriesNumber();
			String acquisition = dob.getAcquisitionNumber();
			String instance = dob.getInstanceNumber();
			name = study+"_"+series+"_"+acquisition+"_"+instance;
			dir = new File(dir, patientID);
			dir = new File(dir, study);
		}

		dir.mkdirs();
		File tempFile = new File(dir, name+".partial");
		File savedFile = new File(dir, name+".dcm");
		return dob.copyTo(tempFile) && tempFile.renameTo(savedFile);
	}

	private void snooze() {
		try { Thread.sleep(1000); }
		catch (Exception ex) { }
	}
}
