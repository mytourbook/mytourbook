/*******************************************************************************
 * Copyright (C) 2023, 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.data;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.location.LocationPartID;
import net.tourbook.tour.location.PartItem;

/**
 * Possible address fields are from <br>
 * <a href=
 * "https://nominatim.org/release-docs/develop/api/Output/#addressdetails">https://nominatim.org/release-docs/develop/api/Output/#addressdetails</a><br>
 * <a href=
 * "https://nominatim.org/release-docs/develop/api/Reverse/">https://nominatim.org/release-docs/develop/api/Reverse/</a>
 *
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
 *      "boundingbox":
 *      [
 *         "47.1159171",
 *         "47.1163167",
 *         "7.9895150",
 *         "7.9897759"
 *      ]
 *   }
 *
 * </pre>
 */
@Entity
public class TourLocation implements Serializable {

   private static final long       serialVersionUID = 1L;

   private static final char       NL               = UI.NEW_LINE;

   public static final int         DB_FIELD_LENGTH  = 1000;

   /**
    * Fields which are not displayed as location part
    */
   public static final Set<String> IGNORED_FIELDS   = Stream.of(

         "ISO3166_2_lvl4",                                                             //$NON-NLS-1$

         "name",                                                                       //$NON-NLS-1$
         "display_name",                                                               //$NON-NLS-1$

         "latitudeMinE6_Normalized",                                                   //$NON-NLS-1$
         "latitudeMaxE6_Normalized",                                                   //$NON-NLS-1$
         "longitudeMinE6_Normalized",                                                  //$NON-NLS-1$
         "longitudeMaxE6_Normalized",                                                  //$NON-NLS-1$

         "latitudeMinE6_Resized_Normalized",                                           //$NON-NLS-1$
         "latitudeMaxE6_Resized_Normalized",                                           //$NON-NLS-1$
         "longitudeMinE6_Resized_Normalized",                                          //$NON-NLS-1$
         "longitudeMaxE6_Resized_Normalized"                                           //$NON-NLS-1$

   ).collect(Collectors.toCollection(HashSet::new));

   /**
    * Contains the entity id
    */
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long                    locationID       = TourDatabase.ENTITY_IS_NOT_SAVED;

   public int                      zoomlevel;

   /*
    * Fields from {@link OSMLocation}, the field names are kept from the downloaded location data
    * when possible
    */
   public String name;
   public String display_name;

   /*
    * Location bounding box values
    */

   /**
    * Contains the normalized latitude value of the requested location
    * <p>
    * <code>normalized = latitude + 90</code>
    */
   public int latitudeE6_Normalized;

   /**
    * Contains the normalized longitude value of the requested location
    * <p>
    * normalized = longitude + 180
    */
   public int longitudeE6_Normalized;

   /**
    * Contains the normalized latitude min value, it's the cardinal direction south.
    * <p>
    * The min value could be larger than the max value when bounding box is resized.
    * <p>
    * <code>normalized = latitude + 90</code>
    */
   public int latitudeMinE6_Normalized;

   /**
    * Contains the normalized latitude max value, it's the cardinal direction north
    * <p>
    * The min value could be larger than the max value when bounding box is resized.
    * <p>
    * normalized = latitude + 90
    */
   public int latitudeMaxE6_Normalized;

   /**
    * Contains the normalized longitude min value, it's the cardinal direction west
    * <p>
    * The min value could be larger than the max value when bounding box is resized.
    * <p>
    * normalized = longitude + 180
    */
   public int longitudeMinE6_Normalized;

   /**
    * Contains the normalized longitude max value, it's the cardinal direction east
    * <p>
    * The min value could be larger than the max value when bounding box is resized.
    * <p>
    * normalized = longitude + 180
    */
   public int longitudeMaxE6_Normalized;

   /**
    * Contains the resized normalized latitude min value, it's the cardinal direction south
    * <p>
    * The min value must be smaller than the max value because this value is used to find a location
    * <p>
    * <code>normalized = latitude + 90</code>
    */
   public int latitudeMinE6_Resized_Normalized;

   /**
    * Contains the resized normalized latitude max value, it's the cardinal direction north
    * <p>
    * The min value must be smaller than the max value because this value is used to find a location
    * <p>
    * normalized = latitude + 90
    */
   public int latitudeMaxE6_Resized_Normalized;

