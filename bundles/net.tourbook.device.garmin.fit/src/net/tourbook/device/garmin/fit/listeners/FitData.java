/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.device.garmin.fit.listeners;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.preference.IPreferenceStore;

import com.garmin.fit.SessionMesg;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.Util;
import net.tourbook.data.GearData;
import net.tourbook.data.SwimData;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.device.garmin.fit.Activator;
import net.tourbook.device.garmin.fit.FitDataReader;
import net.tourbook.device.garmin.fit.IPreferences;
import net.tourbook.tour.TourLogManager;
import net.tourbook.ui.tourChart.ChartLabel;

/**
 * Collects all data from a fit file
 */
public class FitData {

	private static final Integer		DEFAULT_MESSAGE_INDEX	= Integer.valueOf(0);

	private IPreferenceStore			_prefStore					= Activator.getDefault().getPreferenceStore();

	private boolean						_isIgnoreLastMarker;
	private boolean						_isSetLastMarker;
	private int								_lastMarkerTimeSlices;

	private FitDataReader				_fitDataReader2;
	private String							_importFilePathName;

	private HashMap<Long, TourData>	_alreadyImportedTours;
	private HashMap<Long, TourData>	_newlyImportedTours;

	private TourData						_tourData					= new TourData();

	private String							_deviceId;
	private String							_manufacturer;
	private String							_garminProduct;
	private String							_softwareVersion;

	private String							_sessionIndex;
	private ZonedDateTime				_sessionStartTime;

	private final List<TimeData>		_allTimeData				= new ArrayList<>();

	private final List<GearData>		_allGearData				= new ArrayList<>();
	private final List<SwimData>		_allSwimData				= new ArrayList<>();
	private final List<TourMarker>	_allTourMarker				= new ArrayList<>();

	private TimeData						_current_TimeData;
	private TimeData						_previous_TimeData;

	private TourMarker					_current_TourMarker;

	private long							_timeDiffMS;

	public FitData(final FitDataReader fitDataReader2,
						final String importFilePath,
						final HashMap<Long, TourData> alreadyImportedTours,
						final HashMap<Long, TourData> newlyImportedTours) {

		this._fitDataReader2 = fitDataReader2;
		this._importFilePathName = importFilePath;
		this._alreadyImportedTours = alreadyImportedTours;
		this._newlyImportedTours = newlyImportedTours;

		_isIgnoreLastMarker = _prefStore.getBoolean(IPreferences.FIT_IS_IGNORE_LAST_MARKER);
		_isSetLastMarker = _isIgnoreLastMarker == false;
		_lastMarkerTimeSlices = _prefStore.getInt(IPreferences.FIT_IGNORE_LAST_MARKER_TIME_SLICES);

	}

	public void finalizeTour() {

		// reset speed at first position
		if (_allTimeData.size() > 0) {
			_allTimeData.get(0).speed = Float.MIN_VALUE;
		}

// disabled, this is annoing
//    tourData.setTourTitle(getTourTitle());
//    tourData.setTourDescription(getTourDescription());

		_tourData.setImportFilePath(_importFilePathName);

		_tourData.setDeviceId(_deviceId);
		_tourData.setDeviceName(getDeviceName());
		_tourData.setDeviceFirmwareVersion(_softwareVersion);
		_tourData.setDeviceTimeInterval((short) -1);

		final long recordStartTime = _allTimeData.get(0).absoluteTime;

		if (_sessionStartTime != null) {

			final long sessionStartTime = _sessionStartTime.toInstant().toEpochMilli();

			if (recordStartTime != sessionStartTime) {

				final String message =
						"Import file %s has other session start time, sessionStartTime=%s recordStartTime=%s, Difference=%d sec";//$NON-NLS-1$

				TourLogManager.logSubInfo(
						String.format(
								message,
								_importFilePathName,
								TimeTools.getZonedDateTime(sessionStartTime).format(TimeTools.Formatter_DateTime_M),
								TimeTools.getZonedDateTime(recordStartTime).format(TimeTools.Formatter_DateTime_M),
								(recordStartTime - sessionStartTime) / 1000));
			}
		}

		final ZonedDateTime zonedStartTime = TimeTools.getZonedDateTime(recordStartTime);

		_tourData.setTourStartTime(zonedStartTime);

		_tourData.createTimeSeries(_allTimeData, false);

		// after all data are added, the tour id can be created
		final String uniqueId = _fitDataReader2.createUniqueId(_tourData, Util.UNIQUE_ID_SUFFIX_GARMIN_FIT);
		final Long tourId = _tourData.createTourId(uniqueId);

		if (_alreadyImportedTours.containsKey(tourId) == false) {

			// add new tour to the map
			_newlyImportedTours.put(tourId, _tourData);

			// create additional data
			_tourData.computeComputedValues();
			_tourData.computeAltimeterGradientSerie();

			// must be called after time series are created
			finalizeTour_Gears(_tourData, _allGearData);

			finalizeTour_Marker(_tourData, _allTourMarker);
			_tourData.finalizeTour_SwimData(_tourData, _allSwimData);
		}
	}

