/*******************************************************************************
 * Copyright (C) 2023 Frédéric Bard
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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.tourbook.Messages;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

public class SubMenu_SetPausesType extends SubMenu {

   private ITourProvider       _tourProvider;
   private int[]               _tourPausesViewSelectedIndices;
   private ActionSetPausesType _actionSetAutomaticPauseType;
   private ActionSetPausesType _actionSetManualPauseType;

   private boolean             _changeAllTourPauses;

   private class ActionSetPausesType extends Action {

      boolean _isSetAutoPause;

      public ActionSetPausesType(final String text, final boolean isSetAutoPause) {

         super(text, AS_CHECK_BOX);

         _isSetAutoPause = isSetAutoPause;
      }

      @Override
      public void run() {
         setPausesType(_isSetAutoPause);
      }
   }

   public SubMenu_SetPausesType(final ITourProvider tourProvider, final boolean changeAllTourPauses) {

      super(Messages.Action_PauseType_Set, AS_DROP_DOWN_MENU);

      _tourProvider = tourProvider;

      _changeAllTourPauses = changeAllTourPauses;

      _actionSetAutomaticPauseType = new ActionSetPausesType(Messages.Action_PauseType_Set_Automatic, true);
      _actionSetManualPauseType = new ActionSetPausesType(Messages.Action_PauseType_Set_Manual, false);
   }

   @Override
   public void enableActions() {

      final ArrayList<TourData> selectedTours = _tourProvider.getSelectedTours();
      if (selectedTours.size() > 1) {

         _actionSetAutomaticPauseType.setChecked(false);
         _actionSetManualPauseType.setChecked(false);

      } else if (selectedTours.size() == 1) {

         final TourData tourData = selectedTours.get(0);
         final long[] pausedTime_Data = tourData.getPausedTime_Data();
         if (pausedTime_Data == null) {
            _actionSetAutomaticPauseType.setChecked(true);
            _actionSetManualPauseType.setChecked(false);
            return;
         }

         // Pause type change request is coming from the Tour Pauses view
         if (!_changeAllTourPauses &&
               _tourPausesViewSelectedIndices.length > 0) {

            final boolean isPauseTypeAutomatic = pausedTime_Data[_tourPausesViewSelectedIndices[0]] == 1;
            _actionSetAutomaticPauseType.setChecked(isPauseTypeAutomatic);
            _actionSetManualPauseType.setChecked(!isPauseTypeAutomatic);

         } else {
            // Pause type change request is coming from the Tour Book view

            final Set<Long> distinct = Arrays.stream(pausedTime_Data).boxed().collect(Collectors.toSet());

            final boolean allAutoPauses = distinct.size() == 1 && distinct.iterator().next() == 1;
            _actionSetAutomaticPauseType.setChecked(allAutoPauses);

            final boolean allManualPauses = distinct.size() == 1 && distinct.iterator().next() == 0;
            _actionSetManualPauseType.setChecked(allManualPauses);
         }
      }
   }

   @Override
   public void fillMenu(final Menu menu) {

      new ActionContributionItem(_actionSetAutomaticPauseType).fill(menu, -1);
      new ActionContributionItem(_actionSetManualPauseType).fill(menu, -1);
   }

   private long[] getPausedTime_Data(final TourData tourData) {

      long[] pausedTime_Data = tourData.getPausedTime_Data();

      if (pausedTime_Data == null) {

         final long[] pausedTime_Start = tourData.getPausedTime_Start();

         pausedTime_Data = new long[pausedTime_Start.length];
         Arrays.setAll(pausedTime_Data, value -> 1);
      }
      return pausedTime_Data;
   }

   public void setPausesType(final boolean isSetAutoPause) {

      // check if the tour editor contains a modified tour
      if (TourManager.isTourEditorModified()) {
         return;
      }

      final List<TourData> selectedTours = _tourProvider.getSelectedTours();

      if (selectedTours == null || selectedTours.isEmpty()) {

         // a tour is not selected
         MessageDialog.openInformation(
               Display.getCurrent().getActiveShell(),
               Messages.Dialog_SetWeatherDescription_Dialog_Title,
               Messages.UI_Label_TourIsNotSelected);

         return;
      }

      final List<TourData> selectedToursWithPauses = new ArrayList<>();
      for (final TourData tourData : selectedTours) {

         if (tourData.getTourDeviceTime_Paused() > 0) {
            selectedToursWithPauses.add(tourData);
         }
      }

      final ArrayList<TourData> modifiedTours = setToursPausesType(isSetAutoPause, selectedToursWithPauses);

      if (modifiedTours.size() > 0) {
         TourManager.saveModifiedTours(modifiedTours);
      }

   }

   public void setTourPauses(final int[] tourPausesViewSelectedIndices) {

      _tourPausesViewSelectedIndices = tourPausesViewSelectedIndices;
   }

   private ArrayList<TourData> setToursPausesType(final boolean isSetAutoPause,
                                                  final List<TourData> selectedToursWithPauses) {

      final ArrayList<TourData> modifiedTours = new ArrayList<>();
      for (final TourData tourData : selectedToursWithPauses) {

         final long[] pausedTime_Data = getPausedTime_Data(tourData);

         if (_changeAllTourPauses) {

            Arrays.setAll(pausedTime_Data, value -> isSetAutoPause ? 1 : 0);

         } else {

            for (int index = 0; index < _tourPausesViewSelectedIndices.length; index++) {

               pausedTime_Data[_tourPausesViewSelectedIndices[index]] = isSetAutoPause ? 1 : 0;
            }
         }

         tourData.setPausedTime_Data(pausedTime_Data);

         modifiedTours.add(tourData);
      }

      return modifiedTours;
   }
}
