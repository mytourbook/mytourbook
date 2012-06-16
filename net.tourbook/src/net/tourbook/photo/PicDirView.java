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
import net.tourbook.photo.manager.GallerySorting;
import net.tourbook.photo.manager.ThumbnailStore;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.UI;
import net.tourbook.ui.ViewerDetailForm;
import net.tourbook.util.PostSelectionProvider;
import net.tourbook.util.Util;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class PicDirView extends ViewPart {

	static public final String				ID									= "net.tourbook.photo.PicDirView";			//$NON-NLS-1$

	private static final String				STATE_TREE_WIDTH					= "STATE_TREE_WIDTH";						//$NON-NLS-1$
	private static final String				STATE_GALLERY_SORTING				= "STATE_GALLERY_SORTING";					//$NON-NLS-1$
	private static final String				STATE_IMAGE_FILTER					= "STATE_IMAGE_FILTER";					//$NON-NLS-1$
	private static final String				STATE_PHOTO_INFO_DATE				= "STATE_PHOTO_INFO_DATE";					//$NON-NLS-1$
	private static final String				STATE_IS_SHOW_PHOTO_NAME_IN_GALLERY	= "STATE_IS_SHOW_PHOTO_NAME_IN_GALLERY";	//$NON-NLS-1$
	private static final String				STATE_IS_SHOW_PHOTO_GPS_ANNOTATION	= "STATE_IS_SHOW_PHOTO_GPS_ANNOTATION";	//$NON-NLS-1$
	private static final String				STATE_IS_SHOW_PHOTO_TOOLTIP			= "STATE_IS_SHOW_PHOTO_TOOLTIP";			//$NON-NLS-1$

	static final String						IMAGE_PHOTO_FILTER_NO_FILTER		= "IMAGE_PHOTO_FILTER_NO_FILTER";			//$NON-NLS-1$
	static final String						IMAGE_PHOTO_FILTER_GPS				= "IMAGE_PHOTO_FILTER_GPS";				//$NON-NLS-1$
	static final String						IMAGE_PHOTO_FILTER_NO_GPS			= "IMAGE_PHOTO_FILTER_NO_GPS";				//$NON-NLS-1$

	private static final IDialogSettings	_state								= TourbookPlugin.getDefault()//
																						.getDialogSettingsSection(
																								"PhotoDirectoryView");		//$NON-NLS-1$
	private static final IPreferenceStore	_prefStore							= TourbookPlugin.getDefault()//
																						.getPreferenceStore();

	private GallerySorting					_gallerySorting;

	private IPartListener2					_partListener;
	private IPropertyChangeListener			_prefChangeListener;

	private PicDirFolder					_picDirFolder;
	private PicDirImages					_picDirImages;

	private ActionImageFilterGPS			_actionImageFilterGPS;
	private ActionImageFilterNoGPS			_actionImageFilterNoGPS;
	private ActionShowPhotoName				_actionShowPhotoName;
	private ActionShowPhotoDate				_actionShowPhotoDate;
	private ActionShowPhotoTooltip			_actionShowPhotoTooltip;
	private ActionShowGPSAnnotations		_actionShowGPSAnnotation;
	private ActionSortByFileDate			_actionSortFileByDate;
	private ActionSortByFileName			_actionSortByFileName;

	/*
	 * UI controls
	 */
	private ViewerDetailForm				_containerMasterDetail;
	private Composite						_containerFolder;
	private Composite						_containerImages;

	private ImageFilter						_currentImageFilter					= ImageFilter.NoFilter;

	static {
		UI.IMAGE_REGISTRY.put(
				IMAGE_PHOTO_FILTER_NO_FILTER,
				TourbookPlugin.getImageDescriptor(Messages.Image__PhotoFilterNoFilter));
		UI.IMAGE_REGISTRY.put(//
				IMAGE_PHOTO_FILTER_GPS,
				TourbookPlugin.getImageDescriptor(Messages.Image__PhotoFilterGPS));
		UI.IMAGE_REGISTRY.put(
				IMAGE_PHOTO_FILTER_NO_GPS,
				TourbookPlugin.getImageDescriptor(Messages.Image__PhotoFilterNoGPS));
	}

	private int								_thumbnailSize;
	private int								_textMinThumbSize;

	private PhotoDateInfo					_photoDateInfo;

	private PostSelectionProvider			_postSelectionProvider;

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

	void actionImageFilter(final Action actionImageFilter) {

		/*
		 * get selected filter, uncheck other
		 */
		if (actionImageFilter == _actionImageFilterGPS) {

			_currentImageFilter = actionImageFilter.isChecked() ? ImageFilter.GPS : ImageFilter.NoFilter;

			_actionImageFilterNoGPS.setChecked(false);

		} else if (actionImageFilter == _actionImageFilterNoGPS) {

			_currentImageFilter = actionImageFilter.isChecked() ? ImageFilter.NoGPS : ImageFilter.NoFilter;

			_actionImageFilterGPS.setChecked(false);
		}

		// update gallery

		_picDirImages.filterGallery(_currentImageFilter);
	}

	void actionShowPhotoInfo(final Action action) {

		if (action == _actionShowPhotoDate) {

			// toggle date info

			if (_photoDateInfo == PhotoDateInfo.NoDateTime) {

				// nothing -> date

				_photoDateInfo = PhotoDateInfo.Date;

			} else if (_photoDateInfo == PhotoDateInfo.Date) {

				// date -> time

				_photoDateInfo = PhotoDateInfo.Time;

			} else if (_photoDateInfo == PhotoDateInfo.Time) {

				// time -> date/time

				_photoDateInfo = PhotoDateInfo.DateTime;

			} else {

				// time -> nothing

				_photoDateInfo = PhotoDateInfo.NoDateTime;
			}

			_actionShowPhotoDate.setChecked(_photoDateInfo != PhotoDateInfo.NoDateTime);
		}

		_picDirImages.showInfo(//
				_actionShowPhotoName.isChecked(),
				_photoDateInfo,
				_actionShowGPSAnnotation.isChecked(),
				_actionShowPhotoTooltip.isChecked());
	}

	void actionSortByDate() {

		final boolean isChecked = _actionSortFileByDate.isChecked();

		if (isChecked) {
			_gallerySorting = GallerySorting.FILE_DATE;
			_actionSortByFileName.setChecked(false);
		} else {
			_gallerySorting = GallerySorting.FILE_NAME;
			_actionSortByFileName.setChecked(true);
		}

		_picDirImages.sortGallery(_gallerySorting);
	}

	void actionSortByName() {

		final boolean isChecked = _actionSortByFileName.isChecked();

		if (isChecked) {
			_gallerySorting = GallerySorting.FILE_NAME;
			_actionSortFileByDate.setChecked(false);
		} else {
			_gallerySorting = GallerySorting.FILE_DATE;
			_actionSortFileByDate.setChecked(true);
		}

		_picDirImages.sortGallery(_gallerySorting);
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

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.PHOTO_VIEWER_PREF_EVENT_IMAGE_VIEWER_UI_IS_MODIFIED)) {
					updateFromPrefStore();
				}

				_picDirFolder.handlePrefStoreModifications(event);
				_picDirImages.handlePrefStoreModifications(event);
			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	private void createActions() {

		_actionImageFilterGPS = new ActionImageFilterGPS(this);
		_actionImageFilterNoGPS = new ActionImageFilterNoGPS(this);

		_actionShowGPSAnnotation = new ActionShowGPSAnnotations(this);

		_actionShowPhotoName = new ActionShowPhotoName(this);
		_actionShowPhotoDate = new ActionShowPhotoDate(this);
		_actionShowPhotoTooltip = new ActionShowPhotoTooltip(this);

		_actionSortByFileName = new ActionSortByFileName(this);
		_actionSortFileByDate = new ActionSortByFileDate(this);
	}

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);
		createActions();

		fillActionBars();

		addPartListener();
		addPrefListener();

		// set selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider());

		/*
		 * restore async because a previous folder can contain many files and it can take a long
		 * time to show the UI, this can be worrisome for the user when the UI is not displayed
		 * during application startup
		 */
		parent.getDisplay().asyncExec(new Runnable() {
			public void run() {
				restoreState();
			}
		});
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

	private void enableActions() {

		final boolean isEnableGalleryText = _thumbnailSize >= _textMinThumbSize;

		_actionShowPhotoName.setEnabled(isEnableGalleryText);
		_actionShowPhotoDate.setEnabled(isEnableGalleryText);
	}

	/**
	 * fill view toolbar
	 */
	private void fillActionBars() {

		/*
		 * fill view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(_actionShowPhotoDate);
		tbm.add(_actionShowPhotoName);
		tbm.add(_actionShowPhotoTooltip);
		tbm.add(_actionShowGPSAnnotation);

		tbm.add(new Separator());
		tbm.add(_actionImageFilterGPS);
		tbm.add(_actionImageFilterNoGPS);

		tbm.add(new Separator());
		tbm.add(_actionSortFileByDate);
		tbm.add(_actionSortByFileName);

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

		_picDirImages.fillViewMenu(menuMgr);
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
		 * image filter
		 */
		final String prefImageFilter = Util.getStateString(_state, STATE_IMAGE_FILTER, ImageFilter.NoFilter.name());
		try {
			_currentImageFilter = ImageFilter.valueOf(prefImageFilter);
		} catch (final Exception e) {
			_currentImageFilter = ImageFilter.NoFilter;
		}
		_actionImageFilterGPS.setChecked(_currentImageFilter == ImageFilter.GPS);
		_actionImageFilterNoGPS.setChecked(_currentImageFilter == ImageFilter.NoGPS);

		/*
		 * photo info
		 */
		final String prefDateInfo = Util.getStateString(_state, STATE_PHOTO_INFO_DATE, PhotoDateInfo.Date.name());
		try {
			_photoDateInfo = PhotoDateInfo.valueOf(prefDateInfo);
		} catch (final Exception e) {
			_photoDateInfo = PhotoDateInfo.DateTime;
		}

		final boolean isShowPhotoName = Util.getStateBoolean(_state, STATE_IS_SHOW_PHOTO_NAME_IN_GALLERY, true);
		final boolean isShowTooltip = Util.getStateBoolean(_state, STATE_IS_SHOW_PHOTO_TOOLTIP, true);
		final boolean isShowPhotoAnnotations = Util.getStateBoolean(_state, //
				STATE_IS_SHOW_PHOTO_GPS_ANNOTATION,
				true);

		_actionShowPhotoName.setChecked(isShowPhotoName);
		_actionShowPhotoDate.setChecked(_photoDateInfo != PhotoDateInfo.NoDateTime);
		_actionShowPhotoTooltip.setChecked(isShowTooltip);
		_actionShowGPSAnnotation.setChecked(isShowPhotoAnnotations);

		_picDirImages.restoreInfo(isShowPhotoName, _photoDateInfo, isShowPhotoAnnotations, isShowTooltip);

		/*
		 * gallery sorting
		 */
		final String prefSorting = Util.getStateString(_state, STATE_GALLERY_SORTING, GallerySorting.FILE_DATE.name());
		try {
			_gallerySorting = GallerySorting.valueOf(prefSorting);
		} catch (final Exception e) {
			_gallerySorting = GallerySorting.FILE_DATE;
		}
		_actionSortFileByDate.setChecked(_gallerySorting == GallerySorting.FILE_DATE);
		_actionSortByFileName.setChecked(_gallerySorting == GallerySorting.FILE_NAME);

		_picDirImages.setSorting(_gallerySorting);
		_picDirImages.setFilter(_currentImageFilter);

		/*
		 * image restore must be done BEFORE folder restore because folder restore is also loading
		 * the folder and updates folder history
		 */
		// 1.
		_picDirImages.restoreState(_state);

		// 2.
		_picDirFolder.restoreState(_state);

		/*
		 * the thumbnail size enables/disables actions
		 */
		_thumbnailSize = _picDirImages.getThumbnailSize();
		_textMinThumbSize = _prefStore.getInt(ITourbookPreferences.PHOTO_VIEWER_TEXT_MIN_THUMB_SIZE);

		enableActions();
	}

	private void saveState() {

		if (_containerFolder.isDisposed()) {
			// this happened
			return;
		}

		/*
		 * gallery sorting
		 */
		_state.put(STATE_GALLERY_SORTING, _actionSortFileByDate.isChecked()
				? GallerySorting.FILE_DATE.name()
				: GallerySorting.FILE_NAME.name());

		// keep width of the dir folder view in the master detail container
		_state.put(STATE_TREE_WIDTH, _containerMasterDetail.getViewerWidth());

		_state.put(STATE_IS_SHOW_PHOTO_NAME_IN_GALLERY, _actionShowPhotoName.isChecked());
		_state.put(STATE_IS_SHOW_PHOTO_TOOLTIP, _actionShowPhotoTooltip.isChecked());
		_state.put(STATE_IS_SHOW_PHOTO_GPS_ANNOTATION, _actionShowGPSAnnotation.isChecked());

		_state.put(STATE_PHOTO_INFO_DATE, _photoDateInfo.name());
		_state.put(STATE_IMAGE_FILTER, _currentImageFilter.name());

		_picDirFolder.saveState(_state);
		_picDirImages.saveState(_state);
	}

	@Override
	public void setFocus() {
		_picDirFolder.getTree().setFocus();
	}

	void setMaximizedControl(final boolean isShowFolderAndGallery) {
		_containerMasterDetail.setMaximizedControl(isShowFolderAndGallery ? null : _containerImages);
	}

	void setSelection(final StructuredSelection structuredSelection) {
		_postSelectionProvider.setSelection(structuredSelection);
	}

	void setThumbnailSize(final int photoWidth) {

		_thumbnailSize = photoWidth;

		enableActions();
	}

	private void updateFromPrefStore() {

		_textMinThumbSize = _prefStore.getInt(ITourbookPreferences.PHOTO_VIEWER_TEXT_MIN_THUMB_SIZE);

		enableActions();
	}
}
