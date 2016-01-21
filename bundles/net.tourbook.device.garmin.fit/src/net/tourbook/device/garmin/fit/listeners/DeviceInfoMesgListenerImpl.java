package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.device.garmin.fit.FitContext;

import com.garmin.fit.AntplusDeviceType;
import com.garmin.fit.DeviceInfoMesg;
import com.garmin.fit.DeviceInfoMesgListener;

public class DeviceInfoMesgListenerImpl extends AbstractMesgListener implements DeviceInfoMesgListener {

	public DeviceInfoMesgListenerImpl(final FitContext context) {
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
				|| deviceType.equals(AntplusDeviceType.BIKE_SPEED_CADENCE)
				|| deviceType.equals(AntplusDeviceType.STRIDE_SPEED_DISTANCE);
	}

	@Override
	public void onMesg(final DeviceInfoMesg mesg) {

		final Short deviceType = mesg.getDeviceType();
		final Short antDeviceType = mesg.getAntDeviceType();
		final Short antplusDeviceType = mesg.getAntplusDeviceType();

		if (deviceType != null) {

			final boolean hasSpeedSensor = hasSpeedSensor(deviceType);
			final boolean hasHeartRateSensor = hasHeartRateSensor(deviceType);
			final boolean hasPowerSensor = hasPowerSensor(deviceType);

			/*
			 * This event occures several times and can set a true to false, therefore only true is
			 * set, false is the default.
			 */

			if (hasSpeedSensor) {
				context.setSpeedSensorPresent(hasSpeedSensor);
			}

			if (hasHeartRateSensor) {
				context.setHeartRateSensorPresent(hasHeartRateSensor);
			}

			if (hasPowerSensor) {
				context.setPowerSensorPresent(hasPowerSensor);
			}
		}

		if (deviceType != null || antDeviceType != null || antplusDeviceType != null) {

//			System.out.println(String.format("\n"//
//					+ "DeviceInfoMesg" //
//					+ "\tdev %d" //
//					+ "\tantDev %d" //
//					+ "\tantplusDev %d"
//					+ "\n", //$NON-NLS-1$
////
//					deviceType,
//					antDeviceType,
//					antplusDeviceType
//			//
//					));
//			// TODO remove SYSTEM.OUT.PRINTLN
		}
	}

}
