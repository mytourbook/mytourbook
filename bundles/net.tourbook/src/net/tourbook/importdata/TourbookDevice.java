/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.importdata;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import net.tourbook.common.UI;
import net.tourbook.common.util.FileUtils;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;

public abstract class TourbookDevice implements IRawDataReader {

   protected static final String XML_START_ID                    = "<?xml";                                                    //$NON-NLS-1$
   protected static final String XML_HEADER                      = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";              //$NON-NLS-1$

   private static final String   SYS_PROP__CREATE_RANDOM_TOUR_ID = "createRandomTourId";                                       //$NON-NLS-1$
   private static final boolean  _isCreateRandomTourId           = System.getProperty(SYS_PROP__CREATE_RANDOM_TOUR_ID) != null;

   static {

      if (_isCreateRandomTourId) {

         Util.logSystemProperty_IsEnabled(TourbookDevice.class,
               SYS_PROP__CREATE_RANDOM_TOUR_ID,
               "Every imported tour has a unique tour ID"); //$NON-NLS-1$
      }
   }

   /**
    * Temperature scale when a device supports scaled temperature values. A value greater than 10
    * does not make sense for a tour program.
    * <p>
    * since version 10.11
    */
   /*
    * disabled when float was introduces in 11.after8
    */
//   public static final int   TEMPERATURE_SCALE      = 10;

   /**
    * Unique id for each device reader
    */
   public String deviceId;

   /**
    * Visible device name, e.g. HAC4, HAC5
    */
   public String visibleName;

   /**
    * File extension used when tour data are imported from a file
    */
   public String fileExtension;

   /**
    * Sort priority (since version 10.11), default will sort devices to the end.
    */
   public int    extensionSortPriority = Integer.MAX_VALUE;

// disabled in version 10.10, it seems to be not used anymore
//   /**
//    * <code>true</code> when this device reader can read from the device, <code>false</code>
//    * (default) when the device reader can only import from a file
//    */
//   public boolean   canReadFromDevice      = false;
//
//   /**
//    * when <code>true</code>, multiple files can be selected in the import, default is
//    * <code>false</code>
//    */
//   public boolean   canSelectMultipleFilesInImportDialog   = false;

   /**
    * When set to {@link RawDataManager#ADJUST_IMPORT_YEAR_IS_DISABLED} this is ignored otherwise
    * this year is used as the import year.
    */
   public int     importYear           = RawDataManager.ADJUST_IMPORT_YEAR_IS_DISABLED;

   /**
    * When <code>true</code> the tracks in one file will be merged into one track, a marker is
    * created for each track.
    */
   public boolean isMergeTracks        = false;

   /**
    * when <code>true</code> validate the checksum when importing data
    */
   public boolean isChecksumValidation = true;

   /**
    * A tour id will be created with elapsed time when <code>true</code>.
    */
   public boolean isCreateTourIdWithElapsedTime;

   /**
    * When <code>true</code> imported waypoints will be converted into {@link TourMarker}.
    */
   public boolean isConvertWayPoints;

   public TourbookDevice() {}

   public TourbookDevice(final String deviceName) {
      visibleName = deviceName;
   }

   public abstract String buildFileNameFromRawData(String rawDataFileName);

   /**
    * Check if the received data are correct for this device, Returns <code>true</code> when the
    * received data are correct for this device
    *
    * @param byteIndex
    *           index in the byte stream, this will be incremented when the return value is true
    * @param newByte
    *           received byte
    * @return Return <code>true</code> when the receive data are correct for this device
    */
   public abstract boolean checkStartSequence(int byteIndex, int newByte);

   /**
    * Creates a unique id for the tour, {@link TourData#createTimeSeries()} must be called ahead, to
    * create elapsed time.
    * <p>
    * Elapsed time is added to the tour id when {@link #isCreateTourIdWithElapsedTime} is
    * <code>true</code>.
    *
    * @param tourData
    * @param distanceSerie
    * @param defaultKey
    *           The default key is used when distance serie is not available.
    * @return Returns a unique key for a tour.
    */
   public String createUniqueId(final TourData tourData, final String defaultKey) {

      if (_isCreateRandomTourId) {

         final Double randomNumber = Double.valueOf(Math.random());

         //We remove the "0." as it creates issues when, later, parsing back into a long
         return randomNumber.toString().substring(2);
      }

      String uniqueKey;
      final float[] distanceSerie = tourData.getMetricDistanceSerie();

      if (isCreateTourIdWithElapsedTime) {

         /*
          * 25.5.2009: added elapsed time to the tour distance for the unique key because tour
          * export and import found a wrong tour when exporting was done with camouflage speed ->
          * this resulted in a NEW tour
          */
         final int tourElapsedTime = (int) tourData.getTourDeviceTime_Elapsed();

         if (distanceSerie == null) {
            uniqueKey = Integer.toString(tourElapsedTime);
         } else {

            final long tourDistance = (long) distanceSerie[(distanceSerie.length - 1)];

            uniqueKey = Long.toString(tourDistance + tourElapsedTime);
         }

      } else {

         /*
          * original version to create a tour id
          */
         if (distanceSerie == null) {
            uniqueKey = defaultKey;
         } else {
            uniqueKey = Integer.toString((int) distanceSerie[distanceSerie.length - 1]);
         }
      }

      return uniqueKey;
   }

