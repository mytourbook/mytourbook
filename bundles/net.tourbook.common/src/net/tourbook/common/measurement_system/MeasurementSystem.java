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

import java.util.Objects;

import net.tourbook.common.util.StatusUtil;

/**
 * Contains all system measurement data for one system profile.
 */
public class MeasurementSystem implements Cloneable {

   private String                   _name;

   private Unit_Distance            _distance;
   private Unit_Elevation           _elevation;
   private Unit_Height_Body         _height;
   private Unit_Length              _length;
   private Unit_Length_Small        _length_Small;
   private Unit_Pace                _pace;
   private Unit_Pressure_Atmosphere _pressure_Atmosphere;
   private Unit_Temperature         _temperature;
   private Unit_Weight              _weight_Body;

   private boolean                  _savedState_IsProfileActive;

   public MeasurementSystem(final String name,
                            final Unit_Distance distance,
                            final Unit_Length length,
                            final Unit_Length_Small smallLength,
                            final Unit_Elevation elevation,
                            final Unit_Height_Body height,
                            final Unit_Pace pace,
                            final Unit_Pressure_Atmosphere pressure,
                            final Unit_Temperature temperature,
                            final Unit_Weight weight) {

      _name = name;

      _distance = distance;
      _length = length;
      _length_Small = smallLength;
      _elevation = elevation;
      _height = height;
      _pace = pace;
      _pressure_Atmosphere = pressure;
      _temperature = temperature;
      _weight_Body = weight;
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
   public Unit_Distance getDistance() {
      return _distance;
   }

   /**
    * @return the elevation
    */
   public Unit_Elevation getElevation() {
      return _elevation;
   }

   public Unit_Height_Body getHeight() {
      return _height;
   }

   public Unit_Length getLength() {
      return _length;
   }

   public Unit_Length_Small getLengthSmall() {
      return _length_Small;
   }

   /**
    * @return the name
    */
   public String getName() {
      return _name;
   }

   public Unit_Pace getPace() {
      return _pace;
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

   /**
    * @return Returns the hash code including all measurement system data fields but not the profile
    *         name.
    */
   public int getSystemDataHash() {

      return Objects.hash(
            _distance,
            _length,
            _length_Small,
            _elevation,
            _height,
            _pace,
            _pressure_Atmosphere,
            _temperature,
            _weight_Body);
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
      return _weight_Body;
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

   public void setHeight(final Unit_Height_Body height) {
      _height = height;
   }

   public void setLength(final Unit_Length length) {
      _length = length;
   }

   public void setLength_Small(final Unit_Length_Small smallLength) {
      _length_Small = smallLength;
   }

   /**
    * @param name
    *           the name to set
    */
   public void setName(final String name) {
      _name = name;
   }

   public void setPace(final Unit_Pace pace) {
      _pace = pace;
   }

   /**
    * @param atmosphericPressure
    *           the _atmosphericPressure to set
    */
   public void setPressure_Atmospheric(final Unit_Pressure_Atmosphere atmosphericPressure) {
      _pressure_Atmosphere = atmosphericPressure;
   }

   public void setSavedState_IsProfileActive(final boolean isProfileActive) {
      _savedState_IsProfileActive = isProfileActive;
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
      _weight_Body = weight;
   }

   @Override
   public String toString() {

      return "MeasurementSystem\n" //$NON-NLS-1$

            + "[\n" //$NON-NLS-1$

            + "_name=" + _name + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "_distance=" + _distance + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "_elevation=" + _elevation + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "_height=" + _height + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "_length=" + _length + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "_length_Small=" + _length_Small + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "_pace=" + _pace + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "_pressure_Atmosphere=" + _pressure_Atmosphere + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "_temperature=" + _temperature + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "_weight_Body=" + _weight_Body + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "_savedState_IsProfileActive=" + _savedState_IsProfileActive //$NON-NLS-1$

            + "\n]"; //$NON-NLS-1$
   }
}
