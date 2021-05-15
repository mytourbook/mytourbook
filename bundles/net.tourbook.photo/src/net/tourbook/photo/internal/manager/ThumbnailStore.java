/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.photo.internal.manager;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;

import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.photo.ImageQuality;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoActivator;
import net.tourbook.photo.internal.Messages;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;

public class ThumbnailStore {

   private static final long   MBYTE                         = 1024 * 1024;

   static final String         THUMBNAIL_IMAGE_EXTENSION_JPG = "jpg";             //$NON-NLS-1$
   private static final String THUMBNAIL_STORE_OS_PATH       = "thumbnail-store"; //$NON-NLS-1$

   /*
    * photo image properties saved in a properties file
    */
   private static final String        PROPERTIES_FILE_EXTENSION = "properties";           //$NON-NLS-1$
//	private static final String			PROPERTIES_FILE_HEADER			= "Image properties ";		//$NON-NLS-1$
   public static final String         ORIGINAL_IMAGE_WIDTH      = "OriginalImageWidth";   //$NON-NLS-1$
   public static final String         ORIGINAL_IMAGE_HEIGHT     = "OriginalImageHeight";  //$NON-NLS-1$

   private static IPreferenceStore    _prefStore                = PhotoActivator.getPrefStore();

   private static IPath               _storePath                = getThumbnailStorePath();

   /**
    * Images will be deleted when they are older than this date (in milliseconds)
    */
   private static long                _dateToDeleteOlderImagesMillis;

   private static long                _deleteUI_UpdateTime;
   private static int                 _deleteUI_DeletedFiles;
   private static int                 _deleteUI_CheckedFiles;
   private static long                _deleteUI_FilesSize;
   private static File                _errorFile;

   private static final ReentrantLock SAVE_LOCK                 = new ReentrantLock();

   private static IPath checkPath(final IPath storeImageFilePath) {

      final IPath imagePathWithoutExt = storeImageFilePath.removeFileExtension();

      // check store sub directory
      final File storeSubDir = imagePathWithoutExt.removeLastSegments(1).toFile();
      if (storeSubDir.exists() == false) {

         SAVE_LOCK.lock();

         try {
            // check again
            if (storeSubDir.exists() == false) {

               // create store sub directory

               if (storeSubDir.mkdirs() == false) {

                  StatusUtil.log(NLS.bind(//
                        "Thumbnail image path \"{0}\" cannot be created", //$NON-NLS-1$
                        storeSubDir.getAbsolutePath()), new Exception());

                  return null;
               }
            }

         } finally {
            SAVE_LOCK.unlock();
         }
      }
      return imagePathWithoutExt;
   }

   /**
    * @param isDeleteAllImages
    *           When <code>true</code> all images will be deleted and
    *           <code>isIgnoreCleanupPeriod</code> is ignored
    * @param isIgnoreCleanupPeriod
    */
   public static void cleanupStoreFiles(final boolean isDeleteAllImages, final boolean isIgnoreCleanupPeriod) {

      if (isDeleteAllImages) {

         doCleanup(Integer.MIN_VALUE, Long.MIN_VALUE, true);

         return;
      }

      // check if cleanup is enabled
      final boolean isCleanup = _prefStore.getBoolean(IPhotoPreferences.PHOTO_THUMBNAIL_STORE_IS_CLEANUP);
      if (isIgnoreCleanupPeriod == false && isCleanup == false) {
         return;
      }

      final int daysToKeepImages = _prefStore.getInt(//
            IPhotoPreferences.PHOTO_THUMBNAIL_STORE_NUMBER_OF_DAYS_TO_KEEP_IMAGES);

      // ckeck if cleanup is done always
      if (daysToKeepImages == 0) {

         doCleanup(Integer.MIN_VALUE, Long.MIN_VALUE, true);

      } else {

         final long lastCleanup = _prefStore.getLong(//
               IPhotoPreferences.PHOTO_THUMBNAIL_STORE_LAST_CLEANUP_DATE_TIME);
         final int cleanupPeriod = _prefStore.getInt(//
               IPhotoPreferences.PHOTO_THUMBNAIL_STORE_CLEANUP_PERIOD);

         ZonedDateTime dtLastCleanup;
         if (lastCleanup == 0) {
            // cleanup is never done
            dtLastCleanup = TimeTools.now().minusYears(99);
         } else {
            dtLastCleanup = TimeTools.getZonedDateTime(lastCleanup);
         }

         final ZonedDateTime today = TimeTools.now();

         final long lastCleanupDayDiff = ChronoUnit.DAYS.between(dtLastCleanup, today);

         // check if cleanup needs to be done
         if (isIgnoreCleanupPeriod || cleanupPeriod == 0 || lastCleanupDayDiff >= cleanupPeriod) {

            // convert kept days into a date in milliseconds
            final ZonedDateTime cleanupDate = today.minusDays(daysToKeepImages);

            final ZonedDateTime cleanupDateTime = TimeTools
                  .now()
                  .withYear(cleanupDate.getYear())
                  .withMonth(cleanupDate.getMonthValue())
                  .withDayOfMonth(cleanupDate.getDayOfMonth());

            final long dateToDeleteOlderImagesMillis = cleanupDateTime.toInstant().toEpochMilli();

            doCleanup(daysToKeepImages, dateToDeleteOlderImagesMillis, false);
         }
      }

      if (_errorFile != null) {
         StatusUtil.showStatus(
               NLS.bind(Messages.Thumbnail_Store_Error_CannotDeleteFolder, _errorFile.toString()),
               new Exception());
      }
   }

