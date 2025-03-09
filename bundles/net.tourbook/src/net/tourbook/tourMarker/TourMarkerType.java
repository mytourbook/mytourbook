/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.tourMarker;

import java.util.Objects;

import net.tourbook.common.util.StatusUtil;

import org.eclipse.swt.graphics.Color;

public class TourMarkerType implements Cloneable {

   public long   id;

   public String name;
   public String description;

   public Color  foregroundColor;
   public Color  backgroundColor;

   @Override
   public TourMarkerType clone() {

      TourMarkerType clonedObject = null;

      try {

         clonedObject = (TourMarkerType) super.clone();

      } catch (final CloneNotSupportedException e) {
         StatusUtil.log(e);
      }

      return clonedObject;
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

      final TourMarkerType other = (TourMarkerType) obj;

      return id == other.id;
   }

   @Override
   public int hashCode() {

      return Objects.hash(id);
   }
}
