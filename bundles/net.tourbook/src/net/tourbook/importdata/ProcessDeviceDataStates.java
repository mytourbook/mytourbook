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
 * Different IN and OUT states for the import/re-import process
 */
public class ProcessDeviceDataStates {

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
    * When <code>true</code> then the tour import/re-import is running concurrently, default is
    * <code>false</code>
    */
   public boolean       isRunningConcurrently;

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
   public ProcessDeviceDataStates() {

      setIsLog_DEFAULT(true);
      setIsLog_INFO(true);
      setIsLog_OK(true);
   }

   /**
    * IN state:
    *
    * @param isLog
    * @return
    */
   public ProcessDeviceDataStates setIsLog_DEFAULT(final boolean isLog) {

      isLog_DEFAULT = isLog;

      return this;
   }

   /**
    * IN state:
    *
    * @param isLog
    * @return
    */
   public ProcessDeviceDataStates setIsLog_INFO(final boolean isLog) {

      isLog_INFO = isLog;

      return this;
   }

   /**
    * IN state:
    *
    * @param isLog
    * @return
    */
   public ProcessDeviceDataStates setIsLog_OK(final boolean isLog) {

      isLog_OK = isLog;

      return this;
   }

   public ProcessDeviceDataStates setIsReimport(final boolean isReimport) {

      this.isReimport = isReimport;

      return this;
   }

   public ProcessDeviceDataStates setIsRunningConcurrently(final boolean isRunningConcurrently) {

      this.isRunningConcurrently = isRunningConcurrently;

      return this;
   }
}
