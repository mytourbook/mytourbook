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
import com.garmin.fit.DeviceIndex;
import com.garmin.fit.DeviceInfoMesg;
import com.garmin.fit.Event;
import com.garmin.fit.EventMesg;
import com.garmin.fit.EventType;
import com.garmin.fit.File;
import com.garmin.fit.FileEncoder;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Fit;
import com.garmin.fit.FitRuntimeException;
import com.garmin.fit.Gender;
import com.garmin.fit.LapMesg;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.Mesg;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.Sport;
import com.garmin.fit.UserProfileMesg;
import com.garmin.fit.util.SemicirclesConverter;

import java.nio.ByteBuffer;
import java.time.Period;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.export.Activator;

import org.osgi.framework.Version;

public class FitExporter {

   private TourData _tourData;

   private static byte[] convertUUIDToBytes(final UUID uuid) {

      final ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
      byteBuffer.putLong(uuid.getMostSignificantBits());
      byteBuffer.putLong(uuid.getLeastSignificantBits());
      return byteBuffer.array();
   }

   private static void createFitFile(final List<Mesg> messages,
                                     final String filename,
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
         fileEncoder = new FileEncoder(new java.io.File(filename), Fit.ProtocolVersion.V2_0);
      } catch (final FitRuntimeException e) {
         StatusUtil.log(e);
         return;
      }

      final UserProfileMesg userProfileMesg = createUserProfile();
      if (userProfileMesg != null) {
         fileEncoder.write(userProfileMesg);
      }

      // Every FIT file MUST contain a File ID message
      final FileIdMesg fileIdMesg = new FileIdMesg();
      fileIdMesg.setType(File.ACTIVITY);
      fileIdMesg.setTimeCreated(startTime);
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

   private static UserProfileMesg createUserProfile() {

      final TourPerson activePerson = TourbookPlugin.getActivePerson();
      if (activePerson == null) {
         return null;
      }

      final UserProfileMesg userProfileMesg = new UserProfileMesg();
      userProfileMesg.setGender(activePerson.getGender() == 0 ? Gender.MALE : Gender.FEMALE);
      userProfileMesg.setWeight(activePerson.getWeight());
      final int age = Period.between(TimeTools.now().toLocalDate(), TimeTools.getZonedDateTime(activePerson.getBirthDay()).toLocalDate()).getYears();
      userProfileMesg.setAge((short) age);
      userProfileMesg.setFriendlyName(activePerson.getName());

      return userProfileMesg;
   }

   // Official documentation: https://developer.garmin.com/fit/cookbook/
   public void export(final TourData tourData, final String exportFilePath) {

      _tourData = tourData;
      final List<Mesg> messages = new ArrayList<>();

      // The starting timestamp for the activity
      final DateTime startTime = new DateTime(Date.from(_tourData.getTourStartTime().toInstant()));

      // Timer Events are a BEST PRACTICE for FIT ACTIVITY files
      final EventMesg eventMesg = new EventMesg();
      eventMesg.setTimestamp(startTime);
      eventMesg.setEvent(Event.TIMER);
      eventMesg.setEventType(EventType.START);
      messages.add(eventMesg);

      // Create the Developer Id message for the developer data fields.
      final DeveloperDataIdMesg developerIdMesg = new DeveloperDataIdMesg();

      // It is a BEST PRACTICE to reuse the same Guid for all FIT files created by your platform
      final byte[] appId = convertUUIDToBytes(UUID.fromString("e1b60f54-e7cb-46d9-88df-ced1fe7ecf0f")); //$NON-NLS-1$
      for (int i = 0; i < appId.length; i++) {
         developerIdMesg.setApplicationId(i, appId[i]);
      }

      developerIdMesg.setDeveloperDataIndex((short) 0);
      final Version softwareVersion = Activator.getDefault().getVersion();
      final Float version = Float.valueOf(softwareVersion.getMajor() + UI.SYMBOL_DOT + softwareVersion.getMinor());
      developerIdMesg.setApplicationVersion((long) (version * 100));
      messages.add(developerIdMesg);

      // Every FIT ACTIVITY file MUST contain Record messages
      final int previousTimeSerieValue = 0;

      int previousTime = 0;
      DateTime timestamp = null;
      // Create one hour (3600 seconds) of Record data
      for (int index = 0; index < _tourData.timeSerie.length; ++index) {

         startTime.add(_tourData.timeSerie[index] - previousTime);
         previousTime = _tourData.timeSerie[index];
         timestamp = new DateTime(startTime);
         // Create a new Record message and set the timestamp
         final RecordMesg recordMesg = new RecordMesg();
         recordMesg.setTimestamp(timestamp);

         setDataSerieValue(index, recordMesg);

         // Write the Record message to the output stream
         messages.add(recordMesg);

         // Increment the timestamp by the number of seconds between the previous
         // timestamp and the current one
         timestamp.add(_tourData.timeSerie[index] - previousTimeSerieValue);
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
      // sessionMesg.setTimestamp(timestamp);
      sessionMesg.setStartTime(startTime);
      sessionMesg.setTotalElapsedTime((float) _tourData.getTourDeviceTime_Elapsed());
      sessionMesg.setTotalTimerTime((float) _tourData.getTourDeviceTime_Recorded());
      //todo fb do a map function with the tour type and by default use generic
      sessionMesg.setSport(Sport.STAND_UP_PADDLEBOARDING);
      sessionMesg.setFirstLapIndex(0);
      sessionMesg.setNumLaps(_tourData.getTourMarkers().size());
      setAvgValues(sessionMesg);
      messages.add(sessionMesg);

      // Every FIT ACTIVITY file MUST contain EXACTLY one Activity message
      final ActivityMesg activityMesg = new ActivityMesg();
      // activityMesg.setTimestamp(timestamp);
      activityMesg.setNumSessions(1);
      messages.add(activityMesg);

      createFitFile(messages, exportFilePath, startTime, version);
   }

   private void setAvgValues(final SessionMesg sessionMesg) {

      sessionMesg.setTotalCalories(_tourData.getCalories());
      sessionMesg.setAvgCadence((short) _tourData.getAvgCadence());
      sessionMesg.setAvgHeartRate((short) _tourData.getAvgPulse());
      sessionMesg.setAvgPower((int) _tourData.getPower_Avg());
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
         recordMesg.setSpeed(speedSerie[index]);
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
   }
}
