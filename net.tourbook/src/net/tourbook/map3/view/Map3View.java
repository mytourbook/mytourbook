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
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.event.RenderingListener;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.GlobeAnnotation;
import gov.nasa.worldwind.util.PerformanceStatistic;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;

import javax.swing.SwingUtilities;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.common.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorUtil;
import net.tourbook.common.color.IGradientColors;
import net.tourbook.common.color.IMapColorProvider;
import net.tourbook.common.color.MapColorId;
import net.tourbook.common.color.MapLegendImageConfig;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.SWTPopupOverAWT;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.extension.export.ActionExport;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.map2.view.IDiscreteColors;
import net.tourbook.map2.view.TourMapColors;
import net.tourbook.map3.action.ActionMapColor;
import net.tourbook.map3.action.ActionOpenMap3LayerView;
import net.tourbook.map3.action.ActionShowChartSliderInMap;
import net.tourbook.map3.action.ActionShowDirectionArrows;
import net.tourbook.map3.action.ActionShowEntireTour;
import net.tourbook.map3.action.ActionShowLegendInMap3;
import net.tourbook.map3.action.ActionShowTourInMap3;
import net.tourbook.map3.action.ActionSyncMapWithChartSlider;
import net.tourbook.map3.action.ActionSyncMapWithTour;
import net.tourbook.map3.action.ActionTourColor;
import net.tourbook.map3.layer.ChartSliderLayer;
import net.tourbook.map3.layer.TourInfoLayer;
import net.tourbook.map3.layer.TrackPointAnnotation;
import net.tourbook.map3.layer.tourtrack.ITrackPath;
import net.tourbook.map3.layer.tourtrack.TourMap3Position;
import net.tourbook.map3.layer.tourtrack.TourTrackConfig;
import net.tourbook.map3.layer.tourtrack.TourTrackConfigManager;
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

	private static final String					SLIDER_TEXT_ALTITUDE					= "%.1f %s";								//$NON-NLS-1$
	private static final String					SLIDER_TEXT_GRADIENT					= "%.1f %%";								//$NON-NLS-1$
	private static final String					SLIDER_TEXT_PACE						= "%s %s";									//$NON-NLS-1$
	private static final String					SLIDER_TEXT_PULSE						= "%.0f %s";								//$NON-NLS-1$
	private static final String					SLIDER_TEXT_SPEED						= "%.1f %s";								//$NON-NLS-1$

	public static final String					ID										= "net.tourbook.map3.view.Map3ViewId";		//$NON-NLS-1$

	private static final String					STATE_IS_CHART_SLIDER_VISIBLE			= "STATE_IS_CHART_SLIDERVISIBLE";			//$NON-NLS-1$

	private static final String					STATE_IS_LEGEND_VISIBLE					= "STATE_IS_LEGEND_VISIBLE";				//$NON-NLS-1$
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
	private ActionShowChartSliderInMap			_actionShowChartSliderInMap;
	private ActionShowDirectionArrows			_actionShowDirectionArrows;
	private ActionShowEntireTour				_actionShowEntireTour;
	private ActionShowLegendInMap3				_actionShowLegendInMap;
	private ActionShowTourInMap3				_actionShowTourInMap3;
	private ActionSyncMapWithChartSlider		_actionSynMapWithChartSlider;
	private ActionSyncMapWithTour				_actionSynMapWithTour;
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

	private MouseAdapter						_wwMouseListener;
	private RenderingListener					_wwStatisticListener;

	private boolean								_isPartActive;
	private boolean								_isPartVisible;
	private boolean								_isRestored;

	private ISelection							_lastHiddenSelection;

	private boolean								_isSyncMapWithChartSlider;
	private boolean								_isSyncMapViewWithTour;
	private boolean								_isTourVisible;
	private boolean								_isLegendVisible;
	private boolean								_isChartSliderVisible;

	private int									_statisticUpdateInterval				= 500;
	private long								_statisticLastUpdate;

	/**
	 * Contains all tours which are displayed in the map.
	 */
	private ArrayList<TourData>					_allTours								= new ArrayList<TourData>();

	/**
	 * Color id for the currently displayed tour tracks.
	 */
	private MapColorId							_tourColorId;

	/*
	 * current position for the x-sliders
	 */
	private int									_currentLeftSliderValueIndex;
	private int									_currentRightSliderValueIndex;
	private int									_currentSelectedSliderValueIndex;
	//
	/*
	 * UI controls
	 */
	private Composite							_mapContainer;

	private Frame								_awtFrame;

	private Menu								_swtContextMenu;

	private ITrackPath							_previousHoveredTrack;
	private Integer								_previousHoveredTrackPosition;
	private Position							_previousMapSliderPosition;

	/**
	 * <code>true</code> will log frame/cache... statistics like in {@link PerformanceStatistic}
	 */
	private boolean								_isLogStatistics						= false;

	private int									_allTourIdHash;

	private int									_allTourDataHash;

	private class Map3ContextMenu extends SWTPopupOverAWT {

		public Map3ContextMenu(final Display display, final Menu swtContextMenu) {
			super(display, swtContextMenu);
		}

	}

	public Map3View() {}

	/**
	 * @param eyePosition
	 * @return Returns altitude offset depending on the configuration settings.
	 */
	public static double getAltitudeOffset(final Position eyePosition) {

		if (eyePosition == null) {
			return 0;
		}

		final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();

		final boolean isAbsoluteAltitudeMode = config.altitudeMode == WorldWind.ABSOLUTE;
		final boolean isAltitudeOffset = config.isAltitudeOffset;
		final boolean isOffsetModeAbsolute = config.altitudeOffsetMode == TourTrackConfigManager.ALTITUDE_OFFSET_MODE_ABSOLUTE;
		final boolean isOffsetModeRelative = config.altitudeOffsetMode == TourTrackConfigManager.ALTITUDE_OFFSET_MODE_RELATIVE;

		final int relativeOffset = config.altitudeOffsetDistanceRelative;

		double altitudeOffset = 0;

		if (isAbsoluteAltitudeMode && isAltitudeOffset) {

			if (isOffsetModeAbsolute) {

				altitudeOffset = config.altitudeOffsetDistanceAbsolute;

			} else if (isOffsetModeRelative && relativeOffset > 0) {

				final double eyeElevation = eyePosition.getElevation();

				altitudeOffset = eyeElevation / 100.0 * relativeOffset;
			}

			if (config.isAltitudeOffsetRandom) {

				// this needs to be implemented, is not yet done
			}
		}

		return altitudeOffset;
	}

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

	public void actionShowChartSlider(final boolean isVisible) {

		_isChartSliderVisible = isVisible;

		enableActions();

		Map3Manager.setLayerVisible_ChartSlider(isVisible);
	}

	public void actionShowDirectionArrows(final boolean isVisible) {

		final TourTrackLayer tourTrackLayer = Map3Manager.getLayer_TourTrack();
		final TourTrackConfig trackConfig = TourTrackConfigManager.getActiveConfig();

		trackConfig.isShowDirectionArrows = isVisible;

		// invalidate cached data and force a redraw
		tourTrackLayer.setExpired();
	}

	public void actionShowLegendInMap(final boolean isLegendVisible) {

		_isLegendVisible = isLegendVisible;

		Map3Manager.setLayerVisible_Legend(isLegendVisible);
	}

	public void actionShowTour(final boolean isTrackVisible) {

		_isTourVisible = isTrackVisible;

		Map3Manager.setLayerVisible_TourTrack(isTrackVisible);

		showAllTours_InternalTours();
	}

	public void actionSynchMapPositionWithSlider() {

		final boolean isSync = _actionSynMapWithChartSlider.isChecked();

		_isSyncMapWithChartSlider = isSync;

		if (isSync) {

			// ensure that the chart sliders are displayed

			_actionShowChartSliderInMap.setChecked(true);

//			actionShowChartSlider(true);
		}
	}

	public void actionSynchMapViewWithTour() {

		_isSyncMapViewWithTour = _actionSynMapWithTour.isChecked();

		if (_isSyncMapViewWithTour) {
			showAllTours_InternalTours();
		}
	}

	public void actionZoomShowEntireTour() {

		showAllTours(true);
	}

	private void addMap3Listener() {

		_wwMouseListener = new MouseAdapter() {

			@Override
			public void mouseClicked(final MouseEvent e) {
				onAWTMouseClick(e);
			}
		};

		_wwCanvas.getInputHandler().addMouseListener(_wwMouseListener);

		/*
		 * Statistics
		 */
		if (_isLogStatistics) {

			_wwCanvas.setPerFrameStatisticsKeys(PerformanceStatistic.ALL_STATISTICS_SET);
			_wwStatisticListener = new RenderingListener() {

				@Override
				public void stageChanged(final RenderingEvent event) {

					final long now = System.currentTimeMillis();

					final String stage = event.getStage();

					if (stage.equals(RenderingEvent.AFTER_BUFFER_SWAP)
							&& event.getSource() instanceof WorldWindow
							&& now - _statisticLastUpdate > _statisticUpdateInterval) {

						EventQueue.invokeLater(new Runnable() {
							public void run() {
								updateStatistics();
							}
						});

						_statisticLastUpdate = now;
					}
				}
			};

			_wwCanvas.addRenderingListener(_wwStatisticListener);
		}
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

						onSelection(_lastHiddenSelection);

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

				onSelection(selection);
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

					showAllTours_InternalTours();

				} else if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
					if ((modifiedTours != null) && (modifiedTours.size() > 0)) {
						updateModifiedTours(modifiedTours);
					}

				} else if (eventId == TourEventId.UPDATE_UI || eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

					clearView();

				} else if (eventId == TourEventId.SLIDER_POSITION_CHANGED) {
					onSelection((ISelection) eventData);
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void cleanupOldTours() {

		_previousMapSliderPosition = null;
		_previousHoveredTrack = null;
		_previousHoveredTrackPosition = null;

		_postSelectionProvider.clearSelection();

		_allTours.clear();
	}

	private void clearView() {

		cleanupOldTours();

		showAllTours_InternalTours();
	}

	/**
	 * Compute a center position from an eye position and an orientation. If the view is looking at
	 * the earth, the center position is the intersection point of the globe and a ray beginning at
	 * the eye point, in the direction of the forward vector. If the view is looking at the horizon,
	 * the center position is the eye position. Otherwise, the center position is null.
	 * 
	 * @param eyePosition
	 *            The eye position.
	 * @param forward
	 *            The forward vector.
	 * @param pitch
	 *            View pitch.
	 * @param altitudeMode
	 *            Altitude mode of {@code eyePosition}.
	 * @return The center position of the view.
	 */
	protected Position computeCenterPosition(	final Position eyePosition,
												final Vec4 forward,
												final Angle pitch,
												final int altitudeMode) {
		double height;

		final Angle latitude = eyePosition.getLatitude();
		final Angle longitude = eyePosition.getLongitude();

		final Globe globe = _wwCanvas.getModel().getGlobe();

		if (altitudeMode == WorldWind.CLAMP_TO_GROUND) {
			height = globe.getElevation(latitude, longitude);
		} else if (altitudeMode == WorldWind.RELATIVE_TO_GROUND) {
			height = globe.getElevation(latitude, longitude) + eyePosition.getAltitude();
		} else {
			height = eyePosition.getAltitude();
		}

		final Vec4 eyePoint = globe.computePointFromPosition(new Position(latitude, longitude, height));

		// Find the intersection of the globe and the camera's forward vector. Looking at the horizon (tilt == 90)
		// is a special case because it is a valid view, but the view vector does not intersect the globe.
		Position lookAtPosition;
		final double tolerance = 0.001;
		if (Math.abs(pitch.degrees - 90.0) > tolerance) {
			lookAtPosition = globe.getIntersectionPosition(new Line(eyePoint, forward));
		} else {
			lookAtPosition = globe.computePositionFromPoint(eyePoint);
		}

		return lookAtPosition;
	}

	private void createActions(final Composite parent) {

		_actionOpenMap3LayerView = new ActionOpenMap3LayerView();

		_actionMapColor = new ActionMapColor(this, _state);

		_actionShowChartSliderInMap = new ActionShowChartSliderInMap(this);
		_actionShowDirectionArrows = new ActionShowDirectionArrows(this);
		_actionShowEntireTour = new ActionShowEntireTour(this);
		_actionShowLegendInMap = new ActionShowLegendInMap3(this);
		_actionShowTourInMap3 = new ActionShowTourInMap3(this, parent);
		_actionSynMapWithChartSlider = new ActionSyncMapWithChartSlider(this);
		_actionSynMapWithTour = new ActionSyncMapWithTour(this);

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

					onSelection(_lastHiddenSelection);

					_lastHiddenSelection = null;

				} else if (_allTours.size() == 0) {

					// a tour is not displayed, find a tour provider which provides a tour
					showToursFromTourProvider();

				} else {

					showAllTours_InternalTours();
				}
			}
		});
	}

	private String createSliderText(final int positionIndex, final TourData tourData) {

		String graphValueText = null;

		switch (_tourColorId) {

		case Altitude:

			final float[] altitudeSerie = tourData.altitudeSerie;
			if (altitudeSerie != null) {

				final float altitudeMetric = altitudeSerie[positionIndex];
				final float altitude = altitudeMetric / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE;

				graphValueText = String.format(SLIDER_TEXT_ALTITUDE, altitude, UI.UNIT_LABEL_ALTITUDE);
			}
			break;

		case Gradient:

			final float[] gradientSerie = tourData.gradientSerie;
			if (gradientSerie != null) {
				graphValueText = String.format(SLIDER_TEXT_GRADIENT, gradientSerie[positionIndex]);
			}
			break;

		case Pace:

			final float[] paceSerie = tourData.getPaceSerie();
			if (paceSerie != null) {
				final float pace = paceSerie[positionIndex];
				graphValueText = String.format(
						SLIDER_TEXT_PACE,
						net.tourbook.ui.UI.format_mm_ss((long) pace),
						UI.UNIT_LABEL_PACE);
			}
			break;

		case HrZone:
		case Pulse:

			final float[] pulseSerie = tourData.pulseSerie;
			if (pulseSerie != null) {
				graphValueText = String.format(
						SLIDER_TEXT_PULSE,
						pulseSerie[positionIndex],
						Messages.Graph_Label_Heartbeat_unit);
			}

			break;

		case Speed:

			final float[] speedSerie = tourData.getSpeedSerie();
			if (speedSerie != null) {
				graphValueText = String.format(SLIDER_TEXT_SPEED, speedSerie[positionIndex], UI.UNIT_LABEL_SPEED);
			}
			break;

		default:
			break;
		}

		if (graphValueText != null) {
			return graphValueText;
		} else {
			return UI.EMPTY_STRING;
		}
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

		_awtFrame.addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(final ComponentEvent e) {
				Map3Manager.getLayer_TourLegend().resizeLegendImage();
			}
		});

