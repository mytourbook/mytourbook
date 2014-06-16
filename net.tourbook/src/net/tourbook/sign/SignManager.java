/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.sign;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.SQLUtils;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourSign;
import net.tourbook.data.TourSignCategory;
import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;
import net.tourbook.photo.ILoadCallBack;
import net.tourbook.photo.ImageQuality;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoImageCache;
import net.tourbook.photo.PhotoLoadManager;
import net.tourbook.photo.PhotoLoadingState;
import net.tourbook.photo.PhotosWithExifSelection;
import net.tourbook.preferences.PrefPageSigns;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 */
public class SignManager {

	/**
	 * Update interval in ms.
	 */
	private static final int							MONITOR_UPDATE_INTERVAL	= 100;

	private static final String							KEY_PART_SEPARATOR		= "__";									//$NON-NLS-1$
	private static final ImageQuality					SIGN_IMAGE_QUALITY		= ImageQuality.HQ;
	public static final long							ROOT_SIGN_ID			= -1L;

	public static final String[]						EXPAND_TYPE_NAMES		= {
			Messages.app_action_expand_type_flat,
			Messages.app_action_expand_type_year_day,
			Messages.app_action_expand_type_year_month_day						};

	public static final int[]							EXPAND_TYPES			= {
			TourTag.EXPAND_TYPE_FLAT,
			TourTag.EXPAND_TYPE_YEAR_DAY,
			TourTag.EXPAND_TYPE_YEAR_MONTH_DAY									};

	/**
	 * Key is sign ID.
	 */
	private static volatile HashMap<Long, TourSign>		_tourSigns;

	/**
	 * Key is the sign category ID or <code>-1</code> for the root.
	 */
	private static HashMap<Long, SignCollection>		_signCollections		= new HashMap<Long, SignCollection>();

	/**
	 * Contains {@link TourSign}'s during sign importing, key is the {@link TourSign#getSignId()}.
	 */
	private static HashMap<String, TourSign>			_importedSigns			= new HashMap<String, TourSign>();

	/**
	 * Contains {@link TourSignCategory}'s during sign category importing, key is the
	 * {@link TourSignCategory#getCategoryKey()}.
	 */
	private static HashMap<String, TourSignCategory>	_importedSignCategories	= new HashMap<String, TourSignCategory>();

	private static long									_parseUIUpdateTime;
	private static int									_fsItemCounter;

	private static int									_folderDepth;

	/**
	 * Number of segments in the {@link #_baseFolder}.
	 */
	private static int									_baseFolderSegments;

	/**
	 * Removes all tour signs which are loaded from the database so the next time they will be
	 * reloaded.
	 */
	public static synchronized void clearTourSigns() {

		if (_tourSigns != null) {

			_tourSigns.clear();
			_tourSigns = null;
		}

		_signCollections.clear();

		_importedSigns.clear();
		_importedSignCategories.clear();
	}

	/**
	 * Imports all images from the selected folder and all subfolders, the foldername will be used
	 * as the category name.
	 * 
	 * @param selectedFolder
	 */
	public static void createSignImages(final File selectedFolder) {

		final Path baseFolder = new Path(selectedFolder.getAbsolutePath());

		// confirm import
		if (MessageDialog.openConfirm(
				Display.getCurrent().getActiveShell(),
				Messages.Dialog_ImportSigns_ConfirmImportFolder_Title,
				NLS.bind(//
						Messages.Dialog_ImportSigns_ConfirmImportFolder_Message,
						baseFolder.toOSString())) == false) {
			return;
		}

		TEMP_DB_cleanup();

		_baseFolderSegments = baseFolder.segmentCount();

		try {

			final IRunnableWithProgress runnable = new IRunnableWithProgress() {

				@Override
				public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

					final String taskName = Messages.Dialog_ImportSigns_MonitorTask;
					monitor.beginTask(taskName, IProgressMonitor.UNKNOWN);

					// folder depth position is the parent of the root
					_folderDepth = -1;

					parseFolder_10_FilesFolder(//
							selectedFolder,
							UI.EMPTY_STRING,
							null,
							monitor);
				}
			};

			final ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(Display
					.getCurrent()
					.getActiveShell());
			progressMonitorDialog.run(true, true, runnable);

		} catch (final Exception e) {
			StatusUtil.showStatus(e);
		}

