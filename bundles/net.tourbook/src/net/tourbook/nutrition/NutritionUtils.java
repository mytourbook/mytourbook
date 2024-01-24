/*******************************************************************************
 * Copyright (C) 2024 Frédéric Bard
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.tourbook.application.ApplicationVersion;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourBeverageContainer;
import net.tourbook.data.TourNutritionProduct;
import net.tourbook.nutrition.openfoodfacts.Product;

public class NutritionUtils {

   /**
    * Documentation:
    * https://world.openfoodfacts.org/files/api-documentation.html#jump-SearchRequests-Searchingforproducts
    */
   private static final String OPENFOODFACTS_SEARCH_BY_NAME_URL =
         "https://world.openfoodfacts.org/cgi/search.pl?action=process&sort_by=unique_scans_n&page_size=20&json=true&search_terms=";                                             //$NON-NLS-1$
   private static final String OPENFOODFACTS_SEARCH_BY_CODE_URL =
         "https://world.openfoodfacts.org/api/v3/product/%s?fields=code,brands,product_name,nutriscore_data,nutriments,quantity,product_quantity,serving_quantity,serving_size"; //$NON-NLS-1$

   private static HttpClient   _httpClient                      = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(5)).build();
   // todo fb ask wolfgang how to do that. I don't mind putting my email but not in the public github repo
   private static final String _userAgent                       = "MyTourbook/" + ApplicationVersion.getVersionSimple() + " ()";                                                 //$NON-NLS-1$

   public static String buildTourBeverageContainerName(final TourBeverageContainer tourBeverageContainer) {

      return tourBeverageContainer.getName() + " (" + tourBeverageContainer.getCapacity() + " L)";
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

   public static String getProductFullName(final String brand, final String name) {

      return Stream.of(brand, name)
            .filter(string -> StringUtils.hasContent(string))
            .collect(Collectors.joining(UI.DASH_WITH_SPACE));
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

   public static double getTotalSodium(final Set<TourNutritionProduct> tourNutritionProducts) {

      final double totalSodium = tourNutritionProducts.stream().mapToDouble(i -> i.getSodium() * i.getConsumedQuantity()).sum();

      return totalSodium;
   }

   static List<Product> searchProduct(final String searchText, final ProductSearchType productSearchType) {

      URI searchUri = URI.create(UI.EMPTY_STRING);

      switch (productSearchType) {

      case ByCode:
         searchUri = URI.create(String.format(OPENFOODFACTS_SEARCH_BY_CODE_URL, searchText));
         break;

      case ByName:
         searchUri = URI.create(OPENFOODFACTS_SEARCH_BY_NAME_URL + searchText.replace(" ", "+"));
         break;
      }

      final HttpRequest request = HttpRequest.newBuilder()
            .GET()
            // .header(WEB.HTTP_HEADER_USER_AGENT, _userAgent)
            .uri(searchUri)
            .build();

      try {
         final HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString());

         if (response.statusCode() == HttpURLConnection.HTTP_OK && StringUtils.hasContent(response.body())) {

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
