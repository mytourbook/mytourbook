/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartType;
import net.tourbook.chart.ComputeChartValue;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.util.MtMath;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringToArrayConverter;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.database.MyTourbookException;
import net.tourbook.database.TourDatabase;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageViews;
import net.tourbook.search.FTSearchManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.ITourProviderAll;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartConfiguration;
import net.tourbook.ui.tourChart.TourChartView;
import net.tourbook.ui.tourChart.X_AXIS_START_TIME;
import net.tourbook.ui.views.TourChartAnalyzerInfo;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.joda.time.DateTime;

public class TourManager {

	private static final String				GRAPH_LABEL_ALTIMETER					= net.tourbook.common.Messages.Graph_Label_Altimeter;
	private static final String				GRAPH_LABEL_ALTITUDE					= net.tourbook.common.Messages.Graph_Label_Altitude;
	private static final String				GRAPH_LABEL_CADENCE						= net.tourbook.common.Messages.Graph_Label_Cadence;
	private static final String				GRAPH_LABEL_CADENCE_UNIT				= net.tourbook.common.Messages.Graph_Label_Cadence_Unit;
	private static final String				GRAPH_LABEL_GEARS						= net.tourbook.common.Messages.Graph_Label_Gears;
	private static final String				GRAPH_LABEL_GRADIENT					= net.tourbook.common.Messages.Graph_Label_Gradient;
	private static final String				GRAPH_LABEL_GRADIENT_UNIT				= net.tourbook.common.Messages.Graph_Label_Gradient_Unit;
	private static final String				GRAPH_LABEL_HEARTBEAT					= net.tourbook.common.Messages.Graph_Label_Heartbeat;
	private static final String				GRAPH_LABEL_HEARTBEAT_UNIT				= net.tourbook.common.Messages.Graph_Label_Heartbeat_Unit;
	private static final String				GRAPH_LABEL_PACE						= net.tourbook.common.Messages.Graph_Label_Pace;
	private static final String				GRAPH_LABEL_POWER						= net.tourbook.common.Messages.Graph_Label_Power;
	private static final String				GRAPH_LABEL_POWER_UNIT					= net.tourbook.common.Messages.Graph_Label_Power_Unit;
	private static final String				GRAPH_LABEL_SPEED						= net.tourbook.common.Messages.Graph_Label_Speed;
	private static final String				GRAPH_LABEL_TEMPERATURE					= net.tourbook.common.Messages.Graph_Label_Temperature;
	private static final String				GRAPH_LABEL_TOUR_COMPARE				= net.tourbook.common.Messages.Graph_Label_Tour_Compare;
	private static final String				GRAPH_LABEL_TOUR_COMPARE_UNIT			= net.tourbook.common.Messages.Graph_Label_Tour_Compare_Unit;

	public static final String				CUSTOM_DATA_TOUR_ID						= "tourId";													//$NON-NLS-1$
	public static final String				CUSTOM_DATA_TOUR_DATA					= "tourData";													//$NON-NLS-1$
	public static final String				CUSTOM_DATA_TOUR_CHART_CONFIGURATION	= "tourChartConfig";											//$NON-NLS-1$

	public static final String				CUSTOM_DATA_ALTIMETER					= "altimeter";													//$NON-NLS-1$
	public static final String				CUSTOM_DATA_ALTITUDE					= "altitude";													//$NON-NLS-1$
	public static final String				CUSTOM_DATA_CADENCE						= "cadence";													//$NON-NLS-1$
	public static final String				CUSTOM_DATA_DISTANCE					= "distance";													//$NON-NLS-1$
	public static final String				CUSTOM_DATA_GEAR_RATIO					= "gearRatio";													//$NON-NLS-1$
	public static final String				CUSTOM_DATA_GRADIENT					= "gradient";													//$NON-NLS-1$
	public static final String				CUSTOM_DATA_HISTORY						= "history";													//$NON-NLS-1$
	public static final String				CUSTOM_DATA_PACE						= "pace";														//$NON-NLS-1$
	public static final String				CUSTOM_DATA_POWER						= "power";														//$NON-NLS-1$
	public static final String				CUSTOM_DATA_PULSE						= "pulse";														//$NON-NLS-1$
	public static final String				CUSTOM_DATA_SPEED						= "speed";														//$NON-NLS-1$
	public static final String				CUSTOM_DATA_TEMPERATURE					= "temperature";												//$NON-NLS-1$
	public static final String				CUSTOM_DATA_TIME						= "time";														//$NON-NLS-1$

	public static final String				CUSTOM_DATA_SEGMENT_VALUES				= "segmentValues";												//$NON-NLS-1$
	public static final String				CUSTOM_DATA_ANALYZER_INFO				= "analyzerInfo";												//$NON-NLS-1$
	public static final String				CUSTOM_DATA_CONCONI_TEST				= "CUSTOM_DATA_CONCONI_TEST";									//$NON-NLS-1$

	public static final String				X_AXIS_TIME								= "time";														//$NON-NLS-1$
	public static final String				X_AXIS_DISTANCE							= "distance";													//$NON-NLS-1$

	public static final String				GEAR_TEETH_FORMAT						= "%2d:%2d";													//$NON-NLS-1$
	public static final String				GEAR_VALUE_FORMAT						= GEAR_TEETH_FORMAT + " - %1.2f";								//$NON-NLS-1$

	public static final int					GRAPH_ALTITUDE							= 1000;
	public static final int					GRAPH_SPEED								= 1001;
	public static final int					GRAPH_ALTIMETER							= 1002;
	public static final int					GRAPH_PULSE								= 1003;
	public static final int					GRAPH_TEMPERATURE						= 1004;
	public static final int					GRAPH_CADENCE							= 1005;
	public static final int					GRAPH_GRADIENT							= 1006;
	public static final int					GRAPH_POWER								= 1007;
	public static final int					GRAPH_PACE								= 1008;
	public static final int					GRAPH_GEARS								= 1009;
	public static final int					GRAPH_TOUR_COMPARE						= 2000;

	private static final int[]				_allGraphIDs							= new int[] {
			GRAPH_ALTITUDE,
			GRAPH_SPEED,
			GRAPH_ALTIMETER,
			GRAPH_PULSE,
			GRAPH_TEMPERATURE,
			GRAPH_CADENCE,
			GRAPH_GRADIENT,
			GRAPH_POWER,
			GRAPH_PACE,
			GRAPH_GEARS,
			GRAPH_TOUR_COMPARE														};

	private static TourManager				_instance;

	private final static IPreferenceStore	_prefStore								= TourbookPlugin.getPrefStore();

	/**
	 * contains the instance of the {@link TourDataEditorView} or <code>null</code> when this part
	 * is not opened
	 */
	private static TourDataEditorView		_tourDataEditorInstance;

	private ComputeChartValue				_computeAltimeterAvg;
	private ComputeChartValue				_computeGradientAvg;
	private ComputeChartValue				_computePaceAvg;
	private ComputeChartValue				_computePowerAvg;
	private ComputeChartValue				_computeSpeedAvg;

	private final TourDataCache				_tourDataCache;

	private static final ListenerList		_tourEventListeners						= new ListenerList(
																							ListenerList.IDENTITY);
	private static final ListenerList		_tourSaveListeners						= new ListenerList(
																							ListenerList.IDENTITY);

	/**
	 * tour chart which shows the selected tour
	 */
	private TourChart						_activeTourChart;

