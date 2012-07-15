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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.tourbook.chart.ChartLabel;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.device.InvalidDeviceSAXException;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.preferences.TourTypeColorDefinition;

import org.eclipse.osgi.util.NLS;
import org.joda.time.DateTime;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.byteholder.geoclipse.map.UI;

/**
 * This sax handler performs data import for the following scema:
 * 
 * <pre>
 * &lt;xs:schema version="1.0"
 * 	xmlns:xs="http://www.w3.org/2001/XMLSchema"
 * 	targetNamespace="http://www.polarpersonaltrainer.com"
 * 	xmlns="http://www.polarpersonaltrainer.com"
 * 	elementFormDefault="qualified"
 * 	attributeFormDefault="unqualified"&gt;
 * 
 * 	&lt;xs:annotation&gt;
 * 		&lt;xs:appinfo&gt;Polar Personaltrainer.com data export&lt;/xs:appinfo&gt;
 * 		&lt;xs:documentation xml:lang="en"&gt;
 * 			XML Schema for validating data objects exported from Polar Personaltrainer.com
 * 
 * 			&lt;b&gt;ALL UNITS ARE ASSUMED METRIC.&lt;/b&gt;
 * 		&lt;/xs:documentation&gt;
 * 	&lt;/xs:annotation&gt;
 * </pre>
 */
public class PolarTrainerSAXHandler extends DefaultHandler {

	private static final String		DEVICE_NAME_POLAR_PERSONALTRAINER	= "Polar Personal Trainer";											//$NON-NLS-1$

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
	 * 
	 * value <code>DISTANCE</code> is available but not documented in
	 */
	private static final String		SAMPLE_TYPE_ALTITUDE				= "ALTITUDE";															//$NON-NLS-1$
	private static final String		SAMPLE_TYPE_CADENCE					= "CADENCE";															//$NON-NLS-1$
	private static final String		SAMPLE_TYPE_DISTANCE				= "DISTANCE";															//$NON-NLS-1$
	private static final String		SAMPLE_TYPE_HEARTRATE				= "HEARTRATE";															//$NON-NLS-1$
	private static final String		SAMPLE_TYPE_SPEED					= "SPEED";																//$NON-NLS-1$
	private static final String		SAMPLE_TYPE_TEMPERATURE				= "TEMPERATURE";														//$NON-NLS-1$
//	private static final String		SAMPLE_TYPE_POWER			= "POWER";
//	private static final String		SAMPLE_TYPE_POWER_PI		= "POWER_PI";
//	private static final String		SAMPLE_TYPE_POWER_LRB		= "POWER_LRB";
//	private static final String		SAMPLE_TYPE_AIR_PRESSURE	= "AIR_PRESSURE";
//	private static final String		SAMPLE_TYPE_RUN_CADENCE		= "RUN_CADENCE";
																																				//
	private static final String		PATTERN_HHMMSS						= "(\\d{0,2}):*(\\d{0,2}):*(\\d{0,2}).*";								//$NON-NLS-1$
	private static final String		PATTERN_DATE_TIME					= "(\\d{4})-(\\d{2})-(\\d{2})\\s(\\d{2}):(\\d{2}):(\\d{2})\\.\\d{1}";	//$NON-NLS-1$
	//
	private static final Pattern	_patternHHMMSS						= Pattern.compile(
																				PATTERN_HHMMSS,
																				Pattern.DOTALL);
	private static final Pattern	_patternDateTime					= Pattern.compile(
																				PATTERN_DATE_TIME,
																				Pattern.DOTALL);
	//
	private static final String		TAG_ROOT							= "polar-exercise-data";												//$NON-NLS-1$
	private static final String		TAG_ROOT_XMLNS						= "xmlns";																//$NON-NLS-1$
	private static final String		TAG_ROOT_XMLNS_POLARTRAINER			= "http://www.polarpersonaltrainer.com";								//$NON-NLS-1$
	private static final String		TAG_ROOT_VERSION					= "version";															//$NON-NLS-1$
	private static final String		TAG_ROOT_VERSION_1					= "1.0";																//$NON-NLS-1$
	//
	private static final String		TAG_EXERCISE						= "exercise";															//$NON-NLS-1$
	private static final String		TAG_EXERCISE_CREATED				= "created";															//$NON-NLS-1$
	private static final String		TAG_EXERCISE_TIME					= "time";																//$NON-NLS-1$
	private static final String		TAG_EXERCISE_NAME					= "name";																//$NON-NLS-1$
	private static final String		TAG_EXERCISE_SPORT					= "sport";																//$NON-NLS-1$
	//
	private static final String		TAG_RESULT							= "result";															//$NON-NLS-1$
	private static final String		TAG_RESULT_DURATION					= "duration";															//$NON-NLS-1$
	private static final String		TAG_RESULT_CALORIES					= "calories";															//$NON-NLS-1$
	private static final String		TAG_RESULT_RECORDING_RATE			= "recording-rate";													//$NON-NLS-1$
	//
	private static final String		TAG_LAPS							= "laps";																//$NON-NLS-1$
	private static final String		TAG_LAP								= "lap";																//$NON-NLS-1$
	private static final String		TAG_LAP_DURATION					= "duration";															//$NON-NLS-1$
	private static final String		TAG_LAP_DISTANCE					= "distance";															//$NON-NLS-1$
	//
	private static final String		TAG_SAMPLES							= "samples";															//$NON-NLS-1$
	private static final String		TAG_SAMPLE							= "sample";															//$NON-NLS-1$
	private static final String		TAG_SAMPLE_TYPE						= "type";																//$NON-NLS-1$
	private static final String		TAG_SAMPLE_VALUES					= "values";															//$NON-NLS-1$
	//
	private static final String		TAG_USER_SETTINGS					= "user-settings";														//$NON-NLS-1$
	private static final String		TAG_USER_SETTINGS_RESTING			= "resting";															//$NON-NLS-1$

