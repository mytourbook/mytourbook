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
import java.util.List;

import net.tourbook.application.ApplicationActionBarAdvisor;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.photo.gallery.AbstractGalleryItemRenderer;
import net.tourbook.photo.gallery.AbstractGridGroupRenderer;
import net.tourbook.photo.gallery.GalleryMT;
import net.tourbook.photo.gallery.GalleryMTItem;
import net.tourbook.photo.manager.ILoadCallBack;
import net.tourbook.photo.manager.Photo;
import net.tourbook.photo.manager.PhotoImageCache;
import net.tourbook.photo.manager.PhotoLoadingState;
import net.tourbook.photo.manager.PhotoManager;
import net.tourbook.ui.UI;
import net.tourbook.util.Util;

import org.apache.commons.sanselan.Sanselan;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
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
import org.eclipse.swt.widgets.ProgressBar;
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

	private static final int				MAX_HISTORY_ENTRIES		= 200;

	private static final String				STATE_FOLDER_HISTORY	= "STATE_FOLDER_HISTORY";		//$NON-NLS-1$

	/*
	 * worker thread management
	 */
	/**
	 * Worker start time
	 */
	private long							_workerStart;
	/**
	 * Lock for all worker control data and state
	 */
	private final Object					_workerLock				= new Object();

	/**
	 * The worker's thread
	 */
	private volatile Thread					_workerThread			= null;

	/**
	 * True if the worker must exit on completion of the current cycle
	 */
	private volatile boolean				_workerStopped			= false;

	/**
	 * True if the worker must cancel its operations prematurely perhaps due to a state update
	 */
	private volatile boolean				_workerCancelled		= false;

	/**
	 * Worker state information -- this is what gets synchronized by an update
	 */
	private volatile File					_workerStateDir			= null;

	/**
	 * State information to use for the next cycle
	 */
	private volatile File					_workerNextFolder		= null;

	/**
	 * Manages the worker's thread
	 */
	private final Runnable					_workerRunnable;

	/**
	 * 
	 */
	public static final Comparator<File>	NATURAL_SORT			= new SortNatural<File>(true);

	/**
	 * 
	 */
	public static Comparator<File>			DATE_SORT;

//	private Comparator<File>				_currentComparator	= SortingUtils.DATE_SORT;
	private Comparator<File>				_currentComparator		= DATE_SORT;

	private AbstractGalleryItemRenderer		_itemRenderer;
	private AbstractGridGroupRenderer		_groupRenderer;

	private int								_photoSize				= 64;

	private File[]							_photoFiles;
	private FileFilter						_fileFilter;

	private PicDirView						_picDirView;
	private PicDirFolder					_picDirFolder;

	private boolean							_isKey;

	private int								_selectedHistoryIndex;
	private ArrayList<String>				_folderHistory			= new ArrayList<String>();

	private ActionNavigateHistoryBackward	_actionNavigateBackward;
	private ActionNavigateHistoryForward	_actionNavigateForward;
	private ActionClearNavigationHistory	_actionClearNavigationHistory;

	/*
	 * UI controls
	 */
	private Display							_display;
	private Composite						_uiContainer;

	private Composite						_containerActionBar;
	private ToolBar							_toolbar;

	private GalleryMT						_gallery;
	private CLabel							_lblStatusInfo;
	private ProgressBar						_progbarLoading;

	private PageBook						_pageBook;
	private Label							_lblLoading;
	private Composite						_pageLoading;
	private Combo							_comboPathHistory;
	private Composite						_containerStatusLine;

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
		public void callBackImageIsLoaded(final boolean isImageStillVisible) {

			if (isImageStillVisible == false) {
				return;
			}

			_display.asyncExec(new Runnable() {

				public void run() {

					if (__galleryItem.isDisposed() || _gallery.isDisposed()) {
						return;
					}

					final Rectangle galleryItemBounds = __galleryItem.getBounds();

//					System.out.println("redraw: " + galleryItemBounds);
//					// TODO remove SYSTEM.OUT.PRINTLN

					_gallery.redraw(
							galleryItemBounds.x,
							galleryItemBounds.y,
							galleryItemBounds.width,
							galleryItemBounds.height,
							false);
				}
			});
		}

