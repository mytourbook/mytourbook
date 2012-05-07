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
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Set;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.photo.gallery.MT20.GalleryMT20;
import net.tourbook.photo.gallery.MT20.GalleryMT20Item;
import net.tourbook.photo.manager.GallerySorting;
import net.tourbook.photo.manager.ILoadCallBack;
import net.tourbook.photo.manager.Photo;
import net.tourbook.photo.manager.PhotoImageCache;
import net.tourbook.photo.manager.PhotoManager;
import net.tourbook.photo.manager.PhotoWrapper;
import net.tourbook.photo.manager.ThumbnailStore;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.util.StatusUtil;
import net.tourbook.util.UI;
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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
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

	private static final String						STATE_FOLDER_HISTORY			= "STATE_FOLDER_HISTORY";			//$NON-NLS-1$
	private static final String						STATE_THUMB_IMAGE_SIZE			= "STATE_THUMB_IMAGE_SIZE";		//$NON-NLS-1$
	private static final String						STATE_GALLERY_POSITION_FOLDER	= "STATE_GALLERY_POSITION_FOLDER";	//$NON-NLS-1$
	private static final String						STATE_GALLERY_POSITION_VALUE	= "STATE_GALLERY_POSITION_VALUE";	//$NON-NLS-1$

	private static final String						DEFAULT_GALLERY_FONT			= "arial,sans-serif";				//$NON-NLS-1$

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
	public static Comparator<PhotoWrapper>			SORT_BY_FILE_DATE;
	public static Comparator<PhotoWrapper>			SORT_BY_FILE_NAME;

	/**
	 * Contains current gallery sorting id: {@link PicDirView#GALLERY_SORTING_BY_DATE} or
	 * {@link PicDirView#GALLERY_SORTING_BY_NAME}
	 */
	private GallerySorting							_sortingAlgorithm;

	private PicDirView								_picDirView;

	private PhotoRenderer							_photoRenderer;

	/**
	 * Folder which images are currently be displayed
	 */
	private File									_photoFolder;

	/**
	 * Contains ALL gallery items for the current photo folder
	 */
	private PhotoWrapper[]							_allPhotoWrapper;

	/**
	 * Contains filtered gallery items.
	 * <p>
	 * Only these items are displayed in the gallery, the {@link #_allPhotoWrapper} items contains
	 * also hidden gallery items.
	 */
	private PhotoWrapper[]							_filteredPhotoWrapper;

	private FileFilter								_fileFilter;

	private PicDirFolder							_picDirFolder;

	private boolean									_isComboKeyPressed;

	private int										_selectedHistoryIndex;
	private ArrayList<String>						_folderHistory					= new ArrayList<String>();

	private ActionNavigateHistoryBackward			_actionNavigateBackward;
	private ActionNavigateHistoryForward			_actionNavigateForward;
	private ActionClearNavigationHistory			_actionClearNavigationHistory;
	private ActionRemoveInvalidFoldersFromHistory	_actionRemoveInvalidFoldersFromHistory;

	private int										_prevThumbSize					= -1;

	/**
	 * keep gallery position for each used folder
	 */
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

	private GalleryMT20								_gallery;
	private CLabel									_lblStatusInfo;

	private PageBook								_pageBook;
	private Label									_lblLoading;
	private Composite								_pageLoading;
	private Composite								_containerStatusLine;

	private ImageSizeIndicator						_canvasImageSizeIndicator;

	private Font									_galleryFont;

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

		SORT_BY_FILE_DATE = new Comparator<PhotoWrapper>() {
			@Override
			public int compare(final PhotoWrapper wrapper1, final PhotoWrapper wrapper2) {

				if (_workerCancelled) {
					// couldn't find another way how to stop sorting
					return 0;
				}

				final long time1 = wrapper1.imageFileLastModified;
				final long time2 = wrapper2.imageFileLastModified;

				final long diff = time1 - time2;

				return diff < 0 ? -1 : diff > 0 ? 1 : 0;

//				return (int) (wrapper1.imageFileLastModified - wrapper2.imageFileLastModified);
			}
		};

		SORT_BY_FILE_NAME = new Comparator<PhotoWrapper>() {
			@Override
			public int compare(final PhotoWrapper wrapper1, final PhotoWrapper wrapper2) {

				if (_workerCancelled) {
					// couldn't find another way how to stop sorting
					return 0;
				}

				return wrapper1.imageFilePathName.compareToIgnoreCase(wrapper2.imageFilePathName);
			}
		};
	}

	//	/**
