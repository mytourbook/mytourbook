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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.tourbook.util.StatusUtil;

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
import org.eclipse.jface.layout.GridLayoutFactory;
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
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.byteholder.geoclipse.Activator;
import de.byteholder.geoclipse.Messages;
import de.byteholder.geoclipse.map.event.IPOIListener;
import de.byteholder.geoclipse.map.event.IPositionListener;
import de.byteholder.geoclipse.map.event.IZoomListener;
import de.byteholder.geoclipse.map.event.MapPOIEvent;
import de.byteholder.geoclipse.map.event.MapPositionEvent;
import de.byteholder.geoclipse.map.event.ZoomEvent;
import de.byteholder.geoclipse.mapprovider.ImageDataResources;
import de.byteholder.geoclipse.mapprovider.MP;
import de.byteholder.geoclipse.preferences.IMappingPreferences;
import de.byteholder.geoclipse.ui.TextWrapPainter;
import de.byteholder.geoclipse.ui.ToolTip;
import de.byteholder.gpx.GeoPosition;

public class Map extends Canvas {

	private static final String							DIRECTION_E									= "E";
	private static final String							DIRECTION_N									= "N";

//	private static final String							WIKI_PARAMETER_DIM							= "dim";																	//$NON-NLS-1$
	private static final String							WIKI_PARAMETER_TYPE							= "type";														//$NON-NLS-1$

//	http://toolserver.org/~geohack/geohack.php?pagename=Sydney&language=de&params=33.85_S_151.2_E_region:AU-NSW_type:city(3641422)
//	http://toolserver.org/~geohack/geohack.php?pagename=Palm_Island,_Queensland&params=18_44_S_146_35_E_scale:20000_type:city
//	http://toolserver.org/~geohack/geohack.php?pagename=P%C3%B3voa_de_Varzim&params=41_22_57_N_8_46_45_W_region:PT_type:city//
//
//	where D is degrees, M is minutes, S is seconds, and NS/EWO are the directions
//
//	D;D
//	D_N_D_E
//	D_M_N_D_M_E
//	D_M_S_N_D_M_S_E

	private static final String							PATTERN_SEPARATOR							= "_";															//$NON-NLS-1$
	private static final String							PATTERN_END									= "_?(.*)";

	private static final String							PATTERN_WIKI_URL							= ".*pagename=([^&]*).*params=(.*)";							//$NON-NLS-1$
	private static final String							PATTERN_WIKI_PARAMETER_KEY_VALUE_SEPARATOR	= ":";															//$NON-NLS-1$

	private static final String							PATTERN_DOUBLE								= "([-+]?[0-9]*\\.?[0-9]+)";
	private static final String							PATTERN_DOUBLE_SEP							= PATTERN_DOUBLE
																											+ PATTERN_SEPARATOR;

	private static final String							PATTERN_DIRECTION_NS						= "([NS])_";
	private static final String							PATTERN_DIRECTION_WE						= "([WE])";

//	private static final String							PATTERN_WIKI_POSITION_10					= "([-+]?[0-9]*\\.?[0-9]+)_([NS])_([-+]?[0-9]*\\.?[0-9]+)_([WE])_?(.*)";	//$NON-NLS-1$
//	private static final String							PATTERN_WIKI_POSITION_20					= "([0-9]*)_([NS])_([0-9]*)_([WE])_?(.*)";									//$NON-NLS-1$
//	private static final String							PATTERN_WIKI_POSITION_21					= "([0-9]*)_([0-9]*)_([NS])_([0-9]*)_([0-9]*)_([WE])_?(.*)";				//$NON-NLS-1$
//	private static final String							PATTERN_WIKI_POSITION_22					= "([0-9]*)_([0-9]*)_([0-9]*)_([NS])_([0-9]*)_([0-9]*)_([0-9]*)_([WE])_?(.*)";	//$NON-NLS-1$

	private static final String							PATTERN_WIKI_POSITION_D_D					= PATTERN_DOUBLE
																											+ ";"
																											+ PATTERN_DOUBLE
																											+ PATTERN_END;

	private static final String							PATTERN_WIKI_POSITION_D_N_D_E				= PATTERN_DOUBLE_SEP
																											+ PATTERN_DIRECTION_NS
																											+ PATTERN_DOUBLE_SEP
																											+ PATTERN_DIRECTION_WE
																											+ PATTERN_END;

	private static final String							PATTERN_WIKI_POSITION_D_M_N_D_M_E			= PATTERN_DOUBLE_SEP
																											+ PATTERN_DOUBLE_SEP
																											+ PATTERN_DIRECTION_NS
																											+ PATTERN_DOUBLE_SEP
																											+ PATTERN_DOUBLE_SEP
																											+ PATTERN_DIRECTION_WE
																											+ PATTERN_END;

	private static final String							PATTERN_WIKI_POSITION_D_M_S_N_D_M_S_E		= PATTERN_DOUBLE_SEP
																											+ PATTERN_DOUBLE_SEP
																											+ PATTERN_DOUBLE_SEP
																											+ PATTERN_DIRECTION_NS
																											+ PATTERN_DOUBLE_SEP
																											+ PATTERN_DOUBLE_SEP
																											+ PATTERN_DOUBLE_SEP
																											+ PATTERN_DIRECTION_WE
																											+ PATTERN_END;

	private static final Pattern						_patternWikiUrl								= Pattern
																											.compile(PATTERN_WIKI_URL);
	private static final Pattern						_patternWikiPosition_D_D					= Pattern
																											.compile(PATTERN_WIKI_POSITION_D_D);
	private static final Pattern						_patternWikiPosition_D_N_D_E				= Pattern
																											.compile(PATTERN_WIKI_POSITION_D_N_D_E);
	private static final Pattern						_patternWikiPosition_D_M_N_D_M_E			= Pattern
																											.compile(PATTERN_WIKI_POSITION_D_M_N_D_M_E);
	private static final Pattern						_patternWikiPosition_D_M_S_N_D_M_S_E		= Pattern
																											.compile(PATTERN_WIKI_POSITION_D_M_S_N_D_M_S_E);
	private static final Pattern						_patternWikiParamter						= Pattern
																											.compile(PATTERN_SEPARATOR);
	private static final Pattern						_patternWikiKeyValue						= Pattern
																											.compile(PATTERN_WIKI_PARAMETER_KEY_VALUE_SEPARATOR);

	// [181,208,208] is the color of water in the standard OSM material
	public final static RGB								DEFAULT_BACKGROUND_RGB						= new RGB(
																											181,
																											208,
																											208);

	private static final RGB							_transparentRGB								= new RGB(
																											0xfe,
																											0xfe,
																											0xfe);

	/**
	 * The zoom level. Normally a value between around 0 and 20.
	 */
	private int											_mapZoomLevel								= 0;

	/**
	 * Image which contains the map
	 */
	private Image										_mapImage;

	private Image										_image9Parts;

	private GC											_gc9Parts;

	/**
	 * Indicates whether or not to draw the borders between tiles. Defaults to false.
	 * not very nice looking, very much a product of testing Consider whether this should really be
	 * a property or not.
	 */
	private boolean										_isShowTileInfo								= false;

	/**
	 * Factory used by this component to grab the tiles necessary for painting the map.
	 */
	private MP											_MP;

	/**
	 * The position in latitude/longitude of the "address" being mapped. This is a special
	 * coordinate that, when moved, will cause the map to be moved as well. It is separate from
	 * "center" in that "center" tracks the current center (in pixels) of the view port whereas this
	 * will not change when panning or zooming. Whenever the addressLocation is changed, however,
	 * the map will be repositioned.
	 */
	private GeoPosition									_addressLocation;

	/**
	 * Specifies whether zooming is enabled (the mouse wheel, for example, zooms)
	 */
	private boolean										_zoomEnabled								= true;

	/**
	 * Indicates whether the component should re-center the map when the "middle" mouse button is
	 * pressed
	 */
	private boolean										_recenterOnClickEnabled						= true;

	private boolean										_zoomOnDoubleClickEnabled					= true;

	/**
	 * The overlay to delegate to for painting the "foreground" of the map component. This would
	 * include painting waypoints, day/night, etc. Also receives mouse events.
	 */
	private final List<MapPainter>						_overlays									= new ArrayList<MapPainter>();

	private final TileLoadObserver						_tileLoadObserver							= new TileLoadObserver();

	private final Cursor								_cursorPan;
	private final Cursor								_cursorDefault;
	private final Cursor								_cursorCross;

	private int											_redrawMapCounter							= 0;
	private int											_overlayRunnableCounter						= 0;

	private boolean										_isLeftMouseButtonPressed					= false;

	private Point										_mouseMovePosition;
	private Point										_mousePanPosition;
	private boolean										_isMapPanned;

	private final Thread								_overlayThread;
	private long										_nextOverlayRedrawTime;

	private final NumberFormat							_nf											= NumberFormat
																											.getNumberInstance();
	private final NumberFormat							_nfLatLon									= NumberFormat
																											.getNumberInstance();

	{
		_nfLatLon.setMinimumFractionDigits(4);
		_nfLatLon.setMaximumFractionDigits(4);
	}

	private final TextWrapPainter						_textWrapper								= new TextWrapPainter();

	/**
	 * cache for overlay images
	 */
	private final OverlayImageCache						_overlayImageCache;

	/**
	 * This queue contains tiles which overlay image must be painted
	 */
	private final ConcurrentLinkedQueue<Tile>			_tileOverlayPaintQueue						= new ConcurrentLinkedQueue<Tile>();

	private boolean										_isDrawOverlayRunning						= false;

	private String										_overlayKey;

	/**
	 * this painter is called when the map is painted in the onPaint event
	 */
	private IDirectPainter								_directMapPainter;

	private final DirectPainterContext					_directMapPainterContext					= new DirectPainterContext();

	/**
	 * when <code>true</code> the overlays are painted
	 */
	private boolean										_isDrawOverlays								= false;

	/**
	 * contains a legend which is painted in the map
	 */
	private MapLegend									_mapLegend;

	private boolean										_isLegendVisible;

	/**
	 * The position, in <I>map coordinates</I> of the center point. This is defined as the distance
	 * from the top and left edges of the map in pixels. Dragging the map component will change the
	 * center position. Zooming in/out will cause the center to be recalculated so as to remain in
	 * the center of the new "map". <br>
	 * <br>
	 * <br>
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! <br>
	 * <br>
	 * This center MUST be kept in double because the center would move to another position when the
	 * map is zoomed because of rounding errors <br>
	 * <br>
	 * This cost me some hours to fix it <br>
	 * <br>
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! <br>
	 */
	private Point2D										_mapCenterInWorldPixel						= new Point2D.Double(
																											0,
																											0);

	/**
	 * Viewport in the world map where the {@link #_mapImage} is painted <br>
	 * <br>
	 * <b>x</b> and <b>y</b> contains the position in world pixel, which is the <b>top/left
	 * corner</b>, <br>
	 * <b>width</b> and <b>height</b> contains the visible area in device pixel
	 */
	private Rectangle									_worldPixelViewport;

	/**
	 * Contains the client area of the map without trimmings, this rectangle has the width and
	 * height of the map image
	 */
	private org.eclipse.swt.graphics.Rectangle			_clientArea;

	private final List<IZoomListener>					_zoomListeners;

	private final ListenerList							_mousePositionListeners						= new ListenerList(
																											ListenerList.IDENTITY);
	private final ListenerList							_poiListeners								= new ListenerList(
																											ListenerList.IDENTITY);

