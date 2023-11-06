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
package net.tourbook.tour;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import net.tourbook.common.UI;

/**
 * Possible address fields:
 *
 * https://nominatim.org/release-docs/develop/api/Output/#addressdetails
 *
 * <pre>
 *
 *    continent
 *    country
 *    country_code
 *
 *    region
 *    state
 *    state_district
 *    county
 *    ISO3166-2-lvl
 *
 *    municipality
 *    city
 *    town
 *    village
 *
 *    city_district
 *    district
 *    borough
 *    suburb
 *    subdivision
 *
 *    hamlet
 *    croft
 *    isolated_dwelling
 *
 *    neighbourhood
 *    allotments
 *    quarter
 *
 *    city_block
 *    residential
 *    farm
 *    farmyard
 *    industrial
 *    commercial
 *    retail
 *
 *    road
 *
 *    house_number
 *    house_name
 *
 *    emergency
 *    historic
 *    military
 *    natural
 *    landuse
 *    place
 *    railway
 *    man_made
 *    aerialway
 *    boundary
 *    amenity
 *    aeroway
 *    club
 *    craft
 *    leisure
 *    office
 *    mountain_pass
 *    shop
 *    tourism
 *    bridge
 *    tunnel
 *    waterway
 *
 *    postcode
 *
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class OSMAddress {

   private static final char NL = UI.NEW_LINE;

   public String             country;
   public String             country_code;
   public String             neighbourhood;
   public String             farm;

   public String             house_number;
   public String             road;
   public String             hamlet;
   public String             town;
   public String             county;
   public String             state;
   public String             postcode;

   public String             village;
   public String             municipality;

   public String             amenity;

   @JsonAlias({ "ISO3166-2-lvl4" })
   public String             ISO3166_2_lvl4;

   public String logAddress() {

      return UI.EMPTY_STRING

            + "OSMAddress" + NL //                                   //$NON-NLS-1$

            + NL

            + "  country         = " + country + NL //               //$NON-NLS-1$
            + "  country_code    = " + country_code + NL //          //$NON-NLS-1$
            + "  state           = " + state + NL //                 //$NON-NLS-1$

            + NL

            + "  county          = " + county + NL //                //$NON-NLS-1$
            + "  village         = " + village + NL //               //$NON-NLS-1$
            + "  hamlet          = " + hamlet + NL //                //$NON-NLS-1$
            + "  municipality    = " + municipality + NL //          //$NON-NLS-1$
            + "  neighbourhood   = " + neighbourhood + NL //         //$NON-NLS-1$
            + "  farm            = " + farm + NL //                  //$NON-NLS-1$

            + NL

            + "  postcode        = " + postcode + NL //              //$NON-NLS-1$
            + "  town            = " + town + NL //                  //$NON-NLS-1$
            + "  road            = " + road + NL //                  //$NON-NLS-1$
            + "  house_number    = " + house_number + NL //          //$NON-NLS-1$

//          + " ISO3166_2_lvl4 = " + ISO3166_2_lvl4 + NL //          //$NON-NLS-1$

      ;
   }

}