   /**
    * @param tourData
    * @param tourDistance
    * @return Returns the legacy tour id with
    *         <p>
    *         <code>Integer.toString(tourDistance)</code>
    *         <p>
    *         as default, when elapsed time is not used as it was in the initial implementation.
    */
   public String createUniqueId_Legacy(final TourData tourData, final int tourDistance) {

      String uniqueKey;

      if (isCreateTourIdWithElapsedTime) {

         uniqueKey = Long.toString(tourDistance + tourData.getTourDeviceTime_Elapsed());

      } else {

         /*
          * This represents the original (1st) implementation without elapsed time.
          */

         uniqueKey = Integer.toString(tourDistance);
      }

      return uniqueKey;
   }

   /**
    * @param portName
    * @return returns the serial port parameters which are use to receive data from the device or
    *         <code>null</code> when data transfer from a device is not supported
    */
   public abstract SerialParameters getPortParameters(String portName);

   /**
    * Returns the number of bytes which will be checked in the startsequence. For a HAC4/5 this can
    * be set to 4 because the first 4 bytes of the input stream are always the characters AFRO
    *
    * @return
    */
   public abstract int getStartSequenceSize();

   /**
    * Check if the file is a valid device xml file.
    *
    * @param importFilePath
    * @param deviceTag
    *           The deviceTag starts on the second line of a xml file.
    * @return Returns <code>true</code> when the file contains content with the requested tag.
    */
   protected boolean isValidXMLFile(final String importFilePath, final String deviceTag) {

      return isValidXMLFile(importFilePath, deviceTag, false);
   }

   /**
    * Check if the file is a valid device xml file.
    *
    * @param importFilePath
    * @param deviceTag
    * @param isRemoveBOM
    *           When <code>true</code> the BOM (Byte Order Mark) is removed from the file.
    * @return Returns <code>true</code> when the file contains content with the requested tag.
    */
   protected boolean isValidXMLFile(final String importFilePath, final String deviceTag, final boolean isRemoveBOM) {

      final String deviceTagLower = deviceTag.toLowerCase();

      try (FileInputStream inputStream = new FileInputStream(importFilePath);
            BufferedReader fileReader = new BufferedReader(new InputStreamReader(inputStream, UI.UTF_8))) {

         if (isRemoveBOM) {

            try {
               FileUtils.consumeBOM(inputStream, UI.UTF_8);
            } catch (final IOException e) {
               // just ignore it
            }
         }

         String line = fileReader.readLine();
         if (line == null || line.toLowerCase().contains(XML_START_ID) == false) {
            return false;
         }

         final int maxLines = 100;
         int lineCounter = 0;

         while (true) {

            if (line.toLowerCase().contains(deviceTagLower)) {
               return true;
            }

            line = fileReader.readLine();

            if (line == null || lineCounter++ > maxLines) {
               return false;
            }
         }

      } catch (final Exception e1) {
         StatusUtil.log(e1);
      }

      return true;
   }

   public void setConvertWayPoints(final boolean isConvertWayPoints) {
      this.isConvertWayPoints = isConvertWayPoints;
   }

   public void setCreateTourIdWithTime(final boolean isCreateTourIdWithTime) {
      this.isCreateTourIdWithElapsedTime = isCreateTourIdWithTime;
   }

   public void setImportYear(final int importYear) {
      this.importYear = importYear;
   }

   public void setIsChecksumValidation(final boolean isChecksumValidation) {
      this.isChecksumValidation = isChecksumValidation;
   }

   public void setMergeTracks(final boolean isMergeTracks) {
      this.isMergeTracks = isMergeTracks;
   }

   @Override
   public String toString() {
      return "TourbookDevice [deviceId=" //$NON-NLS-1$
            + deviceId
            + ", visibleName=" //$NON-NLS-1$
            + visibleName
            + ", fileExtension=" //$NON-NLS-1$
            + fileExtension
            + ", extensionSortPriority=" //$NON-NLS-1$
            + extensionSortPriority
            + "]"; //$NON-NLS-1$
   }

   public String userConfirmationMessage() {
      return UI.EMPTY_STRING;
   }

   public boolean userConfirmationRequired() {
      return false;
   }

}
