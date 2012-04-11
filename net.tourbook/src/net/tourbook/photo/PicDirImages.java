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
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.photo.gallery.AbstractGalleryItemRenderer;
import net.tourbook.photo.gallery.GalleryMT;
import net.tourbook.photo.gallery.GalleryMTItem;
import net.tourbook.photo.manager.ILoadCallBack;
import net.tourbook.photo.manager.Photo;
import net.tourbook.photo.manager.PhotoImageCache;
import net.tourbook.photo.manager.PhotoLoadingState;
import net.tourbook.photo.manager.PhotoManager;
import net.tourbook.photo.manager.ThumbnailStore;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.UI;
import net.tourbook.util.Util;

import org.apache.commons.sanselan.Sanselan;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.part.PageBook;

/**
 * This class is a compilation from different source codes:
 *
 * <pre>
 * org.eclipse.swt.examples.fileviewer
 * org.sharemedia.gui.libraryviews.GalleryLibraryView
 * org.dawb.common
 * org.apache.commons.sanselan
 * </pre>
 */
public class PicDirImages {

	private static final int						MAX_HISTORY_ENTRIES				= 200;

	static final int								MIN_ITEM_WIDTH					= 10;
	static final int								MAX_ITEM_WIDTH					= 2000;

	private static final String						STATE_FOLDER_HISTORY			= "STATE_FOLDER_HISTORY";				//$NON-NLS-1$
	private static final String						STATE_THUMB_IMAGE_SIZE			= "STATE_THUMB_IMAGE_SIZE";			//$NON-NLS-1$
	private static final String						STATE_GALLERY_POSITION_FOLDER	= "STATE_GALLERY_POSITION_FOLDER";		//$NON-NLS-1$
	private static final String						STATE_GALLERY_POSITION_VALUE	= "STATE_GALLERY_POSITION_VALUE";		//$NON-NLS-1$

	private final IPreferenceStore					_prefStore						= TourbookPlugin.getDefault() //
																							.getPreferenceStore();

	/*
	 * worker thread management
	 */
	/**
	 * Worker start time
	 */
	private long									_workerStart;
	/**
	 * Lock for all worker control data and state
	 */
	private final Object							_workerLock						= new Object();

	/**
	 * The worker's thread
	 */
	private volatile Thread							_workerThread					= null;

	/**
	 * True if the worker must exit on completion of the current cycle
	 */
	private volatile boolean						_workerStopped					= false;

	/**
	 * True if the worker must cancel its operations prematurely perhaps due to a state update
	 */
	private volatile boolean						_workerCancelled				= false;

	/**
	 * Worker state information -- this is what gets synchronized by an update
	 */
	private volatile File							_workerStateDir					= null;

	/**
	 * State information to use for the next cycle
	 */
	private volatile File							_workerNextFolder				= null;

	/**
	 * Manages the worker's thread
	 */
	private final Runnable							_workerRunnable;

	/**
	 *
	 */
	public static final Comparator<File>			NATURAL_SORT					= new SortNatural<File>(true);

	/**
	 *
	 */
	public static Comparator<File>					DATE_SORT;

//	private Comparator<File>						_currentComparator	= SortingUtils.DATE_SORT;
	private Comparator<File>						_currentComparator				= DATE_SORT;

	private PicDirView								_picDirView;

	private AbstractGalleryItemRenderer				_itemRenderer;
	private NoGroupRendererMT						_groupRenderer;

	/**
	 * Photo image height (thumbnail size)
	 */
	private int										_photoWidth						= PhotoManager.IMAGE_SIZE_THUMBNAIL;

	/**
	 * Folder which images are currently be displayed
	 */
	private File									_photoFolder;
	private File[]									_photoFiles;
	private FileFilter								_fileFilter;

	private PicDirFolder							_picDirFolder;

	private boolean									_isComboKeyPressed;

	private int										_selectedHistoryIndex;
	private ArrayList<String>						_folderHistory					= new ArrayList<String>();

	private ActionNavigateHistoryBackward			_actionNavigateBackward;
	private ActionNavigateHistoryForward			_actionNavigateForward;
	private ActionClearNavigationHistory			_actionClearNavigationHistory;
	private ActionRemoveInvalidFoldersFromHistory	_actionRemoveInvalidFoldersFromHistory;

	private LinkedHashMap<String, Double>			_galleryPositions				= new LinkedHashMap<String, Double>(
																							100,
																							0.75f,
																							true);

	/*
	 * UI controls
	 */
	private Display									_display;
	private Composite								_uiContainer;

	private Composite								_containerActionBar;
	private ToolBar									_toolbar;
	private Spinner									_spinnerThumbSize;
	private Combo									_comboPathHistory;

