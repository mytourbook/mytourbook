/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.export;

import org.dinopolis.gpstool.gpsinput.garmin.GarminTrackpoint;
import org.dinopolis.gpstool.gpsinput.garmin.GarminTrackpointAdapter;

public class GarminTrackpointAdapterExtended extends GarminTrackpointAdapter {

   private double _speed       = Double.NaN;
   private double _temperature = Double.NaN;
   private short  _power       = Short.MIN_VALUE;
   private long   _gear;

   public GarminTrackpointAdapterExtended(final GarminTrackpoint trackpoint) {
      super(trackpoint);
   }

   public long getGear() {
      return _gear;
   }

   public short getPower() {
      return _power;
   }

   public double getSpeed() {
      return _speed;
   }

   /**
    * @return Returns temperature or {@link Double#NaN} when temperature is not set
    */
   public double getTemperature() {
      return _temperature;
   }

   public boolean hasValidCoordinates() {

      return getLatitude() != 0.0 && getLongitude() != 0.0;
   }

   public boolean hasValidExtension() {

      return hasValidTemperature() || hasValidHeartrate() || hasValidCadence() || hasValidDistance() || hasValidGear();
   }

   /**
    * @return Returns <code>true</code> when valid gear is available.
    */
   public boolean hasValidGear() {
      return _gear != 0;
   }

   public boolean hasValidPower() {
      return Short.MIN_VALUE != _power;
   }

   public boolean hasValidSpeed() {
      return !Double.isNaN(_speed);
   }

   /**
    * Returns <code>true</code> if the temperature of this waypoint is valid. This is equal to the
    * expression <code>!Double.isNaN(getTemperature())</code>.
    *
    * @return Returns <code>true</code> if waypoint has valid temperature.
    */
   public boolean hasValidTemperature() {
      return !Double.isNaN(_temperature);
   }

   public void setGear(final long gear) {
      _gear = gear;
   }

   public void setPower(final short power) {
      _power = power;
   }

   public void setSpeed(final double speed) {
      _speed = speed;
   }

   public void setTemperature(final double temperature) {
      _temperature = temperature;
   }

}
