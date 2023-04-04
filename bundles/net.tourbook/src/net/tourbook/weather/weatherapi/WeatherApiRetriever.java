/*******************************************************************************
 * Copyright (C) 2022, 2023 Frédéric Bard
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.weather.IWeather;
import net.tourbook.data.TourData;
import net.tourbook.weather.HistoricalWeatherRetriever;
import net.tourbook.weather.WeatherUtils;

public class WeatherApiRetriever extends HistoricalWeatherRetriever {

   private static final String baseApiUrl    = WeatherUtils.OAUTH_PASSEUR_APP_URL + "/weatherapi"; //$NON-NLS-1$

   private HistoryResult       historyResult = null;

   public WeatherApiRetriever(final TourData tourData) {

      super(tourData);
   }

   public static String convertWeatherCodeToMTWeatherClouds(final int weatherCode) {

      String weatherType;

      // Weather Icons and Codes  : https://www.weatherapi.com/docs/#weather-icons
      switch (weatherCode) {
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
      case 1207:
      case 1243:
      case 1246:
      case 1252:
      case 1276:
         weatherType = IWeather.WEATHER_ID_RAIN;
         break;
      case 1066:
      case 1069:
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
      case 1186:
      case 1189:
      case 1204:
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
      case 1180:
      case 1183:
      case 1198:
         weatherType = IWeather.WEATHER_ID_DRIZZLE;
         break;
      default:
         weatherType = UI.EMPTY_STRING;
         break;
      }

      return weatherType;
   }

   public static String getBaseApiUrl() {
      return baseApiUrl;
   }

   @Override
   protected String buildDetailedWeatherLog(final boolean isCompressed) {

      final List<String> fullWeatherDataList = new ArrayList<>();

      for (final Hour hour : historyResult.getHourList()) {

         final TourDateTime tourDateTime = TimeTools.createTourDateTime(
               hour.getTime_epoch() * 1000L,
               tour.getTimeZoneId());

         final boolean isDisplayEmptyValues = !isCompressed;
         String fullWeatherData = WeatherUtils.buildFullWeatherDataString(
               (float) hour.getTemp_c(),
               WeatherUtils.getWeatherIcon(
                     WeatherUtils.getWeatherIndex(
                           convertWeatherCodeToMTWeatherClouds(
                                 hour.getCondition().getCode()))),
               hour.getCondition().getText(),
               (float) hour.getFeelslike_c(),
               (float) hour.getWind_kph(),
               hour.getWind_degree(),
               hour.getHumidity(),
               (int) hour.getPressure_mb(),
               (float) hour.getPrecip_mm(),
               0,
               0,
               tourDateTime,
               isDisplayEmptyValues);

         if (isCompressed) {
            fullWeatherData = fullWeatherData.replaceAll("\\s+", UI.SPACE1); //$NON-NLS-1$
         }

         fullWeatherDataList.add(fullWeatherData);
      }

      final String fullWeatherData = String.join(
            UI.SYSTEM_NEW_LINE,
            fullWeatherDataList);

      return fullWeatherData;
   }

   private String buildWeatherApiRequest(final LocalDate requestedDate) {

      final StringBuilder weatherRequestWithParameters = new StringBuilder(baseApiUrl + UI.SYMBOL_QUESTION_MARK);

// SET_FORMATTING_OFF

      weatherRequestWithParameters.append(      "lat"  + "=" + searchAreaCenter.getLatitude()); //$NON-NLS-1$ //$NON-NLS-2$
      weatherRequestWithParameters.append("&" + "lon"  + "=" + searchAreaCenter.getLongitude()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      weatherRequestWithParameters.append("&" + "lang" + "=" + Locale.getDefault().getLanguage()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      weatherRequestWithParameters.append("&" + "dt"   + "=" + requestedDate.format(TimeTools.Formatter_YearMonthDay)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

// SET_FORMATTING_ON

      return weatherRequestWithParameters.toString();
   }

   private HistoryResult deserializeWeatherData(final String weatherDataResponse) {

      HistoryResult newHistoryResult = null;
      try {

         final ObjectMapper mapper = new ObjectMapper();
         final String weatherResults = mapper
               .readValue(weatherDataResponse, JsonNode.class)
               .toString();

         newHistoryResult = mapper.readValue(
               weatherResults,
               new TypeReference<HistoryResult>() {});

      } catch (final Exception e) {

         StatusUtil.logError(
               "WeatherApiRetriever.deserializeWeatherData : Error while " + //$NON-NLS-1$
                     "deserializing the historical weather JSON object :" //$NON-NLS-1$
                     + weatherDataResponse + UI.SYSTEM_NEW_LINE + e.getMessage());
      }

      return newHistoryResult;
   }

   @Override
   public boolean retrieveHistoricalWeatherData() {

      historyResult = new HistoryResult();

      LocalDate requestedDate = tour.getTourStartTime().toLocalDate();

      final LocalDate tomorrow = LocalDate.now().plusDays(1);

      //Send an API request as long as we don't have the results covering the
      //entire duration of the tour
      while (true) {

         final String weatherRequestWithParameters = buildWeatherApiRequest(requestedDate);

         final String rawWeatherData = sendWeatherApiRequest(weatherRequestWithParameters);
         if (StringUtils.isNullOrEmpty(rawWeatherData)) {
            return false;
         }

         final HistoryResult newHistoryResult = deserializeWeatherData(rawWeatherData);
         if (newHistoryResult == null) {
            return false;
         }

         historyResult.addHourList(newHistoryResult.getForecastdayHourList());
         final List<Hour> hourList = historyResult.getHourList();

         final int lastWeatherDataHour = hourList.get(hourList.size() - 1).getTime_epoch();
         if (WeatherUtils.isTourWeatherDataComplete(
               hourList.get(0).getTime_epoch(),
               lastWeatherDataHour,
               tourStartTime,
               tourEndTime)) {
            break;
         }

         //Setting the requested time to the next day to retrieve the next set of weather data
         requestedDate = requestedDate.plusDays(1);

         // We avoid requesting data in the future
         if (requestedDate.compareTo(tomorrow) > 0) {

            if (hourList.isEmpty()) {
               return false;
            } else {
               break;
            }
         }
      }

      final boolean hourlyDataExists = historyResult.filterHourData(tourStartTime, tourEndTime);
      if (!hourlyDataExists) {
         return false;
      }

      //We look for the weather data in the middle of the tour to populate the weather conditions
      historyResult.findMiddleHour(tourMiddleTime);

// SET_FORMATTING_OFF

      tour.setWeather(                       historyResult.getWeatherDescription());
      tour.setWeather_Clouds(                historyResult.getWeatherType());

      tour.setWeather_Temperature_Average(   historyResult.getTemperatureAverage());
      tour.setWeather_Humidity((short)       historyResult.getAverageHumidity());
      tour.setWeather_Precipitation(         historyResult.getTotalPrecipitation());
      tour.setWeather_Pressure((short)       historyResult.getAveragePressure());
      tour.setWeather_Temperature_Max(       historyResult.getTemperatureMax());
      tour.setWeather_Temperature_Min(       historyResult.getTemperatureMin());
      tour.setWeather_Temperature_WindChill( historyResult.getAverageWindChill());

      historyResult.computeAverageWindSpeedAndDirection();
      tour.setWeather_Wind_Speed(            historyResult.getAverageWindSpeed());
      tour.setWeather_Wind_Direction(        historyResult.getAverageWindDirection());

// SET_FORMATTING_ON

      return true;
   }
}