//	 * Sort files for the folder
//	 *
//	 * @param folder
//	 * @return
//	 */
//	private List<File> sortFiles(final File folder) {
//
//		final Comparator<PhotoWrapper> comparator = _sortingAlgorithm == GallerySorting.FILE_DATE
//				? SORT_BY_FILE_DATE
//				: SORT_BY_FILE_NAME;
//
//		// We make file list in this thread for speed reasons
//	final List<File>	files	= SortingUtils.getSortedFileList(folder, _fileFilter, comparator);

	class LoadImageCallback implements ILoadCallBack {

		private GalleryMT20Item	__galleryItem;

		/**
		 * @param galleryItem
		 */
		public LoadImageCallback(final GalleryMT20Item galleryItem) {

			__galleryItem = galleryItem;
		}

		@Override
		public void callBackImageIsLoaded(final boolean isUpdateUI) {

			if (isUpdateUI == false) {
				return;
			}

			// mark image area as needed to be redrawn
			_display.syncExec(new Runnable() {

				public void run() {

					if (_gallery.isDisposed()) {
						return;
					}

					/*
					 * Visibility check must be done in the UI thread because scrolling the gallery
					 * can reposition the gallery item. This can be a BIG problem because the
					 * redraw() method is painting the background color at the specified rectangle,
					 * this cost me a lot of time to figure it out.
					 */
					final boolean isItemVisible = __galleryItem.galleryMT20.isItemVisible(__galleryItem);

					if (isItemVisible) {

						// redraw gallery item WITH background
						_gallery.redraw(
								__galleryItem.viewPortX,
								__galleryItem.viewPortY,
								__galleryItem.width,
								__galleryItem.height,
								false);
					}
				}
			});
		}
	}

	private class PhotoGallery extends GalleryMT20 {

		public PhotoGallery(final Composite parent, final int style) {
			super(parent, style);
		}

		@Override
		public int initItem(final GalleryMT20Item galleryItem, final int filterIndex) {

			// initialize a wrapper for an image file

			final PhotoWrapper photoWrapper = _filteredPhotoWrapper[filterIndex];

			photoWrapper.photo = new Photo(photoWrapper);

			// set wrapper as custom data in the gallery item
			galleryItem.setData(photoWrapper, photoWrapper.imageFilePathName);

			return photoWrapper.wrapperIndex;
		}
	}

	PicDirImages(final PicDirView picDirView) {
		_picDirView = picDirView;
	}

