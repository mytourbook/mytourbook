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
/*
 * Author: Wolfgang Schramm Created: 23.05.2005
 */
package net.tourbook.device.hac4;

import java.io.IOException;
import java.io.RandomAccessFile;

import net.tourbook.data.DataUtil;
import net.tourbook.ui.UI;

/**
 * Contains all data read from the device except the tour data
 *
 * @author Wolfgang Schramm
 */
public class HAC4DeviceData {

   private static final String NL = UI.NEW_LINE;

   /**
    * cccc (h) "B735"
    */
   public String               deviceType;

   /**
    * pppp (h) wheel perimeter (mm)
    */
   public int                  wheelPerimeter;

   /**
    * wwww (h) weight (kg)
    */
   public int                  personWeight;

   /**
    * aaaa (h) home altitude (m) "FFFF" not set
    */
   public int                  homeAltitude;

   /**
    * bbbb (h) 1. pulse upper bound (bpm)
    */
   public int                  pulse1UpperBound;

   /**
    * cccc (h) 1. pulse lower bound (bpm)
    */
   public int                  pulse1LowerBound;

   /**
    * dddd (h) 2. pulse upper bound (bpm)
    */
   public int                  pulse2UpperBound;

   /**
    * eeee (h) 2. pulse lower bound (bpm)
    */
   public int                  pulse2LowerBound;

   /**
    * aa (d) 1. count down minutes
    */
   public short                count1min;

   /**
    * bb (d) 1. count down seconds
    */
   public short                count1sec;

   /**
    * cc (d) 2. count down minutes
    */
   public short                count2min;

   /**
    * dd (d) 2. count down seconds
    */
   public short                count2sec;

   /**
    * llll (h) total distance at end of last tour (km) * 2^16
    */
   public int                  totalDistanceHigh;

   /**
    * kkkk (h) total distance at end of last tour (km)
    */
   public int                  totalDistanceLow;

   /**
    * eeee (h) altitude error correction
    */
   public int                  altitudeError;

   /**
    * uuuu (h) total altitude up at end of last tour (m)
    */
   public int                  totalAltitudeUp;

   /**
    * dddd (h) total altitude down at end of last tour (m)
    */
   public int                  totalAltitudeDown;

   /**
    * aaaa (h) max altitude (m)
    */
   public int                  maxAltitude;

   /**
    * hh (d) hour of total travel time
    */
   public short                totalTravelTimeHourLow;

   /**
    * HH (d) hour of total travel time * 100
    */
   public short                totalTravelTimeHourHigh;

   /**
    * ss (d) seconds of total travel time
    */
   public short                totalTravelTimeSec;

   /**
    * mm (d) minute of total travel time
    */
   public short                totalTravelTimeMin;

   /**
    * oooo (h) next free memory offset
    */
   public int                  offsetNextMemory;

   /**
    * cccc (o) offset of last CC-record
    */
   public int                  offsetCCRecord;

   /**
    * dddd (o) offset of last DD-record
    */
   public int                  offsetDDRecord;

   /**
    * eeee (o) offset of last compare record
    */
   public int                  offsetCompareRecord;

   /**
    * yyyy (d) year of transfer
    */
   public short                transferYear;

   /**
    * mm (d) month of transfer
    */
   public short                transferMonth;

   /**
    * dd (d) day of transfer
    */
   public short                transferDay;

   public HAC4DeviceData() {}

   public static int parseInt(final byte[] buffer) {

      int value = 0;
      try {
         value = Integer.parseInt(new String(buffer, 0, 4), 16);
      } catch (final NumberFormatException e) {
         e.printStackTrace();
      }

      return value;
   }

   public static short parseShort(final byte[] buffer, final int offset, final int length) {

      short value = 0;
      try {
         value = Short.parseShort(new String(buffer, offset, length));
      } catch (final NumberFormatException e) {
         e.printStackTrace();
      }
      return value;
   }

