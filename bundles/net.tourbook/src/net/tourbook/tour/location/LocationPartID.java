/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.location;

public enum LocationPartID {

   NONE, //

   OSM_DEFAULT_NAME, //
   OSM_NAME, //

//   CUSTOM_CITY_LARGEST, //
//   CUSTOM_CITY_SMALLEST, //
//   CUSTOM_CITY_WITH_ZIP_LARGEST, //
//   CUSTOM_CITY_WITH_ZIP_SMALLEST, //

   CUSTOM_STREET_WITH_HOUSE_NUMBER, //

   /**
    * This is a computed part
    */
   settlementSmall, //

   /**
    * The following names are all from <a href=
    * "https://nominatim.org/release-docs/develop/api/Output/#addressdetails">https://nominatim.org/
    * release-docs/develop/api/Output/#addressdetails</a>
    */

   continent, //
   country, //
   country_code, //

   region, //
   state, //
   state_district, //
   county, //

   ISO3166_2_lvl4, //

   municipality, //
   city, //
   town, //
   village, //

   city_district, //
   district, //
   borough, //
   suburb, //
   subdivision, //

   hamlet, //
   croft, //
   isolated_dwelling, //

   neighbourhood, //
   allotments, //
   quarter, //

   city_block, //
   residential, //
   farm, //
   farmyard, //
   industrial, //
   commercial, //
   retail, //

   road, //

   house_number, //
   house_name, //

   aerialway, //
   aeroway, //
   amenity, //
   boundary, //
   bridge, //
   club, //
   craft, //
   emergency, //
   historic, //
   landuse, //
   leisure, //
   man_made, //
   military, //
   mountain_pass, //
   natural2, //
   office, //
   place, //
   railway, //
   shop, //
   tourism, //
   tunnel, //
   waterway, //

   postcode, //

}
