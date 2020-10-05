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
package net.tourbook.device.suunto;

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

public class SuuntoQuestDeviceDataReader extends TourbookDevice {

   private static final String DOCTYPE_XML        = "<!doctype xml>"; //$NON-NLS-1$
   private static final String MOVESCOUNT_TAG     = "<movescount";    //$NON-NLS-1$
   private static final String SUUNTO_TAG_SAMPLES = "<samples>";      //$NON-NLS-1$

   public SuuntoQuestDeviceDataReader() {}

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
    * Check if the file is a valid Suunto Quest XML file by checking some tags.
    *
    * @param importFilePath
    * @return Returns <code>true</code> when the file contains Suunto content.
    */
   private boolean isValidSuuntoXMLFile(final String importFilePath) {

      try (final FileInputStream inputStream = new FileInputStream(importFilePath);
            final BufferedReader fileReader = new BufferedReader(new InputStreamReader(inputStream, UI.UTF_8))) {

         String line = fileReader.readLine();
         if (StringUtils.isNullOrEmpty(line) || line.toLowerCase().startsWith(XML_START_ID) == false) {
            return false;
         }

         line = fileReader.readLine();
         if (StringUtils.isNullOrEmpty(line) || line.toLowerCase().startsWith(DOCTYPE_XML) == false) {
            return false;
         }

         line = fileReader.readLine();
         if (StringUtils.isNullOrEmpty(line) || line.toLowerCase().startsWith(MOVESCOUNT_TAG) == false) {
            return false;
         }

         while ((line = fileReader.readLine()) != null) {

            if (line.trim().toLowerCase().startsWith(SUUNTO_TAG_SAMPLES)) {
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

      if (isValidSuuntoXMLFile(importFilePath) == false) {
         return false;
      }

      final SuuntoQuestSAXHandler saxHandler =
            new SuuntoQuestSAXHandler(
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
      return isValidSuuntoXMLFile(fileName);
   }
}
