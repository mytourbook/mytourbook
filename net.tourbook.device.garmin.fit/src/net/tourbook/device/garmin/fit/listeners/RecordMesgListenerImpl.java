package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.data.TimeData;
import net.tourbook.device.garmin.fit.DataConverters;
import net.tourbook.device.garmin.fit.FitActivityContext;

import com.garmin.fit.DateTime;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.RecordMesgListener;

public class RecordMesgListenerImpl extends AbstractMesgListener implements RecordMesgListener {

	public RecordMesgListenerImpl(final FitActivityContext context) {
		super(context);
	}

	@Override
	public void onMesg(final RecordMesg mesg) {

		context.beforeRecord();

		final TimeData timeData = getTimeData();

		final DateTime timestamp = mesg.getTimestamp();
		if (timestamp != null) {
			timeData.absoluteTime = DataConverters.convertTimestamp(timestamp);
		}

		final Integer positionLat = mesg.getPositionLat();
		if (positionLat != null) {
			timeData.latitude = DataConverters.convertSemicirclesToDegrees(positionLat);
		}

		final Integer positionLong = mesg.getPositionLong();
		if (positionLong != null) {
			timeData.longitude = DataConverters.convertSemicirclesToDegrees(positionLong);
		}

		final Float altitude = mesg.getAltitude();
		if (altitude != null) {
			timeData.absoluteAltitude = altitude;
		}

		final Short heartRate = mesg.getHeartRate();
		if (heartRate != null) {
			timeData.pulse = heartRate;
		}

		final Short cadence = mesg.getCadence();
		if (cadence != null) {
			timeData.cadence = cadence;
		}

		final Float distance = mesg.getDistance();
		if (distance != null) {
			timeData.absoluteDistance = DataConverters.convertDistance(distance);
		}

		final Float speed = mesg.getSpeed();
		if (speed != null) {
			timeData.speed = DataConverters.convertSpeed(speed);
		}

		final Integer power = mesg.getPower();
		if (power != null) {
			timeData.power = power;
		}

		final Byte temperature = mesg.getTemperature();
		if (temperature != null) {
			timeData.temperature = temperature;
		}

		context.afterRecord();
	}

}
