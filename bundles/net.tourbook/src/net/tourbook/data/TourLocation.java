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
package net.tourbook.data;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import net.tourbook.common.UI;
import net.tourbook.database.TourDatabase;

@Entity
public class TourLocation implements Comparable<Object>, Serializable {

   private static final long serialVersionUID = 1L;

   private static final char NL               = UI.NEW_LINE;

   public static final int   DB_FIELD_LENGTH  = 1000;

   /**
    * Contains the entity id
    */
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long              locationID       = TourDatabase.ENTITY_IS_NOT_SAVED;

   /*
    * Fields from {@link OSMLocation}
    */
   public String name;
   public String displayName;

   /*
    * Bounding box
    */
   public double latitudeMin;
   public double latitudeMax;
   public double longitudeMin;
   public double longitudeMax;

   /*
    * Fields from {@link OSMAddress}
    */
   public String continent;
   public String country;
   public String countryCode;

//   public String region;
//   public String state;
//   public String stateDistrict;
//   public String county;
//
//   public String municipality;
//   public String city;
//   public String town;
//   public String village;
//
//   public String cityDistrict;
//   public String district;
//   public String borough;
//   public String suburb;
//   public String subdivision;
//
//   public String hamlet;
//   public String croft;
//   public String isolatedDwelling;
//
//   public String neighbourhood;
//   public String allotments;
//   public String quarter;
//
//   public String cityBlock;
//   public String residential;
//   public String farm;
//   public String farmyard;
//   public String industrial;
//   public String commercial;
//   public String retail;
//
//   public String road;
//
//   public String houseNumber;
//   public String houseName;
//
//   public String aerialway;
//   public String aeroway;
//   public String amenity;
//   public String boundary;
//   public String bridge;
//   public String club;
//   public String craft;
//   public String emergency;
//   public String historic;
//   public String landuse;
//   public String leisure;
//   public String manMade;
//   public String military;
//   public String mountainPass;
//   public String natural;
//   public String office;
//   public String place;
//   public String railway;
//   public String shop;
//   public String tourism;
//   public String tunnel;
//   public String waterway;
//
//   public String postcode;

   /**
    * Default constructor used in ejb
    */
   public TourLocation() {}

   public TourLocation(final String name) {

   }

   @Override
   public int compareTo(final Object o) {
      // TODO Auto-generated method stub
      return 0;
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

   public long getLocationId() {
      return locationID;
   }

   @Override
   public int hashCode() {

      return Objects.hash(locationID);
   }

}
