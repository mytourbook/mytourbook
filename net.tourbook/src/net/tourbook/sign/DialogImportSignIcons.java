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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.SQLUtils;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourPhoto;
import net.tourbook.data.TourSign;
import net.tourbook.data.TourSignCategory;
import net.tourbook.database.TourDatabase;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotosWithExifSelection;
import net.tourbook.preferences.PrefPageSigns;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Dialog to import tour sign icons.
 */
public class DialogImportSignIcons extends TitleAreaDialog {

	/**
	 * Update interval in ms.
	 */
	private static final int		MONITOR_UPDATE_INTERVAL	= 100;

	private final IDialogSettings	_state					= TourbookPlugin.getState("DialogImportTourSignIcons"); //$NON-NLS-1$

	private long					_parseUIUpdateTime;
	private int						_fsItemCounter;

	private Path					_baseFolder;
	protected int					_folderDepth;

	/**
	 * Number of segments in the {@link #_baseFolder}.
	 */
	private int						_baseFolderSements;

	/*
	 * none UI
	 */
	private PixelConverter			_pc;

	public class SignName {

	}

	/*
	 * UI controls
	 */

	/**
	 * Imports all images from the selected folder and all subfolders, the foldername will be used
	 * as the category name.
	 * 
	 * @param parentShell
	 * @param selectedFolder
	 */
	public DialogImportSignIcons(final Shell parentShell, final File selectedFolder) {

		super(parentShell);

//		TEMP_DB_cleanup();

		parseFolder(selectedFolder);
	}

	/**
	 * @param parentShell
	 * @param selectedPhotosWithExif
	 * @param tourData
	 * @param initialTourMarker
	 *            TourMarker which is selected when the dialog is opened
	 */
	public DialogImportSignIcons(final Shell parentShell, final PhotosWithExifSelection selectedPhotosWithExif) {

		super(parentShell);

//		TEMP_DB_cleanup();

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

		// set icon for the window
		setDefaultImage(TourbookPlugin.getImageDescriptor(Messages.Image__TourSignImport).createImage());

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
	}

	@Override
	public boolean close() {

		saveState();

		SignManager.saveAllImportedSigns();

		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {

				// ensure newly saved sign are loaded
				SignManager.clearTourSigns();

				PreferencesUtil.createPreferenceDialogOn(getShell(), PrefPageSigns.ID, null, null).open();
			}
		});

		return super.close();
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.Dialog_ImportSigns_Dialog_Title);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite dlgContainer = (Composite) super.createDialogArea(parent);

		createUI(dlgContainer);

		restoreState();

		setTitle(Messages.Dialog_ImportSigns_Dialog_Title);
		setMessage(Messages.Dialog_ImportSigns_Dialog_Title_Message);

		enableControls();

		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {
				close();
			}
		});

		return dlgContainer;
	}

	private void createUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		final Composite marginContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(marginContainer);
		GridLayoutFactory.swtDefaults().applyTo(marginContainer);
		{
			final Composite dlgContainer = new Composite(marginContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(dlgContainer);
			GridLayoutFactory.swtDefaults().applyTo(dlgContainer);
			{
				final Label label = new Label(dlgContainer, SWT.NONE);
				GridDataFactory.fillDefaults().applyTo(label);
				label.setText("test"); //$NON-NLS-1$

			}
		}
	}

	private void enableControls() {

	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		return _state;
//		return null;
	}

	/**
	 * @param imageFolder
	 * @return Returns number of photos which set in {@link TourPhoto}s for a given folder.
	 */
	private ArrayList<String> getTourPhotos(final String imageFolder) {

		final ArrayList<String> tourPhotoImages = new ArrayList<String>();

		Connection conn = null;

		try {

			conn = TourDatabase.getInstance().getConnection();

			final String sql = "SELECT imageFileName" // 						//$NON-NLS-1$
					+ " FROM " + TourDatabase.TABLE_TOUR_PHOTO //			//$NON-NLS-1$
					+ " WHERE imageFilePath=?"; //							//$NON-NLS-1$

			final PreparedStatement stmt = conn.prepareStatement(sql);

			stmt.setString(1, imageFolder);

			final ResultSet result = stmt.executeQuery();

			while (result.next()) {
				tourPhotoImages.add(result.getString(1));
			}

		} catch (final SQLException e) {
			SQLUtils.showSQLException(e);
		} finally {
			Util.closeSql(conn);
		}

		return tourPhotoImages;
	}

	@Override
	protected void okPressed() {

		saveIcons();

		super.okPressed();
	}

	private void parseFolder(final File selectedFolder) {

		_baseFolder = new Path(selectedFolder.getAbsolutePath());
		_baseFolderSements = _baseFolder.segmentCount();

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

			new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, true, runnable);

		} catch (final Exception e) {
			StatusUtil.showStatus(e);
		}
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
	private void parseFolder_10_FilesFolder(final File fileOrFolder,
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
			final String[] relativeSegments = fileOrFolderPath.removeFirstSegments(_baseFolderSements).segments();
			final String folderKey = StringUtils.join(relativeSegments, SignManager.KEY_PART_SEPARATOR);

			TourSignCategory signCategory = null;

			// don't create a sign category for the parent of the root
			if (isRootParent == false) {

				// get/create sign category
				signCategory = SignManager.getImportedSignCategoryByKey(//
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
	private void parseFolder_20_CreateTourSign(	final File fileOrFolder,
												final String fileOrFolderName,
												final String categoryKey,
												final TourSignCategory parentSignCategory,
												final boolean isRoot) {

		// get/create sign
		final TourSign importedSign = SignManager.getImportedSignByKey(//
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

			SignManager.keepImportedSign(savedSign);

			if (parentSignCategory != null) {

				// set category for this sign
				importedSign.getTourSignCategories().add(parentSignCategory);

				// this parent category has a new child
				parentSignCategory.addTourSign(savedSign);
			}
		}
	}

	private void restoreState() {

	}

	private void saveIcons() {

//		for (final Photo signIcon : _signIcons) {
//
//			final Path signPath = new Path(signIcon.imageFilePathName);
//			final String signName = signPath.removeFileExtension().lastSegment();
//
//			System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//					+ ("\tsignName: " + signName));
//			// TODO remove SYSTEM.OUT.PRINTLN
//
//			final String[] signNameParts = signName.split(SIGN_NAME_SEPARATOR);
//
//			// process categories
//			for (int partIndex = 0; partIndex < signNameParts.length - 1; partIndex++) {
//
//				final String categoryName = signNameParts[partIndex];
//
//				SignManager.getSignEntries(categoryName);
//			}
//
//			// process tour sign (last part)
//		}
	}

	private void saveState() {

	}

	private void TEMP_DB_cleanup() {

		SignManager.clearTourSigns();

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

	private void updateUI_Monitor(final File fileFolder, final IProgressMonitor monitor) {

		final long time = System.currentTimeMillis();

		_fsItemCounter++;

		if (time > _parseUIUpdateTime + MONITOR_UPDATE_INTERVAL) {

			_parseUIUpdateTime = time;

			monitor.subTask(NLS.bind(
					Messages.Dialog_ImportSigns_MonitorTask_SubTask,
					new Object[] { _fsItemCounter, UI.shortenText(fileFolder.toString(), 100) }));
		}
	}

}
