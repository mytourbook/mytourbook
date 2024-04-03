/*******************************************************************************
 * Copyright (C) 2023, 2024 Frédéric Bard and Contributors
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
package net.tourbook.export.fit;

import com.garmin.fit.ActivityMesg;
import com.garmin.fit.DateTime;
import com.garmin.fit.DeveloperDataIdMesg;
import com.garmin.fit.DeviceIndex;
import com.garmin.fit.DeviceInfoMesg;
import com.garmin.fit.Event;
import com.garmin.fit.EventMesg;
import com.garmin.fit.EventType;
import com.garmin.fit.Factory;
import com.garmin.fit.Field;
import com.garmin.fit.File;
import com.garmin.fit.FileEncoder;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Fit;
import com.garmin.fit.FitRuntimeException;
import com.garmin.fit.Gender;
import com.garmin.fit.HrvMesg;
import com.garmin.fit.LapMesg;
import com.garmin.fit.LocalDateTime;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.Mesg;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.Sport;
import com.garmin.fit.SubSport;
import com.garmin.fit.UserProfileMesg;
import com.garmin.fit.util.SemicirclesConverter;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.DeviceSensor;
import net.tourbook.data.DeviceSensorValue;
import net.tourbook.data.GearData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourPerson;
import net.tourbook.export.Activator;

import org.osgi.framework.Version;

public class FitExporter {

   private TourData   _tourData;
   private long[]     _pausedTime_Start;
   private long[]     _pausedTime_End;
   private List<Long> _listPausedTime_Data;
   private List<Mesg> _messages = new ArrayList<>();

   private static byte[] convertUUIDToBytes(final UUID uuid) {

      final ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
      byteBuffer.putLong(uuid.getMostSignificantBits());
      byteBuffer.putLong(uuid.getLeastSignificantBits());
      return byteBuffer.array();
   }

   private static UserProfileMesg createUserProfile() {

      final TourPerson activePerson = TourbookPlugin.getActivePerson();
      if (activePerson == null) {
         return null;
      }

      final UserProfileMesg userProfileMesg = new UserProfileMesg();
      userProfileMesg.setGender(activePerson.getGender() == 0 ? Gender.MALE : Gender.FEMALE);
      userProfileMesg.setWeight(activePerson.getWeight());
      final int age = Period.between(
            TimeTools.now().toLocalDate(),
            TimeTools.getZonedDateTime(
                  activePerson.getBirthDay()).toLocalDate()).getYears();
      userProfileMesg.setAge((short) age);
      userProfileMesg.setFriendlyName(activePerson.getName());

      return userProfileMesg;
   }

   private void addFinalEventMessage(final DateTime finalTimestamp) {

      final EventMesg eventMesgStop = new EventMesg();
      eventMesgStop.setTimestamp(finalTimestamp);
      eventMesgStop.setEvent(Event.TIMER);
      eventMesgStop.setEventType(EventType.STOP_ALL);

      _messages.add(eventMesgStop);
   }

   private void addStartEventMessage(final DateTime startTime) {

      final EventMesg eventMesgStart = new EventMesg();
      eventMesgStart.setTimestamp(startTime);
      eventMesgStart.setEvent(Event.TIMER);
      eventMesgStart.setEventType(EventType.START);
      _messages.add(eventMesgStart);
   }

   private int createBatteryEvent(final DateTime timestamp,
                                  int batteryTimeIndex,
                                  final int timeSerieValue) {

      final int[] battery_Time = _tourData.getBattery_Time();

      if (battery_Time != null &&
            battery_Time.length > 0 &&
            batteryTimeIndex < battery_Time.length &&
            battery_Time[batteryTimeIndex] / 1000 >= timeSerieValue) {

         final short[] battery_Percentage = _tourData.getBattery_Percentage();

         final Mesg mesg = Factory.createMesg(104);

         final ActivityMesg activityMesg = new ActivityMesg();
         activityMesg.setTimestamp(timestamp);
         final Field timeStampField = new Field(activityMesg.getField(253));
         mesg.setField(timeStampField);

         mesg.setFieldValue(2, battery_Percentage[batteryTimeIndex]);

         _messages.add(mesg);

         ++batteryTimeIndex;
      }

      return batteryTimeIndex;
   }

   private DeviceInfoMesg createDeviceInfoMesg(final DateTime timeStamp,
                                               final DeviceSensorValue deviceSensorValue) {

      final DeviceInfoMesg deviceInfoMesg = new DeviceInfoMesg();

      deviceInfoMesg.setTimestamp(timeStamp);

      final DeviceSensor deviceSensor = deviceSensorValue.getDeviceSensor();
      deviceInfoMesg.setSerialNumber(deviceSensor.getSerialNumberAsLong());
      deviceInfoMesg.setManufacturer(deviceSensor.getManufacturerNumber());
      deviceInfoMesg.setProduct(deviceSensor.getProductNumber());

      return deviceInfoMesg;
   }

   private DeviceInfoMesg createDeviceInfoMesgEnd(final DateTime timeStamp,
                                                  final DeviceSensorValue deviceSensorValue) {

      final DeviceInfoMesg deviceInfoMesg = createDeviceInfoMesg(timeStamp, deviceSensorValue);

      deviceInfoMesg.setBatteryLevel((short) deviceSensorValue.getBatteryLevel_End());
      deviceInfoMesg.setBatteryStatus((short) deviceSensorValue.getBatteryStatus_End());
      final float batteryVoltage_End = deviceSensorValue.getBatteryVoltage_End();
      deviceInfoMesg.setBatteryVoltage(batteryVoltage_End < 0 ? null : batteryVoltage_End);

      return deviceInfoMesg;
   }

   private DeviceInfoMesg createDeviceInfoMesgStart(final DateTime timeStamp,
                                                    final DeviceSensorValue deviceSensorValue) {

      final DeviceInfoMesg deviceInfoMesg = createDeviceInfoMesg(timeStamp, deviceSensorValue);

      deviceInfoMesg.setBatteryLevel((short) deviceSensorValue.getBatteryLevel_Start());
      deviceInfoMesg.setBatteryStatus((short) deviceSensorValue.getBatteryStatus_Start());
      final float batteryVoltage_Start = deviceSensorValue.getBatteryVoltage_Start();
      deviceInfoMesg.setBatteryVoltage(batteryVoltage_Start < 0 ? null : batteryVoltage_Start);

      return deviceInfoMesg;
   }

   private void createFitFile(final String filename,
                              final DateTime startTime,
                              final Float version) {

      // A Device Info message is a BEST PRACTICE for FIT ACTIVITY files
      final DeviceInfoMesg deviceInfoMesg = new DeviceInfoMesg();
      deviceInfoMesg.setDeviceIndex(DeviceIndex.CREATOR);
      deviceInfoMesg.setManufacturer(Manufacturer.DEVELOPMENT);
      deviceInfoMesg.setProductName("MyTourbook"); //$NON-NLS-1$
      deviceInfoMesg.setSoftwareVersion(version);
      deviceInfoMesg.setTimestamp(startTime);

      // Create the output stream
      FileEncoder fileEncoder;

      try {
         fileEncoder = new FileEncoder(
               new java.io.File(filename),
               Fit.ProtocolVersion.V2_0);
      } catch (final FitRuntimeException e) {
         StatusUtil.log(e);
         return;
      }

      // Every FIT file MUST contain a File ID message
      final FileIdMesg fileIdMesg = new FileIdMesg();
      fileIdMesg.setType(File.ACTIVITY);
      fileIdMesg.setTimeCreated(startTime);
      fileEncoder.write(fileIdMesg);
      fileEncoder.write(deviceInfoMesg);

      final UserProfileMesg userProfileMesg = createUserProfile();
      if (userProfileMesg != null) {
         fileEncoder.write(userProfileMesg);
      }

      _messages.forEach(message -> fileEncoder.write(message));

      // Close the output stream
      try {
         fileEncoder.close();
      } catch (final FitRuntimeException e) {
         StatusUtil.log(e);
      }
   }

   private GearData createGearEvent(final DateTime timestamp,
                                    GearData previousGearData,
                                    final int timeSerieIndex) {

      final long[] gearSerie = _tourData.gearSerie;
      if (gearSerie == null) {
         return null;
      }

      final long currentGear = gearSerie[timeSerieIndex];

      if (previousGearData == null ||
            previousGearData.gears != currentGear) {

         final EventMesg gearEventMesg = new EventMesg();
         gearEventMesg.setTimestamp(timestamp);
         gearEventMesg.setGearChangeData(currentGear);

         final GearData gearData = new GearData();
         gearData.gears = currentGear;

         final Event event = previousGearData != null &&
               previousGearData.getFrontGearTeeth() != gearData.getFrontGearTeeth()
                     ? Event.FRONT_GEAR_CHANGE
                     : Event.REAR_GEAR_CHANGE;
         gearEventMesg.setEvent(event);

         _messages.add(gearEventMesg);

         previousGearData = gearData;
      }

      return previousGearData;
   }

   /**
    * Depending on the current heart rate there may be between 1 and 5 values
    * Source:
    * https://forums.garmin.com/developer/fit-sdk/f/discussion/255690/fit-file-hrv-data-array-interpretation
    *
    * @param pulseSerieIndex
    * @param timeSerieIndex
    *
    * @return
    */
   private int createHrvMessage(int pulseSerieIndex,
                                final int timeSerieIndex) {

      final int[] pulseTime_Milliseconds = _tourData.pulseTime_Milliseconds;
      final int[] pulseTime_TimeIndex = _tourData.pulseTime_TimeIndex;

      if (pulseTime_Milliseconds != null && pulseTime_TimeIndex != null) {

         final HrvMesg hrvMesg = new HrvMesg();

         for (int timeIndex = 0; pulseSerieIndex < pulseTime_TimeIndex[timeSerieIndex]; ++pulseSerieIndex, ++timeIndex) {

            //Possible case: if more than 5 values are consecutive, we skip the next ones
            //as it means that no time stamp data is associated with them
            if (timeIndex < 5) {

               hrvMesg.setTime(timeIndex, pulseTime_Milliseconds[pulseSerieIndex] / 1000.0f);
            }
         }

         final Float[] hrvMesgTime = hrvMesg.getTime();
         if (hrvMesgTime != null) {
            _messages.add(hrvMesg);
         }
      }

      return pulseSerieIndex;
   }

   private int createLapMessage(int markerIndex) {

      final List<TourMarker> markers = _tourData.getTourMarkersSorted();
      if (markers == null || markers.isEmpty() ||
            markers.size() == markerIndex) {
         return markerIndex;
      }

      final float previousTotalDistance = markerIndex == 0 ? 0 : markers.get(markerIndex - 1).getDistance();
      final TourMarker tourMarker = markers.get(markerIndex);
      final float lapDistance = tourMarker.getDistance() - previousTotalDistance;
      final DateTime timestamp = new DateTime(Date.from(Instant.ofEpochMilli(tourMarker.getDeviceLapTime())));
      final int pausedTime = _tourData.getPausedTime(0, tourMarker.getSerieIndex());
      final float totalTimerTime = (float) tourMarker.getTime() - pausedTime;
      final float totalElapsedTime = tourMarker.getTime();

      final LapMesg lapMessage = createLapMessage(markerIndex, lapDistance, timestamp, totalTimerTime, totalElapsedTime);

      _messages.add(lapMessage);

      return ++markerIndex;
   }

   private LapMesg createLapMessage(final int markerIndex,
                                    final float lapDistance,
                                    final DateTime startTime,
                                    final float totalTimerTime,
                                    final float totalElapsedTime) {

      final LapMesg lapMessage = new LapMesg();
      lapMessage.setMessageIndex(markerIndex);
      lapMessage.setStartTime(startTime);
      lapMessage.setTimestamp(startTime);
      lapMessage.setTotalDistance(lapDistance);
      lapMessage.setTotalTimerTime(totalTimerTime);
      lapMessage.setTotalElapsedTime(totalElapsedTime);
      lapMessage.setEvent(Event.LAP);
      return lapMessage;
   }

   private void createPauseEvent(final int[] pauseTimeIndices,
                                 final int currentTimeSerieValue) {

      if (_pausedTime_Start == null || _pausedTime_Start.length == 0 ||
            _pausedTime_End.length == pauseTimeIndices[1]) {
         return;
      }

      final long currentTime = currentTimeSerieValue * 1000L + _tourData.getTourStartTimeMS();

      if (_pausedTime_Start.length > pauseTimeIndices[0]) {

         final long currentPauseTimeStart = _pausedTime_Start[pauseTimeIndices[0]];
         // If the current time serie has not passed the next pause yet, we return.
         if (currentTime >= currentPauseTimeStart) {

            final EventMesg eventMesgStop = new EventMesg();
            final Date pausedTime_Start_Date = Date.from(Instant.ofEpochMilli(currentPauseTimeStart));
            eventMesgStop.setTimestamp(new DateTime(pausedTime_Start_Date));
            /**
             * eventData == 0: user stop<br>
             * eventData == 1: auto-stop
             */
            final Long pauseType = _listPausedTime_Data == null ? 1L : _listPausedTime_Data.get(pauseTimeIndices[0]);
            eventMesgStop.setData(pauseType);
            eventMesgStop.setEvent(Event.TIMER);
            eventMesgStop.setEventType(EventType.STOP);

            _messages.add(eventMesgStop);

            ++pauseTimeIndices[0];

            return;
         }
      }

      final long currentPauseTimeEnd = _pausedTime_End[pauseTimeIndices[1]];
      // If the current time serie has not passed the next pause yet, we return.
      if (currentTime >= currentPauseTimeEnd) {

         final EventMesg eventMesgStart = new EventMesg();
         final Date pausedTime_End_Date = Date.from(Instant.ofEpochMilli(currentPauseTimeEnd));
         eventMesgStart.setTimestamp(new DateTime(pausedTime_End_Date));
         eventMesgStart.setEvent(Event.TIMER);
         eventMesgStart.setEventType(EventType.START);

         _messages.add(eventMesgStart);

         ++pauseTimeIndices[1];
      }
   }

   // Official documentation: https://developer.garmin.com/fit/cookbook/
   public void export(final TourData tourData, final String exportFilePath) {

      _tourData = tourData;
      _pausedTime_Start = _tourData.getPausedTime_Start();
      _pausedTime_End = _tourData.getPausedTime_End();
      final long[] pausedTime_Data = _tourData.getPausedTime_Data();
      _listPausedTime_Data = pausedTime_Data == null
            ? null
            : Arrays.stream(pausedTime_Data).boxed().toList();

      _messages.clear();

      // The starting timestamp for the activity
      final DateTime startTime = new DateTime(Date.from(_tourData.getTourStartTime().toInstant()));

      // Timer Events are a BEST PRACTICE for FIT ACTIVITY files
      addStartEventMessage(startTime);

      // Create the Developer Id message for the developer data fields.
      final DeveloperDataIdMesg developerIdMesg = new DeveloperDataIdMesg();

      // It is a BEST PRACTICE to reuse the same Guid for all FIT files created by your platform
      final byte[] appId = convertUUIDToBytes(UUID.fromString("e1b60f54-e7cb-46d9-88df-ced1fe7ecf0f")); //$NON-NLS-1$
      for (int index = 0; index < appId.length; index++) {
         developerIdMesg.setApplicationId(index, appId[index]);
      }

      developerIdMesg.setDeveloperDataIndex((short) 0);
      final Version softwareVersion = Activator.getDefault().getVersion();
      final Float version = Float.valueOf(softwareVersion.getMajor() + UI.SYMBOL_DOT + softwareVersion.getMinor());
      developerIdMesg.setApplicationVersion((long) (version * 100));
      _messages.add(developerIdMesg);

      // Every FIT ACTIVITY file MUST contain Record messages

      final DateTime timestamp = new DateTime(startTime);
      final int[] timeSerie = _tourData.timeSerie;
      if (timeSerie != null) {

         int previousTimeSerieValue = 0;
         int pulseSerieIndex = 0;
         int batteryTimeIndex = 0;
         final int[] pauseTimeIndices = new int[2];
         int markerIndex = 0;
         GearData previousGearData = null;

         for (int index = 0; index < timeSerie.length; ++index) {

            final int currentTimeSerieValue = _tourData.timeSerie[index];

            timestamp.add((long) currentTimeSerieValue - previousTimeSerieValue);

            // Create a new Record message and set the timestamp
            final RecordMesg recordMesg = new RecordMesg();
            recordMesg.setTimestamp(timestamp);

            setDataSerieValue(index, recordMesg);

            // Write the Record message to the output stream
            _messages.add(recordMesg);

            pulseSerieIndex = createHrvMessage(pulseSerieIndex, index);

            previousGearData = createGearEvent(timestamp, previousGearData, index);

            batteryTimeIndex = createBatteryEvent(timestamp, batteryTimeIndex, currentTimeSerieValue);

            createPauseEvent(pauseTimeIndices, currentTimeSerieValue);

            markerIndex = createLapMessage(markerIndex);

            // Increment the timestamp by the number of seconds between the previous
            // timestamp and the current one
            previousTimeSerieValue = currentTimeSerieValue;
         }
      }

      // Every FIT ACTIVITY file MUST contain at least one Lap message
      final List<TourMarker> markers = _tourData.getTourMarkersSorted();
      if (markers == null || markers.isEmpty()) {

         final LapMesg lapMessage = createLapMessage(0,
               _tourData.getTourDistance(),
               startTime,
               _tourData.getTourDeviceTime_Recorded(),
               _tourData.getTourDeviceTime_Elapsed());

         _messages.add(lapMessage);
      }

      addFinalEventMessage(timestamp);

      final Date creationTime_Date = Date.from(Instant.now());
      final DateTime creationTime_Timestamp = new DateTime(creationTime_Date);
      final LocalDateTime creationTime_LocalTimestamp = new LocalDateTime(creationTime_Date);

      // Every FIT ACTIVITY file MUST contain at least one Session message
      final SessionMesg sessionMesg = new SessionMesg();
      sessionMesg.setMessageIndex(0);
      sessionMesg.setStartTime(startTime);
      sessionMesg.setTotalElapsedTime((float) _tourData.getTourDeviceTime_Elapsed());
      sessionMesg.setTotalTimerTime((float) _tourData.getTourDeviceTime_Recorded());
      sessionMesg.setFirstLapIndex(0);
      sessionMesg.setNumLaps(_tourData.getTourMarkers().isEmpty() ? 1 : _tourData.getTourMarkers().size());
      sessionMesg.setTimestamp(creationTime_Timestamp);
      setValues(sessionMesg);
      _messages.add(sessionMesg);

      // Every FIT ACTIVITY file MUST contain EXACTLY one Activity message
      final ActivityMesg activityMesg = new ActivityMesg();
      activityMesg.setNumSessions(1);
      activityMesg.setTimestamp(creationTime_Timestamp);
      activityMesg.setLocalTimestamp(creationTime_LocalTimestamp.getTimestamp());
      activityMesg.setTotalTimerTime((float) _tourData.getTourDeviceTime_Recorded());
      _messages.add(activityMesg);

      final Set<DeviceSensorValue> deviceSensorValues = _tourData.getDeviceSensorValues();
      if (deviceSensorValues != null) {

         for (final DeviceSensorValue deviceSensorValue : deviceSensorValues) {

            final DeviceInfoMesg deviceInfoMesgStart = createDeviceInfoMesgStart(startTime, deviceSensorValue);
            _messages.add(deviceInfoMesgStart);

            final DeviceInfoMesg deviceInfoMesgEnd = createDeviceInfoMesgEnd(timestamp, deviceSensorValue);
            _messages.add(deviceInfoMesgEnd);
         }
      }

      createFitFile(exportFilePath, creationTime_Timestamp, version);
   }

   private void setDataSerieValue(final int index, final RecordMesg recordMesg) {

      final double[] latitudeSerie = _tourData.latitudeSerie;
      if (latitudeSerie != null) {
         recordMesg.setPositionLat(SemicirclesConverter.degreesToSemicircles(latitudeSerie[index]));
      }

      final double[] longitudeSerie = _tourData.longitudeSerie;
      if (latitudeSerie != null) {
         recordMesg.setPositionLong(SemicirclesConverter.degreesToSemicircles(longitudeSerie[index]));
      }

      final float[] distanceSerie = _tourData.distanceSerie;
      if (distanceSerie != null) {
         recordMesg.setDistance(distanceSerie[index]);
      }

      final float[] speedSerie = _tourData.getSpeedSerieMetric();
      if (speedSerie != null) {
         recordMesg.setSpeed(UI.convertSpeed_KmhToMs(speedSerie[index]));
      }

      final float[] pulseSerie = _tourData.pulseSerie;
      if (pulseSerie != null) {
         recordMesg.setHeartRate((short) pulseSerie[index]);
      }

      final float[] cadenceSerie = _tourData.getCadenceSerie();
      if (cadenceSerie != null) {
         recordMesg.setCadence((short) cadenceSerie[index]);
      }

      final float[] powerSerie = _tourData.getPowerSerie();
      if (powerSerie != null) {
         recordMesg.setPower((int) powerSerie[index]);
      }

      final float[] altitudeSerie = _tourData.altitudeSerie;
      if (altitudeSerie != null) {
         recordMesg.setAltitude(altitudeSerie[index]);
      }

      final float[] temperatureSerie = _tourData.temperatureSerie;
      if (temperatureSerie != null) {
         recordMesg.setTemperature((byte) temperatureSerie[index]);
      }

      final float[] stanceTimeSerie = _tourData.getRunDyn_StanceTime();
      if (stanceTimeSerie != null) {
         recordMesg.setStanceTime(stanceTimeSerie[index]);
      }

      final float[] stanceTimeBalanceSerie = _tourData.getRunDyn_StanceTimeBalance();
      if (stanceTimeBalanceSerie != null) {
         recordMesg.setStanceTimeBalance(stanceTimeBalanceSerie[index]);
      }

      final float[] stepLengthSerie = _tourData.getRunDyn_StepLength();
      if (stepLengthSerie != null) {
         recordMesg.setStepLength(stepLengthSerie[index]);
      }

      final float[] verticalOscillationSerie = _tourData.getRunDyn_VerticalOscillation();
      if (verticalOscillationSerie != null) {
         recordMesg.setVerticalOscillation(verticalOscillationSerie[index]);
      }

      final float[] verticalRatioSerie = _tourData.getRunDyn_VerticalRatio();
      if (verticalRatioSerie != null) {
         recordMesg.setVerticalRatio(verticalRatioSerie[index]);
      }
   }

   private void setValues(final SessionMesg sessionMesg) {

      final Sport sport = FitSportMapper.mapTourTypeToSport(_tourData.getTourType());
      sessionMesg.setSport(sport);
      final SubSport subSport = FitSportMapper.mapTourTypeToSubSport(_tourData.getTourType());
      sessionMesg.setSubSport(subSport);

      // Totals
      sessionMesg.setTotalCalories(_tourData.getCalories() / 1000);
      sessionMesg.setTotalDistance(_tourData.getTourDistance());
      sessionMesg.setTotalAscent(_tourData.getTourAltUp());
      sessionMesg.setTotalDescent(_tourData.getTourAltDown());
      sessionMesg.setTotalWork(_tourData.getPower_TotalWork());
      sessionMesg.setTotalAnaerobicTrainingEffect(_tourData.getTraining_TrainingEffect_Anaerob());

      //Averages
      sessionMesg.setAvgCadence((short) _tourData.getAvgCadence());
      sessionMesg.setAvgHeartRate((short) _tourData.getAvgPulse());
      sessionMesg.setAvgPower((int) _tourData.getPower_Avg());
      sessionMesg.setAvgTemperature((byte) _tourData.getWeather_Temperature_Average_Device());

      // Maximums
      sessionMesg.setMaxPower(_tourData.getPower_Max());

      // Misc
      sessionMesg.setNormalizedPower(_tourData.getPower_Normalized());
      sessionMesg.setLeftRightBalance(_tourData.getPower_PedalLeftRightBalance());
      sessionMesg.setAvgLeftTorqueEffectiveness(_tourData.getPower_AvgLeftTorqueEffectiveness());
      sessionMesg.setAvgRightTorqueEffectiveness(_tourData.getPower_AvgRightTorqueEffectiveness());
      sessionMesg.setAvgLeftPedalSmoothness(_tourData.getPower_AvgLeftPedalSmoothness());
      sessionMesg.setAvgRightPedalSmoothness(_tourData.getPower_AvgRightPedalSmoothness());
      sessionMesg.setTrainingStressScore(_tourData.getPower_TrainingStressScore());
      sessionMesg.setIntensityFactor(_tourData.getPower_IntensityFactor());
      sessionMesg.setThresholdPower(_tourData.getPower_FTP());
      sessionMesg.setTotalTrainingEffect(_tourData.getTraining_TrainingEffect_Aerob());
   }
}
