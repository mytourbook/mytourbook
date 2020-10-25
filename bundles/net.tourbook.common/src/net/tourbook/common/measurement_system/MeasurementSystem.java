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

   private String      _name;

   private Distance    _distance;
   private Elevation   _elevation;
   private Temperature _temperature;
   private Weight      _weight;

   private boolean     _savedState_IsProfileActive;

   public MeasurementSystem(final String name,
                            final Distance distance,
                            final Elevation elevation,
                            final Temperature temperature,
                            final Weight weight) {

      _name = name;

      _distance = distance;
      _elevation = elevation;
      _temperature = temperature;
      _weight = weight;
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
      return _distance;
   }

   /**
    * @return the elevation
    */
   public Elevation getElevation() {
      return _elevation;
   }

   /**
    * @return the name
    */
   public String getName() {
      return _name;
   }

   /**
    * @return the _state_IsProfileActive
    */
   public boolean getSaveState_IsProfileActive() {
      return _savedState_IsProfileActive;
   }

   /**
    * @return the temperature
    */
   public Temperature getTemperature() {
      return _temperature;
   }

   /**
    * @return the weight
    */
   public Weight getWeight() {
      return _weight;
   }

   /**
    * @param distance
    *           the distance to set
    */
   public void setDistance(final Distance distance) {
      this._distance = distance;
   }

   /**
    * @param elevation
    *           the elevation to set
    */
   public void setElevation(final Elevation elevation) {
      this._elevation = elevation;
   }

   /**
    * @param name
    *           the name to set
    */
   public void setName(final String name) {
      this._name = name;
   }

   public void setSavedState_IsProfileActive(final boolean isProfileActive) {
      _savedState_IsProfileActive = isProfileActive;
   }

   /**
    * @param temperature
    *           the temperature to set
    */
   public void setTemperature(final Temperature temperature) {
      this._temperature = temperature;
   }

   /**
    * @param weight
    *           the weight to set
    */
   public void setWeight(final Weight weight) {
      this._weight = weight;
   }

   @Override
   public String toString() {

      return "MeasurementSystem\n" //$NON-NLS-1$

            + "[\n" //$NON-NLS-1$

            + "_name=" + _name + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "_distance=" + _distance + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "_elevation=" + _elevation + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "_temperature=" + _temperature + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "_weight=" + _weight + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "_savedState_IsProfileActive=" + _savedState_IsProfileActive + "\n" //$NON-NLS-1$ //$NON-NLS-2$

            + "]"; //$NON-NLS-1$
   }
}