	//
	private boolean					_isPolarDataValid					= false;
	private int						_dataVersion						= -1;
	//
	private boolean					_isInExercise;
	private boolean					_isInExerciseCreated;
	private boolean					_isInExerciseTime;
	private boolean					_isInExerciseName;
	private boolean					_isInExerciseSport;
	//
	private boolean					_isInResult;
	private boolean					_isInResultCalories;
	private boolean					_isInResultDuration;
	private boolean					_isInResultRecordingRate;
	//
	private boolean					_isInLaps;
	private boolean					_isInLap;
	private boolean					_isInLapDuration;
	private boolean					_isInLapDistance;
	//
	private boolean					_isInSamples;
	private boolean					_isInSample;
	private boolean					_isInSampleType;
	private boolean					_isInSampleValues;
	//
	private boolean					_isInUserSettings;
	private boolean					_isInUserSettingsResting;

	private TourbookDevice			_device;
	private String					_importFilePath;
	private HashMap<Long, TourData>	_alreadyImportedTours;
	private HashMap<Long, TourData>	_newlyImportedTours;
	private ArrayList<TourType>		_allTourTypes;

	private boolean					_isImported;
	private boolean					_isDebug							= false;
	private boolean					_isNewTourType						= false;

	private ArrayList<TimeData>		_timeSlices							= new ArrayList<TimeData>();
	private ArrayList<Lap>			_laps								= new ArrayList<Lap>();

	private Exercise				_currentExercise;
	private Lap						_currentLap;
	private String					_currentSampleType;

	private StringBuilder			_characters							= new StringBuilder(100);

	private class Exercise {

		private String		tourTitle;
		private String		sport;

		private DateTime	tourStart;
		private DateTime	dtCreated;
		private DateTime	dtStartTime;

		private short		timeInterval;

		/**
		 * in kcal
		 */
		private int			calories	= -1;

		private int			restPulse;
		private int			recordingRate;

		/**
		 * in seconds
		 */
		private long		duration	= -1;

		private float[]		altitudeValues;
		private float[]		cadenceValues;
		private float[]		distanceValues;
		private float[]		pulseValues;
		private float[]		speedValues;
		private float[]		temperatureValues;

	}

	private class Lap {

		private int		duration	= -1;	// <!-- 500 hours max -->
		private float	distance;

		@Override
		public String toString() {
			return "Lap [duration=" + duration + ", distance=" + distance + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

	}

