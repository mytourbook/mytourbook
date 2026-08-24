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
package net.tourbook.map2.action;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.tourbook.common.UI;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.data.TourMarker;
import net.tourbook.map2.view.Map2View;
import net.tourbook.tour.TourManager;
import net.tourbook.tourMarker.RecentMarker;
import net.tourbook.tourMarker.TourMarkerManager;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Menu;

/**
 * Set the label of an existing {@link TourMarker} from recently used tour marker labels
 */
public class ActionMapPoint_RenameMarkerWithRecentMarkerInMap_SubMenu extends SubMenu {

   private Map2View                 _map2View;

   private List<ActionRecentMarker> _allRecentMarkerActions = new ArrayList<>();

   private class ActionRecentMarker extends Action {

      private RecentMarker __recentMarker;

      public ActionRecentMarker() {

         super(UI.EMPTY_STRING, AS_PUSH_BUTTON);
      }

      @Override
      public void run() {

         // make sure the tour editor does not contain a modified tour
         if (TourManager.isTourEditorModified()) {
            return;
         }

         final TourMarker tourMarker = _map2View.updateTourMarker(__recentMarker);

         // set last used marker to the top of the list
         TourMarkerManager.addRecentMarker(tourMarker.getLabel());
      }
   }

   public ActionMapPoint_RenameMarkerWithRecentMarkerInMap_SubMenu(final Map2View map2View) {

      super("Replace Marker Label &with", AS_DROP_DOWN_MENU);

      _map2View = map2View;

      // create submenu actions which will be updated when displayed
      for (int actionIndex = 0; actionIndex < TourMarkerManager.MAX_NUMBER_OF_RECENT_MARKERS; actionIndex++) {
         _allRecentMarkerActions.add(new ActionRecentMarker());
      }
   }

   @Override
   public void enableActions() {}

   @Override
   public void fillMenu(final Menu menu) {

      final LinkedList<RecentMarker> allRecentMarkers = TourMarkerManager.getRecentMarkers();
      final int numRecentMarkers = allRecentMarkers.size();

      for (int markerIndex = 0; markerIndex < numRecentMarkers; markerIndex++) {

         final RecentMarker recentMarker = allRecentMarkers.get(markerIndex);

         if (recentMarker == null) {
            break;
         }

         // update recycled marker action
         final ActionRecentMarker actionRecentMarker = _allRecentMarkerActions.get(markerIndex);

         actionRecentMarker.setText(recentMarker.label);
         actionRecentMarker.__recentMarker = recentMarker;

         addActionToMenu(actionRecentMarker);
      }
   }
}
