package net.tourbook.device.suunto;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

public class Suunto9DeviceDataReader extends TourbookDevice {

	private String _importFilePath;


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
		_importFilePath = importFilePath;

		createTourData(alreadyImportedTours, newlyImportedTours);
		return true;
	}


	@Override
	public boolean validateRawData(final String fileName) {
		return isValidJSONFile(fileName);
	}


	/**
	 * Check if the file is a valid device JSON file.
	 * 
	 * @param importFilePath
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

		} catch (final Exception e) {
			StatusUtil.log(e);
		} finally {
			Util.closeReader(fileReader);
		}

		return true;
	}


	private void createTourData(final HashMap<Long, TourData> alreadyImportedTours,
			final HashMap<Long, TourData> newlyImportedTours) {

		String jsonFileContent = GetJsonContentFromGZipFile(_importFilePath);

		// create data object for each tour
		final TourData tourData = ImportTour(jsonFileContent);

		tourData.setImportFilePath(_importFilePath);

		// tourData.setCalories(_calories);
		// tourData.setRestPulse(_sectionParams.restHR == Integer.MIN_VALUE ? 0 :
		// _sectionParams.restHR);

		tourData.setDeviceId(deviceId);
		// after all data are added, the tour id can be created
		final Long tourId = tourData.createTourId("49837533398");

		// check if the tour is already imported
		if (alreadyImportedTours.containsKey(tourId) == false) {

			// add new tour to other tours
			newlyImportedTours.put(tourId, tourData);

			// create additional data
			tourData.computeTourDrivingTime();
			tourData.computeComputedValues();
			tourData.computeAltitudeUpDown();
		}
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


	private TourData ImportTour(String jsonFileContent) {
		final TourData tourData = new TourData();
		JSONArray samples = null;
		try {
			JSONObject jsonContent = new JSONObject(jsonFileContent);
			samples = (JSONArray) jsonContent.get("Samples");
		} catch (JSONException ex) {
			return null;
		}

		JSONObject firstSample = (JSONObject) samples.get(0);

		/*
		 * set tour start date/time
		 */
		ZonedDateTime previousLapStartTime = ZonedDateTime.parse(firstSample.get("TimeISO8601").toString());
		tourData.setTourStartTime(previousLapStartTime);

		ArrayList<TimeData> _sampleList = new ArrayList<>();
		for (int i = 0; i < samples.length(); i++) {
			JSONObject sample = samples.getJSONObject(i);
			JSONObject currentSampleAttributes = new JSONObject(sample.get("Attributes").toString());
			JSONObject currentSampleSml = new JSONObject(currentSampleAttributes.get("suunto/sml").toString());
			String currentSampleData = currentSampleSml.get("Sample").toString();

			// GPS point
			if (currentSampleData.contains("GPSAltitude") && currentSampleData.contains("Latitude")
					&& currentSampleData.contains("Longitude")) {
				TryAddGpsData(new JSONObject(currentSampleData), _sampleList);
			}

		}
		tourData.createTimeSeries(_sampleList, true);

		return tourData;
	}


	private void TryAddGpsData(JSONObject currentSample, ArrayList<TimeData> sampleList) {
		ZonedDateTime time = ZonedDateTime.parse(currentSample.get("UTC").toString());
		float latitude = Float.parseFloat(currentSample.get("Latitude").toString());
		float longitude = Float.parseFloat(currentSample.get("Longitude").toString());
		float altitude = Float.parseFloat(currentSample.get("GPSAltitude").toString());
		TimeData dejfn = new TimeData();
		dejfn.latitude = (latitude * 180) / Math.PI;
		dejfn.longitude = (longitude * 180) / Math.PI;
		// dejfn.time = time;
		dejfn.altitude = altitude;

		sampleList.add(dejfn);
		/*
		 * if (TryRetrieveFloatElementValue( currentSample, SuuntoDataNames.GpsAltitude,
		 * out float altitude) && TryRetrieveFloatElementValue( currentSample,
		 * SuuntoDataNames.Latitude, out float latitude) &&
		 * TryRetrieveFloatElementValue( currentSample, SuuntoDataNames.Longitude, out
		 * float longitude) && TryRetrieveDateTimeElementValue( currentSample,
		 * SuuntoDataNames.Utc, out DateTime time)) { if (activity.GPSRoute == null) {
		 * activity.GPSRoute = new GPSRoute(); }
		 * 
		 * GPSPoint gpsPoint = new GPSPoint( (float)(latitude * 180.0 / Math.PI),
		 * (float)(longitude * 180.0 / Math.PI), altitude); activity.GPSRoute.Add(time,
		 * gpsPoint); } }
		 */
	}
}
