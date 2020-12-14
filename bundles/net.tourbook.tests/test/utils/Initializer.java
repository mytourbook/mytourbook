/*******************************************************************************
 * Copyright (C) 2020 Frédéric Bard
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

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.tourbook.common.UI;
import net.tourbook.data.TourData;
import net.tourbook.device.garmin.GarminDeviceDataReader;
import net.tourbook.device.gpx.GPX_SAX_Handler;
import net.tourbook.importdata.DeviceData;

import org.xml.sax.SAXException;

public class Initializer {

   public static TourData importTour() {
      final SAXParser parser = Initializer.initializeParser();
      final DeviceData deviceData = new DeviceData();
      final HashMap<Long, TourData> newlyImportedTours = new HashMap<>();
      final HashMap<Long, TourData> alreadyImportedTours = new HashMap<>();
      final GarminDeviceDataReader deviceDataReader = new GarminDeviceDataReader();

      final String IMPORT_FILE_PATH = "/utils/files/LongsPeak-Manual.gpx"; //$NON-NLS-1$

      final InputStream gpx = Initializer.class.getResourceAsStream(IMPORT_FILE_PATH);

      final GPX_SAX_Handler handler = new GPX_SAX_Handler(
            deviceDataReader,
            IMPORT_FILE_PATH,
            deviceData,
            alreadyImportedTours,
            newlyImportedTours);

      try {
         if (parser != null) {
            parser.parse(gpx, handler);
         }
      } catch (SAXException | IOException e) {
         e.printStackTrace();
      }

      return Comparison.retrieveImportedTour(newlyImportedTours);
   }

   public static SAXParser initializeParser() {
      final SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser parser = null;
      try {
         parser = factory.newSAXParser();
         parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, UI.EMPTY_STRING);
         parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, UI.EMPTY_STRING);
      } catch (final ParserConfigurationException | SAXException e) {
         e.printStackTrace();
      }

      return parser;
   }
}
