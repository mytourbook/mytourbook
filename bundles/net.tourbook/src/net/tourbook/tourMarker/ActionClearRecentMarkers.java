/*******************************************************************************
 * Copyright (C) 2026 Wolfgang Schramm and Contributors
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

import org.eclipse.jface.action.Action;

public class ActionClearRecentMarkers extends Action {

   public ActionClearRecentMarkers() {

      super("&Clear all Recent Markers", AS_PUSH_BUTTON);

      setToolTipText("A single recent marker can be removed\nby also pressing <Ctrl> when selecting the marker");
   }

   @Override
   public void run() {

      TourMarkerManager.clearRecentMarkers();
   }
}