	private GalleryMT								_gallery;
	private CLabel									_lblStatusInfo;

	private PageBook								_pageBook;
	private Label									_lblLoading;
	private Composite								_pageLoading;
	private Composite								_containerStatusLine;

	{
		_workerRunnable = new Runnable() {
			public void run() {

				while (!_workerStopped) {

					synchronized (_workerLock) {
						_workerCancelled = false;
						_workerStateDir = _workerNextFolder;
					}

					workerExecute();

					synchronized (_workerLock) {
						try {
							if ((!_workerCancelled) && (_workerStateDir == _workerNextFolder)) {

								/*
								 * wait until the next images should be displayed
								 */

								_workerLock.wait();
							}
						} catch (final InterruptedException e) {}
					}
				}

				_workerThread = null;

				/*
				 * wake up UI thread in case it is in a modal loop awaiting thread termination (see
				 * workerStop())
				 */
				_display.wake();
			}
		};

		DATE_SORT = new Comparator<File>() {
			@Override
			public int compare(final File one, final File two) {

				if (_workerCancelled) {
					// couldn't find another way how to stop sorting
					return 0;
				}

				final long diff = one.lastModified() - two.lastModified();

				if (diff == 0) {
					return NATURAL_SORT.compare(one, two);
				}

				if (diff > 0) {
					return (int) two.lastModified();
				}
				return -1;
			}
		};
	}

	private class LoadImageCallback implements ILoadCallBack {

		private GalleryMTItem	__galleryItem;

		/**
		 * @param galleryItem
		 */
		public LoadImageCallback(final GalleryMTItem galleryItem) {

			__galleryItem = galleryItem;
		}

		@Override
		public void callBackImageIsLoaded(	final boolean isImageStillVisible,
											final boolean isImageLoadedOrHasLoadingError) {

			if (isImageStillVisible == false) {
				return;
			}

			if (isImageLoadedOrHasLoadingError) {

				_display.asyncExec(new Runnable() {

					public void run() {

						if (__galleryItem.isDisposed() || _gallery.isDisposed()) {
							return;
						}

						final Rectangle galleryItemBounds = __galleryItem.getBounds();

						_gallery.redraw(
								galleryItemBounds.x,
								galleryItemBounds.y,
								galleryItemBounds.width,
								galleryItemBounds.height,
								false);
					}
				});
			}
		}
	}

	PicDirImages(final PicDirView picDirView) {
		_picDirView = picDirView;
	}

	void actionClearHistory() {

		final String selectedFolder = _comboPathHistory.getText();

		_comboPathHistory.removeAll();
		_comboPathHistory.add(selectedFolder);
		_comboPathHistory.select(0);

		_folderHistory.clear();
		_folderHistory.add(selectedFolder);

		_actionClearNavigationHistory.setEnabled(false);
		_actionRemoveInvalidFoldersFromHistory.setEnabled(false);
		_actionNavigateBackward.setEnabled(false);
		_actionNavigateForward.setEnabled(false);
	}

// LOG ALL BINDINGS
//
//		final IWorkbench workbench = PlatformUI.getWorkbench();
//		final IBindingService bindingService = (IBindingService) workbench.getAdapter(IBindingService.class);
//
//		System.out.println(bindingService.getActiveScheme());
//
//		for (final Binding binding : bindingService.getBindings()) {
//			System.out.println(binding);
//		}

	void actionNavigateBackward() {

		final int historySize = _folderHistory.size();
		if (_selectedHistoryIndex >= historySize - 1) {

			// last entry is already selected

			_selectedHistoryIndex = historySize - 1;
			_actionNavigateBackward.setEnabled(false);

			return;
		}

		_selectedHistoryIndex++;

		// select combo history
		_comboPathHistory.select(_selectedHistoryIndex);

		// enabel/disable history navigation
		_actionNavigateBackward.setEnabled(_selectedHistoryIndex < historySize - 1);
		_actionNavigateForward.setEnabled(true);

		BusyIndicator.showWhile(_display, new Runnable() {
			public void run() {

				final String prevFolderPathName = _folderHistory.get(_selectedHistoryIndex);
				final boolean isFolderAvailable = _picDirFolder.selectFolder(prevFolderPathName, false, true);

				if (isFolderAvailable == false) {
					removeInvalidFolder(prevFolderPathName);
				}
			}
		});
	}

