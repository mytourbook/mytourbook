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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
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
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import de.byteholder.geoclipse.map.event.IMapListener;
import de.byteholder.geoclipse.map.event.IZoomListener;
import de.byteholder.geoclipse.map.event.MapEvent;
import de.byteholder.geoclipse.map.event.ZoomEvent;
import de.byteholder.geoclipse.ui.TextWrapPainter;
import de.byteholder.gpx.GeoPosition;

public class Map extends Canvas {

	/**
	 * The zoom level. Normally a value between around 0 and 20.
	 */
	protected int								fMapZoomLevel				= 1;

	/**
	 * Image which contains the map
	 */
	private Image								fMapImage;

	/**
	 * The position, in <I>map coordinates</I> of the center point. This is defined as the distance
	 * from the top and left edges of the map in pixels. Dragging the map component will change the
	 * center position. Zooming in/out will cause the center to be recalculated so as to remain in
	 * the center of the new "map".
	 */
	protected Point2D							fMapPixelCenter				= new Point2D.Double(0, 0);

	/**
	 * Indicates whether or not to draw the borders between tiles. Defaults to false.
	 * not very nice looking, very much a product of testing Consider whether this should really be
	 * a property or not.
	 */
	private boolean								fIsShowTileInfo				= false;

	/**
	 * Factory used by this component to grab the tiles necessary for painting the map.
	 */
	private TileFactory							fTileFactory;

	/**
	 * The position in latitude/longitude of the "address" being mapped. This is a special
	 * coordinate that, when moved, will cause the map to be moved as well. It is separate from
	 * "center" in that "center" tracks the current center (in pixels) of the view port whereas this
	 * will not change when panning or zooming. Whenever the addressLocation is changed, however,
	 * the map will be repositioned.
	 */
	private GeoPosition							addressLocation;

	/**
	 * Specifies whether panning is enabled. Panning is being able to click and drag the map around
	 * to cause it to move
	 */
	private boolean								panEnabled					= true;

	/**
	 * Specifies whether zooming is enabled (the mouse wheel, for example, zooms)
	 */
	private boolean								zoomEnabled					= true;

	/**
	 * Indicates whether the component should re-center the map when the "middle" mouse button is
	 * pressed
	 */
	private boolean								recenterOnClickEnabled		= true;

	private boolean								zoomOnDoubleClickEnabled	= true;

	private boolean								restrictOutsidePanning		= true;

	/**
	 * The overlay to delegate to for painting the "foreground" of the map component. This would
	 * include painting waypoints, day/night, etc. Also receives mouse events.
	 */
	private final List<MapPainter>				overlays					= new ArrayList<MapPainter>();

	private final TileLoadObserver				fTileLoadObserver			= new TileLoadObserver();

	private final Cursor						cursorPan;
	private final Cursor						cursorDefault;

	private int									fRedrawMapCounter			= 0;
	private int									overlayRunnableCounter		= 0;

	private boolean								isLeftMouseButtonPressed	= false;

	private Point								fMouseMovePosition;
	private Point								fMousePanPosition;
	private boolean								isMapPanned;

	private final Thread						overlayThread;
	private long								nextOverlayRedrawTime;

	private NumberFormat						fNf							= NumberFormat.getNumberInstance();
	private NumberFormat						fNfLatLon					= NumberFormat.getNumberInstance();
	{
		fNfLatLon.setMaximumFractionDigits(4);
	}

	private TextWrapPainter						fTextWrapper				= new TextWrapPainter();

	/**
	 * cache for overlay images
	 */
	private final OverlayImageCache				fOverlayImageCache;

	/**
	 * cache for overlay image which are part of another tile
	 */
//	private final OverlayImageCache				fPartOverlayImageCache;

	/**
	 * This queue contains tiles which overlay image must be painted
	 */
	private final ConcurrentLinkedQueue<Tile>	fTileOverlayPaintQueue		= new ConcurrentLinkedQueue<Tile>();

	private boolean								isDrawOverlayRunning		= false;

	private String								fOverlayKey;

	/**
	 * this painter is called when the map is painted in the onPaint event
	 */
	private IDirectPainter						directMapPainter;

	private final DirectPainterContext			directMapPainterContext		= new DirectPainterContext();

	/**
	 * when <code>true</code> the overlays are painted
	 */
	private boolean								fIsDrawOverlays				= false;

	/**
	 * contains a legend which is painted in the map
	 */
	private MapLegend							mapLegend;

	private boolean								isLegendVisible;

	/**
	 * viewport of the map when the {@link #mapImage} is painted
	 */
	private Rectangle							fMapViewport;

	private Point								fViewPortSize;
	private int									fViewPortBorder;
	private Rectangle							fMapImageSize;

	private List<IZoomListener>					zoomListeners;
	private final ListenerList					fMapListeners				= new ListenerList(ListenerList.IDENTITY);

	// measurement system
	private float								fDistanceUnitValue			= 1;
	private String								fDistanceUnitLabel			= UI.EMPTY_STRING;

	private boolean								fIsScaleVisible				= false;

	private static final RGB					fTransparentRGB				= new RGB(0xff, 0xff, 0xfe);

	// [181,208,208] is the color of water in the standard OSM material
	public final static RGB						DefaultBackgroundRGB		= new RGB(181, 208, 208);
	private Color								defaultBackgroundColor;

	/**
	 * when <code>true</code> the loading... image is not displayed
	 */
	private boolean								fIsLiveView;

	private long								fRequestedRedrawTime;
	private long								fDrawTime;

	// used to pan using the arrow keys
	private class PanKeyListener extends KeyAdapter {

		private static final int	OFFSET	= 10;

		@Override
		public void keyPressed(final KeyEvent e) {

			int delta_x = 0;
			int delta_y = 0;

			switch (e.keyCode) {
			case SWT.ARROW_LEFT:
				delta_x = -OFFSET;
				break;
			case SWT.ARROW_RIGHT:
				delta_x = OFFSET;
				break;
			case SWT.ARROW_UP:
				delta_y = -OFFSET;
				break;
			case SWT.ARROW_DOWN:
				delta_y = OFFSET;
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

			if (delta_x != 0 || delta_y != 0) {
				final Rectangle bounds = getViewport();
				final double x = bounds.getCenterX() + delta_x;
				final double y = bounds.getCenterY() + delta_y;
				final Point2D.Double pixelCenter = new Point2D.Double(x, y);
				setMapPixelCenter(fTileFactory.pixelToGeo(pixelCenter, fMapZoomLevel), pixelCenter);
				queueMapRedraw();
			}
		}
	}

	// used to pan using press and drag mouse gestures
	private class PanMouseListener implements MouseListener, MouseMoveListener, Listener {

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

		public void mouseDown(final MouseEvent e) {

			if (e.button == 1) {
				// if the left mb is clicked remember this point (for panning)
				fMousePanPosition = new Point(e.x, e.y);
				isLeftMouseButtonPressed = true;

				if (isPanEnabled()) {
					setCursor(cursorPan);
				}
			}
		}