	private void finalizeTour_Gears(final TourData tourData, final List<GearData> gearList) {

		if (gearList == null) {
			return;
		}

		/*
		 * validate gear list
		 */
		final int[] timeSerie = tourData.timeSerie;
		final long tourStartTime = tourData.getTourStartTimeMS();
		final long tourEndTime = tourStartTime + (timeSerie[timeSerie.length - 1] * 1000);

		final List<GearData> validatedGearList = new ArrayList<>();
		GearData startGear = null;

		for (final GearData gearData : gearList) {

			final long gearTime = gearData.absoluteTime;

			// ensure time is valid
			if (gearTime < tourStartTime) {
				startGear = gearData;
			}

			final int rearTeeth = gearData.getRearGearTeeth();

			if (rearTeeth == 0) {

				/**
				 * This case happened but it should not. After checking the raw data they contained the
				 * wrong values.
				 * <p>
				 * <code>
				 *
				 *  2015-08-30 08:12:50.092'345 [FitContextData]
				 *
				 *    Gears: GearData [absoluteTime=2015-08-27T17:39:08.000+02:00,
				 *          FrontGearNum   = 2,
				 *          FrontGearTeeth = 50,
				 *          RearGearNum    = 172,   <---
				 *          RearGearTeeth  = 0      <---
				 * ]
				 * </code>
				 */

				/*
				 * Set valid value but make it visible that the values are wrong, visible value is 0x10
				 * / 0x30 = 0.33
				 */

				gearData.gears = 0x10013001;
			}

			if (gearTime >= tourStartTime && gearTime <= tourEndTime) {

				// set initial gears when available
				if (startGear != null) {

					// set time to tour start
					startGear.absoluteTime = tourStartTime;

					validatedGearList.add(startGear);
					startGear = null;
				}

				validatedGearList.add(gearData);
			}
		}

		if (validatedGearList.size() > 0) {

			// set end gear
			final GearData lastGearData = validatedGearList.get(validatedGearList.size() - 1);
			if (lastGearData.absoluteTime < tourEndTime) {

				final GearData lastGear = new GearData();
				lastGear.absoluteTime = tourEndTime;
				lastGear.gears = lastGearData.gears;

				validatedGearList.add(lastGear);
			}

			tourData.setGears(validatedGearList);
		}
	}

