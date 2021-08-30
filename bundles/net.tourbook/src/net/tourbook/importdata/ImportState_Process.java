/*******************************************************************************
 * Copyright (C) 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.importdata;

import java.util.concurrent.atomic.AtomicBoolean;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

/**
 * IN and OUT states for the whole import/re-import process
 */
public class ImportState_Process {

   /**
    * IN state:
    * <p>
    * When <code>true</code> then tours will be skipped when the import file is not defined or not
    * available
    */
   public boolean       isSkipToursWithFileNotFound;

   /**
    * IN state:
    * <p>
    * When <code>true</code> then it is a re-import, otherwise it is a normal import, default is
    * <code>false</code>
    */
   public boolean       isReimport;

   /**
    * IN state:
    * <p>
    * Is <code>true</code> when the import is started from easy import
    */
   public boolean       isEasyImport;

   /**
    * INTERNAL state:
    * <p>
    * Contains a unique id so that each import can be identified.
    */
   public long          importId              = System.currentTimeMillis();

   /**
    * OUT state:
    * <p>
    * Is <code>true</code> when the import was canceled by the user
    */
   public boolean       isImportCanceled_ByMonitor;

   /**
    * OUT state:
    * <p>
    * Is <code>true</code> when the import was canceled after a dialog was displayed to the user
    */
   public boolean       isImportCanceled_ByUserDialog;

   /**
    * IN state:
    * <p>
    * When set to <code>true</code> then {@link #runPostProcess()} should be run AFTER all is
    * imported.
    */
   public AtomicBoolean isCreated_NewTag      = new AtomicBoolean();

   /**
    * IN state:
    * <p>
    * When set to <code>true</code> then {@link #runPostProcess()} should be run AFTER all is
    * imported.
    */
   public AtomicBoolean isCreated_NewTourType = new AtomicBoolean();

   public boolean       isLog_DEFAULT;
   public boolean       isLog_INFO;
   public boolean       isLog_OK;

   /**
    * When <code>true</code> then errors are not displayed to the user
    */
   public boolean       isSilentError;

   /**
    *
    */
   public ImportState_Process() {

      setIsLog_DEFAULT(true);
      setIsLog_INFO(true);
      setIsLog_OK(true);
   }

   /**
    * Run post process actions, e.g. when new tour tags or tour types were created, update the UI
    */
   public void runPostProcess() {

      if (isCreated_NewTourType.get()) {

         TourbookPlugin.getPrefStore().setValue(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED, Math.random());
      }

      if (isCreated_NewTag.get()) {

         TourManager.fireEvent(TourEventId.TAG_STRUCTURE_CHANGED);
      }
   }

   /**
    * IN state:
    *
    * @param isEasyImport
    * @return
    */
   public ImportState_Process setIsEasyImport(final boolean isEasyImport) {

      this.isEasyImport = isEasyImport;

      return this;
   }

   /**
    * IN state:
    *
    * @param isLog
    * @return
    */
   public ImportState_Process setIsLog_DEFAULT(final boolean isLog) {

      isLog_DEFAULT = isLog;

      return this;
   }

   /**
    * IN state:
    *
    * @param isLog
    * @return
    */
   public ImportState_Process setIsLog_INFO(final boolean isLog) {

      isLog_INFO = isLog;

      return this;
   }

   /**
    * IN state:
    *
    * @param isLog
    * @return
    */
   public ImportState_Process setIsLog_OK(final boolean isLog) {

      isLog_OK = isLog;

      return this;
   }

   /**
    * INTERNAL state:
    *
    * @param isReimport
    * @return
    */
   public ImportState_Process setIsReimport(final boolean isReimport) {

      this.isReimport = isReimport;

      return this;
   }

   public ImportState_Process setIsSkipToursWithFileNotFound(final boolean isSkipToursWithFileNotFound) {

      this.isSkipToursWithFileNotFound = isSkipToursWithFileNotFound;

      return this;
   }
}
