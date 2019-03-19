/*
 * 2007-04-29
 * - completely removed DesignTime; this can be re-added later if needed
 * - extends JXPanel -> extends Canvas
 * - changed default constructor to receive standard SWT parameters parent and style
 * - added getHeight() and getWidth() for those Swing calls
 * - added isOpaque() for the same reason
 * - added getInsets() for the same reason
 * - renamed doPaintComponent to paintControl (SWT default); it now also receives a
 * PaintEvent as parameter instead of a Graphics object; it's not private anymore,
 * which is also a SWT convention
 * - addPaintListener() for that method
 * - ported paintControl() to SWT graphics operations
 * - added computeSize()
 * ! basically works
 * however needs much work, no Listeners are implemented yet and there are some
 * issues with thread access
 * - thread access problem found and fixed; UI methods may only be called from the
 * UI thread!
 * 2007-04-30
 * - fixed memory leaks; all images should now be disposed, when no longer needed
 * - implemented Listeners except MouseWheel
 */
package de.byteholder.geoclipse.map;

import de.byteholder.geoclipse.Messages;
import de.byteholder.geoclipse.map.event.IMapGridListener;
import de.byteholder.geoclipse.map.event.IMapInfoListener;
import de.byteholder.geoclipse.map.event.IMapPositionListener;
import de.byteholder.geoclipse.map.event.IPOIListener;
import de.byteholder.geoclipse.map.event.IPositionListener;
import de.byteholder.geoclipse.map.event.MapPOIEvent;
import de.byteholder.geoclipse.map.event.MapPositionEvent;
import de.byteholder.geoclipse.mapprovider.ImageDataResources;
import de.byteholder.geoclipse.mapprovider.MP;
import de.byteholder.geoclipse.mapprovider.MapProviderManager;
import de.byteholder.geoclipse.preferences.IMappingPreferences;
import de.byteholder.geoclipse.ui.TextWrapPainter;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorCacheSWT;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.util.HoveredAreaContext;
import net.tourbook.common.util.IToolTipHideListener;
import net.tourbook.common.util.IToolTipProvider;
import net.tourbook.common.util.ITourToolTipProvider;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.TourToolTip;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourWayPoint;
import net.tourbook.map2.view.WayPointToolTipProvider;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.filter.geo.TourGeoFilterItem;
import net.tourbook.tour.filter.geo.TourGeoFilterManager;
import net.tourbook.ui.IInfoToolTipProvider;
import net.tourbook.ui.IMapToolTipProvider;
import net.tourbook.ui.MTRectangle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.dnd.URLTransfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

public class Map extends Canvas {

   private static final String IMAGE_POI_IN_MAP                      = de.byteholder.geoclipse.poi.Messages.Image_POI_InMap;
   private static final String IMAGE_SEARCH_TOURS_BY_LOCATION        = net.tourbook.Messages.Image__SearchToursByLocation;
   private static final String IMAGE_SEARCH_TOURS_BY_LOCATION_SCROLL = net.tourbook.Messages.Image__SearchToursByLocation_Scroll;

   /**
    * Min zoomlevels which the maps supports
    */
   public static final int     MAP_MIN_ZOOM_LEVEL                    = 0;

   /**
    * Max zoomlevels which the maps supports
    */
   public static final int     MAP_MAX_ZOOM_LEVEL                    = 19;

   /**
    * these zoom levels are displayed in the UI therefore they start with 1 instead of 0
    */
   public static final int     UI_MIN_ZOOM_LEVEL                     = MAP_MIN_ZOOM_LEVEL + 1;

   public static final int     UI_MAX_ZOOM_LEVEL                     = MAP_MAX_ZOOM_LEVEL + 1;

   private static final String DIRECTION_E                           = "E";                                                      //$NON-NLS-1$
   private static final String DIRECTION_N                           = "N";                                                      //$NON-NLS-1$

   /*
    * Wikipedia data
    */
//   private static final String WIKI_PARAMETER_DIM                     = "dim";                                                   //$NON-NLS-1$
   private static final String WIKI_PARAMETER_TYPE = "type"; //$NON-NLS-1$

//   http://toolserver.org/~geohack/geohack.php?pagename=Sydney&language=de&params=33.85_S_151.2_E_region:AU-NSW_type:city(3641422)
//   http://toolserver.org/~geohack/geohack.php?pagename=Palm_Island,_Queensland&params=18_44_S_146_35_E_scale:20000_type:city
//   http://toolserver.org/~geohack/geohack.php?pagename=P%C3%B3voa_de_Varzim&params=41_22_57_N_8_46_45_W_region:PT_type:city//
//
//   where D is degrees, M is minutes, S is seconds, and NS/EWO are the directions
//
//   D;D
//   D_N_D_E
//   D_M_N_D_M_E
//   D_M_S_N_D_M_S_E

   private static final String PATTERN_SEPARATOR                          = "_";                               //$NON-NLS-1$
   private static final String PATTERN_END                                = "_?(.*)";                          //$NON-NLS-1$

   private static final String PATTERN_WIKI_URL                           = ".*pagename=([^&]*).*params=(.*)"; //$NON-NLS-1$
   private static final String PATTERN_WIKI_PARAMETER_KEY_VALUE_SEPARATOR = ":";                               //$NON-NLS-1$

   private static final String PATTERN_DOUBLE                             = "([-+]?[0-9]*\\.?[0-9]+)";         //$NON-NLS-1$
   private static final String PATTERN_DOUBLE_SEP                         = PATTERN_DOUBLE + PATTERN_SEPARATOR;

   private static final String PATTERN_DIRECTION_NS                       = "([NS])_";                         //$NON-NLS-1$
   private static final String PATTERN_DIRECTION_WE                       = "([WE])";                          //$NON-NLS-1$

//   private static final String      PATTERN_WIKI_POSITION_10               = "([-+]?[0-9]*\\.?[0-9]+)_([NS])_([-+]?[0-9]*\\.?[0-9]+)_([WE])_?(.*)";   //$NON-NLS-1$
//   private static final String      PATTERN_WIKI_POSITION_20               = "([0-9]*)_([NS])_([0-9]*)_([WE])_?(.*)";                           //$NON-NLS-1$
//   private static final String      PATTERN_WIKI_POSITION_21               = "([0-9]*)_([0-9]*)_([NS])_([0-9]*)_([0-9]*)_([WE])_?(.*)";            //$NON-NLS-1$
//   private static final String      PATTERN_WIKI_POSITION_22               = "([0-9]*)_([0-9]*)_([0-9]*)_([NS])_([0-9]*)_([0-9]*)_([0-9]*)_([WE])_?(.*)";   //$NON-NLS-1$

   private static final String        PATTERN_WIKI_POSITION_D_D             = PATTERN_DOUBLE + ";"                                        //$NON-NLS-1$
         + PATTERN_DOUBLE
         + PATTERN_END;

   private static final String        PATTERN_WIKI_POSITION_D_N_D_E         = PATTERN_DOUBLE_SEP
         + PATTERN_DIRECTION_NS
         + PATTERN_DOUBLE_SEP
         + PATTERN_DIRECTION_WE
         + PATTERN_END;

   private static final String        PATTERN_WIKI_POSITION_D_M_N_D_M_E     = PATTERN_DOUBLE_SEP
         + PATTERN_DOUBLE_SEP
         + PATTERN_DIRECTION_NS
         + PATTERN_DOUBLE_SEP
         + PATTERN_DOUBLE_SEP
         + PATTERN_DIRECTION_WE
         + PATTERN_END;

   private static final String        PATTERN_WIKI_POSITION_D_M_S_N_D_M_S_E = PATTERN_DOUBLE_SEP
         + PATTERN_DOUBLE_SEP
         + PATTERN_DOUBLE_SEP
         + PATTERN_DIRECTION_NS
         + PATTERN_DOUBLE_SEP
         + PATTERN_DOUBLE_SEP
         + PATTERN_DOUBLE_SEP
         + PATTERN_DIRECTION_WE
         + PATTERN_END;

   private static final Pattern       _patternWikiUrl                       = Pattern.compile(PATTERN_WIKI_URL);
   private static final Pattern       _patternWikiPosition_D_D              = Pattern.compile(PATTERN_WIKI_POSITION_D_D);
   private static final Pattern       _patternWikiPosition_D_N_D_E          = Pattern.compile(PATTERN_WIKI_POSITION_D_N_D_E);
   private static final Pattern       _patternWikiPosition_D_M_N_D_M_E      = Pattern.compile(PATTERN_WIKI_POSITION_D_M_N_D_M_E);
   private static final Pattern       _patternWikiPosition_D_M_S_N_D_M_S_E  = Pattern.compile(PATTERN_WIKI_POSITION_D_M_S_N_D_M_S_E);
   private static final Pattern       _patternWikiParamter                  = Pattern.compile(PATTERN_SEPARATOR);
   private static final Pattern       _patternWikiKeyValue                  = Pattern.compile(PATTERN_WIKI_PARAMETER_KEY_VALUE_SEPARATOR);

   private final static ColorCacheSWT _colorCache                           = new ColorCacheSWT();

   // [181,208,208] is the color of water in the standard OSM material
   public static final RGB        OSM_BACKGROUND_RGB         = new RGB(181, 208, 208);

   private static final RGB       MAP_DEFAULT_BACKGROUND_RGB = new RGB(0x40, 0x40, 0x40);

   private static RGB             MAP_TRANSPARENT_RGB;

   private final IPreferenceStore _prefStore                 = TourbookPlugin.getPrefStore();
   {
      MAP_TRANSPARENT_RGB = net.tourbook.common.UI.IS_OSX //
//            ? new RGB(0x7e, 0x7f, 0x80)
//            ? new RGB(0xfe, 0x00, 0x00)
            ? new RGB(0xfe, 0xfe, 0xfe)
            : new RGB(0xfe, 0xfe, 0xfe)
//
      ;
   }

   private Color                  SYS_COLOR_BLACK;
   private Color                  SYS_COLOR_DARK_GRAY;
   private Color                  SYS_COLOR_GRAY;
   private Color                  SYS_COLOR_WHITE;
   private Color                  SYS_COLOR_YELLOW;

   /**
    * Map zoom level which is currently be used to display tiles. Normally a value between around 0
    * and 20.
    */
   private int                    _mapZoomLevel;
   private boolean                _isZoomWithMousePosition;

   /**
    * This image contains the map which is painted in the map viewport
    */
   private Image                  _mapImage;

   private Image                  _9PartImage;
   private GC                     _9PartGC;

   /**
    * Indicates whether or not to draw the borders between tiles. Defaults to false. not very nice
    * looking, very much a product of testing Consider whether this should really be a property or
    * not.
    */
   private boolean                _isShowDebug_TileInfo;
   private boolean                _isShowDebug_TileBorder;
   private boolean                _isShowDebug_GeoGrid;

   /**
    * Factory used by this component to grab the tiles necessary for painting the map.
    */
   private MP                     _mp;

   /**
    * The position in latitude/longitude of the "address" being mapped. This is a special coordinate
    * that, when moved, will cause the map to be moved as well. It is separate from "center" in that
    * "center" tracks the current center (in pixels) of the view port whereas this will not change
    * when panning or zooming. Whenever the addressLocation is changed, however, the map will be
    * repositioned.
    */
   private GeoPosition            _addressLocation;

   /**
    * The overlay to delegate to for painting the "foreground" of the map component. This would
    * include painting waypoints, day/night, etc. Also receives mouse events.
    */
   private final List<MapPainter> _overlays               = new ArrayList<>();

   private final TileLoadObserver _tileImageLoadObserver  = new TileLoadObserver();

   private final Cursor           _cursorCross;
   private final Cursor           _cursorDefault;
   private final Cursor           _cursorPan;
   private final Cursor           _cursorSearchTour;
   private final Cursor           _cursorSearchTour_Scroll;

   private final AtomicInteger    _redrawMapCounter       = new AtomicInteger();
   private final AtomicInteger    _overlayRunnableCounter = new AtomicInteger();

   private boolean                _isLeftMouseButtonPressed;
   private boolean                _isMapPanned;

   private int                    _mouseMovePositionX     = Integer.MIN_VALUE;
   private int                    _mouseMovePositionY     = Integer.MIN_VALUE;
   private int                    _mouseMovePositionX_Last;
   private int                    _mouseMovePositionY_Last;
   private Point                  _mouseDownPosition;

   private Thread                 _overlayThread;
   private long                   _nextOverlayRedrawTime;

   private final NumberFormat     _nf1;
   private final NumberFormat     _nf2;
   private final NumberFormat     _nf3;
   private final NumberFormat     _nfLatLon;
   {
      _nf1 = NumberFormat.getNumberInstance();
      _nf2 = NumberFormat.getNumberInstance();
      _nf3 = NumberFormat.getNumberInstance();

      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
      _nf2.setMinimumFractionDigits(2);
      _nf2.setMaximumFractionDigits(2);
      _nf3.setMinimumFractionDigits(3);
      _nf3.setMaximumFractionDigits(3);

      _nfLatLon = NumberFormat.getNumberInstance();
      _nfLatLon.setMinimumFractionDigits(4);
      _nfLatLon.setMaximumFractionDigits(4);
   }

   private final TextWrapPainter                    _textWrapper             = new TextWrapPainter();

   /**
    * cache for overlay images
    */
   private OverlayImageCache                        _overlayImageCache;

   /**
    * This queue contains tiles which overlay image must be painted
    */
   private final ConcurrentLinkedQueue<Tile>        _tileOverlayPaintQueue   = new ConcurrentLinkedQueue<>();

   private boolean                                  _isRunningDrawOverlay;

   private String                                   _overlayKey;

   /**
    * this painter is called when the map is painted in the onPaint event
    */
   private IDirectPainter                           _directMapPainter;

   private final DirectPainterContext               _directMapPainterContext = new DirectPainterContext();

   /**
    * when <code>true</code> the overlays are painted
    */
   private boolean                                  _isDrawOverlays;

   /**
    * contains a legend which is painted in the map
    */
   private MapLegend                                _mapLegend;

   private boolean                                  _isLegendVisible;

   /**
    * This is the most important point for the map because all operations depend on it.
    * <p>
    * Center position of the map viewport in <I>world pixel</I>. Dragging the map component will
    * change the center position. Zooming in/out will cause the center to be recalculated so as to
    * remain in the center of the new "map".
    * <p>
    * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    * <br>
    * This MUST be in {@link Double} to be accurate when the map is zoomed<br>
    * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    * <br>
    */
   private Point2D                                  _worldPixelMapCenter     = null;
   /**
    * Viewport in the map where the {@link #_mapImage} is painted <br>
    * <br>
    * <b>x</b> and <b>y</b> is the <b>top/left</b> corner in world pixel<br>
    * <b>width</b> and <b>height</b> contains the visible area in device pixel
    * <p>
    * <b>!!! x/y values can also be negative when the map is smaller than the viewport !!!</b>
    * <p>
    * I havn't yet fully understood how it works but I adjusted the map successfully in 10.7 and
    * tried to document this behaviour.
    */
   private Rectangle                                _worldPixelTopLeftViewport;
   /**
    * Size in device pixel where the map is displayed
    */
   private Rectangle                                _devMapViewport;

   /**
    * Size of the map in tiles at the current zoom level {@link #_mapZoomLevel} (num tiles tall by
    * num tiles wide)
    */
   private Dimension                                _mapTileSize;

   /**
    * Size of a tile in pixel (tile is quadratic)
    */
   private int                                      _tilePixelSize;

   /**
    * Size of a geo grid 0.01 degree in pixel
    */
   private double                                   _geoGridPixelSizeX;
   private double                                   _geoGridPixelSizeY;

   /**
    * Contains the client area of the map without trimmings, this rectangle has the width and height
    * of the map image
    */
   private Rectangle                                _clientArea;

   private final ListenerList<IMapGridListener>     _allMapGridListener      = new ListenerList<>(ListenerList.IDENTITY);
   private final ListenerList<IMapInfoListener>     _allMapInfoListener      = new ListenerList<>(ListenerList.IDENTITY);
   private final ListenerList<IMapPositionListener> _allMapPositionListener  = new ListenerList<>(ListenerList.IDENTITY);
   private final ListenerList<IPositionListener>    _mousePositionListeners  = new ListenerList<>(ListenerList.IDENTITY);
   private final ListenerList<IPOIListener>         _poiListeners            = new ListenerList<>(ListenerList.IDENTITY);

   // measurement system
   private float           _distanceUnitValue   = 1;
   private String          _distanceUnitLabel   = UI.EMPTY_STRING;
   private boolean         _isScaleVisible;

   private final Color     _transparentColor;
   private final Color     _defaultBackgroundColor;
   /*
    * POI image
    */
   private boolean         _isPoiVisible;
   private boolean         _isPoiPositionInViewport;
   //
   private final Image     _poiImage;
   private final Rectangle _poiImageBounds;
   private final Point     _poiImageDevPosition = new Point(0, 0);
   /*
    * POI tooltip
    */
   private PoiToolTip      _poiTT;
   private final int       _poiTTOffsetY        = 5;

   private TourToolTip     _tourToolTip;

   /**
    * when <code>true</code> the loading... image is not displayed
    */
   private boolean         _isLiveView;
   private long            _lastMapDrawTime;

   /*
    * These 4 tile positions correspond to the tiles which are needed to draw the map
    */
   private int           _tilePosMinX;
   private int           _tilePosMaxX;
   private int           _tilePosMinY;
   private int           _tilePosMaxY;
   //
   private final Display _display;
   private final Thread  _displayThread;
   //
   private int           _jobCounterSplitImages = 0;
   private Object        _splitJobFamily        = new Object();
   private boolean       _isCancelSplitJobs;
   //
   /**
    * When <code>true</code> the tour is painted in the map in the enhanced mode otherwise in the
    * simple mode
    */
   private boolean       _isTourPaintMethodEnhanced;
   //
   /*
    * Download offline images
    */
   private boolean _offline_IsSelectingOfflineArea;
   private boolean _offline_IsOfflineSelectionStarted;
   private boolean _offline_IsPaintOfflineArea;

   private Point   _offline_DevMouse_Start;
   private Point   _offline_DevMouse_End;
   private Point   _offline_DevTileStart;
   private Point   _offline_DevTileEnd;

   private Point   _offline_WorldMouse_Start;
   private Point   _offline_WorldMouse_End;
   private Point   _offline_WorldMouse_Move;

//   private Rectangle _offline_CurrentOfflineArea;
//   private Rectangle _offline_PreviousOfflineArea;
//   private int       _offline_PreviousOfflineArea_MapZoomLevel;

   /**
    * Top/left position
    */
   private Point               _grid_SelectedPosition_Geo_1_E2;

   /**
    * Bottom/right position
    */
   private Point               _grid_SelectedPosition_Geo_2_E2;

   private IMapContextProvider _mapContextProvider;

   /**
    * Is <code>true</code> when the map context menu can be displayed
    */
   private boolean             _isContextMenuEnabled   = true;

   private DropTarget          _dropTarget;

   private boolean             _isRedrawEnabled        = true;

   private HoveredAreaContext  _hoveredAreaContext;

   private int                 _overlayAlpha           = 0xff;

   private MapGridBoxItem      _grid_GridBoxItem_Hovered;
   private MapGridBoxItem      _grid_GridBoxItem_Selected;

   private int[]               _grid_AutoScrollCounter = new int[1];
   private boolean             _isGridAutoScroll;

   /**
    * This observer is called in the {@link Tile} when a tile image is set into the tile
    */
   private final class TileLoadObserver implements Observer {

      @Override
      public void update(final Observable observable, final Object arg) {

         if (observable instanceof Tile) {

            final Tile tile = (Tile) observable;

            if (tile.getZoom() == _mapZoomLevel) {

               /*
                * Because we are not in the UI thread, we have to queue the call for redraw and
                * cannot do it directly.
                */
               paint();

               tile.deleteObserver(this);
            }
         }
      }
   }

   /**
    * Create a new Map
    */
   public Map(final Composite parent, final int style) {

      super(parent, style | SWT.DOUBLE_BUFFERED);

      _display = getDisplay();
      _displayThread = _display.getThread();

      addAllListener();
      addDropTarget();

      createContextMenu();

      updateGraphColors();

      _cursorPan = new Cursor(_display, SWT.CURSOR_SIZEALL);
      _cursorCross = new Cursor(_display, SWT.CURSOR_CROSS);
      _cursorDefault = new Cursor(_display, SWT.CURSOR_ARROW);
      _cursorSearchTour = UI.createCursorFromImage(TourbookPlugin.getImageDescriptor(IMAGE_SEARCH_TOURS_BY_LOCATION));
      _cursorSearchTour_Scroll = UI.createCursorFromImage(TourbookPlugin.getImageDescriptor(IMAGE_SEARCH_TOURS_BY_LOCATION_SCROLL));

      _transparentColor = new Color(_display, MAP_TRANSPARENT_RGB);
      _defaultBackgroundColor = new Color(_display, MAP_DEFAULT_BACKGROUND_RGB);

      SYS_COLOR_BLACK = _display.getSystemColor(SWT.COLOR_BLACK);
      SYS_COLOR_DARK_GRAY = _display.getSystemColor(SWT.COLOR_DARK_GRAY);
      SYS_COLOR_GRAY = _display.getSystemColor(SWT.COLOR_GRAY);
      SYS_COLOR_WHITE = _display.getSystemColor(SWT.COLOR_WHITE);
      SYS_COLOR_YELLOW = _display.getSystemColor(SWT.COLOR_YELLOW);

      _poiImage = TourbookPlugin.getImageDescriptor(IMAGE_POI_IN_MAP).createImage();
      _poiImageBounds = _poiImage.getBounds();

      paintOverlay_0_SetupThread();
   }

