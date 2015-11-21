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

import org.eclipse.jface.preference.IPreferenceStore;
import org.joda.time.DateTime;

import com.garmin.fit.EventMesg;

/**
 * Garmin FIT activity context used by message listeners to store activity data loaded from FIT
 * file. The loaded data is converted into {@link TourData} records (one record for each session).
 * 
 * @author Marcin Kuthan <marcin.kuthan@gmail.com>
 */
public class FitContext {

	private IPreferenceStore		_prefStore	= Activator.getDefault().getPreferenceStore();

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

	private DateTime				_sessionTime;

	private float					_lapDistance;
	private int						_lapTime;

	private Map<Long, TourData>		_alreadyImportedTours;
	private HashMap<Long, TourData>	_newlyImportedTours;

	private boolean					_isIgnoreLastMarker;
	private boolean					_isSetLastMarker;
	private int						_lastMarkerTimeSlices;

	public FitContext(	final TourbookDevice device,
						final String importFilePath,
						final Map<Long, TourData> alreadyImportedTours,
						final HashMap<Long, TourData> newlyImportedTours) {

		_device = device;
		_importFilePathName = importFilePath;
		_alreadyImportedTours = alreadyImportedTours;
		_newlyImportedTours = newlyImportedTours;

		_contextData = new FitContextData();

		_isIgnoreLastMarker = _prefStore.getBoolean(IPreferences.FIT_IS_IGNORE_LAST_MARKER);
		_isSetLastMarker = _isIgnoreLastMarker == false;
		_lastMarkerTimeSlices = _prefStore.getInt(IPreferences.FIT_IGNORE_LAST_MARKER_TIME_SLICES);
	}

	public void finalizeTour() {

		final FitContextDataHandler contextHandler = new FitContextDataHandler() {

			@Override
			public void finalizeTour(	final TourData tourData,
										final List<TimeData> timeDataList,
										final List<TourMarker> tourMarkers,
										final List<GearData> gears) {

				resetSpeedAtFirstPosition(timeDataList);

// disabled, this is annoing
//				tourData.setTourTitle(getTourTitle());
//				tourData.setTourDescription(getTourDescription());

				tourData.setImportFilePath(_importFilePathName);

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

					StatusUtil
							.log(String
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

					final int rearTeeth = gearData.getRearGearTeeth();

					if (rearTeeth == 0) {

						/**
						 * This case happened but it should not. After checking the raw data they
						 * contained the wrong values.
						 * <p>
						 * <code>
						 * 
						 *  2015-08-30 08:12:50.092'345 [FitContextData]
						 * 
						 * 	Gears: GearData [absoluteTime=2015-08-27T17:39:08.000+02:00,
						 * 			FrontGearNum	= 2,
						 * 			FrontGearTeeth	= 50,
						 * 			RearGearNum		= 172,	<---
						 * 			RearGearTeeth	= 0		<---
						 * ]
						 * </code>
						 */

//						StatusUtil.log(new Exception("Wrong gear data, rearTeeth=0"));

						/*
						 * Set valid value but make it visible that the values are wrong, visible
						 * value is 0x10 / 0x30 = 0.33
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

			private void setupTour_Marker(final TourData tourData, final List<TourMarker> tourMarkers) {

				if (tourMarkers == null || tourMarkers.size() == 0) {
					return;
				}

				final int[] timeSerie = tourData.timeSerie;
				final int serieSize = timeSerie.length;

				final long absoluteTourStartTime = tourData.getTourStartTimeMS();
				final long absoluteTourEndTime = tourData.getTourEndTimeMS();

				final ArrayList<TourMarker> validatedTourMarkers = new ArrayList<>();
				final int tourMarkerSize = tourMarkers.size();

				int markerIndex = 0;
				int serieIndex = 0;

				boolean isBreakMarkerLoop = false;

				markerLoop:

				for (; markerIndex < tourMarkerSize; markerIndex++) {

					final TourMarker tourMarker = tourMarkers.get(markerIndex);
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
							 * a last marker can be set when it's far enough away from the end, this
							 * will disable the last tour marker
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

				final Set<TourMarker> tourTourMarkers = new HashSet<TourMarker>(validatedTourMarkers);

				tourData.setTourMarkers(tourTourMarkers);
			}
		};

		_contextData.processAllTours(contextHandler);
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

	public String getTourTitle() {

		return String.format("%s (%s)", _importFilePathName, _sessionIndex); //$NON-NLS-1$
	}

	public void onMesg(final EventMesg mesg) {
		_contextData.ctxEventMesg(mesg);
	}

	public void onMesgLap_10_Before() {
		_contextData.onMesgLap_Marker_10_Initialize();
	}

	public void onMesgLap_20_After() {
		_contextData.onMesgLap_Marker_20_Finalize();
	}

	public void onMesgRecord_10_Before() {
		_contextData.onMesgRecord_Time_10_Initialize();
	}

	public void onMesgRecord_20_After() {
		_contextData.onMesgRecord_Time_20_Finalize();
	}

	public void onMesgSession_10_Before() {
		_contextData.onMesgSession_Tour_10_Initialize();
	}

	public void onMesgSession_20_After() {
		_contextData.onMesgSession_Tour_20_Finalize();
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
