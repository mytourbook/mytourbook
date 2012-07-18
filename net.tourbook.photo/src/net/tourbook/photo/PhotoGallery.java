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
import java.lang.reflect.InvocationTargetException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.photo.internal.Activator;
import net.tourbook.photo.internal.ImageFilter;
import net.tourbook.photo.internal.ImageSizeIndicator;
import net.tourbook.photo.internal.Messages;
import net.tourbook.photo.internal.PhotoDateInfo;
import net.tourbook.photo.internal.PhotoRenderer;
import net.tourbook.photo.internal.PhotoToolTip;
import net.tourbook.photo.internal.gallery.MT20.FullSizeViewer;
import net.tourbook.photo.internal.gallery.MT20.GalleryMT20;
import net.tourbook.photo.internal.gallery.MT20.GalleryMT20Item;
import net.tourbook.photo.internal.gallery.MT20.IGalleryContextMenuProvider;
import net.tourbook.photo.internal.gallery.MT20.IGalleryCustomData;
import net.tourbook.photo.internal.gallery.MT20.IItemHovereredListener;
import net.tourbook.photo.internal.manager.GallerySorting;
import net.tourbook.photo.internal.manager.ImageUtils;
import net.tourbook.photo.internal.manager.PhotoImageMetadata;
import net.tourbook.photo.internal.manager.ThumbnailStore;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.progress.UIJob;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

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
public class PhotoGallery implements IItemHovereredListener, IGalleryContextMenuProvider {

	private static final String									MENU_ID_PHOTO_GALLERY			= "menu.net.tourbook.photo.photoGallery";	//$NON-NLS-1$

	private static final int									IMAGE_INDICATOR_SIZE			= 16;

	private static final int									DELAY_JOB_SUBSEQUENT_FILTER		= 500;										// ms
	private static final long									DELAY_JOB_UI_FILTER				= 200;										// ms
	private static final long									DELAY_JOB_UI_LOADING			= 200;										// ms

	public static final int										MIN_GALLERY_ITEM_WIDTH			= 10;										// pixel
	public static final int										MAX_GALLERY_ITEM_WIDTH			= 2000;									// pixel

	private static final String									STATE_THUMB_IMAGE_SIZE			= "STATE_THUMB_IMAGE_SIZE";				//$NON-NLS-1$
	private static final String									STATE_GALLERY_POSITION_FOLDER	= "STATE_GALLERY_POSITION_FOLDER";			//$NON-NLS-1$
	private static final String									STATE_GALLERY_POSITION_VALUE	= "STATE_GALLERY_POSITION_VALUE";			//$NON-NLS-1$
	private static final String									STATE_IMAGE_SORTING				= "STATE_IMAGE_SORTING";					//$NON-NLS-1$
	private static final String									STATE_SELECTED_ITEMS			= "STATE_SELECTED_ITEMS";					//$NON-NLS-1$

	private static final String									DEFAULT_GALLERY_FONT			= "arial,sans-serif";						//$NON-NLS-1$

	private final IPreferenceStore								_prefStore						= Activator
																										.getDefault()
																										.getPreferenceStore();

	/*
	 * worker thread management
	 */
	/**
	 * Worker start time
	 */
	private long												_workerStart;

	/**
	 * Lock for all worker control data and state
	 */
	private final Object										_workerLock						= new Object();

	/**
	 * The worker's thread
	 */
	private volatile Thread										_workerThread					= null;

	/**
	 * True if the worker must exit on completion of the current cycle
	 */
	private volatile boolean									_workerStopped					= false;

	/**
	 * True if the worker must cancel its operations prematurely perhaps due to a state update
	 */
	private volatile boolean									_workerCancelled				= false;

	/**
	 * Worker state information -- this is what gets synchronized by an update
	 */
	private volatile File										_workerStateDir					= null;

	/**
	 * State information to use for the next cycle
	 */
	private volatile File										_workerNextFolder				= null;

	/**
	 * Manages the worker's thread
	 */
	private final Runnable										_workerRunnable;

	/**
	 *
	 */
	public static Comparator<PhotoWrapper>						SORT_BY_FILE_DATE;
	public static Comparator<PhotoWrapper>						SORT_BY_FILE_NAME;

	/**
	 * Contains current gallery sorting id: {@link PicDirView#GALLERY_SORTING_BY_DATE} or
	 * {@link PicDirView#GALLERY_SORTING_BY_NAME}
	 */
	private Comparator<PhotoWrapper>							_currentComparator;
	private GallerySorting										_currentSorting;

	private PhotoRenderer										_photoRenderer;

	private FullSizeViewer										_fullSizeViewer;
	private PhotoToolTip										_photoTooltip;

	/**
	 * Folder which images are currently be displayed
	 */
	private File												_photoFolder;

	/**
	 * Folder which images should be displayed in the gallery
	 */
	private File												_photoFolderWhichShouldBeDisplayed;

	/**
	 * Contains photo wrapper for ALL gallery items for the current photo folder
	 */
	private PhotoWrapper[]										_allPhotoWrapper;

	private IPhotoGalleryProvider								_photoGalleryProvider;

	/**
	 * Contains filtered gallery items.
	 * <p>
	 * Only these items are displayed in the gallery, {@link #_allPhotoWrapper} items contains also
	 * hidden gallery items.
	 */
	private PhotoWrapper[]										_sortedAndFilteredPhotoWrapper;

	FileFilter													_fileFilter;

	private int													_prevGalleryItemSize			= -1;
	private int													_photoImageSize;
	private int													_photoBorderSize;

	private boolean												_isShowTooltip;

	/**
	 * keep gallery position for each used folder
	 */
	private LinkedHashMap<String, Double>						_galleryPositions;

	private int[]												_restoredSelection;

