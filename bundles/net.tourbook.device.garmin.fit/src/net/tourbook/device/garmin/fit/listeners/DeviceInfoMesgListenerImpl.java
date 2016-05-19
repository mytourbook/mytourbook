package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.common.UI;
import net.tourbook.device.garmin.fit.FitContext;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.garmin.fit.AntNetwork;
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

//		final DateTime timestamp = mesg.getTimestamp();

//		final Integer manufacturer = mesg.getManufacturer();
//		final Integer product = mesg.getProduct();
//		final String productName = mesg.getProductName();
//		final Integer garminProduct = mesg.getGarminProduct();
//		final String descriptor = mesg.getDescriptor();
//
//		final Short batteryStatus = mesg.getBatteryStatus();
//		final Float batteryVoltage = mesg.getBatteryVoltage();
//		final Long cumOperatingTime = mesg.getCumOperatingTime();
//
//		final Long serialNumber = mesg.getSerialNumber();
//		final Short deviceIndex = mesg.getDeviceIndex();
//		final Short hardwareVersion = mesg.getHardwareVersion();
//		final Float softwareVersion = mesg.getSoftwareVersion();
//
//		final AntNetwork antNetwork = mesg.getAntNetwork();
//		final Short antDeviceType = mesg.getAntDeviceType();
//		final Integer antDeviceNumber = mesg.getAntDeviceNumber();
//		final Short antplusDeviceType = mesg.getAntplusDeviceType();
//		final Short antTransmissionType = mesg.getAntTransmissionType();
//
//		final SourceType sourceType = mesg.getSourceType();
//		final BodyLocation sensorPosition = mesg.getSensorPosition();
//
//		if (/* serialNumber != null && batteryVoltage != null */true) {
//
//			final long javaTime = (timestamp.getTimestamp() * 1000) + com.garmin.fit.DateTime.OFFSET;
//
//			System.out.println(String.format(
//
//			"%s" //
//
//					+ "   manu:"
//					+ " %10s"
//					+ " %10s"
////					+ " %10s"
//					+ " %10s"
////					+ " %10s"
//
//					+ "   ser:"
//					+ " %10s"
//					+ " %10s"
//					+ " %10s"
//					+ " %10s"
//
//					+ "   bat:"
//					+ " %10s"
//					+ " %10s V"
//					+ " %10s"
//
//					+ "  ant:"
//					+ " %10s"
//					+ " %10s"
//					+ " %10s"
//					+ " %10s"
//					+ " %10s"
//					+ " %10s"
//
////					+ "   src:"
////					+ " %10s"
//					//
//					,
//
//					_dtFormatter.print(javaTime),
//
//					// manu
//					removeNull(manufacturer),
//					removeNull(product),
////					productName,
//					removeNull(garminProduct),
////					descriptor,
//
//					// ser
//					removeNull(serialNumber),
//					removeNull(deviceIndex),
//					removeNull(hardwareVersion),
//					removeNull_3(softwareVersion),
//
//					// bat
//					removeNull(batteryStatus),
//					removeNull_3(batteryVoltage),
//					removeNull(cumOperatingTime),
//
//					// ant
//					sourceType,
//					removeNull(antNetwork),
//					removeNull(antDeviceType),
//					removeNull(antDeviceNumber),
//					removeNull(antplusDeviceType),
//					removeNull(antTransmissionType)
//
//					// src
////					sensorPosition
//
//					//
//					));
//
//			// TODO remove SYSTEM.OUT.PRINTLN
//		}

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

	private String removeNull(final AntNetwork value) {

		return value == null //
				? UI.EMPTY_STRING
				: value.toString();
	}

	private String removeNull(final Integer value) {

		return value == null //
				? UI.EMPTY_STRING
				: value.toString();
	}

	private String removeNull(final Long value) {

		return value == null //
				? UI.EMPTY_STRING
				: value.toString();
	}

	private String removeNull(final Short value) {

		return value == null //
				? UI.EMPTY_STRING
				: value.toString();
	}

	private String removeNull_3(final Float value) {

		return value == null //
				? UI.EMPTY_STRING
				: String.format("%10.3f", value);
	}

}
