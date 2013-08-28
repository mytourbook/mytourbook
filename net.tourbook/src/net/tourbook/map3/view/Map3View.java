/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.map3.view;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.color.IMapColorProvider;
import net.tourbook.common.color.MapColorId;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.SWTPopupOverAWT;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.extension.export.ActionExport;
import net.tourbook.map2.view.TourMapColors;
import net.tourbook.map3.action.ActionMapColor;
import net.tourbook.map3.action.ActionOpenMap3LayerView;
import net.tourbook.map3.action.ActionShowEntireTour;
import net.tourbook.map3.action.ActionShowTourInMap3;
import net.tourbook.map3.action.ActionSyncMapPositionWithSlider;
import net.tourbook.map3.action.ActionSyncMapViewWithTour;
import net.tourbook.map3.action.ActionTourColor;
import net.tourbook.map3.layer.tourtrack.ITrackPath;
import net.tourbook.map3.layer.tourtrack.TourMap3Position;
import net.tourbook.map3.layer.tourtrack.TourTrackLayer;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.printing.ActionPrint;
import net.tourbook.tour.ActionOpenAdjustAltitudeDialog;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionOpenTour;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

/**
 * Display 3-D map with tour tracks.
 */
public class Map3View extends ViewPart implements ITourProvider {

	public static final String					ID										= "net.tourbook.map3.view.Map3ViewId";		//$NON-NLS-1$

	private static final String					STATE_IS_SYNC_MAP_VIEW_WITH_TOUR		= "STATE_IS_SYNC_MAP_VIEW_WITH_TOUR";		//$NON-NLS-1$

	private static final String					STATE_IS_SYNC_MAP_POSITION_WITH_SLIDER	= "STATE_IS_SYNC_MAP_POSITION_WITH_SLIDER"; //$NON-NLS-1$
	private static final String					STATE_IS_TOUR_VISIBLE					= "STATE_IS_TOUR_VISIBLE";					//$NON-NLS-1$
	private static final String					STATE_MAP3_VIEW							= "STATE_MAP3_VIEW";						//$NON-NLS-1$
	private static final String					STATE_TOUR_COLOR_ID						= "STATE_TOUR_COLOR_ID";					//$NON-NLS-1$
	private final IPreferenceStore				_prefStore								= TourbookPlugin.getDefault()//
																								.getPreferenceStore();

	private final IDialogSettings				_state									= TourbookPlugin
																								.getStateSection(getClass()
																										.getCanonicalName());

	private static final WorldWindowGLCanvas	_wwCanvas								= Map3Manager.getWWCanvas();

	private ActionOpenMap3LayerView				_actionOpenMap3LayerView;

	private ActionMapColor						_actionMapColor;

	private ActionShowEntireTour				_actionShowEntireTour;
	private ActionShowTourInMap3				_actionShowTourInMap3;
	private ActionSyncMapPositionWithSlider		_actionSynMapPositionWithSlider;
	private ActionSyncMapViewWithTour			_actionSynMapViewWithTour;
	private ActionTourColor						_actionTourColorAltitude;
	private ActionTourColor						_actionTourColorGradient;
	private ActionTourColor						_actionTourColorPulse;
	private ActionTourColor						_actionTourColorSpeed;
	private ActionTourColor						_actionTourColorPace;
	private ActionTourColor						_actionTourColorHrZone;
	// context menu actions
	private ActionEditQuick						_actionEditQuick;

	private ActionEditTour						_actionEditTour;
	private ActionExport						_actionExportTour;
	private ActionOpenAdjustAltitudeDialog		_actionOpenAdjustAltitudeDialog;
	private ActionOpenMarkerDialog				_actionOpenMarkerDialog;
	private ActionOpenTour						_actionOpenTour;
	private ActionPrint							_actionPrintTour;
	private PostSelectionProvider				_postSelectionProvider;

	private IPartListener2						_partListener;

	private ISelectionListener					_postSelectionListener;
	private IPropertyChangeListener				_prefChangeListener;
	private ITourEventListener					_tourEventListener;
	private MouseAdapter						_awtMouseListener;
	private boolean								_isPartActive;

	private boolean								_isPartVisible;
	private boolean								_isRestored;
	private ISelection							_lastHiddenSelection;
	private boolean								_isSyncMapPositionWithSlider;

