package net.tourbook.device.garmin.fit.listeners;

import net.tourbook.data.TimeData;
import net.tourbook.device.garmin.fit.Activator;
import net.tourbook.device.garmin.fit.DataConverters;
import net.tourbook.device.garmin.fit.FitContext;
import net.tourbook.device.garmin.fit.IPreferences;

import org.eclipse.jface.preference.IPreferenceStore;

import com.garmin.fit.DateTime;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.RecordMesgListener;

public class RecordMesgListenerImpl extends AbstractMesgListener implements RecordMesgListener {

	private IPreferenceStore	_prefStore				= Activator.getDefault().getPreferenceStore();

	private float				_temperatureAdjustment;

	private boolean				_isRemoveExceededTimeSlice;
	private long				_exceededTimeSliceLimit;
	private long				_exceededTimeSliceDuration;
	private long				_previousAbsoluteTime	= Long.MIN_VALUE;

	public RecordMesgListenerImpl(final FitContext context) {

		super(context);

		_temperatureAdjustment = _prefStore.getFloat(IPreferences.FIT_TEMPERATURE_ADJUSTMENT);
		_isRemoveExceededTimeSlice = _prefStore.getBoolean(IPreferences.FIT_IS_REMOVE_EXCEEDED_TIME_SLICE);

		// convert into seconds
		_exceededTimeSliceLimit = (long) _prefStore.getInt(IPreferences.FIT_EXCEEDED_TIME_SLICE_DURATION) * 1000;
	}

	@Override
	public void onMesg(final RecordMesg mesg) {

		context.onMesgRecord_10_Before();

		final TimeData timeData = getTimeData();

		final DateTime garminTime = mesg.getTimestamp();
		if (garminTime != null) {

			// convert garmin time into java time
			final long garminTimeS = garminTime.getTimestamp();
			final long garminTimeMS = garminTimeS * 1000;
			final long sliceJavaTime = garminTimeMS + com.garmin.fit.DateTime.OFFSET;

			long absoluteTime = sliceJavaTime;

			if (_isRemoveExceededTimeSlice) {

				// set initial value
				if (_previousAbsoluteTime == Long.MIN_VALUE) {
					_previousAbsoluteTime = sliceJavaTime;
				}

				// check if time slice is exceeded
				final long timeDiff = sliceJavaTime - _previousAbsoluteTime;
				if (timeDiff >= _exceededTimeSliceLimit) {

					// calculated exceeded time and add 1 second that 2 slices do not have the same time
					_exceededTimeSliceDuration = timeDiff + 1 * 1000;
				}

				absoluteTime -= _exceededTimeSliceDuration;
				_previousAbsoluteTime = sliceJavaTime;
			}

			timeData.absoluteTime = absoluteTime;
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

			final Float fractCadence = mesg.getFractionalCadence();

			if (fractCadence == null) {
				timeData.cadence = cadence;
			} else {
				timeData.cadence = cadence + fractCadence;
			}
		}

		final Float distance = mesg.getDistance();
		if (distance != null) {
			timeData.absoluteDistance = distance;
		}

		final Float speed = mesg.getSpeed();
		if (speed != null) {
			timeData.speed = DataConverters.convertSpeed(speed);
		}

		final Integer power = mesg.getPower();
		if (power != null) {
			timeData.power = power;
		}

		final Byte mesgTemperature = mesg.getTemperature();
		if (mesgTemperature != null) {

			if (_temperatureAdjustment != 0.0f) {

				// adjust temperature when this is set in the fit pref page
				timeData.temperature = mesgTemperature + _temperatureAdjustment;

			} else {

				timeData.temperature = mesgTemperature;
			}
		}

		context.onMesgRecord_20_After();
	}

}
