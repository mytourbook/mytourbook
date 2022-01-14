/*******************************************************************************
 * Copyright (C) 2020, 2022 Frédéric Bard
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
package utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.persistence.Persistence;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.tourbook.common.UI;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.device.garmin.GarminTCX_DeviceDataReader;
import net.tourbook.device.gpx.GPX_SAX_Handler;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.ImportState_File;
import net.tourbook.importdata.ImportState_Process;

import org.xml.sax.SAXException;

public class Initializer {

   public static TourData createManualTour() {

      final TourData manualTour = new TourData();
      manualTour.setTourStartTime(2022, 1, 3, 17, 16, 0);
      manualTour.setTourDistance(10);
      manualTour.setTourDeviceTime_Elapsed(3600);
      manualTour.setTourTitle("Manual Tour"); //$NON-NLS-1$

      final TourType tourType = new TourType();
      tourType.setName("Running"); //$NON-NLS-1$
      manualTour.setTourType(tourType);

      return manualTour;
   }

   public static TourData importTour() {

      final SAXParser parser = Initializer.initializeParser();
      final DeviceData deviceData = new DeviceData();
      final HashMap<Long, TourData> newlyImportedTours = new HashMap<>();
      final HashMap<Long, TourData> alreadyImportedTours = new HashMap<>();
      final GarminTCX_DeviceDataReader deviceDataReader = new GarminTCX_DeviceDataReader();

      final String IMPORT_FILE_PATH = "/utils/files/LongsPeak-Manual.gpx"; //$NON-NLS-1$

      final InputStream gpx = Initializer.class.getResourceAsStream(IMPORT_FILE_PATH);

      final GPX_SAX_Handler handler = new GPX_SAX_Handler(

            IMPORT_FILE_PATH,
            deviceData,
            alreadyImportedTours,
            newlyImportedTours,

            new ImportState_File(),
            new ImportState_Process(),

            deviceDataReader);

      if (parser != null) {
         try {
            parser.parse(gpx, handler);
         } catch (SAXException | IOException e) {
            e.printStackTrace();
         }
      }

      return Comparison.retrieveImportedTour(newlyImportedTours);
   }

   public static void initializeDatabase() {

      Persistence.createEntityManagerFactory("tourdatabase").createEntityManager(); //$NON-NLS-1$
   }

   public static SAXParser initializeParser() {

      try {

         final SAXParserFactory factory = SAXParserFactory.newInstance();
         final SAXParser parser = factory.newSAXParser();
         parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, UI.EMPTY_STRING);
         parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, UI.EMPTY_STRING);
         return parser;
      } catch (final ParserConfigurationException | SAXException e) {
         e.printStackTrace();
      }

      return null;
   }
}