	void actionNavigateForward() {

		final int historySize = _folderHistory.size();
		if (_selectedHistoryIndex == 0) {

			// first entry is already selected

			_actionNavigateForward.setEnabled(false);

			return;
		}

		_selectedHistoryIndex--;

		// select combo history
		_comboPathHistory.select(_selectedHistoryIndex);

		// enabel/disable history navigation
		_actionNavigateBackward.setEnabled(historySize > 1);
		_actionNavigateForward.setEnabled(_selectedHistoryIndex > 0);

		BusyIndicator.showWhile(_display, new Runnable() {
			public void run() {
				final String prevFolderPathName = _folderHistory.get(_selectedHistoryIndex);
				final boolean isFolderAvailable = _picDirFolder.selectFolder(prevFolderPathName, false, true);

				if (isFolderAvailable == false) {
					removeInvalidFolder(prevFolderPathName);
				}
			}
		});
	}

	void actionRemoveInvalidFolders() {

		BusyIndicator.showWhile(_display, new Runnable() {
			public void run() {
				removeInvalidFolders();
			}
		});
	}

	void actionShowNavigationHistory() {

		_comboPathHistory.setFocus();

		// this is not working with osx: https://bugs.eclipse.org/bugs/show_bug.cgi?id=300979
		_comboPathHistory.setListVisible(true);
	}

	private void createActions() {

		final ToolBarManager tbm = new ToolBarManager(_toolbar);

		_actionNavigateBackward = new ActionNavigateHistoryBackward(this, _picDirView);
		_actionNavigateForward = new ActionNavigateHistoryForward(this, _picDirView);

		// this action activates the shortcut key <Ctrl><Shift>H but the action is not displayed
		new ActionNavigateShowHistory(this, _picDirView);

		/*
		 * fill actionbar
		 */
		tbm.add(_actionNavigateBackward);
		tbm.add(_actionNavigateForward);

		_toolbar.setMenu(createContextMenu(_toolbar));

		tbm.update(true);
	}

