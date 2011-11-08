package net.tourbook.device.garmin.fit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.util.Util;

import org.apache.commons.io.FilenameUtils;

/**
 * Garmin FIT activity context used by message listeners to store activity data loaded from FIT
 * file. The loaded data is converted into {@link TourData} records (one record for each session).
 * 
 * @author Marcin Kuthan <marcin.kuthan@gmail.com>
 */
public class FitActivityContext {

	private final String			_filimportFilePathename;

	private FitActivityContextData	_contextData;

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

	public FitActivityContext(	final String importFilePath,
								final Map<Long, TourData> alreadyImportedTours,
								final HashMap<Long, TourData> newlyImportedTours) {

		_filimportFilePathename = importFilePath;
		_alreadyImportedTours = alreadyImportedTours;
		_newlyImportedTours = newlyImportedTours;

		_contextData = new FitActivityContextData();
	}

	public void afterLap() {
		_contextData.finalizeTourMarker();
	}

	public void afterRecord() {
		_contextData.finalizeTimeData();
		_serieIndex++;
	}

	public void afterSession() {
		_contextData.finalizeTourData();
	}

	public void beforeLap() {
		_contextData.initializeTourMarker();
	}

	public void beforeRecord() {
		_contextData.initializeTimeData();
	}

	public void beforeSession() {
		_serieIndex = 0;
		_contextData.initializeTourData();
	}

	public FitActivityContextData getContextData() {
		return _contextData;
	}

	public String getDeviceId() {
		return _deviceId;
	}

	public String getDeviceName() {
		final StringBuilder deviceName = new StringBuilder();
		if (getManufacturer() != null) {
			deviceName.append(getManufacturer()).append(" "); //$NON-NLS-1$
		}

		if (getGarminProduct() != null) {
			deviceName.append(getGarminProduct()).append(" "); //$NON-NLS-1$
		}

		if (getSoftwareVersion() != null) {
			deviceName.append("(").append(getSoftwareVersion()).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		return deviceName.toString();
	}

	public String getGarminProduct() {
		return _garminProduct;
	}

	public float getLapDistance() {
		return _lapDistance;
	}

	public int getLapTime() {
		return _lapTime;
	}

	public String getManufacturer() {
		return _manufacturer;
	}

	public int getSerieIndex() {
		return _serieIndex;
	}

	public String getSessionIndex() {
		return _sessionIndex;
	}

	public String getSoftwareVersion() {
		return _softwareVersion;
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

	public void processData() {
		_contextData.processData(new FitActivityContextDataHandler() {

			@Override
			public void handleTour(	final TourData tourData,
									final ArrayList<TimeData> timeDataList,
									final Set<TourMarker> tourMarkerSet) {
				resetSpeedAtFirstPosition(timeDataList);

				tourData.setTourTitle(getTourTitle());
				tourData.setTourDescription(getTourDescription());

				tourData.importRawDataFile = _filimportFilePathename;
				tourData.setTourImportFilePath(_filimportFilePathename);

				tourData.setDeviceId(getDeviceId());
				tourData.setDeviceName(getDeviceName());
				tourData.setDeviceTimeInterval((short) -1);

				tourData.setIsDistanceFromSensor(isSpeedSensorPresent());

				final Long tourId = tourData.createTourId(Util.UNIQUE_ID_SUFFIX_GARMIN_FIT);
				if (_alreadyImportedTours.containsKey(tourId) == false) {
					tourData.createTimeSeries(timeDataList, false);

					//tourData.computeTourDrivingTime();
					tourData.computeComputedValues();
					tourData.computeAltimeterGradientSerie();

					tourData.setTourMarkers(tourMarkerSet);

					_newlyImportedTours.put(tourId, tourData);
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
