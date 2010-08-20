package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.device.garmin.fit.FitActivityContext;

import com.garmin.fit.DeviceInfoMesg;
import com.garmin.fit.DeviceInfoMesgListener;
import com.garmin.fit.DeviceType;

public class DeviceInfoMesgListenerImpl extends AbstractMesgListener implements DeviceInfoMesgListener {

	public DeviceInfoMesgListenerImpl(FitActivityContext context) {
		super(context);
	}

	@Override
	public void onMesg(DeviceInfoMesg mesg) {
		Short deviceType = mesg.getDeviceType();
		if (deviceType != null) {
			context.setSpeedSensorPresent(hasSpeedSensor(deviceType));
			context.setHeartRateSensorPresent(hasHeartRateSensor(deviceType));
		}
	}

	private boolean hasSpeedSensor(Short deviceType) {
		return deviceType.equals(DeviceType.BIKE_SPEED) || deviceType.equals(DeviceType.BIKE_SPEED_CADENCE);
	}

	private boolean hasHeartRateSensor(Short deviceType) {
		return deviceType.equals(DeviceType.HEART_RATE);
	}
}
