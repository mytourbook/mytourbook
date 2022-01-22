/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.IContextMenuProvider;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.LRUMap;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.photo.internal.GalleryActionBar;
import net.tourbook.photo.internal.GalleryType;
import net.tourbook.photo.internal.Messages;
import net.tourbook.photo.internal.PhotoDateInfo;
import net.tourbook.photo.internal.PhotoFilterGPS;
import net.tourbook.photo.internal.PhotoFilterTour;
import net.tourbook.photo.internal.PhotoGalleryToolTip;
import net.tourbook.photo.internal.PhotoRenderer;
import net.tourbook.photo.internal.RatingStarBehaviour;
import net.tourbook.photo.internal.TableColumnFactory;
import net.tourbook.photo.internal.gallery.MT20.FullScreenImageViewer;
import net.tourbook.photo.internal.gallery.MT20.GalleryMT20;
import net.tourbook.photo.internal.gallery.MT20.GalleryMT20Item;
import net.tourbook.photo.internal.gallery.MT20.IGalleryContextMenuProvider;
import net.tourbook.photo.internal.gallery.MT20.IItemListener;
import net.tourbook.photo.internal.manager.ExifCache;
import net.tourbook.photo.internal.manager.GallerySorting;
import net.tourbook.photo.internal.manager.ThumbnailStore;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.progress.UIJob;

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
public abstract class ImageGallery implements IItemListener, IGalleryContextMenuProvider, IPhotoProvider, ITourViewer,
      IPhotoEvictionListener {

   /**
    * Number of gallery positions which are cached
    */
   private static final int       MAX_GALLERY_POSITIONS        = 100;

   private static final String    MENU_ID_PHOTO_GALLERY        = "menu.net.tourbook.photo.PhotoGallery"; //$NON-NLS-1$

   private static final int       DELAY_JOB_SUBSEQUENT_FILTER  = 500;                                    // ms
   private static final long      DELAY_JOB_UI_FILTER          = 200;                                    // ms
   private static final long      DELAY_JOB_UI_LOADING         = 200;                                    // ms

   public static final int        MIN_GALLERY_ITEM_WIDTH       = 10;                                     // pixel
   public static final int        MAX_GALLERY_ITEM_WIDTH       = 2000;                                   // pixel

   public static final String     STATE_THUMB_IMAGE_SIZE       = "STATE_THUMB_IMAGE_SIZE";               //$NON-NLS-1$
   private static final String    STATE_GALLERY_POSITION_KEY   = "STATE_GALLERY_POSITION_KEY";           //$NON-NLS-1$
   private static final String    STATE_GALLERY_POSITION_VALUE = "STATE_GALLERY_POSITION_VALUE";         //$NON-NLS-1$
   private static final String    STATE_IMAGE_SORTING          = "STATE_IMAGE_SORTING";                  //$NON-NLS-1$
   private static final String    STATE_SELECTED_ITEMS         = "STATE_SELECTED_ITEMS";                 //$NON-NLS-1$

   private static final String    DEFAULT_GALLERY_FONT         = "arial,sans-serif";                     //$NON-NLS-1$

   private IDialogSettings        _state;
   private final IPreferenceStore _prefStore                   = PhotoActivator.getPrefStore();

   /*
    * worker thread management
    */
   /**
    * Worker start time
    */
   private long             _workerStart;

   /**
    * Lock for all worker control data and state
    */
   private final Object     _workerLock       = new Object();

   /**
    * The worker's thread
    */
   private volatile Thread  _workerThread     = null;

   /**
    * True if the worker must exit on completion of the current cycle
    */
   private volatile boolean _workerStopped    = false;

   /**
    * True if the worker must cancel its operations prematurely perhaps due to a state update
    */
   private volatile boolean _workerCancelled  = false;

   /**
    * Worker state information -- this is what gets synchronized by an update
    */
   private volatile File    _workerStateDir   = null;

   /**
    * State information to use for the next cycle
    */
   private volatile File    _workerNextFolder = null;

   /**
    * Manages the worker's thread
    */
   private final Runnable   _workerRunnable;

   /*
    * image loading/filtering
    */
   private PhotoFilterGPS          _imageFilterGPS;
   private PhotoFilterTour         _imageFilterTour;

   private boolean                 _filterJob1stRun;
   private boolean                 _filterJobIsCanceled;

   private ReentrantLock           JOB_LOCK                        = new ReentrantLock();
   private Job                     _jobFilter;

   private AtomicBoolean           _jobFilterIsSubsequentScheduled = new AtomicBoolean();
   private int                     _jobFilterDirtyCounter;

   private UIJob                   _jobUIFilter;
   private AtomicBoolean           _jobUIFilterJobIsScheduled      = new AtomicBoolean();
   private int                     _jobUIFilterDirtyCounter;
   private Photo[]                 _jobUIFilterPhoto;

   private Job                     _jobUILoading;
   private AtomicBoolean           _jobUILoadingIsScheduled        = new AtomicBoolean();
   private int                     _jobUILoadingDirtyCounter;

   private int                     _currentExifRunId;

   /**
    *
    */
   public Comparator<Photo>        SORT_BY_IMAGE_DATE;
   public Comparator<Photo>        SORT_BY_FILE_NAME;

   /**
    * Contains current gallery sorting id: {@link PicDirView#GALLERY_SORTING_BY_DATE} or
    * {@link PicDirView#GALLERY_SORTING_BY_NAME}
    */
   private Comparator<Photo>       _currentComparator;
   private GallerySorting          _currentSorting;

   private PhotoRenderer           _photoRenderer;
   private FullScreenImageViewer   _fullScreenImageViewer;

   private PhotoGalleryToolTip     _photoGalleryTooltip;

   /**
    * Folder which images are currently be displayed
    */
   private File                    _photoFolder;

   /**
    * Folder which images should be displayed in the gallery
    */
   private File                    _photoFolderWhichShouldBeDisplayed;

   protected IPhotoGalleryProvider _photoGalleryProvider;

   private int                     _galleryStyle;

   private boolean                 _isShowCustomActionBar;
   private boolean                 _isShowThumbsize;
   private boolean                 _isHandleRatingStars;
   private boolean                 _isAttributesPainted;

   /**
    * Contains photos for <b>ALL</b> gallery items including <b>HIDDEN</b> items
    */
   private Photo[]                 _allPhotos;

   /**
    * Contains filtered gallery items.
    * <p>
    * Only these items are displayed in the gallery, {@link #_allPhotos} items contains also hidden
    * gallery items.
    */
   private Photo[]                 _sortedAndFilteredPhotos;

   FileFilter                      _fileFilter;

   /**
    * Photo image size without border
    */
   private int                     _photoImageSize;

   private int                     _photoBorderSize;

   private boolean                 _isShowTooltip;
   private boolean                 _isShowAnnotations;

   /**
    * keep gallery position for each used folder
    */
   private LRUMap<String, Double>  _galleryPositions;

   private String                  _newGalleryPositionKey;

   private String                  _currentGalleryPositionKey;
   private String                  _defaultStatusMessage           = UI.EMPTY_STRING;
   private int[]                   _restoredSelection;

   private GalleryType             _galleryType;

   private TableViewer             _photoViewer;
   private ColumnManager           _columnManager;

   private MenuManager             _viewerMenuManager;
   private IContextMenuProvider    _tableViewerContextMenuProvider = new TableContextMenuProvider();

   private PixelConverter          _pc;

   private GalleryMT20Item         _hoveredItem;
   private GalleryMT20Item[]       _sortedGalleryItems;

   private int[]                   _delayCounter                   = { 0 };

   private Double                  _contentGalleryPosition;
   private boolean                 _isLinkPhotoDisplayed;

   private final NumberFormat      _nf1                            = NumberFormat.getNumberInstance();
   {
      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
   }

   /*
    * UI resources
    */
   private Font _galleryFont;

   /*
    * UI controls
    */
   private Display          _display;

   private Composite        _uiContainer;

   private GalleryMT20      _galleryMT20;
   private GalleryActionBar _galleryActionBar;

   private PageBook         _pageBook;
   private Composite        _pageDefault;
   private Composite        _pageGalleryInfo;
   private Composite        _pageDetails;

   private Label            _lblDefaultPage;
   private Label            _lblGalleryInfo;

   private Menu             _tableContextMenu;

   {
      _galleryPositions = new LRUMap<>(MAX_GALLERY_POSITIONS);

      _workerRunnable = new Runnable() {
         @Override
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

      SORT_BY_IMAGE_DATE = new Comparator<>() {
         @Override
         public int compare(final Photo photo1, final Photo photo2) {

            if (_workerCancelled) {
               // couldn't find another way how to stop sorting
               return 0;
            }

            final long diff = photo1.imageExifTime - photo2.imageExifTime;

            return diff < 0 ? -1 : diff > 0 ? 1 : 0;
         }
      };

      SORT_BY_FILE_NAME = new Comparator<>() {
         @Override
         public int compare(final Photo photo1, final Photo photo2) {

            if (_workerCancelled) {
               // couldn't find another way how to stop sorting
               return 0;
            }

            return photo1.imageFilePathName.compareToIgnoreCase(photo2.imageFilePathName);
         }
      };
   }

   private class ContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {

//         return _sortedGalleryItems

         if (_sortedAndFilteredPhotos == null) {
            return new Photo[0];
         }

         return _sortedAndFilteredPhotos;
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   private class GalleryImplementation extends GalleryMT20 {

      public GalleryImplementation(final Composite parent, final int style, final IDialogSettings state) {
         super(parent, style, state);
      }

      @Override
      public Photo getPhoto(final int filterIndex) {

         if (filterIndex >= _sortedAndFilteredPhotos.length) {
            return null;
         }

         final Photo photo = _sortedAndFilteredPhotos[filterIndex];

         return photo;
      }

      @Override
      protected void onBeforeModifySelection() {
         onBeforeModifyGallerySelection();
      }
   }

   private class LoadCallbackExif implements ILoadCallBack {

      private int   __runId;
      private Photo __photo;

      public LoadCallbackExif(final Photo photo, final int runId) {

         __photo = photo;
         __runId = runId;
      }

      @Override
      public void callBackImageIsLoaded(final boolean isUpdateUI) {

         // keep exif metadata
         final PhotoImageMetadata metadata = __photo.getImageMetaDataRaw();

         if (metadata != null) {
            ExifCache.put(__photo.imageFilePathName, metadata);
         }

         updateSqlState();

         if (__runId != _currentExifRunId) {

            // this callback is from an older run ID, ignore it

            return;
         }

         jobFilter_22_ScheduleSubsequent(DELAY_JOB_SUBSEQUENT_FILTER);
         jobUILoading_20_Schedule();
      }

      private void updateSqlState() {

         final AtomicReference<PhotoSqlLoadingState> sqlLoadingState = __photo.getSqlLoadingState();

         final boolean isInLoadingQueue = sqlLoadingState.get() == PhotoSqlLoadingState.IS_IN_LOADING_QUEUE;

         final boolean isSqlLoaded = sqlLoadingState.compareAndSet(
               PhotoSqlLoadingState.IS_LOADED,
               PhotoSqlLoadingState.IS_IN_LOADING_QUEUE);

         if (isInLoadingQueue == false && isSqlLoaded == false) {

            final IPhotoServiceProvider photoServiceProvider = Photo.getPhotoServiceProvider();

            PhotoLoadManager.putPhotoInLoadingQueueSql(__photo, this, photoServiceProvider, false);
         }
      }
   }

   public class TableContextMenuProvider implements IContextMenuProvider {

      @Override
      public void disposeContextMenu() {

         if (_tableContextMenu != null) {
            _tableContextMenu.dispose();
         }
      }

      @Override
      public Menu getContextMenu() {
         return _tableContextMenu;
      }

      @Override
      public Menu recreateContextMenu() {

         disposeContextMenu();

         _tableContextMenu = createUI_36_CreateViewerContextMenu();

         return _tableContextMenu;
      }

   }

   public ImageGallery(final IDialogSettings state) {

      _state = state;
   }

   public void closePhotoTooltip() {
      _photoGalleryTooltip.close();
   }

   private void createGalleryFont() {

      if (_galleryFont != null) {
         _galleryFont.dispose();
      }

      final String prefGalleryFont = _prefStore.getString(IPhotoPreferences.PHOTO_VIEWER_FONT);
      if (prefGalleryFont.length() > 0) {
         try {

//            System.out.println(UI.timeStamp() + "setting gallery font: " + prefGalleryFont); //$NON-NLS-1$

            _galleryFont = new Font(_display, new FontData(prefGalleryFont));

         } catch (final Exception e) {
            // ignore
         }
      }

      if (_galleryFont == null) {
         StatusUtil.logError("This font cannot be created: \"" + prefGalleryFont + "\"");//$NON-NLS-1$ //$NON-NLS-2$
         _galleryFont = new Font(_display, DEFAULT_GALLERY_FONT, 7, SWT.NORMAL);
      }
   }

   /**
    * @param parent
    * @param style
    * @param photoGalleryProvider
    * @param state
    */
   public void createImageGallery(final Composite parent,
                                  final int style,
                                  final IPhotoGalleryProvider photoGalleryProvider) {

      PhotoUI.init();
      createMenuManager();

      _galleryStyle = style;
      _photoGalleryProvider = photoGalleryProvider;

      _pc = new PixelConverter(parent);

      _columnManager = new ColumnManager(this, _state);
      defineAllColumns();

      jobFilter_10_Create();
      jobUIFilter_10_Create();
      jobUILoading_10_Create();

      PhotoCache.addEvictionListener(this);

      createUI(parent);

      parent.addDisposeListener(new DisposeListener() {
         @Override
         public void widgetDisposed(final DisposeEvent e) {
            onDispose();
         }
      });
   }

   private void createMenuManager() {

      _viewerMenuManager = new MenuManager();
      _viewerMenuManager.setRemoveAllWhenShown(true);
      _viewerMenuManager.addMenuListener(new IMenuListener() {
         @Override
         public void menuAboutToShow(final IMenuManager menuManager) {

            fillContextMenu(menuManager);
         }
      });
   }

   /**
    * @return Returns a selection with all selected photos.
    */
   private PhotoSelection createPhotoSelection() {

      final Collection<GalleryMT20Item> allItems = _galleryMT20.getSelection();
      final ArrayList<Photo> photos = new ArrayList<>();

      for (final GalleryMT20Item item : allItems) {

         final Photo photo = item.photo;
         if (photo != null) {
            photos.add(photo);
         }
      }

      return new PhotoSelection(photos, allItems, _galleryMT20.getSelectionIndex(), _isLinkPhotoDisplayed);
   }

   /**
    * @param isAllImages
    * @return
    */
   private PhotosWithExifSelection createPhotoSelectionWithExif(final boolean isAllImages) {

      final ArrayList<Photo> loadedExifData = getLoadedExifImageData(_photoFolderWhichShouldBeDisplayed, isAllImages);

      if (loadedExifData == null) {

         MessageDialog.openInformation(
               _display.getActiveShell(),
               Messages.Pic_Dir_Dialog_Photos_Title,
               Messages.Pic_Dir_Dialog_Photos_DialogInterrupted_Message);

         return null;
      }

      /*
       * check if a photo is selected
       */
      if (loadedExifData.isEmpty()) {

         if (isAllImages) {

            MessageDialog.openInformation(
                  _display.getActiveShell(),
                  Messages.Pic_Dir_Dialog_Photos_Title,
                  NLS.bind(Messages.Pic_Dir_Dialog_Photos_NoSelectedImagesInFolder_Message, //
                        _photoFolderWhichShouldBeDisplayed.getAbsolutePath()));

         } else {

            MessageDialog.openInformation(
                  _display.getActiveShell(),
                  Messages.Pic_Dir_Dialog_Photos_Title,
                  Messages.Pic_Dir_Dialog_Photos_NoSelectedImage_Message);
         }

         return null;
      }

      return new PhotosWithExifSelection(loadedExifData);
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
//      container.setBackground(_display.getSystemColor(SWT.COLOR_DARK_RED));
      {
         if (_isShowThumbsize || _isShowCustomActionBar) {
            _galleryActionBar = new GalleryActionBar(container, this, _isShowThumbsize, _isShowCustomActionBar);
         }

         _pageBook = new PageBook(container, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageBook);
         {
            createUI_20_PageGallery(_pageBook);
            createUI_30_PageDetails(_pageBook);
            createUI_99_PageDefault(_pageBook);
            createUI_40_PageGalleryInfo(_pageBook);
         }
      }

      _photoGalleryTooltip = new PhotoGalleryToolTip(_galleryMT20);
      _photoGalleryTooltip.setReceiveMouseMoveEvent(true);

      _galleryMT20.addItemListener(this);
   }

   /**
    * Create gallery
    */
   private void createUI_20_PageGallery(final Composite parent) {

      _galleryMT20 = new GalleryImplementation(parent, _galleryStyle, _state);

      _galleryMT20.setHigherQualityDelay(200);
//      _gallery.setAntialias(SWT.OFF);
//      _gallery.setInterpolation(SWT.LOW);
      _galleryMT20.setAntialias(SWT.ON);
      _galleryMT20.setInterpolation(SWT.HIGH);
      _galleryMT20.setItemMinMaxSize(MIN_GALLERY_ITEM_WIDTH, MAX_GALLERY_ITEM_WIDTH);

      _galleryMT20.addListener(SWT.Modify, new Listener() {

         // a modify event is fired when gallery is zoomed in/out

         @Override
         public void handleEvent(final Event event) {

            PhotoLoadManager.stopImageLoading(false);

            updateUI_AfterZoomInOut(event.width);
         }
      });

      _galleryMT20.addListener(SWT.Selection, new Listener() {

         // a gallery item is selected/deselected

         @Override
         public void handleEvent(final Event event) {

            onSelectPhoto();
         }
      });

      _galleryMT20.setContextMenuProvider(this);

      _photoGalleryProvider.registerContextMenu(MENU_ID_PHOTO_GALLERY, _galleryMT20.getContextMenuManager());

      _fullScreenImageViewer = _galleryMT20.getFullScreenImageViewer();

      // allow the gallery to get access to the photo
      _galleryMT20.setPhotoProvider(this);

      // set photo renderer which paints the image but also starts the image loading
      _photoRenderer = new PhotoRenderer(_galleryMT20, this);

      _galleryMT20.setItemRenderer(_photoRenderer);
   }

   private void createUI_30_PageDetails(final PageBook parent) {

      _pageDetails = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageDetails);
      GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(_pageDetails);
      {
         createUI_32_PhotoViewer(_pageDetails);
      }
   }

   private void createUI_32_PhotoViewer(final Composite parent) {

      final Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);

      GridDataFactory.fillDefaults().grab(true, true).applyTo(table);
      table.setHeaderVisible(true);
//      table.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

      /*
       * create table viewer
       */
      _photoViewer = new TableViewer(table);
      _columnManager.createColumns(_photoViewer);

      _photoViewer.setUseHashlookup(true);
      _photoViewer.setContentProvider(new ContentProvider());
//      _photoViewer.setComparator(new ContentComparator());

      _photoViewer.addSelectionChangedListener(new ISelectionChangedListener() {
         @Override
         public void selectionChanged(final SelectionChangedEvent event) {
            final ISelection eventSelection = event.getSelection();
            if (eventSelection instanceof StructuredSelection) {
//               onSelectTour(((StructuredSelection) eventSelection).toArray());
            }
         }
      });

      createUI_34_ContextMenu();
   }

   /**
    * create the views context menu
    *
    * @param isRecreate
    */
   private void createUI_34_ContextMenu() {

      _tableContextMenu = createUI_36_CreateViewerContextMenu();

      final Table table = _photoViewer.getTable();

      _columnManager.createHeaderContextMenu(table, _tableViewerContextMenuProvider);
   }

   private Menu createUI_36_CreateViewerContextMenu() {

      final Table table = _photoViewer.getTable();

      final Menu tableContextMenu = _viewerMenuManager.createContextMenu(table);

      table.setMenu(tableContextMenu);

      return tableContextMenu;
   }

   private void createUI_40_PageGalleryInfo(final PageBook parent) {

      _pageGalleryInfo = new Composite(parent, SWT.NONE);
//      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
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

   private void createUI_99_PageDefault(final PageBook parent) {

      _pageDefault = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults()//
            .numColumns(1)
            .margins(5, 5)
            .applyTo(_pageDefault);
      {
         _lblDefaultPage = new Label(_pageDefault, SWT.WRAP);
         GridDataFactory.fillDefaults()//
               .grab(true, true)
               .align(SWT.FILL, SWT.FILL)
               .applyTo(_lblDefaultPage);
      }
   }

   private void defineAllColumns() {

      defineColumn_ImageFileName();
      defineColumn_AdjustedDate();
      defineColumn_AdjustedTime();
      defineColumn_ExifTime();
      defineColumn_Dimension();
      defineColumn_Orientation();
      defineColumn_ImageDirectionText();
      defineColumn_ImageDirectionDegree();
//      defineColumnAltitude();
//      defineColumnLatitude();
//      defineColumnLongitude();
      defineColumn_Location();
   }

   /**
    * column: date
    */
   private void defineColumn_AdjustedDate() {

      final ColumnDefinition colDef = TableColumnFactory.PHOTO_FILE_DATE.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

//            final Photo photo = (Photo) cell.getElement();
//
//            cell.setText(_dateFormatter.print(photo.adjustedTime));
         }
      });
   }

   /**
    * column: time
    */
   private void defineColumn_AdjustedTime() {

      final ColumnDefinition colDef = TableColumnFactory.PHOTO_FILE_TIME.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

//            final Photo photo = (Photo) cell.getElement();
//
//            cell.setText(_timeFormatter.print(photo.adjustedTime));
         }
      });
   }

   /**
    * column: width x height
    */
   private void defineColumn_Dimension() {

      final ColumnDefinition colDef = TableColumnFactory.PHOTO_FILE_DIMENSION.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Photo photo = (Photo) cell.getElement();

            cell.setText(photo.getDimensionText());
         }
      });
   }

   /**
    * column: tour start time
    */
   private void defineColumn_ExifTime() {

      final ColumnDefinition colDef = TableColumnFactory.PHOTO_FILE_TIME.createColumn(_columnManager, _pc);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Photo photo = (Photo) cell.getElement();

            cell.setText(TimeTools.getZonedDateTime(photo.imageExifTime).format(TimeTools.Formatter_Time_M));
         }
      });
   }

   /**
    * column: image direction degree
    */
   private void defineColumn_ImageDirectionDegree() {

      final ColumnDefinition colDef = TableColumnFactory.PHOTO_FILE_IMAGE_DIRECTION_DEGREE//
            .createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Photo photo = (Photo) cell.getElement();
            final double imageDirection = photo.getImageDirection();

            if (imageDirection == Double.MIN_VALUE) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(Integer.toString((int) imageDirection));
            }
         }
      });
   }

