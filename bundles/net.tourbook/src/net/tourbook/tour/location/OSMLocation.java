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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import net.tourbook.common.UI;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OSMLocation {

   private static final char NL = UI.NEW_LINE;

   /**
    * <pre>
    *
    *   {
    *      "place_id"      : 78981669,
    *      "osm_id"        : 44952014,
   
    *      "licence"       : "Data © OpenStreetMap contributors, ODbL 1.0. http://osm.org/copyright",
    *
    *      "place_rank"    : 30,
    *      "importance"    : 0.00000999999999995449,
    *
    *      "addresstype"   : "leisure",
    *      "class"         : "leisure",
    *      "osm_type"      : "way",
    *      "type"          : "pitch",
   
    *      "lat"           : "47.116116899999994",
    *      "lon"           : "7.989645450000001",
    *
    *      "name"          : "",
    *      "display_name"  : "5a, Schlossfeldstrasse, St. Niklausenberg, Guon, Willisau, Luzern, 6130, Schweiz/Suisse/Svizzera/Svizra",
    *
    *      "address": {
    *         "house_number"     : "5a",
    *         "road"             : "Schlossfeldstrasse",
    *         "neighbourhood"    : "St. Niklausenberg",
    *         "farm"             : "Guon",
    *         "village"          : "Willisau",
    *         "state"            : "Luzern",
    *         "ISO3166-2-lvl4"   : "CH-LU",
    *         "postcode"         : "6130",
    *         "country"          : "Schweiz/Suisse/Svizzera/Svizra",
    *         "country_code"     : "ch"
    *      },
    *
    *      "boundingbox": [
    *         "47.1159171",
    *         "47.1163167",
    *         "7.9895150",
    *         "7.9897759"
    *      ]
    *   }
    *
    * </pre>
    */

   public String             name;
   public String             display_name;

   public OSMAddress         address;

   public double             lat;
   public double             lon;
   public double[]           boundingbox;

   public long               place_id;
   public long               osm_id;

   public String             licence;

   public String             osm_type;
   public String             type;
   public String             addresstype;

   @JsonAlias({ "class" })
   public String             locationClass;

   public int                place_rank;
   public double             importance;

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "OSMLocation" + NL //                               //$NON-NLS-1$

//          + " place_id       = " + place_id + NL //              //$NON-NLS-1$
//          + " osm_id         = " + osm_id + NL //                //$NON-NLS-1$
//          + " licence        = " + licence + NL //               //$NON-NLS-1$
//          + " osm_type       = " + osm_type + NL //              //$NON-NLS-1$
//          + " type           = " + type + NL //                  //$NON-NLS-1$
//          + " addresstype    = " + addresstype + NL //           //$NON-NLS-1$
//          + " locationClass  = " + locationClass + NL //         //$NON-NLS-1$
//          + " lat            = " + lat + NL //                   //$NON-NLS-1$
//          + " lon            = " + lon + NL //                   //$NON-NLS-1$
            + " name           = " + name + NL //                  //$NON-NLS-1$
            + " display_name   = " + display_name + NL //          //$NON-NLS-1$
//          + " place_rank     = " + place_rank + NL //            //$NON-NLS-1$
//          + " importance     = " + importance + NL //            //$NON-NLS-1$

//          + " boundingbox    = " + (boundingbox != null //       //$NON-NLS-1$
//               ? Arrays.toString(boundingbox)
//               : UI.EMPTY_STRING) + NL

            + NL

            + " address        = " + address + NL //              //$NON-NLS-1$

      ;
   }

}