	private void finalizeTour_Marker(final TourData tourData, final List<TourMarker> allTourMarkers) {

		if (allTourMarkers == null || allTourMarkers.size() == 0) {
			return;
		}

		final int[] timeSerie = tourData.timeSerie;
		final int serieSize = timeSerie.length;

		final long absoluteTourStartTime = tourData.getTourStartTimeMS();
		final long absoluteTourEndTime = tourData.getTourEndTimeMS();

		final ArrayList<TourMarker> validatedTourMarkers = new ArrayList<>();
		final int tourMarkerSize = allTourMarkers.size();

		int markerIndex = 0;
		int serieIndex = 0;

		boolean isBreakMarkerLoop = false;

		markerLoop:

		for (; markerIndex < tourMarkerSize; markerIndex++) {

			final TourMarker tourMarker = allTourMarkers.get(markerIndex);
			final long absoluteMarkerTime = tourMarker.getDeviceLapTime();

			boolean isSetMarker = false;

			for (; serieIndex < serieSize; serieIndex++) {

				int relativeTourTimeS = timeSerie[serieIndex];
				long absoluteTourTime = absoluteTourStartTime + relativeTourTimeS * 1000;

				final long timeDiffEnd = absoluteTourEndTime - absoluteMarkerTime;
				if (timeDiffEnd < 0) {

					// there cannot be a marker after the tour
					if (markerIndex < tourMarkerSize) {

						// there are still markers available which are not set in the tour, set a last marker into the last time slice

						// set values for the last time slice
						serieIndex = serieSize - 1;
						relativeTourTimeS = timeSerie[serieIndex];
						absoluteTourTime = absoluteTourStartTime + relativeTourTimeS * 1000;

						isSetMarker = true;
					}

					isBreakMarkerLoop = true;
				}

				final long timeDiffMarker = absoluteMarkerTime - absoluteTourTime;
				if (timeDiffMarker <= 0) {

					// time for the marker is found

					isSetMarker = true;
				}

				if (isSetMarker) {

					/*
					 * a last marker can be set when it's far enough away from the end, this will disable
					 * the last tour marker
					 */
					final boolean canSetLastMarker = _isIgnoreLastMarker
							&& serieIndex < serieSize - _lastMarkerTimeSlices;

					if (_isSetLastMarker || canSetLastMarker) {

						tourMarker.setTime(relativeTourTimeS, absoluteTourTime);
						tourMarker.setSerieIndex(serieIndex);

						tourData.completeTourMarker(tourMarker, serieIndex);

						validatedTourMarkers.add(tourMarker);
					}

					// check next marker
					break;
				}
			}

			if (isBreakMarkerLoop) {
				break markerLoop;
			}
		}

		final Set<TourMarker> tourTourMarkers = new HashSet<>(validatedTourMarkers);

		tourData.setTourMarkers(tourTourMarkers);
	}

	List<TimeData> getAllTimeData() {
		return _allTimeData;
	}

	TimeData getCurrent_TimeData() {

		if (_current_TimeData == null) {
			throw new IllegalArgumentException("Time data is not initialized"); //$NON-NLS-1$
		}

		return _current_TimeData;

	}

	TourMarker getCurrent_TourMarker() {

		if (_current_TourMarker == null) {
			throw new IllegalArgumentException("Tour marker is not initialized"); //$NON-NLS-1$
		}

		return _current_TourMarker;
	}

	String getDeviceName() {

		final StringBuilder deviceName = new StringBuilder();

		if (_manufacturer != null) {
			deviceName.append(_manufacturer).append(UI.SPACE);
		}

		if (_garminProduct != null) {
			deviceName.append(_garminProduct);
		}

		return deviceName.toString();
	}

	List<GearData> getGearData() {
		return _allGearData;
	}

	List<SwimData> getSwimData() {
		return _allSwimData;
	}

	long getTimeDiffMS() {
		return _timeDiffMS;
	}

	TourData getTourData() {
		return _tourData;
	}

	String getTourTitle() {

		return String.format("%s (Session: %s)", _importFilePathName, _sessionIndex); //$NON-NLS-1$
	}

	void onSetup_Lap_10_Initialize() {

		final List<TourMarker> tourMarkers = _allTourMarker;

		_current_TourMarker = new TourMarker(_tourData, ChartLabel.MARKER_TYPE_DEVICE);

		tourMarkers.add(_current_TourMarker);
	}

	void onSetup_Lap_20_Finalize() {

		_current_TourMarker = null;
	}

