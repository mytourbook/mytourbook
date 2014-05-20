/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.device.suunto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import net.tourbook.chart.ChartLabel;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SuuntoSAXHandler extends DefaultHandler {

//	private static final String				TAG_ACTIVITY				= "Activity";							//$NON-NLS-1$
//	private static final String				TAG_ACTIVITY_CADENCE		= "Cadence";							//$NON-NLS-1$
//	private static final String				TAG_ACTIVITY_CALORIES		= "Calories";							//$NON-NLS-1$
//	private static final String				TAG_ACTIVITY_CATEGORY		= "Category";							//$NON-NLS-1$
//	private static final String				TAG_ACTIVITY_DURATION		= "Duration";							//$NON-NLS-1$
//	private static final String				TAG_ACTIVITY_DISTANCE		= "Distance";							//$NON-NLS-1$
//	private static final String				TAG_ACTIVITY_ELEVATION		= "Elevation";							//$NON-NLS-1$
//	private static final String				TAG_ACTIVITY_EQUIPMENT_ITEM	= "EquipmentItem";						//$NON-NLS-1$
//	private static final String				TAG_ACTIVITY_HEART_RATE		= "HeartRate";							//$NON-NLS-1$
//	private static final String				TAG_ACTIVITY_LOCATION		= "Location";							//$NON-NLS-1$
//	private static final String				TAG_ACTIVITY_NAME			= "Name";								//$NON-NLS-1$
//	private static final String				TAG_ACTIVITY_NOTES			= "Notes";								//$NON-NLS-1$
//	private static final String				TAG_ACTIVITY_WEATHER		= "Weather";							//$NON-NLS-1$
//
//	private static final String				ATTRIB_NAME					= "Name";								//$NON-NLS-1$
//	private static final String				ATTRIB_START_TIME			= "StartTime";							//$NON-NLS-1$
//	private static final String				ATTRIB_TOTAL_SECONDS		= "TotalSeconds";						//$NON-NLS-1$
//	private static final String				ATTRIB_TOTAL_METERS			= "TotalMeters";						//$NON-NLS-1$
//	private static final String				ATTRIB_TOTAL_CAL			= "TotalCal";							//$NON-NLS-1$
//	private static final String				ATTRIB_ASCEND_METERS		= "AscendMeters";						//$NON-NLS-1$
//	private static final String				ATTRIB_DESCEND_METERS		= "DescendMeters";						//$NON-NLS-1$
//	private static final String				ATTRIB_AVERAGE_BPM			= "AverageBPM";						//$NON-NLS-1$
//	private static final String				ATTRIB_MAXIMUM_BPM			= "MaximumBPM";						//$NON-NLS-1$
//	private static final String				ATTRIB_AVERAGE_RPM			= "AverageRPM";						//$NON-NLS-1$
//	private static final String				ATTRIB_WEATHER_TEMP			= "Temp";								//$NON-NLS-1$
//	private static final String				ATTRIB_WEATHER_CONDITIONS	= "Conditions";						//$NON-NLS-1$
//	//
//	private static final String				TAG_TRACK					= "Track";								//$NON-NLS-1$
//	private static final String				TAG_TRACK_PT				= "pt";								//$NON-NLS-1$
//	private static final String				ATTRIB_PT_TM				= "tm";								//$NON-NLS-1$
//	private static final String				ATTRIB_PT_LAT				= "lat";								//$NON-NLS-1$
//	private static final String				ATTRIB_PT_LON				= "lon";								//$NON-NLS-1$
//	private static final String				ATTRIB_PT_DIST				= "dist";								//$NON-NLS-1$
//	private static final String				ATTRIB_PT_ELE				= "ele";								//$NON-NLS-1$
//	private static final String				ATTRIB_PT_HR				= "hr";								//$NON-NLS-1$
//	private static final String				ATTRIB_PT_CADENCE			= "cadence";							//$NON-NLS-1$
//	private static final String				ATTRIB_PT_POWER				= "power";								//$NON-NLS-1$
//	//
//	private static final String				TAG_LAPS					= "Laps";								//$NON-NLS-1$
//	private static final String				TAG_LAP						= "Lap";								//$NON-NLS-1$
	//
	private String							_importFilePath;
	private SuuntoDeviceDataReader			_device;
	private HashMap<Long, TourData>			_alreadyImportedTours;
	private HashMap<Long, TourData>			_newlyImportedTours;
	private Activity						_currentActivity;

	private double							_prevLatitude;
	private double							_prevLongitude;
	private double							_distanceAbsolute;

	private boolean							_isImported					= false;
	private boolean							_isNewTag					= false;
	private boolean							_isNewTourType				= false;

	private boolean							_isInActivity;
	private boolean							_isInTrack;
	private boolean							_isInName;

	private boolean							_isInNotes;
	private boolean							_isInWeather;
	private StringBuilder					_characters					= new StringBuilder(100);

	private boolean							_isInLaps;
	private ArrayList<TourType>				_allTourTypes;

	private static final DateTimeFormatter	_dtParser					= ISODateTimeFormat.dateTimeParser();

	private class Activity {

		private ArrayList<TimeData>	timeSlices			= new ArrayList<TimeData>();
		private ArrayList<Lap>		laps				= new ArrayList<Lap>();
		private ArrayList<String>	equipmentName		= new ArrayList<String>();

		private DateTime			tourStartTime;
		private long				tourStartTimeMills	= Long.MIN_VALUE;
//		private DateTime			trackTourDateTime;
//		private long				trackTourStartTime	= Long.MIN_VALUE;

		private String				location;
		private String				name;
		private String				notes;
		private String				categoryName;

		private int					calories;
		private int					duration;
		private int					distance;
		private int					elevationUp;
		private int					elevationDown;
		private int					avgPulse;
		private int					maxPulse;
		private int					avgCadence;
//		private int					maxCadence;		is not yet supported

		private String				weatherText;
		private String				weatherConditions;
		private float				weatherTemperature	= Float.MIN_VALUE;
	}

	private class Lap {

		private long	lapStartTime;
	}

	public SuuntoSAXHandler(final SuuntoDeviceDataReader device,
							final String importFilePath,
							final HashMap<Long, TourData> alreadyImportedTours,
							final HashMap<Long, TourData> newlyImportedTours) {

		_device = device;
		_importFilePath = importFilePath;
		_alreadyImportedTours = alreadyImportedTours;
		_newlyImportedTours = newlyImportedTours;

		_allTourTypes = TourDatabase.getAllTourTypes();
	}

	@Override
	public void characters(final char[] chars, final int startIndex, final int length) throws SAXException {

//		if (_isInName || _isInNotes || _isInWeather) {
//
//			_characters.append(chars, startIndex, length);
//		}
	}

	@Override
	public void endElement(final String uri, final String localName, final String name) throws SAXException {

//		/*
//		 * get values
//		 */
//		if (_isInName || _isInNotes || _isInWeather) {
//			parseActivity02End(name);
//		}
//
//		/*
//		 * set state
//		 */
//		if (name.equals(TAG_TRACK)) {
//
//			_isInTrack = false;
//
//		} else if (name.equals(TAG_LAPS)) {
//
//			_isInLaps = false;
//
//		} else if (name.equals(TAG_ACTIVITY)) {
//
//			// activity/tour ends
//
//			_isInActivity = false;
//
//			finalizeTour();
//		}
	}

	private void finalizeTour() {

		boolean isComputeDrivingTime = true;

		// create data object for each tour
		final TourData tourData = new TourData();

		tourData.setTourStartTime(_currentActivity.tourStartTime);

		tourData.setTourTitle(_currentActivity.name);
		tourData.setTourDescription(_currentActivity.notes);
		tourData.setTourStartPlace(_currentActivity.location);

		tourData.setCalories(_currentActivity.calories);

		/*
		 * weather
		 */
//		tourData.setWeather(_currentActivity.weatherText);
//		tourData.setWeatherClouds(_weatherId.get(_currentActivity.weatherConditions));

		final float weatherTemperature = _currentActivity.weatherTemperature;
		if (weatherTemperature != Float.MIN_VALUE) {
			tourData.setAvgTemperature(weatherTemperature);
		}

		tourData.importRawDataFile = _importFilePath;
		tourData.setTourImportFilePath(_importFilePath);

		tourData.setDeviceTimeInterval((short) -1);

		if (_currentActivity.timeSlices.size() == 0) {

			// tour do not contain a track

			tourData.setTourDistance(_currentActivity.distance);

			tourData.setTourRecordingTime(_currentActivity.duration);
			tourData.setTourDrivingTime(_currentActivity.duration);
			isComputeDrivingTime = false;

			tourData.setTourAltUp(_currentActivity.elevationUp);
			tourData.setTourAltDown(_currentActivity.elevationDown);

		} else {

			// create 'normal' tour

			tourData.createTimeSeries(_currentActivity.timeSlices, false);
		}

		if (tourData.pulseSerie == null) {
			tourData.setAvgPulse(_currentActivity.avgPulse);
			tourData.setMaxPulse(_currentActivity.maxPulse);
		}

		if (tourData.cadenceSerie == null) {
			tourData.setAvgCadence(_currentActivity.avgCadence);
		}

		tourData.setDeviceId(_device.deviceId);
		tourData.setDeviceName(_device.visibleName);

		// after all data are added, the tour id can be created because it depends on the tour distance
		final String uniqueId = _device.createUniqueId(tourData, Util.UNIQUE_ID_SUFFIX_SPORT_TRACKS_FITLOG);
		final Long tourId = tourData.createTourId(uniqueId);

		// check if the tour is already imported
		if (_alreadyImportedTours.containsKey(tourId) == false) {

			// add new tour to other tours
			_newlyImportedTours.put(tourId, tourData);

			// create additional data
			if (isComputeDrivingTime) {
				tourData.computeTourDrivingTime();
			}

			tourData.computeAltitudeUpDown();
			tourData.computeComputedValues();

			finalizeTour_10_CreateMarkers(tourData);
		}

		// cleanup
		_currentActivity.timeSlices.clear();
		_currentActivity.laps.clear();
		_currentActivity.equipmentName.clear();

		_isImported = true;
	}


	private void finalizeTour_10_CreateMarkers(final TourData tourData) {

		final ArrayList<Lap> _laps = _currentActivity.laps;
		if (_laps.size() == 0) {
			return;
		}

		final int[] timeSerie = tourData.timeSerie;
		if (timeSerie == null || timeSerie.length == 0) {
			// fixed bug: http://sourceforge.net/support/tracker.php?aid=3232030
			return;
		}

		final Set<TourMarker> tourMarkers = tourData.getTourMarkers();
		final float[] distanceSerie = tourData.distanceSerie;

		/*
		 * tour and track can have different start times
		 */
		final long tourStartTime = _currentActivity.tourStartTimeMills;
//		final long tour2sliceTimeDiff = _currentActivity.trackTourStartTime - tourStartTime;

		int lapCounter = 1;

		for (final Lap lap : _laps) {

			final long startTimeDiff = lap.lapStartTime - tourStartTime;// - tour2sliceTimeDiff;
			long lapRelativeTime = startTimeDiff / 1000;
			int serieIndex = 0;

			// get serie index
			for (final int tourRelativeTime : timeSerie) {
				if (tourRelativeTime >= lapRelativeTime) {
					break;
				}
				serieIndex++;
			}

			if (lapRelativeTime < 0) {
				// this case occured
				lapRelativeTime = 0;
			}

			// check array bounds
			if (serieIndex >= timeSerie.length) {
				serieIndex = timeSerie.length - 1;
			}

			final TourMarker tourMarker = new TourMarker(tourData, ChartLabel.MARKER_TYPE_DEVICE);

			tourMarker.setLabel(Integer.toString(lapCounter));
			tourMarker.setSerieIndex(serieIndex);
			tourMarker.setTime((int) lapRelativeTime);
			tourMarker.setVisualPosition(ChartLabel.VISUAL_HORIZONTAL_ABOVE_GRAPH_CENTERED);

			if (distanceSerie != null) {
				tourMarker.setDistance(distanceSerie[serieIndex]);
			}

			tourMarkers.add(tourMarker);

			lapCounter++;
		}
	}

	private void initTour(final Attributes attributes) {

		_currentActivity = new Activity();

		_distanceAbsolute = 0;

		_prevLatitude = Double.MIN_VALUE;
		_prevLongitude = Double.MIN_VALUE;

//		final String startTime = attributes.getValue(ATTRIB_START_TIME);
//		if (startTime != null) {
//			final DateTime tourDateTime = _dtParser.parseDateTime(startTime);
//			_currentActivity.tourStartTime = tourDateTime;
//			_currentActivity.tourStartTimeMills = tourDateTime.getMillis();
//		}
	}

	public boolean isImported() {
		return _isImported;
	}

	public boolean isNewTag() {
		return _isNewTag;
	}

	public boolean isNewTourType() {
		return _isNewTourType;
	}

//	private void parseActivity01Start(final String name, final Attributes attributes) {
//
//		if (name.equals(TAG_ACTIVITY_NAME)) {
//
//			_isInName = true;
//
//		} else if (name.equals(TAG_ACTIVITY_NOTES)) {
//
//			_isInNotes = true;
//
//		} else if (name.equals(TAG_ACTIVITY_LOCATION)) {
//
//			_currentActivity.location = attributes.getValue(ATTRIB_NAME);
//
//		} else if (name.equals(TAG_ACTIVITY_CATEGORY)) {
//
//			_currentActivity.categoryName = attributes.getValue(ATTRIB_NAME);
//		} else if (name.equals(TAG_ACTIVITY_EQUIPMENT_ITEM)) {
//
//			_currentActivity.equipmentName.add(attributes.getValue(ATTRIB_NAME));
//
//		} else if (name.equals(TAG_ACTIVITY_CALORIES)) {
//
//			//		<xs:element name="Calories">
//			//			<xs:complexType>
//			//				<xs:attribute name="TotalCal" type="xs:decimal" use="optional"/>
//			//			</xs:complexType>
//			//		</xs:element>
//			_currentActivity.calories = Util.parseInt0(attributes, ATTRIB_TOTAL_CAL);
//
//		} else if (name.equals(TAG_ACTIVITY_DURATION)) {
//
//			//		<xs:element name="Duration">
//			//			<xs:complexType>
//			//				<xs:attribute name="TotalSeconds" type="xs:decimal" use="optional"/>
//			//			</xs:complexType>
//			//		</xs:element>
//			_currentActivity.duration = Util.parseInt0(attributes, ATTRIB_TOTAL_SECONDS);
//
//		} else if (name.equals(TAG_ACTIVITY_DISTANCE)) {
//
//			//		<xs:element name="Distance">
//			//			<xs:complexType>
//			//				<xs:attribute name="TotalMeters" type="xs:decimal" use="optional"/>
//			//			</xs:complexType>
//			//		</xs:element>
//			_currentActivity.distance = Util.parseInt0(attributes, ATTRIB_TOTAL_METERS);
//
//		} else if (name.equals(TAG_ACTIVITY_ELEVATION)) {
//
//			//		<xs:element name="Elevation">
//			//			<xs:complexType>
//			//				<xs:attribute name="DescendMeters" type="xs:decimal" use="optional"/>
//			//				<xs:attribute name="AscendMeters" type="xs:decimal" use="optional"/>
//			//			</xs:complexType>
//			//		</xs:element>
//			_currentActivity.elevationUp = Util.parseInt0(attributes, ATTRIB_ASCEND_METERS);
//			_currentActivity.elevationDown = Util.parseInt0(attributes, ATTRIB_DESCEND_METERS);
//
//		} else if (name.equals(TAG_ACTIVITY_HEART_RATE)) {
//
//			//		<xs:element name="HeartRate">
//			//			<xs:complexType>
//			//				<xs:attribute name="AverageBPM" type="xs:decimal" use="optional"/>
//			//				<xs:attribute name="MaximumBPM" type="xs:decimal" use="optional"/>
//			//			</xs:complexType>
//			//		</xs:element>
//			_currentActivity.avgPulse = Util.parseInt0(attributes, ATTRIB_AVERAGE_BPM);
//			_currentActivity.maxPulse = Util.parseInt0(attributes, ATTRIB_MAXIMUM_BPM);
//
//		} else if (name.equals(TAG_ACTIVITY_CADENCE)) {
//
//			//		<xs:element name="Cadence">
//			//			<xs:complexType>
//			//				<xs:attribute name="AverageRPM" type="xs:decimal" use="optional"/>
//			//				<xs:attribute name="MaximumRPM" type="xs:decimal" use="optional "/>
//			//			</xs:complexType>
//			//		</xs:element>
//			_currentActivity.avgCadence = Util.parseInt0(attributes, ATTRIB_AVERAGE_RPM);
////	!!! not yet supported !!!
////			_currentActivity.maxCadence = Util.parseInt0(attributes, ATTRIB_MAXIMUM_RPM);
//
//		} else if (name.equals(TAG_ACTIVITY_WEATHER)) {
//
//			_isInWeather = true;
//			_currentActivity.weatherTemperature = Util.parseFloat(attributes, ATTRIB_WEATHER_TEMP);
//			_currentActivity.weatherConditions = attributes.getValue(ATTRIB_WEATHER_CONDITIONS);
//
//		} else {
//			return;
//		}
//
//		_characters.delete(0, _characters.length());
//	}
//
//	private void parseActivity02End(final String name) {
//
//		if (_isInName) {
//
//			_isInName = false;
//			_currentActivity.name = _characters.toString();
//
//		} else if (_isInNotes) {
//
//			_isInNotes = false;
//			_currentActivity.notes = _characters.toString();
//
//		} else if (_isInWeather) {
//
//			_isInWeather = false;
//			_currentActivity.weatherText = _characters.toString();
//		}
//	}
//
//	private void parseLaps(final String name, final Attributes attributes) {
//
//		if (name.equals(TAG_LAP)) {
//
//			final String startTime = attributes.getValue(ATTRIB_START_TIME);
//
//			if (startTime != null) {
//
//				final Lap lap = new Lap();
//
//				lap.lapStartTime = _dtParser.parseDateTime(startTime).getMillis();
//
//				_currentActivity.laps.add(lap);
//			}
//		}
//	}
//
//	private void parseTrack(final Attributes attributes) {
//
//		final String startTime = attributes.getValue(ATTRIB_START_TIME);
//
//		if (startTime != null) {
//			_currentActivity.trackTourDateTime = _dtParser.parseDateTime(startTime);
//			_currentActivity.trackTourStartTime = _currentActivity.trackTourDateTime.getMillis();
//		}
//	}

//	private void parseTrackPoints(final String name, final Attributes attributes) throws InvalidDeviceSAXException {
//
//		if (name.equals(TAG_TRACK_PT)) {
//
//			if (_currentActivity.tourStartTimeMills == Long.MIN_VALUE) {
//				throw new InvalidDeviceSAXException(NLS.bind(Messages.FitLog_Error_InvalidStartTime, _importFilePath));
//			}
//
//			final TimeData timeSlice = new TimeData();
//
//			// relative time in seconds
//			final long tmValue = Util.parseLong(attributes, ATTRIB_PT_TM);
//			if (tmValue != Long.MIN_VALUE) {
//				timeSlice.absoluteTime = _currentActivity.tourStartTimeMills + (tmValue * 1000);
//			}
//
//			final double tpDistance = Util.parseDouble(attributes, ATTRIB_PT_DIST);
//			final double latitude = Util.parseDouble(attributes, ATTRIB_PT_LAT);
//			final double longitude = Util.parseDouble(attributes, ATTRIB_PT_LON);
//
//			if (tpDistance != Double.MIN_VALUE) {
//				_distanceAbsolute = tpDistance;
//			} else if (tpDistance == Double.MIN_VALUE
//					&& latitude != Double.MIN_VALUE
//					&& longitude != Double.MIN_VALUE
//					&& _prevLatitude != Double.MIN_VALUE
//					&& _prevLongitude != Double.MIN_VALUE) {
//
//				// get distance from lat/lon when it's not set
//				_distanceAbsolute += MtMath.distanceVincenty(_prevLatitude, _prevLongitude, latitude, longitude);
//			}
//
//			if (latitude != Double.MIN_VALUE && longitude != Double.MIN_VALUE) {
//				_prevLatitude = latitude;
//				_prevLongitude = longitude;
//			}
//
//			timeSlice.absoluteDistance = (float) _distanceAbsolute;
//			timeSlice.absoluteAltitude = Util.parseFloat(attributes, ATTRIB_PT_ELE);
//			timeSlice.cadence = Util.parseFloat(attributes, ATTRIB_PT_CADENCE);
//			timeSlice.pulse = Util.parseFloat(attributes, ATTRIB_PT_HR);
//			timeSlice.power = Util.parseFloat(attributes, ATTRIB_PT_POWER);
//			timeSlice.latitude = latitude;
//			timeSlice.longitude = longitude;
//
//			_currentActivity.timeSlices.add(timeSlice);
//		}
//	}

	@Override
	public void startElement(final String uri, final String localName, final String name, final Attributes attributes)
			throws SAXException {

		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") + ("\tname: " + name));
		// TODO remove SYSTEM.OUT.PRINTLN


//		if (_isInActivity) {
//
//			if (_isInTrack) {
//				parseTrackPoints(name, attributes);
//			} else if (_isInLaps) {
//				parseLaps(name, attributes);
//			} else {
//				parseActivity01Start(name, attributes);
//			}
//		}
//
//		if (name.equals(TAG_TRACK)) {
//
//			_isInTrack = true;
//
////			parseTrack(attributes);
//
//		} else if (name.equals(TAG_LAPS)) {
//
//			_isInLaps = true;
//
//		} else if (name.equals(TAG_ACTIVITY)) {
//
//			/*
//			 * a new exercise/tour starts
//			 */
//
//			_isInActivity = true;
//
//			initTour(attributes);
//		}
	}

}
