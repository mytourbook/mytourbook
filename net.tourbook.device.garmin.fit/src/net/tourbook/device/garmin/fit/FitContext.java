package net.tourbook.device.garmin.fit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.GearData;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.importdata.TourbookDevice;

import org.apache.commons.io.FilenameUtils;
import org.joda.time.DateTime;

import com.garmin.fit.EventMesg;

/**
 * Garmin FIT activity context used by message listeners to store activity data loaded from FIT
 * file. The loaded data is converted into {@link TourData} records (one record for each session).
 * 
 * @author Marcin Kuthan <marcin.kuthan@gmail.com>
 */
public class FitContext {

	private TourbookDevice			_device;
	private final String			_importFilePathName;

	private FitContextData			_contextData;

	private boolean					_isSpeedSensorPresent;
	private boolean					_isHeartRateSensorPresent;
	private boolean					_isPowerSensorPresent;

	private String					_deviceId;
	private String					_manufacturer;
	private String					_garminProduct;
	private String					_softwareVersion;

	private String					_sessionIndex;
	private int						_serieIndex;

	private DateTime				_sessionTime;

	private float					_lapDistance;
	private int						_lapTime;

	private Map<Long, TourData>		_alreadyImportedTours;
	private HashMap<Long, TourData>	_newlyImportedTours;

	public FitContext(	final TourbookDevice device,
						final String importFilePath,
						final Map<Long, TourData> alreadyImportedTours,
						final HashMap<Long, TourData> newlyImportedTours) {

		_device = device;
		_importFilePathName = importFilePath;
		_alreadyImportedTours = alreadyImportedTours;
		_newlyImportedTours = newlyImportedTours;

		_contextData = new FitContextData();
	}

