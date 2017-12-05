/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.map25;

import gnu.trove.list.array.TIntArrayList;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Frame;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.ICloseOpenedDialogs;
import net.tourbook.common.tooltip.IOpeningDialog;
import net.tourbook.common.tooltip.OpenDialogManager;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.SWTPopupOverAWT;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.map.IMapSyncListener;
import net.tourbook.map.MapInfoManager;
import net.tourbook.map.MapManager;
import net.tourbook.map.bookmark.ActionMapBookmarks;
import net.tourbook.map.bookmark.IMapBookmarkListener;
import net.tourbook.map.bookmark.IMapBookmarks;
import net.tourbook.map.bookmark.MapBookmark;
import net.tourbook.map.bookmark.MapBookmarkManager;
import net.tourbook.map.bookmark.MapLocation;
import net.tourbook.map25.action.ActionMap25_ShowMarker;
import net.tourbook.map25.action.ActionSelectMap25Provider;
import net.tourbook.map25.action.ActionShowEntireTour;
import net.tourbook.map25.action.ActionSyncMap2WithOtherMap;
import net.tourbook.map25.action.ActionSynchMapWithChartSlider;
import net.tourbook.map25.action.ActionSynchMapWithTour;
import net.tourbook.map25.layer.marker.MapMarker;
import net.tourbook.map25.layer.marker.MarkerLayer;
import net.tourbook.map25.layer.tourtrack.TourLayer;
import net.tourbook.map25.ui.SlideoutMap25_MapOptions;
import net.tourbook.map25.ui.SlideoutMap25_TrackOptions;
import net.tourbook.photo.PhotoSelection;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.SelectionTourMarker;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.map.Animator;
import org.oscim.map.Map;
import org.oscim.utils.Easing;

import de.byteholder.gpx.PointOfInterest;

