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
package net.tourbook.importdata;

import net.tourbook.common.NIO;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;

public class ImportConfig implements Cloneable {

   public static final String DEVICE_FILES_DEFAULT = "*";                 //$NON-NLS-1$

   private static long        _idCreator;

   public String              name                 = UI.EMPTY_STRING;

   /** When <code>true</code> then a backup of the tour file is created. */
   public boolean             isCreateBackup       = true;
   private String             _backupFolder        = UI.EMPTY_STRING;

   private int                _deviceType          = 0;
   private String             _deviceFolder        = UI.EMPTY_STRING;

   /**
    * Glob pattern to get device files.
    */
   public String              fileGlobPattern      = DEVICE_FILES_DEFAULT;

   /** When <code>true</code> then the device watching is turned off after tours are imported. */
   public boolean             isTurnOffWatching    = false;

   public boolean             isDeleteDeviceFiles  = false;

   private long               _id;

   public ImportConfig() {

      _id = ++_idCreator;
   }

   @Override
   protected ImportConfig clone() {

      ImportConfig clonedObject = null;

      try {

         clonedObject = (ImportConfig) super.clone();

         clonedObject._id = ++_idCreator;

      } catch (final CloneNotSupportedException e) {
         StatusUtil.log(e);
      }

      return clonedObject;
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (getClass() != obj.getClass()) {
         return false;
      }
      final ImportConfig other = (ImportConfig) obj;
      if (_id != other._id) {
         return false;
      }
      return true;
   }

   public String getBackupFolder() {
      return _backupFolder;
   }

   /**
    * @return Returns the backup OS folder or <code>null</code> when not available.
    */
   public String getBackupOSFolder() {

      if (isCreateBackup) {
         return getOSFolder(_backupFolder);
      }

      return null;
   }

   public String getDeviceFolder() {
      return _deviceFolder;
   }

   /**
    * @return Returns the device OS folder or <code>null</code> when not available.
    */
   public String getDeviceOSFolder() {
      return getOSFolder(_deviceFolder);
   }

   public int getDeviceType() {
      return _deviceType;
   }

   public long getId() {
      return _id;
   }

   private String getOSFolder(final String folder) {

      if (folder == null || folder.trim().length() == 0) {
         return null;
      }

      if (NIO.isDeviceNameFolder(folder) ||
            NIO.isTourBookFileSystem(folder)) {

         return NIO.convertToOSPath(folder);

      } else {

         return folder;
      }
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (_id ^ (_id >>> 32));
      return result;
   }

   /**
    * @return Returns <code>true</code> when the device or backup folder should be watched.
    *         <p>
    *         The folders are not checked in the filesystem only the definition. This prevents
    *         delays when accessing the fs.
    */
   public boolean isWatchAnything() {

      final boolean isWatch_DeviceFolder = _deviceFolder != null && _deviceFolder.trim().length() > 0;
      final boolean isWatch_BackupFolder = _backupFolder != null && _backupFolder.trim().length() > 0;

      return isWatch_DeviceFolder || (isCreateBackup && isWatch_BackupFolder);
   }

   public void setBackupFolder(final String backupFolder) {
      _backupFolder = backupFolder;
   }

   public void setDeviceFolder(final String deviceFolder) {
      _deviceFolder = deviceFolder;
   }

   public void setDeviceType(final int deviceType) {
      this._deviceType = deviceType;
   }

   @Override
   public String toString() {

      return "ImportConfig [\n" //$NON-NLS-1$

            + ("name=" + name + ", \n") //$NON-NLS-1$ //$NON-NLS-2$
            + ("isCreateBackup=" + isCreateBackup + ", \n") //$NON-NLS-1$ //$NON-NLS-2$
            + ("isDeleteDeviceFiles=" + isDeleteDeviceFiles + ", \n") //$NON-NLS-1$ //$NON-NLS-2$
            + ("isTurnOffWatching=" + isTurnOffWatching + ", \n") //$NON-NLS-1$ //$NON-NLS-2$
            + ("_id=" + _id + ", \n") //$NON-NLS-1$ //$NON-NLS-2$
            + ("_backupFolder=" + _backupFolder + ", \n") //$NON-NLS-1$ //$NON-NLS-2$
            + ("_deviceType=" + _deviceType + ", \n") //$NON-NLS-1$ //$NON-NLS-2$
            + ("_deviceFolder=" + _deviceFolder + ", \n") //$NON-NLS-1$ //$NON-NLS-2$
            + ("deviceFiles=" + fileGlobPattern) //$NON-NLS-1$

            + "\n]"; //$NON-NLS-1$
   }
}
