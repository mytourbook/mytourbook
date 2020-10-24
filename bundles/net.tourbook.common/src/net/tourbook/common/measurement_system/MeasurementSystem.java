/*******************************************************************************
 * Copyright (C) 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.common.measurement_system;

import net.tourbook.common.util.StatusUtil;

public class MeasurementSystem implements Cloneable {

   private String      name;

   private Distance    distance;
   private Elevation   elevation;
   private Temperature temperature;
   private Weight      weight;

   public MeasurementSystem(final String name,
                            final Distance distance,
                            final Elevation elevation,
                            final Temperature temperature,
                            final Weight weight) {

      this.name = name;

      this.distance = distance;
      this.elevation = elevation;
      this.temperature = temperature;
      this.weight = weight;
   }

   @Override
   public MeasurementSystem clone() {

      MeasurementSystem clonedProfile = null;

      try {

         clonedProfile = (MeasurementSystem) super.clone();

      } catch (final CloneNotSupportedException e) {
         StatusUtil.log(e);
      }

      return clonedProfile;
   }

   /**
    * @return the distance
    */
   public Distance getDistance() {
      return distance;
   }

   /**
    * @return the elevation
    */
   public Elevation getElevation() {
      return elevation;
   }

   /**
    * @return the name
    */
   public String getName() {
      return name;
   }

   /**
    * @return the temperature
    */
   public Temperature getTemperature() {
      return temperature;
   }

   /**
    * @return the weight
    */
   public Weight getWeight() {
      return weight;
   }

   /**
    * @param distance
    *           the distance to set
    */
   public void setDistance(final Distance distance) {
      this.distance = distance;
   }

   /**
    * @param elevation
    *           the elevation to set
    */
   public void setElevation(final Elevation elevation) {
      this.elevation = elevation;
   }

   /**
    * @param name
    *           the name to set
    */
   public void setName(final String name) {
      this.name = name;
   }

   /**
    * @param temperature
    *           the temperature to set
    */
   public void setTemperature(final Temperature temperature) {
      this.temperature = temperature;
   }

   /**
    * @param weight
    *           the weight to set
    */
   public void setWeight(final Weight weight) {
      this.weight = weight;
   }
}