public class Map25View extends ViewPart implements IMapBookmarks, ICloseOpenedDialogs, IMapBookmarkListener,
		IMapSyncListener {

// SET_FORMATTING_OFF
	//
	private static final String		IMAGE_ACTION_SHOW_TOUR_IN_MAP			= net.tourbook.map2.Messages.Image__Tour;
	private static final String		IMAGE_ACTION_SHOW_TOUR_IN_MAP_DISABLED	= net.tourbook.map2.Messages.Image__Tour_Disabled;
	private static final String		MAP_ACTION_SHOW_TOUR_IN_MAP 			= net.tourbook.map2.Messages.map_action_show_tour_in_map;
	//
	public static final String		ID										= "net.tourbook.map25.Map25View";				//$NON-NLS-1$
	//
	private static final String		STATE_IS_LAYER_BASE_MAP_VISIBLE			= "STATE_IS_LAYER_BASE_MAP_VISIBLE";			//$NON-NLS-1$
	private static final String		STATE_IS_LAYER_BUILDING_VISIBLE			= "STATE_IS_LAYER_BUILDING_VISIBLE";			//$NON-NLS-1$
	private static final String		STATE_IS_LAYER_LABEL_VISIBLE			= "STATE_IS_LAYER_LABEL_VISIBLE";				//$NON-NLS-1$
	private static final String		STATE_IS_LAYER_MARKER_VISIBLE			= "STATE_IS_LAYER_MARKER_VISIBLE";				//$NON-NLS-1$
	private static final String		STATE_IS_LAYER_SCALE_BAR_VISIBLE		= "STATE_IS_LAYER_SCALE_BAR_VISIBLE";			//$NON-NLS-1$
	private static final String		STATE_IS_LAYER_TILE_INFO_VISIBLE		= "STATE_IS_LAYER_TILE_INFO_VISIBLE";			//$NON-NLS-1$
	private static final String		STATE_IS_LAYER_TOUR_VISIBLE				= "STATE_IS_LAYER_TOUR_VISIBLE";				//$NON-NLS-1$
	private static final String 	STATE_IS_SYNC_MAP25_WITH_OTHER_MAP		= "STATE_IS_SYNC_MAP25_WITH_OTHER_MAP";			//$NON-NLS-1$
	private static final String		STATE_IS_SYNCH_MAP_WITH_CHART_SLIDER	= "STATE_SYNCH_MAP_WITH_CHART_SLIDER";			//$NON-NLS-1$
	private static final String		STATE_IS_SYNCH_MAP_WITH_TOUR			= "STATE_SYNCH_MAP_WITH_TOUR";					//$NON-NLS-1$
	//
// SET_FORMATTING_ON
	//
	private static final IDialogSettings	_state									= TourbookPlugin.getState(ID);
	//
	private Map25App						_mapApp;
	//
	private OpenDialogManager				_openDlgMgr								= new OpenDialogManager();
	private final MapInfoManager			_mapInfoManager							= MapInfoManager.getInstance();
	//
	private boolean							_isPartVisible;
	private boolean							_isShowTour;
	//
	private IPartListener2					_partListener;
	private ISelectionListener				_postSelectionListener;
	private ITourEventListener				_tourEventListener;
	//
	private ISelection						_lastHiddenSelection;
	private ISelection						_selectionWhenHidden;
	private int								_lastSelectionHash;
	//
	private ActionMapBookmarks				_actionMapBookmarks;
	private ActionMap25_Options				_actionMapOptions;
	private ActionMap25_ShowMarker			_actionShowMarker_WithOptions;
	private ActionSelectMap25Provider		_actionSelectMapProvider;
	private ActionSynchMapWithChartSlider	_actionSyncMap_WithChartSlider;
	private ActionSynchMapWithTour			_actionSyncMap_WithTour;
	private ActionSyncMap2WithOtherMap		_actionSyncMap_WithOtherMap;
	private ActionShowEntireTour			_actionShowEntireTour;
	private ActionShowTour_WithConfig		_actionShowTour_WithOptions;
	//
	private ArrayList<TourData>				_allTourData							= new ArrayList<>();
	private TIntArrayList					_allTourStarts							= new TIntArrayList();
	private GeoPoint[]						_allGeoPoints;
	private BoundingBox						_allBoundingBox;
	//
	private int								_hashTourId;
	private int								_hashTourData;
	//
	private boolean							_isMapSynched_WithOtherMap;
	private boolean							_isMapSynched_WithChartSlider;
	private boolean							_isMapSynched_WithTour;
	private long							_lastFiredSyncEventTime;
	//
	//
	// context menu
	private boolean							_isContextMenuVisible;
//	private MouseAdapter					_wwMouseListener;
	private Menu							_swtContextMenu;
	//
	/*
	 * UI controls
	 */
	private Composite						_swtContainer;

	Composite								_parent;

	private class ActionMap25_Options extends ActionToolbarSlideout {

		public ActionMap25_Options() {

			super(
					TourbookPlugin.getImageDescriptor(Messages.Image__MapOptions),
					TourbookPlugin.getImageDescriptor(Messages.Image__MapOptions_Disabled));
		}

		@Override
		protected ToolbarSlideout createSlideout(final ToolBar toolbar) {
			return new SlideoutMap25_MapOptions(_parent, toolbar, Map25View.this);
		}

		@Override
		protected void onBeforeOpenSlideout() {
			closeOpenedDialogs(this);
		}
	}

	private class ActionShowTour_WithConfig extends ActionToolbarSlideout {

		public ActionShowTour_WithConfig() {

			super(
					TourbookPlugin.getImageDescriptor(Map25View.IMAGE_ACTION_SHOW_TOUR_IN_MAP),
					TourbookPlugin.getImageDescriptor(Map25View.IMAGE_ACTION_SHOW_TOUR_IN_MAP_DISABLED));

			isToggleAction = true;
			notSelectedTooltip = MAP_ACTION_SHOW_TOUR_IN_MAP;
		}

		@Override
		protected ToolbarSlideout createSlideout(final ToolBar toolbar) {
			return new SlideoutMap25_TrackOptions(_parent, toolbar, Map25View.this);
		}

		@Override
		protected void onBeforeOpenSlideout() {
			closeOpenedDialogs(this);
		}

		@Override
		protected void onSelect() {

			super.onSelect();

			actionShowTour(getSelection());
		}
	}

	private class Map3ContextMenu extends SWTPopupOverAWT {

		public Map3ContextMenu(final Display display, final Menu swtContextMenu) {
			super(display, swtContextMenu);
		}

	}

	void actionContextMenu(final int relativeX, final int relativeY) {

		// open context menu

		// set state here because opening the context menu is async
		_isContextMenuVisible = true;

		_swtContainer.getDisplay().asyncExec(new Runnable() {

			@Override
			public void run() {

				final Point screenPoint = _swtContainer.toDisplay(relativeX, relativeY);

				createContextMenu(screenPoint.x, screenPoint.y);
			}
		});

	}

	/**
	 * Show/hide tour tracks.
	 * 
	 * @param isTrackVisible
	 */
	public void actionShowTour(final boolean isTrackVisible) {

		_isShowTour = isTrackVisible;

		_mapApp.getLayer_Tour().setEnabled(_isShowTour);
		_mapApp.getMap().render();

		enableActions();
	}

	public void actionShowTourMarker(final boolean isMarkerVisible) {

		_mapApp.getLayer_Marker().setEnabled(isMarkerVisible);
		_mapApp.getMap().render();

		enableActions();
	}

	public void actionSync_WithChartSlider() {

		_isMapSynched_WithChartSlider = _actionSyncMap_WithChartSlider.isChecked();

		if (_isMapSynched_WithChartSlider) {

			// ensure that the track sliders are displayed

			deactivateMapSync();

			_actionShowTour_WithOptions.setSelection(true);

			// map must be synched with selected tour
			_actionSyncMap_WithTour.setChecked(true);
			_isMapSynched_WithTour = true;

			paintTours_AndUpdateMap();
		}

	}

	public void actionSync_WithOtherMap(final boolean isSelected) {

		_isMapSynched_WithOtherMap = isSelected;

		if (_isMapSynched_WithOtherMap) {

			deactivateTourSync();
			deactivateSliderSync();
		}
	}

	public void actionSync_WithTour() {

		_isMapSynched_WithTour = _actionSyncMap_WithTour.isChecked();

		if (_isMapSynched_WithTour) {

			deactivateMapSync();

			paintTours_AndUpdateMap();

		} else {

			deactivateSliderSync();
		}

	}

	public void actionZoomShowEntireTour() {

		if (_allBoundingBox == null) {

			// a tour is not yet displayed

			showToursFromTourProvider();

			return;
		}

		final Map map25 = _mapApp.getMap();

		map25.post(new Runnable() {

			@Override
			public void run() {

				final Animator animator = map25.animator();

				animator.cancel();
				animator.animateTo(//
						2000,
						_allBoundingBox,
						Easing.Type.SINE_INOUT,
						Animator.ANIM_MOVE | Animator.ANIM_SCALE);

				map25.updateMap(true);
			}
		});

	}

	private void addPartListener() {

		_partListener = new IPartListener2() {

			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == Map25View.this) {
					saveState();
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == Map25View.this) {
					_isPartVisible = false;
				}
			}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == Map25View.this) {

					_isPartVisible = true;

					if (_lastHiddenSelection != null) {

						onSelectionChanged(_lastHiddenSelection);

						_lastHiddenSelection = null;
					}
				}
			}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		_postSelectionListener = new ISelectionListener() {
			@Override
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
				onSelectionChanged(selection);
			}
		};

		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == Map25View.this) {
					return;
				}

				if (eventId == TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED) {

//					resetMap();

				} else if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

