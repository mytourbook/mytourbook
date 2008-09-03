/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

package net.tourbook.mapping;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.colors.ColorDefinition;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourPropertyListener;
import net.tourbook.tour.SelectionActiveEditor;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourEditor;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICatalogReferenceTour;
import net.tourbook.ui.views.tourCatalog.TVICompareResultComparedTour;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.part.ViewPart;

import de.byteholder.geoclipse.GeoclipseExtensions;
import de.byteholder.geoclipse.map.TileFactory;
import de.byteholder.geoclipse.map.TileFactoryInfo;
import de.byteholder.geoclipse.swt.Map;
import de.byteholder.geoclipse.swt.MapLegend;
import de.byteholder.gpx.GeoPosition;
import de.byteholder.gpx.ext.PointOfInterest;

/**
 * @author Wolfgang Schramm
 * @since 1.3.0
 */
public class MappingView extends ViewPart {

	public static final String						ID									= "net.tourbook.mapping.mappingViewID";	//$NON-NLS-1$

	public static final int							TOUR_COLOR_DEFAULT					= 0;
	public static final int							TOUR_COLOR_ALTITUDE					= 10;
	public static final int							TOUR_COLOR_GRADIENT					= 20;
	public static final int							TOUR_COLOR_PULSE					= 30;
	public static final int							TOUR_COLOR_SPEED					= 40;
	public static final int							TOUR_COLOR_PACE						= 50;

	private static final String						MEMENTO_SHOW_START_END_IN_MAP		= "action.show-start-end-in-map";			//$NON-NLS-1$
	private static final String						MEMENTO_SHOW_TOUR_MARKER			= "action.show-tour-marker";				//$NON-NLS-1$
	private static final String						MEMENTO_SHOW_SLIDER_IN_MAP			= "action.show-slider-in-map";				//$NON-NLS-1$
	private static final String						MEMENTO_SHOW_SLIDER_IN_LEGEND		= "action.show-slider-in-legend";			//$NON-NLS-1$
	private static final String						MEMENTO_SHOW_LEGEND_IN_MAP			= "action.show-legend-in-map";				//$NON-NLS-1$
	private static final String						MEMENTO_ZOOM_CENTERED				= "action.zoom-centered";					//$NON-NLS-1$
	private static final String						MEMENTO_SHOW_TOUR_IN_MAP			= "action.show-tour-in-map";				//$NON-NLS-1$
	private static final String						MEMENTO_SYNCH_WITH_SELECTED_TOUR	= "action.synch-with-selected-tour";		//$NON-NLS-1$
	private static final String						MEMENTO_SYNCH_WITH_TOURCHART_SLIDER	= "action.synch-with-tourchart-slider";	//$NON-NLS-1$
	private static final String						MEMENTO_SYNCH_TOUR_ZOOM_LEVEL		= "synch-tour-zoom-level";					//$NON-NLS-1$
	private static final String						MEMENTO_CURRENT_FACTORY_ID			= "current.factory-id";					//$NON-NLS-1$

	private static final String						MEMENTO_DEFAULT_POSITION_ZOOM		= "default.position.zoom-level";			//$NON-NLS-1$
	private static final String						MEMENTO_DEFAULT_POSITION_LATITUDE	= "default.position.latitude";				//$NON-NLS-1$
	private static final String						MEMENTO_DEFAULT_POSITION_LONGITUDE	= "default.position.longitude";			//$NON-NLS-1$

	private static final String						MEMENTO_TOUR_COLOR_ID				= "tour-color-id";							//$NON-NLS-1$

	final static String								SHOW_TILE_INFO						= "show.tile-info";						//$NON-NLS-1$

	public static final int							LEGEND_MARGIN_TOP_BOTTOM			= 10;
	public static final int							LEGEND_UNIT_DISTANCE				= 60;

	private static IMemento							fSessionMemento;

	private Map										fMap;

	private ISelectionListener						fPostSelectionListener;
	private IPropertyChangeListener					fPrefChangeListener;
	private IPropertyChangeListener					fTourbookPrefChangeListener;
	private IPartListener2							fPartListener;
	private ITourPropertyListener					fTourPropertyListener;
	private IPropertyListener						fTourChangeListener;

	private TourData								fTourData;
	private TourData								fPreviousTourData;

	private ActionFailedMapImages					fActionFailedMapImages;
	private ActionSelectMapProvider					fActionSelectMapProvider;
	private ActionSaveDefaultPosition				fActionSaveDefaultPosition;
	private ActionSetDefaultPosition				fActionSetDefaultPosition;
	private ActionShowTourInMap						fActionShowTourInMap;
	private ActionShowLegendInMap					fActionShowLegendInMap;
	private ActionShowSliderInMap					fActionShowSliderInMap;
	private ActionShowSliderInLegend				fActionShowSliderInLegend;
	private ActionShowStartEndInMap					fActionShowStartEndInMap;
	private ActionShowTourMarker					fActionShowTourMarker;
	private ActionSynchWithTour						fActionSynchWithTour;
	private ActionSynchWithSlider					fActionSynchWithSlider;
	private ActionSynchTourZoomLevel				fActionSynchTourZoomLevel;
	private ActionTourColor							fActionTourColorAltitude;
	private ActionTourColor							fActionTourColorGradient;
	private ActionTourColor							fActionTourColorPulse;
	private ActionTourColor							fActionTourColorSpeed;
	private ActionTourColor							fActionTourColorPace;
	private ActionZoomIn							fActionZoomIn;
	private ActionZoomOut							fActionZoomOut;
	private ActionZoomCentered						fActionZoomCentered;
	private ActionZoomShowAll						fActionZoomShowAll;
	private ActionZoomShowEntireTour				fActionZoomShowEntireTour;

	private boolean									fIsMapSynchedWithTour;
	private boolean									fIsMapSynchedWithSlider;
	private boolean									fIsPositionCentered;

	private List<MapProvider>						fTileFactories;

	private int										fDefaultZoom;
	private GeoPosition								fDefaultPosition					= null;

	private boolean									fIsTour;

	/**
	 * Position for the current point of interest
	 */
	private GeoPosition								fPOIPosition;

	private final DirectMappingPainter				fDirectMappingPainter				= new DirectMappingPainter();

	/*
	 * current position for the x-sliders
	 */
	private int										fCurrentLeftSliderValueIndex;
	private int										fCurrentRightSliderValueIndex;
	private int										fCurrentSelectedSliderValueIndex;

	private MapLegend								fMapLegend;
	private final HashMap<Integer, ILegendProvider>	fLegendProviders					= new HashMap<Integer, ILegendProvider>();

	public MappingView() {}

	public void actionFailedMapImages() {
		fMap.reload();
	}