// ORIGINAL
//		public void mediaLoaded(final IMedia media, final int definition, final Image img) {
//			ImageService.getInstance().acquire(img);
//			GalleryLibraryView.this.galleryImageCache.setImage(media, definition, img);
//
//			Display.getDefault().syncExec(new Runnable() {
//
//				public void run() {
//					if (LoadItemCallback.this.item.isDisposed() || LoadItemCallback.this.callbackGallery.isDisposed()) {
//						return;
//					}
//
//					final Rectangle bounds = LoadItemCallback.this.item.getBounds();
//
//					LoadItemCallback.this.callbackGallery
//							.redraw(bounds.x, bounds.y, bounds.width, bounds.height, false);
//				}
//			});
//		}
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

	void actionClearHistory() {

		final String selectedFolder = _comboPathHistory.getText();

		_comboPathHistory.removeAll();
		_comboPathHistory.add(selectedFolder);
		_comboPathHistory.select(0);

		_folderHistory.clear();
		_folderHistory.add(selectedFolder);

		_actionClearNavigationHistory.setEnabled(false);
		_actionNavigateBackward.setEnabled(false);
		_actionNavigateForward.setEnabled(false);
	}

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
				_picDirFolder.selectFolder(prevFolderPathName, false, true);
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
				_picDirFolder.selectFolder(prevFolderPathName, false, true);
			}
		});
	}

	private void createActions() {

		final ToolBarManager tbm = new ToolBarManager(_toolbar);

		_actionNavigateBackward = new ActionNavigateHistoryBackward(this);
		_actionNavigateForward = new ActionNavigateHistoryForward(this);

		tbm.add(_actionNavigateBackward);
		tbm.add(_actionNavigateForward);

		_toolbar.setMenu(createContextMenu(_toolbar));

		final ApplicationActionBarAdvisor actionbarAdvisor = TourbookPlugin
				.getDefault()
				.getApplicationActionBarAdvisor();

		actionbarAdvisor.registerAction(_actionNavigateBackward);
		actionbarAdvisor.registerAction(_actionNavigateForward);

		tbm.update(true);
	}

	/**
	 * create context menu
	 */
	private Menu createContextMenu(final Control parent) {

		_actionClearNavigationHistory = new ActionClearNavigationHistory(PicDirImages.this);

		final MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);

		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager menuMgr) {

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

	void createUI(final PicDirView picDirView, final PicDirFolder picDirFolder, final Composite parent) {

		_picDirView = picDirView;
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
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(_containerActionBar);
		{
			/*
			 * toolbar actions
			 */
			_toolbar = new ToolBar(_containerActionBar, SWT.FLAT);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_toolbar);

			/*
			 * combo: path history
			 */
			_comboPathHistory = new Combo(_containerActionBar, SWT.SIMPLE | SWT.DROP_DOWN);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_comboPathHistory);
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
			 * This combination of key and selection listener causes a folder selection only with
			 * the <Enter> key or with a mouse selection in the drop down box.
			 */
			_comboPathHistory.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(final KeyEvent e) {

					_isKey = true;

					if (e.keyCode == SWT.CR) {
						onSelectHistoryFolder(_comboPathHistory.getText());
					}
				}
			});

			_comboPathHistory.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {

					final boolean isKey = _isKey;
					_isKey = false;

					if (isKey == false) {
						onSelectHistoryFolder(_comboPathHistory.getText());
					}
				}
			});
		}
	}

	/**
	 * Create gallery
	 */
	private void createUI_20_PageGallery(final Composite parent) {

		_gallery = new GalleryMT(parent, SWT.V_SCROLL | SWT.VIRTUAL | SWT.MULTI);
		//		GridDataFactory.fillDefaults().grab(true, true).applyTo(_gallery);

		_gallery.setLowQualityOnUserAction(true);
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
				onSetGalleryItemData(event);
			}
		});

		_gallery.addListener(SWT.PaintItem, new Listener() {
			public void handleEvent(final Event event) {
				onPaintItem(event);
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
		_groupRenderer.setItemSize((int) (_photoSize * (float) 15 / 11), _photoSize);
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

	void dispose() {

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

	private void onPaintItem(final Event event) {

		final GalleryMTItem galleryItem = (GalleryMTItem) event.item;

		if (galleryItem != null && galleryItem.getParentItem() != null) {

			/*
			 * check if the photo image is available, if not, image must be loaded
			 */

			final Photo photo = (Photo) galleryItem.getData();

			final int imageQuality = _photoSize > PhotoManager.THUMBNAIL_DEFAULT_SIZE
					? PhotoManager.IMAGE_QUALITY_600
					: PhotoManager.IMAGE_QUALITY_THUMB_160;

			// check if image is already loaded or has an loading error
			final PhotoLoadingState photoLoadingState = photo.getLoadingState(imageQuality);
			if (photoLoadingState == PhotoLoadingState.IMAGE_HAS_A_LOADING_ERROR
					|| photoLoadingState == PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE) {
				return;
			}

			final Image photoImage = PhotoImageCache.getImage(photo.getImageKey(imageQuality));
			if (photoImage == null || photoImage.isDisposed()) {

				// the requested image is not available in the image cache -> image must be loaded

				final LoadImageCallback imageLoadCallback = new LoadImageCallback(galleryItem);

				PhotoManager.putImageInLoadingQueue(galleryItem, photo, imageQuality, imageLoadCallback);

//				final PhotoImageLoaderItem loaderItem = new PhotoImageLoaderItem(//
//						galleryItem,
//						photo,
//						imageQuality,
//						imageLoadCallback);
//
//				loaderItem.loadImage();
			}

// ORIGINAL
//			final IMedia m = (IMedia) galleryItem.getData(DATA_MEDIA);
//			final int definition = itemHeight > 140 ? IConstants.IMAGE_LOW : IConstants.IMAGE_THUMB;
//
//			Image img = getImageCache().getImage(m, definition);
//
//			if (img == null) {
//				img = getImageCache().getImage(m, itemHeight > 140 ? IConstants.IMAGE_THUMB : IConstants.IMAGE_LOW);
//
//				final LoadItemCallback callback = new LoadItemCallback(_gallery, galleryItem);
//
//				if (img == null && definition == IConstants.IMAGE_LOW) {
//					MediaDownload.getInstance().load(m, IConstants.IMAGE_THUMB, callback);
//				}
//				MediaDownload.getInstance().load(m, definition, callback);
//			}
		}
	}

	private void onSelectHistoryFolder(final String selectedFolder) {

		updateHistory(selectedFolder);

		BusyIndicator.showWhile(_display, new Runnable() {
			public void run() {
				_picDirFolder.selectFolder(selectedFolder, false, false);
			}
		});
	}

	private void onSetGalleryItemData(final Event event) {

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

	void restoreState(final IDialogSettings state) {

		final String[] historyEntries = Util.getStateArray(state, STATE_FOLDER_HISTORY, null);

		if (historyEntries != null) {

			// update history and combo
			for (final String history : historyEntries) {
				_folderHistory.add(history);
				_comboPathHistory.add(history);
			}
		}
	}

	void saveState(final IDialogSettings state) {

		state.put(STATE_FOLDER_HISTORY, _folderHistory.toArray(new String[_folderHistory.size()]));
	}

	void setThumbnailSize(final int imageSize) {

		_photoSize = imageSize;

		_groupRenderer.setItemSize((int) (_photoSize * (float) 15 / 11), _photoSize);
	}

	/**
	 * Display images for the selected folder.
	 * 
	 * @param imageFolder
	 * @param isNavigation
	 */
	void showImages(final File imageFolder, final boolean isNavigation) {

		//////////////////////////////////////////
		//
		// MUST BE REMOVED, IS ONLY FOR TESTING
		//
//		PhotoImageCache.dispose();
		//
		// MUST BE REMOVED, IS ONLY FOR TESTING
		//
		//////////////////////////////////////////

		if (imageFolder == null) {
			_lblLoading.setText(Messages.Pic_Dir_Label_FolderIsNotSelected);
		} else {

			_lblLoading.setText(NLS.bind(Messages.Pic_Dir_Label_Loading, imageFolder.getAbsolutePath()));

			if (isNavigation == false) {
				/*
				 * don't update history when the navigation in the history caused to display images
				 */
				updateHistory(imageFolder.getAbsolutePath());
			}
		}

		_pageBook.showPage(_pageLoading);

		PhotoManager.stopImageLoading();

		workerUpdate(imageFolder);
	}

	void updateColors(final Color fgColor, final Color bgColor) {

		/*
		 * action bar
		 */
		_containerActionBar.setForeground(fgColor);
		_containerActionBar.setBackground(bgColor);
		_toolbar.setForeground(fgColor);
		_toolbar.setBackground(bgColor);

		_comboPathHistory.setForeground(fgColor);
		_comboPathHistory.setBackground(bgColor);

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
		final int historySize = _folderHistory.size();
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

		// enabel/disable history navigation
		_selectedHistoryIndex = 0;
		_actionNavigateBackward.setEnabled(historySize > 1);
		_actionNavigateForward.setEnabled(false);
		_actionClearNavigationHistory.setEnabled(historySize > 1);

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

			_photoFiles = newPhotoFiles;

			_display.syncExec(new Runnable() {
				public void run() {

					// guard against the ui being closed before this runs
					if (_uiContainer.isDisposed()) {
						return;
					}

					// this will update the gallery
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
	 */
	private void workerUpdate(final File newFolder) {

		if (newFolder == null) {
			return;
		}

		if ((_workerNextFolder != null) && (_workerNextFolder.equals(newFolder))) {
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