   /**
    * Cleanup store files for a specific folder.
    *
    * @param folderPath
    */
   public static void cleanupStoreFiles(final File[] imageFiles) {

      for (final File imageFile : imageFiles) {

         final String imageFilePath = imageFile.getPath();

         cleanupStoreFiles_10_QFile(imageFile, ImageQuality.THUMB, Photo.getImageKeyThumb(imageFilePath));
         cleanupStoreFiles_10_QFile(imageFile, ImageQuality.HQ, Photo.getImageKeyHQ(imageFilePath));
      }
   }

   private static void cleanupStoreFiles_10_QFile(final File imageFile,
                                                  final ImageQuality imageQuality,
                                                  final String imageKey) {

      final IPath storeImagePath = getStoreImagePath(imageFile.getName(), imageKey, imageQuality);
      final IPath propImagePath = getPropertiesPathFromImagePath(storeImagePath);

      cleanupStoreFiles_20_Delete(storeImagePath);
      cleanupStoreFiles_20_Delete(propImagePath);
   }

   private static void cleanupStoreFiles_20_Delete(final IPath storeImagePath) {

      final String storeFilePath = storeImagePath.toOSString();

      final File storeFile = new File(storeFilePath);

      if (storeFile.isFile()) {

//			/*
//			 * Java 7
//			 */
//			try {
//
//				final java.nio.file.Path path = FileSystems.getDefault().getPath(storeFilePath);
//
//				Files.delete(path);
//
//			} catch (final Exception e) {
//				StatusUtil.log(NLS.bind("cannot delete file: {0}", storeFilePath)); //$NON-NLS-1$
//			}

         final boolean isDeleted = storeFile.delete();

         if (isDeleted == false) {
            StatusUtil.log(NLS.bind("cannot delete file: {0}", storeFilePath)); //$NON-NLS-1$
         }
      }
   }

