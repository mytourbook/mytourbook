package net.tourbook.device.garmin.fit.listeners;

import java.util.ArrayList;

import net.tourbook.data.TimeData;

import com.garmin.fit.DateTime;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.RecordMesgListener;

public class RecordMesgListenerImpl extends AbstractTimeDataMesgListener implements RecordMesgListener {

    public RecordMesgListenerImpl(ArrayList<TimeData> timeDataList) {
	super(timeDataList);
    }

    @Override
    public void onMesg(RecordMesg mesg) {
	TimeData timeData = new TimeData();

	DateTime timestamp = mesg.getTimestamp();
	if (timestamp != null) {
	    timeData.absoluteTime = timestamp.getTimestamp();
	}

	Integer positionLat = mesg.getPositionLat();
	if (positionLat != null) {
	    timeData.latitude = DataConverters.convertSemicirclesToDegrees(positionLat);
	}

	Integer positionLong = mesg.getPositionLong();
	if (positionLong != null) {
	    timeData.longitude = DataConverters.convertSemicirclesToDegrees(positionLong);
	}

	Float altitude = mesg.getAltitude();
	if (altitude != null) {
	    timeData.absoluteAltitude = altitude;
	}

	Short heartRate = mesg.getHeartRate();
	if (heartRate != null) {
	    timeData.pulse = heartRate;
	}

	Short cadence = mesg.getCadence();
	if (cadence != null) {
	    timeData.cadence = cadence;
	}

	Float distance = mesg.getDistance();
	if (distance != null) {
	    timeData.absoluteDistance = DataConverters.convertDistance(distance);
	}

	Float speed = mesg.getSpeed();
	if (speed != null) {
	    timeData.speed = DataConverters.convertSpeed(speed);
	}

	Integer power = mesg.getPower();
	if (power != null) {
	    timeData.power = power;
	}

	Byte temperature = mesg.getTemperature();
	if (temperature != null) {
	    timeData.temperature = temperature;
	}

	timeDataList.add(timeData);
    }

}