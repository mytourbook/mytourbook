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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.util.Util;
import net.tourbook.extension.export.ActionExport;
import net.tourbook.extension.upload.ActionUpload;
import net.tourbook.preferences.PrefPageAppearance_TourActions;
import net.tourbook.tag.ActionAddRecentTags;
import net.tourbook.tag.ActionAddRecentTourTypes;
import net.tourbook.tag.ActionAddTourTag_SubMenu;
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

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;

public class TourActionManager {

   public static final String              AUTO_OPEN                            = "#AutoOpen";                                //$NON-NLS-1$

   private static final String             ID                                   = "net.tourbook.ui.action.TourActionManager"; //$NON-NLS-1$

   private static final String             STATE_ALL_SORTED_TOUR_ACTIONS        = "STATE_ALL_SORTED_TOUR_ACTIONS";            //$NON-NLS-1$
   private static final String             STATE_ALL_VISIBLE_TOUR_ACTIONS       = "STATE_ALL_VISIBLE_TOUR_ACTIONS";           //$NON-NLS-1$
   private static final String             STATE_IS_CUSTOMIZE_TOUR_ACTIONS      = "STATE_IS_CUSTOMIZE_TOUR_ACTIONS";          //$NON-NLS-1$
   private static final String             STATE_IS_SHOW_ONLY_AVAILABLE_ACTIONS = "STATE_IS_SHOW_ONLY_AVAILABLE_ACTIONS";     //$NON-NLS-1$

   private static final IDialogSettings    _state                               = TourbookPlugin.getState(ID);

   private static List<TourAction>         _allDefinedActions;
   private static List<TourAction>         _allSortedActions;
   private static List<TourAction>         _allVisibleActions;
   
   /**
    * Key is the action class name or in special cases a modified class name
    */
   private static Map<String, TourAction>  _allDefinedActionsMap;

   private static ActionOpenPrefDialog     _actionCustomizeTourActions;

   private static Boolean                  _isCustomizeActions;

   /**
    * Contains all tour action ID's from all views, key is the view ID
    */
   private static Map<String, Set<String>> _allViewActions                      = new HashMap<>();

   static {

      createActions();
   }

   /**
    * The sequence of the actions/categories in _allDefinedActions is VERY important, this is the
    * default sorting.
    */
   private static void createActions() {

      // create a map with all available actions
      _allDefinedActions = new ArrayList<>();
      _allDefinedActionsMap = new HashMap<>();

      createActions_10_Edit();
      createActions_20_Tags();
      createActions_30_TourTypes();
      createActions_40_Export();
      createActions_50_Adjust();

      _actionCustomizeTourActions = new ActionOpenPrefDialog(
            Messages.Tour_Action_ContextMenu_Customize,
            PrefPageAppearance_TourActions.ID);
   }

