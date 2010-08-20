package net.tourbook.device.garmin.fit;

import java.util.ArrayList;
import java.util.Map;

import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;

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

	private String						deviceId;

	private String						manufacturer;

	private String						garminProduct;

	private String						softwareVersion;

	private String						sessionIndex;

	public FitActivityContext(String filename, Map<Long, TourData> tourDataMap) {
		this.filename = filename;
		this.tourDataMap = tourDataMap;

		contextData = new FitActivityContextData();
	}

	public void beforeSession() {
		contextData.initializeTourData();
	}

	public void afterSession() {
		contextData.finalizeTourData();
	}

	public void beforeRecord() {
		contextData.initializeTimeData();
	}

	public void afterRecord() {
		contextData.finalizeTimeData();
	}

	public void processData() {
		contextData.processData(new FitActivityContextDataHandler() {

			@Override
			public void handleTour(TourData tourData, ArrayList<TimeData> timeDataList) {
				resetSpeedAtFirstPosition(timeDataList);

				tourData.setTourTitle(getTourTitle());
				tourData.setTourDescription(getTourDescription());

				tourData.importRawDataFile = filename;
				tourData.setTourImportFilePath(filename);

				tourData.setDeviceId(getDeviceId());
				tourData.setDeviceName(getDeviceName());
				tourData.setDeviceTimeInterval((short) -1);

				tourData.setIsDistanceFromSensor(isSpeedSensorPresent());

				Long tourId = tourData.createTourId(getDeviceId());
				if (!tourDataMap.containsKey(tourId)) {
					tourData.createTimeSeries(timeDataList, false);

					//tourData.computeTourDrivingTime();
					tourData.computeComputedValues();
					tourData.computeAltimeterGradientSerie();

					tourDataMap.put(tourId, tourData);
				}
			}
		});
	}

	public String getDeviceName() {
		StringBuilder deviceName = new StringBuilder();
		if (getManufacturer() != null) {
			deviceName.append(getManufacturer()).append(" ");
		}

		if (getGarminProduct() != null) {
			deviceName.append(getGarminProduct()).append(" ");
		}

		if (getSoftwareVersion() != null) {
			deviceName.append("(").append(getSoftwareVersion()).append(")");
		}

		return deviceName.toString();
	}

	public String getTourTitle() {
		return String.format("%s (%s)", FilenameUtils.getBaseName(filename), getSessionIndex());
	}

	public String getTourDescription() {
		return getTourTitle();
	}

	public TourData getTourData() {
		return contextData.getCurrentTourData();
	}

	public TimeData getTimeData() {
		return contextData.getCurrentTimeData();
	}

	public boolean isSpeedSensorPresent() {
		return speedSensorPresent;
	}

	public void setSpeedSensorPresent(boolean speedSensorPresent) {
		this.speedSensorPresent = speedSensorPresent;
	}

	public boolean isHeartRateSensorPresent() {
		return heartRateSensorPresent;
	}

	public void setHeartRateSensorPresent(boolean heartRateSensorPresent) {
		this.heartRateSensorPresent = heartRateSensorPresent;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public void setManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
	}

	public String getGarminProduct() {
		return garminProduct;
	}

	public void setGarminProduct(String garminProduct) {
		this.garminProduct = garminProduct;
	}

	public String getSoftwareVersion() {
		return softwareVersion;
	}

	public void setSoftwareVersion(String softwareVersion) {
		this.softwareVersion = softwareVersion;
	}

	public String getSessionIndex() {
		return sessionIndex;
	}

	public void setSessionIndex(String sessionIndex) {
		this.sessionIndex = sessionIndex;
	}

	private void resetSpeedAtFirstPosition(ArrayList<TimeData> timeDataList) {
		if (!timeDataList.isEmpty()) {
			timeDataList.get(0).speed = Integer.MIN_VALUE;
		}
	}

}
