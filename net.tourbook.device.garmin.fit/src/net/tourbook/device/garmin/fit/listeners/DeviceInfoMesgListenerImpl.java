package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.device.garmin.fit.FitActivityContext;

import com.garmin.fit.AntplusDeviceType;
import com.garmin.fit.DeviceInfoMesg;
import com.garmin.fit.DeviceInfoMesgListener;

public class DeviceInfoMesgListenerImpl extends AbstractMesgListener implements DeviceInfoMesgListener {

	public DeviceInfoMesgListenerImpl(final FitActivityContext context) {
		super(context);
	}

	private boolean hasHeartRateSensor(final Short deviceType) {
		return deviceType.equals(AntplusDeviceType.HEART_RATE);
	}

	private boolean hasPowerSensor(final Short deviceType) {
		return deviceType.equals(AntplusDeviceType.BIKE_POWER);
	}

	private boolean hasSpeedSensor(final Short deviceType) {

		return deviceType.equals(AntplusDeviceType.BIKE_SPEED)
				|| deviceType.equals(AntplusDeviceType.BIKE_SPEED_CADENCE);
	}

	@Override
	public void onMesg(final DeviceInfoMesg mesg) {

		final Short deviceType = mesg.getDeviceType();

		if (deviceType != null) {

			context.setSpeedSensorPresent(hasSpeedSensor(deviceType));
			context.setHeartRateSensorPresent(hasHeartRateSensor(deviceType));
			context.setPowerSensorPresent(hasPowerSensor(deviceType));
		}
	}

}
