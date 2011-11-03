package net.tourbook.device.garmin.fit;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.util.Util;

import org.apache.commons.io.FilenameUtils;

/**
 * Garmin FIT activity context used by message listeners to store activity data loaded from FIT
 * file.
 * The loaded data is converted into {@link TourData} records (one record for each session).
 * 
 * @author Marcin Kuthan <marcin.kuthan@gmail.com>
 */
public class FitActivityContext {

	private final String				filename;

	private final Map<Long, TourData>	tourDataMap;

	private FitActivityContextData		contextData;

	private boolean						speedSensorPresent;

	private boolean						heartRateSensorPresent;

	private boolean						powerSensorPresent;

	private String						deviceId;

	private String						manufacturer;

	private String						garminProduct;

	private String						softwareVersion;

	private String						sessionIndex;

	private int							serieIndex;

	private float						lapDistance;

	private int							lapTime;

	public FitActivityContext(final String filename, final Map<Long, TourData> tourDataMap) {
		this.filename = filename;
		this.tourDataMap = tourDataMap;

		contextData = new FitActivityContextData();
	}

	public void afterLap() {
		contextData.finalizeTourMarker();
	}

	public void afterRecord() {
		contextData.finalizeTimeData();
		serieIndex++;
	}

	public void afterSession() {
		contextData.finalizeTourData();
	}

	public void beforeLap() {
		contextData.initializeTourMarker();
	}

	public void beforeRecord() {
		contextData.initializeTimeData();
	}

	public void beforeSession() {
		serieIndex = 0;
		contextData.initializeTourData();
	}

	public FitActivityContextData getContextData() {
		return contextData;
	}

	public String getDeviceId() {
		return deviceId;
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
		return garminProduct;
	}

	public float getLapDistance() {
		return lapDistance;
	}

	public int getLapTime() {
		return lapTime;
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public int getSerieIndex() {
		return serieIndex;
	}

	public String getSessionIndex() {
		return sessionIndex;
	}

	public String getSoftwareVersion() {
		return softwareVersion;
	}

	public String getTourDescription() {
		return getTourTitle();
	}

	public String getTourTitle() {
		return String.format("%s (%s)", FilenameUtils.getBaseName(filename), getSessionIndex()); //$NON-NLS-1$
	}

	public boolean isHeartRateSensorPresent() {
		return heartRateSensorPresent;
	}

	public boolean isPowerSensorPresent() {
		return powerSensorPresent;
	}

	public boolean isSpeedSensorPresent() {
		return speedSensorPresent;
	}

	public void processData() {
		contextData.processData(new FitActivityContextDataHandler() {

			@Override
			public void handleTour(final TourData tourData, final ArrayList<TimeData> timeDataList, final Set<TourMarker> tourMarkerSet) {
				resetSpeedAtFirstPosition(timeDataList);

				tourData.setTourTitle(getTourTitle());
				tourData.setTourDescription(getTourDescription());

				tourData.importRawDataFile = filename;
				tourData.setTourImportFilePath(filename);

				tourData.setDeviceId(getDeviceId());
				tourData.setDeviceName(getDeviceName());
				tourData.setDeviceTimeInterval((short) -1);

				tourData.setIsDistanceFromSensor(isSpeedSensorPresent());

				final Long tourId = tourData.createTourId(Util.UNIQUE_ID_SUFFIX_GARMIN_FIT);
				if (!tourDataMap.containsKey(tourId)) {
					tourData.createTimeSeries(timeDataList, false);

					//tourData.computeTourDrivingTime();
					tourData.computeComputedValues();
					tourData.computeAltimeterGradientSerie();

					tourData.setTourMarkers(tourMarkerSet);

					tourDataMap.put(tourId, tourData);
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
		this.deviceId = deviceId;
	}

	public void setGarminProduct(final String garminProduct) {
		this.garminProduct = garminProduct;
	}

	public void setHeartRateSensorPresent(final boolean heartRateSensorPresent) {
		this.heartRateSensorPresent = heartRateSensorPresent;
	}

	public void setLapDistance(final float lapDistance2) {
		this.lapDistance = lapDistance2;
	}

	public void setLapTime(final int lapTime) {
		this.lapTime = lapTime;
	}

	public void setManufacturer(final String manufacturer) {
		this.manufacturer = manufacturer;
	}

	public void setPowerSensorPresent(final boolean powerSensorPresent) {
		this.powerSensorPresent = powerSensorPresent;
	}

	public void setSessionIndex(final String sessionIndex) {
		this.sessionIndex = sessionIndex;
	}

	public void setSoftwareVersion(final String softwareVersion) {
		this.softwareVersion = softwareVersion;
	}

	public void setSpeedSensorPresent(final boolean speedSensorPresent) {
		this.speedSensorPresent = speedSensorPresent;
	}

}
