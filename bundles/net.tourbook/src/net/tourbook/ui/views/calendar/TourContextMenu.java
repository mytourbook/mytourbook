/*******************************************************************************
 * Copyright (C) 2011, 2021 Matthias Helmling and Contributors
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

import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.extension.export.ActionExport;
import net.tourbook.extension.upload.ActionUpload;
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
import net.tourbook.ui.action.ActionJoinTours;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.action.ActionSetElevationValuesFromSRTM;
import net.tourbook.ui.action.ActionSetPerson;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
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

   private TagMenuManager                             _tagMenuMgr;

   private ActionComputeDistanceValuesFromGeoposition _actionComputeDistanceValuesFromGeoposition;
   private ActionDuplicateTour                        _actionDuplicateTour;
   private ActionEditQuick                            _actionEditQuick;
   private ActionEditTour                             _actionEditTour;
   private ActionExport                               _actionExportTour;
   private ActionJoinTours                            _actionJoinTours;
   private ActionOpenTour                             _actionOpenTour;
   private ActionOpenAdjustAltitudeDialog             _actionOpenAdjustAltitudeDialog;
   private ActionOpenMarkerDialog                     _actionOpenMarkerDialog;
   private ActionMergeTour                            _actionMergeTour;
   private ActionPrint                                _actionPrintTour;
   private ActionSetElevationValuesFromSRTM           _actionSetElevationFromSRTM;
   private ActionSetPerson                            _actionSetOtherPerson;
   private ActionSetTourTypeMenu                      _actionSetTourType;
   private ActionUpload                               _actionUploadTour;

   public TourContextMenu() {}

   private void createActions(final ITourProvider tourProvider) {

      _actionDuplicateTour = new ActionDuplicateTour(tourProvider);
      _actionEditQuick = new ActionEditQuick(tourProvider);
      _actionEditTour = new ActionEditTour(tourProvider);
      _actionOpenTour = new ActionOpenTour(tourProvider);
      // _actionDeleteTour = new ActionDeleteTourMenu(tourProvider);

      _actionOpenMarkerDialog = new ActionOpenMarkerDialog(tourProvider, true);
      _actionOpenAdjustAltitudeDialog = new ActionOpenAdjustAltitudeDialog(tourProvider);
      _actionMergeTour = new ActionMergeTour(tourProvider);
      _actionJoinTours = new ActionJoinTours(tourProvider);
      _actionComputeDistanceValuesFromGeoposition = new ActionComputeDistanceValuesFromGeoposition(tourProvider);
      _actionSetElevationFromSRTM = new ActionSetElevationValuesFromSRTM(tourProvider);
      _actionSetOtherPerson = new ActionSetPerson(tourProvider);

      _actionSetTourType = new ActionSetTourTypeMenu(tourProvider);

      _actionExportTour = new ActionExport(tourProvider);
      _actionPrintTour = new ActionPrint(tourProvider);
      _actionUploadTour = new ActionUpload(tourProvider);

      _tagMenuMgr = new TagMenuManager(tourProvider, true);

   }

   public Menu createContextMenu(final CalendarView calendarView,
                                 final Control control,
                                 final ArrayList<Action> localActions) {

      createActions(calendarView);

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

      menuMgr.add(_actionEditQuick);
      menuMgr.add(_actionEditTour);
      menuMgr.add(_actionOpenMarkerDialog);
      menuMgr.add(_actionOpenAdjustAltitudeDialog);
      menuMgr.add(_actionOpenTour);
      menuMgr.add(_actionDuplicateTour);
      menuMgr.add(_actionMergeTour);
      // menuMgr.add(_actionJoinTours); // until now we only allow single tour selection
      menuMgr.add(_actionComputeDistanceValuesFromGeoposition);
      menuMgr.add(_actionSetElevationFromSRTM);

      tagMenuMgr.fillTagMenu(menuMgr, true);

      // tour type actions
      menuMgr.add(new Separator());
      menuMgr.add(_actionSetTourType);
      TourTypeMenuManager.fillMenuWithRecentTourTypes(menuMgr, calendarView, true);

      menuMgr.add(new Separator());
      menuMgr.add(_actionUploadTour);
      menuMgr.add(_actionExportTour);
      menuMgr.add(_actionPrintTour);

      menuMgr.add(new Separator());
      menuMgr.add(_actionSetOtherPerson);

      return contextMenu;

   }

   private void enableActions(final ITourProvider tourProvider) {

      /*
       * count number of selected items
       */
      final ArrayList<TourData> selectedTours = tourProvider.getSelectedTours();

      final int tourItems = selectedTours.size();
      final boolean isTourSelected = tourItems > 0;
      final boolean isOneTour = tourItems == 1;
      boolean isDeviceTour = false;

      TourData firstSavedTour = null;

      if (isOneTour) {
         firstSavedTour = TourManager.getInstance().getTourData(selectedTours.get(0).getTourId());
         isDeviceTour = firstSavedTour.isManualTour() == false;
      }

      /*
       * enable actions
       */
      // _tourDoubleClickState.canEditTour = isOneTour;
      // _tourDoubleClickState.canOpenTour = isOneTour;
      // _tourDoubleClickState.canQuickEditTour = isOneTour;
      // _tourDoubleClickState.canEditMarker = isOneTour;
      // _tourDoubleClickState.canAdjustAltitude = isOneTour;

      _actionDuplicateTour.setEnabled(isOneTour && !isDeviceTour);
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

      // enable delete ation when at least one tour is selected
