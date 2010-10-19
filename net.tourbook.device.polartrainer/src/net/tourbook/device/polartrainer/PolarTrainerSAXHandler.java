/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.device.polartrainer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.device.InvalidDeviceSAXException;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.util.StatusUtil;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PolarTrainerSAXHandler extends DefaultHandler {

	/**
	 * <pre>
	 *       <xs:enumeration value="HEARTRATE"/>
	 *       <xs:enumeration value="SPEED"/>
	 *       <xs:enumeration value="CADENCE"/>
	 *       <xs:enumeration value="ALTITUDE"/>
	 *       <xs:enumeration value="POWER"/>
	 *       <xs:enumeration value="POWER_PI"/>
	 *       <xs:enumeration value="POWER_LRB"/>
	 *       <xs:enumeration value="AIR_PRESSURE"/>
	 *       <xs:enumeration value="RUN_CADENCE"/>
	 *       <xs:enumeration value="TEMPERATURE"/>
	 * </pre>
	 */
	private static final String				SAMPLE_TYPE_HEARTRATE		= "HEARTRATE";
	private static final String				SAMPLE_TYPE_SPEED			= "SPEED";
	private static final String				SAMPLE_TYPE_CADENCE			= "CADENCE";
	private static final String				SAMPLE_TYPE_ALTITUDE		= "ALTITUDE";
//	private static final String				SAMPLE_TYPE_POWER			= "POWER";
//	private static final String				SAMPLE_TYPE_POWER_PI		= "POWER_PI";
//	private static final String				SAMPLE_TYPE_POWER_LRB		= "POWER_LRB";
//	private static final String				SAMPLE_TYPE_AIR_PRESSURE	= "AIR_PRESSURE";
//	private static final String				SAMPLE_TYPE_RUN_CADENCE		= "RUN_CADENCE";
	private static final String				SAMPLE_TYPE_TEMPERATURE		= "TEMPERATURE";

	private static final String				PATTERN_HHMMSS				= "(\\d{0,2}):*(\\d{0,2}):*(\\d{0,2}).*";

	private static final Pattern			_patternHHMMSS				= Pattern.compile(
																				PATTERN_HHMMSS,
																				Pattern.DOTALL);

	private static final String				TAG_ROOT					= "polar-exercise-data";							//$NON-NLS-1$
	private static final String				TAG_ROOT_XMLNS				= "xmlns";											//$NON-NLS-1$
	private static final String				TAG_ROOT_XMLNS_POLARTRAINER	= "http://www.polarpersonaltrainer.com";			//$NON-NLS-1$
	private static final String				TAG_ROOT_VERSION			= "version";										//$NON-NLS-1$
	private static final String				TAG_ROOT_VERSION_1			= "1.0";											//$NON-NLS-1$
	//
	private static final String				TAG_EXERCISE				= "exercise";
	//
	private static final String				TAG_LAPS					= "laps";
	private static final String				TAG_LAP						= "lap";											//$NON-NLS-1$
	private static final String				TAG_LAP_DURATION			= "duration";
	private static final String				TAG_LAP_DISTANCE			= "distance";
	//
	private static final String				TAG_SAMPLES					= "samples";										//$NON-NLS-1$
	private static final String				TAG_SAMPLE					= "sample";
	private static final String				TAG_SAMPLE_TYPE				= "type";
	private static final String				TAG_SAMPLE_VALUES			= "values";

	private static final DateTimeFormatter	_dtParser					= ISODateTimeFormat.dateTimeParser();

	private static final SimpleDateFormat	TIME_FORMAT					= new SimpleDateFormat(
																				"yyyy-MM-dd'T'HH:mm:ss'Z'");				//$NON-NLS-1$

	private static final SimpleDateFormat	TIME_FORMAT_SSSZ			= new SimpleDateFormat(
																				"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");			//$NON-NLS-1$
	private static final SimpleDateFormat	TIME_FORMAT_RFC822			= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");	//$NON-NLS-1$

	private int								_dataVersion				= -1;

	private boolean							_isPolarData				= false;
	private boolean							_isInExercise;
	//
	private boolean							_isInLaps;
	private boolean							_isInLap;
	private boolean							_isInLapDuration;
	private boolean							_isInLapDistance;
	//
	private boolean							_isInSamples;
	private boolean							_isInSample;
	private boolean							_isInSampleType;
	private boolean							_isInSampleValues;

	private ArrayList<Lap>					_laps						= new ArrayList<Lap>();
	private ArrayList<TimeData>				_dtList						= new ArrayList<TimeData>();

	private TimeData						_timeData;
	private TourbookDevice					_deviceDataReader;
	private String							_importFilePath;

	private HashMap<Long, TourData>			_tourDataMap;

	private boolean							_isImported;

	private String							_activitySport				= null;
	private int								_calories;
	private boolean							_isDistanceFromSensor		= false;

	private StringBuilder					_characters					= new StringBuilder(100);

	private Lap								_currentLap;

	private boolean							_debug						= false;

	private String							_sampleType;
	private float[]							_altitudeValues;
	private float[]							_cadenceValues;
	private float[]							_pulseValues;
	private float[]							_speedValues;
	private float[]							_temperatureValues;

	{
		TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
		TIME_FORMAT_SSSZ.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
		TIME_FORMAT_RFC822.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
	}

	private class Lap {

		private long	duration	= -1;
		public float	distance;

		@Override
		public String toString() {
			return "Lap [duration=" + duration + ", distance=" + distance + "]";
		}

	}

	public PolarTrainerSAXHandler(	final TourbookDevice deviceDataReader,
									final String importFileName,
									final DeviceData deviceData,
									final HashMap<Long, TourData> tourDataMap) {

		_deviceDataReader = deviceDataReader;
		_importFilePath = importFileName;
		_tourDataMap = tourDataMap;
	}

	@Override
	public void characters(final char[] chars, final int startIndex, final int length) throws SAXException {

		if (_isPolarData && (//
				_isInLapDuration || _isInLapDistance
				//
				)) {

			_characters.append(chars, startIndex, length);
		}
	}

	public void dispose() {
		_laps.clear();
		_dtList.clear();
	}

	@Override
	public void endElement(final String uri, final String localName, final String name) throws SAXException {

		//System.out.println("</" + name + ">");

		try {

			if (_isInLap) {
				parseLap02End(name);
			}

			if (_isInSample) {
				parseSample02End(name);
			}

			if (name.equals(TAG_SAMPLES)) {

				_isInSamples = false;

			} else if (name.equals(TAG_SAMPLE)) {

				_isInSample = false;

				finalizeSample();

			} else if (name.equals(TAG_LAPS)) {

				_isInLaps = false;

			} else if (name.equals(TAG_LAP)) {

				_isInLap = false;

				_laps.add(_currentLap);

				if (_debug) {
					System.out.println("\t" + _currentLap);
				}

			} else if (name.equals(TAG_EXERCISE)) {

				/*
				 * exercise/tour ends
				 */

				_isInExercise = false;

				finalizeTour();
			}

		} catch (final NumberFormatException e) {
			StatusUtil.showStatus(e);
		}

	}

	private void finalizeSample() {
		// TODO Auto-generated method stub

	}

	private void finalizeTour() {

		// check if data are available
		if (_dtList.size() == 0) {
			return;
		}

		validateTimeSeries();

		// create data object for each tour
		final TourData tourData = new TourData();

		// set tour notes
//		setTourNotes(tourData);

		/*
		 * set tour start date/time
		 */

		/*
		 * Check if date time starts with the date 2007-04-01, this can happen when the tcx file is
		 * partly corrupt. When tour starts with the date 2007-04-01, move forward in the list until
		 * another date occures and use this as the start date.
		 */
		int validIndex = 0;
		DateTime dt = null;

		for (final TimeData timeData : _dtList) {

			dt = new DateTime(timeData.absoluteTime);

			if (dt.getYear() == 2007 && dt.getMonthOfYear() == 4 && dt.getDayOfMonth() == 1) {

				// this is an invalid time slice

				validIndex++;
				continue;

			} else {

				// this is a valid time slice
				break;
			}
		}

		if (validIndex == 0) {

			// date is not 2007-04-01

		} else {

			if (validIndex == _dtList.size()) {

				// all time data start with 2007-04-01

				dt = new DateTime(_dtList.get(0).absoluteTime);

			} else {

				// the date starts with 2007-04-01 but it changes to another date

				dt = new DateTime(_dtList.get(validIndex).absoluteTime);

				/*
				 * create a new list by removing invalid time slices
				 */

				final ArrayList<TimeData> oldDtList = _dtList;
				_dtList = new ArrayList<TimeData>();

				int _tdIndex = 0;
				for (final TimeData timeData : oldDtList) {

					if (_tdIndex < validIndex) {
						_tdIndex++;
						continue;
					}

					_dtList.add(timeData);
				}

//				StatusUtil.showStatus(NLS.bind(//
//						Messages.Garmin_SAXHandler_InvalidDate_2007_04_01,
//						_importFilePath,
//						dt.toString()));
			}
		}

		tourData.setIsDistanceFromSensor(_isDistanceFromSensor);

		tourData.setStartHour((short) dt.getHourOfDay());
		tourData.setStartMinute((short) dt.getMinuteOfHour());
		tourData.setStartSecond((short) dt.getSecondOfMinute());

		tourData.setStartYear((short) dt.getYear());
		tourData.setStartMonth((short) dt.getMonthOfYear());
		tourData.setStartDay((short) dt.getDayOfMonth());

		tourData.setWeek(dt);

		tourData.setDeviceTimeInterval((short) -1);
		tourData.importRawDataFile = _importFilePath;
		tourData.setTourImportFilePath(_importFilePath);

		tourData.createTimeSeries(_dtList, true);
		tourData.computeAltitudeUpDown();

		tourData.setDeviceModeName(_activitySport);

		tourData.setCalories(_calories);

		// after all data are added, the tour id can be created
		final int[] distanceSerie = tourData.getMetricDistanceSerie();
		String uniqueKey;

		if (_deviceDataReader.isCreateTourIdWithRecordingTime) {

			/*
			 * 25.5.2009: added recording time to the tour distance for the unique key because tour
			 * export and import found a wrong tour when exporting was done with camouflage speed ->
			 * this will result in a NEW tour
			 */
			final int tourRecordingTime = tourData.getTourRecordingTime();

			if (distanceSerie == null) {
				uniqueKey = Integer.toString(tourRecordingTime);
			} else {

				final long tourDistance = distanceSerie[(distanceSerie.length - 1)];

				uniqueKey = Long.toString(tourDistance + tourRecordingTime);
			}

		} else {

			/*
			 * original version to create tour id
			 */
			if (distanceSerie == null) {
				uniqueKey = "42984"; //$NON-NLS-1$
			} else {
				uniqueKey = Integer.toString(distanceSerie[distanceSerie.length - 1]);
			}
		}

		final Long tourId = tourData.createTourId(uniqueKey);

		// check if the tour is already imported
		if (_tourDataMap.containsKey(tourId) == false) {

			tourData.computeTourDrivingTime();
			tourData.computeComputedValues();

			tourData.setDeviceId(_deviceDataReader.deviceId);
			tourData.setDeviceName(_deviceDataReader.visibleName);

			// add new tour to other tours
			_tourDataMap.put(tourId, tourData);
		}

		_isImported = true;
	}

	private float getFloatValue(final String textValue) {

		try {
			if (textValue != null) {
				return Float.parseFloat(textValue);
			} else {
				return Float.MIN_VALUE;
			}

		} catch (final NumberFormatException e) {
			return Float.MIN_VALUE;
		}
	}

	/**
	 * Parse hh:mm:ss values with <code>\d{0,2}:*\d{0,2}:*\d{0,2}</code>
	 * 
	 * @param hhmmssValue
	 * @return Returns hh:mm:ss value in seconds or <code>-1</code> when hhmmssValue falue cannot be
	 *         parsed.
	 */
	private long getHHMMSS(final String hhmmssValue) {

		final Matcher matcherResult = _patternHHMMSS.matcher(hhmmssValue);
		if (matcherResult.matches()) {

			try {

				long returnValue = -1;

				final int groupCount = matcherResult.groupCount();
				if (groupCount < 3) {
					return -1;
				}

				for (int groupNo = 1; groupNo <= groupCount; groupNo++) {

					final String stringValue = matcherResult.group(groupNo);
					if (stringValue == null) {
						return -1;
					}

					final long value = Long.parseLong(stringValue);

					if (groupNo == 1) {
						returnValue = value * 3600;
					} else if (groupNo == 2) {
						returnValue += value * 60;
					} else if (groupNo == 3) {
						returnValue += value;
					}
				}

				return returnValue;

			} catch (final NumberFormatException e) {
				return -1;
			}
		}

		return -1;
	}

	private void initNewTour() {

		_laps.clear();
		_dtList.clear();

		_sampleType = null;

		_altitudeValues = null;
		_cadenceValues = null;
		_pulseValues = null;
		_speedValues = null;
		_temperatureValues = null;

	}

	/**
	 * @return Returns <code>true</code> when a tour was imported
	 */
	public boolean isImported() {
		return _isImported;
	}

	private void parseLap01Start(final String name) {

		if (name.equals(TAG_LAP_DURATION)) {
			_isInLapDuration = true;
			_characters.delete(0, _characters.length());

		} else if (name.equals(TAG_LAP_DISTANCE)) {
			_isInLapDistance = true;
			_characters.delete(0, _characters.length());
		}
	}

	private void parseLap02End(final String name) {

		if (_isInLapDuration && name.equals(TAG_LAP_DURATION)) {

			_isInLapDuration = false;
			_currentLap.duration = getHHMMSS(_characters.toString());

		} else if (_isInLapDistance && name.equals(TAG_LAP_DISTANCE)) {

			_isInLapDistance = false;
			_currentLap.distance = getFloatValue(_characters.toString());

		}
	}

	private void parseSample01Start(final String name) {

		if (name.equals(TAG_SAMPLE_TYPE)) {

			_isInSampleType = true;
			_characters.delete(0, _characters.length());

		} else if (name.equals(TAG_SAMPLE_VALUES)) {

			_isInSampleValues = true;
			_characters.delete(0, _characters.length());
		}
	}

	private void parseSample02End(final String name) {

		if (_isInSampleType && name.equals(TAG_SAMPLE_TYPE)) {

			_isInSampleType = false;

			final String sampleType = _characters.toString();

			if (sampleType.equals(SAMPLE_TYPE_HEARTRATE)) {
				_sampleType = sampleType;
			} else {
				_sampleType = null;
			}

		} else if (_isInSampleValues && name.equals(TAG_SAMPLE_VALUES)) {

			_isInSampleValues = false;

			parseSampleValues(_characters.toString());
		}
	}

	private void parseSampleValues(final String valueString) {

		if (_sampleType == null) {
			return;
		}

		final float[] sampleValues = parseSampleValues10(valueString);
		if (sampleValues == null) {
			return;
		}

		if (_sampleType.equals(SAMPLE_TYPE_ALTITUDE)) {
			_altitudeValues = sampleValues;
		} else if (_sampleType.equals(SAMPLE_TYPE_CADENCE)) {
			_cadenceValues = sampleValues;
		} else if (_sampleType.equals(SAMPLE_TYPE_HEARTRATE)) {
			_pulseValues = sampleValues;
		} else if (_sampleType.equals(SAMPLE_TYPE_SPEED)) {
			_speedValues = sampleValues;
		} else if (_sampleType.equals(SAMPLE_TYPE_TEMPERATURE)) {
			_temperatureValues = sampleValues;
		}
	}

	/**
	 * Parses sample values
	 * 
	 * @param valueString
	 * @return Returns samples as floating values.
	 */
	private float[] parseSampleValues10(final String valueString) {

		float[] floatValues = null;

		try {

			final ArrayList<String> floatStrings = new ArrayList<String>();

			final StringTokenizer tokenizer = new StringTokenizer(valueString);
			while (tokenizer.hasMoreElements()) {
				floatStrings.add((String) tokenizer.nextElement());
			}

			final String[] floatValueStrings = floatStrings.toArray(new String[floatStrings.size()]);
			final int floatValueLength = floatValueStrings.length;

			floatValues = new float[floatValueLength];

			for (int floatIndex = 0; floatIndex < floatValueLength; floatIndex++) {
				floatValues[floatIndex] = Float.parseFloat(floatValueStrings[floatIndex]);
			}

		} catch (final NumberFormatException e) {
			StatusUtil.showStatus(e);
			return null;
		}

		return floatValues;
	}

	@Override
	public void startElement(final String uri, final String localName, final String name, final Attributes attributes)
			throws SAXException {

//		System.out.print("<" + name + ">\n");

		if (_dataVersion == 1) {

			/*
			 * xmlns="http://www.polarpersonaltrainer.com" version="1.0"
			 */
			if (_isInExercise) {

				if (_isInLaps && _isInLap) {

					parseLap01Start(name);

				} else if (_isInSamples && _isInSample) {

					parseSample01Start(name);

				} else if (name.equals(TAG_LAPS)) {

					_isInLaps = true;

				} else if (name.equals(TAG_LAP)) {

					// a new lap starts

					_isInLap = true;
					_currentLap = new Lap();

				} else if (name.equals(TAG_SAMPLES)) {

					// /polar-exercise-data/calendar-items/exercise/result/samples

					_isInSamples = true;

				} else if (name.equals(TAG_SAMPLE)) {

					// /polar-exercise-data/calendar-items/exercise/result/samples/sample

					_isInSample = true;
				}

			} else if (name.equals(TAG_EXERCISE)) {

				/*
				 * a new exercise/tour starts
				 */

				_isInExercise = true;

				initNewTour();
			}

		} else if (_dataVersion < 0) {

			// this must be the first element

			if (name.equals(TAG_ROOT)) {

				// check version and root tag in the xml file
				boolean isPolarTrainer = false;
				_dataVersion = -1;

				for (int attrIndex = 0; attrIndex < attributes.getLength(); attrIndex++) {

					final String attrName = attributes.getQName(attrIndex);
					final String attrValue = attributes.getValue(attrIndex);

					if (attrName.equalsIgnoreCase(TAG_ROOT_XMLNS)) {
						if (attrValue.equalsIgnoreCase(TAG_ROOT_XMLNS_POLARTRAINER)) {
							isPolarTrainer = true;
							continue;
						}
					}

					if (attrName.equalsIgnoreCase(TAG_ROOT_VERSION)) {
						if (attrValue.equalsIgnoreCase(TAG_ROOT_VERSION_1)) {
							_dataVersion = 1;
							continue;
						}
					}
				}

				if (isPolarTrainer && _dataVersion > -1) {
					_isPolarData = true;
					return;
				}
			}

			throw new InvalidDeviceSAXException();

		}
	}

	/**
	 * Remove duplicated entries
	 * <p>
	 * There are cases where the lap end time and the next lap start time have the same time value,
	 * so there are duplicated times which causes problems like markers are not displayed because
	 * the marker time is twice available.
	 */
	private void validateTimeSeries() {

		final ArrayList<TimeData> removeTimeData = new ArrayList<TimeData>();

		TimeData previousTimeData = null;
		TimeData firstMarkerTimeData = null;

		for (final TimeData timeData : _dtList) {

			if (previousTimeData != null) {

				if (previousTimeData.absoluteTime == timeData.absoluteTime) {

					// current slice has the same time as the previous slice

					if (firstMarkerTimeData == null) {

						// initialize first item

						firstMarkerTimeData = previousTimeData;
					}

					// copy marker into the first time data

					if (firstMarkerTimeData.markerLabel == null && timeData.markerLabel != null) {

						firstMarkerTimeData.marker = timeData.marker;
						firstMarkerTimeData.markerLabel = timeData.markerLabel;
					}

					// remove obsolete time data
					removeTimeData.add(timeData);

				} else {

					/*
					 * current slice time is different than the previous
					 */
					firstMarkerTimeData = null;
				}
			}

			previousTimeData = timeData;
		}

		_dtList.removeAll(removeTimeData);
	}
}