//		_mapContainer.addControlListener(new ControlAdapter() {
//
//			@Override
//			public void controlResized(final ControlEvent e) {
//				Map3Manager.getTourLegendLayer().resizeLegendImage();
//			}
//
//		});

		parent.layout();
	}

	@Override
	public void dispose() {

		Map3Manager.setMap3View(null);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		getViewSite().getPage().removePostSelectionListener(_postSelectionListener);
		getViewSite().getPage().removePartListener(_partListener);

		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		_wwCanvas.getInputHandler().removeMouseListener(_wwMouseListener);

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

		final boolean isChartSliderVisible = Map3Manager.getLayer_ChartSlider().isEnabled();
		final boolean isLegendVisible = Map3Manager.getLayer_TourLegend().isEnabled();
		final boolean isTrackVisible = Map3Manager.getLayer_TourTrack().isEnabled();

		final boolean isTourAvailable = _allTours.size() > 0;

		_actionShowTourInMap3.setState(isTrackVisible, isTourAvailable);
		_actionSynMapWithChartSlider.setEnabled(isTourAvailable && _isChartSliderVisible);
		_actionSynMapWithTour.setEnabled(isTourAvailable);

		_actionShowLegendInMap.setChecked(isLegendVisible);
		_actionShowChartSliderInMap.setChecked(isChartSliderVisible);
	}

	private void enableContextMenuActions() {

		final ITrackPath selectedTrack = Map3Manager.getLayer_TourTrack().getSelectedTrack();
		final boolean isTourSelected = selectedTrack != null;
		final boolean isTourAvailable = _allTours.size() > 0;

		_actionShowChartSliderInMap.setEnabled(isTourAvailable);
		_actionShowLegendInMap.setEnabled(isTourAvailable);

		_actionMapColor.setEnabled(isTourAvailable);

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
		tbm.add(_actionSynMapWithTour);
		tbm.add(_actionSynMapWithChartSlider);
		tbm.add(new Separator());

		tbm.add(new Separator());

		tbm.add(_actionOpenMap3LayerView);
	}

	private void fillContextMenu(final Menu menu) {

		fillMenuItem(menu, _actionShowDirectionArrows);
		fillMenuItem(menu, _actionShowChartSliderInMap);
		fillMenuItem(menu, _actionShowLegendInMap);

		// set color before menu is filled, this sets the action image and color id
		_actionMapColor.setColorId(_tourColorId);

		if (_tourColorId != MapColorId.HrZone) {

			// hr zone has a different color provider and is not yet supported

			(new Separator()).fill(menu, -1);
			fillMenuItem(menu, _actionMapColor);
		}

		(new Separator()).fill(menu, -1);
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

		enableContextMenuActions();
	}

	private void fillMenuItem(final Menu menu, final Action action) {

		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(menu, -1);
	}

	public ArrayList<TourData> getAllTours() {
		return _allTours;
	}

	/**
	 * @return Returns {@link ChartSliderLayer} or <code>null</code> when layer is not displayed.
	 */
	private ChartSliderLayer getChartSliderLayer() {

		final ChartSliderLayer chartSliderLayer = Map3Manager.getLayer_ChartSlider();

		if (chartSliderLayer.isEnabled() == false) {
			// layer is not displayed
			return null;
		}

		return chartSliderLayer;
	}

	private float getDataSerieValue(final float[] dataSerie, final int positionIndex, final float legendMinValue) {

		final float legendValue = dataSerie == null ? legendMinValue : dataSerie[positionIndex];

		return legendValue;
	}

	public java.awt.Rectangle getMapSize() {
		return _wwCanvas.getBounds();
	}

	/**
	 * @param allTours
	 * @return Returns only tours which can be displayed in the map (which contains geo
	 *         coordinates).
	 */
	private ArrayList<TourData> getMapTours(final ArrayList<TourData> allTours) {

		final ArrayList<TourData> mapTours = new ArrayList<TourData>(allTours.size());

		for (final TourData tourData : allTours) {

			final double[] latitudeSerie = tourData.latitudeSerie;

			if (latitudeSerie != null && latitudeSerie.length > 0) {
				mapTours.add(tourData);
			}
		}

		return mapTours;
	}

	/**
	 * @param chartSliderLayer
	 * @return Returns {@link TourData} of the selected tour track or <code>null</code> when a tour
	 *         is not selected.
	 */
	private TourData getSelectedTour(final ChartSliderLayer chartSliderLayer) {

		TourData tourData;
		final ITrackPath selectedTrack = Map3Manager.getLayer_TourTrack().getSelectedTrack();

		if (selectedTrack == null) {

			if (_allTours.size() == 0) {

				return null;

			} else {

				// a track is not selected, get first tour

				tourData = _allTours.get(0);
			}

		} else {

			// get selected tour

			tourData = selectedTrack.getTourTrack().getTourData();
		}

		return tourData;
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		final ITrackPath selectedTrack = Map3Manager.getLayer_TourTrack().getSelectedTrack();

		if (selectedTrack != null) {

			final ArrayList<TourData> selectedTours = new ArrayList<TourData>();

			selectedTours.add(selectedTrack.getTourTrack().getTourData());

			return selectedTours;
		}

		return null;
	}

	private double getSliderYPosition(final float trackAltitude, final Position eyePosition) {

		final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();

		double sliderYPosition = 0;

		switch (config.altitudeMode) {
		case WorldWind.ABSOLUTE:

			sliderYPosition = trackAltitude + getAltitudeOffset(eyePosition);
			break;

		case WorldWind.RELATIVE_TO_GROUND:

			sliderYPosition = trackAltitude;
			break;

		default:

			// WorldWind.CLAMP_TO_GROUND -> y position = 0

			break;
		}

		return sliderYPosition;
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

	void onModifyConfig() {

		// altitude mode can have been changed, do a slider repositioning
		updateChartSlider();
	}

	private void onSelection(final ISelection selection) {

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

		final boolean isTourTrackVisible = Map3Manager.getLayer_TourTrack().isEnabled();

		if (selection instanceof SelectionTourData) {

			final SelectionTourData selectionTourData = (SelectionTourData) selection;
			final TourData tourData = selectionTourData.getTourData();

			showTour(tourData);

//			paintPhotoSelection(selection);

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

		} else if (selection instanceof SelectionChartInfo) {

			if (_isChartSliderVisible == false) {
				return;
			}

			final SelectionChartInfo chartInfo = (SelectionChartInfo) selection;

			final ChartDataModel chartDataModel = chartInfo.chartDataModel;
			if (chartDataModel != null) {

				final Object tourId = chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_ID);
				if (tourId instanceof Long) {

					syncMapWith_ChartSlider(chartInfo, (Long) tourId);
				}
			}

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

	private void restoreState() {

		final boolean isTourAvailable = _allTours.size() > 0;

		// sync map with tour
		_isSyncMapViewWithTour = Util.getStateBoolean(_state, STATE_IS_SYNC_MAP_VIEW_WITH_TOUR, true);
		_actionSynMapWithTour.setChecked(_isSyncMapViewWithTour);

		// sync map position with slider
		_isSyncMapWithChartSlider = Util.getStateBoolean(_state, STATE_IS_SYNC_MAP_POSITION_WITH_SLIDER, false);
		_actionSynMapWithChartSlider.setChecked(_isSyncMapWithChartSlider);

		// is tour visible / available
		_isTourVisible = Util.getStateBoolean(_state, STATE_IS_TOUR_VISIBLE, true);
		_actionShowTourInMap3.setState(_isTourVisible, isTourAvailable);

		// is legend visible
		_isLegendVisible = Util.getStateBoolean(_state, STATE_IS_LEGEND_VISIBLE, true);
		_actionShowLegendInMap.setChecked(_isLegendVisible);
		Map3Manager.setLayerVisible_Legend(_isLegendVisible);

		// is chart slider visible
		_isChartSliderVisible = Util.getStateBoolean(_state, STATE_IS_CHART_SLIDER_VISIBLE, true);
		_actionShowChartSliderInMap.setChecked(_isChartSliderVisible);
		Map3Manager.setLayerVisible_ChartSlider(_isChartSliderVisible);

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

		_actionShowDirectionArrows.setChecked(TourTrackConfigManager.getActiveConfig().isShowDirectionArrows);

		// restore 3D view
		final String stateMap3View = Util.getStateString(_state, STATE_MAP3_VIEW, null);
		if (stateMap3View != null) {

			final View view = _wwCanvas.getView();
			view.restoreState(stateMap3View);

			view.firePropertyChange(AVKey.VIEW, null, view);
		}

	}

	private void saveState() {

		/*
		 * It can happen that this view is not yet restored with restoreState() but the saveState()
		 * method is called which causes a NPE
		 */
		if (_tourColorId == null) {
			return;
		}

		_state.put(STATE_IS_CHART_SLIDER_VISIBLE, _isChartSliderVisible);
		_state.put(STATE_IS_LEGEND_VISIBLE, _isLegendVisible);
		_state.put(STATE_IS_SYNC_MAP_POSITION_WITH_SLIDER, _isSyncMapWithChartSlider);
		_state.put(STATE_IS_SYNC_MAP_VIEW_WITH_TOUR, _isSyncMapViewWithTour);
		_state.put(STATE_IS_TOUR_VISIBLE, _isTourVisible);

		_state.put(STATE_TOUR_COLOR_ID, _tourColorId.name());

		final View view = _wwCanvas.getView();

		_state.put(STATE_MAP3_VIEW, view.getRestorableState());
	}

	private void setAnnotationColors(final TourData tourData, final int positionIndex, final GlobeAnnotation trackPoint) {

		final Color bgColor;
		final Color fgColor;

		final IMapColorProvider colorProvider = TourMapColors.getColorProvider(_tourColorId);

		Integer colorValue = null;

		if (colorProvider instanceof IGradientColors) {

			final IGradientColors gradientColorProvider = (IGradientColors) colorProvider;

			final MapLegendImageConfig legendImageConfig = gradientColorProvider.getMapLegendImageConfig();
			final float legendMinValue = legendImageConfig.legendMinValue;

			float graphValue = legendMinValue;

			switch (_tourColorId) {

			case Altitude:

				graphValue = getDataSerieValue(tourData.altitudeSerie, positionIndex, legendMinValue);
				break;

			case Gradient:

				graphValue = getDataSerieValue(tourData.gradientSerie, positionIndex, legendMinValue);
				break;

			case Pace:

				graphValue = getDataSerieValue(tourData.getPaceSerie(), positionIndex, legendMinValue);
				break;

			case Pulse:

				graphValue = getDataSerieValue(tourData.pulseSerie, positionIndex, legendMinValue);
				break;

			case Speed:

				graphValue = getDataSerieValue(tourData.getSpeedSerie(), positionIndex, legendMinValue);
				break;

			default:
				break;
			}

			// get color according to the value
			colorValue = gradientColorProvider.getColorValue(graphValue);

		} else if (colorProvider instanceof IDiscreteColors) {

			final IDiscreteColors discreteColorProvider = (IDiscreteColors) colorProvider;

			colorValue = discreteColorProvider.getColorValue(tourData, positionIndex);
		}

		if (colorValue == null) {

			// set default color

			bgColor = null;
			fgColor = null;

		} else {

			final int red = (colorValue & 0xFF) >>> 0;
			final int green = (colorValue & 0xFF00) >>> 8;
			final int blue = (colorValue & 0xFF0000) >>> 16;

			bgColor = new Color(red, green, blue);
			fgColor = ColorUtil.getContrastColor(red, green, blue);
		}

		final AnnotationAttributes attributes = trackPoint.getAttributes();

		attributes.setBackgroundColor(bgColor);
		attributes.setTextColor(fgColor);
	}

	private void setAnnotationPosition(	final TrackPointAnnotation sliderAnnotation,
										final TourData tourData,
										final int positionIndex) {

		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;

		final double latitude = latitudeSerie[positionIndex];
		final double longitude = longitudeSerie[positionIndex];

		final float[] altitudeSerie = tourData.altitudeSerie;
		final float trackAltitude = altitudeSerie == null ? 0 : altitudeSerie[positionIndex];

		final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();

		final LatLon latLon = new LatLon(//
				Angle.fromDegrees(latitude),
				Angle.fromDegrees(longitude));

		sliderAnnotation.latLon = latLon;
		sliderAnnotation.trackAltitude = trackAltitude;

		sliderAnnotation.setAltitudeMode(config.altitudeMode);
	}

	private void setColorProvider(final MapColorId colorId) {

		final IMapColorProvider colorProvider = TourMapColors.getColorProvider(colorId);

		Map3Manager.getLayer_TourTrack().setColorProvider(colorProvider);
		Map3Manager.getLayer_TourLegend().setColorProvider(colorProvider);
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

	public void setTourInfo(final ITrackPath hoveredTrackPath, final Integer hoveredPositionIndex) {

		final TourInfoLayer tourInfoLayer = Map3Manager.getLayer_TourInfo();

		if (hoveredTrackPath == null) {

			// hide tour info

			_previousHoveredTrack = null;
			_previousHoveredTrackPosition = null;

			tourInfoLayer.setTrackPointVisible(false);

		} else {

			// a tour is hovered

			if (hoveredPositionIndex == null) {

				// a position is not hovered, keep tour info opened

			} else {

				// ckeck if a new position is hovered
				if (_previousHoveredTrack != null
						&& _previousHoveredTrack == hoveredTrackPath
						&& _previousHoveredTrackPosition != null
						&& _previousHoveredTrackPosition.intValue() == hoveredPositionIndex.intValue()) {

					return;
				}

//				System.out.println(UI.timeStampNano()
//						+ " ["
//						+ getClass().getSimpleName()
//						+ "] \thoveredTrackPath: "
//						+ hoveredTrackPath
//						+ "\thoveredPositionIndex: "
//						+ hoveredPositionIndex);
//				// TODO remove SYSTEM.OUT.PRINTLN

				// keep hovered position
				_previousHoveredTrack = hoveredTrackPath;
				_previousHoveredTrackPosition = hoveredPositionIndex;

				final TourData tourData = hoveredTrackPath.getTourTrack().getTourData();

				final TrackPointAnnotation trackPoint = tourInfoLayer.getTrackPoint();
				trackPoint.setText(createSliderText(hoveredPositionIndex, tourData));

				setAnnotationPosition(trackPoint, tourData, hoveredPositionIndex);
				setAnnotationColors(tourData, hoveredPositionIndex, trackPoint);

				tourInfoLayer.setTrackPointVisible(true);
			}
		}
	}

	/**
	 * Shows all tours in the map which are set in {@link #_allTours}.
	 * 
	 * @param isSyncMapViewWithTour
	 *            When <code>true</code> the map will zoomed and positioned to show all tours.
	 */
	public void showAllTours(final boolean isSyncMapViewWithTour) {

		enableActions();

		final TourTrackLayer tourTrackLayer = Map3Manager.getLayer_TourTrack();

		final ArrayList<TourMap3Position> allPositions = tourTrackLayer.createTrackPaths(_allTours);

		Map3Manager.getLayer_TourLegend().updateLegendImage();

		showAllTours_Final(isSyncMapViewWithTour, allPositions);
	}

	private void showAllTours_Final(final boolean isSyncMapViewWithTour, final ArrayList<TourMap3Position> allPositions) {

		syncMapWith_Tour(isSyncMapViewWithTour, allPositions);

		updateChartSlider();

		Map3Manager.redrawMap();
	}

	private void showAllTours_InternalTours() {

		showAllTours(_isSyncMapViewWithTour);
	}

	private void showAllTours_NewTours(final ArrayList<TourData> allTours) {

		// check if new tours are already displayed
		if (allTours.hashCode() == _allTours.hashCode()) {
			return;
		}

		cleanupOldTours();

		_allTours.addAll(getMapTours(allTours));

		showAllTours_InternalTours();
	}

	private void showTour(final TourData newTourData) {

		if (newTourData == null) {
			return;
		}

		// check if this tour is already displayed
		for (final TourData existingTour : _allTours) {
			if (newTourData.equals(existingTour)) {

				/*
				 * the new tour is already displayed in the map, just select it but only when
				 * multiple tours are displayed, otherwise one tour gets selected which is a very
				 * annoying
				 */

				if (_allTours.size() > 1) {

					final TourTrackLayer tourTrackLayer = Map3Manager.getLayer_TourTrack();
					final ArrayList<TourMap3Position> trackPositions = tourTrackLayer.selectTrackPath(newTourData);

					if (trackPositions == null) {
						// track is already selected
						return;
					}

					showAllTours_Final(_isSyncMapViewWithTour, trackPositions);
				}

				return;
			}
		}

		final ArrayList<TourData> allTours = new ArrayList<TourData>();
		allTours.add(newTourData);

		showAllTours_NewTours(allTours);
	}

	private void showTours(final ArrayList<Long> allTourIds) {

		if (allTourIds.hashCode() != _allTourIdHash || _allTours.hashCode() != _allTourDataHash) {

			// tour data needs to be loaded

			final ArrayList<TourData> allTourData = new ArrayList<TourData>();

			TourManager.loadTourData(allTourIds, allTourData);

			_allTourIdHash = allTourIds.hashCode();
			_allTourDataHash = allTourData.hashCode();

			showAllTours_NewTours(allTourData);

		} else {

			showAllTours_NewTours(_allTours);
		}
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
					showAllTours_NewTours(allTours);
				}
			}
		});
	}

	private void syncMapWith_ChartSlider(final SelectionChartInfo chartInfo, final Long tourId) {

		TourData tourData = TourManager.getInstance().getTourData(tourId);
		if (tourData == null) {

			// tour is not in the database, try to get it from the raw data manager

			final HashMap<Long, TourData> rawData = RawDataManager.getInstance().getImportedTours();
			tourData = rawData.get(tourId);
		}

		final ChartSliderLayer chartSliderLayer = getChartSliderLayer();

		if (tourData == null || tourData.latitudeSerie == null) {

			chartSliderLayer.setSliderVisible(false);

		} else {

			// sync map with chart slider

			final int valuesIndex = chartInfo.selectedSliderValuesIndex;

			final double latitude = tourData.latitudeSerie[valuesIndex];
			final double longitude = tourData.longitudeSerie[valuesIndex];

			final float[] altitudeSerie = tourData.altitudeSerie;

			final View view = _wwCanvas.getView();
			if (view instanceof BasicOrbitView) {

				final BasicOrbitView orbitView = (BasicOrbitView) view;
				final Position eyePos = orbitView.getCurrentEyePosition();

				final float trackAltitude = altitudeSerie == null ? 0 : altitudeSerie[valuesIndex];
				final double elevation = getSliderYPosition(trackAltitude, eyePos);
				final LatLon sliderDegrees = LatLon.fromDegrees(latitude, longitude);

				/*
				 * Prevent setting the same location because this will jitter the map slider and is
				 * unnecessary.
				 */
				if (_previousMapSliderPosition != null) {

					if (_previousMapSliderPosition.getLatitude().equals(sliderDegrees.latitude)
							&& _previousMapSliderPosition.getLongitude().equals(sliderDegrees.longitude)
							&& _previousMapSliderPosition.elevation == elevation) {

						return;
					}
				}

				chartSliderLayer.setSliderVisible(false);

				final Position mapSliderPosition = new Position(sliderDegrees, elevation);

				/*
				 * This fragment is copied from
				 * gov.nasa.worldwindx.applications.sar.AnalysisPanel.updateView(boolean)
				 */

				// Send a message to stop all changes to the view's center position.
//					orbitView.stopMovementOnCenter();

				// Set the view to center on the track position,
				// while keeping the eye altitude constant.
				try {

					// New eye lat/lon will follow the ground position.
					final LatLon newEyeLatLon = eyePos.add(mapSliderPosition.subtract(orbitView.getCenterPosition()));

					// Eye elevation will not change unless it is below the ground position elevation.
					final double newEyeElev = eyePos.getElevation() < mapSliderPosition.getElevation() //
							? mapSliderPosition.getElevation()
							: eyePos.getElevation();

					final Position newEyePos = new Position(newEyeLatLon, newEyeElev);

					if (_isSyncMapWithChartSlider) {
						orbitView.setOrientation(newEyePos, mapSliderPosition);
					}

					// keep current position
					_previousMapSliderPosition = mapSliderPosition;
				}

				// Fallback to setting center position.
				catch (final Exception e) {

					if (_isSyncMapWithChartSlider) {

						orbitView.setCenterPosition(mapSliderPosition);
						// View/OrbitView will have logged the exception, no need to log it here.
					}
				}
			}

			// update slider UI
			updateChartSlider_10_Position(
					chartSliderLayer,
					tourData,
					chartInfo.leftSliderValuesIndex,
					chartInfo.rightSliderValuesIndex,
					valuesIndex);

			chartSliderLayer.setSliderVisible(true);

			enableActions();
		}
	}

	private void syncMapWith_Tour(final boolean isSyncMapViewWithTour, final ArrayList<TourMap3Position> allPositions) {

		if (isSyncMapViewWithTour) {

			final Map3ViewController viewController = Map3ViewController.create(_wwCanvas);

			viewController.goToDefaultView(allPositions);
		}
	}

	private void updateChartSlider() {

		final ChartSliderLayer chartSliderLayer = getChartSliderLayer();
		if (chartSliderLayer == null) {
			return;
		}

		final TourData tourData = getSelectedTour(chartSliderLayer);
		if (tourData == null) {
			chartSliderLayer.setSliderVisible(false);
			return;
		}

		chartSliderLayer.setSliderVisible(true);

		updateChartSlider_10_Position(
				chartSliderLayer,
				tourData,
				_currentLeftSliderValueIndex,
				_currentRightSliderValueIndex,
				_currentSelectedSliderValueIndex);
	}

	private void updateChartSlider_10_Position(	final ChartSliderLayer chartSliderLayer,
												final TourData tourData,
												int leftPosIndex,
												int rightPosIndex,
												int selectedPosIndex) {

		final double[] latitudeSerie = tourData.latitudeSerie;

		final int lastIndex = latitudeSerie.length - 1;

		// check array bounds
		if (leftPosIndex < 0 || leftPosIndex > lastIndex) {
			leftPosIndex = 0;
		}
		if (rightPosIndex < 0 || rightPosIndex > lastIndex) {
			rightPosIndex = lastIndex;
		}
		if (selectedPosIndex < 0 || selectedPosIndex > lastIndex) {
			selectedPosIndex = lastIndex;
		}

		_currentLeftSliderValueIndex = leftPosIndex;
		_currentRightSliderValueIndex = rightPosIndex;
		_currentSelectedSliderValueIndex = selectedPosIndex;

		updateChartSlider_20_Data(//
				chartSliderLayer.getLeftSlider(),
				_currentLeftSliderValueIndex,
				tourData,
				true);

		updateChartSlider_20_Data(//
				chartSliderLayer.getRightSlider(),
				_currentRightSliderValueIndex,
				tourData,
				false);

		Map3Manager.redrawMap();
	}

	private void updateChartSlider_20_Data(	final TrackPointAnnotation slider,
											final int positionIndex,
											final TourData tourData,
											final boolean isLeftSlider) {

		/*
		 * set position and text
		 */
		setAnnotationPosition(slider, tourData, positionIndex);
		setAnnotationColors(tourData, positionIndex, slider);

		slider.setText(createSliderText(positionIndex, tourData));
	}

	private void updateMapColors() {

		Map3Manager.getLayer_TourTrack().updateColors(_allTours);
//		Map3Manager.getLayer_TourLegend().updateLegendImage();

		showAllTours(false);
	}