   /**
    * Contains the resized normalized longitude min value, it's the cardinal direction west
    * <p>
    * The min value must be smaller than the max value because this value is used to find a location
    * <p>
    * normalized = longitude + 180
    */
   public int longitudeMinE6_Resized_Normalized;

   /**
    * Contains the resized normalized longitude max value, it's the cardinal direction east
    * <p>
    * The min value must be smaller than the max value because this value is used to find a location
    * <p>
    * normalized = longitude + 180
    */
   public int longitudeMaxE6_Resized_Normalized;

   /*
    * Fields from {@link OSMAddress}
    */
   public String continent;

   public String country;
   public String country_code;
   public String region;

   public String state;
   public String state_district;
   public String county;
   public String municipality;

   public String city;
   public String town;
   public String village;
   public String city_district;

   public String district;
   public String borough;
   public String suburb;
   public String subdivision;
   public String hamlet;

   public String croft;
   public String isolated_dwelling;
   public String neighbourhood;

   public String allotments;
   public String quarter;
   public String city_block;

   public String residential;
   public String farm;
   public String farmyard;
   public String industrial;
   public String commercial;
   public String retail;
   public String road;
   //
   public String house_number;
   public String house_name;
   //
   public String aerialway;
   public String aeroway;
   public String amenity;
   public String boundary;
   public String bridge;
   public String club;
   public String craft;
   public String emergency;
   public String historic;
   public String landuse;
   public String leisure;
   public String man_made;
   public String military;
   public String mountain_pass;

   /**
    * "natural" seems to be a SQL name :-?
    * <p>
    * ERROR 42X01: Syntax error: Encountered "natural" at line 55, column 4.
    */
   public String natural2;

   public String office;
   public String place;
   public String railway;
   public String shop;
   public String tourism;
   public String tunnel;
   public String waterway;
   public String postcode;

   @Transient
   public int    houseNumberValue = Integer.MIN_VALUE;

   @Transient
   public int    postcodeValue    = Integer.MIN_VALUE;

   @Transient
   public String settlementSmall;
   @Transient
   public String settlementLarge;

   @Transient
   public double latitude;
   @Transient
   public double longitude;

   @Transient
   public int    latitudeE6;
   @Transient
   public int    longitudeE6;

   /** Cardinal direction: South */
   @Transient
   public double latitudeMin;

   /** Cardinal direction: North */
   @Transient
   public double latitudeMax;

   /** Cardinal direction: West */
   @Transient
   public double longitudeMin;

   /** Cardinal direction: East */
   @Transient
   public double longitudeMax;

   /** Cardinal direction: South */
   @Transient
   public double latitudeMin_Resized;

   /** Cardinal direction: North */
   @Transient
   public double latitudeMax_Resized;

   /** Cardinal direction: West */
   @Transient
   public double longitudeMin_Resized;

   /** Cardinal direction: East */
   @Transient
   public double longitudeMax_Resized;

   /**
    * Key for the <b>NOT</b> resized bounding box, is e.g. used to identify the location color
    */
   @Transient
   public long   boundingBoxKey;

   /**
    * Default constructor used also in ejb
    */
   public TourLocation() {}

   public TourLocation(final double latitude, final double longitude) {

      this.latitude = latitude;
      this.longitude = longitude;

      latitudeE6 = (int) (latitude * 1E6);
      longitudeE6 = (int) (longitude * 1E6);

      latitudeE6_Normalized = latitudeE6 + 90_000_000;
      longitudeE6_Normalized = longitudeE6 + 180_000_000;
   }

   @Override
   public boolean equals(final Object obj) {

      if (this == obj) {
         return true;
      }

      if (obj == null) {
         return false;
      }

      if (getClass() != obj.getClass()) {
         return false;
      }

      final TourLocation other = (TourLocation) obj;

      return locationID == other.locationID;
   }

   public int getLatitudeDiff() {

      final int latitudeDiff = latitudeE6_Normalized < latitudeMinE6_Normalized

            ? latitudeE6_Normalized - latitudeMinE6_Normalized
            : latitudeE6_Normalized > latitudeMaxE6_Normalized

                  ? latitudeE6_Normalized - latitudeMaxE6_Normalized
                  : 0;

      return latitudeDiff;
   }

   public long getLocationId() {

      return locationID;
   }

