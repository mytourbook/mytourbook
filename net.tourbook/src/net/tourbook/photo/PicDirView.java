/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.photo;

import java.io.File;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.photo.manager.ThumbnailStore;
import net.tourbook.ui.ViewerDetailForm;
import net.tourbook.util.Util;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class PicDirView extends ViewPart {

	static public final String				ID									= "net.tourbook.photo.PicDirView";			//$NON-NLS-1$

	static final int						GALLERY_SORTING_BY_DATE				= 0;
	static final int						GALLERY_SORTING_BY_NAME				= 1;

	private static final String				STATE_TREE_WIDTH					= "STATE_TREE_WIDTH";						//$NON-NLS-1$
	private static final String				STATE_GALLERY_SORTING				= "STATE_GALLERY_SORTING";					//$NON-NLS-1$
	private static final String				STATE_IS_SHOW_PHOTO_NAME_IN_GALLERY	= "STATE_IS_SHOW_PHOTO_NAME_IN_GALLERY";	//$NON-NLS-1$
	private static final String				STATE_IS_SHOW_PHOTO_DATE_IN_GALLERY	= "STATE_IS_SHOW_PHOTO_DATE_IN_GALLERY";	//$NON-NLS-1$

	private static final IDialogSettings	_state								= TourbookPlugin.getDefault()//
																						.getDialogSettingsSection(
																								"PhotoDirectoryView");		//$NON-NLS-1$
	private static final IPreferenceStore	_prefStore							= TourbookPlugin.getDefault()//
																						.getPreferenceStore();

	private int								_gallerySorting;

	private IPartListener2					_partListener;
	private IPropertyChangeListener			_prefChangeListener;

	private PicDirFolder					_picDirFolder;
	private PicDirImages					_picDirImages;

	private ActionShowPhotoDate				_actionShowPhotoDate;
	private ActionShowPhotoName				_actionShowPhotoName;
	private ActionSortByFileDate			_actionSortFileByDate;
	private ActionSortByFileName			_actionSortByFileName;

	/*
	 * UI controls
	 */
	private ViewerDetailForm				_containerMasterDetail;
	private Composite						_containerFolder;
	private Composite						_containerImages;

	static int compareFiles(final File file1, final File file2) {

//		boolean aIsDir = a.isDirectory();
//		boolean bIsDir = b.isDirectory();
//		if (aIsDir && ! bIsDir) return -1;
//		if (bIsDir && ! aIsDir) return 1;

		// sort case-sensitive files in a case-insensitive manner
		final String file1Name = file1.getName();
		final String file2Name = file2.getName();

		// try to sort by numbers
		try {

			final int file1No = Integer.parseInt(file1Name);
			final int file2No = Integer.parseInt(file2Name);

			return file1No - file2No;

		} catch (final Exception e) {
			// at least one filename co not contain a number, sort by string
		}

		int compare = file1Name.compareToIgnoreCase(file2Name);

		if (compare == 0) {
			compare = file1Name.compareTo(file2Name);
		}
		return compare;
	}

	/**
	 * Gets a directory listing
	 * 
	 * @param file
	 *            the directory to be listed
	 * @return an array of files this directory contains, may be empty but not null
	 */
	static File[] getDirectoryList(final File file) {
		final File[] list = file.listFiles();
		if (list == null) {
			return new File[0];
		}
		sortFiles(list);
		return list;
	}

	private static void sortBlock(final File[] files, final int start, final int end, final File[] mergeTemp) {
		final int length = end - start + 1;
		if (length < 8) {
			for (int i = end; i > start; --i) {
				for (int j = end; j > start; --j) {
					if (compareFiles(files[j - 1], files[j]) > 0) {
						final File temp = files[j];
						files[j] = files[j - 1];
						files[j - 1] = temp;
					}
				}
			}
			return;
		}
		final int mid = (start + end) / 2;
		sortBlock(files, start, mid, mergeTemp);
		sortBlock(files, mid + 1, end, mergeTemp);
		int x = start;
		int y = mid + 1;
		for (int i = 0; i < length; ++i) {
			if ((x > mid) || ((y <= end) && compareFiles(files[x], files[y]) > 0)) {
				mergeTemp[i] = files[y++];
			} else {
				mergeTemp[i] = files[x++];
			}
		}
		for (int i = 0; i < length; ++i) {
			files[i + start] = mergeTemp[i];
		}
	}

	/**
	 * Sorts files lexicographically by name.
	 * 
	 * @param files
	 *            the array of Files to be sorted
	 */
	static void sortFiles(final File[] files) {

		/* Very lazy merge sort algorithm */
		sortBlock(files, 0, files.length - 1, new File[files.length]);
	}

	void actionShowPhotoInfo() {
		_picDirImages.showInfo(_actionShowPhotoName.isChecked(), _actionShowPhotoDate.isChecked(), true);
	}

	void actionSortByDate() {

		final boolean isChecked = _actionSortFileByDate.isChecked();

		if (isChecked) {
			_gallerySorting = GALLERY_SORTING_BY_DATE;
			_actionSortByFileName.setChecked(false);
		} else {
			_gallerySorting = GALLERY_SORTING_BY_NAME;
			_actionSortByFileName.setChecked(true);
		}

		sortGallery();
	}

	void actionSortByName() {

		final boolean isChecked = _actionSortByFileName.isChecked();

		if (isChecked) {
			_gallerySorting = GALLERY_SORTING_BY_NAME;
			_actionSortFileByDate.setChecked(false);
		} else {
			_gallerySorting = GALLERY_SORTING_BY_DATE;
			_actionSortFileByDate.setChecked(true);
		}

		sortGallery();
	}

	private void addPartListener() {

		_partListener = new IPartListener2() {
			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == PicDirView.this) {
					onPartClose();
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				_picDirFolder.handlePrefStoreModifications(event);
				_picDirImages.handlePrefStoreModifications(event);
			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	private void createActions() {

		_actionShowPhotoName = new ActionShowPhotoName(this);
		_actionShowPhotoDate = new ActionShowPhotoDate(this);

		_actionSortByFileName = new ActionSortByFileName(this);
		_actionSortFileByDate = new ActionSortByFileDate(this);
	}

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);
		createActions();

		fillToolbar();

		addPartListener();
		addPrefListener();

		restoreState();
	}

	private void createUI(final Composite parent) {

		_picDirImages = new PicDirImages(this);
		_picDirFolder = new PicDirFolder(this, _picDirImages);

		final Composite masterDetailContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(masterDetailContainer);
		GridLayoutFactory.fillDefaults().applyTo(masterDetailContainer);
		{
			// file folder
			_containerFolder = new Composite(masterDetailContainer, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(_containerFolder);
			GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(_containerFolder);
			{
				_picDirFolder.createUI(_containerFolder);
			}

			// sash
			final Sash sash = new Sash(masterDetailContainer, SWT.VERTICAL);

			// photos
			_containerImages = new Composite(masterDetailContainer, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(_containerImages);
			GridLayoutFactory.fillDefaults().applyTo(_containerImages);
//			_containerImages.setLayout(new FillLayout());
			{
				_picDirImages.createUI(_picDirFolder, _containerImages);
			}

			// master/detail form
			_containerMasterDetail = new ViewerDetailForm(
					masterDetailContainer,
					_containerFolder,
					sash,
					_containerImages);
		}
	}

	@Override
	public void dispose() {

		getViewSite().getPage().removePartListener(_partListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void fillToolbar() {
		/*
		 * fill view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(_actionShowPhotoName);
		tbm.add(_actionShowPhotoDate);
		tbm.add(new Separator());

		tbm.add(_actionSortByFileName);
		tbm.add(_actionSortFileByDate);
	}

	private void onPartClose() {

		// close images first, this will stop loading images
		_picDirImages.onClose();

		ThumbnailStore.cleanupStoreFiles(false, false);

		saveState();
	}

	private void restoreState() {

		_containerMasterDetail.setViewerWidth(Util.getStateInt(_state, STATE_TREE_WIDTH, 200));

		/*
		 * photo info
		 */
		final boolean isShowPhotoName = Util.getStateBoolean(_state, STATE_IS_SHOW_PHOTO_NAME_IN_GALLERY, true);
		final boolean isShowPhotoDate = Util.getStateBoolean(_state, STATE_IS_SHOW_PHOTO_DATE_IN_GALLERY, true);

		_actionShowPhotoName.setChecked(isShowPhotoName);
		_actionShowPhotoDate.setChecked(isShowPhotoDate);
		_picDirImages.showInfo(isShowPhotoName, isShowPhotoDate, false);

		/*
		 * gallery sorting
		 */
		_gallerySorting = Util.getStateInt(_state, STATE_GALLERY_SORTING, GALLERY_SORTING_BY_DATE);
		_actionSortFileByDate.setChecked(_gallerySorting == GALLERY_SORTING_BY_DATE);
		_actionSortByFileName.setChecked(_gallerySorting == GALLERY_SORTING_BY_NAME);

		_picDirImages.sortGallery(_gallerySorting, false);

		/*
		 * image restore must be done BEFORE folder restore because folder restore is also loading
		 * the folder and updates folder history
		 */
		// 1.
		_picDirImages.restoreState(_state);

		// 2.
		_picDirFolder.restoreState(_state);
	}

	private void saveState() {

		if (_containerFolder.isDisposed()) {
			// this happened
			return;
		}

		// gallery sorting
		_state.put(STATE_GALLERY_SORTING, _actionSortFileByDate.isChecked()
				? GALLERY_SORTING_BY_DATE
				: GALLERY_SORTING_BY_NAME);

		// keep width of the dir folder view in the master detail container
		final Tree tree = _picDirFolder.getTree();
		if (tree != null) {
			_state.put(STATE_TREE_WIDTH, tree.getSize().x);
		}

		_state.put(STATE_IS_SHOW_PHOTO_NAME_IN_GALLERY, _actionShowPhotoName.isChecked());
		_state.put(STATE_IS_SHOW_PHOTO_DATE_IN_GALLERY, _actionShowPhotoDate.isChecked());

		_picDirFolder.saveState(_state);
		_picDirImages.saveState(_state);
	}

	@Override
	public void setFocus() {
		_picDirFolder.getTree().setFocus();
	}

	private void sortGallery() {
		_picDirImages.sortGallery(_gallerySorting, true);
	}

}
