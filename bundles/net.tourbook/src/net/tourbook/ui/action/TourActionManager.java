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
import net.tourbook.extension.export.ActionExport;
import net.tourbook.extension.upload.ActionUpload;
import net.tourbook.tag.Action_AddTourTag_SubMenu;
import net.tourbook.tag.Action_RemoveTourTag_SubMenu;
import net.tourbook.tag.TagMenuManager.ActionClipboard_CopyTags;
import net.tourbook.tag.TagMenuManager.ActionClipboard_PasteTags;
import net.tourbook.tag.TagMenuManager.ActionShowTourTagsView;
import net.tourbook.tag.TagMenuManager.ActionTagGroups_SubMenu;
import net.tourbook.tag.TagMenuManager.Action_RemoveAllTags;
import net.tourbook.tour.ActionOpenAdjustAltitudeDialog;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.printing.ActionPrint;
import net.tourbook.ui.views.rawData.ActionDeleteTourValues;
import net.tourbook.ui.views.rawData.ActionMergeTour;
import net.tourbook.ui.views.rawData.ActionReimportTours;
import net.tourbook.ui.views.rawData.SubMenu_AdjustTourValues;
import net.tourbook.ui.views.tourBook.ActionDeleteTourMenu;
import net.tourbook.ui.views.tourBook.ActionExportViewCSV;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;

public class TourActionManager {

   private static final String            ID                              = "net.tourbook.ui.action.TourActionManager"; //$NON-NLS-1$

   private static final String            STATE_ALL_SORTED_TOUR_ACTIONS   = "STATE_ALL_SORTED_TOUR_ACTIONS";            //$NON-NLS-1$
   private static final String            STATE_ALL_VISIBLE_TOUR_ACTIONS  = "STATE_ALL_VISIBLE_TOUR_ACTIONS";           //$NON-NLS-1$
   private static final String            STATE_IS_CUSTOMIZE_TOUR_ACTIONS = "STATE_IS_CUSTOMIZE_TOUR_ACTIONS";          //$NON-NLS-1$

   private static final IDialogSettings   _state                          = TourbookPlugin.getState(ID);

   private static List<TourAction>        _allDefinedActions;
   private static List<TourAction>        _allSortedActions;
   private static List<TourAction>        _allVisibleActions;

   private static Boolean                 _isCustomizeActions;

   /**
    * Key is the action class name
    */
   private static Map<String, TourAction> _allActionsMap;

   static {

      createActions();
   }