   /**
    * @return Returns rgb values for the color which is used as transparent color in the map.
    */
   public static RGB getTransparentRGB() {
      return MAP_TRANSPARENT_RGB;
   }

   public void actionManageOfflineImages(final Event event) {

      // check if offline image is active
      final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
      if (prefStore.getBoolean(IMappingPreferences.OFFLINE_CACHE_USE_OFFLINE) == false) {

         MessageDialog.openInformation(
               _display.getActiveShell(),
               Messages.Dialog_OfflineArea_Error,
               Messages.Dialog_OfflineArea_Error_NoOffline);

         return;
      }

      // check if offline loading is running
      if (OfflineLoadManager.isLoading()) {

         MessageDialog.openInformation(
               _display.getActiveShell(),
               Messages.Dialog_OfflineArea_Error,
               Messages.Dialog_OfflineArea_Error_IsLoading);

         return;
      }

//      if (_offline_DevMouse_Start != null //
//
//            && _offline_CurrentOfflineArea != null
//            && (event.stateMask & SWT.CONTROL) != 0) {
//
//         /*
//          * use old offline area when the ctrl-key is pressed
//          */
//
//         _offline_PreviousOfflineArea = null;
//         _offline_IsPaintOfflineArea = true;
//
//         redraw();
//         paint();
//
//         offline_OpenOfflineImageDialog();
//
//         return;
//      }

//      _offline_PreviousOfflineArea = _offline_CurrentOfflineArea;
//      _offline_PreviousOfflineArea_MapZoomLevel = _mapZoomLevel;

      _offline_IsPaintOfflineArea = true;
      _offline_IsSelectingOfflineArea = true;

      _offline_DevMouse_Start = null;
      _offline_DevMouse_End = null;

      setCursor(_cursorCross);

      redraw();
      paint();
   }

   public void actionSearchTourByLocation(final Event event) {

      _grid_GridBoxItem_Hovered = new MapGridBoxItem();

      // set initial mouse move position from the current mouse position
      _grid_GridBoxItem_Hovered.worldMouse_Move = new Point(
            _worldPixelTopLeftViewport.x + _mouseMovePositionX,
            _worldPixelTopLeftViewport.y + _mouseMovePositionY);

      _grid_GridBoxItem_Hovered.gridBoxSizeMuliplier = Util.getStateInt(TourGeoFilterManager.getState(),
            TourGeoFilterManager.STATE_GRID_BOX_SIZE,
            TourGeoFilterManager.STATE_GRID_BOX_SIZE_DEFAULT);

      setCursor(_cursorSearchTour);

      redraw();
      paint();
   }

   private void addAllListener() {

      addPaintListener(new PaintListener() {
         @Override
         public void paintControl(final PaintEvent e) {
            onPaint(e);
         }
      });

      addDisposeListener(new DisposeListener() {
         @Override
         public void widgetDisposed(final DisposeEvent e) {
            onDispose(e);
         }
      });

      addFocusListener(new FocusListener() {
         @Override
         public void focusGained(final FocusEvent e) {
            updatePoiVisibility();
         }

         @Override
         public void focusLost(final FocusEvent e) {
// this is critical because the tool tip get's hidden when there are actions available in the tool tip shell
//            hidePoiToolTip();
         }
      });

      addMouseListener(new MouseListener() {

         @Override
         public void mouseDoubleClick(final MouseEvent event) {
            onMouse_DoubleClick(event);
         }

         @Override
         public void mouseDown(final MouseEvent event) {
            onMouse_Down(event);
         }

         @Override
         public void mouseUp(final MouseEvent event) {
            onMouse_Up(event);
         }
      });

      addMouseTrackListener(new MouseTrackListener() {

         @Override
         public void mouseEnter(final MouseEvent e) {}

         @Override
         public void mouseExit(final MouseEvent e) {

            // keep position for out of the map events, e.g. recenter map
            _mouseMovePositionX_Last = _mouseMovePositionX;
            _mouseMovePositionY_Last = _mouseMovePositionY;

            // set position out of the map that to tool tip is not activated again
            _mouseMovePositionX = Integer.MIN_VALUE;
            _mouseMovePositionY = Integer.MIN_VALUE;

            // stop grid autoscrolling
            _isGridAutoScroll = false;
         }

         @Override
         public void mouseHover(final MouseEvent e) {}
      });

      addMouseMoveListener(new MouseMoveListener() {
         @Override
         public void mouseMove(final MouseEvent event) {
            onMouse_Move(event);
         }
      });

      addListener(SWT.MouseVerticalWheel, new Listener() {
         @Override
         public void handleEvent(final Event event) {
            onMouse_Wheel(event);
         }
      });

      addListener(SWT.MouseHorizontalWheel, new Listener() {
         @Override
         public void handleEvent(final Event event) {
            onMouse_Wheel(event);
         }
      });

      addListener(SWT.KeyDown, new Listener() {
         @Override
         public void handleEvent(final Event event) {
            onKey_Down(event);
         }
      });

      addControlListener(new ControlAdapter() {
         @Override
         public void controlResized(final ControlEvent e) {
            onResize();
         }
      });

      addTraverseListener(new TraverseListener() {
         @Override
         public void keyTraversed(final TraverseEvent e) {
            // enable travers keys
            e.doit = true;
         }
      });
   }

   /**
    * Set map as drop target
    */
   private void addDropTarget() {

      _dropTarget = new DropTarget(this, DND.DROP_MOVE | DND.DROP_COPY);
      _dropTarget.setTransfer(new Transfer[] { URLTransfer.getInstance(), TextTransfer.getInstance() });

      _dropTarget.addDropListener(new DropTargetAdapter() {
         @Override
         public void dragEnter(final DropTargetEvent event) {
            if ((event.detail == DND.DROP_DEFAULT) || (event.detail == DND.DROP_MOVE)) {
               event.detail = DND.DROP_COPY;
            }
         }

         @Override
         public void dragLeave(final DropTargetEvent event) {

         }

         @Override
         public void dragOver(final DropTargetEvent event) {
            if ((event.detail == DND.DROP_DEFAULT) || (event.detail == DND.DROP_MOVE)) {
               event.detail = DND.DROP_COPY;
            }
         }

         @Override
         public void drop(final DropTargetEvent event) {

            if (event.data == null) {
               event.detail = DND.DROP_NONE;
               return;
            }

            /*
             * run async to free the mouse cursor from the drop operation
             */
            _display.asyncExec(new Runnable() {
               @Override
               public void run() {
                  onDropRunnable(event);
               }
            });
         }
      });
   }

   public void addMapGridBoxListener(final IMapGridListener mapListener) {
      _allMapGridListener.add(mapListener);
   }

   public void addMapInfoListener(final IMapInfoListener mapInfoListener) {
      _allMapInfoListener.add(mapInfoListener);
   }

   public void addMapPositionListener(final IMapPositionListener mapPanListener) {
      _allMapPositionListener.add(mapPanListener);
   }

   public void addMousePositionListener(final IPositionListener mapListener) {
      _mousePositionListeners.add(mapListener);
   }

   /**
    * Adds a map overlay. This is a Painter which will paint on top of the map. It can be used to
    * draw waypoints, lines, or static overlays like text messages.
    *
    * @param overlay
    *           the map overlay to use
    * @see org.jdesktop.swingx.painters.Painter
    */
   public void addOverlayPainter(final MapPainter overlay) {

      _overlays.add(overlay);

      paint();
   }

   public void addPOIListener(final IPOIListener poiListener) {
      _poiListeners.add(poiListener);
   }

   /**
    * Checks if an image can be reused, this is true if the image exists and has the same size
    *
    * @param newWidth
    * @param newHeight
    * @return
    */
   private boolean canReuseImage(final Image image, final Rectangle clientArea) {

      // check if we could reuse the existing image

      if ((image == null) || image.isDisposed()) {
         return false;
      }

      // image exist, check image bounds
      final Rectangle oldBounds = image.getBounds();

      if (!((oldBounds.width == clientArea.width) && (oldBounds.height == clientArea.height))) {
         return false;
      }

      return true;
   }

   /**
    * make sure that the parted overlay image has the correct size
    */
   private void checkImageTemplate9Parts() {

      final int parts = 3;
      final int partedTileSize = _tilePixelSize * parts;

      if ((_9PartImage != null) && (_9PartImage.isDisposed() == false)) {
         if (_9PartImage.getBounds().width == partedTileSize) {
            // image is OK
            return;
         }
      }
      if (_9PartImage != null) {
         _9PartImage.dispose();
      }
      if (_9PartGC != null) {
         _9PartGC.dispose();
      }

      // create 9 part image/gc
      final ImageData transparentImageData = de.byteholder.geoclipse.map.UI.createTransparentImageData(
            partedTileSize);

      _9PartImage = new Image(_display, transparentImageData);
      _9PartGC = new GC(_9PartImage);
   }

   /**
    * Checks validation of a world pixel by using the current zoom level and map tile size.
    *
    * @param newWorldPixelCenter
    * @return Returns adjusted world pixel when necessary.
    */
   private Point2D.Double checkWorldPixel(final Point2D newWorldPixelCenter) {

      final long maxWidth = _mapTileSize.width * _tilePixelSize;
      final long maxHeight = _mapTileSize.height * _tilePixelSize;

      double newCenterX = newWorldPixelCenter.getX();
      double newCenterY = newWorldPixelCenter.getY();

      if (newCenterX < 0) {
         newCenterX = -1;
      }
      if (newCenterX > maxWidth) {
         newCenterX = maxWidth + 1;
      }

      if (newCenterY < 0) {
         newCenterY = -1;
      }

      if (newCenterY > maxHeight) {
         newCenterY = maxHeight + 1;
      }

      return new Point2D.Double(newCenterX, newCenterY);
   }

   private Point2D.Double checkWorldPixel(final Point2D newWorldPixelCenter, final int zoomLevel) {

      final Dimension mapTileSize = _mp.getMapTileSize(zoomLevel);

      final long maxWidth = mapTileSize.width * _tilePixelSize;
      final long maxHeight = mapTileSize.height * _tilePixelSize;

      double newCenterX = newWorldPixelCenter.getX();
      double newCenterY = newWorldPixelCenter.getY();

      if (newCenterX < 0) {
         newCenterX = -1;
      }
      if (newCenterX > maxWidth) {
         newCenterX = maxWidth + 1;
      }

      if (newCenterY < 0) {
         newCenterY = -1;
      }

      if (newCenterY > maxHeight) {
         newCenterY = maxHeight + 1;
      }

      return new Point2D.Double(newCenterX, newCenterY);
   }

   @Override
   public org.eclipse.swt.graphics.Point computeSize(final int wHint, final int hHint, final boolean changed) {
      return getParent().getSize();
   }

   /**
    * create the context menu
    */
   private void createContextMenu() {

      final MenuManager menuMgr = new MenuManager();

      menuMgr.setRemoveAllWhenShown(true);

      menuMgr.addMenuListener(new IMenuListener() {
         @Override
         public void menuAboutToShow(final IMenuManager menuMgr) {

            if ((_mp == null) || _isContextMenuEnabled == false) {
               return;
            }

            if (_mapContextProvider != null) {
               _mapContextProvider.fillContextMenu(menuMgr);
            }

            menuMgr.add(new Separator());
            menuMgr.add(new ActionManageOfflineImages(Map.this));
         }
      });

      setMenu(menuMgr.createContextMenu(this));
   }

   /**
    * Creates a new image, old image is disposed
    *
    * @param display
    * @param image
    *           image which will be disposed if the image is not null
    * @param clientArea
    * @return returns a new created image
    */
   private Image createMapImage(final Display display, final Image image, final Rectangle clientArea) {

      if (image != null) {
         image.dispose();
      }

      // ensure the image has a width/height of 1, otherwise this causes troubles
      final int width = Math.max(1, clientArea.width);
      final int height = Math.max(1, clientArea.height);

      return new Image(display, width, height);
   }

   public void deleteFailedImageFiles() {
      MapProviderManager.deleteOfflineMap(_mp, true);
   }

   public synchronized void dimMap(final int dimLevel, final RGB dimColor) {

      _mp.setDimLevel(dimLevel, dimColor);

      // remove all cached map images
      _mp.disposeTileImages();

      resetAll();
   }

   /**
    * Disposes all overlay image cache and the overlay painting queue
    */
   public synchronized void disposeOverlayImageCache() {

      if (_mp != null) {
         _mp.resetOverlays();
      }

      _tileOverlayPaintQueue.clear();

      _overlayImageCache.dispose();
   }

   private void disposeResource(final Resource resource) {

      if ((resource != null) && !resource.isDisposed()) {
         resource.dispose();
      }
   }

   public void disposeTiles() {
      _mp.disposeTiles();
   }

   private void fireMapGridEvent(final boolean isGridSelected, final MapGridBoxItem gridBoxItem) {

      final Object[] listeners = _allMapGridListener.getListeners();

      final GeoPosition geoCenter = getMapGeoCenter();

      for (final Object listener : listeners) {
         ((IMapGridListener) listener).onMapGrid(
               _grid_SelectedPosition_Geo_1_E2,
               _grid_SelectedPosition_Geo_2_E2,
               _mapZoomLevel,
               geoCenter,
               isGridSelected,
               gridBoxItem);
      }
   }

   private void fireMapInfoEvent() {

      final GeoPosition geoCenter = getMapGeoCenter();

      final Object[] listeners = _allMapInfoListener.getListeners();

      for (final Object listener : listeners) {
         ((IMapInfoListener) listener).onMapInfo(geoCenter, _mapZoomLevel);
      }
   }

   /**
    * @param isZoomed
    *           Is <code>true</code> when the event is fired by zooming
    */
   private void fireMapPositionEvent(final boolean isZoomed) {

      final GeoPosition geoCenter = getMapGeoCenter();

      final Object[] listeners = _allMapPositionListener.getListeners();

      for (final Object listener : listeners) {
         ((IMapPositionListener) listener).onMapPosition(geoCenter, _mapZoomLevel, isZoomed);
      }
   }

   private void fireMousePosition() {

      // check position, can initially be null
      if ((_mouseMovePositionX == Integer.MIN_VALUE) || (_mp == null)) {
         return;
      }

      /*
       * !!! DON'T OPTIMIZE THE NEXT LINE, OTHERWISE THE WRONG MOUSE POSITION IS FIRED !!!
       */
      final Rectangle topLeftViewPort = getWorldPixelTopLeftViewport(_worldPixelMapCenter);

      final int worldMouseX = topLeftViewPort.x + _mouseMovePositionX;
      final int worldMouseY = topLeftViewPort.y + _mouseMovePositionY;

      final GeoPosition geoPosition = _mp.pixelToGeo(new Point2D.Double(worldMouseX, worldMouseY), _mapZoomLevel);
      final MapPositionEvent event = new MapPositionEvent(geoPosition, _mapZoomLevel);

      final Object[] listeners = _mousePositionListeners.getListeners();
      for (final Object listener : listeners) {
         ((IPositionListener) listener).setPosition(event);
      }
   }

   private void firePOIEvent(final GeoPosition geoPosition, final String poiText) {

      final MapPOIEvent event = new MapPOIEvent(geoPosition, _mapZoomLevel, poiText);

      final Object[] listeners = _poiListeners.getListeners();
      for (final Object listener : listeners) {
         ((IPOIListener) listener).setPOI(event);
      }
   }

   /**
    * Gets the current address location of the map. This property does not change when the user pans
    * the map. This property is bound.
    *
    * @return the current map location (address)
    */
   public GeoPosition getAddressLocation() {
      return _addressLocation;
   }

   /**
    * Parse bounding box string.
    *
    * @param boundingBox
    * @return Returns a set with bounding box positions or <code>null</code> when boundingBox cannot
    *         be parsed.
    */
   private Set<GeoPosition> getBoundingBoxPositions(final String boundingBox) {

// example
//      "48.4838981628418,48.5500030517578,9.02030849456787,9.09173774719238"

      final String[] boundingBoxValues = boundingBox.split(","); //$NON-NLS-1$

      if (boundingBoxValues.length != 4) {
         return null;
      }

      try {

         final Set<GeoPosition> positions = new HashSet<>();

         positions.add(
               new GeoPosition(//
                     Double.parseDouble(boundingBoxValues[0]),
                     Double.parseDouble(boundingBoxValues[2])));

         positions.add(
               new GeoPosition(//
                     Double.parseDouble(boundingBoxValues[1]),
                     Double.parseDouble(boundingBoxValues[3])));

         return positions;

      } catch (final Exception e) {
         return null;
      }
   }

   public Rectangle getBoundingRect(final Set<GeoPosition> positions, final int zoom) {

      final java.awt.Point geoPixel = _mp.geoToPixel(positions.iterator().next(), zoom);
      final Rectangle rect = new Rectangle(geoPixel.x, geoPixel.y, 0, 0);

      for (final GeoPosition pos : positions) {
         final java.awt.Point point = _mp.geoToPixel(pos, zoom);
         rect.add(new Rectangle(point.x, point.y, 0, 0));
      }
      return rect;
   }

   /**
    * @return Returns the legend of the map
    */
   public MapLegend getLegend() {
      return _mapLegend;
   }

   /**
    * A property indicating the center position of the map, or <code>null</code> when a tile factory
    * is not set
    *
    * @return Returns the current center position of the map in latitude/longitude
    */
   public GeoPosition getMapGeoCenter() {

      if (_mp == null) {
         return null;
      }

      return _mp.pixelToGeo(_worldPixelMapCenter, _mapZoomLevel);
   }

   /**
    * @return Returns the overlay map painter which are defined as plugin extension
    */
   public List<MapPainter> getMapPainter() {
      return _overlays;
   }

   /**
    * Get the current map provider
    *
    * @return Returns the current map provider
    */
   public MP getMapProvider() {
      return _mp;
   }

   /**
    * @param tileKey
    * @return Returns the key to identify overlay images in the image cache
    */
   private String getOverlayKey(final Tile tile) {
      return _overlayKey + tile.getTileKey();
   }

   /**
    * @param tile
    * @param xOffset
    * @param yOffset
    * @param projectionId
    * @return
    */
   private String getOverlayKey(final Tile tile, final int xOffset, final int yOffset, final String projectionId) {
      return _overlayKey + tile.getTileKey(xOffset, yOffset, projectionId);
   }

   private PoiToolTip getPoi() {

      if (_poiTT == null) {
         _poiTT = new PoiToolTip(getShell());
      }

      return _poiTT;
   }

   /**
    * @param positions
    *           Geo positions
    * @param zoom
    *           Requested zoom level
    * @return Returns a rectangle in world positions which contains all geo positions for the given
    *         zoom level
    */
   public Rectangle getWorldPixelFromGeoPositions(final Set<GeoPosition> positions, final int zoom) {

      // set first point
      final java.awt.Point point1 = _mp.geoToPixel(positions.iterator().next(), zoom);
      final MTRectangle mtRect = new MTRectangle(point1.x, point1.y, 0, 0);

      // set 2..n points
      for (final GeoPosition pos : positions) {
         final java.awt.Point point = _mp.geoToPixel(pos, zoom);
         mtRect.add(point.x, point.y);
      }

      return new Rectangle(mtRect.x, mtRect.y, mtRect.width, mtRect.height);
   }

   /**
    * Returns the bounds of the viewport in pixels. This can be used to transform points into the
    * world bitmap coordinate space. The viewport is the part of the map, that you can currently see
    * on the screen.
    *
    * @return Returns the bounds in <em>pixels</em> of the "view" of this map
    */
   private Rectangle getWorldPixelTopLeftViewport(final Point2D worldPixelMapCenter) {

      if (_clientArea == null) {
         _clientArea = getClientArea();
      }

      final int devWidth = _clientArea.width;
      final int devHeight = _clientArea.height;

      final int worldX = (int) (worldPixelMapCenter.getX() - (devWidth / 2d));
      final int worldY = (int) (worldPixelMapCenter.getY() - (devHeight / 2d));

      return new Rectangle(worldX, worldY, devWidth, devHeight);
   }

   /**
    * @return Returns the map viewport in world pixel for the current map center
    *         <p>
    *         <b>x</b> and <b>y</b> contains the position in world pixel of the top left viewport in
    *         the map<br>
    *         <b>width</b> and <b>height</b> contains the visible area in device pixel
    */
   public Rectangle getWorldPixelViewport() {
      return getWorldPixelTopLeftViewport(_worldPixelMapCenter);
   }

