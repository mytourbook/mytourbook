/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimeZone;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.Util;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.tour.TourManager;

/**
 * This Suunto importer is implemented with info from
 * <p>
 * <a href="http://wiki.oldhu.com/doku.php?id=suunto_moveslink2_xml_file_format"
 * >http://wiki.oldhu.com/doku.php?id=suunto_moveslink2_xml_file_format</a>
 */
public class Suunto3SAXHandler extends DefaultHandler {

	private static final double				RADIANT_TO_DEGREE			= 57.2957795131;

	private static final SimpleDateFormat	TIME_FORMAT					= new SimpleDateFormat(		//
			"yyyy-MM-dd'T'HH:mm:ss'Z'");																			//$NON-NLS-1$
	private static final SimpleDateFormat	TIME_FORMAT_SSSZ			= new SimpleDateFormat(		//
			"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");																		//$NON-NLS-1$
	private static final SimpleDateFormat	TIME_FORMAT_RFC822		= new SimpleDateFormat(		//
			"yyyy-MM-dd'T'HH:mm:ssZ");                                                          //$NON-NLS-1$
	private static final SimpleDateFormat	TIME_FORMAT_LOCAL			= new SimpleDateFormat(		//
			"yyyy-MM-dd'T'HH:mm:ss");																				//$NON-NLS-1$

	private static final String				SAMPLE_TYPE_GPS_BASE		= "gps-base";					//$NON-NLS-1$
	private static final String				SAMPLE_TYPE_GPS_TINY		= "gps-tiny";					//$NON-NLS-1$
	private static final String				SAMPLE_TYPE_GPS_SMALL	= "gps-small";					//$NON-NLS-1$
	private static final String				SAMPLE_TYPE_PERIODIC		= "periodic";					//$NON-NLS-1$

	// root tags
	private static final String	TAG_DEVLOG				= "DeviceLog";	//$NON-NLS-1$
	private static final String	TAG_DEVLOG_DEVICE		= "Device";		//$NON-NLS-1$
	private static final String	TAG_DEVLOG_HEADER		= "Header";		//$NON-NLS-1$
	private static final String	TAG_DEVLOG_SAMPLES	= "Samples";	//$NON-NLS-1$

	// header tags
	private static final String	TAG_ENERGY		= "Energy";		//$NON-NLS-1$
	private static final String	TAG_DATETIME	= "DateTime";	//$NON-NLS-1$

	// device tags
	private static final String	TAG_DEVICE_SW		= "SW";		//$NON-NLS-1$
	private static final String	TAG_DEVICE_NAME	= "Name";	//$NON-NLS-1$

	// sample tags
	private static final String	TAG_ALTITUDE		= "Altitude";		//$NON-NLS-1$
	private static final String	TAG_CADENCE			= "Cadence";		//$NON-NLS-1$
	private static final String	TAG_DISTANCE		= "Distance";		//$NON-NLS-1$
	private static final String	TAG_EVENTS			= "Events";			//$NON-NLS-1$
	private static final String	TAG_HR				= "HR";				//$NON-NLS-1$
	private static final String	TAG_LAP				= "Lap";				//$NON-NLS-1$
	private static final String	TAG_LATITUDE		= "Latitude";		//$NON-NLS-1$
	private static final String	TAG_LONGITUDE		= "Longitude";		//$NON-NLS-1$
	private static final String	TAG_SAMPLE			= "Sample";			//$NON-NLS-1$
	private static final String	TAG_SAMPLE_TYPE	= "SampleType";	//$NON-NLS-1$
	private static final String	TAG_TEMPERATURE	= "Temperature";	//$NON-NLS-1$
	private static final String	TAG_UTC				= "UTC";				//$NON-NLS-1$
	private static final String	TAG_TIME				= "Time";			//$NON-NLS-1$

	//
	private HashMap<Long, TourData>	_alreadyImportedTours;
	private HashMap<Long, TourData>	_newlyImportedTours;
	private TourbookDevice				_device;
	private String							_importFilePath;
	//
	private TimeData						_sampleData;
	private ArrayList<TimeData>		_sampleList	= new ArrayList<TimeData>();

