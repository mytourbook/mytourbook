/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.device.garmin.fit.listeners;

import com.garmin.fit.DateTime;
import com.garmin.fit.DeveloperField;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.RecordMesgListener;
import com.garmin.fit.util.SemicirclesConverter;

import net.tourbook.common.UI;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.device.garmin.fit.Activator;
import net.tourbook.device.garmin.fit.DataConverters;
import net.tourbook.device.garmin.fit.FitData;
import net.tourbook.device.garmin.fit.IPreferences;
import net.tourbook.device.garmin.fit.Messages;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.joda.time.Period;
import org.joda.time.PeriodType;

public class MesgListener_Record extends AbstractMesgListener implements RecordMesgListener {

   private static final String DEV_FIELD_NAME__CADENCE              = "Cadence";                                  //$NON-NLS-1$
   private static final String DEV_FIELD_NAME__GROUND_TIME          = "Ground Time";                              //$NON-NLS-1$
   private static final String DEV_FIELD_NAME__LEG_SPRING_STIFFNESS = "Leg Spring Stiffness";                     //$NON-NLS-1$
   //Power Data from  Stryd
   private static final String DEV_FIELD_NAME__POWER                = "Power";                                    //$NON-NLS-1$
   //Power Data from Garmin Running Dynamics Pod
   private static final String DEV_FIELD_NAME__RP_POWER             = "RP_Power";                                 //$NON-NLS-1$
   private static final String DEV_FIELD_NAME__FORM_POWER           = "Form Power";                               //$NON-NLS-1$
   private static final String DEV_FIELD_NAME__ELEVATION            = "Elevation";                                //$NON-NLS-1$
   private static final String DEV_FIELD_NAME__VERTICAL_OSCILLATION = "Vertical Oscillation";                     //$NON-NLS-1$

   private IPreferenceStore    _prefStore                           = Activator.getDefault().getPreferenceStore();

   private float               _temperatureAdjustment;

   private boolean             _isIgnoreSpeedValues;
   private boolean             _isReplaceExceededTimeSlice;

   private long                _exceededTimeSliceLimit;
   private long                _exceededTimeSliceDuration;
   private long                _previousAbsoluteTime                = Long.MIN_VALUE;

   public MesgListener_Record(final FitData fitData) {

      super(fitData);

      _isIgnoreSpeedValues = _prefStore.getBoolean(IPreferences.FIT_IS_IGNORE_SPEED_VALUES);

      _temperatureAdjustment = _prefStore.getFloat(IPreferences.FIT_TEMPERATURE_ADJUSTMENT);

      _isReplaceExceededTimeSlice = _prefStore.getBoolean(IPreferences.FIT_IS_REPLACE_EXCEEDED_TIME_SLICE);
      _exceededTimeSliceLimit = _prefStore.getInt(IPreferences.FIT_EXCEEDED_TIME_SLICE_DURATION);

      // convert into milliseconds
      _exceededTimeSliceLimit *= 1000;
   }

   @Override
   public void onMesg(final RecordMesg mesg) {

//      System.out.println((System.currentTimeMillis() + " onMesg Record"));
//      // TODO remove SYSTEM.OUT.PRINTLN

      fitData.onSetup_Record_10_Initialize();
      {
         setRecord(mesg);
      }
      fitData.onSetup_Record_20_Finalize();
   }

   private void setRecord(final RecordMesg mesg) {

      final TimeData timeData = fitData.getCurrent_TimeData();

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

         final long sliceJavaTime = garminTime.getDate().getTime();

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

            fitData.onSetup_Lap_10_Initialize();
            {
               final TourMarker tourMarker = fitData.getCurrent_TourMarker();

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
            fitData.onSetup_Lap_20_Finalize();
         }
      }

      /*
       * Lat & lon
       */
      final Integer positionLat = mesg.getPositionLat();
      if (positionLat != null) {
         timeData.latitude = SemicirclesConverter.semicirclesToDegrees(positionLat);
      }

      final Integer positionLong = mesg.getPositionLong();
      if (positionLong != null) {
         timeData.longitude = SemicirclesConverter.semicirclesToDegrees(positionLong);
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
      
      //	|| fieldName.equals("stance_time") //				     253.0  ms
      //	|| fieldName.equals("stance_time_balance") //		   51.31 percent
      //	|| fieldName.equals("step_length") //				    1526.0  mm
      // || fieldName.equals("vertical_oscillation") //       105.2  mm          //$NON-NLS-1$
      // || fieldName.equals("vertical_ratio") //               8.96 percent     //$NON-NLS-1$
      
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

      final Float batterySoc = mesg.getBatterySoc();
      if (batterySoc != null) {
//         System.out.println((System.currentTimeMillis() + " battery Soc: " + batterySoc));
         // TODO remove SYSTEM.OUT.PRINTLN
      }

      setRecord_DeveloperData(mesg, timeData);
   }

