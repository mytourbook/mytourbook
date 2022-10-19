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
package net.tourbook.weather;

import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;

import java.util.ArrayList;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.weather.IWeather;
import net.tourbook.data.TourData;

public class WeatherUtils {

   public static final int    SECONDS_PER_THIRTY_MINUTE = 1800;

   public static final String HEROKU_APP_URL            = "https://passeur-mytourbook-oauthapps.cyclic.app"; //$NON-NLS-1$

   /**
    * Returns the fully detailed weather data as a human readable string.
    * Example:
    * 12h 6°C feels like 3°C 12km/h from 84° humidity 97% pressure 1012mbar precipitation 3mm
    * snowfall 0mm
    *
    * @param temperatureValue
    *           in Celsius
    * @param windChill
    *           in Celsius
    * @param windSpeed
    *           in km/h
    * @param windDirection
    *           in degrees
    * @param humidity
    *           in *
    * @param precipitation
    *           in mm
    * @param snowFall
    *           in mm
    * @param snowFallValue
    *           in epoch seconds
    * @param timeZonedId
    * @param isDisplayEmptyValues
    * @return
    */
   public static String buildFullWeatherDataString(final float temperatureValue,
                                                   final float windChill,
                                                   final float windSpeed,
                                                   final int windDirection,
                                                   final int humidityValue,
                                                   final int pressureValue,
                                                   final float precipitationValue,
                                                   final float snowFallValue,
                                                   final TourDateTime tourDateTime,
                                                   final boolean isDisplayEmptyValues) {

      if (tourDateTime == null) {
         return UI.EMPTY_STRING;
      }

      final String tourTime = String.format("%3s", tourDateTime.tourZonedDateTime.getHour() + UI.UNIT_LABEL_TIME); //$NON-NLS-1$

      final String temperature = String.format("%5s", //$NON-NLS-1$
            FormatManager.formatTemperature(UI.convertTemperatureFromMetric(temperatureValue)))
            + UI.UNIT_LABEL_TEMPERATURE;

      final String feelsLike = Messages.Log_HistoricalWeatherRetriever_001_WeatherData_Temperature_FeelsLike
            + UI.SPACE
            + String.format("%5s", FormatManager.formatTemperature(UI.convertTemperatureFromMetric(windChill))) //$NON-NLS-1$
            + UI.UNIT_LABEL_TEMPERATURE;

      final String wind = String.format("%5s", FormatManager.formatTemperature(UI.convertSpeed_FromMetric(windSpeed))) //$NON-NLS-1$
            + UI.UNIT_LABEL_SPEED
            + UI.SPACE
            + Messages.Log_HistoricalWeatherRetriever_001_WeatherData_WindDirection
            + UI.SPACE
            + String.format("%3d", windDirection) //$NON-NLS-1$
            + UI.SYMBOL_DEGREE;

      final String humidity = Messages.Log_HistoricalWeatherRetriever_001_WeatherData_Humidity
            + UI.SPACE
            + String.format("%3s", humidityValue); //$NON-NLS-1$

      final String pressure = Messages.Log_HistoricalWeatherRetriever_001_WeatherData_Pressure
            + UI.SPACE
            + String.format("%6s", roundDoubleToFloat(pressureValue)) //$NON-NLS-1$
            + UI.UNIT_LABEL_PRESSURE_MBAR_OR_INHG;

      String precipitation = UI.EMPTY_STRING;
      if (precipitationValue > 0 || isDisplayEmptyValues) {

         precipitation = Messages.Log_HistoricalWeatherRetriever_001_WeatherData_Precipitation
               + UI.SPACE
               + String.format("%5s", roundDoubleToFloat(UI.convertPrecipitation_FromMetric(precipitationValue))) //$NON-NLS-1$
               + UI.UNIT_LABEL_DISTANCE_MM_OR_INCH;
      }

      String snowFall = UI.EMPTY_STRING;
      if (snowFallValue > 0 || isDisplayEmptyValues) {

         snowFall = Messages.Log_HistoricalWeatherRetriever_001_WeatherData_Snowfall
               + UI.SPACE
               + String.format("%5s", roundDoubleToFloat(UI.convertPrecipitation_FromMetric(snowFallValue))) //$NON-NLS-1$
               + UI.UNIT_LABEL_DISTANCE_MM_OR_INCH;
      }

      final String fullWeatherData = UI.EMPTY_STRING

            + tourTime + UI.SPACE3
            + temperature + UI.SPACE3
            + feelsLike + UI.SPACE3
            + wind + UI.SPACE3
            + humidity + UI.SYMBOL_PERCENTAGE + UI.SPACE3
            + pressure + UI.SPACE3
            + precipitation + UI.SPACE3
            + snowFall;

      return fullWeatherData;
   }