   private static void createActions() {

      // create a map with all available actions

      _allActionsMap = new HashMap<>();
      _allDefinedActions = new ArrayList<>();

// SET_FORMATTING_OFF

      /**
       * The sequence of the actions/categories is VERY important
       */

      /*
       * EDIT ACTIONS
       */

      final TourAction categoryAction_Edit               = new TourAction(
            "EDIT TOUR",
            TourActionCategory.EDIT
            );

      final TourAction actionEditQuick                   = new TourAction(
            ActionEditQuick.class,
            Messages.app_action_quick_edit,
            TourbookPlugin.getThemedImageDescriptor(Images.App_Edit),
            TourbookPlugin.getThemedImageDescriptor(Images.App_Edit_Disabled),
            TourActionCategory.EDIT);

      final TourAction actionEditTour                    = new TourAction(
            ActionEditTour.class,
            Messages.App_Action_edit_tour,
            TourbookPlugin.getThemedImageDescriptor(Images.EditTour),
            TourbookPlugin.getImageDescriptor(Images.EditTour_Disabled),
            TourActionCategory.EDIT);

      final TourAction actionOpenMarkerDialog            = new TourAction(
            ActionOpenMarkerDialog.class,
            Messages.app_action_edit_tour_marker,
            TourbookPlugin.getThemedImageDescriptor(Images.TourMarker),
            TourbookPlugin.getThemedImageDescriptor(Images.TourMarker_Disabled),
            TourActionCategory.EDIT);

      final TourAction actionOpenAdjustAltitudeDialog    = new TourAction(
            ActionOpenAdjustAltitudeDialog.class,
            Messages.app_action_edit_adjust_altitude,
            TourbookPlugin.getThemedImageDescriptor(Images.AdjustElevation),
            TourbookPlugin.getThemedImageDescriptor(Images.AdjustElevation_Disabled),
            TourActionCategory.EDIT);

      final TourAction actionSetStartEndLocation         = new TourAction(
            ActionSetStartEndLocation.class,
            Messages.Tour_Location_Action_ManageStartEndLocation,
            TourbookPlugin.getThemedImageDescriptor(Images.Tour_StartEnd),
            null,
            TourActionCategory.EDIT);

      final TourAction actionOpenTour                    = new TourAction(
            ActionOpenTour.class,
            Messages.app_action_open_tour,
            TourbookPlugin.getThemedImageDescriptor(Images.TourViewer),
            null,
            TourActionCategory.EDIT);

      final TourAction actionDuplicateTour               = new TourAction(
            ActionDuplicateTour.class,
            Messages.Tour_Action_DuplicateTour,
            TourbookPlugin.getImageDescriptor(Images.Tour_Duplicate),
            TourbookPlugin.getImageDescriptor(Images.Tour_Duplicate_Disabled),
            TourActionCategory.EDIT);

      final TourAction actionMergeTour                   = new TourAction(
            ActionMergeTour.class,
            Messages.app_action_merge_tour,
            TourbookPlugin.getImageDescriptor(Images.MergeTours),
            null,
            TourActionCategory.EDIT);

      final TourAction actionJoinTours                   = new TourAction(
            ActionJoinTours.class,
            Messages.App_Action_JoinTours,
            null,
            null,
            TourActionCategory.EDIT);

      /*
       * TAG ACTIONS
       */
      final TourAction categoryAction_Tag             = new TourAction(
            "TAGS",
            TourActionCategory.TAG);

      final TourAction actionAddTourTag               = new TourAction(
            Action_AddTourTag_SubMenu.class,
            Messages.action_tag_add,
            null,
            null,
            TourActionCategory.TAG);

      final TourAction actionAddTagGroups             = new TourAction(
            ActionTagGroups_SubMenu.class,
            Messages.Action_Tag_AddGroupedTags,
            null,
            null,
            TourActionCategory.TAG);

      final TourAction actionClipboard_CopyTags       = new TourAction(
            ActionClipboard_CopyTags.class,
            "&Copy Tags",
            null,
            null,
            TourActionCategory.TAG);

      final TourAction actionClipboard_PasteTags      = new TourAction(
            ActionClipboard_PasteTags.class,
            "&Paste Tags",
            null,
            null,
            TourActionCategory.TAG);

      final TourAction actionRemoveTourTag            = new TourAction(
            Action_RemoveTourTag_SubMenu.class,
            Messages.action_tag_remove,
            null,
            null,
            TourActionCategory.TAG);

      final TourAction actionRemoveAllTags            = new TourAction(
            Action_RemoveAllTags.class,
            Messages.action_tag_remove_all,
            null,
            null,
            TourActionCategory.TAG);

      final TourAction actionShowTourTagsView         = new TourAction(
            ActionShowTourTagsView.class,
            Messages.Action_Tag_SetTags,
            TourbookPlugin.getImageDescriptor(Images.TourTags),
            null,
            TourActionCategory.TAG);

//      private ActionContributionItem         _actionAddTagAdvanced;

      /*
       * EXPORT ACTIONS
       */

      final TourAction categoryAction_Export             = new TourAction(
            "EXPORT TOUR",
            TourActionCategory.EXPORT);

      final TourAction actionUploadTour                  = new TourAction(
            ActionUpload.class,
            Messages.App_Action_Upload_Tour,
            null,
            null,
            TourActionCategory.EXPORT);

      final TourAction actionExportTour                  = new TourAction(
            ActionExport.class,
            Messages.action_export_tour,
            null,
            null,
            TourActionCategory.EXPORT);

      final TourAction actionExportTourCSV               = new TourAction(
            ActionExportViewCSV.class,
            Messages.App_Action_ExportViewCSV,
            TourbookPlugin.getImageDescriptor(Images.CSVFormat),
            TourbookPlugin.getImageDescriptor(Images.CSVFormat_Disabled),
            TourActionCategory.EXPORT);

      final TourAction actionPrintTour                  = new TourAction(
            ActionPrint.class,
            Messages.action_print_tour,
            null,
            null,
            TourActionCategory.EXPORT);

      /*
       * ADJUST ACTIONS
       */

      final TourAction categoryAction_Adjust             = new TourAction(
            "ADJUST TOUR",
            TourActionCategory.ADJUST);

      final TourAction actionAdjustTourValues            = new TourAction(
            SubMenu_AdjustTourValues.class,
            Messages.Tour_Action_AdjustTourValues,
            null,
            null,
            TourActionCategory.ADJUST);

      final TourAction actionDeletTourValues             = new TourAction(
            ActionDeleteTourValues.class,
            Messages.Dialog_DeleteTourValues_Action_OpenDialog,
            null,
            null,
            TourActionCategory.ADJUST);

      final TourAction actionReimportTours               = new TourAction(
            ActionReimportTours.class,
            Messages.Dialog_ReimportTours_Action_OpenDialog,
            null,
            null,
            TourActionCategory.ADJUST);

      final TourAction actionSetOtherPersion             = new TourAction(
            ActionSetPerson.class,
            Messages.App_Action_SetPerson,
            null,
            null,
            TourActionCategory.ADJUST);

      final TourAction actionDeleteTourMenu              = new TourAction(
            ActionDeleteTourMenu.class,
            Messages.Tour_Book_Action_delete_selected_tours_menu,
            TourbookPlugin.getImageDescriptor(Images.State_Delete),
            null,
            TourActionCategory.ADJUST);


      // edit actions
      _allDefinedActions.add(categoryAction_Edit);

      _allDefinedActions.add(actionEditQuick);
      _allDefinedActions.add(actionEditTour);
      _allDefinedActions.add(actionOpenMarkerDialog);
      _allDefinedActions.add(actionOpenAdjustAltitudeDialog);
      _allDefinedActions.add(actionSetStartEndLocation);
      _allDefinedActions.add(actionOpenTour);
      _allDefinedActions.add(actionDuplicateTour);
      _allDefinedActions.add(actionMergeTour);
      _allDefinedActions.add(actionJoinTours);

      _allActionsMap.put(categoryAction_Edit             .getCategoryClassName(),   categoryAction_Edit);

      _allActionsMap.put(ActionEditQuick                 .class.getName(),          actionEditQuick);
      _allActionsMap.put(ActionEditTour                  .class.getName(),          actionEditTour);
      _allActionsMap.put(ActionOpenMarkerDialog          .class.getName(),          actionOpenMarkerDialog);
      _allActionsMap.put(ActionOpenAdjustAltitudeDialog  .class.getName(),          actionOpenAdjustAltitudeDialog);
      _allActionsMap.put(ActionSetStartEndLocation       .class.getName(),          actionSetStartEndLocation);
      _allActionsMap.put(ActionOpenTour                  .class.getName(),          actionOpenTour);
      _allActionsMap.put(ActionDuplicateTour             .class.getName(),          actionDuplicateTour);
      _allActionsMap.put(ActionMergeTour                 .class.getName(),          actionMergeTour);
      _allActionsMap.put(ActionJoinTours                 .class.getName(),          actionJoinTours);

      // tag actions
      _allDefinedActions.add(categoryAction_Tag);

      _allDefinedActions.add(actionShowTourTagsView);
      _allDefinedActions.add(actionAddTourTag);
      _allDefinedActions.add(actionAddTagGroups);
      _allDefinedActions.add(actionClipboard_CopyTags);
      _allDefinedActions.add(actionClipboard_PasteTags);
      _allDefinedActions.add(actionRemoveTourTag);
      _allDefinedActions.add(actionRemoveAllTags);

      _allActionsMap.put(categoryAction_Tag              .getCategoryClassName(),   categoryAction_Tag);

      _allActionsMap.put(Action_AddTourTag_SubMenu       .class.getName(),          actionAddTourTag);
      _allActionsMap.put(ActionTagGroups_SubMenu         .class.getName(),          actionAddTagGroups);
      _allActionsMap.put(ActionClipboard_CopyTags        .class.getName(),          actionClipboard_CopyTags);
      _allActionsMap.put(ActionClipboard_PasteTags       .class.getName(),          actionClipboard_PasteTags);
      _allActionsMap.put(Action_RemoveTourTag_SubMenu    .class.getName(),          actionRemoveTourTag);
      _allActionsMap.put(Action_RemoveAllTags            .class.getName(),          actionRemoveAllTags);
      _allActionsMap.put(ActionShowTourTagsView          .class.getName(),          actionShowTourTagsView);

      // export actions
      _allDefinedActions.add(categoryAction_Export);

      _allDefinedActions.add(actionUploadTour);
      _allDefinedActions.add(actionExportTour);
      _allDefinedActions.add(actionExportTourCSV);
      _allDefinedActions.add(actionPrintTour);

      _allActionsMap.put(categoryAction_Export           .getCategoryClassName(),   categoryAction_Export);

      _allActionsMap.put(ActionUpload                    .class.getName(),          actionUploadTour);
      _allActionsMap.put(ActionExport                    .class.getName(),          actionExportTour);
      _allActionsMap.put(ActionExportViewCSV             .class.getName(),          actionExportTourCSV);
      _allActionsMap.put(ActionPrint                     .class.getName(),          actionPrintTour);

      // adjust actions
      _allDefinedActions.add(categoryAction_Adjust);

      _allDefinedActions.add(actionAdjustTourValues);
      _allDefinedActions.add(actionDeletTourValues);
      _allDefinedActions.add(actionReimportTours);
      _allDefinedActions.add(actionSetOtherPersion);
      _allDefinedActions.add(actionDeleteTourMenu);

      _allActionsMap.put(categoryAction_Adjust           .getCategoryClassName(),   categoryAction_Adjust);

      _allActionsMap.put(SubMenu_AdjustTourValues        .class.getName(),          actionAdjustTourValues);
      _allActionsMap.put(ActionDeleteTourValues          .class.getName(),          actionDeletTourValues);
      _allActionsMap.put(ActionReimportTours             .class.getName(),          actionReimportTours);
      _allActionsMap.put(ActionSetPerson                 .class.getName(),          actionSetOtherPersion);
      _allActionsMap.put(ActionDeleteTourMenu            .class.getName(),          actionDeleteTourMenu);

// SET_FORMATTING_ON

   }

