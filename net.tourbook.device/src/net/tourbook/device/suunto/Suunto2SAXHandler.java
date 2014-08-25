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

import net.tourbook.common.UI;
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

/**
 * This Suunto importer is implemented with info from
 * <p>
 * <a href="http://wiki.oldhu.com/doku.php?id=suunto_moveslink2_xml_file_format"
 * >http://wiki.oldhu.com/doku.php?id=suunto_moveslink2_xml_file_format</a>
 */
public class Suunto2SAXHandler extends DefaultHandler {

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

	private static final String				SAMPLE_TYPE_GPS_BASE	= "gps-base";										//$NON-NLS-1$
	private static final String				SAMPLE_TYPE_GPS_TINY	= "gps-tiny";										//$NON-NLS-1$
	private static final String				SAMPLE_TYPE_GPS_SMALL	= "gps-small";										//$NON-NLS-1$
	private static final String				SAMPLE_TYPE_PERIODIC	= "periodic";										//$NON-NLS-1$

	// root tags
	private static final String				TAG_ROOT_DEVICE			= "Device";										//$NON-NLS-1$
	private static final String				TAG_ROOT_HEADER			= "header";										//$NON-NLS-1$
	private static final String				TAG_ROOT_SAMPLES		= "Samples";										//$NON-NLS-1$

	// header tags
	private static final String				TAG_ENERGY				= "Energy";										//$NON-NLS-1$

	// device tags
	private static final String				TAG_SW					= "SW";											//$NON-NLS-1$

	// sample tags
	private static final String				TAG_ALTITUDE			= "Altitude";										//$NON-NLS-1$
	private static final String				TAG_CADENCE				= "Cadence";										//$NON-NLS-1$
	private static final String				TAG_DISTANCE			= "Distance";										//$NON-NLS-1$
	private static final String				TAG_EVENTS				= "Events";										//$NON-NLS-1$
	private static final String				TAG_HR					= "HR";											//$NON-NLS-1$
	private static final String				TAG_LAP					= "Lap";											//$NON-NLS-1$
	private static final String				TAG_LATITUDE			= "Latitude";										//$NON-NLS-1$
	private static final String				TAG_LONGITUDE			= "Longitude";										//$NON-NLS-1$
	private static final String				TAG_SAMPLE				= "Sample";										//$NON-NLS-1$
	private static final String				TAG_SAMPLE_TYPE			= "SampleType";									//$NON-NLS-1$
	private static final String				TAG_TEMPERATURE			= "Temperature";									//$NON-NLS-1$
	private static final String				TAG_UTC					= "UTC";											//$NON-NLS-1$

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

	private TimeData						_markerData;
	private ArrayList<TimeData>				_markerList				= new ArrayList<TimeData>();

	private boolean							_isImported;

	private StringBuilder					_characters				= new StringBuilder();
	private String							_currentSampleType;
	private long							_currentTime;
	private long							_prevSampleTime;

	private boolean							_isInRootDevice;
	private boolean							_isInRootSamples;
	private boolean							_isInRootHeader;

	private boolean							_isInAltitude;
	private boolean							_isInCadence;
	private boolean							_isInDistance;
	private boolean							_isInEnergy;
	private boolean							_isInEvents;
	private boolean							_isInHR;
	private boolean							_isInLatitude;
	private boolean							_isInLongitude;
	private boolean							_isInSample;
	private boolean							_isInSampleType;
	private boolean							_isInSW;
	private boolean							_isInUTC;
	private boolean							_isInTemperature;

	private int								_tourCalories;
	private String							_tourSW;

	static {

		final TimeZone utc = TimeZone.getTimeZone("UTC"); //$NON-NLS-1$

		TIME_FORMAT.setTimeZone(utc);
		TIME_FORMAT_SSSZ.setTimeZone(utc);
		TIME_FORMAT_RFC822.setTimeZone(utc);
	}

	public Suunto2SAXHandler(final TourbookDevice deviceDataReader,
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
				|| _isInCadence
				|| _isInDistance
				|| _isInEnergy
				|| _isInHR
				|| _isInLatitude
				|| _isInLongitude
				|| _isInSW
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
		_markerList.clear();
	}

	@Override
	public void endElement(final String uri, final String localName, final String name) throws SAXException {

		if (_isInRootSamples) {

			endElement_InSamples(name);

		} else if (_isInRootHeader) {

			endElement_InHeader(name);

		} else if (_isInRootDevice) {

			endElement_InDevice(name);
		}

		if (name.equals(TAG_SAMPLE)) {

			_isInSample = false;

			finalizeSample();

		} else if (name.equals(TAG_ROOT_SAMPLES)) {

			_isInRootSamples = false;

		} else if (name.equals(TAG_ROOT_HEADER)) {

			_isInRootHeader = false;

		} else if (name.equals(TAG_ROOT_DEVICE)) {

			_isInRootDevice = false;

		} else if (name.equals(Suunto2DeviceDataReader.TAG_SUUNTO)) {

			finalizeTour();
		}
	}

