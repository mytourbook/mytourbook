/*******************************************************************************
 * Copyright (C) 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.device.mt;

import java.time.ZonedDateTime;
import java.util.Map;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamSource;

import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.importdata.TourbookDevice;

/**
 * Import tours which are exported from MyTourbook
 */
public class MT_STAXHandler {

   private static final String TAG_MT   = "mt";
   private static final String TAG_TOUR = "tour";

   private Map<Long, TourData> _alreadyImportedTours;
   private Map<Long, TourData> _newlyImportedTours;

   private TourbookDevice      _device;

   private boolean             _isImported;

   private TourData            _tourData;

   public MT_STAXHandler(final TourbookDevice deviceDataReader,
                         final String importFilePath,
                         final Map<Long, TourData> alreadyImportedTours,
                         final Map<Long, TourData> newlyImportedTours) throws XMLStreamException {

      _device = deviceDataReader;

      _alreadyImportedTours = alreadyImportedTours;
      _newlyImportedTours = newlyImportedTours;

      parseXML(importFilePath);
   }

   public void dispose() {

   }

   private void finalizeTour() {

      // create tour id
      final String uniqueId = _device.createUniqueId(_tourData, Util.UNIQUE_ID_SUFFIX_MT);
      final Long tourId = _tourData.createTourId(uniqueId);

      // check if the tour is already imported
      if (_alreadyImportedTours.containsKey(tourId) == false) {

         // add new tour to other tours
         _newlyImportedTours.put(tourId, _tourData);

         // create additional data
//         _tourData.finalizeTour_TimerPauses(_pausedTime_Start, _pausedTime_End, null);
//         _tourData.setTourDeviceTime_Recorded(_tourData.getTourDeviceTime_Elapsed() - _tourData.getTourDeviceTime_Paused());
//
//         _tourData.computeAltitudeUpDown();
//         _tourData.computeTourMovingTime();
//         _tourData.computeComputedValues();
      }

      _tourData = null;

      _isImported = true;
   }

   /**
    * @return Returns <code>true</code> when a tour was imported
    */
   public boolean isImported() {
      return _isImported;
   }

   private void parseXML(final String importFilePath) throws FactoryConfigurationError, XMLStreamException {

      final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
      final XMLEventReader eventReader = inputFactory.createXMLEventReader(new StreamSource("file:" + importFilePath)); //$NON-NLS-1$

      while (eventReader.hasNext()) {

         final XMLEvent xmlEvent = eventReader.nextEvent();

         if (xmlEvent.isStartElement()) {

            final StartElement startElement = xmlEvent.asStartElement();
            final String elementName = startElement.getName().getLocalPart();

            switch (elementName) {

            case TAG_TOUR:
               parseXML_10_Tour(eventReader, startElement);
               break;
            }
         }

         if (xmlEvent.isEndElement()) {

            final String elementName = xmlEvent.asEndElement().getName().getLocalPart();

            switch (elementName) {

            case TAG_MT:
               finalizeTour();
               break;
            }
         }
      }
   }

   private void parseXML_10_Tour(final XMLEventReader eventReader, final StartElement startElement) throws XMLStreamException {

      _tourData = new TourData();

      parseXML_12_Tour_Attributes(startElement);

      while (eventReader.hasNext()) {

         final XMLEvent xmlEvent = eventReader.nextEvent();

         if (xmlEvent.isStartElement()) {

            final StartElement startElement2 = xmlEvent.asStartElement();
            final String elementName = startElement2.getName().getLocalPart();

// SET_FORMATTING_OFF

            switch (elementName) {

            case "tourDescription":    _tourData.setTourDescription(    eventReader.nextEvent().asCharacters().getData());    break;
            case "tourTitle":          _tourData.setTourTitle(          eventReader.nextEvent().asCharacters().getData());    break;
            case "tourStartPlace":     _tourData.setTourStartPlace(     eventReader.nextEvent().asCharacters().getData());    break;
            case "tourEndPlace":       _tourData.setTourEndPlace(       eventReader.nextEvent().asCharacters().getData());    break;
            case "weather":            _tourData.setWeather(            eventReader.nextEvent().asCharacters().getData());    break;
            case "importFileName":     _tourData.setTourImportFilePath( eventReader.nextEvent().asCharacters().getData());    break;
            case "importFilePath":     _tourData.setTourImportFileName( eventReader.nextEvent().asCharacters().getData());    break;

            }

// SET_FORMATTING_ON

         }

         if (xmlEvent.isEndElement()) {

            if (TAG_TOUR.equals(xmlEvent.asEndElement().getName().getLocalPart())) {

               // </tour>
               break;
            }
         }
      }
   }