	/**
	 * create context menu
	 */
	private Menu createContextMenu(final Control parent) {

		_actionClearNavigationHistory = new ActionClearNavigationHistory(this);
		_actionRemoveInvalidFoldersFromHistory = new ActionRemoveInvalidFoldersFromHistory(this);

		final MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);

		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager menuMgr) {

				menuMgr.add(_actionRemoveInvalidFoldersFromHistory);
				menuMgr.add(_actionClearNavigationHistory);
			}
		});

		return menuMgr.createContextMenu(parent);
	}

	/**
	 * This will be configured from options but for now it is any image accepted.
	 *
	 * @return
	 */
	private FileFilter createFileFilter() {

		return new FileFilter() {
			@Override
			public boolean accept(final File pathname) {

				if (pathname.isDirectory()) {
					return false;
				}

				if (pathname.isHidden()) {
					return false;
				}

				final String name = pathname.getName();
				if (name == null || name.length() == 0) {
					return false;
				}

				if (name.startsWith(".")) { //$NON-NLS-1$
					return false;
				}

				if (Sanselan.hasImageFileExtension(pathname)) {
					return true;
				}

				return false;
			}
		};
	}

	void createUI(final PicDirFolder picDirFolder, final Composite parent) {

		_picDirFolder = picDirFolder;
		_uiContainer = parent;
		_display = parent.getDisplay();

		_fileFilter = createFileFilter();

		createUI_0(parent);

		_lblLoading.setText(Messages.Pic_Dir_Label_FolderIsNotSelected);

		createActions();
	}

	void createUI_0(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(container);
//		container.setBackground(_display.getSystemColor(SWT.COLOR_RED));
		{
			createUI_10_ActionBar(container);

			_pageBook = new PageBook(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageBook);
			{
				createUI_20_PageGallery(_pageBook);
				createUI_30_PageLoading(_pageBook);
			}

			createUI_50_StatusLine(container);
		}
	}

	private void createUI_10_ActionBar(final Composite parent) {

		_containerActionBar = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_containerActionBar);
		GridLayoutFactory.fillDefaults()//
				.numColumns(3)
				.extendedMargins(0, 0, 2, 2)
				.applyTo(_containerActionBar);
		{
			/*
			 * toolbar actions
			 */
			_toolbar = new ToolBar(_containerActionBar, SWT.FLAT);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_toolbar);

			createUI_18_ComboHistory(_containerActionBar);
			createUI_19_ImageSize(_containerActionBar);
		}
	}

	/**
	 * combo: path history
	 */
	void createUI_18_ComboHistory(final Composite parent) {

		_comboPathHistory = new Combo(parent, SWT.SIMPLE | SWT.DROP_DOWN);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(_comboPathHistory);
		_comboPathHistory.setVisibleItemCount(30);

		_comboPathHistory.addMouseListener(new MouseListener() {

			@Override
			public void mouseDoubleClick(final MouseEvent e) {}

			@Override
			public void mouseDown(final MouseEvent e) {

				// show list
				_comboPathHistory.setListVisible(true);
			}

			@Override
			public void mouseUp(final MouseEvent e) {}
		});

		/**
		 * This combination of key and selection listener causes a folder selection only with the
		 * <Enter> key or with a selection with the mouse in the drop down box
		 */
		_comboPathHistory.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {

				_isComboKeyPressed = true;

				if (e.keyCode == SWT.CR) {
					onSelectHistoryFolder(_comboPathHistory.getText());
				}
			}
		});

		_comboPathHistory.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				final boolean isKey = _isComboKeyPressed;
				_isComboKeyPressed = false;

				if (isKey == false) {
					onSelectHistoryFolder(_comboPathHistory.getText());
				}
			}
		});
	}

	/**
	 * spinner: thumb size
	 */
	private void createUI_19_ImageSize(final Composite parent) {

		_spinnerThumbSize = new Spinner(parent, SWT.BORDER);
		GridDataFactory.fillDefaults() //
				.align(SWT.BEGINNING, SWT.FILL)
				.applyTo(_spinnerThumbSize);
		_spinnerThumbSize.setMinimum(MIN_ITEM_WIDTH);
		_spinnerThumbSize.setMaximum(MAX_ITEM_WIDTH);
		_spinnerThumbSize.setIncrement(10);
		_spinnerThumbSize.setPageIncrement(50);
		_spinnerThumbSize.setToolTipText(UI.IS_OSX
				? Messages.Pic_Dir_Spinner_ThumbnailSize_Tooltip_OSX
				: Messages.Pic_Dir_Spinner_ThumbnailSize_Tooltip);
		_spinnerThumbSize.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectThumbnailSize(_spinnerThumbSize.getSelection());
			}
		});
		_spinnerThumbSize.addMouseWheelListener(new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onSelectThumbnailSize(_spinnerThumbSize.getSelection());
			}
		});
	}

	/**
	 * Create gallery
	 */
	private void createUI_20_PageGallery(final Composite parent) {

		_gallery = new GalleryMT(parent, SWT.V_SCROLL | SWT.VIRTUAL | SWT.MULTI);
		//		GridDataFactory.fillDefaults().grab(true, true).applyTo(_gallery);

		_gallery.setHigherQualityDelay(200);
//		_gallery.setAntialias(SWT.OFF);
//		_gallery.setInterpolation(SWT.LOW);
		_gallery.setAntialias(SWT.ON);
		_gallery.setInterpolation(SWT.HIGH);

		_gallery.setVirtualGroups(true);
		_gallery.setVirtualGroupDefaultItemCount(1);
		_gallery.setVirtualGroupsCompatibilityMode(true);

		_gallery.addListener(SWT.SetData, new Listener() {
			public void handleEvent(final Event event) {
				onGallery1SetItemData(event);
			}
		});

		_gallery.addListener(SWT.PaintItem, new Listener() {
			public void handleEvent(final Event event) {
				onGallery2PaintItem(event);
			}
		});

		_gallery.addListener(SWT.Modify, new Listener() {

			// a modify event is fired when gallery is zoomed in/out

			public void handleEvent(final Event event) {
				onSelectThumbnailSize(event.width);
			}
		});

		/*
		 * set renderer
		 */
		_itemRenderer = new PhotoRenderer();
		final PhotoRenderer photoRenderer = (PhotoRenderer) _itemRenderer;
		photoRenderer.setShowLabels(true);
//		photoRenderer.setDropShadows(true);
//		photoRenderer.setDropShadowsSize(5);
		_gallery.setItemRenderer(_itemRenderer);

		_groupRenderer = new NoGroupRendererMT();
		_groupRenderer.setItemSize(_photoWidth, (int) (_photoWidth * (float) 15 / 11));
		_groupRenderer.setItemHeightMinMax(MIN_ITEM_WIDTH, MAX_ITEM_WIDTH);
		_groupRenderer.setAutoMargin(true);
		_groupRenderer.setMinMargin(0);

		_gallery.setGroupRenderer(_groupRenderer);

		// create root item (is needed)
		new GalleryMTItem(_gallery, SWT.VIRTUAL);

		_gallery.setItemCount(1);
	}

	private void createUI_30_PageLoading(final PageBook parent) {

		_pageLoading = new Composite(parent, SWT.NONE);
//		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(1)
				.margins(5, 5)
				.applyTo(_pageLoading);
		{
			_lblLoading = new Label(_pageLoading, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.align(SWT.FILL, SWT.FILL)
					.applyTo(_lblLoading);
		}
	}

	private void createUI_50_StatusLine(final Composite parent) {

		_containerStatusLine = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_containerStatusLine);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_containerStatusLine);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * label: info
			 */
			_lblStatusInfo = new CLabel(_containerStatusLine, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblStatusInfo);