   /**
    * EDIT ACTIONS
    */
   private static void createActions_10_Edit() {

// SET_FORMATTING_OFF

      final TourAction categoryAction_Edit               = new TourAction(
            Messages.Tour_Action_Category_EditTour,
            TourActionCategory.EDIT
            );

      final TourAction actionEditQuick                   = new TourAction(
            ActionEditQuick.class.getName(),
            Messages.app_action_quick_edit,
            TourbookPlugin.getThemedImageDescriptor(Images.App_Edit),
            TourbookPlugin.getThemedImageDescriptor(Images.App_Edit_Disabled),
            TourActionCategory.EDIT);

      final TourAction actionEditTour                    = new TourAction(
            ActionEditTour.class.getName(),
            Messages.App_Action_edit_tour,
            TourbookPlugin.getThemedImageDescriptor(Images.EditTour),
            TourbookPlugin.getImageDescriptor(Images.EditTour_Disabled),
            TourActionCategory.EDIT);

      final TourAction actionOpenMarkerDialog            = new TourAction(
            ActionOpenMarkerDialog.class.getName(),
            Messages.app_action_edit_tour_marker,
            TourbookPlugin.getThemedImageDescriptor(Images.TourMarker),
            TourbookPlugin.getThemedImageDescriptor(Images.TourMarker_Disabled),
            TourActionCategory.EDIT);

      final TourAction actionOpenAdjustAltitudeDialog    = new TourAction(
            ActionOpenAdjustAltitudeDialog.class.getName(),
            Messages.app_action_edit_adjust_altitude,
            TourbookPlugin.getThemedImageDescriptor(Images.AdjustElevation),
            TourbookPlugin.getThemedImageDescriptor(Images.AdjustElevation_Disabled),
            TourActionCategory.EDIT);

      final TourAction actionSetStartEndLocation         = new TourAction(
            ActionSetStartEndLocation.class.getName(),
            Messages.Tour_Location_Action_ManageStartEndLocation,
            TourbookPlugin.getThemedImageDescriptor(Images.Tour_StartEnd),
            null,
            TourActionCategory.EDIT);

      final TourAction actionOpenTour                    = new TourAction(
            ActionOpenTour.class.getName(),
            Messages.app_action_open_tour,
            TourbookPlugin.getThemedImageDescriptor(Images.TourViewer),
            null,
            TourActionCategory.EDIT);

      final TourAction actionDuplicateTour               = new TourAction(
            ActionDuplicateTour.class.getName(),
            Messages.Tour_Action_DuplicateTour,
            TourbookPlugin.getImageDescriptor(Images.Tour_Duplicate),
            TourbookPlugin.getImageDescriptor(Images.Tour_Duplicate_Disabled),
            TourActionCategory.EDIT);

      final TourAction actionMergeTour                   = new TourAction(
            ActionMergeTour.class.getName(),
            Messages.app_action_merge_tour,
            TourbookPlugin.getImageDescriptor(Images.MergeTours),
            null,
            TourActionCategory.EDIT);

      final TourAction actionJoinTours                   = new TourAction(
            ActionJoinTours.class.getName(),
            Messages.App_Action_JoinTours,
            TourActionCategory.EDIT);


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

      _allDefinedActionsMap.put(categoryAction_Edit             .getCategoryClassName(),   categoryAction_Edit);

      _allDefinedActionsMap.put(ActionEditQuick                 .class.getName(),          actionEditQuick);
      _allDefinedActionsMap.put(ActionEditTour                  .class.getName(),          actionEditTour);
      _allDefinedActionsMap.put(ActionOpenMarkerDialog          .class.getName(),          actionOpenMarkerDialog);
      _allDefinedActionsMap.put(ActionOpenAdjustAltitudeDialog  .class.getName(),          actionOpenAdjustAltitudeDialog);
      _allDefinedActionsMap.put(ActionSetStartEndLocation       .class.getName(),          actionSetStartEndLocation);
      _allDefinedActionsMap.put(ActionOpenTour                  .class.getName(),          actionOpenTour);
      _allDefinedActionsMap.put(ActionDuplicateTour             .class.getName(),          actionDuplicateTour);
      _allDefinedActionsMap.put(ActionMergeTour                 .class.getName(),          actionMergeTour);
      _allDefinedActionsMap.put(ActionJoinTours                 .class.getName(),          actionJoinTours);

// SET_FORMATTING_ON
   }