	private TourManager() {

		final int cacheSize = _prefStore.getInt(ITourbookPreferences.TOUR_CACHE_SIZE);
		if (cacheSize > 0) {

			_tourDataCache = new TourDataCache(cacheSize);

		} else {

			/**
			 * VERY IMPORTANT
			 * <p>
			 * When cache size is 0, each time when the same tour is requested from the tour
			 * manager, a new TourData entity is created. So each opened view gets a new tourdata
			 * for the same tour which causes a LOT of troubles.
			 */
			_tourDataCache = new TourDataCache(10);
		}

		createAvgCallbacks();

		_prefStore.addPropertyChangeListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				if (event.getProperty().equals(ITourbookPreferences.CLEAR_TOURDATA_CACHE)) {

					clearTourDataCache();

					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {

							// fire modify event
							TourManager.fireEvent(TourEventId.UPDATE_UI);
						}
					});
				}
			}
		});
	}

	/**
	 * Compares two {@link TourData}
	 * 
	 * @param tourData1
	 * @param tourData2
	 * @return Returns <code>true</code> when they are the same, otherwise this is an internal error
	 * @throws MyTourbookException
	 *             throws an exception when {@link TourData} are corrupted
	 */
	public static boolean checkTourData(final TourData tourData1, final TourData tourData2) throws MyTourbookException {

		if (tourData1 == null || tourData2 == null) {
			return true;
		}

		final long tourId1 = tourData1.getTourId().longValue();
		final long tourId2 = tourData2.getTourId().longValue();

		if (tourId1 == tourId2 && tourData1 != tourData2) {

			final StringBuilder sb = new StringBuilder()//
					.append("ERROR: ") //$NON-NLS-1$
					.append("The internal structure of the application is out of synch.") //$NON-NLS-1$
					.append(UI.NEW_LINE2)
					.append("You can solve the problem by:") //$NON-NLS-1$
					.append(UI.NEW_LINE2)
					.append("- restarting the application") //$NON-NLS-1$
					.append(UI.NEW_LINE)
					.append("- close the tour editor in all perspectives") //$NON-NLS-1$
					.append(UI.NEW_LINE)
					.append("- save/revert tour and select another tour") //$NON-NLS-1$
					.append(UI.NEW_LINE2)
					.append(UI.NEW_LINE)
					.append("The tour editor contains the selected tour, but the data are different.") //$NON-NLS-1$
					.append(UI.NEW_LINE2)
					.append("Tour in Editor:") //$NON-NLS-1$
					.append(tourData2.toStringWithHash())
					.append(UI.NEW_LINE)
					.append("Selected Tour:") //$NON-NLS-1$
					.append(tourData1.toStringWithHash())
					.append(UI.NEW_LINE2)
					.append(UI.NEW_LINE)
					.append("You should also inform the author of the application how this error occured. ") //$NON-NLS-1$
					.append(
							"However it isn't very easy to find out, what actions are exactly done, before this error occured. ") //$NON-NLS-1$
					.append(UI.NEW_LINE2)
					.append("These actions must be reproducable otherwise the bug cannot be identified."); //$NON-NLS-1$

			MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error: Out of Synch", sb.toString()); //$NON-NLS-1$

			throw new MyTourbookException(sb.toString());
		}

		return true;
	}

	/**
	 * Computes distance values from geo position.
	 * 
	 * @param tourDataList
	 * @return Returns <code>true</code> when distance values are computed and {@link TourData} are
	 *         updated but not yet saved.
	 */
	public static boolean computeDistanceValuesFromGeoPosition(final ArrayList<TourData> tourDataList) {

		if (tourDataList == null || tourDataList.size() == 0) {
			return false;
		}

		if (MessageDialog.openConfirm(
				Display.getCurrent().getActiveShell(),
				Messages.TourEditor_Dialog_ComputeDistanceValues_Title,
				NLS.bind(Messages.TourEditor_Dialog_ComputeDistanceValues_Message, UI.UNIT_LABEL_DISTANCE)) == false) {
			return false;
		}

		final boolean[] retValue = { false };

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			@Override
			public void run() {

				for (final TourData tourData : tourDataList) {

					final boolean isComputed = computeDistanceValuesFromGeoPosition(tourData);

					if (isComputed) {
						retValue[0] = true;
					}
				}
			}
		});

		return retValue[0];
	}

	public static boolean computeDistanceValuesFromGeoPosition(final TourData tourData) {

		final double[] latSerie = tourData.latitudeSerie;
		final double[] lonSerie = tourData.longitudeSerie;

		if (latSerie == null) {
			return false;
		}

		final float[] distanceSerie = new float[latSerie.length];

		double distance = 0;
		double latStart = latSerie[0];
		double lonStart = lonSerie[0];

		// compute distance for every time slice
		for (int serieIndex = 1; serieIndex < latSerie.length; serieIndex++) {

			final double latEnd = latSerie[serieIndex];
			final double lonEnd = lonSerie[serieIndex];

			/*
			 * vincenty algorithm is much more accurate compared with haversine
			 */
//			final double distDiff = Util.distanceHaversine(latStart, lonStart, latEnd, lonEnd);
			final double distDiff = MtMath.distanceVincenty(latStart, lonStart, latEnd, lonEnd);

			distance += distDiff;
			distanceSerie[serieIndex] = (float) distance;

			latStart = latEnd;
			lonStart = lonEnd;
		}

		// update tour distance which is displayed in views/tour editor
		tourData.setTourDistance((int) distance);

		// set distance in markers
		final Set<TourMarker> allTourMarker = tourData.getTourMarkers();
		if (allTourMarker != null) {

			for (final TourMarker tourMarker : allTourMarker) {
				final float markerDistance = distanceSerie[tourMarker.getSerieIndex()];
				tourMarker.setDistance(markerDistance);
			}
		}

		tourData.distanceSerie = distanceSerie;

		return true;
	}

	public static float computeTourSpeed(final TourData tourData, final int startIndex, final int endIndex) {

		final float[] distanceSerie = tourData.getMetricDistanceSerie();
		final int[] timeSerie = tourData.timeSerie;

		if (timeSerie == null
				|| timeSerie.length == 0
				|| startIndex >= distanceSerie.length
				|| endIndex >= distanceSerie.length) {
			return 0;
		}

		final float distance = distanceSerie[endIndex] - distanceSerie[startIndex];
		final int time = Math.max(
				0,
				timeSerie[endIndex] - timeSerie[startIndex] - tourData.getBreakTime(startIndex, endIndex));

		return distance / time * 3.6f;
	}

	/**
	 * Create a tour chart configuration by reading the settings from the pref store.
	 * 
	 * @return Returns a new tour chart configuration.
	 */
	public static TourChartConfiguration createDefaultTourChartConfig() {

		final TourChartConfiguration tcc = new TourChartConfiguration(true);

		/*
		 * convert graph ids from the preferences into visible graphs in the chart panel
		 * configuration
		 */
		final String[] prefGraphIds = StringToArrayConverter.convertStringToArray(//
				_prefStore.getString(ITourbookPreferences.GRAPH_VISIBLE));

		for (final String prefGraphId : prefGraphIds) {
			tcc.addVisibleGraph(Integer.parseInt(prefGraphId));
		}

		tcc.isHrZoneDisplayed = _prefStore.getBoolean(//
				ITourbookPreferences.GRAPH_IS_HR_ZONE_BACKGROUND_VISIBLE);

		tcc.hrZoneStyle = _prefStore.getString(ITourbookPreferences.GRAPH_HR_ZONE_STYLE);

		// set the unit which is shown on the x-axis
		final boolean isShowTime = _prefStore.getString(ITourbookPreferences.GRAPH_X_AXIS).equals(X_AXIS_TIME);
		tcc.isShowTimeOnXAxis = isShowTime;
		tcc.isShowTimeOnXAxisBackup = isShowTime;

		final boolean isTourStartTime = _prefStore.getBoolean(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME);
		tcc.xAxisTime = isTourStartTime ? X_AXIS_START_TIME.TOUR_START_TIME : X_AXIS_START_TIME.START_WITH_0;
		tcc.isSRTMDataVisible = _prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_SRTM_VISIBLE);

		tcc.isGraphOverlapped = _prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_GRAPH_OVERLAPPED);

		tcc.isShowTourPhotos = _prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_TOUR_PHOTO_VISIBLE);
		tcc.isShowTourPhotoTooltip = _prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_TOUR_PHOTO_TOOLTIP_VISIBLE);

		tcc.isShowBreaktimeValues = _prefStore.getBoolean(//
				ITourbookPreferences.GRAPH_IS_BREAKTIME_VALUES_VISIBLE);

		updateZoomOptionsInChartConfig(tcc, _prefStore);

		return tcc;
	}

	public static void fireEvent(final TourEventId tourEventId) {

		final Object[] allListeners = _tourEventListeners.getListeners();
		for (final Object listener : allListeners) {
			((ITourEventListener) listener).tourChanged(null, tourEventId, null);
		}
	}

	public static void fireEvent(final TourEventId tourEventId, final ArrayList<TourData> modifiedTours) {
		fireEvent(tourEventId, new TourEvent(modifiedTours));
	}

	public static void fireEvent(final TourEventId tourEventId, final Object customData, final IWorkbenchPart part) {

		final Object[] allListeners = _tourEventListeners.getListeners();
		for (final Object listener : allListeners) {
			((ITourEventListener) listener).tourChanged(part, tourEventId, customData);
		}
	}

	public static void fireEvent(final TourEventId tourEventId, final TourEvent tourEvent) {

		final Object[] allListeners = _tourEventListeners.getListeners();
		for (final Object listener : allListeners) {
			((ITourEventListener) listener).tourChanged(null, tourEventId, tourEvent);
		}
	}

	public static void fireEvent(final TourEventId tourEventId, final TourEvent tourEvent, final IWorkbenchPart part) {

		final Object[] allListeners = _tourEventListeners.getListeners();
		for (final Object listener : allListeners) {
			((ITourEventListener) listener).tourChanged(part, tourEventId, tourEvent);
		}
	}

	public static void fireEventWithCustomData(	final TourEventId tourEventId,
												final Object customData,
												final IWorkbenchPart part) {

		fireEvent(tourEventId, customData, part);

//		final Object[] allListeners = _tourEventListeners.getListeners();
//		for (final Object listener : allListeners) {
//			((ITourEventListener) listener).tourChanged(part, tourEventId, customData);
//		}
	}

	/**
	 * Try to get the tour chart and/or editor from the active part.
	 * 
	 * @param tourData
	 * @return Returns the {@link TourChart} for the requested {@link TourData}
	 */
	public static TourChart getActiveTourChart(final TourData tourData) {

		// get tour chart from the active editor part
		for (final IWorkbenchWindow wbWindow : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (final IWorkbenchPage wbPage : wbWindow.getPages()) {

				final IEditorPart activeEditor = wbPage.getActiveEditor();
				if (activeEditor instanceof TourEditor) {

					/*
					 * check if the tour data in the editor is the same
					 */
					final TourChart tourChart = ((TourEditor) activeEditor).getTourChart();
					final TourData tourChartTourData = tourChart.getTourData();
					if (tourChartTourData == tourData) {

						try {
							TourManager.checkTourData(tourData, tourChartTourData);
						} catch (final MyTourbookException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

		// get tour chart from the tour chart view
		for (final IWorkbenchWindow wbWindow : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (final IWorkbenchPage wbPage : wbWindow.getPages()) {

				final IViewReference viewRef = wbPage.findViewReference(TourChartView.ID);
				if (viewRef != null) {

					final IViewPart view = viewRef.getView(false);
					if (view instanceof TourChartView) {

						final TourChartView tourChartView = ((TourChartView) view);

						/*
						 * check if the tour data in the tour chart is the same
						 */
						final TourChart tourChart = tourChartView.getTourChart();
						final TourData tourChartTourData = tourChart.getTourData();
						if (tourChartTourData == tourData) {
							try {
								TourManager.checkTourData(tourData, tourChartTourData);
							} catch (final MyTourbookException e) {
								e.printStackTrace();
							}

							return tourChart;
						}
					}
				}
			}
		}

		return null;
	}

	public static int[] getAllGraphIDs() {
		return _allGraphIDs;
	}

	public static TourManager getInstance() {

		if (_instance == null) {
			_instance = new TourManager();
		}

		return _instance;
	}
	/**
	 * Searches all tour providers in the workbench and returns tours which are selected
	 * 
	 * @return Returns tour id's or <code>null</code> when tours are not found
	 */
	public static ArrayList<TourData> getSelectedTours() {

		final IWorkbenchWindow[] wbWindows = PlatformUI.getWorkbench().getWorkbenchWindows();

		// get all tourProviders
		for (final IWorkbenchWindow wbWindow : wbWindows) {
			final IWorkbenchPage[] pages = wbWindow.getPages();

			for (final IWorkbenchPage wbPage : pages) {
				final IViewReference[] viewRefs = wbPage.getViewReferences();

				for (final IViewReference viewRef : viewRefs) {
					final IViewPart view = viewRef.getView(false);

					if (view instanceof ITourProvider) {

						final ITourProvider tourProvider = (ITourProvider) view;
						final ArrayList<TourData> selectedTours = tourProvider.getSelectedTours();

						if (selectedTours != null && selectedTours.size() > 0) {

							/*
							 * a tour provider is found which also provides tours
							 */
							return selectedTours;
						}
					}
				}
			}
		}

		return null;
	}

	/**
	 * Get a tour from the cache, the cache is necessary because getting a tour from the database
	 * creates always a new instance.
	 * <p>
	 * This is a shortcut for {@link TourManager.getInstance().getTourData(tourId)}.
	 * 
	 * @param requestedTourId
	 * @return Returns the tour data for the tour id or <code>null</code> when tour is not in the
	 *         database.
	 */
	public static TourData getTour(final Long requestedTourId) {

		return getInstance().getTourData(requestedTourId);
	}

	/**
	 * @return Returns the instance of the {@link TourDataEditorView} or <code>null</code> when this
	 *         part is not opened
	 */
	public static TourDataEditorView getTourDataEditor() {
		return _tourDataEditorInstance;
	}

	public static String getTourDateFull(final TourData tourData) {
		return net.tourbook.ui.UI.DateFormatterFull.format(tourData.getTourStartTimeMS());
	}

	private static String getTourDateLong(final Date date) {
		return net.tourbook.ui.UI.DateFormatterLong.format(date.getTime());
	}

	/**
	 * @return returns the date of this tour
	 */
	public static String getTourDateShort(final TourData tourData) {

		if (tourData == null) {
			return UI.EMPTY_STRING;
		}

		return net.tourbook.ui.UI.DateFormatterShort.format(tourData.getTourStartTimeMS());
	}

	/**
	 * @return Returns date/time of the tour start
	 */
	public static DateTime getTourDateTime(final TourData tourData) {

		return tourData.getTourStartTime();
	}

	public static String getTourDateTimeFull(final Date dt) {
		return net.tourbook.ui.UI.DateFormatterFull.format(dt) + UI.DASH_WITH_SPACE

		;
	}

	/**
	 * @return Returns a tour title which contains: full date + full time
	 */
	public static String getTourDateTimeFull(final TourData tourData) {

		return getTourDateFull(tourData) //
				+ UI.DASH_WITH_SPACE
				+ getTourTimeShort(tourData);
	}

	public static String getTourDateTimeShort(final TourData tourData) {
		return net.tourbook.ui.UI.DTFormatterShort.print(tourData.getTourStartTime());
	}

	private static String getTourTimeShort(final Date date) {
		return net.tourbook.ui.UI.TimeFormatterShort.format(date.getTime());
	}

	/**
	 * @return returns the date of this tour
	 */
	public static String getTourTimeShort(final TourData tourData) {
		return net.tourbook.ui.UI.TimeFormatterShort.format(tourData.getTourStartTimeMS());
	}

	public static String getTourTitle(final Date date) {
		return getTourDateLong(date) //
				+ UI.DASH_WITH_SPACE
				+ getTourTimeShort(date);
	}

	/**
	 * @return returns the title of this tour
	 */
	public static String getTourTitle(final TourData tourData) {
		return getTourDateLong(getTourDateTime(tourData).toDate())//
				+ UI.DASH_WITH_SPACE
				+ getTourTimeShort(tourData);
	}

	/**
	 * @return returns the detailed title of this tour which contains:<br>
	 *         date + time + title as it is displayed in the tour chart
	 */
	public static String getTourTitleDetailed(final TourData tourData) {

		final String tourTitle = tourData.getTourTitle();

		return getTourDateFull(tourData) //
				+ UI.DASH_WITH_SPACE
				+ getTourTimeShort(tourData)
				+ ((tourTitle.length() == 0) ? UI.EMPTY_STRING : UI.DASH_WITH_SPACE + tourTitle);
	}

	/**
	 * Checks if {@link TourData} can be painted
	 * 
	 * @param tourData
	 * @return <code>true</code> when {@link TourData} contains a tour which can be painted in the
	 *         map
	 */
	public static boolean isPaintDataValid(final TourData tourData) {

		if (tourData == null) {
			return false;
		}

		// check if coordinates are available

		final double[] longitudeSerie = tourData.longitudeSerie;
		final double[] latitudeSerie = tourData.latitudeSerie;

		if ((longitudeSerie == null)
				|| (longitudeSerie.length == 0)
				|| (latitudeSerie == null)
				|| (latitudeSerie.length == 0)) {
			return false;
		}

		return true;
	}

	/**
	 * Checks if a tour in the {@link TourDataEditorView} is modified and shows the editor when it's
	 * modified. A message dialog informs the user about the modified tour and that the requested
	 * actions cannot be done.
	 * 
	 * @return Returns <code>true</code> when the tour is modified in the {@link TourDataEditorView}
	 */
	public static boolean isTourEditorModified() {
		return isTourEditorModified(true);
	}

	public static boolean isTourEditorModified(final boolean isOpenEditor) {

		final TourDataEditorView tourDataEditor = TourManager.getTourDataEditor();
		if (tourDataEditor != null && tourDataEditor.isDirty()) {

			if (isOpenEditor) {
				openTourEditor(true);
			}

			MessageDialog.openInformation(
					Display.getCurrent().getActiveShell(),
					Messages.dialog_is_tour_editor_modified_title,
					Messages.dialog_is_tour_editor_modified_message);

			return true;
		}

		return false;
	}

	/**
	 * @param allTourIds
	 * @param allTourData
	 *            Contains {@link TourData} for all tour ids.
	 * @return Returns a unique key for all {@link TourData}.
	 */
	public static long loadTourData(final ArrayList<Long> allTourIds, final ArrayList<TourData> allTourData) {

		allTourData.clear();

		// create a unique key for all tours
		final long newOverlayKey[] = { 0 };

		if (allTourIds.size() > 50) {

			try {

				final IRunnableWithProgress saveRunnable = new IRunnableWithProgress() {
					@Override
					public void run(final IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {

						int loadCounter = 0;
						final int idSize = allTourIds.size();

						monitor.beginTask(Messages.Tour_Data_LoadTourData_Monitor, idSize);

						for (final Long tourId : allTourIds) {

							monitor.subTask(NLS.bind(
									Messages.Tour_Data_LoadTourData_Monitor_SubTask,
									++loadCounter,
									idSize));

							if (monitor.isCanceled()) {
								break;
							}

							final TourData tourData = TourManager.getInstance().getTourData(tourId);
							if (isPaintDataValid(tourData)) {

								// keep tour data for each tour id
								allTourData.add(tourData);
								newOverlayKey[0] += tourData.getTourId();
							}

							monitor.worked(1);
						}
					}
				};

				new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, true, saveRunnable);

			} catch (final InvocationTargetException e) {
				StatusUtil.showStatus(e);
			} catch (final InterruptedException e) {
				StatusUtil.showStatus(e);
			}

		} else {

			for (final Long tourId : allTourIds) {

				final TourData tourData = TourManager.getInstance().getTourData(tourId);
				if (isPaintDataValid(tourData)) {

					// keep tour data for each tour id
					allTourData.add(tourData);
					newOverlayKey[0] += tourData.getTourId();
				}
			}
		}

		return newOverlayKey[0];
	}

	public static TourDataEditorView openTourEditor(final boolean isActive) {

		final TourDataEditorView tourDataEditorView[] = { null };

		/*
		 * must be run in the UI thread because PlatformUI.getWorkbench().getActiveWorkbenchWindow()
		 * returns null in none UI threads
		 */
		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {

				try {

					final IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

					final IWorkbenchPage page = activeWorkbenchWindow.getActivePage();

					final IViewPart viewPart = page.showView(TourDataEditorView.ID, null, IWorkbenchPage.VIEW_VISIBLE);

					if (viewPart instanceof TourDataEditorView) {

						tourDataEditorView[0] = (TourDataEditorView) viewPart;

						if (isActive) {

							page.showView(TourDataEditorView.ID, null, IWorkbenchPage.VIEW_ACTIVATE);

						} else if (page.isPartVisible(viewPart) == false || isActive) {

							page.bringToTop(viewPart);
						}

// HINT: this does not restore the part when it's in a fast view
//
//			final IWorkbenchPartReference partRef = page.getReference(viewPart);
//			final int partState = page.getPartState(partRef);
//			page.setPartState(partRef, IWorkbenchPage.STATE_MAXIMIZED);
//			page.setPartState(partRef, IWorkbenchPage.STATE_RESTORED);

					}

				} catch (final PartInitException e) {
					StatusUtil.log(e);
				}
			}
		});

		return tourDataEditorView[0];
	}

	/**
	 * Remove time slices from {@link TourData}
	 * 
	 * @param tourData
	 * @param firstIndex
	 * @param lastIndex
	 * @param isRemoveTime
	 */
	public static void removeTimeSlices(final TourData tourData,
										final int firstIndex,
										final int lastIndex,
										final boolean isRemoveTime) {

		if (isRemoveTime) {
			// this must be done before the time series are modified
			removeTimeSlices_TimeAndDistance(tourData, firstIndex, lastIndex);
		}

		long[] longSerie;
		float[] floatSerie;
		double[] doubleSerie;

		floatSerie = tourData.altitudeSerie;
		if (floatSerie != null) {
			tourData.altitudeSerie = removeTimeSlices_Float(floatSerie, firstIndex, lastIndex);
		}

		floatSerie = tourData.cadenceSerie;
		if (floatSerie != null) {
			tourData.cadenceSerie = removeTimeSlices_Float(floatSerie, firstIndex, lastIndex);
		}

		floatSerie = tourData.distanceSerie;
		if (floatSerie != null) {
			tourData.distanceSerie = removeTimeSlices_Float(floatSerie, firstIndex, lastIndex);
		}

		longSerie = tourData.gearSerie;
		if (longSerie != null) {
			tourData.setGears(removeTimeSlices_Long(longSerie, firstIndex, lastIndex));
		}

		floatSerie = tourData.pulseSerie;
		if (floatSerie != null) {
			tourData.pulseSerie = removeTimeSlices_Float(floatSerie, firstIndex, lastIndex);
		}

		floatSerie = tourData.temperatureSerie;
		if (floatSerie != null) {
			tourData.temperatureSerie = removeTimeSlices_Float(floatSerie, firstIndex, lastIndex);
		}

		final int[] intSerie = tourData.timeSerie;
		if (intSerie != null) {

			tourData.timeSerie = removeTimeSlices_Integer(intSerie, firstIndex, lastIndex);

			doubleSerie = tourData.getTimeSerieDouble();
			tourData.setTimeSerieDouble(removeTimeSlices_Double(doubleSerie, firstIndex, lastIndex));
		}

		doubleSerie = tourData.latitudeSerie;
		if (doubleSerie != null) {
			tourData.latitudeSerie = removeTimeSlices_Double(doubleSerie, firstIndex, lastIndex);
		}
		doubleSerie = tourData.longitudeSerie;
		if (doubleSerie != null) {
			tourData.longitudeSerie = removeTimeSlices_Double(doubleSerie, firstIndex, lastIndex);
		}

		/*
		 * get speed/power data when it's from the device
		 */
		final boolean isTourPower = tourData.isPowerSerieFromDevice();
		final boolean isTourSpeed = tourData.isSpeedSerieFromDevice();
		if (isTourPower) {
			floatSerie = tourData.getPowerSerie();
			if (floatSerie != null) {
				tourData.setPowerSerie(removeTimeSlices_Float(floatSerie, firstIndex, lastIndex));
			}
		}
		if (isTourSpeed) {
			floatSerie = tourData.getSpeedSerieFromDevice();
			if (floatSerie != null) {
				tourData.setSpeedSerie(removeTimeSlices_Float(floatSerie, firstIndex, lastIndex));
			}
		}

		// reset computed data series and clear cached world positions
		tourData.clearComputedSeries();
		tourData.clearWorldPositions();

		// segments must be recomputed
		tourData.segmentSerieIndex = null;

		removeTourMarkers(tourData, firstIndex, lastIndex, isRemoveTime);
	}

	private static double[] removeTimeSlices_Double(final double[] dataSerie, final int firstIndex, final int lastIndex) {

		final int oldSerieLength = dataSerie.length;
		final int newSerieLength = oldSerieLength - (lastIndex - firstIndex + 1);

		final double[] newDataSerie = new double[newSerieLength];

		if (firstIndex == 0) {

			// delete from start, copy data by skipping removed slices
			System.arraycopy(dataSerie, lastIndex + 1, newDataSerie, 0, newSerieLength);

		} else if (lastIndex == oldSerieLength - 1) {

			// delete until the end
			System.arraycopy(dataSerie, 0, newDataSerie, 0, newSerieLength);

		} else {

			// delete somewhere in the middle

			// copy start segment
			System.arraycopy(dataSerie, 0, newDataSerie, 0, firstIndex);

			// copy end segment
			final int copyLength = oldSerieLength - (lastIndex + 1);
			System.arraycopy(dataSerie, lastIndex + 1, newDataSerie, firstIndex, copyLength);
		}

		return newDataSerie;
	}

	private static float[] removeTimeSlices_Float(final float[] dataSerie, final int firstIndex, final int lastIndex) {

		final int oldSerieLength = dataSerie.length;
		final int newSerieLength = oldSerieLength - (lastIndex - firstIndex + 1);

		final float[] newDataSerie = new float[newSerieLength];

		if (firstIndex == 0) {

			// delete from start, copy data by skipping removed slices
			System.arraycopy(dataSerie, lastIndex + 1, newDataSerie, 0, newSerieLength);

		} else if (lastIndex == oldSerieLength - 1) {

			// delete until the end
			System.arraycopy(dataSerie, 0, newDataSerie, 0, newSerieLength);

		} else {

			// delete somewhere in the middle

			// copy start segment
			System.arraycopy(dataSerie, 0, newDataSerie, 0, firstIndex);

			// copy end segment
			final int copyLength = oldSerieLength - (lastIndex + 1);
			System.arraycopy(dataSerie, lastIndex + 1, newDataSerie, firstIndex, copyLength);
		}

		return newDataSerie;
	}

	private static int[] removeTimeSlices_Integer(final int[] oldDataSerie, final int firstIndex, final int lastIndex) {

		final int oldSerieLength = oldDataSerie.length;
		final int newSerieLength = oldSerieLength - (lastIndex - firstIndex + 1);

		final int[] newDataSerie = new int[newSerieLength];

		if (firstIndex == 0) {

			// delete from start, copy data by skipping removed slices
			System.arraycopy(oldDataSerie, lastIndex + 1, newDataSerie, 0, newSerieLength);

		} else if (lastIndex == oldSerieLength - 1) {

			// delete until the end
			System.arraycopy(oldDataSerie, 0, newDataSerie, 0, newSerieLength);

		} else {

			// delete somewhere in the middle

			// copy start segment
			System.arraycopy(oldDataSerie, 0, newDataSerie, 0, firstIndex);

			// copy end segment
			final int copyLength = oldSerieLength - (lastIndex + 1);
			System.arraycopy(oldDataSerie, lastIndex + 1, newDataSerie, firstIndex, copyLength);
		}

		return newDataSerie;
	}

	private static long[] removeTimeSlices_Long(final long[] dataSerie, final int firstIndex, final int lastIndex) {

		final int oldSerieLength = dataSerie.length;
		final int newSerieLength = oldSerieLength - (lastIndex - firstIndex + 1);

		final long[] newDataSerie = new long[newSerieLength];

		if (firstIndex == 0) {

			// delete from start, copy data by skipping removed slices
			System.arraycopy(dataSerie, lastIndex + 1, newDataSerie, 0, newSerieLength);

		} else if (lastIndex == oldSerieLength - 1) {

			// delete until the end
			System.arraycopy(dataSerie, 0, newDataSerie, 0, newSerieLength);

		} else {

			// delete somewhere in the middle

			// copy start segment
			System.arraycopy(dataSerie, 0, newDataSerie, 0, firstIndex);

			// copy end segment
			final int copyLength = oldSerieLength - (lastIndex + 1);
			System.arraycopy(dataSerie, lastIndex + 1, newDataSerie, firstIndex, copyLength);
		}

		return newDataSerie;
	}

	private static void removeTimeSlices_TimeAndDistance(	final TourData _tourData,
														final int firstIndex,
														final int lastIndex) {

		final int[] timeSerie = _tourData.timeSerie;
		final float[] distSerie = _tourData.distanceSerie;

		if ((timeSerie == null) || (timeSerie.length == 0)) {
			return;
		}

		/*
		 * check if lastIndex is the last time slice, this will already remove time and distance
		 */
		if (lastIndex == timeSerie.length - 1) {
			return;
		}

		final int timeDiff = timeSerie[lastIndex + 1] - timeSerie[firstIndex];
		float distDiff = -1;

		if (distSerie != null) {
			distDiff = distSerie[lastIndex + 1] - distSerie[firstIndex];
		}

		// update remaining time and distance data series
		for (int serieIndex = lastIndex + 1; serieIndex < timeSerie.length; serieIndex++) {

			timeSerie[serieIndex] = timeSerie[serieIndex] - timeDiff;

			if (distDiff != -1) {
				distSerie[serieIndex] = distSerie[serieIndex] - distDiff;
			}
		}
	}

	/**
	 * Removes markers which are deleted and updates marker serie index which are positioned after
	 * the deleted time slices
	 * 
	 * @param tourData
	 * @param firstSerieIndex
	 * @param lastSerieIndex
	 * @param isRemoveTime
	 */
	private static void removeTourMarkers(	final TourData tourData,
											final int firstSerieIndex,
											final int lastSerieIndex,
											final boolean isRemoveTime) {

		// check if markers are available
		final Set<TourMarker> allTourMarkers = tourData.getTourMarkers();
		if (allTourMarkers.size() == 0) {
			return;
		}

		/*
		 * remove deleted markers
		 */
		final TourMarker[] markerCloneList = allTourMarkers.toArray(new TourMarker[allTourMarkers.size()]);
		for (final TourMarker tourMarker : markerCloneList) {

			final int markerSerieIndex = tourMarker.getSerieIndex();

			if ((markerSerieIndex >= firstSerieIndex) && (markerSerieIndex <= lastSerieIndex)) {
				allTourMarkers.remove(tourMarker);
			}
		}

		/*
		 * update marker index in the remaining markers
		 */
		final int diffSerieIndex = lastSerieIndex - firstSerieIndex + 1;
		final long tourStartTimeMS = tourData.getTourStartTimeMS();

		final int[] timeSerie = tourData.timeSerie;
		final float[] distSerie = tourData.distanceSerie;

		for (final TourMarker tourMarker : allTourMarkers) {

			final int markerSerieIndex = tourMarker.getSerieIndex();

			// check if the current marker is positioned after the removed time slices
			if (markerSerieIndex > lastSerieIndex) {

				final int serieIndex = markerSerieIndex - diffSerieIndex;
				tourMarker.setSerieIndex(serieIndex);

				if (isRemoveTime) {

					if (timeSerie != null) {
						final int relativeTime = timeSerie[serieIndex];
						tourMarker.setTime(relativeTime, tourStartTimeMS + (relativeTime * 1000));
					}

					if (distSerie != null) {
						tourMarker.setDistance(distSerie[serieIndex]);
					}
				}
			}
		}
	}

	/**
	 * Saves tours which have been modified and updates the tour data editor.
	 * <p>
	 * If a tour is openend in the {@link TourDataEditorView}, the tour will be saved only when the
	 * tour is not dirty, if the tour is dirty, saving is not done.
	 * <p>
	 * The event {@link TourEventId#TOUR_CHANGED} is fired always.
	 * 
	 * @param tourData
	 *            modified tour
	 * @return Returns the persisted {@link TourData}
	 */
	public static TourData saveModifiedTour(final TourData tourData) {
		return saveModifiedTour(tourData, true);
	}

	/**
	 * @param tourData
	 * @param canFireNotification
	 *            When <code>true</code>, a notification is fired when the data are saved
	 * @return Returns the saved {@link TourData} or <code>null</code> when saving fails
	 */
	public static TourData saveModifiedTour(final TourData tourData, final boolean canFireNotification) {

		final ArrayList<TourData> modifiedTours = new ArrayList<TourData>();
		modifiedTours.add(tourData);

		final ArrayList<TourData> savedTourData = saveModifiedTours(modifiedTours, canFireNotification);

		if (savedTourData == null || savedTourData.size() == 0) {
			return null;
		} else {
			return savedTourData.get(0);
		}
	}

	/**
	 * Saves tours which have been modified and updates the tour data editor, fires a
	 * {@link TourEventId#TOUR_CHANGED} event.<br>
	 * <br>
	 * If a tour is openend in the {@link TourDataEditorView}, the tour will be saved only when the
	 * tour is not dirty, if the tour is dirty, saving is not done. The change event is always
	 * fired.
	 * 
	 * @param modifiedTours
	 *            modified tours
	 * @return Returns a list with all persisted {@link TourData}
	 */
	public static ArrayList<TourData> saveModifiedTours(final ArrayList<TourData> modifiedTours) {
		return saveModifiedTours(modifiedTours, true);
	}

	/**
	 * Saves tours which have been modified and updates the tour data editor, fires a
	 * {@link TourEventId#TOUR_CHANGED} event.<br>
	 * <br>
	 * If a tour is openend in the {@link TourDataEditorView}, the tour will be saved only when the
	 * tour is not dirty, if the tour is dirty, saving is not done.
	 * 
	 * @param modifiedTours
	 *            modified tours
	 * @param canFireNotification
	 *            when <code>true</code>, a notification is fired when the data are saved
	 * @return a list with all persisted {@link TourData}
	 */
	private static ArrayList<TourData> saveModifiedTours(	final ArrayList<TourData> modifiedTours,
															final boolean canFireNotification) {

		final ArrayList<TourData> savedTours = new ArrayList<TourData>();

		if (modifiedTours.size() == 0) {
			// there is nothing modified
			return savedTours;
		}

		final TourData[] tourDataEditorSavedTour = { null };
		final boolean[] doFireChangeEvent = { false };

		if (modifiedTours.size() == 1) {

			// no progress when only 1 tour is saved

			saveModifiedToursOneTour(savedTours, tourDataEditorSavedTour, doFireChangeEvent, modifiedTours.get(0));

		} else {

			try {

				final IRunnableWithProgress saveRunnable = new IRunnableWithProgress() {
					@Override
					public void run(final IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {

						int saveCounter = 0;
						final int tourSize = modifiedTours.size();

						monitor.beginTask(Messages.Tour_Data_SaveTour_Monitor, tourSize);

						for (final TourData tourData : modifiedTours) {

							monitor.subTask(NLS.bind(
									Messages.Tour_Data_SaveTour_MonitorSubtask,
									++saveCounter,
									tourSize));

							saveModifiedToursOneTour(savedTours, tourDataEditorSavedTour, doFireChangeEvent, tourData);

							monitor.worked(1);
						}
					}
				};

				new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, false, saveRunnable);

			} catch (final InvocationTargetException e) {
				StatusUtil.showStatus(e);
			} catch (final InterruptedException e) {
				StatusUtil.showStatus(e);
			}
		}

		FTSearchManager.updateIndex(savedTours);

		if (canFireNotification && doFireChangeEvent[0]) {
			final TourEvent propertyData = new TourEvent(savedTours);
			propertyData.tourDataEditorSavedTour = tourDataEditorSavedTour[0];
			fireEvent(TourEventId.TOUR_CHANGED, propertyData);
		}

		return savedTours;
	}

	private static void saveModifiedToursOneTour(	final ArrayList<TourData> savedTours,
													final TourData[] tourDataEditorSavedTour,
													final boolean[] doFireChangeEvent,
													final TourData tourData) {
		boolean doSaveTour = false;
		TourData savedTour = null;

		final TourDataEditorView tourDataEditor = getTourDataEditor();
		if (tourDataEditor != null) {

			final TourData tourDataInEditor = tourDataEditor.getTourData();

			try {
				checkTourData(tourData, tourDataInEditor);
			} catch (final MyTourbookException e) {
				// error is already displayed, just log it
				e.printStackTrace();
				return;
			}

			if (tourDataInEditor == tourData) {

				// selected tour is in the tour data editor

				if (tourDataEditor.isDirty()) {

					// tour in the editor is already dirty, tour MUST BE SAVED IN THE TOUR EDITOR

					savedTour = tourData;

					/*
					 * make the tour data editor visible, it could be hidden and confuses the user
					 * when the changes are not visible
					 */
					TourManager.openTourEditor(false);

				} else {

					/*
					 * tour in the editor is not dirty, save tour and update editor ui
					 */

					savedTour = TourDatabase.saveTour(tourData, true);

					/*
					 * set flag for the tour data editor that the tour is saved and the ui is
					 * updated
					 */
					tourDataEditorSavedTour[0] = savedTour;
				}

				/*
				 * update UI in the tour data editor with the modified tour data
				 */
				tourDataEditor.updateUI(savedTour);

				doFireChangeEvent[0] = true;

			} else {

				// tour is not in the tour editor

				doSaveTour = true;
			}
		} else {

			// tour is not in the tour editor

			doSaveTour = true;
		}

		if (doSaveTour) {

			// save the tour
			savedTour = TourDatabase.saveTour(tourData, true);

			doFireChangeEvent[0] = true;
		}

		if (savedTour != null) {
			savedTours.add(savedTour);
		}

//		System.out.println(UI.timeStampNano() + " [saveModifiedToursOneTour] Old Tour\t" + tourData);
//		System.out.println(UI.timeStampNano() + " [saveModifiedToursOneTour] Saved Tour\t" + savedTour);
//		// TODO remove SYSTEM.OUT.PRINTLN
	}

	public static boolean setAltitudeValuesFromSRTM(final ArrayList<TourData> tourDataList) {

		if (tourDataList == null || tourDataList.size() == 0) {
			return false;
		}

		if (MessageDialog.openConfirm(
				Display.getCurrent().getActiveShell(),
				Messages.TourEditor_Dialog_SetAltitudeFromSRTM_Title,
				Messages.TourEditor_Dialog_SetAltitudeFromSRTM_Message) == false) {
			return false;
		}

		final boolean[] retValue = { false };

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			@Override
			public void run() {

				for (final TourData tourData : tourDataList) {

					final boolean isReplaced = tourData.replaceAltitudeWithSRTM();

					retValue[0] = retValue[0] || isReplaced;
				}
			}
		});

		return retValue[0];
	}

	/**
	 * set the graph colors from the pref store
	 * 
	 * @param prefStore
	 * @param yData
	 * @param graphName
	 */
	public static void setGraphColor(final ChartDataYSerie yData, final String graphName) {

		final String prefGraphName = ICommonPreferences.GRAPH_COLORS + graphName + "."; //$NON-NLS-1$

		// get COLOR from common pref store
		final IPreferenceStore _commonPrefStore = CommonActivator.getPrefStore();

		final RGB prefLineColor = PreferenceConverter.getColor(//
				_commonPrefStore,
				prefGraphName + GraphColorManager.PREF_COLOR_LINE);

		final RGB prefTextColor = PreferenceConverter.getColor(//
				_commonPrefStore,
				prefGraphName + GraphColorManager.PREF_COLOR_TEXT);

		final RGB prefDarkColor = PreferenceConverter.getColor(//
				_commonPrefStore,
				prefGraphName + GraphColorManager.PREF_COLOR_DARK);

		final RGB prefBrightColor = PreferenceConverter.getColor(//
				_commonPrefStore,
				prefGraphName + GraphColorManager.PREF_COLOR_BRIGHT);

		/**
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		 * <p>
		 * Default color is used to draw the y-axis text, using the prefTextColor can cause problems
		 * when the color is white for a dark gradient color.
		 * <p>
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		 */
		yData.setDefaultRGB(prefLineColor);

		yData.setRgbLine(new RGB[] { prefLineColor });
		yData.setRgbText(new RGB[] { prefTextColor });
		yData.setRgbDark(new RGB[] { prefDarkColor });
		yData.setRgbBright(new RGB[] { prefBrightColor });
	}

	public static void setTourDataEditor(final TourDataEditorView tourDataEditorView) {
		_tourDataEditorInstance = tourDataEditorView;
	}

	/**
	 * update the zoom options in the chart configuration from the pref store
	 * 
	 * @param chartConfig
	 * @param prefStore
	 */
	public static void updateZoomOptionsInChartConfig(	final TourChartConfiguration chartConfig,
														final IPreferenceStore prefStore) {

		chartConfig.autoZoomToSlider = prefStore.getBoolean(ITourbookPreferences.GRAPH_ZOOM_AUTO_ZOOM_TO_SLIDER);
		chartConfig.moveSlidersWhenZoomed = prefStore.getBoolean(ITourbookPreferences.GRAPH_MOVE_SLIDERS_WHEN_ZOOMED);
	}

	public void addTourEventListener(final ITourEventListener listener) {
		_tourEventListeners.add(listener);
	}

	/**
	 * Tour save listeners will be called to save tours before the application is shut down
	 * 
	 * @param listener
	 */
	public void addTourSaveListener(final ITourSaveListener listener) {
		_tourSaveListeners.add(listener);
	}

	/**
	 * Remove all {@link TourData} from the cache so they need to be reloaded the next time with
	 * {@link #getTourData} from the database.
	 * <p>
	 * When this method is called and a tour is modified in the tour editor, the calling method is
	 * responsible to update the tour in the tour editor:
	 */
	public void clearTourDataCache() {

		_tourDataCache.clear();

		if (_tourDataEditorInstance != null && _tourDataEditorInstance.isDirty()) {

			final TourData tourDataInEditor = _tourDataEditorInstance.getTourData();
			if (tourDataInEditor != null) {

				// keep modified tour in cache
				_tourDataCache.put(tourDataInEditor.getTourId(), tourDataInEditor);
			}
		}
	}

	/**
	 * Clip values when a minimum distance is fallen short of
	 * 
	 * @param tourData
	 */
	private void computeValueClipping(final TourData tourData) {

		final int[] timeSerie = tourData.timeSerie;

		final float[] gradientSerie = tourData.gradientSerie;
		final float[] speedSerie = tourData.getSpeedSerie();
		final float[] paceSerie = tourData.getPaceSerie();
		final float[] altimeterSerie = tourData.getAltimeterSerie();
		final double[] distanceSerie = tourData.getDistanceSerieDouble();

		if ((timeSerie == null || timeSerie.length == 0)
				|| speedSerie == null
				|| paceSerie == null
				|| altimeterSerie == null
				|| distanceSerie == null
				|| gradientSerie == null) {
			return;
		}

		int clippingTime;
		if (_prefStore.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_VALUE_CLIPPING)) {
			// use custom clipping
			clippingTime = _prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_VALUE_CLIPPING_TIMESLICE);
		} else {
			// use internal clipping, value was evaluated with experiments
			clippingTime = 15;
		}

		float paceClipping;
		if (_prefStore.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_PACE_CLIPPING)) {
			// use custom clipping
			paceClipping = _prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_PACE_CLIPPING_VALUE);
		} else {
			// use internal clipping, value was evaluated with experiments
			paceClipping = 15;
		}

		paceClipping /= 10;

		final int serieLength = timeSerie.length;
		final int serieLengthLast = serieLength - 1;

		final int deviceTimeInterval = tourData.getDeviceTimeInterval();
		if (deviceTimeInterval > 0) {

			/*
			 * clipping for constanct time intervals
			 */

			final int slices = Math.max(1, clippingTime / deviceTimeInterval);

			for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

				// adjust index to the array size
				int sliceIndex = serieIndex + slices;
				final int sliceIndexMax = ((0 >= sliceIndex) ? 0 : sliceIndex);
				sliceIndex = (sliceIndexMax <= serieLengthLast) ? sliceIndexMax : serieLengthLast;

				final double distance = distanceSerie[sliceIndex] - distanceSerie[serieIndex];

				if (distance == 0) {
					altimeterSerie[serieIndex] = 0;
					gradientSerie[serieIndex] = 0;
					speedSerie[serieIndex] = 0;
				}

				// remove peaks in pace
				if (speedSerie[serieIndex] <= paceClipping) {
					paceSerie[serieIndex] = 0;
				}
			}

		} else {

			/*
			 * clipping for variable time intervals
			 */

			for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

				// adjust index to the array size
				final int serieIndexPrev = serieIndex - 1;
				int lowIndex = ((0 >= serieIndexPrev) ? 0 : serieIndexPrev);

				int timeDiff = timeSerie[serieIndex] - timeSerie[lowIndex];
				double distDiff = 0;

				while (timeDiff < clippingTime) {

					// make sure to be in the array range
					if (lowIndex < 1) {
						break;
					}

					lowIndex--;

					timeDiff = timeSerie[serieIndex] - timeSerie[lowIndex];
				}

				distDiff = distanceSerie[serieIndex] - distanceSerie[lowIndex];

				if (distDiff == 0) {
					altimeterSerie[serieIndex] = 0;
					gradientSerie[serieIndex] = 0;
					speedSerie[serieIndex] = 0;
				}

				// remove peaks in pace
				if (speedSerie[serieIndex] <= paceClipping) {
					paceSerie[serieIndex] = 0;
				}
			}
		}
	}

	/**
	 * create the callbacks which compute the average
	 */
	private void createAvgCallbacks() {

		_computeSpeedAvg = new ComputeChartValue() {

			/*
			 * Compute the average speed in km/h between the two sliders
			 */
			@Override
			public float compute() {

				final Object customDataDistance = chartModel.getCustomData(TourManager.CUSTOM_DATA_DISTANCE);
				if (customDataDistance == null) {
					return 0;
				}

				final double[] timeValues = ((ChartDataXSerie) (chartModel.getCustomData(TourManager.CUSTOM_DATA_TIME)))
						.getHighValuesDouble()[0];
				final double[] distanceValues = ((ChartDataXSerie) (customDataDistance)).getHighValuesDouble()[0];
				if (timeValues == null) {
					return 0;
				}

				TourData tourData = null;
				final Object tourId = chartModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_ID);
				if (tourId instanceof Long) {
					tourData = TourManager.getInstance().getTourData((Long) tourId);
					if (tourData == null) {
						return 0;
					}
				}

				final double leftDistance = distanceValues[valueIndexLeft];
				final double rightDistance = distanceValues[valueIndexRight];
				final double leftTime = timeValues[valueIndexLeft];
				final double rightTime = timeValues[valueIndexRight];

				if (leftTime == rightTime) {

					// left and right slider are at the same position
					return 0;

				} else {

					final double time = Math.max(
							0,
							rightTime - leftTime - tourData.getBreakTime(valueIndexLeft, valueIndexRight));

					if (time == 0) {
						return 0;
					}

					final double distance = rightDistance - leftDistance;
					final double speed = distance / time * 3.6;

					return (float) speed;
				}
			}
		};

		_computePaceAvg = new ComputeChartValue() {

			/*
			 * Compute the average pace between two sliders
			 */
			@Override
			public float compute() {

				final Object customDataDistance = chartModel.getCustomData(TourManager.CUSTOM_DATA_DISTANCE);
				if (customDataDistance == null) {
					return 0;
				}

				final double[] timeValues = ((ChartDataXSerie) (chartModel.getCustomData(TourManager.CUSTOM_DATA_TIME)))
						.getHighValuesDouble()[0];
				final double[] distanceValues = ((ChartDataXSerie) (customDataDistance)).getHighValuesDouble()[0];
				if (timeValues == null) {
					return 0;
				}

				TourData tourData = null;
				final Object tourId = chartModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_ID);
				if (tourId instanceof Long) {
					tourData = TourManager.getInstance().getTourData((Long) tourId);
					if (tourData == null) {
						return 0;
					}
				}

				final double leftDistance = distanceValues[valueIndexLeft];
				final double rightDistance = distanceValues[valueIndexRight];
				final double leftTime = timeValues[valueIndexLeft];
				final double rightTime = timeValues[valueIndexRight];

				if (leftTime == rightTime) {

					// left and right slider are at the same position
					return 0;

				} else {

					final double time = Math.max(
							0,
							rightTime - leftTime - tourData.getBreakTime(valueIndexLeft, valueIndexRight));
					final double distance = rightDistance - leftDistance;

					if (distance == 0) {
						return 0;
					} else {
						return (float) (time * 1000 / distance);
					}
				}
			}
		};

		_computeAltimeterAvg = new ComputeChartValue() {

			/*
			 * Compute the average altimeter speed between the two sliders
			 */
			@Override
			public float compute() {

				final Object customDataAltitude = chartModel.getCustomData(TourManager.CUSTOM_DATA_ALTITUDE);
				if (customDataAltitude == null) {
					return 0;
				}

				final double[] timeValues = ((ChartDataXSerie) (chartModel.getCustomData(TourManager.CUSTOM_DATA_TIME)))
						.getHighValuesDouble()[0];
				if (timeValues == null) {
					return 0;
				}

				final float[] altitudeValues = ((ChartDataYSerie) (customDataAltitude)).getHighValuesFloat()[0];

				TourData tourData = null;
				final Object tourId = chartModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_ID);
				if (tourId instanceof Long) {
					tourData = TourManager.getInstance().getTourData((Long) tourId);
					if (tourData == null) {
						return 0;
					}
				}

				final float leftAltitude = altitudeValues[valueIndexLeft];
				final float rightAltitude = altitudeValues[valueIndexRight];
				final double leftTime = timeValues[valueIndexLeft];
				final double rightTime = timeValues[valueIndexRight];

				if (leftTime == rightTime) {

					// left and right slider are at the same position
					return 0;

				} else {

					final double time = Math.max(
							0,
							rightTime - leftTime - tourData.getBreakTime(valueIndexLeft, valueIndexRight));

					if (time == 0) {
						return 0;
					}

					return (float) (((rightAltitude - leftAltitude) / time) * 3600);
				}
			}
		};

		_computeGradientAvg = new ComputeChartValue() {

			/*
			 * Compute the average altimeter speed between the two sliders
			 */
			@Override
			public float compute() {

				final Object customDataAltitude = chartModel.getCustomData(TourManager.CUSTOM_DATA_ALTITUDE);
				final Object customDataDistance = chartModel.getCustomData(TourManager.CUSTOM_DATA_DISTANCE);
				if (customDataAltitude == null || customDataDistance == null) {
					return 0;
				}

				final float[] altitudeValues = ((ChartDataYSerie) (customDataAltitude)).getHighValuesFloat()[0];
				final double[] distanceValues = ((ChartDataXSerie) (customDataDistance)).getHighValuesDouble()[0];

				final float leftAltitude = altitudeValues[valueIndexLeft];
				final float rightAltitude = altitudeValues[valueIndexRight];
				final double leftDistance = distanceValues[valueIndexLeft];
				final double rightDistance = distanceValues[valueIndexRight];

				if (leftDistance == rightDistance) {
					// left and right slider are at the same position
					return 0;
				} else {
					return (float) (((rightAltitude - leftAltitude)) / (rightDistance - leftDistance) * 100);
				}
			}
		};
	}

	/**
	 * Creates a chart data fDataModel from the tour data
	 * 
	 * @param tourData
	 *            data which contains the tour data
	 * @param tourChartProperty
	 * @param fTourChartConfig
	 * @return
	 */
	public ChartDataModel createChartDataModel(final TourData tourData, final TourChartConfiguration chartConfig) {

		return createChartDataModel_10(tourData, chartConfig, false);
	}

	public ChartDataModel createChartDataModel(	final TourData tourData,
												final TourChartConfiguration chartConfig,
												final boolean hasPropertyChanged) {

		return createChartDataModel_10(tourData, chartConfig, hasPropertyChanged);
	}

	private ChartDataModel createChartDataModel_10(	final TourData tourData,
													final TourChartConfiguration tourChartConfig,
													final boolean hasPropertyChanged) {

		final ChartDataModel chartDataModel = new ChartDataModel(ChartType.LINE);

		/*
		 * TIME SERIE is a MUST data serie
		 */
		final boolean isTimeSerie = tourData.timeSerie != null && tourData.timeSerie.length > 0;
		final boolean isHistorySerie = tourData.timeSerieHistory != null && tourData.timeSerieHistory.length > 0;

		if (isTimeSerie == false && isHistorySerie == false) {
			return chartDataModel;
		}

		if (hasPropertyChanged) {
			tourData.clearComputedSeries();
		}

		tourData.computeSpeedSerie();
		tourData.computeAltimeterGradientSerie();

		computeValueClipping(tourData);

		/*
		 * distance
		 */
		final double[] distanceSerie = tourData.getDistanceSerieDouble();
		ChartDataXSerie xDataDist = null;
		if (distanceSerie != null) {
			xDataDist = new ChartDataXSerie(distanceSerie);
			xDataDist.setLabel(Messages.tour_editor_label_distance);
			xDataDist.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
			xDataDist.setValueDivisor(1000);
			xDataDist.setDefaultRGB(new RGB(0, 0, 0));

			// do not show average values but show the other values with 3 digits
			xDataDist.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(false, false, null, 3));
		}

		/*
		 * time
		 */
		final ChartDataXSerie xDataTime = new ChartDataXSerie(tourData.getTimeSerieWithTimeZoneAdjusted());
		xDataTime.setLabel(Messages.tour_editor_label_time);
		xDataTime.setUnitLabel(Messages.tour_editor_label_time_unit);
		xDataTime.setDefaultRGB(new RGB(0, 0, 0));
		xDataTime.setAxisUnit(ChartDataXSerie.AXIS_UNIT_HOUR_MINUTE_OPTIONAL_SECOND);

		/*
		 * show the distance on the x-axis when a distance is available, otherwise the time is
		 * displayed
		 */
		boolean isShowTimeOnXAxis;
		if (xDataDist == null) {
			isShowTimeOnXAxis = true;
			tourChartConfig.isForceTimeOnXAxis = true;
		} else {
			isShowTimeOnXAxis = tourChartConfig.isShowTimeOnXAxisBackup;
			tourChartConfig.isForceTimeOnXAxis = false;
		}
		tourChartConfig.isShowTimeOnXAxis = isShowTimeOnXAxis;

		if (isShowTimeOnXAxis) {

			// time is displayed on the X axis

			chartDataModel.setXData(xDataTime);
			chartDataModel.addXyData(xDataTime);

			if (xDataDist != null) {
				chartDataModel.setXData2nd(xDataDist);
				chartDataModel.addXyData(xDataDist);
			}

			final DateTime tourStartTime = tourData.getTourStartTime();

			// set date/time when x-axis starts
			xDataTime.setStartDateTime(tourStartTime);

			/*
			 * when time is displayed, the x-axis can show the start time starting from 0 or from
			 * the current time of the day
			 */
			final int tourTimeOfDay = (tourStartTime.getHourOfDay() * 3600)
					+ (tourStartTime.getMinuteOfHour() * 60)
					+ tourStartTime.getSecondOfMinute();

			final int photoTimeOfDay = tourTimeOfDay - tourData.getPhotoTimeAdjustment();

			final X_AXIS_START_TIME configXAxisTime = tourChartConfig.xAxisTime;

			final double xAxisStartValue = configXAxisTime == X_AXIS_START_TIME.START_WITH_0
					? 0
					: configXAxisTime == X_AXIS_START_TIME.TOUR_START_TIME ? tourTimeOfDay : photoTimeOfDay;

			xDataTime.setStartValue(xAxisStartValue);

			xDataTime.setIsTimeSerieWithTimeZoneAdjustment(tourData.isTimeSerieWithTimeZoneAdjustment());

		} else {

			// distance is available and is displayed on the x axis

			chartDataModel.setXData(xDataDist);
			chartDataModel.setXData2nd(xDataTime);

			chartDataModel.addXyData(xDataDist);
			chartDataModel.addXyData(xDataTime);
		}

		/*
		 * Don't draw a (visible) line when a break occures, break time can be minutes, hours or
		 * days. This feature prevents to draw triangles between 2 value points
		 */
		xDataTime.setNoLine(tourData.getBreakTimeSerie());

		ChartType chartType;
		final String chartTypeName = _prefStore.getString(ITourbookPreferences.GRAPH_PROPERTY_CHARTTYPE);
		try {
			chartType = ChartType.valueOf(ChartType.class, chartTypeName);
		} catch (final Exception e) {
			// set default value
			chartType = ChartType.LINE;
		}

		// HR zones can be displayed when they are available
		tourChartConfig.canShowHrZones = tourData.getNumberOfHrZones() > 0;
		final boolean isHrZoneDisplayed = tourChartConfig.canShowHrZones && tourChartConfig.isHrZoneDisplayed;

		/*
		 * altitude
		 */
		ChartDataYSerie yDataAltitude = null;

		final float[] altitudeSerie = tourData.getAltitudeSmoothedSerie(true);
		if (altitudeSerie != null) {

			if (tourData.isSRTMAvailable()) {

				tourChartConfig.canShowSRTMData = true;

				if (tourChartConfig.isSRTMDataVisible) {

					final float[] srtmDataSerie = tourData.getSRTMSerie();
					if (srtmDataSerie != null) {

						// create altitude dataserie and adjust min/max values with with the srtm values
						yDataAltitude = createChartDataSerie(//
								new float[][] { altitudeSerie, srtmDataSerie },
								chartType);
					}
				}

			} else {

				// SRTM data are not available
				tourChartConfig.canShowSRTMData = false;
			}

			if (yDataAltitude == null) {
				yDataAltitude = createChartDataSerie(altitudeSerie, chartType);
			}

			yDataAltitude.setYTitle(GRAPH_LABEL_ALTITUDE);
			yDataAltitude.setUnitLabel(UI.UNIT_LABEL_ALTITUDE);
			yDataAltitude.setShowYSlider(true);
			yDataAltitude.setDisplayedFractionalDigits(2);
			yDataAltitude.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_ALTITUDE);
			yDataAltitude.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true));

			if (isHrZoneDisplayed) {
				yDataAltitude.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
			} else {
				yDataAltitude.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
			}

			setGraphColor(yDataAltitude, GraphColorManager.PREF_GRAPH_ALTITUDE);
