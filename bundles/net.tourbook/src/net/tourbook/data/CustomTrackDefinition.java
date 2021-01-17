/*******************************************************************************

Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
This program is free software; you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation version 2 of the License.
This program is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with
this program; if not, write to the Free Software Foundation,
Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
*******************************************************************************/
package net.tourbook.data;

import java.io.Serializable;

public class CustomTrackDefinition implements Serializable {
   /**
    *
    */
   private static final long  serialVersionUID          = -4880414084528375769L;

   public static final String DEFAULT_CUSTOM_TRACK_NAME = "default";            //$NON-NLS-1$

   private String             _name;
   private String             _id;
   private String             _unit;

   public String getId() {
      return _id;
   }

   public String getName() {
      return _name;
   }

   public String getUnit() {
      return _unit;
   }

   public void setId(final String id) {
      _id = id;
   }

   public void setName(final String name) {
      _name = name;
   }

   public void setUnit(final String unit) {
      _unit = unit;

   }
}
