/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamSource;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.Util;
import net.tourbook.common.util.XmlUtils;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.math.Fmath;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

/**
 * This Suunto importer is implemented with info from
 * <p>
 * <a href="http://wiki.oldhu.com/doku.php?id=suunto_moveslink2_xml_file_format"
 * >http://wiki.oldhu.com/doku.php?id=suunto_moveslink2_xml_file_format</a>
 */
class Suunto3_STAXHandler {

   private static final double           RADIANT_TO_DEGREE     = 57.2957795131;

   private static final SimpleDateFormat TIME_FORMAT           = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");     //$NON-NLS-1$
   private static final SimpleDateFormat TIME_FORMAT_SSSZ      = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); //$NON-NLS-1$
   private static final SimpleDateFormat TIME_FORMAT_RFC822    = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");       //$NON-NLS-1$
   private static final SimpleDateFormat TIME_FORMAT_LOCAL     = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");        //$NON-NLS-1$

   private static final String           SAMPLE_TYPE_GPS_BASE  = "gps-base";                                           //$NON-NLS-1$
   private static final String           SAMPLE_TYPE_GPS_TINY  = "gps-tiny";                                           //$NON-NLS-1$
   private static final String           SAMPLE_TYPE_GPS_SMALL = "gps-small";                                          //$NON-NLS-1$
   private static final String           SAMPLE_TYPE_PERIODIC  = "periodic";                                           //$NON-NLS-1$

   // root tags
   private static final String TAG_DEVLOG         = "DeviceLog"; //$NON-NLS-1$
   private static final String TAG_DEVLOG_DEVICE  = "Device";    //$NON-NLS-1$
   private static final String TAG_DEVLOG_HEADER  = "Header";    //$NON-NLS-1$
   private static final String TAG_DEVLOG_SAMPLES = "Samples";   //$NON-NLS-1$

   // header tags
   private static final String TAG_HEADER_BATTERY_CHARGE          = "BatteryCharge";        //$NON-NLS-1$
   private static final String TAG_HEADER_BATTERY_CHARGE_AT_START = "BatteryChargeAtStart"; //$NON-NLS-1$
   private static final String TAG_HEADER_ENERGY                  = "Energy";               //$NON-NLS-1$
   private static final String TAG_HEADER_DATETIME                = "DateTime";             //$NON-NLS-1$
   private static final String TAG_HEADER_PEAK_TRAINING_EFFECT    = "PeakTrainingEffect";   //$NON-NLS-1$

   // device tags
   private static final String TAG_DEVICE_SW   = "SW";   //$NON-NLS-1$
   private static final String TAG_DEVICE_NAME = "Name"; //$NON-NLS-1$

   // sample tags
   private static final String TAG_SAMPLE                   = "Sample";           //$NON-NLS-1$
   private static final String TAG_SAMPLE_ALTITUDE          = "Altitude";         //$NON-NLS-1$
   private static final String TAG_SAMPLE_CADENCE           = "Cadence";          //$NON-NLS-1$
   private static final String TAG_SAMPLE_DISTANCE          = "Distance";         //$NON-NLS-1$
   private static final String TAG_SAMPLE_EVENTS            = "Events";           //$NON-NLS-1$
   private static final String TAG_SAMPLE_HR                = "HR";               //$NON-NLS-1$
   private static final String TAG_SAMPLE_LAP               = "Lap";              //$NON-NLS-1$
   private static final String TAG_SAMPLE_LATITUDE          = "Latitude";         //$NON-NLS-1$
   private static final String TAG_SAMPLE_LONGITUDE         = "Longitude";        //$NON-NLS-1$
   private static final String TAG_SAMPLE_PAUSE             = "Pause";            //$NON-NLS-1$
   private static final String TAG_SAMPLE_PERFORMANCE_LEVEL = "PerformanceLevel"; //$NON-NLS-1$
   private static final String TAG_SAMPLE_STATE             = "State";            //$NON-NLS-1$
   private static final String TAG_SAMPLE_TEMPERATURE       = "Temperature";      //$NON-NLS-1$
   private static final String TAG_SAMPLE_TIME              = "Time";             //$NON-NLS-1$
   private static final String TAG_SAMPLE_TYPE              = "SampleType";       //$NON-NLS-1$
   private static final String TAG_SAMPLE_UTC               = "UTC";              //$NON-NLS-1$

   static {

      final TimeZone utc = TimeZone.getTimeZone("UTC"); //$NON-NLS-1$

      TIME_FORMAT.setTimeZone(utc);
      TIME_FORMAT_SSSZ.setTimeZone(utc);
      TIME_FORMAT_RFC822.setTimeZone(utc);

      // TIME_FORMAT_LOCAL
      // For indoor activities, even though the time is provided in the UTC element
      // is the actual recorded local time.
   }
   //
   private Map<Long, TourData> _alreadyImportedTours;
   private Map<Long, TourData> _newlyImportedTours;
   private TourbookDevice      _device;
   private String              _importFilePath;
   //
   private TimeData            _sampleData;

   private ArrayList<TimeData> _sampleList       = new ArrayList<>();
   private TimeData            _gpsData;

   private ArrayList<TimeData> _gpsList          = new ArrayList<>();
   private TimeData            _markerData;

   private ArrayList<TimeData> _markerList       = new ArrayList<>();

   private List<Long>          _pausedTime_Start = new ArrayList<>();
   private List<Long>          _pausedTime_End   = new ArrayList<>();

   private boolean             _isImported;
   private String              _currentSampleType;
   private long                _currentUtcTime;
   private long                _currentTime;

   private long                _prevSampleTime;

   private boolean             _isInEvents;

   private float               _tourPeakTrainingEffect;
   private float               _tourPerformanceLevel;

   private int                 _tourCalories;

   private short               _tourBatteryPercentageStart;
   private short               _tourBatteryPercentageEnd;

   /**
    * This time is used when a time is not available.
    */
   private long                _tourStartTime;
   private String              _tourDeviceSW;

   private String              _tourDeviceName;

   Suunto3_STAXHandler(final TourbookDevice deviceDataReader,
                              final String importFilePath,
                              final Map<Long, TourData> alreadyImportedTours,
                              final Map<Long, TourData> newlyImportedTours) throws XMLStreamException {

      _device = deviceDataReader;
      _importFilePath = importFilePath;
      _alreadyImportedTours = alreadyImportedTours;
      _newlyImportedTours = newlyImportedTours;

      parseXML(importFilePath);
   }

   void dispose() {

      _sampleList.clear();
      _gpsList.clear();
      _markerList.clear();
      _pausedTime_Start.clear();
      _pausedTime_End.clear();
   }

   private void finalizeSample() {

      final long sampleTime;
      if (_currentUtcTime == Long.MIN_VALUE &&
            _currentTime != Long.MIN_VALUE) {

         if (!_sampleList.isEmpty() && _sampleList.get(0).absoluteTime != Long.MIN_VALUE) {
            sampleTime = ((_sampleList.get(0).absoluteTime / 1000) + _currentTime) * 1000;
         } else {
            sampleTime = _tourStartTime;
         }

      } else {

         /*
          * Remove milliseconds because this can cause wrong data. Position of a marker can be at
          * the wrong second and multiple samples can have the same second but other milliseconds.
          */
         sampleTime = _currentUtcTime / 1000 * 1000;
      }

      /*
       * A lap do not contain a sample type
       */
      if (_markerData.marker == 1) {

         // set virtual time if time is not available
         _markerData.absoluteTime = sampleTime;

         _markerList.add(_markerData);

      } else {

         if (_currentSampleType != null) {

            if (_currentSampleType.equals(SAMPLE_TYPE_PERIODIC)) {

               /*
                * Skip samples with the same time in seconds
                */
               boolean isSkipSample = false;

               if ((_currentUtcTime != Long.MIN_VALUE || _currentTime != Long.MIN_VALUE)
                     && _prevSampleTime != Long.MIN_VALUE && sampleTime == _prevSampleTime) {

                  isSkipSample = true;
               }

               if (isSkipSample == false) {

                  // set virtual time if time is not available
                  _sampleData.absoluteTime = sampleTime;

                  _sampleList.add(_sampleData);

                  _prevSampleTime = sampleTime;
               }

            } else if (_currentSampleType.equals(SAMPLE_TYPE_GPS_BASE)
                  || _currentSampleType.equals(SAMPLE_TYPE_GPS_SMALL)
                  || _currentSampleType.equals(SAMPLE_TYPE_GPS_TINY)) {

               // set virtual time if time is not available
               _gpsData.absoluteTime = sampleTime;

               _gpsList.add(_gpsData);
            }
         }
      }

      _sampleData = null;
      _gpsData = null;
      _markerData = null;

      _currentUtcTime = Long.MIN_VALUE;
      _currentTime = Long.MIN_VALUE;
      _currentSampleType = null;
   }

   private void finalizeTour() {

      // check if data are available
      if (_sampleList.isEmpty()) {
         return;
      }

      setData_GPS();
      setData_Marker();

      // create data object for each tour
      final TourData tourData = new TourData();

      /*
       * set tour start date/time
       */
      final ZonedDateTime zonedStartTime = TimeTools.getZonedDateTime(_sampleList.get(0).absoluteTime);
      tourData.setTourStartTime(zonedStartTime);

      tourData.setDeviceTimeInterval((short) -1);
      tourData.setImportFilePath(_importFilePath);

      tourData.setCalories(_tourCalories);

      tourData.setBattery_Percentage_Start(_tourBatteryPercentageStart);
      tourData.setBattery_Percentage_End(_tourBatteryPercentageEnd);

      tourData.setTraining_TrainingEffect_Aerob(_tourPeakTrainingEffect);
      tourData.setTraining_TrainingPerformance(_tourPerformanceLevel);

      tourData.setDeviceId(_device.deviceId);
      tourData.setDeviceName(_tourDeviceName == null ? _device.visibleName : _tourDeviceName);
      tourData.setDeviceFirmwareVersion(_tourDeviceSW == null ? UI.EMPTY_STRING : _tourDeviceSW);

      tourData.createTimeSeries(_sampleList, true);

      setDistanceSerie(tourData);

      // after all data are added, the tour id can be created
      final String uniqueId = _device.createUniqueId(tourData, Util.UNIQUE_ID_SUFFIX_SUUNTO3);
      final Long tourId = tourData.createTourId(uniqueId);

      /*
       * The tour start time timezone is set from lat/lon in createTimeSeries()
       */
      final ZonedDateTime tourStartTime_FromLatLon = tourData.getTourStartTime();

      if (zonedStartTime.equals(tourStartTime_FromLatLon) == false) {

         // time zone is different -> fix tour start components with adjusted time zone
         tourData.setTourStartTime_YYMMDD(tourStartTime_FromLatLon);
      }

      // check if the tour is already imported
      if (_alreadyImportedTours.containsKey(tourId) == false) {

         // add new tour to other tours
         _newlyImportedTours.put(tourId, tourData);

         // create additional data
         tourData.finalizeTour_TimerPauses(_pausedTime_Start, _pausedTime_End, null);
         tourData.setTourDeviceTime_Recorded(tourData.getTourDeviceTime_Elapsed() - tourData.getTourDeviceTime_Paused());

         tourData.computeAltitudeUpDown();
         tourData.computeTourMovingTime();
         tourData.computeComputedValues();
      }

      _isImported = true;
   }

   /**
    * @return Returns <code>true</code> when a tour was imported
    */
   public boolean isImported() {
      return _isImported;
   }

   private void openError(final Exception e) {

      Display.getDefault().syncExec(() -> {
         final String message = e.getMessage();
         MessageDialog.openError(Display.getCurrent().getActiveShell(),
               "Error", //$NON-NLS-1$
               message);
         System.err.println(message + " in " + _importFilePath); //$NON-NLS-1$
      });
   }

   private void parseXML(final String importFilePath) throws FactoryConfigurationError, XMLStreamException {

      final XMLInputFactory inputFactory = XmlUtils.initializeFactory();
      final XMLEventReader eventReader = inputFactory.createXMLEventReader(new StreamSource("file:" + importFilePath)); //$NON-NLS-1$

      while (eventReader.hasNext()) {

         final XMLEvent xmlEvent = eventReader.nextEvent();

         if (xmlEvent.isStartElement()) {

            final StartElement startElement = xmlEvent.asStartElement();
            final String elementName = startElement.getName().getLocalPart();

            switch (elementName) {

            case TAG_DEVLOG_HEADER:
               parseXML_10_Header(eventReader);
               break;

            case TAG_DEVLOG_DEVICE:
               parseXML_20_Device(eventReader);
               break;

            case TAG_DEVLOG_SAMPLES:
               parseXML_30_Samples(eventReader);
               break;
            }
         }

         if (xmlEvent.isEndElement()) {

            final EndElement endElement = xmlEvent.asEndElement();
            final String elementName = endElement.getName().getLocalPart();

            switch (elementName) {

            case TAG_DEVLOG:

               // </DeviceLog> - end of tour

               finalizeTour();
               break;
            }
         }
      }
   }

   private void parseXML_10_Header(final XMLEventReader eventReader) throws XMLStreamException {

      // DeviceLog/Header

      String data;

      while (eventReader.hasNext()) {

         final XMLEvent xmlEvent = eventReader.nextEvent();

         if (xmlEvent.isStartElement()) {

            final StartElement startElement = xmlEvent.asStartElement();
            final String elementName = startElement.getName().getLocalPart();

            switch (elementName) {

            case TAG_HEADER_BATTERY_CHARGE_AT_START:

               data = ((Characters) eventReader.nextEvent()).getData();

               _tourBatteryPercentageStart = (short) (Util.parseFloat(data) * 100);

               break;

            case TAG_HEADER_BATTERY_CHARGE:

               data = ((Characters) eventReader.nextEvent()).getData();

               _tourBatteryPercentageEnd = (short) (Util.parseFloat(data) * 100);

               break;

            case TAG_HEADER_ENERGY:

               data = ((Characters) eventReader.nextEvent()).getData();

               _tourCalories = (int) (Util.parseFloat(data) / 4184);

               break;

            case TAG_HEADER_PEAK_TRAINING_EFFECT:

               data = ((Characters) eventReader.nextEvent()).getData();
               _tourPeakTrainingEffect = Util.parseFloat(data);

               break;

            case TAG_HEADER_DATETIME:

               data = ((Characters) eventReader.nextEvent()).getData();
               try {
                  _tourStartTime = TIME_FORMAT_LOCAL.parse(data).getTime();
               } catch (final ParseException e) {
                  openError(e);
               }
               break;
            }
         }

         if (xmlEvent.isEndElement()) {

            final EndElement endElement = xmlEvent.asEndElement();
            final String elementName = endElement.getName().getLocalPart();

            if (TAG_DEVLOG_HEADER.equals(elementName)) {

               // </Header>

               break;
            }
         }
      }
   }

   private void parseXML_20_Device(final XMLEventReader eventReader) throws XMLStreamException {

      // DeviceLog/Device

      while (eventReader.hasNext()) {

         final XMLEvent xmlEvent = eventReader.nextEvent();

         if (xmlEvent.isStartElement()) {

            final StartElement startElement = xmlEvent.asStartElement();
            final String elementName = startElement.getName().getLocalPart();

            switch (elementName) {

            case TAG_DEVICE_NAME:

               _tourDeviceName = ((Characters) eventReader.nextEvent()).getData();
               break;

            case TAG_DEVICE_SW:

               _tourDeviceSW = ((Characters) eventReader.nextEvent()).getData();
               break;
            }
         }

         if (xmlEvent.isEndElement()) {

            final EndElement endElement = xmlEvent.asEndElement();
            final String elementName = endElement.getName().getLocalPart();

            if (TAG_DEVLOG_DEVICE.equals(elementName)) {

               // </Device>

               break;
            }
         }
      }
   }

   private void parseXML_30_Samples(final XMLEventReader eventReader) throws XMLStreamException {

      // DeviceLog/Samples

      while (eventReader.hasNext()) {

         final XMLEvent xmlEvent = eventReader.nextEvent();

         if (xmlEvent.isStartElement()) {

            final StartElement startElement = xmlEvent.asStartElement();
            final String elementName = startElement.getName().getLocalPart();

            switch (elementName) {

            case TAG_SAMPLE:
               parseXML_35_Sample(eventReader);
               break;
            }
         }

         if (xmlEvent.isEndElement()) {

            final EndElement endElement = xmlEvent.asEndElement();
            final String elementName = endElement.getName().getLocalPart();

            if (TAG_DEVLOG_SAMPLES.equals(elementName)) {

               // </Samples>

               break;
            }
         }
      }
   }

   private void parseXML_35_Sample(final XMLEventReader eventReader) throws XMLStreamException {

      // create new time items, "sampleType" defines which time data are used
      _gpsData = new TimeData();
      _markerData = new TimeData();
      _sampleData = new TimeData();

      String data;

      while (eventReader.hasNext()) {

         final XMLEvent xmlEvent = eventReader.nextEvent();

         if (xmlEvent.isStartElement()) {

            final StartElement startElement = xmlEvent.asStartElement();
            final String elementName = startElement.getName().getLocalPart();

            switch (elementName) {
            case TAG_SAMPLE_TYPE:
               data = ((Characters) eventReader.nextEvent()).getData();
               _currentSampleType = data;
               break;

            case TAG_SAMPLE_ALTITUDE:
               data = ((Characters) eventReader.nextEvent()).getData();
               _sampleData.absoluteAltitude = Util.parseFloat(data);
               break;

            case TAG_SAMPLE_CADENCE:
               data = ((Characters) eventReader.nextEvent()).getData();
               _sampleData.cadence = Util.parseFloat(data) * 60.0f;
               break;

            case TAG_SAMPLE_EVENTS:
               _isInEvents = true;
               parseXML_36_Events(eventReader);
               break;

            case TAG_SAMPLE_DISTANCE:

               data = ((Characters) eventReader.nextEvent()).getData();

               if (_isInEvents) {

                  // ignore this value because <Distance> can also occur within <Events>

               } else {

                  _sampleData.absoluteDistance = Util.parseFloat(data);
               }
               break;

            case TAG_SAMPLE_HR:
               data = ((Characters) eventReader.nextEvent()).getData();
               // HR * 60 = bpm
               final float hr = Util.parseFloat(data);
               _sampleData.pulse = hr * 60.0f;
               break;

            case TAG_SAMPLE_LATITUDE:
               data = ((Characters) eventReader.nextEvent()).getData();
               _gpsData.latitude = Util.parseDouble(data) * RADIANT_TO_DEGREE;
               break;

            case TAG_SAMPLE_LONGITUDE:
               data = ((Characters) eventReader.nextEvent()).getData();
               _gpsData.longitude = Util.parseDouble(data) * RADIANT_TO_DEGREE;
               break;

            case TAG_SAMPLE_PERFORMANCE_LEVEL:
               data = ((Characters) eventReader.nextEvent()).getData();
               _tourPerformanceLevel = Util.parseFloat(data);
               break;

            case TAG_SAMPLE_TEMPERATURE:
               data = ((Characters) eventReader.nextEvent()).getData();
               final float kelvin = Util.parseFloat(data);
               _sampleData.temperature = (float) (kelvin + Fmath.T_ABS);
               break;

            case TAG_SAMPLE_UTC:
               data = ((Characters) eventReader.nextEvent()).getData();
               final String timeString = data;
               try {
                  _currentUtcTime = ZonedDateTime.parse(timeString).toInstant().toEpochMilli();
               } catch (final Exception e0) {
                  try {
                     _currentUtcTime = TIME_FORMAT.parse(timeString).getTime();
                  } catch (final ParseException e1) {
                     try {
                        _currentUtcTime = TIME_FORMAT_SSSZ.parse(timeString).getTime();
                     } catch (final ParseException e2) {
                        try {
                           _currentUtcTime = TIME_FORMAT_RFC822.parse(timeString).getTime();
                        } catch (final ParseException e3) {
                           try {
                              _currentUtcTime = TIME_FORMAT_LOCAL.parse(timeString).getTime();
                           } catch (final ParseException e4) {
                              openError(e4);
                           }
                        }
                     }
                  }
               }
               break;

            case TAG_SAMPLE_TIME:
               data = ((Characters) eventReader.nextEvent()).getData();
               _currentTime = Double.valueOf(data).longValue();
               break;
            }
         }

         if (xmlEvent.isEndElement()) {

            final EndElement endElement = xmlEvent.asEndElement();
            final String elementName = endElement.getName().getLocalPart();

            switch (elementName) {

            case TAG_SAMPLE_EVENTS:
               _isInEvents = false;
               break;

            case TAG_SAMPLE:
               finalizeSample();
               return;
            }
         }
      }
   }

   private void parseXML_36_Events(final XMLEventReader eventReader) throws XMLStreamException {

      while (eventReader.hasNext()) {

         final XMLEvent xmlEvent = eventReader.nextEvent();

         if (xmlEvent.isStartElement()) {

            final StartElement startElement = xmlEvent.asStartElement();
            final String elementName = startElement.getName().getLocalPart();

            switch (elementName) {
            case TAG_SAMPLE_PAUSE:
               parseXML_37_Pause(eventReader);
               break;

            case TAG_SAMPLE_LAP:
               // set a marker
               _markerData.marker = 1;
               break;

            }
         }

         if (xmlEvent.isEndElement()) {

            final String elementName = xmlEvent.asEndElement().getName().getLocalPart();

            if (TAG_SAMPLE_EVENTS.equals(elementName)) {

               // </Events>

               break;
            }
         }
      }
   }

   private void parseXML_37_Pause(final XMLEventReader eventReader) throws XMLStreamException {

      String data;

      while (eventReader.hasNext()) {

         final XMLEvent xmlEvent = eventReader.nextEvent();

         if (xmlEvent.isStartElement()) {

            final String elementName = xmlEvent.asStartElement().getName().getLocalPart();

            switch (elementName) {
            case TAG_SAMPLE_STATE:
               data = ((Characters) eventReader.nextEvent()).getData();

               if (data.equalsIgnoreCase(Boolean.TRUE.toString())) {

                  _pausedTime_Start.add(_currentUtcTime);

               } else if (data.equalsIgnoreCase(Boolean.FALSE.toString())) {

                  if (_pausedTime_Start.isEmpty()) {
                     return;
                  }

                  _pausedTime_End.add(_currentUtcTime);

               }
               break;

            }
         }

         if (xmlEvent.isEndElement()) {

            final String elementName = xmlEvent.asEndElement().getName().getLocalPart();

            if (TAG_SAMPLE_PAUSE.equals(elementName)) {

               // </Pause>

               break;
            }
         }
      }
   }

   /**
    * Merge GPS data into tour data by time.
    * <p>
    * Merge is necessary because there are separate time slices for GPS data and not every 'normal'
    * time slice has it's own GPS time slice.
    */
   private void setData_GPS() {

      if (_gpsList.isEmpty()) {
         return;
      }

      final int gpsSize = _gpsList.size();

      TimeData nextGPSData = _gpsList.get(0);
      TimeData prevGPSData = nextGPSData;

      long nextGpsTime = nextGPSData.absoluteTime;
      long prevGpsTime = nextGpsTime;

      int gpsIndex = 0;

      for (final TimeData sampleData : _sampleList) {

         final long sampleTime = sampleData.absoluteTime;

         while (true) {

            if (sampleTime > nextGpsTime) {

               gpsIndex++;

               if (gpsIndex < gpsSize) {

                  prevGpsTime = nextGpsTime;
                  prevGPSData = nextGPSData;

                  nextGPSData = _gpsList.get(gpsIndex);
                  nextGpsTime = nextGPSData.absoluteTime;
               } else {
                  break;
               }
            } else {
               break;
            }
         }

         if (sampleTime == prevGpsTime) {

            sampleData.latitude = prevGPSData.latitude;
            sampleData.longitude = prevGPSData.longitude;

         } else if (sampleTime == nextGpsTime) {

            sampleData.latitude = nextGPSData.latitude;
            sampleData.longitude = nextGPSData.longitude;

         } else {

            // interpolate position

            final double gpsTimeDiff = nextGpsTime - prevGpsTime;
            final double sampleDiff = sampleTime - prevGpsTime;

            final double sampleRatio = gpsTimeDiff == 0 ? 0 : sampleDiff / gpsTimeDiff;

            final double latDiff = nextGPSData.latitude - prevGPSData.latitude;
            final double lonDiff = nextGPSData.longitude - prevGPSData.longitude;

            sampleData.latitude = prevGPSData.latitude + latDiff * sampleRatio;
            sampleData.longitude = prevGPSData.longitude + lonDiff * sampleRatio;
         }
      }
   }

   /**
    * Merge Marker data into tour data by time.
    * <p>
    * Merge is necessary because there are separate time slices for markers.
    */
   private void setData_Marker() {

      final int markerSize = _markerList.size();

      if (markerSize == 0) {
         return;
      }

      int markerIndex = 0;

      long markerTime = _markerList.get(markerIndex).absoluteTime;

      for (final TimeData sampleData : _sampleList) {

         final long sampleTime = sampleData.absoluteTime;

         if (sampleTime >= markerTime) {

            sampleData.marker = 1;
            sampleData.markerLabel = Integer.toString(markerIndex + 1);

            /*
             * check if another marker is available
             */
            markerIndex++;

            if (markerIndex >= markerSize) {
               break;
            }

            markerTime = _markerList.get(markerIndex).absoluteTime;
         }
      }
   }

   /**
    * Check if distance values are not changed when geo position changed, when <code>true</code>
    * compute distance values from geo position.
    *
    * @param tourData
    */
   private void setDistanceSerie(final TourData tourData) {

      /*
       * There are currently no data available to check if distance is changing, current data keep
       * distance for some slices and then jumps to the next value.
       */
      TourManager.computeDistanceValuesFromGeoPosition(tourData);
   }

}