   /**
    * Field values from developer fields are only set, when the values are not yet set.
    *
    * @param mesg
    * @param timeData
    */
   private void setRecord_DeveloperData(final RecordMesg mesg, final TimeData timeData) {

      int developerFieldCount = 0;
      for (final DeveloperField developerField : mesg.getDeveloperFields()) {
         final String fieldName = developerField.getName();
         if (fieldName != null) {
            developerFieldCount++;
         }
      }

      if (developerFieldCount == 0) {
         return;
      }

      int powerDataSources = 0;

      for (final DeveloperField developerField : mesg.getDeveloperFields()) {
         final String fieldName = developerField.getName();

         if (fieldName != null && (fieldName.equals(DEV_FIELD_NAME__POWER) ||
               fieldName.equals(DEV_FIELD_NAME__RP_POWER))) {
            ++powerDataSources;
         }
      }

      for (final DeveloperField devField : mesg.getDeveloperFields()) {

         final String fieldName = devField.getName();
         if (fieldName == null) {
            continue;
         }

         switch (fieldName) {

         case DEV_FIELD_NAME__CADENCE:

            // 91 RPM

            if (timeData.cadence == Float.MIN_VALUE) {

               final Float fieldValue = devField.getFloatValue();
               if (fieldValue != null) {
                  timeData.cadence = fieldValue;
               }
            }

            break;

         case DEV_FIELD_NAME__ELEVATION:

            // 315 Meters

            if (timeData.altitude == Float.MIN_VALUE) {

               final Float fieldValue = devField.getFloatValue();
               if (fieldValue != null) {
                  timeData.altitude = fieldValue;
               }
            }

            break;

         case DEV_FIELD_NAME__FORM_POWER:

            // 32 Watts

            break;

         case DEV_FIELD_NAME__POWER:
         case DEV_FIELD_NAME__RP_POWER:

            //If the current power data source is not the one
            //specified by the user as the "preferred" data source,
            //we do not import it.
            if (powerDataSources > 1) {

               //Stryd
               if (fieldName.equals(DEV_FIELD_NAME__POWER) &&
                     _prefStore.getInt(IPreferences.FIT_PREFERRED_POWER_DATA_SOURCE) != 0) {
                  break;
               }

               //Garmin RD Pod
               if (fieldName.equals(DEV_FIELD_NAME__RP_POWER) &&
                     _prefStore.getInt(IPreferences.FIT_PREFERRED_POWER_DATA_SOURCE) != 1) {
                  break;
               }
            }

            timeData.powerDataSource = fieldName.equals(DEV_FIELD_NAME__POWER) ? "Stryd" : "Garmin Running Dynamics Pod"; //$NON-NLS-1$ //$NON-NLS-2$

            //  112 Watts

            if (timeData.power == Float.MIN_VALUE) {

               final Float fieldValue = devField.getFloatValue();
               if (fieldValue != null) {
                  timeData.power = fieldValue;
               }
            }

            break;

         case DEV_FIELD_NAME__GROUND_TIME:

            // 660 Milliseconds

            if (timeData.runDyn_StanceTime == Short.MIN_VALUE) {

               final Short fieldValue = devField.getShortValue();
               if (fieldValue != null) {
                  timeData.runDyn_StanceTime = fieldValue.shortValue();

               }
            }

            break;

         case DEV_FIELD_NAME__LEG_SPRING_STIFFNESS:

            // 0.0 kN/m

            break;

         case DEV_FIELD_NAME__VERTICAL_OSCILLATION:

            //  Vertical Oscillation     6.375 Centimeters
            //  Vertical Oscillation     6.375 Centimeters
            //  Vertical Oscillation     6.125 Centimeters
            //  Vertical Oscillation       6.0 Centimeters
            //  Vertical Oscillation     5.875 Centimeters
            //  Vertical Oscillation     5.875 Centimeters
            //  Vertical Oscillation      5.75 Centimeters
            //  Vertical Oscillation       6.0 Centimeters
            //  Vertical Oscillation       6.0 Centimeters

            if (timeData.runDyn_VerticalOscillation == Short.MIN_VALUE) {

               final Float fieldValue = devField.getFloatValue();
               if (fieldValue != null) {

                  timeData.runDyn_VerticalOscillation = (short) (fieldValue

                        * TourData.RUN_DYN_DATA_MULTIPLIER

                        // adjust to mm
                        * 10);
               }
            }

            break;

         default:
            break;
         }

      }

   }

}