	private final NumberFormat									_nf1							= NumberFormat
																										.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
	}

	/**
	 * Cache for exif meta data, key is file path
	 */
	private ConcurrentLinkedHashMap<String, PhotoImageMetadata>	_exifCache;

	private ImageFilter											_currentImageFilter;

	private boolean												_filterJob1stRun;
	private boolean												_filterJobIsCanceled;

	private ReentrantLock										JOB_LOCK						= new ReentrantLock();
	private Job													_jobFilter;
	private AtomicBoolean										_jobFilterIsSubsequentScheduled	= new AtomicBoolean();
	private int													_jobFilterDirtyCounter;

	private UIJob												_jobUIFilter;
	private AtomicBoolean										_jobUIFilterJobIsScheduled		= new AtomicBoolean();
	private int													_jobUIFilterDirtyCounter;
	private PhotoWrapper[]										_jobUIFilterPhotoWrapper;

	private int													_currentExifRunId;

	private Job													_jobUILoading;
	private AtomicBoolean										_jobUILoadingIsScheduled		= new AtomicBoolean();
	private int													_jobUILoadingDirtyCounter;

	/*
	 * UI resources
	 */
	private Font												_galleryFont;

	/*
	 * UI controls
	 */
	private Display												_display;
	private Composite											_uiContainer;

	private Composite											_containerActionBar;
	private Composite											_customActionBarContainer;
	private ToolBar												_toolbar;
	private Spinner												_spinnerThumbSize;

	private GalleryImplementation								_gallery;
	private CLabel												_lblStatusLine;

	private PageBook											_pageBook;
	private Label												_lblLoading;
	private Composite											_pageLoading;
	private Composite											_containerStatusLine;

	private Composite											_pageGalleryInfo;
	private Label												_lblGalleryInfo;

	private ImageSizeIndicator									_canvasImageSizeIndicator;

	{
		_galleryPositions = new LinkedHashMap<String, Double>(100, 0.75f, true);

		_exifCache = new ConcurrentLinkedHashMap.Builder<String, PhotoImageMetadata>()//
				.maximumWeightedCapacity(20000)
				.build();

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

				final long diff = wrapper1.imageSortingTime - wrapper2.imageSortingTime;

				return diff < 0 ? -1 : diff > 0 ? 1 : 0;
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

	private class GalleryImplementation extends GalleryMT20 {

		public GalleryImplementation(final Composite parent, final int style) {
			super(parent, style);
		}

		@Override
		public IGalleryCustomData getCustomData(final int filterIndex) {

			if (filterIndex >= _sortedAndFilteredPhotoWrapper.length) {
				return null;
			}

			final PhotoWrapper photoWrapper = _sortedAndFilteredPhotoWrapper[filterIndex];

			if (photoWrapper.photo == null) {

				// create photo which is used to draw the photo image
				photoWrapper.photo = new Photo(photoWrapper);
			}

			return photoWrapper;
		}
	}

	private class LoadCallbackExif implements ILoadCallBack {

		private int		__runId;
		private Photo	__photo;

		public LoadCallbackExif(final Photo photo, final int runId) {

			__photo = photo;
			__runId = runId;
		}

		@Override
		public void callBackImageIsLoaded(final boolean isUpdateUI) {

			// keep exif metadata
			final PhotoImageMetadata metadata = __photo.getImageMetaDataRaw();

			if (metadata != null) {
				_exifCache.put(__photo.getPhotoWrapper().imageFilePathName, metadata);
			}

			if (__runId != _currentExifRunId) {

				// this callback is from an older run ID, ignore it

				return;
			}

			jobFilter_22_ScheduleSubsequent(DELAY_JOB_SUBSEQUENT_FILTER);
			jobUILoading_20_Schedule();
		}
	}

	public PhotoGallery(final Composite parent, final IPhotoGalleryProvider photoGalleryProvider) {

		_photoGalleryProvider = photoGalleryProvider;

		jobFilter_10_Create();
		jobUIFilter_10_Create();
		jobUILoading_10_Create();

		createUI(parent);
	}

	private void createGalleryFont() {

		if (_galleryFont != null) {
			_galleryFont.dispose();
		}

		final String prefGalleryFont = _prefStore.getString(IPhotoPreferences.PHOTO_VIEWER_FONT);
		if (prefGalleryFont.length() > 0) {
			try {

				System.out.println(UI.timeStamp() + "setting gallery font: " + prefGalleryFont); //$NON-NLS-1$

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

	/**
	 * @return Returns a selection with all selected photos.
	 */
	private PhotoSelection createPhotoSelection() {

		final Collection<GalleryMT20Item> allItems = _gallery.getSelection();

		final ArrayList<Photo> photos = new ArrayList<Photo>();

		for (final GalleryMT20Item item : allItems) {

			final IGalleryCustomData customData = item.customData;

			if (customData instanceof PhotoWrapper) {
				photos.add(((PhotoWrapper) customData).photo);
			}
		}

		return new PhotoSelection(photos);
	}

	private void createUI(final Composite parent) {

		_uiContainer = parent;
		_display = parent.getDisplay();

		_fileFilter = ImageUtils.createImageFileFilter();

		createUI_05(parent);
	}

	private void createUI_05(final Composite parent) {

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
				createUI_40_PageGalleryInfo(_pageBook);
			}

			createUI_50_StatusLine(container);
		}

		_photoTooltip = new PhotoToolTip(_gallery);
		_photoTooltip.setHideOnMouseMove(true);
		_gallery.addItemHoveredListener(this);

		// force that the 1st tab is in the gallery
		container.setTabList(new Control[] { _pageBook, _containerActionBar });
	}

	private void createUI_10_ActionBar(final Composite parent) {

		_containerActionBar = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_containerActionBar);
		GridLayoutFactory.fillDefaults()//
				.numColumns(5)
				.extendedMargins(0, 2, 2, 2)
				.spacing(3, 0)
				.applyTo(_containerActionBar);
		{
			/*
			 * toolbar actions
			 */
			_toolbar = new ToolBar(_containerActionBar, SWT.FLAT);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_toolbar);

			createUI_16_CustomActionBar(_containerActionBar);
			createUI_17_ImageSize(_containerActionBar);
			createUI_18_ImageSizeIndicator(_containerActionBar);
		}
	}

	private void createUI_16_CustomActionBar(final Composite parent) {

		_customActionBarContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_customActionBarContainer);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_customActionBarContainer);
	}

	/**
	 * spinner: thumb size
	 */
	private void createUI_17_ImageSize(final Composite parent) {

		_spinnerThumbSize = new Spinner(parent, SWT.BORDER);
		GridDataFactory.fillDefaults() //
				.align(SWT.BEGINNING, SWT.FILL)
				.applyTo(_spinnerThumbSize);
		_spinnerThumbSize.setMinimum(MIN_GALLERY_ITEM_WIDTH);
		_spinnerThumbSize.setMaximum(MAX_GALLERY_ITEM_WIDTH);
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
				Util.adjustSpinnerValueOnMouseScroll(event);
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
				.hint(IMAGE_INDICATOR_SIZE, IMAGE_INDICATOR_SIZE)
				.align(SWT.CENTER, SWT.CENTER)
				.applyTo(_canvasImageSizeIndicator);
	}

	/**
	 * Create gallery
	 */
	private void createUI_20_PageGallery(final Composite parent) {

		_gallery = new GalleryImplementation(parent, SWT.MULTI | SWT.V_SCROLL | SWT.MULTI);

		_gallery.setHigherQualityDelay(200);
//		_gallery.setAntialias(SWT.OFF);
//		_gallery.setInterpolation(SWT.LOW);
		_gallery.setAntialias(SWT.ON);
		_gallery.setInterpolation(SWT.HIGH);
		_gallery.setItemMinMaxSize(MIN_GALLERY_ITEM_WIDTH, MAX_GALLERY_ITEM_WIDTH);

		_gallery.addListener(SWT.Modify, new Listener() {

			// a modify event is fired when gallery is zoomed in/out

			public void handleEvent(final Event event) {

				PhotoLoadManager.stopImageLoading(false);

				updateUI_AfterZoomInOut(event.width);
			}
		});

		_gallery.addListener(SWT.Selection, new Listener() {

			// a gallery item is selected/deselected

			@Override
			public void handleEvent(final Event event) {
				onSelectPhoto();
			}
		});

		_gallery.setContextMenuProvider(this);
		_photoGalleryProvider.registerContextMenu(MENU_ID_PHOTO_GALLERY, _gallery.getContextMenuManager());

		_fullSizeViewer = _gallery.getFullsizeViewer();

		// set photo renderer which paints the image but also starts the image loading
		_photoRenderer = new PhotoRenderer(_gallery, this);

		_gallery.setItemRenderer(_photoRenderer);
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

	private void createUI_40_PageGalleryInfo(final PageBook parent) {

		_pageGalleryInfo = new Composite(parent, SWT.NONE);
//		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(1)
				.margins(5, 5)
				.applyTo(_pageGalleryInfo);
		{
			_lblGalleryInfo = new Label(_pageGalleryInfo, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.applyTo(_lblGalleryInfo);
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
			_lblStatusLine = new CLabel(_containerStatusLine, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblStatusLine);

		}
	}

	private void deselectAll() {

		_gallery.deselectAll();

		// update UI
		onSelectPhoto();
	}

	public void dispose() {

		// stop jobs
		jobFilter_12_Stop();

		if (_galleryFont != null) {
			_galleryFont.dispose();
		}

		PhotoLoadManager.stopImageLoading(true);

		//////////////////////////////////////////
		//
		// MUST BE REMOVED, IS ONLY FOR TESTING
		//
//		PhotoImageCache.dispose();
		//
		// MUST BE REMOVED, IS ONLY FOR TESTING
		//
		//////////////////////////////////////////

		workerStop();
	}

	/**
	 * Disposes and deletes all thumb images.
	 */
	private void disposeAndDeleteAllImages() {

		PhotoLoadManager.stopImageLoading(true);
		ThumbnailStore.cleanupStoreFiles(true, true);

		PhotoImageCache.disposeAll();

		_exifCache.clear();
	}

	@Override
	public void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(new Separator(UI.MENU_SEPARATOR_ADDITIONS));

//		menuMgr.add(_actionMergePhotosWithTours);

	}

	/**
	 * This is called when a filter button is pressed.
	 * 
	 * @param currentImageFilter
	 * @param isUpdateGallery
	 */
	public void filterGallery(final ImageFilter currentImageFilter) {

		_currentImageFilter = currentImageFilter;

		/*
		 * deselect all, this could be better implemented to keep selection, but is not yet done
		 */
		deselectAll();

		jobFilter_22_ScheduleSubsequent(0);
	}

	private Comparator<PhotoWrapper> getCurrentComparator() {
		return _currentSorting == GallerySorting.FILE_NAME ? SORT_BY_FILE_NAME : SORT_BY_FILE_DATE;
	}

	public Composite getCustomActionBar() {
		return _customActionBarContainer;
	}

	public FullSizeViewer getFullSizeViewer() {
		return _fullSizeViewer;
	}

	public Collection<GalleryMT20Item> getGallerySelection() {
		return _gallery.getSelection();
	}

	/**
	 * @param selectedFolder
	 * @param isGetAllImages
	 * @return Returns photo data for the images in the requested folder or <code>null</code> when
	 *         loading was canceled by the user.
	 */
	private ArrayList<PhotoWrapper> getLoadedExifImageData(final File selectedFolder, final boolean isGetAllImages) {

		final boolean isFolderFilesLoaded = _photoFolder.getAbsolutePath().equals(
				_photoFolderWhichShouldBeDisplayed.getAbsolutePath());

		if (PhotoLoadManager.getExifQueueSize() > 0 || isFolderFilesLoaded == false) {

			/*
			 * wait until all image exif data are loaded
			 */
			if (isEXIFDataLoaded() == false) {
				return null;
			}
		}

		PhotoWrapper[] sortedPhotoWrapper;

		if (isGetAllImages) {

			// get all filtered photos

			sortedPhotoWrapper = _sortedAndFilteredPhotoWrapper.clone();

		} else {

			// get all selected photos

			final Collection<GalleryMT20Item> galleryItems = _gallery.getSelection();

			sortedPhotoWrapper = new PhotoWrapper[galleryItems.size()];

			int itemIndex = 0;

			for (final GalleryMT20Item item : galleryItems) {

				final IGalleryCustomData customData = item.customData;

				if (customData instanceof PhotoWrapper) {
					sortedPhotoWrapper[itemIndex++] = (PhotoWrapper) customData;
				}
			}

		}

		// sort photos by date/time
		Arrays.sort(sortedPhotoWrapper, SORT_BY_FILE_DATE);

		final ArrayList<PhotoWrapper> sortedPhotos = new ArrayList<PhotoWrapper>(sortedPhotoWrapper.length);

		for (final PhotoWrapper photoWrapper : sortedPhotoWrapper) {
			sortedPhotos.add(photoWrapper);
		}

		return sortedPhotos;
	}

	public ISelection getMergePhotoTourSelection(final boolean isAllImages) {

		final ArrayList<PhotoWrapper> loadedExifData = getLoadedExifImageData(
				_photoFolderWhichShouldBeDisplayed,
				isAllImages);

		if (loadedExifData == null) {

			MessageDialog.openInformation(
					_display.getActiveShell(),
					Messages.Pic_Dir_Dialog_MergePhotos_Title,
					Messages.Pic_Dir_Dialog_MergePhotos_DialogInterrupted_Message);

			return null;
		}

		/*
		 * check if a photo is selected
		 */
		if (loadedExifData.size() == 0) {

			if (isAllImages) {

				MessageDialog.openInformation(
						_display.getActiveShell(),
						Messages.Pic_Dir_Dialog_MergePhotos_Title,
						NLS.bind(Messages.Pic_Dir_Dialog_MergePhotos_NoSelectedImagesInFolder_Message,//
								_photoFolderWhichShouldBeDisplayed.getAbsolutePath()));

			} else {

				MessageDialog.openInformation(
						_display.getActiveShell(),
						Messages.Pic_Dir_Dialog_MergePhotos_Title,
						Messages.Pic_Dir_Dialog_MergePhotos_NoSelectedImage_Message);
			}

			return null;
		}

		return new MergePhotoTourSelection(loadedExifData);
	}

	/**
	 * @return Returns folder which is currently displayed.
	 */
	public File getPhotoFolder() {
		return _photoFolder;
	}

	/**
	 * This message is displayed when no other message is displayed.
	 * 
	 * @return
	 */
	private String getStatusDefaultMessage() {

		final Collection<GalleryMT20Item> allSelectedPhoto = _gallery.getSelection();

		return NLS.bind(Messages.Pic_Dir_StatusLabel_SelectedImages, allSelectedPhoto.size());
	}

	public int getThumbnailSize() {
		return _spinnerThumbSize.getSelection();
	}

	public ToolBar getToolbar() {
		return _toolbar;
	}

	public void handlePrefStoreModifications(final PropertyChangeEvent event) {

		final String property = event.getProperty();

		if (property.equals(IPhotoPreferences.PHOTO_VIEWER_PREF_EVENT_IMAGE_QUALITY_IS_MODIFIED)) {

			_display.asyncExec(new Runnable() {
				public void run() {

					final MessageDialog messageDialog = new MessageDialog(
							_display.getActiveShell(),
							Messages.Pic_Dir_Dialog_CleanupStoreImages_Title,
							null,
							Messages.Pic_Dir_Dialog_CleanupStoreImages_Message,
							MessageDialog.QUESTION,
							new String[] {
									Messages.Pic_Dir_Dialog_CleanupStoreImages_KeepImages,
									Messages.Pic_Dir_Dialog_CleanupStoreImages_DiscardImages },
							0);

					if (messageDialog.open() == 1) {
						// discard all imaged
						disposeAndDeleteAllImages();
					}

					_gallery.updateGallery(false, _gallery.getGalleryPosition());

					updateUI_ImageIndicatorTooltip();
				}
			});

		} else if (property.equals(IPhotoPreferences.PHOTO_VIEWER_PREF_EVENT_IMAGE_VIEWER_UI_IS_MODIFIED)
				|| property.equals(IPhotoPreferences.PHOTO_VIEWER_PREF_EVENT_FULLSIZE_VIEWER_IS_MODIFIED)) {

			updateUI_FromPrefStore(true);

			updateUI_AfterZoomInOut(_prevGalleryItemSize);

		} else if (property.equals(IPhotoPreferences.PHOTO_VIEWER_FONT)) {

			onModifyFont();
		}
	}

	@Override
	public void hoveredItem(final GalleryMT20Item hoveredItem) {

		if (_isShowTooltip) {
			_photoTooltip.show(hoveredItem);
		}
	}

	public boolean isDisposed() {
		return _gallery.isDisposed();
	}

	/**
	 * @return Returns <code>true</code> when EXIF data for all (not filtered) photos are loaded.
	 */
	private boolean isEXIFDataLoaded() {

		final boolean isLoaded[] = new boolean[] { true };

		try {

			final IRunnableWithProgress runnable = new IRunnableWithProgress() {

				@Override
				public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

					final boolean isFolderFilesLoadedInitialValue = _photoFolder.getAbsolutePath().equals(
							_photoFolderWhichShouldBeDisplayed.getAbsolutePath());

					/*
					 * ensure files for the requested folder are read from the filesystem
					 */
					if (isFolderFilesLoadedInitialValue == false) {

						monitor.beginTask(NLS.bind(
								Messages.Pic_Dir_Dialog_LoadingFolderFiles,
								_photoFolderWhichShouldBeDisplayed), IProgressMonitor.UNKNOWN);

						boolean isFolderFilesLoaded = isFolderFilesLoadedInitialValue;

						while (isFolderFilesLoaded == false) {

							/*
							 * wait until files are loaded from the file system
							 */
							Thread.sleep(10);

							isFolderFilesLoaded = _photoFolder.getAbsolutePath().equals(
									_photoFolderWhichShouldBeDisplayed.getAbsolutePath());
						}

						/*
						 * wait until the loading job has started otherwise it is possible that the
						 * exif queue is empty and is checked before it is filled (this happened)
						 */
						Thread.sleep(100);
					}

					/*
					 * ensure all image EXIF data are loaded
					 */
					final int allPhotoSize = _allPhotoWrapper.length;
					monitor.beginTask(Messages.Pic_Dir_Dialog_LoadingEXIFData, IProgressMonitor.UNKNOWN);

					int exifLoadingQueueSize = PhotoLoadManager.getExifQueueSize();

					while (exifLoadingQueueSize > 0) {

						Thread.sleep(100);

						if (monitor.isCanceled()) {
							isLoaded[0] = false;
							return;
						}

						// show loading progress

						final int newExifLoadingQueueSize = PhotoLoadManager.getExifQueueSize();
						final double _percent = (double) (allPhotoSize - exifLoadingQueueSize) / allPhotoSize * 100.0;

						monitor.subTask(NLS.bind(Messages.Pic_Dir_Dialog_LoadingEXIFData_Subtask, new Object[] {
								exifLoadingQueueSize,
								allPhotoSize,
								_nf1.format(_percent) }));

						exifLoadingQueueSize = newExifLoadingQueueSize;
					}
				}
			};

			new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, true, runnable);

		} catch (final InvocationTargetException e) {
			StatusUtil.log(e);
		} catch (final InterruptedException e) {
			StatusUtil.log(e);
		}

		return isLoaded[0];
	}

	private void jobFilter_10_Create() {

		_jobFilter = new Job("PicDirImages: Filtering images") {//$NON-NLS-1$

			@Override
			protected IStatus run(final IProgressMonitor monitor) {

				_filterJobIsCanceled = false;
				_jobFilterIsSubsequentScheduled.set(false);

				try {

					if (_filterJob1stRun) {

						_filterJob1stRun = false;

						jobFilter_30_Run1st();

					} else {
						jobFilter_32_RunSubsequent();
					}

				} catch (final Exception e) {
					StatusUtil.log(e);
				}

				return Status.OK_STATUS;
			}
		};

		_jobFilter.setSystem(true);
	}

	private void jobFilter_12_Stop() {

		_filterJobIsCanceled = true;
		_jobUIFilterPhotoWrapper = null;

		// wait until the filter job has ended
		try {

			_jobFilter.cancel();
			_jobFilter.join();

		} catch (final InterruptedException e) {
			StatusUtil.log(e);
		}
	}

	private void jobFilter_20_Schedule1st() {

		// filter must be stopped before new wrappers are set
		jobFilter_12_Stop();

		JOB_LOCK.lock();
		{
			try {

				// this is the initial run of the filter job
				_filterJob1stRun = true;

				_sortedAndFilteredPhotoWrapper = new PhotoWrapper[0];

				_currentExifRunId++;

				_jobFilter.schedule();

			} finally {
				JOB_LOCK.unlock();
			}
		}

		/*
		 * hide status message for the first delay, because when nothing is filtered, the UI is
		 * flickering with an initial message
		 */
		updateUI_StatusMessageInUIThread(UI.EMPTY_STRING);
	}

	private void jobFilter_22_ScheduleSubsequent(final long delay) {

		JOB_LOCK.lock();
		{
			try {

				_jobFilterDirtyCounter++;

				if ((_jobFilter.getState() == Job.RUNNING) == false) {

					// filter job is NOT running, schedule it

					final boolean isScheduled = _jobFilterIsSubsequentScheduled.getAndSet(true);

					if (isScheduled == false) {
						_jobFilter.schedule(delay);
					}
				}

			} finally {
				JOB_LOCK.unlock();
			}
		}
	}

	private void jobFilter_23_ScheduleSubsequentWithoutRunCheck() {

		_jobFilterDirtyCounter++;

		// filter job is NOT running, schedule it

		final boolean isScheduled = _jobFilterIsSubsequentScheduled.getAndSet(true);

		if (isScheduled == false) {
			_jobFilter.schedule(DELAY_JOB_SUBSEQUENT_FILTER);
		}
	}

	/**
	 * Filter and sort photos and load EXIF data (which is required for sorting).
	 */
	private void jobFilter_30_Run1st() {

//		final long start = System.nanoTime();

		if (_allPhotoWrapper.length == 0) {

			// there are no images in the current folder,

			/*
			 * gallery MUST be updated even when no images are displayed because images from the
			 * previous folder are still displayed
			 */

			updateUI_GalleryInfo();

			return;
		}

		final boolean isGPSFilter = _currentImageFilter == ImageFilter.GPS;
		final boolean isNoGPSFilter = _currentImageFilter == ImageFilter.NoGPS;

		// get current dirty counter
		final int currentDirtyCounter = _jobFilterDirtyCounter;

		PhotoWrapper[] newFilteredWrapper = null;

		if (isGPSFilter || isNoGPSFilter) {

			final int numberOfWrapper = _allPhotoWrapper.length;
			final PhotoWrapper[] tempFilteredWrapper = new PhotoWrapper[numberOfWrapper];

			// filterindex is incremented when the filter contains a gallery item
			int filterIndex = 0;
			int wrapperIndex = 0;

			// loop: all photos
			for (final PhotoWrapper photoWrapper : _allPhotoWrapper) {

				if (_filterJobIsCanceled) {
					return;
				}

				int gpsState = photoWrapper.gpsState;

				if (gpsState == -1) {

					// image is not yet loaded, it must be loaded to get the gps state

					putInExifLoadingQueue(photoWrapper);
				}

				// check again, the gps state could have been cached and set
				gpsState = photoWrapper.gpsState;

				if (gpsState != -1) {

					if (isGPSFilter) {
						if (gpsState == 1) {

							tempFilteredWrapper[filterIndex] = _allPhotoWrapper[wrapperIndex];

							filterIndex++;
						}

					} else if (isNoGPSFilter) {

						if (gpsState == 0) {

							tempFilteredWrapper[filterIndex] = _allPhotoWrapper[wrapperIndex];

							filterIndex++;
						}
					}
				}

				wrapperIndex++;
			}

			// remove trailing array items which are not set
			newFilteredWrapper = Arrays.copyOf(tempFilteredWrapper, filterIndex);

		} else {

			// a filter is not set, display all images but load exif data which is necessary when sorting by date

			newFilteredWrapper = Arrays.copyOf(_allPhotoWrapper, _allPhotoWrapper.length);

			// loop: all photos
			for (final PhotoWrapper photoWrapper : _allPhotoWrapper) {

				if (_filterJobIsCanceled) {
					return;
				}

				final int gpsState = photoWrapper.gpsState;

				if (gpsState == -1) {

					// image is not yet loaded, it must be loaded to get the gps state
					putInExifLoadingQueue(photoWrapper);
				}
			}
		}

		// check sorting
//		if (_initialSorting != _currentSorting) {
//
//			/*
//			 * wrapper must be sorted because sorting is different than the initial sorting, this
//			 * will sort only the filtered wrapper
//			 */
//
		Arrays.sort(newFilteredWrapper, getCurrentComparator());
//		}

		/**
		 * Update UI
		 * <p>
		 * gallery MUST be updated even when no images are displayed because images from the
		 * previous folder are still displayed
		 */
		updateUI_GalleryItems(newFilteredWrapper);

		if (_jobFilterDirtyCounter > currentDirtyCounter) {

			// filter is dirty again

			jobFilter_23_ScheduleSubsequentWithoutRunCheck();

		} else {

			// clear progress bar

			jobUILoading_20_Schedule();
		}

//		final float timeDiff = (float) (System.nanoTime() - start) / 1000000;
////		if (timeDiff > 10) {}
//		System.out.println("filterJob_20_RunInitial:\t" + timeDiff + " ms\t"); //$NON-NLS-1$ //$NON-NLS-2$
//		// TODO remove SYSTEM.OUT.PRINTLN
	}

	/**
	 * Run filter and sorting again with newly loaded EXIF data until all EXIF data are loaded.
	 */
	private void jobFilter_32_RunSubsequent() {

//		final long start = System.nanoTime();

		final boolean isGPSFilter = _currentImageFilter == ImageFilter.GPS;
		final boolean isNoGPSFilter = _currentImageFilter == ImageFilter.NoGPS;

		// get current dirty counter
		final int currentDirtyCounter = _jobFilterDirtyCounter;

		PhotoWrapper[] newFilteredWrapper = null;

		if (isGPSFilter || isNoGPSFilter) {

			final int numberOfWrapper = _allPhotoWrapper.length;
			final PhotoWrapper[] tempFilteredWrapper = new PhotoWrapper[numberOfWrapper];

			// filterindex is incremented when the filter contains a gallery item
			int filterIndex = 0;
			int wrapperIndex = 0;

			// loop: all photos
			for (final PhotoWrapper photoWrapper : _allPhotoWrapper) {

				if (_filterJobIsCanceled) {
					return;
				}

				final int gpsState = photoWrapper.gpsState;

				if (isGPSFilter) {
					if (gpsState == 1) {

						tempFilteredWrapper[filterIndex] = _allPhotoWrapper[wrapperIndex];

						filterIndex++;
					}

				} else if (isNoGPSFilter) {

					if (gpsState == 0) {

						tempFilteredWrapper[filterIndex] = _allPhotoWrapper[wrapperIndex];

						filterIndex++;
					}
				}

				wrapperIndex++;
			}

			// remove trailing array items which are not set
			newFilteredWrapper = Arrays.copyOf(tempFilteredWrapper, filterIndex);

		} else {

			// a filter is not set, display all images but load exif data which is necessary when filtering by date

			newFilteredWrapper = Arrays.copyOf(_allPhotoWrapper, _allPhotoWrapper.length);
		}

		// check sorting
//		if (_initialSorting != _currentSorting) {
//
//			/*
//			 * wrapper must be sorted because sorting is different than the initial sorting, this
//			 * will sort only the filtered wrapper
//			 */
//
		Arrays.sort(newFilteredWrapper, getCurrentComparator());
//		}

		/**
		 * UI update must be run in a UI job because the update can be very long when many
		 * (thousands) small images are displayed
		 */
		_jobUIFilterPhotoWrapper = newFilteredWrapper;

		jobUIFilter_20_Schedule(0);

		if (_jobFilterDirtyCounter > currentDirtyCounter) {

			// filter is dirty again

			jobFilter_23_ScheduleSubsequentWithoutRunCheck();

		} else {

			// clear progress bar

			jobUILoading_20_Schedule();
		}

//		final float timeDiff = (float) (System.nanoTime() - start) / 1000000;
//		System.out.println("filterJob_30_RunSubsequent:\t" + timeDiff + " ms\t"); //$NON-NLS-1$ //$NON-NLS-2$
//		// TODO remove SYSTEM.OUT.PRINTLN
	}

	private void jobUIFilter_10_Create() {

		_jobUIFilter = new UIJob("PicDirImages: Update UI with filtered gallery images") { //$NON-NLS-1$

			@Override
			public IStatus runInUIThread(final IProgressMonitor monitor) {

				_jobUIFilterJobIsScheduled.set(false);

				try {
					jobUIFilter_30_Run();
				} catch (final Exception e) {
					StatusUtil.log(e);
				}

				return Status.OK_STATUS;
			}
		};

		_jobUIFilter.setSystem(true);
	}

	private void jobUIFilter_20_Schedule(final long delay) {

		JOB_LOCK.lock();
		{
			try {

				_jobUIFilterDirtyCounter++;

				if ((_jobUIFilter.getState() == Job.RUNNING) == false) {

					// filter job is NOT running, schedule it

					final boolean isScheduled = _jobUIFilterJobIsScheduled.getAndSet(true);

					if (isScheduled == false) {
						_jobUIFilter.schedule(delay);
					}
				}

			} finally {
				JOB_LOCK.unlock();
			}
		}
	}

	private void jobUIFilter_30_Run() {

		final PhotoWrapper[] uiUpdatePhotoWrapper = _jobUIFilterPhotoWrapper;
		_jobUIFilterPhotoWrapper = null;

		if (uiUpdatePhotoWrapper == null) {
			return;
		}

		final int currentDirtyCounter = _jobUIFilterDirtyCounter;

		updateUI_GalleryItems(uiUpdatePhotoWrapper);

		if (_jobUIFilterDirtyCounter > currentDirtyCounter) {
			// UI is dirty again
			jobUIFilter_20_Schedule(DELAY_JOB_UI_FILTER);
		}
	}

	private void jobUILoading_10_Create() {

		_jobUILoading = new UIJob("PicDirImages: Update UI when loading images") {//$NON-NLS-1$

			@Override
			public IStatus runInUIThread(final IProgressMonitor monitor) {

				_jobUILoadingIsScheduled.set(false);

				try {
					jobUILoading_30_Run();
				} catch (final Exception e) {
					StatusUtil.log(e);
				}

				return Status.OK_STATUS;
			}

		};

		_jobUILoading.setSystem(true);
	}

	public void jobUILoading_20_Schedule() {

		JOB_LOCK.lock();
		{
			try {

				_jobUILoadingDirtyCounter++;

				if ((_jobUILoading.getState() == Job.RUNNING) == false) {

					// UI job is NOT running, schedule it

					final boolean isScheduled = _jobUILoadingIsScheduled.getAndSet(true);

					if (isScheduled == false) {
						_jobUILoading.schedule(DELAY_JOB_UI_LOADING);
					}
				}

			} finally {
				JOB_LOCK.unlock();
			}
		}
	}

	private void jobUILoading_21_ScheduleWithoutRunCheck() {

		_jobUILoadingDirtyCounter++;

		final boolean isScheduled = _jobUILoadingIsScheduled.getAndSet(true);

		if (isScheduled == false) {
			_jobUILoading.schedule(DELAY_JOB_UI_LOADING);
		}
	}

	private void jobUILoading_30_Run() {

		final int currentDirtyCounter = _jobUILoadingDirtyCounter;

		final int exifQueueSize = PhotoLoadManager.getExifQueueSize();
		final int imageQueueSize = PhotoLoadManager.getImageQueueSize();
		final int imageHQQueueSize = PhotoLoadManager.getImageHQQueueSize();

		if (exifQueueSize > 0) {

			updateUI_StatusMessageInUIThread(NLS.bind(Messages.Pic_Dir_StatusLabel_LoadingImagesFilter, new Object[] {
					imageQueueSize,
					imageHQQueueSize,
					exifQueueSize }));

		} else if (imageQueueSize > 0 || imageHQQueueSize > 0) {

			updateUI_StatusMessageInUIThread(NLS.bind(//
					Messages.Pic_Dir_StatusLabel_LoadingImages,
					new Object[] { imageQueueSize, imageHQQueueSize, exifQueueSize }));

		} else {

			// hide last message

			updateUI_StatusMessageInUIThread(UI.EMPTY_STRING);
		}

		if (_jobUILoadingDirtyCounter > currentDirtyCounter) {

			// UI is dirty again, schedule it again

			jobUILoading_21_ScheduleWithoutRunCheck();
		}
	}

	private void onModifyFont() {

		createGalleryFont();

		_photoRenderer.setFont(_galleryFont);
	}

	private void onSelectPhoto() {

		// show default message
		updateUI_StatusMessage(UI.EMPTY_STRING);

		// fire selection
		_photoGalleryProvider.setSelection(createPhotoSelection());
	}

	private void onSelectThumbnailSize(final int newImageSize) {

		int newGalleryItemSize = newImageSize + _photoBorderSize;

		if (newGalleryItemSize == _prevGalleryItemSize) {
			// nothing has changed
			return;
		}

		final int prevGalleryItemSize = _gallery.getItemWidth();

		// update gallery
		final int adjustedItemSize = _gallery.zoomGallery(newGalleryItemSize);

		if (adjustedItemSize == prevGalleryItemSize) {
			// nothing has changed
			return;
		}

		PhotoLoadManager.stopImageLoading(false);

		if (adjustedItemSize != newGalleryItemSize) {

			/*
			 * size has been modified, this case can occure when the gallery is switching the
			 * scrollbars on/off depending on the content
			 */

			newGalleryItemSize = adjustedItemSize;
		}

		updateUI_AfterZoomInOut(newGalleryItemSize);
	}

	public void putInExifCache(final String imageFilePathName, final PhotoImageMetadata metadata) {
		_exifCache.put(imageFilePathName, metadata);
	}

	/**
	 * Get gps state and exif data
	 * 
	 * @return Returns <code>true</code> when exif data is already available from the cache and must
	 *         not be loaded.
	 */
	private boolean putInExifLoadingQueue(final PhotoWrapper photoWrapper) {

		// create photo which is used to draw the photo image
		Photo photo = photoWrapper.photo;

		if (photo == null) {
			photo = photoWrapper.photo = new Photo(photoWrapper);
		}

		final PhotoImageMetadata photoImageMetadata = _exifCache.get(photoWrapper.imageFilePathName);

		if (photoImageMetadata != null) {

			photo.updateImageMetadata(photoImageMetadata);

			return true;
		}

		PhotoLoadManager.putImageInLoadingQueueExif(photo, new LoadCallbackExif(photo, _currentExifRunId));

		return false;
	}

	public void redrawGallery(	final int viewPortX,
								final int viewPortY,
								final int width,
								final int height,
								final boolean all) {
		_gallery.redraw(viewPortX, viewPortY, width, height, all);
	}

	/**
	 * Remove all cached metadata which starts with the folder path.
	 * 
	 * @param folderPath
	 */
	public void removeCachedExifData(final String folderPath) {

		// remove cached exif data
		for (final String cachedPath : _exifCache.keySet()) {
			if (cachedPath.startsWith(folderPath)) {
				_exifCache.remove(cachedPath);
			}
		}
	}

	public void restoreInfo(final boolean isShowPhotoName,
							final PhotoDateInfo photoDateInfo,
							final boolean isShowAnnotations,
							final boolean isShowTooltip) {

		_photoRenderer.setShowLabels(isShowPhotoName, photoDateInfo, isShowAnnotations);

		_isShowTooltip = isShowTooltip;
	}

	public void restoreState(final IDialogSettings state) {

		_gallery.restoreState(state);

		// set font
		onModifyFont();

		/*
		 * gallery sorting
		 */
		final GallerySorting defaultSorting = GallerySorting.FILE_NAME;
		final String stateValue = Util.getStateString(state, STATE_IMAGE_SORTING, defaultSorting.name());
		try {
			_currentSorting = GallerySorting.valueOf(stateValue);
		} catch (final Exception e) {
			_currentSorting = defaultSorting;
		}

		// pref store settings
		updateUI_FromPrefStore(false);

		updateUI_ImageIndicatorTooltip();

		/**
		 * !!! very important !!!
		 * <p>
		 * show gallery to initialize client area, otherwise the width is 0 until the page is
		 * displayed in a later step
		 */
		_pageBook.showPage(_gallery);

		// show loading page until a folder is selected
		_lblLoading.setText(Messages.Pic_Dir_Label_ReadingFolders);
		_pageBook.showPage(_pageLoading);

		/**
		 * set thumbnail size after gallery client area is set
		 */
		final int stateThumbSize = Util.getStateInt(
				state,
				STATE_THUMB_IMAGE_SIZE,
				PhotoLoadManager.IMAGE_SIZE_THUMBNAIL);
		_spinnerThumbSize.setSelection(stateThumbSize);

		// restore thumbnail image size
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

		_restoredSelection = Util.getState(state, STATE_SELECTED_ITEMS, null);
	}

	public void saveState(final IDialogSettings state) {

		state.put(STATE_THUMB_IMAGE_SIZE, _photoImageSize);

		state.put(STATE_IMAGE_SORTING, _currentSorting.name());

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

		Util.setState(state, STATE_SELECTED_ITEMS, _gallery.getSelectionIndex());

		_gallery.saveState(state);
	}

	/**
	 * This is called when a filter button is pressed.
	 * 
	 * @param currentImageFilter
	 */
	public void setFilter(final ImageFilter currentImageFilter) {
		_currentImageFilter = currentImageFilter;
	}

	public void setFocus() {
//		_gallery.setFocus();
	}

	public void setSorting(final GallerySorting gallerySorting) {

		// set new sorting algorithm
		_currentSorting = gallerySorting;
	}

	/**
	 * Display images for the selected folder.
	 * 
	 * @param imageFolder
	 * @param isReloadFolder
	 */
	public void showImages(final File imageFolder, final boolean isReloadFolder) {

		jobFilter_12_Stop();

		PhotoLoadManager.stopImageLoading(true);

		//////////////////////////////////////////
		//
		// MUST BE REMOVED, IS ONLY FOR TESTING
		//
//		disposeAndDeleteAllImages();
//		PhotoLoadManager.removeInvalidImageFiles();
		//
		// MUST BE REMOVED, IS ONLY FOR TESTING
		//
		//////////////////////////////////////////

		if (imageFolder == null) {
			_lblLoading.setText(Messages.Pic_Dir_Label_ReadingFolders);
		} else {

			_lblLoading.setText(NLS.bind(Messages.Pic_Dir_Label_Loading, imageFolder.getAbsolutePath()));
		}

		_pageBook.showPage(_pageLoading);

		_photoFolderWhichShouldBeDisplayed = imageFolder;

		workerUpdate(imageFolder, isReloadFolder);
	}

	/**
	 * @param isShowPhotoName
	 * @param photoDateInfo
	 * @param isShowAnnotations
	 * @param isShowTooltip
	 */
	public void showInfo(	final boolean isShowPhotoName,
							final PhotoDateInfo photoDateInfo,
							final boolean isShowAnnotations,
							final boolean isShowTooltip) {

		_photoRenderer.setShowLabels(isShowPhotoName, photoDateInfo, isShowAnnotations);

		_isShowTooltip = isShowTooltip;

		// reset tooltip, otherwise it could be displayed if it should not
		_photoTooltip.reset();

		_gallery.redraw();
	}

	public void showRestoreFolder(final String restoreFolderName) {

		if (restoreFolderName == null) {
			_lblLoading.setText(Messages.Pic_Dir_StatusLabel_NoFolder);
		} else {
			_lblLoading.setText(NLS.bind(Messages.Pic_Dir_StatusLabel_RestoringFolder, restoreFolderName));
		}

		_pageBook.showPage(_pageLoading);
	}

	public void sortGallery(final GallerySorting gallerySorting) {

		// check if resorting is needed
		if (_currentSorting == gallerySorting) {
			return;
		}

		/*
		 * deselect all, this could be better implemented to keep selection, but is not yet done
		 */
		deselectAll();

		// set new sorting algorithm
		_currentSorting = gallerySorting;

		BusyIndicator.showWhile(_display, new Runnable() {
			public void run() {
				sortGallery_10_Runnable();
			}
		});
	}

	/**
	 * This will sort all already created gallery items
	 */
	private void sortGallery_10_Runnable() {

		if (_allPhotoWrapper == null || _allPhotoWrapper.length == 0) {
			// there are no files
			return;
		}

		final GalleryMT20Item[] virtualGalleryItems = _gallery.getAllVirtualItems();
		final int virtualSize = virtualGalleryItems.length;

		if (virtualSize == 0) {
			// there are no items
			return;
		}

		// sort photos with new sorting algorithm
		Arrays.sort(_sortedAndFilteredPhotoWrapper, getCurrentComparator());

		updateUI_GalleryItems(_sortedAndFilteredPhotoWrapper);
	}

	/**
	 * @param fgColor
	 * @param bgColor
	 * @param selectionFgColor
	 * @param noFocusSelectionFgColor
	 * @param initUI
	 *            Is <code>true</code> after a restore to update the UI that not a default UI color
	 *            is displayed.
	 */
	public void updateColors(	final Color fgColor,
								final Color bgColor,
								final Color selectionFgColor,
								final Color noFocusSelectionFgColor,
								final boolean initUI) {

		/*
		 * action bar, setting action bar color in OSX looks not very good
		 */
		if (UI.IS_OSX == false) {

			_containerActionBar.setForeground(fgColor);
			_containerActionBar.setBackground(bgColor);
			_toolbar.setForeground(fgColor);
			_toolbar.setBackground(bgColor);

			_spinnerThumbSize.setForeground(fgColor);
			_spinnerThumbSize.setBackground(bgColor);

			_canvasImageSizeIndicator.setForeground(fgColor);
			_canvasImageSizeIndicator.setBackground(bgColor);
		}

		/*
		 * gallery
		 */
		_gallery.setColors(fgColor, bgColor);

		_photoRenderer.setColors(fgColor, bgColor, selectionFgColor, noFocusSelectionFgColor);

		_pageLoading.setBackground(bgColor);

		if (initUI) {
//			_gallery.redraw();
//			_gallery.update();
		}

		/*
		 * status line
		 */
		_containerStatusLine.setForeground(fgColor);
		_containerStatusLine.setBackground(bgColor);

		_lblStatusLine.setForeground(fgColor);
		_lblStatusLine.setBackground(bgColor);

		/*
		 * loading page
		 */
		_lblLoading.setForeground(fgColor);
		_lblLoading.setBackground(bgColor);

		/*
		 * page: folder info
		 */
		_pageGalleryInfo.setForeground(fgColor);
		_pageGalleryInfo.setBackground(bgColor);
		_lblGalleryInfo.setForeground(fgColor);
		_lblGalleryInfo.setBackground(bgColor);
	}

	private void updateUI_AfterZoomInOut(final int galleryItemSize) {

		final int imageSize = galleryItemSize - _photoBorderSize;

		if (imageSize != _photoImageSize) {

			// image size has changed

			_photoImageSize = imageSize;

			_spinnerThumbSize.setSelection(imageSize);
			_prevGalleryItemSize = galleryItemSize;

			final boolean isHqImage = imageSize > PhotoLoadManager.IMAGE_SIZE_THUMBNAIL;
			_canvasImageSizeIndicator.setIndicator(isHqImage);

			_photoGalleryProvider.setThumbnailSize(imageSize);

			_photoTooltip.setImageSize(_photoImageSize);
			_photoTooltip.reset();
		}
	}

	private void updateUI_FromPrefStore(final boolean isUpdateUI) {

		/*
		 * image quality
		 */
		final boolean isShowHighQuality = _prefStore.getBoolean(//
				IPhotoPreferences.PHOTO_VIEWER_IS_SHOW_IMAGE_WITH_HIGH_QUALITY);

		final int hqMinSize = _prefStore.getInt(//
				IPhotoPreferences.PHOTO_VIEWER_HIGH_QUALITY_IMAGE_MIN_SIZE);

		_gallery.setImageQuality(isShowHighQuality, hqMinSize);

		/*
		 * text minimum thumb size
		 */
		_photoRenderer.setTextMinThumbSize(_prefStore.getInt(IPhotoPreferences.PHOTO_VIEWER_TEXT_MIN_THUMB_SIZE));
		_photoRenderer.setImageBorderSize(_prefStore.getInt(IPhotoPreferences.PHOTO_VIEWER_IMAGE_BORDER_SIZE));

		// get update border size
		_photoBorderSize = _photoRenderer.getBorderSize();

		_fullSizeViewer.setPrefSettings(isUpdateUI);
	}

	private void updateUI_GalleryInfo() {

		_display.syncExec(new Runnable() {
			public void run() {

				if (_gallery.isDisposed()) {
					return;
				}

				_pageBook.showPage(_pageGalleryInfo);

				final int imageCount = _allPhotoWrapper.length;

				if (imageCount == 0) {

					_lblGalleryInfo.setText(Messages.Pic_Dir_StatusLabel_NoImagesInFolder);

				} else {

					final int exifLoadingQueueSize = PhotoLoadManager.getExifQueueSize();

					if (exifLoadingQueueSize > 0) {

						// show filter message only when image files are being loaded

						_lblGalleryInfo.setText(NLS.bind(
								Messages.Pic_Dir_StatusLabel_FilteringImages,
								exifLoadingQueueSize));

					} else {

						if (_sortedAndFilteredPhotoWrapper.length == 0) {

							if (imageCount == 1) {
								_lblGalleryInfo.setText(Messages.Pic_Dir_StatusLabel_1ImageIsHiddenByFilter);
							} else {
								_lblGalleryInfo.setText(NLS.bind(
										Messages.Pic_Dir_StatusLabel_nImagesAreHiddenByFilter,
										imageCount));
							}

						} else {
							// this case should not happen, the gallery is displayed
						}
					}
				}
			}
		});
	}

	/*
	 * set gallery items into a list according to the new sorting/filtering
	 */
	private void updateUI_GalleryItems(final PhotoWrapper[] filteredAndSortedWrapper) {

		final HashMap<String, GalleryMT20Item> existingGalleryItems = _gallery.getCreatedGalleryItems();

		final int wrapperSize = filteredAndSortedWrapper.length;
		final GalleryMT20Item[] sortedGalleryItems = new GalleryMT20Item[wrapperSize];

		// convert sorted photos into sorted gallery items
		for (int itemIndex = 0; itemIndex < wrapperSize; itemIndex++) {

			final PhotoWrapper sortedPhotoWrapper = filteredAndSortedWrapper[itemIndex];

			// get gallery item for the current photo
			final GalleryMT20Item galleryItem = existingGalleryItems.get(sortedPhotoWrapper.imageFilePathName);

			if (galleryItem != null) {
				sortedGalleryItems[itemIndex] = galleryItem;
			}
		}

		_sortedAndFilteredPhotoWrapper = filteredAndSortedWrapper;

		_display.syncExec(new Runnable() {
			public void run() {

				if (_gallery.isDisposed()) {
					return;
				}

				if (sortedGalleryItems.length > 0) {

					// gallery items are available

					_pageBook.showPage(_gallery);

					// update gallery
					_gallery.setVirtualItems(sortedGalleryItems);

				} else {

					// there is no gallery item

					updateUI_GalleryInfo();
				}
			}
		});
	}

	private void updateUI_ImageIndicatorTooltip() {

		_canvasImageSizeIndicator.setToolTipText(NLS.bind(
				Messages.Pic_Dir_ImageSizeIndicator_Tooltip,
				_prefStore.getString(IPhotoPreferences.PHOTO_VIEWER_IMAGE_FRAMEWORK)));
	}

	public void updateUI_StatusMessage(final String message) {

		if (message.length() == 0) {
			_lblStatusLine.setText(getStatusDefaultMessage());
		} else {
			_lblStatusLine.setText(message);
		}
	}

	public void updateUI_StatusMessageInUIThread(final String message) {

		_display.asyncExec(new Runnable() {
			public void run() {

				if (_gallery.isDisposed()) {
					return;
				}

				if (message.length() == 0) {
					_lblStatusLine.setText(getStatusDefaultMessage());
				} else {
					_lblStatusLine.setText(message);
				}
			}
		});
	}

	/**
	 * Get image files from current directory
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

					_lblStatusLine.setText(UI.EMPTY_STRING);
				}
			});

			// get all image files, sorting is not yet done
			final File[] files = _workerStateDir.listFiles(_fileFilter);

			// check if interruption occred
			if (_workerCancelled) {
				return;
			}

			int numberOfImages = 0;

			if (files == null) {
				// prevent NPE
				newPhotoWrapper = new PhotoWrapper[0];
			} else {

				// image files are available

				numberOfImages = files.length;

				newPhotoWrapper = new PhotoWrapper[numberOfImages];

				// create a wrapper for each image file
				for (int fileIndex = 0; fileIndex < numberOfImages; fileIndex++) {
					newPhotoWrapper[fileIndex] = new PhotoWrapper(files[fileIndex]);
				}

				/*
				 * sort wrappers with currently selected comparator
				 */
				_currentComparator = getCurrentComparator();

				// keep initial sorting algorithm