   /**
    * Gets the current zoom level, or <code>null</code> when a tile factory is not set
    *
    * @return Returns the current zoom level of the map
    */
   public int getZoom() {
      return _mapZoomLevel;
   }

   /**
    * @param boundingBox
    * @return Returns zoom level or -1 when bounding box is <code>null</code>.
    */
   public int getZoom(final String boundingBox) {

      // original: setBoundsZoomLevel

      if (boundingBox == null) {
         return -1;
      }

      final Set<GeoPosition> positions = getBoundingBoxPositions(boundingBox);
      if (positions == null) {
         return -1;
      }

      final MP mp = getMapProvider();

      final int maximumZoomLevel = mp.getMaximumZoomLevel();
      int zoom = mp.getMinimumZoomLevel();

      Rectangle positionRect = getWorldPixelFromGeoPositions(positions, zoom);
      Rectangle viewport = getWorldPixelViewport();

      // zoom in until bounding box is larger than the viewport
      while ((positionRect.width < viewport.width) && (positionRect.height < viewport.height)) {

         // center position in the map
         final java.awt.Point center = new java.awt.Point(//
               positionRect.x + positionRect.width / 2,
               positionRect.y + positionRect.height / 2);

         setMapCenter(mp.pixelToGeo(center, zoom));

         zoom++;

         // check zoom level
         if (zoom >= maximumZoomLevel) {
            break;
         }
         setZoom(zoom);

         positionRect = getWorldPixelFromGeoPositions(positions, zoom);
         viewport = getWorldPixelViewport();
      }

      // the algorithm generated a larger zoom level as necessary
      zoom--;

      setZoom(zoom);

      return zoom;
   }

   /**
    * Hide geo grid and reset all states
    */
   private void grid_DisableGridBoxSelection() {

      _isContextMenuEnabled = true;

      setCursor(_cursorDefault);
      redraw();
   }

   /**
    * @param mouseBorderPosition
    * @param eventTime
    */
   private void grid_DoAutoScroll(final Point mouseBorderPosition) {

      final int AUTO_SCROLL_INTERVAL = 50; // 20ms == 50fps

      _isGridAutoScroll = true;
      _grid_AutoScrollCounter[0]++;
      setCursor(_cursorSearchTour_Scroll);

      getDisplay().timerExec(AUTO_SCROLL_INTERVAL, new Runnable() {

         final int __runnableScrollCounter = _grid_AutoScrollCounter[0];

         @Override
         public void run() {

            if (__runnableScrollCounter != _grid_AutoScrollCounter[0]) {
               // a new runnable is created
               return;
            }

            if (isDisposed() || _isGridAutoScroll == false) {
               // auto scrolling is stopped
               return;
            }

            /*
             * set new map center
             */

            final int mapDiffX = mouseBorderPosition.x;
            final int mapDiffY = mouseBorderPosition.y;

            final double oldCenterX = _worldPixelMapCenter.getX();
            final double oldCenterY = _worldPixelMapCenter.getY();

            final double newCenterX = oldCenterX - mapDiffX;
            final double newCenterY = oldCenterY - mapDiffY;

            // set new map center
            setMapCenterInWorldPixel(new Point2D.Double(newCenterX, newCenterY));
            updateViewPortData();

            paint();

            fireMapPositionEvent(false);

            // start scrolling again when the bounds have not been reached
            final Point mouseBorderPosition = grid_GetMouseBorderPosition();
            final boolean isRepeatScrolling = mouseBorderPosition != null;
            if (isRepeatScrolling) {
               getDisplay().timerExec(AUTO_SCROLL_INTERVAL, this);
            } else {
               _isGridAutoScroll = false;
               setCursor(_cursorSearchTour);
            }
         }
      });
   }

   /**
    * @param mouseEvent
    * @return Returns mouse positon in the map border or <code>null</code> when the border is not
    *         hovered. The returned absolute values are higher when the mouse is closer to the
    *         border.
    */
   private Point grid_GetMouseBorderPosition() {

      final int mapBorderSize = 30;

      final int mapWidth = _clientArea.width;
      final int mapHeight = _clientArea.height;

      // check map min size
      if (mapWidth < 2 * mapBorderSize || mapHeight < 2 * mapBorderSize) {
         return null;
      }

      final int mouseX = _mouseMovePositionX;
      final int mouseY = _mouseMovePositionY;

      int x = 0;
      int y = 0;

      boolean isInBorder = false;

      // check left border, returns -x
      if (mouseX < mapBorderSize) {
         isInBorder = true;
         x = (mapBorderSize - mouseX);
      }

      // check right border, returns +x
      if (mouseX > mapWidth - mapBorderSize) {
         isInBorder = true;
         x = -(mapBorderSize - (mapWidth - mouseX));
      }

      // check top border, returns +y
      if (mouseY < mapBorderSize) {
         isInBorder = true;
         y = mapBorderSize - mouseY;
      }

      // check bottom border, returns -y
      if (mouseY > mapHeight - mapBorderSize) {
         isInBorder = true;
         y = -(mapBorderSize - (mapHeight - mouseY));
      }

      if (isInBorder) {

         return new Point(x, y);

      } else {

         return null;
      }
   }

   private void grid_UpdateEndPosition(final MouseEvent mouseEvent, final MapGridBoxItem gridBoxItem) {

      final int worldMouseX = _worldPixelTopLeftViewport.x + mouseEvent.x;
      final int worldMouseY = _worldPixelTopLeftViewport.y + mouseEvent.y;

      final Point worldMouse_End = new Point(worldMouseX, worldMouseY);

      gridBoxItem.dev_End = new Point(mouseEvent.x, mouseEvent.y);
      gridBoxItem.world_End = worldMouse_End;
      gridBoxItem.geo_End = _mp.pixelToGeo(new Point2D.Double(worldMouse_End.x, worldMouse_End.y), _mapZoomLevel);
   }

   private void hideHoveredArea() {

      if (_tourToolTip == null) {
         return;
      }

      // update tool tip because it has it's own mouse move listener for the map
      _tourToolTip.hideHoveredArea();

      if (_hoveredAreaContext != null) {

         // hide hovered area
         _hoveredAreaContext = null;

         redraw();
      }
   }

   private void initMap() {

      _mapTileSize = _mp.getMapTileSize(_mapZoomLevel);
      _tilePixelSize = _mp.getTileSize();

      final double tileDefaultCenter = (double) _tilePixelSize / 2;

      _worldPixelMapCenter = new Point2D.Double(tileDefaultCenter, tileDefaultCenter);
      _worldPixelTopLeftViewport = getWorldPixelTopLeftViewport(_worldPixelMapCenter);
   }

   /**
    * Checks if a part within the parted image is modified. This method is optimized to search first
    * all 4 borders and then the whole image.
    *
    * @param imageData9Parts
    * @param srcXStart
    * @param srcYStart
    * @param tileSize
    * @return Returns <code>true</code> when a part is modified
    */
   private boolean isPartImageDataModified(final ImageData imageData9Parts,
                                           final int srcXStart,
                                           final int srcYStart,
                                           final int tileSize) {

      final int transRed = MAP_TRANSPARENT_RGB.red;
      final int transGreen = MAP_TRANSPARENT_RGB.green;
      final int transBlue = MAP_TRANSPARENT_RGB.blue;

      final byte[] srcData = imageData9Parts.data;
      final int srcBytesPerLine = imageData9Parts.bytesPerLine;
      final int pixelBytes = imageData9Parts.depth == 32 ? 4 : 3;

      int srcIndex;
      int srcRed, srcGreen, srcBlue;

      // check border: top
      {
         final int srcY = srcYStart;
         final int srcYBytesPerLine = srcY * srcBytesPerLine;

         for (int srcX = srcXStart; srcX < srcXStart + tileSize; srcX++) {

            srcIndex = srcYBytesPerLine + (srcX * pixelBytes);

            srcBlue = srcData[srcIndex] & 0xFF;
            srcGreen = srcData[srcIndex + 1] & 0xFF;
            srcRed = srcData[srcIndex + 2] & 0xFF;

            if ((srcRed != transRed) || (srcGreen != transGreen) || (srcBlue != transBlue)) {
               return true;
            }
         }
      }

      // check border: bottom
      {
         final int srcY = srcYStart + tileSize - 1;
         final int srcYBytesPerLine = srcY * srcBytesPerLine;

         for (int srcX = srcXStart; srcX < srcXStart + tileSize; srcX++) {

            srcIndex = srcYBytesPerLine + (srcX * pixelBytes);

            srcBlue = srcData[srcIndex] & 0xFF;
            srcGreen = srcData[srcIndex + 1] & 0xFF;
            srcRed = srcData[srcIndex + 2] & 0xFF;

            if ((srcRed != transRed) || (srcGreen != transGreen) || (srcBlue != transBlue)) {
               return true;
            }
         }
      }

      // check border: left
      {
         final int srcX = srcXStart * pixelBytes;

         for (int srcY = srcYStart; srcY < srcYStart + tileSize; srcY++) {

            srcIndex = (srcY * srcBytesPerLine) + srcX;

            srcBlue = srcData[srcIndex] & 0xFF;
            srcGreen = srcData[srcIndex + 1] & 0xFF;
            srcRed = srcData[srcIndex + 2] & 0xFF;

            if ((srcRed != transRed) || (srcGreen != transGreen) || (srcBlue != transBlue)) {
               return true;
            }
         }
      }

      // check border: right
      {
         final int srcX = (srcXStart + tileSize - 1) * pixelBytes;

         for (int srcY = srcYStart; srcY < srcYStart + tileSize; srcY++) {

            srcIndex = (srcY * srcBytesPerLine) + srcX;

            srcBlue = srcData[srcIndex] & 0xFF;
            srcGreen = srcData[srcIndex + 1] & 0xFF;
            srcRed = srcData[srcIndex + 2] & 0xFF;

            if ((srcRed != transRed) || (srcGreen != transGreen) || (srcBlue != transBlue)) {
               return true;
            }
         }
      }

      // check whole image
      for (int srcY = srcYStart; srcY < srcYStart + tileSize; srcY++) {

         final int srcYBytesPerLine = srcY * srcBytesPerLine;

         for (int srcX = srcXStart; srcX < srcXStart + tileSize; srcX++) {

            srcIndex = srcYBytesPerLine + (srcX * pixelBytes);

            srcBlue = srcData[srcIndex] & 0xFF;
            srcGreen = srcData[srcIndex + 1] & 0xFF;
            srcRed = srcData[srcIndex + 2] & 0xFF;

            if ((srcRed != transRed) || (srcGreen != transGreen) || (srcBlue != transBlue)) {
               return true;
            }
         }
      }

      return false;
   }

   /**
    * Checks is a tile position is within a map. It is possible that the tile is outside of the map
    * when it's value is negative or greater than the map border.
    * <p>
    * {@link #setMapCenterInWorldPixel(Point2D)}
    * <p>
    * Before version 10.6 the map was repeated on the x axis.
    *
    * @param tilePosX
    * @param tilePosY
    * @return
    */
   private boolean isTileOnMap(final int tilePosX, final int tilePosY) {

      if (tilePosY < 0 || tilePosY >= _mapTileSize.height) {
         return false;
      } else {

         if (_mapZoomLevel < 5) {

            if (tilePosX < 0 || tilePosX >= _mapTileSize.width) {
               return false;
            }
         } else {

            // display one additional tile when the the map is zoomed enough
            if (tilePosX < -1 || tilePosX > _mapTileSize.width) {
               return false;
            }
         }
         return true;
      }
   }

   /**
    * Hide offline area and all states
    */
   private void offline_DisableOfflineAreaSelection() {

      _offline_IsSelectingOfflineArea = false;
      _offline_IsPaintOfflineArea = false;
      _offline_IsOfflineSelectionStarted = false;

      _isContextMenuEnabled = true;

      setCursor(_cursorDefault);
      redraw();
   }

   /**
    * Create top/left geo grid position from world position
    *
    * @param worldPosX
    * @param worldPosY
    * @return
    */
   private Point offline_GetDevGeoGridPosition(final int worldPosX, final int worldPosY) {

      final Point2D.Double worldPixel = new Point2D.Double(worldPosX, worldPosY);

      final GeoPosition geoPos = _mp.pixelToGeo(worldPixel, _mapZoomLevel);

      // truncate to 0.01

      final double geoLat = (int) (geoPos.latitude * 100) / 100.0;
      final double geoLon = (int) (geoPos.longitude * 100) / 100.0;

      final java.awt.Point worldGrid = _mp.geoToPixel(new GeoPosition(geoLat, geoLon), _mapZoomLevel);

      // get device rectangle for the position
      final Point gridGeoPos = new Point(//
            worldGrid.x - _worldPixelTopLeftViewport.x,
            worldGrid.y - _worldPixelTopLeftViewport.y);

      /*
       * Adjust Y that X and Y are at the top/left position otherwise Y is at the bottom/left
       * position
       */
      gridGeoPos.y -= _geoGridPixelSizeY;

      return gridGeoPos;
   }

   private Point offline_GetTilePosition(final int worldPosX, final int worldPosY) {

      int tilePosX = (int) Math.floor((double) worldPosX / (double) _tilePixelSize);
      int tilePosY = (int) Math.floor((double) worldPosY / (double) _tilePixelSize);

      final int mapTiles = _mapTileSize.width;

      /*
       * adjust tile position to the map border
       */
      tilePosX = tilePosX % mapTiles;
      if (tilePosX < -mapTiles) {
         tilePosX += mapTiles;
         if (tilePosX == mapTiles) {
            tilePosX = 0;
         }
      }

      if (tilePosY < 0) {
         tilePosY = 0;
      } else if ((tilePosY >= mapTiles) && (mapTiles > 0)) {
         tilePosY = mapTiles - 1;
      }

      // get device rectangle for this tile
      return new Point(//
            tilePosX * _tilePixelSize - _worldPixelTopLeftViewport.x,
            tilePosY * _tilePixelSize - _worldPixelTopLeftViewport.y);
   }

   private void offline_OpenOfflineImageDialog() {

      new DialogManageOfflineImages(
            _display.getActiveShell(),
            _mp,
            _offline_WorldMouse_Start,
            _offline_WorldMouse_End,
            _mapZoomLevel).open();

      offline_DisableOfflineAreaSelection();

      // force to reload map images
      _mp.disposeTileImages();

      redraw();
      paint();
   }

   private void offline_UpdateOfflineAreaEndPosition(final MouseEvent mouseEvent) {

      final int worldMouseX = _worldPixelTopLeftViewport.x + mouseEvent.x;
      final int worldMouseY = _worldPixelTopLeftViewport.y + mouseEvent.y;

      _offline_DevMouse_End = new Point(mouseEvent.x, mouseEvent.y);
      _offline_WorldMouse_End = new Point(worldMouseX, worldMouseY);

      _offline_DevTileEnd = offline_GetTilePosition(worldMouseX, worldMouseY);
   }

   /**
    * onDispose is called when the map is disposed
    *
    * @param e
    */
   private void onDispose(final DisposeEvent e) {

      if (_mp != null) {
         _mp.resetAll(false);
      }
      if (_dropTarget != null) {
         _dropTarget.dispose();
      }

      disposeResource(_mapImage);
      disposeResource(_poiImage);

      disposeResource(_9PartImage);
      disposeResource(_9PartGC);

      disposeResource(_cursorCross);
      disposeResource(_cursorDefault);
      disposeResource(_cursorPan);
      disposeResource(_cursorSearchTour);
      disposeResource(_cursorSearchTour_Scroll);

      disposeResource(_defaultBackgroundColor);
      disposeResource(_transparentColor);

      // dispose resources in the overlay plugins
      for (final MapPainter overlay : _overlays) {
         overlay.dispose();
      }

      _overlayImageCache.dispose();
      _colorCache.dispose();

      if (_directMapPainter != null) {
         _directMapPainter.dispose();
      }

      // dispose legend image
      if (_mapLegend != null) {
         disposeResource(_mapLegend.getImage());
      }

      if (_poiTT != null) {
         _poiTT.dispose();
      }

      // stop overlay thread
      _overlayThread.interrupt();
   }

   private void onDropRunnable(final DropTargetEvent event) {

      final TransferData transferDataType = event.currentDataType;

      boolean isPOI = false;

      if (TextTransfer.getInstance().isSupportedType(transferDataType)) {

         if (event.data instanceof String) {
            isPOI = parsePOIText((String) event.data);
         }

      } else if (URLTransfer.getInstance().isSupportedType(transferDataType)) {
         isPOI = parsePOIText((String) event.data);
      }

      if (isPOI == false) {

         String poiText = Messages.Dialog_DropNoPOI_InvalidData;

         if (event.data instanceof String) {

            poiText = (String) event.data;

            final int maxLength = 1000;
            if (poiText.length() > maxLength) {
               poiText = poiText.substring(0, maxLength) + "..."; //$NON-NLS-1$
            }
         }

         MessageDialog.openInformation(
               getShell(), //
               Messages.Dialog_DropNoPOI_Title,
               NLS.bind(Messages.Dialog_DropNoPOI_Message, poiText));
      }
   }

   private void onKey_Down(final Event event) {

      if (_offline_IsSelectingOfflineArea) {
         offline_DisableOfflineAreaSelection();
         return;
      }

      if (_grid_GridBoxItem_Hovered != null) {

         _grid_GridBoxItem_Hovered = null;
         _isGridAutoScroll = false;
         setCursor(null);

         grid_DisableGridBoxSelection();

         return;
      }

      // accelerate with Ctrl + Shift key
      int offset = (event.stateMask & SWT.CTRL) != 0 ? 20 : 1;

      if (offset == 1) {
         // check if command (OSX) is set
         offset = (event.stateMask & SWT.COMMAND) != 0 ? 20 : 1;
      }
      offset *= (event.stateMask & SWT.SHIFT) != 0 ? 1 : 40;

      int xDiff = 0;
      int yDiff = 0;

      switch (event.keyCode) {
      case SWT.ARROW_LEFT:
         xDiff = offset;
         break;
      case SWT.ARROW_RIGHT:
         xDiff = -offset;
         break;
      case SWT.ARROW_UP:
         yDiff = offset;
         break;
      case SWT.ARROW_DOWN:
         yDiff = -offset;
         break;
      }

      switch (event.character) {
      case '+':
         zoomIn();
         break;
      case '-':
         zoomOut();
         break;
      }

      if (xDiff != 0 || yDiff != 0) {
         recenterMap(xDiff, yDiff);
      }
   }

   private void onMouse_DoubleClick(final MouseEvent mouseEvent) {

      if (mouseEvent.button == 1) {

         /*
          * set new map center
          */
         final double x = _worldPixelTopLeftViewport.x + mouseEvent.x;
         final double y = _worldPixelTopLeftViewport.y + mouseEvent.y;

         setMapCenterInWorldPixel(new Point2D.Double(x, y));

         // ensure that all internal data are correctly setup
         setZoom(_mapZoomLevel);

         paint();
      }
   }

   private void onMouse_Down(final MouseEvent mouseEvent) {

      // check if left mouse button is pressed
      if (mouseEvent.button != 1) {
         return;
      }

      hideHoveredArea();
      setPoiVisible(false);

      final Point devMousePosition = new Point(mouseEvent.x, mouseEvent.y);

      if (_offline_IsSelectingOfflineArea) {

         _offline_IsOfflineSelectionStarted = true;

         final int worldMouseX = _worldPixelTopLeftViewport.x + mouseEvent.x;
         final int worldMouseY = _worldPixelTopLeftViewport.y + mouseEvent.y;

         _offline_DevMouse_Start = devMousePosition;
         _offline_DevMouse_End = devMousePosition;

         _offline_WorldMouse_Start = new Point(worldMouseX, worldMouseY);
         _offline_WorldMouse_End = _offline_WorldMouse_Start;

         _offline_DevTileStart = offline_GetTilePosition(worldMouseX, worldMouseY);

         redraw();

      } else if (_grid_GridBoxItem_Hovered != null) {

         _grid_GridBoxItem_Hovered.isSelectionStarted = true;

         final int worldMouseX = _worldPixelTopLeftViewport.x + mouseEvent.x;
         final int worldMouseY = _worldPixelTopLeftViewport.y + mouseEvent.y;
         final Point worldMousePosition = new Point(worldMouseX, worldMouseY);

         _grid_GridBoxItem_Hovered.dev_Start = devMousePosition;
         _grid_GridBoxItem_Hovered.dev_End = devMousePosition;

         _grid_GridBoxItem_Hovered.world_Start = worldMousePosition;
         _grid_GridBoxItem_Hovered.world_End = worldMousePosition;

         final GeoPosition geoMousePosition = _mp.pixelToGeo(new Point2D.Double(worldMousePosition.x, worldMousePosition.y), _mapZoomLevel);
         _grid_GridBoxItem_Hovered.geo_Start = geoMousePosition;
         _grid_GridBoxItem_Hovered.geo_End = geoMousePosition;

         redraw();

      } else {

         // if the left mb is clicked remember this point (for panning)
         _isLeftMouseButtonPressed = true;
         _mouseDownPosition = devMousePosition;

         setCursor(_cursorPan);
      }
   }

