/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

import org.eclipse.ui.IWorkbenchPart;

public interface ITourEventListener {

   /**
    * @param workbenchPart
    *           Part where the property was fired, can be <code>null</code> when the part is not
    *           set.
    * @param tourEventId
    *           Id is required.
    * @param eventData
    *           Can be <code>null</code>.
    */
   public void tourChanged(final IWorkbenchPart workbenchPart, TourEventId tourEventId, Object eventData);

}