//   /**
//    * column: altitude
//    */
//   private void defineColumnAltitude() {
//
//      final ColumnDefinition colDef = TableColumnFactory.PHOTO_FILE_ALTITUDE.createColumn(_columnManager, _pc);
//      colDef.setIsDefaultColumn();
//      colDef.setLabelProvider(new CellLabelProvider() {
//         @Override
//         public void update(final ViewerCell cell) {
//
//            final Photo photo = (Photo) cell.getElement();
//            final double altitude = photo.getAltitude();
//
//            if (altitude == Double.MIN_VALUE) {
//               cell.setText(UI.EMPTY_STRING);
//            } else {
//               cell.setText(Integer.toString((int) (altitude / UI.UNIT_VALUE_ELEVATION)));
//            }
//         }
//      });
//   }

   /**
    * column: image direction degree
    */
   private void defineColumn_ImageDirectionText() {

      final ColumnDefinition colDef = TableColumnFactory.PHOTO_FILE_IMAGE_DIRECTION_TEXT//
            .createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Photo photo = (Photo) cell.getElement();
            final double imageDirection = photo.getImageDirection();

            if (imageDirection == Double.MIN_VALUE) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(UI.getCardinalDirectionText((int) imageDirection * 10));
            }
         }
      });
   }

   /**
    * column: name
    */
   private void defineColumn_ImageFileName() {

      final ColumnDefinition colDef = TableColumnFactory.PHOTO_FILE_NAME.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Photo photo = (Photo) cell.getElement();

            cell.setText(photo.imageFileName);
         }
      });
   }

