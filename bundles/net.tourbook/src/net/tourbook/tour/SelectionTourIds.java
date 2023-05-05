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
package net.tourbook.tour;

import java.util.ArrayList;

import net.tourbook.common.UI;

import org.eclipse.jface.viewers.ISelection;

/**
 * Selection contains multiple tour id's
 */
public class SelectionTourIds implements ISelection {

   private static final char NL = UI.NEW_LINE;

   private ArrayList<Long>   _tourIds;

   public SelectionTourIds(final ArrayList<Long> tourIds) {

      _tourIds = tourIds;
   }

   public ArrayList<Long> getTourIds() {
      return _tourIds;
   }

   @Override
   public boolean isEmpty() {
      return false;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "SelectionTourIds" + NL //                          //$NON-NLS-1$

            + "[" + NL //$NON-NLS-1$

            + "_tourIds=" + _tourIds + NL //                      //$NON-NLS-1$

            + "]"; //                                             //$NON-NLS-1$
   }

}