//			adjustMinMax(yDataAltitude);
			chartDataModel.addXyData(yDataAltitude);
		}

		/*
		 * heartbeat
		 */
		ChartDataYSerie yDataPulse = null;

		final float[] pulseSerie = tourData.getPulseSmoothedSerie();
		if (pulseSerie != null) {

			yDataPulse = createChartDataSerie(pulseSerie, chartType);

			yDataPulse.setYTitle(GRAPH_LABEL_HEARTBEAT);
			yDataPulse.setUnitLabel(GRAPH_LABEL_HEARTBEAT_UNIT);
			yDataPulse.setShowYSlider(true);
			yDataPulse.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_PULSE);
			yDataPulse.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true));

			if (isHrZoneDisplayed) {
				yDataPulse.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
			} else {
				yDataPulse.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
			}

			setGraphColor(yDataPulse, GraphColorManager.PREF_GRAPH_HEARTBEAT);
			chartDataModel.addXyData(yDataPulse);

			/*
			 * adjust pulse min/max values when it's defined in the pref store
			 */
			if (_prefStore.getBoolean(ITourbookPreferences.GRAPH_PULSE_IS_MIN_ENABLED)) {

				yDataPulse.setVisibleMinValueForced(_prefStore.getInt(ITourbookPreferences.GRAPH_PULSE_MIN_VALUE));
			}

			// set max value after min value
			if (_prefStore.getBoolean(ITourbookPreferences.GRAPH_PULSE_IS_MAX_ENABLED)) {

				yDataPulse.setVisibleMaxValueForced(_prefStore.getInt(ITourbookPreferences.GRAPH_PULSE_MAX_VALUE));
			}
		}

		/*
		 * speed
		 */
		final float[] speedSerie = tourData.getSpeedSerie();
		ChartDataYSerie yDataSpeed = null;
		if (speedSerie != null) {

			yDataSpeed = createChartDataSerie(speedSerie, chartType);

			yDataSpeed.setYTitle(GRAPH_LABEL_SPEED);
			yDataSpeed.setUnitLabel(UI.UNIT_LABEL_SPEED);
			yDataSpeed.setShowYSlider(true);
			yDataSpeed.setDisplayedFractionalDigits(1);
			yDataSpeed.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_SPEED);
			yDataSpeed.setCustomData(CUSTOM_DATA_ANALYZER_INFO, //
					new TourChartAnalyzerInfo(true, true, _computeSpeedAvg, 1));

			if (isHrZoneDisplayed) {
				yDataSpeed.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
			} else {
				yDataSpeed.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
			}

			setGraphColor(yDataSpeed, GraphColorManager.PREF_GRAPH_SPEED);
			chartDataModel.addXyData(yDataSpeed);
		}

		/*
		 * pace
		 */
		final float[] paceSerie = tourData.getPaceSerieSeconds();
		ChartDataYSerie yDataPace = null;
		if (paceSerie != null) {

			yDataPace = createChartDataSerie(paceSerie, chartType);

			yDataPace.setYTitle(GRAPH_LABEL_PACE);
			yDataPace.setUnitLabel(UI.UNIT_LABEL_PACE);
			yDataPace.setShowYSlider(true);
			yDataPace.setAxisUnit(ChartDataSerie.AXIS_UNIT_MINUTE_SECOND);
			yDataPace.setSliderLabelFormat(ChartDataYSerie.SLIDER_LABEL_FORMAT_MM_SS);
			yDataPace.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_PACE);
			yDataPace.setCustomData(CUSTOM_DATA_ANALYZER_INFO, //
					new TourChartAnalyzerInfo(true, false, _computePaceAvg, 1));

			if (isHrZoneDisplayed) {
				yDataPace.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
			} else {
				yDataPace.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
			}

			setGraphColor(yDataPace, GraphColorManager.PREF_GRAPH_PACE);
			chartDataModel.addXyData(yDataPace);

			/*
			 * adjust pace min/max values when it's defined in the pref store
			 */
			if (_prefStore.getBoolean(ITourbookPreferences.GRAPH_PACE_IS_MIN_ENABLED)) {

				yDataPace.setVisibleMinValueForced(_prefStore.getInt(ITourbookPreferences.GRAPH_PACE_MIN_VALUE) * 60);
			}

			// set max value after min value
			if (_prefStore.getBoolean(ITourbookPreferences.GRAPH_PACE_IS_MAX_ENABLED)) {

				yDataPace.setVisibleMaxValueForced(_prefStore.getInt(ITourbookPreferences.GRAPH_PACE_MAX_VALUE) * 60);
			}
		}

		/*
		 * power
		 */
		final float[] powerSerie = tourData.getPowerSerie();
		ChartDataYSerie yDataPower = null;
		if (powerSerie != null) {

			yDataPower = createChartDataSerie(powerSerie, chartType);

			yDataPower.setYTitle(GRAPH_LABEL_POWER);
			yDataPower.setUnitLabel(GRAPH_LABEL_POWER_UNIT);
			yDataPower.setShowYSlider(true);
			yDataPower.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_POWER);
			yDataPower.setCustomData(CUSTOM_DATA_ANALYZER_INFO, //
					new TourChartAnalyzerInfo(true, false, _computePowerAvg, 0));

			if (isHrZoneDisplayed) {
				yDataPower.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
			} else {
				yDataPower.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
			}

			setGraphColor(yDataPower, GraphColorManager.PREF_GRAPH_POWER);
			chartDataModel.addXyData(yDataPower);
		}

		/*
		 * altimeter
		 */
		final float[] altimeterSerie = tourData.getAltimeterSerie();
		ChartDataYSerie yDataAltimeter = null;
		if (altimeterSerie != null) {

			yDataAltimeter = createChartDataSerie(altimeterSerie, chartType);

			yDataAltimeter.setYTitle(GRAPH_LABEL_ALTIMETER);
			yDataAltimeter.setUnitLabel(UI.UNIT_LABEL_ALTIMETER);
			yDataAltimeter.setShowYSlider(true);
			yDataAltimeter.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_ALTIMETER);
			yDataAltimeter.setCustomData(CUSTOM_DATA_ANALYZER_INFO, //
					new TourChartAnalyzerInfo(true, false, _computeAltimeterAvg, 0));

			if (isHrZoneDisplayed) {
				yDataAltimeter.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
			} else {
				yDataAltimeter.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_ZERO);
			}

			setGraphColor(yDataAltimeter, GraphColorManager.PREF_GRAPH_ALTIMETER);
			chartDataModel.addXyData(yDataAltimeter);

			/*
			 * adjust min/max altitude when it's defined in the pref store
			 */
			if (_prefStore.getBoolean(ITourbookPreferences.GRAPH_ALTIMETER_IS_MIN_ENABLED)) {

				final int minValue = _prefStore.getInt(ITourbookPreferences.GRAPH_ALTIMETER_MIN_VALUE);

				yDataAltimeter.setVisibleMinValueForced(minValue + TourChart.MIN_ADJUSTMENT);
			}

			if (_prefStore.getBoolean(ITourbookPreferences.GRAPH_ALTIMETER_IS_MAX_ENABLED)) {

				final double maxValue = _prefStore.getInt(ITourbookPreferences.GRAPH_ALTIMETER_MAX_VALUE);

				final double maxAdjust = 1e-2;
				// set max value after min value, adjust max otherwise values above the max are painted
				yDataAltimeter.setVisibleMaxValueForced(maxValue > 0 //
						? maxValue - maxAdjust
						: maxValue + maxAdjust);
			}
		}

		/*
		 * gradient
		 */
		final float[] gradientSerie = tourData.gradientSerie;
		ChartDataYSerie yDataGradient = null;
		if (gradientSerie != null) {

			yDataGradient = createChartDataSerie(gradientSerie, chartType);

			yDataGradient.setYTitle(GRAPH_LABEL_GRADIENT);
			yDataGradient.setUnitLabel(GRAPH_LABEL_GRADIENT_UNIT);
			yDataGradient.setShowYSlider(true);
			yDataGradient.setDisplayedFractionalDigits(1);
			yDataGradient.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_GRADIENT);
			yDataGradient.setCustomData(CUSTOM_DATA_ANALYZER_INFO, //
					new TourChartAnalyzerInfo(true, true, _computeGradientAvg, 1));

			if (isHrZoneDisplayed) {
				yDataGradient.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
			} else {
				yDataGradient.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_ZERO);
			}

			setGraphColor(yDataGradient, GraphColorManager.PREF_GRAPH_GRADIENT);
			chartDataModel.addXyData(yDataGradient);

			/*
			 * adjust min/max values when defined in the pref store
			 */
			if (_prefStore.getBoolean(ITourbookPreferences.GRAPH_GRADIENT_IS_MIN_ENABLED)) {

				final int minValue = _prefStore.getInt(ITourbookPreferences.GRAPH_GRADIENT_MIN_VALUE);

				yDataGradient.setVisibleMinValueForced(minValue + TourChart.MIN_ADJUSTMENT);
			}

			if (_prefStore.getBoolean(ITourbookPreferences.GRAPH_GRADIENT_IS_MAX_ENABLED)) {

				final double maxValue = _prefStore.getInt(ITourbookPreferences.GRAPH_GRADIENT_MAX_VALUE);

				// set max value after min value, adjust max otherwise values above the max are painted
				yDataGradient.setVisibleMaxValueForced(maxValue > 0 //
						? maxValue - TourChart.MAX_ADJUSTMENT
						: maxValue + TourChart.MAX_ADJUSTMENT);
			}
		}

		/*
		 * cadence
		 */
		final float[] cadenceSerie = tourData.cadenceSerie;
		ChartDataYSerie yDataCadence = null;
		if (cadenceSerie != null) {

			yDataCadence = createChartDataSerie(cadenceSerie, chartType);

			yDataCadence.setYTitle(GRAPH_LABEL_CADENCE);
			yDataCadence.setUnitLabel(GRAPH_LABEL_CADENCE_UNIT);
			yDataCadence.setShowYSlider(true);
			yDataCadence.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_CADENCE);
			yDataCadence.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true));

			if (isHrZoneDisplayed) {
				yDataCadence.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
			} else {
				yDataCadence.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
			}
