package net.tourbook.device.garmin.fit.listeners;

import com.garmin.fit.DateTime;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.RecordMesgListener;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.joda.time.Period;
import org.joda.time.PeriodType;

import net.tourbook.common.UI;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.device.garmin.fit.Activator;
import net.tourbook.device.garmin.fit.DataConverters;
import net.tourbook.device.garmin.fit.FitContext;
import net.tourbook.device.garmin.fit.IPreferences;
import net.tourbook.device.garmin.fit.Messages;

public class Record_MesgListenerImpl extends AbstractMesgListener implements RecordMesgListener {

	private IPreferenceStore	_prefStore					= Activator.getDefault().getPreferenceStore();

	private float					_temperatureAdjustment;

	private boolean				_isIgnoreSpeedValues;
	private boolean				_isReplaceExceededTimeSlice;
	private long					_exceededTimeSliceLimit;
	private long					_exceededTimeSliceDuration;
	private long					_previousAbsoluteTime	= Long.MIN_VALUE;

	public Record_MesgListenerImpl(final FitContext context) {

		super(context);

		_isIgnoreSpeedValues = _prefStore.getBoolean(IPreferences.FIT_IS_IGNORE_SPEED_VALUES);

		_temperatureAdjustment = _prefStore.getFloat(IPreferences.FIT_TEMPERATURE_ADJUSTMENT);

		_isReplaceExceededTimeSlice = _prefStore.getBoolean(IPreferences.FIT_IS_REPLACE_EXCEEDED_TIME_SLICE);
		_exceededTimeSliceLimit = _prefStore.getInt(IPreferences.FIT_EXCEEDED_TIME_SLICE_DURATION);

		// convert into milliseconds
		_exceededTimeSliceLimit *= 1000;
	}