	private boolean								_isSyncMapViewWithTour;
	private boolean								_isTourVisible;
	private static int							_renderCounter;

	/**
	 * Contains all tours which are displayed in the map.
	 */
	private ArrayList<TourData>					_allTours								= new ArrayList<TourData>();

	/**
	 * Color id for the currently displayed tour tracks.
	 */
	private MapColorId							_tourColorId;

	/*
	 * UI controls
	 */
	private Composite							_mapContainer;

	private Frame								_awtFrame;
	private Menu								_swtContextMenu;

	private class Map3ContextMenu extends SWTPopupOverAWT {

		public Map3ContextMenu(final Display display, final Menu swtContextMenu) {
			super(display, swtContextMenu);
		}

	}

	public Map3View() {}

	void actionOpenTrackColorDialog() {

		// set color before menu is filled, this sets the action image and color provider
		_actionMapColor.setColorId(_tourColorId);

		_actionMapColor.run();
	}

	public void actionSetMapColor(final MapColorId colorId) {

		_tourColorId = colorId;

		setColorProvider(colorId);

		updateMapColors();
	}

	public void actionShowTour(final boolean isTrackVisible) {

		_isTourVisible = isTrackVisible;

		Map3Manager.setTourTrackVisible(isTrackVisible);

		showAllTours();
	}

	public void actionSynchMapPositionWithSlider() {

		_isSyncMapPositionWithSlider = _actionSynMapPositionWithSlider.isChecked();
	}

	public void actionSynchMapViewWithTour() {

		_isSyncMapViewWithTour = _actionSynMapViewWithTour.isChecked();

		if (_isSyncMapViewWithTour) {
			showAllTours();
		}
	}

	public void actionZoomShowEntireTour() {

		showAllTours(true);
	}

	private void addMap3Listener() {

		_awtMouseListener = new MouseAdapter() {

			@Override
			public void mouseClicked(final MouseEvent e) {
				onAWTMouseClick(e);
			}
		};

		_wwCanvas.getInputHandler().addMouseListener(_awtMouseListener);
	}

	private void addPartListener() {

		_partListener = new IPartListener2() {
			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == Map3View.this) {
					_isPartActive = true;
				}
			}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == Map3View.this) {
					saveState();
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == Map3View.this) {
					_isPartActive = false;
				}
			}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == Map3View.this) {
					_isPartVisible = false;
				}
			}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == Map3View.this) {

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

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)) {

					// update map colors

					updateMapColors();

				} else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					_actionShowTourInMap3.updateMeasurementSystem();
				}
			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		_postSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (part == Map3View.this) {
					// ignore own selections
					return;
				}

				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == Map3View.this) {
					return;
				}

				if (eventId == TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED) {

					showAllTours();

				} else if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
					if ((modifiedTours != null) && (modifiedTours.size() > 0)) {
						updateModifiedTours(modifiedTours);
					}

				} else if (eventId == TourEventId.UPDATE_UI || eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

					clearView();

				} else if (eventId == TourEventId.SLIDER_POSITION_CHANGED) {
					onSelectionChanged((ISelection) eventData);
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void clearView() {

		_allTours.clear();

		showAllTours();
	}

	private void createActions(final Composite parent) {

		_actionOpenMap3LayerView = new ActionOpenMap3LayerView();

		_actionMapColor = new ActionMapColor(this, _state);

		_actionShowEntireTour = new ActionShowEntireTour(this);
		_actionShowTourInMap3 = new ActionShowTourInMap3(this, parent);
		_actionSynMapPositionWithSlider = new ActionSyncMapPositionWithSlider(this);
		_actionSynMapViewWithTour = new ActionSyncMapViewWithTour(this);

		_actionTourColorAltitude = ActionTourColor.createAction(this, MapColorId.Altitude);
		_actionTourColorGradient = ActionTourColor.createAction(this, MapColorId.Gradient);
		_actionTourColorPace = ActionTourColor.createAction(this, MapColorId.Pace);
		_actionTourColorPulse = ActionTourColor.createAction(this, MapColorId.Pulse);
		_actionTourColorSpeed = ActionTourColor.createAction(this, MapColorId.Speed);
		_actionTourColorHrZone = ActionTourColor.createAction(this, MapColorId.HrZone);

		// context menu actions
		_actionEditQuick = new ActionEditQuick(this);
		_actionEditTour = new ActionEditTour(this);
		_actionExportTour = new ActionExport(this);
		_actionOpenAdjustAltitudeDialog = new ActionOpenAdjustAltitudeDialog(this);
		_actionOpenMarkerDialog = new ActionOpenMarkerDialog(this, true);
		_actionOpenTour = new ActionOpenTour(this);
		_actionPrintTour = new ActionPrint(this);

	}

	/**
	 * Context menu with net.tourbook.common.util.SWTPopupOverAWT
	 * 
	 * @param xPosScreen
	 * @param yPosScreen
	 */
	private void createContextMenu(final int xPosScreen, final int yPosScreen) {

		disposeContextMenu();

		_swtContextMenu = new Menu(_mapContainer);

		// Add listener to repopulate the menu each time
		_swtContextMenu.addMenuListener(new MenuAdapter() {

			boolean	_isFilled;

			@Override
			public void menuShown(final MenuEvent e) {

				if (_isFilled == false) {

					// Ubuntu filled it twice

					_isFilled = true;

					fillContextMenu((Menu) e.widget);
				}
			}
		});

		final Display display = _mapContainer.getDisplay();

		final Map3ContextMenu swt_awt_ContextMenu = new Map3ContextMenu(display, _swtContextMenu);

		display.asyncExec(new Runnable() {
			public void run() {
//				System.out.println("SWT calling menu"); //$NON-NLS-1$
				swt_awt_ContextMenu.swtIndirectShowMenu(xPosScreen, yPosScreen);
			}
		});
	}

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);

		addPartListener();
		addPrefListener();
		addSelectionListener();
		addTourEventListener();
		addMap3Listener();

		createActions(parent);
		fillActionBars();

		// set selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

