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

public class System_Pressure_Atmosphere {

   private String                   _label;
   private Unit_Pressure_Atmosphere _pressure;

   public System_Pressure_Atmosphere(final Unit_Pressure_Atmosphere pressure, final String label) {

      _pressure = pressure;
      _label = label;
   }

   public String getLabel() {
      return _label;
   }

   public Unit_Pressure_Atmosphere getPressure() {
      return _pressure;
   }
}