	void onSetup_Record_10_Initialize() {

		_current_TimeData = new TimeData();
	}

	void onSetup_Record_20_Finalize() {

		if (_current_TimeData == null) {
			// this occured
			return;
		}

		boolean useThisTimeSlice = true;

		if (_previous_TimeData != null) {

			final long prevTime = _previous_TimeData.absoluteTime;
			final long currentTime = _current_TimeData.absoluteTime;

			if (prevTime == currentTime) {

				/*
				 * Ignore and merge duplicated records. The device Bryton 210 creates duplicated enries,
				 * to have valid data for this device, they must be merged.
				 */

				useThisTimeSlice = false;

				if (_previous_TimeData.absoluteAltitude == Float.MIN_VALUE) {
					_previous_TimeData.absoluteAltitude = _current_TimeData.absoluteAltitude;
				}

				if (_previous_TimeData.absoluteDistance == Float.MIN_VALUE) {
					_previous_TimeData.absoluteDistance = _current_TimeData.absoluteDistance;
				}

				if (_previous_TimeData.cadence == Float.MIN_VALUE) {
					_previous_TimeData.cadence = _current_TimeData.cadence;
				}

				if (_previous_TimeData.latitude == Double.MIN_VALUE) {
					_previous_TimeData.latitude = _current_TimeData.latitude;
				}

				if (_previous_TimeData.longitude == Double.MIN_VALUE) {
					_previous_TimeData.longitude = _current_TimeData.longitude;
				}

				if (_previous_TimeData.power == Float.MIN_VALUE) {
					_previous_TimeData.power = _current_TimeData.power;
				}

				if (_previous_TimeData.pulse == Float.MIN_VALUE) {
					_previous_TimeData.pulse = _current_TimeData.pulse;
				}

				if (_previous_TimeData.speed == Float.MIN_VALUE) {
					_previous_TimeData.speed = _current_TimeData.speed;
				}

				if (_previous_TimeData.temperature == Float.MIN_VALUE) {
					_previous_TimeData.temperature = _current_TimeData.temperature;
				}
			}
		}

		if (useThisTimeSlice) {
			_allTimeData.add(_current_TimeData);
		}

		_previous_TimeData = _current_TimeData;
		_current_TimeData = null;
	}

	void onSetup_Session_20_Finalize() {

		onSetup_Record_20_Finalize();

		_timeDiffMS = Long.MIN_VALUE;
	}

	void setDeviceId(final String deviceId) {
		_deviceId = deviceId;
	}

	void setGarminProduct(final String garminProduct) {
		_garminProduct = garminProduct;
	}

	void setHeartRateSensorPresent(final boolean isHeartRateSensorPresent) {
		_tourData.setIsPulseSensorPresent(isHeartRateSensorPresent);
	}

	void setManufacturer(final String manufacturer) {
		_manufacturer = manufacturer;
	}

	void setPowerSensorPresent(final boolean isPowerSensorPresent) {
		_tourData.setIsPowerSensorPresent(isPowerSensorPresent);
	}

	void setSessionIndex(final SessionMesg mesg) {

		final Integer fitMessageIndex = mesg.getFieldIntegerValue(254);

		final Integer messageIndex = fitMessageIndex != null ? fitMessageIndex : DEFAULT_MESSAGE_INDEX;

		_sessionIndex = messageIndex.toString();
	}

	void setSessionStartTime(final ZonedDateTime dateTime) {
		_sessionStartTime = dateTime;
	}

	void setSoftwareVersion(final String softwareVersion) {
		_softwareVersion = softwareVersion;
	}

	void setSpeedSensorPresent(final boolean isSpeedSensorPresent) {
		_tourData.setIsDistanceFromSensor(isSpeedSensorPresent);
	}

	void setStrideSensorPresent(final boolean isStrideSensorPresent) {
		_tourData.setIsStrideSensorPresent(isStrideSensorPresent);
	}

	void setTimeDiffMS(final long timeDiffMS) {

		_timeDiffMS = timeDiffMS;
	}
}
