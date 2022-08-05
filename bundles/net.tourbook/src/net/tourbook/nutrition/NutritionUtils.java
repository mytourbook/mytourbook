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

import java.net.http.HttpClient;
import java.time.Duration;

import pl.coderion.model.ProductResponse;

public class NutritionUtils {

   private static HttpClient _httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(5)).build();

   public static void testSdk() {

      // perform the search by code
      //get a response
      final ProductResponse response = null;

   }
}