	@Override
	public void onMesg(final RecordMesg mesg) {

		context.getContextData().setupRecord_10_Initialize();

		final TimeData timeData = getTimeData();

		/*
		 * Distance
		 */
		final Float distance = mesg.getDistance();
		if (distance != null) {
			timeData.absoluteDistance = distance;
		}

		/*
		 * Time
		 */
		long absoluteTime = 0;
		final DateTime garminTime = mesg.getTimestamp();
		if (garminTime != null) {

			boolean isCreateExceededMarker = false;

			// convert garmin time into java time
			final long garminTimeS = garminTime.getTimestamp();
			final long garminTimeMS = garminTimeS * 1000;
			final long sliceJavaTime = garminTimeMS + com.garmin.fit.DateTime.OFFSET;

			absoluteTime = sliceJavaTime;
			long timeDiff = 0;

			if (_isReplaceExceededTimeSlice) {

				// set initial value
				if (_previousAbsoluteTime == Long.MIN_VALUE) {
					_previousAbsoluteTime = sliceJavaTime;
				}

				// check if time slice is exceeded
				timeDiff = sliceJavaTime - _previousAbsoluteTime;
				if (timeDiff >= _exceededTimeSliceLimit) {

					// time slice has exceeded the limit

					// calculated exceeded time and add 1 second that 2 slices do not have the same time
					_exceededTimeSliceDuration = timeDiff + 1 * 1000;

					isCreateExceededMarker = true;
				}

				absoluteTime -= _exceededTimeSliceDuration;
				_previousAbsoluteTime = sliceJavaTime;
			}

			timeData.absoluteTime = absoluteTime;

//			System.out.println(("[" + getClass().getSimpleName() + "]")
////					+ ("\t timestamp: " + garminTimeS)
////					+ ("\t sliceJavaTime: " + sliceJavaTime)
//					+ ("\t localDT " + new LocalDateTime(absoluteTime))
//
//			);
// TODO remove SYSTEM.OUT.PRINTLN

			if (isCreateExceededMarker) {

				/*
				 * Create a marker for the exceeded time slice
				 */

				context.getContextData().setupLap_Marker_10_Initialize();
				{
					final TourMarker tourMarker = getTourMarker();

					final PeriodType periodTemplate = PeriodType.yearMonthDayTime().withMillisRemoved();
					final Period duration = new Period(0, timeDiff, periodTemplate);

					tourMarker.setLabel(
							NLS.bind(
									Messages.Import_Error_TourMarkerLabel_ExceededTimeSlice,
									duration.toString(UI.DEFAULT_DURATION_FORMATTER_SHORT)));

					if (distance != null) {
						tourMarker.setDistance(distance);
					}

					tourMarker.setDeviceLapTime(absoluteTime);
				}
				context.getContextData().setupLap_Marker_20_Finalize();
			}
		}

		/*
		 * Lat & lon
		 */
		final Integer positionLat = mesg.getPositionLat();
		if (positionLat != null) {
			timeData.latitude = DataConverters.convertSemicirclesToDegrees(positionLat);
		}

		final Integer positionLong = mesg.getPositionLong();
		if (positionLong != null) {
			timeData.longitude = DataConverters.convertSemicirclesToDegrees(positionLong);
		}

		/*
		 * Altitude
		 */
		final Float altitude = mesg.getAltitude();
		final Float altitudeEnhanced = mesg.getEnhancedAltitude();
		if (altitudeEnhanced != null) {
			timeData.absoluteAltitude = altitudeEnhanced;
		} else if (altitude != null) {
			timeData.absoluteAltitude = altitude;
		}

		/*
		 * Heart rate
		 */
		final Short heartRate = mesg.getHeartRate();
		if (heartRate != null) {
			timeData.pulse = heartRate;
		}

		/*
		 * Cadence
		 */
		final Short cadence = mesg.getCadence();
		if (cadence != null) {

			final Float fracttionalCadence = mesg.getFractionalCadence();

			if (fracttionalCadence == null) {
				timeData.cadence = cadence;
			} else {
				timeData.cadence = cadence + fracttionalCadence;
			}

//			System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ()")
//					+ ("                                                              " + timeData.cadence));
//// TODO remove SYSTEM.OUT.PRINTLN

		}

		/*
		 * Speed
		 */
		if (_isIgnoreSpeedValues == false) {

			// use speed values

			final Float speed = mesg.getSpeed();
			final Float speedEnhanced = mesg.getEnhancedSpeed();

			if (speedEnhanced != null) {
				timeData.speed = DataConverters.convertSpeed(speedEnhanced);
			} else if (speed != null) {
				timeData.speed = DataConverters.convertSpeed(speed);
			}
		}

		/*
		 * Power
		 */
		final Integer power = mesg.getPower();
		if (power != null) {
			timeData.power = power;
		}

		/*
		 * Temperature
		 */
		final Byte mesgTemperature = mesg.getTemperature();
		if (mesgTemperature != null) {

			if (_temperatureAdjustment != 0.0f) {

				// adjust temperature when this is set in the fit pref page
				timeData.temperature = mesgTemperature + _temperatureAdjustment;

			} else {

				timeData.temperature = mesgTemperature;
			}
		}

		/**
		 * Running dynamics data <code>
		 *
		//	|| fieldName.equals("stance_time") //				  253.0  ms
		//	|| fieldName.equals("stance_time_balance") //		   51.31 percent
		//	|| fieldName.equals("step_length") //				 1526.0  mm
		//	|| fieldName.equals("vertical_oscillation") //		    7.03 percent
		//	|| fieldName.equals("vertical_ratio") //			  114.2  mm
		 * </code>
		 */
		final Float stanceTime = mesg.getStanceTime();
		if (stanceTime != null) {
			timeData.runDyn_StanceTime = stanceTime.shortValue();
		}

		final Float stanceTimeBalance = mesg.getStanceTimeBalance();
		if (stanceTimeBalance != null) {
			timeData.runDyn_StanceTimeBalance = (short) (stanceTimeBalance * TourData.RUN_DYN_DATA_MULTIPLIER);
		}

		final Float stepLength = mesg.getStepLength();
		if (stepLength != null) {
			timeData.runDyn_StepLength = stepLength.shortValue();
		}

		final Float verticalOscillation = mesg.getVerticalOscillation();
		if (verticalOscillation != null) {
			timeData.runDyn_VerticalOscillation = (short) (verticalOscillation * TourData.RUN_DYN_DATA_MULTIPLIER);
		}

		final Float verticalRatio = mesg.getVerticalRatio();
		if (verticalRatio != null) {
			timeData.runDyn_VerticalRatio = (short) (verticalRatio * TourData.RUN_DYN_DATA_MULTIPLIER);
		}

		context.getContextData().setupRecord_20_Finalize();
	}

}
