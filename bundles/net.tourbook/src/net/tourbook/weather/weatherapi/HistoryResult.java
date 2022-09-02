/*******************************************************************************
 * Copyright (C) 2022 Frédéric Bard
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
package net.tourbook.weather.weatherapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import net.tourbook.common.UI;
import net.tourbook.common.weather.IWeather;
import net.tourbook.weather.WeatherUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoryResult {

   public Forecast    forecast;

   private List<Hour> hourList = new ArrayList<>();
   private int        averageWindSpeed;
   private int        averageWindDirection;
   private Hour       middleHourData;

   public void addHourList(final List<Hour> newHourList) {

      for (final Hour newHour : newHourList) {
         if (!hourList.contains(newHour)) {
            hourList.add(newHour);
         }
      }
   }

   public void computeAverageWindSpeedAndDirection() {

      final double[] windSpeeds = hourList
            .stream()
            .mapToDouble(Hour::getWind_kph)
            .toArray();

      final int[] windDirections = hourList
            .stream()
            .mapToInt(Hour::getWind_degree)
            .toArray();

      final int[] averageWindSpeedAndDirection =
            WeatherUtils.computeAverageWindSpeedAndDirection(windSpeeds, windDirections);

      averageWindSpeed = averageWindSpeedAndDirection[0];
      averageWindDirection = averageWindSpeedAndDirection[1];
   }

   /**
    * Filters and keeps only the values included between the tour start and end times.
    *
    * @return
    */
   public boolean filterHourData(final long tourStartTime, final long tourEndTime) {

      final List<Hour> filteredHourData = new ArrayList<>();

      for (final Hour currentHour : hourList) {

         //The current data is not kept if its measured time is:
         // - more than 30 mins before the tour start time
         // OR
         // - more than 30 mins after the tour end time

         if (currentHour.getTime_epoch() < tourStartTime - WeatherUtils.SECONDS_PER_THIRTY_MINUTE ||
               currentHour.getTime_epoch() > tourEndTime + WeatherUtils.SECONDS_PER_THIRTY_MINUTE) {
            continue;
         }

         filteredHourData.add(currentHour);
      }

      hourList = filteredHourData;

      return hourList.size() > 0;
   }

   /**
    * Finds the hour that is closest to the middle of the tour. This will be used
    * to determine the weather description of the tour.
    */
   public void findMiddleHour(final long tourMiddleTime) {

      middleHourData = null;

      long timeDifference = Long.MAX_VALUE;
      for (final Hour currentHour : hourList) {

         final long currentTimeDifference = Math.abs(currentHour.getTime_epoch() - tourMiddleTime);
         if (currentTimeDifference < timeDifference) {
            middleHourData = currentHour;
            timeDifference = currentTimeDifference;
         }
      }
   }

   public float getAverageHumidity() {

      final OptionalDouble averageHumidity =
            hourList.stream().mapToDouble(Hour::getHumidity).average();

      if (averageHumidity.isPresent()) {
         return WeatherUtils.roundDoubleToFloat(averageHumidity.getAsDouble());
      }

      return 0;
   }

   public float getAveragePressure() {

      final OptionalDouble averagePressure =
            hourList.stream().mapToDouble(Hour::getPressure_mb).average();

      if (averagePressure.isPresent()) {
         return WeatherUtils.roundDoubleToFloat(averagePressure.getAsDouble());
      }

      return 0;
   }

   public float getAverageWindChill() {

      final OptionalDouble averageWindChill =
            hourList.stream().mapToDouble(Hour::getFeelslike_c).average();

      if (averageWindChill.isPresent()) {
         return WeatherUtils.roundDoubleToFloat(averageWindChill.getAsDouble());
      }

      return 0;
   }

   public int getAverageWindDirection() {

      return averageWindDirection;
   }

   public int getAverageWindSpeed() {

      return averageWindSpeed;
   }

   public List<Hour> getForecastdayHourList() {

      if (forecast != null && forecast.getForecastday() != null &&
            forecast.getForecastday().size() > 0) {
         return forecast.getForecastday().get(0).getHour();
      }

      return new ArrayList<>();
   }

   public List<Hour> getHourList() {

      return hourList;
   }

   private Hour getMiddleHourData() {

      return middleHourData;
   }

   public float getTemperatureAverage() {

      final OptionalDouble averageTemperature =
            hourList.stream().mapToDouble(Hour::getTemp_c).average();

      if (averageTemperature.isPresent()) {
         return WeatherUtils.roundDoubleToFloat(averageTemperature.getAsDouble());
      }

      return 0;
   }

   public float getTemperatureMax() {

      final OptionalDouble maxTemperature =
            hourList.stream().mapToDouble(Hour::getTemp_c).max();

      if (maxTemperature.isPresent()) {
         return WeatherUtils.roundDoubleToFloat(maxTemperature.getAsDouble());
      }

      return 0;
   }

   public float getTemperatureMin() {

      final OptionalDouble minTemperature =
            hourList.stream().mapToDouble(Hour::getTemp_c).min();

      if (minTemperature.isPresent()) {
         return WeatherUtils.roundDoubleToFloat(minTemperature.getAsDouble());
      }

      return 0;
   }

   public float getTotalPrecipitation() {

      return WeatherUtils.roundDoubleToFloat(hourList.stream().mapToDouble(Hour::getPrecip_mm).sum());
   }

   public String getWeatherDescription() {

      final String weatherDescription = UI.EMPTY_STRING;

      final Hour middleHour = getMiddleHourData();
      if (middleHour == null) {
         return weatherDescription;
      }

      return middleHour.getCondition().getText();
   }

   public String getWeatherType() {

      String weatherType = UI.EMPTY_STRING;

      if (middleHourData == null) {
         return weatherType;
      }

      // Weather Icons and Codes  : https://www.weatherapi.com/docs/#weather-icons
      switch (middleHourData.getCondition().getCode()) {
      case 1006:
      case 1009:
      case 1030:
      case 1135:
      case 1147:
         weatherType = IWeather.WEATHER_ID_OVERCAST;
         break;
      case 1000:
         weatherType = IWeather.WEATHER_ID_CLEAR;
         break;
      case 1003:
         weatherType = IWeather.WEATHER_ID_PART_CLOUDS;
         break;
      case 1087:
         weatherType = IWeather.WEATHER_ID_LIGHTNING;
         break;
      case 1192:
      case 1195:
      case 1201:
      case 1243:
      case 1246:
      case 1252:
      case 1276:
         weatherType = IWeather.WEATHER_ID_RAIN;
         break;
      case 1066:
      case 1114:
      case 1117:
      case 1210:
      case 1213:
      case 1216:
      case 1219:
      case 1222:
      case 1225:
      case 1237:
      case 1255:
      case 1258:
      case 1261:
      case 1264:
      case 1279:
      case 1282:
         weatherType = IWeather.WEATHER_ID_SNOW;
         break;
      case 1063:
      case 1069:
      case 1180:
      case 1183:
      case 1186:
      case 1189:
      case 1198:
      case 1204:
      case 1207:
      case 1240:
      case 1249:
      case 1273:
         weatherType = IWeather.WEATHER_ID_SCATTERED_SHOWERS;
         break;
      case 1072:
      case 1150:
      case 1153:
      case 1168:
      case 1171:
         weatherType = IWeather.WEATHER_ID_DRIZZLE;
         break;
      default:
         weatherType = UI.EMPTY_STRING;
         break;
      }

      return weatherType;
   }
}
