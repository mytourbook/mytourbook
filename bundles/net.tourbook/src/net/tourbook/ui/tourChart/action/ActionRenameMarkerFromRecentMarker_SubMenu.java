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
package net.tourbook.ui.tourChart.action;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.data.TourMarker;
import net.tourbook.tour.TourManager;
import net.tourbook.tourMarker.ActionClearRecentMarkers;
import net.tourbook.tourMarker.ActionHeader_AllRecentMarkers;
import net.tourbook.tourMarker.ActionHeader_CustomizeRecentMarkers;
import net.tourbook.tourMarker.ActionSortRecentMarkers;
import net.tourbook.tourMarker.RecentMarker;
import net.tourbook.tourMarker.TourMarkerManager;
import net.tourbook.ui.tourChart.ITourMarkerUpdater;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;

/**
 * Set the label of an existing {@link TourMarker} from recently used tour marker labels
 */
public class ActionRenameMarkerFromRecentMarker_SubMenu extends SubMenu {

   private TourMarker                          _tourMarker;
   private ITourMarkerUpdater                  _tourMarkerUpdater;

   private ActionHeader_AllRecentMarkers       _actionHeader_AllRecentMarkers;
   private ActionHeader_CustomizeRecentMarkers _actionHeader_CustomizeRecentMarkers;
   private ActionClearRecentMarkers            _actionClearRecentMarkers;
   private ActionPickRecentMarker              _actionPickRecentMarker;
   private ActionSortRecentMarkers             _actionSortRecentMarkers;

   private List<ActionRecentMarker>            _allRecentMarkerActions = new ArrayList<>();

   private class ActionPickRecentMarker extends Action {

      public ActionPickRecentMarker() {

         super(Messages.Action_TourMarker_PickRecentMarker, AS_PUSH_BUTTON);

         setToolTipText(Messages.Action_TourMarker_PickRecentMarker_Tooltip);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TourMarker_New));
      }

      @Override
      public void run() {

         // set last used marker to the top of the recent markers
         TourMarkerManager.addRecentMarker(_tourMarker.getLabel());
      }
   }

   private class ActionRecentMarker extends Action {

      private RecentMarker __recentMarker;

      public ActionRecentMarker() {

         super(UI.EMPTY_STRING, AS_PUSH_BUTTON);

         setToolTipText(Messages.Action_TourMarker_RecentMarker_Tooltip);
      }

      @Override
      public void runWithEvent(final Event event) {

         actionRenameMarker(__recentMarker, event);
      }
   }

   public ActionRenameMarkerFromRecentMarker_SubMenu(final ITourMarkerUpdater tourMarkerUpdater) {

      super(Messages.Action_TourMarker_ReplaceWithRecentMarker, AS_DROP_DOWN_MENU);

      _tourMarkerUpdater = tourMarkerUpdater;

      createActions();
   }

   private void actionRenameMarker(final RecentMarker recentMarker, final Event event) {

      if (UI.isCtrlKey(event)) {

         // remove this marker

         TourMarkerManager.removeRecentMarker(recentMarker);

         return;
      }

      // make sure the tour editor does not contain a modified tour
      if (TourManager.isTourEditorModified()) {
         return;
      }

      _tourMarker.setLabel(recentMarker.label);

      _tourMarkerUpdater.updateModifiedTourMarker(_tourMarker);

      // set last used marker to the top of the list
      TourMarkerManager.addRecentMarker(_tourMarker.getLabel());
   }

   private void createActions() {

      // create submenu actions which will be updated when displayed
      for (int actionIndex = 0; actionIndex < TourMarkerManager.MAX_NUMBER_OF_RECENT_MARKERS; actionIndex++) {
         _allRecentMarkerActions.add(new ActionRecentMarker());
      }

// SET_FORMATTING_OFF

      _actionHeader_AllRecentMarkers         = new ActionHeader_AllRecentMarkers();
      _actionHeader_CustomizeRecentMarkers   = new ActionHeader_CustomizeRecentMarkers();
      _actionPickRecentMarker                = new ActionPickRecentMarker();
      _actionClearRecentMarkers              = new ActionClearRecentMarkers();
      _actionSortRecentMarkers               = new ActionSortRecentMarkers();

// SET_FORMATTING_ON
   }

   @Override
   public void enableActions() {

   }

   @Override
   public void fillMenu(final Menu menu) {

      addActionToMenu(_actionHeader_AllRecentMarkers);

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

      addSeparatorToMenu();
      addActionToMenu(_actionHeader_CustomizeRecentMarkers);
      addActionToMenu(_actionPickRecentMarker);
      addActionToMenu(_actionSortRecentMarkers);
      addActionToMenu(_actionClearRecentMarkers);
   }

   /**
    * Set the {@link TourMarker} which should be updated
    *
    * @param tourMarker
    */
   public void setTourMarker(final TourMarker tourMarker) {

      _tourMarker = tourMarker;
   }

}