   public int getLongitudeDiff() {

      final int longitudeDiff = longitudeE6_Normalized < longitudeMinE6_Normalized

            ? longitudeE6_Normalized - longitudeMinE6_Normalized
            : longitudeE6_Normalized > longitudeMaxE6_Normalized

                  ? longitudeE6_Normalized - longitudeMaxE6_Normalized
                  : 0;

      return longitudeDiff;
   }

   /**
    * @param partID
    *
    * @return Returns the field value from the field which name is from the provided partID
    *         {@link LocationPartID#name()}
    */
   public String getPartValue(final LocationPartID partID) {

      if (partID == null) {
         return null;
      }

      try {

         final String partName = partID.name();
         final String fieldName;

         if (LocationPartID.OSM_NAME.equals(partID)) {

            fieldName = PartItem.ALLOWED_FIELDNAME_NAME;

         } else if (LocationPartID.OSM_DEFAULT_NAME.equals(partID)) {

            fieldName = PartItem.ALLOWED_FIELDNAME_DISPLAY_NAME;

         } else {
            fieldName = partName;
         }

         final Field addressField = getClass().getField(fieldName);

         final Object fieldValue = addressField.get(this);

         if (fieldValue instanceof final String textValue) {

            return textValue;
         }

      } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {

         StatusUtil.log(e);
      }

      return null;
   }

   @Override
   public int hashCode() {

      return Objects.hash(locationID);
   }

   private String log(final String field, final double value) {

      return field + value + NL;
   }

   private String log(final String field, final long value) {

      return field + value + NL;
   }

   private String log(final String field, final String value) {

      if (value == null || value.length() == 0) {
         return UI.EMPTY_STRING;
      }

      return field + value + NL;
   }

   public void setLocationID(final long locationID) {

      this.locationID = locationID;
   }

   /**
    * Set values which are not saved in the database
    */
   public void setTransientValues() {

      setTransientValues(false);
   }