	private void endElement_InDevice(final String name) {

		if (name.equals(TAG_SW)) {

			_isInSW = false;

			_tourSW = _characters.toString();
		}
	}

	private void endElement_InHeader(final String name) {

		if (name.equals(TAG_ENERGY)) {

			_isInEnergy = false;

			_tourCalories = (int) (Util.parseFloat(_characters.toString()) / 4184);
		}
	}

	private void endElement_InSamples(final String name) {

		if (name.equals(TAG_SAMPLE_TYPE)) {

			_isInSampleType = false;

			_currentSampleType = _characters.toString();

		} else if (name.equals(TAG_ALTITUDE)) {

			_isInAltitude = false;

			_sampleData.absoluteAltitude = Util.parseFloat(_characters.toString());

		} else if (name.equals(TAG_CADENCE)) {

			_isInCadence = false;

			_sampleData.cadence = Util.parseFloat(_characters.toString()) * 60.0f;

		} else if (name.equals(TAG_DISTANCE)) {

			_isInDistance = false;

			if (_isInEvents) {

				// ignore this value because <Distance> can also occure within <Events>

			} else {

				_sampleData.absoluteDistance = Util.parseFloat(_characters.toString());
			}

		} else if (name.equals(TAG_EVENTS)) {

			_isInEvents = false;

		} else if (name.equals(TAG_HR)) {

			_isInHR = false;

			// HR * 60 = bpm
			final float hr = Util.parseFloat(_characters.toString());
			_sampleData.pulse = hr * 60.0f;

		} else if (name.equals(TAG_LAP)) {

			// set a marker
			_markerData.marker = 1;

		} else if (name.equals(TAG_LATITUDE)) {

			_isInLatitude = false;

			_gpsData.latitude = Util.parseDouble(_characters.toString()) * RADIANT_TO_DEGREE;

		} else if (name.equals(TAG_LONGITUDE)) {

			_isInLongitude = false;

			_gpsData.longitude = Util.parseDouble(_characters.toString()) * RADIANT_TO_DEGREE;

		} else if (name.equals(TAG_TEMPERATURE)) {

			_isInTemperature = false;

			final float kelvin = Util.parseFloat(_characters.toString());
			_sampleData.temperature = kelvin - 273.15f;

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
		}
	}

	private void finalizeSample() {

		final long sampleTime;
		if (_currentTime == Long.MIN_VALUE) {

			sampleTime = DEFAULT_TIME;

		} else {

			/*
			 * Remove milliseconds because this can cause wrong data. Position of a marker can be at
			 * the wrong second and multiple samples can have the same second but other
			 * milliseconds.
			 */
			sampleTime = _currentTime / 1000 * 1000;
		}

		/*
		 * A lap do not contain a sample type
		 */
		if (_markerData.marker == 1) {

			// set virtual time if time is not available
			_markerData.absoluteTime = sampleTime;

			_markerList.add(_markerData);

		} else {

			if (_currentSampleType != null) {

				if (_currentSampleType.equals(SAMPLE_TYPE_PERIODIC)) {

					/*
					 * Skip samples with the same time in seconds
					 */
					boolean isSkipSample = false;

					if (_currentTime != Long.MIN_VALUE //
							&& _prevSampleTime != Long.MIN_VALUE
							&& sampleTime == _prevSampleTime) {

						isSkipSample = true;
					}

					if (isSkipSample == false) {

						// set virtual time if time is not available
						_sampleData.absoluteTime = sampleTime;

						_sampleList.add(_sampleData);

						_prevSampleTime = sampleTime;
					}

				} else if (_currentSampleType.equals(SAMPLE_TYPE_GPS_BASE)
						|| _currentSampleType.equals(SAMPLE_TYPE_GPS_SMALL)
						|| _currentSampleType.equals(SAMPLE_TYPE_GPS_TINY)) {

					// set virtual time if time is not available
					_gpsData.absoluteTime = sampleTime;

					_gpsList.add(_gpsData);
				}
			}
		}

		_sampleData = null;
		_gpsData = null;
		_markerData = null;

		_currentTime = Long.MIN_VALUE;
		_currentSampleType = null;
	}

