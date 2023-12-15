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

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourFuelProduct;
import net.tourbook.nutrition.openfoodfacts.Product;

public class NutritionUtils {

   private static final String OPENFOODFACTS_SEARCH_URL =
         "https://world.openfoodfacts.org/cgi/search.pl?action=process&sort_by=unique_scans_n&page_size=20&json=true&search_terms="; //$NON-NLS-1$

   private static HttpClient   _httpClient              = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(5)).build();

   public static String buildNutritionDataString(final Set<TourFuelProduct> set) {

      if (set.isEmpty()) {
         return UI.EMPTY_STRING;
      }

      final StringBuilder sb = new StringBuilder() ;
      set.stream().forEach(i -> {

         //todo fb serving (eng), portion (french)

         sb.append(i.getServingsConsumed() + " serving of " + i.getName() + UI.NEW_LINE);

         //todo fb
         if (i.isFluid()) {
            // 1.5 flask (.75L) of Gu Brew
            //sb.append(i.getServingsConsumed() + " serving of " + i.getName() + UI.NEW_LINE);

         }
      });

      return sb.toString();
   }

   public static int getTotalCalories(final Set<TourFuelProduct> tourFuelProducts) {

      final int totalCalories = tourFuelProducts.stream().mapToInt(i -> i.getCalories() * i.getServingsConsumed()).sum();

      return totalCalories;
   }

   public static double getTotalFluids(final Set<TourFuelProduct> tourFuelProducts) {

      //todo fb
      //final double totalFluids = tourFuelProducts.stream().mapToDouble(i -> i.getFluid() * i.getServingsConsumed()).sum();

      return 0;
   }

   public static double getTotalSodium(final Set<TourFuelProduct> tourFuelProducts) {

      final double totalSodium = tourFuelProducts.stream().mapToDouble(i -> i.getSodium() * i.getServingsConsumed()).sum();

      return totalSodium;
   }

   public static List<Product> searchProduct(final String productName) {

      final HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(OPENFOODFACTS_SEARCH_URL + productName.replace(" ", "+")))
            .build();

      List<Product> serializedProductsResults = new ArrayList<>();

      try {
         final HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString());

         if (response.statusCode() == HttpURLConnection.HTTP_OK && StringUtils.hasContent(response.body())) {

            final ObjectMapper mapper = new ObjectMapper();
            final String productsResults = mapper.readValue(response.body(), JsonNode.class)
                  .get("products") //$NON-NLS-1$
                  .toString();

            serializedProductsResults = mapper.readValue(productsResults,
                  new TypeReference<List<Product>>() {});
            return serializedProductsResults;

            //      final String titi = response.body();

            // System.out.println(titi);
//            final var toto = new ObjectMapper().readValue(response.body(), List<ProductResponse.class>);
//            System.out.println(toto.getProduct().getProductName())
         } else {
            StatusUtil.logError(response.body());
         }
      } catch (IOException | InterruptedException e) {
         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }

      return serializedProductsResults;
   }
}