//		getViewSite().registerContextMenu(menuManager, selectionProvider)

		Map3Manager.setMap3View(this);

		/*
		 * !!! It requires 2x asyncExec that the a tour provider is providing tours !!!
		 */
		Display.getCurrent().asyncExec(new Runnable() {
			@Override
			public void run() {

				restoreState();
				enableActions();

				_isRestored = true;

				if (_lastHiddenSelection != null) {

					onSelectionChanged(_lastHiddenSelection);

					_lastHiddenSelection = null;

				} else if (_allTours.size() == 0) {

					// a tour is not displayed, find a tour provider which provides a tour
					showToursFromTourProvider();

				} else {

					showAllTours();
				}
			}
		});
	}

	private void createUI(final Composite parent) {

		// set parent griddata, this must be done AFTER the content is created, otherwise it fails !!!
		GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);

		// build GUI: container(SWT) -> Frame(AWT) -> Panel(AWT) -> WorldWindowGLCanvas(AWT)
		_mapContainer = new Composite(parent, SWT.EMBEDDED);
		GridDataFactory.fillDefaults().applyTo(_mapContainer);
		{
			_awtFrame = SWT_AWT.new_Frame(_mapContainer);
			final java.awt.Panel awtPanel = new java.awt.Panel(new java.awt.BorderLayout());

			_awtFrame.add(awtPanel);
			awtPanel.add(_wwCanvas, BorderLayout.CENTER);
		}

		parent.layout();
	}

	@Override
	public void dispose() {

		Map3Manager.setMap3View(null);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		getViewSite().getPage().removePostSelectionListener(_postSelectionListener);
		getViewSite().getPage().removePartListener(_partListener);

		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		_wwCanvas.getInputHandler().removeMouseListener(_awtMouseListener);

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

		final boolean isTrackLayerVisible = Map3Manager.getTourTrackLayer().isEnabled();
		final boolean isTourAvailable = _allTours.size() > 0;

		_actionShowTourInMap3.setState(isTrackLayerVisible, isTourAvailable);
		_actionSynMapPositionWithSlider.setEnabled(isTourAvailable);
		_actionSynMapViewWithTour.setEnabled(isTourAvailable);

		/*
		 * tour color actions
		 */
	}

	private void enableContextMenuActions() {

		final ITrackPath selectedTrack = Map3Manager.getTourTrackLayer().getSelectedTrack();
		final boolean isTourSelected = selectedTrack != null;

		_actionEditQuick.setEnabled(isTourSelected);
		_actionEditTour.setEnabled(isTourSelected);
		_actionOpenMarkerDialog.setEnabled(isTourSelected);
		_actionOpenAdjustAltitudeDialog.setEnabled(isTourSelected);
		_actionOpenTour.setEnabled(isTourSelected);
		_actionExportTour.setEnabled(isTourSelected);
		_actionPrintTour.setEnabled(isTourSelected);

	}

	private void fillActionBars() {

		/*
		 * fill view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(_actionTourColorAltitude);
		tbm.add(_actionTourColorPulse);
		tbm.add(_actionTourColorSpeed);
		tbm.add(_actionTourColorPace);
		tbm.add(_actionTourColorGradient);
		tbm.add(_actionTourColorHrZone);
		tbm.add(new Separator());

		tbm.add(_actionShowTourInMap3);
		tbm.add(_actionShowEntireTour);
		tbm.add(_actionSynMapViewWithTour);
//		tbm.add(_actionSynMapPositionWithSlider);
		tbm.add(new Separator());

		tbm.add(new Separator());

		tbm.add(_actionOpenMap3LayerView);
	}

	private void fillContextMenu(final Menu menu) {

		fillMenuItem(menu, _actionEditQuick);
		fillMenuItem(menu, _actionEditTour);
		fillMenuItem(menu, _actionOpenMarkerDialog);
		fillMenuItem(menu, _actionOpenAdjustAltitudeDialog);
		fillMenuItem(menu, _actionOpenTour);

//		_tagMenuMgr.fillTagMenu(menuMgr);
//
//		// tour type actions
//		fillMenuItem(menu, new Separator());
//		fillMenuItem(menu, _actionSetTourType);
//		TourTypeMenuManager.fillMenuWithRecentTourTypes(menuMgr, this, true);

		(new Separator()).fill(menu, -1);
		fillMenuItem(menu, _actionExportTour);
		fillMenuItem(menu, _actionPrintTour);

		// set color before menu is filled, this sets the action image and color id
		_actionMapColor.setColorId(_tourColorId);

		if (_tourColorId != MapColorId.HrZone) {

			// hr zone has a different color provider and is not yet supported

			(new Separator()).fill(menu, -1);
			fillMenuItem(menu, _actionMapColor);
		}

		enableContextMenuActions();
	}

	private void fillMenuItem(final Menu menu, final Action action) {

		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(menu, -1);
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		final ITrackPath selectedTrack = Map3Manager.getTourTrackLayer().getSelectedTrack();

		if (selectedTrack != null) {

			final ArrayList<TourData> selectedTours = new ArrayList<TourData>();

			selectedTours.add(selectedTrack.getTourTrack().getTourData());

			return selectedTours;
		}

		return null;
	}

	MapColorId getTrackColorId() {
		return _tourColorId;
	}

	private void onAWTMouseClick(final MouseEvent mouseEvent) {

		if (mouseEvent == null || mouseEvent.isConsumed()) {
			return;
		}

		final boolean isRightClick = SwingUtilities.isRightMouseButton(mouseEvent);
		if (isRightClick) {

			// open context menu

//			System.out.println(UI.timeStampNano()
//					+ " ["
//					+ getClass().getSimpleName()
//					+ "] \tRight_Click\t"
//					+ mouseEvent.getXOnScreen()
//					+ " : "
//					+ mouseEvent.getYOnScreen());
//			// TODO remove SYSTEM.OUT.PRINTLN

			_mapContainer.getDisplay().asyncExec(new Runnable() {

				public void run() {

//					System.out.println("SWT calling menu");

					createContextMenu(mouseEvent.getXOnScreen(), mouseEvent.getYOnScreen());
				}
			});
			mouseEvent.consume();
		}
	}

	private void onSelectionChanged(final ISelection selection) {

//		System.out.println(UI.timeStampNano() + " Map::onSelectionChanged\t" + selection);
//		// TODO remove SYSTEM.OUT.PRINTLN

		if (_isPartVisible == false || _isRestored == false) {

			if (selection instanceof SelectionTourData
					|| selection instanceof SelectionTourId
					|| selection instanceof SelectionTourIds) {

				// keep only selected tours
				_lastHiddenSelection = selection;
			}

			return;
		}

		final boolean isTourTrackVisible = Map3Manager.getTourTrackLayer().isEnabled();

		if (selection instanceof SelectionTourData) {

//			final SelectionTourData selectionTourData = (SelectionTourData) selection;
//			final TourData tourData = selectionTourData.getTourData();
//
//			paintTours_20_One(tourData, selectionTourData.isForceRedraw(), true);
//			paintPhotoSelection(selection);
//
//			enableActions();

		} else if (selection instanceof SelectionTourId) {

			if (isTourTrackVisible == false) {
				return;
			}

			final Long tourId = ((SelectionTourId) selection).getTourId();
			final TourData tourData = TourManager.getInstance().getTourData(tourId);

			showTour(tourData);
//			paintPhotoSelection(selection);

		} else if (selection instanceof SelectionTourIds) {

			// paint all selected tours

			if (isTourTrackVisible == false) {
				return;
			}

			final ArrayList<Long> tourIds = ((SelectionTourIds) selection).getTourIds();
			if (tourIds.size() == 0) {

				clearView();

				// history tour (without tours) is displayed

//				final ArrayList<Photo> allPhotos = paintPhotoSelection(selection);
//
//				if (allPhotos.size() > 0) {
//
////					centerPhotos(allPhotos, false);
//					showDefaultMap(true);
//
//					enableActions();
//				}

			} else if (tourIds.size() == 1) {

				// only 1 tour is displayed, synch with this tour !!!

				final TourData tourData = TourManager.getInstance().getTourData(tourIds.get(0));

				showTour(tourData);
//				paintTours_20_One(tourData, false, true);
//				paintPhotoSelection(selection);

			} else {

				// paint multiple tours

				showTours(tourIds);
//				paintPhotoSelection(selection);

//				enableActions(true);
			}

//		} else if (selection instanceof SelectionChartInfo) {
//
//			final ChartDataModel chartDataModel = ((SelectionChartInfo) selection).chartDataModel;
//			if (chartDataModel != null) {
//
//				final Object tourId = chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_ID);
//				if (tourId instanceof Long) {
//
//					TourData tourData = TourManager.getInstance().getTourData((Long) tourId);
//					if (tourData == null) {
//
//						// tour is not in the database, try to get it from the raw data manager
//
//						final HashMap<Long, TourData> rawData = RawDataManager.getInstance().getImportedTours();
//						tourData = rawData.get(tourId);
//					}
//
//					if (tourData != null) {
//
//						final SelectionChartInfo chartInfo = (SelectionChartInfo) selection;
//
//						paintTourSliders(
//								tourData,
//								chartInfo.leftSliderValuesIndex,
//								chartInfo.rightSliderValuesIndex,
//								chartInfo.selectedSliderValuesIndex);
//
//						enableActions();
//					}
//				}
//			}
//
//		} else if (selection instanceof SelectionChartXSliderPosition) {
//
//			final SelectionChartXSliderPosition xSliderPos = (SelectionChartXSliderPosition) selection;
//			final Chart chart = xSliderPos.getChart();
//			if (chart == null) {
//				return;
//			}
//
//			final ChartDataModel chartDataModel = chart.getChartDataModel();
//
//			final Object tourId = chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_ID);
//			if (tourId instanceof Long) {
//
//				final TourData tourData = TourManager.getInstance().getTourData((Long) tourId);
//				if (tourData != null) {
//
//					final int leftSliderValueIndex = xSliderPos.getLeftSliderValueIndex();
//					int rightSliderValueIndex = xSliderPos.getRightSliderValueIndex();
//
//					rightSliderValueIndex = rightSliderValueIndex == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION
//							? leftSliderValueIndex
//							: rightSliderValueIndex;
//
//					paintTourSliders(tourData, leftSliderValueIndex, rightSliderValueIndex, leftSliderValueIndex);
//
//					enableActions();
//				}
//			}
//
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
//			paintTourSliders(mapPositionSelection.getTourData(), valueIndex1, valueIndex2, valueIndex1);
//
//			enableActions();
//
//		} else if (selection instanceof PointOfInterest) {
//
//			_isTourOrWayPoint = false;
//
//			clearView();
//
//			final PointOfInterest poi = (PointOfInterest) selection;
//
//			_poiPosition = poi.getPosition();
//			_poiName = poi.getName();
//
//			_poiZoomLevel = poi.getRecommendedZoom();
//			if (_poiZoomLevel == -1) {
//				_poiZoomLevel = _map.getZoom();
//			}
//
//			_map.setPoi(_poiPosition, _poiZoomLevel, _poiName);
//
//			_actionShowPOI.setChecked(true);
//
//			enableActions();
//
//		} else if (selection instanceof StructuredSelection) {
//
//			final StructuredSelection structuredSelection = (StructuredSelection) selection;
//			final Object firstElement = structuredSelection.getFirstElement();
//
//			if (firstElement instanceof TVICatalogComparedTour) {
//
//				final TVICatalogComparedTour comparedTour = (TVICatalogComparedTour) firstElement;
//				final long tourId = comparedTour.getTourId();
//
//				final TourData tourData = TourManager.getInstance().getTourData(tourId);
//				paintTours_20_One(tourData, false, true);
//
//			} else if (firstElement instanceof TVICompareResultComparedTour) {
//
//				final TVICompareResultComparedTour compareResultItem = (TVICompareResultComparedTour) firstElement;
//				final TourData tourData = TourManager.getInstance().getTourData(
//						compareResultItem.getComparedTourData().getTourId());
//				paintTours_20_One(tourData, false, true);
//
//			} else if (firstElement instanceof TourWayPoint) {
//
//				final TourWayPoint wp = (TourWayPoint) firstElement;
//
//				_map.setPOI(_wayPointToolTipProvider, wp);
//			}
//
//			enableActions();
//
//		} else if (selection instanceof PhotoSelection) {
//
//			paintPhotos(((PhotoSelection) selection).galleryPhotos);
//
//			enableActions();
//
//		} else if (selection instanceof SelectionTourCatalogView) {
//
//			// show reference tour
//
//			final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;
//
//			final TVICatalogRefTourItem refItem = tourCatalogSelection.getRefItem();
//			if (refItem != null) {
//
//				final TourData tourData = TourManager.getInstance().getTourData(refItem.getTourId());
//
//				paintTours_20_One(tourData, false, true);
//
//				enableActions();
//			}
		}
	}

	void redraw() {

//		_mapContainer.getDisplay().asyncExec(new Runnable() {
//			public void run() {
//				_mapContainer.redraw();
//
//			}
//		});

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				_awtFrame.validate();
				_awtFrame.repaint();
			}
		});
	}

	private void restoreState() {

		final boolean isTourAvailable = _allTours.size() > 0;

		// sync map with tour
		_isSyncMapViewWithTour = Util.getStateBoolean(_state, STATE_IS_SYNC_MAP_VIEW_WITH_TOUR, true);
		_actionSynMapViewWithTour.setChecked(_isSyncMapViewWithTour);

		// sync map position with slider
		_isSyncMapPositionWithSlider = Util.getStateBoolean(_state, STATE_IS_SYNC_MAP_POSITION_WITH_SLIDER, false);
		_actionSynMapPositionWithSlider.setChecked(_isSyncMapPositionWithSlider);

		// is tour visible / available
		_isTourVisible = Util.getStateBoolean(_state, STATE_IS_TOUR_VISIBLE, true);
		_actionShowTourInMap3.setState(_isTourVisible, isTourAvailable);

		// tour color
		final String stateColorId = Util.getStateString(_state, STATE_TOUR_COLOR_ID, MapColorId.Altitude.name());

		try {
			_tourColorId = MapColorId.valueOf(stateColorId);
		} catch (final Exception e) {
			// set default
			_tourColorId = MapColorId.Altitude;
		}

		switch (_tourColorId) {
		case Altitude:
			_actionTourColorAltitude.setChecked(true);
			break;

		case Gradient:
			_actionTourColorGradient.setChecked(true);
			break;

		case Pace:
			_actionTourColorPace.setChecked(true);
			break;

		case Pulse:
			_actionTourColorPulse.setChecked(true);
			break;

		case Speed:
			_actionTourColorSpeed.setChecked(true);
			break;

		case HrZone:
			_actionTourColorHrZone.setChecked(true);
			break;

		default:
			_tourColorId = MapColorId.Altitude;
			_actionTourColorAltitude.setChecked(true);
			break;
		}

		setColorProvider(_tourColorId);

		// restore 3D view
		final String stateMap3View = Util.getStateString(_state, STATE_MAP3_VIEW, null);
		if (stateMap3View != null) {

			final View view = _wwCanvas.getView();
			view.restoreState(stateMap3View);

			view.firePropertyChange(AVKey.VIEW, null, view);
		}

	}

	private void saveState() {

		_state.put(STATE_IS_SYNC_MAP_POSITION_WITH_SLIDER, _isSyncMapPositionWithSlider);
		_state.put(STATE_IS_SYNC_MAP_VIEW_WITH_TOUR, _isSyncMapViewWithTour);
		_state.put(STATE_IS_TOUR_VISIBLE, _isTourVisible);

		_state.put(STATE_TOUR_COLOR_ID, _tourColorId.name());

		final View view = _wwCanvas.getView();

		_state.put(STATE_MAP3_VIEW, view.getRestorableState());
	}

	private void setColorProvider(final MapColorId colorId) {

		final IMapColorProvider colorProvider = TourMapColors.getColorProvider(colorId);

		Map3Manager.getTourTrackLayer().setColorProvider(colorProvider);
	}

	@Override
	public void setFocus() {

	}

	public void setSelection(final ISelection selection) {

		// run in SWT thread
		_mapContainer.getDisplay().asyncExec(new Runnable() {
			public void run() {

				if (_isPartActive == false) {

					// activate this view, otherwise the selection provider is not working

					Util.showView(ID, true);
				}

				_postSelectionProvider.setSelection(selection);
			}
		});

	}

	private void showAllTours() {

		showAllTours(_isSyncMapViewWithTour);
	}

	private void showAllTours(final ArrayList<TourData> allTours) {

		_allTours.clear();
		_allTours.addAll(allTours);

		showAllTours();
	}

	/**
	 * Shows all tours in the map which are set in {@link #_allTours}.
	 * 
	 * @param isSyncMapViewWithTour
	 */
	public void showAllTours(final boolean isSyncMapViewWithTour) {

		enableActions();

		final TourTrackLayer tourTrackLayer = Map3Manager.getTourTrackLayer();

		final ArrayList<TourMap3Position> allPositions = tourTrackLayer.createTrackPaths(_allTours);

		if (isSyncMapViewWithTour) {

			final Map3ViewController viewController = Map3ViewController.create(Map3Manager.getWWCanvas());
			viewController.goToDefaultView(allPositions);
		}

		_wwCanvas.redraw();
	}

	private void showTour(final TourData tourData) {

		final ArrayList<TourData> allTours = new ArrayList<TourData>();
		allTours.add(tourData);

		showAllTours(allTours);
	}

	private void showTours(final ArrayList<Long> allTourIds) {

		final ArrayList<TourData> allTourData = new ArrayList<TourData>();

		// load all tours
		final long newOverlayKey = TourManager.loadTourData(allTourIds, allTourData);

		showAllTours(allTourData);
	}

	private void showToursFromTourProvider() {

		Display.getCurrent().asyncExec(new Runnable() {
			@Override
			public void run() {

				// validate widget
				if (_mapContainer.isDisposed()) {
					return;
				}

				// check if tour is set from a selection provider
				if (_allTours.size() > 0) {
					return;
				}

				final ArrayList<TourData> allTours = TourManager.getSelectedTours();
				if (allTours != null) {
					showAllTours(allTours);
				}
			}
		});
	}

	private void updateMapColors() {

		Map3Manager.getTourTrackLayer().updateColors(_allTours);

		showAllTours(false);

//		_wwCanvas.redraw();

		Map3Manager.redraw();

	}

	private void updateModifiedTours(final ArrayList<TourData> modifiedTours) {

		_postSelectionProvider.clearSelection();

		_allTours.removeAll(modifiedTours);
		_allTours.addAll(modifiedTours);

		showAllTours();
	}

}
