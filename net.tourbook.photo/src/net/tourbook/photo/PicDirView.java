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

import net.tourbook.common.form.ViewerDetailForm;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.Util;
import net.tourbook.photo.internal.Activator;
import net.tourbook.photo.internal.PicDirFolder;
import net.tourbook.photo.internal.PicDirImages;
import net.tourbook.photo.internal.manager.ThumbnailStore;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class PicDirView extends ViewPart implements IPhotoEventListener {

	static public final String				ID									= "net.tourbook.photo.PicDirView";		//$NON-NLS-1$

	private static final String				SEPARATOR_ID_PIC_DIR_VIEW_TOOL_BAR	= "PicDirViewToolBar";					//$NON-NLS-1$

	private static final String				STATE_TREE_WIDTH					= "STATE_TREE_WIDTH";					//$NON-NLS-1$

	private static final IDialogSettings	_state								= Activator.getDefault()//
																						.getDialogSettingsSection(
																								"PhotoDirectoryView");	//$NON-NLS-1$
	private static final IPreferenceStore	_prefStore							= Activator.getDefault()//
																						.getPreferenceStore();
	private IPartListener2					_partListener;
	private IPropertyChangeListener			_prefChangeListener;

	private PostSelectionProvider			_postSelectionProvider;

	private PicDirFolder					_picDirFolder;
	private PicDirImages					_picDirImages;

	private ISelectionConverter				_selectionConverter;

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
	public static void sortFiles(final File[] files) {

		/* Very lazy merge sort algorithm */
		sortBlock(files, 0, files.length - 1, new File[files.length]);
	}

	public void actionRefreshFolder() {
		_picDirFolder.actionRefreshFolder();
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

	@Override
	public void createPartControl(final Composite parent) {

		fillActionBarsBeforeUI();

		createUI(parent);

		fillActionBars();

		addPartListener();
		addPrefListener();
		PhotoManager.addPhotoEventListener(this);

		// set selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

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

		_picDirImages = new PicDirImages(this, _state);
		_picDirFolder = new PicDirFolder(this, _picDirImages, _state);

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
				_picDirImages.createUI(_containerImages, _picDirFolder);
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

		PhotoManager.removePhotoEventListener(this);

		getViewSite().getPage().removePartListener(_partListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	/**
	 * fill view menu
	 */
	private void fillActionBars() {

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

		_picDirImages.fillViewMenu(menuMgr);
	}

	/**
	 * fill view toolbar
	 */
	private void fillActionBarsBeforeUI() {

		final IToolBarManager tbmMgr = getViewSite().getActionBars().getToolBarManager();

		tbmMgr.add(new Separator(SEPARATOR_ID_PIC_DIR_VIEW_TOOL_BAR));
	}

	public void fireCurrentSelection() {

		final ISelection selectedPhotosWithExif = _picDirImages.getSelectedPhotosWithExif(false);

		fireSelection(selectedPhotosWithExif);
	}

	private void fireSelection(final ISelection selection) {

		// fire selection for the selected photos
		if (selection != null) {

			_postSelectionProvider.setSelection(selection);

//			/*
//			 * reset selection because this selection opens a perspective and it causes a runtime
//			 * exception when this selection is fired again, it causes really trouble !!!
//			 */
//			_postSelectionProvider.setSelection(new ISelection() {
//				@Override
//				public boolean isEmpty() {
//					return true;
//				}
//			});
		}
	}

	/**
	 * Creates a {@link PhotosWithExifSelection}
	 * 
	 * @param isAllImages
	 *            When <code>true</code>, all images which are displayed in the gallery are
	 *            returned, otherwise the selected images.
	 * @return Returns a {@link ISelection} for selected or all images or <code>null</code> when
	 *         loading EXIF data was canceled by the user.
	 */
	public PhotosWithExifSelection getSelectedPhotosWithExif(final boolean isAllImages) {
		return _picDirImages.getSelectedPhotosWithExif(isAllImages);
	}

	private void onPartClose() {

		// also stop loading images
		_picDirImages.stopLoadingImages();

		ThumbnailStore.cleanupStoreFiles(false, false);

		saveState();
	}

	@Override
	public void photoEvent(final IViewPart viewPart, final PhotoEventId photoEventId, final Object data) {

		_picDirImages.photoEvent(photoEventId, data);
	}

	public void refreshUI() {

		_picDirImages.refreshUI();
	}

	public void registerContextMenu(final String menuId, final MenuManager menuMgr) {

		getSite().registerContextMenu(menuId, menuMgr, _postSelectionProvider);
	}

	private void restoreState() {

		_containerMasterDetail.setViewerWidth(Util.getStateInt(_state, STATE_TREE_WIDTH, 200));

		/*
		 * image restore must be done BEFORE folder restore because folder restore is also loading
		 * the folder and updates folder history
		 */
		// 1.
		_picDirImages.restoreState();

		// 2.
		_picDirFolder.restoreState();

		setFocus();

		// set focus
//		getViewSite().getPage().activate(this);
	}

	private void saveState() {

		if (_containerFolder.isDisposed()) {
			// this happened
			return;
		}

		// keep width of the dir folder view in the master detail container
		_state.put(STATE_TREE_WIDTH, _containerMasterDetail.getViewerWidth());

		_picDirFolder.saveState();
		_picDirImages.saveState();
	}

	@Override
	public void setFocus() {

		// 1st set focus to the gallery when it is maximized
		final Control maximizedControl = _containerMasterDetail.getMaximizedControl();

		if (maximizedControl == _containerImages) {
			_picDirImages.setFocus();
		} else {
			_picDirFolder.getTree().setFocus();
		}
	}

	public void setMaximizedControl(final boolean isShowFolderAndGallery) {
		_containerMasterDetail.setMaximizedControl(isShowFolderAndGallery ? null : _containerImages);
	}

	public void setSelection(ISelection selection) {

		if (_selectionConverter != null) {

			/*
			 * convert default selection into a selection from the selection type provider, it
			 * mainly converts into another type, that the new type is fired instead of the old
			 */
			selection = _selectionConverter.convertSelection(selection);
		}

		fireSelection(selection);
	}

	public void setSelectionConverter(final ISelectionConverter selectionConverter) {
		_selectionConverter = selectionConverter;
	}

}
