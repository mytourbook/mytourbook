/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors 
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
package net.tourbook.commands;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.ISaveablePart;

public class TourEditor_Tester extends PropertyTester {

   private static final String PROPERTY_IS_DIRTY           = "isDirty"; //$NON-NLS-1$

   @Override
   public boolean test(final Object receiver, final String property, final Object[] args, final Object expectedValue) {

      // return dirty state from the tour editor or tag view editor
      if (PROPERTY_IS_DIRTY.equals(property) && receiver instanceof ISaveablePart) {

         final boolean isDirty = ((ISaveablePart) receiver).isDirty();

         return isDirty;
      }

      return false;
   }
}