	void actionOpenMapProviderDialog() {

		final ModifyMapProviderDialog dialog = new ModifyMapProviderDialog(Display.getCurrent().getActiveShell(), this);

		if (dialog.open() == Window.OK) {
			fActionSelectMapProvider.updateMapProviders();
		}
	}

	void actionSaveDefaultPosition() {
		fDefaultZoom = fMap.getZoom();
		fDefaultPosition = fMap.getCenterPosition();
	}

	void actionSetDefaultPosition() {
		if (fDefaultPosition == null) {
			fMap.setZoom(fMap.getTileFactory().getInfo().getMinimumZoomLevel());
			fMap.setCenterPosition(new GeoPosition(0, 0));
		} else {
			fMap.setZoom(fDefaultZoom);
			fMap.setCenterPosition(fDefaultPosition);
		}
		fMap.queueRedrawMap();
	}

	void actionSetShowLegendInMap() {

		final boolean isLegendVisible = fActionShowLegendInMap.isChecked();

		fMap.setShowLegend(isLegendVisible);

		fActionShowSliderInLegend.setEnabled(isLegendVisible);
		if (isLegendVisible == false) {
			fActionShowSliderInLegend.setChecked(false);
		}

		// update legend
		actionShowSlider();

		fMap.queueRedrawMap();
	}

	void actionSetShowStartEndInMap() {

		PaintManager.getInstance().setShowStartEnd(fActionShowStartEndInMap.isChecked());

		fMap.resetOverlayImageCache();
		fMap.queueRedrawMap();
	}

	void actionSetShowTourInMap() {

		// show/hide legend
		fMap.setShowLegend(fActionShowTourInMap.isChecked());

		paintTour(fTourData, true, true);
	}

	void actionSetShowTourMarkerInMap() {

		PaintManager.getInstance().setShowTourMarker(fActionShowTourMarker.isChecked());

		fMap.resetOverlayImageCache();
		fMap.queueRedrawMap();
	}

	void actionSetTourColor(final int colorId) {
		PaintManager.getInstance().setLegendProvider(getLegendProvider(colorId));
		fMap.resetOverlayImageCache();
		fMap.queueRedrawMap();
		createLegendImage(getLegendProvider(colorId));
	}

	void actionSetZoomCentered() {
		fIsPositionCentered = fActionZoomCentered.isChecked();
	}

	void actionShowSlider() {

		// repaint map
		fDirectMappingPainter.setPaintContext(fMap,
				fActionShowTourInMap.isChecked(),
				fTourData,
				fCurrentLeftSliderValueIndex,
				fCurrentRightSliderValueIndex,
				fActionShowSliderInMap.isChecked(),
				fActionShowSliderInLegend.isChecked());

		fMap.redraw();
	}

	void actionSynchWithSlider() {

		fIsMapSynchedWithSlider = fActionSynchWithSlider.isChecked();

		if (fIsMapSynchedWithSlider) {

			fActionShowTourInMap.setChecked(true);

			// map must be synched with selected tour
			fActionSynchWithTour.setChecked(true);
			fIsMapSynchedWithTour = true;

			fMap.setShowOverlays(true);

			paintTour(fTourData, false, false);

			setMapToSliderBounds(fTourData);
		}
	}

	void actionSynchWithTour() {

		fIsMapSynchedWithTour = fActionSynchWithTour.isChecked();

		if (fIsMapSynchedWithTour) {

			fActionShowTourInMap.setChecked(true);
			fMap.setShowOverlays(true);

			paintTour(fTourData, true, false);

		} else {

			// disable synch with slider
			fIsMapSynchedWithSlider = false;
			fActionSynchWithSlider.setChecked(false);
		}
	}

	void actionZoomIn() {
		fMap.setZoom(fMap.getZoom() + 1);
		centerTour();
		fMap.queueRedrawMap();
	}

	void actionZoomOut() {
		fMap.setZoom(fMap.getZoom() - 1);
		centerTour();
		fMap.queueRedrawMap();
	}

	void actionZoomShowEntireMap() {
		fMap.setZoom(fMap.getTileFactory().getInfo().getMinimumZoomLevel());
		fMap.queueRedrawMap();
	}

	void actionZoomShowEntireTour() {

		fActionShowTourInMap.setChecked(true);
		fMap.setShowOverlays(true);

		paintEntireTour();
	}

