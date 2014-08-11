package net.tourbook.device.garmin.fit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.device.garmin.fit.FitContextData.ContextTimeData;
import net.tourbook.importdata.TourbookDevice;

import org.apache.commons.io.FilenameUtils;

/**
 * Garmin FIT activity context used by message listeners to store activity data loaded from FIT
 * file. The loaded data is converted into {@link TourData} records (one record for each session).
 * 
 * @author Marcin Kuthan <marcin.kuthan@gmail.com>
 */
public class FitContext {

	private TourbookDevice			_device;
	private final String			_filimportFilePathename;

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

	private float					_lapDistance;
	private int						_lapTime;

	private Map<Long, TourData>		_alreadyImportedTours;
	private HashMap<Long, TourData>	_newlyImportedTours;

	public FitContext(	final TourbookDevice device,
						final String importFilePath,
						final Map<Long, TourData> alreadyImportedTours,
						final HashMap<Long, TourData> newlyImportedTours) {

		_device = device;
		_filimportFilePathename = importFilePath;
		_alreadyImportedTours = alreadyImportedTours;
		_newlyImportedTours = newlyImportedTours;

		_contextData = new FitContextData();
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

		final List<ContextTimeData> allTimeData = _contextData.getAllTimeData();

		final int timeDataSize = allTimeData.size();

		if (_serieIndex >= timeDataSize) {

			/*
			 * The serie index is out of tour scope, try to get the index from time or distance.
			 */

			for (int serieIndex = 0; serieIndex < timeDataSize; serieIndex++) {

				final ContextTimeData contextTimeData = allTimeData.get(serieIndex);
				final TimeData timeData = contextTimeData.getTimeData();

				final long absoluteTime = timeData.absoluteTime;
				if (absoluteTime != Long.MIN_VALUE) {

					final long tourAbsoluteTime = absoluteTime / 1000;

					if (tourAbsoluteTime >= lapAbsoluteTime) {
						return serieIndex;
					}
				}

				// check if distance is recorded
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
		return String.format("%s (%s)", FilenameUtils.getBaseName(_filimportFilePathename), getSessionIndex()); //$NON-NLS-1$
	}

	public boolean isHeartRateSensorPresent() {
		return _isHeartRateSensorPresent;
	}

	public boolean isPowerSensorPresent() {
		return _isPowerSensorPresent;
	}

	public boolean isSpeedSensorPresent() {
		return _isSpeedSensorPresent;
	}

	public void mesgLap_10_Before() {

		_contextData.ctxTourMarker_10_Initialize();
	}

	public void mesgLap_20_After() {

		_contextData.ctxTourMarker_20_Finalize();
	}

	public void mesgRecord_10_Before() {

		_contextData.ctxTimeData_10_Initialize();
	}

	public void mesgRecord_20_After() {

		_contextData.ctxTimeData_20_Finalize();
		_serieIndex++;
	}

	public void mesgSession_10_Before() {

		_serieIndex = 0;
		_contextData.ctxTourData_10_Initialize();
	}

	public void mesgSession_20_After() {

		_contextData.ctxTourData_20_Finalize();
	}

	public void processData() {

		_contextData.processData(new FitContextDataHandler() {

			@Override
			public void handleTour(	final TourData tourData,
									final ArrayList<TimeData> timeDataList,
									final Set<TourMarker> tourMarkerSet) {

				resetSpeedAtFirstPosition(timeDataList);

// disabled, this is annoing
//				tourData.setTourTitle(getTourTitle());
//				tourData.setTourDescription(getTourDescription());

				tourData.importRawDataFile = _filimportFilePathename;
				tourData.setTourImportFilePath(_filimportFilePathename);

				tourData.setDeviceId(getDeviceId());
				tourData.setDeviceName(getDeviceName());
				tourData.setDeviceFirmwareVersion(_softwareVersion);
				tourData.setDeviceTimeInterval((short) -1);

				tourData.setIsDistanceFromSensor(isSpeedSensorPresent());

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

					tourData.setTourMarkers(tourMarkerSet);
				}
			}
		});
	}

	private void resetSpeedAtFirstPosition(final ArrayList<TimeData> timeDataList) {
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

	public void setSoftwareVersion(final String softwareVersion) {
		_softwareVersion = softwareVersion;
	}

	public void setSpeedSensorPresent(final boolean speedSensorPresent) {
		_isSpeedSensorPresent = speedSensorPresent;
	}

}