   /**
    * TAG ACTIONS
    */
   private static void createActions_20_Tags() {

// SET_FORMATTING_OFF

      final TourAction categoryAction_Tag             = new TourAction(
            Messages.Tour_Action_Category_Tags,
            TourActionCategory.TAG);

      final TourAction actionSetTags                  = new TourAction(
            ActionShowTourTagsView.class.getName(),
            Messages.Action_Tag_SetTags,
            TourbookPlugin.getImageDescriptor(Images.TourTags),
            null,
            TourActionCategory.TAG);

      final TourAction actionAddTag_AutoOpen          = new TourAction(
            ActionAddTourTag_SubMenu.class.getName() + AUTO_OPEN,
            Messages.Action_Tag_Add_AutoOpen,
            TourActionCategory.TAG);

      final TourAction actionAddTag                   = new TourAction(
            ActionAddTourTag_SubMenu.class.getName(),
            Messages.action_tag_add,
            TourActionCategory.TAG);

      final TourAction actionAddRecentTags            = new TourAction(
            ActionAddRecentTags.class.getName(),
            Messages.Action_Tag_AddRecentTgs,
            TourActionCategory.TAG);

      final TourAction actionAddTagGroups             = new TourAction(
            ActionTagGroups_SubMenu.class.getName(),
            Messages.Action_Tag_AddGroupedTags,
            TourActionCategory.TAG);

      final TourAction actionClipboard_CopyTags       = new TourAction(
            ActionClipboard_CopyTags.class.getName(),
            Messages.Action_Tag_CopyTags,
            TourActionCategory.TAG);

      final TourAction actionClipboard_PasteTags      = new TourAction(
            ActionClipboard_PasteTags.class.getName(),
            Messages.Action_Tag_PasteTags,
            TourActionCategory.TAG);

      final TourAction actionRemoveTourTag            = new TourAction(
            Action_RemoveTourTag_SubMenu.class.getName(),
            Messages.action_tag_remove,
            TourActionCategory.TAG);

      final TourAction actionRemoveAllTags            = new TourAction(
            Action_RemoveAllTags.class.getName(),
            Messages.action_tag_remove_all,
            TourActionCategory.TAG);

      _allDefinedActions.add(categoryAction_Tag);

      _allDefinedActions.add(actionSetTags);
      _allDefinedActions.add(actionAddTag_AutoOpen);
      _allDefinedActions.add(actionAddTagGroups);
      _allDefinedActions.add(actionAddTag);
      _allDefinedActions.add(actionAddRecentTags);
      _allDefinedActions.add(actionRemoveTourTag);
      _allDefinedActions.add(actionRemoveAllTags);
      _allDefinedActions.add(actionClipboard_CopyTags);
      _allDefinedActions.add(actionClipboard_PasteTags);

      _allDefinedActionsMap.put(categoryAction_Tag              .getCategoryClassName(),      categoryAction_Tag);

      _allDefinedActionsMap.put(ActionShowTourTagsView          .class.getName(),             actionSetTags);
      _allDefinedActionsMap.put(ActionAddTourTag_SubMenu        .class.getName() + AUTO_OPEN, actionAddTag_AutoOpen);
      _allDefinedActionsMap.put(ActionTagGroups_SubMenu         .class.getName(),             actionAddTagGroups);
      _allDefinedActionsMap.put(ActionAddTourTag_SubMenu        .class.getName(),             actionAddTag);
      _allDefinedActionsMap.put(ActionAddRecentTags             .class.getName(),             actionAddRecentTags);
      _allDefinedActionsMap.put(ActionClipboard_CopyTags        .class.getName(),             actionClipboard_CopyTags);
      _allDefinedActionsMap.put(ActionClipboard_PasteTags       .class.getName(),             actionClipboard_PasteTags);
      _allDefinedActionsMap.put(Action_RemoveTourTag_SubMenu    .class.getName(),             actionRemoveTourTag);
      _allDefinedActionsMap.put(Action_RemoveAllTags            .class.getName(),             actionRemoveAllTags);

// SET_FORMATTING_ON

   }

   /**
    * TOUR TYPE ACTIONS
    */
   private static void createActions_30_TourTypes() {

// SET_FORMATTING_OFF

      final TourAction categoryAction_TourType           = new TourAction(
            Messages.Tour_Action_Category_TourTypes,
            TourActionCategory.TOUR_TYPE);

      final TourAction actionSetTourType                 = new TourAction(
            ActionSetTourTypeMenu.class.getName(),
            Messages.App_Action_set_tour_type,
            TourActionCategory.TOUR_TYPE);


      final TourAction actionAddRecentTourTypes          = new TourAction(
            ActionAddRecentTourTypes.class.getName(),
            Messages.Action_TourType_AddRecentTourTypes,
            TourActionCategory.TOUR_TYPE);


      _allDefinedActions.add(categoryAction_TourType);

      _allDefinedActions.add(actionSetTourType);
      _allDefinedActions.add(actionAddRecentTourTypes);


      _allDefinedActionsMap.put(categoryAction_TourType         .getCategoryClassName(),      categoryAction_TourType);

      _allDefinedActionsMap.put(ActionSetTourTypeMenu           .class.getName(),             actionSetTourType);
      _allDefinedActionsMap.put(ActionAddRecentTourTypes        .class.getName(),             actionAddRecentTourTypes);

// SET_FORMATTING_ON

   }