	private TimeData						_gpsData;
	private ArrayList<TimeData>		_gpsList		= new ArrayList<TimeData>();

	private TimeData						_markerData;
	private ArrayList<TimeData>		_markerList	= new ArrayList<TimeData>();

	private boolean						_isImported;

	private StringBuilder				_characters	= new StringBuilder();
	private String							_currentSampleType;
	private long							_currentUtcTime;
	private long							_currentTime;
	private long							_prevSampleTime;

	private boolean						_isInDevice;
	private boolean						_isInDeviceSW;
	private boolean						_isInDeviceName;

	private boolean						_isInSamples;
	private boolean						_isInHeader;

	private boolean						_isInAltitude;
	private boolean						_isInCadence;
	private boolean						_isInDistance;
	private boolean						_isInEnergy;
	private boolean						_isInDateTime;
	private boolean						_isInEvents;
	private boolean						_isInHR;
	private boolean						_isInLatitude;
	private boolean						_isInLongitude;
	private boolean						_isInSample;
	private boolean						_isInSampleType;
	private boolean						_isInUTC;
	private boolean						_isInTime;
	private boolean						_isInTemperature;

	private int								_tourCalories;
	/**
	 * This time is used when a time is not available.
	 */
	private long							_tourStartTime;
	private String							_tourDeviceSW;
	private String							_tourDeviceName;

	static {

		final TimeZone utc = TimeZone.getTimeZone("UTC"); //$NON-NLS-1$

		TIME_FORMAT.setTimeZone(utc);
		TIME_FORMAT_SSSZ.setTimeZone(utc);
		TIME_FORMAT_RFC822.setTimeZone(utc);

		// TIME_FORMAT_LOCAL
		// For indoor activities, even though the time is provided in the UTC element
		// is the actual recorded local time.
	}

	public Suunto3SAXHandler(	final TourbookDevice deviceDataReader,
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

		if (_isInSampleType
				|| _isInAltitude || _isInCadence || _isInDistance || _isInEnergy || _isInDateTime || _isInHR || _isInLatitude
				|| _isInLongitude || _isInDeviceSW || _isInDeviceName || _isInTemperature || _isInUTC || _isInTime) {
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

		if (_isInSamples) {

			endElement_InSamples(name);

		} else if (_isInHeader) {

			endElement_InHeader(name);

		} else if (_isInDevice) {

			endElement_InDevice(name);
		}

		if (name.equals(TAG_SAMPLE)) {

			_isInSample = false;

			finalizeSample();

		} else if (name.equals(TAG_DEVLOG_SAMPLES)) {

			_isInSamples = false;

		} else if (name.equals(TAG_DEVLOG_HEADER)) {

			_isInHeader = false;

		} else if (name.equals(TAG_DEVLOG_DEVICE)) {

			_isInDevice = false;

		} else if (name.equals(TAG_DEVLOG)) {

			finalizeTour();
		}
	}

	private void endElement_InDevice(final String name) {

		if (name.equals(TAG_DEVICE_SW)) {

			_isInDeviceSW = false;

			_tourDeviceSW = _characters.toString();

		} else if (name.equals(TAG_DEVICE_NAME)) {

			_isInDeviceName = false;

			_tourDeviceName = _characters.toString();
		}
	}

	private void endElement_InHeader(final String name) {

		if (name.equals(TAG_ENERGY)) {

			_isInEnergy = false;

			_tourCalories = (int) (Util.parseFloat(_characters.toString()) / 4184);
		} else if (name.equals(TAG_DATETIME)) {

			_isInDateTime = false;

			try {
				_tourStartTime = TIME_FORMAT_LOCAL.parse(_characters.toString()).getTime();
			} catch (ParseException e) {
				openError(e);
			}
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

				// ignore this value because <Distance> can also occur within <Events>

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
				_currentUtcTime = ZonedDateTime.parse(timeString).toInstant().toEpochMilli();
			} catch (final Exception e0) {
				try {
					_currentUtcTime = TIME_FORMAT.parse(timeString).getTime();
				} catch (final ParseException e1) {
					try {
						_currentUtcTime = TIME_FORMAT_SSSZ.parse(timeString).getTime();
					} catch (final ParseException e2) {
						try {
							_currentUtcTime = TIME_FORMAT_RFC822.parse(timeString).getTime();
						} catch (final ParseException e3) {
							try {
								_currentUtcTime = TIME_FORMAT_LOCAL.parse(timeString).getTime();
							} catch (final ParseException e4) {
								openError(e4);
							}
						}
					}
				}
			}
		} else if (name.equals(TAG_TIME))

		{
			_isInTime = false;
			_currentTime = Double.valueOf(_characters.toString()).longValue();
		}
	}