   /**
    * Set values which are not saved in the database
    *
    * @param isUpdateValues
    *           When <code>true</code> then transient values are set even when they are already set,
    *           this is used to update transient values
    */
   public void setTransientValues(final boolean isUpdateValues) {

      if (isUpdateValues == false && (latitudeMin != 0 || longitudeMin != 0)) {
         return;
      }

// SET_FORMATTING_OFF

      final double dbLatitude               = (latitudeE6_Normalized  -  90_000_000) / 1E6;
      final double dbLongitude              = (longitudeE6_Normalized - 180_000_000) / 1E6;

      final double dbLatitudeMin            = (latitudeMinE6_Normalized  -  90_000_000) / 1E6;
      final double dbLatitudeMax            = (latitudeMaxE6_Normalized  -  90_000_000) / 1E6;
      final double dbLongitudeMin           = (longitudeMinE6_Normalized - 180_000_000) / 1E6;
      final double dbLongitudeMax           = (longitudeMaxE6_Normalized - 180_000_000) / 1E6;

      final double dbLatitudeMin_Resized    = (latitudeMinE6_Resized_Normalized  -  90_000_000) / 1E6;
      final double dbLatitudeMax_Resized    = (latitudeMaxE6_Resized_Normalized  -  90_000_000) / 1E6;
      final double dbLongitudeMin_Resized   = (longitudeMinE6_Resized_Normalized - 180_000_000) / 1E6;
      final double dbLongitudeMax_Resized   = (longitudeMaxE6_Resized_Normalized - 180_000_000) / 1E6;

      latitude              = dbLatitude;
      longitude             = dbLongitude;

      latitudeMin           = dbLatitudeMin;
      latitudeMax           = dbLatitudeMax;
      longitudeMin          = dbLongitudeMin;
      longitudeMax          = dbLongitudeMax;

      latitudeMin_Resized   = dbLatitudeMin_Resized;
      latitudeMax_Resized   = dbLatitudeMax_Resized;
      longitudeMin_Resized  = dbLongitudeMin_Resized;
      longitudeMax_Resized  = dbLongitudeMax_Resized;

      boundingBoxKey        = latitudeMinE6_Normalized
                            + latitudeMaxE6_Normalized
                            + longitudeMinE6_Normalized
                            + longitudeMaxE6_Normalized;

// SET_FORMATTING_ON

      /*
       * Convert string values, e.g. postcode or house number into number values
       */
      try {

         houseNumberValue = Integer.parseInt(house_number);

      } catch (final Exception e) {

         // ignore
      }

      try {

         postcodeValue = Integer.parseInt(postcode);

      } catch (final Exception e) {

         // ignore
      }

// SET_FORMATTING_OFF

      // https://wiki.openstreetmap.org/wiki/Template:Generic:Map_Features:place

      settlementSmall =

           allotments         != null ? allotments //          few               Schrebergärten
         : farm               != null ? farm //                few               Bauernhof
         : isolated_dwelling  != null ? isolated_dwelling //   few               Einzelsiedlung
         : hamlet             != null ? hamlet //              100-1000          kleine Siedlung / Weiler
         : city_block         != null ? city_block //                            Häuserblock
         : neighbourhood      != null ? neighbourhood //                         Stadtviertel
         : city_district      != null ? city_district //                         Stadtviertel
         : village            != null ? village //           < 10'000            Dorf
         : quarter            != null ? quarter //                               Ortsteil
         : suburb             != null ? suburb //                                Stadtteil
         : borough            != null ? borough //                               Stadtbezirk
         : town               != null ? town //               10'000 - 100'000   Stadt
         : city               != null ? city //             > 100'000            Grossstadt
         : county             != null ? county //
         : state              != null ? state //
         : country

      ;

      settlementLarge =

           city               != null ? city //             > 100'000            Grossstadt
         : town               != null ? town //               10'000 - 100'000   Stadt
         : borough            != null ? borough //                               Stadtbezirk
         : suburb             != null ? suburb //                                Stadtteil
         : quarter            != null ? quarter //                               Ortsteil
         : village            != null ? village //           < 10'000            Dorf
         : city_district      != null ? city_district //                         Stadtviertel
         : neighbourhood      != null ? neighbourhood //                         Stadtviertel
         : city_block         != null ? city_block //                            Häuserblock
         : hamlet             != null ? hamlet //              100-1000          kleine Siedlung / Weiler
         : isolated_dwelling  != null ? isolated_dwelling //   few               Einzelsiedlung
         : farm               != null ? farm //                few               Bauernhof
         : allotments         != null ? allotments //          few               Schrebergärten
         : null

      ;

      if (settlementLarge == null) {

         settlementLarge =

               state    != null ? state //
             : county   != null ? county //
             : country
         ;
      }

// SET_FORMATTING_ON

   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TourLocation" + NL //                                       //$NON-NLS-1$

            + " locationID          = " + locationID + NL //               //$NON-NLS-1$

            + log(" name                = ", name) //                      //$NON-NLS-1$
            + log(" display_name        = ", display_name) //              //$NON-NLS-1$

            + log(" continent           = ", continent) //                 //$NON-NLS-1$
            + log(" country             = ", country) //                   //$NON-NLS-1$
            + log(" country_code        = ", country_code) //              //$NON-NLS-1$
            + log(" region              = ", region) //                    //$NON-NLS-1$
            + log(" state               = ", state) //                     //$NON-NLS-1$
            + log(" state_district      = ", state_district) //            //$NON-NLS-1$
            + log(" county              = ", county) //                    //$NON-NLS-1$
            + log(" municipality        = ", municipality) //              //$NON-NLS-1$
            + log(" city                = ", city) //                      //$NON-NLS-1$
            + log(" town                = ", town) //                      //$NON-NLS-1$
            + log(" village             = ", village) //                   //$NON-NLS-1$
            + log(" city_district       = ", city_district) //             //$NON-NLS-1$
            + log(" district            = ", district) //                  //$NON-NLS-1$
            + log(" borough             = ", borough) //                   //$NON-NLS-1$
            + log(" suburb              = ", suburb) //                    //$NON-NLS-1$
            + log(" subdivision         = ", subdivision) //               //$NON-NLS-1$
            + log(" hamlet              = ", hamlet) //                    //$NON-NLS-1$
            + log(" croft               = ", croft) //                     //$NON-NLS-1$
            + log(" isolated_dwelling   = ", isolated_dwelling) //         //$NON-NLS-1$
            + log(" neighbourhood       = ", neighbourhood) //             //$NON-NLS-1$
            + log(" allotments          = ", allotments) //                //$NON-NLS-1$
            + log(" quarter             = ", quarter) //                   //$NON-NLS-1$
            + log(" city_block          = ", city_block) //                //$NON-NLS-1$
            + log(" residential         = ", residential) //               //$NON-NLS-1$
            + log(" farm                = ", farm) //                      //$NON-NLS-1$
            + log(" farmyard            = ", farmyard) //                  //$NON-NLS-1$
            + log(" industrial          = ", industrial) //                //$NON-NLS-1$
            + log(" commercial          = ", commercial) //                //$NON-NLS-1$
            + log(" retail              = ", retail) //                    //$NON-NLS-1$
            + log(" road                = ", road) //                      //$NON-NLS-1$
            + log(" house_number        = ", house_number) //              //$NON-NLS-1$
            + log(" house_name          = ", house_name) //                //$NON-NLS-1$
            + log(" aerialway           = ", aerialway) //                 //$NON-NLS-1$
            + log(" aeroway             = ", aeroway) //                   //$NON-NLS-1$
            + log(" amenity             = ", amenity) //                   //$NON-NLS-1$
            + log(" boundary            = ", boundary) //                  //$NON-NLS-1$
            + log(" bridge              = ", bridge) //                    //$NON-NLS-1$
            + log(" club                = ", club) //                      //$NON-NLS-1$
            + log(" craft               = ", craft) //                     //$NON-NLS-1$
            + log(" emergency           = ", emergency) //                 //$NON-NLS-1$
            + log(" historic            = ", historic) //                  //$NON-NLS-1$
            + log(" landuse             = ", landuse) //                   //$NON-NLS-1$
            + log(" leisure             = ", leisure) //                   //$NON-NLS-1$
            + log(" man_made            = ", man_made) //                  //$NON-NLS-1$
            + log(" military            = ", military) //                  //$NON-NLS-1$
            + log(" mountain_pass       = ", mountain_pass) //             //$NON-NLS-1$
            + log(" natural2            = ", natural2) //                  //$NON-NLS-1$
            + log(" office              = ", office) //                    //$NON-NLS-1$
            + log(" place               = ", place) //                     //$NON-NLS-1$
            + log(" railway             = ", railway) //                   //$NON-NLS-1$
            + log(" shop                = ", shop) //                      //$NON-NLS-1$
            + log(" tourism             = ", tourism) //                   //$NON-NLS-1$
            + log(" tunnel              = ", tunnel) //                    //$NON-NLS-1$
            + log(" waterway            = ", waterway) //                  //$NON-NLS-1$
            + log(" postcode            = ", postcode) //                  //$NON-NLS-1$

            + log(" settlementSmall     = ", settlementSmall) //           //$NON-NLS-1$
            + log(" settlementLarge     = ", settlementLarge) //           //$NON-NLS-1$

            + NL

            + log(" latitude                          = ", latitude) //                            //$NON-NLS-1$
            + log(" latitudeE6                        = ", latitudeE6) //                          //$NON-NLS-1$
            + log(" latitudeE6_Normalized             = ", latitudeE6_Normalized) //               //$NON-NLS-1$
            + log(" latitudeMinE6_Normalized          = ", latitudeMinE6_Normalized) //            //$NON-NLS-1$
            + log(" latitudeMinE6_Resized_Normalized  = ", latitudeMinE6_Resized_Normalized) //    //$NON-NLS-1$
            + log(" latitudeMaxE6_Normalized          = ", latitudeMaxE6_Normalized) //            //$NON-NLS-1$
            + log(" latitudeMaxE6_Resized_Normalized  = ", latitudeMaxE6_Resized_Normalized) //    //$NON-NLS-1$

            + log(" longitude                         = ", longitude) //                           //$NON-NLS-1$
            + log(" longitudeE6                       = ", longitudeE6) //                         //$NON-NLS-1$
            + log(" longitudeE6_Normalized            = ", longitudeE6_Normalized) //              //$NON-NLS-1$
            + log(" longitudeMinE6_Normalized         = ", longitudeMinE6_Normalized) //           //$NON-NLS-1$
            + log(" longitudeMinE6_Resized_Normalized = ", longitudeMinE6_Resized_Normalized) //   //$NON-NLS-1$
            + log(" longitudeMaxE6_Normalized         = ", longitudeMaxE6_Normalized) //           //$NON-NLS-1$
            + log(" longitudeMaxE6_Resized_Normalized = ", longitudeMaxE6_Resized_Normalized) //   //$NON-NLS-1$

            + log(" boundingBoxKey                    = ", boundingBoxKey) //                      //$NON-NLS-1$

            + log(" latitudeDiff                      = ", getLatitudeDiff()) //                   //$NON-NLS-1$
            + log(" longitudeDiff                     = ", getLongitudeDiff()) //                  //$NON-NLS-1$
      ;
   }
}