   private static void createActions_40_Export() {

// SET_FORMATTING_OFF

      final TourAction categoryAction_Export             = new TourAction(
            Messages.Tour_Action_Category_ExportTour,
            TourActionCategory.EXPORT);

      final TourAction actionUploadTour                  = new TourAction(
            ActionUpload.class.getName(),
            Messages.App_Action_Upload_Tour,
            TourActionCategory.EXPORT);

      final TourAction actionExportTour                  = new TourAction(
            ActionExport.class.getName(),
            Messages.action_export_tour,
            TourActionCategory.EXPORT);

      final TourAction actionExportTourCSV               = new TourAction(
            ActionExportViewCSV.class.getName(),
            Messages.App_Action_ExportViewCSV,
            TourbookPlugin.getImageDescriptor(Images.CSVFormat),
            TourbookPlugin.getThemedImageDescriptor(Images.CSVFormat_Disabled),
            TourActionCategory.EXPORT);

      final TourAction actionPrintTour                   = new TourAction(
            ActionPrint.class.getName(),
            Messages.action_print_tour,
            TourActionCategory.EXPORT);


      _allDefinedActions.add(categoryAction_Export);

      _allDefinedActions.add(actionUploadTour);
      _allDefinedActions.add(actionExportTour);
      _allDefinedActions.add(actionExportTourCSV);
      _allDefinedActions.add(actionPrintTour);

      _allDefinedActionsMap.put(categoryAction_Export           .getCategoryClassName(),   categoryAction_Export);

      _allDefinedActionsMap.put(ActionUpload                    .class.getName(),          actionUploadTour);
      _allDefinedActionsMap.put(ActionExport                    .class.getName(),          actionExportTour);
      _allDefinedActionsMap.put(ActionExportViewCSV             .class.getName(),          actionExportTourCSV);
      _allDefinedActionsMap.put(ActionPrint                     .class.getName(),          actionPrintTour);

// SET_FORMATTING_ON
   }

   private static void createActions_50_Adjust() {

// SET_FORMATTING_OFF

      final TourAction categoryAction_Adjust             = new TourAction(
            Messages.Tour_Action_Category_AdjustTour,
            TourActionCategory.ADJUST);

      final TourAction actionAdjustTourValues            = new TourAction(
            SubMenu_AdjustTourValues.class.getName(),
            Messages.Tour_Action_AdjustTourValues,
            TourActionCategory.ADJUST);

      final TourAction actionDeletTourValues             = new TourAction(
            ActionDeleteTourValues.class.getName(),
            Messages.Dialog_DeleteTourValues_Action_OpenDialog,
            TourActionCategory.ADJUST);

      final TourAction actionReimportTours               = new TourAction(
            ActionReimportTours.class.getName(),
            Messages.Dialog_ReimportTours_Action_OpenDialog,
            TourActionCategory.ADJUST);

      final TourAction actionSetOtherPersion             = new TourAction(
            ActionSetPerson.class.getName(),
            Messages.App_Action_SetPerson,
            TourActionCategory.ADJUST);

      final TourAction actionDeleteTourMenu              = new TourAction(
            ActionDeleteTourMenu.class.getName(),
            Messages.Tour_Book_Action_delete_selected_tours_menu,
            TourbookPlugin.getImageDescriptor(Images.State_Delete),
            null,
            TourActionCategory.ADJUST);


      _allDefinedActions.add(categoryAction_Adjust);

      _allDefinedActions.add(actionAdjustTourValues);
      _allDefinedActions.add(actionDeletTourValues);
      _allDefinedActions.add(actionReimportTours);
      _allDefinedActions.add(actionSetOtherPersion);
      _allDefinedActions.add(actionDeleteTourMenu);

      _allDefinedActionsMap.put(categoryAction_Adjust           .getCategoryClassName(),   categoryAction_Adjust);

      _allDefinedActionsMap.put(SubMenu_AdjustTourValues        .class.getName(),          actionAdjustTourValues);
      _allDefinedActionsMap.put(ActionDeleteTourValues          .class.getName(),          actionDeletTourValues);
      _allDefinedActionsMap.put(ActionReimportTours             .class.getName(),          actionReimportTours);
      _allDefinedActionsMap.put(ActionSetPerson                 .class.getName(),          actionSetOtherPersion);
      _allDefinedActionsMap.put(ActionDeleteTourMenu            .class.getName(),          actionDeleteTourMenu);

// SET_FORMATTING_ON

   }