	private void finalizeSample() {

		final long sampleTime;
		if (_currentUtcTime == Long.MIN_VALUE &&
				_currentTime != Long.MIN_VALUE) {

			if (!_sampleList.isEmpty() && _sampleList.get(0).absoluteTime != Long.MIN_VALUE) {
				sampleTime = ((_sampleList.get(0).absoluteTime / 1000) + _currentTime) * 1000;
			} else {
				sampleTime = _tourStartTime;
			}

		} else {

			/*
			 * Remove milliseconds because this can cause wrong data. Position of a marker can be at
			 * the wrong second and multiple samples can have the same second but other milliseconds.
			 */
			sampleTime = _currentUtcTime / 1000 * 1000;
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

					if ((_currentUtcTime != Long.MIN_VALUE || _currentTime != Long.MIN_VALUE)
							&& _prevSampleTime != Long.MIN_VALUE && sampleTime == _prevSampleTime) {

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

		_currentUtcTime = Long.MIN_VALUE;
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
		tourData.setTourStartTime(TimeTools.getZonedDateTime(_sampleList.get(0).absoluteTime));

		tourData.setDeviceTimeInterval((short) -1);
		tourData.setImportFilePath(_importFilePath);

		tourData.setCalories(_tourCalories);

		tourData.setDeviceId(_device.deviceId);
		tourData.setDeviceName(_tourDeviceName == null ? _device.visibleName : _tourDeviceName);
		tourData.setDeviceFirmwareVersion(_tourDeviceSW == null ? UI.EMPTY_STRING : _tourDeviceSW);

		tourData.createTimeSeries(_sampleList, true);

		setDistanceSerie(tourData);

		// after all data are added, the tour id can be created
		final String uniqueId = _device.createUniqueId(tourData, Util.UNIQUE_ID_SUFFIX_SUUNTO3);
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

		if (_isInSamples) {

			if (_isInSample) {

				startElement_InSample(name);

			} else if (name.equals(TAG_SAMPLE)) {

				_isInSample = true;

				// create new time items, "sampleType" defines which time data are used
				_gpsData = new TimeData();
				_markerData = new TimeData();
				_sampleData = new TimeData();
			}

		} else if (_isInHeader) {

			startElement_InHeader(name);

		} else if (_isInDevice) {

			startElement_InDevice(name);

		} else if (name.equals(TAG_DEVLOG_SAMPLES)) {

			_isInSamples = true;

		} else if (name.equals(TAG_DEVLOG_HEADER)) {

			_isInHeader = true;

		} else if (name.equals(TAG_DEVLOG_DEVICE)) {

			_isInDevice = true;
		}

	}

	private void startElement_InDevice(final String name) {

		boolean isData = false;

		if (name.equals(TAG_DEVICE_SW)) {

			isData = true;
			_isInDeviceSW = true;

		} else if (name.equals(TAG_DEVICE_NAME)) {

			isData = true;
			_isInDeviceName = true;
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
		} else if (name.equals(TAG_DATETIME)) {

			isData = true;
			_isInDateTime = true;
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
		} else if (name.equals(TAG_TIME)) {

			isData = true;
			_isInTime = true;
		}

		if (isData) {

			// clear char buffer
			_characters.delete(0, _characters.length());
		}
	}

	private void openError(final Exception e) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				final String message = e.getMessage();
				MessageDialog.openError(Display.getCurrent().getActiveShell(),
						"Error", //$NON-NLS-1$
						message);
				System.err.println(message + " in " + _importFilePath); //$NON-NLS-1$
			}
		});
	}
}
