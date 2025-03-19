/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourMarkerType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.PrefPageTourMarkerTypes;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Menu;

/**
 */
public class SubMenu_SetTourMarkerType extends SubMenu {

   private List<TourMarker>     _allTourMarker;

   private ActionOpenPrefDialog _actionOpenTourTypePrefs;

   private class ActionSetMarkerType extends Action {

      private TourMarkerType _markerType;

      public ActionSetMarkerType(final TourMarkerType markerType) {

         super(markerType.getTypeName(), AS_CHECK_BOX);

         _markerType = markerType;
      }

      @Override
      public void run() {

         if (TourManager.isTourEditorModified()) {
            return;
         }

         final Map<Long, TourData> allModifiedToursByID = new HashMap<>();

         for (final TourMarker tourMarker : _allTourMarker) {

            // set marker type
            tourMarker.setTourMarkerType(_markerType);

            // keep the markers tour
            final TourData tourData = tourMarker.getTourData();

            allModifiedToursByID.put(tourData.getTourId(), tourData);
         }

         final ArrayList<TourData> allModifiedTours = new ArrayList<>(allModifiedToursByID.values());

         TourManager.saveModifiedTours(allModifiedTours);
      }
   }

   public SubMenu_SetTourMarkerType() {

      super("Set Tour Marker T&ype", AS_DROP_DOWN_MENU);

      _actionOpenTourTypePrefs = new ActionOpenPrefDialog(
            "Modify Tour Marker T&ype...",
            PrefPageTourMarkerTypes.ID);
   }

   @Override
   public void enableActions() {}

   @Override
   public void fillMenu(final Menu menu) {

      final TourMarkerType currentMarkerType = _allTourMarker.size() == 1
            ? _allTourMarker.get(0).getTourMarkerType()
            : null;

      final List<TourMarkerType> allTourMarkerTypes = TourDatabase.getAllTourMarkerTypes();

      for (final TourMarkerType markerType : allTourMarkerTypes) {

         final ActionSetMarkerType actionSetMarkerType = new ActionSetMarkerType(markerType);

         // setup current marker type
         if (currentMarkerType != null && markerType.getId() == currentMarkerType.getId()) {

            actionSetMarkerType.setChecked(true);
            actionSetMarkerType.setEnabled(false);
         }

         new ActionContributionItem(actionSetMarkerType).fill(menu, -1);
      }

      new Separator().fill(menu, -1);
      new ActionContributionItem(_actionOpenTourTypePrefs).fill(menu, -1);
   }

   public void setTourMarker(final Object[] allTourMarker) {

      _allTourMarker = new ArrayList<>();

      Arrays.stream(allTourMarker).forEach(tourMarker -> _allTourMarker.add((TourMarker) tourMarker));

   }

   public void setTourMarker(final TourMarker tourMarker) {

      final List<TourMarker> arrayList = new ArrayList<>();
      arrayList.add(tourMarker);

      _allTourMarker = arrayList;
   }

}
