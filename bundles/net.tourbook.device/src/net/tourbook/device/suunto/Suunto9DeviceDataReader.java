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

	private final float Kelvin = 273.1499938964845f;

	private ArrayList<TimeData> _sampleList = new ArrayList<TimeData>();


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

		tourData.setDeviceId(deviceId);
		// TODO after all data are added, the tour id can be created
		final Long tourId = tourData.createTourId("49837533398");

		// check if the tour is already imported
		if (alreadyImportedTours.containsKey(tourId) == false) {

			// add new tour to other tours
			newlyImportedTours.put(tourId, tourData);

			// create additional data
			tourData.computeAltitudeUpDown();
			tourData.computeTourDrivingTime();
			tourData.computeComputedValues();
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

		for (int i = 0; i < samples.length(); i++) {
			JSONObject sample = samples.getJSONObject(i);
			JSONObject currentSampleAttributes = new JSONObject(sample.get("Attributes").toString());
			JSONObject currentSampleSml = new JSONObject(currentSampleAttributes.get("suunto/sml").toString());
			String currentSampleData = currentSampleSml.get("Sample").toString();
			long currentSampleDate = ZonedDateTime.parse(sample.get("TimeISO8601").toString()).toInstant()
					.toEpochMilli();

			boolean wasDataPopulated = false;
			TimeData timeData = new TimeData();

			timeData.absoluteTime = currentSampleDate;

			// GPS point
			if (currentSampleData.contains("GPSAltitude") && currentSampleData.contains("Latitude")
					&& currentSampleData.contains("Longitude")) {
				wasDataPopulated |= TryAddGpsData(new JSONObject(currentSampleData), timeData);
			}

			// Heart Rate
			wasDataPopulated |= TryAddHeartRateData(new JSONObject(currentSampleData), timeData);

			// Speed
			wasDataPopulated |= TryAddSpeedData(new JSONObject(currentSampleData), timeData);

			// Cadence
			wasDataPopulated |= TryAddCadenceData(new JSONObject(currentSampleData), timeData);

			// Barometric Altitude
			// TryAddAltitudeData(new JSONObject(currentSampleData), currentSampleDate,
			// timeData);

			// Power
			wasDataPopulated |= TryAddPowerData(new JSONObject(currentSampleData), timeData);

			// Distance
			wasDataPopulated |= TryAddDistanceData(new JSONObject(currentSampleData), timeData);

			// Temperature
			wasDataPopulated |= TryAddTemperatureData(new JSONObject(currentSampleData), timeData);

			if (wasDataPopulated)
				_sampleList.add(timeData);
		}
		tourData.createTimeSeries(_sampleList, true);

		return tourData;
	}


	/**
	 * Attempts to retrieve and add GPS data to the current tour.
	 * 
	 * @param currentSample
	 *            The current sample data in JSON format.
	 * @param sampleList
	 *            The tour's time serie.
	 */
	private boolean TryAddGpsData(JSONObject currentSample, TimeData timeData) {
		try {
			String dateString = currentSample.get("UTC").toString();
			long unixTime = ZonedDateTime.parse(dateString).toInstant().toEpochMilli();
			float latitude = Float.parseFloat(currentSample.get("Latitude").toString());
			float longitude = Float.parseFloat(currentSample.get("Longitude").toString());
			float altitude = Float.parseFloat(currentSample.get("GPSAltitude").toString());

			timeData.latitude = (latitude * 180) / Math.PI;
			timeData.longitude = (longitude * 180) / Math.PI;
			timeData.absoluteTime = unixTime;
			timeData.absoluteAltitude = altitude;

			return true;
		} catch (Exception e) {
		}
		return false;
	}


	/**
	 * Attempts to retrieve and add HR data to the current tour.
	 * 
	 * @param currentSample
	 *            The current sample data in JSON format.
	 * @param sampleList
	 *            The tour's time serie.
	 */
	private boolean TryAddHeartRateData(JSONObject currentSample, TimeData timeData) {
		String value = null;
		if ((value = TryRetrieveStringElementValue(currentSample, "HR")) != null) {
			timeData.pulse = Float.parseFloat(value) * 60;
			return true;
		}

		return false;
	}


	/**
	 * Attempts to retrieve and add speed data to the current tour.
	 * 
	 * @param currentSample
	 *            The current sample data in JSON format.
	 * @param sampleList
	 *            The tour's time serie.
	 */
	private boolean TryAddSpeedData(JSONObject currentSample, TimeData timeData) {
		String value = null;
		if ((value = TryRetrieveStringElementValue(currentSample, "Speed")) != null) {
			timeData.speed = Float.parseFloat(value) * 3600;
			return true;
		}
		return false;
	}


	/**
	 * Attempts to retrieve and add cadence data to the current tour.
	 * 
	 * @param currentSample
	 *            The current sample data in JSON format.
	 * @param sampleList
	 *            The tour's time serie.
	 */
	private boolean TryAddCadenceData(JSONObject currentSample, TimeData timeData) {
		String value = null;
		if ((value = TryRetrieveStringElementValue(currentSample, "Cadence")) != null) {
			timeData.cadence = Float.parseFloat(value) * 60;
			return true;
		}
		return false;
	}


	/**
	 * Attempts to retrieve and add barometric altitude data to the current tour.
	 * 
	 * @param currentSample
	 *            The current sample data in JSON format.
	 * @param sampleList
	 *            The tour's time serie.
	 */
	@SuppressWarnings("unused")
	private boolean TryAddAltitudeData(JSONObject currentSample, TimeData timeData) {
		String value = null;
		if ((value = TryRetrieveStringElementValue(currentSample, "Altitude")) != null) {
			timeData.absoluteAltitude = Float.parseFloat(value);
			return true;
		}
		return false;
	}


	/**
	 * Attempts to retrieve and add power data to the current tour.
	 * 
	 * @param currentSample
	 *            The current sample data in JSON format.
	 * @param sampleList
	 *            The tour's time serie.
	 */
	private boolean TryAddPowerData(JSONObject currentSample, TimeData timeData) {
		String value = null;
		if ((value = TryRetrieveStringElementValue(currentSample, "Power")) != null) {
			timeData.power = Float.parseFloat(value);
			return true;
		}
		return false;
	}


	/**
	 * Attempts to retrieve and add power data to the current tour.
	 * 
	 * @param currentSample
	 *            The current sample data in JSON format.
	 * @param sampleList
	 *            The tour's time serie.
	 */
	private boolean TryAddDistanceData(JSONObject currentSample, TimeData timeData) {
		String value = null;
		if ((value = TryRetrieveStringElementValue(currentSample, "Distance")) != null) {
			timeData.absoluteDistance = Float.parseFloat(value);
			return true;
		}
		return false;
	}


	/**
	 * Attempts to retrieve and add power data to the current tour.
	 * 
	 * @param currentSample
	 *            The current sample data in JSON format.
	 * @param sampleList
	 *            The tour's time serie.
	 */
	private boolean TryAddTemperatureData(JSONObject currentSample, TimeData timeData) {
		String value = null;
		if ((value = TryRetrieveStringElementValue(currentSample, "Temperature")) != null) {
			timeData.temperature = Float.parseFloat(value) - Kelvin;
			return true;
		}
		return false;
	}


	/**
	 * Searches for an element and returns its value as a string.
	 * 
	 * @param token
	 *            The JSON token in which to look for a given element.
	 * @param elementName
	 *            The element name to look for in a JSON content.
	 * @return The element value, if found.
	 */
	private String TryRetrieveStringElementValue(JSONObject token, String elementName) {
		if (!token.toString().contains(elementName))
			return null;

		String result = null;
		try {
			result = token.get(elementName).toString();
		} catch (Exception e) {

		}
		if (result == "null")
			return null;

		return result;
	}
}