	private void addPartListener() {

		fPartListener = new IPartListener2() {
			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId())) {
					saveSettings();
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId())) {
					saveSettings();
				}
			}

			public void partHidden(final IWorkbenchPartReference partRef) {}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {}
		};
		getViewSite().getPage().addPartListener(fPartListener);
	}

	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(SHOW_TILE_INFO)) {

					// map properties has changed

					final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();
					final boolean isShowTileInfo = store.getBoolean(SHOW_TILE_INFO);

					fMap.setDrawTileBorders(isShowTileInfo);
					fMap.queueRedrawMap();

				} else if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)) {

					// update tour and legend

					createLegendImage(PaintManager.getInstance().getLegendProvider());

					fMap.resetOverlayImageCache();
					fMap.queueRedrawMap();
				}
			}
		};
		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		fPostSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
				onChangeSelection(selection);
			}
		};
		getViewSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void addTourbookPrefListener() {

		fTourbookPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					UI.updateUnits();

					createLegendImage(PaintManager.getInstance().getLegendProvider());

					fMap.queueRedrawMap();
				}
			}
		};

		// register the listener
		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fTourbookPrefChangeListener);
	}

	private void addTourChangeListener() {

		fTourChangeListener = new IPropertyListener() {

			public void propertyChanged(final Object source, final int propId) {
				if (propId == TourDatabase.TOUR_IS_CHANGED) {}
				resetMap();
			}
		};

		TourDatabase.getInstance().addPropertyListener(fTourChangeListener);
	}

	private void addTourPropertyListener() {

		fTourPropertyListener = new ITourPropertyListener() {
			@SuppressWarnings("unchecked")
			public void propertyChanged(final int propertyId, final Object propertyData) {

				if (propertyId == TourManager.TOUR_CHART_PROPERTY_IS_MODIFIED) {

					resetMap();

				} else if (propertyId == TourManager.TOUR_PROPERTIES_CHANGED) {

					if (fTourData == null) {
						return;
					}

					// get modified tours
					final ArrayList<TourData> modifiedTours = (ArrayList<TourData>) propertyData;
					final long tourId = fTourData.getTourId();

					// check if the tour in the editor was modified
					for (final TourData tourData : modifiedTours) {
						if (tourData.getTourId() == tourId) {

							// keep changed data
							fTourData = tourData;

							resetMap();

							return;
						}
					}

				} else if (propertyId == TourManager.SLIDER_POSITION_CHANGED) {
					onChangeSelection((ISelection) propertyData);
				}
			}
		};

		TourManager.getInstance().addPropertyListener(fTourPropertyListener);
	}

	/**
	 * Center the tour in the map when action is enabled
	 */
	private void centerTour() {

		if (fIsPositionCentered) {

			final int zoom = fMap.getZoom();

			Set<GeoPosition> positionBounds = null;
			if (fIsTour) {
				positionBounds = PaintManager.getInstance().getTourBounds();
				if (positionBounds == null) {
					return;
				}
			} else {
				if (fPOIPosition == null) {
					return;
				}
				positionBounds = new HashSet<GeoPosition>();
				positionBounds.add(fPOIPosition);
			}

			final Rectangle2D positionRect = getPositionRect(positionBounds, zoom);

			final Point2D center = new Point2D.Double(//
			positionRect.getX() + positionRect.getWidth() / 2,
					positionRect.getY() + positionRect.getHeight() / 2);

			final GeoPosition geoPosition = fMap.getTileFactory().pixelToGeo(center, zoom);

			fMap.setCenterPosition(geoPosition);
		}
	}

	private void createActions() {

		fActionTourColorAltitude = new ActionTourColor(this,
				TOUR_COLOR_ALTITUDE,
				Messages.map_action_tour_color_altitude_tooltip,
				Messages.image_action_tour_color_altitude,
				Messages.image_action_tour_color_altitude_disabled);

		fActionTourColorGradient = new ActionTourColor(this,
				TOUR_COLOR_GRADIENT,
				Messages.map_action_tour_color_gradient_tooltip,
				Messages.image_action_tour_color_gradient,
				Messages.image_action_tour_color_gradient_disabled);

		fActionTourColorPulse = new ActionTourColor(this,
				TOUR_COLOR_PULSE,
				Messages.map_action_tour_color_pulse_tooltip,
				Messages.image_action_tour_color_pulse,
				Messages.image_action_tour_color_pulse_disabled);

		fActionTourColorSpeed = new ActionTourColor(this,
				TOUR_COLOR_SPEED,
				Messages.map_action_tour_color_speed_tooltip,
				Messages.image_action_tour_color_speed,
				Messages.image_action_tour_color_speed_disabled);

		fActionTourColorPace = new ActionTourColor(this,
				TOUR_COLOR_PACE,
				Messages.map_action_tour_color_pase_tooltip,
				Messages.image_action_tour_color_pace,
				Messages.image_action_tour_color_pace_disabled);

		fActionZoomIn = new ActionZoomIn(this);
		fActionZoomOut = new ActionZoomOut(this);
		fActionZoomCentered = new ActionZoomCentered(this);
		fActionZoomShowAll = new ActionZoomShowAll(this);
		fActionZoomShowEntireTour = new ActionZoomShowEntireTour(this);
		fActionSynchWithTour = new ActionSynchWithTour(this);
		fActionSynchWithSlider = new ActionSynchWithSlider(this);
		fActionShowTourInMap = new ActionShowTourInMap(this);
		fActionSynchTourZoomLevel = new ActionSynchTourZoomLevel(this);
		fActionSelectMapProvider = new ActionSelectMapProvider(this);
		fActionSetDefaultPosition = new ActionSetDefaultPosition(this);
		fActionSaveDefaultPosition = new ActionSaveDefaultPosition(this);
		fActionShowSliderInMap = new ActionShowSliderInMap(this);
		fActionShowSliderInLegend = new ActionShowSliderInLegend(this);
		fActionShowLegendInMap = new ActionShowLegendInMap(this);
		fActionShowStartEndInMap = new ActionShowStartEndInMap(this);
		fActionShowTourMarker = new ActionShowTourMarker(this);
		fActionFailedMapImages = new ActionFailedMapImages(this);

		/*
		 * fill view toolbar
		 */
		final IToolBarManager viewTbm = getViewSite().getActionBars().getToolBarManager();

		viewTbm.add(fActionTourColorAltitude);
		viewTbm.add(fActionTourColorPulse);
		viewTbm.add(fActionTourColorSpeed);
		viewTbm.add(fActionTourColorPace);
		viewTbm.add(fActionTourColorGradient);
		viewTbm.add(new Separator());
		viewTbm.add(fActionShowTourInMap);
		viewTbm.add(fActionZoomShowEntireTour);
		viewTbm.add(fActionSynchWithTour);
		viewTbm.add(fActionSynchWithSlider);
		viewTbm.add(new Separator());
		viewTbm.add(fActionZoomCentered);
		viewTbm.add(fActionZoomIn);
		viewTbm.add(fActionZoomOut);
		viewTbm.add(new Separator());
		viewTbm.add(fActionSelectMapProvider);
		viewTbm.add(fActionZoomShowAll);

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

		menuMgr.add(fActionShowStartEndInMap);
		menuMgr.add(fActionShowTourMarker);
		menuMgr.add(fActionShowLegendInMap);
		menuMgr.add(fActionShowSliderInMap);
		menuMgr.add(fActionShowSliderInLegend);
		menuMgr.add(new Separator());
		menuMgr.add(fActionSetDefaultPosition);
		menuMgr.add(fActionSaveDefaultPosition);
		menuMgr.add(new Separator());
		menuMgr.add(fActionSynchTourZoomLevel);
		menuMgr.add(fActionFailedMapImages);
	}

	/**
	 * Creates a new legend image and disposes the old image
	 * 
	 * @param legendProvider
	 */
	private void createLegendImage(final ILegendProvider legendProvider) {

		Image legendImage = fMapLegend.getImage();

		// legend requires a tour with coordinates
		if (legendProvider == null || isPaintDataValid(fTourData) == false) {
			showDefaultMap();
			return;
		}

		// dispose old legend image
		if (legendImage != null && !legendImage.isDisposed()) {
			legendImage.dispose();
		}
		final int legendWidth = 150;
		final int legendHeight = 300;

		final RGB rgbTransparent = new RGB(0xfe, 0xfe, 0xfe);

		final ImageData overlayImageData = new ImageData(legendWidth, legendHeight, 24, //
				new PaletteData(0xff, 0xff00, 0xff00000));

		overlayImageData.transparentPixel = overlayImageData.palette.getPixel(rgbTransparent);

		final Display display = Display.getCurrent();
		legendImage = new Image(display, overlayImageData);
		final Rectangle legendImageBounds = legendImage.getBounds();

		final boolean isDataAvailable = updateLegendValues(legendProvider, legendImageBounds);

		final Color transparentColor = new Color(display, rgbTransparent);
		final GC gc = new GC(legendImage);
		{
			gc.setBackground(transparentColor);
			gc.fillRectangle(legendImageBounds);

			if (isDataAvailable) {
				TourPainter.drawLegendColors(gc, legendImageBounds, legendProvider, true);
			}
		}
		gc.dispose();
		transparentColor.dispose();

		fMapLegend.setImage(legendImage);
	}

	private void createLegendProviders() {

		fLegendProviders.put(MappingView.TOUR_COLOR_PULSE, //
				new LegendProvider(new LegendConfig(), new LegendColor(), MappingView.TOUR_COLOR_PULSE));

		fLegendProviders.put(MappingView.TOUR_COLOR_ALTITUDE, //
				new LegendProvider(new LegendConfig(), new LegendColor(), MappingView.TOUR_COLOR_ALTITUDE));

		fLegendProviders.put(MappingView.TOUR_COLOR_SPEED, //
				new LegendProvider(new LegendConfig(), new LegendColor(), MappingView.TOUR_COLOR_SPEED));

		fLegendProviders.put(MappingView.TOUR_COLOR_PACE, //
				new LegendProvider(new LegendConfig(), new LegendColor(), MappingView.TOUR_COLOR_PACE));

		fLegendProviders.put(MappingView.TOUR_COLOR_GRADIENT, //
				new LegendProvider(new LegendConfig(), new LegendColor(), MappingView.TOUR_COLOR_GRADIENT));

	}

	@Override
	public void createPartControl(final Composite parent) {

		fMap = new Map(parent);
		fMap.setDirectPainter(fDirectMappingPainter);

		fMapLegend = new MapLegend();
		fMap.setLegend(fMapLegend);
		fMap.setShowLegend(true);

		// create list with all map factories
		fTileFactories = new ArrayList<MapProvider>();
		final List<TileFactory> tileFactories = GeoclipseExtensions.getInstance().readExtensions(fMap);
		for (final TileFactory tileFactory : tileFactories) {

			final MapProvider mapProvider = new MapProvider(tileFactory, tileFactory.getProjection());
			fTileFactories.add(mapProvider);
		}

		createActions();
		createLegendProviders();

		addPartListener();
		addPrefListener();
		addSelectionListener();
		addTourPropertyListener();
		addTourbookPrefListener();
		addTourChangeListener();

		restoreSettings();

		// show map from last selection
		onChangeSelection(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		fMap.queueRedrawMap();
	}

	@Override
	public void dispose() {

		// dispose tilefactory resources
		for (final TileFactory tileFactory : fTileFactories) {
			tileFactory.dispose();
		}

		getViewSite().getPage().removePostSelectionListener(fPostSelectionListener);
		getViewSite().getPage().removePartListener(fPartListener);

		TourDatabase.getInstance().removePropertyListener(fTourChangeListener);
		TourManager.getInstance().removePropertyListener(fTourPropertyListener);

		final TourbookPlugin tourbookPlugin = TourbookPlugin.getDefault();
		tourbookPlugin.getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);
		tourbookPlugin.getPluginPreferences().removePropertyChangeListener(fTourbookPrefChangeListener);

		super.dispose();
	}

	private void enableActions() {

		// update legend action
		if (fIsTour) {

			final boolean isLegendVisible = fActionShowLegendInMap.isChecked();

			fMap.setShowLegend(isLegendVisible);

			fActionShowSliderInLegend.setEnabled(isLegendVisible);
			if (isLegendVisible == false) {
				fActionShowSliderInLegend.setChecked(false);
			}
		}

		/*
		 * enable/disable tour actions
		 */
		fActionZoomShowEntireTour.setEnabled(fIsTour);
		fActionSynchTourZoomLevel.setEnabled(fIsTour);
		fActionShowTourInMap.setEnabled(fIsTour);
		fActionSynchWithTour.setEnabled(fIsTour);
		fActionSynchWithSlider.setEnabled(fIsTour);

		fActionShowStartEndInMap.setEnabled(fIsTour);
		fActionShowTourMarker.setEnabled(fIsTour);
		fActionShowLegendInMap.setEnabled(fIsTour);
		fActionShowSliderInMap.setEnabled(fIsTour);
		fActionShowSliderInLegend.setEnabled(fIsTour);

		if (fIsTour && fTourData != null) {
			fActionTourColorAltitude.setEnabled(true);
			fActionTourColorGradient.setEnabled(fTourData.getGradientSerie() != null);
			fActionTourColorPulse.setEnabled(fTourData.pulseSerie != null);
			fActionTourColorSpeed.setEnabled(fTourData.getSpeedSerie() != null);
			fActionTourColorPace.setEnabled(fTourData.getPaceSerie() != null);
		} else {
			fActionTourColorAltitude.setEnabled(false);
			fActionTourColorGradient.setEnabled(false);
			fActionTourColorPulse.setEnabled(false);
			fActionTourColorSpeed.setEnabled(false);
			fActionTourColorPace.setEnabled(false);
		}
	}

	public List<MapProvider> getFactories() {
		return fTileFactories;
	}

	private ILegendProvider getLegendProvider(final int colorId) {
		return fLegendProviders.get(colorId);
	}

	public Map getMap() {
		return fMap;
	}

	private Rectangle2D getPositionRect(final Set<GeoPosition> positions, final int zoom) {

		final TileFactory tileFactory = fMap.getTileFactory();
		final Point2D point1 = tileFactory.geoToPixel(positions.iterator().next(), zoom);
		final Rectangle2D rect = new Rectangle2D.Double(point1.getX(), point1.getY(), 0, 0);

		for (final GeoPosition pos : positions) {
			final Point2D point = tileFactory.geoToPixel(pos, zoom);
			rect.add(point);
		}

		return rect;
	}

	/**
	 * Calculate the bounds for the tour in latitude and longitude values
	 * 
	 * @param tourData
	 * @return
	 */
	private Set<GeoPosition> getTourBounds(final TourData tourData) {

		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;

		/*
		 * get min/max longitude/latitude
		 */
		double minLatitude = latitudeSerie[0];
		double maxLatitude = latitudeSerie[0];
		double minLongitude = longitudeSerie[0];
		double maxLongitude = longitudeSerie[0];

		for (int serieIndex = 0; serieIndex < latitudeSerie.length; serieIndex++) {
			final double latitude = latitudeSerie[serieIndex];
			final double longitude = longitudeSerie[serieIndex];

			minLatitude = Math.min(minLatitude, latitude);
			maxLatitude = Math.max(maxLatitude, latitude);

			minLongitude = Math.min(minLongitude, longitude);
			maxLongitude = Math.max(maxLongitude, longitude);

			if (minLatitude == 0) {
				minLatitude = -180D;
			}
		}

		final Set<GeoPosition> mapPositions = new HashSet<GeoPosition>();
		mapPositions.add(new GeoPosition(minLatitude, minLongitude));
		mapPositions.add(new GeoPosition(maxLatitude, maxLongitude));

		return mapPositions;
	}

	@Override
	public void init(final IViewSite site, final IMemento memento) throws PartInitException {

		super.init(site, memento);

		// set session memento
		if (fSessionMemento == null) {
			fSessionMemento = memento;
		}
	}

	/**
	 * Checks if {@link TourData} can be painted
	 * 
	 * @param tourData
	 * @return <code>true</code> when the {@link TourData} can be painted
	 */
	private boolean isPaintDataValid(final TourData tourData) {

		if (tourData == null) {
			return false;
		}

		// check if coordinates are available
		final double[] longitudeSerie = tourData.longitudeSerie;
		final double[] latitudeSerie = tourData.latitudeSerie;
		if (longitudeSerie == null || longitudeSerie.length == 0 || latitudeSerie == null || latitudeSerie.length == 0) {
			return false;
		}

		return true;
	}

	private void onChangeSelection(final ISelection selection) {

		if (selection instanceof SelectionTourData) {

			final SelectionTourData selectionTourData = (SelectionTourData) selection;
			final TourData tourData = selectionTourData.getTourData();

			paintTour(tourData, false, false);

		} else if (selection instanceof SelectionTourId) {

			final SelectionTourId tourIdSelection = (SelectionTourId) selection;
			final TourData tourData = TourManager.getInstance().getTourData(tourIdSelection.getTourId());

			paintTour(tourData, false, false);

		} else if (selection instanceof SelectionActiveEditor) {

			final IEditorPart editor = ((SelectionActiveEditor) selection).getEditor();

			if (editor instanceof TourEditor) {

				final TourEditor fTourEditor = (TourEditor) editor;
				final TourChart fTourChart = fTourEditor.getTourChart();
				final TourData tourData = fTourChart.getTourData();

				paintTour(tourData, false, false);

				final SelectionChartInfo chartInfo = fTourChart.getChartInfo();
				paintTourSliders(tourData,
						chartInfo.leftSliderValuesIndex,
						chartInfo.rightSliderValuesIndex,
						chartInfo.selectedSliderValuesIndex);
			}

		} else if (selection instanceof SelectionChartInfo) {

			final ChartDataModel chartDataModel = ((SelectionChartInfo) selection).chartDataModel;
			if (chartDataModel != null) {

				final TourData tourData = (TourData) chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_DATA);
				final SelectionChartInfo chartInfo = (SelectionChartInfo) selection;

				paintTourSliders(tourData,
						chartInfo.leftSliderValuesIndex,
						chartInfo.rightSliderValuesIndex,
						chartInfo.selectedSliderValuesIndex);
			}

		} else if (selection instanceof SelectionChartXSliderPosition) {

			final SelectionChartXSliderPosition xSliderPos = (SelectionChartXSliderPosition) selection;

			final ChartDataModel chartDataModel = xSliderPos.getChart().getChartDataModel();
			final TourData tourData = (TourData) chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_DATA);

			final int leftSliderValueIndex = xSliderPos.getSlider1ValueIndex();
			int rightSliderValueIndex = xSliderPos.getSlider2ValueIndex();

			rightSliderValueIndex = rightSliderValueIndex == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION
					? leftSliderValueIndex
					: rightSliderValueIndex;

			paintTourSliders(tourData, leftSliderValueIndex, rightSliderValueIndex, leftSliderValueIndex);

		} else if (selection instanceof PointOfInterest) {

			fIsTour = false;

			// disable tour data
			fTourData = null;
			fPreviousTourData = null;
			PaintManager.getInstance().setTourData(null);

			final PointOfInterest poi = (PointOfInterest) selection;
			fPOIPosition = poi.getPosition();

			fMap.setZoom(poi.getRecommendedZoom());
			fMap.setCenterPosition(fPOIPosition);
			fMap.queueRedrawMap();

		} else if (selection instanceof StructuredSelection) {

			final Object firstElement = ((StructuredSelection) selection).getFirstElement();

			if (firstElement instanceof TVICatalogComparedTour) {

				final TVICatalogComparedTour comparedTour = (TVICatalogComparedTour) firstElement;
				final long tourId = comparedTour.getTourId();

				final TourData tourData = TourManager.getInstance().getTourData(tourId);
				paintTour(tourData, false, false);

			} else if (firstElement instanceof TVICompareResultComparedTour) {

				final TVICompareResultComparedTour compareResultItem = (TVICompareResultComparedTour) firstElement;
				final TourData tourData = TourManager.getInstance().getTourData(compareResultItem.getComparedTourData()
						.getTourId());
				paintTour(tourData, false, false);
			}

		} else if (selection instanceof SelectionTourCatalogView) {

			// show reference tour

			final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;

			final TVICatalogReferenceTour refItem = tourCatalogSelection.getRefItem();
			if (refItem != null) {

				final TourData tourData = TourManager.getInstance().getTourData(refItem.getTourId());
				paintTour(tourData, false, false);
			}

		}

		enableActions();
	}

	private void paintEntireTour() {

		if (isPaintDataValid(fTourData) == false) {
			showDefaultMap();
			return;
		}

		final PaintManager paintManager = PaintManager.getInstance();
		paintManager.setTourData(fTourData);

		// set slider position
		fDirectMappingPainter.setPaintContext(fMap,
				fActionShowTourInMap.isChecked(),
				fTourData,
				fCurrentLeftSliderValueIndex,
				fCurrentRightSliderValueIndex,
				fActionShowSliderInMap.isChecked(),
				fActionShowSliderInLegend.isChecked());

		final Set<GeoPosition> tourBounds = getTourBounds(fTourData);
		paintManager.setTourBounds(tourBounds);

		fMap.setShowOverlays(fActionShowTourInMap.isChecked());

		setTourZoomLevel(tourBounds, false);

		fMap.queueRedrawMap();
	}

	/**
	 * Paint the currently selected tour in the map
	 * 
	 * @param tourData
	 * @param forceRedraw
	 * @param ignoreSynch
	 *            when <code>true</code>, synchronization will be ignored
	 */
	private void paintTour(final TourData tourData, final boolean forceRedraw, final boolean ignoreSynch) {

		if (isPaintDataValid(tourData) == false) {
			showDefaultMap();
			return;
		}

		fIsTour = true;
		final boolean isShowTour = fActionShowTourInMap.isChecked();

		// prevent loading the same tour
		if (forceRedraw == false && tourData == fTourData) {
			return;
		}

		// check if this is a new tour
		boolean isNewTour = true;
		if (fPreviousTourData != null && fPreviousTourData.getTourId().longValue() == tourData.getTourId().longValue()) {
			isNewTour = false;
		}

		final PaintManager paintManager = PaintManager.getInstance();
		paintManager.setTourData(tourData);
		fTourData = tourData;

		// set the paint context (slider position) for the direct mapping
		// painter
		fDirectMappingPainter.setPaintContext(fMap,
				isShowTour,
				tourData,
				fCurrentLeftSliderValueIndex,
				fCurrentRightSliderValueIndex,
				fActionShowSliderInMap.isChecked(),
				fActionShowSliderInLegend.isChecked());

		// set the tour bounds
		final Set<GeoPosition> tourBounds = getTourBounds(tourData);
		paintManager.setTourBounds(tourBounds);

		fMap.setShowOverlays(isShowTour);
		fMap.setShowLegend(isShowTour && fActionShowLegendInMap.isChecked());

		// set position and zoom level for the tour
		if (fIsMapSynchedWithTour && ignoreSynch == false) {

			if (forceRedraw == false && fPreviousTourData != null) {

				/*
				 * keep map configuration for the previous tour
				 */
				fPreviousTourData.mapZoomLevel = fMap.getZoom();

				final GeoPosition centerPosition = fMap.getCenterPosition();
				fPreviousTourData.mapCenterPositionLatitude = centerPosition.getLatitude();
				fPreviousTourData.mapCenterPositionLongitude = centerPosition.getLongitude();
			}

			if (tourData.mapCenterPositionLatitude == Double.MIN_VALUE) {

				// use default position for the tour
				setTourZoomLevel(tourBounds, true);

			} else {

//				if (fIsMapSynchedWithSlider) {
//
//					fMap.setZoom(tourData.mapZoomLevel);
//					setMapToSliderBounds(tourData);
//
//				} else {

				// position tour to the previous position
				fMap.setZoom(tourData.mapZoomLevel);
				fMap.setCenterPosition(new GeoPosition(tourData.mapCenterPositionLatitude,
						tourData.mapCenterPositionLongitude));
//				}
			}
		}

		// keep tour data
		fPreviousTourData = tourData;

		if (isNewTour) {

			// adjust legend for the new tour
			createLegendImage(PaintManager.getInstance().getLegendProvider());

			fMap.setOverlayKey(tourData.getTourId().toString());
			fMap.resetOverlays();

			enableActions();
		}

		fMap.queueRedrawMap();
	}

	private void paintTourSliders(	final TourData tourData,
									final int leftSliderValuesIndex,
									final int rightSliderValuesIndex,
									final int selectedSliderIndex) {

		if (isPaintDataValid(tourData) == false) {
			showDefaultMap();
			return;
		}

		fIsTour = true;
		fCurrentLeftSliderValueIndex = leftSliderValuesIndex;
		fCurrentRightSliderValueIndex = rightSliderValuesIndex;
		fCurrentSelectedSliderValueIndex = selectedSliderIndex;

		fDirectMappingPainter.setPaintContext(fMap,
				fActionShowTourInMap.isChecked(),
				tourData,
				leftSliderValuesIndex,
				rightSliderValuesIndex,
				fActionShowSliderInMap.isChecked(),
				fActionShowSliderInLegend.isChecked());

		if (fIsMapSynchedWithSlider) {

//			fMap.setZoom(tourData.mapZoomLevel);
			setMapToSliderBounds(tourData);

			fMap.queueRedrawMap();

		} else {

			fMap.redraw();
		}
	}

	private void resetMap() {

		if (fTourData == null) {
			return;
		}

		fTourData.clearComputedSeries();

		paintTour(fTourData, true, true);

		fMap.resetOverlayImageCache();
		fMap.queueRedrawMap();
	}

	private void restoreSettings() {

		final IMemento memento = fSessionMemento;
		final PaintManager paintManager = PaintManager.getInstance();

		if (memento != null) {

			final Integer mementoZoomCentered = memento.getInteger(MEMENTO_ZOOM_CENTERED);
			if (mementoZoomCentered != null) {
				final boolean isTourCentered = mementoZoomCentered == 1 ? true : false;
				fActionZoomCentered.setChecked(isTourCentered);
				fIsPositionCentered = isTourCentered;
			}

			final Integer mementoSynchTour = memento.getInteger(MEMENTO_SYNCH_WITH_SELECTED_TOUR);
			if (mementoSynchTour != null) {

				final boolean isSynchTour = mementoSynchTour == 1 ? true : false;

				fActionSynchWithTour.setChecked(isSynchTour);
				fIsMapSynchedWithTour = isSynchTour;
			}

			final Integer mementoSynchSlider = memento.getInteger(MEMENTO_SYNCH_WITH_TOURCHART_SLIDER);
			if (mementoSynchSlider != null) {

				final boolean isSynchSlider = mementoSynchSlider == 1 ? true : false;

				fActionSynchWithSlider.setChecked(isSynchSlider);
				fIsMapSynchedWithSlider = isSynchSlider;
			}

			final Integer mementoShowTour = memento.getInteger(MEMENTO_SHOW_TOUR_IN_MAP);
			if (mementoShowTour != null) {

				final boolean isShowTour = mementoShowTour == 1 ? true : false;

				fActionShowTourInMap.setChecked(isShowTour);
				fMap.setShowOverlays(isShowTour);
				fMap.setShowLegend(isShowTour);
			}

			final Integer zoomLevel = memento.getInteger(MEMENTO_SYNCH_TOUR_ZOOM_LEVEL);
			if (zoomLevel != null) {
				fActionSynchTourZoomLevel.setZoomLevel(zoomLevel);
			}

			// action: show start/end in map
			final Integer mementoShowStartEndInMap = memento.getInteger(MEMENTO_SHOW_START_END_IN_MAP);
			if (mementoShowStartEndInMap != null) {
				fActionShowStartEndInMap.setChecked(mementoShowStartEndInMap == 1);
			}
			paintManager.setShowStartEnd(fActionShowStartEndInMap.isChecked());

			// action: show tour marker
			final Integer mementoShowTourMarker = memento.getInteger(MEMENTO_SHOW_TOUR_MARKER);
			if (mementoShowTourMarker != null) {
				fActionShowTourMarker.setChecked(mementoShowTourMarker == 1);
			}
			paintManager.setShowTourMarker(fActionShowTourMarker.isChecked());

			// action: show legend in map
			final Integer mementoShowLegendInMap = memento.getInteger(MEMENTO_SHOW_LEGEND_IN_MAP);
			if (mementoShowLegendInMap != null) {
				fActionShowLegendInMap.setChecked(mementoShowLegendInMap == 1);
			}

			// action: show slider in map
			final Integer mementoShowSliderInMap = memento.getInteger(MEMENTO_SHOW_SLIDER_IN_MAP);
			if (mementoShowSliderInMap != null) {
				fActionShowSliderInMap.setChecked(mementoShowSliderInMap == 1);
			}

			// action: show slider in legend
			final Integer mementoShowSliderInLegend = memento.getInteger(MEMENTO_SHOW_SLIDER_IN_LEGEND);
			if (mementoShowSliderInLegend != null) {
				fActionShowSliderInLegend.setChecked(mementoShowSliderInLegend == 1);
			}

			// restore: factory ID
			fActionSelectMapProvider.setSelectedFactory(memento.getString(MEMENTO_CURRENT_FACTORY_ID));

			// restore: default position
			final Integer mementoZoom = memento.getInteger(MEMENTO_DEFAULT_POSITION_ZOOM);
			if (mementoZoom != null) {
				fDefaultZoom = mementoZoom;
			}
			final Float mementoLatitude = memento.getFloat(MEMENTO_DEFAULT_POSITION_LATITUDE);
			final Float mementoLongitude = memento.getFloat(MEMENTO_DEFAULT_POSITION_LONGITUDE);
			if (mementoLatitude != null && mementoLongitude != null) {
				fDefaultPosition = new GeoPosition(mementoLatitude, mementoLongitude);
			} else {
				fDefaultPosition = new GeoPosition(0, 0);
			}

			// tour color
			final Integer colorId = memento.getInteger(MEMENTO_TOUR_COLOR_ID);
			if (colorId != null) {

				switch (colorId) {
				case TOUR_COLOR_ALTITUDE:
					fActionTourColorAltitude.setChecked(true);
					break;

				case TOUR_COLOR_GRADIENT:
					fActionTourColorGradient.setChecked(true);
					break;

				case TOUR_COLOR_PULSE:
					fActionTourColorPulse.setChecked(true);
					break;

				case TOUR_COLOR_SPEED:
					fActionTourColorSpeed.setChecked(true);
					break;

				case TOUR_COLOR_PACE:
					fActionTourColorPace.setChecked(true);
					break;

				default:
					fActionTourColorAltitude.setChecked(true);
					break;
				}

				final ILegendProvider legendProvider = getLegendProvider(colorId);
				paintManager.setLegendProvider(legendProvider);
			}

		} else {

			// memento is not available, set default values

			fActionSelectMapProvider.setSelectedFactory(null);

			// draw tour with default color
			fActionTourColorAltitude.setChecked(true);
		}

		final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();

		// check legend provider
		final ILegendProvider legendProvider = paintManager.getLegendProvider();
		if (legendProvider == null) {

			// set default legend provider
			paintManager.setLegendProvider(getLegendProvider(TOUR_COLOR_ALTITUDE));

			// hide legend
			fMap.setShowLegend(false);
		}

		// debug info
		final boolean isShowTileInfo = store.getBoolean(MappingView.SHOW_TILE_INFO);
		fMap.setDrawTileBorders(isShowTileInfo);

		// show map with the default position
		actionSetDefaultPosition();
	}

	private void saveSettings() {
		fSessionMemento = XMLMemento.createWriteRoot("DeviceImportView"); //$NON-NLS-1$
		saveState(fSessionMemento);
	}

	@Override
	public void saveState(final IMemento memento) {

		// save checked actions
		memento.putInteger(MEMENTO_ZOOM_CENTERED, fActionZoomCentered.isChecked() ? 1 : 0);
		memento.putInteger(MEMENTO_SHOW_TOUR_IN_MAP, fActionShowTourInMap.isChecked() ? 1 : 0);
		memento.putInteger(MEMENTO_SYNCH_WITH_SELECTED_TOUR, fActionSynchWithTour.isChecked() ? 1 : 0);
		memento.putInteger(MEMENTO_SYNCH_WITH_TOURCHART_SLIDER, fActionSynchWithSlider.isChecked() ? 1 : 0);
		memento.putInteger(MEMENTO_SYNCH_TOUR_ZOOM_LEVEL, fActionSynchTourZoomLevel.getZoomLevel());

		memento.putInteger(MEMENTO_SHOW_START_END_IN_MAP, fActionShowStartEndInMap.isChecked() ? 1 : 0);
		memento.putInteger(MEMENTO_SHOW_TOUR_MARKER, fActionShowTourMarker.isChecked() ? 1 : 0);
		memento.putInteger(MEMENTO_SHOW_LEGEND_IN_MAP, fActionShowLegendInMap.isChecked() ? 1 : 0);
		memento.putInteger(MEMENTO_SHOW_SLIDER_IN_MAP, fActionShowSliderInMap.isChecked() ? 1 : 0);
		memento.putInteger(MEMENTO_SHOW_SLIDER_IN_LEGEND, fActionShowSliderInLegend.isChecked() ? 1 : 0);

		memento.putString(MEMENTO_CURRENT_FACTORY_ID, fActionSelectMapProvider.getSelectedFactory()
				.getInfo()
				.getFactoryID());

		if (fDefaultPosition == null) {
			memento.putInteger(MEMENTO_DEFAULT_POSITION_ZOOM, fMap.getTileFactory().getInfo().getMinimumZoomLevel());
			memento.putFloat(MEMENTO_DEFAULT_POSITION_LATITUDE, 0.0F);
			memento.putFloat(MEMENTO_DEFAULT_POSITION_LONGITUDE, 0.0F);
		} else {
			memento.putInteger(MEMENTO_DEFAULT_POSITION_ZOOM, fDefaultZoom);
			memento.putFloat(MEMENTO_DEFAULT_POSITION_LATITUDE, (float) fDefaultPosition.getLatitude());
			memento.putFloat(MEMENTO_DEFAULT_POSITION_LONGITUDE, (float) fDefaultPosition.getLongitude());
		}

		// tour color
		int colorId;

		if (fActionTourColorGradient.isChecked()) {
			colorId = TOUR_COLOR_GRADIENT;
		} else if (fActionTourColorPulse.isChecked()) {
			colorId = TOUR_COLOR_PULSE;
		} else if (fActionTourColorSpeed.isChecked()) {
			colorId = TOUR_COLOR_SPEED;
		} else if (fActionTourColorPace.isChecked()) {
			colorId = TOUR_COLOR_PACE;
		} else {
			colorId = TOUR_COLOR_ALTITUDE;
		}
		memento.putInteger(MEMENTO_TOUR_COLOR_ID, colorId);
	}

	@Override
	public void setFocus() {}

	/**
	 * Calculate the bounds for the tour in latitude and longitude values
	 * 
	 * @param tourData
	 * @return
	 */
	private void setMapToSliderBounds(final TourData tourData) {

		if (tourData == null) {
			return;
		}

		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;

//		final double leftSliderLat = latitudeSerie[fCurrentLeftSliderValueIndex];
//		final double leftSliderLong = longitudeSerie[fCurrentLeftSliderValueIndex];
//
//		final double rightSliderLat = latitudeSerie[fCurrentRightSliderValueIndex];
//		final double rightSliderLong = longitudeSerie[fCurrentRightSliderValueIndex];
//
//		final double minLatitude = Math.min(leftSliderLat + 0, rightSliderLat + 0);
//		final double minLongitude = Math.min(leftSliderLong + 0, rightSliderLong + 0);
//
//		final double maxLatitude = Math.max(leftSliderLat + 0, rightSliderLat + 0);
//		final double maxLongitude = Math.max(leftSliderLong + 0, rightSliderLong + 0);
//
//		final double latDiff2 = (maxLatitude - minLatitude) / 2;
//		final double longDiff2 = (maxLongitude - minLongitude) / 2;
//
//		final double sliderLat = minLatitude + latDiff2 - 0;
//		final double sliderLong = minLongitude + longDiff2 - 0;

//		fMap.setCenterPosition(new GeoPosition(sliderLat, sliderLong));
//		fMap.setCenterPosition(new GeoPosition(sliderLat, leftSliderLong));
//		fMap.setCenterPosition(new GeoPosition(leftSliderLat, leftSliderLong));

		fMap.setCenterPosition(new GeoPosition(latitudeSerie[fCurrentSelectedSliderValueIndex],
				longitudeSerie[fCurrentSelectedSliderValueIndex]));

	}

	/**
	 * Calculates a zoom level so that all points in the specified set will be visible on screen.
	 * This is useful if you have a bunch of points in an area like a city and you want to zoom out
	 * so that the entire city and it's points are visible without panning.
	 * 
	 * @param positions
	 *            A set of GeoPositions to calculate the new zoom from
	 * @param adjustZoomLevel
	 *            when <code>true</code> the zoom level will be adjusted to user settings
	 */
	private void setTourZoomLevel(final Set<GeoPosition> positions, final boolean isAdjustZoomLevel) {

		if (positions.size() < 2) {
			return;
		}

		final TileFactory tileFactory = fMap.getTileFactory();
		final TileFactoryInfo tileInfo = tileFactory.getInfo();

		final int maximumZoomLevel = tileInfo.getMaximumZoomLevel();
		int zoom = tileInfo.getMinimumZoomLevel();

		Rectangle2D positionRect = getPositionRect(positions, zoom);
		java.awt.Rectangle viewport = fMap.getViewport();

		// zoom until the tour is visible in the map
		while (!viewport.contains(positionRect)) {

			// center position in the map
			final Point2D center = new Point2D.Double(positionRect.getX() + positionRect.getWidth() / 2,
					positionRect.getY() + positionRect.getHeight() / 2);
			final GeoPosition px = tileFactory.pixelToGeo(center, zoom);
			fMap.setCenterPosition(px);

			// check zoom level
			if (++zoom >= maximumZoomLevel) {
				break;
			}
			fMap.setZoom(zoom);

			positionRect = getPositionRect(positions, zoom);
			viewport = fMap.getViewport();
		}

		// zoom in until the tour is larger than the viewport
		while (positionRect.getWidth() < viewport.width && positionRect.getHeight() < viewport.height) {

			// center position in the map
			final Point2D center = new Point2D.Double(positionRect.getX() + positionRect.getWidth() / 2,
					positionRect.getY() + positionRect.getHeight() / 2);
			final GeoPosition px = tileFactory.pixelToGeo(center, zoom);
			fMap.setCenterPosition(px);

			// check zoom level
			if (++zoom >= maximumZoomLevel) {
				break;
			}
			fMap.setZoom(zoom);

			positionRect = getPositionRect(positions, zoom);
			viewport = fMap.getViewport();
		}

		// the algorithm generated a larger zoom level as necessary
		zoom--;

		int adjustedZoomLevel = 0;
		if (isAdjustZoomLevel) {
			adjustedZoomLevel = PaintManager.getInstance().getSynchTourZoomLevel();
		}

		fMap.setZoom(zoom + adjustedZoomLevel);
	}

	private void showDefaultMap() {

		// disable tour actions in this view
		fIsTour = false;

		// disable tour data
		fTourData = null;
		fPreviousTourData = null;

		// update direct painter to draw nothing
		fDirectMappingPainter.setPaintContext(fMap, false, null, 0, 0, false, false);

		fMap.setShowOverlays(false);
		fMap.setShowLegend(false);

		fMap.queueRedrawMap();
	}

	/**
	 * Update the min/max values in the {@link ILegendProvider} for the currently displayed legend
	 * 
	 * @param legendProvider
	 * @param legendBounds
	 * @return Return <code>true</code> when the legend value could be updated, <code>false</code>
	 *         when data are not available
	 */
	private boolean updateLegendValues(final ILegendProvider legendProvider, final Rectangle legendBounds) {

		if (fTourData == null) {
			return false;
		}

		final GraphColorProvider colorProvider = GraphColorProvider.getInstance();

		ColorDefinition colorDefinition = null;
		final LegendConfig legendConfig = legendProvider.getLegendConfig();

		// tell the legend provider how to draw the legend
		switch (legendProvider.getTourColorId()) {

		case TOUR_COLOR_ALTITUDE:

			final int[] altitudeSerie = fTourData.getAltitudeSerie();
			if (altitudeSerie == null) {
				return false;
			}

			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_ALTITUDE);

			legendProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
			legendProvider.setLegendColorValues(legendBounds, altitudeSerie, UI.UNIT_LABEL_ALTITUDE);

			break;

		case TOUR_COLOR_PULSE:

			final int[] pulseSerie = fTourData.pulseSerie;
			if (pulseSerie == null) {
				return false;
			}

			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_HEARTBEAT);

			legendProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
			legendProvider.setLegendColorValues(legendBounds, pulseSerie, Messages.graph_label_heartbeat_unit);

			break;

		case TOUR_COLOR_SPEED:

			final int[] speedSerie = fTourData.getSpeedSerie();
			if (speedSerie == null) {
				return false;
			}

			legendConfig.unitFactor = 10;
			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_SPEED);

			legendProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
			legendProvider.setLegendColorValues(legendBounds, speedSerie, UI.UNIT_LABEL_SPEED);

			break;

		case TOUR_COLOR_PACE:

			final int[] paceSerie = fTourData.getPaceSerie();
			if (paceSerie == null) {
				return false;
			}

			legendConfig.unitFactor = 10;
			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_PACE);

			legendProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
			legendProvider.setLegendColorValues(legendBounds, paceSerie, UI.UNIT_LABEL_PACE);

			break;

		case TOUR_COLOR_GRADIENT:

			final int[] gradientSerie = fTourData.getGradientSerie();
			if (gradientSerie == null) {
				return false;
			}

			legendConfig.unitFactor = 10;
			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_GRADIENT);

			legendProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
			legendProvider.setLegendColorValues(legendBounds, gradientSerie, Messages.graph_label_gradient_unit);

			break;

		default:
			break;
		}

		return true;
	}
}
