/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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

import static org.eclipse.swt.events.ControlListener.controlResizedAdapter;
import static org.eclipse.swt.events.MouseTrackListener.mouseExitAdapter;

import de.byteholder.geoclipse.Messages;
import de.byteholder.geoclipse.map.event.IBreadcrumbListener;
import de.byteholder.geoclipse.map.event.IGeoPositionListener;
import de.byteholder.geoclipse.map.event.IHoveredTourListener;
import de.byteholder.geoclipse.map.event.IMapGridListener;
import de.byteholder.geoclipse.map.event.IMapInfoListener;
import de.byteholder.geoclipse.map.event.IMapPositionListener;
import de.byteholder.geoclipse.map.event.IMapSelectionListener;
import de.byteholder.geoclipse.map.event.IPOIListener;
import de.byteholder.geoclipse.map.event.ITourSelectionListener;
import de.byteholder.geoclipse.map.event.MapGeoPositionEvent;
import de.byteholder.geoclipse.map.event.MapHoveredTourEvent;
import de.byteholder.geoclipse.map.event.MapPOIEvent;
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
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.tourbook.Images;
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.DPITools;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorCacheSWT;
import net.tourbook.common.color.ThemeUtil;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.HoveredAreaContext;
import net.tourbook.common.util.IToolTipProvider;
import net.tourbook.common.util.ITourToolTipProvider;
import net.tourbook.common.util.MtMath;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.TourToolTip;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourWayPoint;
import net.tourbook.map2.view.Map2View;
import net.tourbook.map2.view.SelectionMapSelection;
import net.tourbook.map2.view.WayPointToolTipProvider;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.Map2_Appearance;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.filter.geo.TourGeoFilter;
import net.tourbook.tour.filter.geo.TourGeoFilter_Manager;
import net.tourbook.ui.IInfoToolTipProvider;
import net.tourbook.ui.IMapToolTipProvider;
import net.tourbook.ui.MTRectangle;

import org.apache.commons.lang3.text.WordUtils;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.dnd.URLTransfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

public class Map2 extends Canvas {

   private static final char            NL                                             = UI.NEW_LINE;

   private static final IDialogSettings _geoFilterState                                = TourGeoFilter_Manager.getState();

   /**
    * Min zoomlevels which the maps supports
    */
   public static final int              MAP_MIN_ZOOM_LEVEL                             = 0;

   /**
    * Max zoomlevels which the maps supports
    */
   public static final int              MAP_MAX_ZOOM_LEVEL                             = 22;

   /**
    * These zoom levels are displayed in the UI therefore they start with 1 instead of 0
    */
   public static final int              UI_MIN_ZOOM_LEVEL                              = MAP_MIN_ZOOM_LEVEL + 1;
   public static final int              UI_MAX_ZOOM_LEVEL                              = MAP_MAX_ZOOM_LEVEL + 1;

   public static final int              EXPANDED_HOVER_SIZE                            = 20;
   public static final int              EXPANDED_HOVER_SIZE2                           = EXPANDED_HOVER_SIZE / 2;

   private static final String          DIRECTION_E                                    = "E";                             //$NON-NLS-1$
   private static final String          DIRECTION_N                                    = "N";                             //$NON-NLS-1$

   private static final int             TEXT_MARGIN                                    = 6;

   private static final String          GEO_GRID_ACTION_UPDATE_GEO_LOCATION_ZOOM_LEVEL = "\uE003";                        //$NON-NLS-1$

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

   private static final String           PATTERN_WIKI_POSITION_D_D             = PATTERN_DOUBLE + ";"                                        //$NON-NLS-1$
         + PATTERN_DOUBLE
         + PATTERN_END;

   private static final String           PATTERN_WIKI_POSITION_D_N_D_E         = PATTERN_DOUBLE_SEP
         + PATTERN_DIRECTION_NS
         + PATTERN_DOUBLE_SEP
         + PATTERN_DIRECTION_WE
         + PATTERN_END;

   private static final String           PATTERN_WIKI_POSITION_D_M_N_D_M_E     = PATTERN_DOUBLE_SEP
         + PATTERN_DOUBLE_SEP
         + PATTERN_DIRECTION_NS
         + PATTERN_DOUBLE_SEP
         + PATTERN_DOUBLE_SEP
         + PATTERN_DIRECTION_WE
         + PATTERN_END;

   private static final String           PATTERN_WIKI_POSITION_D_M_S_N_D_M_S_E = PATTERN_DOUBLE_SEP
         + PATTERN_DOUBLE_SEP
         + PATTERN_DOUBLE_SEP
         + PATTERN_DIRECTION_NS
         + PATTERN_DOUBLE_SEP
         + PATTERN_DOUBLE_SEP
         + PATTERN_DOUBLE_SEP
         + PATTERN_DIRECTION_WE
         + PATTERN_END;

   private static final Pattern          _patternWikiUrl                       = Pattern.compile(PATTERN_WIKI_URL);
   private static final Pattern          _patternWikiPosition_D_D              = Pattern.compile(PATTERN_WIKI_POSITION_D_D);
   private static final Pattern          _patternWikiPosition_D_N_D_E          = Pattern.compile(PATTERN_WIKI_POSITION_D_N_D_E);
   private static final Pattern          _patternWikiPosition_D_M_N_D_M_E      = Pattern.compile(PATTERN_WIKI_POSITION_D_M_N_D_M_E);
   private static final Pattern          _patternWikiPosition_D_M_S_N_D_M_S_E  = Pattern.compile(PATTERN_WIKI_POSITION_D_M_S_N_D_M_S_E);
   private static final Pattern          _patternWikiParamter                  = Pattern.compile(PATTERN_SEPARATOR);
   private static final Pattern          _patternWikiKeyValue                  = Pattern.compile(PATTERN_WIKI_PARAMETER_KEY_VALUE_SEPARATOR);

   private static final IPreferenceStore _prefStore                            = TourbookPlugin.getPrefStore();

   private static final ColorCacheSWT    _colorCache                           = new ColorCacheSWT();

   // [181,208,208] is the color of water in the standard OSM material
   public static final RGB  OSM_BACKGROUND_RGB         = new RGB(181, 208, 208);
   private static final RGB MAP_DEFAULT_BACKGROUND_RGB = new RGB(0x40, 0x40, 0x40);

   private static RGB       MAP_TRANSPARENT_RGB;
   {
      MAP_TRANSPARENT_RGB = UI.IS_OSX
            ? new RGB(0xfe, 0xfe, 0xfe)
            : new RGB(0xfe, 0xfe, 0xfe);
   }

   /**
    * Map zoom level which is currently be used to display tiles. Normally a value between around 0
    * and 20.
    */
   private int                           _mapZoomLevel;

   private CenterMapBy                   _centerMapBy;

   /**
    * This image contains the map which is painted in the map viewport
    */
   private Image                         _mapImage;

   private Image                         _9PartImage;
   private GC                            _9PartGC;

   /**
    * Indicates whether or not to draw the borders between tiles. Defaults to false. not very nice
    * looking, very much a product of testing Consider whether this should really be a property or
    * not.
    */
   private boolean                       _isShowDebug_TileInfo;
   private boolean                       _isShowDebug_TileBorder;
   private boolean                       _isShowDebug_GeoGrid;

   /**
    * Factory used by this component to grab the tiles necessary for painting the map.
    */
   private MP                            _mp;

   /**
    * The position in latitude/longitude of the "address" being mapped. This is a special coordinate
    * that, when moved, will cause the map to be moved as well. It is separate from "center" in that
    * "center" tracks the current center (in pixels) of the view port whereas this will not change
    * when panning or zooming. Whenever the addressLocation is changed, however, the map will be
    * repositioned.
    */
   private GeoPosition                   _addressLocation;

   /**
    * The overlay to delegate to for painting the "foreground" of the map component. This would
    * include painting waypoints, day/night, etc. also receives mouse events.
    */
   private final List<Map2Painter>       _allMapPainter           = new ArrayList<>();

   private final TileImageLoaderCallback _tileImageLoaderCallback = new TileImageLoaderCallback_ForTileImages();

   private Cursor                        _currentCursor;
   private final Cursor                  _cursorCross;
   private final Cursor                  _cursorDefault;
   private final Cursor                  _cursorHand;
   private final Cursor                  _cursorPan;
   private final Cursor                  _cursorSearchTour;
   private final Cursor                  _cursorSearchTour_Scroll;
   private final Cursor                  _cursorSelect;

   private final AtomicInteger           _redrawMapCounter        = new AtomicInteger();
   private final AtomicInteger           _overlayRunnableCounter  = new AtomicInteger();

   private boolean                       _isLeftMouseButtonPressed;
   private boolean                       _isMapPanned;

   private Point                         _mouseDownPosition;
   private GeoPosition                   _mouseDown_ContextMenu_GeoPosition;
   private int                           _mouseMove_DevPosition_X = Integer.MIN_VALUE;
   private int                           _mouseMove_DevPosition_Y = Integer.MIN_VALUE;
   private int                           _mouseMove_DevPosition_X_Last;
   private int                           _mouseMove_DevPosition_Y_Last;
   private GeoPosition                   _mouseMove_GeoPosition;

   private Thread                        _overlayThread;

   private long                          _nextOverlayRedrawTime;