   /**
    * Returns the weather data as a human readable string, depending on the
    * desired data.
    * Example: ☀ Sunny, 19°C, max. 26°C, min. 10°C, feels like 19°C, 6km/h from SSE, 34% humidity
    */
   public static String buildWeatherDataString(final TourData tourData,
                                               final boolean displayMinimumTemperature,
                                               final boolean displayMaximumTemperature,
                                               final boolean displayPressure) {

      final List<String> weatherDataList = new ArrayList<>();

      // Icon
      final String weatherIcon = getWeatherIcon(tourData.getWeatherIndex());
      if (StringUtils.hasContent(weatherIcon)) {

         weatherDataList.add(weatherIcon.trim());
      }

      // Description
      final String weatherText = tourData.getWeather();
      if (StringUtils.hasContent(weatherText)) {

         if (weatherDataList.size() == 1) {
            weatherDataList.set(0, weatherDataList.get(0) + UI.SPACE + weatherText);
         } else {
            weatherDataList.add(weatherText);
         }
      }

      // Average temperature
      final float averageTemperature = tourData.getWeather_Temperature_Average();
      if (averageTemperature != Float.MIN_VALUE) {
         weatherDataList.add(FormatManager.formatTemperature(UI.convertTemperatureFromMetric(averageTemperature)) +
               UI.UNIT_LABEL_TEMPERATURE);
      }

      // Maximum temperature
      if (displayMaximumTemperature) {

         weatherDataList.add(
               Messages.Log_HistoricalWeatherRetriever_001_WeatherData_Temperature_Max +
                     UI.SPACE +
                     FormatManager.formatTemperature(
                           UI.convertTemperatureFromMetric(tourData.getWeather_Temperature_Max())) +
                     UI.UNIT_LABEL_TEMPERATURE);
      }

      // Minimum temperature
      if (displayMinimumTemperature) {

         weatherDataList.add(
               Messages.Log_HistoricalWeatherRetriever_001_WeatherData_Temperature_Min +
                     UI.SPACE +
                     FormatManager.formatTemperature(
                           UI.convertTemperatureFromMetric(tourData.getWeather_Temperature_Min())) +
                     UI.UNIT_LABEL_TEMPERATURE);
      }

      // Wind chill
      final float temperatureWindChill = tourData.getWeather_Temperature_WindChill();
      if (temperatureWindChill != Float.MIN_VALUE) {

         weatherDataList.add(
               Messages.Log_HistoricalWeatherRetriever_001_WeatherData_Temperature_FeelsLike +
                     UI.SPACE +
                     FormatManager.formatTemperature(UI.convertTemperatureFromMetric(temperatureWindChill)) +
                     UI.UNIT_LABEL_TEMPERATURE);
      }

      // Wind
      final int windSpeed = tourData.getWeather_Wind_Speed();
      if (windSpeed != Float.MIN_VALUE) {

         final String windDirection = tourData.getWeather_Wind_Direction() != -1
               ? UI.SPACE +
                     Messages.Log_HistoricalWeatherRetriever_001_WeatherData_WindDirection +
                     UI.SPACE +
                     getWindDirectionText(tourData.getWeather_Wind_Direction())
               : UI.EMPTY_STRING;
         weatherDataList.add(Math.round(UI.convertSpeed_FromMetric(windSpeed)) +
               UI.UNIT_LABEL_SPEED +
               windDirection);
      }

      // Humidity
      final float humidity = tourData.getWeather_Humidity();
      if (humidity != Float.MIN_VALUE) {

         weatherDataList.add((int) humidity +
               UI.SYMBOL_PERCENTAGE +
               UI.SPACE +
               Messages.Log_HistoricalWeatherRetriever_001_WeatherData_Humidity);
      }

      // Pressure
      if (displayPressure) {

         weatherDataList.add(roundDoubleToFloat(tourData.getWeather_Pressure()) +
               UI.UNIT_LABEL_PRESSURE_MBAR_OR_INHG +
               UI.SPACE +
               Messages.Log_HistoricalWeatherRetriever_001_WeatherData_Pressure);
      }

      // Precipitation
      final float precipitation = tourData.getWeather_Precipitation();
      if (precipitation > 0) {

         weatherDataList.add(Messages.Log_HistoricalWeatherRetriever_001_WeatherData_Precipitation +
               UI.SPACE +
               roundDoubleToFloat(UI.convertPrecipitation_FromMetric(precipitation)) +
               UI.UNIT_LABEL_DISTANCE_MM_OR_INCH);
      }

      // Snowfall
      final float snowfall = tourData.getWeather_Snowfall();
      if (snowfall > 0) {

         weatherDataList.add(Messages.Log_HistoricalWeatherRetriever_001_WeatherData_Snowfall +
               UI.SPACE +
               roundDoubleToFloat(UI.convertPrecipitation_FromMetric(snowfall)) +
               UI.UNIT_LABEL_DISTANCE_MM_OR_INCH);
      }

      final String weatherData = String.join(UI.COMMA_SPACE, weatherDataList);

      return weatherData;
   }

