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

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.DeviceSensor;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

/**
 * IN and OUT states for the whole import/re-import process
 */
public class ImportState_Process {

   private boolean                                 isLog_DEFAULT;
   private boolean                                 isLog_INFO;
   private boolean                                 isLog_OK;

   /**
    * IN state:
    * <p>
    * When <code>true</code> then errors are not displayed to the user, default is
    * <code>false</code>
    */
   private boolean                                 isSilentError;

   /**
    * IN state:
    * <p>
    * When <code>true</code> then tours will be skipped when the import file is not defined or not
    * available, default is <code>false</code>
    */
   private boolean                                 isSkipToursWithFileNotFound;

   /**
    * IN state:
    * <p>
    * Is <code>true</code> when the import is started from easy import, default is
    * <code>false</code>
    */
   private boolean                                 isEasyImport;

   /**
    * IN state:
    * <p>
    * Is <code>true</code> when the current import is run within a JUnit test, default is
    * <code>false</code>
    */
   private boolean                                 isJUnitTest;

   /**
    * INTERNAL state:
    * <p>
    * Contains a unique id so that each import can be identified.
    */
   private long                                    importId                      = System.currentTimeMillis();

   /**
    * OUT state:
    * <p>
    * Is <code>true</code> when the import was canceled by the user
    */
   private AtomicBoolean                           isImportCanceled_ByMonitor    = new AtomicBoolean();

   /**
    * OUT state:
    * <p>
    * Is <code>true</code> when the import was canceled after a dialog was displayed to the user
    */
   private AtomicBoolean                           isImportCanceled_ByUserDialog = new AtomicBoolean();

   /**
    * OUT state:
    * <p>
    * When set to <code>true</code> then {@link #runPostProcess()} should be run AFTER all is
    * imported.
    */
   private AtomicBoolean                           isCreated_NewTag              = new AtomicBoolean();

   /**
    * OUT state:
    * <p>
    * When set to <code>true</code> then {@link #runPostProcess()} should be run AFTER all is
    * imported.
    */
   private AtomicBoolean                           isCreated_NewTourType         = new AtomicBoolean();

   /**
    * OUT state:
    * <p>
    * Device sensors which must be updated in the db, key is the serial number
    */
   private ConcurrentHashMap<String, DeviceSensor> _allDeviceSensorToBeUpdated   = new ConcurrentHashMap<>();

   /**
    * IN and OUT states for the whole import/re-import process.
    * <p>
    * This constructor set's all logging to <code>true</code>.
    */
   public ImportState_Process() {

      setIsLog_DEFAULT(true);
      setIsLog_INFO(true);
      setIsLog_OK(true);
   }

   public ConcurrentHashMap<String, DeviceSensor> getAllDeviceSensorsToBeUpdated() {
      return _allDeviceSensorToBeUpdated;
   }

   public long getImportId() {
      return importId;
   }

   /**
    * OUT state:
    * <p>
    * When set to <code>true</code> then {@link #runPostProcess()} should be run AFTER all
    * isimported.
    *
    * @return
    */
   public AtomicBoolean isCreated_NewTag() {
      return isCreated_NewTag;
   }

   /**
    * OUT state:
    * <p>
    * When set to <code>true</code> then {@link #runPostProcess()} should be run AFTER all
    * isimported.
    *
    * @return
    */
   public AtomicBoolean isCreated_NewTourType() {
      return isCreated_NewTourType;
   }

   public boolean isEasyImport() {
      return isEasyImport;
   }

   public AtomicBoolean isImportCanceled_ByMonitor() {
      return isImportCanceled_ByMonitor;
   }

   public AtomicBoolean isImportCanceled_ByUserDialog() {
      return isImportCanceled_ByUserDialog;
   }

   public boolean isJUnitTest() {
      return isJUnitTest;
   }

   public boolean isLog_DEFAULT() {
      return isLog_DEFAULT;
   }

   public boolean isLog_INFO() {
      return isLog_INFO;
   }

   public boolean isLog_OK() {
      return isLog_OK;
   }

   public boolean isSilentError() {
      return isSilentError;
   }

   public boolean isSkipToursWithFileNotFound() {
      return isSkipToursWithFileNotFound;
   }

   /**
    * Run post process actions, e.g. when new tour tags or tour types were created, update the UI
    */
   public void runPostProcess() {

      if (_allDeviceSensorToBeUpdated.size() > 0) {
         updateSensors();
      }

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
    * @param isTest
    * @return
    */
   public ImportState_Process setIsJUnitTest(final boolean isTest) {

      isJUnitTest = isTest;

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
    * IN state:
    *
    * @param isSilentError
    * @return
    */
   public ImportState_Process setIsSilentError(final boolean isSilentError) {

      this.isSilentError = isSilentError;

      return this;
   }

   /**
    * IN state:
    *
    * @param isSkipToursWithFileNotFound
    * @return
    */
   public ImportState_Process setIsSkipToursWithFileNotFound(final boolean isSkipToursWithFileNotFound) {

      this.isSkipToursWithFileNotFound = isSkipToursWithFileNotFound;

      return this;
   }

   public void transferCreateStates(final ImportState_Process importState_Process) {

      if (importState_Process.isCreated_NewTag().get()) {
         isCreated_NewTag.set(true);
      }

      if (importState_Process.isCreated_NewTourType.get()) {
         isCreated_NewTourType.set(true);
      }
   }

   private void updateSensors() {

      final EntityManager em = TourDatabase.getInstance().getEntityManager();
      final EntityTransaction ts = em.getTransaction();

      try {

         for (final Entry<String, DeviceSensor> entrySet : _allDeviceSensorToBeUpdated.entrySet()) {

            final DeviceSensor sensor = entrySet.getValue();

            ts.begin();
            {
               em.merge(sensor);
            }
            ts.commit();
         }

      } catch (final Exception e) {
         StatusUtil.showStatus(e);
      } finally {
         if (ts.isActive()) {
            ts.rollback();
         }
         em.close();
      }
   }

}