   private static void createSortedActions(final List<TourAction> allSortedActions) {

      final String[] stateAllSortedActions = Util.getStateStringArray(_state, STATE_ALL_SORTED_TOUR_ACTIONS, null);

      if (stateAllSortedActions != null) {

         // put all actions in the viewer which are defined in the state

         for (final String actionName : stateAllSortedActions) {

            final TourAction tourAction = _allActionsMap.get(actionName);

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

   private static void createVisibleActions(final List<TourAction> allVisibleActions) {

      final String[] stateAllCheckedActions = Util.getStateStringArray(_state, STATE_ALL_VISIBLE_TOUR_ACTIONS, null);

      if (stateAllCheckedActions == null) {
         return;
      }

      for (final String actionClassName : stateAllCheckedActions) {

         final TourAction tourAction = _allActionsMap.get(actionClassName);

         if (tourAction != null) {

            tourAction.isChecked = true;

            allVisibleActions.add(tourAction);
         }
      }
   }

   /**
    * @param menuMgr
    * @param actionCategory
    * @param allCategoryActions
    *           Contains all actions with the actionCategory {@link TourActionCategory}
    * @param allActiveActions
    */
   public static void fillContextMenu(final IMenuManager menuMgr,
                                      final TourActionCategory actionCategory,
                                      final HashMap<String, Object> allCategoryActions,
                                      final List<TourAction> allActiveActions) {

      for (final TourAction activeTourAction : allActiveActions) {

         if (activeTourAction.actionCategory == actionCategory) {

            if (activeTourAction.actionClass instanceof final Class clazz) {

               final Object tourAction = allCategoryActions.get(clazz.getName());

               if (tourAction instanceof final IAction action) {
                  menuMgr.add(action);
               }
            }
         }
      }
   }

   public static List<TourAction> getActiveActions() {

      if (isCustomizeActions()) {

         return TourActionManager.getVisibleActions();

      } else {

         return TourActionManager.getSortedActions();
      }
   }

   public static List<TourAction> getDefinedActions() {

      return _allDefinedActions;
   }

   /**
    * @return Returns all actions which are defined and sorted
    */
   public static List<TourAction> getSortedActions() {

//      if (_allSortedActions == null) {

      _allSortedActions = new ArrayList<>();

      createSortedActions(_allSortedActions);
//      }

      return _allSortedActions;
   }

   /**
    * @return Returns all actions which should be displayed in the tour context menu
    */
   public static List<TourAction> getVisibleActions() {

      if (_allVisibleActions == null) {

         _allVisibleActions = new ArrayList<>();

         createVisibleActions(_allVisibleActions);
      }

      return _allVisibleActions;
   }

   public static boolean isCustomizeActions() {

      if (_isCustomizeActions == null) {

         _isCustomizeActions = Util.getStateBoolean(_state, STATE_IS_CUSTOMIZE_TOUR_ACTIONS, true);
      }

      return _isCustomizeActions;
   }

   public static void saveActions(final boolean isCustomizeActions,
                                  final String[] allSortedActions,
                                  final String[] allCheckedActions) {

      _state.put(STATE_IS_CUSTOMIZE_TOUR_ACTIONS, isCustomizeActions);
      _state.put(STATE_ALL_SORTED_TOUR_ACTIONS, allSortedActions);
      _state.put(STATE_ALL_VISIBLE_TOUR_ACTIONS, allCheckedActions);

      _isCustomizeActions = isCustomizeActions;

      _allSortedActions.clear();
      _allVisibleActions.clear();

      createSortedActions(_allSortedActions);
      createVisibleActions(_allVisibleActions);
   }
}