//			/*
//			 * progress bar
//			 */
//			_progbarLoading = new ProgressBar(_containerStatusLine, SWT.HORIZONTAL | SWT.SMOOTH);
//			GridDataFactory.fillDefaults().grab(true, false).align(SWT.END, SWT.FILL).applyTo(_progbarLoading);
		}
	}

	private void disposeAllImages() {
		ThumbnailStore.cleanupStoreFiles(true, true);
		PhotoImageCache.dispose();
	}

	void handlePrefStoreModifications(final PropertyChangeEvent event) {

		final String property = event.getProperty();

		if (property.equals(ITourbookPreferences.PHOTO_VIEWER_PREF_STORE_EVENT)) {

			updateImageQuality();

			_display.asyncExec(new Runnable() {
				public void run() {

					if (MessageDialog.openQuestion(
							_display.getActiveShell(),
							Messages.Pic_Dir_Dialog_CleanupStoreImages_Title,
							Messages.Pic_Dir_Dialog_CleanupStoreImages_Message) == false) {

						disposeAllImages();
					}

					_gallery.keepGalleryPosition();

					// this will update the gallery
					_gallery.clearAll();
				}
			});
		}
	}

	void onClose() {

		PhotoManager.stopImageLoading();

		//////////////////////////////////////////
		//
		// MUST BE REMOVED, IS ONLY FOR TESTING
		//
		PhotoImageCache.dispose();
		//
		// MUST BE REMOVED, IS ONLY FOR TESTING
		//
		//////////////////////////////////////////

		workerStop();
	}

	/**
	 * This event is called first of all before a gallery item is painted, it sets the photo into
	 * the gallery item.
	 *
	 * @param event
	 */
	private void onGallery1SetItemData(final Event event) {

		final GalleryMTItem galleryItem = (GalleryMTItem) event.item;

		if (galleryItem.getParentItem() == null) {

			/*
			 * It's a group
			 */

			galleryItem.setItemCount(_photoFiles.length);

		} else {

			/*
			 * It's an item
			 */

			final GalleryMTItem parentItem = galleryItem.getParentItem();
			final int galleryItemIndex = parentItem.indexOf(galleryItem);

			final Photo photo = new Photo(_photoFiles[galleryItemIndex], galleryItemIndex);

			galleryItem.setData(photo);
			galleryItem.setText(photo.getFileName());
		}
	}

	/**
	 * This event checks if the image for the photo is available in the image cache, if not it is
	 * put into a queue to be loaded, the {@link PhotoRenderer} will then paint the image.
	 *
	 * @param event
	 */
	private void onGallery2PaintItem(final Event event) {

		final GalleryMTItem galleryItem = (GalleryMTItem) event.item;

		if (galleryItem != null && galleryItem.getParentItem() != null) {

			/*
			 * check if the photo image is available, if not, image must be loaded
			 */

			final Photo photo = (Photo) galleryItem.getData();

			final int imageQuality = _photoWidth <= PhotoManager.IMAGE_SIZE_THUMBNAIL
					? PhotoManager.IMAGE_QUALITY_EXIF_THUMB_160
					: PhotoManager.IMAGE_QUALITY_HQ_600;

			// check if image is already being loaded or has an loading error
			final PhotoLoadingState photoLoadingState = photo.getLoadingState(imageQuality);
			if (photoLoadingState == PhotoLoadingState.IMAGE_HAS_A_LOADING_ERROR
					|| photoLoadingState == PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE) {
				return;
			}

			// check if image is in the cache
			final Image photoImage = PhotoImageCache.getImage(photo, imageQuality);
			if (photoImage == null || photoImage.isDisposed()) {

				// the requested image is not available in the image cache -> image must be loaded

				final LoadImageCallback imageLoadCallback = new LoadImageCallback(galleryItem);

				PhotoManager.putImageInLoadingQueue(galleryItem, photo, imageQuality, imageLoadCallback);
			}
		}
	}

	private void onSelectHistoryFolder(final String selectedFolder) {

		updateHistory(selectedFolder);

		BusyIndicator.showWhile(_display, new Runnable() {
			public void run() {

				final boolean isFolderAvailable = _picDirFolder.selectFolder(selectedFolder, false, false);

				if (isFolderAvailable == false) {
					removeInvalidFolder(selectedFolder);
				}
			}
		});
	}

	private void onSelectThumbnailSize(final int newThumbSize) {

		final double galleryWidth = _gallery.getClientArea().width;
		final int prevPhotoWidth = _photoWidth;

		final double prevNumberOfImages = galleryWidth / prevPhotoWidth;

		int newPhotoWidth = newThumbSize;

		if (newThumbSize > prevPhotoWidth) {

			// increased width -> fewer images

			int imageCounter = (int) (prevNumberOfImages - 1);

			/**
			 * when only 1 image is displayed horizontally, there is a bug in the drawing algorithm
			 * that only 1 displayed even when multiple image could be displayed vertically
			 */
			if (imageCounter >= 2) {

				newPhotoWidth = (int) (galleryWidth / imageCounter);

				if (newPhotoWidth == prevPhotoWidth) {

					// size has not changed

					while (newPhotoWidth == prevPhotoWidth) {

						if (imageCounter < 3) {
							break;
						}

						newPhotoWidth = (int) (galleryWidth / --imageCounter);
					}
				}
			}

		} else {

			// decreased width -> more images

			final double tempImageCounter = galleryWidth / newThumbSize;

			/*
			 * size by number of images only, when more than 2 images are displayed, otherwise size
			 * by newThumbSize
			 */
			if (tempImageCounter > 2) {

				final int numberOfImages = (int) (prevNumberOfImages + 1);

				final int newTempPhotoWidth = (int) (galleryWidth / numberOfImages);
				if (newTempPhotoWidth > MIN_ITEM_WIDTH) {
					newPhotoWidth = newTempPhotoWidth;
				}
			}
		}

		final int photoWidth = newPhotoWidth;
		final int photoHeight = (int) (newPhotoWidth * (float) 11 / 15);

		if (photoWidth == _photoWidth) {
			// optimize selection
			return;
		}

		PhotoManager.stopImageLoading();

		/*
		 * update UI with new size
		 */
		_photoWidth = photoWidth;

		_spinnerThumbSize.setSelection(photoWidth);
		_groupRenderer.setItemSize(photoWidth, photoHeight);
	}

	private void removeInvalidFolder(final String invalidFolderPathName) {

		// search invalid folder in history
		int invalidIndex = -1;
		int historyIndex = 0;
		for (final String historyFolder : _folderHistory) {

			if (historyFolder.equals(invalidFolderPathName)) {
				invalidIndex = historyIndex;
				break;
			}

			historyIndex++;
		}

		if (invalidIndex == -1) {
			// this should not happen
			return;
		}

		// remove invalid folder
		_folderHistory.remove(invalidIndex);
		_comboPathHistory.remove(invalidIndex);

		// display previously successfully loaded folder
		_comboPathHistory.setText(_photoFolder.getAbsolutePath());
	}

	/**
	 * Checks all folders in the history and removes all folders which are not available any more.
	 */
	private void removeInvalidFolders() {

		final ArrayList<String> invalidFolders = new ArrayList<String>();
		final ArrayList<Integer> invalidFolderIndexes = new ArrayList<Integer>();

		int folderIndex = 0;

		for (final String historyFolder : _folderHistory) {

			final File folder = new File(historyFolder);
			if (folder.isDirectory() == false) {
				invalidFolders.add(historyFolder);
				invalidFolderIndexes.add(folderIndex);
			}

			folderIndex++;
		}

		if (invalidFolders.size() == 0) {
			// nothing to do
			return;
		}

		_folderHistory.removeAll(invalidFolders);

		final Integer[] invalidIndexes = invalidFolderIndexes.toArray(new Integer[invalidFolderIndexes.size()]);

		// remove from the end that the index numbers do not disappear
		for (int index = invalidIndexes.length - 1; index >= 0; index--) {
			_comboPathHistory.remove(invalidIndexes[index]);
		}
	}

	void restoreState(final IDialogSettings state) {

		/*
		 * history
		 */
		final String[] historyEntries = Util.getStateArray(state, STATE_FOLDER_HISTORY, null);
		if (historyEntries != null) {

			// update history and combo
			for (final String history : historyEntries) {
				_folderHistory.add(history);
				_comboPathHistory.add(history);
			}
		}

		/*
		 * image quality
		 */
		updateImageQuality();

		/*
		 * thumbnail size
		 */
		final int stateThumbSize = Util.getStateInt(state, STATE_THUMB_IMAGE_SIZE, PhotoManager.IMAGE_SIZE_THUMBNAIL);
		_spinnerThumbSize.setSelection(stateThumbSize);

		// restore thumbnail size
		onSelectThumbnailSize(stateThumbSize);

		/*
		 * gallery folder image positions
		 */
		final String[] positionFolders = state.getArray(STATE_GALLERY_POSITION_FOLDER);
		final String[] positionValues = state.getArray(STATE_GALLERY_POSITION_VALUE);
		if (positionFolders != null && positionValues != null) {

			// ensure same size
			if (positionFolders.length == positionValues.length) {

				for (int positionIndex = 0; positionIndex < positionFolders.length; positionIndex++) {

					final String positionValueString = positionValues[positionIndex];

					try {
						final Double positionValue = Double.parseDouble(positionValueString);

						_galleryPositions.put(positionFolders[positionIndex], positionValue);

					} catch (final Exception e) {
						// ignore
					}
				}
			}
		}
	}

	void saveState(final IDialogSettings state) {

		state.put(STATE_FOLDER_HISTORY, _folderHistory.toArray(new String[_folderHistory.size()]));

		// thumbnail size
		state.put(STATE_THUMB_IMAGE_SIZE, _spinnerThumbSize.getSelection());

		/*
		 * gallery positions for each folder
		 */

		// get current position
		if (_photoFolder != null) {
			_galleryPositions.put(_photoFolder.getAbsolutePath(), _gallery.getGalleryPosition());
		}

		final Set<String> positionFolders = _galleryPositions.keySet();
		final int positionSize = positionFolders.size();

		if (positionSize > 0) {

			final String[] positionFolderArray = positionFolders.toArray(new String[positionSize]);
			final String[] positionValues = new String[positionSize];

			for (int positionIndex = 0; positionIndex < positionFolderArray.length; positionIndex++) {
				final String positionKey = positionFolderArray[positionIndex];
				positionValues[positionIndex] = _galleryPositions.get(positionKey).toString();
			}

			state.put(STATE_GALLERY_POSITION_FOLDER, positionFolderArray);
			state.put(STATE_GALLERY_POSITION_VALUE, positionValues);
		}

	}

	/**
	 * Display images for the selected folder.
	 *
	 * @param imageFolder
	 * @param isFromNavigationHistory
	 * @param isReloadFolder
	 */
	void showImages(final File imageFolder, final boolean isFromNavigationHistory, final boolean isReloadFolder) {

		//////////////////////////////////////////
		//
		// MUST BE REMOVED, IS ONLY FOR TESTING
		//
//		PhotoImageCache.dispose();
//		ThumbnailStore.cleanupStoreFiles(true, true);
		//
		// MUST BE REMOVED, IS ONLY FOR TESTING
		//
		//////////////////////////////////////////

		if (imageFolder == null) {
			_lblLoading.setText(Messages.Pic_Dir_Label_FolderIsNotSelected);
		} else {

			_lblLoading.setText(NLS.bind(Messages.Pic_Dir_Label_Loading, imageFolder.getAbsolutePath()));

			if (isFromNavigationHistory == false) {
				// don't update history when the navigation in the history has caused to display the images
				updateHistory(imageFolder.getAbsolutePath());
			}
		}

		_pageBook.showPage(_pageLoading);

		PhotoManager.stopImageLoading();

		workerUpdate(imageFolder, isReloadFolder);
	}

	void updateColors(final Color fgColor, final Color bgColor) {

		/*
		 * action bar
		 */
		if (UI.IS_OSX == false) {

			_containerActionBar.setForeground(fgColor);
			_containerActionBar.setBackground(bgColor);
			_toolbar.setForeground(fgColor);
			_toolbar.setBackground(bgColor);

			// combobox list entries are almost invisible when colors are set on osx

			_spinnerThumbSize.setForeground(fgColor);
			_spinnerThumbSize.setBackground(bgColor);

			_comboPathHistory.setForeground(fgColor);
			_comboPathHistory.setBackground(bgColor);
		}

		/*
		 * gallery
		 */
		_gallery.setForeground(fgColor);
		_gallery.setBackground(bgColor);

		final PhotoRenderer photoRenderer = (PhotoRenderer) _itemRenderer;
		photoRenderer.setForegroundColor(fgColor);
		photoRenderer.setBackgroundColor(bgColor);

		_pageLoading.setBackground(bgColor);

		/*
		 * status line
		 */
		_containerStatusLine.setForeground(fgColor);
		_containerStatusLine.setBackground(bgColor);

		_lblStatusInfo.setForeground(fgColor);
		_lblStatusInfo.setBackground(bgColor);

		/*
		 * loading page
		 */
		_lblLoading.setForeground(fgColor);
		_lblLoading.setBackground(bgColor);
	}

	private void updateHistory(final String newFolderPathName) {

		int historyIndex = -1;
		int historyCounter = 0;

		// check if new path is already in the history
		for (final String historyItem : _folderHistory) {
			if (historyItem.equals(newFolderPathName)) {
				historyIndex = historyCounter;
				break;
			}
			historyCounter++;
		}

		if (historyIndex != -1) {

			// this is an existing history entry, move it to the top

			// remove from history
			_folderHistory.remove(historyIndex);

			// remove from combo
			_comboPathHistory.remove(historyIndex);
		}

		// check max history size
		int historySize = _folderHistory.size();
		if (historySize > MAX_HISTORY_ENTRIES) {

			_folderHistory.remove(historySize - 1);
			_comboPathHistory.remove(historySize - 1);
		}

		// update history
		_folderHistory.add(0, newFolderPathName);

		// update combo
		_comboPathHistory.add(newFolderPathName, 0);

		// must be selected otherwise the text field can be empty when selected from the dropdown list
		_comboPathHistory.select(0);

		/*
		 * enabel/disable history navigation
		 */
		_selectedHistoryIndex = 0;
		historySize = _folderHistory.size();

		_actionNavigateBackward.setEnabled(historySize > 1);
		_actionNavigateForward.setEnabled(false);

		_actionClearNavigationHistory.setEnabled(historySize > 1);
		_actionRemoveInvalidFoldersFromHistory.setEnabled(historySize > 1);
	}

	private void updateImageQuality() {

		final boolean isShowHighQuality = _prefStore.getBoolean(//
				ITourbookPreferences.PHOTO_VIEWER_IS_SHOW_IMAGE_WITH_HIGH_QUALITY);

		final int hqMinSize = _prefStore.getInt(//
				ITourbookPreferences.PHOTO_VIEWER_HIGH_QUALITY_IMAGE_MIN_SIZE);

		_gallery.setImageQuality(isShowHighQuality, hqMinSize);

	}

	/**
	 * Updates the gallery contents
	 */
	private void workerExecute() {

		_workerStart = System.currentTimeMillis();

		File[] newPhotoFiles = null;

		if (_workerStateDir != null) {

			_display.syncExec(new Runnable() {
				public void run() {

					// guard against the ui being closed before this runs
					if (_uiContainer.isDisposed()) {
						return;
					}

//					_lblStatusInfo.setText(NLS.bind(Messages.Pic_Dir_Status_Reading, _workerStateDir.getAbsolutePath()));
					_lblStatusInfo.setText(UI.EMPTY_STRING);
				}
			});

			// We make file list in this thread for speed reasons
			final List<File> files = SortingUtils.getSortedFileList(_workerStateDir, _fileFilter, _currentComparator);

			if (_workerCancelled) {
				return;
			}

			if (files == null) {
				// prevent NPE
				newPhotoFiles = new File[0];
			} else {
				newPhotoFiles = files.toArray(new File[files.size()]);
			}

			final File prevPhotoFolder = _photoFolder;
			_photoFolder = _workerStateDir;
			_photoFiles = newPhotoFiles;

			_display.syncExec(new Runnable() {
				public void run() {

					// guard against the ui being closed before this runs
					if (_uiContainer.isDisposed()) {
						return;
					}

					// keep previous gallery position
					if (prevPhotoFolder != null) {
						_galleryPositions.put(prevPhotoFolder.getAbsolutePath(), _gallery.getGalleryPosition());
					}

					// set gallery position
					final Double newPosition = _galleryPositions.get(_photoFolder.getAbsolutePath());
					if (newPosition != null) {
						_gallery.setGalleryPositionWhenUpdated(newPosition);
					}

					/*
					 * this will also update the gallery, it's not easy to understand the update
					 * procedure
					 */
					_gallery.clearAll();

					/*
					 * update status info
					 */
					final long timeDiff = System.currentTimeMillis() - _workerStart;
					final String timeDiffText = NLS.bind(
							Messages.Pic_Dir_Status_Loaded,
							new Object[] { Long.toString(timeDiff), Integer.toString(_photoFiles.length) });

					_lblStatusInfo.setText(timeDiffText);

					_pageBook.showPage(_gallery);
				}
			});
		}
	}

	/**
	 * Stops the worker and waits for it to terminate.
	 */
	private void workerStop() {

		if (_workerThread == null) {
			return;
		}

		synchronized (_workerLock) {

			_workerCancelled = true;
			_workerStopped = true;

			_workerLock.notifyAll();
		}

		while (_workerThread != null) {
			if (!_display.readAndDispatch()) {
				_display.sleep();
			}
		}
	}

	/**
	 * Notifies the worker that it should update itself with new data. Cancels any previous
	 * operation and begins a new one.
	 *
	 * @param newFolder
	 *            the new base directory for the table, null is ignored
	 * @param isReloadFolder
	 */
	private void workerUpdate(final File newFolder, final boolean isReloadFolder) {

		if (newFolder == null) {
			return;
		}

		if (isReloadFolder == false && _workerNextFolder != null && _workerNextFolder.equals(newFolder)) {
			return;
		}

		synchronized (_workerLock) {

			_workerNextFolder = newFolder;

			_workerStopped = false;
			_workerCancelled = true;

			_workerLock.notifyAll();
		}

		if (_workerThread == null) {
			_workerThread = new Thread(_workerRunnable, "PicDirImages: retrieve files"); //$NON-NLS-1$
			_workerThread.start();
		}
	}
}
