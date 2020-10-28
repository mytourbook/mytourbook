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
package net.tourbook.measurement_system;

import java.util.Objects;

import net.tourbook.common.util.StatusUtil;

/**
 * Contains all system measurement data for one profile.
 */
public class MeasurementSystem implements Cloneable {

   private String                   _name;

   private Unit_DayTime             _dayTime;
   private Unit_Distance            _distance;
   private Unit_Elevation           _elevation;
   private Unit_Height              _height;
   private Unit_Length              _length;
   private Unit_Pressure_Atmosphere _pressure_Atmosphere;
   private Unit_SmallLength         _smallLength;
   private Unit_Temperature         _temperature;
   private Unit_Weight              _weight;

   private boolean                  _savedState_IsProfileActive;

   public MeasurementSystem(final String name,
                            final Unit_DayTime dayTime,
                            final Unit_Distance distance,
                            final Unit_Elevation elevation,
                            final Unit_Height height,
                            final Unit_Length length,
                            final Unit_Pressure_Atmosphere pressure,
                            final Unit_SmallLength smallLength,
                            final Unit_Temperature temperature,
                            final Unit_Weight weight) {

      _name = name;

      _pressure_Atmosphere = pressure;
      _dayTime = dayTime;
      _distance = distance;
      _elevation = elevation;
      _height = height;
      _length = length;
      _smallLength = smallLength;
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

   public Unit_DayTime getDayTime() {
      return _dayTime;
   }

   /**
    * @return the distance
    */
   public Unit_Distance getDistance() {
      return _distance;
   }

   /**
    * @return the elevation
    */
   public Unit_Elevation getElevation() {
      return _elevation;
   }

   public Unit_Height getHeight() {
      return _height;
   }

   public Unit_Length getLength() {
      return _length;
   }

   /**
    * @return the name
    */
   public String getName() {
      return _name;
   }

   /**
    * @return the _atmosphericPressure
    */
   public Unit_Pressure_Atmosphere getPressure_Atmosphere() {
      return _pressure_Atmosphere;
   }

   /**
    * @return the _state_IsProfileActive
    */
   public boolean getSaveState_IsProfileActive() {
      return _savedState_IsProfileActive;
   }

   public Unit_SmallLength getSmallLength() {
      return _smallLength;
   }

   /**
    * @return Returns the hash code including all measurement system data fields but not the profile
    *         name.
    */
   public int getSystemDataHash() {

      return Objects.hash(
            _dayTime,
            _distance,
            _elevation,
            _height,
            _length,
            _pressure_Atmosphere,
            _smallLength,
            _temperature,
            _weight);
   }

   /**
    * @return the temperature
    */
   public Unit_Temperature getTemperature() {
      return _temperature;
   }

   /**
    * @return the weight
    */
   public Unit_Weight getWeight() {
      return _weight;
   }

   /**
    * @param _atmosphericPressure
    *           the _atmosphericPressure to set
    */
   public void setAtmosphericPressure(final Unit_Pressure_Atmosphere _atmosphericPressure) {
      this._pressure_Atmosphere = _atmosphericPressure;
   }

   public void setDayTime(final Unit_DayTime _dayTime) {
      this._dayTime = _dayTime;
   }

   /**
    * @param distance
    *           the distance to set
    */
   public void setDistance(final Unit_Distance distance) {
      _distance = distance;
   }

   /**
    * @param elevation
    *           the elevation to set
    */
   public void setElevation(final Unit_Elevation elevation) {
      _elevation = elevation;
   }

   public void setHeight(final Unit_Height _height) {
      this._height = _height;
   }

   public void setLength(final Unit_Length _length) {
      this._length = _length;
   }

   /**
    * @param name
    *           the name to set
    */
   public void setName(final String name) {
      _name = name;
   }

   public void setSavedState_IsProfileActive(final boolean isProfileActive) {
      _savedState_IsProfileActive = isProfileActive;
   }

   public void setSmallLength(final Unit_SmallLength _smallLength) {
      this._smallLength = _smallLength;
   }

   /**
    * @param temperature
    *           the temperature to set
    */
   public void setTemperature(final Unit_Temperature temperature) {
      _temperature = temperature;
   }

   /**
    * @param weight
    *           the weight to set
    */
   public void setWeight(final Unit_Weight weight) {
      _weight = weight;
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
