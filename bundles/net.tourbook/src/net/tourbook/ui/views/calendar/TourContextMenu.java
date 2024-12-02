/*******************************************************************************
 * Copyright (C) 2011, 2024 Matthias Helmling and Contributors
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
package net.tourbook.ui.views.calendar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.extension.export.ActionExport;
import net.tourbook.extension.upload.ActionUpload;
import net.tourbook.preferences.ViewContext;
import net.tourbook.tag.TagMenuManager;
import net.tourbook.tour.ActionOpenAdjustAltitudeDialog;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourTypeMenuManager;
import net.tourbook.tour.printing.ActionPrint;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.action.ActionComputeDistanceValuesFromGeoposition;
import net.tourbook.ui.action.ActionDuplicateTour;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.action.ActionSetElevationValuesFromSRTM;
import net.tourbook.ui.action.ActionSetPerson;
import net.tourbook.ui.action.TourActionCategory;
import net.tourbook.ui.action.TourActionManager;
import net.tourbook.ui.views.ViewNames;
import net.tourbook.ui.views.rawData.ActionMergeTour;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

public class TourContextMenu {

   private String                                     CONTEXT_ID;

   private TagMenuManager                             _tagMenuManager;
   private TourTypeMenuManager                        _tourTypeMenuManager;

   private HashMap<String, Object>                    _allTourActions_Adjust;
   private HashMap<String, Object>                    _allTourActions_Edit;
   private HashMap<String, Object>                    _allTourActions_Export;

   private ActionComputeDistanceValuesFromGeoposition _actionComputeDistanceValuesFromGeoposition;
   private ActionDuplicateTour                        _actionDuplicateTour;
   private ActionEditQuick                            _actionEditQuick;
   private ActionEditTour                             _actionEditTour;
   private ActionExport                               _actionExportTour;
//   private ActionJoinTours                            _actionJoinTours;
   private ActionOpenTour                             _actionOpenTour;
   private ActionOpenAdjustAltitudeDialog             _actionOpenAdjustAltitudeDialog;
   private ActionOpenMarkerDialog                     _actionOpenMarkerDialog;
   private ActionMergeTour                            _actionMergeTour;
   private ActionPrint                                _actionPrintTour;
   private ActionSetElevationValuesFromSRTM           _actionSetElevationFromSRTM;
   private ActionSetPerson                            _actionSetOtherPerson;
   private ActionUpload                               _actionUploadTour;

   public TourContextMenu() {}

   private void createActions(final ITourProvider tourProvider, final String contextID) {

// SET_FORMATTING_OFF

      _tagMenuManager                  = new TagMenuManager(tourProvider, true);
      _tourTypeMenuManager             = new TourTypeMenuManager(tourProvider);

      _actionDuplicateTour             = new ActionDuplicateTour(tourProvider);
      _actionEditQuick                 = new ActionEditQuick(tourProvider);
      _actionEditTour                  = new ActionEditTour(tourProvider);
      _actionOpenTour                  = new ActionOpenTour(tourProvider);

      _actionOpenMarkerDialog          = new ActionOpenMarkerDialog(tourProvider, true);
      _actionOpenAdjustAltitudeDialog  = new ActionOpenAdjustAltitudeDialog(tourProvider);
      _actionMergeTour                 = new ActionMergeTour(tourProvider);
      _actionComputeDistanceValuesFromGeoposition = new ActionComputeDistanceValuesFromGeoposition(tourProvider);
      _actionSetElevationFromSRTM      = new ActionSetElevationValuesFromSRTM(tourProvider);
      _actionSetOtherPerson            = new ActionSetPerson(tourProvider);

      _actionExportTour                = new ActionExport(tourProvider);
      _actionPrintTour                 = new ActionPrint(tourProvider);
      _actionUploadTour                = new ActionUpload(tourProvider);

// SET_FORMATTING_ON

// SET_FORMATTING_OFF

      _allTourActions_Adjust  = new HashMap<>();
      _allTourActions_Edit    = new HashMap<>();
      _allTourActions_Export  = new HashMap<>();

      _allTourActions_Edit.put(_actionEditQuick                   .getClass().getName(),  _actionEditQuick);
      _allTourActions_Edit.put(_actionEditTour                    .getClass().getName(),  _actionEditTour);
      _allTourActions_Edit.put(_actionOpenMarkerDialog            .getClass().getName(),  _actionOpenMarkerDialog);
      _allTourActions_Edit.put(_actionOpenAdjustAltitudeDialog    .getClass().getName(),  _actionOpenAdjustAltitudeDialog);
//    _allTourActions_Edit.put(_actionSetStartEndLocation         .getClass().getName(),  _actionSetStartEndLocation);
      _allTourActions_Edit.put(_actionOpenTour                    .getClass().getName(),  _actionOpenTour);
      _allTourActions_Edit.put(_actionDuplicateTour               .getClass().getName(),  _actionDuplicateTour);
//    _allTourActions_Edit.put(_actionCreateTourMarkers           .getClass().getName(),  _actionCreateTourMarkers);
      _allTourActions_Edit.put(_actionMergeTour                   .getClass().getName(),  _actionMergeTour);

      _allTourActions_Export.put(_actionUploadTour                .getClass().getName(),  _actionUploadTour);
      _allTourActions_Export.put(_actionExportTour                .getClass().getName(),  _actionExportTour);
//    _allTourActions_Export.put(_actionExportViewCSV             .getClass().getName(),  _actionExportViewCSV);
      _allTourActions_Export.put(_actionPrintTour                 .getClass().getName(),  _actionPrintTour);

//    _allTourActions_Adjust.put(_actionAdjustTourValues          .getClass().getName(),  _actionAdjustTourValues);
//    _allTourActions_Adjust.put(_actionDeleteTourValues          .getClass().getName(),  _actionDeleteTourValues);
//    _allTourActions_Adjust.put(_actionReimport_Tours            .getClass().getName(),  _actionReimport_Tours);
      _allTourActions_Adjust.put(_actionSetOtherPerson            .getClass().getName(),  _actionSetOtherPerson);
//    _allTourActions_Adjust.put(_actionDeleteTourMenu            .getClass().getName(),  _actionDeleteTourMenu);

// SET_FORMATTING_ON

      TourActionManager.setAllViewActions(contextID,
            _allTourActions_Edit.keySet(),
            _allTourActions_Export.keySet(),
            _allTourActions_Adjust.keySet(),
            _tagMenuManager.getAllTagActions().keySet(),
            _tourTypeMenuManager.getAllTourTypeActions().keySet());

   }

   public Menu createContextMenu(final CalendarView calendarView,
                                 final Control control,
                                 final List<Action> localActions) {

      CONTEXT_ID = CalendarView.ID;

      createActions(calendarView, CONTEXT_ID);

      // final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
      final MenuManager menuMgr = new MenuManager();
      final TagMenuManager tagMenuMgr = new TagMenuManager(calendarView, true);

      menuMgr.setRemoveAllWhenShown(true);
      menuMgr.addMenuListener(menuManager -> {

         // hide tour tooltip when opened
         calendarView.getTourInfoTooltip().hideToolTip();

         fillContextMenu(menuManager, calendarView, localActions);
      });

      final Menu contextMenu = menuMgr.createContextMenu(control);

      contextMenu.addMenuListener(new MenuAdapter() {
         @Override
         public void menuHidden(final MenuEvent e) {
            tagMenuMgr.onHideMenu();
         }

         @Override
         public void menuShown(final MenuEvent menuEvent) {
            // tagMenuMgr.onShowMenu(menuEvent, _control, Display.getCurrent().getCursorLocation(), _tourInfoToolTip);
            tagMenuMgr.onShowMenu(menuEvent, control, Display.getCurrent().getCursorLocation(), null);
         }
      });

      return contextMenu;
   }

   private void enableActions(final ITourProvider tourProvider) {

      /*
       * Count number of selected items
       */
      final ArrayList<TourData> selectedTours = tourProvider.getSelectedTours();

      final int numTourItems = selectedTours.size();
      final boolean isTourSelected = numTourItems > 0;
      final boolean isOneTour = numTourItems == 1;
      boolean isDeviceTour = false;

      TourData firstSavedTour = null;

      if (isOneTour) {
         firstSavedTour = TourManager.getInstance().getTourData(selectedTours.get(0).getTourId());
         isDeviceTour = firstSavedTour.isManualTour() == false;
      }

      /*
       * Enable actions
       */
      _actionDuplicateTour.setEnabled(isOneTour);
      _actionEditTour.setEnabled(isOneTour);
      _actionEditQuick.setEnabled(isOneTour);
      _actionOpenAdjustAltitudeDialog.setEnabled(isOneTour && isDeviceTour);
      _actionOpenMarkerDialog.setEnabled(isOneTour && isDeviceTour);
      _actionOpenTour.setEnabled(isOneTour);

      _actionMergeTour.setEnabled(
            isOneTour
                  && isDeviceTour
                  && firstSavedTour != null
                  && firstSavedTour.getMergeSourceTourId() != null);
      _actionComputeDistanceValuesFromGeoposition.setEnabled(isTourSelected);
      _actionSetElevationFromSRTM.setEnabled(isTourSelected);

      _actionSetOtherPerson.setEnabled(isTourSelected);

      _actionExportTour.setEnabled(isTourSelected);
      _actionPrintTour.setEnabled(isTourSelected);
      _actionUploadTour.setEnabled(isTourSelected);

      Long tourTypeId = Long.valueOf(-1); // TODO -> NOTOUR
      if (null != firstSavedTour) {

         final ArrayList<Long> tagIds = new ArrayList<>();
         for (final TourTag tag : firstSavedTour.getTourTags()) {
            tagIds.add(tag.getTagId());
         }

         _tagMenuManager.enableTagActions(isTourSelected, isOneTour, tagIds);

         if (isOneTour && null != firstSavedTour.getTourType()) {
            tourTypeId = firstSavedTour.getTourType().getTypeId();
         }

         _tourTypeMenuManager.enableTourTypeActions(isTourSelected, tourTypeId);

      } else {

         _tagMenuManager.enableTagActions(isTourSelected, isOneTour, new ArrayList<>());
         _tourTypeMenuManager.enableTourTypeActions(isTourSelected, tourTypeId);
      }
   }

   private void fillContextMenu(final IMenuManager menuMgr,
                                final ITourProvider tourProvider,
                                final List<Action> localActions) {

      // if a local menu exists and no tour is selected show only the local menu
      final ArrayList<TourData> allSelectedTours = tourProvider.getSelectedTours();
      if (localActions != null && allSelectedTours.isEmpty()) {
         for (final Action action : localActions) {
            menuMgr.add(action);
         }

         return;
      }

      // edit actions
      TourActionManager.fillContextMenu(menuMgr, TourActionCategory.EDIT, _allTourActions_Edit);

      // tag actions
      _tagMenuManager.fillTagMenu_WithActiveActions(menuMgr);

      // tour type actions
      _tourTypeMenuManager.fillContextMenu_WithActiveActions(menuMgr);

      menuMgr.add(new Separator());
      menuMgr.add(_actionComputeDistanceValuesFromGeoposition);
      menuMgr.add(_actionSetElevationFromSRTM);

      // export actions
      TourActionManager.fillContextMenu(menuMgr, TourActionCategory.EXPORT, _allTourActions_Export);

      // adjust actions
      TourActionManager.fillContextMenu(menuMgr, TourActionCategory.ADJUST, _allTourActions_Adjust);

      // customize this context menu
      TourActionManager.fillContextMenu_CustomizeAction(menuMgr)

            // set pref page custom data that actions from this view can be identified
            .setPrefData(new ViewContext(CONTEXT_ID, ViewNames.VIEW_NAME_CALENDAR));

      enableActions(tourProvider);
   }
}