   private void onMouse_Move(final MouseEvent mouseEvent) {

      if (_mp == null) {
         return;
      }

      _mouseMovePositionX = mouseEvent.x;
      _mouseMovePositionY = mouseEvent.y;

      // keep position for out of the map events, e.g. recenter map
      _mouseMovePositionX_Last = _mouseMovePositionX;
      _mouseMovePositionY_Last = _mouseMovePositionY;

      final int worldMouseX = _worldPixelTopLeftViewport.x + _mouseMovePositionX;
      final int worldMouseY = _worldPixelTopLeftViewport.y + _mouseMovePositionY;

      if (_offline_IsSelectingOfflineArea) {

         _offline_WorldMouse_Move = new Point(worldMouseX, worldMouseY);

         offline_UpdateOfflineAreaEndPosition(mouseEvent);

         paint();

         fireMapInfoEvent();

         return;

      } else if (_grid_GridBoxItem_Hovered != null) {

         _grid_GridBoxItem_Hovered.worldMouse_Move = new Point(worldMouseX, worldMouseY);

         final Point mouseBorderPosition = grid_GetMouseBorderPosition();
         if (mouseBorderPosition != null) {

            // scroll map

            grid_DoAutoScroll(mouseBorderPosition);

            return;
         }

         grid_UpdateEndPosition(mouseEvent, _grid_GridBoxItem_Hovered);

         paint();

         fireMapInfoEvent();
         fireMapGridEvent(false, _grid_GridBoxItem_Hovered);

         return;

      }

      if (_isLeftMouseButtonPressed) {
         panMap(mouseEvent);
         return;
      }

      // #######################################################################

      if (_tourToolTip != null && _tourToolTip.isActive()) {

         /*
          * check if the mouse is within a hovered area
          */
         boolean isContextValid = false;
         if (_hoveredAreaContext != null) {

            final int topLeftX = _hoveredAreaContext.hoveredTopLeftX;
            final int topLeftY = _hoveredAreaContext.hoveredTopLeftY;

            if (_mouseMovePositionX >= topLeftX
                  && _mouseMovePositionX < topLeftX + _hoveredAreaContext.hoveredWidth
                  && _mouseMovePositionY >= topLeftY
                  && _mouseMovePositionY < topLeftY + _hoveredAreaContext.hoveredHeight) {

               isContextValid = true;
            }
         }

         if (isContextValid == false) {
            /*
             * old hovered context is not valid any more, update the hovered context
             */
            updateTourToolTip_HoveredArea();
         }
      }

      if (_poiTT != null && _isPoiPositionInViewport) {

         // check if mouse is within the poi image
         if (_isPoiVisible
               && (_mouseMovePositionX > _poiImageDevPosition.x)
               && (_mouseMovePositionX < _poiImageDevPosition.x + _poiImageBounds.width)
               && (_mouseMovePositionY > _poiImageDevPosition.y - _poiTTOffsetY - 5)
               && (_mouseMovePositionY < _poiImageDevPosition.y + _poiImageBounds.height)) {

            // display poi
            showPoi();

         } else {
            setPoiVisible(false);
         }
      }

      fireMousePosition();
   }

   private void onMouse_Up(final MouseEvent mouseEvent) {

      if (_offline_IsSelectingOfflineArea) {

         _isContextMenuEnabled = false;

         if (_offline_IsOfflineSelectionStarted == false) {
            /*
             * offline selection is not started, this can happen when the right mouse button is
             * clicked
             */
            offline_DisableOfflineAreaSelection();

            return;
         }

         offline_UpdateOfflineAreaEndPosition(mouseEvent);

         // reset cursor
         setCursor(_cursorDefault);

         // hide selection
         _offline_IsSelectingOfflineArea = false;

         redraw();
         paint();

         offline_OpenOfflineImageDialog();

      } else if (_grid_GridBoxItem_Hovered != null) {

         // finalize grid selecting

         _isContextMenuEnabled = false;

         if (_grid_GridBoxItem_Hovered.isSelectionStarted == false) {

            // this can happen when the right mouse button is clicked

            _grid_GridBoxItem_Hovered = null;
            _isGridAutoScroll = true;

            grid_DisableGridBoxSelection();

            return;
         }

         /*
          * Show selected grid box
          */

         grid_UpdateEndPosition(mouseEvent, _grid_GridBoxItem_Hovered);

         _grid_GridBoxItem_Selected = _grid_GridBoxItem_Hovered;

         _grid_GridBoxItem_Hovered = null;
         _isGridAutoScroll = true;

         grid_DisableGridBoxSelection();

         redraw();
         paint();

         fireMapGridEvent(true, _grid_GridBoxItem_Selected);

      } else {

         if (mouseEvent.button == 1) {
            if (_isMapPanned) {
               _isMapPanned = false;
               redraw();
            }
            _mouseDownPosition = null;
            _isLeftMouseButtonPressed = false;
            setCursor(_cursorDefault);

         } else if (mouseEvent.button == 2) {
            // if the middle mouse button is clicked, recenter the view
//            recenterMap(event.x, event.y);
         }
      }

      // show poi info when mouse is within the poi image
      if ((_mouseMovePositionX > _poiImageDevPosition.x)
            && (_mouseMovePositionX < _poiImageDevPosition.x + _poiImageBounds.width)
            && (_mouseMovePositionY > _poiImageDevPosition.y - _poiTTOffsetY - 5)
            && (_mouseMovePositionY < _poiImageDevPosition.y + _poiImageBounds.height)) {

         setPoiVisible(true);
      }

   }

   private void onMouse_Wheel(final Event event) {

      if (event.count < 0) {
         zoomOut();
      } else {
         zoomIn();
      }
   }

   /**
    * There are far too many calls from SWT on this method. Much more than would bereally needed. I
    * don't know why this is. As a result of this, the Component uses up much CPU, because it runs
    * through all the tile loading code for every call. The tile loading code should only be called,
    * if something has changed. When something has changed we produce a buffer image with the
    * contents of the view port (Double/Triple buffer). This happens in the queueRedraw() method.
    * The image gets painted on every call of this method.
    */
   private void onPaint(final PaintEvent event) {

      // draw map image to the screen

      if ((_mapImage != null) && !_mapImage.isDisposed()) {

         final GC gc = event.gc;

         gc.drawImage(_mapImage, 0, 0);

         if (_directMapPainter != null) {

            // is drawing sliders in map/legent

            _directMapPainterContext.gc = gc;
            _directMapPainterContext.viewport = _worldPixelTopLeftViewport;

            _directMapPainter.paint(_directMapPainterContext);
         }

         if (_hoveredAreaContext != null) {
            final Image hoveredImage = _hoveredAreaContext.hoveredImage;
            if (hoveredImage != null) {
               gc.drawImage( //
                     hoveredImage,
                     _hoveredAreaContext.hoveredTopLeftX,
                     _hoveredAreaContext.hoveredTopLeftY);
            }
         }

         if (_isPoiVisible && _poiTT != null) {
            if (_isPoiPositionInViewport = updatePoiImageDevPosition()) {
               gc.drawImage(_poiImage, _poiImageDevPosition.x, _poiImageDevPosition.y);
            }
         }

         if (_tourToolTip != null) {
            _tourToolTip.paint(gc, _clientArea);
         }

         if (_offline_IsPaintOfflineArea) {
            paint_OfflineArea(gc);
         }

         if (_grid_GridBoxItem_Selected != null) {
            paint_GridBox_20_Selected(gc, _grid_GridBoxItem_Selected);
         }
         if (_grid_GridBoxItem_Hovered != null) {
            paint_GridBox_10_Hovered(gc, _grid_GridBoxItem_Hovered);
         }
      }
   }

   private void onResize() {

      /*
       * the method getClientArea() is only correct in a dialog when it's called in the create()
       * method after super.create();
       */

      _clientArea = getClientArea();

      updateViewPortData();

      paint();
   }

   /**
    * Put a map redraw into a queue, the last entry in the queue will be executed
    */
   public void paint() {

      final int redrawCounter = _redrawMapCounter.incrementAndGet();

      if (isDisposed() || _mp == null || _isRedrawEnabled == false) {
         return;
      }

      if (_devMapViewport == null) {

         // internal data are not yet initialized, this happens only the first time when a map is displayed

         initMap();
         updateViewPortData();
      }

      // get time when the redraw is requested
      final long requestedRedrawTime = System.currentTimeMillis();
      final long timeDiff = requestedRedrawTime - _lastMapDrawTime;

      if (timeDiff > 100) {

         // update display even when this is not the last created runnable

         _display.syncExec(new Runnable() {

            @Override
            public void run() {

               if (isDisposed()) {
                  return;
               }

               paint_10_PaintMapImage();
            }
         });

      } else {

         final Runnable asynchImageRunnable = new Runnable() {

            final int __asynchRunnableCounter = redrawCounter;

            @Override
            public void run() {

               if (isDisposed()) {
                  return;
               }

               // check if a newer runnable is available
               if (__asynchRunnableCounter != _redrawMapCounter.get()) {
                  // a newer queryRedraw is available
                  return;
               }

               paint_10_PaintMapImage();
            }
         };

         _display.asyncExec(asynchImageRunnable);
      }

      // tell the overlay thread to draw the overlay images
      _nextOverlayRedrawTime = requestedRedrawTime;
   }

   /**
    * Draws map tiles/legend/scale into the map image which is displayed in the SWT paint event.
    */
   private void paint_10_PaintMapImage() {

      if (isDisposed()) {
         return;
      }

      // Draw the map
      GC gcMapImage = null;
      try {

         // check or create map image
         final Image image = _mapImage;
         if ((image == null) || image.isDisposed() || (canReuseImage(image, _clientArea) == false)) {
            _mapImage = createMapImage(_display, image, _clientArea);
         }

         gcMapImage = new GC(_mapImage);
         {
            paint_30_Tiles(gcMapImage);

            if (_isLegendVisible && _mapLegend != null) {
               paint_40_Legend(gcMapImage);
            }

            if (_isScaleVisible) {
               paint_50_Scale(gcMapImage);
            }

            if (_isShowDebug_GeoGrid) {
               paint_Debug_GeoGrid(gcMapImage);
            }
         }

      } catch (final Exception e) {

         e.printStackTrace();

         // map image is corrupt
         _mapImage.dispose();

      } finally {
         if (gcMapImage != null) {
            gcMapImage.dispose();
         }
      }

      redraw();

      _lastMapDrawTime = System.currentTimeMillis();
   }

   /**
    * Draw all visible tiles into the map viewport
    *
    * @param gcMapImage
    */
   private void paint_30_Tiles(final GC gcMapImage) {

      for (int tilePosX = _tilePosMinX; tilePosX <= _tilePosMaxX; tilePosX++) {
         for (int tilePosY = _tilePosMinY; tilePosY <= _tilePosMaxY; tilePosY++) {

            /*
             * convert tile world position into device position
             */
            final int devTileX = tilePosX * _tilePixelSize - _worldPixelTopLeftViewport.x;
            final int devTileY = tilePosY * _tilePixelSize - _worldPixelTopLeftViewport.y;

            final Rectangle devTileViewport = new Rectangle(devTileX, devTileY, _tilePixelSize, _tilePixelSize);

            // check if current tile is within the painting area
            if (devTileViewport.intersects(_devMapViewport)) {

               /*
                * get the tile from the factory. the tile must not have been completely downloaded
                * after this step.
                */

               if (isTileOnMap(tilePosX, tilePosY)) {

                  paintTile(gcMapImage, tilePosX, tilePosY, devTileViewport);

               } else {

                  gcMapImage.setBackground(_defaultBackgroundColor);
                  gcMapImage.fillRectangle(devTileViewport.x, devTileViewport.y, _tilePixelSize, _tilePixelSize);
               }
            }
         }
      }
   }

   private void paint_40_Legend(final GC gc) {

      // get legend image from the legend
      final Image legendImage = _mapLegend.getImage();
      if ((legendImage == null) || legendImage.isDisposed()) {
         return;
      }

      final Rectangle imageBounds = legendImage.getBounds();

      // draw legend on bottom left
      int yPos = _worldPixelTopLeftViewport.height - 5 - imageBounds.height;
      yPos = Math.max(5, yPos);

      final Point legendPosition = new Point(5, yPos);
      _mapLegend.setLegendPosition(legendPosition);

      gc.drawImage(legendImage, legendPosition.x, legendPosition.y);
   }

   /**
    * Paint scale for the map center
    *
    * @param gc
    */
   private void paint_50_Scale(final GC gc) {

      final int viewPortWidth = _worldPixelTopLeftViewport.width;

      final int devScaleWidth = viewPortWidth / 3;
      final float metricWidth = 111.32f / _distanceUnitValue;

      //
      final GeoPosition mapCenter = getMapGeoCenter();
      final double latitude = mapCenter.latitude;
      final double longitude = mapCenter.longitude;

      final double devDistance = _mp.getDistance(//
            new GeoPosition(latitude - 0.5, longitude),
            new GeoPosition(latitude + 0.5, longitude),
            _mapZoomLevel);

      final double scaleGeo = metricWidth * (devScaleWidth / devDistance);

      final double scaleGeoRounded = Util.roundDecimalValue(scaleGeo);
      final int devScaleWidthRounded = (int) (scaleGeoRounded / metricWidth * devDistance);

      // get scale text
      String scaleFormatted;
      if (scaleGeoRounded < 1f) {
         scaleFormatted = _nf2.format(scaleGeoRounded);
      } else {
         scaleFormatted = Integer.toString((int) scaleGeoRounded);
      }
      final String scaleText = scaleFormatted + UI.SPACE + _distanceUnitLabel;
      final Point textExtent = gc.textExtent(scaleText);

      final int devX1 = viewPortWidth - 5 - devScaleWidthRounded;
      final int devX2 = devX1 + devScaleWidthRounded;

      final int devY = _worldPixelTopLeftViewport.height - 5 - 3;

      final int devYScaleLines = devY;

      final Path path1 = new Path(_display);
      final Path path2 = new Path(_display);
      final int offset = -1;
      {
         path1.moveTo(devX1, devY);
         path1.lineTo(devX2, devY);

         path2.moveTo(devX1, devY + offset);
         path2.lineTo(devX2, devY + offset);

         gc.setLineWidth(1);

         gc.setForeground(SYS_COLOR_WHITE);
         gc.drawPath(path1);

         gc.setForeground(SYS_COLOR_BLACK);
         gc.drawPath(path2);
      }
      path1.dispose();
      path2.dispose();

      final int devYText = devYScaleLines - textExtent.y;
      final int devXText = devX1 + devScaleWidthRounded - textExtent.x;

      // paint text with shadow
      final Color borderColor = new Color(_display, 0xF1, 0xEE, 0xE8);
      {
         gc.setForeground(borderColor);
//         gc.drawText(scaleText, devXText + 1, devYText + 1, true);

         gc.drawText(scaleText, devXText - 1, devYText, true);
         gc.drawText(scaleText, devXText + 1, devYText, true);
         gc.drawText(scaleText, devXText, devYText - 1, true);
         gc.drawText(scaleText, devXText, devYText + 1, true);

         gc.setForeground(SYS_COLOR_BLACK);
         gc.drawText(scaleText, devXText, devYText, true);
      }
      borderColor.dispose();
   }

   private void paint_Debug_GeoGrid(final GC gc) {

      final double geoGridPixelSizeX = _geoGridPixelSizeX;
      final double geoGridPixelSizeY = _geoGridPixelSizeY;

      double geoGridPixelSizeXAdjusted = geoGridPixelSizeX;
      double geoGridPixelSizeYAdjusted = geoGridPixelSizeY;

      boolean isAdjusted = false;

      /*
       * Adjust grid size when it's too small
       */
      while (geoGridPixelSizeXAdjusted < 20) {

         geoGridPixelSizeXAdjusted *= 2;
         geoGridPixelSizeYAdjusted *= 2;

         isAdjusted = true;
      }

      final int vpWidth = _worldPixelTopLeftViewport.width;
      final int vpHeight = _worldPixelTopLeftViewport.height;

      int numX = (int) (vpWidth / geoGridPixelSizeXAdjusted);
      int numY = (int) (vpHeight / geoGridPixelSizeYAdjusted);

      // this can occure by high zoom level
      if (numX < 1) {
         numX = 1;
      }
      if (numY < 1) {
         numY = 1;
      }

      gc.setLineWidth(1);
      gc.setLineStyle(SWT.LINE_SOLID);

      // show different color when adjusted
      if (isAdjusted) {
         gc.setForeground(_display.getSystemColor(SWT.COLOR_RED));
      } else {
         gc.setForeground(_display.getSystemColor(SWT.COLOR_BLUE));
      }

      final Point devGeoGrid = offline_GetDevGeoGridPosition(_worldPixelTopLeftViewport.x, _worldPixelTopLeftViewport.y);
      final int topLeftX = devGeoGrid.x;
      final int topLeftY = devGeoGrid.y;

      // draw vertical lines, draw more lines as necessary otherwise sometimes they are not visible
      for (int indexX = -1; indexX < numX + 5; indexX++) {

         final int devX = (int) (topLeftX + indexX * geoGridPixelSizeXAdjusted);

         gc.drawLine(devX, 0, devX, vpHeight);
      }

      // draw horizontal lines
      for (int indexY = -1; indexY < numY + 5; indexY++) {

         final int devY = (int) (topLeftY + indexY * geoGridPixelSizeYAdjusted);

         gc.drawLine(0, devY, vpWidth, devY);
      }
   }

   private void paint_GridBox_10_Hovered(final GC gc, final MapGridBoxItem gridBoxItem) {

      gc.setLineWidth(2);

      /*
       * show info in the top/right corner that selection for the offline area is activ
       */
      paint_GridBox_70_Info_LatLon(gc, gridBoxItem);

      // check if mouse button is hit which sets the start position
      if ((gridBoxItem.dev_Start == null)) {

         final Point topLeft = paint_GridBox_50_Rectangle(gc,
               gridBoxItem.worldMouse_Move,
               gridBoxItem.worldMouse_Move,
               false);

         paint_GridBox_80_Info_Box(gc, gridBoxItem, topLeft);

         return;
      }

      final int dev_Start_X = gridBoxItem.dev_Start.x;
      final int dev_Start_Y = gridBoxItem.dev_Start.y;
      final int dev_End_X = gridBoxItem.dev_End.x;
      final int dev_End_Y = gridBoxItem.dev_End.y;

      final int dev_X1;
      final int dev_Y1;

      final int dev_Width;
      final int dev_Height;

      if (dev_Start_X < dev_End_X) {

         dev_X1 = dev_Start_X;
         dev_Width = dev_End_X - dev_Start_X;

      } else {

         dev_X1 = dev_End_X;
         dev_Width = dev_Start_X - dev_End_X;
      }

      if (dev_Start_Y < dev_End_Y) {

         dev_Y1 = dev_Start_Y;
         dev_Height = dev_End_Y - dev_Start_Y;

      } else {

         dev_Y1 = dev_End_Y;
         dev_Height = dev_Start_Y - dev_End_Y;
      }

      /*
       * Draw geo grid
       */

      final Point topLeft = paint_GridBox_50_Rectangle(gc,
            gridBoxItem.world_Start,
            gridBoxItem.world_End,
            false);

      paint_GridBox_80_Info_Box(gc, gridBoxItem, topLeft);

      gc.setLineStyle(SWT.LINE_SOLID);
      gc.setForeground(SYS_COLOR_BLACK);
      gc.drawRectangle(dev_X1, dev_Y1, dev_Width, dev_Height);

      gc.setLineStyle(SWT.LINE_SOLID);
      gc.setForeground(SYS_COLOR_WHITE);

      gc.setBackground(_display.getSystemColor(SWT.COLOR_DARK_YELLOW));
      gc.setAlpha(0x30);
      gc.fillRectangle(dev_X1 + 1, dev_Y1 + 1, dev_Width - 2, dev_Height - 2);
      gc.setAlpha(0xff);
   }

   private void paint_GridBox_20_Selected(final GC gc, final MapGridBoxItem gridBoxItem) {

      final Point topLeft = paint_GridBox_50_Rectangle(gc,
            gridBoxItem.world_Start,
            gridBoxItem.world_End,
            true);

      paint_GridBox_80_Info_Box(gc, gridBoxItem, topLeft);
   }

