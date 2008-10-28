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
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourPropertyListener;
import net.tourbook.tour.SelectionActiveEditor;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEditor;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourProperties;
import net.tourbook.ui.UI;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICatalogReferenceTour;
import net.tourbook.ui.views.tourCatalog.TVICompareResultComparedTour;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
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
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
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

	private static final int						DEFAULT_LEGEND_WIDTH				= 150;

	private static final int						DEFAULT_LEGEND_HEIGHT				= 300;

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
	private static final String						MEMENTO_SHOW_TOUR_IN_MAP			= "action.show-tour-in-map";				//$NON-NLS-1$
	private static final String						MEMENTO_SYNCH_WITH_SELECTED_TOUR	= "action.synch-with-selected-tour";		//$NON-NLS-1$
	private static final String						MEMENTO_SYNCH_WITH_TOURCHART_SLIDER	= "action.synch-with-tourchart-slider";	//$NON-NLS-1$
	private static final String						MEMENTO_ZOOM_CENTERED				= "action.zoom-centered";					//$NON-NLS-1$
	private static final String						MEMENTO_MAP_DIM_LEVEL				= "action.map-dim-level";					//$NON-NLS-1$

	private static final String						MEMENTO_SYNCH_TOUR_ZOOM_LEVEL		= "synch-tour-zoom-level";					//$NON-NLS-1$
	private static final String						MEMENTO_CURRENT_FACTORY_ID			= "current.factory-id";					//$NON-NLS-1$

	private static final String						MEMENTO_DEFAULT_POSITION_ZOOM		= "default.position.zoom-level";			//$NON-NLS-1$
	private static final String						MEMENTO_DEFAULT_POSITION_LATITUDE	= "default.position.latitude";				//$NON-NLS-1$
	private static final String						MEMENTO_DEFAULT_POSITION_LONGITUDE	= "default.position.longitude";			//$NON-NLS-1$

	private static final String						MEMENTO_TOUR_COLOR_ID				= "tour-color-id";							//$NON-NLS-1$

	final static String								PREF_SHOW_TILE_INFO					= "map.debug.show.tile-info";				//$NON-NLS-1$
	final static String								PREF_DEBUG_MAP_DIM_LEVEL			= "map.debug.dim-map";						//$NON-NLS-1$

	public static final int							LEGEND_MARGIN_TOP_BOTTOM			= 10;
	public static final int							LEGEND_UNIT_DISTANCE				= 60;

	private Map										fMap;

	private ISelectionListener						fPostSelectionListener;
	private IPropertyChangeListener					fPrefChangeListener;
	private IPropertyChangeListener					fTourbookPrefChangeListener;
	private IPartListener2							fPartListener;
	private ITourPropertyListener					fTourPropertyListener;

	/**
	 * contains the tours which are displayed in the map
	 */
	private ArrayList<TourData>						fTourDataList;
	private TourData								fPreviousTourData;

	private ActionDimMap							fActionDimMap;
	private ActionReloadFailedMapImages				fActionReloadFailedMapImages;
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

	/**
	 * when <code>true</code> a tour is painted, <code>false</code> a point of interrest is painted
	 */
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

	private long									fPreviousOverlayKey;

	private int										fMapDimLevel						= -1;
	private RGB										fMapDimColor;

	public MappingView() {}

	void actionDimMap(final int dimLevel) {

		// check if the dim level/color was changed 
		if (fMapDimLevel != dimLevel) {

			fMapDimLevel = dimLevel;

			/*
			 * dim color is stored in the pref store and not in the memento
			 */
			final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();
			final RGB dimColor = PreferenceConverter.getColor(store, ITourbookPreferences.MAP_LAYOUT_DIM_COLOR);

			fMap.dimMap(dimLevel, dimColor);
		}
	}

	private void actionDimMap(final RGB dimColor) {

		if (fMapDimColor != dimColor) {

			fMapDimColor = dimColor;

			fMap.dimMap(fMapDimLevel, dimColor);
		}
	}

	void actionOpenMapProviderDialog() {

		final ModifyMapProviderDialog dialog = new ModifyMapProviderDialog(Display.getCurrent().getActiveShell(), this);

		if (dialog.open() == Window.OK) {
			fActionSelectMapProvider.updateMapProviders();
		}
	}

	void actionReloadFailedMapImages() {
		fMap.reload();
	}

	void actionResetTileOverlays() {
		fMap.resetOverlays();
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

		fMap.disposeOverlayImageCache();
		fMap.queueRedrawMap();
	}

	void actionSetShowTourInMap() {
		paintAllTours();
	}

	void actionSetShowTourMarkerInMap() {

		PaintManager.getInstance().setShowTourMarker(fActionShowTourMarker.isChecked());

		fMap.disposeOverlayImageCache();
		fMap.queueRedrawMap();
	}

	void actionSetTourColor(final int colorId) {
		PaintManager.getInstance().setLegendProvider(getLegendProvider(colorId));
		fMap.disposeOverlayImageCache();
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
				fTourDataList.get(0),
				fCurrentLeftSliderValueIndex,
				fCurrentRightSliderValueIndex,
				fActionShowSliderInMap.isChecked(),
				fActionShowSliderInLegend.isChecked());

		fMap.redraw();
	}

	void actionSynchWithSlider() {

		if (fTourDataList == null) {
			return;
		}

		fIsMapSynchedWithSlider = fActionSynchWithSlider.isChecked();

		if (fIsMapSynchedWithSlider) {

			fActionShowTourInMap.setChecked(true);

			// map must be synched with selected tour
			fActionSynchWithTour.setChecked(true);
			fIsMapSynchedWithTour = true;

			fMap.setShowOverlays(true);

			final TourData firstTourData = fTourDataList.get(0);

			paintOneTour(firstTourData, false, true);
			setMapToSliderBounds(firstTourData);
		}
	}

	void actionSynchWithTour() {

		fIsMapSynchedWithTour = fActionSynchWithTour.isChecked();

		if (fIsMapSynchedWithTour) {

			fActionShowTourInMap.setChecked(true);
			fMap.setShowOverlays(true);

			paintOneTour(fTourDataList.get(0), true, true);

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
				if (partRef.getPart(false) == MappingView.this) {
					saveState();
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {}

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
				final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();

				if (property.equals(PREF_SHOW_TILE_INFO)) {

					// map properties has changed

					final boolean isShowTileInfo = store.getBoolean(PREF_SHOW_TILE_INFO);

					fMap.setDrawTileBorders(isShowTileInfo);
					fMap.queueRedrawMap();

				} else if (property.equals(PREF_DEBUG_MAP_DIM_LEVEL)) {

					float prefDimLevel = store.getInt(MappingView.PREF_DEBUG_MAP_DIM_LEVEL);
					prefDimLevel *= 2.55;
					prefDimLevel -= 255;

					final int dimLevel = (int) Math.abs(prefDimLevel);
					fActionDimMap.setDimLevel(dimLevel);
					actionDimMap(dimLevel);

				} else if (property.equals(ITourbookPreferences.MAP_LAYOUT_DIM_COLOR)) {

					actionDimMap(PreferenceConverter.getColor(store, ITourbookPreferences.MAP_LAYOUT_DIM_COLOR));

				} else if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)) {

					// update tour and legend

					createLegendImage(PaintManager.getInstance().getLegendProvider());

					fMap.disposeOverlayImageCache();
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
				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
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

	private void addTourPropertyListener() {

		fTourPropertyListener = new ITourPropertyListener() {
			public void propertyChanged(final IWorkbenchPart part, final int propertyId, final Object propertyData) {

				if (part == MappingView.this) {
					return;
				}

				if (propertyId == TourManager.TOUR_CHART_PROPERTY_IS_MODIFIED) {

					resetMap();

				} else if (propertyId == TourManager.TOUR_PROPERTIES_CHANGED && propertyData instanceof TourProperties) {

					final ArrayList<TourData> modifiedTours = ((TourProperties) propertyData).getModifiedTours();
					if (modifiedTours != null && modifiedTours.size() > 0) {

						fTourDataList = modifiedTours;
						resetMap();
					}

				} else if (propertyId == TourManager.SLIDER_POSITION_CHANGED) {
					onSelectionChanged((ISelection) propertyData);
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
		fActionReloadFailedMapImages = new ActionReloadFailedMapImages(this);
//		fActionResetTileOverlays = new ActionResetTileOverlays(this);
		fActionDimMap = new ActionDimMap(this);

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
		menuMgr.add(fActionDimMap);
		menuMgr.add(new Separator());
		menuMgr.add(fActionSetDefaultPosition);
		menuMgr.add(fActionSaveDefaultPosition);
		menuMgr.add(new Separator());
		menuMgr.add(fActionSynchTourZoomLevel);
		menuMgr.add(fActionReloadFailedMapImages);
//		menuMgr.add(fActionResetTileOverlays);
	}

	/**
	 * Creates a new legend image and disposes the old image
	 * 
	 * @param legendProvider
	 */
	private void createLegendImage(final ILegendProvider legendProvider) {

		Image legendImage = fMapLegend.getImage();

		// legend requires a tour with coordinates
		if (legendProvider == null /* || isPaintDataValid(fTourData) == false */) {
			showDefaultMap();
			return;
		}

		// dispose old legend image
		if (legendImage != null && !legendImage.isDisposed()) {
			legendImage.dispose();
		}
		final int legendWidth = DEFAULT_LEGEND_WIDTH;
		int legendHeight = DEFAULT_LEGEND_HEIGHT;

		final Rectangle mapBounds = fMap.getBounds();
		legendHeight = Math.max(1, Math.min(legendHeight, mapBounds.height));

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

		fMap.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {

				/*
				 * check if the legend size must be adjusted
				 */
				final Image legendImage = fMapLegend.getImage();
				if (legendImage == null || legendImage.isDisposed()) {
					return;
				}

				final boolean showTour = fActionShowTourInMap.isChecked();
				final boolean showLegend = fActionShowLegendInMap.isChecked();
				if (fIsTour == false || showTour == false || showLegend == false) {
					return;
				}

				/*
				 * check height
				 */
				final Rectangle mapBounds = fMap.getBounds();
				final Rectangle legendBounds = legendImage.getBounds();

				if (mapBounds.height < DEFAULT_LEGEND_HEIGHT //
						|| (mapBounds.height > DEFAULT_LEGEND_HEIGHT //
						&& legendBounds.height < DEFAULT_LEGEND_HEIGHT)) {

					createLegendImage(PaintManager.getInstance().getLegendProvider());
				}
			}
		});

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

		restoreState();

		// show map from last selection
//		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		if (fTourDataList == null) {
			// a tour is not displayed, find a tour provider which provides a tour
			showToursFromTourProvider();
		} else {
			fMap.queueRedrawMap();
		}

		if (fMapDimLevel < 30) {
			showDimWarning();
		}
	}

	@Override
	public void dispose() {

		// dispose tilefactory resources
		for (final TileFactory tileFactory : fTileFactories) {
			tileFactory.dispose();
		}

		fMap.disposeOverlayImageCache();

		getViewSite().getPage().removePostSelectionListener(fPostSelectionListener);
		getViewSite().getPage().removePartListener(fPartListener);

		TourManager.getInstance().removePropertyListener(fTourPropertyListener);

		final TourbookPlugin tourbookPlugin = TourbookPlugin.getDefault();
		tourbookPlugin.getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);
		tourbookPlugin.getPluginPreferences().removePropertyChangeListener(fTourbookPrefChangeListener);

		super.dispose();
	}

	private void enableActions(final boolean isForceTourColor) {

		// update legend action
		if (fIsTour) {

			final boolean isLegendVisible = fActionShowLegendInMap.isChecked();

			fMap.setShowLegend(isLegendVisible);

			fActionShowSliderInLegend.setEnabled(isLegendVisible);
			if (isLegendVisible == false) {
				fActionShowSliderInLegend.setChecked(false);
			}
		}

		final boolean isMultipleTours = fTourDataList != null && fTourDataList.size() > 1;
		final boolean isOneTour = fIsTour && isMultipleTours == false;

		/*
		 * enable/disable tour actions
		 */
		fActionZoomShowEntireTour.setEnabled(isOneTour);
		fActionSynchTourZoomLevel.setEnabled(isOneTour);
		fActionShowTourInMap.setEnabled(fIsTour);
		fActionSynchWithTour.setEnabled(isOneTour);
		fActionSynchWithSlider.setEnabled(isOneTour);

		fActionShowStartEndInMap.setEnabled(isOneTour);
		fActionShowTourMarker.setEnabled(isOneTour);
		fActionShowLegendInMap.setEnabled(fIsTour);
		fActionShowSliderInMap.setEnabled(isOneTour);
		fActionShowSliderInLegend.setEnabled(isOneTour);

		if (fTourDataList == null) {

			fActionTourColorAltitude.setEnabled(false);
			fActionTourColorGradient.setEnabled(false);
			fActionTourColorPulse.setEnabled(false);
			fActionTourColorSpeed.setEnabled(false);
			fActionTourColorPace.setEnabled(false);

		} else if (isForceTourColor) {

			fActionTourColorAltitude.setEnabled(true);
			fActionTourColorGradient.setEnabled(true);
			fActionTourColorPulse.setEnabled(true);
			fActionTourColorSpeed.setEnabled(true);
			fActionTourColorPace.setEnabled(true);

		} else if (isOneTour) {

			final TourData oneTourData = fTourDataList.get(0);
			fActionTourColorAltitude.setEnabled(true);
			fActionTourColorGradient.setEnabled(oneTourData.getGradientSerie() != null);
			fActionTourColorPulse.setEnabled(oneTourData.pulseSerie != null);
			fActionTourColorSpeed.setEnabled(oneTourData.getSpeedSerie() != null);
			fActionTourColorPace.setEnabled(oneTourData.getPaceSerie() != null);

		} else {

			fActionTourColorAltitude.setEnabled(false);
			fActionTourColorGradient.setEnabled(false);
			fActionTourColorPulse.setEnabled(false);
			fActionTourColorSpeed.setEnabled(false);
			fActionTourColorPace.setEnabled(false);
		}
	}

	/**
	 * Converts the tour id's into {@link TourData}
	 * 
	 * @param tourIdList
	 * @return unique overlay key
	 */
	private long fillTourDataList(final ArrayList<Long> tourIdList) {

		// create a unique overlay key for the selected tours
		long overlayKey = 0;

		// get tour data for each tour id
		for (final Long tourId : tourIdList) {

			final TourData tourData = TourManager.getInstance().getTourData(tourId);
			if (isPaintDataValid(tourData)) {
				fTourDataList.add(tourData);
				overlayKey += tourData.getTourId();
			}
		}

		return overlayKey;
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

	public int getMapDimLevel() {
		return fMapDimLevel;
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

		if (latitudeSerie == null || longitudeSerie == null) {
			return null;
		}

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

//	@Override
//	public void init(final IViewSite site, final IMemento memento) throws PartInitException {
//		super.init(site, memento);
//	}

	/**
	 * Checks if {@link TourData} can be painted
	 * 
	 * @param tourData
	 * @return <code>true</code> when {@link TourData} contains a tour which can be painted in the
	 *         map
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

	private void onSelectionChanged(final ISelection selection) {

		if (selection instanceof SelectionTourData) {

			final SelectionTourData selectionTourData = (SelectionTourData) selection;
			final TourData tourData = selectionTourData.getTourData();

			paintOneTour(tourData, selectionTourData.isForceRedraw(), true);

			enableActions(false);

		} else if (selection instanceof SelectionTourId) {

			final SelectionTourId tourIdSelection = (SelectionTourId) selection;
			final TourData tourData = TourManager.getInstance().getTourData(tourIdSelection.getTourId());

			paintOneTour(tourData, false, true);

			enableActions(false);

		} else if (selection instanceof SelectionTourIds) {

			// paint all selected tours

			final ArrayList<Long> tourIds = ((SelectionTourIds) selection).getTourIds();
			if (tourIds.size() == 0) {
				return;
			}

			paintTours(tourIds);

			enableActions(true);

		} else if (selection instanceof SelectionActiveEditor) {

			final IEditorPart editor = ((SelectionActiveEditor) selection).getEditor();
			if (editor instanceof TourEditor) {

				final TourEditor fTourEditor = (TourEditor) editor;
				final TourChart fTourChart = fTourEditor.getTourChart();
				final TourData tourData = fTourChart.getTourData();

				paintOneTour(tourData, false, true);

				final SelectionChartInfo chartInfo = fTourChart.getChartInfo();
				paintTourSliders(tourData,
						chartInfo.leftSliderValuesIndex,
						chartInfo.rightSliderValuesIndex,
						chartInfo.selectedSliderValuesIndex);

				enableActions(false);
			}

		} else if (selection instanceof SelectionChartInfo) {

			final ChartDataModel chartDataModel = ((SelectionChartInfo) selection).chartDataModel;
			if (chartDataModel != null) {

				final TourData tourData = (TourData) chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_DATA);
				if (tourData != null) {

					final SelectionChartInfo chartInfo = (SelectionChartInfo) selection;

					paintTourSliders(tourData,
							chartInfo.leftSliderValuesIndex,
							chartInfo.rightSliderValuesIndex,
							chartInfo.selectedSliderValuesIndex);

					enableActions(false);
				}
			}

		} else if (selection instanceof SelectionChartXSliderPosition) {

			final SelectionChartXSliderPosition xSliderPos = (SelectionChartXSliderPosition) selection;

			final ChartDataModel chartDataModel = xSliderPos.getChart().getChartDataModel();
			final TourData tourData = (TourData) chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_DATA);
			if (tourData != null) {

				final int leftSliderValueIndex = xSliderPos.getSlider1ValueIndex();
				int rightSliderValueIndex = xSliderPos.getSlider2ValueIndex();

				rightSliderValueIndex = rightSliderValueIndex == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION
						? leftSliderValueIndex
						: rightSliderValueIndex;

				paintTourSliders(tourData, leftSliderValueIndex, rightSliderValueIndex, leftSliderValueIndex);

				enableActions(false);
			}

		} else if (selection instanceof PointOfInterest) {

			fIsTour = false;

			// disable tour data
			fTourDataList = null;
			fPreviousTourData = null;
			PaintManager.getInstance().setTourData(new ArrayList<TourData>());

			final PointOfInterest poi = (PointOfInterest) selection;
			fPOIPosition = poi.getPosition();

			fMap.setZoom(poi.getRecommendedZoom());
			fMap.setCenterPosition(fPOIPosition);
			fMap.queueRedrawMap();

			enableActions(false);

		} else if (selection instanceof StructuredSelection) {

			final Object firstElement = ((StructuredSelection) selection).getFirstElement();

			if (firstElement instanceof TVICatalogComparedTour) {

				final TVICatalogComparedTour comparedTour = (TVICatalogComparedTour) firstElement;
				final long tourId = comparedTour.getTourId();

				final TourData tourData = TourManager.getInstance().getTourData(tourId);
				paintOneTour(tourData, false, true);

			} else if (firstElement instanceof TVICompareResultComparedTour) {

				final TVICompareResultComparedTour compareResultItem = (TVICompareResultComparedTour) firstElement;
				final TourData tourData = TourManager.getInstance().getTourData(compareResultItem.getComparedTourData()
						.getTourId());
				paintOneTour(tourData, false, true);
			}

			enableActions(false);

		} else if (selection instanceof SelectionTourCatalogView) {

			// show reference tour

			final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;

			final TVICatalogReferenceTour refItem = tourCatalogSelection.getRefItem();
			if (refItem != null) {

				final TourData tourData = TourManager.getInstance().getTourData(refItem.getTourId());
				paintOneTour(tourData, false, true);

				enableActions(false);
			}
		}

	}

	private void paintAllTours() {

		if (fTourDataList == null) {
			return;
		}

		// show/hide legend
		fMap.setShowLegend(fActionShowTourInMap.isChecked());

		if (fTourDataList.size() > 1) {
			// multiple tours are displayed
			paintTours();

			enableActions(true);
		} else {
			paintOneTour(fTourDataList.get(0), true, false);
			enableActions(false);
		}
	}

	private void paintEntireTour() {

		if (fTourDataList == null || fTourDataList.size() == 0 || isPaintDataValid(fTourDataList.get(0)) == false) {
			showDefaultMap();
			return;
		}

		final PaintManager paintManager = PaintManager.getInstance();
		paintManager.setTourData(fTourDataList);
		final TourData firstTourData = fTourDataList.get(0);

		// set slider position
		fDirectMappingPainter.setPaintContext(fMap,
				fActionShowTourInMap.isChecked(),
				firstTourData,
				fCurrentLeftSliderValueIndex,
				fCurrentRightSliderValueIndex,
				fActionShowSliderInMap.isChecked(),
				fActionShowSliderInLegend.isChecked());

		final Set<GeoPosition> tourBounds = getTourBounds(firstTourData);
		paintManager.setTourBounds(tourBounds);

		fMap.setShowOverlays(fActionShowTourInMap.isChecked());

		setTourZoomLevel(tourBounds, false);

		fMap.queueRedrawMap();
	}

//	/**
//	 * Paint tours with already defined tour data in <code>fTourDataList</code>
//	 */
//	private void paintMultipleTours() {
//
//		fIsTour = true;
//
//		// force single tour to be repainted
//		fPreviousTourData = null;
//
//		final boolean isShowTour = fActionShowTourInMap.isChecked();
//		fMap.setShowOverlays(isShowTour);
//		fMap.setShowLegend(isShowTour && fActionShowLegendInMap.isChecked());
//
//		fMap.queueRedrawMap();
//	}

	/**
	 * Paint the currently selected tour in the map
	 * 
	 * @param tourData
	 * @param forceRedraw
	 * @param isSynchronized
	 *            when <code>true</code>, map will be synchronized
	 */
	private void paintOneTour(final TourData tourData, final boolean forceRedraw, final boolean isSynchronized) {

		if (isPaintDataValid(tourData) == false) {
			showDefaultMap();
			return;
		}

		fIsTour = true;
		final boolean isShowTour = fActionShowTourInMap.isChecked();

		// prevent loading the same tour
		if (forceRedraw == false) {

			if (fTourDataList != null && fTourDataList.size() == 1 && fTourDataList.get(0) == tourData) {
				return;
			}
		}

		// force multiple tours to be repainted
		fPreviousOverlayKey = -1;

		// check if this is a new tour
		boolean isNewTour = true;
		if (fPreviousTourData != null && fPreviousTourData.getTourId().longValue() == tourData.getTourId().longValue()) {
			isNewTour = false;
		}

		final PaintManager paintManager = PaintManager.getInstance();

		paintManager.setTourData(tourData);

		/*
		 * set tour into tour data list, this is currently used to draw the legend, it's also used
		 * to figure out if multiple tours are selected
		 */
		fTourDataList = new ArrayList<TourData>();
		fTourDataList.add(tourData);

		// set the paint context (slider position) for the direct mapping painter
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
		if (fIsMapSynchedWithTour && isSynchronized) {

			if (forceRedraw == false && fPreviousTourData != null || tourData == fPreviousTourData) {

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

				// position tour to the previous position
				fMap.setZoom(tourData.mapZoomLevel);
				fMap.setCenterPosition(new GeoPosition(tourData.mapCenterPositionLatitude,
						tourData.mapCenterPositionLongitude));
			}
		}

		// keep tour data
		fPreviousTourData = tourData;

		if (isNewTour || forceRedraw) {

			// adjust legend values for the new or changed tour
			createLegendImage(PaintManager.getInstance().getLegendProvider());

			fMap.setOverlayKey(tourData.getTourId().toString());
			fMap.resetOverlays();
//			fMap.resetOverlayImageCache();
		}

		fMap.queueRedrawMap();
	}

	/**
	 * paints the tours which are set in {@link #fTourDataList}
	 */
	private void paintTours() {

		fIsTour = true;

		// force single tour to be repainted
		fPreviousTourData = null;

		PaintManager.getInstance().setTourData(fTourDataList);

		fDirectMappingPainter.disablePaintContext();

		final boolean isShowTour = fActionShowTourInMap.isChecked();
		fMap.setShowOverlays(isShowTour);
		fMap.setShowLegend(isShowTour && fActionShowLegendInMap.isChecked());

		// get overlay key for all tours which have valid tour data
		long newOverlayKey = -1;
		for (final TourData tourData : fTourDataList) {

			if (isPaintDataValid(tourData)) {
				newOverlayKey += tourData.getTourId();
			}
		}

		if (fPreviousOverlayKey != newOverlayKey) {

			fPreviousOverlayKey = newOverlayKey;

			fMap.setOverlayKey(Long.toString(newOverlayKey));
			fMap.resetOverlays();
		}

		createLegendImage(PaintManager.getInstance().getLegendProvider());

		fMap.queueRedrawMap();
	}

	private void paintTours(final ArrayList<Long> tourIdList) {

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {

				fIsTour = true;

				// force single tour to be repainted
				fPreviousTourData = null;

				fTourDataList = new ArrayList<TourData>();

				PaintManager.getInstance().setTourData(fTourDataList);

				fDirectMappingPainter.disablePaintContext();

				final boolean isShowTour = fActionShowTourInMap.isChecked();
				fMap.setShowOverlays(isShowTour);
				fMap.setShowLegend(isShowTour && fActionShowLegendInMap.isChecked());

				final long newOverlayKey = fillTourDataList(tourIdList);

				if (fPreviousOverlayKey != newOverlayKey) {

					fPreviousOverlayKey = newOverlayKey;

					fMap.setOverlayKey(Long.toString(newOverlayKey));
					fMap.resetOverlays();
				}

				createLegendImage(PaintManager.getInstance().getLegendProvider());
			}
		});

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

		if (fTourDataList == null) {
			return;
		}

		fMap.disposeOverlayImageCache();

		paintAllTours();

		fMap.queueRedrawMap();
	}

	private void restoreState() {

		final PaintManager paintManager = PaintManager.getInstance();
		final IDialogSettings settings = TourbookPlugin.getDefault().getDialogSettingsSection(ID);

		try {
			final boolean isTourCentered = settings.getBoolean(MEMENTO_ZOOM_CENTERED);

			fActionZoomCentered.setChecked(isTourCentered);
			fIsPositionCentered = isTourCentered;
		} catch (final NumberFormatException e) {}

		try {
			final boolean isSynchTour = settings.getBoolean(MEMENTO_SYNCH_WITH_SELECTED_TOUR);

			fActionSynchWithTour.setChecked(isSynchTour);
			fIsMapSynchedWithTour = isSynchTour;
		} catch (final NumberFormatException e) {}

		try {
			final boolean isSynchSlider = settings.getBoolean(MEMENTO_SYNCH_WITH_TOURCHART_SLIDER);

			fActionSynchWithSlider.setChecked(isSynchSlider);
			fIsMapSynchedWithSlider = isSynchSlider;
		} catch (final NumberFormatException e) {}

		try {
			final boolean isShowTour = settings.getBoolean(MEMENTO_SHOW_TOUR_IN_MAP);

			fActionShowTourInMap.setChecked(isShowTour);
			fMap.setShowOverlays(isShowTour);
			fMap.setShowLegend(isShowTour);
		} catch (final NumberFormatException e) {}

		try {
			fActionSynchTourZoomLevel.setZoomLevel(settings.getInt(MEMENTO_SYNCH_TOUR_ZOOM_LEVEL));
		} catch (final NumberFormatException e) {}

		try {
			fMapDimLevel = settings.getInt(MEMENTO_MAP_DIM_LEVEL);
		} catch (final NumberFormatException e) {}

		// action: show start/end in map
		try {
			fActionShowStartEndInMap.setChecked(settings.getBoolean(MEMENTO_SHOW_START_END_IN_MAP));
		} catch (final NumberFormatException e) {}
		paintManager.setShowStartEnd(fActionShowStartEndInMap.isChecked());

		// action: show tour marker
		try {
			fActionShowTourMarker.setChecked(settings.getBoolean(MEMENTO_SHOW_TOUR_MARKER));
		} catch (final NumberFormatException e) {}
		paintManager.setShowTourMarker(fActionShowTourMarker.isChecked());

		// action: show legend in map
		try {
			fActionShowLegendInMap.setChecked(settings.getBoolean(MEMENTO_SHOW_LEGEND_IN_MAP));
		} catch (final NumberFormatException e) {}

		// action: show slider in map
		try {
			fActionShowSliderInMap.setChecked(settings.getBoolean(MEMENTO_SHOW_SLIDER_IN_MAP));
		} catch (final NumberFormatException e) {}

		// action: show slider in legend
		try {
			fActionShowSliderInLegend.setChecked(settings.getBoolean(MEMENTO_SHOW_SLIDER_IN_LEGEND));
		} catch (final NumberFormatException e) {}

		// restore map factory by selecting the last used map factory
		fActionSelectMapProvider.setSelectedFactory(settings.get(MEMENTO_CURRENT_FACTORY_ID));

		// restore: default position
		try {
			fDefaultZoom = settings.getInt(MEMENTO_DEFAULT_POSITION_ZOOM);
		} catch (final NumberFormatException e) {}

		try {
			fDefaultPosition = new GeoPosition(settings.getFloat(MEMENTO_DEFAULT_POSITION_LATITUDE),
					settings.getFloat(MEMENTO_DEFAULT_POSITION_LONGITUDE));
		} catch (final NumberFormatException e) {
			fDefaultPosition = new GeoPosition(0, 0);
		}

		// tour color
		try {
			final Integer colorId = settings.getInt(MEMENTO_TOUR_COLOR_ID);

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

		} catch (final NumberFormatException e) {
			fActionTourColorAltitude.setChecked(true);
		}

		// draw tour with default color

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
		final boolean isShowTileInfo = store.getBoolean(MappingView.PREF_SHOW_TILE_INFO);
		fMap.setDrawTileBorders(isShowTileInfo);

		// set dim level/color after the map providers are set
		if (fMapDimLevel == -1) {
			fMapDimLevel = 127;
		}
		final RGB dimColor = PreferenceConverter.getColor(store, ITourbookPreferences.MAP_LAYOUT_DIM_COLOR);
		fMap.setDimLevel(fMapDimLevel, dimColor);
		fMapDimLevel = fActionDimMap.setDimLevel(fMapDimLevel);

		// display the map with the default position
		actionSetDefaultPosition();
	}

	private void saveState() {

		final IDialogSettings settings = TourbookPlugin.getDefault().getDialogSettingsSection(ID);

		// save checked actions
		settings.put(MEMENTO_ZOOM_CENTERED, fActionZoomCentered.isChecked());
		settings.put(MEMENTO_SHOW_TOUR_IN_MAP, fActionShowTourInMap.isChecked());
		settings.put(MEMENTO_SYNCH_WITH_SELECTED_TOUR, fActionSynchWithTour.isChecked());
		settings.put(MEMENTO_SYNCH_WITH_TOURCHART_SLIDER, fActionSynchWithSlider.isChecked());
		settings.put(MEMENTO_SYNCH_TOUR_ZOOM_LEVEL, fActionSynchTourZoomLevel.getZoomLevel());

		settings.put(MEMENTO_MAP_DIM_LEVEL, fMapDimLevel);

		settings.put(MEMENTO_SHOW_START_END_IN_MAP, fActionShowStartEndInMap.isChecked());
		settings.put(MEMENTO_SHOW_TOUR_MARKER, fActionShowTourMarker.isChecked());
		settings.put(MEMENTO_SHOW_LEGEND_IN_MAP, fActionShowLegendInMap.isChecked());
		settings.put(MEMENTO_SHOW_SLIDER_IN_MAP, fActionShowSliderInMap.isChecked());
		settings.put(MEMENTO_SHOW_SLIDER_IN_LEGEND, fActionShowSliderInLegend.isChecked());

		settings.put(MEMENTO_CURRENT_FACTORY_ID, fActionSelectMapProvider.getSelectedFactory().getInfo().getFactoryID());

		if (fDefaultPosition == null) {
			settings.put(MEMENTO_DEFAULT_POSITION_ZOOM, fMap.getTileFactory().getInfo().getMinimumZoomLevel());
			settings.put(MEMENTO_DEFAULT_POSITION_LATITUDE, 0.0F);
			settings.put(MEMENTO_DEFAULT_POSITION_LONGITUDE, 0.0F);
		} else {
			settings.put(MEMENTO_DEFAULT_POSITION_ZOOM, fDefaultZoom);
			settings.put(MEMENTO_DEFAULT_POSITION_LATITUDE, (float) fDefaultPosition.getLatitude());
			settings.put(MEMENTO_DEFAULT_POSITION_LONGITUDE, (float) fDefaultPosition.getLongitude());
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
		settings.put(MEMENTO_TOUR_COLOR_ID, colorId);
	}

	@Override
	public void setFocus() {
		fMap.setFocus();
	}

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

		final int sliderIndex = Math.max(0, Math.min(fCurrentSelectedSliderValueIndex, latitudeSerie.length - 1));

		fMap.setCenterPosition(new GeoPosition(latitudeSerie[sliderIndex], longitudeSerie[sliderIndex]));

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

		if (positions == null || positions.size() < 2) {
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
		fTourDataList = null;
		fPreviousTourData = null;

		// update direct painter to draw nothing
		fDirectMappingPainter.setPaintContext(fMap, false, null, 0, 0, false, false);

		fMap.setShowOverlays(false);
		fMap.setShowLegend(false);

		fMap.queueRedrawMap();
	}

	/**
	 * show warning that map is dimmed and can be invisible
	 */
	private void showDimWarning() {

		final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();
		if (store.getBoolean(ITourbookPreferences.MAP_VIEW_CONFIRMATION_SHOW_DIM_WARNING) == false) {

			Display.getCurrent().asyncExec(new Runnable() {
				public void run() {

					final MessageDialogWithToggle dialog = MessageDialogWithToggle.openInformation(Display.getCurrent()
							.getActiveShell(),//
							Messages.map_dlg_dim_warning_title, // title
							Messages.map_dlg_dim_warning_message, // message
							Messages.map_dlg_dim_warning_toggle_message, // toggle message
							false, // toggle default state
							null,
							null);

					store.setValue(ITourbookPreferences.MAP_VIEW_CONFIRMATION_SHOW_DIM_WARNING, dialog.getToggleState());
				}
			});
		}
	}

	private void showToursFromTourProvider() {

		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {

				// validate widget
				if (fMap.isDisposed()) {
					return;
				}

				/*
				 * check if tour is set from a selection provider
				 */
				if (fTourDataList != null) {
					return;
				}

				final ArrayList<TourData> tourDataList = TourManager.getSelectedTours();
				if (tourDataList != null) {

//					final TourData tourData = TourManager.getInstance().getTourData(tourDataList);
//					if (tourData != null) {

					fTourDataList = tourDataList;

					paintAllTours();

//						/*
//						 * set position and zoomlevel to show the entire tour
//						 */
//						final PaintManager paintManager = PaintManager.getInstance();
//						final Set<GeoPosition> tourBounds = getTourBounds(tourData);
//
//						paintManager.setTourBounds(tourBounds);
//						setTourZoomLevel(tourBounds, true);
//
//						paintOneTour(tourData, true, false);
//
//						enableActions(false);
//					}
				}
			}
		});
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

		if (fTourDataList == null || fTourDataList.size() == 0) {
			return false;
		}

		final GraphColorProvider colorProvider = GraphColorProvider.getInstance();

		ColorDefinition colorDefinition = null;
		final LegendConfig legendConfig = legendProvider.getLegendConfig();

		// tell the legend provider how to draw the legend
		switch (legendProvider.getTourColorId()) {

		case TOUR_COLOR_ALTITUDE:

			int minValue = Integer.MIN_VALUE;
			int maxValue = Integer.MAX_VALUE;
			boolean setInitialValue = true;

			for (final TourData tourData : fTourDataList) {

				final int[] dataSerie = tourData.getAltitudeSerie();
				if (dataSerie == null || dataSerie.length == 0) {
					continue;
				}

				/*
				 * get min/max values
				 */
				for (int valueIndex = 0; valueIndex < dataSerie.length; valueIndex++) {

					if (setInitialValue) {
						setInitialValue = false;
						minValue = maxValue = dataSerie[valueIndex];
					}

					final int dataValue = dataSerie[valueIndex];
					minValue = (minValue <= dataValue) ? minValue : dataValue;
					maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
				}
			}

			if (minValue == Integer.MIN_VALUE || maxValue == Integer.MAX_VALUE) {
				return false;
			}

			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_ALTITUDE);

			legendProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
			legendProvider.setLegendColorValues(legendBounds, minValue, maxValue, UI.UNIT_LABEL_ALTITUDE);

			break;

		case TOUR_COLOR_PULSE:

			minValue = Integer.MIN_VALUE;
			maxValue = Integer.MAX_VALUE;
			setInitialValue = true;

			for (final TourData tourData : fTourDataList) {

				final int[] dataSerie = tourData.pulseSerie;
				if (dataSerie == null || dataSerie.length == 0) {
					continue;
				}

				/*
				 * get min/max values
				 */
				for (int valueIndex = 0; valueIndex < dataSerie.length; valueIndex++) {

					if (setInitialValue) {
						setInitialValue = false;
						minValue = maxValue = dataSerie[valueIndex];
					}

					final int dataValue = dataSerie[valueIndex];
					minValue = (minValue <= dataValue) ? minValue : dataValue;
					maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
				}
			}

			if (minValue == Integer.MIN_VALUE || maxValue == Integer.MAX_VALUE) {
				return false;
			}

			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_HEARTBEAT);

			legendProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
			legendProvider.setLegendColorValues(legendBounds, minValue, maxValue, Messages.graph_label_heartbeat_unit);

			break;

		case TOUR_COLOR_SPEED:

			minValue = Integer.MIN_VALUE;
			maxValue = Integer.MAX_VALUE;
			setInitialValue = true;

			for (final TourData tourData : fTourDataList) {

				final int[] dataSerie = tourData.getSpeedSerie();
				if (dataSerie == null || dataSerie.length == 0) {
					continue;
				}

				/*
				 * get min/max values
				 */
				for (int valueIndex = 0; valueIndex < dataSerie.length; valueIndex++) {

					if (setInitialValue) {
						setInitialValue = false;
						minValue = maxValue = dataSerie[valueIndex];
					}

					final int dataValue = dataSerie[valueIndex];
					minValue = (minValue <= dataValue) ? minValue : dataValue;
					maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
				}
			}

			if (minValue == Integer.MIN_VALUE || maxValue == Integer.MAX_VALUE) {
				return false;
			}

			legendConfig.unitFactor = 10;
			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_SPEED);

			legendProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
			legendProvider.setLegendColorValues(legendBounds, minValue, maxValue, UI.UNIT_LABEL_SPEED);

			break;

		case TOUR_COLOR_PACE:

			minValue = Integer.MIN_VALUE;
			maxValue = Integer.MAX_VALUE;
			setInitialValue = true;

			for (final TourData tourData : fTourDataList) {

				final int[] dataSerie = tourData.getPaceSerie();
				if (dataSerie == null || dataSerie.length == 0) {
					continue;
				}

				/*
				 * get min/max values
				 */
				for (int valueIndex = 0; valueIndex < dataSerie.length; valueIndex++) {

					if (setInitialValue) {
						setInitialValue = false;
						minValue = maxValue = dataSerie[valueIndex];
					}

					final int dataValue = dataSerie[valueIndex];
					minValue = (minValue <= dataValue) ? minValue : dataValue;
					maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
				}
			}

			if (minValue == Integer.MIN_VALUE || maxValue == Integer.MAX_VALUE) {
				return false;
			}

			legendConfig.unitFactor = 10;
			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_PACE);

			legendProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
			legendProvider.setLegendColorValues(legendBounds, minValue, maxValue, UI.UNIT_LABEL_PACE);

			break;

		case TOUR_COLOR_GRADIENT:

			minValue = Integer.MIN_VALUE;
			maxValue = Integer.MAX_VALUE;
			setInitialValue = true;

			for (final TourData tourData : fTourDataList) {

				final int[] dataSerie = tourData.getGradientSerie();
				if (dataSerie == null || dataSerie.length == 0) {
					continue;
				}

				/*
				 * get min/max values
				 */
				for (int valueIndex = 0; valueIndex < dataSerie.length; valueIndex++) {

					if (setInitialValue) {
						setInitialValue = false;
						minValue = maxValue = dataSerie[valueIndex];
					}

					final int dataValue = dataSerie[valueIndex];
					minValue = (minValue <= dataValue) ? minValue : dataValue;
					maxValue = (maxValue >= dataValue) ? maxValue : dataValue;
				}
			}

			if (minValue == Integer.MIN_VALUE || maxValue == Integer.MAX_VALUE) {
				return false;
			}

			legendConfig.unitFactor = 10;
			colorDefinition = colorProvider.getGraphColorDefinition(GraphColorProvider.PREF_GRAPH_GRADIENT);

			legendProvider.setLegendColorColors(colorDefinition.getNewLegendColor());
			legendProvider.setLegendColorValues(legendBounds, minValue, maxValue, Messages.graph_label_gradient_unit);

			break;

		default:
			break;
		}

		return true;
	}
}
