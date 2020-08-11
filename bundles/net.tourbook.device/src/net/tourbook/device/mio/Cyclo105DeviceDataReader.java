/*******************************************************************************
 * Copyright (C) 2020 Frédéric Bard and Contributors
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
package net.tourbook.device.mio;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourData;
import net.tourbook.device.InvalidDeviceSAXException;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

public class Cyclo105DeviceDataReader extends TourbookDevice {

   private static final String TAG_MAGELLAN    = UI.SYMBOL_LESS_THAN + Cyclo105SAXHandler.TAG_ROOT_MAGELLAN + UI.SYMBOL_GREATER_THAN;
   private static final String TAG_TRACKPOINTS = UI.SYMBOL_LESS_THAN + Cyclo105SAXHandler.TAG_ROOT_TRACKPOINTS + UI.SYMBOL_GREATER_THAN;

   public Cyclo105DeviceDataReader() {}

   @Override
   public String buildFileNameFromRawData(final String rawDataFileName) {
      return null;
   }

   @Override
   public boolean checkStartSequence(final int byteIndex, final int newByte) {
      return false;
   }

   @Override
   public String getDeviceModeName(final int profileId) {
      return null;
   }

   @Override
   public SerialParameters getPortParameters(final String portName) {
      return null;
   }

   @Override
   public int getStartSequenceSize() {
      return -1;
   }

   @Override
   public int getTransferDataSize() {
      return -1;
   }

   /**
    * Check if the file is a valid Mio Cyclo 105 ACT file by checking some tags.
    *
    * @param importFilePath
    * @return Returns <code>true</code> when the file contains Mio Cyclo 105 ACT content.
    */
   private boolean isValidCyclo105File(final String importFilePath) {

      try (final FileInputStream inputStream = new FileInputStream(importFilePath);
            final BufferedReader fileReader = new BufferedReader(new InputStreamReader(inputStream, UI.UTF_8))) {

         String line = fileReader.readLine();
         if (StringUtils.isNullOrEmpty(line) || line.toLowerCase().startsWith(XML_START_ID) == false) {
            return false;
         }

         line = fileReader.readLine();
         if (StringUtils.isNullOrEmpty(line) || line.equalsIgnoreCase(TAG_MAGELLAN) == false) {
            return false;
         }

         while ((line = fileReader.readLine()) != null) {

            if (line.trim().equalsIgnoreCase(TAG_TRACKPOINTS)) {
               return true;
            }
         }

      } catch (final Exception e) {

         StatusUtil.log(e);
      }

      return false;
   }

   @Override
   public boolean processDeviceData(final String importFilePath,
                                    final DeviceData deviceData,
                                    final HashMap<Long, TourData> alreadyImportedTours,
                                    final HashMap<Long, TourData> newlyImportedTours) {

      if (isValidCyclo105File(importFilePath) == false) {
         return false;
      }

      final Cyclo105SAXHandler saxHandler =
            new Cyclo105SAXHandler(
                  this,
                  importFilePath,
                  alreadyImportedTours,
                  newlyImportedTours);

      try (FileInputStream inputStream = new FileInputStream(importFilePath)) {

         final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

         parser.parse(inputStream, saxHandler);

      } catch (final InvalidDeviceSAXException e) {
         StatusUtil.log(e);
         return false;
      } catch (final Exception e) {
         StatusUtil.log("Error parsing file: " + importFilePath, e); //$NON-NLS-1$
         return false;
      } finally {

         saxHandler.dispose();
      }

      return saxHandler.isImported();
   }

   @Override
   public boolean validateRawData(final String fileName) {
      return isValidCyclo105File(fileName);
   }
}