//				_initialSorting = _currentSorting;

				Arrays.sort(newPhotoWrapper, _currentComparator);
			}

			// check if the previous files retrival has been interrupted
			if (_workerCancelled) {
				return;
			}

			final File prevPhotoFolder = _photoFolder;

			_photoFolder = _workerStateDir;

			_allPhotoWrapper = newPhotoWrapper;

			_display.syncExec(new Runnable() {
				public void run() {

					// guard against the ui being closed before this runs
					if (_uiContainer.isDisposed()) {
						return;
					}

					// initialize tooltip for a new folder
					_photoTooltip.reset();

					// keep current gallery position
					if (prevPhotoFolder != null) {
						_galleryPositions.put(prevPhotoFolder.getAbsolutePath(), _gallery.getGalleryPosition());
					}

					// get old gallery position
					final Double oldPosition = _galleryPositions.get(_photoFolder.getAbsolutePath());

					/*
					 * initialize and update gallery with new items
					 */
					_gallery.setupItems(0, oldPosition == null ? 0 : oldPosition, _restoredSelection);

					_restoredSelection = null;

					/*
					 * update status info
					 */
					final long timeDiff = System.currentTimeMillis() - _workerStart;
					final String timeDiffText = NLS.bind(
							Messages.Pic_Dir_Status_Loaded,
							new Object[] { Long.toString(timeDiff), Integer.toString(_allPhotoWrapper.length) });

					_lblStatusLine.setText(timeDiffText);
				}
			});

			/*
			 * start filter always, even when no filter is set because it is loading exif data which
			 * is used to sort images correctly by exif date (when available) and not by file date
			 */
			jobFilter_20_Schedule1st();
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
			_workerThread = new Thread(_workerRunnable, "PicDirImages: Retrieving folder image files");//$NON-NLS-1$
			_workerThread.start();
		}
	}
}
