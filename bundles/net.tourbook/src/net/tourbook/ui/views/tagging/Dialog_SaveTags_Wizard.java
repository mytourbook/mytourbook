/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tagging;

import java.lang.reflect.InvocationTargetException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourLogState;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

public class Dialog_SaveTags_Wizard extends Wizard {

   private static final String LOG_SAVE_TAGS_START_APPEND_TAGS        = Messages.Log_SaveTags_Start_AppendTags;
   private static final String LOG_SAVE_TAGS_START_REPLACE_TAGS       = Messages.Log_SaveTags_Start_ReplaceTags;
   private static final String LOG_SAVE_TAGS_START_REMOVE_ALL_TAGS    = Messages.Log_SaveTags_Start_RemoveAllTags;
   private static final String LOG_SAVE_TAGS_END                      = Messages.Log_SaveTags_End;

   private static final String LOG_SAVE_TAGS_PROGRESS_APPEND_TAGS     = Messages.Log_SaveTags_Progress_AppendTags;
   private static final String LOG_SAVE_TAGS_PROGRESS_REPLACE_TAGS    = Messages.Log_SaveTags_Progress_ReplaceTags;
   private static final String LOG_SAVE_TAGS_PROGRESS_REMOVE_ALL_TAGS = Messages.Log_SaveTags_Progress_RemoveAllTags;
   //

   private final IPreferenceStore     _prefStore = TourbookPlugin.getPrefStore();

   private Dialog_SaveTags_WizardPage _wizardPage;

   private ArrayList<TourData>        _selectedTours;
//   private ITourProvider2               _tourProvider;
   private HashSet<Long>              _allCheckedTagIds;

   public Dialog_SaveTags_Wizard(final ArrayList<TourData> selectedTours, final HashSet<Long> allCheckedTagIds) {

      super();

      _allCheckedTagIds = allCheckedTagIds;

      setNeedsProgressMonitor(true);

      _selectedTours = selectedTours;
//      _tourProvider = tourProvider;
   }

   @Override
   public void addPages() {

      _wizardPage = new Dialog_SaveTags_WizardPage(_allCheckedTagIds);

      addPage(_wizardPage);
   }

   @Override
   public String getWindowTitle() {
      return Messages.Dialog_SaveTags_Dialog_Title;
   }

   @Override
   public boolean performFinish() {

      final long start = System.currentTimeMillis();

      TourLogManager.showLogView();

      try {

         getContainer().run(true, true, performFinish_getRunnable());

      } catch (InvocationTargetException | InterruptedException e) {
         StatusUtil.log(e);
      }

      TourLogManager.logDefault(String.format(//
            LOG_SAVE_TAGS_END,
            (System.currentTimeMillis() - start) / 1000.0));

      return true;
   }