   public void dumpData() {

      final String transferDate = transferDay + UI.SYMBOL_DOT + transferMonth + UI.SYMBOL_DOT + transferYear;

      final String totalTravelTime = UI.EMPTY_STRING

            + ((totalTravelTimeHourHigh * 100) + totalTravelTimeHourLow) + UI.SYMBOL_COLON
            + totalTravelTimeMin + UI.SYMBOL_COLON
            + totalTravelTimeSec;

      final String dump = UI.EMPTY_STRING

            + "----------------------------------------------------" + NL //        //$NON-NLS-1$
            + "DEVICE DATA" //                                                      //$NON-NLS-1$
            + "----------------------------------------------------" + NL //        //$NON-NLS-1$
            + "Transfer date:               " + transferDate + NL //                //$NON-NLS-1$
            + "Device:                      " + deviceType + NL //                  //$NON-NLS-1$
            + "Wheel perimeter:             " + wheelPerimeter + " mm" + NL //      //$NON-NLS-1$ //$NON-NLS-2$
            + "Person weight:               " + personWeight + " kg" + NL //        //$NON-NLS-1$ //$NON-NLS-2$
            + NL
            + "Home altitude:               " + homeAltitude + " m" + NL //         //$NON-NLS-1$ //$NON-NLS-2$
            + "Altitude error correction:   " + altitudeError + NL //               //$NON-NLS-1$
            + "Max Altitude:                " + maxAltitude + " m" + NL //          //$NON-NLS-1$ //$NON-NLS-2$
            + NL
            + "Pulse 1 upper bound          " + pulse1UpperBound + " bpm" + NL //   //$NON-NLS-1$ //$NON-NLS-2$
            + "Pulse 1 lower bound:         " + pulse1LowerBound + " bpm" + NL //   //$NON-NLS-1$ //$NON-NLS-2$
            + "Pulse 2 upper bound:         " + pulse2UpperBound + " bpm" + NL //   //$NON-NLS-1$ //$NON-NLS-2$
            + "Pulse 2 lower bound:         " + pulse2LowerBound + " bpm" + NL //   //$NON-NLS-1$ //$NON-NLS-2$
            + NL
            + "1. Count down:               " + count1min + UI.SYMBOL_COLON + count1sec + NL //         //$NON-NLS-1$
            + "2. Count down:               " + count2min + UI.SYMBOL_COLON + count2sec + NL //         //$NON-NLS-1$
            + NL
            + "Total distance:              " + ((totalDistanceHigh * (2 ^ 16)) + totalDistanceLow) + " km" + NL //         //$NON-NLS-1$ //$NON-NLS-2$
            + "Total altitude up:           " + totalAltitudeUp + " m" + NL //         //$NON-NLS-1$ //$NON-NLS-2$
            + "Total altitude down:         " + totalAltitudeDown + " m" + NL //       //$NON-NLS-1$ //$NON-NLS-2$
            + "Total travel time:           " + totalTravelTime + NL //$NON-NLS-1$
            + NL
            + "Offset last CC record:       " + offsetCCRecord + NL //                 //$NON-NLS-1$
            + "Offset last DD record:       " + offsetDDRecord + NL //                 //$NON-NLS-1$
            + "Offset next free memory:     " + offsetNextMemory + NL //               //$NON-NLS-1$
            + "Offset compare record:       " + offsetCompareRecord + NL //            //$NON-NLS-1$
      ;

      System.out.print(dump);
   }

   /**
    * @param fileRawData
    * @throws IOException
    * @throws NumberFormatException
    */
   public void readFromFile(final RandomAccessFile fileRawData) throws IOException, NumberFormatException {

      final byte[] buffer = new byte[5];

      fileRawData.read(buffer);
      deviceType = new String(buffer, 0, 4);

      fileRawData.read(buffer);
      wheelPerimeter = parseInt(buffer);

      fileRawData.read(buffer);
      personWeight = parseInt(buffer);

      fileRawData.read(buffer);
      homeAltitude = parseInt(buffer);

      // pulse 1/2 upper/lower
      fileRawData.read(buffer);
      pulse1UpperBound = parseInt(buffer);

      fileRawData.read(buffer);
      pulse1LowerBound = parseInt(buffer);

      fileRawData.read(buffer);
      pulse2UpperBound = parseInt(buffer);

      fileRawData.read(buffer);
      pulse2LowerBound = parseInt(buffer);

      fileRawData.read(buffer);
      count1min = parseShort(buffer, 0, 2);
      count1sec = parseShort(buffer, 2, 2);

      fileRawData.read(buffer);
      count2min = parseShort(buffer, 0, 2);
      count2sec = parseShort(buffer, 2, 2);

      fileRawData.read(buffer);
      altitudeError = parseInt(buffer);

      fileRawData.read(buffer);
      totalDistanceHigh = parseInt(buffer);

      fileRawData.read(buffer);
      totalDistanceLow = parseInt(buffer);

      offsetNextMemory = DataUtil.readFileOffset(fileRawData, buffer);

      fileRawData.read(buffer);
      transferYear = parseShort(buffer, 0, 4);

      fileRawData.read(buffer);
      transferMonth = parseShort(buffer, 0, 2);
      transferDay = parseShort(buffer, 2, 2);

      fileRawData.read(buffer);
      totalAltitudeUp = parseInt(buffer);

      fileRawData.read(buffer);
      totalAltitudeDown = parseInt(buffer);

      fileRawData.read(buffer);
      maxAltitude = parseInt(buffer);

      fileRawData.read(buffer);
      totalTravelTimeHourLow = parseShort(buffer, 0, 2);
      totalTravelTimeHourHigh = parseShort(buffer, 2, 2);

      fileRawData.read(buffer);
      totalTravelTimeSec = parseShort(buffer, 0, 2);
      totalTravelTimeMin = parseShort(buffer, 2, 2);

      offsetCCRecord = DataUtil.readFileOffset(fileRawData, buffer);
      offsetDDRecord = DataUtil.readFileOffset(fileRawData, buffer);
      offsetCompareRecord = DataUtil.readFileOffset(fileRawData, buffer);
   }

}
