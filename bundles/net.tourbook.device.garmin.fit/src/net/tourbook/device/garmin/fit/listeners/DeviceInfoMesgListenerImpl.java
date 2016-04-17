package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.device.garmin.fit.FitContext;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.garmin.fit.AntplusDeviceType;
import com.garmin.fit.DeviceInfoMesg;
import com.garmin.fit.DeviceInfoMesgListener;

public class DeviceInfoMesgListenerImpl extends AbstractMesgListener implements DeviceInfoMesgListener {

	private final DateTimeFormatter	_dtFormatter	= DateTimeFormat.forStyle("MM");	//$NON-NLS-1$

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

	private boolean hasStrideSensor(final Short deviceType) {

		return deviceType.equals(AntplusDeviceType.STRIDE_SPEED_DISTANCE);
	}

	@Override
	public void onMesg(final DeviceInfoMesg mesg) {

		final Short deviceType = mesg.getDeviceType();
//		final Short antDeviceType = mesg.getAntDeviceType();
//		final Short antplusDeviceType = mesg.getAntplusDeviceType();
//
//		final AntNetwork antNetwork = mesg.getAntNetwork();
//		final BodyLocation sensorPosition = mesg.getSensorPosition();
//		final DateTime timestamp = mesg.getTimestamp();
//		final Float softwareVersion = mesg.getSoftwareVersion();
//		final Integer antDeviceNumber = mesg.getAntDeviceNumber();
//		final Integer garminProduct = mesg.getGarminProduct();
//		final Integer manufacturer = mesg.getManufacturer();
//		final Integer product = mesg.getProduct();
//		final Long cumOperatingTime = mesg.getCumOperatingTime();
//		final Long serialNumber = mesg.getSerialNumber();
//		final Short antTransmissionType = mesg.getAntTransmissionType();
//
//		final Short deviceIndex = mesg.getDeviceIndex();
//
//		final Float batteryVoltage = mesg.getBatteryVoltage();
//		final Short batteryStatus = mesg.getBatteryStatus();
//		final Short hardwareVersion = mesg.getHardwareVersion();
//		final SourceType sourceType = mesg.getSourceType();
//		final String descriptor = mesg.getDescriptor();
//		final String productName = mesg.getProductName();
//
////		if (serialNumber != null && batteryVoltage != null) {
//
//		final long javaTime = (timestamp.getTimestamp() * 1000) + com.garmin.fit.DateTime.OFFSET;
//
//		System.out.println(String.format("%s %10s %10s", //
//
//				_dtFormatter.print(javaTime),
//
//				deviceType,
////				antDeviceType,
//				antplusDeviceType
//				//
//				));
//		// TODO remove SYSTEM.OUT.PRINTLN
////		}

		if (deviceType != null) {

			final boolean hasSpeedSensor = hasSpeedSensor(deviceType);
			final boolean hasHeartRateSensor = hasHeartRateSensor(deviceType);
			final boolean hasPowerSensor = hasPowerSensor(deviceType);
			final boolean hasStrideSensor = hasStrideSensor(deviceType);

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

			if (hasStrideSensor) {
				context.setStrideSensorPresent(hasStrideSensor);
			}
		}

//		if (deviceType != null || antDeviceType != null || antplusDeviceType != null) {
//
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
//		}
	}

}
