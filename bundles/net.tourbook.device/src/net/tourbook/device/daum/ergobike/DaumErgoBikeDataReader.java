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
package net.tourbook.device.daum.ergobike;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.ImportState_File;
import net.tourbook.importdata.ImportState_Process;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.preference.IPreferenceStore;

public class DaumErgoBikeDataReader extends TourbookDevice {

   private static final String            DAUM_ERGO_BIKE_CSV_ID =
         "Elapsed Time (s);Distance (km);Phys. kJoule;Slope (%);NM;RPM;Speed (km/h);Watt;Gear;Device Active;Pulse;Pulse Type;Training Type;Training Value;Pulse Time 1;2;3;4;5;6"; //$NON-NLS-1$

   private static final String            CSV_STRING_TOKEN      = ";";                                                                                                             //$NON-NLS-1$

   private static final DateTimeFormatter _dateParser           = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);                                                       //$NON-NLS-1$

   private static final IPreferenceStore  _prefStore            = TourbookPlugin.getPrefStore();

   public DaumErgoBikeDataReader() {
      // plugin constructor
   }

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

   private float parseFloat(final String stringValue, final DecimalFormat decimalFormat) {

      try {
         return decimalFormat.parse(stringValue).floatValue();
      } catch (final ParseException e) {
         StatusUtil.log(e);
      }

      return 0;
   }

   @Override
   public void processDeviceData(final String importFilePath,
                                 final DeviceData deviceData,
                                 final Map<Long, TourData> alreadyImportedTours,
                                 final Map<Long, TourData> newlyImportedTours,
                                 final ImportState_File importState_File,
                                 final ImportState_Process importState_Process) {

      // must be local because of concurrency
      DecimalFormat decimalFormat = (DecimalFormat) DecimalFormat.getInstance();

      if (_prefStore.getBoolean(ITourbookPreferences.REGIONAL_USE_CUSTOM_DECIMAL_FORMAT)) {

         /*
          * Use customized number format
          */
         try {

            final DecimalFormatSymbols dfs = decimalFormat.getDecimalFormatSymbols();

            final String groupSep = _prefStore.getString(ITourbookPreferences.REGIONAL_GROUP_SEPARATOR);
            final String decimalSep = _prefStore.getString(ITourbookPreferences.REGIONAL_DECIMAL_SEPARATOR);

            dfs.setGroupingSeparator(groupSep.charAt(0));
            dfs.setDecimalSeparator(decimalSep.charAt(0));

            decimalFormat.setDecimalFormatSymbols(dfs);

         } catch (final Exception e) {
            e.printStackTrace();
         }

      } else {

         // Use default number format

         decimalFormat = (DecimalFormat) DecimalFormat.getInstance();
      }

      try (FileReader fileReader = new FileReader(importFilePath);
            BufferedReader bufferedReader = new BufferedReader(fileReader)) {

         /*
          * Check if the file is from a Daum Ergometer
          */
         final String fileHeader = bufferedReader.readLine();
         if (fileHeader.startsWith(DAUM_ERGO_BIKE_CSV_ID) == false) {
            return;
         }

         StringTokenizer tokenizer;

         /*
          * Extract data from the file name
          */
         final String fileName = new File(importFilePath).getName();

         //           1         2         3         4         5         6         7
         // 01234567890123456789012345678901234567890123456789012345678901234567890
         //       x  x  x    x  x  x                    x
         // 0379  01_02_2021 19_04_08   42min   18_9km  Manuelles Training (Watt).csv
         // 0380  03_02_2021 19_30_08   41min   18_6km  Manuelles Training (Watt).csv
         // 0381  09_02_2021 19_12_08   42min   19_1km  Manuelles Training (Watt).csv
         // 0382  12_02_2021 19_45_08   32min   13_9km  Manuelles Training (Watt).csv

         // 0382  12 Feb 2021 19_45_08   32min   13_9km  Manuelles Training (Watt).csv
         // 0383  16 Feb 2021 19_40_25   32min   14_4km  Manuelles Training (Watt).csv
         // 0384  5 Mar 2021 19_20_07   42min   18_9km  Manuelles Training (Watt).csv
         // 0385  15 Mar 2021 19_17_08   42min   18_8km  Manuelles Training (Watt).csv
         // 0386  17 Mar 2021 19_29_08   42min   19_2km  Manuelles Training (Watt).csv
         // 0387  17 May 2021 18_10_08    5min    2_2km  Manuelles Training (Watt).csv

         StringTokenizer fileNameToken = new StringTokenizer(fileName);
         fileNameToken.nextToken();
         fileNameToken.nextToken();
         fileNameToken.nextToken();
         final String token4 = fileNameToken.nextToken();
         final boolean is_D_M_Y_Format = token4.contains("min"); //$NON-NLS-1$

         // tokenize again
         fileNameToken = new StringTokenizer(fileName);

         int tourDay = 0;
         int tourMonth = 0;
         int tourYear = 0;
         int tourHour = 0;
         int tourMin = 0;
         int tourSec = 0;

         String title = UI.EMPTY_STRING;

         if (is_D_M_Y_Format) {

            // import date format: 01_02_2021"
            //
            // 0379  01_02_2021 19_04_08   42min   18_9km  Manuelles Training (Watt).csv

            // start date
            tourDay = Integer.parseInt(fileName.substring(6, 8));
            tourMonth = Integer.parseInt(fileName.substring(9, 11));
            tourYear = Integer.parseInt(fileName.substring(12, 16));

            // start time
            tourHour = Integer.parseInt(fileName.substring(17, 19));
            tourMin = Integer.parseInt(fileName.substring(20, 22));
            tourSec = Integer.parseInt(fileName.substring(23, 25));

         } else {

            // import date format: "5 Mar 2021"
            //
            // 0384  5 Mar 2021 19_20_07   42min   18_9km  Manuelles Training (Watt).csv

            // skip file numer, eg 0384
            fileNameToken.nextToken();

            // start date
            final String dayToken = fileNameToken.nextToken();
            final String monthToken = fileNameToken.nextToken();
            final String yearToken = fileNameToken.nextToken();
            final String dateToken = dayToken + UI.SPACE + monthToken + UI.SPACE + yearToken;

            tourDay = Integer.parseInt(dayToken);
            tourMonth = LocalDate.parse(dateToken, _dateParser).getMonthValue();
            tourYear = Integer.parseInt(yearToken);

            final String timeToken = fileNameToken.nextToken();

            // start time
            tourHour = Integer.parseInt(timeToken.substring(0, 2));
            tourMin = Integer.parseInt(timeToken.substring(3, 5));
            tourSec = Integer.parseInt(timeToken.substring(6, 8));

         }

         title = fileName.substring(44);
         title = title.substring(0, title.length() - 4).trim();

         /*
          * Set tour data
          */
         final TourData tourData = new TourData();

         tourData.setTourStartTime(tourYear, tourMonth, tourDay, tourHour, tourMin, tourSec);

         tourData.setTourTitle(title);
         tourData.setTourDescription(fileName);

         tourData.setDeviceMode((short) 0);
         tourData.setDeviceTimeInterval((short) -1);

         tourData.setImportFilePath(importFilePath);

         /*
          * set time serie from the imported trackpoints
          */
         final ArrayList<TimeData> timeDataList = new ArrayList<>();
         TimeData timeData;

         int time;
         int previousTime = 0;

         int distance = 0;
         int previousDistance = 0;

         boolean isFirstTime = true;

         String tokenLine;

         int sumPowerTime = 0;
         float sumPower = 0;
         float kJoule = 0;

         // read all data points
         while ((tokenLine = bufferedReader.readLine()) != null) {

            tokenizer = new StringTokenizer(tokenLine, CSV_STRING_TOKEN);

            time = (short) Integer.parseInt(tokenizer.nextToken()); //                       1  Elapsed Time (s)
            distance = (int) (parseFloat(tokenizer.nextToken(), decimalFormat) * 1000); //   2  Distance (m)
            kJoule = parseFloat(tokenizer.nextToken(), decimalFormat); //                    3  Phys. kJoule
            tokenizer.nextToken(); //                                                        4  Slope (%)
            tokenizer.nextToken(); //                                                        5  NM
            final float cadence = parseFloat(tokenizer.nextToken(), decimalFormat); //       6  RPM
            final float speed = parseFloat(tokenizer.nextToken(), decimalFormat); //         7  Speed (km/h)
            final int power = Integer.parseInt(tokenizer.nextToken()); //                    8  Watt
            tokenizer.nextToken(); //                                                        9  Gear
            tokenizer.nextToken(); //                                                        10 Device Active
            final int pulse = Integer.parseInt(tokenizer.nextToken()); //                    11 Pulse;
            tokenizer.nextToken(); //                                                        12 Pulse Type;
            tokenizer.nextToken(); //                                                        13 Training Type;
            tokenizer.nextToken(); //                                                        14 Training Value;
            final int pulseTime1 = Integer.parseInt(tokenizer.nextToken()); //               15 Pulse Time 1;
            final int pulseTime2 = Integer.parseInt(tokenizer.nextToken()); //               16 2;
            final int pulseTime3 = Integer.parseInt(tokenizer.nextToken()); //               17 3;
            final int pulseTime4 = Integer.parseInt(tokenizer.nextToken()); //               18 4;
            final int pulseTime5 = Integer.parseInt(tokenizer.nextToken()); //               19 5;
            final int pulseTime6 = Integer.parseInt(tokenizer.nextToken()); //               20 6

            timeDataList.add(timeData = new TimeData());

            final int timeDiff = time - previousTime;

            if (isFirstTime) {
               isFirstTime = false;
               timeData.time = 0;
            } else {
               timeData.time = timeDiff;
            }
            timeData.distance = distance - previousDistance;
            timeData.cadence = cadence;
            timeData.pulse = pulse;
            timeData.power = power;
            timeData.speed = speed;
            timeData.pulseTime = new int[] { pulseTime1, pulseTime2, pulseTime3, pulseTime4, pulseTime5, pulseTime6 };

            // ignore small cadence values
            if (cadence > 10) {

               sumPower += power;
               sumPowerTime += timeDiff;
            }

            // prepare next data point
            previousTime = time;
            previousDistance = distance;
         }

         if (timeDataList.isEmpty()) {

            // data are valid but have no data points

            importState_File.isFileImportedWithValidData = true;

            return;
         }

         final float joule = kJoule * 1000;
         final float calories = joule * UI.UNIT_JOULE_2_CALORY;

         tourData.setCalories((int) calories);
         tourData.setPower_TotalWork((long) joule);
         tourData.setPower_Avg(sumPowerTime == 0 ? 0 : sumPower / sumPowerTime);

         tourData.setDeviceId(deviceId);
         tourData.setDeviceName(visibleName);

         /*
          * Set the start distance, this is not available in a .crp file but it's required to
          * create the tour-id.
          */
         tourData.setStartDistance(distance);

         tourData.createTimeSeries(timeDataList, false);

         // after all data are added, the tour id can be created
         final int tourDistance = (int) Math.abs(tourData.getStartDistance());
         final String uniqueId = createUniqueId_Legacy(tourData, tourDistance);
         final Long tourId = tourData.createTourId(uniqueId);

         // check if the tour is in the tour map
         if (alreadyImportedTours.containsKey(tourId) == false) {

            // add new tour to the map
            newlyImportedTours.put(tourId, tourData);

            // create additional data
            tourData.setTourDeviceTime_Recorded(tourData.getTourDeviceTime_Elapsed());
            tourData.computeTourMovingTime();
            tourData.computeComputedValues();
         }

         importState_File.isFileImportedWithValidData = true;

      } catch (final Exception e) {

         StatusUtil.log(importFilePath, e);
      }
   }

   /**
    * checks if the data file has a valid .crp data format
    *
    * @return true for a valid .crp data format
    */
   @Override
   public boolean validateRawData(final String fileName) {

      try (FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader)) {

         final String fileHeader = bufferedReader.readLine();
         if (fileHeader == null) {
            return false;
         }

         if (fileHeader.startsWith(DAUM_ERGO_BIKE_CSV_ID) == false) {
            return false;
         }

      } catch (final IOException e) {
         e.printStackTrace();
      }

      return true;
   }

}