//					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
//					if ((modifiedTours != null) && (modifiedTours.size() > 0)) {
//
//						_allTourData.clear();
//						_allTourData.addAll(modifiedTours);
//
//						resetMap();
//					}

				} else if (eventId == TourEventId.UPDATE_UI || eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

//					clearView();

				} else if (eventId == TourEventId.MARKER_SELECTION) {

//					if (eventData instanceof SelectionTourMarker) {
//
//						onSelectionChanged_TourMarker((SelectionTourMarker) eventData, false);
//					}

				} else if ((eventId == TourEventId.TOUR_SELECTION) && eventData instanceof ISelection) {

					onSelectionChanged((ISelection) eventData);

				} else if (eventId == TourEventId.SLIDER_POSITION_CHANGED && eventData instanceof ISelection) {

					onSelectionChanged((ISelection) eventData);
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void clearView() {

	}

	/**
	 * Close all opened dialogs except the opening dialog.
	 * 
	 * @param openingDialog
	 */
	@Override
	public void closeOpenedDialogs(final IOpeningDialog openingDialog) {
		_openDlgMgr.closeOpenedDialogs(openingDialog);
	}

	private void createActions() {

		_actionShowMarker_WithOptions = new ActionMap25_ShowMarker(this, _parent);
		_actionMapBookmarks = new ActionMapBookmarks(this._parent, this);

		_actionMapOptions = new ActionMap25_Options();
		_actionSelectMapProvider = new ActionSelectMap25Provider(this);
		_actionShowEntireTour = new ActionShowEntireTour(this);
		_actionSyncMap_WithOtherMap = new ActionSyncMap2WithOtherMap(this);
		_actionSyncMap_WithTour = new ActionSynchMapWithTour(this);
		_actionSyncMap_WithChartSlider = new ActionSynchMapWithChartSlider(this);
		_actionShowTour_WithOptions = new ActionShowTour_WithConfig();

	}

	private BoundingBox createBoundingBox(final GeoPoint[] geoPoints) {

		// this is optimized for performance by using an array which BoundingBox do no support
		int minLat = Integer.MAX_VALUE;
		int minLon = Integer.MAX_VALUE;
		int maxLat = Integer.MIN_VALUE;
		int maxLon = Integer.MIN_VALUE;

		for (final GeoPoint geoPoint : geoPoints) {

			if (geoPoint != null) {

				minLat = Math.min(minLat, geoPoint.latitudeE6);
				minLon = Math.min(minLon, geoPoint.longitudeE6);
				maxLat = Math.max(maxLat, geoPoint.latitudeE6);
				maxLon = Math.max(maxLon, geoPoint.longitudeE6);
			}
		}

		return new BoundingBox(minLat, minLon, maxLat, maxLon);
	}

	/**
	 * Context menu with net.tourbook.common.util.SWTPopupOverAWT
	 * 
	 * @param xScreenPos
	 * @param yScreenPos
	 */
	private void createContextMenu(final int xScreenPos, final int yScreenPos) {

		disposeContextMenu();

		_swtContextMenu = new Menu(_swtContainer);

		// Add listener to repopulate the menu each time
		_swtContextMenu.addMenuListener(new MenuAdapter() {

			boolean _isFilled;

			@Override
			public void menuHidden(final MenuEvent e) {

				_isContextMenuVisible = false;

				/*
				 * run async that the context state and tour info reset is done after the context
				 * menu actions has done they tasks
				 */
				Display.getCurrent().asyncExec(new Runnable() {
					@Override
					public void run() {

//						hideTourInfo();
					}
				});
			}

			@Override
			public void menuShown(final MenuEvent e) {

				if (_isFilled == false) {

					// Ubuntu filled it twice

					_isFilled = true;

					fillContextMenu((Menu) e.widget);
				}

				_isContextMenuVisible = true;
			}
		});

		final Display display = _swtContainer.getDisplay();

		final Map3ContextMenu swt_awt_ContextMenu = new Map3ContextMenu(display, _swtContextMenu);

		display.asyncExec(new Runnable() {
			@Override
			public void run() {
//				System.out.println("SWT calling menu"); //$NON-NLS-1$
				swt_awt_ContextMenu.swtIndirectShowMenu(xScreenPos, yScreenPos);
			}
		});
	}

	private List<MapMarker> createMapMarkers(final ArrayList<TourData> allTourData) {

		final List<MapMarker> allMarkerItems = new ArrayList<>();

		for (final TourData tourData : allTourData) {

			final Set<TourMarker> tourMarkerList = tourData.getTourMarkers();

			if (tourMarkerList.size() == 0) {
				continue;
			}

			// check if geo position is available
			final double[] latitudeSerie = tourData.latitudeSerie;
			final double[] longitudeSerie = tourData.longitudeSerie;
			if (latitudeSerie == null || longitudeSerie == null) {
				continue;
			}

			for (final TourMarker tourMarker : tourMarkerList) {

				// skip marker when hidden or not set
				if (tourMarker.isMarkerVisible() == false || tourMarker.getLabel().length() == 0) {
					continue;
				}

				final int serieIndex = tourMarker.getSerieIndex();

				/*
				 * check bounds because when a tour is split, it can happen that the marker serie
				 * index is out of scope
				 */
				if (serieIndex >= latitudeSerie.length) {
					continue;
				}

				/*
				 * draw tour marker
				 */

				final double latitude = latitudeSerie[serieIndex];
				final double longitude = longitudeSerie[serieIndex];

				final MapMarker item = new MapMarker(
						tourMarker.getLabel(),
						tourMarker.getDescription(),
						new GeoPoint(latitude, longitude));

				allMarkerItems.add(item);
			}
		}

		return allMarkerItems;
	}

	@Override
	public void createPartControl(final Composite parent) {

		_parent = parent;

		createActions();
		fillActionBars();

		createUI(parent);

		addPartListener();
		addTourEventListener();
		addSelectionListener();
		MapBookmarkManager.addBookmarkListener(this);
		MapManager.addMapSyncListener(this);
	}

	private void createUI(final Composite parent) {

		_swtContainer = new Composite(parent, SWT.EMBEDDED | SWT.NO_BACKGROUND);
		final Frame awtContainer = SWT_AWT.new_Frame(_swtContainer);

		final Canvas awtCanvas = new Canvas();
		awtContainer.setLayout(new BorderLayout());
		awtCanvas.setIgnoreRepaint(true);

		awtContainer.add(awtCanvas);
		awtCanvas.setFocusable(true);
		awtCanvas.requestFocus();

		awtContainer.addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(final ComponentEvent e) {

				/*
				 * Render map otherwise a black screen is displayed until the map is moved
				 */
				final Map map = _mapApp.getMap();

				// check if initialized
				if (map == null) {
					return;
				}

				map.render();
			}
		});

		_mapApp = Map25App.createMap(this, _state, awtCanvas);
	}

	private void deactivateMapSync() {

		// disable map sync

		_isMapSynched_WithOtherMap = false;
		_actionSyncMap_WithOtherMap.setChecked(false);
	}

	private void deactivateSliderSync() {

		// disable slider sync

		_isMapSynched_WithChartSlider = false;
		_actionSyncMap_WithChartSlider.setChecked(false);
	}

	private void deactivateTourSync() {

		// disable tour sync

		_isMapSynched_WithTour = false;
		_actionSyncMap_WithTour.setChecked(false);
	}

	@Override
	public void dispose() {

		if (_partListener != null) {

			getViewSite().getPage().removePartListener(_partListener);

			_mapApp.stop();
		}

		MapBookmarkManager.removeBookmarkListener(this);
		MapManager.removeMapSyncListener(this);
		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		disposeContextMenu();

		super.dispose();
	}

	private void disposeContextMenu() {

		if (_swtContextMenu != null) {
			_swtContextMenu.dispose();
		}
	}

	/**
	 * Enable actions according to the available tours in {@link #_allTours}.
	 */
	void enableActions() {

		final TourLayer tourLayer = _mapApp.getLayer_Tour();
		final boolean isTourLayerVisible = tourLayer == null ? false : tourLayer.isEnabled();

		final boolean isTourAvailable = _allTourData.size() > 0;

		final boolean canShowTour = isTourAvailable && isTourLayerVisible;

		_actionShowTour_WithOptions.setEnabled(isTourAvailable);
		_actionShowMarker_WithOptions.setEnabled(isTourAvailable);
		_actionShowEntireTour.setEnabled(canShowTour);
		_actionSyncMap_WithTour.setEnabled(canShowTour);
		_actionSyncMap_WithChartSlider.setEnabled(canShowTour);

		_actionMapBookmarks.setEnabled(true);
		_actionMapOptions.setEnabled(true);
	}

	private void enableContextMenuActions() {

	}

	private void fillActionBars() {

		/*
		 * fill view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(_actionShowTour_WithOptions);
		tbm.add(_actionShowEntireTour);
		tbm.add(_actionSyncMap_WithTour);
		tbm.add(_actionSyncMap_WithChartSlider);
		tbm.add(_actionSyncMap_WithOtherMap);

		tbm.add(new Separator());

		tbm.add(_actionShowMarker_WithOptions);
		tbm.add(_actionMapBookmarks);
		tbm.add(_actionMapOptions);
		tbm.add(_actionSelectMapProvider);

		/*
		 * fill view menu
		 */
//		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

//		fillMapContextMenu(menuMgr);
	}

	private void fillContextMenu(final Menu menu) {

		MapBookmarkManager.fillContextMenu_RecentBookmarks(menu, this);

		enableContextMenuActions();
	}

	void fireSyncMapEvent(final MapPosition mapPosition, final int positionFlags) {

		_lastFiredSyncEventTime = System.currentTimeMillis();

		MapManager.fireSyncMapEvent(mapPosition, this, positionFlags);

		updateUI_MapPosition(mapPosition.getLatitude(), mapPosition.getLongitude(), mapPosition.zoomLevel);
	}

	public Map25App getMapApp() {
		return _mapApp;
	}

	@Override
	public MapLocation getMapLocation() {

		final MapPosition mapPosition = _mapApp.getMap().getMapPosition();

		return new MapLocation(mapPosition);
	}

	@Override
	public void moveToMapLocation(final MapBookmark selectedBookmark) {

		MapBookmarkManager.setLastSelectedBookmark(selectedBookmark);

		final Map map = _mapApp.getMap();
		final MapPosition mapPosition = selectedBookmark.getMapPosition();

		Map25ConfigManager.setMapLocation(map, mapPosition);
	}

	void onMapPosition(final GeoPoint mapGeoPoint, final int zoomLevel) {

		updateUI_MapPosition(mapGeoPoint.getLatitude(), mapGeoPoint.getLongitude(), zoomLevel);
	}

	@Override
	public void onSelectBookmark(final MapBookmark mapBookmark) {

		moveToMapLocation(mapBookmark);
	}

	private void onSelectionChanged(final ISelection selection) {

		final int selectionHash = selection.hashCode();
		if (_lastSelectionHash == selectionHash) {

			/*
			 * Last selection has not changed, this can occure when the app lost the focus and got
			 * the focus again.
			 */
			return;
		}

		_lastSelectionHash = selectionHash;

		if (_isPartVisible == false) {

			if (selection instanceof SelectionTourData
					|| selection instanceof SelectionTourId
					|| selection instanceof SelectionTourIds) {

				// keep only selected tours
				_selectionWhenHidden = selection;
			}
			return;
		}

		if (selection instanceof SelectionTourData) {

			final SelectionTourData selectionTourData = (SelectionTourData) selection;
			final TourData tourData = selectionTourData.getTourData();

			paintTour(tourData);

		} else if (selection instanceof SelectionTourId) {

			final SelectionTourId tourIdSelection = (SelectionTourId) selection;
			final TourData tourData = TourManager.getInstance().getTourData(tourIdSelection.getTourId());

			paintTour(tourData);

		} else if (selection instanceof SelectionTourIds) {

			// paint all selected tours

			final ArrayList<Long> tourIds = ((SelectionTourIds) selection).getTourIds();
			if (tourIds.size() == 0) {

				// history tour (without tours) is displayed

			} else if (tourIds.size() == 1) {

				// only 1 tour is displayed, synch with this tour !!!

				final TourData tourData = TourManager.getInstance().getTourData(tourIds.get(0));

				paintTour(tourData);

			} else {

				// paint multiple tours

				paintTours(tourIds);

			}

		} else if (selection instanceof SelectionChartInfo) {

			if (!_isMapSynched_WithChartSlider) {
				return;
			}

			TourData tourData = null;

			final SelectionChartInfo chartInfo = (SelectionChartInfo) selection;

			final Chart chart = chartInfo.getChart();
			if (chart instanceof TourChart) {

				final TourChart tourChart = (TourChart) chart;
				tourData = tourChart.getTourData();
			}

			if (tourData != null && tourData.isMultipleTours()) {

				// multiple tours are selected

			} else {

				// use old behaviour

				final ChartDataModel chartDataModel = chartInfo.chartDataModel;
				if (chartDataModel != null) {

					final Object tourId = chartDataModel.getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
					if (tourId instanceof Long) {

						tourData = TourManager.getInstance().getTourData((Long) tourId);
						if (tourData == null) {

							// tour is not in the database, try to get it from the raw data manager

							final HashMap<Long, TourData> rawData = RawDataManager.getInstance().getImportedTours();
							tourData = rawData.get(tourId);
						}
					}
				}
			}

			if (tourData != null) {

				syncMapWith_ChartSlider(//
						tourData,
						chartInfo.leftSliderValuesIndex,
						chartInfo.rightSliderValuesIndex,
						chartInfo.selectedSliderValuesIndex);

				enableActions();
			}

		} else if (selection instanceof SelectionChartXSliderPosition) {

			if (!_isMapSynched_WithChartSlider) {
				return;
			}

			final SelectionChartXSliderPosition xSliderPos = (SelectionChartXSliderPosition) selection;
			final Chart chart = xSliderPos.getChart();
			if (chart == null) {
				return;
			}

			final ChartDataModel chartDataModel = chart.getChartDataModel();

			final Object tourId = chartDataModel.getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
			if (tourId instanceof Long) {

				final TourData tourData = TourManager.getInstance().getTourData((Long) tourId);
				if (tourData != null) {

					final int leftSliderValueIndex = xSliderPos.getLeftSliderValueIndex();
					int rightSliderValueIndex = xSliderPos.getRightSliderValueIndex();

					rightSliderValueIndex =
							rightSliderValueIndex == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION
									? leftSliderValueIndex
									: rightSliderValueIndex;

					syncMapWith_ChartSlider(//
							tourData,
							leftSliderValueIndex,
							rightSliderValueIndex,
							leftSliderValueIndex);

					enableActions();
				}
			}

		} else if (selection instanceof SelectionTourMarker) {

//			final SelectionTourMarker markerSelection = (SelectionTourMarker) selection;
//
//			onSelectionChanged_TourMarker(markerSelection, true);

//		} else if (selection instanceof SelectionMapPosition) {
//
//			final SelectionMapPosition mapPositionSelection = (SelectionMapPosition) selection;
//
//			final int valueIndex1 = mapPositionSelection.getSlider1ValueIndex();
//			int valueIndex2 = mapPositionSelection.getSlider2ValueIndex();
//
//			valueIndex2 = valueIndex2 == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION
//					? valueIndex1
//					: valueIndex2;
//
//			positionMapTo_TourSliders(//
//					mapPositionSelection.getTourData(),
//					valueIndex1,
//					valueIndex2,
//					valueIndex1,
//					null);
//
//			enableActions();

		} else if (selection instanceof PointOfInterest) {

//			_isTourOrWayPoint = false;
//
//			clearView();
//
//			final PointOfInterest poi = (PointOfInterest) selection;
//
//			_poiPosition = poi.getPosition();
//			_poiName = poi.getName();
//
//			final String boundingBox = poi.getBoundingBox();
//			if (boundingBox == null) {
//				_poiZoomLevel = _map.getZoom();
//			} else {
//				_poiZoomLevel = _map.getZoom(boundingBox);
//			}
//
//			if (_poiZoomLevel == -1) {
//				_poiZoomLevel = _map.getZoom();
//			}
//
//			_map.setPoi(_poiPosition, _poiZoomLevel, _poiName);
//
//			_actionShowPOI.setChecked(true);
//
//			enableActions();

		} else if (selection instanceof StructuredSelection) {

//			final StructuredSelection structuredSelection = (StructuredSelection) selection;
//			final Object firstElement = structuredSelection.getFirstElement();
//
//			if (firstElement instanceof TVICatalogComparedTour) {
//
//				final TVICatalogComparedTour comparedTour = (TVICatalogComparedTour) firstElement;
//				final long tourId = comparedTour.getTourId();
//
//				final TourData tourData = TourManager.getInstance().getTourData(tourId);
//				paintTours_20_One(tourData, false);
//
//			} else if (firstElement instanceof TVICompareResultComparedTour) {
//
//				final TVICompareResultComparedTour compareResultItem = (TVICompareResultComparedTour) firstElement;
//				final TourData tourData = TourManager.getInstance().getTourData(
//						compareResultItem.getComparedTourData().getTourId());
//				paintTours_20_One(tourData, false);
//
//			} else if (firstElement instanceof TourWayPoint) {
//
//				final TourWayPoint wp = (TourWayPoint) firstElement;
//
//				final TourData tourData = wp.getTourData();
//
//				paintTours_20_One(tourData, false);
//
//				_map.setPOI(_wayPointToolTipProvider, wp);
//
//				enableActions();
//			}
//
//			enableActions();

		} else if (selection instanceof PhotoSelection) {

//			paintPhotos(((PhotoSelection) selection).galleryPhotos);
//
//			enableActions();

		} else if (selection instanceof SelectionTourCatalogView) {

//			// show reference tour
//
//			final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;
//
//			final TVICatalogRefTourItem refItem = tourCatalogSelection.getRefItem();
//			if (refItem != null) {
//
//				final TourData tourData = TourManager.getInstance().getTourData(refItem.getTourId());
//
//				paintTours_20_One(tourData, false);
//
//				enableActions();
//			}

		} else if (selection instanceof SelectionDeletedTours) {

			clearView();
		}
	}

	private void paintTour(final TourData tourData) {

		_allTourData.clear();
		_allTourData.add(tourData);

		paintTours_AndUpdateMap();
	}

	private void paintTours(final ArrayList<Long> tourIdList) {

		/*
		 * TESTING if a map redraw can be avoided, 15.6.2015
		 */
		final int tourIdsHashCode = tourIdList.hashCode();
		final int allToursHashCode = _allTourData.hashCode();
		if (tourIdsHashCode == _hashTourId && allToursHashCode == _hashTourData) {
			// skip redrawing
			return;
		}

		if (tourIdList.hashCode() != _hashTourId || _allTourData.hashCode() != _hashTourData) {

			// tour data needs to be loaded

			TourManager.loadTourData(tourIdList, _allTourData, true);

			_hashTourId = tourIdList.hashCode();
			_hashTourData = _allTourData.hashCode();
		}

		paintTours_AndUpdateMap();
	}

	private void paintTours_AndUpdateMap() {

		enableActions();

		if (!_isShowTour) {
			return;
		}

		/*
		 * Tours
		 */
		final TourLayer tourLayer = _mapApp.getLayer_Tour();
		if (tourLayer == null) {

			// tour layer is not yet created, this happened
			return;
		}

		int geoSize = 0;

		for (final TourData tourData : _allTourData) {

			// check if GPS data are available
			if (tourData.latitudeSerie != null) {
				geoSize += tourData.latitudeSerie.length;
			}
		}

		// use array to optimize performance when millions of points are created
		_allGeoPoints = new GeoPoint[geoSize];
		_allTourStarts.clear();

		int tourIndex = 0;
		int geoIndex = 0;

		for (final TourData tourData : _allTourData) {

			// check if GPS data are available
			if (tourData.latitudeSerie == null) {
				continue;
			}

			_allTourStarts.add(tourIndex);

			final double[] latitudeSerie = tourData.latitudeSerie;
			final double[] longitudeSerie = tourData.longitudeSerie;

			// create vtm geo points
			for (int serieIndex = 0; serieIndex < latitudeSerie.length; serieIndex++, tourIndex++) {
				_allGeoPoints[geoIndex++] = (new GeoPoint(latitudeSerie[serieIndex], longitudeSerie[serieIndex]));
			}
		}

		tourLayer.setPoints(_allGeoPoints, _allTourStarts);

		/*
		 * Markers
		 */
		final MarkerLayer markerLayer = _mapApp.getLayer_Marker();

		if (markerLayer.isEnabled()) {

			final List<MapMarker> allMarkers = createMapMarkers(_allTourData);
			markerLayer.replaceMarkers(allMarkers);
		}

		/*
		 * Update map
		 */
		final Map map25 = _mapApp.getMap();

		map25.post(new Runnable() {

			@Override
			public void run() {

				// create outside isSynch that data are available when map is zoomed to show the whole tour
				_allBoundingBox = createBoundingBox(_allGeoPoints);

				if (_isMapSynched_WithTour) {

					final int animationTime = Map25ConfigManager.getActiveTourTrackConfig().animationTime;
					Map25ConfigManager.setMapLocation(map25, _allBoundingBox, animationTime);
				}

				map25.updateMap(true);
			}
		});
	}

	void restoreState() {

		/*
		 * Layer
		 */

		// tour
		_isShowTour = Util.getStateBoolean(_state, STATE_IS_LAYER_TOUR_VISIBLE, true);
		_actionShowTour_WithOptions.setSelection(_isShowTour);
		_mapApp.getLayer_Tour().setEnabled(_isShowTour);

		// marker
		final boolean isMarkerVisible = Util.getStateBoolean(_state, STATE_IS_LAYER_MARKER_VISIBLE, true);
		_actionShowMarker_WithOptions.setSelected(isMarkerVisible);
		_mapApp.getLayer_Marker().setEnabled(isMarkerVisible);

		// other layers
		_mapApp.getLayer_BaseMap().setEnabled(Util.getStateBoolean(_state, STATE_IS_LAYER_BASE_MAP_VISIBLE, true));
		_mapApp.getLayer_Building().setEnabled(Util.getStateBoolean(_state, STATE_IS_LAYER_BUILDING_VISIBLE, true));
		_mapApp.getLayer_Label().setEnabled(Util.getStateBoolean(_state, STATE_IS_LAYER_LABEL_VISIBLE, true));
		_mapApp.getLayer_ScaleBar().setEnabled(Util.getStateBoolean(_state, STATE_IS_LAYER_SCALE_BAR_VISIBLE, true));
		_mapApp.getLayer_TileInfo().setEnabled(Util.getStateBoolean(_state, STATE_IS_LAYER_TILE_INFO_VISIBLE, false));

		/*
		 * Other actions
		 */
		// checkbox: synch map with tour
		final boolean isSynchTour = Util.getStateBoolean(_state, STATE_IS_SYNCH_MAP_WITH_TOUR, true);
		_actionSyncMap_WithTour.setChecked(isSynchTour);
		_isMapSynched_WithTour = isSynchTour;

		// checkbox: synch map with chart slider
		final boolean isSynchWithSlider = Util.getStateBoolean(_state, STATE_IS_SYNCH_MAP_WITH_CHART_SLIDER, false);
		_actionSyncMap_WithChartSlider.setChecked(isSynchWithSlider);
		_isMapSynched_WithChartSlider = isSynchWithSlider;

		// synch map with another map
		_isMapSynched_WithOtherMap = Util.getStateBoolean(_state, STATE_IS_SYNC_MAP25_WITH_OTHER_MAP, false);
		_actionSyncMap_WithOtherMap.setChecked(_isMapSynched_WithOtherMap);

		enableActions();

		showToursFromTourProvider();
	}

	private void saveState() {

		_state.put(STATE_IS_SYNCH_MAP_WITH_CHART_SLIDER, _actionSyncMap_WithChartSlider.isChecked());
		_state.put(STATE_IS_SYNCH_MAP_WITH_TOUR, _actionSyncMap_WithTour.isChecked());
		_state.put(STATE_IS_SYNC_MAP25_WITH_OTHER_MAP, _isMapSynched_WithOtherMap);

		_state.put(STATE_IS_LAYER_BASE_MAP_VISIBLE, _mapApp.getLayer_BaseMap().isEnabled());
		_state.put(STATE_IS_LAYER_BUILDING_VISIBLE, _mapApp.getLayer_Building().isEnabled());
		_state.put(STATE_IS_LAYER_LABEL_VISIBLE, _mapApp.getLayer_Label().isEnabled());
		_state.put(STATE_IS_LAYER_MARKER_VISIBLE, _mapApp.getLayer_Marker().isEnabled());
		_state.put(STATE_IS_LAYER_TILE_INFO_VISIBLE, _mapApp.getLayer_TileInfo().isEnabled());
		_state.put(STATE_IS_LAYER_TOUR_VISIBLE, _mapApp.getLayer_Tour().isEnabled());
		_state.put(STATE_IS_LAYER_SCALE_BAR_VISIBLE, _mapApp.getLayer_ScaleBar().isEnabled());

		Map25ConfigManager.saveState();
	}

	@Override
	public void setFocus() {

//		_swtContainer.setFocus();
	}

	private void showToursFromTourProvider() {

		if (!_isShowTour) {
			return;
		}

		Display.getCurrent().asyncExec(new Runnable() {
			@Override
			public void run() {

				// validate widget
				if (_swtContainer.isDisposed()) {
					return;
				}

				final ArrayList<TourData> tourDataList = TourManager.getSelectedTours();
				if (tourDataList != null) {

					_allTourData.clear();
					_allTourData.addAll(tourDataList);

					paintTours_AndUpdateMap();
				}

				enableActions();
			}
		});
	}

	private void syncMapWith_ChartSlider(	final TourData tourData,
											final int leftSliderValuesIndex,
											final int rightSliderValuesIndex,
											final int selectedSliderIndex) {

//		final TrackSliderLayer chartSliderLayer = getLayerTrackSlider();
//		if (chartSliderLayer == null) {
//			return;
//		}

		if (tourData == null || tourData.latitudeSerie == null) {

//			chartSliderLayer.setSliderVisible(false);

		} else {

			// sync map with chart slider

			syncMapWith_SliderPosition(tourData, /* chartSliderLayer, */ selectedSliderIndex);

//			// update slider UI
//			updateTrackSlider_10_Position(//
//					tourData,
//					leftSliderValuesIndex,
//					rightSliderValuesIndex);

			enableActions();
		}
	}

	private void syncMapWith_SliderPosition(final TourData tourData,
//											final TrackSliderLayer chartSliderLayer,
											int valuesIndex) {

		final double[] latitudeSerie = tourData.latitudeSerie;

		// check bounds
		if (valuesIndex >= latitudeSerie.length) {
			valuesIndex = latitudeSerie.length;
		}

		final double latitude = latitudeSerie[valuesIndex];
		final double longitude = tourData.longitudeSerie[valuesIndex];

		final Map map25 = _mapApp.getMap();
		final MapPosition currentMapPos = new MapPosition();

		// get current position
		map25.viewport().getMapPosition(currentMapPos);

		// set new position
		currentMapPos.setPosition(latitude, longitude);

		// update map
		map25.setMapPosition(currentMapPos);
		map25.render();
	}

	@Override
	public void syncMapWithOtherMap(final MapPosition mapPosition,
									final ViewPart viewPart,
									final int positionFlags) {

		if (!_isMapSynched_WithOtherMap) {

			// sync feature is disabled

			return;
		}

		if (viewPart == this || !_isPartVisible) {

			// event is fired from this map -> ignore

			return;
		}

		final long timeDiff = System.currentTimeMillis() - _lastFiredSyncEventTime;

		if (timeDiff < 1000) {
			// ignore because it causes LOTS of problems when synching moved map
			return;
		}

		final Map map = _mapApp.getMap();

		/*
		 * Keep current tilt/bearing
		 */
		final MapPosition currentMapPos = map.getMapPosition();
		if (mapPosition.bearing == 0) {
			mapPosition.bearing = currentMapPos.bearing;
		}

		if (mapPosition.tilt == 0) {
			mapPosition.tilt = currentMapPos.tilt;
		}

		Map25ConfigManager.setMapLocation(map, mapPosition);
	}

	private void updateUI_MapPosition(final double latitude, final double longitude, final int zoomLevel) {

		// validate widget
		if (_swtContainer.isDisposed()) {
			return;
		}

		_swtContainer.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {

				// validate widget
				if (_swtContainer.isDisposed()) {
					return;
				}

				_mapInfoManager.setMapPosition(latitude, longitude, zoomLevel);
			}
		});
	}

	void updateUI_SelectedMapProvider(final Map25Provider selectedMapProvider) {

		_actionSelectMapProvider.updateUI_SelectedMapProvider(selectedMapProvider);
	}

}
