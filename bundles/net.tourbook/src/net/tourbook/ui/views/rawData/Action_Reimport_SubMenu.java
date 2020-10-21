/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.rawData;

import java.util.Arrays;

import net.tourbook.Messages;
import net.tourbook.common.util.ITourViewer3;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.importdata.RawDataManager.ReImport;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 */
public class Action_Reimport_SubMenu extends Action implements IMenuCreator {

   private Menu                                     _menu;

   private ActionReimportTours                      _actionReimport_Tours;
   private ActionReimport_EntireTour                _actionReimport_EntireTour;
   private ActionReimport_OnlyTimeSlices            _actionReimport_OnlyTimeSlices;
   private ActionReimport_OnlyTourMarkers           _actionReimport_OnlyTourMarkers;
   private ActionReimport_OnlyTourTimerPauses       _actionReimport_OnlyTourTimerPauses;

   private ActionReimport_OnlyCadenceValues         _actionReimport_OnlyCadenceValues;
   private ActionReimport_OnlyElevationValues       _actionReimport_OnlyElevationValues;
   private ActionReimport_OnlyGearValues            _actionReimport_OnlyGearValues;
   private ActionReimport_OnlyPowerPulseValues      _actionReimport_OnlyPowerPulseValues;
   private ActionReimport_OnlyPowerSpeedValues      _actionReimport_OnlyPowerSpeedValues;
   private ActionReimport_OnlyRunningDynamicsValues _actionReimport_OnlyRunningDynamicsValues;
   private ActionReimport_OnlySwimmingValues        _actionReimport_OnlySwimmingValues;
   private ActionReimport_OnlyTemperatureValues     _actionReimport_OnlyTemperatureValues;
   private ActionReimport_OnlyTrainingValues        _actionReimport_OnlyTrainingValues;

   private ITourViewer3                             _tourViewer;

   private class ActionReimport_EntireTour extends Action {

      public ActionReimport_EntireTour() {
         setText(Messages.Import_Data_Action_Reimport_EntireTour);
      }

      @Override
      public void run() {
         RawDataManager.getInstance().actionReimportTour(Arrays.asList(ReImport.Tour), _tourViewer, false);
      }

   }

   private class ActionReimport_OnlyCadenceValues extends Action {

      public ActionReimport_OnlyCadenceValues() {
         setText(Messages.Import_Data_Action_Reimport_OnlyCadenceValues);
      }

      @Override
      public void run() {
         RawDataManager.getInstance().actionReimportTour(Arrays.asList(ReImport.CadenceValues), _tourViewer, false);
      }
   }

   private class ActionReimport_OnlyElevationValues extends Action {

      public ActionReimport_OnlyElevationValues() {
         setText(Messages.Import_Data_Action_Reimport_OnlyAltitudeValues);
      }

      @Override
      public void run() {
         RawDataManager.getInstance().actionReimportTour(Arrays.asList(ReImport.AltitudeValues), _tourViewer, false);
      }
   }

   private class ActionReimport_OnlyGearValues extends Action {

      public ActionReimport_OnlyGearValues() {
         setText(Messages.Import_Data_Action_Reimport_OnlyGearValues);
      }

      @Override
      public void run() {
         RawDataManager.getInstance().actionReimportTour(Arrays.asList(ReImport.GearValues), _tourViewer, false);
      }
   }

   private class ActionReimport_OnlyPowerPulseValues extends Action {

      public ActionReimport_OnlyPowerPulseValues() {
         setText(Messages.Import_Data_Action_Reimport_OnlyPowerAndPulseValues);
      }

      @Override
      public void run() {
         RawDataManager.getInstance()
               .actionReimportTour(
                     Arrays.asList(ReImport.PowerAndPulseValues),
                     _tourViewer,
                     false);
      }
   }

   private class ActionReimport_OnlyPowerSpeedValues extends Action {

      public ActionReimport_OnlyPowerSpeedValues() {
         setText(Messages.Import_Data_Action_Reimport_OnlyPowerAndSpeedValues);
      }

      @Override
      public void run() {
         RawDataManager.getInstance()
               .actionReimportTour(
                     Arrays.asList(ReImport.PowerAndSpeedValues),
                     _tourViewer,
                     false);
      }
   }

   private class ActionReimport_OnlyRunningDynamicsValues extends Action {

      public ActionReimport_OnlyRunningDynamicsValues() {
         setText(Messages.Import_Data_Action_Reimport_OnlyRunningDynamicsValues);
      }

      @Override
      public void run() {
         RawDataManager.getInstance().actionReimportTour(Arrays.asList(ReImport.RunningDynamics), _tourViewer, false);
      }
   }

   private class ActionReimport_OnlySwimmingValues extends Action {

      public ActionReimport_OnlySwimmingValues() {
         setText(Messages.Import_Data_Action_Reimport_OnlySwimmingValues);
      }

      @Override
      public void run() {
         RawDataManager.getInstance().actionReimportTour(Arrays.asList(ReImport.Swimming), _tourViewer, false);
      }
   }

   private class ActionReimport_OnlyTemperatureValues extends Action {

      public ActionReimport_OnlyTemperatureValues() {
         setText(Messages.Import_Data_Action_Reimport_OnlyTemperatureValues);
      }

      @Override
      public void run() {
         RawDataManager.getInstance().actionReimportTour(Arrays.asList(ReImport.TemperatureValues), _tourViewer, false);
      }
   }

   private class ActionReimport_OnlyTimeSlices extends Action {

      public ActionReimport_OnlyTimeSlices() {
         setText(Messages.Import_Data_Action_Reimport_OnlyTimeSlices);
      }

