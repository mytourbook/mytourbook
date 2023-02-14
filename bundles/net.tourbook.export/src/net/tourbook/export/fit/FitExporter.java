/*******************************************************************************
 * Copyright (C) 2023 Frédéric Bard and Contributors
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
import com.garmin.fit.DeveloperField;
import com.garmin.fit.DeviceIndex;
import com.garmin.fit.DeviceInfoMesg;
import com.garmin.fit.Event;
import com.garmin.fit.EventMesg;
import com.garmin.fit.EventType;
import com.garmin.fit.FieldDescriptionMesg;
import com.garmin.fit.File;
import com.garmin.fit.FileEncoder;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Fit;
import com.garmin.fit.FitBaseType;
import com.garmin.fit.FitRuntimeException;
import com.garmin.fit.Gender;
import com.garmin.fit.LapMesg;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.Mesg;
import com.garmin.fit.MesgNum;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.Sport;
import com.garmin.fit.UserProfileMesg;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;

//todo fb: Add unit test -> convert to csv and use it for comparison
public class FitExporter {

   private static void createFitFile(final List<Mesg> messages,
                                     final String filename,
                                     final DateTime startTime) {

      // The combination of file type, manufacturer id, product id, and serial number should be unique.
      // When available, a non-random serial number should be used.
      final File fileType = File.ACTIVITY;
      final short manufacturerId = Manufacturer.DEVELOPMENT;
      final short productId = 0;
      final float softwareVersion = 1.0f;

      final Random random = new Random();
      final int serialNumber = random.nextInt();

      // Every FIT file MUST contain a File ID message
      final FileIdMesg fileIdMesg = new FileIdMesg();
      fileIdMesg.setType(fileType);
      fileIdMesg.setManufacturer((int) manufacturerId);
      fileIdMesg.setProduct((int) productId);
      fileIdMesg.setTimeCreated(startTime);
      fileIdMesg.setSerialNumber((long) serialNumber);

      // A Device Info message is a BEST PRACTICE for FIT ACTIVITY files
      final DeviceInfoMesg deviceInfoMesg = new DeviceInfoMesg();
      deviceInfoMesg.setDeviceIndex(DeviceIndex.CREATOR);
      deviceInfoMesg.setManufacturer(Manufacturer.DEVELOPMENT);
      deviceInfoMesg.setProduct((int) productId);
      deviceInfoMesg.setProductName("MyTourbook"); // Max 20 Chars
      deviceInfoMesg.setSerialNumber((long) serialNumber);
      deviceInfoMesg.setSoftwareVersion(softwareVersion);
      deviceInfoMesg.setTimestamp(startTime);

      // Create the output stream
      FileEncoder fileEncoder;

      try {
         fileEncoder = new FileEncoder(new java.io.File(filename), Fit.ProtocolVersion.V2_0);
      } catch (final FitRuntimeException e) {
         StatusUtil.log(e);
         return;
      }

      //todo fb get from the people data
      final UserProfileMesg userProfileMesg = new UserProfileMesg();
      userProfileMesg.setGender(Gender.FEMALE);
      userProfileMesg.setWeight(63.1F);
      userProfileMesg.setAge((short) 99);
      userProfileMesg.setFriendlyName("TestUser");
      fileEncoder.write(userProfileMesg);

      fileEncoder.write(fileIdMesg);
      fileEncoder.write(deviceInfoMesg);

      messages.forEach(fileEncoder::write);

      // Close the output stream
      try {
         fileEncoder.close();
      } catch (final FitRuntimeException e) {
         StatusUtil.log(e);
      }
   }

   private void convertTourDataToFit(final TourData tourData, final String exportFilePath) {

      final List<Mesg> messages = new ArrayList<>();

      // The starting timestamp for the activity
      final DateTime startTime = new DateTime(new Date());

      // Timer Events are a BEST PRACTICE for FIT ACTIVITY files
      final EventMesg eventMesg = new EventMesg();
      eventMesg.setTimestamp(startTime);
      eventMesg.setEvent(Event.TIMER);
      eventMesg.setEventType(EventType.START);
      messages.add(eventMesg);

      // Create the Developer Id message for the developer data fields.
      final DeveloperDataIdMesg developerIdMesg = new DeveloperDataIdMesg();
      // It is a BEST PRACTICE to reuse the same Guid for all FIT files created by your platform
      final byte[] appId = new byte[] {
            0x1, 0x1, 0x2, 0x3,
            0x5, 0x8, 0xD, 0x15,
            0x22, 0x37, 0x59, (byte) 0x90,
            (byte) 0xE9, 0x79, 0x62, (byte) 0xDB
      };

      for (int i = 0; i < appId.length; i++) {
         developerIdMesg.setApplicationId(i, appId[i]);
      }

      developerIdMesg.setDeveloperDataIndex((short) 0);
      messages.add(developerIdMesg);

      // Create the Developer Data Field Descriptions
      final FieldDescriptionMesg doughnutsFieldDescMesg = new FieldDescriptionMesg();
      doughnutsFieldDescMesg.setDeveloperDataIndex((short) 0);
      doughnutsFieldDescMesg.setFieldDefinitionNumber((short) 0);
      doughnutsFieldDescMesg.setFitBaseTypeId(FitBaseType.FLOAT32);
      doughnutsFieldDescMesg.setUnits(0, "doughnuts");
      doughnutsFieldDescMesg.setNativeMesgNum(MesgNum.SESSION);
      messages.add(doughnutsFieldDescMesg);

      final FieldDescriptionMesg hrFieldDescMesg = new FieldDescriptionMesg();
      hrFieldDescMesg.setDeveloperDataIndex((short) 0);
      hrFieldDescMesg.setFieldDefinitionNumber((short) 1);
      hrFieldDescMesg.setFitBaseTypeId(FitBaseType.UINT8);
      hrFieldDescMesg.setFieldName(0, "Heart Rate");
      hrFieldDescMesg.setUnits(0, "bpm");
      hrFieldDescMesg.setNativeFieldNum((short) RecordMesg.HeartRateFieldNum);
      hrFieldDescMesg.setNativeMesgNum(MesgNum.RECORD);
      messages.add(hrFieldDescMesg);

      // Every FIT ACTIVITY file MUST contain Record messages
      final DateTime timestamp = new DateTime(startTime);
      final int previousTimeSerieValue = 0;

      // Create one hour (3600 seconds) of Record data
      for (int index = 0; index < tourData.timeSerie.length; ++index) {

         // Create a new Record message and set the timestamp
         final RecordMesg recordMesg = new RecordMesg();
         recordMesg.setTimestamp(timestamp);

         // Fake Record Data of Various Signal Patterns
         recordMesg.setDistance(tourData.distanceSerie[index]);
         recordMesg.setSpeed(tourData.getSpeedSerieMetric()[index]);
         recordMesg.setHeartRate((short) tourData.pulseSerie[index]); // Sine
         recordMesg.setCadence((short) tourData.getCadenceSerie()[index]); // Sawtooth
         recordMesg.setPower((int) tourData.getPowerSerie()[index]); //Square
         recordMesg.setAltitude(tourData.altitudeSerie[index]);
         recordMesg.setPositionLat((int) Math.round(tourData.latitudeSerie[index] * 11930465));
         recordMesg.setPositionLong((int) Math.round(tourData.longitudeSerie[index] * 11930465));

         // Add a Developer Field to the Record Message
         final DeveloperField hrDevField = new DeveloperField(hrFieldDescMesg, developerIdMesg);
         recordMesg.addDeveloperField(hrDevField);
         hrDevField.setValue((short) (Math.sin((.01 * index + 10)) + 1.0) * 127.0);

         // Write the Record message to the output stream
         messages.add(recordMesg);

         // Increment the timestamp by the number of seconds between the previous
         // timestamp and the current one
         timestamp.add(tourData.timeSerie[index] - previousTimeSerieValue);
      }

      //todo fb thats where we add the pauses?
      // Timer Events are a BEST PRACTICE for FIT ACTIVITY files
      final EventMesg eventMesgStop = new EventMesg();
      eventMesgStop.setTimestamp(timestamp);
      eventMesgStop.setEvent(Event.TIMER);
      eventMesgStop.setEventType(EventType.STOP_ALL);
      messages.add(eventMesgStop);

      // Every FIT ACTIVITY file MUST contain at least one Lap message
      final LapMesg lapMesg = new LapMesg();
      lapMesg.setMessageIndex(0);
      lapMesg.setTimestamp(timestamp);
      lapMesg.setStartTime(startTime);
      lapMesg.setTotalElapsedTime((float) (timestamp.getTimestamp() - startTime.getTimestamp()));
      lapMesg.setTotalTimerTime((float) (timestamp.getTimestamp() - startTime.getTimestamp()));
      messages.add(lapMesg);

      // Every FIT ACTIVITY file MUST contain at least one Session message
      final SessionMesg sessionMesg = new SessionMesg();
      sessionMesg.setMessageIndex(0);
      sessionMesg.setTimestamp(timestamp);
      sessionMesg.setStartTime(startTime);
      sessionMesg.setTotalElapsedTime((float) tourData.getTourDeviceTime_Elapsed());
      sessionMesg.setTotalTimerTime((float) tourData.getTourDeviceTime_Recorded());
      //do a map function with the tour type and by default use generic
      sessionMesg.setSport(Sport.STAND_UP_PADDLEBOARDING);
      // sessionMesg.setSubSport(SubSport.GENERIC);
      sessionMesg.setFirstLapIndex(0);
      sessionMesg.setNumLaps(tourData.getTourMarkers().size());
      messages.add(sessionMesg);

      // Add a Developer Field to the Session message
      final DeveloperField doughnutsEarnedDevField = new DeveloperField(doughnutsFieldDescMesg, developerIdMesg);
      doughnutsEarnedDevField.setValue(sessionMesg.getTotalElapsedTime() / 1200.0f);
      sessionMesg.addDeveloperField(doughnutsEarnedDevField);

      // Every FIT ACTIVITY file MUST contain EXACTLY one Activity message
      final ActivityMesg activityMesg = new ActivityMesg();
      // activityMesg.setTimestamp(timestamp);
      activityMesg.setNumSessions(1);
      // final TimeZone timeZone = TimeZone.getTimeZone(tourData.getTimeZoneId());
      //  final long timezoneOffset = (timeZone.getRawOffset() + timeZone.getDSTSavings()) / 1000;
      //  activityMesg.setLocalTimestamp(timestamp.getTimestamp() + timezoneOffset);
      //  activityMesg.setTotalTimerTime((float) (timestamp.getTimestamp() - startTime.getTimestamp()));
      messages.add(activityMesg);

      createFitFile(messages, exportFilePath, startTime);
   }

   public void export(final TourData _tourData, final String exportFilePath) {

      convertTourDataToFit(_tourData, exportFilePath);

   }

}