   private static void createSortedActions(final List<TourAction> allSortedActions) {

      final String[] stateAllSortedActions = Util.getStateStringArray(_state, STATE_ALL_SORTED_TOUR_ACTIONS, null);

      if (stateAllSortedActions != null) {

         // put all actions in the viewer which are defined in the state

         for (final String actionName : stateAllSortedActions) {

            final TourAction tourAction = _allDefinedActionsMap.get(actionName);

            if (tourAction != null) {

               allSortedActions.add(tourAction);
            }
         }
      }

      // make sure that all available actions are displayed
      for (final TourAction tourAction : _allDefinedActions) {

         if (allSortedActions.contains(tourAction) == false) {

            allSortedActions.add(tourAction);
         }
      }

      ensureActionCategoryPositions(allSortedActions);
   }

   private static void createVisibleActions(final List<TourAction> allVisibleActions) {

      final String[] stateAllCheckedActions = Util.getStateStringArray(_state, STATE_ALL_VISIBLE_TOUR_ACTIONS, null);

      if (stateAllCheckedActions == null) {
         return;
      }

      for (final String actionClassName : stateAllCheckedActions) {

         final TourAction tourAction = _allDefinedActionsMap.get(actionClassName);

         if (tourAction != null) {

            tourAction.isChecked = true;

            allVisibleActions.add(tourAction);
         }
      }
   }

   /**
    * Ensure that all actions are within its category
    *
    * @param allSortedActions
    */
   @SuppressWarnings("unchecked")
   private static void ensureActionCategoryPositions(final List<TourAction> allSortedActions) {

      /*
       * Get all categories and actions
       */
      final TourActionCategory[] allCategories = TourActionCategory.values();
      final int numCategories = allCategories.length;

      final TourAction[] allCategoryActions = new TourAction[numCategories];
      final List<TourAction>[] allCategoryTourActions = new ArrayList[numCategories];

      for (final TourAction tourAction : allSortedActions) {

         final int categoryOrdinal = tourAction.actionCategory.ordinal();

         if (tourAction.isCategory) {

            allCategoryActions[categoryOrdinal] = tourAction;

         } else {

            List<TourAction> allCatTourActions = allCategoryTourActions[categoryOrdinal];

            if (allCatTourActions == null) {
               allCatTourActions = allCategoryTourActions[categoryOrdinal] = new ArrayList<>();
            }

            allCatTourActions.add(tourAction);
         }
      }

      /*
       * Resort categories and actions
       */
      allSortedActions.clear();

      for (final TourAction categoryAction : allCategoryActions) {

         if (categoryAction == null) {
            continue;
         }

         // add category
         allSortedActions.add(categoryAction);

         // add actions
         final List<TourAction> allCatTourActions = allCategoryTourActions[categoryAction.actionCategory.ordinal()];

         for (final TourAction tourAction : allCatTourActions) {
            allSortedActions.add(tourAction);
         }
      }
   }