	// measurement system
	private float										_distanceUnitValue							= 1;
	private String										_distanceUnitLabel							= UI.EMPTY_STRING;

	private boolean										_isScaleVisible								= false;

	private final Color									_transparentColor;
	private final Color									_defaultBackgroundColor;

	/*
	 * POI image
	 */
	private boolean										_isPoiVisible;
	private boolean										_isPoiPositionInViewport;
	private GeoPosition									_poiGeoPosition;
	private final Image									_poiImage;
	private final org.eclipse.swt.graphics.Rectangle	_poiImageBounds;
	private final Point									_poiImageDevPosition						= new Point(0, 0);

	/*
	 * POI tooltip
	 */
	private ToolTip										_poiTT;
	private String										_poiTTText;
	private Text										_poiTTLabel;
	private final int									_poiTTOffsetY								= 5;

	/**
	 * when <code>true</code> the loading... image is not displayed
	 */
	private boolean										_isLiveView;

	private long										_requestedRedrawTime;
	private long										_drawTime;

	/*
	 * these 4 tile positions correspond to the tiles which are needed to draw the map
	 */
	private int											_tilePosMinX;
	private int											_tilePosMaxX;
	private int											_tilePosMinY;
	private int											_tilePosMaxY;

	// viewport data which are changed when map is resized or zoomed
	private Rectangle									_devVisibleViewport;

	/**
	 * contains the size of the map at the given zoom in tiles (num tiles tall by num tiles wide)
	 */
	private Dimension									_mapTileSize;
	private int											_worldViewportX;
	private int											_worldViewportY;

	private final Display								_display;
	private final Thread								_displayThread;

	private int											_jobCounterSplitImages						= 0;

	/**
	 * when <code>true</code> the tour is painted in the map in the enhanced mode otherwise in the
	 * simple mode
	 */
	private boolean										_isTourPaintMethodEnhanced					= false;

	private boolean										_isSelectOfflineArea;
	private boolean										_isOfflineSelectionStarted					= false;
	private boolean										_isPaintOfflineArea							= false;

	private Point										_offlineDevAreaStart;
	private Point										_offlineDevAreaEnd;
	private Point										_offlineDevTileStart;
	private Point										_offlineDevTileEnd;
	private Point										_offlineWorldStart;
	private Point										_offlineWorldEnd;
	private Point										_offlineWorldMouseMove;

	private org.eclipse.swt.graphics.Rectangle			_currentOfflineArea;
	private org.eclipse.swt.graphics.Rectangle			_previousOfflineArea;

	private IMapContextProvider							_mapContextProvider;

	private boolean										_disableContextMenu							= false;

	private DropTarget									_dropTarget;

	// used to pan using the arrow keys
	private class KeyMapListener extends KeyAdapter {

		@Override
		public void keyPressed(final KeyEvent e) {

			if (_isSelectOfflineArea) {

				disableOfflineAreaSelection();

				return;
			}

			// accelerate with Ctrl + Shift key
			int offset = (e.stateMask & SWT.CONTROL) != 0 ? 20 : 1;
			offset *= (e.stateMask & SWT.SHIFT) != 0 ? 10 : 1;

			int delta_x = 0;
			int delta_y = 0;

			switch (e.keyCode) {
			case SWT.ARROW_LEFT:
				delta_x = -offset;
				break;
			case SWT.ARROW_RIGHT:
				delta_x = offset;
				break;
			case SWT.ARROW_UP:
				delta_y = -offset;
				break;
			case SWT.ARROW_DOWN:
				delta_y = offset;
				break;
			}

			switch (e.character) {
			case '+':
				zoomIn();
				break;
			case '-':
				zoomOut();
				break;
			}

			if ((delta_x != 0) || (delta_y != 0)) {

				final Rectangle bounds = _worldPixelViewport;
				final double x = bounds.getCenterX() + delta_x;
				final double y = bounds.getCenterY() + delta_y;
				final Point2D.Double pixelCenter = new Point2D.Double(x, y);

				setMapCenterInWoldPixel(pixelCenter);

				updateViewPortData();

				queueMapRedraw();
			}
		}
	}

	// used to pan using press and drag mouse gestures
	private class MouseMapListener implements MouseListener, MouseMoveListener, Listener {

		@Override
		public void handleEvent(final Event event) {
			if (event.type == SWT.MouseWheel) {

// 				disabled because it's easier to navigate, centering can be done with double click / 14.1.2008 Wolfgang
//				recenterMap(event.x, event.y);

				if (event.count < 0) {
					zoomOut();
				} else {
					zoomIn();
				}
			}
		}

		@Override
		public void mouseDoubleClick(final MouseEvent e) {

			if (e.button == 1) {
				if (isRecenterOnClickEnabled() || (isZoomEnabled() && isZoomOnDoubleClickEnabled())) {
					recenterMap(e.x, e.y);
				}

				if (isZoomEnabled() && isZoomOnDoubleClickEnabled()) {
					zoomIn();
				}
			}
		}

		@Override
		public void mouseDown(final MouseEvent e) {
			onMouseDown(e);
		}

		@Override
		public void mouseMove(final MouseEvent e) {
			onMouseMove(e);
		}

		@Override
		public void mouseUp(final MouseEvent e) {
			onMouseUp(e);
		}
	}

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
					queueMapRedraw();

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