//   /**
//    * column: latitude
//    */
//   private void defineColumnLatitude() {
//
//      final ColumnDefinition colDef = TableColumnFactory.LATITUDE.createColumn(_columnManager, _pc);
//
//      colDef.setIsDefaultColumn();
//      colDef.setLabelProvider(new CellLabelProvider() {
//         @Override
//         public void update(final ViewerCell cell) {
//
//            final double latitude = ((Photo) cell.getElement()).getLatitude();
//
//            if (latitude == Double.MIN_VALUE) {
//               cell.setText(UI.EMPTY_STRING);
//            } else {
//               cell.setText(_nf8.format(latitude));
//            }
//         }
//      });
//   }

   /**
    * column: location
    */
   private void defineColumn_Location() {

      final ColumnDefinition colDef = TableColumnFactory.PHOTO_FILE_LOCATION.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Photo photo = (Photo) cell.getElement();

            cell.setText(photo.getGpsAreaInfo());
         }
      });
   }

//   /**
//    * column: longitude
//    */
//   private void defineColumnLongitude() {
//
//      final ColumnDefinition colDef = net.tourbook.ui.TableColumnFactory.LONGITUDE.createColumn(_columnManager, _pc);
//
//      colDef.setIsDefaultColumn();
//      colDef.setLabelProvider(new CellLabelProvider() {
//         @Override
//         public void update(final ViewerCell cell) {
//
//            final double longitude = ((Photo) cell.getElement()).getLongitude();
//
//            if (longitude == Double.MIN_VALUE) {
//               cell.setText(UI.EMPTY_STRING);
//            } else {
//               cell.setText(_nf8.format(longitude));
//            }
//         }
//      });
//   }

   /**
    * column: orientation
    */
   private void defineColumn_Orientation() {

      final ColumnDefinition colDef = TableColumnFactory.PHOTO_FILE_ORIENTATION.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Photo photo = (Photo) cell.getElement();

            cell.setText(Integer.toString(photo.getOrientation()));
         }
      });
   }

   private void deselectAll() {

      _galleryMT20.deselectAll();

      // update UI
      onSelectPhoto();
   }

   /**
    * Disposes and deletes all thumb images.
    */
   private void disposeAndDeleteAllImages() {

      PhotoLoadManager.stopImageLoading(true);
      ThumbnailStore.cleanupStoreFiles(true, true);

      PhotoImageCache.disposeAll();

      ExifCache.clear();
   }

   protected abstract void enableActions(final boolean isItemAvailable);

   protected abstract void enableAttributeActions(boolean isAttributesPainted);

   @Override
   public void evictedPhoto(final Photo evictedPhoto) {

      final String evictedImageFilePathName = evictedPhoto.imageFilePathName;

      // check if it is still evicted
      if (PhotoCache.getPhoto(evictedImageFilePathName) != null) {

         // photo is already recreated
         return;
      }

      for (final Photo photo : _allPhotos) {
         if (photo.imageFilePathName.equals(evictedImageFilePathName)) {

            // evicted photo is still displayed, cache is again
            PhotoCache.setPhoto(photo);

            break;
         }
      }
   }

   @Override
   public void fillContextMenu(final IMenuManager menuMgr) {

      menuMgr.add(new Separator(UI.MENU_SEPARATOR_ADDITIONS));
   }

   /**
    * This is called when a filter button is pressed.
    *
    * @param photoFilterGPS
    * @param photoFilterTour
    * @param isUpdateGallery
    */
   public void filterGallery(final PhotoFilterGPS photoFilterGPS, final PhotoFilterTour photoFilterTour) {

      _imageFilterGPS = photoFilterGPS;
      _imageFilterTour = photoFilterTour;

      /*
       * deselect all, this could be better implemented to keep selection, but is not yet done
       */
      deselectAll();

      jobFilter_22_ScheduleSubsequent(0);
   }

   /**
    * Preserves gallery positions for different gallery contents.
    *
    * @return
    */
   private double getCachedGalleryPosition() {

      double galleryPosition = 0;

      // keep current gallery position
      if (_currentGalleryPositionKey != null) {
         _galleryPositions.put(_currentGalleryPositionKey, _galleryMT20.getGalleryPosition());
      }

      // get old position
      if (_newGalleryPositionKey != null) {

         _currentGalleryPositionKey = _newGalleryPositionKey;

         // get old gallery position
         final Double oldPosition = _galleryPositions.get(_newGalleryPositionKey);

         galleryPosition = oldPosition == null ? 0 : oldPosition;

//         System.out.println(UI.timeStampNano() + " get old position " + oldPosition);
//         // TODO remove SYSTEM.OUT.PRINTLN
      }

//      System.out.println(UI.timeStampNano() + " getCachedGalleryPosition() " + galleryPosition);
//      // TODO remove SYSTEM.OUT.PRINTLN

      return galleryPosition;
   }

   @Override
   public ColumnManager getColumnManager() {
      return _columnManager;
   }

   private Comparator<Photo> getCurrentComparator() {
      return _currentSorting == GallerySorting.FILE_NAME ? SORT_BY_FILE_NAME : SORT_BY_IMAGE_DATE;
   }

   /**
    * @return Returns gallery action bar container. {@link #setShowActionBar(boolean)} must be
    *         called with the parameter <code>true</code> that the action bar is created.
    */
   public Composite getCustomActionBarContainer() {
      return _galleryActionBar.getCustomContainer();
   }

   public FullScreenImageViewer getFullScreenImageViewer() {
      return _fullScreenImageViewer;
   }

   public Control getGallery() {
      return _galleryMT20;
   }

   public Control getGalleryContainer() {
      return _pageBook;
   }

   public Collection<GalleryMT20Item> getGallerySelection() {
      return _galleryMT20.getSelection();
   }

   /**
    * @param selectedFolder
    * @param isGetAllImages
    * @return Returns photo data for the images in the requested folder, sorted by date/time or
    *         <code>null</code> when loading was canceled by the user.
    */
   private ArrayList<Photo> getLoadedExifImageData(final File selectedFolder, final boolean isGetAllImages) {

      final boolean isFolderFilesLoaded = _photoFolder.getAbsolutePath().equals(_photoFolderWhichShouldBeDisplayed.getAbsolutePath());

      if (PhotoLoadManager.getExifQueueSize() > 0 || isFolderFilesLoaded == false) {

         /*
          * wait until all image exif data are loaded
          */
         if (isEXIFDataLoaded() == false) {
            return null;
         }
      }

      Photo[] sortedPhotosArray;

      if (isGetAllImages) {

         // get all filtered photos

         sortedPhotosArray = _sortedAndFilteredPhotos.clone();

      } else {

         // get all selected photos

         final Collection<GalleryMT20Item> galleryItems = _galleryMT20.getSelection();

         sortedPhotosArray = new Photo[galleryItems.size()];

         int itemIndex = 0;

         for (final GalleryMT20Item item : galleryItems) {

            final Photo photo = item.photo;
            if (photo != null) {
               sortedPhotosArray[itemIndex++] = photo;
            }
         }

      }

      // sort photos by date/time
      Arrays.sort(sortedPhotosArray, SORT_BY_IMAGE_DATE);

      final ArrayList<Photo> sortedPhotos = new ArrayList<>(sortedPhotosArray.length);

      for (final Photo photo : sortedPhotosArray) {
         sortedPhotos.add(photo);
      }

      return sortedPhotos;
   }

   /**
    * @return Returns folder which is currently displayed.
    */
   public File getPhotoFolder() {
      return _photoFolder;
   }

   /**
    * Creates a {@link PhotosWithExifSelection}
    *
    * @param isAllImages
    * @return Returns a {@link ISelection} for selected or all images or <code>null</code> null
    *         when loading EXIF data was canceled by the user.
    */
   public PhotosWithExifSelection getSelectedPhotosWithExif(final boolean isAllImages) {
      return createPhotoSelectionWithExif(isAllImages);
   }

   @Override
   public Photo[] getSortedAndFilteredPhotos() {
      return _sortedAndFilteredPhotos;
   }

   /**
    * This message is displayed when no other message is displayed.
    *
    * @return
    */
   private String getStatusDefaultMessage() {

      final Collection<GalleryMT20Item> allSelectedPhoto = _galleryMT20.getSelection();
      final int allPhotoSize = allSelectedPhoto.size();

      return allPhotoSize == 0 //
            // hide status message when nothing is selected
            ? UI.EMPTY_STRING
            : NLS.bind(Messages.Pic_Dir_StatusLabel_SelectedImages, allPhotoSize);
   }

   @Override
   public ColumnViewer getViewer() {
      return _photoViewer;
   }

   public void handlePrefStoreModifications(final PropertyChangeEvent event) {

      final String property = event.getProperty();

      if (property.equals(IPhotoPreferences.PHOTO_VIEWER_PREF_EVENT_IMAGE_QUALITY_IS_MODIFIED)) {

         _display.asyncExec(() -> {

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

            _galleryMT20.updateGallery(false, _galleryMT20.getGalleryPosition());

            if (_galleryActionBar != null) {
               _galleryActionBar.updateUI_ImageIndicatorTooltip();
            }
         });

      } else if (property.equals(IPhotoPreferences.PHOTO_VIEWER_PREF_EVENT_IMAGE_VIEWER_UI_IS_MODIFIED)
            || property.equals(IPhotoPreferences.PHOTO_VIEWER_PREF_EVENT_FULLSIZE_VIEWER_IS_MODIFIED)) {

         updateUI_FromPrefStore(true);

         updateUI_AfterZoomInOut(_galleryMT20.getItemWidth());

      } else if (property.equals(IPhotoPreferences.PHOTO_VIEWER_FONT)) {

         onModifyFont();
      }
   }

   public boolean isDisposed() {
      return _galleryMT20.isDisposed();
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

               final boolean isFolderFilesLoadedInitialValue = _photoFolder.getAbsolutePath()
                     .equals(_photoFolderWhichShouldBeDisplayed.getAbsolutePath());

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

                     isFolderFilesLoaded = _photoFolder.getAbsolutePath().equals(_photoFolderWhichShouldBeDisplayed.getAbsolutePath());
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
               final int allPhotoSize = _allPhotos.length;
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

                  monitor.subTask(NLS.bind(Messages.Pic_Dir_Dialog_LoadingEXIFData_Subtask,
                        new Object[] {
                              exifLoadingQueueSize,
                              allPhotoSize,
                              _nf1.format(_percent) }));

                  exifLoadingQueueSize = newExifLoadingQueueSize;
               }
            }
         };

         new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, true, runnable);

      } catch (final InvocationTargetException | InterruptedException e) {
         StatusUtil.log(e);
      }

      return isLoaded[0];
   }

   public boolean isLinkPhotoDisplayed() {
      return _isLinkPhotoDisplayed;
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
      _jobUIFilterPhoto = null;

      // wait until the filter job has ended
      try {

         _jobFilter.cancel();
         _jobFilter.join();

      } catch (final InterruptedException e) {
         StatusUtil.log(e);
      }
   }

   private void jobFilter_20_Schedule1st() {

      // filter must be stopped before new photos are set
      jobFilter_12_Stop();

      JOB_LOCK.lock();
      {
         try {

            // this is the initial run of the filter job
            _filterJob1stRun = true;

            _sortedAndFilteredPhotos = new Photo[0];

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

//      final long start = System.nanoTime();

      if (_allPhotos.length == 0) {

         // there are no images in the current folder,

         /*
          * gallery MUST be updated even when no images are displayed because images from the
          * previous folder are still displayed
          */

         updateUI_GalleryInfo();

         return;
      }

      final boolean isGPSFilter = _imageFilterGPS == PhotoFilterGPS.WITH_GPS;
      final boolean isNoGPSFilter = _imageFilterGPS == PhotoFilterGPS.NO_GPS;
      final boolean isTourFilter = _imageFilterTour == PhotoFilterTour.WITH_TOURS;
      final boolean isNoTourFilter = _imageFilterTour == PhotoFilterTour.NO_TOURS;

      final boolean isFilterSet = isGPSFilter || isNoGPSFilter || isTourFilter || isNoTourFilter;

      // get current dirty counter
      final int currentDirtyCounter = _jobFilterDirtyCounter;

      Photo[] newFilteredPhotos = null;

      if (isFilterSet) {

         final int numberOfPhotos = _allPhotos.length;
         final Photo[] tempFilteredPhotos = new Photo[numberOfPhotos];

         // filterindex is incremented when the filter contains a gallery item
         int filterIndex = 0;
         int photoIndex = 0;

         // loop: all photos
         for (final Photo photo : _allPhotos) {

            if (_filterJobIsCanceled) {
               return;
            }

            boolean isPhotoInFilter = false;

            if (photo.isExifLoaded == false) {

               // image is not yet loaded, it must be loaded to get the gps state

               putInExifLoadingQueue(photo);
            }

            // check again, the gps state could have been cached and set
            if (photo.isExifLoaded) {

               final boolean isPhotoWithGps = _isLinkPhotoDisplayed
                     ? photo.isLinkPhotoWithGps
                     : photo.isTourPhotoWithGps;

               if (isGPSFilter) {
                  if (isPhotoWithGps) {
                     isPhotoInFilter = true;
                  }

               } else if (isNoGPSFilter) {

                  if (!isPhotoWithGps) {
                     isPhotoInFilter = true;
                  }
               }
            }

            final boolean isSavedInTour = photo.isSavedInTour;

            if (isTourFilter) {
               if (isSavedInTour) {
                  isPhotoInFilter = true;
               }

            } else if (isNoTourFilter) {

               if (!isSavedInTour) {
                  isPhotoInFilter = true;
               }
            }

            if (isPhotoInFilter) {

               tempFilteredPhotos[filterIndex] = _allPhotos[photoIndex];

               filterIndex++;
            }

            photoIndex++;
         }

         // remove trailing array items which are not set
         newFilteredPhotos = Arrays.copyOf(tempFilteredPhotos, filterIndex);

      } else {

         // a filter is not set, display all images but load exif data which is necessary when sorting by date

         newFilteredPhotos = Arrays.copyOf(_allPhotos, _allPhotos.length);

         // loop: all photos
         for (final Photo photo : _allPhotos) {

            if (_filterJobIsCanceled) {
               return;
            }

            if (photo.isExifLoaded == false) {

               // image is not yet loaded, it must be loaded to get the gps state
               putInExifLoadingQueue(photo);
            }
         }
      }

      // check sorting
//      if (_initialSorting != _currentSorting) {
//
//         /*
//          * photo must be sorted because sorting is different than the initial sorting, this
//          * will sort only the filtered photo
//          */
//
      Arrays.sort(newFilteredPhotos, getCurrentComparator());
//      }

      /**
       * Update UI
       * <p>
       * gallery MUST be updated even when no images are displayed because images from the
       * previous folder are still displayed
       */
      updateUI_GalleryItems(newFilteredPhotos, null);

      if (_jobFilterDirtyCounter > currentDirtyCounter) {

         // filter is dirty again

         jobFilter_23_ScheduleSubsequentWithoutRunCheck();

      } else {

         // clear progress bar

         jobUILoading_20_Schedule();
      }

//      final float timeDiff = (float) (System.nanoTime() - start) / 1000000;
////      if (timeDiff > 10) {}
//      System.out.println("filterJob_20_RunInitial:\t" + timeDiff + " ms\t"); //$NON-NLS-1$ //$NON-NLS-2$
//      // TODO remove SYSTEM.OUT.PRINTLN
   }

   /**
    * Run filter and sorting again with newly loaded EXIF data until all EXIF data are loaded.
    */
   private void jobFilter_32_RunSubsequent() {

//      final long start = System.nanoTime();

      if (_allPhotos == null) {
         return;
      }

      final boolean isGPSFilter = _imageFilterGPS == PhotoFilterGPS.WITH_GPS;
      final boolean isNoGPSFilter = _imageFilterGPS == PhotoFilterGPS.NO_GPS;
      final boolean isTourFilter = _imageFilterTour == PhotoFilterTour.WITH_TOURS;
      final boolean isNoTourFilter = _imageFilterTour == PhotoFilterTour.NO_TOURS;

      final boolean isFilterSet = isGPSFilter || isNoGPSFilter || isTourFilter || isNoTourFilter;

      // get current dirty counter
      final int currentDirtyCounter = _jobFilterDirtyCounter;

      Photo[] newFilteredPhotos = null;

      if (isFilterSet) {

         final int numberOfPhotos = _allPhotos.length;
         final Photo[] tempFilteredPhotos = new Photo[numberOfPhotos];

         // filterindex is incremented when the filter contains a gallery item
         int filterIndex = 0;
         int photoIndex = 0;

         // loop: all photos
         for (final Photo photo : _allPhotos) {

            if (_filterJobIsCanceled) {
               return;
            }

            boolean isPhotoInFilterGps = false;
            boolean isPhotoInFilterTour = false;

            if (photo.isExifLoaded) {

               final boolean isPhotoWithGps = _isLinkPhotoDisplayed
                     ? photo.isLinkPhotoWithGps
                     : photo.isTourPhotoWithGps;

               if (isGPSFilter) {
                  if (isPhotoWithGps) {
                     isPhotoInFilterGps = true;
                  }

               } else if (isNoGPSFilter) {

                  if (!isPhotoWithGps) {
                     isPhotoInFilterGps = true;
                  }

               } else {

                  // no gps filter

                  isPhotoInFilterGps = true;
               }
            } else {

               // no gps filter

               isPhotoInFilterGps = true;
            }

            final boolean isSavedInTour = photo.isSavedInTour;

            if (isTourFilter) {
               if (isSavedInTour) {
                  isPhotoInFilterTour = true;
               }

            } else if (isNoTourFilter) {

               if (!isSavedInTour) {
                  isPhotoInFilterTour = true;
               }
            } else {

               // no tour filter
               isPhotoInFilterTour = true;
            }

            if (isPhotoInFilterGps && isPhotoInFilterTour) {

               tempFilteredPhotos[filterIndex] = _allPhotos[photoIndex];

               filterIndex++;
            }

            photoIndex++;
         }

         // remove trailing array items which are not set
         newFilteredPhotos = Arrays.copyOf(tempFilteredPhotos, filterIndex);

      } else {

         // a filter is not set, display all images but load exif data which is necessary when filtering by date

         newFilteredPhotos = Arrays.copyOf(_allPhotos, _allPhotos.length);
      }

      // check sorting
//      if (_initialSorting != _currentSorting) {
//
//         /*
//          * photo must be sorted because sorting is different than the initial sorting, this
//          * will sort only the filtered photo
//          */
//
      Arrays.sort(newFilteredPhotos, getCurrentComparator());
//      }

      /**
       * UI update must be run in a UI job because the update can be very long when many
       * (thousands) small images are displayed
       */
      _jobUIFilterPhoto = newFilteredPhotos;

      jobUIFilter_20_Schedule(0);

      if (_jobFilterDirtyCounter > currentDirtyCounter) {

         // filter is dirty again

         jobFilter_23_ScheduleSubsequentWithoutRunCheck();

      } else {

         // clear progress bar

         jobUILoading_20_Schedule();
      }

//      final float timeDiff = (float) (System.nanoTime() - start) / 1000000;
//      System.out.println("filterJob_30_RunSubsequent:\t" + timeDiff + " ms\t"); //$NON-NLS-1$ //$NON-NLS-2$
//      // TODO remove SYSTEM.OUT.PRINTLN
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

      final Photo[] uiUpdatePhoto = _jobUIFilterPhoto;
      _jobUIFilterPhoto = null;

      if (uiUpdatePhoto == null) {
         return;
      }

      final int currentDirtyCounter = _jobUIFilterDirtyCounter;

      updateUI_GalleryItems(uiUpdatePhoto, null);

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

         updateUI_StatusMessageInUIThread(NLS.bind(Messages.Pic_Dir_StatusLabel_LoadingImagesFilter,
               new Object[] {
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

   private void onBeforeModifyGallerySelection() {

      if (_hoveredItem == null) {
         return;
      }

      _hoveredItem.isHovered = false;
      _hoveredItem.isNeedExitUIUpdate = false;
      _hoveredItem.hoveredStars = 0;
      _hoveredItem.isHovered_AnnotationTour = false;
      _hoveredItem.isHovered_InvalidImage = false;

      if (_hoveredItem.allSelectedGalleryItems != null) {

         for (final GalleryMT20Item selectedItems : _hoveredItem.allSelectedGalleryItems) {

            selectedItems.isInHoveredGroup = false;

            _galleryMT20.redrawItem(selectedItems);
         }

         /**
          * This collection cannot be cleared because it's the original list with selected items
          * in the gallery, so only the reference is set to <code>null</code>.
          */
         _hoveredItem.allSelectedGalleryItems = null;

      } else {

         _galleryMT20.redrawItem(_hoveredItem);
      }

      _hoveredItem = null;
   }

   private void onDispose() {

      PhotoCache.removeEvictionListener(this);

      if (_galleryFont != null) {
         _galleryFont.dispose();
      }

      stopLoadingImages();
   }

   @Override
   public boolean onItemMouseDown(final GalleryMT20Item mouseDownItem, final int itemMouseX, final int itemMouseY) {

      if (mouseDownItem == null) {
         return false;
      }

      if (_isHandleRatingStars || _isShowAnnotations || mouseDownItem.photo.isLoadingError()) {

         if (_photoRenderer.isMouseDownHandled(mouseDownItem, itemMouseX, itemMouseY)) {

            _galleryMT20.redrawItem(mouseDownItem);

            return true;
         }
      }

      return false;
   }

   @Override
   public void onItemMouseExit(final GalleryMT20Item exitHoveredItem, final int itemMouseX, final int itemMouseY) {

      if (_isHandleRatingStars == false
            && _isShowAnnotations == false
            && exitHoveredItem.photo.isLoadingError() == false) {
         return;
      }

      /*
       * reset hovering
       */

      final boolean isUpdateUI = exitHoveredItem.isNeedExitUIUpdate;

      exitHoveredItem.isHovered = false;
      exitHoveredItem.isNeedExitUIUpdate = false;

      exitHoveredItem.hoveredStars = 0;
      exitHoveredItem.isHovered_AnnotationTour = false;
      exitHoveredItem.isHovered_InvalidImage = false;

      if (exitHoveredItem.allSelectedGalleryItems != null) {

         for (final GalleryMT20Item selectedItems : exitHoveredItem.allSelectedGalleryItems) {

            selectedItems.isInHoveredGroup = false;

            if (isUpdateUI) {
               _galleryMT20.redrawItem(selectedItems);
            }
         }

         /**
          * This collection cannot be cleared because it's the original list with selected items
          * in the gallery, so only the reference is set to <code>null</code>.
          */
         exitHoveredItem.allSelectedGalleryItems = null;

         _hoveredItem = null;

      } else {

         if (isUpdateUI) {
            _galleryMT20.redrawItem(exitHoveredItem);
         }
      }

   }

   @Override
   public void onItemMouseHovered(final GalleryMT20Item hoveredItem, final int itemMouseX, final int itemMouseY) {

      if (_isShowTooltip) {
         _photoGalleryTooltip.show(hoveredItem);
      }

      if (hoveredItem == null) {
         return;
      }

      final boolean isRatingStarsHandledAndPainted = _isHandleRatingStars && _isAttributesPainted;

      if (isRatingStarsHandledAndPainted || _isShowAnnotations || hoveredItem.photo.isLoadingError()) {

         _hoveredItem = hoveredItem;

         hoveredItem.isHovered = true;

         // this will set allSelectedGalleryItems in the hovered item
         final boolean isModified = _photoRenderer.isItemHovered(hoveredItem, itemMouseX, itemMouseY);

         // don't reset here, this is done in the item exit event
         hoveredItem.isNeedExitUIUpdate |= isModified;

         if (isModified) {

            if (hoveredItem.allSelectedGalleryItems != null) {

               for (final GalleryMT20Item selectedItems : hoveredItem.allSelectedGalleryItems) {

                  selectedItems.isInHoveredGroup = true;

                  _galleryMT20.redrawItem(selectedItems);
               }

            } else {

               _galleryMT20.redrawItem(hoveredItem);
            }
         }
      }
   }

   private void onModifyFont() {

      createGalleryFont();

      _photoRenderer.setFont(_galleryFont);
   }

   public void onReparentShell(final Shell reparentedShell) {

      _photoGalleryTooltip.onReparentShell(reparentedShell);

      /*
       * when a tooltip is reparented, the context menu must be recreated otherwise an exception
       * is thown that the menu shell has the wrong parent
       */
      createUI_34_ContextMenu();
   }

   private void onSelectPhoto() {

      // show default message
      updateUI_StatusMessage(UI.EMPTY_STRING);

      // fire selection
      _photoGalleryProvider.setSelection(createPhotoSelection());
   }

   /**
    * Get gps state and exif data
    *
    * @return Returns <code>true</code> when exif data is already available from the cache and must
    *         not be loaded.
    */
   private boolean putInExifLoadingQueue(final Photo photo) {

      final PhotoImageMetadata photoImageMetadata = ExifCache.get(photo.imageFilePathName);

      if (photoImageMetadata != null) {

         photo.updateImageMetadata(photoImageMetadata);

         return true;
      }

      PhotoLoadManager.putImageInLoadingQueueExif(photo, new LoadCallbackExif(photo, _currentExifRunId));

      return false;
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _pageDetails.setRedraw(false);
      {
         _photoViewer.getTable().dispose();

         createUI_32_PhotoViewer(_pageDetails);
         _pageDetails.layout();

         // update the viewer
         reloadViewer();
      }
      _pageDetails.setRedraw(true);

      return _photoViewer;
   }

   public void redrawItem(final GalleryMT20Item galleryItem) {
      _galleryMT20.redrawItem(galleryItem);
   }

   public void refreshUI() {
      _galleryMT20.redraw();
   }

   @Override
   public void reloadViewer() {
      _photoViewer.setInput(new Object[0]);
   }

   void restoreState() {

      _galleryMT20.restoreState();

      // set font
      onModifyFont();

      /*
       * gallery sorting
       */
      final GallerySorting defaultSorting = GallerySorting.FILE_NAME;
      final String stateValue = Util.getStateString(_state, STATE_IMAGE_SORTING, defaultSorting.name());
      try {
         _currentSorting = GallerySorting.valueOf(stateValue);
      } catch (final Exception e) {
         _currentSorting = defaultSorting;
      }

      // pref store settings
      updateUI_FromPrefStore(false);

      /**
       * !!! very important !!!
       * <p>
       * show gallery to initialize client area, otherwise the width is 0 until the page is
       * displayed in a later step
       */
      showPageBookPage(_galleryMT20, false);

      // show default page
      _lblDefaultPage.setText(_defaultStatusMessage);
      showPageBookPage(_pageDefault, false);

      /*
       * set thumbnail size after gallery client area is set
       */
      final int stateThumbSize = Util.getStateInt(
            _state,
            STATE_THUMB_IMAGE_SIZE,
            PhotoLoadManager.IMAGE_SIZE_THUMBNAIL);

      // restore gallery action bar
      if (_galleryActionBar != null) {
         _galleryActionBar.restoreState(_state, stateThumbSize);
         _galleryActionBar.setThumbnailSizeVisibility(_galleryMT20.isVertical());
      }

      // restore thumbnail image size
      setThumbnailSizeRestore(stateThumbSize);

      /*
       * gallery folder/tour image positions
       */
      final String[] positionKeys = _state.getArray(STATE_GALLERY_POSITION_KEY);
      final String[] positionValues = _state.getArray(STATE_GALLERY_POSITION_VALUE);
      if (positionKeys != null && positionValues != null) {

         // ensure same size
         if (positionKeys.length == positionValues.length) {

            for (int positionIndex = 0; positionIndex < positionKeys.length; positionIndex++) {

               final String positionValueString = positionValues[positionIndex];

               try {
                  final Double positionValue = Double.parseDouble(positionValueString);

                  _galleryPositions.put(positionKeys[positionIndex], positionValue);

               } catch (final Exception e) {
                  // ignore
               }
            }
         }
      }

      _restoredSelection = Util.getStateIntArray(_state, STATE_SELECTED_ITEMS, null);
   }

   public void saveState() {

      if (_galleryMT20.isVertical()) {

         /*
          * image size is only used in a vertical gallery, horizontal gallery is using the
          * clientarea height to get the image size
          */

         _state.put(STATE_THUMB_IMAGE_SIZE, _photoImageSize);
      }

      _state.put(STATE_IMAGE_SORTING, _currentSorting.name());

      /*
       * keep gallery positions
       */

      // preserve current gallery position
      final double galleryPosition = _galleryMT20.getGalleryPosition();

      if (_currentGalleryPositionKey != null) {
         _galleryPositions.put(_currentGalleryPositionKey, galleryPosition);
      } else if (_newGalleryPositionKey != null) {
         _galleryPositions.put(_newGalleryPositionKey, galleryPosition);
      }

      final Set<String> positionKeys = _galleryPositions.keySet();
      final int positionSize = positionKeys.size();

      if (positionSize > 0) {

         final String[] positionKeyArray = positionKeys.toArray(new String[positionSize]);
         final String[] positionValues = new String[positionSize];

         for (int positionIndex = 0; positionIndex < positionKeyArray.length; positionIndex++) {

            final String positionKey = positionKeyArray[positionIndex];
            positionValues[positionIndex] = _galleryPositions.get(positionKey).toString();
         }

         _state.put(STATE_GALLERY_POSITION_KEY, positionKeyArray);
         _state.put(STATE_GALLERY_POSITION_VALUE, positionValues);
      }

      Util.setState(_state, STATE_SELECTED_ITEMS, _galleryMT20.getSelectionIndex());

      _columnManager.saveState(_state);

      _galleryMT20.saveState();
   }

   void selectGalleryType(final GalleryType galleryType) {

      _galleryType = galleryType;

      showPageGalleryContent();
   }

   public void selectItem(final int itemIndex, final String galleryPositionKey) {

      _galleryMT20.selectItem(itemIndex);

//      // ensure to keep position, this has not worked in the full screen gallery when image was resized
////      _currentGalleryPositionKey = galleryPositionKey;
////      _newGalleryPositionKey = galleryPositionKey;
//
//      final double currentGalleryPosition = _galleryMT20.getGalleryPosition();
////      _galleryPositions.put(galleryPositionKey, currentGalleryPosition);
//
//      System.out.println(UI.timeStampNano() + " selectItem()\t" + galleryPositionKey + "\t" + currentGalleryPosition);
//      // TODO remove SYSTEM.OUT.PRINTLN
   }

   public void setDefaultStatusMessage(final String message) {
      _defaultStatusMessage = message;
   }

   public void setExternalMouseListener(final IExternalGalleryListener externalGalleryMouseListener) {
      _galleryMT20.setExternalMouseListener(externalGalleryMouseListener);
   }

   /**
    * @param photoFilterGPS
    * @param photoFilterTour
    */
   public void setFilter(final PhotoFilterGPS photoFilterGPS, final PhotoFilterTour photoFilterTour) {
      _imageFilterGPS = photoFilterGPS;
      _imageFilterTour = photoFilterTour;
   }

   public void setFocus() {
      _galleryMT20.setFocus();
   }

   public void setFullScreenImageViewer(final FullScreenImageViewer fullScreenImageViewer) {

      _fullScreenImageViewer = fullScreenImageViewer;
      _galleryMT20.setFullScreenImageViewer(fullScreenImageViewer);
   }

   private void setIsLinkPhotoDisplayed(final boolean isLinkPhotoDisplayed) {

      _isLinkPhotoDisplayed = isLinkPhotoDisplayed;
      _photoRenderer.setIsLinkPhotoDisplayed(isLinkPhotoDisplayed);
   }

   public void setPhotoInfo(final boolean isShowPhotoName,
                            final PhotoDateInfo photoDateInfo,
                            final boolean isShowAnnotations,
                            final boolean isShowTooltip) {

      _isShowTooltip = isShowTooltip;
      _isShowAnnotations = isShowAnnotations;

      _photoRenderer.setPhotoInfo(isShowPhotoName, photoDateInfo, isShowAnnotations);
   }

   /**
    * A custom actionbar can be displayed, by default it is hidden. The actionbar can be retrieved
    * with {@link #getCustomActionBarContainer()}.
    * <p>
    * This method <b>must</b> be called before
    * {@link #createImageGallery(Composite, int, IPhotoGalleryProvider)} is called.
    */
   public void setShowCustomActionBar() {
      _isShowCustomActionBar = true;
   }

   /**
    * Prevent to open pref dialog, when it's opened it would close this tooltip and the pref dialog
    * is hidden -->> APP IS FREEZING !!!
    */
   public void setShowOtherShellActions(final boolean isShowOtherShellActions) {
      _galleryMT20.setIsShowOtherShellActions(isShowOtherShellActions);
   }

   void setShowPhotoRatingStars(final RatingStarBehaviour ratingStarBehaviour) {

      _isHandleRatingStars = ratingStarBehaviour == RatingStarBehaviour.HOVERED_STARS;

      _photoRenderer.setShowRatingStars(ratingStarBehaviour);

      // redraw gallery with new settings
      _galleryMT20.redraw();
   }

   /**
    * Thumbnail size combobox can be displayed, by default it is hidden, This method <b>must</b> be
    * called before {@link #createImageGallery(Composite, int, IPhotoGalleryProvider)} is called.
    */
   public void setShowThumbnailSize() {
      _isShowThumbsize = true;
   }

   public void setSorting(final GallerySorting gallerySorting) {

      // set new sorting algorithm
      _currentSorting = gallerySorting;
   }

   private void setStatusMessage(final String message) {

      final IStatusLineManager statusLineManager = _photoGalleryProvider.getStatusLineManager();

      if (statusLineManager != null) {
         statusLineManager.setMessage(message);
      }
   }

   /**
    * Setting thumbnail size is done in the gallery action bar, this can be done only for a
    * vertical gallery.
    *
    * @param newImageSize
    */
   public void setThumbnailSize(final int newImageSize) {

      int newGalleryItemSize = newImageSize + _photoBorderSize;

      if (newGalleryItemSize == _galleryMT20.getItemWidth()) {
         // nothing has changed
         return;
      }

      final int prevGalleryItemSize = _galleryMT20.getItemWidth();

      // update gallery
      final int adjustedItemSize = _galleryMT20.zoomGallery(newGalleryItemSize, false);

      if (adjustedItemSize == -1 || adjustedItemSize == prevGalleryItemSize) {
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

   private void setThumbnailSizeRestore(final int thumbSize) {

      PhotoLoadManager.stopImageLoading(false);

      int requestedItemSize = thumbSize + _photoBorderSize;

      // update gallery
      final int adjustedItemSize = _galleryMT20.zoomGallery(requestedItemSize, true);

      // check if size has been modified when zoomed in/out
      if (adjustedItemSize != requestedItemSize) {

         /*
          * size has been modified, this case can occure when the gallery is switching the
          * scrollbars on/off depending on the content
          */

         requestedItemSize = adjustedItemSize;
      }

      updateUI_AfterZoomInOut(requestedItemSize);
   }

   public void setVertical(final boolean isVerticalGallery) {

      if (isVerticalGallery) {

         final int verticalWidth = _galleryMT20.getVerticalItemWidth();

         updateUI_AfterZoomInOut(verticalWidth);
      }

      _galleryActionBar.setThumbnailSizeVisibility(isVerticalGallery);

      _galleryMT20.setVertical(isVerticalGallery);
   }

   public void showImages(final ArrayList<Photo> allPhotos,
                          final String galleryPositionKey,
                          final boolean isLinkPhotoDisplayed,
                          final boolean isFilteredAndSorted) {

      final Photo[] photos = allPhotos.toArray(new Photo[allPhotos.size()]);

      if (isFilteredAndSorted == false) {

         // sort photos with current sorting algorithm
         Arrays.sort(photos, getCurrentComparator());
      }

      showImages(photos, galleryPositionKey, isLinkPhotoDisplayed);
   }

   /**
    * Display images for a selected folder.
    *
    * @param imageFolder
    * @param isReloadFolder
    * @param isLinkPhotoDisplayed
    */
   public void showImages(final File imageFolder, final boolean isReloadFolder, final boolean isLinkPhotoDisplayed) {

      jobFilter_12_Stop();

      PhotoLoadManager.stopImageLoading(true);

      //////////////////////////////////////////
      //
      // MUST BE REMOVED, IS ONLY FOR TESTING
      //
//      disposeAndDeleteAllImages();
//      PhotoLoadManager.removeInvalidImageFiles();
      //
      // MUST BE REMOVED, IS ONLY FOR TESTING
      //
      //////////////////////////////////////////

      setIsLinkPhotoDisplayed(isLinkPhotoDisplayed);

      if (imageFolder == null) {
         _lblDefaultPage.setText(Messages.Pic_Dir_Label_ReadingFolders);
      } else {

         _lblDefaultPage.setText(NLS.bind(Messages.Pic_Dir_Label_Loading, imageFolder.getAbsolutePath()));
      }
      showPageBookPage(_pageDefault, true);

      _photoFolderWhichShouldBeDisplayed = imageFolder;

      workerUpdate(imageFolder, isReloadFolder);
   }

   public void showImages(final Photo[] allFilteredAnsSortedPhotos,
                          final String galleryPositionKey,
                          final boolean isLinkPhotoDisplayed) {

//      System.out.println(UI.timeStampNano() + " showImages() " + galleryPositionKey);
//      // TODO remove SYSTEM.OUT.PRINTLN

      jobFilter_12_Stop();
      PhotoLoadManager.stopImageLoading(true);

      //////////////////////////////////////////
      //
      // MUST BE REMOVED, IS ONLY FOR TESTING
      //
//      disposeAndDeleteAllImages();
//      PhotoLoadManager.removeInvalidImageFiles();
      //
      // MUST BE REMOVED, IS ONLY FOR TESTING
      //
      //////////////////////////////////////////

      setIsLinkPhotoDisplayed(isLinkPhotoDisplayed);

      // images are not loaded from a folder, photos are already available
      _photoFolder = null;

      _newGalleryPositionKey = galleryPositionKey;

      // initialize tooltip for new images
      _photoGalleryTooltip.reset(true);

      _galleryMT20.deselectAll();

      _allPhotos = allFilteredAnsSortedPhotos;

      final double galleryPosition = getCachedGalleryPosition();

      updateUI_GalleryItems(_allPhotos, galleryPosition);
   }

   /**
    * @param isShowPhotoName
    * @param photoDateInfo
    * @param isShowAnnotations
    * @param isShowTooltip
    */
   void showInfo(final boolean isShowPhotoName,
                 final PhotoDateInfo photoDateInfo,
                 final boolean isShowAnnotations,
                 final boolean isShowTooltip) {

      setPhotoInfo(isShowPhotoName, photoDateInfo, isShowAnnotations, isShowTooltip);

      // reset tooltip, otherwise it could be displayed if it should not
      _photoGalleryTooltip.reset(true);

      _galleryMT20.redraw();
   }

   public void showItem(final int itemIndex) {
      _galleryMT20.showItem(itemIndex);
   }

   private void showPageBookPage(final Composite page, final boolean isDelay) {

      if (isDelay) {

         /*
          * delay showing the default page because it is flickering when an image is displayed
          * again within a few milliseconds
          */

         _delayCounter[0]++;

         if (page == _pageDefault || page == _pageGalleryInfo) {

            _pageBook.getDisplay().timerExec(100, new Runnable() {

               private int       __delayCounter = _delayCounter[0];
               private Composite __page         = page;

               @Override
               public void run() {

                  // check if this still the same page which should be displayed
                  if (__delayCounter == _delayCounter[0]) {
                     _pageBook.showPage(__page);
                  }
               }
            });

         } else {

            _pageBook.showPage(page);
         }

      } else {

         _pageBook.showPage(page);
      }

   }

   private void showPageGalleryContent() {

      Composite galleryContent;

      if (_galleryType == GalleryType.DETAILS) {

         galleryContent = _pageDetails;

         _photoViewer.setInput(new Object());

      } else {

         // default is thumnail gallery
         galleryContent = _galleryMT20;

         // update gallery
         _galleryMT20.setVirtualItems(_sortedGalleryItems, _contentGalleryPosition);
      }

      showPageBookPage(galleryContent, true);
   }

   public void showRestoreFolder(final String restoreFolderName) {

      if (restoreFolderName == null) {
         _lblDefaultPage.setText(Messages.Pic_Dir_StatusLabel_NoFolder);
      } else {
         _lblDefaultPage.setText(NLS.bind(Messages.Pic_Dir_StatusLabel_RestoringFolder, restoreFolderName));
      }

      showPageBookPage(_pageDefault, true);
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

      BusyIndicator.showWhile(_display, () -> sortGallery_10_Runnable());
   }

   /**
    * This will sort all already created gallery items
    */
   private void sortGallery_10_Runnable() {

      if (_allPhotos == null || _allPhotos.length == 0) {
         // there are no files
         return;
      }

      final GalleryMT20Item[] virtualGalleryItems = _galleryMT20.getAllVirtualItems();
      final int virtualSize = virtualGalleryItems.length;

      if (virtualSize == 0) {
         // there are no items
         return;
      }

      // sort photos with new sorting algorithm
      Arrays.sort(_sortedAndFilteredPhotos, getCurrentComparator());

      updateUI_GalleryItems(_sortedAndFilteredPhotos, null);
   }

   public void stopLoadingImages() {

      // stop jobs
      jobFilter_12_Stop();

      PhotoLoadManager.stopImageLoading(true);

      workerStop();
   }

   /**
    * @param fgColor
    * @param bgColor
    * @param selectionFgColor
    * @param noFocusSelectionFgColor
    * @param initUI
    *           Is <code>true</code> after a restore to update the UI that not a default UI color
    *           is displayed.
    */
   public void updateColors(final Color fgColor,
                            final Color bgColor,
                            final Color selectionFgColor,
                            final Color noFocusSelectionFgColor,
                            final boolean initUI) {

      /*
       * set color in action bar only for Linux & Windows, setting color in OSX looks not very
       * good
       */
      if (UI.IS_OSX == false && _galleryActionBar != null) {
// looks ugli with a custom toolbar manager
//         _galleryActionBar.updateColors(fgColor, bgColor);
      }

      /*
       * gallery
       */
      _galleryMT20.setColors(fgColor, bgColor);

      _photoRenderer.setColors(fgColor, bgColor, selectionFgColor, noFocusSelectionFgColor);

      _pageDefault.setBackground(bgColor);

      /*
       * loading page
       */
      _lblDefaultPage.setForeground(fgColor);
      _lblDefaultPage.setBackground(bgColor);

      /*
       * page: folder info
       */
      _pageGalleryInfo.setForeground(fgColor);
      _pageGalleryInfo.setBackground(bgColor);
      _lblGalleryInfo.setForeground(fgColor);
      _lblGalleryInfo.setBackground(bgColor);
   }

   /**
    * The UI of the photos are updated.
    *
    * @param arrayList
    *           Contains photos which are modified. Items in this list should be of type
    *           {@link Photo}.
    */
   public void updatePhotos(final ArrayList<?> arrayList) {

      if (_allPhotos == null) {
         return;
      }

      for (int photoIndex = 0; photoIndex < _allPhotos.length; photoIndex++) {

         final Photo galleryPhoto = _allPhotos[photoIndex];
         final String galleryImageFilePathName = galleryPhoto.imageFilePathName;

         for (final Object object : arrayList) {

            if (object instanceof Photo) {

               final Photo updatedPhoto = (Photo) object;

               if (galleryImageFilePathName.equals(updatedPhoto.imageFilePathName)) {

                  _allPhotos[photoIndex] = updatedPhoto;

                  break;
               }
            }
         }
      }

      /*
       * this is the easy way to update the UI for all visible photos
       */
      _galleryMT20.redraw();
   }

   /**
    * @param galleryItemSizeWithBorder
    *           Image size with border
    */
   private void updateUI_AfterZoomInOut(final int galleryItemSizeWithBorder) {

      final int imageSizeWithoutBorder = galleryItemSizeWithBorder - _photoBorderSize;

      if (imageSizeWithoutBorder != _photoImageSize) {

         // image size has changed

         _photoImageSize = imageSizeWithoutBorder;

         if (_galleryActionBar != null) {
            _galleryActionBar.updateUI_AfterZoomInOut(imageSizeWithoutBorder);
         }

         _photoGalleryTooltip.setGalleryImageSize(_photoImageSize);
         _photoGalleryTooltip.reset(true);

         _isAttributesPainted = _photoRenderer.isAttributesPainted(galleryItemSizeWithBorder);

         enableAttributeActions(_isAttributesPainted);
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

      _galleryMT20.setImageQuality(isShowHighQuality, hqMinSize);

      /*
       * text minimum thumb size
       */
      final int minThumbSize = _prefStore.getInt(IPhotoPreferences.PHOTO_VIEWER_TEXT_MIN_THUMB_SIZE);
      final int borderSize = _prefStore.getInt(IPhotoPreferences.PHOTO_VIEWER_IMAGE_BORDER_SIZE);

      _photoRenderer.setSizeImageBorder(borderSize);
      _photoRenderer.setSizeTextMinThumb(minThumbSize);

      // get update border size
      _photoBorderSize = _photoRenderer.getBorderSize();

      _fullScreenImageViewer.setPrefSettings(isUpdateUI);
   }

   private void updateUI_GalleryInfo() {

      _display.syncExec(() -> {

         if (_galleryMT20.isDisposed()) {
            return;
         }

         showPageBookPage(_pageGalleryInfo, true);

         final int imageCount = _allPhotos.length;

         if (imageCount == 0) {

            if (_defaultStatusMessage.length() > 0) {
               _lblGalleryInfo.setText(_defaultStatusMessage);
            } else {
               _lblGalleryInfo.setText(Messages.Pic_Dir_StatusLabel_NoImages);
            }

         } else {

            final int exifLoadingQueueSize = PhotoLoadManager.getExifQueueSize();

            if (exifLoadingQueueSize > 0) {

               // show filter message only when image files are being loaded

               _lblGalleryInfo.setText(NLS.bind(
                     Messages.Pic_Dir_StatusLabel_FilteringImages,
                     exifLoadingQueueSize));

            } else {

               if (_sortedAndFilteredPhotos.length == 0) {

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

         if (imageCount == 0) {
            _isAttributesPainted = false;
         }

         enableActions(imageCount > 0);
         enableAttributeActions(_isAttributesPainted);
      });
   }

   /**
    * Set gallery items into a list according to the new sorting/filtering
    *
    * @param filteredAndSortedPhotos
    * @param galleryPosition
    *           When <code>null</code> the old position is preserved, otherwise images at a new
    *           position are displayed.
    */
   private void updateUI_GalleryItems(final Photo[] filteredAndSortedPhotos, final Double galleryPosition) {

      final HashMap<String, GalleryMT20Item> existingGalleryItems = _galleryMT20.getCreatedGalleryItems();

      final int photoSize = filteredAndSortedPhotos.length;
      final GalleryMT20Item[] sortedGalleryItems = new GalleryMT20Item[photoSize];

      // convert sorted photos into sorted gallery items
      for (int itemIndex = 0; itemIndex < photoSize; itemIndex++) {

         final Photo sortedPhotos = filteredAndSortedPhotos[itemIndex];

         // get gallery item for the current photo
         final GalleryMT20Item galleryItem = existingGalleryItems.get(sortedPhotos.imageFilePathName);

         if (galleryItem != null) {
            sortedGalleryItems[itemIndex] = galleryItem;
         }
      }

      _sortedAndFilteredPhotos = filteredAndSortedPhotos;

      _display.syncExec(() -> {

         if (_galleryMT20.isDisposed()) {
            return;
         }

         final boolean isItemAvailable = sortedGalleryItems.length > 0;

         enableActions(isItemAvailable);

         if (isItemAvailable) {

            // gallery items are available

            _sortedGalleryItems = sortedGalleryItems;
            _contentGalleryPosition = galleryPosition;

            showPageGalleryContent();

         } else {

            // there is no gallery item

            updateUI_GalleryInfo();
         }
      });
   }

   public void updateUI_StatusMessage(final String message) {

      if (message.length() == 0) {
         setStatusMessage(getStatusDefaultMessage());
      } else {
         setStatusMessage(message);
      }
   }

   public void updateUI_StatusMessageInUIThread(final String message) {

      _display.asyncExec(() -> {

         if (_galleryMT20.isDisposed()) {
            return;
         }

         if (message.length() == 0) {
            setStatusMessage(getStatusDefaultMessage());
         } else {
            setStatusMessage(message);
         }
      });
   }

   /**
    * Get image files from current directory
    */
   private void workerExecute() {

      _workerStart = System.currentTimeMillis();

      Photo[] newPhotos = null;

      if (_workerStateDir != null) {

         _display.syncExec(() -> {

            // guard against the ui being closed before this runs
            if (_uiContainer.isDisposed()) {
               return;
            }

            setStatusMessage(UI.EMPTY_STRING);
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

            newPhotos = new Photo[0];

         } else {

            // image files are available

            numberOfImages = files.length;

            newPhotos = new Photo[numberOfImages];

            // create a photo for each image file
            for (int fileIndex = 0; fileIndex < numberOfImages; fileIndex++) {

               final File photoFile = files[fileIndex];

               Photo galleryPhoto = PhotoCache.getPhoto(photoFile.getAbsolutePath());

               if (galleryPhoto == null) {

                  /*
                   * photo is not found in the photo cache, create a new photo
                   */

                  galleryPhoto = new Photo(photoFile);

                  PhotoCache.setPhoto(galleryPhoto);
               }

               newPhotos[fileIndex] = galleryPhoto;
            }

            /*
             * sort photos with currently selected comparator
             */
            _currentComparator = getCurrentComparator();
            Arrays.sort(newPhotos, _currentComparator);
         }

         // check if the previous files retrival has been interrupted
         if (_workerCancelled) {
            return;
         }

         _photoFolder = _workerStateDir;

         if (_photoFolder != null) {
            _newGalleryPositionKey = _photoFolder.getAbsolutePath();
         }

         workerExecute_DisplayImages(newPhotos);
      }
   }

   private void workerExecute_DisplayImages(final Photo[] photos) {

      _allPhotos = photos;

      _display.syncExec(() -> {

         // guard against the ui being closed before this runs
         if (_uiContainer.isDisposed()) {
            return;
         }

         // initialize tooltip for a new folder
         _photoGalleryTooltip.reset(true);

         final double galleryPosition = getCachedGalleryPosition();

         _galleryMT20.setupItems(0, galleryPosition, _restoredSelection);

         _restoredSelection = null;

         /*
          * update status info
          */
         final long timeDiff = System.currentTimeMillis() - _workerStart;
         final String timeDiffText = NLS.bind(
               Messages.Pic_Dir_Status_Loaded,
               new Object[] { Long.toString(timeDiff), Integer.toString(_allPhotos.length) });

         setStatusMessage(timeDiffText);
      });

      /*
       * start filter always, even when no filter is set because it is loading exif data which is
       * used to sort images correctly by exif date (when available) and not by file date
       */
      jobFilter_20_Schedule1st();
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
    *           the new base directory for the table, null is ignored
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