//			yDataCadence.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM_NO_BORDER);
//			yDataCadence.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_NO);
//			yDataCadence.setLineGaps(tourData.getCadenceGaps());

			setGraphColor(yDataCadence, GraphColorManager.PREF_GRAPH_CADENCE);
			chartDataModel.addXyData(yDataCadence);
		}

		/*
		 * Gears
		 */
		final float[][] gearSerie = tourData.getGears();
		ChartDataYSerie yDataGears = null;
		if (gearSerie != null) {

			final float[][] chartGearSerie = new float[][] //
			{
					// gear ratio
					gearSerie[0],

					// front gear number, starting with 1
					gearSerie[3] //
			};

			yDataGears = createChartDataSerie(chartGearSerie, ChartType.HORIZONTAL_BAR);

			yDataGears.setYTitle(GRAPH_LABEL_GEARS);
			yDataGears.setShowYSlider(true);
			yDataGears.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_GEARS);
			yDataGears.setSliderLabelProvider(new GearSliderLabelProvider(gearSerie));

//			yDataGears.setLineGaps(tourData.getCadenceGaps());

			setGraphColor(yDataGears, GraphColorManager.PREF_GRAPH_GEAR);
			chartDataModel.addXyData(yDataGears);
		}

		/*
		 * temperature
		 */
		final float[] temperatureSerie = tourData.getTemperatureSerie();
		ChartDataYSerie yDataTemperature = null;
		if (temperatureSerie != null) {

			yDataTemperature = createChartDataSerie(temperatureSerie, chartType);

			yDataTemperature.setYTitle(GRAPH_LABEL_TEMPERATURE);
			yDataTemperature.setUnitLabel(UI.UNIT_LABEL_TEMPERATURE);
			yDataTemperature.setShowYSlider(true);
			yDataTemperature.setDisplayedFractionalDigits(1);
			yDataTemperature.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_TEMPERATURE);
			yDataTemperature.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true, true));

			if (isHrZoneDisplayed) {
				yDataTemperature.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_CUSTOM);
			} else {
				yDataTemperature.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
			}

			setGraphColor(yDataTemperature, GraphColorManager.PREF_GRAPH_TEMPTERATURE);
