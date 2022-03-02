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
import com.javadocmd.simplelatlng.util.LengthUnit;

import java.util.ArrayList;
import java.util.List;

import net.tourbook.common.UI;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.weather.IWeather;
import net.tourbook.data.TourData;

public class WeatherUtils {
   /**
    * Returns the weather data as a human readable string, depending on the
    * desired data.
    * Example: ☀, Sunny, 98.6°F, 21.2°F, 11.184682mph, from , 1456.6929inch
    *
    * @param tourData
    * @return
    */
   public static String buildWeatherDataString(final TourData tourData,
                                               final boolean displayMinimumTemperature,
                                               final boolean displayMaximumTemperature) {

      final List<String> weatherDataList = new ArrayList<>();

      //todo fb put it in the unit tests by manually entering all the weather data, or taking it from a tour when the Openweather api is usable
      final String weatherIcon = getWeatherIcon(tourData.getWeatherIndex());
      if (StringUtils.hasContent(weatherIcon)) {
         weatherDataList.add(weatherIcon.trim());
      }

      final String weatherText = tourData.getWeather();
      if (StringUtils.hasContent(weatherText)) {

         if (weatherDataList.size() == 1) {
            weatherDataList.set(0, weatherDataList.get(0) + UI.SPACE + weatherText);
         } else {
            weatherDataList.add(weatherText);
         }
      }

      final float averageTemperature = tourData.getWeather_Temperature_Average();
      if (averageTemperature != Float.MIN_VALUE) {
         weatherDataList.add(Math.round(UI.convertTemperatureFromMetric(averageTemperature)) +
               UI.UNIT_LABEL_TEMPERATURE);
      }

      //"feels like"
      final float temperatureWindChill = tourData.getWeather_Temperature_WindChill();
      if (temperatureWindChill != Float.MIN_VALUE) {
         weatherDataList.add("feels like " +
               Math.round(UI.convertTemperatureFromMetric(temperatureWindChill)) +
               UI.UNIT_LABEL_TEMPERATURE);
      }

      //humidity
      final float humidity = tourData.getWeather_Humidity();
      if (humidity != Float.MIN_VALUE) {
         weatherDataList.add(humidity + UI.SYMBOL_PERCENTAGE);
      }

      //  wind \
      final int windSpeed = tourData.getWeatherWindSpeed();
      if (windSpeed != Float.MIN_VALUE) {

         final String windDirection = tourData.getWeatherWindDir() != -1
               ? " from " + getWindDirectionTextIndex(tourData.getWeatherWindDir())
               : UI.EMPTY_STRING;
         weatherDataList.add(Math.round(UI.convertSpeed_FromMetric(windSpeed)) +
               UI.UNIT_LABEL_SPEED +
               windDirection);
      }

      final float precipitation = tourData.getWeather_Precipitation();
      if (precipitation > 0) {
         weatherDataList.add(Math.round(UI.convertPrecipitation_FromMetric(precipitation)) +
               UI.UNIT_LABEL_DISTANCE_MM_OR_INCH);
      }

      final String weatherData = String.join(UI.COMMA_SPACE, weatherDataList);

      return weatherData;
   }

   /**
    * Determines the geographic area covered by a GPS track. The goal is to
    * encompass most of the track to search a weather station as close as possible
    * to the overall course and not just to a specific point.
    */
   public static LatLng determineWeatherSearchAreaCenter(final TourData tour) {

      final double[] latitudeSerie = tour.latitudeSerie;
      final double[] longitudeSerie = tour.longitudeSerie;

      // Looking for the farthest point of the track
      LatLng furthestPoint = null;
      double maxDistance = Double.MIN_VALUE;
      final LatLng startPoint = new LatLng(latitudeSerie[0], longitudeSerie[0]);

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

   public static  String getWindDirectionTextIndex(final int degreeDirection) {

      //todo fb
      //if all the weather data is empty <= for old tours (refactored method that do all the necessary testing in TourData.java ?)
      // or if degreeDirection == -1

      int directionIndex = 0;
      if (degreeDirection != -1) {

      final float degree = (degreeDirection / 10.0f + 11.25f) / 22.5f;

         directionIndex = ((int) degree) % 16;
      }

      return IWeather.windDirectionText[ directionIndex + 1];
   }

}