	private void finalizeTour() {

		// check if data are available
		if (_sampleList.size() == 0) {
			return;
		}

		setData_GPS();
		setData_Marker();

		// create data object for each tour
		final TourData tourData = new TourData();

		/*
		 * set tour start date/time
		 */
		tourData.setTourStartTime(new DateTime(_sampleList.get(0).absoluteTime));

		tourData.setDeviceTimeInterval((short) -1);
		tourData.importRawDataFile = _importFilePath;
		tourData.setTourImportFilePath(_importFilePath);

		tourData.setCalories(_tourCalories);

		tourData.setDeviceId(_device.deviceId);
		tourData.setDeviceName(_device.visibleName);
		tourData.setDeviceFirmwareVersion(_tourSW == null ? UI.EMPTY_STRING : _tourSW);

		tourData.createTimeSeries(_sampleList, true);

		setDistanceSerie(tourData);

		// after all data are added, the tour id can be created
		final String uniqueId = _device.createUniqueId(tourData, Util.UNIQUE_ID_SUFFIX_SUUNTO2);
		final Long tourId = tourData.createTourId(uniqueId);

		// check if the tour is already imported
		if (_alreadyImportedTours.containsKey(tourId) == false) {

			// add new tour to other tours
			_newlyImportedTours.put(tourId, tourData);

			// create additional data
			tourData.computeAltitudeUpDown();
			tourData.computeTourDrivingTime();
			tourData.computeComputedValues();
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
	 * Merge GPS data into tour data by time.
	 * <p>
	 * Merge is necessary because there are separate time slices for GPS data and not every 'normal'
	 * time slice has it's own GPS time slice.
	 */
	private void setData_GPS() {

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

	/**
	 * Merge Marker data into tour data by time.
	 * <p>
	 * Merge is necessary because there are separate time slices for markers.
	 */
	private void setData_Marker() {

		final int markerSize = _markerList.size();

		if (markerSize == 0) {
			return;
		}

		int markerIndex = 0;

		long markerTime = _markerList.get(markerIndex).absoluteTime;

		for (final TimeData sampleData : _sampleList) {

			final long sampleTime = sampleData.absoluteTime;

			if (sampleTime < markerTime) {

				continue;

			} else {

				// markerTime >= sampleTime

				sampleData.marker = 1;
				sampleData.markerLabel = Integer.toString(markerIndex + 1);

				/*
				 * check if another marker is available
				 */
				markerIndex++;

				if (markerIndex >= markerSize) {
					break;
				}

				markerTime = _markerList.get(markerIndex).absoluteTime;
			}
		}
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

	@Override
	public void startElement(final String uri, final String localName, final String name, final Attributes attributes)
			throws SAXException {

		if (_isInRootSamples) {

			if (_isInSample) {

				startElement_InSample(name);

			} else if (name.equals(TAG_SAMPLE)) {

				_isInSample = true;

				// create new time items, "sampleType" defines which time data are used
				_gpsData = new TimeData();
				_markerData = new TimeData();
				_sampleData = new TimeData();
			}

		} else if (_isInRootHeader) {

			startElement_InHeader(name);

		} else if (_isInRootDevice) {

			startElement_InDevice(name);

		} else if (name.equals(TAG_ROOT_SAMPLES)) {

			_isInRootSamples = true;

		} else if (name.equals(TAG_ROOT_HEADER)) {

			_isInRootHeader = true;

		} else if (name.equals(TAG_ROOT_DEVICE)) {

			_isInRootDevice = true;
		}

	}

	private void startElement_InDevice(final String name) {

		boolean isData = false;

		if (name.equals(TAG_SW)) {

			isData = true;
			_isInSW = true;
		}

		if (isData) {

			// clear char buffer
			_characters.delete(0, _characters.length());
		}
	}

	private void startElement_InHeader(final String name) {

		boolean isData = false;

		if (name.equals(TAG_ENERGY)) {

			isData = true;
			_isInEnergy = true;
		}

		if (isData) {

			// clear char buffer
			_characters.delete(0, _characters.length());
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

		} else if (name.equals(TAG_CADENCE)) {

			isData = true;
			_isInCadence = true;

		} else if (name.equals(TAG_DISTANCE)) {

			isData = true;
			_isInDistance = true;

		} else if (name.equals(TAG_EVENTS)) {

			isData = true;
			_isInEvents = true;

		} else if (name.equals(TAG_HR)) {

			isData = true;
			_isInHR = true;

		} else if (name.equals(TAG_LATITUDE)) {

			isData = true;
			_isInLatitude = true;

		} else if (name.equals(TAG_LONGITUDE)) {

			isData = true;
			_isInLongitude = true;

		} else if (name.equals(TAG_TEMPERATURE)) {

			isData = true;
			_isInTemperature = true;

		} else if (name.equals(TAG_UTC)) {

			isData = true;
			_isInUTC = true;
		}

		if (isData) {

			// clear char buffer
			_characters.delete(0, _characters.length());
		}
	}
}
