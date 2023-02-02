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
package net.tourbook.map.model;

import java.util.UUID;

import net.tourbook.common.UI;

/**
 * Animated map model
 */
public class MapModel {

   private static final char NL = UI.NEW_LINE;

   public String             name;
   public String             description;

   public String             filepath;

   /**
    * Angle in degrees that the model is looking forward
    */
   public int                forwardAngle;

   /**
    * The model length needs a factor that the top of the symbol is not before the geo location
    */
   public float              headPositionFactor;

   /**
    * Is <code>true</code> when this model is a default model which is provided by MT
    */
   public boolean            isDefaultModel;

   public String             id = UUID.randomUUID().toString();

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

      final MapModel other = (MapModel) obj;

      if (id == null) {
         if (other.id != null) {
            return false;
         }
      } else if (!id.equals(other.id)) {
         return false;
      }

      return true;
   }

   @Override
   public int hashCode() {

      final int prime = 31;
      int result = 1;

      result = prime * result + ((id == null) ? 0 : id.hashCode());

      return result;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "MapModel" + NL //                                     //$NON-NLS-1$

            + "  name               =" + name + NL //                //$NON-NLS-1$
            + "  filepath           =" + filepath + NL //            //$NON-NLS-1$

            + "  forwardAngle       =" + forwardAngle + NL //        //$NON-NLS-1$
            + "  headPositionFactor =" + headPositionFactor + NL //  //$NON-NLS-1$

            + "  isDefaultModel     =" + isDefaultModel + NL //      //$NON-NLS-1$

//          + "  description        =" + description + NL //         //$NON-NLS-1$
      ;
   }
}
