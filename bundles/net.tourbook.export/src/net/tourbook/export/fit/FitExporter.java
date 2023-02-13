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

import com.garmin.fit.FileEncoder;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Fit;
import com.garmin.fit.Fit.ProtocolVersion;
import com.garmin.fit.FitRuntimeException;
import com.garmin.fit.Gender;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.UserProfileMesg;

//todo fb: Add unit test -> convert to csv and use it for comparison
public class FitExporter {

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
      //fileIdMesg.setType(File.SETTINGS);
      //fileIdMesg.setProduct(1000);
      // fileIdMesg.setSerialNumber(12345L);

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
