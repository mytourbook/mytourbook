/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.Util;
import net.tourbook.tour.ActionOpenAdjustAltitudeDialog;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.ui.views.rawData.ActionMergeTour;

import org.eclipse.jface.dialogs.IDialogSettings;

public class TourActionManager {

   private static final String            ID                             = "net.tourbook.ui.action.TourActionManager"; //$NON-NLS-1$

   private static final String            STATE_ALL_CHECKED_TOUR_ACTIONS = "STATE_ALL_CHECKED_TOUR_ACTIONS";           //$NON-NLS-1$
   private static final String            STATE_ALL_SORTED_TOUR_ACTIONS  = "STATE_ALL_SORTED_TOUR_ACTIONS";            //$NON-NLS-1$

   private static final IDialogSettings   _state                         = TourbookPlugin.getState(ID);

   private static List<TourAction>        _allDefinedActions;
   private static List<TourAction>        _allSortedActions;
   private static List<TourAction>        _allCheckedActions;

   /**
    * Key is the action class name
    */
   private static Map<String, TourAction> _allActionMap;

   static {

      createActions();
   }

   private static void createActions() {

      // create a map with all available actions

      _allActionMap = new HashMap<>();
      _allDefinedActions = new ArrayList<>();

// SET_FORMATTING_OFF

      final TourAction actionEditQuick                   = new TourAction(
            ActionEditQuick.class,
            Messages.app_action_quick_edit,
            TourbookPlugin.getThemedImageDescriptor(Images.App_Edit));

      final TourAction actionEditTour                    = new TourAction(
            ActionEditTour.class,
            Messages.App_Action_edit_tour,
            TourbookPlugin.getThemedImageDescriptor(Images.EditTour));

      final TourAction actionOpenMarkerDialog            = new TourAction(
            ActionOpenMarkerDialog.class,
            Messages.app_action_edit_tour_marker,
            TourbookPlugin.getThemedImageDescriptor(Images.TourMarker));

      final TourAction actionOpenAdjustAltitudeDialog    = new TourAction(
            ActionOpenAdjustAltitudeDialog.class,
            Messages.app_action_edit_adjust_altitude,
            TourbookPlugin.getThemedImageDescriptor(Images.AdjustElevation));

      final TourAction actionSetStartEndLocation         = new TourAction(
            ActionSetStartEndLocation.class,
            Messages.Tour_Location_Action_ManageStartEndLocation,
            TourbookPlugin.getThemedImageDescriptor(Images.Tour_StartEnd));

      final TourAction actionOpenTour                    = new TourAction(
            ActionOpenTour.class,
            Messages.app_action_open_tour,
            TourbookPlugin.getThemedImageDescriptor(Images.TourViewer));

      final TourAction actionDuplicateTour               = new TourAction(
            ActionDuplicateTour.class,
            Messages.Tour_Action_DuplicateTour,
            TourbookPlugin.getImageDescriptor(Images.Tour_Duplicate));

      final TourAction actionMergeTour                   = new TourAction(
            ActionMergeTour.class,
            Messages.app_action_merge_tour,
            TourbookPlugin.getImageDescriptor(Images.MergeTours));

      final TourAction actionJoinTours                   = new TourAction(
            ActionJoinTours.class,
            Messages.App_Action_JoinTours,
            null);

      _allDefinedActions.add(actionEditQuick);
      _allDefinedActions.add(actionEditTour);
      _allDefinedActions.add(actionOpenMarkerDialog);
      _allDefinedActions.add(actionOpenAdjustAltitudeDialog);
      _allDefinedActions.add(actionSetStartEndLocation);
      _allDefinedActions.add(actionOpenTour);
      _allDefinedActions.add(actionDuplicateTour);
      _allDefinedActions.add(actionMergeTour);
      _allDefinedActions.add(actionJoinTours);

      _allActionMap.put(ActionEditQuick.class.getName(),                  actionEditQuick);
      _allActionMap.put(ActionEditTour.class.getName(),                   actionEditTour);
      _allActionMap.put(ActionOpenMarkerDialog.class.getName(),           actionOpenMarkerDialog);
      _allActionMap.put(ActionOpenAdjustAltitudeDialog.class.getName(),   actionOpenAdjustAltitudeDialog);
      _allActionMap.put(ActionSetStartEndLocation.class.getName(),        actionSetStartEndLocation);
      _allActionMap.put(ActionOpenTour.class.getName(),                   actionOpenTour);
      _allActionMap.put(ActionDuplicateTour.class.getName(),              actionDuplicateTour);
      _allActionMap.put(ActionMergeTour.class.getName(),                  actionMergeTour);
      _allActionMap.put(ActionJoinTours.class.getName(),                  actionJoinTours);

// SET_FORMATTING_ON

   }

   private static void createCheckedActions(final List<TourAction> allCheckedActions) {

      final String[] stateAllCheckedActions = Util.getStateStringArray(_state, STATE_ALL_CHECKED_TOUR_ACTIONS, null);

      if (stateAllCheckedActions != null) {

         for (final String actionClassName : stateAllCheckedActions) {

            final TourAction tourAction = _allActionMap.get(actionClassName);

            if (tourAction != null) {

               tourAction.isChecked = true;

               allCheckedActions.add(tourAction);
            }
         }
      }
   }

   private static void createSortedActions(final List<TourAction> allSortedActions) {

      final String[] stateAllSortedActions = Util.getStateStringArray(_state, STATE_ALL_SORTED_TOUR_ACTIONS, null);

      // put all actions in the viewer which are defined in the statet
      if (stateAllSortedActions != null) {

         for (final String actionName : stateAllSortedActions) {

            final TourAction tourAction = _allActionMap.get(actionName);

            if (tourAction != null) {

               allSortedActions.add(tourAction);
            }
         }
      }

      // make sure that all available actions are in the viewer
      for (final TourAction tourAction : _allDefinedActions) {

         if (allSortedActions.contains(tourAction) == false) {

            allSortedActions.add(tourAction);
         }
      }
   }

   public static List<TourAction> getCheckedActions() {

      if (_allCheckedActions != null) {
         return _allCheckedActions;
      }

      _allCheckedActions = new ArrayList<>();

      createCheckedActions(_allCheckedActions);

      return _allCheckedActions;
   }

   public static List<TourAction> getDefinedActions() {

      return _allDefinedActions;
   }

   /**
    * Create a list with all available actions
    */
   public static List<TourAction> getSortedActions() {

      if (_allSortedActions != null) {
         return _allSortedActions;
      }

      _allSortedActions = new ArrayList<>();

      createSortedActions(_allSortedActions);

      return _allSortedActions;
   }

   public static void saveActions(final String[] allSortedActions, final String[] allCheckedActions) {

      _state.put(STATE_ALL_SORTED_TOUR_ACTIONS, allSortedActions);
      _state.put(STATE_ALL_CHECKED_TOUR_ACTIONS, allCheckedActions);

      _allSortedActions.clear();
      _allCheckedActions.clear();

      createSortedActions(_allSortedActions);
      createCheckedActions(_allCheckedActions);
   }
}