      @Override
      public void run() {
         RawDataManager.getInstance().actionReimportTour(Arrays.asList(ReImport.TimeSlices), _tourViewer, false);
      }
   }

   private class ActionReimport_OnlyTourMarkers extends Action {

      public ActionReimport_OnlyTourMarkers() {
         setText(Messages.Import_Data_Action_Reimport_OnlyTourMarkers);
      }

      @Override
      public void run() {
         RawDataManager.getInstance().actionReimportTour(Arrays.asList(ReImport.TourMarkers), _tourViewer, false);
      }
   }

   private class ActionReimport_OnlyTourTimerPauses extends Action {

      public ActionReimport_OnlyTourTimerPauses() {
         setText(Messages.Import_Data_Action_Reimport_OnlyTourTimerPauses);
      }

      @Override
      public void run() {
         RawDataManager.getInstance().actionReimportTour(Arrays.asList(ReImport.TourTimerPauses), _tourViewer, false);
      }
   }

   private class ActionReimport_OnlyTrainingValues extends Action {

      public ActionReimport_OnlyTrainingValues() {
         setText(Messages.Import_Data_Action_Reimport_OnlyTrainingValues);
      }

      @Override
      public void run() {
         RawDataManager.getInstance().actionReimportTour(Arrays.asList(ReImport.TrainingValues), _tourViewer, false);
      }
   }

   private class ActionReimportTours extends Action {

      private final ITourViewer3 _tourViewer;

      public ActionReimportTours(final ITourViewer3 tourViewer) {

         _tourViewer = tourViewer;

         setText(Messages.dialog_reimport_tours_shell_text);
      }

      @Override
      public void run() {
         new DialogReimportTours(Display.getCurrent().getActiveShell(), _tourViewer).open();
      }
   }

   public Action_Reimport_SubMenu(final ITourViewer3 tourViewer) {

      super(Messages.Import_Data_Action_Reimport_Tour, AS_DROP_DOWN_MENU);

      setMenuCreator(this);

      _tourViewer = tourViewer;

      _actionReimport_Tours = new ActionReimportTours(_tourViewer);
      _actionReimport_EntireTour = new ActionReimport_EntireTour();
      _actionReimport_OnlyTimeSlices = new ActionReimport_OnlyTimeSlices();
      _actionReimport_OnlyTourMarkers = new ActionReimport_OnlyTourMarkers();
      _actionReimport_OnlyTourTimerPauses = new ActionReimport_OnlyTourTimerPauses();

      _actionReimport_OnlyCadenceValues = new ActionReimport_OnlyCadenceValues();
      _actionReimport_OnlyElevationValues = new ActionReimport_OnlyElevationValues();
      _actionReimport_OnlyGearValues = new ActionReimport_OnlyGearValues();
      _actionReimport_OnlyPowerPulseValues = new ActionReimport_OnlyPowerPulseValues();
      _actionReimport_OnlyPowerSpeedValues = new ActionReimport_OnlyPowerSpeedValues();
      _actionReimport_OnlyRunningDynamicsValues = new ActionReimport_OnlyRunningDynamicsValues();
      _actionReimport_OnlySwimmingValues = new ActionReimport_OnlySwimmingValues();
      _actionReimport_OnlyTemperatureValues = new ActionReimport_OnlyTemperatureValues();
      _actionReimport_OnlyTrainingValues = new ActionReimport_OnlyTrainingValues();
   }

   @Override
   public void dispose() {

      if (_menu != null) {
         _menu.dispose();
         _menu = null;
      }
   }

   private void fillMenu(final Menu menu) {

      new ActionContributionItem(_actionReimport_Tours).fill(menu, -1);

      new ActionContributionItem(_actionReimport_OnlyCadenceValues).fill(menu, -1);
      new ActionContributionItem(_actionReimport_OnlyElevationValues).fill(menu, -1);
      new ActionContributionItem(_actionReimport_OnlyGearValues).fill(menu, -1);
      new ActionContributionItem(_actionReimport_OnlyPowerPulseValues).fill(menu, -1);
      new ActionContributionItem(_actionReimport_OnlyPowerSpeedValues).fill(menu, -1);
      new ActionContributionItem(_actionReimport_OnlyRunningDynamicsValues).fill(menu, -1);
      new ActionContributionItem(_actionReimport_OnlySwimmingValues).fill(menu, -1);

      new ActionContributionItem(_actionReimport_OnlyTemperatureValues).fill(menu, -1);
      new ActionContributionItem(_actionReimport_OnlyTourMarkers).fill(menu, -1);
      new ActionContributionItem(_actionReimport_OnlyTourTimerPauses).fill(menu, -1);
      new ActionContributionItem(_actionReimport_OnlyTrainingValues).fill(menu, -1);

      new ActionContributionItem(_actionReimport_OnlyTimeSlices).fill(menu, -1);
      new ActionContributionItem(_actionReimport_EntireTour).fill(menu, -1);
   }

   @Override
   public Menu getMenu(final Control parent) {
      return null;
   }

   @Override
   public Menu getMenu(final Menu parent) {

      dispose();

      _menu = new Menu(parent);

      // Add listener to re-populate the menu each time
      _menu.addMenuListener(new MenuAdapter() {
         @Override
         public void menuShown(final MenuEvent e) {

            // dispose old menu items
            for (final MenuItem menuItem : ((Menu) e.widget).getItems()) {
               menuItem.dispose();
            }

            fillMenu(_menu);
         }
      });

      return _menu;
   }

}