   /**
    * Algorithm taken from:
    * https://www.scadacore.com/2014/12/19/average-wind-direction-and-wind-speed/
    * https://www.itron.com/na/blog/forecasting/computing-a-weighted-average-wind-speed-and-wind-direction-across-multiple-weather-stations
    *
    * @param windSpeeds
    *           An array of wind speeds in km/h
    * @param windDirections
    *           An array of wind directions in degrees
    * @return
    */
   public static int[] computeAverageWindSpeedAndDirection(final double[] windSpeeds,
                                                           final int[] windDirections) {

      final int[] averageWindSpeedAndDirection = new int[2];

      final int dataSize = windSpeeds.length;
      if (dataSize == 0 || dataSize != windDirections.length) {
         return averageWindSpeedAndDirection;
      }

      // Step 1: Break Out East/West and North/South Vectors
      float eastWestVectorArray = 0;
      float northSouthVectorArray = 0;

      for (int index = 0; index < dataSize; ++index) {

         final Double currentWindSpeed = windSpeeds[index];
         final double currentWindDirectionRadians = Math.toRadians(windDirections[index]);

         eastWestVectorArray += Math.sin(currentWindDirectionRadians) * currentWindSpeed;
         northSouthVectorArray += Math.cos(currentWindDirectionRadians) * currentWindSpeed;
      }

      final float eastWestVectorAverage = eastWestVectorArray / dataSize * -1;
      final float northSouthVectorAverage = northSouthVectorArray / dataSize * -1;

      // Step 2: Combine Vectors back into a direction and speed
      final double averageWindSpeed = Math.sqrt(
            Math.pow(eastWestVectorAverage, 2) +
                  Math.pow(northSouthVectorAverage, 2));

      averageWindSpeedAndDirection[0] = (int) Math.round(averageWindSpeed);

      final double atan2Direction = Math.atan2(eastWestVectorAverage, northSouthVectorAverage);

      double averageDirection = Math.toDegrees(atan2Direction);

      if (averageDirection > 180) {
         averageDirection -= 180;
      } else if (averageDirection < 180) {
         averageDirection += 180;
      }

      averageWindSpeedAndDirection[1] = (int) Math.round(averageDirection);

      return averageWindSpeedAndDirection;
   }

   /**
    * Determines the geographic area covered by a GPS track. The goal is to
    * encompass most of the track to search a weather station as close as possible
    * to the overall course and not just to a specific point.
    */
   static LatLng determineWeatherSearchAreaCenter(final TourData tour) {

      final double[] latitudeSerie = tour.latitudeSerie;
      final double[] longitudeSerie = tour.longitudeSerie;

      // Looking for the farthest point of the track
      double maxDistance = Double.MIN_VALUE;
      final LatLng startPoint = new LatLng(latitudeSerie[0], longitudeSerie[0]);
      LatLng furthestPoint = startPoint;

      for (int index = 1; index < latitudeSerie.length && index < longitudeSerie.length; ++index) {

         final LatLng currentPoint =
               new LatLng(latitudeSerie[index], longitudeSerie[index]);

         final double distanceFromStart =
               LatLngTool.distance(startPoint, currentPoint, LengthUnit.METER);

         if (distanceFromStart > maxDistance) {
            maxDistance = distanceFromStart;
            furthestPoint = currentPoint;
         }
      }

      final double distanceFromStart =
            LatLngTool.distance(startPoint, furthestPoint, LengthUnit.METER);
      final double bearingBetweenPoint =
            LatLngTool.initialBearing(startPoint, furthestPoint);

      // We find the center of the circle formed by the starting point and the farthest point
      final LatLng searchAreaCenter =
            LatLngTool.travel(startPoint, bearingBetweenPoint, distanceFromStart / 2, LengthUnit.METER);

      return searchAreaCenter;
   }

