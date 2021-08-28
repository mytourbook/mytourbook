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

/**
 * Various IN and OUT states for the import/re-import process
 */
public class ImportStates {

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
   public long          importId           = System.currentTimeMillis();

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
    * OUT state:
    * <p>
    * When <code>true</code> then this code should be fired, when all tours are
    * imported/re-imported, default is <code>false</code>
    * <p>
    * <code>
    * TourManager.fireEvent(TourEventId.TAG_STRUCTURE_CHANGED)
    * </code>
    */
   public AtomicBoolean isFire_NewTag      = new AtomicBoolean();

   /**
    * OUT state:
    * <p>
    * When <code>true</code> then this code should be fired, when all tours are
    * imported/re-imported, default is <code>false</code>
    * <p>
    * <code>
    *
    * TourbookPlugin.getDefault()
    *               .getPreferenceStore()
    *               .setValue(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED, Math.random())
    * </code>
    */
   public AtomicBoolean isFire_NewTourType = new AtomicBoolean();

   public boolean       isLog_DEFAULT;
   public boolean       isLog_INFO;
   public boolean       isLog_OK;

   /**
    *
    */
   public ImportStates() {

      setIsLog_DEFAULT(true);
      setIsLog_INFO(true);
      setIsLog_OK(true);
   }

   /**
    * IN state:
    *
    * @param isEasyImport
    * @return
    */
   public ImportStates setIsEasyImport(final boolean isEasyImport) {

      this.isEasyImport = isEasyImport;

      return this;
   }

   /**
    * IN state:
    *
    * @param isLog
    * @return
    */
   public ImportStates setIsLog_DEFAULT(final boolean isLog) {

      isLog_DEFAULT = isLog;

      return this;
   }

   /**
    * IN state:
    *
    * @param isLog
    * @return
    */
   public ImportStates setIsLog_INFO(final boolean isLog) {

      isLog_INFO = isLog;

      return this;
   }

   /**
    * IN state:
    *
    * @param isLog
    * @return
    */
   public ImportStates setIsLog_OK(final boolean isLog) {

      isLog_OK = isLog;

      return this;
   }

   /**
    * INTERNAL state:
    *
    * @param isReimport
    * @return
    */
   public ImportStates setIsReimport(final boolean isReimport) {

      this.isReimport = isReimport;

      return this;
   }

   public ImportStates setIsSkipToursWithFileNotFound(final boolean isSkipToursWithFileNotFound) {

      this.isSkipToursWithFileNotFound = isSkipToursWithFileNotFound;

      return this;
   }
}