	public PolarTrainerSAXHandler(	final TourbookDevice device,
									final String importFileName,
									final DeviceData deviceData,
									final HashMap<Long, TourData> alreadyImportedTours,
									final HashMap<Long, TourData> newlyImportedTours) {

		_device = device;
		_importFilePath = importFileName;
		_alreadyImportedTours = alreadyImportedTours;
		_newlyImportedTours = newlyImportedTours;

		_allTourTypes = TourDatabase.getAllTourTypes();
	}

	@Override
	public void characters(final char[] chars, final int startIndex, final int length) throws SAXException {

		if (_isPolarDataValid && (//
				_isInLapDuration
						|| _isInLapDistance
						|| _isInSampleType
						|| _isInSampleValues
						|| _isInExerciseCreated
						|| _isInExerciseTime
						|| _isInExerciseName
						|| _isInExerciseSport
						|| _isInResultCalories
						|| _isInResultRecordingRate || _isInResultDuration
				//
				)) {

			_characters.append(chars, startIndex, length);
		}
	}

	public void dispose() {
		_laps.clear();
		_timeSlices.clear();
	}

	@Override
	public void endElement(final String uri, final String localName, final String name) throws SAXException {

//		System.out.println("</" + name + ">\t" + _isInSample);

		try {

			/*
			 * get values
			 */
			if (_isInLap) {

				parseLap02End(name);

			} else if (_isInSample) {

				parseSample02End(name);

			} else if (_isInResult) {

				parseResult02End(name);

			} else if (_isInUserSettings) {

				parseUserSettings02End(name);

			} else if (_isInExerciseCreated) {

				_isInExerciseCreated = false;
				_currentExercise.dtCreated = getDateTime(_characters.toString());

			} else if (_isInExerciseTime) {

				_isInExerciseTime = false;
				_currentExercise.dtStartTime = getDateTime(_characters.toString());

			} else if (_isInExerciseName) {

				_isInExerciseName = false;
				_currentExercise.tourTitle = _characters.toString();

			} else if (_isInExerciseSport) {

				_isInExerciseSport = false;
				_currentExercise.sport = _characters.toString();
			}

			/*
			 * reset state
			 */
			if (name.equals(TAG_SAMPLES)) {

				_isInSamples = false;

			} else if (name.equals(TAG_SAMPLE)) {

				_isInSample = false;

			} else if (name.equals(TAG_LAPS)) {

				_isInLaps = false;

			} else if (name.equals(TAG_LAP)) {

				_isInLap = false;

				_laps.add(_currentLap);

				if (_isDebug) {
					System.out.println("\t" + _currentLap); //$NON-NLS-1$
				}

			} else if (name.equals(TAG_USER_SETTINGS)) {

				// /polar-exercise-data/calendar-items/exercise/result/user-settings/heart-rate/resting

				_isInUserSettings = false;

			} else if (name.equals(TAG_RESULT)) {

				_isInResult = false;

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

	private void finalizeTour() throws InvalidDeviceSAXException {

		if (finalizeTour10CreateTimeSlices() == false) {
			_isImported = false;
			return;
		}

		// create data object for each tour
		final TourData tourData = new TourData();

		/*
		 * set tour start date/time
		 */
		final DateTime tourStart = _currentExercise.tourStart;

		tourData.setStartHour((short) tourStart.getHourOfDay());
		tourData.setStartMinute((short) tourStart.getMinuteOfHour());
		tourData.setStartSecond((short) tourStart.getSecondOfMinute());

		tourData.setStartYear((short) tourStart.getYear());
		tourData.setStartMonth((short) tourStart.getMonthOfYear());
		tourData.setStartDay((short) tourStart.getDayOfMonth());

		tourData.setWeek(tourStart);

		tourData.setDeviceTimeInterval(_currentExercise.timeInterval);

		tourData.importRawDataFile = _importFilePath;

		tourData.setTourImportFilePath(_importFilePath);

		tourData.setTourTitle(_currentExercise.tourTitle);
		tourData.setCalories(_currentExercise.calories);
		tourData.setRestPulse(_currentExercise.restPulse);

		tourData.createTimeSeries(_timeSlices, true);

		finalizeTour20CreateMarkers(tourData);
		finalizeTour30SetTourType(tourData);

		tourData.computeAltitudeUpDown();

		// after all data are added, the tour id can be created
		final Long tourId = tourData
				.createTourId(_device.createUniqueId(tourData, Util.UNIQUE_ID_SUFFIX_POLAR_TRAINER));

		// check if the tour is already imported
		if (_alreadyImportedTours.containsKey(tourId) == false) {

			tourData.computeTourDrivingTime();
			tourData.computeComputedValues();

			tourData.setDeviceId(_device.deviceId);
			tourData.setDeviceName(DEVICE_NAME_POLAR_PERSONALTRAINER);

			tourData.setDeviceFirmwareVersion(_dataVersion == 1 ? TAG_ROOT_VERSION_1 : UI.EMPTY_STRING);

			// add new tour to other tours
			_newlyImportedTours.put(tourId, tourData);
		}

		_isImported = true;
	}

	/**
	 * @throws InvalidDeviceSAXException
	 */
	private boolean finalizeTour10CreateTimeSlices() throws InvalidDeviceSAXException {

		_timeSlices.clear();

		final float[] altitudeValues = _currentExercise.altitudeValues;
		final float[] cadenceValues = _currentExercise.cadenceValues;
		final float[] distanceValues = _currentExercise.distanceValues;
		final float[] pulseValues = _currentExercise.pulseValues;
		final float[] speedValues = _currentExercise.speedValues;
		final float[] temperatureValues = _currentExercise.temperatureValues;

		final DateTime dtCreated = _currentExercise.dtCreated;
		final DateTime dtExerciseStartTime = _currentExercise.dtStartTime;
		final long exerciseDuration = _currentExercise.duration;

		/*
		 * dtCreated is always available (minOccurs="1"), exercise start time is optional
		 */
		DateTime dtStartTime = dtExerciseStartTime;
		if (dtStartTime == null) {
			dtStartTime = dtCreated;
		}
		final long tourStartTime = dtStartTime.getMillis();
		_currentExercise.tourStart = dtStartTime;

		/*
		 * get value length
		 */
		int valueLength = -1;
		if (altitudeValues != null) {
			valueLength = altitudeValues.length;
		} else if (cadenceValues != null) {
			valueLength = cadenceValues.length;
		} else if (distanceValues != null) {
			valueLength = distanceValues.length;
		} else if (pulseValues != null) {
			valueLength = pulseValues.length;
		} else if (speedValues != null) {
			valueLength = speedValues.length;
		} else if (temperatureValues != null) {
			valueLength = temperatureValues.length;
		}

		// check if data are available
		if (valueLength < 1) {
			// at least one value must be available
			return false;
		}

		final boolean isAltitude = altitudeValues != null;
		final boolean isCadence = cadenceValues != null;
		final boolean isDistance = distanceValues != null;
		final boolean isSpeed = speedValues != null;
		final boolean isPulse = pulseValues != null;
		final boolean isTemperature = temperatureValues != null;

		/*
		 * get time interval
		 */
		int timeInterval = _currentExercise.recordingRate;
		if (timeInterval < 1) {

			final float interval = (float) exerciseDuration / valueLength;
			timeInterval = (int) (interval + 0.5);

			if (timeInterval == 0) {
				throw new InvalidDeviceSAXException(NLS.bind("Tour time interval cannot be determined in: {0}", //$NON-NLS-1$
						_importFilePath));
			}
		}
		_currentExercise.timeInterval = (short) timeInterval;

		float absoluteDistance = 0;

		/*
		 * create slices
		 */
		for (int valueIndex = 0; valueIndex < valueLength; valueIndex++) {

			final TimeData timeSlice = new TimeData();

			timeSlice.absoluteTime = tourStartTime + (valueIndex * timeInterval * 1000);

			if (isAltitude) {
				timeSlice.absoluteAltitude = altitudeValues[valueIndex];
			}

			if (isCadence) {
				timeSlice.cadence = cadenceValues[valueIndex];
			}

			if (isDistance) {
				timeSlice.absoluteDistance = distanceValues[valueIndex];
			} else if (isSpeed) {

				// get distance from speed, speed is not saved it is always computed

				final float speedMeterSecond = (float) (speedValues[valueIndex] * (1000.0 / 3600));
				final float distanceDiff = speedMeterSecond * timeInterval;

				absoluteDistance += distanceDiff;

				timeSlice.absoluteDistance = absoluteDistance;
			}

			if (isPulse) {
				timeSlice.pulse = pulseValues[valueIndex];
			}

			if (isTemperature) {
				timeSlice.temperature = temperatureValues[valueIndex];
			}

			_timeSlices.add(timeSlice);
		}

		return true;
	}

	private void finalizeTour20CreateMarkers(final TourData tourData) {

		if (_laps.size() == 0) {
			return;
		}

		final int[] timeSerie = tourData.timeSerie;
		if (timeSerie.length == 0) {
			return;
		}

		final Set<TourMarker> tourMarkers = tourData.getTourMarkers();
		final float[] distanceSerie = tourData.distanceSerie;

		int sumLapDuration = 0;
		int lapCounter = 1;

		for (final Lap lap : _laps) {

			final int lapDuration = lap.duration;
			int serieIndex = 0;

			// get serie index
			for (final int tourRelativeTime : timeSerie) {
				if (tourRelativeTime >= sumLapDuration + lapDuration) {
					break;
				}
				serieIndex++;
			}

			// check array bounds
			if (serieIndex >= timeSerie.length) {
				serieIndex = timeSerie.length - 1;
			}

			final TourMarker tourMarker = new TourMarker(tourData, ChartLabel.MARKER_TYPE_DEVICE);

			tourMarker.setLabel(Integer.toString(lapCounter));
			tourMarker.setSerieIndex(serieIndex);
			tourMarker.setTime(sumLapDuration + lapDuration);
			tourMarker.setVisualPosition(ChartLabel.VISUAL_HORIZONTAL_ABOVE_GRAPH_CENTERED);

			if (distanceSerie != null) {
				tourMarker.setDistance(distanceSerie[serieIndex]);
			}

			tourMarkers.add(tourMarker);

			lapCounter++;
			sumLapDuration += lapDuration;
		}
	}

	/**
	 * Set tour type from category field
	 * 
	 * @param tourData
	 */
	private void finalizeTour30SetTourType(final TourData tourData) {

		final String sport = _currentExercise.sport;

		if (sport == null) {
			return;
		}

		TourType tourType = null;

		// find tour type in existing tour types
		for (final TourType mapTourType : _allTourTypes) {
			if (sport.equalsIgnoreCase(mapTourType.getName())) {
				tourType = mapTourType;
				break;
			}
		}

		TourType newSavedTourType = null;

		if (tourType == null) {

			// create new tour type

			final TourType newTourType = new TourType(sport);

			final TourTypeColorDefinition newColorDefinition = new TourTypeColorDefinition(
					newTourType,
					Long.toString(newTourType.getTypeId()),
					newTourType.getName());

			newTourType.setColorBright(newColorDefinition.getDefaultGradientBright());
			newTourType.setColorDark(newColorDefinition.getDefaultGradientDark());
			newTourType.setColorLine(newColorDefinition.getDefaultLineColor());
			newTourType.setColorText(newColorDefinition.getDefaultTextColor());

			// save new entity
			newSavedTourType = TourDatabase.saveEntity(newTourType, newTourType.getTypeId(), TourType.class);
			if (newSavedTourType != null) {
				tourType = newSavedTourType;
				_allTourTypes.add(tourType);
			}
		}

		tourData.setTourType(tourType);

		_isNewTourType |= newSavedTourType != null;
	}

	/**
	 * Create date/time with the regex:
	 * 
	 * <pre>
	 * (\\d{4})-(\\d{2})-(\\d{2})\\s(\\d{2}):(\\d{2}):(\\d{2})\\.\\d{1}
	 * </pre>
	 * 
	 * @param dtValue
	 * @return Returns parsed date/time or <code>null</code> when the format is not correct.
	 */
	private DateTime getDateTime(final String dtValue) {

		final Matcher matcherResult = _patternDateTime.matcher(dtValue);
		if (matcherResult.matches()) {

			try {

				int year = 0;
				int month = 0;
				int day = 0;
				int hour = 0;
				int minute = 0;
				int seconds = 0;

				final int groupCount = matcherResult.groupCount();
				if (groupCount < 6) {
					return null;
				}

				for (int groupNo = 1; groupNo <= groupCount; groupNo++) {

					final String stringValue = matcherResult.group(groupNo);
					if (stringValue == null) {
						return null;
					}

					final int value = Integer.parseInt(stringValue);

					if (groupNo == 1) {
						year = value;
					} else if (groupNo == 2) {
						month = value;
					} else if (groupNo == 3) {
						day = value;
					} else if (groupNo == 4) {
						hour = value;
					} else if (groupNo == 5) {
						minute = value;
					} else if (groupNo == 6) {
						seconds = value;
					}
				}

				return new DateTime(year, month, day, hour, minute, seconds, 0);

			} catch (final NumberFormatException e) {
				return null;
			}
		}

		return null;
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

	private int getIntValue(final String textValue) {
		try {
			if (textValue != null) {
				return Integer.parseInt(textValue);
			} else {
				return Integer.MIN_VALUE;
			}

		} catch (final NumberFormatException e) {
			return Integer.MIN_VALUE;
		}
	}

	private void initNewTour() {

		_laps.clear();

		_currentSampleType = null;
		_currentExercise = new Exercise();
	}

	/**
	 * @return Returns <code>true</code> when a tour was imported
	 */
	public boolean isImported() {
		return _isImported;
	}

	boolean isNewTourType() {
		return _isNewTourType;
	}

	private void parseLap01Start(final String name) {

		if (name.equals(TAG_LAP_DURATION)) {
			_isInLapDuration = true;
		} else if (name.equals(TAG_LAP_DISTANCE)) {
			_isInLapDistance = true;
		} else {
			return;
		}

		_characters.delete(0, _characters.length());
	}

	private void parseLap02End(final String name) {

		if (_isInLapDuration && name.equals(TAG_LAP_DURATION)) {

			// <duration>02:11:24</duration>

			_isInLapDuration = false;
			_currentLap.duration = (int) getHHMMSS(_characters.toString());

		} else if (_isInLapDistance && name.equals(TAG_LAP_DISTANCE)) {

			// <distance>62200.0</distance>

			_isInLapDistance = false;
			_currentLap.distance = getFloatValue(_characters.toString());
		}
	}

	private void parseResult01Start(final String name) {

		if (name.equals(TAG_RESULT_CALORIES)) {
			_isInResultCalories = true;
		} else if (name.equals(TAG_RESULT_RECORDING_RATE)) {
			_isInResultRecordingRate = true;
		} else if (name.equals(TAG_RESULT_DURATION)) {
			_isInResultDuration = true;
		} else {
			return;
		}

		_characters.delete(0, _characters.length());
	}

	private void parseResult02End(final String name) {

		if (_isInResultCalories && name.equals(TAG_RESULT_CALORIES)) {

			_isInResultCalories = false;
			_currentExercise.calories = getIntValue(_characters.toString());

		} else if (_isInResultRecordingRate && name.equals(TAG_RESULT_RECORDING_RATE)) {

			_isInResultRecordingRate = false;
			_currentExercise.recordingRate = getIntValue(_characters.toString());

		} else if (_isInResultDuration && name.equals(TAG_RESULT_DURATION)) {

			_isInResultDuration = false;
			_currentExercise.duration = getHHMMSS(_characters.toString());
		}
	}

	private void parseSample01Start(final String name) {

		if (name.equals(TAG_SAMPLE_TYPE)) {
			_isInSampleType = true;
		} else if (name.equals(TAG_SAMPLE_VALUES)) {
			_isInSampleValues = true;
		} else {
			return;
		}

		_characters.delete(0, _characters.length());
	}

	private void parseSample02End(final String name) {

		if (_isInSampleType && name.equals(TAG_SAMPLE_TYPE)) {

			// e.g. <type>TEMPERATURE</type>

			_isInSampleType = false;

			final String sampleType = _characters.toString();

			if (sampleType.equals(SAMPLE_TYPE_ALTITUDE)
					|| sampleType.equals(SAMPLE_TYPE_CADENCE)
					|| sampleType.equals(SAMPLE_TYPE_DISTANCE)
					|| sampleType.equals(SAMPLE_TYPE_HEARTRATE)
					|| sampleType.equals(SAMPLE_TYPE_SPEED)
					|| sampleType.equals(SAMPLE_TYPE_TEMPERATURE)) {
				_currentSampleType = sampleType;
			} else {
				_currentSampleType = null;
			}

		} else if (_isInSampleValues && name.equals(TAG_SAMPLE_VALUES)) {

			// e.g. <values>15.1,15.1,15.1,15,15,14.9,14.8 ..... 16.9,16.8</values>

			_isInSampleValues = false;

			parseSampleValues(_characters.toString());
		}
	}

	private void parseSampleValues(final String valueString) {

		if (_currentSampleType == null) {
			return;
		}

		final float[] sampleValues = parseSampleValues10(valueString);
		if (sampleValues == null) {
			return;
		}

		if (_currentSampleType.equals(SAMPLE_TYPE_ALTITUDE)) {
			_currentExercise.altitudeValues = sampleValues;
		} else if (_currentSampleType.equals(SAMPLE_TYPE_CADENCE)) {
			_currentExercise.cadenceValues = sampleValues;
		} else if (_currentSampleType.equals(SAMPLE_TYPE_DISTANCE)) {
			_currentExercise.distanceValues = sampleValues;
		} else if (_currentSampleType.equals(SAMPLE_TYPE_HEARTRATE)) {
			_currentExercise.pulseValues = sampleValues;
		} else if (_currentSampleType.equals(SAMPLE_TYPE_SPEED)) {
			_currentExercise.speedValues = sampleValues;
		} else if (_currentSampleType.equals(SAMPLE_TYPE_TEMPERATURE)) {
			_currentExercise.temperatureValues = sampleValues;
		}
	}

	/**
	 * Get all values from the &lt;sample&gt; tag
	 * 
	 * @param valueString
	 * @return Returns samples as floating values.
	 */
	private float[] parseSampleValues10(final String valueString) {

		float[] floatValues = null;

		try {

			final ArrayList<String> floatStrings = new ArrayList<String>();

			final StringTokenizer tokenizer = new StringTokenizer(valueString, UI.KOMMA);
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

	private void parseUserSettings01Start(final String name) {

		if (name.equals(TAG_USER_SETTINGS_RESTING)) {
			_isInUserSettingsResting = true;
		} else {
			return;
		}

		_characters.delete(0, _characters.length());
	}

	private void parseUserSettings02End(final String name) {

		if (_isInUserSettingsResting && name.equals(TAG_USER_SETTINGS_RESTING)) {

			_isInUserSettingsResting = false;
			_currentExercise.restPulse = getIntValue(_characters.toString());
		}
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

				} else if (_isInResult) {

					parseResult01Start(name);

				} else if (_isInUserSettings) {

					parseUserSettings01Start(name);
				}

				if (name.equals(TAG_LAPS)) {

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

				} else if (name.equals(TAG_USER_SETTINGS)) {

					// /polar-exercise-data/calendar-items/exercise/result/user-settings/heart-rate/resting

					_isInUserSettings = true;

				} else if (name.equals(TAG_EXERCISE_CREATED)) {

					// /polar-exercise-data/calendar-items/exercise/created

					_isInExerciseCreated = true;
					_characters.delete(0, _characters.length());

				} else if (name.equals(TAG_EXERCISE_TIME)) {

					// /polar-exercise-data/calendar-items/exercise/time

					_isInExerciseTime = true;
					_characters.delete(0, _characters.length());

				} else if (name.equals(TAG_EXERCISE_NAME)) {

					// /polar-exercise-data/calendar-items/exercise/name

					_isInExerciseName = true;
					_characters.delete(0, _characters.length());

				} else if (name.equals(TAG_EXERCISE_SPORT)) {

					// /polar-exercise-data/calendar-items/exercise/sport

					_isInExerciseSport = true;
					_characters.delete(0, _characters.length());

				} else if (name.equals(TAG_RESULT)) {

					// /polar-exercise-data/calendar-items/exercise/result

					_isInResult = true;
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
					_isPolarDataValid = true;
					return;
				}
			}

			throw new InvalidDeviceSAXException(NLS.bind("Polar xml data are not valid in: {0}", _importFilePath)); //$NON-NLS-1$

		}
	}

}