   /**
    * Returns an appropriate weather Emoji based on the tour weather icon.
    * To obtain the string representation of the icons in Unicode 7.0,
    * I used the below code:
    * https://stackoverflow.com/a/68537229/7066681
    *
    * @param weatherIndex
    * @return
    */
   public static String getWeatherIcon(final int weatherIndex) {

      String weatherIcon;

      switch (IWeather.cloudIcon[weatherIndex]) {

      case IWeather.WEATHER_ID_CLEAR:
         //https://emojipedia.org/sun/
         weatherIcon = "\u2600"; //$NON-NLS-1$
         break;
      case IWeather.WEATHER_ID_PART_CLOUDS:
         //https://emojipedia.org/sun-behind-cloud/
         weatherIcon = "\u26C5"; //$NON-NLS-1$
         break;
      case IWeather.WEATHER_ID_OVERCAST:
         weatherIcon = "\u2601"; //$NON-NLS-1$
         break;
      case IWeather.WEATHER_ID_SCATTERED_SHOWERS:
      case IWeather.WEATHER_ID_DRIZZLE:
         //https://emojipedia.org/sun-behind-rain-cloud/
         weatherIcon = "\ud83c\udf26"; //$NON-NLS-1$
         break;
      case IWeather.WEATHER_ID_RAIN:
         //https://emojipedia.org/cloud-with-rain/
         weatherIcon = "\ud83c\udf27"; //$NON-NLS-1$
         break;
      case IWeather.WEATHER_ID_LIGHTNING:
         //https://emojipedia.org/cloud-with-lightning/
         weatherIcon = "\ud83c\udf29"; //$NON-NLS-1$
         break;
      case IWeather.WEATHER_ID_SNOW:

         //https://emojipedia.org/snowflake/
         weatherIcon = "\u2744"; //$NON-NLS-1$

         //Below is the official "Cloud with snow" icon but because it looks too
         //much like the "Cloud with rain" icon, instead, we choose the "Snowflake"
         //icon.
         //https://emojipedia.org/cloud-with-snow/
         break;
      case IWeather.WEATHER_ID_SEVERE_WEATHER_ALERT:
         //https://emojipedia.org/warning/
         weatherIcon = "\u26A0"; //$NON-NLS-1$
         break;
      case UI.IMAGE_EMPTY_16:
      default:
         return UI.EMPTY_STRING;
      }

      return UI.SPACE1 + weatherIcon;
   }

   public static int getWeatherIndex(final String weatherClouds) {

      int weatherCloudsIndex = -1;

      if (StringUtils.hasContent(weatherClouds)) {
         // binary search cannot be done because it requires sorting which we cannot...
         for (int cloudIndex = 0; cloudIndex < IWeather.cloudIcon.length; ++cloudIndex) {
            if (IWeather.cloudIcon[cloudIndex].equalsIgnoreCase(weatherClouds)) {
               weatherCloudsIndex = cloudIndex;
               break;
            }
         }
      }

      return weatherCloudsIndex < 0 ? 0 : weatherCloudsIndex;
   }

   private static String getWindDirectionText(final int degreeDirection) {

      return IWeather.windDirectionText[UI.getCardinalDirectionTextIndex(degreeDirection)];
   }

   /**
    * Indicates if the hourly weather data set contains the weather information for every hour
    * of the tour
    *
    * @param weatherDataStartTime
    *           in seconds
    * @param weatherDataEndTime
    *           in seconds
    * @param tourStartTime
    *           in seconds
    * @param tourEndTime
    *           in seconds
    * @return
    */
   public static boolean isTourWeatherDataComplete(final long weatherDataStartTime,
                                                   final long weatherDataEndTime,
                                                   final long tourStartTime,
                                                   final long tourEndTime) {

      if (weatherDataStartTime > tourStartTime ||
            weatherDataEndTime + WeatherUtils.SECONDS_PER_THIRTY_MINUTE < tourEndTime) {
         return false;
      }

      return true;
   }

   public static float roundDoubleToFloat(final double value) {

      return Math.round(value * 100.0) / 100.0f;
   }
}
