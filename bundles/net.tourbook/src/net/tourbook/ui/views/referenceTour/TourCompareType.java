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
package net.tourbook.ui.views.referenceTour;

/**
 * Defines the kind how tours are compared
 */
public enum TourCompareType {

   /**
    * A reference tour is compared with elevation or geo positions
    */
   ANY_COMPARE_REFERENCE_TOUR, //

   /**
    * A reference tour is compared with it's elevation values
    */
   ELEVATION_COMPARE_REFERENCE_TOUR, //

   /**
    * A reference tour is compared with it's geo positions
    */
   GEO_COMPARE_REFERENCE_TOUR, //

   /**
    * Any tour is geo compared
    */
   GEO_COMPARE_ANY_TOUR, //
}
