package net.tourbook.device.suunto;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

public class Suunto9DeviceDataReader extends TourbookDevice {

	// plugin constructor
	public Suunto9DeviceDataReader() {
	}


	@Override
	public String buildFileNameFromRawData(final String rawDataFileName) {
		// NEXT Auto-generated method stub
		return null;
	}


	@Override
	public boolean checkStartSequence(final int byteIndex, final int newByte) {
		return true;
	}


	public String getDeviceModeName(final int profileId) {
		return UI.EMPTY_STRING;
	}


	@Override
	public SerialParameters getPortParameters(final String portName) {
		return null;
	}


	@Override
	public int getStartSequenceSize() {
		return 0;
	}


	public int getTransferDataSize() {
		return -1;
	}


	@Override
	public boolean processDeviceData(final String importFilePath, final DeviceData deviceData,
			final HashMap<Long, TourData> alreadyImportedTours, final HashMap<Long, TourData> newlyImportedTours) {

		if (isValidJSONFile(importFilePath) == false) {
			return false;
		}

		Suunto3SAXHandler saxHandler = null;

		try {

			saxHandler = new Suunto3SAXHandler(//
					this, importFilePath, alreadyImportedTours, newlyImportedTours);

			final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

			parser.parse("file:" + importFilePath, saxHandler);//$NON-NLS-1$

		} catch (final Exception e) {

			StatusUtil.log("Error parsing file: " + importFilePath, e); //$NON-NLS-1$
			return false;

		} finally {
			saxHandler.dispose();
		}

		return saxHandler.isImported();
	}


	@Override
	public boolean validateRawData(final String fileName) {
		return isValidJSONFile(fileName);
	}


	/**
	 * Check if the file is a valid device xml file.
	 * 
	 * @param importFilePath
	 * @param deviceTag
	 * @param isRemoveBOM
	 *            When <code>true</code> the BOM (Byte Order Mark) is removed from
	 *            the file.
	 * @return Returns <code>true</code> when the file contains content with the
	 *         requested tag.
	 */
	protected boolean isValidJSONFile(final String importFilePath) {

		BufferedReader fileReader = null;
		try {

			String jsonFileContent = GetJsonContentFromGZipFile(importFilePath);

			if (jsonFileContent == null) {
				return false;
			}

			try {
				JSONObject jsonContent = new JSONObject(jsonFileContent);
				JSONArray samples = (JSONArray) jsonContent.get("Samples");

				String firstSample = samples.get(0).toString();
				if (firstSample.contains("Lap") && firstSample.contains("Type") && firstSample.contains("Start"))
					return true;

			} catch (JSONException ex) {
				return false;
			}

		} catch (final Exception e1) {
			StatusUtil.log(e1);
		} finally {
			Util.closeReader(fileReader);
		}

		return true;
	}


	private String GetJsonContentFromGZipFile(String gzipFilePath) {
		String jsonFileContent = null;
		try {
			GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(gzipFilePath));
			BufferedReader br = new BufferedReader(new InputStreamReader(gzip));

			jsonFileContent = br.readLine();

			// close resources
			br.close();
			gzip.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return jsonFileContent;
	}
}