//////// LOG ALL BINDINGS
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
		_actionRemoveInvalidFoldersFromHistory.setEnabled(false);
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

	private void createGalleryFont() {

		if (_galleryFont != null) {
			_galleryFont.dispose();
		}

		final String prefGalleryFont = _prefStore.getString(ITourbookPreferences.PHOTO_VIEWER_FONT);
		if (prefGalleryFont.length() > 0) {
			try {

				System.out.println(UI.timeStamp() + "setting gallery font: " + prefGalleryFont);

				_galleryFont = new Font(_display, new FontData(prefGalleryFont));

			} catch (final Exception e) {
				// ignore
			}
		}

		if (_galleryFont == null) {
			StatusUtil.log("This font cannot be created: \"" + prefGalleryFont + "\"");//$NON-NLS-1$ //$NON-NLS-2$
			_galleryFont = new Font(_display, DEFAULT_GALLERY_FONT, 7, SWT.NORMAL);
		}
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
				.numColumns(4)
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

			createUI_16_ComboHistory(_containerActionBar);
			createUI_17_ImageSize(_containerActionBar);
			createUI_18_ImageSizeIndicator(_containerActionBar);
		}
	}

	/**
	 * combo: path history
	 */
	void createUI_16_ComboHistory(final Composite parent) {

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
	private void createUI_17_ImageSize(final Composite parent) {

		_spinnerThumbSize = new Spinner(parent, SWT.BORDER);
		GridDataFactory.fillDefaults() //
				.align(SWT.BEGINNING, SWT.FILL)
				.applyTo(_spinnerThumbSize);
		_spinnerThumbSize.setMinimum(MIN_ITEM_WIDTH);
		_spinnerThumbSize.setMaximum(MAX_ITEM_WIDTH);
		_spinnerThumbSize.setIncrement(1);
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
				net.tourbook.ui.UI.adjustSpinnerValueOnMouseScroll(event);
				onSelectThumbnailSize(_spinnerThumbSize.getSelection());
			}
		});
	}

	/**
	 * canvas: image size indicator
	 */
	private void createUI_18_ImageSizeIndicator(final Composite parent) {

		_canvasImageSizeIndicator = new ImageSizeIndicator(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.hint(16, 16)
				.align(SWT.CENTER, SWT.CENTER)
				.applyTo(_canvasImageSizeIndicator);

		_canvasImageSizeIndicator.setToolTipText(Messages.Pic_Dir_ImageSizeIndicator_Tooltip);
	}

	/**
	 * Create gallery
	 */
	private void createUI_20_PageGallery(final Composite parent) {

		_gallery = new PhotoGallery(parent, SWT.MULTI | SWT.V_SCROLL | SWT.MULTI);

		_gallery.setHigherQualityDelay(200);
//		_gallery.setAntialias(SWT.OFF);
//		_gallery.setInterpolation(SWT.LOW);
		_gallery.setAntialias(SWT.ON);
		_gallery.setInterpolation(SWT.HIGH);
		_gallery.setItemMinMaxSize(MIN_ITEM_WIDTH, MAX_ITEM_WIDTH);

		_gallery.addListener(SWT.Modify, new Listener() {

			// a modify event is fired when gallery is zoomed in/out

			public void handleEvent(final Event event) {

				PhotoManager.stopImageLoading();

				updateUIAfterZoomInOut(event.width);
			}
		});

		/*
		 * set photo renderer
		 */
		_photoRenderer = new PhotoRenderer(_gallery, this);
		_gallery.setItemRenderer(_photoRenderer);

		_gallery.setFilterProvider(new PhotoFilter(_gallery, this));
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
		PhotoManager.stopImageLoading();
		ThumbnailStore.cleanupStoreFiles(true, true);
		PhotoImageCache.dispose();
	}

	int getThumbnailSize() {
		return _spinnerThumbSize.getSelection();
	}

	void handlePrefStoreModifications(final PropertyChangeEvent event) {

		final String property = event.getProperty();

		if (property.equals(ITourbookPreferences.PHOTO_VIEWER_PREF_EVENT_IMAGE_QUALITY_IS_MODIFIED)) {

			_display.asyncExec(new Runnable() {
				public void run() {

					if (MessageDialog.openQuestion(
							_display.getActiveShell(),
							Messages.Pic_Dir_Dialog_CleanupStoreImages_Title,
							Messages.Pic_Dir_Dialog_CleanupStoreImages_Message) == false) {

						disposeAllImages();
					}

					_gallery.updateGallery(false, _gallery.getGalleryPosition());
				}
			});

		} else if (property.equals(ITourbookPreferences.PHOTO_VIEWER_PREF_EVENT_IMAGE_VIEWER_UI_IS_MODIFIED)) {

			updateUIFromPrefStore();

		} else if (property.equals(ITourbookPreferences.PHOTO_VIEWER_FONT)) {

			onModifyFont();
		}
	}

	void onClose() {

		if (_galleryFont != null) {
			_galleryFont.dispose();
		}

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

	private void onModifyFont() {

		createGalleryFont();

		_photoRenderer.setFont(_galleryFont);

		// reset cached text size in the photos
		final GalleryMT20Item[] galleryItems = _gallery.getGalleryItems();
		if (galleryItems != null) {

// this is not yet implemented
//			for (final GalleryMT20Item galleryItem : galleryItems) {
//				final Photo photo = (Photo) galleryItem.data;
//				photo.resetCachedFontSizes();
//			}
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

		if (newThumbSize == _prevThumbSize) {
			// nothing has changed
			return;
		}

		final double galleryWidth = _gallery.getClientArea().width;
		final int prevPhotoWidth = _gallery.getItemWidth();

		final int prevNumberOfImages = _gallery.getNumberOfHorizontalImages();

		int newPhotoWidth = newThumbSize;

		if (newThumbSize > _prevThumbSize) {

			if (prevNumberOfImages > 2) {

				// increased width -> fewer images

				int numberOfImages = prevNumberOfImages - 1;

				newPhotoWidth = (int) (galleryWidth / numberOfImages);

				if (newPhotoWidth == prevPhotoWidth) {

					// size has not changed, decrease number of images until the width has changed

					while (newPhotoWidth == prevPhotoWidth) {

						if (numberOfImages < 3) {
							break;
						}

						newPhotoWidth = (int) (galleryWidth / --numberOfImages);
					}
				}

			} else {

				// number of photos is already 1, only increase photo width

				newPhotoWidth = newThumbSize;
			}

		} else {

			// decreased width -> more images

			final double tempImageCounter = galleryWidth / newThumbSize;

			/*
			 * size by number of images only, when more than 2 images are displayed, otherwise size
			 * by newThumbSize
			 */
			if (tempImageCounter > 2) {

				final int numberOfImages = prevNumberOfImages + 1;

				final int newTempPhotoWidth = (int) (galleryWidth / numberOfImages);
				if (newTempPhotoWidth > MIN_ITEM_WIDTH) {
					newPhotoWidth = newTempPhotoWidth;
				}
			}
		}

		if (newPhotoWidth == prevPhotoWidth) {
			// nothing has changed
			return;
		}

		/*
		 * update UI with new size
		 */

		PhotoManager.stopImageLoading();

		// update gallery
		final int newSize = _gallery.setItemSize(newPhotoWidth);
		if (newSize != newPhotoWidth) {

			/*
			 * size has been modified, this case can occure when the gallery is switching the
			 * scrollbars on/off depending on the content
			 */

			newPhotoWidth = newSize;
		}

		updateUIAfterZoomInOut(newPhotoWidth);
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

		onModifyFont();

		/*
		 * image quality
		 */
		updateUIFromPrefStore();

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
//		disposeAllImages();
		//
		// MUST BE REMOVED, IS ONLY FOR TESTING
		//
		//////////////////////////////////////////

		PhotoManager.stopImageLoading();

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

		workerUpdate(imageFolder, isReloadFolder);
	}

	void showInfo(	final boolean isShowPhotoName,
					final boolean isShowPhotoDate,
					final boolean isShowAnnotations,
					final boolean isUpdateGallery) {

		_photoRenderer.setShowLabels(isShowPhotoName, isShowPhotoDate, isShowAnnotations);

		if (isUpdateGallery) {
			_gallery.redraw();
		}
	}

//
//		return files;
//	}

	void sortGallery(final GallerySorting gallerySorting, final boolean isUpdateGallery) {

		// check if resorting is needed
		if (_sortingAlgorithm == gallerySorting) {
			return;
		}

		// set new sorting algorithm
		_sortingAlgorithm = gallerySorting;

		if (isUpdateGallery) {

			BusyIndicator.showWhile(_display, new Runnable() {
				public void run() {
					sortGallery_Runnable();
				}
			});
		}
	}

	/**
	 * This will sort the gallery items
	 */
	private void sortGallery_Runnable() {

		if (_allPhotoWrapper == null || _allPhotoWrapper.length == 0) {
			// there are no files
			return;
		}

		final GalleryMT20Item[] filteredGalleryItems = _gallery.getFilteredItems();
		if (filteredGalleryItems.length == 0) {
			// there are no items
			return;
		}

//		final int[] filterIndices = _gallery.getFilteredItemsIndices();
//
//		/*
//		 * create a map with all existing gallery items, an item can be null when not yet
//		 * displayed/initialized
//		 */
//		final HashMap<String, GalleryMT20Item> existingGalleryItemsMap = new HashMap<String, GalleryMT20Item>();
//
//		for (final int filterIndex : filterIndices) {
//
//			final GalleryMT20Item existingGalleryItem = galleryItems[filterIndex];
//
//			if (existingGalleryItem != null) {
//
//				final PhotoWrapper photoWrapper = (PhotoWrapper) existingGalleryItem.data;
//				final String photoImageFileName = photoWrapper.imageFilePathName;
//
//				existingGalleryItemsMap.put(photoImageFileName, existingGalleryItem);
//			}
//		}
//
//		/*
//		 * sort gallery items according to the sorted image files
//		 */
//		// sort image files
//		final List<File> sortedFiles = sortFiles(_photoFolder);
//		final File[] sortedFilesArray = sortedFiles.toArray(new File[sortedFiles.size()]);
//
//		final GalleryMT20Item[] sortedGalleryItems = new GalleryMT20Item[existingGalleryItems.length];
//
//		// convert sorted images files into sorted gallery items
//		for (int itemIndex = 0; itemIndex < sortedFilesArray.length; itemIndex++) {
//
//			final File imageFile = sortedFilesArray[itemIndex];
//
//			final GalleryMT20Item galleryItem = existingGalleryItemsMap.get(imageFile.getName());
//
//			if (galleryItem != null) {
//				sortedGalleryItems[itemIndex] = galleryItem;
//			}
//		}
//
//		_gallery.setSortedItems(sortedGalleryItems);
	}

//	/**
//	 * This will sort the gallery items
//	 */
//	private void sortGalleryRunnable_OLD() {
//
//		if (_photoFiles == null || _photoFiles.length == 0) {
//			// there are no files
//			return;
//		}
//
//		final GalleryMT20Item[] galleryItems = _gallery.getGalleryItems();
//		if (galleryItems.length == 0) {
//			// there is no root item
//			return;
//		}
//
//		// sort image files
//		final List<File> sortedFiles = sortFiles(_photoFolder);
//		final File[] sortedFilesArray = sortedFiles.toArray(new File[sortedFiles.size()]);
//
//		/*
//		 * sort gallery items according to the sorted image files
//		 */
//		final GalleryMT20Item rootItem = rootItems[0];
//		final GalleryMT20Item[] existingGalleryItems = rootItem.getItems();
//
//		final HashMap<String, GalleryMT20Item> existingGalleryItemsMap = new HashMap<String, GalleryMT20Item>();
//
//		// create map with existing gallery items, items can be null when not yet displayed
//		for (final GalleryMT20Item existingGalleryItem : existingGalleryItems) {
//			if (existingGalleryItem != null) {
//
//				final Photo photo = (Photo) existingGalleryItem.getData();
//				final String photoImageFileName = photo.getFileName();
//
//				existingGalleryItemsMap.put(photoImageFileName, existingGalleryItem);
//			}
//		}
//
//		final GalleryMT20Item[] sortedGalleryItems = new GalleryMT20Item[existingGalleryItems.length];
//
//		// convert sorted images files into sorted gallery items
//		for (int itemIndex = 0; itemIndex < sortedFilesArray.length; itemIndex++) {
//
//			final File imageFile = sortedFilesArray[itemIndex];
//
//			final GalleryMT20Item galleryItem = existingGalleryItemsMap.get(imageFile.getName());
//
//			if (galleryItem != null) {
//				sortedGalleryItems[itemIndex] = galleryItem;
//			}
//		}
//
//		_gallery.setSortedItems(sortedGalleryItems);
//	}

	void updateColors(final Color fgColor, final Color bgColor, final Color selectionFgColor) {

		/*
		 * action bar, setting action bar color in OSX looks not very good
		 */
		if (UI.IS_OSX == false) {

			_containerActionBar.setForeground(fgColor);
			_containerActionBar.setBackground(bgColor);
			_toolbar.setForeground(fgColor);
			_toolbar.setBackground(bgColor);

			// combobox list entries are almost invisible when colors are set on osx

			_comboPathHistory.setForeground(fgColor);
			_comboPathHistory.setBackground(bgColor);

			_spinnerThumbSize.setForeground(fgColor);
			_spinnerThumbSize.setBackground(bgColor);

			_canvasImageSizeIndicator.setForeground(fgColor);
			_canvasImageSizeIndicator.setBackground(bgColor);
		}

		/*
		 * gallery
		 */
		_gallery.setForeground(fgColor);
		_gallery.setBackground(bgColor);

		_photoRenderer.setColors(fgColor, bgColor, selectionFgColor);

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

	private void updateUIAfterZoomInOut(final int newPhotoWidth) {

		_spinnerThumbSize.setSelection(newPhotoWidth);
		_prevThumbSize = newPhotoWidth;

		final boolean isHqImage = newPhotoWidth > PhotoManager.IMAGE_SIZE_THUMBNAIL;
		_canvasImageSizeIndicator.setIndicator(isHqImage);

		_picDirView.setThumbnailSize(newPhotoWidth);
	}

	private void updateUIFromPrefStore() {

		/*
		 * image quality
		 */
		final boolean isShowHighQuality = _prefStore.getBoolean(//
				ITourbookPreferences.PHOTO_VIEWER_IS_SHOW_IMAGE_WITH_HIGH_QUALITY);

		final int hqMinSize = _prefStore.getInt(//
				ITourbookPreferences.PHOTO_VIEWER_HIGH_QUALITY_IMAGE_MIN_SIZE);

		_gallery.setImageQuality(isShowHighQuality, hqMinSize);

		/*
		 * text minimum thumb size
		 */
		final int textMinThumbSize = _prefStore.getInt(ITourbookPreferences.PHOTO_VIEWER_TEXT_MIN_THUMB_SIZE);
		_photoRenderer.setTextMinThumbSize(textMinThumbSize);
	}

	/**
	 * Updates the gallery contents
	 */
	private void workerExecute() {

		_workerStart = System.currentTimeMillis();

		PhotoWrapper[] newPhotoWrapper = null;

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

			// get all image files, sorting is not yet done
			final File[] files = _workerStateDir.listFiles(_fileFilter);

			// check if interruption occred
			if (_workerCancelled) {
				return;
			}

			if (files == null) {
				// prevent NPE
				newPhotoWrapper = new PhotoWrapper[0];
			} else {

				// image files are available

				final int numberOfImages = files.length;

				newPhotoWrapper = new PhotoWrapper[numberOfImages];

				// create a wrapper for each image file
				for (int fileIndex = 0; fileIndex < numberOfImages; fileIndex++) {
					newPhotoWrapper[fileIndex] = new PhotoWrapper(files[fileIndex]);
				}

				Arrays.sort(newPhotoWrapper, _sortingAlgorithm == GallerySorting.FILE_DATE
						? SORT_BY_FILE_DATE
						: SORT_BY_FILE_NAME);

				/*
				 * set gallery index in each gallery item, this index will never be modified until
				 * new gallery items are set
				 */
				for (int itemIndex = 0; itemIndex < numberOfImages; itemIndex++) {
					newPhotoWrapper[itemIndex].wrapperIndex = itemIndex;
				}
			}

			// check if the previous files retrival has been interrupted
			if (_workerCancelled) {
				return;
			}

			final File prevPhotoFolder = _photoFolder;

			_photoFolder = _workerStateDir;
			_allPhotoWrapper = newPhotoWrapper;
			_filteredPhotoWrapper = newPhotoWrapper;

			_display.syncExec(new Runnable() {
				public void run() {

					// guard against the ui being closed before this runs
					if (_uiContainer.isDisposed()) {
						return;
					}

					// keep current gallery position
					if (prevPhotoFolder != null) {
						_galleryPositions.put(prevPhotoFolder.getAbsolutePath(), _gallery.getGalleryPosition());
					}

					// get old gallery position
					final Double oldPosition = _galleryPositions.get(_photoFolder.getAbsolutePath());

					/*
					 * start gallery update
					 */
					_gallery.setupItems(_filteredPhotoWrapper.length, oldPosition);

					/*
					 * update status info
					 */
					final long timeDiff = System.currentTimeMillis() - _workerStart;
					final String timeDiffText = NLS.bind(
							Messages.Pic_Dir_Status_Loaded,
							new Object[] { Long.toString(timeDiff), Integer.toString(_filteredPhotoWrapper.length) });

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