   private final NumberFormat            _nf0;
   private final NumberFormat            _nf1;
   private final NumberFormat            _nf2;
   private final NumberFormat            _nf3;
   private final NumberFormat            _nfLatLon;
   {
      _nf0 = NumberFormat.getNumberInstance();
      _nf1 = NumberFormat.getNumberInstance();
      _nf2 = NumberFormat.getNumberInstance();
      _nf3 = NumberFormat.getNumberInstance();

      _nf0.setMinimumFractionDigits(0);
      _nf0.setMaximumFractionDigits(0);
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
   private final TextWrapPainter                      _textWrapper               = new TextWrapPainter();

   /**
    * cache for overlay images
    */
   private OverlayImageCache                          _overlayImageCache;

   /**
    * This queue contains tiles which overlay image must be painted
    */
   private final ConcurrentLinkedQueue<Tile>          _tileOverlayPaintQueue     = new ConcurrentLinkedQueue<>();

   private boolean                                    _isRunningDrawOverlay;

   private String                                     _overlayKey;

   /**
    * This painter is called when the map is painted in the onPaint event
    */
   private IDirectPainter                             _directMapPainter;

   private final DirectPainterContext                 _directMapPainterContext   = new DirectPainterContext();

   /**
    * When <code>true</code> the overlays are painted
    */
   private boolean                                    _isDrawOverlays;

   /**
    * Contains a legend which is painted in the map
    */
   private MapLegend                                  _mapLegend;

   private boolean                                    _isLegendVisible;

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
   private Point2D                                    _worldPixel_MapCenter      = null;

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
   private Rectangle                                  _worldPixel_TopLeft_Viewport;
   /**
    * Size in device pixel where the map is displayed
    */
   private Rectangle                                  _devMapViewport;
   /**
    * Size of the map in tiles at the current zoom level {@link #_mapZoomLevel} (num tiles tall by
    * num tiles wide)
    */
   private Dimension                                  _mapTileSize;

   /**
    * Size of a tile in pixel (tile is quadratic)
    */
   private int                                        _tilePixelSize;

   /**
    * Size of a geo grid of 0.01 degree in pixel, this depends on the zoom level
    */
   private double                                     _devGridPixelSize_X;
   private double                                     _devGridPixelSize_Y;

   /**
    * Contains the client area of the map without trimmings, this rectangle has the width and height
    * of the map image
    */
   private Rectangle                                  _clientArea;

   private final ListenerList<IBreadcrumbListener>    _allBreadcrumbListener     = new ListenerList<>(ListenerList.IDENTITY);
   private final ListenerList<IHoveredTourListener>   _allHoveredTourListeners   = new ListenerList<>(ListenerList.IDENTITY);
   private final ListenerList<IMapGridListener>       _allMapGridListener        = new ListenerList<>(ListenerList.IDENTITY);
   private final ListenerList<IMapInfoListener>       _allMapInfoListener        = new ListenerList<>(ListenerList.IDENTITY);
   private final ListenerList<IMapPositionListener>   _allMapPositionListener    = new ListenerList<>(ListenerList.IDENTITY);
   private final ListenerList<IMapSelectionListener>  _allMapSelectionListener   = new ListenerList<>(ListenerList.IDENTITY);
   private final ListenerList<IGeoPositionListener>   _allMousePositionListeners = new ListenerList<>(ListenerList.IDENTITY);
   private final ListenerList<IPOIListener>           _allPOIListeners           = new ListenerList<>(ListenerList.IDENTITY);
   private final ListenerList<ITourSelectionListener> _allTourSelectionListener  = new ListenerList<>(ListenerList.IDENTITY);

   // measurement system
   private float       _distanceUnitValue = 1;
   private String      _distanceUnitLabel = UI.EMPTY_STRING;

   private boolean     _isScaleVisible;
   private boolean     _isShowTour;

   private final Color _transparentColor;
   private Color       _defaultBackgroundColor;

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
   private PoiToolTip       _poi_Tooltip;
   private final int        _poi_Tooltip_OffsetY               = 5;
   private TourToolTip      _tour_ToolTip;

   /**
    * Hovered/selected tour
    */
   private boolean          _isShowHoveredOrSelectedTour       = Map2View.STATE_IS_SHOW_HOVERED_SELECTED_TOUR_DEFAULT;

   private ArrayList<Point> _allHoveredDevPoints               = new ArrayList<>();
   private ArrayList<Long>  _allHoveredTourIds                 = new ArrayList<>();
   private IntArrayList     _allHoveredSerieIndices            = new IntArrayList();

   private long             _hovered_SelectedTourId            = -1;
   private int              _hovered_SelectedSerieIndex_Front  = -1;
   private int              _hovered_SelectedSerieIndex_Behind = -1;

   /**
    * When <code>true</code> then a tour can be selected, otherwise a trackpoint (including tour)
    * can be selected
    */
   private boolean          _hoveredSelectedTour_CanSelectTour;
   private int              _hoveredSelectedTour_Hovered_Opacity;
   private Color            _hoveredSelectedTour_Hovered_Color;
   private int              _hoveredSelectedTour_HoveredAndSelected_Opacity;
   private Color            _hoveredSelectedTour_HoveredAndSelected_Color;
   private int              _hoveredSelectedTour_Selected_Opacity;
   private Color            _hoveredSelectedTour_Selected_Color;

   /**
    * When <code>true</code> the loading... image is not displayed
    */
   private boolean          _isLiveView;

   private long             _lastMapDrawTime;

   /*
    * All painted tiles in the map are within these 4 tile positions
    */
   private int           _tilePos_MinX;
   private int           _tilePos_MaxX;
   private int           _tilePos_MinY;
   private int           _tilePos_MaxY;
   //
   private Tile[][]      _allPaintedTiles;
   //
   private final Display _display;
   private final Thread  _displayThread;
   //
   private int           _jobCounterSplitImages = 0;
   private Object        _splitJobFamily        = new Object();
   private boolean       _isCancelSplitJobs;

   /*
    * Download offline images
    */
   private boolean                   _offline_IsSelectingOfflineArea;
   private boolean                   _offline_IsOfflineSelectionStarted;
   private boolean                   _offline_IsPaintOfflineArea;

   private Point                     _offline_DevMouse_Start;
   private Point                     _offline_DevMouse_End;
   private Point                     _offline_DevTileStart;
   private Point                     _offline_DevTileEnd;

   private Point                     _offline_WorldMouse_Start;
   private Point                     _offline_WorldMouse_End;
   private Point                     _offline_WorldMouse_Move;

   private IMapContextProvider       _mapContextProvider;

   /**
    * Is <code>true</code> when the map context menu can be displayed
    */
   private boolean                   _isContextMenuEnabled      = true;

   private DropTarget                _dropTarget;

   private boolean                   _isRedrawEnabled           = true;

   private HoveredAreaContext        _hoveredAreaContext;

   private int                       _overlayAlpha              = 0xff;

   private MapGridData               _geoGrid_Data_Hovered;
   private MapGridData               _geoGrid_Data_Selected;
   private TourGeoFilter             _geoGrid_TourGeoFilter;

   private boolean                   _geoGrid_Action_IsHovered;
   private Rectangle                 _geoGrid_Action_Outline;
   private boolean                   _geoGrid_Label_IsHovered;
   private Rectangle                 _geoGrid_Label_Outline;

   private GeoPosition               _geoGrid_MapGeoCenter;
   private int                       _geoGrid_MapZoomLevel;
   private int[]                     _geoGrid_AutoScrollCounter = new int[1];

   private boolean                   _geoGrid_IsGridAutoScroll;

   private ActionManageOfflineImages _actionManageOfflineImages;

   /**
    * When <code>true</code> the tour is painted in the map in the enhanced mode otherwise in the
    * simple mode
    */
   private boolean                   _isTourPaintMethodEnhanced;
   private boolean                   _isShowTourPaintMethodEnhancedWarning;

   private boolean                   _isMapBackgroundDark;
   private boolean                   _isFastMapPainting;
   private boolean                   _isFastMapPainting_Active;
   private boolean                   _isInInverseKeyboardPanning;

   /**
    * Tour direction is displayed only when tour is hovered
    */
   private boolean                   _isDrawTourDirection;

   /**
    * When <code>true</code> then the tour directions are displayed always but this is valid only,
    * when just one tour is displayed
    */
   private boolean                   _isDrawTourDirection_Always;

   /*
    * Direction arrows
    */
   private int               _tourDirection_MarkerGap;
   private int               _tourDirection_LineWidth;
   private RGB               _tourDirection_RGB;
   private float             _tourDirection_SymbolSize;

   private int               _fastMapPainting_skippedValues;

   private MapTourBreadcrumb _tourBreadcrumb;
   private boolean           _isShowBreadcrumbs = Map2View.STATE_IS_SHOW_BREADCRUMBS_DEFAULT;

   private Font              _boldFont          = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

   private int               _prefOptions_BorderWidth;
   private boolean           _prefOptions_isCutOffLinesInPauses;
   private boolean           _prefOptions_IsDrawSquare;
   private int               _prefOptions_LineWidth;
   private List<Long>        _allTourIds;

   private static enum HoveredPoint_PaintMode {

      IS_HOVERED, //
      IS_SELECTED, //
      IS_HOVERED_AND_SELECTED, //
   }

   /**
    * This callback is called when a tile image was loaded and is set into the tile
    */
   final class TileImageLoaderCallback_ForTileImages implements TileImageLoaderCallback {

      @Override
      public void update(final Tile tile) {

         if (tile.getZoom() == _mapZoomLevel) {

            /*
             * Because we are not in the UI thread, we have to queue the call for redraw and
             * cannot do it directly.
             */
            paint();
         }
      }
   }

   /**
    * Create a new Map
    *
    * @param state
    */
   public Map2(final Composite parent, final int style, final IDialogSettings state) {

      super(parent, style | SWT.DOUBLE_BUFFERED);

      _display = getDisplay();
      _displayThread = _display.getThread();

      addAllListener();
      addDropTarget();

      createActions();
      createContextMenu();

      updateGraphColors();
      updateMapOptions();

      grid_UpdatePaintingStateData();

// SET_FORMATTING_OFF

      _cursorCross               = new Cursor(_display, SWT.CURSOR_CROSS);
      _cursorDefault             = new Cursor(_display, SWT.CURSOR_ARROW);
      _cursorHand                = new Cursor(_display, SWT.CURSOR_HAND);
      _cursorPan                 = new Cursor(_display, SWT.CURSOR_SIZEALL);

      _cursorSearchTour          = createCursorFromImage(Images.SearchTours_ByLocation);
      _cursorSearchTour_Scroll   = createCursorFromImage(Images.SearchTours_ByLocation_Scroll);
      _cursorSelect              = createCursorFromImage(Images.Cursor_Select);

// SET_FORMATTING_ON

      _transparentColor = new Color(MAP_TRANSPARENT_RGB);

      _poiImage = TourbookPlugin.getImageDescriptor(Images.POI_InMap).createImage();
      _poiImageBounds = _poiImage.getBounds();

      _tourBreadcrumb = new MapTourBreadcrumb(this);

      paint_Overlay_0_SetupThread();

      parent.getDisplay().asyncExec(() -> {

         // must be run async because dark theme colors could not yet be initialized

         _defaultBackgroundColor = UI.IS_DARK_THEME
               ? ThemeUtil.getDarkestBackgroundColor()
               : new Color(MAP_DEFAULT_BACKGROUND_RGB);
      });
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

      _offline_IsPaintOfflineArea = true;
      _offline_IsSelectingOfflineArea = true;

      _offline_DevMouse_Start = null;
      _offline_DevMouse_End = null;

      setCursorOptimized(_cursorCross);

      redraw();
      paint();
   }

   public void actionSearchTourByLocation(final Event event) {

      _geoGrid_Data_Hovered = new MapGridData();

      // auto open geo filter slideout
      final boolean isAutoOpenSlideout = Util.getStateBoolean(TourGeoFilter_Manager.getState(),
            TourGeoFilter_Manager.STATE_IS_AUTO_OPEN_SLIDEOUT,
            TourGeoFilter_Manager.STATE_IS_AUTO_OPEN_SLIDEOUT_DEFAULT);
      if (isAutoOpenSlideout) {
         TourGeoFilter_Manager.setGeoFilter_OpenSlideout(true, false);
      }

      grid_UpdatePaintingStateData();
      _isFastMapPainting_Active = true;

      final Point worldMousePosition = new Point(
            _worldPixel_TopLeft_Viewport.x + _mouseMove_DevPosition_X,
            _worldPixel_TopLeft_Viewport.y + _mouseMove_DevPosition_Y);

      _geoGrid_Data_Hovered.geo_MouseMove = _mp.pixelToGeo(
            new Point2D.Double(worldMousePosition.x, worldMousePosition.y),
            _mapZoomLevel);

      setCursorOptimized(_cursorSearchTour);

      redraw();
      paint();
   }

   private void addAllListener() {

      addPaintListener(this::onPaint);
      addDisposeListener(this::onDispose);

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

      addMouseTrackListener(mouseExitAdapter(mouseEvent -> onMouse_Exit()));
      addMouseMoveListener(this::onMouse_Move);

      addListener(SWT.MouseHorizontalWheel, this::onMouse_Wheel);
      addListener(SWT.MouseVerticalWheel, this::onMouse_Wheel);

      addListener(SWT.KeyDown, this::onKey_Down);

      addControlListener(controlResizedAdapter(controlEvent -> onResize()));

      // enable traverse keys
      addTraverseListener(traverseEvent -> traverseEvent.doit = true);
   }

   public void addBreadcrumbListener(final IBreadcrumbListener listener) {
      _allBreadcrumbListener.add(listener);
   }

   /**
    * Set map as drop target
    */
   private void addDropTarget() {

      _dropTarget = new DropTarget(this, DND.DROP_MOVE | DND.DROP_COPY);
      _dropTarget.setTransfer(URLTransfer.getInstance(), TextTransfer.getInstance());

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
            _display.asyncExec(() -> onDropRunnable(event));
         }
      });
   }

   public void addHoveredTourListener(final IHoveredTourListener hoveredTourListener) {
      _allHoveredTourListeners.add(hoveredTourListener);
   }

   public void addMapGridBoxListener(final IMapGridListener mapListener) {
      _allMapGridListener.add(mapListener);
   }

   public void addMapInfoListener(final IMapInfoListener mapInfoListener) {
      _allMapInfoListener.add(mapInfoListener);
   }

   public void addMapPositionListener(final IMapPositionListener listener) {
      _allMapPositionListener.add(listener);
   }

   public void addMapSelectionListener(final IMapSelectionListener listener) {
      _allMapSelectionListener.add(listener);
   }

   public void addMousePositionListener(final IGeoPositionListener mapListener) {
      _allMousePositionListeners.add(mapListener);
   }

   /**
    * Adds a map overlay. This is a Painter which will paint on top of the map. It can be used to
    * draw waypoints, lines, or static overlays like text messages.
    *
    * @param overlay
    *           the map overlay to use
    * @see org.jdesktop.swingx.painters.Painter
    */
   public void addOverlayPainter(final Map2Painter overlay) {

      _allMapPainter.add(overlay);

      paint();
   }

   public void addPOIListener(final IPOIListener poiListener) {
      _allPOIListeners.add(poiListener);
   }

   public void addTourSelectionListener(final ITourSelectionListener tourSelectionListener) {
      _allTourSelectionListener.add(tourSelectionListener);
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

   private void createActions() {

      _actionManageOfflineImages = new ActionManageOfflineImages(Map2.this);
   }

   /**
    * create the context menu
    */
   private void createContextMenu() {

      final MenuManager menuMgr = new MenuManager();

      menuMgr.setRemoveAllWhenShown(true);

      menuMgr.addMenuListener(menuManager -> {

         if ((_mp == null) || _isContextMenuEnabled == false) {
            return;
         }

         if (_mapContextProvider != null) {
            _mapContextProvider.fillContextMenu(menuManager, _actionManageOfflineImages);
         }
      });

      setMenu(menuMgr.createContextMenu(this));
   }

   private Cursor createCursorFromImage(final String imageName) {

      final String imageName4k = DPITools.get4kImageName(imageName);

      return UI.createCursorFromImage(TourbookPlugin.getImageDescriptor(imageName4k));
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

      final Image mapImage = new Image(display, width, height);

      if (UI.isDarkTheme()) {

         /*
          * It looks ugly when in the dark theme the default map background is white which occure
          * before the map tile images are painted
          */

         final GC gc = new GC(mapImage);
         {
            gc.setBackground(ThemeUtil.getDarkestBackgroundColor());
            gc.fillRectangle(clientArea);
         }
         gc.dispose();
      }

      return mapImage;
   }

   public void deleteFailedImageFiles() {
      MapProviderManager.deleteOfflineMap(_mp, true);
   }

   public synchronized void dimMap(final boolean isDimMap, final int dimLevel, final RGB dimColor) {

      _mp.setDimLevel(isDimMap, dimLevel, dimColor);

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

      grid_UpdatePaintingStateData();
   }

   public void disposeTiles() {

      _mp.disposeTiles();
   }

   private void fireEvent_HoveredTour(final Long hoveredTourId, final int hoveredValuePointIndex) {

      final MapHoveredTourEvent event = new MapHoveredTourEvent(
            hoveredTourId,
            hoveredValuePointIndex,
            _mouseMove_DevPosition_X,
            _mouseMove_DevPosition_Y);

      for (final Object listener : _allHoveredTourListeners.getListeners()) {
         ((IHoveredTourListener) listener).setHoveredTourId(event);
      }
   }

   private void fireEvent_MapGrid(final boolean isGridSelected, final MapGridData mapGridData) {

      final Object[] listeners = _allMapGridListener.getListeners();

      final GeoPosition geoCenter = getMapGeoCenter();

      for (final Object listener : listeners) {

         ((IMapGridListener) listener).onMapGrid(
               _mapZoomLevel,
               geoCenter,
               isGridSelected,
               mapGridData);
      }
   }

   private void fireEvent_MapInfo() {

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
   private void fireEvent_MapPosition(final boolean isZoomed) {

      final GeoPosition geoCenter = getMapGeoCenter();

      final Object[] listeners = _allMapPositionListener.getListeners();

      for (final Object listener : listeners) {
         ((IMapPositionListener) listener).onMapPosition(geoCenter, _mapZoomLevel, isZoomed);
      }
   }

   /**
    * @param selection
    */
   private void fireEvent_MapSelection(final ISelection selection) {

      final Object[] listeners = _allMapSelectionListener.getListeners();

      for (final Object listener : listeners) {
         ((IMapSelectionListener) listener).onMapSelection(selection);
      }
   }

   private void fireEvent_MousePosition() {

      // check position, can initially be null
      if ((_mouseMove_DevPosition_X == Integer.MIN_VALUE) || (_mp == null)) {
         return;
      }

      /*
       * !!! DON'T OPTIMIZE THE NEXT LINE, OTHERWISE THE WRONG MOUSE POSITION IS FIRED !!!
       */
      final Rectangle topLeftViewPort = getWorldPixel_TopLeft_Viewport(_worldPixel_MapCenter);

      final int worldMouseX = topLeftViewPort.x + _mouseMove_DevPosition_X;
      final int worldMouseY = topLeftViewPort.y + _mouseMove_DevPosition_Y;

      final GeoPosition geoPosition = _mp.pixelToGeo(new Point2D.Double(worldMouseX, worldMouseY), _mapZoomLevel);
      final MapGeoPositionEvent event = new MapGeoPositionEvent(geoPosition, _mapZoomLevel);

      final Object[] listeners = _allMousePositionListeners.getListeners();
      for (final Object listener : listeners) {
         ((IGeoPositionListener) listener).setPosition(event);
      }
   }

   private void fireEvent_POI(final GeoPosition geoPosition, final String poiText) {

      final MapPOIEvent event = new MapPOIEvent(geoPosition, _mapZoomLevel, poiText);

      final Object[] listeners = _allPOIListeners.getListeners();
      for (final Object listener : listeners) {
         ((IPOIListener) listener).setPOI(event);
      }
   }

   private void fireEvent_TourBreadcrumb() {

      for (final Object selectionListener : _allBreadcrumbListener.getListeners()) {
         ((IBreadcrumbListener) selectionListener).updateBreadcrumb();
      }
   }

   private void fireEvent_TourSelection(final ISelection selection) {

      for (final Object selectionListener : _allTourSelectionListener.getListeners()) {
         ((ITourSelectionListener) selectionListener).onTourSelection(selection);
      }
   }

   public GeoPosition get_mouseDown_GeoPosition() {
      return _mouseDown_ContextMenu_GeoPosition;
   }

   public GeoPosition get_mouseMove_GeoPosition() {
      return _mouseMove_GeoPosition;
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

         positions.add(new GeoPosition(
               Double.parseDouble(boundingBoxValues[0]),
               Double.parseDouble(boundingBoxValues[2])));

         positions.add(new GeoPosition(
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

   public CenterMapBy getCenterMapBy() {
      return _centerMapBy;
   }

   /**
    * Retrieve, if any, the current tour hovered by the user.
    *
    * @return If found, the current hovered tour otherwise <code>null</code>
    */
   public Long getHoveredTourId() {

      if (_allHoveredTourIds != null && _allHoveredTourIds.size() == 1) {

         return _allHoveredTourIds.get(0);
      }

      return null;
   }

   /**
    * Find a more precise hovered position
    *
    * @param hoveredTourId
    * @param devMouseTileY
    * @param devMouseTileX
    * @param devHoveredTileY
    * @param devHoveredTileX
    * @param allPainted_HoveredRectangle
    * @param allPainted_HoveredTourId
    * @param allPainted_HoveredSerieIndices
    * @return
    */
   private int getHoveredValuePointIndex(final Long hoveredTourId,
                                         final int devMouseTileX,
                                         final int devMouseTileY,
                                         final int devHoveredTileX,
                                         final int devHoveredTileY,
                                         final Rectangle[] allPainted_HoveredRectangle,
                                         final long[] allPainted_HoveredTourId,
                                         final int[] allPainted_HoveredSerieIndices) {

      /*
       * Get indices which are containing the hovered tour id
       */
      int firstPaintIndex = -1;
      int lastPaintIndex = -1;

      for (int paintIndex = 0; paintIndex < allPainted_HoveredTourId.length; paintIndex++) {

         final long paintedTourId = allPainted_HoveredTourId[paintIndex];

         if (firstPaintIndex == -1 && paintedTourId == hoveredTourId) {

            firstPaintIndex = lastPaintIndex = paintIndex;

         } else if (paintedTourId == hoveredTourId) {

            lastPaintIndex = paintIndex;

         } else if (lastPaintIndex != -1) {

            // there are no further positions for the hovered tour

            break;
         }
      }

      /*
       * Find index with the smallest x/y diff value
       */
      int minDiff = Integer.MAX_VALUE;
      int minHoverIndex = 0;

      int hoverIndex;

      for (hoverIndex = firstPaintIndex; hoverIndex <= lastPaintIndex; hoverIndex++) {

         final Rectangle painted_HoveredRectangle = allPainted_HoveredRectangle[hoverIndex];

         final int paintedHovered_CenterX = painted_HoveredRectangle.x + painted_HoveredRectangle.width / 2;
         final int paintedHovered_CenterY = painted_HoveredRectangle.y + painted_HoveredRectangle.height / 2;

         int diffX = devMouseTileX - paintedHovered_CenterX;
         int diffY = devMouseTileY - paintedHovered_CenterY;

         diffX = diffX < 0 ? -diffX : diffX;
         diffY = diffY < 0 ? -diffY : diffY;

         final int allDiff = diffX + diffY;

         if (allDiff < minDiff) {

            minDiff = allDiff;
            minHoverIndex = allPainted_HoveredSerieIndices[hoverIndex];
         }
      }

      return minHoverIndex;
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

      return _mp.pixelToGeo(_worldPixel_MapCenter, _mapZoomLevel);
   }

   /**
    * @return Returns the overlay map painter which are defined as plugin extension
    */
   public List<Map2Painter> getMapPainter() {
      return _allMapPainter;
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

   private PoiToolTip getPoiTooltip() {

      if (_poi_Tooltip == null) {
         _poi_Tooltip = new PoiToolTip(getShell());
      }

      return _poi_Tooltip;
   }

   /**
    * @param tourId
    * @return Return dev X/Y position of the hovered tour or <code>null</code>
    */
   private int[] getReducesTourPositions(final long tourId) {

      final TourData tourData = TourManager.getTour(tourId);

      if (tourData == null) {

         // this happened, it can be that previously a history/multiple tour was displayed
         return null;
      }

      final double[] latitudeSerie = tourData.latitudeSerie;
      final double[] longitudeSerie = tourData.longitudeSerie;

      if (latitudeSerie == null) {
         return null;
      }

      final MP mp = getMapProvider();
      final int zoomLevel = getZoom();

      // paint with much less points to speed it up
      final int numMaxSegments = 1000;
      final float numSlices = latitudeSerie.length;
      final int numSegments = (int) Math.min(numMaxSegments, numSlices);

      final Rectangle worldPosition_Viewport = _worldPixel_TopLeft_Viewport;

      // get world position for the first lat/lon
      final java.awt.Point worldPos_FirstAWT = mp.geoToPixel(
            new GeoPosition(latitudeSerie[0], longitudeSerie[0]),
            zoomLevel);

      // convert world position into device position
      int devPosX1 = worldPos_FirstAWT.x - worldPosition_Viewport.x;
      int devPosY1 = worldPos_FirstAWT.y - worldPosition_Viewport.y;

      final int[] devXY = new int[numSegments * 2];

      // set first position
      devXY[0] = devPosX1;
      devXY[1] = devPosY1;

      for (int segmentIndex = 1; segmentIndex < numSegments; segmentIndex++) {

         final int nextSerieIndex = (int) (numSlices / numSegments * segmentIndex);

         // get world position for the current lat/lon
         final java.awt.Point worldPosAWT = mp.geoToPixel(
               new GeoPosition(latitudeSerie[nextSerieIndex], longitudeSerie[nextSerieIndex]),
               zoomLevel);

         // convert world position into device position
         final int devPosX2 = worldPosAWT.x - worldPosition_Viewport.x;
         final int devPosY2 = worldPosAWT.y - worldPosition_Viewport.y;

         final int devXYIndex = segmentIndex * 2;

         devXY[devXYIndex + 0] = devPosX2;
         devXY[devXYIndex + 1] = devPosY2;

         // advance to the next segment
         devPosX1 = devPosX2;
         devPosY1 = devPosY2;
      }

      return devXY;
   }

   /**
    * Returns the bounds of the viewport in pixels. This can be used to transform points into the
    * world bitmap coordinate space. The viewport is the part of the map, that you can currently see
    * on the screen.
    *
    * @return Returns the bounds in <em>pixels</em> of the "view" of this map
    */
   private Rectangle getWorldPixel_TopLeft_Viewport(final Point2D worldPixelMapCenter) {

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
    * @return Returns the map viewport in world pixel for the current map center
    *         <p>
    *         <b>x</b> and <b>y</b> contains the position in world pixel of the top left viewport in
    *         the map<br>
    *         <b>width</b> and <b>height</b> contains the visible area in device pixel
    */
   public Rectangle getWorldPixelViewport() {
      return getWorldPixel_TopLeft_Viewport(_worldPixel_MapCenter);
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
    * Convert start/end positions into top-left/bottom-end positions.
    *
    * @param geo_Start
    * @param geo_End
    * @param mapGridData
    * @return
    */
   private void grid_Convert_StartEnd_2_TopLeft(GeoPosition geo_Start,
                                                final GeoPosition geo_End,
                                                final MapGridData mapGridData) {

      if (geo_Start == null) {

         // this can occur when hovering but not yet selected

         geo_Start = geo_End;
      }

      final Point world_Start = UI.SWT_Point(_mp.geoToPixel(geo_Start, _mapZoomLevel));
      final Point world_End = UI.SWT_Point(_mp.geoToPixel(geo_End, _mapZoomLevel));

      mapGridData.world_Start = world_Start;
      mapGridData.world_End = world_End;

      final int world_Start_X = world_Start.x;
      final int world_Start_Y = world_Start.y;
      final int world_End_X = world_End.x;
      final int world_End_Y = world_End.y;

      // 1: top/left
      // 2: bottom/right

      double geoLat1 = 0;
      double geoLon1 = 0;

      double geoLat2 = 0;
      double geoLon2 = 0;

      // XY1: top/left
      geoLon1 = world_Start_X <= world_End_X ? geo_Start.longitude : geo_End.longitude;
      geoLat1 = world_Start_Y <= world_End_Y ? geo_Start.latitude : geo_End.latitude;

      // XY2: bottom/right
      geoLon2 = world_Start_X >= world_End_X ? geo_Start.longitude : geo_End.longitude;
      geoLat2 = world_Start_Y >= world_End_Y ? geo_Start.latitude : geo_End.latitude;

      final GeoPosition geo1 = new GeoPosition(geoLat1, geoLon1);
      final GeoPosition geo2 = new GeoPosition(geoLat2, geoLon2);

      // set lat/lon to a grid of 0.01�
      int geoGrid_Lat1_E2 = (int) (geoLat1 * 100);
      int geoGrid_Lon1_E2 = (int) (geoLon1 * 100);

      int geoGrid_Lat2_E2 = (int) (geoLat2 * 100);
      int geoGrid_Lon2_E2 = (int) (geoLon2 * 100);

      // keep geo location
      mapGridData.geoLocation_TopLeft_E2 = new Point(geoGrid_Lon1_E2, geoGrid_Lat1_E2); // X1 / Y1
      mapGridData.geoLocation_BottomRight_E2 = new Point(geoGrid_Lon2_E2, geoGrid_Lat2_E2); // X2 /Y2

      final Point devGrid_1 = grid_Geo2Dev_WithGeoGrid(geo1);
      final Point devGrid_2 = grid_Geo2Dev_WithGeoGrid(geo2);

      int devGrid_X1 = devGrid_1.x;
      int devGrid_Y1 = devGrid_1.y;

      int devGrid_X2 = devGrid_2.x;
      int devGrid_Y2 = devGrid_2.y;

      final int geoGridPixelSizeX = (int) _devGridPixelSize_X;
      final int geoGridPixelSizeY = (int) _devGridPixelSize_Y;

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

         geoGrid_Lon2_E2 += gridSize_E2; // X2
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

      int devWidth = devGrid_X2 - devGrid_X1;
      int devHeight = devGrid_Y2 - devGrid_Y1;

      // ensure it is always visible
      devWidth = (int) Math.max(geoGridPixelSizeX * 0.7, devWidth);
      devHeight = (int) Math.max(geoGridPixelSizeY * 0.7, devHeight);

      mapGridData.devGrid_X1 = devGrid_X1;
      mapGridData.devGrid_Y1 = devGrid_Y1;

      mapGridData.devWidth = devWidth;
      mapGridData.devHeight = devHeight;

      mapGridData.numWidth = (int) (devWidth / _devGridPixelSize_X + 0.5);
      mapGridData.numHeight = (int) (devHeight / _devGridPixelSize_Y + 0.5);

      mapGridData.geoParts_TopLeft_E2 = new Point(geoGrid_Lon1_E2, geoGrid_Lat1_E2); // X1 / Y1
      mapGridData.geoParts_BottomRight_E2 = new Point(geoGrid_Lon2_E2, geoGrid_Lat2_E2); // X2 /Y2
   }

   /**
    * Hide geo grid and reset all states
    */
   private void grid_DisableGridBoxSelection() {

      _isContextMenuEnabled = true;

      TourGeoFilter_Manager.setGeoFilter_OpenSlideout(false, false);

      setCursorOptimized(_cursorDefault);
      redraw();
   }

   /**
    * @param mouseBorderPosition
    * @param eventTime
    */
   private void grid_DoAutoScroll(final Point mouseBorderPosition) {

      final int AUTO_SCROLL_INTERVAL = 50; // 20ms == 50fps

      _geoGrid_IsGridAutoScroll = true;
      _geoGrid_AutoScrollCounter[0]++;
      setCursorOptimized(_cursorSearchTour_Scroll);

      getDisplay().timerExec(AUTO_SCROLL_INTERVAL, new Runnable() {

         final int __runnableScrollCounter = _geoGrid_AutoScrollCounter[0];

         @Override
         public void run() {

            if (__runnableScrollCounter != _geoGrid_AutoScrollCounter[0]) {
               // a new runnable is created
               return;
            }

            if (isDisposed() || _geoGrid_IsGridAutoScroll == false) {
               // auto scrolling is stopped
               return;
            }

            /*
             * set new map center
             */

            final int mapDiffX = mouseBorderPosition.x / 3;
            final int mapDiffY = mouseBorderPosition.y / 3;

            final double oldCenterX = _worldPixel_MapCenter.getX();
            final double oldCenterY = _worldPixel_MapCenter.getY();

            final double newCenterX = oldCenterX - mapDiffX;
            final double newCenterY = oldCenterY - mapDiffY;

            // set new map center
            setMapCenterInWorldPixel(new Point2D.Double(newCenterX, newCenterY));
            updateViewPortData();

            paint();

            fireEvent_MapPosition(false);

            // start scrolling again when the bounds have not been reached
            final Point mouseBorderPosition = grid_GetMouseBorderPosition();
            final boolean isRepeatScrolling = mouseBorderPosition != null;
            if (isRepeatScrolling) {
               getDisplay().timerExec(AUTO_SCROLL_INTERVAL, this);
            } else {
               _geoGrid_IsGridAutoScroll = false;
               setCursorOptimized(_cursorSearchTour);
            }
         }
      });
   }

   /**
    * Convert geo position into grid dev position
    *
    * @return
    */
   private Point grid_Geo2Dev_WithGeoGrid(final GeoPosition geoPos) {

      // truncate to 0.01

      final double geoLat_E2 = geoPos.latitude * 100;
      final double geoLon_E2 = geoPos.longitude * 100;

      final double geoLat = (int) geoLat_E2 / 100.0;
      final double geoLon = (int) geoLon_E2 / 100.0;

      final java.awt.Point worldGrid = _mp.geoToPixel(new GeoPosition(geoLat, geoLon), _mapZoomLevel);

      // get device rectangle for the position
      final Point gridGeoPos = new Point(
            worldGrid.x - _worldPixel_TopLeft_Viewport.x,
            worldGrid.y - _worldPixel_TopLeft_Viewport.y);

      /*
       * Adjust Y that X and Y are at the top/left position otherwise Y is at the bottom/left
       * position
       */
      gridGeoPos.y -= _devGridPixelSize_Y;

      return gridGeoPos;
   }

   /**
    * @param mouseEvent
    * @return Returns mouse position in the map border or <code>null</code> when the border is not
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

      final int mouseX = _mouseMove_DevPosition_X;
      final int mouseY = _mouseMove_DevPosition_Y;

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

   private void grid_UpdateEndPosition(final MouseEvent mouseEvent, final MapGridData mapGridData) {

      final int worldMouseX = _worldPixel_TopLeft_Viewport.x + mouseEvent.x;
      final int worldMouseY = _worldPixel_TopLeft_Viewport.y + mouseEvent.y;

      final Point worldMouse_End = new Point(worldMouseX, worldMouseY);
      final GeoPosition geo_End = _mp.pixelToGeo(new Point2D.Double(worldMouse_End.x, worldMouse_End.y), _mapZoomLevel);

      mapGridData.geo_End = geo_End;

      grid_Convert_StartEnd_2_TopLeft(mapGridData.geo_Start, mapGridData.geo_End, mapGridData);
   }

   /**
    * Update geo grid positions after map relocation
    */
   private void grid_UpdateGeoGridData() {

      if (_geoGrid_Data_Hovered != null) {

         if (_geoGrid_Data_Hovered.geo_Start != null) {

            final GeoPosition geo_Start = _geoGrid_Data_Hovered.geo_Start;
            final GeoPosition geo_End = _geoGrid_Data_Hovered.geo_End;

            grid_Convert_StartEnd_2_TopLeft(geo_Start, geo_End, _geoGrid_Data_Hovered);

         } else {

            /*
             * When hovering has started then there is no geo_Start value but the painting data can
             * be updated from the mouse move positions
             */

            final GeoPosition geo_MouseMove = _geoGrid_Data_Hovered.geo_MouseMove;
            if (geo_MouseMove != null) {

               grid_Convert_StartEnd_2_TopLeft(geo_MouseMove, geo_MouseMove, _geoGrid_Data_Hovered);
            }
         }
      }

      if (_geoGrid_Data_Selected != null && _geoGrid_Data_Selected.geo_Start != null) {

         final GeoPosition geo_Start = _geoGrid_Data_Selected.geo_Start;
         final GeoPosition geo_End = _geoGrid_Data_Selected.geo_End;

         grid_Convert_StartEnd_2_TopLeft(geo_Start, geo_End, _geoGrid_Data_Selected);
      }

      redraw();
   }

   private void grid_UpdatePaintingStateData() {

      // set fast map paining
      _isFastMapPainting = Util.getStateBoolean(_geoFilterState,
            TourGeoFilter_Manager.STATE_IS_FAST_MAP_PAINTING,
            TourGeoFilter_Manager.STATE_IS_FAST_MAP_PAINTING_DEFAULT);

      _fastMapPainting_skippedValues = Util.getStateInt(_geoFilterState,
            TourGeoFilter_Manager.STATE_FAST_MAP_PAINTING_SKIPPED_VALUES,
            TourGeoFilter_Manager.STATE_FAST_MAP_PAINTING_SKIPPED_VALUES_DEFAULT);
   }

   /**
    * Convert geo position into dev position
    *
    * @return
    */
   private Point grid_World2Dev(final Point worldPosition) {

      // get device rectangle for the position
      final Point gridGeoPos = new Point(
            worldPosition.x - _worldPixel_TopLeft_Viewport.x,
            worldPosition.y - _worldPixel_TopLeft_Viewport.y);

      return gridGeoPos;
   }

   private void hideTourTooltipHoveredArea() {

      if (_tour_ToolTip == null) {
         return;
      }

      // update tool tip because it has it's own mouse move listener for the map
      _tour_ToolTip.hideHoveredArea();

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

      _worldPixel_MapCenter = new Point2D.Double(tileDefaultCenter, tileDefaultCenter);
      _worldPixel_TopLeft_Viewport = getWorldPixel_TopLeft_Viewport(_worldPixel_MapCenter);
   }

   public boolean isCutOffLinesInPauses() {
      return _prefOptions_isCutOffLinesInPauses;
   }

   public boolean isMapBackgroundDark() {
      return _isMapBackgroundDark;
   }

   private boolean isPaintTile_With_BasicMethod() {

      return _isTourPaintMethodEnhanced == false || (_isFastMapPainting && _isFastMapPainting_Active);
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
    * @return Returns <code>true</code> when 'Search tour by location' is active
    */
   public boolean isSearchTourByLocation() {

      return _geoGrid_Data_Hovered != null || _geoGrid_IsGridAutoScroll == true;

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
    * The critical parts in this method is to convert mouse position into tile position.
    *
    * @return Returns <code>true</code> when a tour is hovered.
    */
   private boolean isTourHovered() {

      if (_worldPixel_TopLeft_Viewport == null) {

         // this occurred when comparing tours

         return false;
      }

      int numPrevHoveredTours;

      Rectangle[] allPainted_HoveredRectangle = null;
      long[] allPainted_HoveredTourId = null;
      int[] allPainted_HoveredSerieIndices = null;

      int devMouseTileX = 0;
      int devMouseTileY = 0;

      // top/left dev position for the hovered tile
      int devHoveredTileX = 0;
      int devHoveredTileY = 0;

      try {

         Tile hoveredTile = null;

         int hoveredTileIndex_X = 0;
         int hoveredTileIndex_Y = 0;

         final int devMouseX = _mouseMove_DevPosition_X;
         final int devMouseY = _mouseMove_DevPosition_Y;

         final Tile[][] allPaintedTiles = _allPaintedTiles;

         /*
          * Get tile which is hovered
          */
         tileLoop:

         for (int tilePosX = _tilePos_MinX; tilePosX <= _tilePos_MaxX; tilePosX++) {

            hoveredTileIndex_Y = 0;

            for (int tilePosY = _tilePos_MinY; tilePosY <= _tilePos_MaxY; tilePosY++) {

               // convert tile world position into device position
               devHoveredTileX = tilePosX * _tilePixelSize - _worldPixel_TopLeft_Viewport.x;
               devHoveredTileY = tilePosY * _tilePixelSize - _worldPixel_TopLeft_Viewport.y;

               final int devTileXNext = devHoveredTileX + _tilePixelSize;
               final int devTileYNext = devHoveredTileY + _tilePixelSize;

               if (devMouseX >= devHoveredTileX && devMouseX < devTileXNext) {
                  if (devMouseY >= devHoveredTileY && devMouseY < devTileYNext) {

                     // this is the tile which is hovered with the mouse

                     hoveredTile = allPaintedTiles[hoveredTileIndex_X][hoveredTileIndex_Y];

                     break tileLoop;
                  }
               }

               hoveredTileIndex_Y++;
            }

            hoveredTileIndex_X++;
         }

         numPrevHoveredTours = _allHoveredTourIds.size();

         // reset hovered data
         _allHoveredDevPoints.clear();
         _allHoveredTourIds.clear();
         _allHoveredSerieIndices.clear();

         if (hoveredTile == null) {

            // this can occur when map is zoomed
            return false;
         }

         final List<Rectangle> allPainted_HoveredRectangle_List = hoveredTile.allPainted_HoverRectangle;
         if (allPainted_HoveredRectangle_List.isEmpty()) {

            // nothing is painted in this tile
            return false;
         }

         // get mouse relative position in the tile
         devMouseTileX = devMouseX - devHoveredTileX;
         devMouseTileY = devMouseY - devHoveredTileY;

         // optimize performance by removing object references
         allPainted_HoveredTourId = hoveredTile.allPainted_HoverTourID.toArray();
         allPainted_HoveredSerieIndices = hoveredTile.allPainted_HoverSerieIndices.toArray();

         allPainted_HoveredRectangle = allPainted_HoveredRectangle_List.toArray(new Rectangle[allPainted_HoveredRectangle_List.size()]);

         long painted_HoveredTourId = -1;
         final int numPainted_HoveredTourId = allPainted_HoveredTourId.length;

         for (int hoverIndex = 0; hoverIndex < numPainted_HoveredTourId; hoverIndex++) {

            final Rectangle painted_HoveredRectangle = allPainted_HoveredRectangle[hoverIndex];

            final int paintedHoveredX = painted_HoveredRectangle.x;
            final int paintedHoveredY = painted_HoveredRectangle.y;

            // optimized: painted_HoveredRectangle.contains(devMouseTileX, devMouseTileY)
            if (devMouseTileX >= paintedHoveredX
                  && devMouseTileY >= paintedHoveredY
                  && devMouseTileX < (paintedHoveredX + painted_HoveredRectangle.width)
                  && devMouseTileY < (paintedHoveredY + painted_HoveredRectangle.height)) {

               // a tour is hovered

               painted_HoveredTourId = allPainted_HoveredTourId[hoverIndex];

               int devHoveredRect_Center_X = paintedHoveredX + painted_HoveredRectangle.width / 2;
               int devHoveredRect_Center_Y = paintedHoveredY + painted_HoveredRectangle.height / 2;

               // convert from tile position to device position
               devHoveredRect_Center_X += devHoveredTileX;
               devHoveredRect_Center_Y += devHoveredTileY;

               _allHoveredTourIds.add(painted_HoveredTourId);
               _allHoveredSerieIndices.add(allPainted_HoveredSerieIndices[hoverIndex]);
               _allHoveredDevPoints.add(new Point(devHoveredRect_Center_X, devHoveredRect_Center_Y));

               // advance to the next tour id
               int hoverTourIdIndex;
               for (hoverTourIdIndex = hoverIndex + 1; hoverTourIdIndex < numPainted_HoveredTourId; hoverTourIdIndex++) {

                  final long nextPainted_HoveredTourId = allPainted_HoveredTourId[hoverTourIdIndex];

                  if (nextPainted_HoveredTourId != painted_HoveredTourId) {

                     // the next tour id is discovered

                     hoverIndex = hoverTourIdIndex;

                     break;
                  }
               }

               // this must be checked again otherwise the last tour id occurs multiple times
               if (hoverTourIdIndex >= numPainted_HoveredTourId) {
                  break;
               }
            }
         }

      } finally {

         // fire hover event, when a tour is hovered

         final int numHoveredSerieIndices = _allHoveredSerieIndices.size();

         if (numHoveredSerieIndices > 0) {

            int hoveredValuePointIndex;

            if (numHoveredSerieIndices == 1) {

               hoveredValuePointIndex = getHoveredValuePointIndex(

                     _allHoveredTourIds.get(0),

                     devMouseTileX,
                     devMouseTileY,

                     devHoveredTileX,
                     devHoveredTileY,

                     allPainted_HoveredRectangle,
                     allPainted_HoveredTourId,
                     allPainted_HoveredSerieIndices);

               // overwrite initial value
               _allHoveredSerieIndices.set(0, hoveredValuePointIndex);

            } else {

               hoveredValuePointIndex = _allHoveredSerieIndices.get(0);
            }

            fireEvent_HoveredTour(getHoveredTourId(), hoveredValuePointIndex);
         }
      }

      if (_allHoveredTourIds.size() > 0) {

         return true;

      } else {

         // hide previously hovered tour
         if (numPrevHoveredTours > 0) {
            paint();
         }

         return false;
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

      setCursorOptimized(_cursorDefault);
      redraw();
   }

   /**
    * Create top/left geo grid position from world position
    *
    * @param worldPosX
    * @param worldPosY
    * @return
    */
   private Point offline_GetDevGridGeoPosition(final int worldPosX, final int worldPosY) {

      final Point2D.Double worldPixel = new Point2D.Double(worldPosX, worldPosY);

      final GeoPosition geoPos = _mp.pixelToGeo(worldPixel, _mapZoomLevel);

      // truncate to 0.01

      final double geoLat_E2 = geoPos.latitude * 100;
      final double geoLon_E2 = geoPos.longitude * 100;

      final double geoLat = (int) geoLat_E2 / 100.0;
      final double geoLon = (int) geoLon_E2 / 100.0;

      final java.awt.Point worldGrid = _mp.geoToPixel(new GeoPosition(geoLat, geoLon), _mapZoomLevel);

      // get device rectangle for the position
      final Point gridGeoPos = new Point(
            worldGrid.x - _worldPixel_TopLeft_Viewport.x,
            worldGrid.y - _worldPixel_TopLeft_Viewport.y);

      /*
       * Adjust Y that X and Y are at the top/left position otherwise Y is at the bottom/left
       * position
       */
      gridGeoPos.y -= _devGridPixelSize_Y;

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
      return new Point(
            tilePosX * _tilePixelSize - _worldPixel_TopLeft_Viewport.x,
            tilePosY * _tilePixelSize - _worldPixel_TopLeft_Viewport.y);
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

      final int worldMouseX = _worldPixel_TopLeft_Viewport.x + mouseEvent.x;
      final int worldMouseY = _worldPixel_TopLeft_Viewport.y + mouseEvent.y;

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

      UI.disposeResource(_mapImage);
      UI.disposeResource(_poiImage);

      UI.disposeResource(_9PartImage);
      UI.disposeResource(_9PartGC);

      UI.disposeResource(_cursorCross);
      UI.disposeResource(_cursorDefault);
      UI.disposeResource(_cursorHand);
      UI.disposeResource(_cursorPan);
      UI.disposeResource(_cursorSearchTour);
      UI.disposeResource(_cursorSearchTour_Scroll);
      UI.disposeResource(_cursorSelect);

      // dispose resources in the overlay plugins
      for (final Map2Painter overlay : _allMapPainter) {
         overlay.dispose();
      }

      _overlayImageCache.dispose();
      _colorCache.dispose();

      if (_directMapPainter != null) {
         _directMapPainter.dispose();
      }

      // dispose legend image
      if (_mapLegend != null) {
         UI.disposeResource(_mapLegend.getImage());
      }

      if (_poi_Tooltip != null) {
         _poi_Tooltip.dispose();
      }

      // stop overlay thread
      _overlayThread.interrupt();
   }

   private void onDropRunnable(final DropTargetEvent event) {

      final TransferData transferDataType = event.currentDataType;

      boolean isPOI = false;

      if (TextTransfer.getInstance().isSupportedType(transferDataType)) {

         if (event.data instanceof String) {
            isPOI = parseAndDisplayPOIText((String) event.data);
         }

      } else if (URLTransfer.getInstance().isSupportedType(transferDataType)) {
         isPOI = parseAndDisplayPOIText((String) event.data);
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

         MessageDialog.openInformation(getShell(),
               Messages.Dialog_DropNoPOI_Title,
               NLS.bind(Messages.Dialog_DropNoPOI_Message, poiText));
      }
   }

   private void onKey_Down(final Event event) {

      if (_offline_IsSelectingOfflineArea) {
         offline_DisableOfflineAreaSelection();
         return;
      }

      // stop tour search by location
      if (_geoGrid_Data_Hovered != null) {

         _geoGrid_Data_Hovered = null;
         _geoGrid_IsGridAutoScroll = false;

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
         xDiff = _isInInverseKeyboardPanning ? -offset : offset;
         break;
      case SWT.ARROW_RIGHT:
         xDiff = _isInInverseKeyboardPanning ? offset : -offset;
         break;
      case SWT.ARROW_UP:
         yDiff = _isInInverseKeyboardPanning ? -offset : offset;
         break;
      case SWT.ARROW_DOWN:
         yDiff = _isInInverseKeyboardPanning ? offset : -offset;
         break;
      }

      switch (event.character) {
      case '+':
         zoomIn(CenterMapBy.Map);
         break;
      case '-':
         zoomOut(CenterMapBy.Map);
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
         final double x = _worldPixel_TopLeft_Viewport.x + mouseEvent.x;
         final double y = _worldPixel_TopLeft_Viewport.y + mouseEvent.y;

         setMapCenterInWorldPixel(new Point2D.Double(x, y));

         // ensure that all internal data are correctly setup
         setZoom(_mapZoomLevel);

         paint();
      }
   }

   private void onMouse_Down(final MouseEvent mouseEvent) {

      if (_worldPixel_TopLeft_Viewport == null) {

         // map is not yet fully initialized

         return;
      }

      final int devMouseX = mouseEvent.x;
      final int devMouseY = mouseEvent.y;

      // check context menu
      if (mouseEvent.button != 1) {

         // right mouse down is pressed -> keep position

         final Point worldMousePosition = new Point(
               _worldPixel_TopLeft_Viewport.x + devMouseX,
               _worldPixel_TopLeft_Viewport.y + devMouseY);

         final GeoPosition geoMousePosition = _mp.pixelToGeo(
               new Point2D.Double(worldMousePosition.x, worldMousePosition.y),
               _mapZoomLevel);

         _mouseDown_ContextMenu_GeoPosition = geoMousePosition;

         return;
      }

      final boolean isShift = (mouseEvent.stateMask & SWT.SHIFT) != 0;
//    final boolean isCtrl = (mouseEvent.stateMask & SWT.CTRL) != 0;

      hideTourTooltipHoveredArea();
      setPoiVisible(false);

      final Point devMousePosition = new Point(devMouseX, devMouseY);

      if (_offline_IsSelectingOfflineArea) {

         _offline_IsOfflineSelectionStarted = true;

         final int worldMouseX = _worldPixel_TopLeft_Viewport.x + devMouseX;
         final int worldMouseY = _worldPixel_TopLeft_Viewport.y + devMouseY;

         _offline_DevMouse_Start = devMousePosition;
         _offline_DevMouse_End = devMousePosition;

         _offline_WorldMouse_Start = new Point(worldMouseX, worldMouseY);
         _offline_WorldMouse_End = _offline_WorldMouse_Start;

         _offline_DevTileStart = offline_GetTilePosition(worldMouseX, worldMouseY);

         redraw();

      } else if (_geoGrid_Data_Hovered != null) {

         _geoGrid_Data_Hovered.isSelectionStarted = true;

         final Point worldMousePosition = new Point(
               _worldPixel_TopLeft_Viewport.x + devMouseX,
               _worldPixel_TopLeft_Viewport.y + devMouseY);

         _geoGrid_Data_Hovered.isSelectionStarted = true;

         final GeoPosition geoMousePosition = _mp.pixelToGeo(new Point2D.Double(worldMousePosition.x, worldMousePosition.y), _mapZoomLevel);
         _geoGrid_Data_Hovered.geo_Start = geoMousePosition;
         _geoGrid_Data_Hovered.geo_End = geoMousePosition;

         grid_Convert_StartEnd_2_TopLeft(geoMousePosition, geoMousePosition, _geoGrid_Data_Hovered);

         redraw();

      } else if (_isShowHoveredOrSelectedTour
            && _isShowBreadcrumbs

            // check if breadcrumb is hit
            && _tourBreadcrumb.onMouseDown(devMousePosition)) {

         // a crumb is selected

         if (_tourBreadcrumb.isAction_RemoveAllCrumbs()) {

            // crumb action: remove all crumbs

            _tourBreadcrumb.removeAllCrumbs();

            // set tour info icon position in the map
            fireEvent_TourBreadcrumb();

            redraw();

         } else if (_tourBreadcrumb.isAction_UpliftLastCrumb()) {

            // crumb action: set last crumb to the first/top crumb

            _tourBreadcrumb.resetLastBreadcrumb();

            redraw();

         } else {

            // show bread crum tours in the map

            final ArrayList<Long> crumbTourIds = _tourBreadcrumb.getHoveredCrumbedTours_WithReset();

            // hide crumb selection state, this must be done after the crumb is reset
            redraw();

            fireEvent_TourSelection(new SelectionTourIds(crumbTourIds));
         }

      } else if (_geoGrid_Label_IsHovered) {

         // set map location to the selected geo filter default position

         // hide hover color
         _geoGrid_Label_IsHovered = false;

         setCursorOptimized(_cursorDefault);

         /*
          * Zoom to geo filter zoom level
          */
         // set zoom level first, that recalculation is correct ->/ prevent recenter
         setZoom(_geoGrid_MapZoomLevel);

         /*
          * Center to geo filter position
          */
         setMapCenter(new GeoPosition(_geoGrid_MapGeoCenter.latitude, _geoGrid_MapGeoCenter.longitude));

      } else if (_geoGrid_Action_IsHovered) {

         // set selected geo filter default position to the map location

         // hide hover color
         _geoGrid_Action_IsHovered = false;

         setCursorOptimized(_cursorDefault);

         _geoGrid_TourGeoFilter.mapGeoCenter = _geoGrid_MapGeoCenter = getMapGeoCenter();
         _geoGrid_TourGeoFilter.mapZoomLevel = _geoGrid_MapZoomLevel = getZoom();

      } else if (_allHoveredTourIds.size() > 0) {

         onMouse_Down_HoveredTour(isShift);

      } else {

         // when the left mousebutton is clicked remember this point (for panning)
         _isLeftMouseButtonPressed = true;
         _mouseDownPosition = devMousePosition;

         setCursorOptimized(_cursorPan);
      }
   }

   /**
    * A tour is hovered, select/deselect tour or trackpoint
    *
    * @param isShiftKeyPressed
    */
   private void onMouse_Down_HoveredTour(final boolean isShiftKeyPressed) {

      ISelection tourSelection = null;

      if (_allHoveredTourIds.size() == 1) {

         // one tour is hovered -> select tour or trackpoint

         final long hoveredTourId = _allHoveredTourIds.get(0);
         final boolean isAnotherTourSelected = _hovered_SelectedTourId != hoveredTourId;

         if (_hoveredSelectedTour_CanSelectTour) {

            // a tour can be/is selected

            if (isAnotherTourSelected) {

               // toggle selection -> select tour

               _hovered_SelectedTourId = hoveredTourId;

               tourSelection = new SelectionTourId(_hovered_SelectedTourId);

            } else {

               // toggle selection -> hide tour selection

               _hovered_SelectedTourId = -1;
            }

            _hovered_SelectedSerieIndex_Behind = -1;
            _hovered_SelectedSerieIndex_Front = -1;

         } else {

            // a trackpoint can be/is selected

            _hovered_SelectedTourId = hoveredTourId;

            if (isAnotherTourSelected) {

               // fire a selection for a new tour

               tourSelection = new SelectionTourId(_hovered_SelectedTourId);

               _hovered_SelectedSerieIndex_Behind = -1;
               _hovered_SelectedSerieIndex_Front = -1;
            }

            final int hoveredSerieIndex = _allHoveredSerieIndices.get(0);

            if (isShiftKeyPressed) {

               // select 2nd value point

               _hovered_SelectedSerieIndex_Behind = hoveredSerieIndex;

            } else {

               // select 1st value point

               _hovered_SelectedSerieIndex_Front = hoveredSerieIndex;
            }

            /*
             * Ensure that the right index is larger than the left index
             */
            int selectedSerieIndex_Behind = _hovered_SelectedSerieIndex_Behind;
            int selectedSerieIndex_Front = _hovered_SelectedSerieIndex_Front;

            if (_hovered_SelectedSerieIndex_Front == -1) {

               selectedSerieIndex_Behind = -1;
               selectedSerieIndex_Front = _hovered_SelectedSerieIndex_Behind;

            } else if (_hovered_SelectedSerieIndex_Behind == -1) {

               // serie index in front is larger than the behind -> nothing to do

            } else if (_hovered_SelectedSerieIndex_Behind > _hovered_SelectedSerieIndex_Front) {

               final int hovered_SelectedSerieIndex_LeftBackup = _hovered_SelectedSerieIndex_Behind;

               selectedSerieIndex_Behind = _hovered_SelectedSerieIndex_Front;
               selectedSerieIndex_Front = hovered_SelectedSerieIndex_LeftBackup;
            }

            _hovered_SelectedSerieIndex_Front = selectedSerieIndex_Front;
            _hovered_SelectedSerieIndex_Behind = selectedSerieIndex_Behind;

            fireEvent_MapSelection(new SelectionMapSelection(

                  _hovered_SelectedTourId,
                  selectedSerieIndex_Behind,
                  selectedSerieIndex_Front));
         }

      } else {

         // multiple tours are selected

         // hide single tour selection
         _hovered_SelectedTourId = -1;
         _hovered_SelectedSerieIndex_Behind = -1;
         _hovered_SelectedSerieIndex_Front = -1;

         // clone tour id's becauses they will be removed
         final ArrayList<Long> allClonedTourIds = new ArrayList<>();
         allClonedTourIds.addAll(_allHoveredTourIds);

         tourSelection = new SelectionTourIds(_allHoveredTourIds);
      }

      if (tourSelection != null) {
         fireEvent_TourSelection(tourSelection);
      }

      redraw();
   }

   private void onMouse_Exit() {

      // keep position for out of the map events, e.g. recenter map
      _mouseMove_DevPosition_X_Last = _mouseMove_DevPosition_X;
      _mouseMove_DevPosition_Y_Last = _mouseMove_DevPosition_Y;

      // set position out of the map that the tool tip is not activated again
      _mouseMove_DevPosition_X = Integer.MIN_VALUE;
      _mouseMove_DevPosition_Y = Integer.MIN_VALUE;

      // stop grid autoscrolling
      _geoGrid_IsGridAutoScroll = false;

      _geoGrid_Label_IsHovered = false;

      if (_isShowHoveredOrSelectedTour) {

         _tourBreadcrumb.onMouseExit();

         // reset hovered data to hide hovered tour background
         _allHoveredTourIds.clear();
         _allHoveredDevPoints.clear();
         _allHoveredSerieIndices.clear();

         redraw();
      }

      setCursorOptimized(_cursorDefault);
   }

   private void onMouse_Move(final MouseEvent mouseEvent) {

      if (_mp == null) {
         return;
      }

      final Point devMousePosition = new Point(mouseEvent.x, mouseEvent.y);

      _mouseMove_DevPosition_X = mouseEvent.x;
      _mouseMove_DevPosition_Y = mouseEvent.y;

      // keep position for out of the map events, e.g. to recenter map
      _mouseMove_DevPosition_X_Last = _mouseMove_DevPosition_X;
      _mouseMove_DevPosition_Y_Last = _mouseMove_DevPosition_Y;

      final int worldMouseX = _worldPixel_TopLeft_Viewport.x + _mouseMove_DevPosition_X;
      final int worldMouseY = _worldPixel_TopLeft_Viewport.y + _mouseMove_DevPosition_Y;
      final Point worldMouse_Move = new Point(worldMouseX, worldMouseY);

      final GeoPosition geoMouseMove = _mp.pixelToGeo(new Point2D.Double(worldMouseX, worldMouseY), _mapZoomLevel);
      _mouseMove_GeoPosition = geoMouseMove;

      boolean isSomethingHit = false;

      if (_offline_IsSelectingOfflineArea) {

         _offline_WorldMouse_Move = worldMouse_Move;

         offline_UpdateOfflineAreaEndPosition(mouseEvent);

         paint();

         fireEvent_MapInfo();

         return;
      }

      if (_geoGrid_Data_Hovered != null) {

         // tour geo filter is hovered

         _geoGrid_Data_Hovered.geo_MouseMove = geoMouseMove;
         grid_UpdateEndPosition(mouseEvent, _geoGrid_Data_Hovered);

         // pan map when mouse is near map border
         final Point mouseBorderPosition = grid_GetMouseBorderPosition();
         if (mouseBorderPosition != null) {

            // scroll map

            grid_DoAutoScroll(mouseBorderPosition);

            return;
         }

         paint();

         fireEvent_MapInfo();
         fireEvent_MapGrid(false, _geoGrid_Data_Hovered);

         return;
      }

      if (_isLeftMouseButtonPressed) {

         // pan map

         panMap(mouseEvent);

         return;
      }

      // #######################################################################

      if (_tour_ToolTip != null && _tour_ToolTip.isActive()) {

         /*
          * check if the mouse is within a hovered area
          */
         boolean isContextValid = false;
         if (_hoveredAreaContext != null) {

            final int topLeftX = _hoveredAreaContext.hoveredTopLeftX;
            final int topLeftY = _hoveredAreaContext.hoveredTopLeftY;

            if (_mouseMove_DevPosition_X >= topLeftX
                  && _mouseMove_DevPosition_X < topLeftX + _hoveredAreaContext.hoveredWidth
                  && _mouseMove_DevPosition_Y >= topLeftY
                  && _mouseMove_DevPosition_Y < topLeftY + _hoveredAreaContext.hoveredHeight) {

               isContextValid = true;
            }
         }

         if (isContextValid == false) {

            // old hovered context is not valid any more, update the hovered context
            updateTourToolTip_HoveredArea();
         }
      }

      if (_poi_Tooltip != null && _isPoiPositionInViewport) {

         // check if mouse is within the poi image
         if (_isPoiVisible
               && (_mouseMove_DevPosition_X > _poiImageDevPosition.x)
               && (_mouseMove_DevPosition_X < _poiImageDevPosition.x + _poiImageBounds.width)
               && (_mouseMove_DevPosition_Y > _poiImageDevPosition.y - _poi_Tooltip_OffsetY - 5)
               && (_mouseMove_DevPosition_Y < _poiImageDevPosition.y + _poiImageBounds.height)) {

            // display poi
            showPoi();

         } else {

            setPoiVisible(false);
         }
      }

      /*
       * Check if mouse has hovered the grid label
       */
      if (isSomethingHit == false && _geoGrid_Label_Outline != null) {

         final boolean isHovered = _geoGrid_Label_IsHovered;

         _geoGrid_Label_IsHovered = false;

         if (_geoGrid_Label_Outline.contains(_mouseMove_DevPosition_X, _mouseMove_DevPosition_Y)) {

            _geoGrid_Label_IsHovered = true;

            setCursorOptimized(_cursorHand);

            redraw();

            isSomethingHit = true;

         } else if (isHovered) {

            // hide hovered state

            setCursorOptimized(_cursorDefault);

            redraw();
         }
      }

      /*
       * Check if mouse has hovered the grid action
       */
      if (isSomethingHit == false && _geoGrid_Action_Outline != null) {

         final boolean isHovered = _geoGrid_Action_IsHovered;

         _geoGrid_Action_IsHovered = false;

         if (_geoGrid_Action_Outline.contains(_mouseMove_DevPosition_X, _mouseMove_DevPosition_Y)) {

            _geoGrid_Action_IsHovered = true;

            setCursorOptimized(_cursorHand);

            redraw();

            isSomethingHit = true;

         } else if (isHovered) {

            // hide hovered state

            setCursorOptimized(_cursorDefault);

            redraw();
         }
      }

      if (isSomethingHit == false && _isShowHoveredOrSelectedTour) {

         final int numOldHoveredTours = _allHoveredTourIds.size();

         if (_isShowBreadcrumbs && _tourBreadcrumb.onMouseMove(devMousePosition)) {

            // breadcrumb is hovered

            // do not show hovered tour info -> reset hovered data
            _allHoveredTourIds.clear();
            _allHoveredDevPoints.clear();
            _allHoveredSerieIndices.clear();

            if (_tourBreadcrumb.isCrumbHovered()) {
               setCursorOptimized(_cursorHand);
            } else {
               setCursorOptimized(_cursorDefault);
            }

            redraw();

         } else if (isTourHovered()) {

            // tour is hovered

            setCursorOptimized(_cursorSelect);

            // show hovered tour
            redraw();

         } else if (numOldHoveredTours > 0 || _tourBreadcrumb.isCrumbHovered() == false) {

            // reset cursor

            setCursorOptimized(_cursorDefault);
         }
      }

      fireEvent_MousePosition();
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
         setCursorOptimized(_cursorDefault);

         // hide selection
         _offline_IsSelectingOfflineArea = false;

         redraw();
         paint();

         offline_OpenOfflineImageDialog();

      } else if (_geoGrid_Data_Hovered != null) {

         // finalize grid selecting

         _isContextMenuEnabled = false;

         if (_geoGrid_Data_Hovered.isSelectionStarted == false) {

            // this can happen when the right mouse button is clicked

            _geoGrid_Data_Hovered = null;
            _geoGrid_IsGridAutoScroll = true;

            grid_DisableGridBoxSelection();

            return;
         }

         /*
          * Show selected grid box
          */

         grid_UpdateEndPosition(mouseEvent, _geoGrid_Data_Hovered);

         _geoGrid_Data_Selected = _geoGrid_Data_Hovered;

         _geoGrid_Data_Hovered = null;
         _geoGrid_IsGridAutoScroll = true;

         grid_DisableGridBoxSelection();

         redraw();
         paint();

         fireEvent_MapGrid(true, _geoGrid_Data_Selected);

      } else {

         if (mouseEvent.button == 1) {

            if (_isMapPanned) {
               _isMapPanned = false;
               redraw();
            }

            _mouseDownPosition = null;
            _isLeftMouseButtonPressed = false;

            setCursorOptimized(_cursorDefault);

         } else if (mouseEvent.button == 2) {
            // if the middle mouse button is clicked, recenter the view
//            recenterMap(event.x, event.y);
         }
      }

      // show poi info when mouse is within the poi image
      if ((_mouseMove_DevPosition_X > _poiImageDevPosition.x)
            && (_mouseMove_DevPosition_X < _poiImageDevPosition.x + _poiImageBounds.width)
            && (_mouseMove_DevPosition_Y > _poiImageDevPosition.y - _poi_Tooltip_OffsetY - 5)
            && (_mouseMove_DevPosition_Y < _poiImageDevPosition.y + _poiImageBounds.height)) {

         setPoiVisible(true);
      }

      if (_isShowHoveredOrSelectedTour) {

         // when a tour is unselected, show it hovered

         if (isTourHovered()) {

            // show hovered tour
            redraw();
         }
      }
   }

   private void onMouse_Wheel(final Event event) {

      if (event.count < 0) {

         zoomOut(_centerMapBy);

      } else {

         zoomIn(_centerMapBy);
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

            // is drawing sliders in map/legend

            _directMapPainterContext.gc = gc;
            _directMapPainterContext.viewport = _worldPixel_TopLeft_Viewport;

            _directMapPainter.paint(_directMapPainterContext);
         }

         if (_hoveredAreaContext != null) {
            final Image hoveredImage = _hoveredAreaContext.hoveredImage;
            if (hoveredImage != null) {

               gc.drawImage(hoveredImage,
                     _hoveredAreaContext.hoveredTopLeftX,
                     _hoveredAreaContext.hoveredTopLeftY);
            }
         }

         if (_isPoiVisible && _poi_Tooltip != null) {
            if (_isPoiPositionInViewport = updatePoiImageDevPosition()) {
               gc.drawImage(_poiImage, _poiImageDevPosition.x, _poiImageDevPosition.y);
            }
         }

         if (_offline_IsPaintOfflineArea) {
            paint_OfflineArea(gc);
         }

         final boolean isPaintTourInfo = paint_HoveredTour(gc);

         _geoGrid_Label_Outline = null;
         _geoGrid_Action_Outline = null;
         if (_geoGrid_Data_Selected != null) {
            paint_GeoGrid_10_Selected(gc, _geoGrid_Data_Selected);
         }
         if (_geoGrid_Data_Hovered != null) {
            paint_GeoGrid_20_Hovered(gc, _geoGrid_Data_Hovered);
         }

         // paint tooltip icon in the map
         if (_tour_ToolTip != null) {
            _tour_ToolTip.paint(gc, _clientArea);
         }

         // paint info AFTER hovered/selected tour
         if (isPaintTourInfo) {
            paint_HoveredTour_50_TourInfo(gc);
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

         _display.syncExec(() -> {

            if (isDisposed()) {
               return;
            }

            paint_10_PaintMapImage();
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

                  // a newer runnable is available
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

      for (int tilePosX = _tilePos_MinX, tileIndexX = 0; tilePosX <= _tilePos_MaxX; tilePosX++, tileIndexX++) {
         for (int tilePosY = _tilePos_MinY, tileIndexY = 0; tilePosY <= _tilePos_MaxY; tilePosY++, tileIndexY++) {

            /*
             * convert tile world position into device position
             */
            final int devTileX = tilePosX * _tilePixelSize - _worldPixel_TopLeft_Viewport.x;
            final int devTileY = tilePosY * _tilePixelSize - _worldPixel_TopLeft_Viewport.y;

            final Rectangle devTileViewport = new Rectangle(devTileX, devTileY, _tilePixelSize, _tilePixelSize);

            // check if current tile is within the painting area
            if (devTileViewport.intersects(_devMapViewport)) {

               /*
                * get the tile from the factory. the tile must not have been completely downloaded
                * after this step.
                */

               if (isTileOnMap(tilePosX, tilePosY)) {

                  final Tile paintedTile = paint_Tile(gcMapImage, tilePosX, tilePosY, devTileViewport);

                  _allPaintedTiles[tileIndexX][tileIndexY] = paintedTile;

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
      int yPos = _worldPixel_TopLeft_Viewport.height - 5 - imageBounds.height;
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

      final int viewPortWidth = _worldPixel_TopLeft_Viewport.width;

      final int devScaleWidth = viewPortWidth / 3;
      final float metricWidth = 111.32f / _distanceUnitValue;

      //
      final GeoPosition mapCenter = getMapGeoCenter();
      final double latitude = mapCenter.latitude;
      final double longitude = mapCenter.longitude;

      final double devDistance = _mp.getDistance(
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

      final int devY = _worldPixel_TopLeft_Viewport.height - 5 - 3;

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

         gc.setForeground(UI.SYS_COLOR_WHITE);
         gc.drawPath(path1);

         gc.setForeground(UI.SYS_COLOR_BLACK);
         gc.drawPath(path2);
      }
      path1.dispose();
      path2.dispose();

      final int devYText = devYScaleLines - textExtent.y;
      final int devXText = devX1 + devScaleWidthRounded - textExtent.x;

      /*
       * Paint text with shadow
       */

      Color shadeColor;
      Color textColor;

      if (_isMapBackgroundDark) {

         // dark background

         shadeColor = UI.SYS_COLOR_BLACK;
         textColor = UI.SYS_COLOR_WHITE;

      } else {

         // bright background

         shadeColor = UI.SYS_COLOR_WHITE;
         textColor = UI.SYS_COLOR_BLACK;
      }

      // draw shade
      gc.setForeground(shadeColor);
      gc.drawText(scaleText, devXText + 1, devYText, true);
      gc.drawText(scaleText, devXText - 1, devYText, true);
      gc.drawText(scaleText, devXText, devYText + 1, true);
      gc.drawText(scaleText, devXText, devYText - 1, true);

      // draw text
      gc.setForeground(textColor);
      gc.drawText(scaleText, devXText, devYText, true);
   }

   private void paint_Debug_GeoGrid(final GC gc) {

      final double geoGridPixelSizeX = _devGridPixelSize_X;
      final double geoGridPixelSizeY = _devGridPixelSize_Y;

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

      final int vpWidth = _worldPixel_TopLeft_Viewport.width;
      final int vpHeight = _worldPixel_TopLeft_Viewport.height;

      int numX = (int) (vpWidth / geoGridPixelSizeXAdjusted);
      int numY = (int) (vpHeight / geoGridPixelSizeYAdjusted);

      // this can occur by high zoom level
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
         gc.setForeground(UI.SYS_COLOR_DARK_GRAY);
      }

      final Point devGeoGrid = offline_GetDevGridGeoPosition(_worldPixel_TopLeft_Viewport.x, _worldPixel_TopLeft_Viewport.y);
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

   private void paint_GeoGrid_10_Selected(final GC gc, final MapGridData mapGridData) {

      final Point devTopLeft = paint_GeoGrid_50_Outline(gc, mapGridData, true);

      Color fgColor;
      Color bgColor;

      /*
       * Paint label
       */
      if (_geoGrid_Label_IsHovered) {

         // label is hovered

         fgColor = UI.SYS_COLOR_BLACK;
         bgColor = UI.SYS_COLOR_WHITE;

      } else {

         // label is selected

         fgColor = UI.SYS_COLOR_BLACK;
         bgColor = UI.SYS_COLOR_GREEN;
      }

      // draw geo grid label
      _geoGrid_Label_Outline = paint_Text_Label(gc,
            devTopLeft.x,
            devTopLeft.y,
            mapGridData.gridBox_Text,
            fgColor,
            bgColor,
            false);

      /*
       * Paint action
       */
      if (_geoGrid_Label_Outline != null) {

         if (_geoGrid_Action_IsHovered) {

            // action is hovered

            fgColor = UI.SYS_COLOR_BLACK;
            bgColor = UI.SYS_COLOR_WHITE;

         } else {

            // action is selected

            fgColor = UI.SYS_COLOR_BLACK;
            bgColor = UI.SYS_COLOR_GREEN;
         }

         // draw geo grid action
         _geoGrid_Action_Outline = paint_Text_Label(gc,
               _geoGrid_Label_Outline.x + _geoGrid_Label_Outline.width + TEXT_MARGIN / 2,
               _geoGrid_Label_Outline.y + _geoGrid_Label_Outline.height + TEXT_MARGIN / 2,
               GEO_GRID_ACTION_UPDATE_GEO_LOCATION_ZOOM_LEVEL,
               fgColor,
               bgColor,
               false);
      }
   }

   private void paint_GeoGrid_20_Hovered(final GC gc, final MapGridData mapGridData) {

      final Point world_Start = mapGridData.world_Start;
      if (world_Start == null) {
         // map grid is not yet initialized
         return;
      }

      gc.setLineWidth(2);

      /*
       * show info in the top/left corner that selection for the offline area is active
       */
      paint_GeoGrid_70_Info_MouseGeoPos(gc, mapGridData);

      // check if mouse button is hit, this sets the start position
//      if (mapGridData.isSelectionStarted) {
//
//         final Point devTopLeft = paint_GridBox_50_Rectangle(gc, mapGridData, false);
//
//         paint_GridBox_80_Info_Text(gc, mapGridData, devTopLeft);
//      }

      final Point dev_Start = grid_World2Dev(world_Start);
      final Point dev_End = grid_World2Dev(mapGridData.world_End);

      final int dev_Start_X = dev_Start.x;
      final int dev_Start_Y = dev_Start.y;
      final int dev_End_X = dev_End.x;
      final int dev_End_Y = dev_End.y;

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

      final Point devTopLeft = paint_GeoGrid_50_Outline(gc, mapGridData, false);

      paint_Text_WithBorder(gc, mapGridData.gridBox_Text, devTopLeft);

      gc.setLineStyle(SWT.LINE_SOLID);
      gc.setForeground(UI.SYS_COLOR_BLACK);
      gc.drawRectangle(dev_X1, dev_Y1, dev_Width, dev_Height);

      gc.setLineStyle(SWT.LINE_SOLID);
      gc.setForeground(UI.SYS_COLOR_WHITE);

      gc.setBackground(UI.SYS_COLOR_YELLOW);
      gc.setAlpha(0x30);
      gc.fillRectangle(dev_X1 + 1, dev_Y1 + 1, dev_Width - 2, dev_Height - 2);
      gc.setAlpha(0xff);
   }

   /**
    * Paint a rectangle which shows a grid box
    *
    * @param gc
    * @param worldStart
    * @param worldEnd
    * @param mapGridData
    * @param gridPaintData
    * @param isPaintLastGridSelection
    *           When <code>true</code>, the last selected grid is painted, otherwise the currently
    *           selecting grid
    * @return Returns top/left box position in the viewport
    */
   private Point paint_GeoGrid_50_Outline(final GC gc,
                                          final MapGridData mapGridData,
                                          final boolean isPaintLastGridSelection) {

      // x: longitude
      // y: latitude

      // draw geo grid
      final Color boxColor;
      if (isPaintLastGridSelection) {

         final RGB hoverRGB = Util.getStateRGB(_geoFilterState,
               TourGeoFilter_Manager.STATE_RGB_GEO_PARTS_SELECTED,
               TourGeoFilter_Manager.STATE_RGB_GEO_PARTS_SELECTED_DEFAULT);

         boxColor = _colorCache.getColorRGB(hoverRGB);

      } else {

         final RGB hoverRGB = Util.getStateRGB(_geoFilterState,
               TourGeoFilter_Manager.STATE_RGB_GEO_PARTS_HOVER,
               TourGeoFilter_Manager.STATE_RGB_GEO_PARTS_HOVER_DEFAULT);

         boxColor = _colorCache.getColorRGB(hoverRGB);
      }

      final int devGrid_X1 = mapGridData.devGrid_X1;
      final int devGrid_Y1 = mapGridData.devGrid_Y1;
      final int devWidth = mapGridData.devWidth;
      final int devHeight = mapGridData.devHeight;

      gc.setLineStyle(SWT.LINE_SOLID);
      gc.setLineWidth(1);

      // draw outline with selected color
      gc.setForeground(boxColor);
      gc.drawRectangle(devGrid_X1 + 1, devGrid_Y1 + 1, devWidth - 2, devHeight - 2);

      // draw dark outline to make it more visible
      gc.setForeground(UI.SYS_COLOR_BLACK);
      gc.drawRectangle(devGrid_X1, devGrid_Y1, devWidth, devHeight);

      return new Point(devGrid_X1, devGrid_Y1);
   }

   /**
    * @param gc
    * @param mapGridData
    * @param numGridRectangle
    */
   private void paint_GeoGrid_70_Info_MouseGeoPos(final GC gc, final MapGridData mapGridData) {

      gc.setForeground(UI.SYS_COLOR_BLACK);
      gc.setBackground(UI.SYS_COLOR_YELLOW);

      final StringBuilder sb = new StringBuilder();

//      if (mapGridData.isSelectionStarted) {
//
//         // display selected area
//
//         final GeoPosition geoStart = mapGridData.geo_Start;
//         final GeoPosition geoEnd = mapGridData.geo_End;
//
//         sb.append(String.format(" %s / %s  ...  %s / %s", //$NON-NLS-1$
//               _nfLatLon.format(geoStart.latitude),
//               _nfLatLon.format(geoStart.longitude),
//               _nfLatLon.format(geoEnd.latitude),
//               _nfLatLon.format(geoEnd.longitude)));
//
//      } else {
//
      // display mouse move geo position

      sb.append(String.format(" %s / %s", //$NON-NLS-1$
            _nfLatLon.format(_mouseMove_GeoPosition.latitude),
            _nfLatLon.format(_mouseMove_GeoPosition.longitude)));
//      }

      final String infoText = sb.toString();
      final Point textSize = gc.textExtent(infoText);

      gc.drawString(
            infoText,
            _devMapViewport.width - textSize.x,
            0);
   }

   private boolean paint_HoveredTour(final GC gc) {

      Long hoveredTourId = null;
      boolean isTourHoveredAndSelected = false;
      boolean isPaintTourInfo = false;
      boolean isPaintBreadcrumbs = false;

      final int numHoveredTours = _allHoveredDevPoints.size();

      if (numHoveredTours > 0) {

         isPaintTourInfo = true;

         if (numHoveredTours == 1) {

            hoveredTourId = _allHoveredTourIds.get(0);

            if (hoveredTourId == _hovered_SelectedTourId) {
               isTourHoveredAndSelected = true;
            }
         }
      }

      /*
       * Paint selected tour or trackpoint
       */
      if (_hovered_SelectedTourId != -1) {

         if (_hoveredSelectedTour_CanSelectTour) {

            // a tour can be selected

            if (isTourHoveredAndSelected) {

               gc.setAlpha(_hoveredSelectedTour_HoveredAndSelected_Opacity);
               gc.setForeground(_hoveredSelectedTour_HoveredAndSelected_Color);

            } else {

               gc.setAlpha(_hoveredSelectedTour_Selected_Opacity);
               gc.setForeground(_hoveredSelectedTour_Selected_Color);
            }

            paint_HoveredTour_10(gc, _hovered_SelectedTourId);

         } else {

            // a trackpoint can be selected

            paint_HoveredTrackpoint_10(
                  gc,
                  _hovered_SelectedTourId,
                  _hovered_SelectedSerieIndex_Behind,
                  _hovered_SelectedSerieIndex_Front,

                  HoveredPoint_PaintMode.IS_SELECTED);
         }
      }

      /*
       * Paint hovered tour or trackpoint
       */
      if (_isShowHoveredOrSelectedTour) {

         isPaintBreadcrumbs = _isShowBreadcrumbs;

         if (numHoveredTours > 0) {

            isPaintTourInfo = true;

            if (numHoveredTours == 1) {

               if (// paint hovered tour when

               // tour is not selected
               isTourHoveredAndSelected == false

                     // or when a trackpoint is selected
                     || isTourHoveredAndSelected && _hoveredSelectedTour_CanSelectTour == false

               ) {

                  if (_hoveredSelectedTour_CanSelectTour) {

                     // a tour can be selected

                     gc.setAlpha(_hoveredSelectedTour_Hovered_Opacity);
                     gc.setForeground(_hoveredSelectedTour_Hovered_Color);

                     paint_HoveredTour_10(gc, hoveredTourId);

                  } else {

                     // a trackpoint can be selected

                     final int hoveredSerieIndex = _allHoveredSerieIndices.get(0);
                     final boolean isHoveredAndSelected = hoveredSerieIndex == _hovered_SelectedSerieIndex_Behind
                           || hoveredSerieIndex == _hovered_SelectedSerieIndex_Front;

                     paint_HoveredTrackpoint_10(
                           gc,
                           hoveredTourId,
                           hoveredSerieIndex,
                           -1,
                           isHoveredAndSelected
                                 ? HoveredPoint_PaintMode.IS_HOVERED_AND_SELECTED
                                 : HoveredPoint_PaintMode.IS_HOVERED);

                     // paint direction arrows
                     if (_isDrawTourDirection) {

                        final int[] devXYTourPositions = getReducesTourPositions(hoveredTourId);

                        if (devXYTourPositions != null) {
                           paint_HoveredTour_14_DirectionArrows(gc, devXYTourPositions);
                        }
                     }
                  }
               }
            }
         }
      }

      /*
       * Paint tour directions when not yet painted
       */
      if (_isShowTour

            && _isDrawTourDirection

            && _isDrawTourDirection_Always

            // currently only one tour is supported
            && _allTourIds != null && _allTourIds.size() == 1

            // nothing is currently hovered
            && numHoveredTours == 0

      ) {

         final Long tourId = _allTourIds.get(0);

         final int[] devXYTourPositions = getReducesTourPositions(tourId);

         if (devXYTourPositions != null) {
            paint_HoveredTour_14_DirectionArrows(gc, devXYTourPositions);
         }
      }

      /*
       * Paint breadcrumb bar
       */
      final boolean isEnhancedPaintingMethod = isPaintTile_With_BasicMethod() == false;
      final boolean isShowTourPaintMethodEnhancedWarning = isEnhancedPaintingMethod && _isShowTourPaintMethodEnhancedWarning;

      if (isPaintBreadcrumbs || isShowTourPaintMethodEnhancedWarning) {

         _tourBreadcrumb.paint(gc, isPaintBreadcrumbs, isShowTourPaintMethodEnhancedWarning);
      }

      return isPaintTourInfo;
   }

   private void paint_HoveredTour_10(final GC gc, final long tourId) {

      int[] devXYTourPositions;

      gc.setLineWidth(30);

      gc.setLineCap(SWT.CAP_ROUND);
      gc.setLineJoin(SWT.JOIN_ROUND);

      gc.setAntialias(SWT.ON);
      {
         devXYTourPositions = getReducesTourPositions(tourId);
         if (devXYTourPositions != null) {
            gc.drawPolyline(devXYTourPositions);
         }
      }
      gc.setAntialias(SWT.OFF);
      gc.setAlpha(0xff);

      if (_isDrawTourDirection && devXYTourPositions != null) {
         paint_HoveredTour_14_DirectionArrows(gc, devXYTourPositions);
      }
   }

   private void paint_HoveredTour_14_DirectionArrows(final GC gc, final int[] devXY) {

      int devX1 = devXY[0];
      int devY1 = devXY[1];

      int devX1_LastPainted = devX1;
      int devY1_LastPainted = devY1;

      final int numSegments = devXY.length / 2;

      gc.setLineWidth(_tourDirection_LineWidth);
      gc.setAntialias(SWT.ON);

      gc.setLineCap(SWT.CAP_SQUARE);
      gc.setLineJoin(SWT.JOIN_MITER);

      final Color directionColor_Symbol = new Color(_tourDirection_RGB);
      final Color directionColor_Contrast = UI.SYS_COLOR_WHITE;

      final Path directionPath_Color = new Path(_display);
      final Path directionPath_Contrast = new Path(_display);
      final Transform transform = new Transform(_display);
      {
         // draw direction symbol
         final float directionPos1 = 1 * _tourDirection_SymbolSize;
         final float directionPos2 = 0.8f * _tourDirection_SymbolSize;

         directionPath_Color.moveTo(0, directionPos1);
         directionPath_Color.lineTo(directionPos2, 0);
         directionPath_Color.lineTo(0, -directionPos1);

         directionPath_Contrast.moveTo(-1, directionPos1);
         directionPath_Contrast.lineTo(directionPos2 - 1, 0);
         directionPath_Contrast.lineTo(-1, -directionPos1);

         for (int segmentIndex = 1; segmentIndex < numSegments; segmentIndex++) {

            final int devXYIndex = segmentIndex * 2;

            final int devX2 = devXY[devXYIndex + 0];
            final int devY2 = devXY[devXYIndex + 1];

            /*
             * Skip locations which are too narrow
             */
            int xDiff;
            int yDiff;

            if (devX1_LastPainted > devX2) {
               xDiff = devX1_LastPainted - devX2;
            } else {
               xDiff = devX2 - devX1_LastPainted;
            }

            if (devY1_LastPainted > devY2) {
               yDiff = devY1_LastPainted - devY2;
            } else {
               yDiff = devY2 - devY1_LastPainted;
            }

            if (xDiff > _tourDirection_MarkerGap || yDiff > _tourDirection_MarkerGap

            // paint 1st direction arrow
                  || segmentIndex == 1) {

               // paint direction arrow

               final float directionRotation = (float) MtMath.angleOf(devX1, devY1, devX2, devY2);

// when debugging then autoScapeUp provided the wrong values when using a 4k display !!!
//               final int xPos1 = DPIUtil.autoScaleUp(devX1);
//               final int yPos1 = DPIUtil.autoScaleUp(devY1);

               final int xPos1 = devX1;
               final int yPos1 = devY1;

               // VERY IMPORTANT: Reset previous positions !!!
               transform.identity();

               transform.translate(xPos1, yPos1);
               transform.rotate(-directionRotation);

               gc.setTransform(transform);

               gc.setForeground(directionColor_Contrast);
               gc.drawPath(directionPath_Contrast);

               gc.setForeground(directionColor_Symbol);
               gc.drawPath(directionPath_Color);

               // keep last painted position
               devX1_LastPainted = devX1;
               devY1_LastPainted = devY1;
            }

            // advance to the next segment
            devX1 = devX2;
            devY1 = devY2;
         }
      }
      directionPath_Color.dispose();
      directionPath_Contrast.dispose();
      transform.dispose();

      gc.setTransform(null);
   }

   /**
    * Show number of tours, e.g. "Tours 21" when tours are hovered
    *
    * @param gc
    */
   private void paint_HoveredTour_50_TourInfo(final GC gc) {

      /*
       * This is for debugging
       */
//      final boolean isShowHoverRectangle = true;
//      if (isShowHoverRectangle) {
//
//         // paint hovered rectangle
//         gc.setLineWidth(1);
//
//         for (final Point hoveredPoint : _allDevHoveredPoints) {
//
//            gc.setAlpha(0x60);
//            gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
//            gc.fillRectangle(
//                  hoveredPoint.x,
//                  hoveredPoint.y,
//                  EXPANDED_HOVER_SIZE,
//                  EXPANDED_HOVER_SIZE);
//
//            gc.setAlpha(0xff);
//            gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
//            gc.drawRectangle(
//                  hoveredPoint.x,
//                  hoveredPoint.y,
//                  EXPANDED_HOVER_SIZE,
//                  EXPANDED_HOVER_SIZE);
//         }
//      }

      final int devXMouse = _mouseMove_DevPosition_X;
      final int devYMouse = _mouseMove_DevPosition_Y;

      final int numHoveredTours = _allHoveredTourIds.size();
      final int numDisplayedTours = _allTourIds != null
            ? _allTourIds.size()
            : 0;

      if (numHoveredTours == 1) {

         // one tour is hovered -> show tour details

         if (numDisplayedTours > 1) {

            // multiple tours are displayed

            final TourData tourData = TourManager.getTour(_allHoveredTourIds.get(0));

            if (tourData == null) {

               // this occurred, it can be that previously a history/multiple tour was displayed

            } else {

               // tour data are available

               paint_HoveredTour_52_OneTour(gc, devXMouse, devYMouse, tourData, _allHoveredSerieIndices.get(0));
            }

         } else {

            // just one tour is displayed

            // ** tour track info can be displayed in the value point tooltip
            // ** tour info can be displayed with the tour info tooltip
         }

      } else {

         // multiple tours are hovered -> show number of hovered tours

         paint_HoveredTour_54_MultipleTours(gc, devXMouse, devYMouse, _allHoveredTourIds);
      }
   }

   private void paint_HoveredTour_52_OneTour(final GC gc,
                                             final int devXMouse,
                                             final int devYMouse,
                                             final TourData tourData,
                                             final int hoveredSerieIndex) {

      final Font normalFont = gc.getFont();

      final String text_TourDateTime = TourManager.getTourDateTimeFull(tourData);
      final String text_TourTitle = tourData.getTourTitle();
      final boolean isTourTitle = text_TourTitle.length() > 0;

      final long movingTime = tourData.getTourComputedTime_Moving();
      final long recordedTime = tourData.getTourDeviceTime_Recorded();
      final float distance = tourData.getTourDistance() / UI.UNIT_VALUE_DISTANCE;
      final int elevationUp = tourData.getTourAltUp();

      /*
       * Tour values
       */
      final String text_TourMovingTime = String.format(Messages.Map2_TourTooltip_Time,
            OtherMessages.TOUR_TOOLTIP_LABEL_MOVING_TIME,
            FormatManager.formatMovingTime(movingTime));

      final String text_TourRecordedTime = String.format(Messages.Map2_TourTooltip_Time,
            OtherMessages.TOUR_TOOLTIP_LABEL_RECORDED_TIME,
            FormatManager.formatMovingTime(recordedTime));

      final String text_TourDistance = String.format(Messages.Map2_TourTooltip_Distance,
            OtherMessages.TOUR_TOOLTIP_LABEL_DISTANCE,
            FormatManager.formatDistance(distance / 1000.0),
            UI.UNIT_LABEL_DISTANCE);

      final String text_ElevationUp = String.format(Messages.Map2_TourTooltip_Elevation,
            OtherMessages.TOUR_TOOLTIP_LABEL_ELEVATION_UP,
            FormatManager.formatElevation(elevationUp),
            UI.UNIT_LABEL_ELEVATION);

      /*
       * Track values
       */

      // trackpoint

      final StringBuilder sb = new StringBuilder();

      sb.append(NL);
      sb.append(text_TourDateTime + NL);
      sb.append(NL);
      sb.append(text_TourDistance + NL);
      sb.append(text_ElevationUp + NL);
      sb.append(text_TourMovingTime + NL);
      sb.append(text_TourRecordedTime);

      final Point size_DateTime = gc.textExtent(text_TourDateTime);
      final Point size_Values = gc.textExtent(sb.toString());

      Point size_Title = new Point(0, 0);
      String wrappedTitle = null;
      int titleHeight = 0;

      if (isTourTitle) {

         wrappedTitle = WordUtils.wrap(text_TourTitle, 40);

         gc.setFont(_boldFont);
         size_Title = gc.textExtent(wrappedTitle);

         titleHeight = size_Title.y;
      }

      final int lineHeight = size_DateTime.y;

      final int marginHorizontal = 3;
      final int marginVertical = 1;

      final int contentWidth = Math.max(Math.max(size_DateTime.x, size_Values.x), size_Title.x);
      final int contentHeight = 0
            + (isTourTitle ? titleHeight : -lineHeight)
            + size_Values.y;

      final int detailWidth = contentWidth + marginHorizontal * 2;
      final int detailHeight = contentHeight + marginVertical * 2;

      final int marginAboveMouse = 50;
      final int marginBelowMouse = 40;

      int devXDetail = devXMouse + 20;
      int devYDetail = devYMouse - marginAboveMouse;

      // ensure that the tour detail is fully visible
      final int viewportWidth = _devMapViewport.width;
      if (devXDetail + detailWidth > viewportWidth) {
         devXDetail = viewportWidth - detailWidth;
      }
      if (devYDetail - detailHeight < 0) {
         devYDetail = devYMouse + detailHeight + marginBelowMouse;
      }

      final Rectangle clippingRect = new Rectangle(
            devXDetail,
            devYDetail - detailHeight,
            detailWidth,
            detailHeight);

      gc.setClipping(clippingRect);

      gc.setBackground(ThemeUtil.getDefaultBackgroundColor_Shell());
      gc.fillRectangle(clippingRect);

      gc.setForeground(ThemeUtil.getDefaultForegroundColor_Shell());

      final int devX = devXDetail + marginHorizontal;
      int devY = devYDetail - contentHeight - marginVertical;

      if (isTourTitle) {

         gc.setFont(_boldFont);
         gc.drawText(wrappedTitle, devX, devY);

         devY += titleHeight;

      } else {

         devY -= lineHeight;
      }

      gc.setFont(normalFont);
      gc.drawText(sb.toString(), devX, devY);
   }

   private void paint_HoveredTour_54_MultipleTours(final GC gc,
                                                   final int devXMouse,
                                                   final int devYMouse,
                                                   final List<Long> allHoveredTourIds) {

      final StringBuilder sb = new StringBuilder();

      // Show number of hovered tours
      sb.append(String.format("%d %s", //$NON-NLS-1$
            allHoveredTourIds.size(),
            Messages.Map2_Hovered_Tours));
      sb.append(NL);

      for (final Long tourId : allHoveredTourIds) {

         final TourData tourData = TourManager.getTour(tourId);
         if (tourData == null) {
            continue;
         }

         final ZonedDateTime tourStartTime = tourData.getTourStartTime();
         final long movingTime = tourData.getTourComputedTime_Moving();
         final float distance = tourData.getTourDistance() / UI.UNIT_VALUE_DISTANCE;
         final int elevationUp = tourData.getTourAltUp();

         final String text_TourDateTime = String.format("%s\t%s", //$NON-NLS-1$
               tourStartTime.format(TimeTools.Formatter_DateTime_S),
               tourStartTime.format(TimeTools.Formatter_Weekday));

         final String text_TourMovingTime = FormatManager.formatMovingTime(movingTime);
         final String text_TourDistance = String.format("%s %s", //$NON-NLS-1$
               FormatManager.formatDistance(distance / 1000.0),
               UI.UNIT_LABEL_DISTANCE);

         final String text_ElevationUp = String.format("%s %s", //$NON-NLS-1$
               FormatManager.formatElevation(elevationUp),
               UI.UNIT_LABEL_ELEVATION);
         /*
          * Combine tour values
          */
         sb.append(NL);
         sb.append(String.format("%s\t%s\t%8s\t%10s", //$NON-NLS-1$
               text_TourDateTime,
               text_TourMovingTime,
               text_TourDistance,
               text_ElevationUp));

      }

      final Point contentSize = gc.textExtent(sb.toString());

      final int marginHorizontal = 3;
      final int marginVertical = 1;

      final int contentWidth = contentSize.x;
      final int contentHeight = contentSize.y;

      final int tooltipWidth = contentWidth + marginHorizontal * 2;
      final int tooltipHeight = contentHeight + marginVertical * 2;

      final int marginAroundMouse = 20;

      int devXTooltip = devXMouse + marginAroundMouse;
      int devYTooltip = devYMouse - marginAroundMouse;

      /*
       * Ensure that the tour detail is fully visible
       */
      final int viewportWidth = _devMapViewport.width;
      final int viewportHeight = _devMapViewport.height;

      if (devXTooltip + tooltipWidth > viewportWidth) {

         // tooltip is truncated at the right side -> move tooltip to the left of the mouse

         devXTooltip = devXMouse - marginAroundMouse - tooltipWidth;
      }

      if (devYTooltip - tooltipHeight < 0) {

         // tooltip is truncated at the top -> move tooltip below the mouse

         devYTooltip = devYMouse + tooltipHeight + marginAroundMouse;

         if (devYTooltip + tooltipHeight > viewportHeight) {

            // tooltip is truncated at the bottom -> snap to the top

            devYTooltip = tooltipHeight;
         }
      }

      final int devX = devXTooltip + marginHorizontal;
      final int devY = devYTooltip - contentHeight - marginVertical;

      /*
       * Paint tooltip
       */
      final Rectangle clippingRect = new Rectangle(
            devXTooltip,
            devYTooltip - tooltipHeight,
            tooltipWidth,
            tooltipHeight);

      gc.setClipping(clippingRect);

      gc.setBackground(ThemeUtil.getDefaultBackgroundColor_Shell());
      gc.fillRectangle(clippingRect);

      gc.setForeground(ThemeUtil.getDefaultForegroundColor_Shell());
      gc.drawText(sb.toString(), devX, devY);
   }

   private void paint_HoveredTrackpoint_10(final GC gc,
                                           final long tourId,
                                           final int serieIndex1,
                                           final int serieIndex2,
                                           final HoveredPoint_PaintMode valuePoint_PaintMode) {

      final TourData tourData = TourManager.getTour(tourId);

      if (tourData == null) {

         // this occurred, it can be that previously a history/multiple tour was displayed
         return;
      }

      gc.setAntialias(SWT.ON);

      final MP mp = getMapProvider();
      final int zoomLevel = getZoom();

      final double[] latitudeSerie = tourData.latitudeSerie;
      final double[] longitudeSerie = tourData.longitudeSerie;

      final Rectangle worldPosition_Viewport = _worldPixel_TopLeft_Viewport;

      final int dotWidth = 30;
      final int dotWidth2 = dotWidth / 2;

      int serieIndexFront;
      int serieIndexBehind;
      if (serieIndex1 > serieIndex2) {
         serieIndexFront = serieIndex1;
         serieIndexBehind = serieIndex2;
      } else {
         serieIndexFront = serieIndex2;
         serieIndexBehind = serieIndex1;
      }

      // loop: paint 2 points
      for (int paintIndex = 0; paintIndex < 2; paintIndex++) {

         final int serieIndex;
         boolean isFirstIndex = false;

         if (paintIndex == 0) {
            if (serieIndexFront == -1) {
               continue;
            } else {
               serieIndex = serieIndexFront;
            }
            isFirstIndex = true;
         } else {
            if (serieIndexBehind == -1) {
               continue;
            } else {
               serieIndex = serieIndexBehind;
            }
         }

         // fix: java.lang.ArrayIndexOutOfBoundsException: Index 15091 out of bounds for length 8244
         // this could not be reproduced, it happened when 2 tours were displayed/selected after a geo search
         if (serieIndex >= latitudeSerie.length) {
            continue;
         }

         // get world position for the current lat/lon
         final java.awt.Point worldPosAWT = mp.geoToPixel(
               new GeoPosition(latitudeSerie[serieIndex], longitudeSerie[serieIndex]),
               zoomLevel);

         // convert world position into device position
         final int devX = worldPosAWT.x - worldPosition_Viewport.x;
         final int devY = worldPosAWT.y - worldPosition_Viewport.y;

         /*
          * Paint colored margin around a point
          */
         gc.setAlpha(_hoveredSelectedTour_Hovered_Opacity);
         gc.setBackground(_hoveredSelectedTour_Hovered_Color);

         if (_prefOptions_IsDrawSquare) {

            gc.fillRectangle(
                  devX - dotWidth2,
                  devY - dotWidth2,
                  dotWidth,
                  dotWidth);
         } else {

            gc.fillOval(
                  devX - dotWidth2,
                  devY - dotWidth2,
                  dotWidth,
                  dotWidth);
         }

         // paint original symbol but with a more contrast color

         /*
          * Fill symbol
          */
         gc.setAlpha(0xff);

         int symbolSize = _prefOptions_LineWidth;
         int symbolSize2 = symbolSize / 2;

         int paintedDevX = devX - symbolSize2;
         int paintedDevY = devY - symbolSize2;

         if (valuePoint_PaintMode.equals(HoveredPoint_PaintMode.IS_SELECTED)) {

            // paint selected

            gc.setBackground(isFirstIndex
                  ? UI.SYS_COLOR_GREEN
                  : UI.SYS_COLOR_DARK_GREEN);

         } else if (valuePoint_PaintMode.equals(HoveredPoint_PaintMode.IS_HOVERED_AND_SELECTED)) {

            gc.setBackground(UI.SYS_COLOR_RED);

         } else {

            // paint hovered

            gc.setBackground(UI.SYS_COLOR_WHITE);
         }

         if (_prefOptions_IsDrawSquare) {
            gc.fillRectangle(paintedDevX, paintedDevY, symbolSize, symbolSize);
         } else {
            gc.fillOval(paintedDevX, paintedDevY, symbolSize, symbolSize);
         }

         /*
          * Draw symbol border
          */
         gc.setLineWidth(_prefOptions_BorderWidth);

         symbolSize = _prefOptions_LineWidth + (_prefOptions_BorderWidth * 1);
         symbolSize2 = symbolSize / 2;

         // without this offset the border is not at the correct position
         final int oddOffset = symbolSize % 2 == 0 ? 0 : 1;

         paintedDevX = devX - symbolSize2 - oddOffset;
         paintedDevY = devY - symbolSize2 - oddOffset;

         gc.setForeground(UI.SYS_COLOR_BLACK);

         if (_prefOptions_IsDrawSquare) {
            gc.drawRectangle(paintedDevX, paintedDevY, symbolSize, symbolSize);
         } else {
            gc.drawOval(paintedDevX, paintedDevY, symbolSize, symbolSize);
         }
      }

      gc.setAntialias(SWT.OFF);
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
//         gc.setForeground(UI.SYS_COLOR_WHITE);
//         gc.drawRectangle(_offline_PreviousOfflineArea);
//
//         final int devX = _offline_PreviousOfflineArea.x;
//         final int devY = _offline_PreviousOfflineArea.y;
//         gc.setForeground(UI.SYS_COLOR_GRAY);
//         gc.drawRectangle(
//               devX + 1,
//               devY + 1,
//               _offline_PreviousOfflineArea.width - 2,
//               _offline_PreviousOfflineArea.height - 2);
//
//         /*
//          * draw text marker
//          */
//         gc.setForeground(UI.SYS_COLOR_BLACK);
//         gc.setBackground(UI.SYS_COLOR_WHITE);
//         final Point textExtend = gc.textExtent(Messages.Offline_Area_Label_OldAreaMarker);
//         int devYMarker = devY - textExtend.y;
//         devYMarker = devYMarker < 0 ? 0 : devYMarker;
//         gc.drawText(Messages.Offline_Area_Label_OldAreaMarker, devX, devYMarker);
//      }

      /*
       * show info in the top/right corner that selection for the offline area is active
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
            gc.setForeground(UI.SYS_COLOR_DARK_GRAY);
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
      gc.setForeground(UI.SYS_COLOR_BLACK);
      gc.drawRectangle(devArea_X1, devArea_Y1, devArea_Width, devArea_Height);

      gc.setLineStyle(SWT.LINE_SOLID);
      gc.setForeground(UI.SYS_COLOR_WHITE);

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

      gc.setForeground(UI.SYS_COLOR_BLACK);
      gc.setBackground(UI.SYS_COLOR_WHITE);
      gc.drawText(Messages.Offline_Area_Label_AreaMarker, devArea_X1, devYMarker);
   }

   private void paint_OfflineArea_10_Info(final GC gc) {

      gc.setForeground(UI.SYS_COLOR_BLACK);
      gc.setBackground(UI.SYS_COLOR_YELLOW);

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
   private void paint_Overlay_0_SetupThread() {

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
                        paint_Overlay_10_RunThread();
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

   private void paint_Overlay_10_RunThread() {

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

               paint_Overlay_20_Tiles();

            } catch (final Exception e) {
               e.printStackTrace();
            } finally {
               _isRunningDrawOverlay = false;
            }
         }
      };

      _display.asyncExec(uiOverlayRunnable);
   }

   private void paint_Overlay_20_Tiles() {

      BusyIndicator.showWhile(_display, () -> {

         Tile tile;

         checkImageTemplate9Parts();

         final long startTime = System.currentTimeMillis();

         while ((tile = _tileOverlayPaintQueue.poll()) != null) {

            // skip tiles from another zoom level
            if (tile.getZoom() == _mapZoomLevel) {

               // set state that this tile is checked
               tile.setOverlayTourStatus(OverlayTourState.TILE_IS_CHECKED);

               // cleanup previous positions
               tile.allPainted_Hash.clear();
               tile.allPainted_HoverRectangle.clear();
               tile.allPainted_HoverSerieIndices.clear();
               tile.allPainted_HoverTourID.clear();

               /*
                * Check if a tour, marker or photo is within the current tile
                */
               boolean isPaintingNeeded = false;

               for (final Map2Painter mapPainter : _allMapPainter) {

                  isPaintingNeeded = mapPainter.isPaintingNeeded(Map2.this, tile);

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
               if (isPaintTile_With_BasicMethod()) {
                  paint_Overlay_22_PaintTileBasic(tile);
               } else {
                  paint_Overlay_30_PaintTileEnhanced(tile);
               }

               // allow to display painted overlays
               final long paintTime = System.currentTimeMillis();
               if (paintTime > startTime + 500) {
                  break;
               }

            } else {

               // tile has a different zoom level, ignore this tile
               tile.setOverlayTourStatus(OverlayTourState.TILE_IS_NOT_CHECKED);
            }
         }
      });
   }

   /**
    * Paint the tour in basic mode
    *
    * @param tile
    */
   private void paint_Overlay_22_PaintTileBasic(final Tile tile) {

      boolean isOverlayPainted = false;

      // create 1 part image/gc
      final ImageData transparentImageData = de.byteholder.geoclipse.map.UI.createTransparentImageData(_tilePixelSize);

      final Image overlayImage = new Image(_display, transparentImageData);
      final GC gc1Part = new GC(overlayImage);

      /*
       * Ubuntu 12.04 fails, when background is not filled, it draws a black background
       */
      gc1Part.setBackground(_transparentColor);
      gc1Part.fillRectangle(overlayImage.getBounds());

      {
         // paint all overlays for the current tile
         for (final Map2Painter overlayPainter : _allMapPainter) {

            final boolean isPainted = overlayPainter.doPaint(
                  gc1Part,
                  Map2.this,
                  tile,
                  1,
                  _isFastMapPainting && _isFastMapPainting_Active,
                  _fastMapPainting_skippedValues);

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
   private void paint_Overlay_30_PaintTileEnhanced(final Tile tile) {

      final int parts = 3;

      boolean isOverlayPainted = false;

      {
         // clear 9 part image
         _9PartGC.setBackground(_transparentColor);
         _9PartGC.fillRectangle(_9PartImage.getBounds());

         // paint all overlays for the current tile
         for (final Map2Painter overlayPainter : _allMapPainter) {

            final boolean isPainted = overlayPainter.doPaint(
                  _9PartGC,
                  Map2.this,
                  tile,
                  parts,
                  _isFastMapPainting && _isFastMapPainting_Active,
                  _fastMapPainting_skippedValues);

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

                  paint_Overlay_31_SplitParts(tile, imageData9Parts);

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
   private void paint_Overlay_31_SplitParts(final Tile tile, final ImageData imageData9Parts) {

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
               idResources.drawTileImageData(
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
               neighborIDResources.drawNeighborImageData(
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

   /**
    * Draw border around text to make it more visible
    */
   private void paint_Text_Border(final GC gc, final String text, final int devX, final int devY) {

      gc.setForeground(UI.SYS_COLOR_WHITE);

      gc.drawString(text, devX + 1, devY + 1, true);
      gc.drawString(text, devX - 1, devY - 1, true);

      gc.drawString(text, devX + 1, devY - 1, true);
      gc.drawString(text, devX - 1, devY + 1, true);

      gc.drawString(text, devX + 1, devY, true);
      gc.drawString(text, devX - 1, devY, true);

      gc.drawString(text, devX, devY + 1, true);
      gc.drawString(text, devX, devY - 1, true);
   }

   /**
    * @param gc
    * @param devXMouse
    * @param devYMouse
    * @param text
    * @param fgColor
    * @param bgColor
    * @param isAdjustToMousePosition
    * @return Returns outline of the painted text or <code>null</code> when text is not painted
    */
   private Rectangle paint_Text_Label(final GC gc,
                                      final int devXMouse,
                                      final int devYMouse,
                                      final String text,
                                      final Color fgColor,
                                      final Color bgColor,
                                      final boolean isAdjustToMousePosition) {

      if (text == null) {
         return null;
      }

      final int textMargin = TEXT_MARGIN;
      final int textMargin2 = textMargin / 2;

      final Point textSize = gc.stringExtent(text);
      final int textWidth = textSize.x;
      final int textHeight = textSize.y;

      final int contentWidth = textWidth + textMargin;
      final int contentHeight = textHeight + textMargin;

      int devXDetail = devXMouse;
      int devYDetail = devYMouse;

      // ensure that the tour detail is fully visible
      final int viewportWidth = _devMapViewport.width;
      final int viewportHeight = _devMapViewport.height;

      if (isAdjustToMousePosition) {

         final int mouseHeight = 24;

         if (devXDetail + textWidth + textMargin > viewportWidth) {
            devXDetail = viewportWidth - textWidth - textMargin;
         }

         if (devYDetail - textHeight - textMargin - mouseHeight < 0) {
            devYDetail = devYMouse + textHeight + textMargin + mouseHeight;
         }

      } else {

         // make sure that the text is fully visible

         // left border
         if (devXDetail < 0) {
            devXDetail = 0;
         }

         // right border
         if (devXDetail + contentWidth > viewportWidth) {
            devXDetail = viewportWidth - contentWidth;
         }

         // top border
         if (devYDetail - contentHeight < 0) {
            devYDetail = contentHeight;
         }

         // bottom border
         if (devYDetail > viewportHeight) {
            devYDetail = viewportHeight;
         }
      }

      final int devYText = devYDetail - textHeight - textMargin2;

      final Rectangle textOutline = new Rectangle(devXDetail,
            devYText - textMargin,
            textWidth + textMargin,
            textHeight + textMargin);

      gc.setAlpha(0xff);

      gc.setBackground(bgColor);
      gc.fillRectangle(textOutline);

      gc.setForeground(fgColor);
      gc.drawString(
            text,
            devXDetail + textMargin2,
            devYText - textMargin2,
            true);

      return textOutline;
   }

   private void paint_Text_WithBorder(final GC gc, final String text, final Point topLeft) {

      if (text == null) {
         return;
      }

      final Point textSize = gc.stringExtent(text);

      final int devX = topLeft.x;
      final int devY = topLeft.y - textSize.y - 5;

      paint_Text_Border(gc, text, devX, devY);

      /*
       * Draw text
       */
      gc.setForeground(UI.SYS_COLOR_BLACK);
      gc.drawString(text, devX, devY, true);
   }

   private Tile paint_Tile(final GC gcMapImage,
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
         paint_Tile_10_Image(gcMapImage, tile, devTileViewport);
      }

      if (_isDrawOverlays) {

         gcMapImage.setAlpha(_overlayAlpha);

         paint_Tile_20_Overlay(gcMapImage, tile, devTileViewport);

         gcMapImage.setAlpha(0xff);
      }

      if (_isShowDebug_TileInfo || _isShowDebug_TileBorder) {
         paint_Tile_30_Info(gcMapImage, tile, devTileViewport);
      }

      return tile;
   }

   /**
    * draw the tile map image
    */
   private void paint_Tile_10_Image(final GC gcMapImage, final Tile tile, final Rectangle devTileViewport) {

      if (tile.isLoadingError()) {

         // map image contains an error, it could not be loaded

         final Image errorImage = _mp.getErrorImage();
         final Rectangle imageBounds = errorImage.getBounds();

         gcMapImage.setBackground(UI.SYS_COLOR_GRAY);
         gcMapImage.fillRectangle(devTileViewport.x, devTileViewport.y, imageBounds.width, imageBounds.height);

         paint_TileInfo_Error(gcMapImage, devTileViewport, tile);

         return;
      }

      if (tile.isOfflineError()) {

         //map image could not be loaded from offline file

         gcMapImage.drawImage(_mp.getErrorImage(), devTileViewport.x, devTileViewport.y);

         paint_TileInfo_Error(gcMapImage, devTileViewport, tile);

         return;
      }

      /*
       * the tile image is not yet loaded, register an observer that handles redrawing when the tile
       * image is available. Tile image loading is started, when the tile is retrieved from the tile
       * factory which is done in drawTile()
       */
      tile.setImageLoaderCallback(_tileImageLoaderCallback);

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
   private void paint_Tile_20_Overlay(final GC gcMapImage, final Tile tile, final Rectangle devTileViewport) {

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
   private void paint_Tile_30_Info(final GC gc, final Tile tile, final Rectangle devTileViewport) {

      final ConcurrentHashMap<String, Tile> childrenWithErrors = tile.getChildrenWithErrors();

      if (tile.isLoadingError()
            || tile.isOfflineError()
            || ((childrenWithErrors != null) && (childrenWithErrors.size() > 0))) {

         paint_TileInfo_Error(gc, devTileViewport, tile);

         return;
      }

      if (_isShowDebug_TileBorder) {

         // draw tile border
         gc.setForeground(_display.getSystemColor(SWT.COLOR_YELLOW));
         gc.drawRectangle(devTileViewport.x, devTileViewport.y, _tilePixelSize, _tilePixelSize);
      }

      if (_isShowDebug_TileInfo) {

         // draw tile info
         gc.setForeground(UI.SYS_COLOR_WHITE);
         gc.setBackground(_display.getSystemColor(SWT.COLOR_DARK_BLUE));

         final int leftMargin = 10;

         paint_TileInfo_LatLon(gc, tile, devTileViewport, 10, leftMargin);
         paint_TileInfo_Position(gc, devTileViewport, tile, 50, leftMargin);

         // draw tile image path/url
         final StringBuilder sb = new StringBuilder();

         paint_TileInfo_Path(tile, sb);

         _textWrapper.printText(
               gc,
               sb.toString(),
               devTileViewport.x + leftMargin,
               devTileViewport.y + 80,
               devTileViewport.width - 20);
      }
   }

   private void paint_TileInfo_Error(final GC gc, final Rectangle devTileViewport, final Tile tile) {

      // draw tile border
      gc.setForeground(UI.SYS_COLOR_DARK_GRAY);
      gc.drawRectangle(devTileViewport.x, devTileViewport.y, _tilePixelSize, _tilePixelSize);

      gc.setForeground(UI.SYS_COLOR_WHITE);
      gc.setBackground(_display.getSystemColor(SWT.COLOR_DARK_MAGENTA));

      final int leftMargin = 10;

      paint_TileInfo_LatLon(gc, tile, devTileViewport, 10, leftMargin);
      paint_TileInfo_Position(gc, devTileViewport, tile, 50, leftMargin);

      // display loading error
      final StringBuilder sb = new StringBuilder();

      final String loadingError = tile.getLoadingError();
      if (loadingError != null) {
         sb.append(loadingError);
         sb.append(NL);
         sb.append(NL);
      }

      final ConcurrentHashMap<String, Tile> childrenLoadingError = tile.getChildrenWithErrors();
      if ((childrenLoadingError != null) && (childrenLoadingError.size() > 0)) {

         for (final Tile childTile : childrenLoadingError.values()) {
            sb.append(childTile.getLoadingError());
            sb.append(NL);
            sb.append(NL);
         }
      }

      paint_TileInfo_Path(tile, sb);

      final ArrayList<Tile> tileChildren = tile.getChildren();
      if (tileChildren != null) {
         for (final Tile childTile : tileChildren) {
            paint_TileInfo_Path(childTile, sb);
         }
      }

      _textWrapper.printText(
            gc,
            sb.toString(),
            devTileViewport.x + leftMargin,
            devTileViewport.y + 80,
            devTileViewport.width - 20);
   }

   private void paint_TileInfo_LatLon(final GC gc,
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
   private void paint_TileInfo_Path(final Tile tile, final StringBuilder sb) {

      final String url = tile.getUrl();
      if (url != null) {
         sb.append(url);
         sb.append(NL);
         sb.append(NL);
      }

      final String offlinePath = tile.getOfflinePath();
      if (offlinePath != null) {
         sb.append(offlinePath);
         sb.append(NL);
         sb.append(NL);
      }

      final ArrayList<Tile> tileChildren = tile.getChildren();
      if (tileChildren != null) {
         for (final Tile childTile : tileChildren) {
            paint_TileInfo_Path(childTile, sb);
         }
      }
   }

   private void paint_TileInfo_Position(final GC gc,
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

      final double oldCenterX = _worldPixel_MapCenter.getX();
      final double oldCenterY = _worldPixel_MapCenter.getY();

      final double newCenterX = oldCenterX - mapDiffX;
      final double newCenterY = oldCenterY - mapDiffY;

      _mouseDownPosition = movePosition;
      _isMapPanned = true;

      // set new map center
      setMapCenterInWorldPixel(new Point2D.Double(newCenterX, newCenterY));
      updateViewPortData();

      paint();

      fireEvent_MapPosition(false);
   }

   /**
    * @param text
    * @return Returns <code>true</code> when POI could be identified and it's displayed in the map
    */
   private boolean parseAndDisplayPOIText(String text) {

      try {
         text = URLDecoder.decode(text, UI.UTF_8);
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
// type                                               ratio           m / pixel      {scale}    {mmscale} {span}  {altitude}   {zoom}   {osmzoom}
//
// country, satellite                                 1 : 10,000,000    3528        10000000    10000000    10.0        1430        1           5
// state                                              1 : 3,000,000     1058         3000000     4000000     3.0         429        3           7
// adm1st                                             1 : 1,000,000      353         1000000     1000000     1.0         143        4           9
// adm2nd (default)                                   1 : 300,000        106          300000      200000     0.3          42        5          11
// adm3rd, city, mountain, isle, river, waterbody     1 : 100,000         35.3        100000      100000     0.1          14        6          12
// event, forest, glacier                             1 : 50,000          17.6         50000       50000     0.05          7        7          13
// airport                                            1 : 30,000          10.6         30000       25000     0.03          4        7          14
// edu, pass, landmark, railwaystation                1 : 10,000          3.53         10000       10000     0.01          1        8          15

                  if (type.equals("country") //                   //$NON-NLS-1$
                        || type.equals("satellite")) { //         //$NON-NLS-1$

                     zoom = 5 - 1;

                  } else if (type.equals("state")) { //           //$NON-NLS-1$

                     zoom = 7 - 1;

                  } else if (type.equals("adm1st")) { //          //$NON-NLS-1$

                     zoom = 9 - 1;

                  } else if (type.equals("adm2nd")) { //          //$NON-NLS-1$

                     zoom = 11 - 1;

                  } else if (type.equals("adm3rd") //             //$NON-NLS-1$
                        || type.equals("city") //                 //$NON-NLS-1$
                        || type.equals("mountain") //             //$NON-NLS-1$
                        || type.equals("isle") //                 //$NON-NLS-1$
                        || type.equals("river") //                //$NON-NLS-1$
                        || type.equals("waterbody")) { //         //$NON-NLS-1$

                     zoom = 12 - 1;

                  } else if (type.equals("event")//               //$NON-NLS-1$
                        || type.equals("forest") //               //$NON-NLS-1$
                        || type.equals("glacier")) { //           //$NON-NLS-1$

                     zoom = 13 - 1;

                  } else if (type.equals("airport")) { //         //$NON-NLS-1$

                     zoom = 14 - 1;

                  } else if (type.equals("edu") //                //$NON-NLS-1$
                        || type.equals("pass") //                 //$NON-NLS-1$
                        || type.equals("landmark") //             //$NON-NLS-1$
                        || type.equals("railwaystation")) { //    //$NON-NLS-1$

                     zoom = 15 - 1;
                  }
               }

               final String poiText = pageName.replace('_', ' ');
               final double lat1 = lat;
               final double lon1 = lon;
               final int zoom1 = zoom;

               // hide previous tooltip
               setPoiVisible(false);

               final GeoPosition poiGeoPosition = new GeoPosition(lat1, lon1);

               final PoiToolTip poi = getPoiTooltip();
               poi.geoPosition = poiGeoPosition;
               poi.setText(poiText);

               setZoom(zoom1);
               setMapCenter(poiGeoPosition);

               _isPoiVisible = true;

               fireEvent_POI(poiGeoPosition, poiText);

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

      final Rectangle bounds = _worldPixel_TopLeft_Viewport;

      final double newCenterX = bounds.x + bounds.width / 2.0 + xDiff;
      final double newCenterY = bounds.y + bounds.height / 2.0 + yDiff;

      final Point2D.Double pixelCenter = new Point2D.Double(newCenterX, newCenterY);

      setMapCenterInWorldPixel(pixelCenter);
      updateViewPortData();

      paint();

      fireEvent_MapPosition(false);
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

   public void removeMousePositionListener(final IGeoPositionListener listener) {
      _allMousePositionListeners.remove(listener);
   }

   /**
    * Removes a map overlay.
    *
    * @return the current map overlay
    */
   public void removeOverlayPainter(final Map2Painter overlay) {

      _allMapPainter.remove(overlay);

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
    * Reset hovered tour data
    */
   public void resetTours_HoveredData() {

      _allHoveredTourIds.clear();
      _allHoveredDevPoints.clear();
      _allHoveredSerieIndices.clear();

      if (_allPaintedTiles != null) {

         for (final Tile[] allTileArrays : _allPaintedTiles) {

            for (final Tile tile : allTileArrays) {

               if (tile != null) {

                  tile.allPainted_HoverRectangle.clear();
                  tile.allPainted_HoverTourID.clear();
                  tile.allPainted_HoverSerieIndices.clear();
               }
            }
         }
      }
   }

   /**
    * Reset selected tour data
    */
   public void resetTours_SelectedData() {

      _hovered_SelectedTourId = -1;
      _hovered_SelectedSerieIndex_Behind = -1;
      _hovered_SelectedSerieIndex_Front = -1;

   }

   public void setCenterMapBy(final CenterMapBy centerMapBy) {
      _centerMapBy = centerMapBy;
   }

   public void setConfig_HoveredSelectedTour(final boolean isShowHoveredOrSelectedTour,
                                             final boolean isShowBreadcrumbs,

                                             final int breadcrumbItems,
                                             final RGB hoveredRGB,
                                             final int hoveredOpacity,
                                             final RGB hoveredAndSelectedRGB,
                                             final int hoveredAndSelectedOpacity,
                                             final RGB selectedRGB,
                                             final int selectedOpacity) {

// SET_FORMATTING_OFF

      _isShowHoveredOrSelectedTour                    = isShowHoveredOrSelectedTour;
      _isShowBreadcrumbs                              = isShowBreadcrumbs;


      _hoveredSelectedTour_Hovered_Color              = new Color(hoveredRGB);
      _hoveredSelectedTour_Hovered_Opacity            = hoveredOpacity;

      _hoveredSelectedTour_HoveredAndSelected_Color   = new Color(hoveredAndSelectedRGB);
      _hoveredSelectedTour_HoveredAndSelected_Opacity = hoveredAndSelectedOpacity;

      _hoveredSelectedTour_Selected_Color             = new Color(selectedRGB);
      _hoveredSelectedTour_Selected_Opacity           = selectedOpacity;

// SET_FORMATTING_ON

      _tourBreadcrumb.setVisibleBreadcrumbs(breadcrumbItems);

      if (isShowHoveredOrSelectedTour == false) {

         // hide hovered/selected tour
         resetTours_SelectedData();
      }

      resetTours_HoveredData();

      disposeOverlayImageCache();

      paint();
   }

   public void setConfig_TourDirection(final boolean isShowTourDirection,
                                       final boolean isShowTourDirection_Always,
                                       final int markerGap,
                                       final int lineWidth,
                                       final float symbolSize,
                                       final RGB tourDirection_RGB) {

      _isDrawTourDirection = isShowTourDirection;
      _isDrawTourDirection_Always = isShowTourDirection_Always;

      _tourDirection_MarkerGap = markerGap;
      _tourDirection_LineWidth = lineWidth;
      _tourDirection_SymbolSize = symbolSize;
      _tourDirection_RGB = tourDirection_RGB;
   }

   /**
    * Set cursor only when needed, to optimize performance
    *
    * @param cursor
    */
   private void setCursorOptimized(final Cursor cursor) {

      if (cursor == _currentCursor) {
         return;
      }

      _currentCursor = cursor;

      setCursor(cursor);
   }

   /**
    * Set map dimming level for the current map factory, this will dim the map images
    *
    * @param mapDimLevel
    * @param dimColor
    * @param isDimMap
    * @param isBackgroundDark
    */
   public void setDimLevel(final boolean isDimMap, final int mapDimLevel, final RGB dimColor, final boolean isBackgroundDark) {

      _isMapBackgroundDark = isBackgroundDark;

      if (_mp != null) {
         _mp.setDimLevel(isDimMap, mapDimLevel, dimColor);
      }
   }

   public void setDirectPainter(final IDirectPainter directPainter) {
      _directMapPainter = directPainter;
   }

   /**
    * Activate/deactivate fast painting, map will be repainted afterwards.
    *
    * @param isFastMapPaintingActive
    */
   public void setIsFastMapPainting_Active(final boolean isFastMapPaintingActive) {

      _isFastMapPainting_Active = isFastMapPaintingActive;

      disposeOverlayImageCache();

      redraw();
   }

   public void setIsInInverseKeyboardPanning(final boolean isInInverseKeyboardPanning) {
      _isInInverseKeyboardPanning = isInInverseKeyboardPanning;
   }

   public void setIsMultipleTours(final boolean isMultipleTours) {
      _hoveredSelectedTour_CanSelectTour = isMultipleTours;
   }

   public void setIsShowTour(final boolean isShowTour) {
      _isShowTour = isShowTour;
   }

   /**
    * Set the legend for the map, the legend image will be disposed when the map is disposed,
    *
    * @param legend
    *           Legend for the map or <code>null</code> to disable the legend
    */
   public void setLegend(final MapLegend legend) {

      if (legend == null && _mapLegend != null) {

         // dispose legend image
         UI.disposeResource(_mapLegend.getImage());
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

      if (_mp == null) {

         // this occurred when restore state had a wrong map provider

         setMapProvider(MapProviderManager.getDefaultMapProvider());

         return;
      }

      final java.awt.Point newMapCenter = _mp.geoToPixel(geoPosition, _mapZoomLevel);

      if (Thread.currentThread() == _displayThread) {

         setMapCenterInWorldPixel(newMapCenter);

      } else {

         // current thread is not the display thread

         _display.syncExec(() -> {
            if (!isDisposed()) {
               setMapCenterInWorldPixel(newMapCenter);
            }
         });
      }

      updateViewPortData();
      updateTourToolTip();

      stopOldPainting();

      paint();
   }

   /**
    * Sets the center of the map {@link #_worldPixel_MapCenter} in world pixel coordinates with the
    * current zoom level
    *
    * @param newWorldPixelCenter
    */
   private void setMapCenterInWorldPixel(final Point2D newWorldPixelCenter) {

      _worldPixel_MapCenter = checkWorldPixel(newWorldPixelCenter);

      fireEvent_MousePosition();
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

      if (_mp == null) {
         return;
      }

      Rectangle wpTourRect;
      Rectangle wpMapRect;

      GeoPosition geoTourCenter;

      java.awt.Point wpTourCenter;
      java.awt.geom.Point2D.Double wpMapCenter;

      // keep current zoom level
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
      wpTourCenter = new java.awt.Point(
            wpTourRect.x + wpTourRect.width / 2,
            wpTourRect.y + wpTourRect.height / 2);

      // set tour geo center in the center of the tour rectangle
      geoTourCenter = _mp.pixelToGeo(wpTourCenter, zoom);

      wpMapCenter = checkWorldPixel(_mp.geoToPixel(geoTourCenter, zoom), zoom);
      wpMapRect = getWorldPixel_TopLeft_Viewport(wpMapCenter);

      // use an offset that the slider are not at the map border and almost not visible
      final int offset = 30;

      // zoom out until the tour is smaller than the map viewport
      while ((wpTourRect.width + offset > wpMapRect.width) //
            || (wpTourRect.height + offset > wpMapRect.height)) {

         // check zoom level
         if (zoom - 1 < minZoomLevel) {
            // this should not occur -> a tour should not be larger than the earth
            break;
         }

         // zoom out
         zoom--;

         tourZoomLevel = zoom - zoomLevelDiff;
         tourZoomLevel = tourZoomLevel > maximumZoomLevel ? maximumZoomLevel : tourZoomLevel;

         wpTourRect = getWorldPixelFromGeoPositions(geoTourPositions, tourZoomLevel);

         wpMapCenter = checkWorldPixel(_mp.geoToPixel(geoTourCenter, zoom), zoom);
         wpMapRect = getWorldPixel_TopLeft_Viewport(wpMapCenter);
      }

      if (zoom != currentZoomLevel) {

         // set new zoom level ONLY when it was modified -> this will dispose old overlay images !!!

         setZoom(zoom);
      }

      // update map with with new map center position
      {
         // zoom position is the same as previous !!!
         // _mapZoomLevel == _mapZoomLevel

         // set new map center
         _worldPixel_MapCenter = wpMapCenter;

         updateViewPortData();
      }

      fireEvent_MapInfo();
   }

   /**
    * Sets the map provider for the map and redraws the map
    *
    * @param mapProvider
    *           new map provider
    */
   public void setMapProvider(final MP mapProvider) {

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

      _mp = mapProvider;

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

         setZoom(mapProvider.getDefaultZoomLevel());
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

      final PoiToolTip poiToolTip = getPoiTooltip();
      poiToolTip.geoPosition = poiGeoPosition;
      poiToolTip.setText(poiText);

      setZoom(zoomLevel);

      _isPoiPositionInViewport = updatePoiImageDevPosition();

      if (_isPoiPositionInViewport == false) {

         // recenter map only when poi is not visible

         setMapCenter(poiGeoPosition);
      }

      /*
       * When poi is set, it is possible that the mouse is already over the poi -> update tooltip
       */
      final Point devMouse = this.toControl(getDisplay().getCursorLocation());
      final int devMouseX = devMouse.x;
      final int devMouseY = devMouse.y;

      // check if mouse is within the poi image
      if (_isPoiVisible
            && (devMouseX > _poiImageDevPosition.x)
            && (devMouseX < _poiImageDevPosition.x + _poiImageBounds.width)
            && (devMouseY > _poiImageDevPosition.y - _poi_Tooltip_OffsetY - 5)
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
               _worldPixel_TopLeft_Viewport.width / 2,
               _worldPixel_TopLeft_Viewport.height / 2,
               _worldPixel_TopLeft_Viewport,
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
         _tour_ToolTip.setHoveredContext(_hoveredAreaContext);

         _tour_ToolTip.show(new Point(_hoveredAreaContext.hoveredTopLeftX, _hoveredAreaContext.hoveredTopLeftY));

         redraw();
      }
   }

   /**
    * Sets the visibility of the poi tooltip. Poi tooltip is visible when the tooltip is available
    * and the poi image is within the map view port
    *
    * @param isVisible
    *           <code>false</code> will hide the tooltip
    */
   private void setPoiVisible(final boolean isVisible) {

      if (_poi_Tooltip == null) {
         return;
      }

      if (isVisible) {

         if (_isPoiPositionInViewport = updatePoiImageDevPosition()) {

            final Point poiDisplayPosition = toDisplay(_poiImageDevPosition);

            _poi_Tooltip.show(
                  poiDisplayPosition.x,
                  poiDisplayPosition.y,
                  _poiImageBounds.width,
                  _poiImageBounds.height,
                  _poi_Tooltip_OffsetY);
         }
      } else {
         _poi_Tooltip.hide();
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

   /**
    * Set tours which are currently painted in the map
    *
    * @param allTourIds
    */
   public void setTourIds(final List<Long> allTourIds) {
      _allTourIds = allTourIds;
   }

   public void setTourPaintMethodEnhanced(final boolean isEnhanced, final boolean isShowWarning) {

      _isTourPaintMethodEnhanced = isEnhanced;
      _isShowTourPaintMethodEnhancedWarning = isShowWarning;

      disposeOverlayImageCache();
   }

   public void setTourToolTip(final TourToolTip tourToolTip) {

      _tour_ToolTip = tourToolTip;

      tourToolTip.addHideListener(event -> {

         // hide hovered area
         _hoveredAreaContext = null;

         redraw();
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

      setZoom(newZoomLevel, CenterMapBy.Map);
   }

   /**
    * Set the zoom level for the map and centers the map to <code>centerMapBy</code>. The zoom level
    * is checked if the map provider supports the requested zoom level.
    * <p>
    * The map is initialize when this is not yet done be setting all internal data !!!
    *
    * @param newZoomLevel
    *           zoom level for the map, it is adjusted to the min/max zoom levels
    * @param centerMapBy
    */
   public void setZoom(final int newZoomLevel, final CenterMapBy centerMapBy) {

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

      boolean isNewZoomLevel = false;

      // check if zoom level has changed
      if (oldZoomLevel == adjustedZoomLevel) {

         // this is disabled that a double click can set the center of the map

         // return;

      } else {

         // a new zoom level is set

         isNewZoomLevel = true;
      }

      if (oldZoomLevel != adjustedZoomLevel) {

         // zoom level has changed -> stop downloading images for the old zoom level
         _mp.resetAll(true);
      }

      final Dimension oldMapTileSize = _mp.getMapTileSize(oldZoomLevel);

      // check if map is initialized or zoom level has not changed
      Point2D wpCurrentMapCenter = _worldPixel_MapCenter;
      if (wpCurrentMapCenter == null) {

         // setup map center

         initMap();

         wpCurrentMapCenter = _worldPixel_MapCenter;
      }

      _mapZoomLevel = adjustedZoomLevel;

      // update values for the new zoom level !!!
      _mapTileSize = _mp.getMapTileSize(adjustedZoomLevel);

      final double relativeWidth = (double) _mapTileSize.width / oldMapTileSize.width;
      final double relativeHeight = (double) _mapTileSize.height / oldMapTileSize.height;

      Point2D.Double wpNewMapCenter;

      if (CenterMapBy.Mouse.equals(centerMapBy)

            // fixes this "issue" https://github.com/wolfgang-ch/mytourbook/issues/370
            && isNewZoomLevel) {

         // set map center to the current mouse position but only when a new zoom level is set !!!

         final Rectangle wpViewPort = _worldPixel_TopLeft_Viewport;

         wpCurrentMapCenter = new Point2D.Double(
               wpViewPort.x + _mouseMove_DevPosition_X_Last,
               wpViewPort.y + _mouseMove_DevPosition_Y_Last);

      } else {

         // for any other cases: center zoom to the map center

         // this is also the zoom behavior until 18.5
      }

      wpNewMapCenter = new Point2D.Double(
            wpCurrentMapCenter.getX() * relativeWidth,
            wpCurrentMapCenter.getY() * relativeHeight);

      setMapCenterInWorldPixel(wpNewMapCenter);

      updateViewPortData();
      updateTourToolTip();
//    updatePoiVisibility();

      isTourHovered();
      paint();

      // update zoom level in status bar
      fireEvent_MapInfo();

      fireEvent_MapPosition(true);
   }

   /**
    * @param boundingBox
    * @return Returns zoom level or -1 when bounding box is <code>null</code>.
    */
   public int setZoomToBoundingBox(final String boundingBox) {

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
         final java.awt.Point center = new java.awt.Point(
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
    * @param tourGeoFilter
    *           Show geo grid box for this geo filter, when <code>null</code> the selected grid box
    *           is set to hidden
    */
   public void showGeoSearchGrid(final TourGeoFilter tourGeoFilter) {

      if (_mp == null) {

         // the map has currently no map provider
         return;
      }

      grid_UpdatePaintingStateData();

      if (tourGeoFilter == null) {

         // hide geo grid
         _geoGrid_Data_Selected = null;
         _geoGrid_TourGeoFilter = null;

         redraw();

      } else {

         // show requested geo grid

         _geoGrid_TourGeoFilter = tourGeoFilter;

         // geo grid is displayed
         _isFastMapPainting_Active = true;

         final boolean isSyncMapPosition = Util.getStateBoolean(_geoFilterState,
               TourGeoFilter_Manager.STATE_IS_SYNC_MAP_POSITION,
               TourGeoFilter_Manager.STATE_IS_SYNC_MAP_POSITION_DEFAULT);

         _geoGrid_MapZoomLevel = tourGeoFilter.mapZoomLevel;
         _geoGrid_MapGeoCenter = tourGeoFilter.mapGeoCenter;

         if (isSyncMapPosition) {

            // set zoom level first, that recalculation is correct
            setZoom(_geoGrid_MapZoomLevel);
         }

         MapGridData mapGridData = tourGeoFilter.mapGridData;

         if (mapGridData == null) {

            // This can occur when geofilter is loaded from xml file and not created in the map

            // create map grid box from tour geo filter
            mapGridData = new MapGridData();

            final GeoPosition geo_Start = tourGeoFilter.geoLocation_TopLeft;
            final GeoPosition geo_End = tourGeoFilter.geoLocation_BottomRight;

            mapGridData.geo_Start = geo_Start;
            mapGridData.geo_End = geo_End;

            grid_Convert_StartEnd_2_TopLeft(geo_Start, geo_End, mapGridData);

            tourGeoFilter.mapGridData = mapGridData;
         }

         _geoGrid_Data_Selected = mapGridData;

         final Rectangle world_MapViewPort = getWorldPixel_TopLeft_Viewport(_worldPixel_MapCenter);

         /*
          * Reposition map
          */
         if (isSyncMapPosition) {

            // check if grid box is already visible

            if (world_MapViewPort.contains(_geoGrid_Data_Selected.world_Start)
                  && world_MapViewPort.contains(_geoGrid_Data_Selected.world_End)) {

               // grid box is visible -> nothing to do

            } else {

               // recenter map to make it visible

               setMapCenter(new GeoPosition(_geoGrid_MapGeoCenter.latitude, _geoGrid_MapGeoCenter.longitude));
            }

         } else {

            // map is not synched but location is wrong

            updateViewPortData();
            paint();
         }
      }
   }

   private void showPoi() {

      if (_poi_Tooltip != null && _poi_Tooltip.isVisible()) {
         // poi is hidden
         return;
      }

      final PoiToolTip poiTT = getPoiTooltip();
      final Point poiDisplayPosition = this.toDisplay(_poiImageDevPosition);

      poiTT.show(
            poiDisplayPosition.x,
            poiDisplayPosition.y,
            _poiImageBounds.width,
            _poiImageBounds.height,
            _poi_Tooltip_OffsetY);
   }

   private void stopOldPainting() {

      // stop old painting jobs

      _isCancelSplitJobs = true;
      {
         Job.getJobManager().cancel(_splitJobFamily);
      }
      _isCancelSplitJobs = false;
   }

   public MapTourBreadcrumb tourBreadcrumb() {
      return _tourBreadcrumb;
   }

   public void updateGraphColors() {

      _overlayAlpha = _prefStore.getBoolean(ITourbookPreferences.MAP2_LAYOUT_IS_TOUR_TRACK_OPACITY)

            ? _prefStore.getInt(ITourbookPreferences.MAP2_LAYOUT_TOUR_TRACK_OPACITY)

            // no opacity
            : 0xff;
   }

   public void updateMapOptions() {

      final String drawSymbol = _prefStore.getString(ITourbookPreferences.MAP_LAYOUT_PLOT_TYPE);

      _prefOptions_IsDrawSquare = drawSymbol.equals(Map2_Appearance.PLOT_TYPE_SQUARE);
      _prefOptions_LineWidth = _prefStore.getInt(ITourbookPreferences.MAP_LAYOUT_SYMBOL_WIDTH);
      _prefOptions_BorderWidth = _prefStore.getInt(ITourbookPreferences.MAP_LAYOUT_BORDER_WIDTH);

      final boolean isCutOffLinesInPauses = _prefStore.getBoolean(ITourbookPreferences.MAP_LAYOUT_IS_CUT_OFF_LINES_IN_PAUSES);
      _prefOptions_isCutOffLinesInPauses = isCutOffLinesInPauses && drawSymbol.equals(Map2_Appearance.PLOT_TYPE_LINE);
   }

   /**
    * @return Returns <code>true</code> when the POI image is visible and the position is set in
    *         {@link #_poiImageDevPosition}
    */
   private boolean updatePoiImageDevPosition() {

      final GeoPosition poiGeoPosition = getPoiTooltip().geoPosition;
      if (poiGeoPosition == null) {
         return false;
      }

      // get world position for the poi coordinates
      final java.awt.Point worldPoiPos = _mp.geoToPixel(poiGeoPosition, _mapZoomLevel);

      // adjust view port to contain the poi image
      final Rectangle adjustedViewport = new Rectangle(
            _worldPixel_TopLeft_Viewport.x,
            _worldPixel_TopLeft_Viewport.y,
            _worldPixel_TopLeft_Viewport.width,
            _worldPixel_TopLeft_Viewport.height);

      adjustedViewport.x -= _poiImageBounds.width;
      adjustedViewport.y -= _poiImageBounds.height;
      adjustedViewport.width += _poiImageBounds.width * 2;
      adjustedViewport.height += _poiImageBounds.height * 2;

      // check if poi is visible
      if (adjustedViewport.intersects(
            worldPoiPos.x - _poiImageBounds.width / 2,
            worldPoiPos.y - _poiImageBounds.height,
            _poiImageBounds.width,
            _poiImageBounds.height)) {

         // convert world position into device position
         final int devPoiPosX = worldPoiPos.x - _worldPixel_TopLeft_Viewport.x;
         final int devPoiPosY = worldPoiPos.y - _worldPixel_TopLeft_Viewport.y;

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
               && (devMouseY > _poiImageDevPosition.y - _poi_Tooltip_OffsetY - 5)
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

      if (_mp != null && _tour_ToolTip != null && _tour_ToolTip.isActive()) {

         /*
          * redraw must be forced because the hovered area can be the same but can be at a different
          * location
          */
         updateTourToolTip_HoveredArea();

         _tour_ToolTip.update();
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

      final ArrayList<ITourToolTipProvider> toolTipProvider = _tour_ToolTip.getToolTipProvider();

      /*
       * check tour info tool tip provider as first
       */
      for (final IToolTipProvider tttProvider : toolTipProvider) {

         if (tttProvider instanceof IInfoToolTipProvider) {

            final HoveredAreaContext hoveredContext = ((IInfoToolTipProvider) tttProvider).getHoveredContext(
                  _mouseMove_DevPosition_X,
                  _mouseMove_DevPosition_Y);

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
                     _mouseMove_DevPosition_X,
                     _mouseMove_DevPosition_Y,
                     _worldPixel_TopLeft_Viewport,
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
      _tour_ToolTip.setHoveredContext(_hoveredAreaContext);

      if (_hoveredAreaContext != null) {
         redraw();
      }

      /*
       * Hide hovered area, this must be done because when a tile do not contain a way point, the
       * hovered area can sill be displayed when another position is set with setMapCenter()
       */
      if (oldHoveredContext != null && _hoveredAreaContext == null) {

         // update tool tip because it has it's own mouse move listener for the map
         _tour_ToolTip.hideHoveredArea();

         redraw();
      }

   }

   /**
    * Sets all viewport data which are necessary to draw the map tiles in
    * {@link #paint_30_Tiles(GC)}. Some values are cached to optimize performance.
    * <p>
    * {@link #_worldPixel_MapCenter} and {@link #_mapZoomLevel} are the base values for the other
    * viewport fields.
    */
   private void updateViewPortData() {

      if (_mp == null) {
         // the map has currently no map provider
         return;
      }

      _worldPixel_TopLeft_Viewport = getWorldPixel_TopLeft_Viewport(_worldPixel_MapCenter);

      final int visiblePixelWidth = _worldPixel_TopLeft_Viewport.width;
      final int visiblePixelHeight = _worldPixel_TopLeft_Viewport.height;

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
      final int tileOffsetX = (int) Math.floor((double) _worldPixel_TopLeft_Viewport.x / (double) _tilePixelSize);
      final int tileOffsetY = (int) Math.floor((double) _worldPixel_TopLeft_Viewport.y / (double) _tilePixelSize);

      _tilePos_MinX = tileOffsetX;
      _tilePos_MinY = tileOffsetY;
      _tilePos_MaxX = _tilePos_MinX + numTileWidth;
      _tilePos_MaxY = _tilePos_MinY + numTileHeight;

      _allPaintedTiles = new Tile[numTileWidth + 1][numTileHeight + 1];

      /*
       * Pixel size for one geo grid, 0.01 degree
       */
      final Point2D.Double viewportMapCenter_worldPixel = new Point2D.Double(
            _worldPixel_TopLeft_Viewport.x + visiblePixelWidth / 2,
            _worldPixel_TopLeft_Viewport.y + visiblePixelHeight / 2);

      final GeoPosition geoPos = _mp.pixelToGeo(viewportMapCenter_worldPixel, _mapZoomLevel);

      // round to 0.01
      final double geoLat1 = Math.round(geoPos.latitude * 100) / 100.0;
      final double geoLon1 = Math.round(geoPos.longitude * 100) / 100.0;
      final double geoLat2 = geoLat1 + 0.01;
      final double geoLon2 = geoLon1 + 0.01;

      final Point2D.Double worldGrid1 = _mp.geoToPixelDouble(new GeoPosition(geoLat1, geoLon1), _mapZoomLevel);
      final Point2D.Double worldGrid2 = _mp.geoToPixelDouble(new GeoPosition(geoLat2, geoLon2), _mapZoomLevel);

      _devGridPixelSize_X = Math.abs(worldGrid2.x - worldGrid1.x);
      _devGridPixelSize_Y = Math.abs(worldGrid2.y - worldGrid1.y);

      grid_UpdateGeoGridData();
   }

   public void zoomIn(final CenterMapBy centerMapBy) {

      setZoom(_mapZoomLevel + 1, centerMapBy);

      paint();
   }

   public void zoomOut(final CenterMapBy centerMapBy) {

      setZoom(_mapZoomLevel - 1, centerMapBy);

      paint();
   }

}
