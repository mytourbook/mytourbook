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
import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.photo.manager.Photo;
import net.tourbook.photo.manager.PhotoManager;
import net.tourbook.ui.UI;
import net.tourbook.ui.ViewerDetailForm;
import net.tourbook.util.PostSelectionProvider;
import net.tourbook.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class PicDirView extends ViewPart {

	static public final String				ID							= "net.tourbook.photo.PicDirView";		//$NON-NLS-1$

	private static final String				STATE_TREE_WIDTH			= "STATE_TREE_WIDTH";					//$NON-NLS-1$
	private static final String				STATE_THUMB_IMAGE_SIZE		= "STATE_THUMB_IMAGE_SIZE";			//$NON-NLS-1$
	private static final String				STATE_SELECTED_FOLDER		= "STATE_SELECTED_FOLDER";				//$NON-NLS-1$

	private static final IDialogSettings	_state						= TourbookPlugin.getDefault()//
																				.getDialogSettingsSection(
																						"PhotoDirectoryView");	//$NON-NLS-1$
	private static final IPreferenceStore	_prefStore					= TourbookPlugin.getDefault()//
																				.getPreferenceStore();

	private IPartListener2					_partListener;
	private IPropertyChangeListener			_prefChangeListener;
	private PostSelectionProvider			_postSelectionProvider;

	private ArrayList<Photo>				_photos						= new ArrayList<Photo>();

	private PicDirFolder					_picDirFolder;
	private PicDirImages					_picDirImages;

	private int								_prevSelectedThumbnailIndex	= -1;

	/*
	 * UI resources
	 */
	private Color							_fgColor					= new Color(
																				Display.getDefault(),
																				0xbb,
																				0xbb,
																				0xbb);
	private Color							_bgColor					= new Color(
																				Display.getDefault(),
																				0x50,
																				0x50,
																				0x50);

	/*
	 * UI controls
	 */
	private ViewerDetailForm				_containerMasterDetail;
	private Composite						_containerFolder;
	private Composite						_containerImages;
	private Scale							_scaleThumbnailSize;

	static int compareFiles(final File a, final File b) {

//		boolean aIsDir = a.isDirectory();
//		boolean bIsDir = b.isDirectory();
//		if (aIsDir && ! bIsDir) return -1;
//		if (bIsDir && ! aIsDir) return 1;

		// sort case-sensitive files in a case-insensitive manner
		int compare = a.getName().compareToIgnoreCase(b.getName());
		if (compare == 0) {
			compare = a.getName().compareTo(b.getName());
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

	static void sortBlock(final File[] files, final int start, final int end, final File[] mergeTemp) {
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

	private void addPartListener() {

		_partListener = new IPartListener2() {
			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == PicDirView.this) {
					saveState();
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

			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);

		addPartListener();
		addPrefListener();

		// set selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider());

		restoreState();

		// set thumbnail size
		onSelectThumbnailSize();

		final String selectedFolder = Util.getStateString(_state, STATE_SELECTED_FOLDER, null);
		_picDirFolder.initialRefresh(selectedFolder);
	}

	private void createUI(final Composite parent) {

		_picDirImages = new PicDirImages();
		_picDirFolder = new PicDirFolder(_picDirImages);

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
				_picDirFolder.setColor(_fgColor, _bgColor);
				createUI_10_ImageSize(_containerFolder);
			}

			// sash
			final Sash sash = new Sash(masterDetailContainer, SWT.VERTICAL);

			// photos
			_containerImages = new Composite(masterDetailContainer, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(_containerImages);
			_containerImages.setLayout(new FillLayout());
			{
				_picDirImages.createUI(_containerImages);
				_picDirImages.setColor(_fgColor, _bgColor);
			}

			// master/detail form
			_containerMasterDetail = new ViewerDetailForm(
					masterDetailContainer,
					_containerFolder,
					sash,
					_containerImages);
		}
	}

	private void createUI_10_ImageSize(final Composite parent) {

		_scaleThumbnailSize = new Scale(parent, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_scaleThumbnailSize);
		_scaleThumbnailSize.setMinimum(0);
		_scaleThumbnailSize.setMaximum((PhotoManager.THUMBNAIL_SIZES.length - 1) * 10);
		_scaleThumbnailSize.setIncrement(10);
		_scaleThumbnailSize.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectThumbnailSize();
			}
		});

		_scaleThumbnailSize.addMouseWheelListener(new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustScaleValueOnMouseScroll(event);
				onSelectThumbnailSize();
			}
		});
	}

	@Override
	public void dispose() {

		_fgColor.dispose();
		_bgColor.dispose();

		_picDirImages.dispose();

		getViewSite().getPage().removePartListener(_partListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void onSelectThumbnailSize() {

		final int selectedSizeIndex = _scaleThumbnailSize.getSelection();

		if (selectedSizeIndex == _prevSelectedThumbnailIndex) {
			// optimize selection
			return;
		}

		_prevSelectedThumbnailIndex = selectedSizeIndex;

		PhotoManager.stopImageLoading();

		/**
		 * must be muliplied with 10 that enough increment labels are displayed
		 */
		final int thumbnailSize = PhotoManager.THUMBNAIL_SIZES[selectedSizeIndex / 10];

		_picDirImages.setThumbnailSize(thumbnailSize);

		_scaleThumbnailSize.setToolTipText(NLS.bind("Thumbnail size: {0}", Integer.toString(thumbnailSize)));
	}

	private void restoreState() {

		_containerMasterDetail.setViewerWidth(Util.getStateInt(_state, STATE_TREE_WIDTH, 200));

		/*
		 * thumbnail size
		 */
		final int stateSize = Util.getStateInt(_state, STATE_THUMB_IMAGE_SIZE, PhotoManager.THUMBNAIL_DEFAULT_SIZE);

		int thumbSize = -1;
		for (final int thumbnailSize : PhotoManager.THUMBNAIL_SIZES) {
			if (thumbnailSize == stateSize) {
				thumbSize = thumbnailSize;
				break;
			}
		}
		final int thumbnailSize = thumbSize == -1 ? PhotoManager.THUMBNAIL_DEFAULT_SIZE : thumbSize;
		int thumbnailSizeIndex = -1;

		for (int sizeIndex = 0; sizeIndex < PhotoManager.THUMBNAIL_SIZES.length; sizeIndex++) {
			if (PhotoManager.THUMBNAIL_SIZES[sizeIndex] == thumbnailSize) {
				thumbnailSizeIndex = sizeIndex;
				break;
			}
		}

		_scaleThumbnailSize.setSelection(thumbnailSizeIndex * 10);
	}

	private void saveState() {

		if (_containerFolder.isDisposed()) {
			// this happened
			return;
		}

		// keep width of the dir folder view in the master detail container
		final Tree tree = _picDirFolder.getTree();
		if (tree != null) {
			_state.put(STATE_TREE_WIDTH, tree.getSize().x);
		}

		// selected folder
		final File selectedFolder = _picDirFolder.getSelectedFolder();
		if (selectedFolder != null) {
			_state.put(STATE_SELECTED_FOLDER, selectedFolder.getAbsolutePath());
		}

		// thumbnail size
		final int sizeSelection = _scaleThumbnailSize.getSelection();
		final int thumbnailSize = PhotoManager.THUMBNAIL_SIZES[sizeSelection / 10];

		_state.put(STATE_THUMB_IMAGE_SIZE, thumbnailSize);
	}

	@Override
	public void setFocus() {
		_picDirFolder.getTree().setFocus();
	}

}
