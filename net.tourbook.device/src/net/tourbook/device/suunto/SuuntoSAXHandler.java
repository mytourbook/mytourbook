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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimeZone;

import net.tourbook.common.util.Util;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SuuntoSAXHandler extends DefaultHandler {

	private static final double				RADIANT_TO_DEGREE		= 57.2957795131;

	/**
	 * This time is used when a time is not available.
	 */
	private static final long				DEFAULT_TIME			= new DateTime(2007, 4, 1, 0, 0, 0, 0).getMillis();

	private static final DateTimeFormatter	_dtParser				= ISODateTimeFormat.dateTimeParser();

	private static final SimpleDateFormat	TIME_FORMAT				= new SimpleDateFormat(//
																			"yyyy-MM-dd'T'HH:mm:ss'Z'");				//$NON-NLS-1$
	private static final SimpleDateFormat	TIME_FORMAT_SSSZ		= new SimpleDateFormat(//
																			"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");			//$NON-NLS-1$
	private static final SimpleDateFormat	TIME_FORMAT_RFC822		= new SimpleDateFormat(//
																			"yyyy-MM-dd'T'HH:mm:ssZ");					//$NON-NLS-1$

	private static final String				SAMPLE_TYPE_GPS_BASE	= "gps-base";
	private static final String				SAMPLE_TYPE_GPS_TINY	= "gps-tiny";
	private static final String				SAMPLE_TYPE_GPS_SMALL	= "gps-small";
	private static final String				SAMPLE_TYPE_PERIODIC	= "periodic";

	private static final String				TAG_ALTITUDE			= "Altitude";
	private static final String				TAG_DISTANCE			= "Distance";
	private static final String				TAG_EVENTS				= "Events";
	private static final String				TAG_IBI					= "IBI";
	private static final String				TAG_LATITUDE			= "Latitude";
	private static final String				TAG_LONGITUDE			= "Longitude";
	private static final String				TAG_SAMPLE				= "Sample";
	private static final String				TAG_SAMPLES				= "Samples";
	private static final String				TAG_SAMPLE_TYPE			= "SampleType";
	private static final String				TAG_TEMPERATURE			= "Temperature";
	private static final String				TAG_UTC					= "UTC";

	//
	private HashMap<Long, TourData>			_alreadyImportedTours;
	private HashMap<Long, TourData>			_newlyImportedTours;
	private TourbookDevice					_device;
	private String							_importFilePath;
	//
	private TimeData						_sampleData;
	private ArrayList<TimeData>				_sampleList				= new ArrayList<TimeData>();

	private TimeData						_gpsData;
	private ArrayList<TimeData>				_gpsList				= new ArrayList<TimeData>();

	private boolean							_isImported;

	private StringBuilder					_characters				= new StringBuilder();
	private String							_currentSampleType;
	private long							_currentTime;
	private TourData						_currentTourData;

	private boolean							_isInAltitude;
	private boolean							_isInDistance;
	private boolean							_isInEvents;
	private boolean							_isInIBI;
	private boolean							_isInLatitude;
	private boolean							_isInLongitude;
	private boolean							_isInSample;
	private boolean							_isInSamples;
	private boolean							_isInSampleType;
	private boolean							_isInUTC;
	private boolean							_isInTemperature;

	private int								_sampleCounter;

	static {

		final TimeZone utc = TimeZone.getTimeZone("UTC");

		TIME_FORMAT.setTimeZone(utc);
		TIME_FORMAT_SSSZ.setTimeZone(utc);
		TIME_FORMAT_RFC822.setTimeZone(utc);
	}

	public SuuntoSAXHandler(final TourbookDevice deviceDataReader,
							final String importFileName,
							final HashMap<Long, TourData> alreadyImportedTours,
							final HashMap<Long, TourData> newlyImportedTours) {

		_device = deviceDataReader;
		_importFilePath = importFileName;
		_alreadyImportedTours = alreadyImportedTours;
		_newlyImportedTours = newlyImportedTours;
	}

	@Override
	public void characters(final char[] chars, final int startIndex, final int length) throws SAXException {

		if (_isInSampleType //
				|| _isInAltitude
				|| _isInDistance
				|| _isInIBI
				|| _isInLatitude
				|| _isInLongitude
				|| _isInTemperature
				|| _isInUTC
		//
		) {
			_characters.append(chars, startIndex, length);
		}
	}

	public void dispose() {

		_sampleList.clear();
		_gpsList.clear();
	}

	@Override
	public void endElement(final String uri, final String localName, final String name) throws SAXException {

		if (_isInSample) {

			endElement_Sample(name);

		} else if (_isInIBI) {

			endElement_IBI(name);
		}

		if (name.equals(TAG_SAMPLE)) {

			_isInSample = false;

			finalizeSample();

		} else if (name.equals(TAG_SAMPLES)) {

			_isInSamples = false;

			finalizeTour();
		}
	}

	private void endElement_IBI(final String name) {

		if (name.equals(TAG_IBI) == false) {
			return;
		}

		_isInIBI = false;

		final String ibi = _characters.toString();

		final String[] allIBI = ibi.split(" ");

//		System.out.println("\t" + TAG_IBI + "\t" + allIBI.length);
	}

	private void endElement_Sample(final String name) {

		if (name.equals(TAG_SAMPLE_TYPE)) {

			_isInSampleType = false;

			_currentSampleType = _characters.toString();

//			System.out.println("\t" + TAG_SAMPLE_TYPE + "\t" + _currentSampleType);

		} else if (name.equals(TAG_ALTITUDE)) {

			_isInAltitude = false;

			_sampleData.absoluteAltitude = Util.parseFloat(_characters.toString());

//			System.out.println("\t" + TAG_ALTITUDE + "\t" + _timeData.absoluteAltitude);

		} else if (name.equals(TAG_DISTANCE)) {

			_isInDistance = false;

			if (_isInEvents) {

				// ignore this value because <Distance> can also occure within <Events>

			} else {

				_sampleData.absoluteDistance = Util.parseFloat(_characters.toString());

//				System.out.println("\t" + TAG_DISTANCE + "\t" + _timeData.absoluteDistance);
			}

		} else if (name.equals(TAG_EVENTS)) {

			_isInEvents = false;

//			System.out.println("\t" + TAG_EVENTS);

		} else if (name.equals(TAG_LATITUDE)) {

			_isInLatitude = false;

			_gpsData.latitude = Util.parseDouble(_characters.toString()) * RADIANT_TO_DEGREE;

//			System.out.println("\t" + TAG_LATITUDE + "\t" + _timeData.latitude);

		} else if (name.equals(TAG_LONGITUDE)) {

			_isInLongitude = false;

			_gpsData.longitude = Util.parseDouble(_characters.toString()) * RADIANT_TO_DEGREE;

//			System.out.println("\t" + TAG_LONGITUDE + "\t" + _timeData.longitude);

		} else if (name.equals(TAG_TEMPERATURE)) {

			_isInTemperature = false;

			final float kelvin = Util.parseFloat(_characters.toString());
			_sampleData.temperature = kelvin - 273.15f;

//			System.out.println("\t" + TAG_TEMPERATURE + "\t" + _timeData.temperature);

		} else if (name.equals(TAG_UTC)) {

			_isInUTC = false;

			final String timeString = _characters.toString();

			try {
				_currentTime = _dtParser.parseDateTime(timeString).getMillis();
			} catch (final Exception e0) {
				try {
					_currentTime = TIME_FORMAT.parse(timeString).getTime();
				} catch (final ParseException e1) {
					try {
						_currentTime = TIME_FORMAT_SSSZ.parse(timeString).getTime();
					} catch (final ParseException e2) {
						try {
							_currentTime = TIME_FORMAT_RFC822.parse(timeString).getTime();
						} catch (final ParseException e3) {

							Display.getDefault().syncExec(new Runnable() {
								public void run() {
									final String message = e3.getMessage();
									MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error", message); //$NON-NLS-1$
									System.err.println(message + " in " + _importFilePath); //$NON-NLS-1$
								}
							});
						}
					}
				}
			}

//			System.out.println("\t" + TAG_UTC + "\t\t\t" + new DateTime(_currentTime));
			// TODO remove SYSTEM.OUT.PRINTLN
		}
	}

	private void finalizeSample() {

		if (_currentSampleType != null) {

			if (_currentSampleType.equals(SAMPLE_TYPE_PERIODIC)) {

//				if (_sampleData != null) {
//				}

				// set virtual time if time is not available
				_sampleData.absoluteTime = _currentTime == Long.MIN_VALUE ? DEFAULT_TIME : _currentTime;

				_sampleList.add(_sampleData);

			} else if (_currentSampleType.equals(SAMPLE_TYPE_GPS_BASE)
					|| _currentSampleType.equals(SAMPLE_TYPE_GPS_SMALL)
					|| _currentSampleType.equals(SAMPLE_TYPE_GPS_TINY)) {

				// set virtual time if time is not available
				_gpsData.absoluteTime = _currentTime == Long.MIN_VALUE ? DEFAULT_TIME : _currentTime;

				_gpsList.add(_gpsData);
			}
		}

//		System.out.println("^^^ " + _sampleCounter++ + "\n");
//		// TODO remove SYSTEM.OUT.PRINTLN

		_sampleData = null;
		_gpsData = null;
		_currentSampleType = null;
		_currentTime = Long.MIN_VALUE;
	}

	private void finalizeTour() {

		// check if data are available
		if (_sampleList.size() == 0) {
			return;
		}

		setGPSSerie();

		// create data object for each tour
		final TourData tourData = new TourData();

		/*
		 * set tour start date/time
		 */
		tourData.setTourStartTime(new DateTime(_sampleList.get(0).absoluteTime));

//		tourData.setIsDistanceFromSensor(_isDistanceFromSensor);
		tourData.setDeviceTimeInterval((short) -1);
		tourData.importRawDataFile = _importFilePath;
		tourData.setTourImportFilePath(_importFilePath);

//		tourData.setDeviceModeName(_activitySport);
//		tourData.setCalories(_tourCalories);
//
//		final String deviceName = _sport.creatorName;
//		final String majorVersion = _sport.creatorVersionMajor;
//		final String minorVersion = _sport.creatorVersionMinor;

		tourData.setDeviceId(_device.deviceId);

//		tourData.setDeviceName(_device.visibleName + (deviceName == null //
//				? UI.EMPTY_STRING
//				: UI.SPACE + deviceName));
//
//		tourData.setDeviceFirmwareVersion(majorVersion == null //
//				? UI.EMPTY_STRING
//				: majorVersion + (minorVersion == null //
//						? UI.EMPTY_STRING
//						: UI.SYMBOL_DOT + minorVersion));

		tourData.createTimeSeries(_sampleList, true);

		setDistanceSerie(tourData);

		// after all data are added, the tour id can be created
		final String uniqueId = _device.createUniqueId(tourData, Util.UNIQUE_ID_SUFFIX_GARMIN_TCX);
		final Long tourId = tourData.createTourId(uniqueId);

		// check if the tour is already imported
		if (_alreadyImportedTours.containsKey(tourId) == false) {

			// add new tour to other tours
			_newlyImportedTours.put(tourId, tourData);

			// create additional data
			tourData.computeAltitudeUpDown();
			tourData.computeTourDrivingTime();
			tourData.computeComputedValues();

			_currentTourData = tourData;
		}

		_isImported = true;
	}

	/**
	 * @return Returns <code>true</code> when a tour was imported
	 */
	public boolean isImported() {
		return _isImported;
	}

	/**
	 * Check if distance values are not changed when geo position changed, when <code>true</code>
	 * compute distance values from geo position.
	 * 
	 * @param tourData
	 */
	private void setDistanceSerie(final TourData tourData) {

		/*
		 * There are currently no data available to check if distance is changing, current data keep
		 * distance for some slices and then jumps to the next value.
		 */
		TourManager.computeDistanceValuesFromGeoPosition(tourData);
	}

	/**
	 * Merge by time GPS data tour data.
	 */
	private void setGPSSerie() {

		if (_gpsList.size() == 0) {
			return;
		}

		final int gpsSize = _gpsList.size();

		TimeData nextGPSData = _gpsList.get(0);
		TimeData prevGPSData = nextGPSData;

		long nextGpsTime = nextGPSData.absoluteTime;
		long prevGpsTime = nextGpsTime;

		int gpsIndex = 0;

		for (final TimeData sampleData : _sampleList) {

			final long sampleTime = sampleData.absoluteTime;

			while (true) {

				if (sampleTime > nextGpsTime) {

					gpsIndex++;

					if (gpsIndex < gpsSize) {

						prevGpsTime = nextGpsTime;
						prevGPSData = nextGPSData;

						nextGPSData = _gpsList.get(gpsIndex);
						nextGpsTime = nextGPSData.absoluteTime;
					} else {
						break;
					}
				} else {
					break;
				}
			}

			if (sampleTime == prevGpsTime) {

				sampleData.latitude = prevGPSData.latitude;
				sampleData.longitude = prevGPSData.longitude;

			} else if (sampleTime == nextGpsTime) {

				sampleData.latitude = nextGPSData.latitude;
				sampleData.longitude = nextGPSData.longitude;

			} else {

				// interpolate position

				final double gpsTimeDiff = nextGpsTime - prevGpsTime;
				final double sampleDiff = sampleTime - prevGpsTime;

				final double sampleRatio = gpsTimeDiff == 0 ? 0 : sampleDiff / gpsTimeDiff;

				final double latDiff = nextGPSData.latitude - prevGPSData.latitude;
				final double lonDiff = nextGPSData.longitude - prevGPSData.longitude;

				sampleData.latitude = prevGPSData.latitude + latDiff * sampleRatio;
				sampleData.longitude = prevGPSData.longitude + lonDiff * sampleRatio;
			}
		}
	}

	@Override
	public void startElement(final String uri, final String localName, final String name, final Attributes attributes)
			throws SAXException {

		if (_isInSamples) {

			if (_isInSample) {

				startElement_InSample(name);

			} else if (name.equals(TAG_SAMPLE)) {

				_isInSample = true;

				// create new time item, the sampleType defines which time data are used
				_gpsData = new TimeData();
				_sampleData = new TimeData();
			}

		} else if (name.equals(TAG_SAMPLES)) {

			_isInSamples = true;

		} else if (name.equals(TAG_IBI)) {

			_isInIBI = true;
		}

	}

	private void startElement_InSample(final String name) {

		boolean isData = false;

		if (name.equals(TAG_SAMPLE_TYPE)) {

			isData = true;
			_isInSampleType = true;

		} else if (name.equals(TAG_ALTITUDE)) {

			isData = true;
			_isInAltitude = true;

		} else if (name.equals(TAG_DISTANCE)) {

			isData = true;
			_isInDistance = true;

		} else if (name.equals(TAG_EVENTS)) {

			isData = true;
			_isInEvents = true;

		} else if (name.equals(TAG_UTC)) {

			isData = true;
			_isInUTC = true;

		} else if (name.equals(TAG_LATITUDE)) {

			isData = true;
			_isInLatitude = true;

		} else if (name.equals(TAG_LONGITUDE)) {

			isData = true;
			_isInLongitude = true;

		} else if (name.equals(TAG_TEMPERATURE)) {

			isData = true;
			_isInTemperature = true;
		}

		if (isData) {

			// clear char buffer
			_characters.delete(0, _characters.length());
		}
	}
}