   /**
    * Paint a rectangle which shows a grid box
    *
    * @param gc
    * @param worldStart
    * @param worldEnd
    * @param isPaintLastGridSelection
    *           When <code>true</code>, the last selected grid is painted, otherwise the currently
    *           selecting grid
    * @return Returns top/left box position in the viewport
    */
   private Point paint_GridBox_50_Rectangle(final GC gc,
                                            final Point worldStart,
                                            final Point worldEnd,
                                            final boolean isPaintLastGridSelection) {

      final int worldStartX = worldStart.x;
      final int worldStartY = worldStart.y;
      final int worldEndX = worldEnd.x;
      final int worldEndY = worldEnd.y;

      // XY1: top/left
      final int world_X1 = Math.min(worldStartX, worldEndX);
      final int world_Y1 = Math.min(worldStartY, worldEndY);

      // XY2: bottom/right
      final int world_X2 = Math.max(worldStartX, worldEndX);
      final int world_Y2 = Math.max(worldStartY, worldEndY);

      final Point2D.Double worldPixel_1 = new Point2D.Double(world_X1, world_Y1);
      final Point2D.Double worldPixel_2 = new Point2D.Double(world_X2, world_Y2);

      final GeoPosition selectedPosition_Geo_1 = _mp.pixelToGeo(worldPixel_1, _mapZoomLevel);
      final GeoPosition selectedPosition_Geo_2 = _mp.pixelToGeo(worldPixel_2, _mapZoomLevel);

      final double geoLat1 = selectedPosition_Geo_1.latitude;
      final double geoLon1 = selectedPosition_Geo_1.longitude;

      final double geoLat2 = selectedPosition_Geo_2.latitude;
      final double geoLon2 = selectedPosition_Geo_2.longitude;

      // set lat/lon to a grid of 0.01°
      int geoGrid_Lat1_E2 = (int) (geoLat1 * 100);
      int geoGrid_Lon1_E2 = (int) (geoLon1 * 100);

      int geoGrid_Lat2_E2 = (int) (geoLat2 * 100);
      int geoGrid_Lon2_E2 = (int) (geoLon2 * 100);

      final Point devGeoGrid_1 = offline_GetDevGeoGridPosition(world_X1, world_Y1);
      final Point devGeoGrid_2 = offline_GetDevGeoGridPosition(world_X2, world_Y2);

      int devGrid_X1 = devGeoGrid_1.x;
      int devGrid_Y1 = devGeoGrid_1.y;

      int devGrid_X2 = devGeoGrid_2.x;
      int devGrid_Y2 = devGeoGrid_2.y;

      final int geoGridPixelSizeX = (int) _geoGridPixelSizeX;
      final int geoGridPixelSizeY = (int) _geoGridPixelSizeY;

      final int gridSize_E2 = 1;

      /**
       * Adjust lat/lon +/-, this algorithm is created with many many many try and error
       */
      if (geoLat1 > 0 && geoLon1 > 0 && geoLat2 > 0 && geoLon2 > 0) {

         // 1: + / +
         // 2: + / +

         //     |
         //     | xx
         // ---------
         //     |
         //     |

         devGrid_X2 += geoGridPixelSizeX;
         devGrid_Y2 += geoGridPixelSizeY;

         geoGrid_Lon2_E2 += gridSize_E2; // X1
         geoGrid_Lat1_E2 += gridSize_E2; // Y1

      } else if (geoLat1 > 0 && geoLon1 > 0 && geoLat2 < 0 && geoLon2 > 0) {

         // 1: + / +
         // 2: - / +

         //     |
         //     | xx
         // ------xx-
         //     | xx
         //     |

         devGrid_X2 += geoGridPixelSizeX;
         devGrid_Y2 += geoGridPixelSizeY * 2;

         geoGrid_Lon2_E2 += gridSize_E2; // X2

         geoGrid_Lat1_E2 += gridSize_E2; // Y1
         geoGrid_Lat2_E2 -= gridSize_E2; // Y2

      } else if (geoLat1 < 0 && geoLon1 > 0 && geoLat2 < 0 && geoLon2 > 0) {

         // 1: - / +
         // 2: - / +

         //     |
         //     |
         // ---------
         //     | xx
         //     |

         devGrid_X2 += geoGridPixelSizeX;
         devGrid_Y1 += geoGridPixelSizeY;
         devGrid_Y2 += geoGridPixelSizeY * 2;

         geoGrid_Lon2_E2 += gridSize_E2; // X2
         geoGrid_Lat2_E2 -= gridSize_E2; // Y2

      } else if (geoLat1 > 0 && geoLon1 < 0 && geoLat2 > 0 && geoLon2 < 0) {

         // 1: + / -
         // 2: + / -

         //     |
         //  xx |
         // ---------
         //     |
         //     |

         devGrid_X1 -= geoGridPixelSizeX;
         devGrid_Y2 += geoGridPixelSizeY;

         geoGrid_Lon1_E2 -= gridSize_E2; // X1
         geoGrid_Lat1_E2 += gridSize_E2; // Y1

      } else if (geoLat1 > 0 && geoLon1 < 0 && geoLat2 < 0 && geoLon2 < 0) {

         // 1: + / -
         // 2: - / -

         //     |
         //  xx |
         // -xx------
         //  xx |
         //     |

         devGrid_X1 -= geoGridPixelSizeX;
         devGrid_Y2 += geoGridPixelSizeY * 2;

         geoGrid_Lon1_E2 -= gridSize_E2; // X1
         geoGrid_Lat1_E2 += gridSize_E2; // Y1

         geoGrid_Lat2_E2 -= gridSize_E2; // Y2

      } else if (geoLat1 < 0 && geoLon1 < 0 && geoLat2 < 0 && geoLon2 < 0) {

         // 1: - / -
         // 2: - / -

         //     |
         //     |
         // ---------
         //  xx |
         //     |

         devGrid_X1 -= geoGridPixelSizeX;
         devGrid_Y1 += geoGridPixelSizeY;

         devGrid_Y2 += geoGridPixelSizeY * 2;

         geoGrid_Lon1_E2 -= gridSize_E2; // X1
         geoGrid_Lat2_E2 -= gridSize_E2; // Y2

      } else if (geoLat1 > 0 && geoLon1 < 0 && geoLat2 > 0 && geoLon2 > 0) {

         // 1: + / -
         // 2: + / +

         //     |
         //   xxxxx
         // ---------
         //     |
         //     |

         devGrid_X1 -= geoGridPixelSizeX;
         devGrid_Y2 += geoGridPixelSizeY;

         devGrid_X2 += geoGridPixelSizeX;

         geoGrid_Lon1_E2 -= gridSize_E2; // X1
         geoGrid_Lat1_E2 += gridSize_E2; // Y1

         geoGrid_Lon2_E2 += gridSize_E2; // X2

      } else if (geoLat1 < 0 && geoLon1 < 0 && geoLat2 < 0 && geoLon2 > 0) {

         // 1: - / -
         // 2: - / +

         //     |
         //     |
         // ---------
         //   xxxxx
         //     |

         devGrid_X1 -= geoGridPixelSizeX;
         devGrid_Y1 += geoGridPixelSizeY;

         devGrid_X2 += geoGridPixelSizeX;
         devGrid_Y2 += geoGridPixelSizeY * 2;

         geoGrid_Lon1_E2 -= gridSize_E2; // X1

         geoGrid_Lon2_E2 += gridSize_E2; // X2
         geoGrid_Lat2_E2 -= gridSize_E2; // Y2

      } else if (geoLat1 > 0 && geoLon1 < 0 && geoLat2 < 0 && geoLon2 > 0) {

         // 1: + / -
         // 2: - / +

         //     |
         //   xxxxx
         // --xxxxx--
         //   xxxxx
         //     |

         devGrid_X1 -= geoGridPixelSizeX;

         devGrid_X2 += geoGridPixelSizeX;
         devGrid_Y2 += geoGridPixelSizeY * 2;

         geoGrid_Lon1_E2 -= gridSize_E2; // X1
         geoGrid_Lat1_E2 += gridSize_E2; // Y1

         geoGrid_Lon2_E2 += gridSize_E2; // X2
         geoGrid_Lat2_E2 -= gridSize_E2; // Y2
      }

      // x: longitude
      // y: latitude

      _grid_SelectedPosition_Geo_1_E2 = new Point(geoGrid_Lon1_E2, geoGrid_Lat1_E2);
      _grid_SelectedPosition_Geo_2_E2 = new Point(geoGrid_Lon2_E2, geoGrid_Lat2_E2);

      int width = devGrid_X2 - devGrid_X1;
      int height = devGrid_Y2 - devGrid_Y1;

      // ensure it is always visible
      width = Math.max(1, width);
      height = Math.max(1, height);

      // draw geo grid
      final Color boxColor;
      if (isPaintLastGridSelection) {
         final RGB hoverRGB = Util.getStateRGB(TourGeoFilterManager.getState(),
               TourGeoFilterManager.STATE_RGB_GEO_PARTS_SELECTED,
               TourGeoFilterManager.STATE_RGB_GEO_PARTS_SELECTED_DEFAULT);

         boxColor = _colorCache.getColorRGB(hoverRGB);

      } else {

         final RGB hoverRGB = Util.getStateRGB(TourGeoFilterManager.getState(),
               TourGeoFilterManager.STATE_RGB_GEO_PARTS_HOVER,
               TourGeoFilterManager.STATE_RGB_GEO_PARTS_HOVER_DEFAULT);

         boxColor = _colorCache.getColorRGB(hoverRGB);
      }

      gc.setLineStyle(SWT.LINE_SOLID);
      gc.setLineWidth(1);

      // draw outline with selected color
      gc.setForeground(boxColor);
      gc.drawRectangle(devGrid_X1 + 1, devGrid_Y1 + 1, width - 2, height - 2);

      // draw dark outline to make it more visible
      gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
      gc.drawRectangle(devGrid_X1, devGrid_Y1, width, height);

      return new Point(devGrid_X1, devGrid_Y1);
   }

   /**
    * @param gc
    * @param gridBoxItem
    * @param numGridRectangle
    */
   private void paint_GridBox_70_Info_LatLon(final GC gc, final MapGridBoxItem gridBoxItem) {

      gc.setForeground(SYS_COLOR_BLACK);
      gc.setBackground(SYS_COLOR_YELLOW);

      final StringBuilder sb = new StringBuilder();

      if (gridBoxItem.dev_Start != null) {

         // display selected area

         final Point2D.Double worldPixel_Start = new Point2D.Double(gridBoxItem.world_Start.x, gridBoxItem.world_Start.y);
         final Point2D.Double worldPixel_End = new Point2D.Double(gridBoxItem.world_End.x, gridBoxItem.world_End.y);

         final GeoPosition geoStart = _mp.pixelToGeo(worldPixel_Start, _mapZoomLevel);
         final GeoPosition geoEnd = _mp.pixelToGeo(worldPixel_End, _mapZoomLevel);

         sb.append(String.format(" %s / %s  ...  %s / %s", //$NON-NLS-1$
               _nfLatLon.format(geoStart.latitude),
               _nfLatLon.format(geoStart.longitude),
               _nfLatLon.format(geoEnd.latitude),
               _nfLatLon.format(geoEnd.longitude)));

      } else {

         // display mouse move geo position

         final Point worldMouse_Move = gridBoxItem.worldMouse_Move;
         final Point2D.Double worldPixel_Mouse = new Point2D.Double(worldMouse_Move.x, worldMouse_Move.y);

         final GeoPosition mouseGeo = _mp.pixelToGeo(worldPixel_Mouse, _mapZoomLevel);

         sb.append(String.format(" %s / %s", //$NON-NLS-1$
               _nfLatLon.format(mouseGeo.latitude),
               _nfLatLon.format(mouseGeo.longitude)));
      }

      gc.drawString(sb.toString(), 0, 0);
   }

   private void paint_GridBox_80_Info_Box(final GC gc, final MapGridBoxItem gridBoxItem, final Point topLeft) {

      final String infoText = gridBoxItem.gridBoxText;

      if (infoText == null) {
         return;
      }

      final Point textSize = gc.stringExtent(infoText);

      final int devX = topLeft.x;
      final int devY = topLeft.y - textSize.y - 5;

      gc.setForeground(SYS_COLOR_WHITE);
      gc.drawString(infoText, devX + 1, devY + 1, true);
      gc.drawString(infoText, devX - 1, devY + 1, true);
      gc.drawString(infoText, devX + 1, devY - 1, true);
      gc.drawString(infoText, devX - 1, devY - 1, true);

      gc.setForeground(SYS_COLOR_BLACK);
      gc.drawString(infoText, devX, devY, true);

      gridBoxItem.gridBoxText_Position = new Point(devX, devY);
   }

   private void paint_OfflineArea(final GC gc) {

      gc.setLineWidth(2);

      /*
       * Draw previous area box
       */
//
// DISABLED: Wrong location when map is relocated
//
//      if (_offline_PreviousOfflineArea != null
//
//            // show only at the same zoomlevel
//            && _offline_PreviousOfflineArea_MapZoomLevel == _mapZoomLevel) {
//
//         gc.setLineStyle(SWT.LINE_SOLID);
//         gc.setForeground(SYS_COLOR_WHITE);
//         gc.drawRectangle(_offline_PreviousOfflineArea);
//
//         final int devX = _offline_PreviousOfflineArea.x;
//         final int devY = _offline_PreviousOfflineArea.y;
//         gc.setForeground(SYS_COLOR_GRAY);
//         gc.drawRectangle(//
//               devX + 1,
//               devY + 1,
//               _offline_PreviousOfflineArea.width - 2,
//               _offline_PreviousOfflineArea.height - 2);
//
//         /*
//          * draw text marker
//          */
//         gc.setForeground(SYS_COLOR_BLACK);
//         gc.setBackground(SYS_COLOR_WHITE);
//         final Point textExtend = gc.textExtent(Messages.Offline_Area_Label_OldAreaMarker);
//         int devYMarker = devY - textExtend.y;
//         devYMarker = devYMarker < 0 ? 0 : devYMarker;
//         gc.drawText(Messages.Offline_Area_Label_OldAreaMarker, devX, devYMarker);
//      }

      /*
       * show info in the top/right corner that selection for the offline area is activ
       */
      if (_offline_IsSelectingOfflineArea) {
         paint_OfflineArea_10_Info(gc);
      }

      // check if mouse button is hit which sets the start position
      if ((_offline_DevMouse_Start == null) || (_offline_WorldMouse_Move == null)) {
         return;
      }

      /*
       * Draw tile box for tiles which are selected within the area box
       */
      final int devTileStartX = _offline_DevTileStart.x;
      final int devTileStartY = _offline_DevTileStart.y;
      final int devTileEndX = _offline_DevTileEnd.x;
      final int devTileEndY = _offline_DevTileEnd.y;

      final int devTileStartX2 = Math.min(devTileStartX, devTileEndX);
      final int devTileStartY2 = Math.min(devTileStartY, devTileEndY);
      final int devTileEndX2 = Math.max(devTileStartX, devTileEndX);
      final int devTileEndY2 = Math.max(devTileStartY, devTileEndY);

      for (int devX = devTileStartX2; devX <= devTileEndX2; devX += _tilePixelSize) {
         for (int devY = devTileStartY2; devY <= devTileEndY2; devY += _tilePixelSize) {

            gc.setLineStyle(SWT.LINE_SOLID);
            gc.setForeground(_display.getSystemColor(SWT.COLOR_YELLOW));
            gc.drawRectangle(devX, devY, _tilePixelSize, _tilePixelSize);

            gc.setLineStyle(SWT.LINE_DASH);
            gc.setForeground(SYS_COLOR_DARK_GRAY);
            gc.drawRectangle(devX, devY, _tilePixelSize, _tilePixelSize);
         }
      }

      final int devArea_Start_X = _offline_DevMouse_Start.x;
      final int devArea_Start_Y = _offline_DevMouse_Start.y;
      final int devArea_End_X = _offline_DevMouse_End.x;
      final int devArea_End_Y = _offline_DevMouse_End.y;

      final int devArea_X1;
      final int devArea_Y1;

      final int devArea_Width;
      final int devArea_Height;

      if (devArea_Start_X < devArea_End_X) {

         devArea_X1 = devArea_Start_X;
         devArea_Width = devArea_End_X - devArea_Start_X;

      } else {

         devArea_X1 = devArea_End_X;
         devArea_Width = devArea_Start_X - devArea_End_X;
      }

      if (devArea_Start_Y < devArea_End_Y) {

         devArea_Y1 = devArea_Start_Y;
         devArea_Height = devArea_End_Y - devArea_Start_Y;

      } else {

         devArea_Y1 = devArea_End_Y;
         devArea_Height = devArea_Start_Y - devArea_End_Y;
      }

      /*
       * Draw selected area box
       */
      gc.setLineStyle(SWT.LINE_SOLID);
      gc.setForeground(SYS_COLOR_BLACK);
      gc.drawRectangle(devArea_X1, devArea_Y1, devArea_Width, devArea_Height);

      gc.setLineStyle(SWT.LINE_SOLID);
      gc.setForeground(SYS_COLOR_WHITE);

      gc.setBackground(_display.getSystemColor(SWT.COLOR_DARK_YELLOW));
      gc.setAlpha(0x30);
      gc.fillRectangle(devArea_X1 + 1, devArea_Y1 + 1, devArea_Width - 2, devArea_Height - 2);
      gc.setAlpha(0xff);

      /*
       * Draw text marker
       */
      final Point textExtend = gc.textExtent(Messages.Offline_Area_Label_AreaMarker);
      int devYMarker = devArea_Y1 - textExtend.y;
      devYMarker = devYMarker < 0 ? 0 : devYMarker;

      gc.setForeground(SYS_COLOR_BLACK);
      gc.setBackground(SYS_COLOR_WHITE);
      gc.drawText(Messages.Offline_Area_Label_AreaMarker, devArea_X1, devYMarker);
   }

   private void paint_OfflineArea_10_Info(final GC gc) {

      gc.setForeground(SYS_COLOR_BLACK);
      gc.setBackground(SYS_COLOR_YELLOW);

      final StringBuilder sb = new StringBuilder();
      sb.append(UI.SPACE + Messages.Offline_Area_Label_SelectInfo);

      if (_offline_DevMouse_Start != null) {

         // display offline area geo position

         final Point2D.Double worldPixel_Start = new Point2D.Double(_offline_WorldMouse_Start.x, _offline_WorldMouse_Start.y);
         final Point2D.Double worldPixel_End = new Point2D.Double(_offline_WorldMouse_End.x, _offline_WorldMouse_End.y);

         final GeoPosition geoStart = _mp.pixelToGeo(worldPixel_Start, _mapZoomLevel);
         final GeoPosition geoEnd = _mp.pixelToGeo(worldPixel_End, _mapZoomLevel);

         sb.append(String.format("   %s / %s  ...  %s / %s", //$NON-NLS-1$
               _nfLatLon.format(geoStart.latitude),
               _nfLatLon.format(geoStart.longitude),
               _nfLatLon.format(geoEnd.latitude),
               _nfLatLon.format(geoEnd.longitude)));

      } else {

         // display mouse move geo position

         if (_offline_WorldMouse_Move != null) {

            final Point2D.Double worldPixel_Mouse = new Point2D.Double(_offline_WorldMouse_Move.x, _offline_WorldMouse_Move.y);

            final GeoPosition mouseGeo = _mp.pixelToGeo(worldPixel_Mouse, _mapZoomLevel);

            sb.append(String.format("   %s / %s", //$NON-NLS-1$
                  _nfLatLon.format(mouseGeo.latitude),
                  _nfLatLon.format(mouseGeo.longitude)));
         }
      }

      gc.drawText(sb.toString(), 0, 0);
   }

   /**
    * Define and start the overlay thread
    */
   private void paintOverlay_0_SetupThread() {

      _overlayImageCache = new OverlayImageCache();

      _overlayThread = new Thread("PaintOverlayImages") { //$NON-NLS-1$
         @Override
         public void run() {

            while (!isInterrupted()) {

               try {

                  Thread.sleep(20);

                  if (_isRunningDrawOverlay) {
                     continue;
                  }

                  // overlay drawing is not running

                  final long currentTime = System.currentTimeMillis();

                  if (currentTime > _nextOverlayRedrawTime + 50) {
                     if (_tileOverlayPaintQueue.size() > 0) {

                        // create overlay images
                        paintOverlay_10_RunThread();
                     }
                  }

               } catch (final InterruptedException e) {
                  interrupt();
               } catch (final Exception e) {
                  e.printStackTrace();
               }
            }
         }
      };

      _overlayThread.setDaemon(true);
      _overlayThread.start();
   }

   private void paintOverlay_10_RunThread() {

      final int currentRunnableCounter = _overlayRunnableCounter.incrementAndGet();

      if (isDisposed()) {
         return;
      }

      final Runnable uiOverlayRunnable = new Runnable() {

         final int __runnableCounter = currentRunnableCounter;

         @Override
         public void run() {

            if (isDisposed()) {
               return;
            }

            // check if a newer runnable is available
            if (__runnableCounter != _overlayRunnableCounter.get()) {
               return;
            }

            _isRunningDrawOverlay = true;

            try {

               paintOverlay_20_Tiles();

            } catch (final Exception e) {
               e.printStackTrace();
            } finally {
               _isRunningDrawOverlay = false;
            }
         }
      };

      _display.asyncExec(uiOverlayRunnable);
   }