//		if (isTourSelected) {
//			_actionDeleteTour.setEnabled(true);
//		} else {
//			_actionDeleteTour.setEnabled(false);
//		}

      _actionJoinTours.setEnabled(tourItems > 1);
      _actionSetOtherPerson.setEnabled(isTourSelected);

      _actionExportTour.setEnabled(isTourSelected);
      _actionPrintTour.setEnabled(isTourSelected);
      _actionUploadTour.setEnabled(isTourSelected);

      final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();
      _actionSetTourType.setEnabled(isTourSelected && tourTypes.size() > 0);

      Long tourTypeId = Long.valueOf(-1); // TODO -> NOTOUR
      if (null != firstSavedTour) {
         final ArrayList<Long> tagIds = new ArrayList<>();
         for (final TourTag tag : firstSavedTour.getTourTags()) {
            tagIds.add(tag.getTagId());
         }
         _tagMenuMgr.enableTagActions(isTourSelected, isOneTour, tagIds);
         if (isOneTour && null != firstSavedTour.getTourType()) {
            tourTypeId = firstSavedTour.getTourType().getTypeId();
         }
         TourTypeMenuManager.enableRecentTourTypeActions(isTourSelected, tourTypeId);
      } else {
         _tagMenuMgr.enableTagActions(isTourSelected, isOneTour, new ArrayList<>());
         TourTypeMenuManager.enableRecentTourTypeActions(isTourSelected, tourTypeId);
      }
   }

   private void fillContextMenu(final IMenuManager menuMgr,
                                final ITourProvider tourProvider,
                                final ArrayList<Action> localActions) {

      // if a local menu exists and no tour is selected show only the local menu
      final ArrayList<TourData> selectedTours = tourProvider.getSelectedTours();
      if (null != localActions && selectedTours.isEmpty()) {
         for (final Action action : localActions) {
            menuMgr.add(action);
         }
         return;
      }

      menuMgr.add(_actionEditQuick);
      menuMgr.add(_actionEditTour);
      menuMgr.add(_actionOpenMarkerDialog);
      menuMgr.add(_actionOpenAdjustAltitudeDialog);
      menuMgr.add(_actionOpenTour);
      menuMgr.add(_actionDuplicateTour);
      menuMgr.add(_actionMergeTour);
      menuMgr.add(_actionJoinTours);
      menuMgr.add(_actionComputeDistanceValuesFromGeoposition);
      menuMgr.add(_actionSetElevationFromSRTM);

      _tagMenuMgr.fillTagMenu(menuMgr, true);

      // tour type actions
      menuMgr.add(new Separator());
      menuMgr.add(_actionSetTourType);
      TourTypeMenuManager.fillMenuWithRecentTourTypes(menuMgr, tourProvider, true);

      menuMgr.add(new Separator());
      menuMgr.add(_actionUploadTour);
      menuMgr.add(_actionExportTour);
      menuMgr.add(_actionPrintTour);

      menuMgr.add(new Separator());
      menuMgr.add(_actionSetOtherPerson);
//		menuMgr.add(_actionDeleteTour);

      enableActions(tourProvider);
   }

}
