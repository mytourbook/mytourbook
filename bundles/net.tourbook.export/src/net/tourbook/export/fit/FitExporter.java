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

import com.garmin.fit.Activity;
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
import com.garmin.fit.Fit.ProtocolVersion;
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
import com.garmin.fit.SubSport;
import com.garmin.fit.UserProfileMesg;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

//todo fb: Add unit test -> convert to csv and use it for comparison
public class FitExporter {

   public static void CreateActivityFile(final List<Mesg> messages, final String filename, final DateTime startTime) {
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
      deviceInfoMesg.setProductName("FIT Cookbook"); // Max 20 Chars
      deviceInfoMesg.setSerialNumber((long) serialNumber);
      deviceInfoMesg.setSoftwareVersion(softwareVersion);
      deviceInfoMesg.setTimestamp(startTime);

      // Create the output stream
      FileEncoder encode;

      try {
          encode = new FileEncoder(new java.io.File(filename), Fit.ProtocolVersion.V2_0);
      } catch (final FitRuntimeException e) {
          System.err.println("Error opening file " + filename);
          e.printStackTrace();
          return;
      }

      encode.write(fileIdMesg);
      encode.write(deviceInfoMesg);

      for (final Mesg message : messages) {
          encode.write(message);
      }

      // Close the output stream
      try {
          encode.close();
      } catch (final FitRuntimeException e) {
          System.err.println("Error closing encode.");
          e.printStackTrace();
          return;
      }
      System.out.println("Encoded FIT Activity file " + filename);
  }

   private void example() {
      final double twoPI = Math.PI * 2.0;
      final double semiCirclesPerMeter = 107.173;
      final String filename = "ActivityEncodeRecipe.fit";

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

      // Create one hour (3600 seconds) of Record data
      for (int i = 0; i <= 3600; i++) {
         // Create a new Record message and set the timestamp
         final RecordMesg recordMesg = new RecordMesg();
         recordMesg.setTimestamp(timestamp);

         // Fake Record Data of Various Signal Patterns
         recordMesg.setDistance((float) i);
         recordMesg.setSpeed((float) 1);
         recordMesg.setHeartRate((short) ((Math.sin(twoPI * (0.01 * i + 10)) + 1.0) * 127.0)); // Sine
         recordMesg.setCadence((short) (i % 255)); // Sawtooth
         recordMesg.setPower(((short) (i % 255) < 157 ? 150 : 250)); //Square
         recordMesg.setAltitude((float) (Math.abs(i % 255.0) - 127.0)); // Triangle
         recordMesg.setPositionLat(0);
         recordMesg.setPositionLong((int) Math.round(i * semiCirclesPerMeter));

         // Add a Developer Field to the Record Message
         final DeveloperField hrDevField = new DeveloperField(hrFieldDescMesg, developerIdMesg);
         recordMesg.addDeveloperField(hrDevField);
         hrDevField.setValue((short) (Math.sin(twoPI * (.01 * i + 10)) + 1.0) * 127.0);

         // Write the Record message to the output stream
         messages.add(recordMesg);

         // Increment the timestamp by one second
         timestamp.add(1);
      }

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
      sessionMesg.setTotalElapsedTime((float) (timestamp.getTimestamp() - startTime.getTimestamp()));
      sessionMesg.setTotalTimerTime((float) (timestamp.getTimestamp() - startTime.getTimestamp()));
      sessionMesg.setSport(Sport.STAND_UP_PADDLEBOARDING);
      sessionMesg.setSubSport(SubSport.GENERIC);
      sessionMesg.setFirstLapIndex(0);
      sessionMesg.setNumLaps(1);
      messages.add(sessionMesg);

      // Add a Developer Field to the Session message
      final DeveloperField doughnutsEarnedDevField = new DeveloperField(doughnutsFieldDescMesg, developerIdMesg);
      doughnutsEarnedDevField.setValue(sessionMesg.getTotalElapsedTime() / 1200.0f);
      sessionMesg.addDeveloperField(doughnutsEarnedDevField);

      // Every FIT ACTIVITY file MUST contain EXACTLY one Activity message
      final ActivityMesg activityMesg = new ActivityMesg();
      activityMesg.setTimestamp(timestamp);
      activityMesg.setNumSessions(1);
      final TimeZone timeZone = TimeZone.getTimeZone("America/Denver");
      final long timezoneOffset = (timeZone.getRawOffset() + timeZone.getDSTSavings()) / 1000;
      activityMesg.setLocalTimestamp(timestamp.getTimestamp() + timezoneOffset);
      activityMesg.setTotalTimerTime((float) (timestamp.getTimestamp() - startTime.getTimestamp()));
      messages.add(activityMesg);

      CreateActivityFile(messages, filename, startTime);
   }

   public void export(final String filePath) {

      System.out.printf("FIT Encode Example Application - Protocol %d.%d Profile %.2f %s\n",
            Fit.PROTOCOL_VERSION_MAJOR,
            Fit.PROTOCOL_VERSION_MINOR,
            Fit.PROFILE_VERSION / 100.0,
            Fit.PROFILE_TYPE);

      FileEncoder encode;

      try {
         encode = new FileEncoder(new java.io.File(filePath), ProtocolVersion.V2_0);
      } catch (final FitRuntimeException e) {
         System.err.println("Error opening file ExampleSettings.fit");
         return;
      }

      //Generate FileIdMessage
      final FileIdMesg fileIdMesg = new FileIdMesg(); // Every FIT file MUST contain a 'File ID' message as the first message
      fileIdMesg.setManufacturer(Manufacturer.DYNASTREAM);
      fileIdMesg.setType(File.ACTIVITY);
      //fileIdMesg.setProduct(1000);
      // fileIdMesg.setSerialNumber(12345L);

//      final RecordMesg recordMesg = new RecordMesg();
//      final Field field = new Field("timestamp",
//            978538286,
//            0,
//            "position_lat",
//            "480457070",
//            "semicircles",
//            "position_long",
//            "-1259320222",
//            "semicircles");
//      recordMesg.addField(field);

      final ActivityMesg toto = new ActivityMesg();
      toto.setType(Activity.MANUAL);
      encode.write(toto); // Encode the FileIDMesg

      encode.write(fileIdMesg); // Encode the FileIDMesg

      //Generate UserProfileMesg
      final UserProfileMesg userProfileMesg = new UserProfileMesg();
      userProfileMesg.setGender(Gender.FEMALE);
      userProfileMesg.setWeight(63.1F);
      userProfileMesg.setAge((short) 99);
      userProfileMesg.setFriendlyName("TestUser");

      encode.write(userProfileMesg); // Encode the UserProfileMesg

      try {
         encode.close();
      } catch (final FitRuntimeException e) {
         System.err.println("Error closing encode.");
         return;
      }

      System.out.println("Encoded FIT file ExampleSettings.fit.");
   }
}