//	public static final String	ALL						= "gov.nasa.worldwind.perfstat.All";
//
//	public static final String	FRAME_RATE				= "gov.nasa.worldwind.perfstat.FrameRate";
//	public static final String	FRAME_TIME				= "gov.nasa.worldwind.perfstat.FrameTime";
//	public static final String	PICK_TIME				= "gov.nasa.worldwind.perfstat.PickTime";
//
//	public static final String	TERRAIN_TILE_COUNT		= "gov.nasa.worldwind.perfstat.TerrainTileCount";
//	public static final String	IMAGE_TILE_COUNT		= "gov.nasa.worldwind.perfstat.ImageTileCount";
//
//	public static final String	AIRSPACE_GEOMETRY_COUNT	= "gov.nasa.worldwind.perfstat.AirspaceGeometryCount";
//	public static final String	AIRSPACE_VERTEX_COUNT	= "gov.nasa.worldwind.perfstat.AirspaceVertexCount";
//
//	public static final String	JVM_HEAP				= "gov.nasa.worldwind.perfstat.JvmHeap";
//	public static final String	JVM_HEAP_USED			= "gov.nasa.worldwind.perfstat.JvmHeapUsed";
//
//	public static final String	MEMORY_CACHE			= "gov.nasa.worldwind.perfstat.MemoryCache";
//	public static final String	TEXTURE_CACHE			= "gov.nasa.worldwind.perfstat.TextureCache";