	public void finalizeTour() {

		_contextData.processAllTours(new FitContextDataHandler() {

			@Override
			public void finalizeTour(	final TourData tourData,
										final List<TimeData> timeDataList,
										final List<TourMarker> tourMarkers,
										final List<GearData> gears) {

				resetSpeedAtFirstPosition(timeDataList);

// disabled, this is annoing
//				tourData.setTourTitle(getTourTitle());
//				tourData.setTourDescription(getTourDescription());

				tourData.importRawDataFile = _importFilePathName;
				tourData.setTourImportFilePath(_importFilePathName);

				tourData.setDeviceId(getDeviceId());
				tourData.setDeviceName(getDeviceName());
				tourData.setDeviceFirmwareVersion(_softwareVersion);
				tourData.setDeviceTimeInterval((short) -1);

				tourData.setIsDistanceFromSensor(_isSpeedSensorPresent);
				tourData.setIsPulseSensorPresent(_isHeartRateSensorPresent);
				tourData.setIsPowerSensorPresent(_isPowerSensorPresent);

				final long recordStartTime = timeDataList.get(0).absoluteTime;
				final long sessionStartTime = _sessionTime.getMillis();

				if (recordStartTime != sessionStartTime) {

					StatusUtil.log(String
							.format(
									"Import file %s has other session start time, sessionStartTime=%s recordStartTime=%s, Difference=%d sec",//$NON-NLS-1$
									_importFilePathName,
									new DateTime(sessionStartTime),
									new DateTime(recordStartTime),
									(recordStartTime - sessionStartTime) / 1000));
				}

				tourData.setTourStartTime(new DateTime(recordStartTime));

				tourData.createTimeSeries(timeDataList, false);

				// after all data are added, the tour id can be created
				final String uniqueId = _device.createUniqueId(tourData, Util.UNIQUE_ID_SUFFIX_GARMIN_FIT);
				final Long tourId = tourData.createTourId(uniqueId);

				if (_alreadyImportedTours.containsKey(tourId) == false) {

					// add new tour to the map
					_newlyImportedTours.put(tourId, tourData);

					// create additional data
					tourData.computeComputedValues();
					tourData.computeAltimeterGradientSerie();

					// must be called after time series are created
					setupTour_Gears(tourData, gears);

					setupTour_Marker(tourData, tourMarkers);
				}
			}

			private void setupTour_Gears(final TourData tourData, final List<GearData> gearList) {

				if (gearList == null) {
					return;
				}

				/*
				 * validate gear list
				 */
				final int[] timeSerie = tourData.timeSerie;
				final long tourStartTime = tourData.getTourStartTimeMS();
				final long tourEndTime = tourStartTime + (timeSerie[timeSerie.length - 1] * 1000);

				final List<GearData> validatedGearList = new ArrayList<GearData>();
				GearData startGear = null;

				for (final GearData gearData : gearList) {

					final long gearTime = gearData.absoluteTime;

					// ensure time is valid
					if (gearTime < tourStartTime) {
						startGear = gearData;
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

			private void setupTour_Marker(final TourData tourData, final List<TourMarker> tourMarkers) {

				if (tourMarkers == null) {
					return;
				}

				final int[] timeSerie = tourData.timeSerie;
				final int lastSerieIndex = timeSerie.length;
				final long tourStartTimeS = tourData.getTourStartTimeMS() / 1000;

				final Set<TourMarker> validatedTourMarkers = new HashSet<TourMarker>();

				for (final TourMarker tourMarker : tourMarkers) {

					final long markerLapTimeS = tourMarker.getDeviceLapTime() / 1000;

					for (int serieIndex = 0; serieIndex < timeSerie.length; serieIndex++) {

						final int relativeTimeS = timeSerie[serieIndex];
						final long tourTimeS = tourStartTimeS + relativeTimeS;

						if (markerLapTimeS <= tourTimeS) {

							int markerSerieIndex = serieIndex
							// ensure that the correct index is set for the marker
							- 1;

							// check bounds
							if (markerSerieIndex < 0) {
								markerSerieIndex = 0;
							}

							/*
							 * Fit devices adds a marker at the end, this is annoing therefore it is
							 * removed. It is not only the last time slice it can also be about the
							 * last 5 time slices.
							 */
							if (markerSerieIndex > lastSerieIndex - 5) {

								// check next marker
								break;
							}

							tourMarker.setSerieIndex(markerSerieIndex);

							validatedTourMarkers.add(tourMarker);

							// check next marker
							break;
						}
					}
				}

				tourData.setTourMarkers(validatedTourMarkers);
				tourData.finalizeTourMarkerWithRelativeTime();
			}
		});
	}

	public FitContextData getContextData() {
		return _contextData;
	}

	public String getDeviceId() {
		return _deviceId;
	}

	public String getDeviceName() {

		final StringBuilder deviceName = new StringBuilder();

		if (_manufacturer != null) {
			deviceName.append(_manufacturer).append(UI.SPACE);
		}

		if (_garminProduct != null) {
			deviceName.append(_garminProduct);
		}

		return deviceName.toString();
	}

	public float getLapDistance() {
		return _lapDistance;
	}

	public int getLapTime() {
		return _lapTime;
	}

	/**
	 * When tour markers are created, they are created in the sequence of the time slices or at the
	 * end of the session.
	 * 
	 * @param lapAbsoluteTime
	 * @param lapDistance
	 * @return
	 */
	public int getSerieIndex(final long lapAbsoluteTime, final float lapDistance) {

		final List<TimeData> allTimeData = _contextData.getAllTimeData();

		final int timeDataSize = allTimeData.size();

		if (_serieIndex >= timeDataSize) {

			/*
			 * The serie index is out of tour scope, try to get the index from time or distance.
			 */

			for (int serieIndex = 0; serieIndex < timeDataSize; serieIndex++) {

				final TimeData timeData = allTimeData.get(serieIndex);

				final long absoluteTime = timeData.absoluteTime;
				if (absoluteTime != Long.MIN_VALUE) {

					final long tourAbsoluteTime = absoluteTime / 1000;

					if (tourAbsoluteTime >= lapAbsoluteTime) {
						return serieIndex;
					}
				}

				// check if distance is recorded, fixed problem when a fit tour do not contain distance data
				if (lapDistance > 0) {

					final float tourAbsoluteDistance = timeData.absoluteDistance;
					if (tourAbsoluteDistance != Float.MIN_VALUE) {

						if (tourAbsoluteDistance >= lapDistance) {
							return serieIndex;
						}
					}
				}
			}
		}

		return _serieIndex;
	}

	public String getSessionIndex() {
		return _sessionIndex;
	}

	public String getTourDescription() {
		return getTourTitle();
	}

	public String getTourTitle() {
		return String.format("%s (%s)", FilenameUtils.getBaseName(_importFilePathName), getSessionIndex()); //$NON-NLS-1$
	}

	public void mesgEvent(final EventMesg mesg) {

		_contextData.ctxEventMesg(mesg);
	}

	public void mesgLap_10_Before() {

		_contextData.ctxMarker_10_Initialize();
	}

	public void mesgLap_20_After() {

		_contextData.ctxMarker_20_Finalize();
	}

	public void mesgRecord_10_Before() {

		_contextData.ctxTime_10_Initialize();
	}

	public void mesgRecord_20_After() {

		_contextData.ctxTime_20_Finalize();
		_serieIndex++;
	}

	public void mesgSession_10_Before() {

		_serieIndex = 0;
		_contextData.ctxTour_10_Initialize();
	}

	public void mesgSession_20_After() {

		_contextData.ctxTour_20_Finalize();
	}

	private void resetSpeedAtFirstPosition(final List<TimeData> timeDataList) {
		if (!timeDataList.isEmpty()) {
			timeDataList.get(0).speed = Float.MIN_VALUE;
		}
	}

	public void setDeviceId(final String deviceId) {
		_deviceId = deviceId;
	}

	public void setGarminProduct(final String garminProduct) {
		_garminProduct = garminProduct;
	}

	public void setHeartRateSensorPresent(final boolean heartRateSensorPresent) {
		_isHeartRateSensorPresent = heartRateSensorPresent;
	}

	public void setLapDistance(final float lapDistance2) {
		_lapDistance = lapDistance2;
	}

	public void setLapTime(final int lapTime) {
		_lapTime = lapTime;
	}

	public void setManufacturer(final String manufacturer) {
		_manufacturer = manufacturer;
	}

	public void setPowerSensorPresent(final boolean powerSensorPresent) {
		_isPowerSensorPresent = powerSensorPresent;
	}

	public void setSessionIndex(final String sessionIndex) {
		_sessionIndex = sessionIndex;
	}

	public void setSessionStartTime(final org.joda.time.DateTime dateTime) {
		_sessionTime = dateTime;
	}

	public void setSoftwareVersion(final String softwareVersion) {
		_softwareVersion = softwareVersion;
	}

	public void setSpeedSensorPresent(final boolean speedSensorPresent) {
		_isSpeedSensorPresent = speedSensorPresent;
	}

}
