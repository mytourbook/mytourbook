/*******************************************************************************
 * Copyright (C) 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.map2.view;

import org.eclipse.jface.viewers.ISelection;

public class SelectionMapSelection implements ISelection {

   private Long _tourId;

   private int  _valueIndex1;
   private int  _valueIndex2;

   public SelectionMapSelection(final Long tourId,
                                final int valueIndex1,
                                final int valueIndex2) {

      _tourId = tourId;

      _valueIndex1 = valueIndex1;
      _valueIndex2 = valueIndex2;
   }

   public Long getTourId() {
      return _tourId;
   }

   public int getValueIndex1() {
      return _valueIndex1;
   }

   public int getValueIndex2() {
      return _valueIndex2;
   }

   @Override
   public boolean isEmpty() {
      return false;
   }

}