//2013-10-06 10:12:12.317'955 [Map3View] 	         0  Frame Rate (fps)
//2013-10-06 10:12:12.317'982 [Map3View] 	       152  Frame Time (ms)
//2013-10-06 10:12:12.318'366 [Map3View] 	         8  Pick Time (ms)

//2013-10-06 10:12:12.318'392 [Map3View] 	        91  Terrain Tiles

//2013-10-06 10:12:12.318'169 [Map3View] 	      6252  Cache Size (Kb): Terrain
//2013-10-06 10:12:12.318'191 [Map3View] 	         0  Cache Size (Kb): Placename Tiles
//2013-10-06 10:12:12.318'214 [Map3View] 	      1860  Cache Size (Kb): Texture Tiles
//2013-10-06 10:12:12.318'289 [Map3View] 	      3870  Cache Size (Kb): Elevation Tiles
//2013-10-06 10:12:12.318'414 [Map3View] 	    422617  Texture Cache size (Kb)

//2013-10-06 10:12:12.318'007 [Map3View] 	         2  Blue Marble (WMS) 2004 Tiles
//2013-10-06 10:12:12.318'044 [Map3View] 	        35  i-cubed Landsat Tiles
//2013-10-06 10:12:12.318'069 [Map3View] 	        84  MS Virtual Earth Aerial Tiles
//2013-10-06 10:12:12.318'093 [Map3View] 	        84  Bing Imagery Tiles