   private void paintOverlay_20_Tiles() {

      BusyIndicator.showWhile(_display, new Runnable() {
         @Override
         public void run() {

            Tile tile;

            checkImageTemplate9Parts();

            final long startTime = System.currentTimeMillis();

            while ((tile = _tileOverlayPaintQueue.poll()) != null) {

               // skip tiles from another zoom level
               if (tile.getZoom() == _mapZoomLevel) {

                  // set state that this tile is checked
                  tile.setOverlayTourStatus(OverlayTourState.TILE_IS_CHECKED);

                  /*
                   * check if a tour, marker or photo is within the current tile
                   */
                  boolean isPaintingNeeded = false;

                  for (final MapPainter overlayPainter : _overlays) {

                     isPaintingNeeded = overlayPainter.isPaintingNeeded(Map.this, tile);

                     if (isPaintingNeeded) {
                        break;
                     }
                  }

                  if (isPaintingNeeded == false) {

                     // set tile state
                     tile.setOverlayImageState(OverlayImageState.NO_IMAGE);

                     continue;
                  }

                  // paint overlay
                  if (_isTourPaintMethodEnhanced) {
                     paintOverlay_30_PaintTileEnhanced(tile);
                  } else {
                     paintOverlay_22_PaintTileBasic(tile);
                  }

                  // allow to display painted overlays
                  final long paintTime = System.currentTimeMillis();
                  if (paintTime > startTime + 100) {
                     break;
                  }

               } else {

                  // tile has a different zoom level, ignore this tile
                  tile.setOverlayTourStatus(OverlayTourState.TILE_IS_NOT_CHECKED);
               }
            }
         }
      });
   }

   /**
    * Paint the tour in basic mode
    *
    * @param tile
    */
   private void paintOverlay_22_PaintTileBasic(final Tile tile) {

      boolean isOverlayPainted = false;

      // create 1 part image/gc
      final ImageData transparentImageData = de.byteholder.geoclipse.map.UI.createTransparentImageData(
            _tilePixelSize);

      final Image overlayImage = new Image(_display, transparentImageData);
      final GC gc1Part = new GC(overlayImage);

      /*
       * Ubuntu 12.04 fails, when background is not filled, it draws a black background
       */
      gc1Part.setBackground(_transparentColor);
      gc1Part.fillRectangle(overlayImage.getBounds());

      {
         // paint all overlays for the current tile
         for (final MapPainter overlayPainter : _overlays) {

            final boolean isPainted = overlayPainter.doPaint(gc1Part, Map.this, tile, 1);

            isOverlayPainted = isOverlayPainted || isPainted;
         }
      }
      gc1Part.dispose();

      if (isOverlayPainted) {

         // overlay is painted

         final String overlayKey = getOverlayKey(tile, 0, 0, _mp.getProjection().getId());

         tile.setOverlayImage(overlayImage);
         _overlayImageCache.add(overlayKey, overlayImage);

         // set tile state
         tile.setOverlayImageState(OverlayImageState.TILE_HAS_CONTENT);
         tile.incrementOverlayContent();

         paint();

      } else {

         // image is not needed
         overlayImage.dispose();

         // set tile state
         tile.setOverlayImageState(OverlayImageState.NO_IMAGE);
      }
   }

   /**
    * Paints the overlay into the overlay image which is bigger than the tile image so that the
    * drawings are not clipped at the tile border. The overlay image is afterwards splitted into
    * parts which are drawn into the tile images
    *
    * @param tile
    */
   private void paintOverlay_30_PaintTileEnhanced(final Tile tile) {

      final int parts = 3;

      boolean isOverlayPainted = false;

      {
         // clear 9 part image
         _9PartGC.setBackground(_transparentColor);
         _9PartGC.fillRectangle(_9PartImage.getBounds());

         // paint all overlays for the current tile
         for (final MapPainter overlayPainter : _overlays) {

            final boolean isPainted = overlayPainter.doPaint(_9PartGC, Map.this, tile, parts);

            isOverlayPainted = isOverlayPainted || isPainted;
         }

         if (isOverlayPainted) {

            tile.setOverlayImageState(OverlayImageState.IMAGE_IS_CREATED);

            final ImageData imageData9Parts = _9PartImage.getImageData();

            /**
             * overlay image is created, split overlay image into 3*3 part images where the center
             * image is the requested tile image
             */

            final Job splitJob = new Job("SplitOverlayImages() " + _jobCounterSplitImages++) { //$NON-NLS-1$

               @Override
               public boolean belongsTo(final Object family) {

                  return family == _splitJobFamily;
               }

               @Override
               protected IStatus run(final IProgressMonitor monitor) {

                  paintOverlay_31_SplitParts(tile, imageData9Parts);

                  if (_isCancelSplitJobs) {
                     return Status.CANCEL_STATUS;
                  }

                  paint();

                  return Status.OK_STATUS;
               }

            };

            splitJob.setSystem(true);
            splitJob.schedule();

         } else {

            if (tile.getOverlayContent() == 0) {
               tile.setOverlayImageState(OverlayImageState.NO_IMAGE);
            } else {
               tile.setOverlayImageState(OverlayImageState.TILE_HAS_PART_CONTENT);
            }
         }
      }
   }

   /**
    * Splits the overlay tile image into 3*3 parts, the center image is the tile overlay image
    *
    * <pre>
    *
    * y,x
    *
    * 0,0      0,1      0,2
    * 1,0      1,1      1,2
    * 2,0      2,1      2,2
    * </pre>
    *
    * @param tile
    * @param imageData9Parts
    */
   private void paintOverlay_31_SplitParts(final Tile tile, final ImageData imageData9Parts) {

      final TileCache tileCache = MP.getTileCache();

      final String projectionId = _mp.getProjection().getId();
      final int partSize = _tilePixelSize;

      final int tileZoom = tile.getZoom();
      final int tileX = tile.getX();
      final int tileY = tile.getY();
      final int maxTiles = (int) Math.pow(2, tileZoom);

      final ArrayList<Rectangle> partMarkerBounds = tile.getPartMarkerBounds(tileZoom);
      final boolean isMarkerBounds = partMarkerBounds != null && partMarkerBounds.size() > 0;

      for (int yIndex = 0; yIndex < 3; yIndex++) {
         for (int xIndex = 0; xIndex < 3; xIndex++) {

            if (_isCancelSplitJobs) {
               return;
            }

            // check if the tile is within the map border
            if (((tileX - xIndex < -1) //
                  || (tileX + xIndex > maxTiles + 1))
                  || ((tileY - yIndex < -1) //
                        || (tileY + yIndex > maxTiles + 1))) {
               continue;
            }

            final int devXPart = partSize * xIndex;
            final int devYPart = partSize * yIndex;

            // check if there are any drawings in the current part
            if (isPartImageDataModified(imageData9Parts, devXPart, devYPart, partSize) == false) {

               // there are no drawings within the current part
               continue;
            }

            /*
             * there are drawings in the current part
             */

            final int xOffset = xIndex - 1;
            final int yOffset = yIndex - 1;

            final String partImageKey = getOverlayKey(tile, xOffset, yOffset, projectionId);
            final boolean isCenterPart = (xIndex == 1) && (yIndex == 1);
            Image tileOverlayImage = null;

            if (isCenterPart) {

               // center part, this is the part which belongs to the current tile

               // get image data resources
               final ImageDataResources idResources = tile.getOverlayImageDataResources();

               // draw center part into the tile image data
               idResources.drawTileImageData(//
                     imageData9Parts,
                     devXPart,
                     devYPart,
                     partSize,
                     partSize);

               tileOverlayImage = tile.createOverlayImage(_display);

               // set tile state
               tile.setOverlayImageState(OverlayImageState.TILE_HAS_CONTENT);
               tile.incrementOverlayContent();

            } else {

               // neighbor part

               final String neighborTileCacheKey = tile.getTileKey(xOffset, yOffset, projectionId);

               boolean isNeighborTileCreated = false;

               Tile neighborTile = tileCache.get(neighborTileCacheKey);
               if (neighborTile == null) {

                  // create neighbor tile

                  isNeighborTileCreated = true;

                  final MP mp = tile.getMP();

                  neighborTile = new Tile(
                        mp,
                        tile.getZoom(), //
                        tile.getX() + xOffset,
                        tile.getY() + yOffset,
                        null);

                  neighborTile.setBoundingBoxEPSG4326();
                  mp.doPostCreation(neighborTile);

                  tileCache.add(neighborTileCacheKey, neighborTile);
               }

               // get neighbor image data resources
               final ImageDataResources neighborIDResources = neighborTile.getOverlayImageDataResources();

               // draw part image into the neighbor image
               neighborIDResources.drawNeighborImageData(//
                     imageData9Parts,
                     devXPart,
                     devYPart,
                     partSize,
                     partSize);

               if (isNeighborTileCreated == false) {

                  /*
                   * create overlay image only when the neighbor tile was not created so that the
                   * normal tile image loading happens
                   */

                  tileOverlayImage = neighborTile.createOverlayImage(_display);
               }

               // set state for the neighbor tile
               neighborTile.incrementOverlayContent();

               final OverlayImageState partImageState = neighborTile.getOverlayImageState();
               if (partImageState != OverlayImageState.TILE_HAS_CONTENT) {
                  neighborTile.setOverlayImageState(OverlayImageState.TILE_HAS_PART_CONTENT);
               }
            }

            if (isMarkerBounds) {
               for (final Rectangle markerBound : partMarkerBounds) {

                  /*
                   * set marker bounds into the tile
                   */

                  // adjust tile center pos to top/left pos for a 9 part image
                  final int devXMarker = markerBound.x;// + partSize;
                  final int devYMarker = markerBound.y;// + partSize;

                  final int markerWidth = markerBound.width;
                  final int markerHeight = markerBound.height;

                  // check if marker intersects the part
                  final boolean isMarkerInTile = //
                        (devXMarker < devXPart + partSize)
                              && (devYMarker < devYPart + partSize)
                              && (devXMarker + markerWidth > devXPart)
                              && (devYMarker + markerHeight > devYPart);

                  if (isMarkerInTile) {
                     tile.addMarkerBounds(devXMarker, devYMarker, markerWidth, markerHeight, tileZoom);
                  }
               }
            }

            /*
             * keep image in the cache that not too much image resources are created and that all
             * images can be disposed
             */
            if (tileOverlayImage != null) {
               _overlayImageCache.add(partImageKey, tileOverlayImage);
            }
         }
      }
   }

   private void paintTile(final GC gcMapImage,
                          final int tilePositionX,
                          final int tilePositionY,
                          final Rectangle devTileViewport) {

      // get tile from the map provider, this also starts the loading of the tile image
      final Tile tile = _mp.getTile(tilePositionX, tilePositionY, _mapZoomLevel);

      final Image tileImage = tile.getCheckedMapImage();
      if (tileImage != null) {

         // tile map image is available and valid

         gcMapImage.drawImage(tileImage, devTileViewport.x, devTileViewport.y);

      } else {
         paintTile10_Image(gcMapImage, tile, devTileViewport);
      }

      if (_isDrawOverlays) {

         gcMapImage.setAlpha(_overlayAlpha);

         paintTile20_Overlay(gcMapImage, tile, devTileViewport);

         gcMapImage.setAlpha(0xff);
      }

      if (_isShowDebug_TileInfo || _isShowDebug_TileBorder) {
         paintTile30_Info(gcMapImage, tile, devTileViewport);
      }
   }

   /**
    * draw the tile map image
    */
   private void paintTile10_Image(final GC gcMapImage, final Tile tile, final Rectangle devTileViewport) {

      if (tile.isLoadingError()) {

         // map image contains an error, it could not be loaded

         final Image errorImage = _mp.getErrorImage();
         final Rectangle imageBounds = errorImage.getBounds();

         gcMapImage.setBackground(SYS_COLOR_GRAY);
         gcMapImage.fillRectangle(devTileViewport.x, devTileViewport.y, imageBounds.width, imageBounds.height);

         paintTileInfoError(gcMapImage, devTileViewport, tile);

         return;
      }

      if (tile.isOfflineError()) {

         //map image could not be loaded from offline file

         gcMapImage.drawImage(_mp.getErrorImage(), devTileViewport.x, devTileViewport.y);

         paintTileInfoError(gcMapImage, devTileViewport, tile);

         return;
      }

      /*
       * the tile image is not yet loaded, register an observer that handles redrawing when the tile
       * image is available. Tile image loading is started, when the tile is retrieved from the tile
       * factory which is done in drawTile()
       */
      tile.addObserver(_tileImageLoadObserver);

      if (_isLiveView == false) {

         // check if the offline image is available
         if (tile.isOfflimeImageAvailable()) {

            /*
             * offline image is available but not yet loaded into the cache (this is done in the
             * background ), draw nothing to prevent flickering of the loading... message
             */

         } else {

            /*
             * offline image is not availabe, show loading... message
             */

            gcMapImage.drawImage(_mp.getLoadingImage(), devTileViewport.x, devTileViewport.y);

//            gc.setForeground(_display.getSystemColor(SWT.COLOR_BLACK));
//            gc.drawString(Messages.geoclipse_extensions_loading, devTileViewport.x, devTileViewport.y, true);
         }
      }
   }

   /**
    * Draw overlay image when it's available or request the image
    *
    * @param gcMapImage
    * @param tile
    * @param devTileViewport
    *           Position of the tile
    */
   private void paintTile20_Overlay(final GC gcMapImage, final Tile tile, final Rectangle devTileViewport) {

      /*
       * Priority 1: draw overlay image
       */
      final OverlayImageState imageState = tile.getOverlayImageState();
      final int overlayContent = tile.getOverlayContent();

      if ((imageState == OverlayImageState.IMAGE_IS_CREATED)
            || ((imageState == OverlayImageState.NO_IMAGE) && (overlayContent == 0))) {

         // there is no image for the tile overlay or the image is currently being created
         return;
      }

      Image drawingImage = null;
      Image partOverlayImage = null;
      Image tileOverlayImage = null;

      if (overlayContent > 0) {

         // tile has overlay content, check if an image is available

         final String overlayKey = getOverlayKey(tile);

         // get overlay image from the cache
         drawingImage = partOverlayImage = _overlayImageCache.get(overlayKey);

         if (partOverlayImage == null) {

            /**
             * get image from the tile, it's possible that the part image is disposed but the tile
             * image is still available
             */
            tileOverlayImage = tile.getOverlayImage();
            if ((tileOverlayImage != null) && (tileOverlayImage.isDisposed() == false)) {
               drawingImage = tileOverlayImage;
            }
         }
      }

      // draw overlay image
      if ((drawingImage != null) && (drawingImage.isDisposed() == false)) {
         try {
            gcMapImage.drawImage(drawingImage, devTileViewport.x, devTileViewport.y);
         } catch (final Exception e) {

            /*
             * ignore, it's still possible that the image is disposed when the images are changing
             * very often and the cache is small
             */
            partOverlayImage = null;
         }
      }

      /*
       * Priority 2: check state for the overlay
       */
      final OverlayTourState tourState = tile.getOverlayTourStatus();

      if (tourState == OverlayTourState.TILE_IS_CHECKED) {

         // it is possible that the image is disposed but the tile has overlay content

         /**
          * check if the tile overlay image (not the surrounding part images) is available, when not
          * the image must be created
          */
         if (tileOverlayImage == null) {
            tileOverlayImage = tile.getOverlayImage();
         }

         if ((tileOverlayImage == null) || tileOverlayImage.isDisposed()) {

            // overlay image is NOT available

            // check if tile has overlay content
            if (overlayContent == 0) {

               /**
                * tile has no overlay content -> set state that the drawing of the overlay is as
                * fast as possible
                */
               tile.setOverlayImageState(OverlayImageState.NO_IMAGE);

            } else {

               // tile has overlay content but no image, this is not good, create image again

               if (imageState == OverlayImageState.TILE_HAS_CONTENT) {

                  // overlay content is created from this tile

                  queueOverlayPainting(tile);

                  return;

               } else {

                  if (partOverlayImage == null) {

                     // tile is checked and has no image but the content is created from a part tile

                     // this method will do an endless loop and is disabled
                     // -> this problem is currently not solved
                     //   queueOverlayPainting(tile);
                     return;
                  }
               }
            }

         } else {

            // overlay image is available

            if (imageState == OverlayImageState.NOT_SET) {

               if (overlayContent == 0) {
                  tile.setOverlayImageState(OverlayImageState.NO_IMAGE);
               } else {
                  // something is wrong
                  queueOverlayPainting(tile);
               }
            }
         }

         // tile tours are checked and the state is OK
         return;
      }

      // when tile is queued, nothing more to do, just wait
      if (tourState == OverlayTourState.IS_QUEUED) {
         return;
      }

      // overlay tour status is not yet checked, overlayTourStatus == OverlayTourStatus.NOT_CHECKED
      queueOverlayPainting(tile);
   }

   /**
    * @param gc
    * @param tile
    * @param devTileViewport
    */
   private void paintTile30_Info(final GC gc, final Tile tile, final Rectangle devTileViewport) {

      final ConcurrentHashMap<String, Tile> childrenWithErrors = tile.getChildrenWithErrors();

      if (tile.isLoadingError()
            || tile.isOfflineError()
            || ((childrenWithErrors != null) && (childrenWithErrors.size() > 0))) {

         paintTileInfoError(gc, devTileViewport, tile);

         return;
      }

      if (_isShowDebug_TileBorder) {

         // draw tile border
         gc.setForeground(SYS_COLOR_DARK_GRAY);
         gc.drawRectangle(devTileViewport.x, devTileViewport.y, _tilePixelSize, _tilePixelSize);
      }

      if (_isShowDebug_TileInfo) {

         // draw tile info
         gc.setForeground(SYS_COLOR_WHITE);
         gc.setBackground(_display.getSystemColor(SWT.COLOR_DARK_BLUE));

         final int leftMargin = 10;

         paintTileInfoLatLon(gc, tile, devTileViewport, 10, leftMargin);
         paintTileInfoPosition(gc, devTileViewport, tile, 50, leftMargin);

         // draw tile image path/url
         final StringBuilder sb = new StringBuilder();

         paintTileInfoPath(tile, sb);

         _textWrapper.printText(
               gc,
               sb.toString(),
               devTileViewport.x + leftMargin,
               devTileViewport.y + 80,
               devTileViewport.width - 20);
      }
   }

   private void paintTileInfoError(final GC gc, final Rectangle devTileViewport, final Tile tile) {

      // draw tile border
      gc.setForeground(SYS_COLOR_DARK_GRAY);
      gc.drawRectangle(devTileViewport.x, devTileViewport.y, _tilePixelSize, _tilePixelSize);

      gc.setForeground(SYS_COLOR_WHITE);
      gc.setBackground(_display.getSystemColor(SWT.COLOR_DARK_MAGENTA));

      final int leftMargin = 10;

      paintTileInfoLatLon(gc, tile, devTileViewport, 10, leftMargin);
      paintTileInfoPosition(gc, devTileViewport, tile, 50, leftMargin);

      // display loading error
      final StringBuilder sb = new StringBuilder();

      final String loadingError = tile.getLoadingError();
      if (loadingError != null) {
         sb.append(loadingError);
         sb.append(UI.NEW_LINE);
         sb.append(UI.NEW_LINE);
      }

      final ConcurrentHashMap<String, Tile> childrenLoadingError = tile.getChildrenWithErrors();
      if ((childrenLoadingError != null) && (childrenLoadingError.size() > 0)) {

         for (final Tile childTile : childrenLoadingError.values()) {
            sb.append(childTile.getLoadingError());
            sb.append(UI.NEW_LINE);
            sb.append(UI.NEW_LINE);
         }
      }

      paintTileInfoPath(tile, sb);

      final ArrayList<Tile> tileChildren = tile.getChildren();
      if (tileChildren != null) {
         for (final Tile childTile : tileChildren) {
            paintTileInfoPath(childTile, sb);
         }
      }

      _textWrapper.printText(
            gc,
            sb.toString(),
            devTileViewport.x + leftMargin,
            devTileViewport.y + 80,
            devTileViewport.width - 20);
   }

   private void paintTileInfoLatLon(final GC gc,
                                    final Tile tile,
                                    final Rectangle devTileViewport,
                                    final int topMargin,
                                    final int leftMargin) {

      final StringBuilder sb = new StringBuilder();

      final int devLineHeight = gc.getFontMetrics().getHeight();
      final int dev2ndColumn = 80;

      final BoundingBoxEPSG4326 bbox = tile.getBbox();

      sb.setLength(0);

      // lat - bottom
      sb.append(Messages.TileInfo_Position_Latitude);
      sb.append(_nfLatLon.format(bbox.bottom));
      gc.drawString(
            sb.toString(), //
            devTileViewport.x + leftMargin,
            devTileViewport.y + topMargin);

      // lat - top
      gc.drawString(
            _nfLatLon.format(bbox.top), //
            devTileViewport.x + leftMargin + dev2ndColumn,
            devTileViewport.y + topMargin);

      sb.setLength(0);

      // lon - left
      sb.append(Messages.TileInfo_Position_Longitude);
      sb.append(_nfLatLon.format(bbox.left));
      gc.drawString(
            sb.toString(), //
            devTileViewport.x + leftMargin,
            devTileViewport.y + topMargin + devLineHeight);

      // lon - right
      gc.drawString(
            _nfLatLon.format(bbox.right), //
            devTileViewport.x + leftMargin + dev2ndColumn,
            devTileViewport.y + topMargin + devLineHeight);
   }