   private void parseXML_12_Tour_Attributes(final StartElement startElement) {

      startElement.getAttributes().forEachRemaining(attribute -> {

         final String value = attribute.getValue();

// SET_FORMATTING_OFF

         final String attributeName = attribute.getName().getLocalPart();

         switch (attributeName) {

         case "tourStartTime":               _tourData.setTourStartTime(            ZonedDateTime.parse(value));     break;
         case "tourComputedTime_Moving":     _tourData.setTourComputedTime_Moving(  Util.parseInt_0(value));         break;
         case "tourDeviceTime_Elapsed":      _tourData.setTourDeviceTime_Elapsed(   Util.parseLong_0(value));        break;
         case "tourDeviceTime_Paused":       _tourData.setTourDeviceTime_Paused(    Util.parseLong_0(value));        break;
         case "tourDeviceTime_Recorded":     _tourData.setTourDeviceTime_Recorded(  Util.parseLong_0(value));        break;

//         case "":  _tourData.          break;

//               case "avgAltitudeChange":  _tourData.          break;
//               case "avgCadence":  _tourData.          break;
//               case "avgPulse":  _tourData.          break;
//               case "battery_Percentage_End":  _tourData.          break;
//               case "battery_Percentage_Start":  _tourData.          break;
//               case "bodyFat":  _tourData.          break;
//               case "bodyWeight":  _tourData.          break;
//               case "
//               case "cadenceMultiplier":  _tourData.          break;
//               case "cadenceZone_FastTime":  _tourData.          break;
//               case "cadenceZone_SlowTime":  _tourData.          break;
//               case "cadenceZones_DelimiterValue":  _tourData.          break;
//               case "
//               case "calories":  _tourData.          break;
//               case "conconiDeflection":  _tourData.          break;
//               case "
//               case "dateTimeCreated":  _tourData.          break;
//               case "dateTimeModified":  _tourData.          break;
//               case "
//               case "deviceFirmwareVersion":  _tourData.          break;
//               case "deviceModeName":  _tourData.          break;
//               case "devicePluginName":  _tourData.          break;
//               case "deviceTimeInterval":  _tourData.          break;
//               case "dpTolerance":  _tourData.          break;
//               case "frontShiftCount":  _tourData.          break;
//               case "hasGeoData":  _tourData.          break;
//               case "
//               case "hrZone0":  _tourData.          break;
//               case "hrZone1":  _tourData.          break;
//               case "hrZone2":  _tourData.          break;
//               case "hrZone3":  _tourData.          break;
//               case "hrZone4":  _tourData.          break;
//               case "hrZone5":  _tourData.          break;
//               case "hrZone6":  _tourData.          break;
//               case "hrZone7":  _tourData.          break;
//               case "hrZone8":  _tourData.          break;
//               case "hrZone9":  _tourData.          break;
//               case "
//               case "isDistanceFromSensor":  _tourData.          break;
//               case "isPowerSensorPresent":  _tourData.          break;
//               case "isPulseSensorPresent":  _tourData.          break;
//               case "isStrideSensorPresent":  _tourData.          break;
//               case "isWeatherDataFromProvider":  _tourData.          break;
//               case "
//               case "maxAltitude":  _tourData.          break;
//               case "maxPace":  _tourData.          break;
//               case "maxPulse":  _tourData.          break;
//               case "maxSpeed":  _tourData.          break;
//               case "
//               case "mergedAltitudeOffset":  _tourData.          break;
//               case "mergedTourTimeOffset":  _tourData.          break;
//               case "
//               case "numberOfHrZones":  _tourData.          break;
//               case "numberOfPhotos":  _tourData.          break;
//               case "numberOfTimeSlices":  _tourData.          break;
//               case "
//               case "photoTimeAdjustment":  _tourData.          break;
//               case "
//               case "power_Avg":  _tourData.          break;
//               case "power_AvgLeftPedalSmoothness":  _tourData.          break;
//               case "power_AvgLeftTorqueEffectiveness":  _tourData.          break;
//               case "power_AvgRightPedalSmoothness":  _tourData.          break;
//               case "power_AvgRightTorqueEffectiveness":  _tourData.          break;
//               case "
//               case "power_FTP":  _tourData.          break;
//               case "power_IntensityFactor":  _tourData.          break;
//               case "power_Max":  _tourData.          break;
//               case "power_Normalized":  _tourData.          break;
//               case "power_PedalLeftRightBalance":  _tourData.          break;
//               case "power_TotalWork":  _tourData.          break;
//               case "power_TrainingStressScore":  _tourData.          break;
//               case "
//               case "rearShiftCount":  _tourData.          break;
//               case "restPulse":  _tourData.          break;
//               case "
//               case "runDyn_StanceTimeBalance_Avg":  _tourData.          break;
//               case "runDyn_StanceTimeBalance_Max":  _tourData.          break;
//               case "runDyn_StanceTimeBalance_Min":  _tourData.          break;
//               case "runDyn_StanceTime_Avg":  _tourData.          break;
//               case "runDyn_StanceTime_Max":  _tourData.          break;
//               case "runDyn_StanceTime_Min":  _tourData.          break;
//               case "runDyn_StepLength_Avg":  _tourData.          break;
//               case "runDyn_StepLength_Max":  _tourData.          break;
//               case "runDyn_StepLength_Min":  _tourData.          break;
//               case "runDyn_VerticalOscillation_Avg":  _tourData.          break;
//               case "runDyn_VerticalOscillation_Max":  _tourData.          break;
//               case "runDyn_VerticalOscillation_Min":  _tourData.          break;
//               case "runDyn_VerticalRatio_Avg":  _tourData.          break;
//               case "runDyn_VerticalRatio_Max":  _tourData.          break;
//               case "runDyn_VerticalRatio_Min":  _tourData.          break;
//               case "
//               case "startAltitude":  _tourData.          break;
//               case "startDistance":  _tourData.          break;
//               case "startPulse":  _tourData.          break;
//               case "
//               case "surfing_IsMinDistance":  _tourData.          break;
//               case "surfing_MinDistance":  _tourData.          break;
//               case "surfing_MinSpeed_StartStop":  _tourData.          break;
//               case "surfing_MinSpeed_Surfing":  _tourData.          break;
//               case "surfing_MinTimeDuration":  _tourData.          break;
//               case "surfing_NumberOfEvents":  _tourData.          break;
//               case "
//               case "temperatureScale":  _tourData.          break;
//               case "
//               case "tourAltDown":  _tourData.          break;
//               case "tourAltUp":  _tourData.          break;
//               case "tourDistance":  _tourData.          break;
//               case "
//               case "training_TrainingEffect_Aerob":  _tourData.          break;
//               case "training_TrainingEffect_Anaerob":  _tourData.          break;
//               case "training_TrainingPerformance":  _tourData.          break;
//               case "
//               case "weather_Clouds":  _tourData.          break;
//               case "weather_Humidity":  _tourData.          break;
//               case "weather_Precipitation":  _tourData.          break;
//               case "weather_Pressure":  _tourData.          break;
//               case "weather_Snowfall":  _tourData.          break;
//               case "weather_Temperature_Average":  _tourData.          break;
//               case "weather_Temperature_Average_Device":  _tourData.          break;
//               case "weather_Temperature_Max":  _tourData.          break;
//               case "weather_Temperature_Max_Device":  _tourData.          break;
//               case "weather_Temperature_Min":  _tourData.          break;
//               case "weather_Temperature_Min_Device":  _tourData.          break;
//               case "weather_Temperature_WindChill":  _tourData.          break;
//               case "weather_Wind_Direction":  _tourData.          break;
//               case "weather_Wind_Speed":  _tourData.          break;

// SET_FORMATTING_ON

         default:
            break;
         }

      });
   }

}