   /**
    * A separator is added before the actions
    *
    * @param menuMgr
    * @param actionCategory
    *           Only actions with this category will be filled into the context menu
    * @param allCategoryActions
    *           Contains all actions with the same actionCategory {@link TourActionCategory}
    */
   public static void fillContextMenu(final IMenuManager menuMgr,
                                      final TourActionCategory actionCategory,
                                      final HashMap<String, Object> allCategoryActions) {

      final List<TourAction> allActiveActions = getActiveActions();

      menuMgr.add(new Separator());

      for (final TourAction activeTourAction : allActiveActions) {

         if (activeTourAction.actionCategory != actionCategory) {

            // skip other categories

            continue;
         }

         final String actionClassName = activeTourAction.actionClassName;

         final Object tourAction = allCategoryActions.get(actionClassName);

         if (tourAction instanceof final IActionProvider actionProvider) {

            actionProvider.fillActions(menuMgr);

         } else if (tourAction instanceof final IAction action) {

            menuMgr.add(action);

         } else if (tourAction instanceof final ActionContributionItem action) {

            menuMgr.add(action);
         }
      }
   }

   public static ActionOpenPrefDialog fillContextMenu_CustomizeAction(final IMenuManager menuMgr) {

      menuMgr.add(new Separator());
      menuMgr.add(_actionCustomizeTourActions);

      _actionCustomizeTourActions.setText(isCustomizeActions()

            // Modify Customized Conte&xt Menu...
            ? Messages.Tour_Action_ContextMenu_Modify

            // Customize Conte&xt Menu...
            : Messages.Tour_Action_ContextMenu_Customize);

      return _actionCustomizeTourActions;
   }

   public static List<TourAction> getActiveActions() {

      if (isCustomizeActions()) {

         return getVisibleActions();

      } else {

         return getSortedActions();
      }
   }

   /**
    * Contains all tour action ID's from all views, key is the view ID
    *
    * @return
    */
   public static Map<String, Set<String>> getAllViewActions() {

      return _allViewActions;
   }

   public static List<TourAction> getDefinedActions() {

      return _allDefinedActions;
   }

   /**
    * @return Returns all actions which are defined and sorted
    */
   public static List<TourAction> getSortedActions() {

      if (_allSortedActions == null) {

         _allSortedActions = new ArrayList<>();

         createSortedActions(_allSortedActions);
      }

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

         _isCustomizeActions = Util.getStateBoolean(_state, STATE_IS_CUSTOMIZE_TOUR_ACTIONS, false);
      }

      return _isCustomizeActions;
   }

   public static boolean isShowOnlyAvailableActions() {

      return Util.getStateBoolean(_state, STATE_IS_SHOW_ONLY_AVAILABLE_ACTIONS, false);
   }

   public static void saveActions(final boolean isCustomizeActions,
                                  final boolean isShowOnlyAvailableActions,
                                  final String[] allSortedActions,
                                  final String[] allCheckedActions) {

      _state.put(STATE_IS_CUSTOMIZE_TOUR_ACTIONS, isCustomizeActions);
      _state.put(STATE_IS_SHOW_ONLY_AVAILABLE_ACTIONS, isShowOnlyAvailableActions);
      _state.put(STATE_ALL_SORTED_TOUR_ACTIONS, allSortedActions);
      _state.put(STATE_ALL_VISIBLE_TOUR_ACTIONS, allCheckedActions);

      _isCustomizeActions = isCustomizeActions;

      _allSortedActions.clear();
      _allVisibleActions.clear();

      createSortedActions(_allSortedActions);
      createVisibleActions(_allVisibleActions);
   }

   /**
    * Set all view actions to make the actions more visible which are contained in a view
    *
    * @param contextID
    * @param allViewsActions
    */
   @SafeVarargs
   public static void setAllViewActions(final String contextID,
                                        final Set<String>... allViewsActions) {

      final Set<String> allCollectedActions = new HashSet<>();

      for (final Set<String> allActions : allViewsActions) {
         allCollectedActions.addAll(allActions);
      }

      _allViewActions.put(contextID, allCollectedActions);
   }

}
