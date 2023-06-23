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
import java.util.List;

import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.nutrition.openfoodfacts.Product;

import pl.coderion.model.ProductResponse;
import pl.coderion.service.impl.OpenFoodFactsWrapperImpl;

public class NutritionUtils {

   private static final String OPENFOODFACTS_SEARCH_URL =
         "https://world.openfoodfacts.org/cgi/search.pl?action=process&sort_by=unique_scans_n&page_size=20&json=true&search_terms="; //$NON-NLS-1$

   private static HttpClient _httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(5)).build();

   public static List<Product> searchProduct(final String productName)
   {
      final OpenFoodFactsWrapperImpl wrapper = new OpenFoodFactsWrapperImpl();
      final ProductResponse productResponse = wrapper.fetchProductByCode("737628064502");

      final HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(OPENFOODFACTS_SEARCH_URL + productName.replace(" ", "+")))
            .build();

      try {
         final HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString());

         if (response.statusCode() == HttpURLConnection.HTTP_OK && StringUtils.hasContent(response.body())) {

            //weather
            final ObjectMapper mapper = new ObjectMapper();
            final String weatherResults = mapper.readValue(response.body(), JsonNode.class)
                  .get("products") //$NON-NLS-1$
                  .toString();

            final var serializedWeatherData = mapper.readValue(weatherResults,
                  new TypeReference<List<Product>>() {});
            return serializedWeatherData;

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

      return null;
   }

   public static void testSdk() {

      // perform the search by name
      //
      //perform the search by code

      //get a response
//      final ProductResponse response = new ProductResponse();
//      final String productName = response.getProduct().getProductName();
//      final float carbs = response.getProduct().getNutriments().getCarbohydrates();
//      final float sodium = response.getProduct().getNutriments().getSodium();
      //didn't find the caffeine, ask them ?
      //it's harder for the fluid because we need to know the size of the flask...

   }
}
