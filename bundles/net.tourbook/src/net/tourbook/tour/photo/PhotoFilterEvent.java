/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.photo;

public class PhotoFilterEvent {

   /**
    * Number of selected rating stars.
    */
   public int filterRatingStars;

   /**
    * Can be one of these operators:
    * <p>
    * {@link Slideout_Map2_PhotoFilter#OPERATOR_IS_EQUAL},
    * {@link Slideout_Map2_PhotoFilter#OPERATOR_IS_LESS_OR_EQUAL} or
    * {@link Slideout_Map2_PhotoFilter#OPERATOR_IS_MORE_OR_EQUAL}
    */
   public int fiterRatingStarOperator;

   @Override
   public String toString() {

      return "PhotoFilterEvent\n" //$NON-NLS-1$
            + "[\n" //$NON-NLS-1$
            + "filterRatingStars=" + filterRatingStars + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "fiterRatingStarOperator=" + fiterRatingStarOperator + "\n" //$NON-NLS-1$ //$NON-NLS-2$
            + "]"; //$NON-NLS-1$
   }

}