   private IRunnableWithProgress performFinish_getRunnable() {

      _wizardPage.saveState();

      final int timeZoneAction = _prefStore.getInt(ITourbookPreferences.DIALOG_SET_TIME_ZONE_ACTION);
      final String timeZoneId = _prefStore.getString(ITourbookPreferences.DIALOG_SET_TIME_ZONE_SELECTED_ZONE_ID);
      final ZoneId selectedzoneId = ZoneId.of(timeZoneId);

      /*
       * Create start log message
       */
      String startLogMessage = null;

      switch (timeZoneAction) {
      case Dialog_SaveTags.SAVE_TAG_ACTION_REPLACE_TAGS:

         startLogMessage = NLS.bind(LOG_SAVE_TAGS_START_REPLACE_TAGS, _selectedTours.size());
         break;

      case Dialog_SaveTags.SAVE_TAG_ACTION_APPEND_NEW_TAGS:

         startLogMessage = NLS.bind(LOG_SAVE_TAGS_START_APPEND_TAGS, timeZoneId, _selectedTours.size());
         break;

      case Dialog_SaveTags.SAVE_TAG_ACTION_REMOVE_ALL_TAGS:

         startLogMessage = NLS.bind(LOG_SAVE_TAGS_START_REMOVE_ALL_TAGS, _selectedTours.size());
         break;
      }

      TourLogManager.addLog(TourLogState.DEFAULT, startLogMessage);

      final IRunnableWithProgress runnable = new IRunnableWithProgress() {

         @Override
         public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

            monitor.beginTask(Messages.Dialog_SetTimeZone_Label_Progress_Task, _selectedTours.size());

            // sort tours by date
            Collections.sort(_selectedTours);

            int workedTours = 0;
            final ArrayList<TourData> savedTours = new ArrayList<>();

            for (final TourData tourData : _selectedTours) {

               if (monitor.isCanceled()) {
                  break;
               }

               monitor.worked(1);
               monitor.subTask(NLS.bind(
                     Messages.Dialog_SetTimeZone_Label_Progress_SubTask,
                     ++workedTours,
                     _selectedTours.size()));

               final String tourDateTime = TourManager.getTourDateTimeShort(tourData);

               switch (timeZoneAction) {

               case Dialog_SaveTags.SAVE_TAG_ACTION_APPEND_NEW_TAGS:

                  // set time zone which is selected in a list

//                  tourData.setTimeZoneId(selectedzoneId.getId());
//
//                  TourLogManager.addLog(
//                        TourLogState.DEFAULT,
//                        NLS.bind(LOG_SAVE_TAGS_PROGRESS_APPEND_TAGS, tourDateTime));

                  break;

               case Dialog_SaveTags.SAVE_TAG_ACTION_REPLACE_TAGS:

                  // set time zone from the tour geo position

//                  if (tourData.latitudeSerie != null) {
//
//                     // get time zone from lat/lon
//                     final double lat = tourData.latitudeSerie[0];
//                     final double lon = tourData.longitudeSerie[0];
//
//                     final String rawZoneId = TimezoneMapper.latLngToTimezoneString(lat, lon);
//                     final ZoneId zoneId = ZoneId.of(rawZoneId);
//
//                     tourData.setTimeZoneId(zoneId.getId());
//
//                     TourLogManager.addLog(
//                           TourLogState.DEFAULT,
//                           NLS.bind(LOG_SAVE_TAGS_PROGRESS_REPLACE_TAGS, zoneId.getId(), tourDateTime));
//                  }

                  break;

               case Dialog_SaveTags.SAVE_TAG_ACTION_REMOVE_ALL_TAGS:

                  // remove time zone

//                  tourData.setTimeZoneId(null);
//
//                  TourLogManager.addLog(
//                        TourLogState.DEFAULT,
//                        NLS.bind(LOG_SAVE_TAGS_PROGRESS_REMOVE_ALL_TAGS, tourDateTime));

                  break;

               default:
                  // this should not happen
                  continue;
               }
//TODO
//               final TourData savedTourData = TourManager.saveModifiedTour(tourData, false);
//
//               if (savedTourData != null) {
//                  savedTours.add(savedTourData);
//               }
            }

            // update the UI
            if (savedTours.size() > 0) {

               Display.getDefault().asyncExec(new Runnable() {
                  @Override
                  public void run() {

                     Util.clearSelection();

                     /*
                      * Ensure the tour data editor contains the correct tour data
                      */
                     TourData tourDataInEditor = null;

                     final TourDataEditorView tourDataEditor = TourManager.getTourDataEditor();
                     if (tourDataEditor != null) {
                        tourDataInEditor = tourDataEditor.getTourData();
                     }

                     final TourEvent tourEvent = new TourEvent(savedTours);
                     tourEvent.tourDataEditorSavedTour = tourDataInEditor;
                     TourManager.fireEvent(TourEventId.TOUR_CHANGED, tourEvent);

                     // do a reselection of the selected tours to fire the multi tour data selection
//                     _tourProvider.toursAreModified(savedTours);
                  }
               });
            }
         }
      };

      return runnable;
   }
}
