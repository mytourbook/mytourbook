package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.data.TourData;

import com.garmin.fit.DeviceInfoMesg;
import com.garmin.fit.DeviceInfoMesgListener;
import com.garmin.fit.DeviceType;

public class DeviceInfoMesgListenerImpl extends AbstractTourDataMesgListener implements DeviceInfoMesgListener {

    public DeviceInfoMesgListenerImpl(TourData tourData) {
	super(tourData);
    }

    @Override
    public void onMesg(DeviceInfoMesg mesg) {
	Short deviceType = mesg.getDeviceType();
	if (deviceType != null) {
	    if (hasSpeedSensor(deviceType)) {
		tourData.setIsDistanceFromSensor(true);
	    }

	    if (hasHeartRateSensor(deviceType)) {
		// TODO
	    }
	}
    }

    private boolean hasSpeedSensor(Short deviceType) {
	return deviceType.equals(DeviceType.BIKE_SPEED) || deviceType.equals(DeviceType.BIKE_SPEED_CADENCE);
    }

    private boolean hasHeartRateSensor(Short deviceType) {
	return deviceType.equals(DeviceType.HEART_RATE);
    }
}