		importSignImages_Final();
	}

	/**
	 * Import sign images from a {@link PhotosWithExifSelection}.
	 * 
	 * @param selectedPhotosWithExif
	 */
	public static void createSignImages(final PhotosWithExifSelection selectedPhotosWithExif) {

		// confirm import
		if (MessageDialog.openConfirm(
				Display.getCurrent().getActiveShell(),
				Messages.Dialog_ImportSigns_ConfirmImportSelection_Title,
				Messages.Dialog_ImportSigns_ConfirmImportSelection_Message) == false) {
			return;
		}

		TEMP_DB_cleanup();

		for (final Photo signPhoto : selectedPhotosWithExif.photos) {

			// get file/folder name without extension
			final File fileOrFolder = signPhoto.imageFile;
			final Path fileOrFolderPath = new Path(fileOrFolder.getAbsolutePath());
			final String fileOrFolderName = fileOrFolderPath.removeFileExtension().lastSegment();

			parseFolder_20_CreateTourSign(//
					fileOrFolder,
					fileOrFolderName,
					TourSignCategory.ROOT_KEY,
					null,
					true);
		}

		importSignImages_Final();
	}

	/**
	 * Ensure that the sign key (which are folder names) do not contain the sql string separator.
	 * 
	 * @param signName
	 * @param categoryKey
	 * @param isRoot
	 * @return
	 */
	public static String createSignKey(final String signName, final String categoryKey, final boolean isRoot) {

		String cleanSignKey;

		if (isRoot) {

			cleanSignKey = SQLUtils.getCleanString(signName);

		} else {

			cleanSignKey = SQLUtils.getCleanString(categoryKey)
					+ KEY_PART_SEPARATOR
					+ SQLUtils.getCleanString(signName);
		}

		return cleanSignKey;
	}

	/**
	 * @param categoryName
	 * @param categoryKey
	 * @param isRoot
	 * @param imageFilePathName
	 * @return Returns a cached {@link TourSign} or creates a new.
	 */
	public static TourSign getImportedSignByKey(final String signName, final String categoryKey, final boolean isRoot) {

		final String signKey = createSignKey(signName, categoryKey, isRoot);

		// check if signs are loaded
		TourSign sign = _importedSigns.get(signKey);
		if (sign != null) {
			return sign;
		}

		/*
		 * read signs from the database
		 */
		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		Assert.isTrue(em != null);

		final String sql = "SELECT sign" //$NON-NLS-1$
				+ (" FROM " + TourSign.class.getSimpleName() + " AS sign") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" WHERE sign.signKey=" + SQLUtils.getSqlString(signKey)); //$NON-NLS-1$

		try {

			final Query emQuery = em.createQuery(sql);

			@SuppressWarnings("unchecked")
			final ArrayList<TourSign> resultList = (ArrayList<TourSign>) emQuery.getResultList();

			if (resultList.size() > 0) {

				sign = resultList.get(0);

			} else {

				sign = new TourSign(signName, signKey);
			}

			_importedSigns.put(signKey, sign);

		} catch (final Exception e) {

			StatusUtil.log(sql, e);

		} finally {

			em.close();
		}

		return sign;
	}

	/**
	 * Creates {@link TourSignCategory} by the categoryKey when it is not available.
	 * 
	 * @param categoryName
	 * @param categoryKey
	 * @param isRoot
	 * @return Returns {@link TourSignCategory} for the categoryKey.
	 */
	public static TourSignCategory getImportedSignCategoryByKey(final String categoryName,
																final String categoryKey,
																final boolean isRoot) {

		final String cleanCategoryName = SQLUtils.getCleanString(categoryName);
		final String cleanCategoryKey = SQLUtils.getCleanString(categoryKey);

		// check if sign categories are loaded
		TourSignCategory signCategory = _importedSignCategories.get(cleanCategoryKey);
		if (signCategory != null) {
			return signCategory;
		}

		/*
		 * read sign categories from the database
		 */
		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		Assert.isTrue(em != null);

		final String sql = "SELECT signCategory" //$NON-NLS-1$
				+ (" FROM " + TourSignCategory.class.getSimpleName() + " AS signCategory") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" WHERE" //$NON-NLS-1$
						+ (" signCategory.categoryKey=" + SQLUtils.getSqlString(cleanCategoryKey)) //$NON-NLS-1$
						+ " AND" //$NON-NLS-1$
						+ " signCategory.name=" + SQLUtils.getSqlString(cleanCategoryName)); //$NON-NLS-1$

		try {

			final Query emQuery = em.createQuery(sql);

			@SuppressWarnings("unchecked")
			final ArrayList<TourSignCategory> resultList = (ArrayList<TourSignCategory>) emQuery.getResultList();

			if (resultList.size() > 0) {

				signCategory = resultList.get(0);

			} else {

				final TourSignCategory newSignCategory = new TourSignCategory(cleanCategoryName, cleanCategoryKey);

				newSignCategory.setRoot(isRoot);

				signCategory = TourDatabase.saveEntity(
						newSignCategory,
						newSignCategory.getCategoryId(),
						TourSignCategory.class);
			}

			_importedSignCategories.put(cleanCategoryKey, signCategory);

		} catch (final Exception e) {

			StatusUtil.log(sql, e);

		} finally {

			em.close();
		}

		return signCategory;
	}

	@SuppressWarnings("unchecked")
	public static SignCollection getRootSigns() {

		// check if root signs are loaded
		SignCollection rootEntry = _signCollections.get(Long.valueOf(ROOT_SIGN_ID));
		if (rootEntry != null) {
			return rootEntry;
		}

		/*
		 * read root signs from the database
		 */
		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em == null) {
			return null;
		}

		try {

			rootEntry = new SignCollection();

			/*
			 * read sign categories from db
			 */
			Query emQuery = em.createQuery(//
					//
					"SELECT signCategory" //$NON-NLS-1$
							+ (" FROM " + TourSignCategory.class.getSimpleName() + " AS signCategory") //$NON-NLS-1$ //$NON-NLS-2$
							+ (" WHERE signCategory.isRoot=1") //$NON-NLS-1$
							+ (" ORDER BY signCategory.name")); //$NON-NLS-1$

			rootEntry.tourSignCategories = (ArrayList<TourSignCategory>) emQuery.getResultList();

			/*
			 * read tour signs from db
			 */
			emQuery = em.createQuery(//
					//
					"SELECT sign" //$NON-NLS-1$
							+ (" FROM " + TourSign.class.getSimpleName() + " AS sign ") //$NON-NLS-1$ //$NON-NLS-2$
							+ (" WHERE sign.isRoot=1") //$NON-NLS-1$
							+ (" ORDER BY sign.name")); //$NON-NLS-1$

			rootEntry.tourSigns = (ArrayList<TourSign>) emQuery.getResultList();

		} finally {

			em.close();
		}

		_signCollections.put(ROOT_SIGN_ID, rootEntry);

		return rootEntry;
	}

	/**
	 * @param categoryId
	 * @return Returns a {@link SignCollection} which contains all signs and categories for the
	 *         category Id.
	 */
	public static SignCollection getSignEntries(final long categoryId) {

		final Long categoryIdValue = Long.valueOf(categoryId);

		SignCollection categoryEntries = _signCollections.get(categoryIdValue);
		if (categoryEntries != null) {
			return categoryEntries;
		}

		/*
		 * read sign category children from the database
		 */

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em == null) {
			return null;
		}

		try {

			categoryEntries = new SignCollection();

			final TourSignCategory tourSignCategory = em.find(TourSignCategory.class, categoryIdValue);

			// get signs
			final Set<TourSign> lazyTourSigns = tourSignCategory.getTourSigns();
			categoryEntries.tourSigns = new ArrayList<TourSign>(lazyTourSigns);
			Collections.sort(categoryEntries.tourSigns);

			// get categories
			final Set<TourSignCategory> lazyTourSignCategories = tourSignCategory.getTourSignCategories();
			categoryEntries.tourSignCategories = new ArrayList<TourSignCategory>(lazyTourSignCategories);
			Collections.sort(categoryEntries.tourSignCategories);

		} catch (final Exception e) {
			StatusUtil.log(e);
		} finally {

			em.close();
		}

		_signCollections.put(categoryIdValue, categoryEntries);

		return categoryEntries;
	}

	/**
	 * @param signPhoto
	 * @return Returns the photo image or <code>null</code> when image is not loaded.
	 */
	public static Image getSignImage(final Photo signPhoto) {

		return PhotoImageCache.getImage(signPhoto, SIGN_IMAGE_QUALITY);
	}

	/**
	 * @param signPhoto
	 * @param imageLoadCallback
	 *            This callback is used to load the photo image.
	 * @return Returns the photo image or <code>null</code> when image is not loaded.
	 */
	public static Image getSignImage(final Photo signPhoto, final ILoadCallBack imageLoadCallback) {

		Image photoImage = null;

		// check if image has an loading error
		final PhotoLoadingState photoLoadingState = signPhoto.getLoadingState(SIGN_IMAGE_QUALITY);

		if (photoLoadingState != PhotoLoadingState.IMAGE_IS_INVALID) {

			// image is not yet loaded

			// check if image is in the cache
			photoImage = PhotoImageCache.getImage(signPhoto, SIGN_IMAGE_QUALITY);

			if ((photoImage == null || photoImage.isDisposed())
					&& photoLoadingState == PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE == false) {

				// the requested image is not available in the image cache -> image must be loaded

				PhotoLoadManager.putImageInLoadingQueueThumbGallery(
						null,
						signPhoto,
						SIGN_IMAGE_QUALITY,
						imageLoadCallback);
			}
		}

		return photoImage;
	}

	private static void importSignImages_Final() {

		saveAllImportedSigns();

		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {

				// ensure newly saved sign are loaded
				clearTourSigns();

				PreferencesUtil.createPreferenceDialogOn(
						Display.getCurrent().getActiveShell(),
						PrefPageSigns.ID,
						null,
						null).open();
			}
		});
	}

	public static void keepImportedSign(final TourSign importedSign) {

		_importedSigns.put(importedSign.getSignKey(), importedSign);
	}

	/**
	 * !!!!!!!!!!!!!! RECURSIVE !!!!!!!!!!!!!!!!!<br>
	 * <br>
	 * <br>
	 * !!!!!!!!!!!!!! RECURSIVE !!!!!!!!!!!!!!!!!
	 * 
	 * @param fileOrFolder
	 * @param categoryName
	 * @param categoryKey
	 *            Is an empty string when parent folder is the base folder.
	 * @param parentSignCategory
	 * @param monitor
	 * @return Returns <code>true</code> if all deletions were successful
	 */
	private static void parseFolder_10_FilesFolder(	final File fileOrFolder,
													String categoryKey,
													final TourSignCategory parentSignCategory,
													final IProgressMonitor monitor) {

		if (monitor.isCanceled()) {
			return;
		}

		// update monitor every n seconds
		updateUI_Monitor(fileOrFolder, monitor);

		// get file/folder name without extension
		final Path fileOrFolderPath = new Path(fileOrFolder.getAbsolutePath());
		final String fileOrFolderName = fileOrFolderPath.removeFileExtension().lastSegment();

		final boolean isRootParent = _folderDepth == -1;
		final boolean isRoot = _folderDepth == 0;
		if (isRoot) {
			categoryKey = TourSignCategory.ROOT_KEY;
		}

		if (fileOrFolder.isDirectory()) {

			// fs item is a folder

			// create folder key from folder path.
			final String[] relativeSegments = fileOrFolderPath.removeFirstSegments(_baseFolderSegments).segments();
			final String folderKey = StringUtils.join(relativeSegments, KEY_PART_SEPARATOR);

			TourSignCategory signCategory = null;

			// don't create a sign category for the parent of the root
			if (isRootParent == false) {

				// get/create sign category
				signCategory = getImportedSignCategoryByKey(//
						fileOrFolderName,
						folderKey,
						isRoot);

				if (parentSignCategory != null) {
					parentSignCategory.addTourSignCategory(signCategory);
				}
			}

			// get all FS items
			final String[] allFSItems = fileOrFolder.list();
			for (final String fsPathName : allFSItems) {

				_folderDepth++;

				parseFolder_10_FilesFolder(//
						new File(fileOrFolder, fsPathName),
						folderKey,
						signCategory,
						monitor);

				_folderDepth--;
			}

		} else {

			// fs item is a file

			parseFolder_20_CreateTourSign(//
					fileOrFolder,
					fileOrFolderName,
					categoryKey,
					parentSignCategory,
					isRoot);
		}

		/*
		 * !!! canceled must be checked before isFileFolderDeleted is checked because this returns
		 * false when the monitor is canceled !!!
		 */
		if (monitor.isCanceled()) {
			return;
		}

		if (true) {
//			monitor.setCanceled(true);
		}
	}

	/**
	 * @param fileOrFolder
	 * @param fileOrFolderName
	 * @param categoryKey
	 * @param parentSignCategory
	 *            Parent category for this sign, can be <code>null</code> when sign is in the root.
	 * @param isRoot
	 */
	private static void parseFolder_20_CreateTourSign(	final File fileOrFolder,
														final String fileOrFolderName,
														final String categoryKey,
														final TourSignCategory parentSignCategory,
														final boolean isRoot) {

		// get/create sign
		final TourSign importedSign = getImportedSignByKey(//
				fileOrFolderName,
				categoryKey,
				isRoot);

		if (importedSign.getSignId() == TourDatabase.ENTITY_IS_NOT_SAVED) {

			// sign entity is not yet saved

			importedSign.setRoot(isRoot);
			importedSign.setImageFilePathName(fileOrFolder.getAbsolutePath());

			final TourSign savedSign = TourDatabase.saveEntity(//
					importedSign,
					importedSign.getSignId(),
					TourSign.class);

			keepImportedSign(savedSign);

			if (parentSignCategory != null) {

				// set category for this sign
				importedSign.getTourSignCategories().add(parentSignCategory);

				// this parent category has a new child
				parentSignCategory.addTourSign(savedSign);
			}
		}
	}

	public static void removeCachedCategory(final long categoryId) {

		_signCollections.remove(categoryId);
	}

	private static void saveAllImportedSigns() {

//		// save imported signs
//		for (final Entry<String, TourSign> tourSignEntry : _importedSigns.entrySet()) {
//
//			final TourSign tourSign = tourSignEntry.getValue();
//
//			final TourSign savedSign = TourDatabase.saveEntity(//
//					tourSign,
//					tourSign.getSignId(),
//					TourSign.class);
//
//			/*
//			 * Replace sign with saved sign, otherwise this error occures: deleted entity passed to
//			 * persist
//			 */
//			final TourSignCategory signCategory = savedSign.getSignCategory();
//			signCategory.addTourSign(savedSign);
//		}

		// save imported categories
		for (final Entry<String, TourSignCategory> tourSignCategoryEntry : _importedSignCategories.entrySet()) {

			final TourSignCategory tourSignCategory = tourSignCategoryEntry.getValue();

			TourDatabase.saveEntity(//
					tourSignCategory,
					tourSignCategory.getCategoryId(),
					TourSignCategory.class);
		}

		// remove imported signs/categories, saved sign/categories do have other instances
		_importedSigns.clear();
		_importedSignCategories.clear();
	}

	private static void TEMP_DB_cleanup() {

		clearTourSigns();

		Connection conn = null;
		PreparedStatement prepStmt = null;

		String sql = UI.EMPTY_STRING;

		try {

			conn = TourDatabase.getInstance().getConnection();

			final String allSql[] = {
					//
					"DELETE FROM " + TourDatabase.JOINTABLE_TOURSIGNCATEGORY_TOURSIGN, //$NON-NLS-1$
					"DELETE FROM " + TourDatabase.JOINTABLE_TOURSIGNCATEGORY_TOURSIGNCATEGORY, //$NON-NLS-1$
					//
					"DELETE FROM " + TourDatabase.TABLE_TOUR_SIGN, //$NON-NLS-1$
					"DELETE FROM " + TourDatabase.TABLE_TOUR_SIGN_CATEGORY, //$NON-NLS-1$
			//
			};

			for (final String sqlExec : allSql) {

				sql = sqlExec;

				prepStmt = conn.prepareStatement(sql);
				prepStmt.execute();
				prepStmt.close();
			}

		} catch (final SQLException e) {
			SQLUtils.showSQLException(e);
		} finally {
			TourDatabase.closeConnection(conn);
		}
	}

	private static void updateUI_Monitor(final File fileFolder, final IProgressMonitor monitor) {

		final long time = System.currentTimeMillis();

		_fsItemCounter++;

		if (time > _parseUIUpdateTime + MONITOR_UPDATE_INTERVAL) {

			_parseUIUpdateTime = time;

			monitor.subTask(NLS.bind(
					Messages.Dialog_ImportSigns_MonitorTask_SubTask,
					new Object[] { _fsItemCounter, UI.shortenText(fileFolder.toString(), 100, false) }));
		}
	}

}