   /**
    * !!! Recursive !!!
    *
    * @param tile
    * @param sb
    */
   private void paintTileInfoPath(final Tile tile, final StringBuilder sb) {

      final String url = tile.getUrl();
      if (url != null) {
         sb.append(url);
         sb.append(UI.NEW_LINE);
         sb.append(UI.NEW_LINE);
      }

      final String offlinePath = tile.getOfflinePath();
      if (offlinePath != null) {
         sb.append(offlinePath);
         sb.append(UI.NEW_LINE);
         sb.append(UI.NEW_LINE);
      }

      final ArrayList<Tile> tileChildren = tile.getChildren();
      if (tileChildren != null) {
         for (final Tile childTile : tileChildren) {
            paintTileInfoPath(childTile, sb);
         }
      }
   }

   private void paintTileInfoPosition(final GC gc,
                                      final Rectangle devTileViewport,
                                      final Tile tile,
                                      final int topMargin,
                                      final int leftMargin) {

      final StringBuilder text = new StringBuilder()//
            .append(Messages.TileInfo_Position_Zoom)
            .append(tile.getZoom() + 1)
            .append(Messages.TileInfo_Position_X)
            .append(tile.getX())
            .append(Messages.TileInfo_Position_Y)
            .append(tile.getY());

      gc.drawString(text.toString(), devTileViewport.x + leftMargin, devTileViewport.y + topMargin);
   }

   /**
    * pan the map
    */
   private void panMap(final MouseEvent mouseEvent) {

      /*
       * set new map center
       */
      final Point movePosition = new Point(mouseEvent.x, mouseEvent.y);

      final int mapDiffX = movePosition.x - _mouseDownPosition.x;
      final int mapDiffY = movePosition.y - _mouseDownPosition.y;

      final double oldCenterX = _worldPixelMapCenter.getX();
      final double oldCenterY = _worldPixelMapCenter.getY();

      final double newCenterX = oldCenterX - mapDiffX;
      final double newCenterY = oldCenterY - mapDiffY;

      _mouseDownPosition = movePosition;
      _isMapPanned = true;

      // set new map center
      setMapCenterInWorldPixel(new Point2D.Double(newCenterX, newCenterY));
      updateViewPortData();

      paint();

      fireMapPositionEvent(false);
   }

   private boolean parsePOIText(String text) {

      try {
         text = URLDecoder.decode(text, "UTF-8"); //$NON-NLS-1$
      } catch (final UnsupportedEncodingException e) {
         StatusUtil.log(e);
      }

      // linux has 2 lines: 1: url, 2. text
      final String[] dndText = text.split(de.byteholder.geoclipse.map.UI.NEW_LINE);
      if (dndText.length == 0) {
         return false;
      }

      // parse wiki url
      final Matcher wikiUrlMatcher = _patternWikiUrl.matcher(dndText[0]);
      if (wikiUrlMatcher.matches()) {

         // osm url was found

         final String pageName = wikiUrlMatcher.group(1);
         final String position = wikiUrlMatcher.group(2);

         if (position != null) {

            double lat = 0;
            double lon = 0;
            String otherParams = null;

            //   match D;D

            final Matcher wikiPos1Matcher = _patternWikiPosition_D_D.matcher(position);
            if (wikiPos1Matcher.matches()) {

               final String latPosition = wikiPos1Matcher.group(1);
               final String lonPosition = wikiPos1Matcher.group(2);
               otherParams = wikiPos1Matcher.group(3);

               if (lonPosition != null) {
                  try {

                     lat = Double.parseDouble(latPosition);
                     lon = Double.parseDouble(lonPosition);

                  } catch (final NumberFormatException e) {
                     return false;
                  }
               }
            } else {

               //   match D_N_D_E

               final Matcher wikiPos20Matcher = _patternWikiPosition_D_N_D_E.matcher(position);
               if (wikiPos20Matcher.matches()) {

                  final String latDegree = wikiPos20Matcher.group(1);
                  final String latDirection = wikiPos20Matcher.group(2);

                  final String lonDegree = wikiPos20Matcher.group(3);
                  final String lonDirection = wikiPos20Matcher.group(4);

                  otherParams = wikiPos20Matcher.group(5);

                  if (lonDirection != null) {
                     try {

                        final double latDeg = Double.parseDouble(latDegree);
                        final double lonDeg = Double.parseDouble(lonDegree);

                        lat = latDeg * (latDirection.equals(DIRECTION_N) ? 1 : -1);
                        lon = lonDeg * (lonDirection.equals(DIRECTION_E) ? 1 : -1);

                     } catch (final NumberFormatException e) {
                        return false;
                     }
                  }

               } else {

                  // match D_M_N_D_M_E

                  final Matcher wikiPos21Matcher = _patternWikiPosition_D_M_N_D_M_E.matcher(position);
                  if (wikiPos21Matcher.matches()) {

                     final String latDegree = wikiPos21Matcher.group(1);
                     final String latMinutes = wikiPos21Matcher.group(2);
                     final String latDirection = wikiPos21Matcher.group(3);

                     final String lonDegree = wikiPos21Matcher.group(4);
                     final String lonMinutes = wikiPos21Matcher.group(5);
                     final String lonDirection = wikiPos21Matcher.group(6);

                     otherParams = wikiPos21Matcher.group(7);

                     if (lonDirection != null) {
                        try {

                           final double latDeg = Double.parseDouble(latDegree);
                           final double latMin = Double.parseDouble(latMinutes);

                           final double lonDeg = Double.parseDouble(lonDegree);
                           final double lonMin = Double.parseDouble(lonMinutes);

                           lat = (latDeg + (latMin / 60f)) * (latDirection.equals(DIRECTION_N) ? 1 : -1);
                           lon = (lonDeg + (lonMin / 60f)) * (lonDirection.equals(DIRECTION_E) ? 1 : -1);

                        } catch (final NumberFormatException e) {
                           return false;
                        }
                     }
                  } else {

                     // match D_M_S_N_D_M_S_E

                     final Matcher wikiPos22Matcher = _patternWikiPosition_D_M_S_N_D_M_S_E.matcher(position);
                     if (wikiPos22Matcher.matches()) {

                        final String latDegree = wikiPos22Matcher.group(1);
                        final String latMinutes = wikiPos22Matcher.group(2);
                        final String latSeconds = wikiPos22Matcher.group(3);
                        final String latDirection = wikiPos22Matcher.group(4);

                        final String lonDegree = wikiPos22Matcher.group(5);
                        final String lonMinutes = wikiPos22Matcher.group(6);
                        final String lonSeconds = wikiPos22Matcher.group(7);
                        final String lonDirection = wikiPos22Matcher.group(8);

                        otherParams = wikiPos22Matcher.group(9);

                        if (lonDirection != null) {
                           try {

                              final double latDeg = Double.parseDouble(latDegree);
                              final double latMin = Double.parseDouble(latMinutes);
                              final double latSec = Double.parseDouble(latSeconds);

                              final double lonDeg = Double.parseDouble(lonDegree);
                              final double lonMin = Double.parseDouble(lonMinutes);
                              final double lonSec = Double.parseDouble(lonSeconds);

                              lat = (latDeg + (latMin / 60f) + (latSec / 3600f))
                                    * (latDirection.equals(DIRECTION_N) ? 1 : -1);

                              lon = (lonDeg + (lonMin / 60f) + (lonSec / 3600f))
                                    * (lonDirection.equals(DIRECTION_E) ? 1 : -1);

                           } catch (final NumberFormatException e) {
                              return false;
                           }
                        }
                     } else {
                        return false;
                     }
                  }
               }
            }

            // set default zoom level
            int zoom = 10;

            // get zoom level from parameter values
            if (otherParams != null) {

//                        String dim = null;
               String type = null;

               final String[] allKeyValues = _patternWikiParamter.split(otherParams);

               for (final String keyValue : allKeyValues) {

                  final String[] splittedKeyValue = _patternWikiKeyValue.split(keyValue);

                  if (splittedKeyValue.length > 1) {

                     if (splittedKeyValue[0].startsWith(WIKI_PARAMETER_TYPE)) {
                        type = splittedKeyValue[1];
//                              } else if (splittedKeyValue[0].startsWith(WIKI_PARAMETER_DIM)) {
//                                 dim = splittedKeyValue[1];
                     }
                  }
               }

               /*
                * !!! disabled because the zoom level is not correct !!!
                */
//                        if (dim != null) {
//                           final int scale = Integer.parseInt(dim);
//                           zoom = (int) (18 - (Math.round(Math.log(scale) - Math.log(1693)))) - 1;//, [2, 18];
//                        } else

               if (type != null) {

// source: https://wiki.toolserver.org/view/GeoHack
//
// type:                                   ratio           m / pixel   {scale}    {mmscale}   {span}   {altitude}   {zoom}   {osmzoom}
//
// country, satellite                         1 : 10,000,000    3528       10000000    10000000    10.0    1430       1       5
// state                                  1 : 3,000,000    1058       3000000    4000000    3.0    429       3       7
// adm1st                                  1 : 1,000,000    353       1000000    1000000    1.0    143       4       9
// adm2nd (default)                         1 : 300,000    106       300000       200000       0.3    42          5       11
// adm3rd, city, mountain, isle, river, waterbody    1 : 100,000    35.3       100000       100000       0.1    14          6       12
// event, forest, glacier                      1 : 50,000       17.6       50000       50000       0.05    7          7       13
// airport                                  1 : 30,000       10.6       30000       25000       0.03    4          7       14
// edu, pass, landmark, railwaystation             1 : 10,000       3.53       10000       10000       0.01    1          8       15

                  if (type.equals("country") //             //$NON-NLS-1$
                        || type.equals("satellite")) { //   //$NON-NLS-1$
                     zoom = 5 - 1;
                  } else if (type.equals("state")) { //      //$NON-NLS-1$
                     zoom = 7 - 1;
                  } else if (type.equals("adm1st")) { //      //$NON-NLS-1$
                     zoom = 9 - 1;
                  } else if (type.equals("adm2nd")) { //      //$NON-NLS-1$
                     zoom = 11 - 1;
                  } else if (type.equals("adm3rd") //         //$NON-NLS-1$
                        || type.equals("city") //         //$NON-NLS-1$
                        || type.equals("mountain") //      //$NON-NLS-1$
                        || type.equals("isle") //         //$NON-NLS-1$
                        || type.equals("river") //         //$NON-NLS-1$
                        || type.equals("waterbody")) { //   //$NON-NLS-1$
                     zoom = 12 - 1;
                  } else if (type.equals("event")//         //$NON-NLS-1$
                        || type.equals("forest") //       //$NON-NLS-1$
                        || type.equals("glacier")) { //      //$NON-NLS-1$
                     zoom = 13 - 1;
                  } else if (type.equals("airport")) { //      //$NON-NLS-1$
                     zoom = 14 - 1;
                  } else if (type.equals("edu") //         //$NON-NLS-1$
                        || type.equals("pass") //         //$NON-NLS-1$
                        || type.equals("landmark") //      //$NON-NLS-1$
                        || type.equals("railwaystation")) { //$NON-NLS-1$
                     zoom = 15 - 1;
                  }
               }

               // hide previous tooltip
               setPoiVisible(false);

               final GeoPosition poiGeoPosition = new GeoPosition(lat, lon);
               final String poiText = pageName.replace('_', ' ');

               final PoiToolTip poi = getPoi();
               poi.geoPosition = poiGeoPosition;
               poi.setText(poiText);

               setZoom(zoom);
               setMapCenter(poiGeoPosition);

               _isPoiVisible = true;

               firePOIEvent(poiGeoPosition, poiText);

               return true;
            }
         }
      }

      return false;
   }

   /**
    * Set tile in the overlay painting queue
    *
    * @param tile
    */
   public void queueOverlayPainting(final Tile tile) {

      tile.setOverlayTourStatus(OverlayTourState.IS_QUEUED);
      tile.setOverlayImageState(OverlayImageState.NOT_SET);

      _tileOverlayPaintQueue.add(tile);
   }

   private void recenterMap(final int xDiff, final int yDiff) {

      if (xDiff == 0 && yDiff == 0) {
         // nothing to do
         return;
      }

      final Rectangle bounds = _worldPixelTopLeftViewport;

      final double newCenterX = bounds.x + bounds.width / 2.0 + xDiff;
      final double newCenterY = bounds.y + bounds.height / 2.0 + yDiff;

      final Point2D.Double pixelCenter = new Point2D.Double(newCenterX, newCenterY);

      setMapCenterInWorldPixel(pixelCenter);
      updateViewPortData();

      paint();

      fireMapPositionEvent(false);
   }

   /**
    * Re-centers the map to have the current address location be at the center of the map,
    * accounting for the map's width and height.
    *
    * @see getAddressLocation
    */
   public void recenterToAddressLocation() {

      final java.awt.Point worldPixel = _mp.geoToPixel(getAddressLocation(), _mapZoomLevel);

      setMapCenterInWorldPixel(worldPixel);

      paint();
   }

   public void removeMousePositionListener(final IPositionListener listner) {
      _mousePositionListeners.remove(listner);
   }

   /**
    * Removes a map overlay.
    *
    * @return the current map overlay
    */
   public void removeOverlayPainter(final MapPainter overlay) {

      _overlays.remove(overlay);

      paint();
   }

   /**
    * Reload the map by discarding all cached tiles and entries in the loading queue
    */
   public synchronized void resetAll() {

      _mp.resetAll(false);

      paint();
   }

   /**
    * Set map dimming level for the current map factory, this will dimm the map images
    *
    * @param mapDimLevel
    * @param dimColor
    */
   public void setDimLevel(final int mapDimLevel, final RGB dimColor) {
      if (_mp != null) {
         _mp.setDimLevel(mapDimLevel, dimColor);
      }
   }

   public void setDirectPainter(final IDirectPainter directPainter) {
      _directMapPainter = directPainter;
   }

   public void setIsZoomWithMousePosition(final boolean isZoomWithMousePosition) {
      _isZoomWithMousePosition = isZoomWithMousePosition;
   }

   /**
    * Set the legend for the map, the legend image will be disposed when the map is disposed,
    *
    * @param legend
    *           Legend for the map or <code>null</code> to disable the legend
    */
   public void setLegend(final MapLegend legend) {

      if ((legend == null) && (_mapLegend != null)) {
         // dispose legend image
         disposeResource(_mapLegend.getImage());
      }

      _mapLegend = legend;
   }

   /**
    * When set to <code>false</code>, a loading image is displayed when the tile image is not in the
    * cache. When set to <code>true</code> a loading... image is not displayed which can confuse the
    * user because the map is not displaying the current state.
    *
    * @param isLiveView
    */
   public void setLiveView(final boolean isLiveView) {
      _isLiveView = isLiveView;
   }

   /**
    * Set the center of the map to a geo position (with lat/long) and redraw the map.
    *
    * @param geoPosition
    *           Center position in lat/lon
    */
   public synchronized void setMapCenter(final GeoPosition geoPosition) {

      final java.awt.Point newMapCenter = _mp.geoToPixel(geoPosition, _mapZoomLevel);

      if (Thread.currentThread() == _displayThread) {

         setMapCenterInWorldPixel(newMapCenter);

      } else {

         // current thread is not the display thread

         _display.syncExec(new Runnable() {
            @Override
            public void run() {
               if (!isDisposed()) {
                  setMapCenterInWorldPixel(newMapCenter);
               }
            }
         });
      }

      updateViewPortData();
      updateTourToolTip();

      stopOldPainting();

      paint();
   }

   /**
    * Sets the center of the map {@link #_worldPixelMapCenter} in world pixel coordinates with the
    * current zoomlevel
    *
    * @param newWorldPixelCenter
    */
   private void setMapCenterInWorldPixel(final Point2D newWorldPixelCenter) {

      _worldPixelMapCenter = checkWorldPixel(newWorldPixelCenter);

      fireMousePosition();
   }

   /**
    * Set map context menu provider
    *
    * @param mapContextProvider
    */
   public void setMapContextProvider(final IMapContextProvider mapContextProvider) {
      _mapContextProvider = mapContextProvider;
   }

   /**
    * Center the map within the geo positions.
    *
    * @param geoTourPositions
    * @param isAdjustZoomLevel
    * @param requestedZoomLevelAdjustment
    */
   public void setMapPosition(final Set<GeoPosition> geoTourPositions,
                              final boolean isAdjustZoomLevel,
                              final int requestedZoomLevelAdjustment) {

      Rectangle wpTourRect;
      Rectangle wpMapRect;

      GeoPosition geoTourCenter;

      java.awt.Point wpTourCenter;
      java.awt.geom.Point2D.Double wpMapCenter;

      // keep current zoomlevel
      final int currentZoomLevel = _mapZoomLevel;

      final int minZoomLevel = _mp.getMinimumZoomLevel();
      final int maximumZoomLevel = _mp.getMaximumZoomLevel();

      int zoom = maximumZoomLevel;

      /**
       * Adjust tour zoom level to the requested zoom level, this is used that the tour is more
       * visible and not painted at the map border
       */
      final int zoomLevelDiff = isAdjustZoomLevel ? requestedZoomLevelAdjustment : 0;
      int tourZoomLevel = zoom - zoomLevelDiff;
      tourZoomLevel = tourZoomLevel > maximumZoomLevel ? maximumZoomLevel : tourZoomLevel;

      wpTourRect = getWorldPixelFromGeoPositions(geoTourPositions, tourZoomLevel);

      // get tour center in world pixel for the max zoom level
      wpTourCenter = new java.awt.Point(//
            wpTourRect.x + wpTourRect.width / 2,
            wpTourRect.y + wpTourRect.height / 2);

      // set tour geo center in the center of the tour rectangle
      geoTourCenter = _mp.pixelToGeo(wpTourCenter, zoom);

      wpMapCenter = checkWorldPixel(_mp.geoToPixel(geoTourCenter, zoom), zoom);
      wpMapRect = getWorldPixelTopLeftViewport(wpMapCenter);

      // use an offset that the slider are not at the map border and almost not visible
      final int offset = 30;

      // zoom out until the tour is smaller than the map viewport
      while ((wpTourRect.width + offset > wpMapRect.width) //
            || (wpTourRect.height + offset > wpMapRect.height)) {

         // check zoom level
         if (zoom - 1 < minZoomLevel) {
            // this should not occure -> a tour should not be larger than the earth
            break;
         }

         // zoom out
         zoom--;

         tourZoomLevel = zoom - zoomLevelDiff;
         tourZoomLevel = tourZoomLevel > maximumZoomLevel ? maximumZoomLevel : tourZoomLevel;

         wpTourRect = getWorldPixelFromGeoPositions(geoTourPositions, tourZoomLevel);

         wpMapCenter = checkWorldPixel(_mp.geoToPixel(geoTourCenter, zoom), zoom);
         wpMapRect = getWorldPixelTopLeftViewport(wpMapCenter);
      }

      if (zoom != currentZoomLevel) {

         // set new zoomlevel ONLY when it was modified -> this will dispose old overlay images !!!

         setZoom(zoom);
      }

      // update map with with new map center position
      {
         // zoom position is the same as previous !!!
         // _mapZoomLevel == _mapZoomLevel

         // set new map center
         _worldPixelMapCenter = wpMapCenter;

         updateViewPortData();
      }

      fireMapInfoEvent();
   }

   /**
    * Sets the map provider for the map and redraws the map
    *
    * @param mp
    *           new map provider
    */
   public void setMapProvider(final MP mp) {

      GeoPosition center = null;
      int zoom = 0;
      boolean refresh = false;

      if (_mp != null) {

         // stop downloading images for the old map provider
         _mp.resetAll(true);

         center = getMapGeoCenter();
         zoom = _mapZoomLevel;
         refresh = true;
      }

      _mp = mp;

//      // check if the map is initialized
//      if (_worldPixelViewport == null) {
//         onResize();
//      }

      /*
       * !!! initialize map by setting the zoom level which setups all important data !!!
       */
      if (refresh) {
         setZoom(zoom);
         setMapCenter(center);
      } else {
         setZoom(mp.getDefaultZoomLevel());
      }

      paint();
   }

   /**
    * Resets current tile factory and sets a new one. The new tile factory is displayed at the same
    * position as the previous tile factory
    *
    * @param mp
    */
   public synchronized void setMapProviderWithReset(final MP mp) {

      if (_mp != null) {
         // keep tiles with loading errors that they are not loaded again when the factory has not changed
         _mp.resetAll(_mp == mp);
      }

      _mp = mp;

      paint();
   }

   public void setMeasurementSystem(final float distanceUnitValue, final String distanceUnitLabel) {
      _distanceUnitValue = distanceUnitValue;
      _distanceUnitLabel = distanceUnitLabel;
   }

   /**
    * Set a key to uniquely identify overlays which is used to cache the overlays
    *
    * @param key
    */
   public void setOverlayKey(final String key) {
      _overlayKey = key;
   }