//			adjustMinMax(yDataTemperature);
			chartDataModel.addXyData(yDataTemperature);
		}

		/*
		 * tour compare altitude difference
		 */
		final float[] tourCompareSerie = tourData.tourCompareSerie;
		ChartDataYSerie yDataTourCompare = null;
		if (tourCompareSerie != null && tourCompareSerie.length > 0) {

			yDataTourCompare = createChartDataSerie(tourCompareSerie, chartType);

			yDataTourCompare.setYTitle(GRAPH_LABEL_TOUR_COMPARE);
			yDataTourCompare.setUnitLabel(GRAPH_LABEL_TOUR_COMPARE_UNIT);
			yDataTourCompare.setShowYSlider(true);
			yDataTourCompare.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
			yDataTourCompare.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_TOUR_COMPARE);

			setGraphColor(yDataTourCompare, GraphColorManager.PREF_GRAPH_TOUR_COMPARE);
			chartDataModel.addXyData(yDataTourCompare);
		}

		/*
		 * all visible graphs are added as y-data to the chart data model in the sequence as they
		 * were activated
		 */
		for (final int actionId : tourChartConfig.getVisibleGraphs()) {

			switch (actionId) {
			case GRAPH_ALTITUDE:
				if (yDataAltitude != null) {
					chartDataModel.addYData(yDataAltitude);
					chartDataModel.setCustomData(CUSTOM_DATA_ALTITUDE, yDataAltitude);
				}
				break;

			case GRAPH_PULSE:
				if (yDataPulse != null) {
					chartDataModel.addYData(yDataPulse);
					chartDataModel.setCustomData(CUSTOM_DATA_PULSE, yDataPulse);
				}
				break;

			case GRAPH_SPEED:
				if (yDataSpeed != null) {
					chartDataModel.addYData(yDataSpeed);
					chartDataModel.setCustomData(CUSTOM_DATA_SPEED, yDataSpeed);
				}
				break;

			case GRAPH_PACE:
				if (yDataPace != null) {
					chartDataModel.addYData(yDataPace);
					chartDataModel.setCustomData(CUSTOM_DATA_PACE, yDataPace);
				}
				break;

			case GRAPH_POWER:
				if (yDataPower != null) {
					chartDataModel.addYData(yDataPower);
					chartDataModel.setCustomData(CUSTOM_DATA_POWER, yDataPower);
				}
				break;

			case GRAPH_ALTIMETER:
				if (yDataAltimeter != null) {
					chartDataModel.addYData(yDataAltimeter);
					chartDataModel.setCustomData(CUSTOM_DATA_ALTIMETER, yDataAltimeter);
				}
				break;

			case GRAPH_GRADIENT:
				if (yDataGradient != null) {
					chartDataModel.addYData(yDataGradient);
					chartDataModel.setCustomData(CUSTOM_DATA_GRADIENT, yDataGradient);
				}
				break;

			case GRAPH_CADENCE:
				if (yDataCadence != null) {
					chartDataModel.addYData(yDataCadence);
					chartDataModel.setCustomData(CUSTOM_DATA_CADENCE, yDataCadence);
				}
				break;

			case GRAPH_GEARS:
				if (yDataGears != null) {
					chartDataModel.addYData(yDataGears);
					chartDataModel.setCustomData(CUSTOM_DATA_GEAR_RATIO, yDataGears);
				}
				break;

			case GRAPH_TEMPERATURE:
				if (yDataTemperature != null) {
					chartDataModel.addYData(yDataTemperature);
					chartDataModel.setCustomData(CUSTOM_DATA_TEMPERATURE, yDataTemperature);
				}
				break;

			case GRAPH_TOUR_COMPARE:
				if (yDataTourCompare != null) {
					chartDataModel.addYData(yDataTourCompare);
				}
				break;

			default:
				break;
			}
		}

		if (isHistorySerie) {

			chartDataModel.setChartType(ChartType.HISTORY);

			xDataTime.setAxisUnit(ChartDataSerie.X_AXIS_UNIT_HISTORY);

			// set date/time when x-axis starts
			xDataTime.setStartDateTime(tourData.getTourStartTime());

			// history do not have any y-data, create dummy values
			final float[] yHistorySerie = new float[tourData.timeSerieHistory.length];
			final ChartDataYSerie yDataHistory = createChartDataSerie(yHistorySerie, ChartType.HISTORY);

			yDataHistory.setAxisUnit(ChartDataSerie.AXIS_UNIT_HISTORY);

			setGraphColor(yDataHistory, GraphColorManager.PREF_GRAPH_HISTORY);

			chartDataModel.addXyData(yDataHistory);
			chartDataModel.addYData(yDataHistory);
			chartDataModel.setCustomData(CUSTOM_DATA_HISTORY, yDataHistory);
		}

		chartDataModel.setShowNoLineValues(tourChartConfig.isShowBreaktimeValues);
		chartDataModel.setIsGraphOverlapped(tourChartConfig.isGraphOverlapped);

		chartDataModel.setCustomData(CUSTOM_DATA_TIME, xDataTime);
		chartDataModel.setCustomData(CUSTOM_DATA_DISTANCE, xDataDist);

		chartDataModel.setCustomData(CUSTOM_DATA_TOUR_DATA, tourData);
		chartDataModel.setCustomData(CUSTOM_DATA_TOUR_ID, tourData.getTourId());

		chartDataModel.setCustomData(CUSTOM_DATA_TOUR_CHART_CONFIGURATION, tourChartConfig);

		return chartDataModel;
	}

	private ChartDataYSerie createChartDataSerie(final float[] dataSerie, final ChartType chartType) {
		return new ChartDataYSerie(chartType, dataSerie);
	}

	private ChartDataYSerie createChartDataSerie(final float[][] dataSerie, final ChartType chartType) {
		return new ChartDataYSerie(chartType, dataSerie);
	}

	public TourChart getActiveTourChart() {
		return _activeTourChart;
	}

	/**
	 * @param tourIds
	 * @return Returns a list with {@link TourData} for all tour ids. <code>Null</code> is returned
	 *         when {@link TourData} are not available.
	 */
	public ArrayList<TourData> getTourData(final ArrayList<Long> tourIds) {

		final ArrayList<TourData> tourDataList = new ArrayList<TourData>();

		for (final Long tourId : tourIds) {
			final TourData tourData = getTourData(tourId);
			if (tourData != null) {
				tourDataList.add(tourData);
			}
		}

		return tourDataList.size() == 0 ? null : tourDataList;
	}

	/**
	 * Get a tour from the cache, the cache is necessary because getting a tour from the database
	 * creates always a new instance.
	 * 
	 * @param requestedTourId
	 * @return Returns the tour data for the tour id or <code>null</code> when tour is not in the
	 *         database.
	 */
	public TourData getTourData(final Long requestedTourId) {

		if (requestedTourId == null) {
			return null;
		}

		/*
		 * get tour from tour editor when it contains the requested tour
		 */
		if (_tourDataEditorInstance != null) {

			final TourData tourDataInEditor = _tourDataEditorInstance.getTourData();
			if (tourDataInEditor != null && tourDataInEditor.getTourId().equals(requestedTourId)) {

				// cache tour data
				_tourDataCache.put(tourDataInEditor.getTourId(), tourDataInEditor);

				return tourDataInEditor;
			}
		}

		/*
		 * get tour from cache or database
		 */
		TourData existingTourData = null;
		TourData tourDataInCache = null;

		tourDataInCache = _tourDataCache.get(requestedTourId);

		if (tourDataInCache != null) {
			existingTourData = tourDataInCache;
		} else {

			final TourData tourDataFromDb = TourDatabase.getTourFromDb(requestedTourId);

			if (tourDataFromDb == null) {

				// try to get tour from raw data manager
				final TourData tourDataFromRawManager = RawDataManager
						.getInstance()
						.getImportedTours()
						.get(requestedTourId);

				return tourDataFromRawManager;
			}

			// cache tour data
			_tourDataCache.put(tourDataFromDb.getTourId(), tourDataFromDb);

			existingTourData = tourDataFromDb;
		}

		if (existingTourData != null) {
			replaceTourInTourEditor(existingTourData);
		}

		return existingTourData;
	}

	/**
	 * Get a tour from the database and keep it in the cache
	 * 
	 * @param tourId
	 * @return Returns the tour data for the tour id or <code>null</code> when the tour is not in
	 *         the database
	 */
	public TourData getTourDataFromDb(final Long tourId) {

		if (tourId == null) {
			return null;
		}

		final TourData tourDataFromDb = TourDatabase.getTourFromDb(tourId);
		if (tourDataFromDb == null) {
			return null;
		}

		// keep the tour data
		updateTourInCache(tourDataFromDb);

		return tourDataFromDb;
	}

	/**
	 * Opens the tour for the given tour id
	 * 
	 * @param tourId
	 */
	public void openTourInEditorArea(final Long tourId) {

		if (tourId == null) {
			return;
		}

		try {
			PlatformUI
					.getWorkbench()
					.getActiveWorkbenchWindow()
					.getActivePage()
					.openEditor(new TourEditorInput(tourId), TourEditor.ID, true);

		} catch (final PartInitException e) {
			e.printStackTrace();
		}
	}

	public void removeAllToursFromCache() {

//		final ArrayList<TourData> modifiedTour = new ArrayList<TourData>(fTourDataCache.values());

		_tourDataCache.clear();

		// notify listener to reload the tours
		/*
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! this is not
		 * working because the tour data editor does not reload the tour
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		 */
//		fireEvent(TourEventId.TOUR_CHANGED, new TourEvent(modifiedTour));
	}

	public void removeTourEventListener(final ITourEventListener listener) {
		if (listener != null) {
			_tourEventListeners.remove(listener);
		}
	}

	/**
	 * Removes {@link TourData} with the tourId from the cache.
	 * <p>
	 * When the tour is requested next time with {@link #getTourData(Long)}, is will be loaded from
	 * the database.
	 * 
	 * @param tourId
	 */
	public void removeTourFromCache(final Long tourId) {

		_tourDataCache.remove(tourId);
	}

	public void removeTourSaveListener(final ITourSaveListener listener) {
		if (listener != null) {
			_tourSaveListeners.remove(listener);
		}
	}

	/**
	 * Check tour in tour editor. When tour is modified and it contains a wrong tourData instance,
	 * show an error, otherwise replace (silently) the tour data in the editor
	 */
	private void replaceTourInTourEditor(final TourData tourDataForEditor) {

		if (tourDataForEditor == null || _tourDataEditorInstance == null) {
			return;
		}

		final TourData tourDataInEditor = _tourDataEditorInstance.getTourData();
		if (tourDataInEditor == null) {
			return;
		}

		final long tourIdInEditor = tourDataInEditor.getTourId().longValue();
		final long tourIdForEditor = tourDataForEditor.getTourId().longValue();
		if (tourIdInEditor != tourIdForEditor) {
			// tour editor contains another tour
			return;
		}

		/*
		 * tour editor contains the same tour
		 */

		if (tourDataInEditor == tourDataForEditor) {
			return;
		}

		/*
		 * tour editor contains the wrong tour data instance
		 */
		if (_tourDataEditorInstance.isDirty()) {

			final StringBuilder sb = new StringBuilder()//
					.append("ERROR: ") //$NON-NLS-1$
					.append("The internal structure of the application is out of synch.") //$NON-NLS-1$
					.append(UI.NEW_LINE2)
					.append("You can solve the problem by:") //$NON-NLS-1$
					.append(UI.NEW_LINE2)
					.append("Save or revert the tour in the tour editor and select another tour") //$NON-NLS-1$
					.append(UI.NEW_LINE2)
					.append(UI.NEW_LINE)
					.append("The tour editor contains the selected tour, but the data are different.") //$NON-NLS-1$
					.append(UI.NEW_LINE2)
					.append("Tour in Editor:") //$NON-NLS-1$
					.append(tourDataForEditor.toStringWithHash())
					.append(UI.NEW_LINE)
					.append("Selected Tour:") //$NON-NLS-1$
					.append(tourDataInEditor.toStringWithHash())
					.append(UI.NEW_LINE2)
					.append(UI.NEW_LINE)
					.append("You should also inform the author of the application how this error occured. ") //$NON-NLS-1$
					.append(
							"However it isn't very easy to find out, what actions are exactly done, before this error occured. ") //$NON-NLS-1$
					.append(UI.NEW_LINE2)
					.append("These actions must be reproducable otherwise the bug cannot be identified."); //$NON-NLS-1$

			MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error: Out of Synch", sb.toString()); //$NON-NLS-1$

		} else {

			/*
			 * silently replace tour data in editor
			 */
			_tourDataEditorInstance.setTourData(tourDataForEditor);
		}
	}

	public void resetMapPositions() {

		for (final TourData tourData : _tourDataCache.getCache().values()) {
			tourData.mapCenterPositionLatitude = Double.MIN_VALUE;
			tourData.mapCenterPositionLongitude = Double.MIN_VALUE;
		}
	}

	/**
	 * Before the application is shut down, the tour save listeners are called to save unsaved data.
	 * 
	 * @return Returns <code>true</code> when the tours have been saved or false when it was
	 *         canceled by the user
	 */
	public boolean saveTours() {

		final Object[] allListeners = _tourSaveListeners.getListeners();
		for (final Object tourSaveListener : allListeners) {
			if (((ITourSaveListener) tourSaveListener).saveTour() == false) {
				return false;
			}
		}

		return true;
	}

	public void setActiveTourChart(final TourChart tourChart) {
		_activeTourChart = tourChart;
	}

	/**
	 * Do custom actions when a tour in a table/tree/chart is double clicked
	 * 
	 * @param tourProvider
	 * @param tourDoubleClickState
	 */
	public void tourDoubleClickAction(final ITourProvider tourProvider, final TourDoubleClickState tourDoubleClickState) {

		ArrayList<TourData> selectedTours = tourProvider.getSelectedTours();
		if (selectedTours.size() == 0) {

			if (tourProvider instanceof ITourProviderAll) {
				final ITourProviderAll allTourProvider = (ITourProviderAll) tourProvider;
				selectedTours = allTourProvider.getAllSelectedTours();

				if (selectedTours.size() == 0) {
					return;
				}
			} else {
				return;
			}
		}

		final String action = _prefStore.getString(ITourbookPreferences.VIEW_DOUBLE_CLICK_ACTIONS);
		final TourData firstTour = selectedTours.get(0);

		String actionInfo = null;

		if (action.equals(PrefPageViews.VIEW_DOUBLE_CLICK_ACTION_ADJUST_ALTITUDE)) {

			if (tourDoubleClickState.canAdjustAltitude) {
				ActionOpenAdjustAltitudeDialog.doAction(tourProvider, false);
			} else {
				actionInfo = Messages.PrefPage_ViewActions_Label_DoubleClick_AdjustAltitude;
			}

		} else if (action.equals(PrefPageViews.VIEW_DOUBLE_CLICK_ACTION_EDIT_MARKER)) {

			if (tourDoubleClickState.canEditMarker) {
				ActionOpenMarkerDialog.doAction(tourProvider, true, null);
			} else {
				actionInfo = Messages.PrefPage_ViewActions_Label_DoubleClick_EditMarker;
			}

		} else if (action.equals(PrefPageViews.VIEW_DOUBLE_CLICK_ACTION_EDIT_TOUR)) {

			if (tourDoubleClickState.canEditTour) {
				ActionEditTour.doAction(tourProvider);
			} else {
				actionInfo = Messages.PrefPage_ViewActions_Label_DoubleClick_EditTour;
			}

		} else if (action.equals(PrefPageViews.VIEW_DOUBLE_CLICK_ACTION_OPEN_TOUR_IN_EDIT_AREA)) {

			if (tourDoubleClickState.canOpenTour && firstTour != null) {
				TourManager.getInstance().openTourInEditorArea(firstTour.getTourId());
			} else {
				actionInfo = Messages.PrefPage_ViewActions_Label_DoubleClick_OpenTour;
			}

		} else if (action.equals(PrefPageViews.VIEW_DOUBLE_CLICK_ACTION_NONE_NO_WARNING)) {

			// do nothing and don't show a warning

		} else if (action.equals(PrefPageViews.VIEW_DOUBLE_CLICK_ACTION_NONE)) {

			// do nothing but show info that this can be customized

			MessageDialog.openInformation(
					Display.getCurrent().getActiveShell(),
					Messages.Dialog_DoubleClickAction_NoAction_Title,
					NLS.bind(Messages.Dialog_DoubleClickAction_NoAction_Message, actionInfo));

		} else {

			// default is quick edit

			if (tourDoubleClickState.canQuickEditTour) {
				ActionEditQuick.doAction(tourProvider);
			} else {
				actionInfo = Messages.PrefPage_ViewActions_Label_DoubleClick_QuickEdit;
			}
		}

		if (actionInfo != null) {
			MessageDialog.openInformation(
					Display.getCurrent().getActiveShell(),
					Messages.Dialog_DoubleClickAction_InvalidAction_Title,
					NLS.bind(Messages.Dialog_DoubleClickAction_InvalidAction_Message, actionInfo));
		}
	}

	public void updateTourInCache(final TourData tourData) {

		if (tourData == null) {
			return;
		}

		_tourDataCache.put(tourData.getTourId(), tourData);

		replaceTourInTourEditor(tourData);
	}
}