//2013-10-06 10:12:12.318'118 [Map3View] 	    463659  JVM total memory (Kb)
//2013-10-06 10:12:12.318'141 [Map3View] 	    431273  JVM used memory (Kb)

	private void updateModifiedTours(final ArrayList<TourData> modifiedTours) {

		// cleanup old tours, this method cannot be used: cleanupOldTours();
		_postSelectionProvider.clearSelection();
		_previousMapSliderPosition = null;

		_allTours.removeAll(modifiedTours);
		_allTours.addAll(getMapTours(modifiedTours));

		showAllTours_InternalTours();
	}

	private void updateStatistics() {

		final Collection<PerformanceStatistic> stats = _wwCanvas.getSceneController().getPerFrameStatistics();

		if (stats.size() < 1) {
			return;
		}

		final PerformanceStatistic[] pfs = stats.toArray(new PerformanceStatistic[stats.size()]);
		Arrays.sort(pfs, new Comparator<PerformanceStatistic>() {

			@Override
			public int compare(final PerformanceStatistic o1, final PerformanceStatistic o2) {

				// sort stats by key to group them by the same type
				return o1.getKey().compareTo(o2.getKey());
			}
		});

		System.out.println(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] \t"); //$NON-NLS-1$ //$NON-NLS-2$
		System.out.println(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] \t"); //$NON-NLS-1$ //$NON-NLS-2$

		for (final PerformanceStatistic stat : pfs) {
			System.out.println(UI.timeStampNano() + " [" //$NON-NLS-1$
					+ getClass().getSimpleName()
					+ "] \t" //$NON-NLS-1$
					+ String.format("%10s  %s", stat.getValue(), stat.getDisplayString())); //$NON-NLS-1$
//					+ String.format("%-40s%10s", stat.getDisplayString(), stat.getValue()));
		}
	}

}
