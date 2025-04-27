/*******************************************************************************
 * Copyright (C) 2024, 2025 Frédéric Bard
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
package net.tourbook.nutrition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.tourbook.application.ApplicationVersion;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourBeverageContainer;
import net.tourbook.data.TourData;
import net.tourbook.data.TourNutritionProduct;
import net.tourbook.nutrition.openfoodfacts.Product;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.web.WEB;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.preference.IPreferenceStore;

public class NutritionUtils {

   /**
    * Documentation:
    * https://world.openfoodfacts.org/files/api-documentation.html#jump-SearchRequests-Searchingforproducts
    */
   private static final String           OPENFOODFACTS_SEARCH_BY_NAME_URL =
         "https://world.openfoodfacts.org/cgi/search.pl?action=process&sort_by=unique_scans_n&page_size=20&json=true&search_terms=";                                                                //$NON-NLS-1$
   private static final String           OPENFOODFACTS_SEARCH_BY_CODE_URL =
         "https://world.openfoodfacts.org/api/v3/product/%s?fields=code,brands,product_name,nutriscore_data,nutrition_data_per,nutriments,quantity,product_quantity,serving_quantity,serving_size"; //$NON-NLS-1$

   private static final String           OPENFOODFACTS_BASEPATH           = "https://world.openfoodfacts.org/product/";                                                                             //$NON-NLS-1$
   private static HttpClient             _httpClient                      = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(1)).build();

   private static final IPreferenceStore _prefStore                       = TourbookPlugin.getPrefStore();

   // Official documentation:
   // you have to add a User-Agent HTTP Header with the name of your app, the version, system and a url (if any), not to be blocked by mistake.
   // For example: User-Agent: NameOfYourApp - Android - Version 1.0 - www.yourappwebsite.com
   // Source: https://world.openfoodfacts.org/files/api-documentation.html
   private static final String       USER_AGENT = String.format("MyTourbook - %s - Version %s - https://mytourbook.sourceforge.io",              //$NON-NLS-1$
         System.getProperty("os.name"),                                                                                                          //$NON-NLS-1$
         ApplicationVersion.getVersionSimple());

   private static final NumberFormat _nf2       = NumberFormat.getNumberInstance();

   public static String buildTourBeverageContainerName(final TourBeverageContainer tourBeverageContainer) {

      return String.format("%s (%s %s)", tourBeverageContainer.getName(), tourBeverageContainer.getCapacity(), UI.UNIT_FLUIDS_L); //$NON-NLS-1$
   }

   public static String computeAverageCaloriesPerHour(final TourData tourData) {

      final int totalCalories = getTotalCalories(tourData.getTourNutritionProducts());
      final float averageCaloriesPerHour = computeAveragePerHour(tourData, totalCalories);
      final String averageCaloriesPerHourFormatted = FormatManager.formatNumber_0(averageCaloriesPerHour);

      return averageCaloriesPerHourFormatted;
   }

   public static String computeAverageCarbohydratesPerHour(final TourData tourData) {

      final int totalCarbohydrates = getTotalCarbohydrates(tourData.getTourNutritionProducts());
      final float averageCarbohydratesPerHour = computeAveragePerHour(tourData, totalCarbohydrates);
      final String averageCarbohydratesPerHourFormatted = FormatManager.formatNumber_0(averageCarbohydratesPerHour);

      return averageCarbohydratesPerHourFormatted;
   }

   public static String computeAverageFluidsPerHour(final TourData tourData) {

      final float totalFluids = getTotalFluids(tourData.getTourNutritionProducts()) * 100 / 100;
      final float averageFluidsPerHour = computeAveragePerHour(tourData, totalFluids);

      _nf2.setMinimumFractionDigits(0);
      _nf2.setMaximumFractionDigits(2);
      final String averageFluidsPerHourFormatted = _nf2.format(averageFluidsPerHour);

      return averageFluidsPerHourFormatted.equals("0") ? UI.EMPTY_STRING : averageFluidsPerHourFormatted; //$NON-NLS-1$
   }

   private static float computeAveragePerHour(final TourData tourData, final float totalAmount) {

      long tourDeviceTime_Elapsed = tourData.getTourDeviceTime_Elapsed();

      if (tourDeviceTime_Elapsed > 7200 && _prefStore.getBoolean(ITourbookPreferences.NUTRITION_IGNORE_FIRST_HOUR)) {
         tourDeviceTime_Elapsed -= 3600;
      } else if (tourDeviceTime_Elapsed <= 3600) {
         return totalAmount;
      }

      return totalAmount * 60 / (tourDeviceTime_Elapsed / 60f);
   }

   public static String computeAverageSodiumPerLiter(final TourData tourData) {

      final float totalSodium = getTotalSodium(tourData.getTourNutritionProducts());
      final float totalFluids = getTotalFluids(tourData.getTourNutritionProducts()) * 100 / 100;
      final float averageSodiumPerLiter = totalFluids == 0 ? 0 : totalSodium / totalFluids;
      final String averageSodiumPerLiterFormatted = FormatManager.formatNumber_0(averageSodiumPerLiter);

      return averageSodiumPerLiterFormatted;
   }

   private static List<Product> deserializeResponse(final String body, final ProductSearchType productSearchType) {

      final ObjectMapper mapper = new ObjectMapper();

      List<Product> deserializedProductsResults = new ArrayList<>();

      try {

         if (productSearchType == ProductSearchType.ByCode) {

            String productResult;
            productResult = mapper.readValue(body, JsonNode.class)
                  .get("product") //$NON-NLS-1$
                  .toString();
            deserializedProductsResults.add(mapper.readValue(productResult,
                  new TypeReference<Product>() {}));

         } else if (productSearchType == ProductSearchType.ByName) {

            final String productsResults = mapper.readValue(body, JsonNode.class)
                  .get("products") //$NON-NLS-1$
                  .toString();
            deserializedProductsResults = mapper.readValue(productsResults,
                  new TypeReference<List<Product>>() {});

         }
      } catch (final JsonProcessingException e) {
         StatusUtil.log(e);
      }

      return deserializedProductsResults;

   }

   public static int getTotalCalories(final Set<TourNutritionProduct> tourNutritionProducts) {

      int totalCalories = 0;

      for (final TourNutritionProduct tourNutritionProduct : tourNutritionProducts) {

         switch (tourNutritionProduct.getQuantityType()) {

         case Servings:

            totalCalories += tourNutritionProduct.getCalories_Serving() * tourNutritionProduct.getConsumedQuantity();
            break;

         case Products:

            totalCalories += tourNutritionProduct.getCalories() * tourNutritionProduct.getConsumedQuantity();
            break;
         }
      }

      return totalCalories;
   }

   public static int getTotalCarbohydrates(final Set<TourNutritionProduct> tourNutritionProducts) {

      int totalCarbohydrates = 0;

      for (final TourNutritionProduct tourNutritionProduct : tourNutritionProducts) {

         switch (tourNutritionProduct.getQuantityType()) {

         case Servings:

            totalCarbohydrates += tourNutritionProduct.getCarbohydrates_Serving() * tourNutritionProduct.getConsumedQuantity();
            break;

         case Products:

            totalCarbohydrates += tourNutritionProduct.getCarbohydrates() * tourNutritionProduct.getConsumedQuantity();
            break;
         }
      }

      return totalCarbohydrates;
   }

   /**
    * Computes the total amount of fluids consumed for a given list of {@link TourNutritionProduct}
    *
    * @param tourNutritionProducts
    *
    * @return
    */
   public static float getTotalFluids(final Set<TourNutritionProduct> tourNutritionProducts) {

      float totalFluids = 0;

      for (final TourNutritionProduct tourNutritionProduct : tourNutritionProducts) {

         final TourBeverageContainer tourBeverageContainer = tourNutritionProduct.getTourBeverageContainer();
         if (tourBeverageContainer != null) {

            totalFluids += tourNutritionProduct.getContainersConsumed()
                  * tourBeverageContainer.getCapacity();

         } else if (tourNutritionProduct.isBeverage()) {

            totalFluids += tourNutritionProduct.getBeverageQuantity() / 1000f
                  * tourNutritionProduct.getConsumedQuantity();

         }
      }

      return totalFluids;
   }

   /**
    * Computes the total amount of sodium consumed for a given list of {@link TourNutritionProduct}
    *
    * @param tourNutritionProducts
    *
    * @return
    */
   public static float getTotalSodium(final Set<TourNutritionProduct> tourNutritionProducts) {

      int totalSodium = 0;

      for (final TourNutritionProduct tourNutritionProduct : tourNutritionProducts) {

         switch (tourNutritionProduct.getQuantityType()) {

         case Servings:

            totalSodium += tourNutritionProduct.getSodium_Serving() * tourNutritionProduct.getConsumedQuantity();
            break;

         case Products:

            totalSodium += tourNutritionProduct.getSodium() * tourNutritionProduct.getConsumedQuantity();
            break;
         }
      }

      return totalSodium;
   }

   public static void openProductWebPage(final String productCode) {

      if (StringUtils.isNumeric(productCode)) {
         WEB.openUrl(OPENFOODFACTS_BASEPATH + productCode);
      }
   }

   public static List<Product> searchProduct(final String searchText, final ProductSearchType productSearchType) {

      String searchUrl = UI.EMPTY_STRING;

      switch (productSearchType) {

      case ByCode:
         searchUrl = String.format(OPENFOODFACTS_SEARCH_BY_CODE_URL, searchText);
         break;

      case ByName:
         searchUrl = OPENFOODFACTS_SEARCH_BY_NAME_URL + searchText.replace(UI.SPACE1, UI.SYMBOL_PLUS);
         break;
      }

      final HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .header(WEB.HTTP_HEADER_USER_AGENT, USER_AGENT)
            .uri(URI.create(searchUrl))
            .build();

      try {
         final HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString());

         if (response.statusCode() == HttpURLConnection.HTTP_OK && net.tourbook.common.util.StringUtils.hasContent(response.body())) {

            return deserializeResponse(response.body(), productSearchType);

         } else {
            StatusUtil.logError(response.body());
         }
      } catch (IOException | InterruptedException e) {
         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }

      return new ArrayList<>();
   }
}