		public void mouseMove(final MouseEvent e) {

			onMouseMove(e);
		}

		public void mouseUp(final MouseEvent e) {

			if (e.button == 1) {
				if (isMapPanned) {
					isMapPanned = false;
					redraw();
				}
				fMousePanPosition = null;
				isLeftMouseButtonPressed = false;
				setCursor(cursorDefault);

			} else if (e.button == 2) {
				// if the middle mouse button is clicked, recenter the view
				if (isRecenterOnClickEnabled()) {
					recenterMap(e.x, e.y);
				}
			}
		}

		private void recenterMap(final int ex, final int ey) {
			final Rectangle bounds = getViewport();
			final double x = bounds.getX() + ex;
			final double y = bounds.getY() + ey;
			final Point2D.Double pixelCenter = new Point2D.Double(x, y);
			setMapPixelCenter(fTileFactory.pixelToGeo(pixelCenter, fMapZoomLevel), pixelCenter);
			queueMapRedraw();
		}
	}

	/**
	 * This observer is called in the {@link Tile} when a tile image is set into the tile
	 */
	private final class TileLoadObserver implements Observer {

		public void update(final Observable observable, final Object arg) {

			if (observable instanceof Tile) {

				final Tile tile = (Tile) observable;

				if (tile.getZoom() == getZoom()) {

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
	 * Checks if an image can be reused, this is true if the image exists and has the same size
	 * 
	 * @param newWidth
	 * @param newHeight
	 * @return
	 */
	public static boolean canReuseImage(final Image image, final Rectangle rect) {

		// check if we could reuse the existing image

		if (image == null || image.isDisposed()) {
			return false;
		} else {
			// image exist, check for the bounds
			final org.eclipse.swt.graphics.Rectangle oldBounds = image.getBounds();

			if (!(oldBounds.width == rect.width && oldBounds.height == rect.height)) {
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
	 * @param rect
	 * @return returns a new created image
	 */
	public static Image createImage(final Display display, final Image image, final Rectangle rect) {

		if (image != null && !image.isDisposed()) {
			image.dispose();
		}

		final int width = Math.max(1, rect.width);
		final int height = Math.max(1, rect.height);

		return new Image(display, width, height);
	}

	/**
	 * Create a new Map
	 */
	public Map(final Composite parent, final int style) {

		super(parent, style | SWT.DOUBLE_BUFFERED);

		zoomListeners = new ArrayList<IZoomListener>();

		addPaintListener(new PaintListener() {
			public void paintControl(final PaintEvent e) {
				onPaint(e);
			}
		});

		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				onDispose(e);
			}
		});

		final PanMouseListener mouseListener = new PanMouseListener();
		addMouseListener(mouseListener);
		addMouseMoveListener(mouseListener);
		addListener(SWT.MouseWheel, mouseListener);

		addKeyListener(new PanKeyListener());

		addControlListener(new ControlListener() {

			public void controlMoved(final ControlEvent e) {
			// just do nothing, it's not necessary
			}

			public void controlResized(final ControlEvent e) {
				onResize();
			}
		});

		/*
		 * enable travers keys
		 */
		addTraverseListener(new TraverseListener() {
			public void keyTraversed(final TraverseEvent e) {
				e.doit = true;
			}
		});

		final Display display = getDisplay();

		cursorPan = new Cursor(display, SWT.CURSOR_SIZEALL);
		cursorDefault = new Cursor(display, SWT.CURSOR_ARROW);

		defaultBackgroundColor = new Color(display, DefaultBackgroundRGB);

		fOverlayImageCache = new OverlayImageCache();
//		fPartOverlayImageCache = new OverlayImageCache();

		overlayThread = new Thread("PaintOverlayImages") { //$NON-NLS-1$
			@Override
			public void run() {

				while (!isInterrupted()) {

					try {

						Thread.sleep(20);

						if (isDrawOverlayRunning == false) {

							// overlay drawing is not running

							final long currentTime = System.currentTimeMillis();

							if (currentTime > nextOverlayRedrawTime + 50) {
								if (fTileOverlayPaintQueue.size() > 0) {

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

		overlayThread.setDaemon(true);
		overlayThread.start();

	}

	public void addMapListener(final IMapListener mapListener) {
		fMapListeners.add(mapListener);
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
		overlays.add(overlay);
		queueMapRedraw();
	}

//	public void addTileListener(final ITileListener tileListener) {
//		fTileListeners.add(tileListener);
//	}

	public void addZoomListener(final IZoomListener listener) {
		zoomListeners.add(listener);
	}

	/**
	 * Calculates (and sets) the greatest zoom level, so that all positions are visible on screen.
	 * This is useful if you have a bunch of points in an area like a city and you want to zoom out
	 * so that the entire city and it's points are visible without panning.
	 * 
	 * @param positions
	 *            A set of GeoPositions to calculate the new zoom from
	 */
	public void calculateZoomFrom(final Set<GeoPosition> positions) {
		if (positions.size() < 2) {
			return;
		}

		final TileFactoryInfo factoryInfo = fTileFactory.getInfo();
		int zoom = factoryInfo.getMinimumZoomLevel();
		Rectangle rect = getBoundingRect(positions, zoom);

		while (getViewport().contains(rect) && zoom < factoryInfo.getMaximumZoomLevel()) {
			zoom++;
			rect = getBoundingRect(positions, zoom);
		}
		final Point2D center = new Point2D.Double(rect.getCenterX(), rect.getCenterY());

		setMapPixelCenter(fTileFactory.pixelToGeo(center, zoom), center);
		setZoom(zoom);

		queueMapRedraw();
	}

	@Override
	public org.eclipse.swt.graphics.Point computeSize(final int wHint, final int hHint, final boolean changed) {
		return getParent().getSize();
	}

	public synchronized void dimMap(final int dimLevel, final RGB dimColor) {

		fTileFactory.setDimLevel(dimLevel, dimColor);

		// remove all cached map images
		fTileFactory.dispose();

		reload();
	}

	public void disposeCachedImages() {
		fTileFactory.disposeCachedImages();
	}

	/**
	 * Clear the overlay image cache
	 */
	public synchronized void disposeOverlayImageCache() {
		fOverlayImageCache.dispose();
//		fPartOverlayImageCache.dispose();
		fTileOverlayPaintQueue.clear();
	}

	private void disposeResource(final Resource resource) {
		if (resource != null && !resource.isDisposed()) {
			resource.dispose();
		}
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
			Image image = fMapImage;
			if (image == null || image.isDisposed() || canReuseImage(image, fMapImageSize) == false) {
				image = createImage(getDisplay(), image, fMapImageSize);
			}
			fMapImage = image;

			gc = new GC(fMapImage);
			{
				drawMapTiles(gc);
				drawMapLegend(gc);

				if (fIsScaleVisible) {
					drawMapScale(gc);
				}
			}

		} catch (final Exception e) {

			e.printStackTrace();

			// map image is corrupt
			fMapImage.dispose();

		} finally {
			if (gc != null) {
				gc.dispose();
			}
		}

		fDrawTime = System.currentTimeMillis();

		redraw();
	}

	private void drawMapLegend(final GC gc) {

		if (isLegendVisible == false) {
			return;
		}

		if (mapLegend == null) {
			return;
		}

		// get legend image from the legend
		final Image legendImage = mapLegend.getImage();
		if (legendImage == null || legendImage.isDisposed()) {
			return;
		}

		final org.eclipse.swt.graphics.Rectangle imageBounds = legendImage.getBounds();

		// draw legend on bottom left
		int yPos = fMapViewport.height - 5 - imageBounds.height;
		yPos = Math.max(5, yPos);

		final Point legendPosition = new Point(5, yPos);
		mapLegend.setLegendPosition(legendPosition);

		gc.drawImage(legendImage, legendPosition.x, legendPosition.y);
	}

	private void drawMapScale(final GC gc) {

		final int viewPortWidth = fMapViewport.width;

		final int devScaleWidth = viewPortWidth / 3;
		final float metricWidth = 111.32f / fDistanceUnitValue;

		final GeoPosition mapCenter = getCenterPosition();
		final double latitude = mapCenter.getLatitude();
		final double longitude = mapCenter.getLongitude();

		final double devDistance = fTileFactory.getDistance(
				new GeoPosition(latitude - 0.5, longitude),
				new GeoPosition(latitude + 0.5, longitude),
				fMapZoomLevel);

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
			fNf.setMinimumFractionDigits(1);
			fNf.setMaximumFractionDigits(1);
			scaleUI = fNf.format(scaleLat);
		} else if (scaleLat >= 1f) {
			fNf.setMinimumFractionDigits(2);
			fNf.setMaximumFractionDigits(2);
			scaleUI = fNf.format(scaleLat);
		} else {
			fNf.setMinimumFractionDigits(3);
			fNf.setMaximumFractionDigits(3);
			scaleUI = fNf.format(scaleLat);
		}
		final String scaleText = scaleUI + UI.SPACE + fDistanceUnitLabel;
		final Point textExtent = gc.textExtent(scaleText);

		final int devX1 = viewPortWidth - 5 - devScaleWidth;
		int devY = fMapViewport.height - 5 - 3;
//		final int x1 = viewPortWidth / 2 - devScaleWidth / 2;
//		int devY = fMapViewport.height / 2;

		final int devX2 = devX1 + devScaleWidth;
		final int segmentWidth = devScaleWidth / 4;

		final int devYScaleLines = devY;

		final Display display = getDisplay();
		final Color white = display.getSystemColor(SWT.COLOR_WHITE);
		final Color black = display.getSystemColor(SWT.COLOR_BLACK);
		final Color gray = display.getSystemColor(SWT.COLOR_DARK_GRAY);

		drawMapScaleLine(gc, devX1, devX2, devY++, segmentWidth, gray, gray);
		drawMapScaleLine(gc, devX1, devX2, devY++, segmentWidth, white, black);
		drawMapScaleLine(gc, devX1, devX2, devY++, segmentWidth, white, black);
		drawMapScaleLine(gc, devX1, devX2, devY++, segmentWidth, white, black);
		drawMapScaleLine(gc, devX1, devX2, devY, segmentWidth, gray, gray);

		final int devYText = devYScaleLines - textExtent.y;
		final int devXText = devX1 + devScaleWidth - textExtent.x;

		final Color borderColor = new Color(display, 0xF1, 0xEE, 0xE8);
		{
			gc.setForeground(borderColor);
			gc.drawText(scaleText, devXText - 1, devYText, true);
			gc.drawText(scaleText, devXText + 1, devYText, true);
			gc.drawText(scaleText, devXText, devYText - 1, true);
			gc.drawText(scaleText, devXText, devYText + 1, true);
		}
		borderColor.dispose();

		gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
		gc.drawText(scaleText, devXText, devYText, true);
	}

	private void drawMapScaleLine(	final GC gc,
									final int devX1,
									final int devX2,
									final int devY,
									final int segmentWidth,
									final Color firstColor,
									final Color secondColor) {

		final Display display = getDisplay();

		gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
		gc.drawPoint(devX1, devY);

		gc.setForeground(firstColor);
		gc.drawLine(devX1 + 1, devY, (devX1 + segmentWidth), devY);

		gc.setForeground(secondColor);
		gc.drawLine(devX1 + segmentWidth, devY, devX1 + 2 * segmentWidth, devY);

		gc.setForeground(firstColor);
		gc.drawLine(devX1 + 2 * segmentWidth, devY, devX1 + 3 * segmentWidth, devY);

		gc.setForeground(secondColor);
		gc.drawLine(devX1 + 3 * segmentWidth, devY, devX2, devY);

		gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
		gc.drawPoint(devX2, devY);
	}

	/**
	 * Draw all visible tiles into the map viewport
	 * 
	 * @param gc
	 */
	private void drawMapTiles(final GC gc) {

		// optimize performance by keeping the viewport
		final Rectangle viewport = fMapViewport = getViewport();

		final int worldViewportX = viewport.x;
		final int worldViewportY = viewport.y;
		final int devVisibleWidth = viewport.width;
		final int devVisibleHeight = viewport.height;

		final Rectangle devVisibleViewport = new Rectangle(0, 0, devVisibleWidth, devVisibleHeight);

		final int tileSize = fTileFactory.getTileSize();
		final Dimension tileMapSize = fTileFactory.getMapSize(fMapZoomLevel);

		// get the visible tiles which can be displayed in the viewport area
		final int numTileWidth = (int) Math.ceil((double) devVisibleWidth / (double) tileSize);
		final int numTileHeight = (int) Math.ceil((double) devVisibleHeight / (double) tileSize);

		/*
		 * tpx and tpy are the x- and y-values for the offset of the visible screen to the Map's
		 * origin.
		 */
		final int vox = (int) Math.floor((double) worldViewportX / (double) tileSize);
		final int voy = (int) Math.floor((double) worldViewportY / (double) tileSize);
		final Point tileOffset = new Point(vox, voy);

		final int tileOffsetX = tileOffset.x;
		final int tileOffsetY = tileOffset.y;

		final Display display = getDisplay();

		/*
		 * draw all visible tiles
		 */
		for (int relativeX = 0; relativeX <= numTileWidth; relativeX++) {
			for (int relativeY = 0; relativeY <= numTileHeight; relativeY++) {

				// get tile position of the tile  
				final int tilePositionX = tileOffsetX + relativeX;
				final int tilePositionY = tileOffsetY + relativeY;

				// get device rectangle for this tile
				final Rectangle devTilePosition = new Rectangle(
						tilePositionX * tileSize - worldViewportX,
						tilePositionY * tileSize - worldViewportY,
						tileSize,
						tileSize);

				// check if current tile is within the painting area
				if (devTilePosition.intersects(devVisibleViewport)) {

					/*
					 * get the tile from the factory. the tile must not have been completely
					 * downloaded after this step.
					 */

					if (isTileOnMap(tilePositionX, tilePositionY, tileMapSize)) {

						drawTile(gc, tilePositionX, tilePositionY, devTilePosition);

					} else {

						/*
						 * if tile is off the map to the north or south, draw map background
						 */

						gc.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
						gc.fillRectangle(devTilePosition.x, devTilePosition.y, tileSize, tileSize);
					}
				}
			}
		}
	}

	private void drawTile(	final GC gc,
							final int tilePositionX,
							final int tilePositionY,
							final Rectangle devTileRectangle) {

		// get tile from the tile factory, this also starts the loading of the tile image
		final Tile tile = fTileFactory.getTile(tilePositionX, tilePositionY, fMapZoomLevel);

		drawTileImage(gc, tile, devTileRectangle);

		if (fIsDrawOverlays) {
			drawTileOverlay(gc, tile, devTileRectangle);
		}

		if (fIsShowTileInfo) {
			drawTileInfo(gc, tile, devTileRectangle);
		}
	}

	/**
	 * draw the tile map image
	 */
	private void drawTileImage(final GC gc, final Tile tile, final Rectangle devTilePosition) {

		final Image tileImage = tile.getCheckedMapImage();

		if (tileImage != null) {

			// map image was loaded

			gc.drawImage(tileImage, devTilePosition.x, devTilePosition.y);

			return;
		}

		if (tile.isLoadingError()) {

			// map image contains an error, it could not be loaded

			/*
			 * image was disposed,
			 */
			final Image errorImage = fTileFactory.getErrorImage();
			final org.eclipse.swt.graphics.Rectangle imageBounds = errorImage.getBounds();

			gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_GRAY));
			gc.fillRectangle(devTilePosition.x, devTilePosition.y, imageBounds.width, imageBounds.height);

			drawTileInfoError(gc, devTilePosition, tile); //$NON-NLS-1$

			return;
		}

		if (tile.isOfflineError()) {

			//map image could not be loaded from offline file

			gc.drawImage(fTileFactory.getErrorImage(), devTilePosition.x, devTilePosition.y);

			drawTileInfoError(gc, devTilePosition, tile); //$NON-NLS-1$

			return;
		}

		/*
		 * the tile image is not yet loaded, register an observer that handles redrawing when the
		 * tile image is available. Tile image loading is started, when the tile is retrieved from
		 * the tile factory which is done in drawTile()
		 */
		tile.addObserver(fTileLoadObserver);

		if (fIsLiveView == false) {

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

				gc.drawImage(fTileFactory.getLoadingImage(), devTilePosition.x, devTilePosition.y);
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
				|| (childrenWithErrors != null && childrenWithErrors.size() > 0)) {

			drawTileInfoError(gc, devTilePosition, tile);

			return;
		}

		final Display display = getDisplay();
		final int tileSize = fTileFactory.getTileSize();

		// draw tile border
		gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
		gc.drawRectangle(devTilePosition.x, devTilePosition.y, tileSize, tileSize);

		// draw tile info
		gc.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
		gc.setBackground(display.getSystemColor(SWT.COLOR_DARK_BLUE));

		final int leftMargin = 10;

		drawTileInfoLatLon(gc, tile, devTilePosition, 10, leftMargin);
		drawTileInfoPosition(gc, devTilePosition, tile, 50, leftMargin);

		// draw tile image path/url
		final StringBuilder sb = new StringBuilder();

		drawTileInfoPath(tile, sb);

		fTextWrapper.printText(
				gc,
				sb.toString(),
				devTilePosition.x + leftMargin,
				devTilePosition.y + 80,
				devTilePosition.width - 20);

	}

	private void drawTileInfoError(final GC gc, final Rectangle devTilePosition, final Tile tile) {

		final Display display = getDisplay();

		final int tileSize = fTileFactory.getTileSize();

		// draw tile border
		gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
		gc.drawRectangle(devTilePosition.x, devTilePosition.y, tileSize, tileSize);

		gc.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
		gc.setBackground(display.getSystemColor(SWT.COLOR_DARK_MAGENTA));

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
		if (childrenLoadingError != null && childrenLoadingError.size() > 0) {

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

		fTextWrapper.printText(
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
		sb.append("Lat: "); //$NON-NLS-1$
		sb.append(fNfLatLon.format(bbox.bottom));
		gc.drawString(sb.toString(), //
				devTilePosition.x + leftMargin,
				devTilePosition.y + topMargin);

		// lat - top
		gc.drawString(fNfLatLon.format(bbox.top), //
				devTilePosition.x + leftMargin + dev2ndColumn,
				devTilePosition.y + topMargin);

		sb.setLength(0);

		// lon - left
		sb.append("Lon: "); //$NON-NLS-1$
		sb.append(fNfLatLon.format(bbox.left));
		gc.drawString(sb.toString(), //
				devTilePosition.x + leftMargin,
				devTilePosition.y + topMargin + devLineHeight);

		// lon - right
		gc.drawString(fNfLatLon.format(bbox.right), //
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
				.append("x:") //$NON-NLS-1$
				.append(tile.getX())
				.append(" y:") //$NON-NLS-1$
				.append(tile.getY())
				.append(" zoom:") //$NON-NLS-1$
				.append(tile.getZoom() + 1);

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

		if (tile.getX() == 34328 && tile.getY() == 22950) {
			int a = 0;
			a++;
		}
		if (imageState == OverlayImageState.NO_IMAGE) {
			// there is no image for the tile overlay
			return;
		}

		Image overlayImage = null;

		if (imageState == OverlayImageState.HAS_CONTENT) {

			// tile can have an overlay image

			// get overlay image from the tile
			overlayImage = tile.getOverlayImage();

			if (overlayImage == null) {

				// get the overlay image from the cache
				overlayImage = fOverlayImageCache.get(getOverlayKey(tile));

				if (overlayImage != null) {

					// overlay image is available in the cache, keep the image in the tile
					tile.setOverlayImage(overlayImage);
				}
			}
		}

		// draw overlay image
		if (overlayImage != null) {
			gc.drawImage(overlayImage, devTileRectangle.x, devTileRectangle.y);
		}

		/*
		 * Priority 2: check state for the overlay
		 */
		final int overlayContent = tile.getOverlayContent();
		final OverlayTourStatus tourState = tile.getOverlayTourStatus();

		if (tourState == OverlayTourStatus.IS_CHECKED) {

			// it is possible that the image is disposed but the tile has overlay content

			if (overlayImage == null) {

				if (overlayContent != 0) {

					queueOverlayPainting(tile);
					return;
				}

				// tile has no overlay content -> image
				tile.setOverlayImageState(OverlayImageState.NO_IMAGE);
			}

			// tour is check, all is OK
			return;
		}

		// when tile is queued, nothing more to do, just wait
		if (tourState == OverlayTourStatus.IS_QUEUED) {
			return;
		}

		// overlay tour status is not yet checked, overlayTourStatus == OverlayTourStatus.NOT_CHECKED
		queueOverlayPainting(tile);

	}

	private void fireMapEvent(final GeoPosition geoPosition) {

		final MapEvent event = new MapEvent(geoPosition, fMapZoomLevel);

		final Object[] listeners = fMapListeners.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			((IMapListener) listeners[i]).mapInfo(event);
		}
	}

	private void fireZoomEvent(final int zoom) {

		final ZoomEvent event = new ZoomEvent(zoom);
		for (final IZoomListener l : zoomListeners) {
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
		return addressLocation;
	}

	public Rectangle getBoundingRect(final Set<GeoPosition> positions, final int zoom) {
		final java.awt.Point point1 = fTileFactory.geoToPixel(positions.iterator().next(), zoom);
		final Rectangle rect = new Rectangle(point1.x, point1.y, 0, 0);

		for (final GeoPosition pos : positions) {
			final java.awt.Point point = fTileFactory.geoToPixel(pos, zoom);
			rect.add(new Rectangle(point.x, point.y, 0, 0));
		}
		return rect;
	}

	/**
	 * Gets the current pixel center of the map. This point is in the global bitmap coordinate
	 * system, not as lat/longs.
	 * 
	 * @return the current center of the map as a pixel value
	 */
	public Point2D getCenter() {
		return fMapPixelCenter;
	}

	/**
	 * A property indicating the center position of the map, or <code>null</code> when a tile
	 * factory is not set
	 * 
	 * @return the current center position
	 */
	public GeoPosition getCenterPosition() {

		if (fTileFactory == null) {
			return null;
		}

		return fTileFactory.pixelToGeo(getCenter(), fMapZoomLevel);
	}

	/**
	 * @return Returns the legend of the map
	 */
	public MapLegend getLegend() {
		return mapLegend;
	}

	/**
	 * @param tileKey
	 * @return Returns the key to identify overlay images in the image cache
	 */
	private String getOverlayKey(final Tile tile) {
		return fOverlayKey + tile.getTileKey();
	}

	/**
	 * @param tile
	 * @param xOffset
	 * @param yOffset
	 * @return
	 */
	private String getOverlayKey(final Tile tile, final int xOffset, final int yOffset) {
		return fOverlayKey + tile.getTileKey(xOffset, yOffset);
	}

	public List<MapPainter> getOverlays() {
		return overlays;
	}

	public java.awt.Point getRelativePixel(final java.awt.Point absolutePixel) {
		final Rectangle viewport = getViewport();
		return new java.awt.Point(absolutePixel.x - viewport.x, absolutePixel.y - viewport.y);
	}

	/**
	 * Get the current factory
	 * 
	 * @return Returns the current tile factory
	 */
	public TileFactory getTileFactory() {
		return fTileFactory;
	}

	/**
	 * @return Returns the viewport<br>
	 *         <b>x</b> and <b>y</b> contains the position in world pixel, <br>
	 *         <b>width</b> and <b>height</b> contains the visible area in device pixel
	 */
	public Rectangle getViewport() {
		return getViewport(getCenter());
	}

	/**
	 * Returns the bounds of the viewport in pixels. This can be used to transform points into the
	 * world bitmap coordinate space. The viewport is the part of the map, that you can currently
	 * see on the screen.
	 * 
	 * @return the bounds in <em>pixels</em> of the "view" of this map
	 */
	private Rectangle getViewport(final Point2D center) {

		if (fViewPortSize == null) {
			fViewPortSize = getSize();
			fViewPortBorder = getBorderWidth();
		}

		// calculate the "visible" viewport area in pixels
		final int devWidth = fViewPortSize.x - 2 * fViewPortBorder;
		final int devHeight = fViewPortSize.y - 2 * fViewPortBorder;

		final int worldWidth = (int) (center.getX() - devWidth / 2d);
		final int worldHeight = (int) (center.getY() - devHeight / 2d);

		final Rectangle viewPort = new Rectangle(worldWidth, worldHeight, devWidth, devHeight);

		return viewPort;
	}

	/**
	 * Gets the current zoom level, or <code>null</code> when a tile
	 * factory is not set
	 * 
	 * @return Returns the current zoom level of the map
	 */
	public int getZoom() {
		return fMapZoomLevel;
	}

	/**
	 * Indicates if the tile borders should be drawn. Mainly used for debugging.
	 * 
	 * @return the value of this property
	 */
	public boolean isDrawTileBorders() {
		return fIsShowTileInfo;
	}

	/**
	 * A property indicating if the map should be pannable by the user using the mouse.
	 * 
	 * @return property value
	 */
	public boolean isPanEnabled() {
		return panEnabled;
	}

	private boolean isPartImageModified(final ImageData overlayImageData,
										final int srcXStart,
										final int srcYStart,
										final int tileSize) {

		final int transRed = fTransparentRGB.red;
		final int transGreen = fTransparentRGB.green;
		final int transBlue = fTransparentRGB.blue;

		final byte[] srcData = overlayImageData.data;
		final int srcBytesPerLine = overlayImageData.bytesPerLine;

		int srcIndex;
		int srcRed, srcGreen, srcBlue;

		for (int srcY = srcYStart; srcY < srcYStart + tileSize; srcY++) {

			final int srcYBytesPerLine = srcY * srcBytesPerLine;

			for (int srcX = srcXStart; srcX < srcXStart + tileSize; srcX++) {

				srcIndex = srcYBytesPerLine + (srcX * 3);

				srcBlue = srcData[srcIndex] & 0xFF;
				srcGreen = srcData[srcIndex + 1] & 0xFF;
				srcRed = srcData[srcIndex + 2] & 0xFF;

				if (srcRed != transRed || srcGreen != transGreen || srcBlue != transBlue) {
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
		return recenterOnClickEnabled;
	}

	public boolean isRestrictOutsidePanning() {
		return restrictOutsidePanning;
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
		return zoomEnabled;
	}

	public boolean isZoomOnDoubleClickEnabled() {
		return zoomOnDoubleClickEnabled;
	}

	private void onDispose(final DisposeEvent e) {

		if (fTileFactory != null) {
			fTileFactory.resetAll(false);
		}

		disposeResource(fMapImage);

		disposeResource(cursorPan);
		disposeResource(cursorDefault);
		disposeResource(defaultBackgroundColor);

		// dispose resources in the overlay plugins
		for (final MapPainter overlay : getOverlays()) {
			overlay.dispose();
		}

		fOverlayImageCache.dispose();
//		fPartOverlayImageCache.dispose();

		if (directMapPainter != null) {
			directMapPainter.dispose();
		}

		// dispose legend image
		if (mapLegend != null) {
			disposeResource(mapLegend.getImage());
		}

		// stop overlay thread
		overlayThread.interrupt();
	}

	private void onMouseMove(final MouseEvent mouseEvent) {

//		long startTime = System.currentTimeMillis();

		fMouseMovePosition = new Point(mouseEvent.x, mouseEvent.y);

		if ((isLeftMouseButtonPressed && panEnabled) == false) {
			updateMouseMapPosition();
			return;
		}

		/*
		 * pan map
		 */

		/*
		 * set new map center
		 */
		final Point movePosition = new Point(mouseEvent.x, mouseEvent.y);
		final double mapPixelCenterX = fMapPixelCenter.getX();
		final double mapPixelCenterY = fMapPixelCenter.getY();
		final double newCenterX = mapPixelCenterX - (movePosition.x - fMousePanPosition.x);
		double newCenterY = mapPixelCenterY - (movePosition.y - fMousePanPosition.y);

		if (newCenterY < 0) {
			newCenterY = 0;
		}

		final int maxHeight = (int) (fTileFactory.getMapSize(getZoom()).getHeight() * fTileFactory.getTileSize());
		if (newCenterY > maxHeight) {
			newCenterY = maxHeight;
		}

		final Point2D.Double mapCenter = new Point2D.Double(newCenterX, newCenterY);
		setMapPixelCenter(fTileFactory.pixelToGeo(mapCenter, fMapZoomLevel), mapCenter);

		fMousePanPosition = movePosition;

		// force a repaint of the moved map
		isMapPanned = true;
		redraw();

//		long endTime = System.currentTimeMillis();
//		System.out.println("onMouseMove:\t\t" + (endTime - startTime) + " ms");

		queueMapRedraw();
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

		if (fMapImage != null && !fMapImage.isDisposed()) {

			final GC gc = event.gc;
			gc.drawImage(fMapImage, 0, 0);

			if (directMapPainter != null) {
				directMapPainterContext.gc = gc;
				directMapPainterContext.viewport = fMapViewport;
				directMapPainter.paint(directMapPainterContext);
			}
		}
	}

	private void onResize() {

		// get map size, enforce minimum size

		/*
		 * the method getSize() is only correct in a dialog when it's called in the create() method
		 * after super.create();
		 */
		fViewPortSize = getSize();
		fViewPortBorder = getBorderWidth();

		final Rectangle mapViewport = getViewport();

		int mapWidth = mapViewport.width;
		int mapHeight = mapViewport.height;

		mapWidth = Math.max(1, mapWidth);
		mapHeight = Math.max(1, mapHeight);

		fMapImageSize = new Rectangle(mapWidth, mapHeight);

		queueMapRedraw();
	}

	private void paintOverlay10() {

		if (isDisposed()) {
			return;
		}

		overlayRunnableCounter++;

		final Runnable overlayRunnable = new Runnable() {

			final int	runnableCounter	= overlayRunnableCounter;

			public void run() {

				if (isDisposed()) {
					return;
				}

				// check if a newer runnable is available
				if (runnableCounter != overlayRunnableCounter) {
					return;
				}

				isDrawOverlayRunning = true;

				try {

					paintOverlay20Tiles();

				} catch (final Exception e) {
					e.printStackTrace();
				} finally {
					isDrawOverlayRunning = false;
				}

				queueMapRedraw();
			}
		};

		getDisplay().asyncExec(overlayRunnable);
	}

	private void paintOverlay20Tiles() {

		Tile tile;

		while ((tile = fTileOverlayPaintQueue.poll()) != null) {

			// check zoom level
			if (tile.getZoom() == fMapZoomLevel) {

				if (tile.getX() == 17166 && tile.getY() == 11472) {
					int a = 0;
					a++;
				}

				paintOverlay30Tile(tile);

				tile.setOverlayTourStatus(OverlayTourStatus.IS_CHECKED);

			} else {

				// tile has a different zoom level, ignore this tile
				tile.setOverlayTourStatus(OverlayTourStatus.NOT_CHECKED);
			}
		}
	}

	/**
	 * Paints the overlay into the overlay image which is bigger than the tile image so that the
	 * drawings are not clipped at the tile border. The overlay image is afterwards splitted into
	 * parts which are drawn into the tile images
	 * 
	 * @param tile
	 */
	private void paintOverlay30Tile(final Tile tile) {

		final int parts = 3;
		final int tileSize = fTileFactory.getInfo().getTileSize();
		final int partedTileSize = tileSize * parts;

		final ImageData overlayImageData = new ImageData(partedTileSize, partedTileSize, 24, //
				new PaletteData(0xff, 0xff00, 0xff0000));

		overlayImageData.transparentPixel = overlayImageData.palette.getPixel(fTransparentRGB);

		boolean isOverlayPainted = false;
		final Display display = getDisplay();

		final Color transparentColor = new Color(display, fTransparentRGB);
		final Image overlayImage = new Image(display, overlayImageData);
		final GC gc = new GC(overlayImage);
		{
			gc.setBackground(transparentColor);
			gc.fillRectangle(overlayImage.getBounds());

			// paint all overlays for the current tile
			for (final MapPainter overlay : getOverlays()) {

				final boolean isPainted = overlay.paint(gc, Map.this, tile, parts);

				isOverlayPainted = isOverlayPainted || isPainted;
			}

			if (isOverlayPainted) {

				/*
				 * overlay image is created, devide overlay image into part images where the center
				 * image is the requested tile image
				 */

				paintOverlay40SplitParts(tile, overlayImage, display, tileSize, transparentColor);
			}
		}
		gc.dispose();
		transparentColor.dispose();
		overlayImage.dispose();
	}

	/**
	 * Splits the overlay tile image into 3*3 parts
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
	 * @param overlayImage
	 * @param display
	 * @param tileSize
	 * @param transparentColor
	 */
	private void paintOverlay40SplitParts(	final Tile tile,
											final Image overlayImage,
											final Device display,
											final int tileSize,
											final Color transparentColor) {

		final TileCache tileCache = fTileFactory.getTileCache();
		final ImageData overlayImageData = overlayImage.getImageData();

		final int tileZoom = tile.getZoom();
		final int tileX = tile.getX();
		final int tileY = tile.getY();
		final int maxTiles = (int) Math.pow(2, tileZoom);
		Tile partTile;

		for (int yIndex = 0; yIndex < 3; yIndex++) {
			for (int xIndex = 0; xIndex < 3; xIndex++) {

//				// 1, 2, 4, 8, 16...
//				partFlag = partFlag == 0 ? 1 : partFlag * 2;

				// check bounds
				if ((tileX - xIndex < 0 || tileX + xIndex > maxTiles)
						|| (tileY - yIndex < 0 || tileY + yIndex > maxTiles)) {
					continue;
				}

				final int srcX = tileSize * xIndex;
				final int srcY = tileSize * yIndex;

				// check if there are any drawings in the current part
				if (isPartImageModified(overlayImageData, srcX, srcY, tileSize) == false) {

					// there are no drawings within the current part
					continue;
				}

				final String partKey = getOverlayKey(tile, xIndex - 1, yIndex - 1);
				final boolean isCenterPart = xIndex == 1 && yIndex == 1;

				// create transparent part image
				final ImageData partImageData = new ImageData(tileSize, tileSize, 24, //
						new PaletteData(0xff, 0xff00, 0xff0000));

				partImageData.transparentPixel = partImageData.palette.getPixel(fTransparentRGB);

				final Image partImage = new Image(display, partImageData);

				// draw into part image
				final GC gcPartImage = new GC(partImage);
				{
					gcPartImage.setBackground(transparentColor);
					gcPartImage.fillRectangle(partImage.getBounds());

					Image cachedImage;

//					// draw existing part overlay image into the part image
//					cachedImage = fPartOverlayImageCache.get(partKey);
//					if (cachedImage != null && cachedImage.isDisposed() == false) {
//						gcPartImage.drawImage(cachedImage, 0, 0);
//					}

					// draw existing overlay image into the part image
					cachedImage = fOverlayImageCache.get(partKey);
					if (cachedImage != null && cachedImage.isDisposed() == false) {
						gcPartImage.drawImage(cachedImage, 0, 0);
					}

					// draw overlay image part into the part image
					gcPartImage.drawImage(overlayImage, srcX, srcY, tileSize, tileSize, 0, 0, tileSize, tileSize);

					if (isCenterPart) {
						partTile = tile;
					} else {
						partTile = tileCache.get(tile.getTileKey(xIndex - 1, yIndex - 1));
					}

					if (partTile != null) {

						// set image into the part

						partTile.setOverlayImage(partImage);

						// set a flag that the tile has overlay content
						partTile.incrementOverlayContent();
					}

					// keep image
//					if (isCenterPart) {
					fOverlayImageCache.add(partKey, partImage);
//					} else {
//						fPartOverlayImageCache.add(partKey, partImage);
//					}
				}
				gcPartImage.dispose();

				tile.setOverlayImageState(OverlayImageState.HAS_CONTENT);

			}
		}
	}

	/**
	 * Put a map redraw into a queue, the last entry in the queue will be executed
	 */
	public void queueMapRedraw() {

		if (isDisposed() || fTileFactory == null) {
			return;
		}

		fRedrawMapCounter++;

		fRequestedRedrawTime = System.currentTimeMillis();

		if (fRequestedRedrawTime - fDrawTime > 20) {

			// update display even when this is not the last created runnable

			final Runnable synchImageRunnable = new Runnable() {

				final int	fSynchRunnableCounter	= fRedrawMapCounter;

				public void run() {

					if (isDisposed()) {
						return;
					}

					// check if a newer runnable is available
					if (fSynchRunnableCounter != fRedrawMapCounter) {
						// a newer queryRedraw is available
						return;
					}

					drawMap();
				}
			};

			getDisplay().syncExec(synchImageRunnable);

			/*
			 * set an additional asynch runnable because it's possible that the synch runnable do
			 * not draw all tiles
			 */
			getDisplay().asyncExec(synchImageRunnable);

		} else {

			final Runnable asynchImageRunnable = new Runnable() {

				final int	fAsynchRunnableCounter	= fRedrawMapCounter;

				public void run() {

					if (isDisposed()) {
						return;
					}

					// check if a newer runnable is available
					if (fAsynchRunnableCounter != fRedrawMapCounter) {
						// a newer queryRedraw is available
						return;
					}

					drawMap();
				}
			};

			getDisplay().asyncExec(asynchImageRunnable);
		}

		// tell the overlay thread to draw the overlay images
		nextOverlayRedrawTime = fRequestedRedrawTime;
	}

	/**
	 * Set tile in the overlay painting queue
	 * 
	 * @param tile
	 */
	private void queueOverlayPainting(final Tile tile) {

		tile.setOverlayTourStatus(OverlayTourStatus.IS_QUEUED);
		tile.setOverlayImageState(OverlayImageState.NOT_SET);

		fTileOverlayPaintQueue.add(tile);
	}

	/**
	 * Re-centers the map to have the current address location be at the center of the map,
	 * accounting for the map's width and height.
	 * 
	 * @see getAddressLocation
	 */
	public void recenterToAddressLocation() {
		final GeoPosition addressLocation = getAddressLocation();
		setMapPixelCenter(addressLocation, fTileFactory.geoToPixel(addressLocation, getZoom()));
		queueMapRedraw();
	}

	/**
	 * Reload the map by discarding all cached tiles and entries in the loading queue
	 */
	public synchronized void reload() {

		fTileFactory.resetAll(false);
		queueMapRedraw();
	}

	public void removeMapListener(final IMapListener listner) {
		fMapListeners.remove(listner);
	}

	/**
	 * Removes a map overlay.
	 * 
	 * @return the current map overlay
	 */
	public void removeOverlayPainter(final MapPainter overlay) {
		overlays.remove(overlay);
		queueMapRedraw();
	}

	public void removeZoomListener(final IZoomListener listner) {
		zoomListeners.remove(listner);
	}

	/**
	 * Reset overlay information for the current map provider by setting the overlay status to
	 * {@link OverlayTourStatus#OVERLAY_NOT_CHECKED} in all tiles
	 */
	public synchronized void resetOverlays() {
		if (fTileFactory != null) {
			fTileFactory.resetOverlays();
		}
	}

	public synchronized void resetTileFactory() {

		if (fTileFactory != null) {
			fTileFactory.resetAll(false);
		}

		queueMapRedraw();
	}

	/**
	 * Resets current tile factory and sets a new one. The new tile factory is displayed at the same
	 * position as the previous tile factory
	 * 
	 * @param tileFactory
	 */
	public synchronized void resetTileFactory(final TileFactory tileFactory) {

		if (fTileFactory != null) {
			// keep tiles with loading errors that they are not loaded again when the factory has not changed
			fTileFactory.resetAll(fTileFactory == tileFactory);
		}

		fTileFactory = tileFactory;

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
		this.addressLocation = addressLocation;
		setMapPixelCenter(addressLocation, fTileFactory.geoToPixel(addressLocation, getZoom()));
		queueMapRedraw();
	}

	/**
	 * Set map dimming level for the current map factory, this will dimm the map images
	 * 
	 * @param mapDimLevel
	 * @param dimColor
	 */
	public void setDimLevel(final int mapDimLevel, final RGB dimColor) {
		if (fTileFactory != null) {
			fTileFactory.setDimLevel(mapDimLevel, dimColor);
		}
	}

	/*
	 * (non-Javadoc)
	 * @seede.byteholder.geoclipse.swt.IDirectPainter#setDirectPainter(net.tourbook.mapping.
	 * DirectMappingPainter)
	 */
	public void setDirectPainter(final IDirectPainter directPainter) {
		directMapPainter = directPainter;
	}

	/**
	 * A property indicating the center position of the map
	 * 
	 * @param geoPosition
	 *            the new property value
	 */
	public void setGeoCenterPosition(final GeoPosition geoPosition) {

		getDisplay().syncExec(new Runnable() {
			public void run() {
				if (!isDisposed()) {
					setMapPixelCenter(geoPosition, fTileFactory.geoToPixel(geoPosition, fMapZoomLevel));
				}
			}
		});

		queueMapRedraw();
	}

	/**
	 * Set the legend for the map, the legend image will be disposed when the map is disposed,
	 * 
	 * @param legend
	 *            Legend for the map or <code>null</code> to disable the legend
	 */
	public void setLegend(final MapLegend legend) {

		if (legend == null && mapLegend != null) {
			// dispose legend image
			disposeResource(mapLegend.getImage());
		}

		mapLegend = legend;
	}

	public void setLiveView(final boolean isLiveView) {
		fIsLiveView = isLiveView;
	}

	/**
	 * Sets the center of the map in pixel coordinates.
	 * 
	 * @param fMapPixelCenter
	 *            the new center of the map in pixel coordinates
	 */
	private void setMapPixelCenter(final GeoPosition mapGeoCenter, Point2D mapPixelCenter) {

		if (isRestrictOutsidePanning()) {

			/*
			 * check if the center is within the map
			 */

			final int viewportHeight = getViewport().height;
			final Rectangle newVP = getViewport(mapPixelCenter);

			// don't let the user pan over the top edge
			if (newVP.y < 0) {
				final double centerY = viewportHeight / 2d;
				mapPixelCenter = new Point2D.Double(mapPixelCenter.getX(), centerY);
			}

			// don't let the user pan over the bottom edge
			final Dimension mapSize = fTileFactory.getMapSize(getZoom());
			final int mapHeight = (int) mapSize.getHeight() * fTileFactory.getTileSize();
			if (newVP.y + newVP.height > mapHeight) {
				final double centerY = mapHeight - viewportHeight / 2;
				mapPixelCenter = new Point2D.Double(mapPixelCenter.getX(), centerY);
			}

			// if map is too small then just center it
			if (mapHeight < newVP.height) {
				final double centerY = mapHeight / 2d;
				mapPixelCenter = new Point2D.Double(mapPixelCenter.getX(), centerY);
			}
		}

		fMapPixelCenter = mapPixelCenter;

//		fireMapEvent(mapGeoCenter);
		updateMouseMapPosition();
	}

	public void setMeasurementSystem(final float distanceUnitValue, final String distanceUnitLabel) {
		fDistanceUnitValue = distanceUnitValue;
		fDistanceUnitLabel = distanceUnitLabel;
	}

	/**
	 * Set a key to uniquely identify overlays which is used to cache the overlays
	 * 
	 * @param key
	 */
	public void setOverlayKey(final String key) {
		fOverlayKey = key;
	}

	/**
	 * A property indicating if the map should be pannable by the user using the mouse.
	 * 
	 * @param panEnabled
	 *            new property value
	 */
	public void setPanEnabled(final boolean panEnabled) {
		this.panEnabled = panEnabled;
	}

	/**
	 * Sets whether the map should recenter itself on mouse clicks (middle mouse clicks?)
	 * 
	 * @param b
	 *            if should recenter
	 */
	public void setRecenterOnClickEnabled(final boolean b) {
		recenterOnClickEnabled = b;
	}

	public void setRestrictOutsidePanning(final boolean restrictOutsidePanning) {
		this.restrictOutsidePanning = restrictOutsidePanning;
	}

	/**
	 * Set if the tile borders should be drawn. Mainly used for debugging.
	 * 
	 * @param isShowDebugInfo
	 *            new value of this drawTileBorders
	 */
	public void setShowDebugInfo(final boolean isShowDebugInfo) {
		fIsShowTileInfo = isShowDebugInfo;
		queueMapRedraw();
	}

	/**
	 * Legend will be drawn into the map when the visibility is <code>true</code>
	 * 
	 * @param visibility
	 */
	public void setShowLegend(final boolean visibility) {
		isLegendVisible = visibility;
	}

	/**
	 * set status if overlays are painted, a {@link #queueMapRedraw()} must be called to update the
	 * map
	 * 
	 * @param showOverlays
	 *            set <code>true</code> to see the overlays, <code>false</code> to hide the overlays
	 */
	public void setShowOverlays(final boolean showOverlays) {
		fIsDrawOverlays = showOverlays;
	}

	public void setShowScale(final boolean isScaleVisible) {
		fIsScaleVisible = isScaleVisible;
	}

	/**
	 * Set the tile factory for the map and redraw the map with the new tile factory
	 * 
	 * @param factory
	 *            the new property value
	 */
	public void setTileFactory(final TileFactory factory) {

		GeoPosition center = null;
		int zoom = 0;
		boolean refresh = false;

		if (fTileFactory != null) {
			center = getCenterPosition();
			zoom = getZoom();
			refresh = true;
		}

		fTileFactory = factory;

		if (refresh) {
			setZoom(zoom);
			setGeoCenterPosition(center);
		} else {
			if (factory.getInfo() != null) {
				setZoom(factory.getInfo().getDefaultZoomLevel());
			}
		}

		queueMapRedraw();
	}

	/**
	 * Set the current zoom level and centers the map to the previous center
	 * 
	 * @param zoom
	 *            the new zoom level, the zoom level is adjusted to the min/max zoom levels
	 */
	public void setZoom(int zoom) {

		if (fTileFactory == null) {
			return;
		}

		fTileFactory.resetTileQueue();

		final TileFactoryInfo info = fTileFactory.getInfo();

		// do nothing if we are out of the valid zoom levels
		if (info != null && (zoom < info.getMinimumZoomLevel() || zoom > info.getMaximumZoomLevel())) {
			zoom = Math.max(zoom, info.getMinimumZoomLevel());
			zoom = Math.min(zoom, info.getMaximumZoomLevel());
		}

		final int oldzoom = fMapZoomLevel;
		final Point2D oldCenter = getCenter();
		final Dimension oldMapSize = fTileFactory.getMapSize(oldzoom);
		fMapZoomLevel = zoom;

		final Dimension mapSize = fTileFactory.getMapSize(zoom);

		final Point2D.Double pixelCenter = new Point2D.Double(oldCenter.getX()
				* (mapSize.getWidth() / oldMapSize.getWidth()), oldCenter.getY()
				* (mapSize.getHeight() / oldMapSize.getHeight()));

		setMapPixelCenter(fTileFactory.pixelToGeo(pixelCenter, zoom), pixelCenter);

		fireZoomEvent(zoom);
	}

	/**
	 * A property indicating if the map should be zoomable by the user using the mouse wheel.
	 * 
	 * @param zoomEnabled
	 *            the new value of the property
	 */
	public void setZoomEnabled(final boolean zoomEnabled) {
		this.zoomEnabled = zoomEnabled;
	}

	public void setZoomOnDoubleClickEnabled(final boolean zoomOnDoubleClickEnabled) {
		this.zoomOnDoubleClickEnabled = zoomOnDoubleClickEnabled;
	}

	private void updateMouseMapPosition() {

		// check position, can be initially be null
		if (fMouseMovePosition == null || fTileFactory == null) {
			return;
		}

		final Rectangle viewPort = getViewport();
		final int worldMouseX = viewPort.x + fMouseMovePosition.x;
		final int worldMouseY = viewPort.y + fMouseMovePosition.y;

		fireMapEvent(fTileFactory.pixelToGeo(new Point2D.Double(worldMouseX, worldMouseY), fMapZoomLevel));
	}

	public void zoomIn() {
		setZoom(getZoom() + 1);
		queueMapRedraw();
	}

	public void zoomOut() {
		setZoom(getZoom() - 1);
		queueMapRedraw();
	}
}
