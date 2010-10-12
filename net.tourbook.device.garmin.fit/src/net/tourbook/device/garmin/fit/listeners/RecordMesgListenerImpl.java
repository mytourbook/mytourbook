package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.device.garmin.fit.DataConverters;
import net.tourbook.device.garmin.fit.FitActivityContext;

import com.garmin.fit.DateTime;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.RecordMesgListener;

public class RecordMesgListenerImpl extends AbstractMesgListener implements RecordMesgListener {

	public RecordMesgListenerImpl(FitActivityContext context) {
		super(context);
	}

	@Override
	public void onMesg(final RecordMesg mesg) {
		context.beforeRecord();

		final DateTime timestamp = mesg.getTimestamp();
		if (timestamp != null) {
			getTimeData().absoluteTime = DataConverters.convertTimestamp(timestamp);
		}

		final Integer positionLat = mesg.getPositionLat();
		if (positionLat != null) {
			getTimeData().latitude = DataConverters.convertSemicirclesToDegrees(positionLat);
		}

		final Integer positionLong = mesg.getPositionLong();
		if (positionLong != null) {
			getTimeData().longitude = DataConverters.convertSemicirclesToDegrees(positionLong);
		}

		final Float altitude = mesg.getAltitude();
		if (altitude != null) {
			getTimeData().absoluteAltitude = altitude;
		}

		final Short heartRate = mesg.getHeartRate();
		if (heartRate != null) {
			getTimeData().pulse = heartRate;
		}

		final Short cadence = mesg.getCadence();
		if (cadence != null) {
			getTimeData().cadence = cadence;
		}

		final Float distance = mesg.getDistance();
		if (distance != null) {
			getTimeData().absoluteDistance = DataConverters.convertDistance(distance);
		}

		final Float speed = mesg.getSpeed();
		if (speed != null) {
			getTimeData().speed = DataConverters.convertSpeed(speed);
		}

		final Integer power = mesg.getPower();
		if (power != null) {
			getTimeData().power = power;
		}

		final Byte temperature = mesg.getTemperature();
		if (temperature != null) {
			getTimeData().temperature = temperature;
		}

		context.afterRecord();
	}

}