		_zoomListeners = new ArrayList<IZoomListener>();

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
				showHidePoiTooltip();
			}

			@Override
			public void focusLost(final FocusEvent e) {
// this is critical because the tool tip get's hidden when there are actions available in the tool tip shell
//				hidePoiToolTip();
			}
		});

		final MouseMapListener mouseListener = new MouseMapListener();
		addMouseListener(mouseListener);
		addMouseMoveListener(mouseListener);
		addListener(SWT.MouseWheel, mouseListener);

		addKeyListener(new KeyMapListener());

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

		addDropTarget(this);

		createContextMenu();

		_cursorPan = new Cursor(_display, SWT.CURSOR_SIZEALL);
		_cursorCross = new Cursor(_display, SWT.CURSOR_CROSS);
		_cursorDefault = new Cursor(_display, SWT.CURSOR_ARROW);

		_defaultBackgroundColor = new Color(_display, DEFAULT_BACKGROUND_RGB);

		_transparentColor = new Color(_display, _transparentRGB);

		_poiImage = Activator.getImageDescriptor(Messages.Image_POI_InMap).createImage();
		_poiImageBounds = _poiImage.getBounds();

		_overlayImageCache = new OverlayImageCache();

		_overlayThread = new Thread("PaintOverlayImages") { //$NON-NLS-1$
			@Override
			public void run() {

				while (!isInterrupted()) {

					try {

						Thread.sleep(20);

						if (_isDrawOverlayRunning == false) {

							// overlay drawing is not running

							final long currentTime = System.currentTimeMillis();

							if (currentTime > _nextOverlayRedrawTime + 50) {
								if (_tileOverlayPaintQueue.size() > 0) {

									// create overlay images
									paintOverlay10();
								}
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

	/**
	 * Checks if an image can be reused, this is true if the image exists and has the same size
	 * 
	 * @param newWidth
	 * @param newHeight
	 * @return
	 */
	public static boolean canReuseImage(final Image image, final org.eclipse.swt.graphics.Rectangle clientArea) {

		// check if we could reuse the existing image

		if ((image == null) || image.isDisposed()) {
			return false;
		} else {
			// image exist, check for the bounds
			final org.eclipse.swt.graphics.Rectangle oldBounds = image.getBounds();

			if (!((oldBounds.width == clientArea.width) && (oldBounds.height == clientArea.height))) {
				return false;
			}
		}

		return true;
	}

	/**
	 * creates a new image
	 * 
	 * @param display
	 * @param image
	 *            image which will be disposed if the image is not null
	 * @param clientArea
	 * @return returns a new created image
	 */
	public static Image createImage(final Display display,
									final Image image,
									final org.eclipse.swt.graphics.Rectangle clientArea) {

		if ((image != null) && !image.isDisposed()) {
			image.dispose();
		}

		// ensure the image has a width/height of 1, otherwise this causes troubles
		final int width = Math.max(1, clientArea.width);
		final int height = Math.max(1, clientArea.height);

		return new Image(display, width, height);
	}

	public static RGB getTransparentRGB() {
		return _transparentRGB;
	}

	void actionManageOfflineImages(final Event event) {

		// check if offline image is active
		final IPreferenceStore prefStore = Activator.getDefault().getPreferenceStore();
		if (prefStore.getBoolean(IMappingPreferences.OFFLINE_CACHE_USE_OFFLINE) == false) {

			MessageDialog.openInformation(
					Display.getCurrent().getActiveShell(),
					Messages.Dialog_OfflineArea_Error,
					Messages.Dialog_OfflineArea_Error_NoOffline);

			return;
		}

		// check if offline loading is running
		if (OfflineLoadManager.isLoading()) {

			MessageDialog.openInformation(
					Display.getCurrent().getActiveShell(),
					Messages.Dialog_OfflineArea_Error,
					Messages.Dialog_OfflineArea_Error_IsLoading);

			return;
		}

		if ((_offlineDevAreaStart != null //
				)
				&& (_currentOfflineArea != null)
				&& ((event.stateMask & SWT.CONTROL) != 0)) {

			/*
			 * use old offline area when the ctrl-key is pressed
			 */

			_previousOfflineArea = null;

			_isPaintOfflineArea = true;

			redraw();
			queueMapRedraw();

			new DialogManageOfflineImages(
					Display.getCurrent().getActiveShell(),
					_MP,
					_offlineWorldStart,
					_offlineWorldEnd,
					_mapZoomLevel).open();

			disableOfflineAreaSelection();

			return;
		}

		_previousOfflineArea = _currentOfflineArea;

		_offlineDevAreaStart = _offlineDevAreaEnd = null;

		_isPaintOfflineArea = true;
		_isSelectOfflineArea = true;

		setCursor(_cursorCross);

		redraw();
		queueMapRedraw();
	}

	private void addDropTarget(final Canvas canvas) {

		_dropTarget = new DropTarget(canvas, DND.DROP_MOVE | DND.DROP_COPY);
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
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						onDropRunnable(event);
					}
				});
			}
		});
	}

	public void addMousePositionListener(final IPositionListener mapListener) {
		_mousePositionListeners.add(mapListener);
	}

	/**
	 * Adds a map overlay. This is a Painter which will paint on top of the map. It can be used to
	 * draw waypoints, lines, or static overlays like text messages.
	 * 
	 * @param overlay
	 *            the map overlay to use
	 * @see org.jdesktop.swingx.painters.Painter
	 */
	public void addOverlayPainter(final MapPainter overlay) {
		_overlays.add(overlay);
		queueMapRedraw();
	}

	public void addPOIListener(final IPOIListener poiListener) {
		_poiListeners.add(poiListener);
	}

	public void addZoomListener(final IZoomListener listener) {
		_zoomListeners.add(listener);
	}

	/**
	 * make sure that the parted overlay image has the correct size
	 */
	private void checkImageTemplate9Parts() {

		final int parts = 3;
		final int tileSize = _MP.getTileSize();
		final int partedTileSize = tileSize * parts;

		if ((_image9Parts != null) && (_image9Parts.isDisposed() == false)) {
			if (_image9Parts.getBounds().width == partedTileSize) {
				// image is OK
				return;
			}
		}
		if (_image9Parts != null) {
			_image9Parts.dispose();
		}
		if (_gc9Parts != null) {
			_gc9Parts.dispose();
		}

		// create 9 part image/gc
		final ImageData transparentImageData = UI.createTransparentImageData(partedTileSize, _transparentRGB);

		_image9Parts = new Image(_display, transparentImageData);

		_gc9Parts = new GC(_image9Parts);
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

				if ((_MP == null) || _disableContextMenu) {
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

	private Composite createToolTip(final Shell shell) {

		final Display display = shell.getDisplay();
		final Color bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);

		final Composite container = new Composite(shell, SWT.NONE);
		container.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		container.setBackground(bgColor);
		GridLayoutFactory.swtDefaults().applyTo(container);
		{
			_poiTTLabel = new Text(container, SWT.WRAP | SWT.READ_ONLY);
			_poiTTLabel.setBackground(bgColor);
		}

		return container;
	}

	public synchronized void dimMap(final int dimLevel, final RGB dimColor) {

		_MP.setDimLevel(dimLevel, dimColor);

		// remove all cached map images
		_MP.disposeTileImages();

		resetAll();
	}

//	/**
//	 * Calculates (and sets) the greatest zoom level, so that all positions are visible on screen.
//	 * This is useful if you have a bunch of points in an area like a city and you want to zoom out
//	 * so that the entire city and it's points are visible without panning.
//	 *
//	 * @param positions
//	 *            A set of GeoPositions to calculate the new zoom from
//	 */
//	public void calculateZoomFrom(final Set<GeoPosition> positions) {
//		if (positions.size() < 2) {
//			return;
//		}
//
//		int zoom = _MP.getMinimumZoomLevel();
//		Rectangle rect = getBoundingRect(positions, zoom);
//
//		while (getViewport().contains(rect) && zoom < _MP.getMaximumZoomLevel()) {
//			zoom++;
//			rect = getBoundingRect(positions, zoom);
//		}
//		final Point2D center = new Point2D.Double(rect.getCenterX(), rect.getCenterY());
//
//		setMapPixelCenter(_MP.pixelToGeo(center, zoom), center);
//		setZoom(zoom);
//
//		queueMapRedraw();
//	}

	/**
	 * hide offline area and all states
	 */
	private void disableOfflineAreaSelection() {

		_isSelectOfflineArea = false;
		_isPaintOfflineArea = false;
		_disableContextMenu = false;
		_isOfflineSelectionStarted = false;

		setCursor(_cursorDefault);

		redraw();
	}

	/**
	 * Disposes all overlay image cache and the overlay painting queue
	 */
	public synchronized void disposeOverlayImageCache() {

		if (_MP != null) {
			_MP.resetOverlays();
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
		_MP.disposeTiles();
	}

	/**
	 * This method is called every time, the view port content has changed. It draws the map itself
	 * first and then the overlay(s). Draws everything to BufferedImage in the first step, which
	 * makes the code better portable to other GUI toolkits.
	 */
	private void drawMap() {

		if (isDisposed()) {
			return;
		}

		// Draw the map
		GC gc = null;
		try {

			// check or create map image
			Image image = _mapImage;
			if ((image == null) || image.isDisposed() || (canReuseImage(image, _clientArea) == false)) {
				image = createImage(_display, image, _clientArea);
			}
			_mapImage = image;

			gc = new GC(_mapImage);
			{
				drawMapTiles(gc);
				drawMapLegend(gc);

				if (_isScaleVisible) {
					drawMapScale(gc);
				}
			}

		} catch (final Exception e) {

			e.printStackTrace();

			// map image is corrupt
			_mapImage.dispose();

		} finally {
			if (gc != null) {
				gc.dispose();
			}
		}

		_drawTime = System.currentTimeMillis();

		redraw();
	}

	private void drawMapLegend(final GC gc) {

		if (_isLegendVisible == false) {
			return;
		}

		if (_mapLegend == null) {
			return;
		}

		// get legend image from the legend
		final Image legendImage = _mapLegend.getImage();
		if ((legendImage == null) || legendImage.isDisposed()) {
			return;
		}

		final org.eclipse.swt.graphics.Rectangle imageBounds = legendImage.getBounds();

		// draw legend on bottom left
		int yPos = _worldPixelViewport.height - 5 - imageBounds.height;
		yPos = Math.max(5, yPos);

		final Point legendPosition = new Point(5, yPos);
		_mapLegend.setLegendPosition(legendPosition);

		gc.drawImage(legendImage, legendPosition.x, legendPosition.y);
	}

	private void drawMapScale(final GC gc) {

		final int viewPortWidth = _worldPixelViewport.width;

		final int devScaleWidth = viewPortWidth / 3;
		final float metricWidth = 111.32f / _distanceUnitValue;

		final GeoPosition mapCenter = getGeoCenter();
		final double latitude = mapCenter.latitude;
		final double longitude = mapCenter.longitude;

		final double devDistance = _MP.getDistance(new GeoPosition(latitude - 0.5, longitude), new GeoPosition(
				latitude + 0.5,
				longitude), _mapZoomLevel);

		final double scaleLat = metricWidth * (devScaleWidth / devDistance);

//		if (scaleLat > 3000) {
//			// hide scale because it's getting inaccurate
//			return;
//		}

		// get scale text
		String scaleUI;
		if (scaleLat >= 100f) {
			scaleUI = Integer.toString((int) scaleLat);
		} else if (scaleLat >= 10f) {
			_nf.setMinimumFractionDigits(1);
			_nf.setMaximumFractionDigits(1);
			scaleUI = _nf.format(scaleLat);
		} else if (scaleLat >= 1f) {
			_nf.setMinimumFractionDigits(2);
			_nf.setMaximumFractionDigits(2);
			scaleUI = _nf.format(scaleLat);
		} else {
			_nf.setMinimumFractionDigits(3);
			_nf.setMaximumFractionDigits(3);
			scaleUI = _nf.format(scaleLat);
		}
		final String scaleText = scaleUI + UI.SPACE + _distanceUnitLabel;
		final Point textExtent = gc.textExtent(scaleText);

		final int devX1 = viewPortWidth - 5 - devScaleWidth;
		int devY = _worldPixelViewport.height - 5 - 3;
//		final int x1 = viewPortWidth / 2 - devScaleWidth / 2;
//		int devY = fMapViewport.height / 2;

		final int devX2 = devX1 + devScaleWidth;
		final int segmentWidth = devScaleWidth / 4;

		final int devYScaleLines = devY;

		final Color white = _display.getSystemColor(SWT.COLOR_WHITE);
		final Color black = _display.getSystemColor(SWT.COLOR_BLACK);
		final Color gray = _display.getSystemColor(SWT.COLOR_DARK_GRAY);

		drawMapScaleLine(gc, devX1, devX2, devY++, segmentWidth, gray, gray);
		drawMapScaleLine(gc, devX1, devX2, devY++, segmentWidth, white, black);
		drawMapScaleLine(gc, devX1, devX2, devY++, segmentWidth, white, black);
		drawMapScaleLine(gc, devX1, devX2, devY++, segmentWidth, white, black);
		drawMapScaleLine(gc, devX1, devX2, devY, segmentWidth, gray, gray);

		final int devYText = devYScaleLines - textExtent.y;
		final int devXText = devX1 + devScaleWidth - textExtent.x;

		final Color borderColor = new Color(_display, 0xF1, 0xEE, 0xE8);
		{
			gc.setForeground(borderColor);
			gc.drawText(scaleText, devXText - 1, devYText, true);
			gc.drawText(scaleText, devXText + 1, devYText, true);
			gc.drawText(scaleText, devXText, devYText - 1, true);
			gc.drawText(scaleText, devXText, devYText + 1, true);
		}
		borderColor.dispose();

		gc.setForeground(_display.getSystemColor(SWT.COLOR_BLACK));
		gc.drawText(scaleText, devXText, devYText, true);
	}

	private void drawMapScaleLine(	final GC gc,
									final int devX1,
									final int devX2,
									final int devY,
									final int segmentWidth,
									final Color firstColor,
									final Color secondColor) {

		gc.setForeground(_display.getSystemColor(SWT.COLOR_DARK_GRAY));
		gc.drawPoint(devX1, devY);

		gc.setForeground(firstColor);
		gc.drawLine(devX1 + 1, devY, (devX1 + segmentWidth), devY);

		gc.setForeground(secondColor);
		gc.drawLine(devX1 + segmentWidth, devY, devX1 + 2 * segmentWidth, devY);

		gc.setForeground(firstColor);
		gc.drawLine(devX1 + 2 * segmentWidth, devY, devX1 + 3 * segmentWidth, devY);

		gc.setForeground(secondColor);
		gc.drawLine(devX1 + 3 * segmentWidth, devY, devX2, devY);

		gc.setForeground(_display.getSystemColor(SWT.COLOR_DARK_GRAY));
		gc.drawPoint(devX2, devY);
	}

	/**
	 * Draw all visible tiles into the map viewport
	 * 
	 * @param gc
	 */
	private void drawMapTiles(final GC gc) {

		final int tileSize = _MP.getTileSize();

		/*
		 * draw all visible tiles
		 */
		for (int tilePosX = _tilePosMinX; tilePosX <= _tilePosMaxX; tilePosX++) {
			for (int tilePosY = _tilePosMinY; tilePosY <= _tilePosMaxY; tilePosY++) {

				// get device rectangle for this tile
				final Rectangle devTilePosition = new Rectangle(//
						tilePosX * tileSize - _worldViewportX,
						tilePosY * tileSize - _worldViewportY,
						tileSize,
						tileSize);

				// check if current tile is within the painting area
				if (devTilePosition.intersects(_devVisibleViewport)) {

					/*
					 * get the tile from the factory. the tile must not have been completely
					 * downloaded after this step.
					 */

					if (isTileOnMap(tilePosX, tilePosY, _mapTileSize)) {

						drawTile(gc, tilePosX, tilePosY, devTilePosition);

					} else {

						/*
						 * if tile is off the map to the north or south, draw map background
						 */

						gc.setBackground(_display.getSystemColor(SWT.COLOR_WHITE));
						gc.fillRectangle(devTilePosition.x, devTilePosition.y, tileSize, tileSize);
					}
				}
			}
		}
	}

	private void drawTile(final GC gc, final int tilePositionX, final int tilePositionY, final Rectangle devTilePosition) {

		// get tile from the map provider, this also starts the loading of the tile image
		final Tile tile = _MP.getTile(tilePositionX, tilePositionY, _mapZoomLevel);

		final Image tileImage = tile.getCheckedMapImage();
		if (tileImage != null) {

			// map image is available and valid

			gc.drawImage(tileImage, devTilePosition.x, devTilePosition.y);

		} else {
			drawTileImage(gc, tile, devTilePosition);
		}

		if (_isDrawOverlays) {
			drawTileOverlay(gc, tile, devTilePosition);
		}

		if (_isShowTileInfo) {
			drawTileInfo(gc, tile, devTilePosition);
		}
	}

	/**
	 * draw the tile map image
	 */
	private void drawTileImage(final GC gc, final Tile tile, final Rectangle devTilePosition) {

		if (tile.isLoadingError()) {

			// map image contains an error, it could not be loaded

			final Image errorImage = _MP.getErrorImage();
			final org.eclipse.swt.graphics.Rectangle imageBounds = errorImage.getBounds();

			gc.setBackground(_display.getSystemColor(SWT.COLOR_GRAY));
			gc.fillRectangle(devTilePosition.x, devTilePosition.y, imageBounds.width, imageBounds.height);

			drawTileInfoError(gc, devTilePosition, tile);

			return;
		}

		if (tile.isOfflineError()) {

			//map image could not be loaded from offline file

			gc.drawImage(_MP.getErrorImage(), devTilePosition.x, devTilePosition.y);

			drawTileInfoError(gc, devTilePosition, tile);

			return;
		}

		/*
		 * the tile image is not yet loaded, register an observer that handles redrawing when the
		 * tile image is available. Tile image loading is started, when the tile is retrieved from
		 * the tile factory which is done in drawTile()
		 */
		tile.addObserver(_tileLoadObserver);

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

				gc.drawImage(_MP.getLoadingImage(), devTilePosition.x, devTilePosition.y);
			}
		}
	}

	/**
	 * @param gc
	 * @param tile
	 * @param devTilePosition
	 */
	private void drawTileInfo(final GC gc, final Tile tile, final Rectangle devTilePosition) {

		final ConcurrentHashMap<String, Tile> childrenWithErrors = tile.getChildrenWithErrors();

		if (tile.isLoadingError()
				|| tile.isOfflineError()
				|| ((childrenWithErrors != null) && (childrenWithErrors.size() > 0))) {

			drawTileInfoError(gc, devTilePosition, tile);

			return;
		}

		final int tileSize = _MP.getTileSize();

		// draw tile border
		gc.setForeground(_display.getSystemColor(SWT.COLOR_DARK_GRAY));
		gc.drawRectangle(devTilePosition.x, devTilePosition.y, tileSize, tileSize);

		// draw tile info
		gc.setForeground(_display.getSystemColor(SWT.COLOR_WHITE));
		gc.setBackground(_display.getSystemColor(SWT.COLOR_DARK_BLUE));

		final int leftMargin = 10;

		drawTileInfoLatLon(gc, tile, devTilePosition, 10, leftMargin);
		drawTileInfoPosition(gc, devTilePosition, tile, 50, leftMargin);

		// draw tile image path/url
		final StringBuilder sb = new StringBuilder();

		drawTileInfoPath(tile, sb);

		_textWrapper.printText(
				gc,
				sb.toString(),
				devTilePosition.x + leftMargin,
				devTilePosition.y + 80,
				devTilePosition.width - 20);

	}

	private void drawTileInfoError(final GC gc, final Rectangle devTilePosition, final Tile tile) {

		final int tileSize = _MP.getTileSize();

		// draw tile border
		gc.setForeground(_display.getSystemColor(SWT.COLOR_DARK_GRAY));
		gc.drawRectangle(devTilePosition.x, devTilePosition.y, tileSize, tileSize);

		gc.setForeground(_display.getSystemColor(SWT.COLOR_WHITE));
		gc.setBackground(_display.getSystemColor(SWT.COLOR_DARK_MAGENTA));

		final int leftMargin = 10;

		drawTileInfoLatLon(gc, tile, devTilePosition, 10, leftMargin);
		drawTileInfoPosition(gc, devTilePosition, tile, 50, leftMargin);

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

		drawTileInfoPath(tile, sb);

		final ArrayList<Tile> tileChildren = tile.getChildren();
		if (tileChildren != null) {
			for (final Tile childTile : tileChildren) {
				drawTileInfoPath(childTile, sb);
			}
		}

		_textWrapper.printText(
				gc,
				sb.toString(),
				devTilePosition.x + leftMargin,
				devTilePosition.y + 80,
				devTilePosition.width - 20);
	}

	private void drawTileInfoLatLon(final GC gc,
									final Tile tile,
									final Rectangle devTilePosition,
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
		gc.drawString(sb.toString(), //
				devTilePosition.x + leftMargin,
				devTilePosition.y + topMargin);

		// lat - top
		gc.drawString(_nfLatLon.format(bbox.top), //
				devTilePosition.x + leftMargin + dev2ndColumn,
				devTilePosition.y + topMargin);

		sb.setLength(0);

		// lon - left
		sb.append(Messages.TileInfo_Position_Longitude);
		sb.append(_nfLatLon.format(bbox.left));
		gc.drawString(sb.toString(), //
				devTilePosition.x + leftMargin,
				devTilePosition.y + topMargin + devLineHeight);

		// lon - right
		gc.drawString(_nfLatLon.format(bbox.right), //
				devTilePosition.x + leftMargin + dev2ndColumn,
				devTilePosition.y + topMargin + devLineHeight);
	}

	/**
	 * !!! Recursive !!!
	 * 
	 * @param tile
	 * @param sb
	 */
	private void drawTileInfoPath(final Tile tile, final StringBuilder sb) {

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
				drawTileInfoPath(childTile, sb);
			}
		}
	}

	private void drawTileInfoPosition(	final GC gc,
										final Rectangle devTilePosition,
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

		gc.drawString(text.toString(), devTilePosition.x + leftMargin, devTilePosition.y + topMargin);
	}

	/**
	 * Draw overlay image when it's available or request the image
	 * 
	 * @param gc
	 * @param tile
	 * @param devTileRectangle
	 *            Position of the tile
	 */
	private void drawTileOverlay(final GC gc, final Tile tile, final Rectangle devTileRectangle) {

		/*
		 * Priority 1: draw overlay image
		 */
		final OverlayImageState imageState = tile.getOverlayImageState();
		final int overlayContent = tile.getOverlayContent();

		if ((imageState == OverlayImageState.IMAGE_IS_CREATED)
				|| ((imageState == OverlayImageState.NO_IMAGE) && (overlayContent == 0))) {
			// there is no image for the tile overlay or the image is currently created
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
				 * get image from the tile, it's possible that the part image is disposed but the
				 * tile image is still available
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
				gc.drawImage(drawingImage, devTileRectangle.x, devTileRectangle.y);
			} catch (final Exception e) {

				/*
				 * ignore, it's still possible that the image is disposed when the images are
				 * changing very often and the cache is small
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
			 * check if the tile overlay image (not the surrounding part images) is available, when
			 * not the image must be created
			 */
			if (tileOverlayImage == null) {
				tileOverlayImage = tile.getOverlayImage();
			}

			if ((tileOverlayImage == null) || tileOverlayImage.isDisposed()) {

				// overlay image is NOT available

				// check if tile has overlay content
				if (overlayContent == 0) {

					/**
					 * tile has no overlay content -> set state that the drawing of the
					 * overlay is as fast as possible
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
							//	queueOverlayPainting(tile);
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

	private void fireMousePositionEvent(final GeoPosition geoPosition) {

		final MapPositionEvent event = new MapPositionEvent(geoPosition, _mapZoomLevel);

		final Object[] listeners = _mousePositionListeners.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			((IPositionListener) listeners[i]).setPosition(event);
		}
	}

	private void firePOIEvent(final GeoPosition geoPosition, final String poiText) {

		final MapPOIEvent event = new MapPOIEvent(geoPosition, _mapZoomLevel, poiText);

		final Object[] listeners = _poiListeners.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			((IPOIListener) listeners[i]).setPOI(event);
		}
	}

	private void fireZoomEvent(final int zoom) {

		final ZoomEvent event = new ZoomEvent(zoom);
		for (final IZoomListener l : _zoomListeners) {
			l.zoomChanged(event);
		}
	}

	/**
	 * Gets the current address location of the map. This property does not change when the user
	 * pans the map. This property is bound.
	 * 
	 * @return the current map location (address)
	 */
	public GeoPosition getAddressLocation() {
		return _addressLocation;
	}

	public Rectangle getBoundingRect(final Set<GeoPosition> positions, final int zoom) {
		final java.awt.Point point1 = _MP.geoToPixel(positions.iterator().next(), zoom);
		final Rectangle rect = new Rectangle(point1.x, point1.y, 0, 0);

		for (final GeoPosition pos : positions) {
			final java.awt.Point point = _MP.geoToPixel(pos, zoom);
			rect.add(new Rectangle(point.x, point.y, 0, 0));
		}
		return rect;
	}

	/**
	 * A property indicating the center position of the map, or <code>null</code> when a tile
	 * factory is not set
	 * 
	 * @return Returns the current center position of the map in latitude/longitude
	 */
	public GeoPosition getGeoCenter() {

		if (_MP == null) {
			return null;
		}

		return _MP.pixelToGeo(_mapCenterInWorldPixel, _mapZoomLevel);
	}

	/**
	 * @return Returns the legend of the map
	 */
	public MapLegend getLegend() {
		return _mapLegend;
	}

	/**
	 * @return Returns the map viewport<br>
	 *         <b>x</b> and <b>y</b> contains the position in world pixel of the center <br>
	 *         <b>width</b> and <b>height</b> contains the visible area in device pixel
	 */
	public Rectangle getMapPixelViewport() {
		return getWorldPixelTopLeftViewport(_mapCenterInWorldPixel);
	}

	/**
	 * Get the current map provider
	 * 
	 * @return Returns the current map provider
	 */
	public MP getMapProvider() {
		return _MP;
	}

	private Point getOfflineAreaTilePosition(final int worldPosX, final int worldPosY) {

		final int tileSize = _MP.getTileSize();
		int tilePosX = (int) Math.floor((double) worldPosX / (double) tileSize);
		int tilePosY = (int) Math.floor((double) worldPosY / (double) tileSize);

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
				tilePosX * tileSize - _worldViewportX,
				tilePosY * tileSize - _worldViewportY);
	}

	/**
	 * @param tileKey
	 * @return Returns the key to identify overlay images in the image cache
	 */
	private String getOverlayKey(final Tile tile) {
		return _overlayKey + tile.getTileKey();
	}

//	/**
//	 * Gets the current pixel center of the map. This point is in the global bitmap coordinate
//	 * system, not as lat/longs.
//	 *
//	 * @return Returns the current center of the map in as a world pixel value
//	 */
//	public Point2D getCenterInWorldPixel() {
//		return _mapCenterInWorldPixel;
//	}

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

	public List<MapPainter> getOverlays() {
		return _overlays;
	}

	/**
	 * Returns the bounds of the viewport in pixels. This can be used to transform points into the
	 * world bitmap coordinate space. The viewport is the part of the map, that you can currently
	 * see on the screen.
	 * 
	 * @return Returns the bounds in <em>pixels</em> of the "view" of this map
	 */
	private Rectangle getWorldPixelTopLeftViewport(final Point2D mapPixelCenter) {

		if (_clientArea == null) {
			_clientArea = getClientArea();
		}

		// calculate the "visible" viewport area in pixels
		final int devWidth = _clientArea.width;
		final int devHeight = _clientArea.height;

		final int worldX = (int) (mapPixelCenter.getX() - devWidth / 2d);
		final int worldY = (int) (mapPixelCenter.getY() - devHeight / 2d);

		final Rectangle viewPort = new Rectangle(worldX, worldY, devWidth, devHeight);

		return viewPort;
	}

	/**
	 * Gets the current zoom level, or <code>null</code> when a tile
	 * factory is not set
	 * 
	 * @return Returns the current zoom level of the map
	 */
	public int getZoom() {
		return _mapZoomLevel;
	}

	private void hidePoiToolTip() {
		if (_poiTT != null) {
			_poiTT.hide();
		}
	}

	/**
	 * Indicates if the tile borders should be drawn. Mainly used for debugging.
	 * 
	 * @return the value of this property
	 */
	public boolean isDrawTileBorders() {
		return _isShowTileInfo;
	}

	/**
	 * Checks if a part within the parted image is modified. This method is optimized to search
	 * first all 4 borders and then the whole image.
	 * 
	 * @param imageData9Parts
	 * @param srcXStart
	 * @param srcYStart
	 * @param tileSize
	 * @return Returns <code>true</code> when a part is modified
	 */
	private boolean isPartImageModified(final ImageData imageData9Parts,
										final int srcXStart,
										final int srcYStart,
										final int tileSize) {

		final int transRed = _transparentRGB.red;
		final int transGreen = _transparentRGB.green;
		final int transBlue = _transparentRGB.blue;

		final byte[] srcData = imageData9Parts.data;
		final int srcBytesPerLine = imageData9Parts.bytesPerLine;

		int srcIndex;
		int srcRed, srcGreen, srcBlue;

		// check border: top
		{
			final int srcY = srcYStart;
			final int srcYBytesPerLine = srcY * srcBytesPerLine;

			for (int srcX = srcXStart; srcX < srcXStart + tileSize; srcX++) {

				srcIndex = srcYBytesPerLine + (srcX * 3);

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

				srcIndex = srcYBytesPerLine + (srcX * 3);

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
			final int srcX = srcXStart * 3;

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
			final int srcX = (srcXStart + tileSize - 1) * 3;

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

				srcIndex = srcYBytesPerLine + (srcX * 3);

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
	 * Indicates if the map should recenter itself on mouse clicks.
	 * 
	 * @return boolean indicating if the map should recenter itself
	 */
	public boolean isRecenterOnClickEnabled() {
		return _recenterOnClickEnabled;
	}

	private boolean isTileOnMap(final int x, final int y, final Dimension mapSize) {

		if (y >= mapSize.getHeight()) {
			return false;
		} else if (y < 0) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * A property indicating if the map should be zoomable by the user using the mouse wheel.
	 * 
	 * @return the current property value
	 */
	public boolean isZoomEnabled() {
		return _zoomEnabled;
	}

	public boolean isZoomOnDoubleClickEnabled() {
		return _zoomOnDoubleClickEnabled;
	}

	/**
	 * onDispose is called when the map is disposed
	 * 
	 * @param e
	 */
	private void onDispose(final DisposeEvent e) {

		if (_MP != null) {
			_MP.resetAll(false);
		}
		if (_dropTarget != null) {
			_dropTarget.dispose();
		}

		disposeResource(_mapImage);
		disposeResource(_poiImage);

		disposeResource(_image9Parts);
		disposeResource(_gc9Parts);

		disposeResource(_cursorDefault);
		disposeResource(_cursorPan);
		disposeResource(_cursorCross);

		disposeResource(_defaultBackgroundColor);
		disposeResource(_transparentColor);

		// dispose resources in the overlay plugins
		for (final MapPainter overlay : getOverlays()) {
			overlay.dispose();
		}

		_overlayImageCache.dispose();

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

			MessageDialog.openInformation(getShell(), //
					Messages.Dialog_DropNoPOI_Title,
					NLS.bind(Messages.Dialog_DropNoPOI_Message, poiText));
		}
	}

	private void onMouseDown(final MouseEvent e) {

		// check if left mouse button is pressed
		if (e.button != 1) {
			return;
		}

		hidePoiToolTip();

		if (_isSelectOfflineArea) {

			_isOfflineSelectionStarted = true;

			final Rectangle viewPort = _worldPixelViewport;
			final int worldMouseX = viewPort.x + e.x;
			final int worldMouseY = viewPort.y + e.y;

			_offlineDevAreaStart = _offlineDevAreaEnd = new Point(e.x, e.y);
			_offlineWorldStart = _offlineWorldEnd = new Point(worldMouseX, worldMouseY);

			_offlineDevTileStart = getOfflineAreaTilePosition(worldMouseX, worldMouseY);

			redraw();

		} else {

			// if the left mb is clicked remember this point (for panning)
			_mousePanPosition = new Point(e.x, e.y);
			_isLeftMouseButtonPressed = true;

			setCursor(_cursorPan);
		}
	}

	private void onMouseMove(final MouseEvent event) {

		final int devMouseX = event.x;
		final int devMouseY = event.y;

		_mouseMovePosition = new Point(devMouseX, devMouseY);

		if (_isSelectOfflineArea) {

			final Rectangle viewPort = _worldPixelViewport;
			_offlineWorldMouseMove = new Point(viewPort.x + devMouseX, viewPort.y + devMouseY);

			updateOfflineAreaEndPosition(event);

			queueMapRedraw();

			return;
		}

		if (_isLeftMouseButtonPressed) {
			panMap(event);
			return;
		}

		if ((_poiGeoPosition != null) && (_isPoiPositionInViewport)) {

			// display poi info

			// check if mouse is within the poi image
			if (_isPoiVisible
					&& (devMouseX > _poiImageDevPosition.x)
					&& (devMouseX < _poiImageDevPosition.x + _poiImageBounds.width)
					&& (devMouseY > _poiImageDevPosition.y - _poiTTOffsetY - 5)
					&& (devMouseY < _poiImageDevPosition.y + _poiImageBounds.height)) {

				showPoiInfo();

			} else {
				hidePoiToolTip();
			}
		}

		updateMouseMapPosition();
	}

	private void onMouseUp(final MouseEvent event) {

		final int devMouseX = event.x;
		final int devMouseY = event.y;

		if (_isSelectOfflineArea) {

			_disableContextMenu = true;

			if (_isOfflineSelectionStarted == false) {
				/*
				 * offline selection is not started, this can happen when the right mouse button is
				 * clicked
				 */
				disableOfflineAreaSelection();

				return;
			}

			updateOfflineAreaEndPosition(event);

			_isSelectOfflineArea = false;

			// hide previous area
			_previousOfflineArea = null;

			setCursor(_cursorDefault);
			redraw();
			queueMapRedraw();

			new DialogManageOfflineImages(
					Display.getCurrent().getActiveShell(),
					_MP,
					_offlineWorldStart,
					_offlineWorldEnd,
					_mapZoomLevel).open();

			disableOfflineAreaSelection();

		} else {

			if (event.button == 1) {
				if (_isMapPanned) {
					_isMapPanned = false;
					redraw();
				}
				_mousePanPosition = null;
				_isLeftMouseButtonPressed = false;
				setCursor(_cursorDefault);

			} else if (event.button == 2) {
				// if the middle mouse button is clicked, recenter the view
				if (isRecenterOnClickEnabled()) {
					recenterMap(event.x, event.y);
				}
			}
		}

		// show poi info when mouse is within the poi image
		if ((devMouseX > _poiImageDevPosition.x)
				&& (devMouseX < _poiImageDevPosition.x + _poiImageBounds.width)
				&& (devMouseY > _poiImageDevPosition.y - _poiTTOffsetY - 5)
				&& (devMouseY < _poiImageDevPosition.y + _poiImageBounds.height)) {

			showPoiTooltip();
		}

	}

	/*
	 * There are far too many calls from SWT on this method. Much more than would bereally needed. I
	 * don't know why this is. As a result of this, the Component uses up much CPU, because it runs
	 * through all the tile loading code for every call. The tile loading code should only be
	 * called, if something has changed. When something has changed we produce a buffer image with
	 * the contents of the view port (Double/Triple buffer). This happens in the queueRedraw()
	 * method. The image gets painted on every call of this method.
	 */
	private void onPaint(final PaintEvent event) {

		// draw map image to the screen

		if ((_mapImage != null) && !_mapImage.isDisposed()) {

			final GC gc = event.gc;
			gc.drawImage(_mapImage, 0, 0);

			if (_directMapPainter != null) {
				_directMapPainterContext.gc = gc;
				_directMapPainterContext.viewport = _worldPixelViewport;
				_directMapPainter.paint(_directMapPainterContext);
			}

			if (_isPoiVisible && (_poiGeoPosition != null)) {
				if (_isPoiPositionInViewport = updatePoiImageDevPosition()) {
					gc.drawImage(_poiImage, _poiImageDevPosition.x, _poiImageDevPosition.y);
				}
			}

			if (_isPaintOfflineArea) {
				paintOfflineArea(gc);
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

		queueMapRedraw();
	}

	private void paintOfflineArea(final GC gc) {

		gc.setLineWidth(1);

		/*
		 * draw previous area box
		 */
		if (_previousOfflineArea != null) {

			gc.setLineStyle(SWT.LINE_SOLID);
			gc.setForeground(_display.getSystemColor(SWT.COLOR_WHITE));
			gc.drawRectangle(_previousOfflineArea);

			gc.setForeground(_display.getSystemColor(SWT.COLOR_GRAY));
			final int devX = _previousOfflineArea.x;
			final int devY = _previousOfflineArea.y;
			gc.drawRectangle(//
					devX + 1,
					devY + 1,
					_previousOfflineArea.width - 2,
					_previousOfflineArea.height - 2);

			/*
			 * draw text marker
			 */
			gc.setForeground(_display.getSystemColor(SWT.COLOR_BLACK));
			gc.setBackground(_display.getSystemColor(SWT.COLOR_WHITE));
			final Point textExtend = gc.textExtent(Messages.Offline_Area_Label_OldAreaMarker);
			int devYMarker = devY - textExtend.y;
			devYMarker = devYMarker < 0 ? 0 : devYMarker;
			gc.drawText(Messages.Offline_Area_Label_OldAreaMarker, devX, devYMarker);
		}

		/*
		 * show info in the top/right corner that selection for the offline area is activ
		 */
		if (_isSelectOfflineArea) {

			gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
			gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

			final StringBuilder sb = new StringBuilder();
			sb.append(Messages.Offline_Area_Label_SelectInfo);

			if (_offlineDevAreaStart != null) {

				// display offline area geo position

				final GeoPosition geoStart = _MP.pixelToGeo(new Point2D.Double(
						_offlineWorldStart.x,
						_offlineWorldStart.y), _mapZoomLevel);

				sb.append("   "); //$NON-NLS-1$
				sb.append(_nfLatLon.format(geoStart.latitude));
				sb.append(" / "); //$NON-NLS-1$
				sb.append(_nfLatLon.format(geoStart.longitude));

				final GeoPosition geoEnd = _MP.pixelToGeo(new Point2D.Double(//
						_offlineWorldEnd.x,
						_offlineWorldEnd.y), _mapZoomLevel);

				sb.append(" - "); //$NON-NLS-1$
				sb.append(_nfLatLon.format(geoEnd.latitude));
				sb.append(" / "); //$NON-NLS-1$
				sb.append(_nfLatLon.format(geoEnd.longitude));

			} else {

				// display mouse move geo position

				if (_offlineWorldMouseMove != null) {

					final GeoPosition mouseGeo = _MP.pixelToGeo(new Point2D.Double(
							_offlineWorldMouseMove.x,
							_offlineWorldMouseMove.y), _mapZoomLevel);

					sb.append("   "); //$NON-NLS-1$
					sb.append(_nfLatLon.format(mouseGeo.latitude));
					sb.append("/"); //$NON-NLS-1$
					sb.append(_nfLatLon.format(mouseGeo.longitude));

				}
			}

			gc.drawText(sb.toString(), 0, 0);
		}

		// check if mouse button is hit which sets the start position
		if ((_offlineDevAreaStart == null) || (_offlineWorldMouseMove == null)) {
			return;
		}

		/*
		 * draw tile box for tiles which are selected within the area box
		 */
		final int tileSize = _MP.getTileSize();
		final int devTileStartX = _offlineDevTileStart.x;
		final int devTileStartY = _offlineDevTileStart.y;
		final int devTileEndX = _offlineDevTileEnd.x;
		final int devTileEndY = _offlineDevTileEnd.y;

		final int devTileStartX2 = Math.min(devTileStartX, devTileEndX);
		final int devTileStartY2 = Math.min(devTileStartY, devTileEndY);
		final int devTileEndX2 = Math.max(devTileStartX, devTileEndX);
		final int devTileEndY2 = Math.max(devTileStartY, devTileEndY);

		for (int devX = devTileStartX2; devX <= devTileEndX2; devX += tileSize) {
			for (int devY = devTileStartY2; devY <= devTileEndY2; devY += tileSize) {

//				gc.setLineStyle(SWT.LINE_SOLID);
//				gc.setForeground(_display.getSystemColor(SWT.COLOR_YELLOW));
//				gc.drawRectangle(devX, devY, tileSize, tileSize);
//
//				gc.setLineStyle(SWT.LINE_DASH);
//				gc.setForeground(_display.getSystemColor(SWT.COLOR_DARK_GRAY));
//				gc.drawRectangle(devX, devY, tileSize, tileSize);
			}
		}

		/*
		 * draw selected area box
		 */
		final int devAreaX1 = _offlineDevAreaStart.x;
		final int devAreaX2 = _offlineDevAreaEnd.x;
		final int devAreaY1 = _offlineDevAreaStart.y;
		final int devAreaY2 = _offlineDevAreaEnd.y;
		final int devX;
		final int devY;
		final int devWidth;
		final int devHeight;

		if (devAreaX1 < devAreaX2) {
			devX = devAreaX1;
			devWidth = devAreaX2 - devAreaX1;
		} else {
			devX = devAreaX2;
			devWidth = devAreaX1 - devAreaX2;
		}
		if (devAreaY1 < devAreaY2) {
			devY = devAreaY1;
			devHeight = devAreaY2 - devAreaY1;
		} else {
			devY = devAreaY2;
			devHeight = devAreaY1 - devAreaY2;
		}

		_currentOfflineArea = new org.eclipse.swt.graphics.Rectangle(devX, devY, devWidth, devHeight);

		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setForeground(_display.getSystemColor(SWT.COLOR_BLACK));
		gc.drawRectangle(devX, devY, devWidth, devHeight);

		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setForeground(_display.getSystemColor(SWT.COLOR_WHITE));
//		gc.drawRectangle(devX + 1, devY + 1, devWidth - 2, devHeight - 2);

		gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_YELLOW));
		gc.setAlpha(0x30);
		gc.fillRectangle(devX + 1, devY + 1, devWidth - 2, devHeight - 2);
		gc.setAlpha(0xff);

		/*
		 * draw text marker
		 */
		gc.setForeground(_display.getSystemColor(SWT.COLOR_BLACK));
		gc.setBackground(_display.getSystemColor(SWT.COLOR_WHITE));
		final Point textExtend = gc.textExtent(Messages.Offline_Area_Label_AreaMarker);
		int devYMarker = devY - textExtend.y;
		devYMarker = devYMarker < 0 ? 0 : devYMarker;
		gc.drawText(Messages.Offline_Area_Label_AreaMarker, devX, devYMarker);
	}

	private void paintOverlay10() {

		if (isDisposed()) {
			return;
		}

		_overlayRunnableCounter++;

		final Runnable overlayRunnable = new Runnable() {

			final int	runnableCounter	= _overlayRunnableCounter;

			@Override
			public void run() {

				if (isDisposed()) {
					return;
				}

				// check if a newer runnable is available
				if (runnableCounter != _overlayRunnableCounter) {
					return;
				}

				_isDrawOverlayRunning = true;

				try {

					paintOverlay20Tiles();

				} catch (final Exception e) {
					e.printStackTrace();
				} finally {
					_isDrawOverlayRunning = false;
				}
			}
		};

		_display.asyncExec(overlayRunnable);
	}

	private void paintOverlay20Tiles() {

		BusyIndicator.showWhile(_display, new Runnable() {
			@Override
			public void run() {

				Tile tile;

				checkImageTemplate9Parts();

				while ((tile = _tileOverlayPaintQueue.poll()) != null) {

					// skip tiles from another zoom level
					if (tile.getZoom() == _mapZoomLevel) {

						// set state that this tile is checked that it contains tours
						tile.setOverlayTourStatus(OverlayTourState.TILE_IS_CHECKED);

						/*
						 * check if the tour is within the tile
						 */
						boolean isPaintingNeeded = false;
						for (final MapPainter overlayPainter : getOverlays()) {

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

						if (_isTourPaintMethodEnhanced) {
							paintOverlay30PaintTile(tile);
						} else {
							paintOverlay80PaintTile(tile);
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
	 * Paints the overlay into the overlay image which is bigger than the tile image so that the
	 * drawings are not clipped at the tile border. The overlay image is afterwards splitted into
	 * parts which are drawn into the tile images
	 * 
	 * @param tile
	 */
	private void paintOverlay30PaintTile(final Tile tile) {

		final int parts = 3;

		boolean isOverlayPainted = false;

		{
			// clear 9 part image
			_gc9Parts.setBackground(_transparentColor);
			_gc9Parts.fillRectangle(_image9Parts.getBounds());

			// paint all overlays for the current tile
			for (final MapPainter overlay : getOverlays()) {

				final boolean isPainted = overlay.doPaint(_gc9Parts, Map.this, tile, parts);

				isOverlayPainted = isOverlayPainted || isPainted;
			}

			if (isOverlayPainted) {

				tile.setOverlayImageState(OverlayImageState.IMAGE_IS_CREATED);

				final ImageData imageData9Parts = _image9Parts.getImageData();

				/**
				 * overlay image is created, split overlay image into 3*3 part images where the
				 * center image is the requested tile image
				 */

				final Job splitJob = new Job("SplitOverlayImages() " + _jobCounterSplitImages++) { //$NON-NLS-1$

					@Override
					protected IStatus run(final IProgressMonitor monitor) {

						paintOverlay40SplitParts(tile, imageData9Parts);

						queueMapRedraw();

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
	 * 0,0		0,1		0,2
	 * 1,0		1,1		1,2
	 * 2,0		2,1		2,2
	 * 
	 * </pre>
	 * 
	 * @param tile
	 * @param imageData9Parts
	 */
	private void paintOverlay40SplitParts(final Tile tile, final ImageData imageData9Parts) {

		final TileCache tileCache = MP.getTileCache();

		final String projectionId = _MP.getProjection().getId();
		final int tileSize = _MP.getTileSize();

		final int tileZoom = tile.getZoom();
		final int tileX = tile.getX();
		final int tileY = tile.getY();
		final int maxTiles = (int) Math.pow(2, tileZoom);

		for (int yIndex = 0; yIndex < 3; yIndex++) {
			for (int xIndex = 0; xIndex < 3; xIndex++) {

				// check if the tile is within the map border
				if (((tileX - xIndex < 0) || (tileX + xIndex > maxTiles))
						|| ((tileY - yIndex < 0) || (tileY + yIndex > maxTiles))) {
					continue;
				}

				final int devXFrom = tileSize * xIndex;
				final int devYFrom = tileSize * yIndex;

				// check if there are any drawings in the current part
				if (isPartImageModified(imageData9Parts, devXFrom, devYFrom, tileSize) == false) {

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
							devXFrom,
							devYFrom,
							tileSize,
							tileSize);

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

						neighborTile = new Tile(mp, tile.getZoom(), //
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
							devXFrom,
							devYFrom,
							tileSize,
							tileSize);

					if (isNeighborTileCreated == false) {

						/*
						 * create overlay image only when the neighbor tile was not created so that
						 * the normal tile image loading happens
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

				/*
				 * keep image in the cache that not too much image resources are created and
				 * that all images can be disposed
				 */
				if (tileOverlayImage != null) {
					_overlayImageCache.add(partImageKey, tileOverlayImage);
				}
			}
		}
	}

	/**
	 * Paint the tour in basic mode
	 * 
	 * @param tile
	 */
	private void paintOverlay80PaintTile(final Tile tile) {

		boolean isOverlayPainted = false;

		// create 1 part image/gc
		final ImageData transparentImageData = UI.createTransparentImageData(_MP.getTileSize(), _transparentRGB);

		final Image overlayImage = new Image(_display, transparentImageData);
		final GC gc1Part = new GC(overlayImage);
		{
			// paint all overlays for the current tile
			for (final MapPainter mapPainter : getOverlays()) {

				final boolean isPainted = mapPainter.doPaint(gc1Part, Map.this, tile, 1);

				isOverlayPainted = isOverlayPainted || isPainted;
			}
		}
		gc1Part.dispose();

		if (isOverlayPainted) {

			// overlay is painted

			final String overlayKey = getOverlayKey(tile, 0, 0, _MP.getProjection().getId());

			tile.setOverlayImage(overlayImage);
			_overlayImageCache.add(overlayKey, overlayImage);

			// set tile state
			tile.setOverlayImageState(OverlayImageState.TILE_HAS_CONTENT);
			tile.incrementOverlayContent();

			queueMapRedraw();

		} else {

			// image is not needed
			overlayImage.dispose();

			// set tile state
			tile.setOverlayImageState(OverlayImageState.NO_IMAGE);
		}
	}

	/**
	 * pan the map
	 */
	private void panMap(final MouseEvent mouseEvent) {

		/*
		 * set new map center
		 */
		final Point movePosition = new Point(mouseEvent.x, mouseEvent.y);
		final double mapPixelCenterX = _mapCenterInWorldPixel.getX();
		final double mapPixelCenterY = _mapCenterInWorldPixel.getY();
		final double newCenterX = mapPixelCenterX - (movePosition.x - _mousePanPosition.x);
		double newCenterY = mapPixelCenterY - (movePosition.y - _mousePanPosition.y);

		if (newCenterY < 0) {
			newCenterY = 0;
		}

		final int maxHeight = (int) (_MP.getMapTileSize(_mapZoomLevel).getHeight() * _MP.getTileSize());
		if (newCenterY > maxHeight) {
			newCenterY = maxHeight;
		}

		final Point2D.Double mapCenter = new Point2D.Double(newCenterX, newCenterY);
		setMapCenterInWoldPixel(mapCenter);

		_mousePanPosition = movePosition;

		// force a repaint of the moved map
		_isMapPanned = true;
		redraw();

		updateViewPortData();

		queueMapRedraw();
	}

	private boolean parsePOIText(String text) {

		try {
			text = URLDecoder.decode(text, "UTF-8"); //$NON-NLS-1$
		} catch (final UnsupportedEncodingException e) {
			StatusUtil.log(e);
		}

		// parse wiki url
		final Matcher wikiUrlMatcher = _patternWikiUrl.matcher(text);
		if (wikiUrlMatcher.matches()) {

			// osm url was found

			final String pageName = wikiUrlMatcher.group(1);
			final String position = wikiUrlMatcher.group(2);

			if (position != null) {

				double lat = 0;
				double lon = 0;
				String otherParams = null;

				//	match D;D

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

					//	match D_N_D_E

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

//								String dim = null;
					String type = null;

					final String[] allKeyValues = _patternWikiParamter.split(otherParams);

					for (final String keyValue : allKeyValues) {

						final String[] splittedKeyValue = _patternWikiKeyValue.split(keyValue);

						if (splittedKeyValue.length > 1) {

							if (splittedKeyValue[0].startsWith(WIKI_PARAMETER_TYPE)) {
								type = splittedKeyValue[1];
//										} else if (splittedKeyValue[0].startsWith(WIKI_PARAMETER_DIM)) {
//											dim = splittedKeyValue[1];
							}
						}
					}

					/*
					 * !!! disabled because the zoom level is not correct !!!
					 */
//								if (dim != null) {
//									final int scale = Integer.parseInt(dim);
//									zoom = (int) (18 - (Math.round(Math.log(scale) - Math.log(1693)))) - 1;//, [2, 18];
//								} else

					if (type != null) {

// source: https://wiki.toolserver.org/view/GeoHack
//
// type: 	 										ratio 	 		m / pixel	{scale} 	{mmscale}	{span}	{altitude}	{zoom}	{osmzoom}
//
// country, satellite 								1 : 10,000,000 	3528 		10000000 	10000000 	10.0 	1430 		1 		5
// state 											1 : 3,000,000 	1058 		3000000 	4000000 	3.0 	429 		3 		7
// adm1st 											1 : 1,000,000 	353 		1000000 	1000000 	1.0 	143 		4 		9
// adm2nd (default) 								1 : 300,000 	106 		300000 		200000 		0.3 	42 			5 		11
// adm3rd, city, mountain, isle, river, waterbody 	1 : 100,000 	35.3 		100000 		100000 		0.1 	14 			6 		12
// event, forest, glacier 							1 : 50,000 		17.6 		50000 		50000 		0.05 	7 			7 		13
// airport 											1 : 30,000 		10.6 		30000 		25000 		0.03 	4 			7 		14
// edu, pass, landmark, railwaystation 				1 : 10,000 		3.53 		10000 		10000 		0.01 	1 			8 		15

						if (type.equals("country") // 				//$NON-NLS-1$
								|| type.equals("satellite")) { //	//$NON-NLS-1$
							zoom = 5 - 1;
						} else if (type.equals("state")) { //		//$NON-NLS-1$
							zoom = 7 - 1;
						} else if (type.equals("adm1st")) { //		//$NON-NLS-1$
							zoom = 9 - 1;
						} else if (type.equals("adm2nd")) { //		//$NON-NLS-1$
							zoom = 11 - 1;
						} else if (type.equals("adm3rd") //			//$NON-NLS-1$
								|| type.equals("city") //			//$NON-NLS-1$
								|| type.equals("mountain") //		//$NON-NLS-1$
								|| type.equals("isle") //			//$NON-NLS-1$
								|| type.equals("river") //			//$NON-NLS-1$
								|| type.equals("waterbody")) { //	//$NON-NLS-1$
							zoom = 12 - 1;
						} else if (type.equals("event")//			//$NON-NLS-1$
								|| type.equals("forest") // 		//$NON-NLS-1$
								|| type.equals("glacier")) { //		//$NON-NLS-1$
							zoom = 13 - 1;
						} else if (type.equals("airport")) { //		//$NON-NLS-1$
							zoom = 14 - 1;
						} else if (type.equals("edu") //			//$NON-NLS-1$
								|| type.equals("pass") //			//$NON-NLS-1$
								|| type.equals("landmark") //		//$NON-NLS-1$
								|| type.equals("railwaystation")) { //$NON-NLS-1$
							zoom = 15 - 1;
						}
					}

					_poiGeoPosition = new GeoPosition(lat, lon);
					_poiTTText = pageName.replace('_', ' ');

					setZoom(zoom);
					setGeoCenterPosition(_poiGeoPosition);

					// hide previous tooltip
					hidePoiToolTip();

					_isPoiVisible = true;

					firePOIEvent(_poiGeoPosition, _poiTTText);

					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Put a map redraw into a queue, the last entry in the queue will be executed
	 */
	public void queueMapRedraw() {

		if (isDisposed() || (_MP == null)) {
			return;
		}

		if (_devVisibleViewport == null) {
			// viewport is not yet initialized, this happens only the first time when a map is displayed
			updateViewPortData();
		}

		_redrawMapCounter++;
		_requestedRedrawTime = System.currentTimeMillis();

		if (_requestedRedrawTime - _drawTime > 50) {

			// update display even when this is not the last created runnable

			final Runnable synchImageRunnable = new Runnable() {

				final int	fSynchRunnableCounter	= _redrawMapCounter;

				@Override
				public void run() {

					if (isDisposed()) {
						return;
					}

					// check if a newer runnable is available
					if (fSynchRunnableCounter != _redrawMapCounter) {
						// a newer queryRedraw is available
						return;
					}

					drawMap();
				}
			};

			_display.syncExec(synchImageRunnable);

			/*
			 * set an additional asynch runnable because it's possible that the synch runnable do
			 * not draw all tiles
			 */
			_display.asyncExec(synchImageRunnable);

		} else {

			final Runnable asynchImageRunnable = new Runnable() {

				final int	fAsynchRunnableCounter	= _redrawMapCounter;

				@Override
				public void run() {

					if (isDisposed()) {
						return;
					}

					// check if a newer runnable is available
					if (fAsynchRunnableCounter != _redrawMapCounter) {
						// a newer queryRedraw is available
						return;
					}

					drawMap();
				}
			};

			_display.asyncExec(asynchImageRunnable);
		}

		// tell the overlay thread to draw the overlay images
		_nextOverlayRedrawTime = _requestedRedrawTime;
	}

	/**
	 * Set tile in the overlay painting queue
	 * 
	 * @param tile
	 */
	private void queueOverlayPainting(final Tile tile) {

		tile.setOverlayTourStatus(OverlayTourState.IS_QUEUED);
		tile.setOverlayImageState(OverlayImageState.NOT_SET);

		_tileOverlayPaintQueue.add(tile);
	}

	private void recenterMap(final int ex, final int ey) {
		final Rectangle bounds = getMapPixelViewport();
		final double x = bounds.getX() + ex;
		final double y = bounds.getY() + ey;
		final Point2D.Double pixelCenter = new Point2D.Double(x, y);
		setMapCenterInWoldPixel(pixelCenter);
		queueMapRedraw();
	}

	/**
	 * Re-centers the map to have the current address location be at the center of the map,
	 * accounting for the map's width and height.
	 * 
	 * @see getAddressLocation
	 */
	public void recenterToAddressLocation() {
		final GeoPosition geoLocation = getAddressLocation();
		setMapCenterInWoldPixel(_MP.geoToPixel(geoLocation, _mapZoomLevel));
		queueMapRedraw();
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
		queueMapRedraw();
	}

	public void removeZoomListener(final IZoomListener listner) {
		_zoomListeners.remove(listner);
	}

	/**
	 * Reload the map by discarding all cached tiles and entries in the loading queue
	 */
	public synchronized void resetAll() {

		_MP.resetAll(false);

		queueMapRedraw();
	}

	/**
	 * Gets the current address location of the map
	 * 
	 * @param addressLocation
	 *            the new address location
	 * @see getAddressLocation()
	 */
	public void setAddressLocation(final GeoPosition addressLocation) {
		_addressLocation = addressLocation;
		setMapCenterInWoldPixel(_MP.geoToPixel(addressLocation, _mapZoomLevel));
		queueMapRedraw();
	}

	/**
	 * Set map dimming level for the current map factory, this will dimm the map images
	 * 
	 * @param mapDimLevel
	 * @param dimColor
	 */
	public void setDimLevel(final int mapDimLevel, final RGB dimColor) {
		if (_MP != null) {
			_MP.setDimLevel(mapDimLevel, dimColor);
		}
	}

	public void setDirectPainter(final IDirectPainter directPainter) {
		_directMapPainter = directPainter;
	}

	/**
	 * Set the center of the map to a geo position (with lat/long)
	 * 
	 * @param geoPosition
	 *            Center position in lat/lon
	 */
	public void setGeoCenterPosition(final GeoPosition geoPosition) {

		if (Thread.currentThread() == _displayThread) {

			setMapCenterInWoldPixel(_MP.geoToPixel(geoPosition, _mapZoomLevel));

		} else {

			// current thread is not the display thread

			_display.syncExec(new Runnable() {
				@Override
				public void run() {
					if (!isDisposed()) {
						setMapCenterInWoldPixel(_MP.geoToPixel(geoPosition, _mapZoomLevel));
					}
				}
			});
		}

		updateViewPortData();

		queueMapRedraw();
	}

	/**
	 * Set the legend for the map, the legend image will be disposed when the map is disposed,
	 * 
	 * @param legend
	 *            Legend for the map or <code>null</code> to disable the legend
	 */
	public void setLegend(final MapLegend legend) {

		if ((legend == null) && (_mapLegend != null)) {
			// dispose legend image
			disposeResource(_mapLegend.getImage());
		}

		_mapLegend = legend;
	}

	public void setLiveView(final boolean isLiveView) {
		_isLiveView = isLiveView;
	}

	/**
	 * Sets the center of the map in pixel coordinates.
	 * 
	 * @param centerInWorldPixel
	 */
	private void setMapCenterInWoldPixel(Point2D centerInWorldPixel) {

		/*
		 * check if the center is within the map
		 */

		final int viewportPixelHeight = getMapPixelViewport().height;
		final Rectangle newTopLeftPixelVP = getWorldPixelTopLeftViewport(centerInWorldPixel);

		// don't let the user pan over the top edge
		if (newTopLeftPixelVP.y < 0) {
			final double centerY = viewportPixelHeight / 2d;
			centerInWorldPixel = new Point2D.Double(centerInWorldPixel.getX(), centerY);
		}

		// don't let the user pan over the bottom edge
		final Dimension mapTileSize = _MP.getMapTileSize(_mapZoomLevel);
		final int mapHeight = (int) mapTileSize.getHeight() * _MP.getTileSize();

		if (newTopLeftPixelVP.y + newTopLeftPixelVP.height > mapHeight) {
			final double centerY = mapHeight - viewportPixelHeight / 2;
			centerInWorldPixel = new Point2D.Double(centerInWorldPixel.getX(), centerY);
		}

		// if map is too small then just center it
		if (mapHeight < newTopLeftPixelVP.height) {
			final double centerY = mapHeight / 2d;
			centerInWorldPixel = new Point2D.Double(centerInWorldPixel.getX(), centerY);
		}

		_mapCenterInWorldPixel = centerInWorldPixel;

		updateMouseMapPosition();

		/*
		 * hide previous offline area when map position has changed because the area has no absolute
		 * position
		 */
//		_currentOfflineArea = null;
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
	 * Sets the map provider for the map and redraws the map
	 * 
	 * @param mp
	 *            new map provider
	 */
	public void setMapProvider(final MP mp) {

		GeoPosition center = null;
		int zoom = 0;
		boolean refresh = false;

		if (_MP != null) {
			center = getGeoCenter();
			zoom = _mapZoomLevel;
			refresh = true;
		}

		_MP = mp;

		// check if the map is initialized
		if (_worldPixelViewport == null) {
			onResize();
		}

		if (refresh) {
			setZoom(zoom);
			setGeoCenterPosition(center);
		} else {
			setZoom(mp.getDefaultZoomLevel());
		}

		queueMapRedraw();
	}

	/**
	 * Resets current tile factory and sets a new one. The new tile factory is displayed at the same
	 * position as the previous tile factory
	 * 
	 * @param mp
	 * @param isDrawMap
	 *            When <code>true</code> the map is queued to be redrawn
	 */
	public synchronized void setMapProviderWithReset(final MP mp, final boolean isDrawMap) {

		if (_MP != null) {
			// keep tiles with loading errors that they are not loaded again when the factory has not changed
			_MP.resetAll(_MP == mp);
		}

		_MP = mp;

		if (isDrawMap) {
			queueMapRedraw();
		}
	}

	public void setMeasurementSystem(final float distanceUnitValue, final String distanceUnitLabel) {
		_distanceUnitValue = distanceUnitValue;
		_distanceUnitLabel = distanceUnitLabel;
	}

//	public void setRestrictOutsidePanning(final boolean restrictOutsidePanning) {
//		this._restrictOutsidePanning = restrictOutsidePanning;
//	}

	/**
	 * Set a key to uniquely identify overlays which is used to cache the overlays
	 * 
	 * @param key
	 */
	public void setOverlayKey(final String key) {
		_overlayKey = key;
	}

	public void setPOI(final GeoPosition poiPosition, final int zoomLevel, final String poiText) {

		_isPoiVisible = true;

		_poiGeoPosition = poiPosition;
		_poiTTText = poiText;

		setZoom(zoomLevel);
		setGeoCenterPosition(poiPosition);

		queueMapRedraw();
	}

	/**
	 * Sets whether the map should recenter itself on mouse clicks (middle mouse clicks?)
	 * 
	 * @param b
	 *            if should recenter
	 */
	public void setRecenterOnClickEnabled(final boolean b) {
		_recenterOnClickEnabled = b;
	}

	/**
	 * Set if the tile borders should be drawn. Mainly used for debugging.
	 * 
	 * @param isShowDebugInfo
	 *            new value of this drawTileBorders
	 */
	public void setShowDebugInfo(final boolean isShowDebugInfo) {
		_isShowTileInfo = isShowDebugInfo;
		queueMapRedraw();
	}

	/**
	 * Legend will be drawn into the map when the visibility is <code>true</code>
	 * 
	 * @param visibility
	 */
	public void setShowLegend(final boolean visibility) {
		_isLegendVisible = visibility;
	}

	/**
	 * set status if overlays are painted, a {@link #queueMapRedraw()} must be called to update the
	 * map
	 * 
	 * @param showOverlays
	 *            set <code>true</code> to see the overlays, <code>false</code> to hide the overlays
	 */
	public void setShowOverlays(final boolean showOverlays) {
		_isDrawOverlays = showOverlays;
	}

	public void setShowPOI(final boolean isShowPOI) {

		_isPoiVisible = isShowPOI;

		queueMapRedraw();
	}

	public void setShowScale(final boolean isScaleVisible) {
		_isScaleVisible = isScaleVisible;
	}

	public void setTourPaintMethodEnhanced(final boolean isEnhanced) {

		_isTourPaintMethodEnhanced = isEnhanced;

		disposeOverlayImageCache();
	}

	/**
	 * Set the zoom level for the map and centers the map to the previous center. The zoom level is
	 * checked if the map provider supports the requested zoom level.
	 * 
	 * @param newZoomLevel
	 *            the new zoom level, the zoom level is adjusted to the min/max zoom levels
	 */
	public void setZoom(int newZoomLevel) {

		if (_MP == null) {
			return;
		}

		// check if the requested zoom level is within the bounds of the map provider
		final int mpMinimumZoomLevel = _MP.getMinimumZoomLevel();
		final int mpMaximumZoomLevel = _MP.getMaximumZoomLevel();
		if (((newZoomLevel < mpMinimumZoomLevel) || (newZoomLevel > mpMaximumZoomLevel))) {
			// adjust zoom level
			newZoomLevel = Math.max(newZoomLevel, mpMinimumZoomLevel);
			newZoomLevel = Math.min(newZoomLevel, mpMaximumZoomLevel);
		}

		final int oldzoom = _mapZoomLevel;
		final Point2D oldCenter = _mapCenterInWorldPixel;
		final Dimension oldMapTileSize = _MP.getMapTileSize(oldzoom);

		_mapZoomLevel = newZoomLevel;

		final Dimension mapTileSize = _MP.getMapTileSize(newZoomLevel);

		final Point2D.Double pixelCenter = new Point2D.Double(//
				oldCenter.getX() * (mapTileSize.getWidth() / oldMapTileSize.getWidth()),
				oldCenter.getY() * (mapTileSize.getHeight() / oldMapTileSize.getHeight()));

		setMapCenterInWoldPixel(pixelCenter);

		updateViewPortData();

		showHidePoiTooltip();

		fireZoomEvent(newZoomLevel);
	}

	/**
	 * A property indicating if the map should be zoomable by the user using the mouse wheel.
	 * 
	 * @param zoomEnabled
	 *            the new value of the property
	 */
	public void setZoomEnabled(final boolean zoomEnabled) {
		this._zoomEnabled = zoomEnabled;
	}

	public void setZoomOnDoubleClickEnabled(final boolean zoomOnDoubleClickEnabled) {
		this._zoomOnDoubleClickEnabled = zoomOnDoubleClickEnabled;
	}

	private void showHidePoiTooltip() {
		/*
		 * show poi info when mouse is within the poi image
		 */
		if (_isPoiPositionInViewport = updatePoiImageDevPosition()) {

			final Display display = Display.getCurrent();
			final Point displayMouse = display.getCursorLocation();
			final Point devMouse = this.toControl(displayMouse);

			final int devMouseX = devMouse.x;
			final int devMouseY = devMouse.y;

			if ((devMouseX > _poiImageDevPosition.x)
					&& (devMouseX < _poiImageDevPosition.x + _poiImageBounds.width)
					&& (devMouseY > _poiImageDevPosition.y - _poiTTOffsetY - 5)
					&& (devMouseY < _poiImageDevPosition.y + _poiImageBounds.height)) {

				showPoiTooltip();
			} else {
				hidePoiToolTip();
			}
		} else {
			hidePoiToolTip();
		}
	}

	private void showPoiInfo() {

		if ((_poiTT != null) && _poiTT.isVisible()) {
			return;
		}

		if (_poiTT == null) {

			// create poi info

			_poiTT = new ToolTip(getShell()) {
				@Override
				protected Composite setContent(final Shell shell) {
					return createToolTip(shell);
				}
			};
		}

		_poiTTLabel.setText(_poiTTText == null ? UI.EMPTY_STRING : _poiTTText);

		final Point poiDisplayPosition = this.toDisplay(_poiImageDevPosition);

		_poiTT.show(//
				poiDisplayPosition.x,
				poiDisplayPosition.y,
				_poiImageBounds.width,
				_poiImageBounds.height,
				_poiTTOffsetY);
	}

	private void showPoiTooltip() {

		if (_poiTT == null) {
			return;
		}

		if (_isPoiPositionInViewport = updatePoiImageDevPosition()) {

			final Point poiDisplayPosition = this.toDisplay(_poiImageDevPosition);

			_poiTT.show(
					poiDisplayPosition.x,
					poiDisplayPosition.y,
					_poiImageBounds.width,
					_poiImageBounds.height,
					_poiTTOffsetY);
		}

	}

	private void updateMouseMapPosition() {

		// check position, can be initially be null
		if ((_mouseMovePosition == null) || (_MP == null)) {
			return;
		}

		/*
		 * !!! DON'T OPTIMIZE THE NEXT LINE, OTHERWISE THE WRONG MOUSE POSITION IS FIRED !!!
		 */
		final Rectangle topLeftViewPort = getMapPixelViewport();

		final int worldMouseX = topLeftViewPort.x + _mouseMovePosition.x;
		final int worldMouseY = topLeftViewPort.y + _mouseMovePosition.y;

		final GeoPosition geoPosition = _MP.pixelToGeo(new Point2D.Double(worldMouseX, worldMouseY), _mapZoomLevel);

		fireMousePositionEvent(geoPosition);
	}

	private void updateOfflineAreaEndPosition(final MouseEvent mouseEvent) {

		final Rectangle viewPort = _worldPixelViewport;
		final int worldMouseX = viewPort.x + mouseEvent.x;
		final int worldMouseY = viewPort.y + mouseEvent.y;

		_offlineDevAreaEnd = new Point(mouseEvent.x, mouseEvent.y);
		_offlineWorldEnd = new Point(worldMouseX, worldMouseY);

		_offlineDevTileEnd = getOfflineAreaTilePosition(worldMouseX, worldMouseY);
	}

	/**
	 * @return Returns <code>true</code> when the POI image is visible and the position is set in
	 *         {@link #_poiImageDevPosition}
	 */
	private boolean updatePoiImageDevPosition() {

		if (_poiGeoPosition == null) {
			return false;
		}

		// get world position for the poi coordinates
		final java.awt.Point worldPoiPos = _MP.geoToPixel(_poiGeoPosition, _mapZoomLevel);

		// adjust view port to contain the poi image
		final java.awt.Rectangle adjustedViewport = (Rectangle) _worldPixelViewport.clone();
		adjustedViewport.x -= _poiImageBounds.width;
		adjustedViewport.y -= _poiImageBounds.height;
		adjustedViewport.width += _poiImageBounds.width * 2;
		adjustedViewport.height += _poiImageBounds.height * 2;

		// check if poi is visible
		if (adjustedViewport.contains(
				worldPoiPos.x - _poiImageBounds.width / 2,
				worldPoiPos.y - _poiImageBounds.height,
				_poiImageBounds.width,
				_poiImageBounds.height)) {

			// convert world position into device position
			final int devPoiPosX = worldPoiPos.x - _worldPixelViewport.x;
			final int devPoiPosY = worldPoiPos.y - _worldPixelViewport.y;

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
	 * Sets all viewport data which are necessary to draw the map tiles in {@link #drawMapTiles(GC)}
	 * 
	 * @return
	 */
	private void updateViewPortData() {

		if (_MP == null) {
			// the map has currently no map provider
			return;
		}

		// optimize performance by keeping the viewport
		final Rectangle viewport = _worldPixelViewport = getMapPixelViewport();

		_worldViewportX = viewport.x;
		_worldViewportY = viewport.y;

		final int visiblePixelWidth = viewport.width;
		final int visiblePixelHeight = viewport.height;

		_devVisibleViewport = new Rectangle(0, 0, visiblePixelWidth, visiblePixelHeight);

		final int tileSize = _MP.getTileSize();
		_mapTileSize = _MP.getMapTileSize(_mapZoomLevel);

		// get the visible tiles which can be displayed in the viewport area
		final int numTileWidth = (int) Math.ceil((double) visiblePixelWidth / (double) tileSize);
		final int numTileHeight = (int) Math.ceil((double) visiblePixelHeight / (double) tileSize);

		/*
		 * tileOffsetX and tileOffsetY are the x- and y-values for the offset of the visible screen
		 * to the Map's origin.
		 */
		final int tileOffsetX = (int) Math.floor((double) _worldViewportX / (double) tileSize);
		final int tileOffsetY = (int) Math.floor((double) _worldViewportY / (double) tileSize);

		_tilePosMinX = tileOffsetX;
		_tilePosMinY = tileOffsetY;
		_tilePosMaxX = tileOffsetX + numTileWidth;
		_tilePosMaxY = tileOffsetY + numTileHeight;

		_MP.setMapViewPort(new MapViewPortData(//
				_mapZoomLevel,
				_tilePosMinX,
				_tilePosMaxX,
				_tilePosMinY,
				_tilePosMaxY));
	}

	public void zoomIn() {
		setZoom(_mapZoomLevel + 1);
		queueMapRedraw();
	}

	public void zoomOut() {
		setZoom(_mapZoomLevel - 1);
		queueMapRedraw();
	}

}