   private static void doCleanup(final int daysToKeepImages,
                                 final long dateToDeleteOlderImagesMillis,
                                 final boolean isDeleteAll) {

      _errorFile = null;
      _dateToDeleteOlderImagesMillis = dateToDeleteOlderImagesMillis;

      _deleteUI_UpdateTime = System.currentTimeMillis();
      _deleteUI_DeletedFiles = 0;
      _deleteUI_CheckedFiles = 0;
      _deleteUI_FilesSize = 0;

      try {

         final IRunnableWithProgress runnable = new IRunnableWithProgress() {

            @Override
            public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

               final File rootFolder = _storePath.toFile();
               final File[] rootFiles = rootFolder.listFiles();

               // show tasks info
               String message;
               if (isDeleteAll) {
                  message = Messages.Thumbnail_Store_CleanupTask_AllFiles;
               } else {
                  message = NLS.bind(Messages.Thumbnail_Store_CleanupTask, daysToKeepImages);
               }
               monitor.beginTask(message, rootFiles.length);

               for (final File folder : rootFiles) {

                  if (isDeleteAll) {
                     doCleanupAll(folder, monitor);
                  } else {

                     doCleanupDeleteFiles(folder, monitor);
                  }

                  if (monitor.isCanceled()) {
                     return;
                  }
                  monitor.worked(1);
               }
            }
         };

         new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, true, runnable);

      } catch (final InvocationTargetException | InterruptedException e) {
         StatusUtil.log(e);
      }

      // update last cleanup time
      _prefStore.setValue(//
            IPhotoPreferences.PHOTO_THUMBNAIL_STORE_LAST_CLEANUP_DATE_TIME,
            TimeTools.now().toInstant().toEpochMilli());
   }

   /**
    * recursively delete directory
    *
    * @param directory
    * @param monitor
    */
   private static boolean doCleanupAll(final File directory, final IProgressMonitor monitor) {

      boolean result = false;

      if (directory.isDirectory()) {
         final File[] files = directory.listFiles();

         for (final File file : files) {
            if (file.isDirectory()) {
               doCleanupAll(file, monitor);
            }

            /*
             * !!! canceled must be checked before isFileFolderDeleted is checked because this
             * returns false when the monitor is canceled !!!
             */
            if (monitor.isCanceled()) {
               return true;
            }

            file.delete();
         }

         result = directory.delete();
      }

      return result;
   }

   /**
    * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! RECURSIVE !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
    * <br>
    * Deletes files and subdirectories. If a deletion fails, the method stops attempting to delete.
    * <br>
    * <br>
    * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! RECURSIVE !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
    *
    * @param fileOrFolder
    * @param monitor
    * @return Returns number of deleted files
    */
   private static int doCleanupDeleteFiles(final File fileOrFolder, final IProgressMonitor monitor) {

      if (monitor.isCanceled()) {
         return 0;
      }

      boolean isFile = false;
      boolean doDeleteFileFolder = false;

      if (fileOrFolder.isDirectory()) {

         // file is a folder

         int deletedFileFolder = 0;
         final String[] allFileFolder = fileOrFolder.list();

         for (final String fileFolder2 : allFileFolder) {
            deletedFileFolder += doCleanupDeleteFiles(new File(fileOrFolder, fileFolder2), monitor);
         }

         // delete this folder when all children are deleted
         if (deletedFileFolder == allFileFolder.length) {
            doDeleteFileFolder = true;
         }

         // update monitor every 200ms
         final long time = System.currentTimeMillis();
         if (time > _deleteUI_UpdateTime + 200) {

            _deleteUI_UpdateTime = time;

            // show only the ending part of the folder name
            final String fileFolderName = fileOrFolder.toString();
            final int endIndex = fileFolderName.length();
            int beginIndex = endIndex - 90;
            beginIndex = beginIndex < 0 ? 0 : beginIndex;

            monitor.subTask(NLS.bind(Messages.Thumbnail_Store_CleanupTask_Subtask, //
                  new Object[] {
                        _deleteUI_CheckedFiles,
                        _deleteUI_DeletedFiles,
                        Long.toString(_deleteUI_FilesSize / MBYTE),
                        fileFolderName.substring(beginIndex, endIndex) }));
         }

      } else {

         // file is a file

         if (_dateToDeleteOlderImagesMillis == Long.MIN_VALUE
               || fileOrFolder.lastModified() < _dateToDeleteOlderImagesMillis) {
            doDeleteFileFolder = true;
            isFile = true;
         }
      }

      boolean isFileFolderDeleted = false;
      if (doDeleteFileFolder) {

         if (isFile) {
            _deleteUI_FilesSize += fileOrFolder.length();
         }

         // the folder is now empty so it can be deleted
         isFileFolderDeleted = fileOrFolder.delete();

         _deleteUI_DeletedFiles++;
      }

      _deleteUI_CheckedFiles++;

      /*
       * !!! canceled must be checked before isFileFolderDeleted is checked because this returns
       * false when the monitor is canceled !!!
       */
      if (monitor.isCanceled()) {
         return isFileFolderDeleted ? 1 : 0;
      }

      if (doDeleteFileFolder && isFileFolderDeleted == false) {
         _errorFile = fileOrFolder;
         monitor.setCanceled(true);
      }

      return isFileFolderDeleted ? 1 : 0;
   }

   static IPath getPropertiesPathFromImagePath(final IPath storeImageFilePath) {

      final String rawFileName = storeImageFilePath.removeFileExtension().lastSegment();

      final IPath propImageFilePath = storeImageFilePath
            .removeLastSegments(1)
            .append(rawFileName)
            .addFileExtension(PROPERTIES_FILE_EXTENSION);

      return propImageFilePath;
   }

   static synchronized IPath getStoreImagePath(final Photo photo, final ImageQuality imageQuality) {

      if (photo.imageFilePathName.startsWith(_storePath.toOSString())) {

         // photo image is already from the thumb store

         return new Path(photo.imageFilePathName);
      }

      final String rawImageFileName = photo.imageFileName;
      final String imageKey = photo.getImageKey(imageQuality);

      final IPath imageFilePath = getStoreImagePath(rawImageFileName, imageKey, imageQuality);

      return imageFilePath;
   }

   private static IPath getStoreImagePath(final String rawImageFileName,
                                          final String imageKey,
                                          final ImageQuality imageQuality) {

      // thumbnail images are stored as jpg file
      IPath jpgPhotoFilePath = new Path(rawImageFileName);
      jpgPhotoFilePath = jpgPhotoFilePath.removeFileExtension().addFileExtension(THUMBNAIL_IMAGE_EXTENSION_JPG);

      final String imageKey1Folder = imageKey.substring(0, 2);
      final String imageFileName = imageKey + "_" + imageQuality.name() + "_" + jpgPhotoFilePath.toOSString(); //$NON-NLS-1$ //$NON-NLS-2$

      final IPath imageFilePath = _storePath//
            .append(imageKey1Folder)
            .append(imageFileName);

      return imageFilePath;
   }

   /**
    * @return Returns the file path for the thumbnail store
    */
   private static IPath getThumbnailStorePath() {

      final boolean useDefaultLocation = _prefStore.getBoolean(//
            IPhotoPreferences.PHOTO_THUMBNAIL_STORE_IS_DEFAULT_LOCATION);

      String tnFolderName;
      if (useDefaultLocation) {
         tnFolderName = Platform.getInstanceLocation().getURL().getPath();
      } else {
         tnFolderName = _prefStore.getString(IPhotoPreferences.PHOTO_THUMBNAIL_STORE_CUSTOM_LOCATION);
      }

      if (tnFolderName.trim().length() == 0) {
         tnFolderName = _prefStore.getString(IPhotoPreferences.PHOTO_THUMBNAIL_STORE_CUSTOM_LOCATION);
      }

      final File tnFolderFile = new File(tnFolderName);

      if (tnFolderFile.exists() == false || tnFolderFile.isDirectory() == false) {

         StatusUtil.logInfo(NLS.bind("Thumbnail folder \"{0}\" is not available, it will be created now", //$NON-NLS-1$
               tnFolderFile.getAbsolutePath()));

         final String errorMsgCannotCreateFolder = NLS.bind("Thumbnail folder \"{0}\" cannot be created", //$NON-NLS-1$
               tnFolderFile.getAbsolutePath());

         // try to create thumbnail folder
         try {

            final boolean isCreated = tnFolderFile.mkdirs();
            if (isCreated) {
               StatusUtil.logInfo(NLS.bind("Thumbnail folder \"{0}\" created", tnFolderFile.getAbsolutePath())); //$NON-NLS-1$
            } else {
               StatusUtil.showStatus(errorMsgCannotCreateFolder);
            }

         } catch (final Exception e) {
            throw new RuntimeException(errorMsgCannotCreateFolder, e);
         }
      }

      // append a unique folder so that deleting images is not being done in the wrong directory
      final IPath tnFolderPath = new Path(tnFolderName).append(THUMBNAIL_STORE_OS_PATH);
      final File tnFolderFileUnique = tnFolderPath.toFile();
      if (tnFolderFileUnique.exists() == false || tnFolderFileUnique.isDirectory() == false) {

         final String errorMsgCannotCreateFolder = NLS.bind("Thumbnail folder \"{0}\" cannot be created", //$NON-NLS-1$
               tnFolderFileUnique.getAbsolutePath());

         // try to create thumbnail unique folder
         try {
            final boolean isCreated = tnFolderFileUnique.mkdirs();
            if (isCreated) {
               StatusUtil.logInfo(NLS.bind("Thumbnail folder \"{0}\" created", //$NON-NLS-1$
                     tnFolderFileUnique.getAbsolutePath()));
            } else {
               StatusUtil.showStatus(errorMsgCannotCreateFolder);
            }
         } catch (final Exception e) {
            throw new RuntimeException(errorMsgCannotCreateFolder, e);
         }
      }

//		StatusUtil.logInfo(NLS.bind("Using thumbnail folder: \"{0}\"", //$NON-NLS-1$
//				tnFolderFileUnique.getAbsolutePath()));

      return tnFolderPath.addTrailingSeparator();
   }

   private static void saveProperties(final IPath storeImageFilePath, final Properties originalImageProperties) {

      IPath propImageFilePath = null;
      FileOutputStream fileStream = null;

      try {

         propImageFilePath = getPropertiesPathFromImagePath(storeImageFilePath);

         final File propFile = new File(propImageFilePath.toOSString());

         fileStream = new FileOutputStream(propFile);

         originalImageProperties.store(fileStream, null);

      } catch (final Exception e) {

         StatusUtil.log(NLS.bind(//
               "Cannot save properties file: \"{0}\"", //$NON-NLS-1$
               propImageFilePath.toOSString()), e);
      } finally {

         if (fileStream != null) {
            try {
               fileStream.close();
            } catch (final IOException e) {
               e.printStackTrace();
            }
         }
      }
   }

   /**
    * @param thumbImg
    * @param storeImageFilePath
    * @param originalImageProperties
    * @return Returns <code>true</code>when the image could be saved in the thumb store.
    */
   static boolean saveThumbImageWithAWT(final BufferedImage thumbImg,
                                        final IPath storeImageFilePath,
                                        final Properties originalImageProperties) {

      try {

         final IPath imagePathWithoutExt = checkPath(storeImageFilePath);
         if (imagePathWithoutExt == null) {
            return false;
         }

         ImageIO.write(thumbImg, THUMBNAIL_IMAGE_EXTENSION_JPG, new File(storeImageFilePath.toOSString()));

         saveProperties(storeImageFilePath, originalImageProperties);

      } catch (final Exception e) {

         StatusUtil.log(NLS.bind(//
               "Cannot save thumbnail image with AWT: \"{0}\"", //$NON-NLS-1$
               storeImageFilePath.toOSString()), e);

         return false;
      }

      return true;
   }

   static void saveThumbImageWithSWT(final Image thumbnailImage,
                                     final IPath storeImageFilePath,
                                     final Properties originalImageProperties) {

      try {

         final IPath imagePathWithoutExt = checkPath(storeImageFilePath);
         if (imagePathWithoutExt == null) {
            return;
         }

         final ImageLoader imageLoader = new ImageLoader();
         imageLoader.data = new ImageData[] { thumbnailImage.getImageData() };

         final IPath fullImageFilePath = imagePathWithoutExt.addFileExtension(THUMBNAIL_IMAGE_EXTENSION_JPG);

         /*
          * save thumbnail as jpg image, Eclipse 3.8 M5 saves it with better quality, default is
          * 75%, compression in the imageloader could be set
          */
         imageLoader.compression = 75;
         imageLoader.save(fullImageFilePath.toOSString(), SWT.IMAGE_JPEG);

         saveProperties(storeImageFilePath, originalImageProperties);

      } catch (final Exception e) {

         StatusUtil.log(NLS.bind(//
               "Cannot save thumbnail image with SWT: \"{0}\"", //$NON-NLS-1$
               storeImageFilePath.toOSString()), e);
      }
   }

   public static void updateStoreLocation() {
      _storePath = getThumbnailStorePath();
   }
}
