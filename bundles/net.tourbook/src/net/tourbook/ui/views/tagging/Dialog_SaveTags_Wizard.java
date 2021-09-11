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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

public class Dialog_SaveTags_Wizard extends Wizard {

   private static final String LOG_SAVE_TAGS_START_APPEND_TAGS             = Messages.Log_SaveTags_Start_AppendTags;
   private static final String LOG_SAVE_TAGS_START_REPLACE_TAGS            = Messages.Log_SaveTags_Start_ReplaceTags;
   private static final String LOG_SAVE_TAGS_START_REMOVE_ALL_TAGS         = Messages.Log_SaveTags_Start_RemoveAllTags;
   private static final String LOG_SAVE_TAGS_START_REMOVE_SELECTED_TAGS    = Messages.Log_SaveTags_Start_RemoveSelectedTags;
   private static final String LOG_SAVE_TAGS_END                           = Messages.Log_SaveTags_End;

   private static final String LOG_SAVE_TAGS_PROGRESS_APPEND_TAGS          = Messages.Log_SaveTags_Progress_AppendTags;
   private static final String LOG_SAVE_TAGS_PROGRESS_REPLACE_TAGS         = Messages.Log_SaveTags_Progress_ReplaceTags;
   private static final String LOG_SAVE_TAGS_PROGRESS_REMOVE_ALL_TAGS      = Messages.Log_SaveTags_Progress_RemoveAllTags;
   private static final String LOG_SAVE_TAGS_PROGRESS_REMOVE_SELECTED_TAGS = Messages.Log_SaveTags_Progress_RemoveSelectedTags;
   //

   private final IPreferenceStore     _prefStore = TourbookPlugin.getPrefStore();

   private Dialog_SaveTags_WizardPage _wizardPage;

   private ArrayList<TourData>        _selectedTours;
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

      _wizardPage = new Dialog_SaveTags_WizardPage(_selectedTours, _allCheckedTagIds);

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

      TourLogManager.log_DEFAULT(String.format(
            LOG_SAVE_TAGS_END,
            (System.currentTimeMillis() - start) / 1000.0));

      return true;
   }

   private IRunnableWithProgress performFinish_getRunnable() {

      _wizardPage.saveState();

      final int saveTagAction = _prefStore.getInt(ITourbookPreferences.DIALOG_SAVE_TAGS_ACTION);

      /*
       * Create start log message
       */
      String startLogMessage = null;

      switch (saveTagAction) {
      case Dialog_SaveTags.SAVE_TAG_ACTION_REPLACE_TAGS:

         startLogMessage = NLS.bind(LOG_SAVE_TAGS_START_REPLACE_TAGS, _selectedTours.size());
         break;

      case Dialog_SaveTags.SAVE_TAG_ACTION_APPEND_NEW_TAGS:

         startLogMessage = NLS.bind(LOG_SAVE_TAGS_START_APPEND_TAGS, _selectedTours.size());
         break;

      case Dialog_SaveTags.SAVE_TAG_ACTION_REMOVE_ALL_TAGS:

         startLogMessage = NLS.bind(LOG_SAVE_TAGS_START_REMOVE_ALL_TAGS, _selectedTours.size());
         break;

      case Dialog_SaveTags.SAVE_TAG_ACTION_REMOVE_SELECTED_TAGS:

         startLogMessage = NLS.bind(LOG_SAVE_TAGS_START_REMOVE_SELECTED_TAGS, _selectedTours.size());
         break;
      }

      TourLogManager.log_INFO(startLogMessage);

      // log selected tags
      if (_allCheckedTagIds.size() > 0) {
         final String tagNamesText = TourDatabase.getTagNamesText(_allCheckedTagIds, false);
         TourLogManager.subLog_DEFAULT(tagNamesText);
      }

      final IRunnableWithProgress runnable = new IRunnableWithProgress() {

         @Override
         public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

            monitor.beginTask(Messages.Dialog_SaveTags_Label_Progress_Task, _selectedTours.size());

            // sort tours by date
            Collections.sort(_selectedTours);

            int workedTours = 0;
            final ArrayList<TourData> savedTours = new ArrayList<>();

            final HashMap<Long, TourTag> allTourTags = TourDatabase.getAllTourTags();

            for (final TourData tourData : _selectedTours) {

               if (monitor.isCanceled()) {
                  break;
               }

               monitor.worked(1);
               monitor.subTask(NLS.bind(Messages.Dialog_SaveTags_Label_Progress_SubTask,
                     ++workedTours,
                     _selectedTours.size()));

               final String tourDateTime = TourManager.getTourTitleDetailed(tourData);

               boolean isSaveTourData = false;
               String logMessage = null;

               final Set<TourTag> currentTourTags = tourData.getTourTags();

               switch (saveTagAction) {

               case Dialog_SaveTags.SAVE_TAG_ACTION_APPEND_NEW_TAGS:

                  // append new tags

                  // append all checked tags to the current tour
                  for (final Long requestedTagId : _allCheckedTagIds) {

                     final TourTag requestedTag = allTourTags.get(requestedTagId);

                     // check if new tags are set in the current tour
                     if (currentTourTags.contains(requestedTag) == false) {

                        // new tag is discovered -> save it

                        currentTourTags.add(requestedTag);
                        isSaveTourData = true;
                     }
                  }

                  if (isSaveTourData) {
                     logMessage = NLS.bind(LOG_SAVE_TAGS_PROGRESS_APPEND_TAGS, tourDateTime);
                  }

                  break;

               case Dialog_SaveTags.SAVE_TAG_ACTION_REPLACE_TAGS:

                  // replace existing tags

                  // get all checked tags
                  final ArrayList<TourTag> allRequestedTourTags = new ArrayList<>();
                  for (final Long requestedTagId : _allCheckedTagIds) {
                     allRequestedTourTags.add(allTourTags.get(requestedTagId));
                  }

                  currentTourTags.clear();
                  currentTourTags.addAll(allRequestedTourTags);

                  isSaveTourData = true;

                  logMessage = NLS.bind(LOG_SAVE_TAGS_PROGRESS_REPLACE_TAGS, tourDateTime);

                  break;

               case Dialog_SaveTags.SAVE_TAG_ACTION_REMOVE_SELECTED_TAGS:

                  // remove selected tags

                  for (final Long requestedTagId : _allCheckedTagIds) {

                     final TourTag requestedTag = allTourTags.get(requestedTagId);

                     // check if tag is available in the current tour
                     if (currentTourTags.contains(requestedTag)) {

                        // tag is discovered -> remove it

                        currentTourTags.remove(requestedTag);
                        isSaveTourData = true;
                     }
                  }

                  if (isSaveTourData) {
                     logMessage = NLS.bind(LOG_SAVE_TAGS_PROGRESS_REMOVE_SELECTED_TAGS, tourDateTime);
                  }

                  break;

               case Dialog_SaveTags.SAVE_TAG_ACTION_REMOVE_ALL_TAGS:

                  // remove all tags

                  currentTourTags.clear();

                  isSaveTourData = true;

                  logMessage = NLS.bind(LOG_SAVE_TAGS_PROGRESS_REMOVE_ALL_TAGS, tourDateTime);

                  break;

               default:

                  // this should not happen
                  continue;
               }

               if (isSaveTourData) {

                  final TourData savedTourData = TourManager.saveModifiedTour(tourData, false);

                  if (savedTourData != null) {

                     savedTours.add(savedTourData);

                     TourLogManager.subLog_DEFAULT(logMessage);
                  }
               }
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