   /**
    * @param isRedrawEnabled
    *           Set <code>true</code> to enable map drawing (which is the default). When
    *           <code>false</code>, map drawing is disabled.
    *           <p>
    *           This feature can enable the drawing of the map very late that flickering of the map
    *           is prevented when the map is setup.
    */
   public void setPainting(final boolean isRedrawEnabled) {

      _isRedrawEnabled = isRedrawEnabled;

      if (isRedrawEnabled) {
         paint();
      }
   }

   public void setPoi(final GeoPosition poiGeoPosition, final int zoomLevel, final String poiText) {

      _isPoiVisible = true;

      final PoiToolTip poiToolTip = getPoi();
      poiToolTip.geoPosition = poiGeoPosition;
      poiToolTip.setText(poiText);

      setZoom(zoomLevel);

      _isPoiPositionInViewport = updatePoiImageDevPosition();

      if (_isPoiPositionInViewport == false) {

         // recenter map only when poi is not visible

         setMapCenter(poiGeoPosition);
      }

      /*
       * when poi is set, it is possible that the mouse is already over the poi -> update tooltip
       */
      final Point devMouse = this.toControl(getDisplay().getCursorLocation());
      final int devMouseX = devMouse.x;
      final int devMouseY = devMouse.y;

      // check if mouse is within the poi image
      if (_isPoiVisible
            && (devMouseX > _poiImageDevPosition.x)
            && (devMouseX < _poiImageDevPosition.x + _poiImageBounds.width)
            && (devMouseY > _poiImageDevPosition.y - _poiTTOffsetY - 5)
            && (devMouseY < _poiImageDevPosition.y + _poiImageBounds.height)) {

         showPoi();

      } else {
         setPoiVisible(false);
      }

      paint();
   }

   public void setPOI(final IToolTipProvider tourToolTipProvider, final TourWayPoint twp) {

      // display poi in the center of the map which make it also visible
      setMapCenter(twp.getPosition());

      if (tourToolTipProvider instanceof WayPointToolTipProvider) {

         final WayPointToolTipProvider wpToolTipProvider = (WayPointToolTipProvider) tourToolTipProvider;

         final HoveredAreaContext hoveredContext = wpToolTipProvider.getHoveredContext(
               _worldPixelTopLeftViewport.width / 2,
               _worldPixelTopLeftViewport.height / 2,
               _worldPixelTopLeftViewport,
               _mp,
               _mapZoomLevel,
               _tilePixelSize,
               _isTourPaintMethodEnhanced,
               twp);

         if (hoveredContext == null) {
            // this case should not happen
            return;
         }

         _hoveredAreaContext = hoveredContext;

         // update tool tip control
         _tourToolTip.setHoveredContext(_hoveredAreaContext);

         _tourToolTip.show(new Point(_hoveredAreaContext.hoveredTopLeftX, _hoveredAreaContext.hoveredTopLeftY));

         redraw();
      }
   }

   /**
    * Sets the visibility of the poi tooltip. Poi tooltip is visible when the tooltip is available
    * and the poi image is withing the map view port
    *
    * @param isVisible
    *           <code>false</code> will hide the tooltip
    */
   private void setPoiVisible(final boolean isVisible) {

      if (_poiTT == null) {
         return;
      }

      if (isVisible) {

         if (_isPoiPositionInViewport = updatePoiImageDevPosition()) {

            final Point poiDisplayPosition = toDisplay(_poiImageDevPosition);

            _poiTT.show(
                  poiDisplayPosition.x,
                  poiDisplayPosition.y,
                  _poiImageBounds.width,
                  _poiImageBounds.height,
                  _poiTTOffsetY);
         }
      } else {
         _poiTT.hide();
      }
   }

   public void setShowDebugInfo(final boolean isShowDebugInfo, final boolean isShowTileBorder) {

      setShowDebugInfo(isShowDebugInfo, isShowTileBorder, false);
   }

   /**
    * Set if the tile borders should be drawn. Mainly used for debugging.
    *
    * @param isShowDebugInfo
    *           new value of this drawTileBorders
    * @param isShowTileBorder
    * @param isShowGeoGrid
    */
   public void setShowDebugInfo(final boolean isShowDebugInfo, final boolean isShowTileBorder, final boolean isShowGeoGrid) {

      _isShowDebug_TileInfo = isShowDebugInfo;
      _isShowDebug_TileBorder = isShowTileBorder;
      _isShowDebug_GeoGrid = isShowGeoGrid;

      paint();
   }

   /**
    * Legend will be drawn into the map when the visibility is <code>true</code>
    *
    * @param isVisibility
    */
   public void setShowLegend(final boolean isVisibility) {
      _isLegendVisible = isVisibility;
   }

   /**
    * set status if overlays are painted, a {@link #paint()} must be called to update the map
    *
    * @param showOverlays
    *           set <code>true</code> to see the overlays, <code>false</code> to hide the overlays
    */
   public void setShowOverlays(final boolean showOverlays) {
      _isDrawOverlays = showOverlays;
   }

   public void setShowPOI(final boolean isShowPOI) {

      _isPoiVisible = isShowPOI;

      paint();
   }

   public void setShowScale(final boolean isScaleVisible) {
      _isScaleVisible = isScaleVisible;
   }

   public void setTourPaintMethodEnhanced(final boolean isEnhanced) {

      _isTourPaintMethodEnhanced = isEnhanced;

      disposeOverlayImageCache();
   }

   public void setTourToolTip(final TourToolTip tourToolTip) {

      _tourToolTip = tourToolTip;

      tourToolTip.addHideListener(new IToolTipHideListener() {
         @Override
         public void afterHideToolTip(final Event event) {

            // hide hovered area
            _hoveredAreaContext = null;

            redraw();
         }
      });
   }

   /**
    * Set the zoom level for the map and centers the map to the previous center. The zoom level is
    * checked if the map provider supports the requested zoom level.
    * <p>
    * The map is initialize when this is not yet done be setting all internal data !!!
    *
    * @param newZoomLevel
    *           zoom level for the map, it is adjusted to the min/max zoom levels
    */
   public void setZoom(final int newZoomLevel) {

      if (_mp == null) {
         return;
      }

      final int oldZoomLevel = _mapZoomLevel;

      /*
       * check if the requested zoom level is within the bounds of the map provider
       */
      int adjustedZoomLevel = newZoomLevel;
      final int mpMinimumZoomLevel = _mp.getMinimumZoomLevel();
      final int mpMaximumZoomLevel = _mp.getMaximumZoomLevel();
      if (((newZoomLevel < mpMinimumZoomLevel) || (newZoomLevel > mpMaximumZoomLevel))) {
         adjustedZoomLevel = Math.max(newZoomLevel, mpMinimumZoomLevel);
         adjustedZoomLevel = Math.min(adjustedZoomLevel, mpMaximumZoomLevel);
      }

      // check if zoom level has changed
      if (oldZoomLevel == adjustedZoomLevel) {
         // this is disabled that a double click can set the center of the map
         // return;
      }

      if (oldZoomLevel != adjustedZoomLevel) {

         // zoomlevel has changed -> stop downloading images for the old zoom level
         _mp.resetAll(true);
      }

      final Dimension oldMapTileSize = _mp.getMapTileSize(oldZoomLevel);

      // check if map is initialized or zoom level has not changed
      Point2D wpCurrentMapCenter = _worldPixelMapCenter;
      if (wpCurrentMapCenter == null) {

         // setup map center

         initMap();

         wpCurrentMapCenter = _worldPixelMapCenter;
      }

      _mapZoomLevel = adjustedZoomLevel;

      // update values for the new zoom level !!!
      _mapTileSize = _mp.getMapTileSize(adjustedZoomLevel);

      final double relativeWidth = (double) _mapTileSize.width / oldMapTileSize.width;
      final double relativeHeight = (double) _mapTileSize.height / oldMapTileSize.height;

      Point2D.Double wpNewMapCenter;

      if (_isZoomWithMousePosition) {

         // set map center to the current mouse position

         final Rectangle wpViewPort = _worldPixelTopLeftViewport;

         wpCurrentMapCenter = new Point2D.Double(
               wpViewPort.x + _mouseMovePositionX_Last,
               wpViewPort.y + _mouseMovePositionY_Last);

      } else {

         // zoom behaviour until 18.5
      }

      wpNewMapCenter = new Point2D.Double(//
            wpCurrentMapCenter.getX() * relativeWidth,
            wpCurrentMapCenter.getY() * relativeHeight);

      setMapCenterInWorldPixel(wpNewMapCenter);

      updateViewPortData();

      updateTourToolTip();
//      updatePoiVisibility();
      updateGeoGridAfterZoom();

      // update zoom level in status bar
      fireMapInfoEvent();

      fireMapPositionEvent(true);
   }

   /**
    * @param geoFilter
    *           Show geo grid box, when <code>null</code> the selected grid box is set to hidden
    */
   public void showGeoGrid(final TourGeoFilterItem geoFilter) {

      if (geoFilter == null) {

         // hide geo grid

         _grid_GridBoxItem_Selected = null;

         redraw();

      } else {

         // show requested grid box

         final MapGridBoxItem mapGridBoxItem = geoFilter.mapGridBoxItem;

         if (mapGridBoxItem == null) {

            // This can occure when geofilter is loaded from xml file and not created in the map

            System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ()") //$NON-NLS-1$ //$NON-NLS-2$
                  + ("\t: mapGridBoxItem == null")); //$NON-NLS-1$
// TODO remove SYSTEM.OUT.PRINTLN

         } else {

            _grid_GridBoxItem_Selected = mapGridBoxItem;

            setZoom(geoFilter.mapZoomLevel);

            final Rectangle wpMapViewPort = getWorldPixelTopLeftViewport(_worldPixelMapCenter);

            // chck if gird box is already visible

            if (wpMapViewPort.contains(_grid_GridBoxItem_Selected.world_Start)
                  && wpMapViewPort.contains(_grid_GridBoxItem_Selected.world_End)) {

               // grid box is visile -> nothing to do

            } else {

               setMapCenter(new GeoPosition(geoFilter.mapGeoCenter.latitude, geoFilter.mapGeoCenter.longitude));
            }
         }
      }
   }

   private void showPoi() {

      if (_poiTT != null && _poiTT.isVisible()) {
         // poi is hidden
         return;
      }

      final PoiToolTip poiTT = getPoi();
      final Point poiDisplayPosition = this.toDisplay(_poiImageDevPosition);

      poiTT.show(//
            poiDisplayPosition.x,
            poiDisplayPosition.y,
            _poiImageBounds.width,
            _poiImageBounds.height,
            _poiTTOffsetY);
   }

   private void stopOldPainting() {

      // stop old painting jobs

      _isCancelSplitJobs = true;
      {
         Job.getJobManager().cancel(_splitJobFamily);
      }
      _isCancelSplitJobs = false;
   }

   /**
    * Update geo grid world position when zoom level changes
    */
   private void updateGeoGridAfterZoom() {

      if (_grid_GridBoxItem_Hovered != null && _grid_GridBoxItem_Hovered.geo_Start != null) {

         final java.awt.Point world_Start = _mp.geoToPixel(_grid_GridBoxItem_Hovered.geo_Start, _mapZoomLevel);
         final java.awt.Point world_End = _mp.geoToPixel(_grid_GridBoxItem_Hovered.geo_End, _mapZoomLevel);

         _grid_GridBoxItem_Hovered.world_Start = new Point(world_Start.x, world_Start.y);
         _grid_GridBoxItem_Hovered.world_End = new Point(world_End.x, world_End.y);
      }

      if (_grid_GridBoxItem_Selected != null && _grid_GridBoxItem_Selected.geo_Start != null) {

         final java.awt.Point worldLast_Start = _mp.geoToPixel(_grid_GridBoxItem_Selected.geo_Start, _mapZoomLevel);
         final java.awt.Point worldLast_End = _mp.geoToPixel(_grid_GridBoxItem_Selected.geo_End, _mapZoomLevel);

         _grid_GridBoxItem_Selected.world_Start = new Point(worldLast_Start.x, worldLast_Start.y);
         _grid_GridBoxItem_Selected.world_End = new Point(worldLast_End.x, worldLast_End.y);
      }
   }

   public void updateGraphColors() {

      _overlayAlpha = _prefStore.getBoolean(ITourbookPreferences.MAP2_LAYOUT_IS_TOUR_TRACK_OPACITY)

            // convert % to 0xff
            ? 0xff * _prefStore.getInt(ITourbookPreferences.MAP2_LAYOUT_TOUR_TRACK_OPACITY) / 100

            // no opacity
            : 0xff;
   }

   /**
    * @return Returns <code>true</code> when the POI image is visible and the position is set in
    *         {@link #_poiImageDevPosition}
    */
   private boolean updatePoiImageDevPosition() {

      final GeoPosition poiGeoPosition = getPoi().geoPosition;
      if (poiGeoPosition == null) {
         return false;
      }

      // get world position for the poi coordinates
      final java.awt.Point worldPoiPos = _mp.geoToPixel(poiGeoPosition, _mapZoomLevel);

      // adjust view port to contain the poi image
      final Rectangle adjustedViewport = new Rectangle(
            _worldPixelTopLeftViewport.x,
            _worldPixelTopLeftViewport.y,
            _worldPixelTopLeftViewport.width,
            _worldPixelTopLeftViewport.height);

      adjustedViewport.x -= _poiImageBounds.width;
      adjustedViewport.y -= _poiImageBounds.height;
      adjustedViewport.width += _poiImageBounds.width * 2;
      adjustedViewport.height += _poiImageBounds.height * 2;

      // check if poi is visible
      if (adjustedViewport.intersects(//
            worldPoiPos.x - _poiImageBounds.width / 2,
            worldPoiPos.y - _poiImageBounds.height,
            _poiImageBounds.width,
            _poiImageBounds.height)) {

         // convert world position into device position
         final int devPoiPosX = worldPoiPos.x - _worldPixelTopLeftViewport.x;
         final int devPoiPosY = worldPoiPos.y - _worldPixelTopLeftViewport.y;

         // get poi size
         final int poiImageWidth = _poiImageBounds.width;
         final int poiImageHeight = _poiImageBounds.height;

         _poiImageDevPosition.x = devPoiPosX - (poiImageWidth / 2);
         _poiImageDevPosition.y = devPoiPosY - poiImageHeight;

         return true;

      } else {

         return false;
      }
   }

   /**
    * show poi info when mouse is within the poi image
    */
   private void updatePoiVisibility() {

      boolean isVisible = false;

      if (_isPoiPositionInViewport = updatePoiImageDevPosition()) {

         final Point displayMouse = _display.getCursorLocation();
         final Point devMouse = this.toControl(displayMouse);

         final int devMouseX = devMouse.x;
         final int devMouseY = devMouse.y;

         if ((devMouseX > _poiImageDevPosition.x)
               && (devMouseX < _poiImageDevPosition.x + _poiImageBounds.width)
               && (devMouseY > _poiImageDevPosition.y - _poiTTOffsetY - 5)
               && (devMouseY < _poiImageDevPosition.y + _poiImageBounds.height)) {

            isVisible = true;
         }
      }

      setPoiVisible(isVisible);
   }

   /**
    * Update tour tool tip, this must be done after the view port data are updated
    */
   private void updateTourToolTip() {

      if (_mp != null && _tourToolTip != null && _tourToolTip.isActive()) {

         /*
          * redraw must be forced because the hovered area can be the same but can be at a different
          * location
          */
         updateTourToolTip_HoveredArea();

         _tourToolTip.update();
      }
   }

   /**
    * Set hovered area context for the current mouse position or <code>null</code> when a tour
    * hovered area (e.g. way point) is not hovered.
    *
    * @param isForceRedraw
    */
   private void updateTourToolTip_HoveredArea() {

      final HoveredAreaContext oldHoveredContext = _hoveredAreaContext;
      HoveredAreaContext newHoveredContext = null;

      final ArrayList<ITourToolTipProvider> toolTipProvider = _tourToolTip.getToolTipProvider();

      /*
       * check tour info tool tip provider as first
       */
      for (final IToolTipProvider tttProvider : toolTipProvider) {

         if (tttProvider instanceof IInfoToolTipProvider) {

            final HoveredAreaContext hoveredContext = ((IInfoToolTipProvider) tttProvider).getHoveredContext(
                  _mouseMovePositionX,
                  _mouseMovePositionY);

            if (hoveredContext != null) {
               newHoveredContext = hoveredContext;
               break;
            }
         }
      }

      /*
       * check map tool tip provider as second
       */
      if (newHoveredContext == null) {

         for (final IToolTipProvider tttProvider : toolTipProvider) {

            if (tttProvider instanceof IMapToolTipProvider) {

               final HoveredAreaContext hoveredContext = ((IMapToolTipProvider) tttProvider).getHoveredContext(
                     _mouseMovePositionX,
                     _mouseMovePositionY,
                     _worldPixelTopLeftViewport,
                     _mp,
                     _mapZoomLevel,
                     _tilePixelSize,
                     _isTourPaintMethodEnhanced,
                     null);

               if (hoveredContext != null) {
                  newHoveredContext = hoveredContext;
                  break;
               }
            }
         }
      }

      _hoveredAreaContext = newHoveredContext;

      // update tool tip control
      _tourToolTip.setHoveredContext(_hoveredAreaContext);

      if (_hoveredAreaContext != null) {
         redraw();
      }

      /*
       * hide hovered area, this must be done because when a tile do not contain a way point, the
       * hovered area can sill be displayed when another position is set with setMapCenter()
       */
      if (oldHoveredContext != null && _hoveredAreaContext == null) {

         // update tool tip because it has it's own mouse move listener for the map
         _tourToolTip.hideHoveredArea();

         redraw();
      }

   }

   /**
    * Sets all viewport data which are necessary to draw the map tiles in
    * {@link #paint_30_Tiles(GC)}. Some values are cached to optimize performance.
    * <p>
    * {@link #_worldPixelMapCenter} and {@link #_mapZoomLevel} are the base values for the other
    * viewport fields.
    */
   private void updateViewPortData() {

      if (_mp == null) {
         // the map has currently no map provider
         return;
      }

      _worldPixelTopLeftViewport = getWorldPixelTopLeftViewport(_worldPixelMapCenter);

      final int visiblePixelWidth = _worldPixelTopLeftViewport.width;
      final int visiblePixelHeight = _worldPixelTopLeftViewport.height;

      _devMapViewport = new Rectangle(0, 0, visiblePixelWidth, visiblePixelHeight);

      _mapTileSize = _mp.getMapTileSize(_mapZoomLevel);
      _tilePixelSize = _mp.getTileSize();

      // get the visible tiles which can be displayed in the viewport area
      final int numTileWidth = (int) Math.ceil((double) visiblePixelWidth / (double) _tilePixelSize);
      final int numTileHeight = (int) Math.ceil((double) visiblePixelHeight / (double) _tilePixelSize);

      /*
       * tileOffsetX and tileOffsetY are the x- and y-values for the offset of the visible screen to
       * the map's origin.
       */
      final int tileOffsetX = (int) Math.floor((double) _worldPixelTopLeftViewport.x / (double) _tilePixelSize);
      final int tileOffsetY = (int) Math.floor((double) _worldPixelTopLeftViewport.y / (double) _tilePixelSize);

      _tilePosMinX = tileOffsetX;
      _tilePosMinY = tileOffsetY;
      _tilePosMaxX = _tilePosMinX + numTileWidth;
      _tilePosMaxY = _tilePosMinY + numTileHeight;

      /*
       * Pixel size for one geo grid, 0.01 degree
       */
      final Point2D.Double viewportMapCenter_worldPixel = new Point2D.Double(
            _worldPixelTopLeftViewport.x + visiblePixelWidth / 2,
            _worldPixelTopLeftViewport.y + visiblePixelHeight / 2);

      final GeoPosition geoPos = _mp.pixelToGeo(viewportMapCenter_worldPixel, _mapZoomLevel);

      // round to 0.01
      final double geoLat1 = Math.round(geoPos.latitude * 100) / 100.0;
      final double geoLon1 = Math.round(geoPos.longitude * 100) / 100.0;
      final double geoLat2 = geoLat1 + 0.01;
      final double geoLon2 = geoLon1 + 0.01;

      final Point2D.Double worldGrid1 = _mp.geoToPixelDouble(new GeoPosition(geoLat1, geoLon1), _mapZoomLevel);
      final Point2D.Double worldGrid2 = _mp.geoToPixelDouble(new GeoPosition(geoLat2, geoLon2), _mapZoomLevel);

      _geoGridPixelSizeX = Math.abs(worldGrid2.x - worldGrid1.x);
      _geoGridPixelSizeY = Math.abs(worldGrid2.y - worldGrid1.y);

//      System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ()")
//            + ("\t_geoGridPixelSizeX: " + _geoGridPixelSizeX)
//            + ("\t_geoGridPixelSizeY: " + _geoGridPixelSizeY)
//            );
//// TODO remove SYSTEM.OUT.PRINTLN
   }

   public void zoomIn() {

      setZoom(_mapZoomLevel + 1);
      paint();
   }

   public void zoomOut() {

      setZoom(_mapZoomLevel - 1);
      paint();
   }
}
