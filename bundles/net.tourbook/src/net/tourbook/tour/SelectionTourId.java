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

import org.eclipse.jface.viewers.ISelection;

/**
 * selection contains a tour id
 */
public class SelectionTourId implements ISelection {

   private Long    _tourId;

   /**
    * When <code>true</code> then this tour id is set into the map breadcrumb bar
    */
   private boolean _isSetBreadcrumbOnly;

   public SelectionTourId(final Long tourId) {
      _tourId = tourId;
   }

   public Long getTourId() {
      return _tourId;
   }

   @Override
   public boolean isEmpty() {
      return false;
   }

   public boolean isSetBreadcrumbOnly() {
      return _isSetBreadcrumbOnly;
   }

   public void setIsSetBreadcrumbOnly(final boolean isSetBreadcrumb) {
      _isSetBreadcrumbOnly = isSetBreadcrumb;
   }

   @Override
   public String toString() {

      final StringBuilder sb = new StringBuilder();

      sb.append("[SelectionTourId] ");//$NON-NLS-1$
      sb.append("tourId:" + _tourId);//$NON-NLS-1$

      return sb.toString();
   }
}
