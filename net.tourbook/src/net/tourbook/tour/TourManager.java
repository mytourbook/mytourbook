/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ComputeChartValue;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.data.TourData;
import net.tourbook.database.MyTourbookException;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.UI;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartConfiguration;
import net.tourbook.ui.views.TourChartAnalyzerInfo;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;
import net.tourbook.util.StringToArrayConverter;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.joda.time.DateTime;

public class TourManager {

	public static final String					CUSTOM_DATA_TOUR_ID			= "tourdata";								//$NON-NLS-1$

	public static final String					CUSTOM_DATA_TIME			= "time";									//$NON-NLS-1$
	public static final String					CUSTOM_DATA_DISTANCE		= "distance";								//$NON-NLS-1$
	public static final String					CUSTOM_DATA_ALTITUDE		= "altitude";								//$NON-NLS-1$
	public static final String					CUSTOM_DATA_SPEED			= "speed";									//$NON-NLS-1$
	public static final String					CUSTOM_DATA_PACE			= "pace";									//$NON-NLS-1$
	public static final String					CUSTOM_DATA_POWER			= "power";									//$NON-NLS-1$
	public static final String					CUSTOM_DATA_GRADIENT		= "gradient";								//$NON-NLS-1$
	public static final String					CUSTOM_DATA_ALTIMETER		= "altimeter";								//$NON-NLS-1$
	public static final String					CUSTOM_DATA_PULSE			= "pulse";									//$NON-NLS-1$
	public static final String					CUSTOM_DATA_TEMPERATURE		= "temperature";							//$NON-NLS-1$

	public static final String					CUSTOM_DATA_SEGMENT_VALUES	= "segmentValues";							//$NON-NLS-1$
	public static final String					CUSTOM_DATA_ANALYZER_INFO	= "analyzerInfo";							//$NON-NLS-1$

	public static final String					X_AXIS_TIME					= "time";									//$NON-NLS-1$
	public static final String					X_AXIS_DISTANCE				= "distance";								//$NON-NLS-1$

	public static final int						GRAPH_ALTITUDE				= 1000;
	public static final int						GRAPH_SPEED					= 1001;
	public static final int						GRAPH_ALTIMETER				= 1002;
	public static final int						GRAPH_PULSE					= 1003;
	public static final int						GRAPH_TEMPERATURE			= 1004;
	public static final int						GRAPH_CADENCE				= 1005;
	public static final int						GRAPH_GRADIENT				= 1006;
	public static final int						GRAPH_POWER					= 1007;
	public static final int						GRAPH_PACE					= 1008;
	public static final int						GRAPH_TOUR_COMPARE			= 2000;

	public static final int[]					allGraphIDs					= new int[] {
			GRAPH_ALTITUDE,
			GRAPH_SPEED,
			GRAPH_ALTIMETER,
			GRAPH_PULSE,
			GRAPH_TEMPERATURE,
			GRAPH_CADENCE,
			GRAPH_GRADIENT,
			GRAPH_POWER,
			GRAPH_PACE,
			GRAPH_TOUR_COMPARE												};

	private static final int					SPEED_DIVISOR				= 10;
	public static final int						PACE_DIVISOR				= 10;
	public static final int						GRADIENT_DIVISOR			= 10;

	private static TourManager					instance;

	/**
	 * contains the instance of the {@link TourDataEditorView} or <code>null</code> when this part
	 * is not opened
	 */
	private static TourDataEditorView			fTourDataEditorInstance;

	private ComputeChartValue					computeAltimeterAvg;
	private ComputeChartValue					computeGradientAvg;
	private ComputeChartValue					computePaceAvg;
	private ComputeChartValue					computePowerAvg;
	private ComputeChartValue					computeSpeedAvg;

	private final LinkedHashMap<Long, TourData>	fTourDataCache				= new LinkedHashMap<Long, TourData>();

// 
//	TourDataCache is disabled because it's much slower when tours are compared a 2nd or more times	
//	
//	private final TourDataCache			fTourDataCache			= new TourDataCache();

	private static final ListenerList			fTourEventListeners			= new ListenerList(ListenerList.IDENTITY);
	private static final ListenerList			fTourSaveListeners			= new ListenerList(ListenerList.IDENTITY);

	/**
	 * tour chart which shows the selected tour
	 */
	private TourChart							fActiveTourChart;

	public static float computeTourSpeed(final TourData tourData, final int startIndex, final int endIndex) {

		final int[] distanceSerie = tourData.getMetricDistanceSerie();
		final int[] timeSerie = tourData.timeSerie;

		if (timeSerie == null || timeSerie.length == 0) {
			return 0;
		}

		final int distance = distanceSerie[endIndex] - distanceSerie[startIndex];
		final int time = Math.max(0, timeSerie[endIndex]
				- timeSerie[startIndex]
				- tourData.getBreakTime(startIndex, endIndex));

		return (float) ((float) distance / time * 3.6);
	}

	/**
	 * create the tour chart configuration by reading the settings from the preferences
	 * 
	 * @return
	 */
	public static TourChartConfiguration createTourChartConfiguration() {

		final TourChartConfiguration chartConfig = new TourChartConfiguration(true);

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		// convert the graph ids from the preferences into visible graphs in
		// the chart panel configuration
		final String[] prefGraphIds = StringToArrayConverter.convertStringToArray(prefStore.getString(ITourbookPreferences.GRAPH_VISIBLE));
		for (final String prefGraphId : prefGraphIds) {
			chartConfig.addVisibleGraph(Integer.valueOf(prefGraphId));
		}

		// set the unit which is shown on the x-axis
		if (prefStore.getString(ITourbookPreferences.GRAPH_X_AXIS).equals(X_AXIS_TIME)) {
			chartConfig.showTimeOnXAxis = true;
		} else {
			chartConfig.showTimeOnXAxis = false;
		}
		chartConfig.showTimeOnXAxisBackup = chartConfig.showTimeOnXAxis;

		chartConfig.isStartTime = prefStore.getBoolean(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME);
		chartConfig.isSRTMDataVisible = prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_SRTM_VISIBLE);

		updateZoomOptionsInChartConfig(chartConfig, prefStore);

		return chartConfig;
	}

	public static void fireEvent(final TourEventId tourEventId) {
		fireEvent(tourEventId, null);
	}

	public static void fireEvent(final TourEventId tourEventId, final ArrayList<TourData> modifiedTours) {
		fireEvent(tourEventId, new TourEvent(modifiedTours));
	}

	public static void fireEvent(final TourEventId tourEventId, final Object eventData) {

		final Object[] allListeners = fTourEventListeners.getListeners();
		for (final Object listener : allListeners) {
			((ITourEventListener) listener).tourChanged(null, tourEventId, eventData);
		}
	}

	public static void fireEvent(final TourEventId tourEventId, final Object eventData, final IWorkbenchPart part) {

		final Object[] allListeners = fTourEventListeners.getListeners();
		for (final Object listener : allListeners) {
			((ITourEventListener) listener).tourChanged(part, tourEventId, eventData);
		}
	}

	public static TourManager getInstance() {

		if (instance == null) {
			instance = new TourManager();
		}

		return instance;
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
	 * @return Returns the instance of the {@link TourDataEditorView} or <code>null</code> when this
	 *         part is not opened
	 */
	public static TourDataEditorView getTourDataEditor() {
		return fTourDataEditorInstance;
	}

	/**
	 * @return returns the date of the tour
	 */
	public static DateTime getTourDate(final TourData tourData) {

		return new DateTime(tourData.getStartYear(),
				tourData.getStartMonth(),
				tourData.getStartDay(),
				tourData.getStartHour(),
				tourData.getStartMinute(),
				tourData.getStartSecond(),
				0);
	}

	public static String getTourDateFull(final TourData tourData) {

		final Calendar calendar = GregorianCalendar.getInstance();

		calendar.set(tourData.getStartYear(), tourData.getStartMonth() - 1, tourData.getStartDay());

		return UI.DateFormatterFull.format(calendar.getTime());
	}

	private static String getTourDateLong(final Date date) {
		return UI.DateFormatterLong.format(date.getTime());
	}

	/**
	 * @return returns the date of this tour
	 */
	public static String getTourDateShort(final TourData tourData) {

		final Calendar calendar = GregorianCalendar.getInstance();

		calendar.set(tourData.getStartYear(), tourData.getStartMonth() - 1, tourData.getStartDay());

		return UI.DateFormatterShort.format(calendar.getTime());
	}

	private static String getTourTimeShort(final Date date) {
		return UI.TimeFormatterShort.format(date.getTime());
	}

	/**
	 * @return returns the date of this tour
	 */
	public static String getTourTimeShort(final TourData tourData) {

		final Calendar calendar = GregorianCalendar.getInstance();

		calendar.set(tourData.getStartYear(),
				tourData.getStartMonth() - 1,
				tourData.getStartDay(),
				tourData.getStartHour(),
				tourData.getStartMinute());

		return UI.TimeFormatterShort.format(calendar.getTime());
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
		return getTourDateLong(getTourDate(tourData).toDate())//
				+ UI.DASH_WITH_SPACE
				+ getTourTimeShort(tourData);
	}

	/**
	 * @return returns the detailed title of this tour (displayed as chart title)
	 */
	public static String getTourTitleDetailed(final TourData tourData) {

		final String tourTitle = tourData.getTourTitle();

		return getTourDateFull(tourData) + //
				" - " //$NON-NLS-1$
				+ getTourTimeShort(tourData)
				+ ((tourTitle.length() == 0) ? UI.EMPTY_STRING : UI.DASH_WITH_SPACE + tourTitle); //$NON-NLS-1$ //$NON-NLS-2$ 
	}

	/**
	 * Saves tours which have been modified and updates the tour data editor, fires a
	 * {@link TourEventId#TOUR_CHANGED} event.
	 * <p>
	 * If a tour is openend in the {@link TourDataEditorView}, the tour will be saved only when the
	 * tour is not dirty, if the tour is dirty, saving is not done.
	 * <p>
	 * Event {@link TourEventId#TOUR_CHANGED} is always fired.
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
	private static TourData saveModifiedTour(final TourData tourData, final boolean canFireNotification) {

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
	 * {@link TourManager#TOUR_CHANGED} event.<br>
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
	 * {@link TourManager#TOUR_CHANGED} event.<br>
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

		TourData tourDataEditorSavedTour = null;
		boolean fireChangeEvent = false;
		final ArrayList<TourData> savedTours = new ArrayList<TourData>();

		for (final TourData tourData : modifiedTours) {

			boolean doSaveTour = false;
			TourData savedTour = null;

			final TourDataEditorView tourDataEditor = getTourDataEditor();
			if (tourDataEditor != null) {

				final TourData tourDataInEditor = tourDataEditor.getTourData();

				try {
					UI.checkTourData(tourData, tourDataInEditor);
				} catch (final MyTourbookException e) {
					e.printStackTrace();
				}

				if (tourDataInEditor == tourData) {

					// selected tour is in the tour data editor

					if (tourDataEditor.isDirty()) {

						// tour in the editor is already dirty, tour MUST BE SAVED IN THE TOUR EDITOR

						savedTour = tourData;

						/*
						 * make the tour data editor visible, it could be hidden and confuses the
						 * user when the changes are not visible
						 */
						UI.openTourEditor(false);

					} else {

						/*
						 * tour in the editor is not dirty, save tour and update editor ui
						 */

						savedTour = TourDatabase.saveTour(tourData);

						/*
						 * set flag for the tour data editor that the tour is saved and the ui is
						 * updated
						 */
						tourDataEditorSavedTour = savedTour;
					}

					/*
					 * update UI in the tour data editor with the modified tour data
					 */
					tourDataEditor.updateUI(savedTour);

					fireChangeEvent = true;

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
				savedTour = TourDatabase.saveTour(tourData);

				fireChangeEvent = true;
			}

			if (savedTour != null) {
				savedTours.add(savedTour);
			}
		}

		if (canFireNotification && fireChangeEvent) {
			final TourEvent propertyData = new TourEvent(savedTours);
			propertyData.tourDataEditorSavedTour = tourDataEditorSavedTour;
			fireEvent(TourEventId.TOUR_CHANGED, propertyData);
		}

		return savedTours;
	}

	/**
	 * set the graph colors from the pref store
	 * 
	 * @param prefStore
	 * @param yData
	 * @param graphName
	 */
	public static void setGraphColor(	final IPreferenceStore prefStore,
										final ChartDataYSerie yData,
										final String graphName) {

		final String prefGraphName = ITourbookPreferences.GRAPH_COLORS + graphName + "."; //$NON-NLS-1$

		final RGB lineColor = PreferenceConverter.getColor(prefStore, //
				prefGraphName + GraphColorProvider.PREF_COLOR_LINE);

		yData.setDefaultRGB(lineColor);

		yData.setRgbLine(new RGB[] { lineColor });

		yData.setRgbDark(new RGB[] { PreferenceConverter.getColor(prefStore, prefGraphName
				+ GraphColorProvider.PREF_COLOR_DARK) });

		yData.setRgbBright(new RGB[] { PreferenceConverter.getColor(prefStore, prefGraphName
				+ GraphColorProvider.PREF_COLOR_BRIGHT) });
	}

	public static void setTourDataEditor(final TourDataEditorView tourDataEditorView) {
		fTourDataEditorInstance = tourDataEditorView;
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

	private TourManager() {}

	public void addTourEventListener(final ITourEventListener listener) {
		fTourEventListeners.add(listener);
	}

	/**
	 * Tour save listeners will be called to save tours before the application is shut down
	 * 
	 * @param listener
	 */
	public void addTourSaveListener(final ITourSaveListener listener) {
		fTourSaveListeners.add(listener);
	}

	/**
	 * adjust the min/max values to make them more visible and not at the same vertical position as
	 * the x-axis or the top of the chart
	 */
	private void adjustMinMax(final ChartDataYSerie yData) {

		yData.setVisibleMaxValue(yData.getVisibleMaxValue() + 1);

		final int visibleMinValue = yData.getVisibleMinValue();
		if (visibleMinValue > 0) {
			yData.setVisibleMinValue(visibleMinValue - 1);
		}
	}

	/**
	 * Remove all {@link TourData} from the cache so they need to be reloaded the next time with
	 * {@link #getTourData} from the database.
	 * <p>
	 * When this method is called and a tour is modified in the tour editor, the calling method is
	 * responsible to update the tour in the tour editor:
	 */
	public void clearTourDataCache() {

		fTourDataCache.clear();

		if (fTourDataEditorInstance != null && fTourDataEditorInstance.isDirty()) {

			final TourData tourDataInEditor = fTourDataEditorInstance.getTourData();
			if (tourDataInEditor != null) {

				// keep modified tour in cache
				fTourDataCache.put(tourDataInEditor.getTourId(), tourDataInEditor);
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
		final int[] gradientSerie = tourData.gradientSerie;

		final int[] speedSerie = tourData.getSpeedSerie();
		final int[] paceSerie = tourData.getPaceSerie();
		final int[] altimeterSerie = tourData.getAltimeterSerie();
		final int[] distanceSerie = tourData.getDistanceSerie();

		if ((timeSerie == null || timeSerie.length == 0)
				|| speedSerie == null
				|| paceSerie == null
				|| altimeterSerie == null
				|| distanceSerie == null
				|| gradientSerie == null) {
			return;
		}

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		int clippingTime;
		if (prefStore.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_VALUE_CLIPPING)) {
			// use custom clipping
			clippingTime = prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_VALUE_CLIPPING_TIMESLICE);
		} else {
			// use internal clipping, value was evaluated with experiments
			clippingTime = 15;
		}

		int paceClipping;
		if (prefStore.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_PACE_CLIPPING)) {
			// use custom clipping
			paceClipping = prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_PACE_CLIPPING_VALUE);
		} else {
			// use internal clipping, value was evaluated with experiments
			paceClipping = 15;
		}

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

				final int distance = distanceSerie[sliceIndex] - distanceSerie[serieIndex];

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
				int distDiff = 0;

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

		computeSpeedAvg = new ComputeChartValue() {

			/*
			 * Compute the average speed in km/h between the two sliders
			 */
			@Override
			public float compute() {

				final Object customDataDistance = chartModel.getCustomData(TourManager.CUSTOM_DATA_DISTANCE);
				if (customDataDistance == null) {
					return 0;
				}

				final int[] timeValues = ((ChartDataSerie) (chartModel.getCustomData(TourManager.CUSTOM_DATA_TIME))).getHighValues()[0];
				final int[] distanceValues = ((ChartDataSerie) (customDataDistance)).getHighValues()[0];
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

				final int leftDistance = distanceValues[valueIndexLeft];
				final int rightDistance = distanceValues[valueIndexRight];
				final int leftTime = timeValues[valueIndexLeft];
				final int rightTime = timeValues[valueIndexRight];

				if (leftTime == rightTime) {

					// left and right slider are at the same position
					return 0;

				} else {

					final float time = Math.max(0, rightTime
							- leftTime
							- tourData.getBreakTime(valueIndexLeft, valueIndexRight));
					final float distance = rightDistance - leftDistance;

					final float speed = distance / time * 3.6f;

					return speed;
				}

			}
		};

		computePaceAvg = new ComputeChartValue() {

			/*
			 * Compute the average pace between two sliders
			 */
			@Override
			public float compute() {

				final Object customDataDistance = chartModel.getCustomData(TourManager.CUSTOM_DATA_DISTANCE);
				if (customDataDistance == null) {
					return 0;
				}

				final int[] timeValues = ((ChartDataSerie) (chartModel.getCustomData(TourManager.CUSTOM_DATA_TIME))).getHighValues()[0];
				final int[] distanceValues = ((ChartDataSerie) (customDataDistance)).getHighValues()[0];
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

				final int leftDistance = distanceValues[valueIndexLeft];
				final int rightDistance = distanceValues[valueIndexRight];
				final int leftTime = timeValues[valueIndexLeft];
				final int rightTime = timeValues[valueIndexRight];

				if (leftTime == rightTime) {

					// left and right slider are at the same position
					return 0;

				} else {

					final float time = Math.max(0, rightTime
							- leftTime
							- tourData.getBreakTime(valueIndexLeft, valueIndexRight));
					final float distance = rightDistance - leftDistance;

					if (distance == 0) {
						return 0;
					} else {
						return (float) (time * 16.666 / distance);
					}
				}
			}
		};

		computeAltimeterAvg = new ComputeChartValue() {

			/*
			 * Compute the average altimeter speed between the two sliders
			 */
			@Override
			public float compute() {

				final Object customDataAltitude = chartModel.getCustomData(TourManager.CUSTOM_DATA_ALTITUDE);
				if (customDataAltitude == null) {
					return 0;
				}

				final int[] altitudeValues = ((ChartDataSerie) (customDataAltitude)).getHighValues()[0];
				final int[] timeValues = ((ChartDataSerie) (chartModel.getCustomData(TourManager.CUSTOM_DATA_TIME))).getHighValues()[0];
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

				final int leftAltitude = altitudeValues[valueIndexLeft];
				final int rightAltitude = altitudeValues[valueIndexRight];
				final int leftTime = timeValues[valueIndexLeft];
				final int rightTime = timeValues[valueIndexRight];

				if (leftTime == rightTime) {

					// left and right slider are at the same position
					return 0;

				} else {

					final float time = Math.max(0, rightTime
							- leftTime
							- tourData.getBreakTime(valueIndexLeft, valueIndexRight));

					return (((rightAltitude - leftAltitude) / time) * 3600);
				}
			}
		};

		computeGradientAvg = new ComputeChartValue() {

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

				final int[] altitudeValues = ((ChartDataSerie) (customDataAltitude)).getHighValues()[0];
				final int[] distanceValues = ((ChartDataSerie) (customDataDistance)).getHighValues()[0];

				final int leftAltitude = altitudeValues[valueIndexLeft];
				final int rightAltitude = altitudeValues[valueIndexRight];
				final int leftDistance = distanceValues[valueIndexLeft];
				final int rightDistance = distanceValues[valueIndexRight];

				if (leftDistance == rightDistance) {
					// left and right slider are at the same position
					return 0;
				} else {
					return (float) ((rightAltitude - leftAltitude)) / (rightDistance - leftDistance) * 100;
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

		return createChartDataModelInternal(tourData, chartConfig, false);
	}

	public ChartDataModel createChartDataModel(	final TourData tourData,
												final TourChartConfiguration chartConfig,
												final boolean hasPropertyChanged) {

		return createChartDataModelInternal(tourData, chartConfig, hasPropertyChanged);
	}

	private ChartDataModel createChartDataModelInternal(final TourData tourData,
														final TourChartConfiguration chartConfig,
														final boolean hasPropertyChanged) {

		// check if the callbacks are created
		if (computeSpeedAvg == null) {
			createAvgCallbacks();
		}

		final ChartDataModel chartDataModel = new ChartDataModel(ChartDataModel.CHART_TYPE_LINE);

		if (tourData.timeSerie == null || tourData.timeSerie.length == 0) {
			return chartDataModel;
		}

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		if (hasPropertyChanged) {
			tourData.clearComputedSeries();
		}

		tourData.computeSpeedSerie();
		tourData.computeAltimeterGradientSerie();

		computeValueClipping(tourData);

		/*
		 * distance
		 */

		final int[] distanceSerie = tourData.getDistanceSerie();
		ChartDataXSerie xDataDistance = null;
		if (distanceSerie != null) {
			xDataDistance = new ChartDataXSerie(distanceSerie);
			xDataDistance.setLabel(Messages.tour_editor_label_distance);
			xDataDistance.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
			xDataDistance.setValueDivisor(1000);
			xDataDistance.setDefaultRGB(new RGB(0, 0, 0));
		}

		/*
		 * time
		 */
		final ChartDataXSerie xDataTime = new ChartDataXSerie(tourData.timeSerie);
		xDataTime.setLabel(Messages.tour_editor_label_time);
		xDataTime.setUnitLabel(Messages.tour_editor_label_time_unit);
		xDataTime.setDefaultRGB(new RGB(0, 0, 0));
		xDataTime.setAxisUnit(ChartDataXSerie.AXIS_UNIT_HOUR_MINUTE_SECOND);

		/*
		 * show the distance on the x-axis when a distance is available, otherwise the time is
		 * displayed
		 */
		boolean showTimeOnXAxis;
		if (xDataDistance == null) {
			showTimeOnXAxis = true;
			chartConfig.isForceTimeOnXAxis = true;
		} else {
			showTimeOnXAxis = chartConfig.showTimeOnXAxisBackup;
			chartConfig.isForceTimeOnXAxis = false;
		}
		chartConfig.showTimeOnXAxis = showTimeOnXAxis;

		if (showTimeOnXAxis) {

			// time is displayed on the X axis

			chartDataModel.setXData(xDataTime);

			chartDataModel.addXyData(xDataTime);

			if (xDataDistance != null) {
				chartDataModel.setXData2nd(xDataDistance);
				chartDataModel.addXyData(xDataDistance);
			}

			/*
			 * when time is displayed, the x-axis can show the start time starting from 0 or from
			 * the current time of the day
			 */
			final int startTime = chartConfig.isStartTime ? //
					(tourData.getStartHour() * 3600) + (tourData.getStartMinute() * 60)
					: 0;

			xDataTime.setStartValue(startTime);

		} else {

			// distance is available and is displayed on the x axis

			chartDataModel.setXData(xDataDistance);
			chartDataModel.setXData2nd(xDataTime);

			chartDataModel.addXyData(xDataDistance);
			chartDataModel.addXyData(xDataTime);
		}

		final int chartType = prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_CHARTTYPE);

		/*
		 * altitude
		 */
		ChartDataYSerie yDataAltitude = null;

		final int[] altitudeSerie = tourData.getAltitudeSerie();
		if (altitudeSerie != null) {

			if (tourData.isSRTMAvailable()) {

				chartConfig.canShowSRTMData = true;

				if (chartConfig.isSRTMDataVisible) {

					final int[] srtmDataSerie = tourData.getSRTMSerie();
					if (srtmDataSerie != null) {

						// create altitude dataserie and adjust min/max values with with the srtm values
						yDataAltitude = createChartDataSerie(new int[][] { altitudeSerie, srtmDataSerie }, chartType);
					}
				}

			} else {

				// SRTM data are not available
				chartConfig.canShowSRTMData = false;
			}

			if (yDataAltitude == null) {
				yDataAltitude = createChartDataSerie(altitudeSerie, chartType);
			}

			yDataAltitude.setYTitle(Messages.Graph_Label_Altitude);
			yDataAltitude.setUnitLabel(UI.UNIT_LABEL_ALTITUDE);
			yDataAltitude.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
			yDataAltitude.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_ALTITUDE);
			yDataAltitude.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true));
			yDataAltitude.setShowYSlider(true);

			setGraphColor(prefStore, yDataAltitude, GraphColorProvider.PREF_GRAPH_ALTITUDE);
			adjustMinMax(yDataAltitude);
			chartDataModel.addXyData(yDataAltitude);
		}

		/*
		 * heartbeat
		 */
		ChartDataYSerie yDataPulse = null;

		final int[] pulseSerie = tourData.pulseSerie;
		if (pulseSerie != null) {

			yDataPulse = createChartDataSerie(pulseSerie, chartType);

			yDataPulse.setYTitle(Messages.Graph_Label_Heartbeat);
			yDataPulse.setUnitLabel(Messages.Graph_Label_Heartbeat_unit);
			yDataPulse.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
			yDataPulse.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_PULSE);
			yDataPulse.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true));
			yDataPulse.setShowYSlider(true);

			setGraphColor(prefStore, yDataPulse, GraphColorProvider.PREF_GRAPH_HEARTBEAT);
			chartDataModel.addXyData(yDataPulse);
		}

		/*
		 * speed
		 */
		final int[] speedSerie = tourData.getSpeedSerie();
		ChartDataYSerie yDataSpeed = null;
		if (speedSerie != null) {

			yDataSpeed = createChartDataSerie(speedSerie, chartType);

			yDataSpeed.setYTitle(Messages.Graph_Label_Speed);
			yDataSpeed.setUnitLabel(UI.UNIT_LABEL_SPEED);
			yDataSpeed.setValueDivisor(SPEED_DIVISOR);
			yDataSpeed.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
			yDataSpeed.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_SPEED);
			yDataSpeed.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true,
					true,
					computeSpeedAvg,
					2));
			yDataSpeed.setShowYSlider(true);

			setGraphColor(prefStore, yDataSpeed, GraphColorProvider.PREF_GRAPH_SPEED);
			chartDataModel.addXyData(yDataSpeed);
		}

		/*
		 * pace
		 */
		final int[] paceSerie = tourData.getPaceSerie();
		ChartDataYSerie yDataPace = null;
		if (paceSerie != null) {

			yDataPace = createChartDataSerie(paceSerie, chartType);

			yDataPace.setYTitle(Messages.Graph_Label_Pace);
			yDataPace.setUnitLabel(UI.UNIT_LABEL_PACE);
			yDataPace.setValueDivisor(PACE_DIVISOR);
			yDataPace.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
			yDataPace.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_PACE);
			yDataPace.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true, true, computePaceAvg, 1));
			yDataPace.setShowYSlider(true);

			setGraphColor(prefStore, yDataPace, GraphColorProvider.PREF_GRAPH_PACE);
			chartDataModel.addXyData(yDataPace);

			// adjust pace when it's defined in the pref store
			if (prefStore.getBoolean(ITourbookPreferences.GRAPH_PACE_MINMAX_IS_ENABLED)) {

				yDataPace.setVisibleMinValue(prefStore.getInt(ITourbookPreferences.GRAPH_PACE_MIN_VALUE) * PACE_DIVISOR,
						true);

				// set max value after min value
				yDataPace.setVisibleMaxValue(prefStore.getInt(ITourbookPreferences.GRAPH_PACE_MAX_VALUE) * PACE_DIVISOR,
						true);
			}
		}

		/*
		 * power
		 */
		final int[] powerSerie = tourData.getPowerSerie();
		ChartDataYSerie yDataPower = null;
		if (powerSerie != null) {

			yDataPower = createChartDataSerie(powerSerie, chartType);

			yDataPower.setYTitle(Messages.Graph_Label_Power);
			yDataPower.setUnitLabel(Messages.Graph_Label_Power_unit);
			yDataPower.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
			yDataPower.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_POWER);
			yDataPower.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true,
					false,
					computePowerAvg,
					0));
			yDataPower.setShowYSlider(true);

			setGraphColor(prefStore, yDataPower, GraphColorProvider.PREF_GRAPH_POWER);
			chartDataModel.addXyData(yDataPower);
		}

		/*
		 * altimeter
		 */
		final int[] altimeterSerie = tourData.getAltimeterSerie();
		ChartDataYSerie yDataAltimeter = null;
		if (altimeterSerie != null) {

			yDataAltimeter = createChartDataSerie(altimeterSerie, chartType);

			yDataAltimeter.setYTitle(Messages.Graph_Label_Altimeter);
			yDataAltimeter.setUnitLabel(UI.UNIT_LABEL_ALTIMETER);
			yDataAltimeter.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_ZERO);
			yDataAltimeter.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_ALTIMETER);
			yDataAltimeter.setCustomData(CUSTOM_DATA_ANALYZER_INFO,
					new TourChartAnalyzerInfo(true, computeAltimeterAvg));
			yDataAltimeter.setShowYSlider(true);
			setGraphColor(prefStore, yDataAltimeter, GraphColorProvider.PREF_GRAPH_ALTIMETER);
			chartDataModel.addXyData(yDataAltimeter);

			// adjust min altitude when it's defined in the pref store
			if (prefStore.getBoolean(ITourbookPreferences.GRAPH_ALTIMETER_MIN_IS_ENABLED)) {
				yDataAltimeter.setVisibleMinValue(prefStore.getInt(ITourbookPreferences.GRAPH_ALTIMETER_MIN_VALUE),
						true);
			}
		}

		/*
		 * gradient
		 */
		final int[] gradientSerie = tourData.gradientSerie;
		ChartDataYSerie yDataGradient = null;
		if (gradientSerie != null) {

			yDataGradient = createChartDataSerie(gradientSerie, chartType);

			yDataGradient.setYTitle(Messages.Graph_Label_Gradient);
			yDataGradient.setUnitLabel(Messages.Graph_Label_Gradiend_unit);
			yDataGradient.setValueDivisor(GRADIENT_DIVISOR);
			yDataGradient.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_ZERO);
			yDataGradient.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_GRADIENT);
			yDataGradient.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true,
					true,
					computeGradientAvg,
					1));
			yDataGradient.setShowYSlider(true);
			setGraphColor(prefStore, yDataGradient, GraphColorProvider.PREF_GRAPH_GRADIENT);
			chartDataModel.addXyData(yDataGradient);

			// adjust min value when defined in the pref store
			if (prefStore.getBoolean(ITourbookPreferences.GRAPH_GRADIENT_MIN_IS_ENABLED)) {
				yDataGradient.setVisibleMinValue(prefStore.getInt(ITourbookPreferences.GRAPH_GRADIENT_MIN_VALUE)
						* GRADIENT_DIVISOR, true);
			}
		}

		/*
		 * cadence
		 */
		final int[] cadenceSerie = tourData.cadenceSerie;
		ChartDataYSerie yDataCadence = null;
		if (cadenceSerie != null) {

			yDataCadence = createChartDataSerie(cadenceSerie, chartType);

			yDataCadence.setYTitle(Messages.Graph_Label_Cadence);
			yDataCadence.setUnitLabel(Messages.Graph_Label_Cadence_unit);
			yDataCadence.setShowYSlider(true);
			yDataCadence.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
			yDataCadence.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_CADENCE);
			yDataCadence.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true));
			setGraphColor(prefStore, yDataCadence, GraphColorProvider.PREF_GRAPH_CADENCE);
			chartDataModel.addXyData(yDataCadence);
		}

		/*
		 * temperature
		 */
		final int[] temperatureSerie = tourData.getTemperatureSerie();
		ChartDataYSerie yDataTemperature = null;
		if (temperatureSerie != null) {

			yDataTemperature = createChartDataSerie(temperatureSerie, chartType);

			yDataTemperature.setYTitle(Messages.Graph_Label_Temperature);
			yDataTemperature.setUnitLabel(UI.UNIT_LABEL_TEMPERATURE);
			yDataTemperature.setShowYSlider(true);
			yDataTemperature.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
			yDataTemperature.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_TEMPERATURE);
			yDataTemperature.setCustomData(CUSTOM_DATA_ANALYZER_INFO, new TourChartAnalyzerInfo(true, true));
			setGraphColor(prefStore, yDataTemperature, GraphColorProvider.PREF_GRAPH_TEMPTERATURE);
			adjustMinMax(yDataTemperature);
			chartDataModel.addXyData(yDataTemperature);
		}

		/*
		 * tour compare altitude difference
		 */
		final int[] tourCompareSerie = tourData.tourCompareSerie;
		ChartDataYSerie yDataTourCompare = null;
		if (tourCompareSerie != null && tourCompareSerie.length > 0) {

			yDataTourCompare = createChartDataSerie(tourCompareSerie, chartType);

			yDataTourCompare.setYTitle(Messages.Graph_Label_Tour_Compare);
			yDataTourCompare.setUnitLabel(Messages.Graph_Label_Tour_Compare_unit);
			yDataTourCompare.setShowYSlider(true);
			yDataTourCompare.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
			yDataTourCompare.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_TOUR_COMPARE);
			setGraphColor(prefStore, yDataTourCompare, GraphColorProvider.PREF_GRAPH_TOUR_COMPARE);
			chartDataModel.addXyData(yDataTourCompare);
		}

		/*
		 * all visible graphs are added as y-data to the chart data model in the sequence as they
		 * were activated
		 */
		for (final int actionId : chartConfig.getVisibleGraphs()) {

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

		chartDataModel.setCustomData(CUSTOM_DATA_TIME, xDataTime);
		chartDataModel.setCustomData(CUSTOM_DATA_DISTANCE, xDataDistance);

		chartDataModel.setCustomData(CUSTOM_DATA_TOUR_ID, tourData.getTourId());

		return chartDataModel;
	}

	private ChartDataYSerie createChartDataSerie(final int[] dataSerie, final int chartType) {

		if (chartType == 0 || chartType == ChartDataModel.CHART_TYPE_LINE) {
			return new ChartDataYSerie(ChartDataModel.CHART_TYPE_LINE, dataSerie);

		} else {
			return new ChartDataYSerie(ChartDataModel.CHART_TYPE_LINE_WITH_BARS, dataSerie);
		}
	}

	private ChartDataYSerie createChartDataSerie(final int[][] dataSerie, final int chartType) {

		if (chartType == 0 || chartType == ChartDataModel.CHART_TYPE_LINE) {
			return new ChartDataYSerie(ChartDataModel.CHART_TYPE_LINE, dataSerie);
		} else {
			return new ChartDataYSerie(ChartDataModel.CHART_TYPE_LINE_WITH_BARS, dataSerie);
		}
	}

	/**
	 * @param tourData
	 * @param useNormalizedData
	 */
	// public void createTour(final TourData tourData) {
	//
	// openTourEditor(createTourEditorInput(tourData));
	// }
	/**
	 * @param tourData
	 */
	public void createTour(TourData tourData) {

		if (tourData.getTourPerson() != null) {
			// load tour from database
			tourData = TourManager.getInstance().getTourData(tourData.getTourId());
		}
	}

	public TourChart getActiveTourChart() {
		return fActiveTourChart;
	}

	/**
	 * Get a tour from the cache, the cache is necessary because getting a tour from the database
	 * creates always a new instance
	 * 
	 * @param requestedTourId
	 * @return Returns the tour data for the tour id or <code>null</code> when the tour is not in
	 *         the database
	 */
	public TourData getTourData(final Long requestedTourId) {

		if (requestedTourId == null) {
			return null;
		}

		/*
		 * get tour from tour editor when it contains the requested tour
		 */
		if (fTourDataEditorInstance != null) {

			final TourData tourDataInEditor = fTourDataEditorInstance.getTourData();
			if (tourDataInEditor != null && tourDataInEditor.getTourId() == requestedTourId) {

				// cache tour data
				fTourDataCache.put(tourDataInEditor.getTourId(), tourDataInEditor);

				return tourDataInEditor;
			}
		}

		/*
		 * get tour from cache or database
		 */
		TourData availableTourData = null;

		final TourData tourDataInCache = fTourDataCache.get(requestedTourId);
		if (tourDataInCache != null) {
			availableTourData = tourDataInCache;
		} else {

			final TourData tourDataFromDb = TourDatabase.getTourFromDb(requestedTourId);

			if (tourDataFromDb == null) {
				return null;
			}

			// cache tour data
			fTourDataCache.put(tourDataFromDb.getTourId(), tourDataFromDb);

			availableTourData = tourDataFromDb;
		}

		if (availableTourData != null) {
			replaceTourInTourEditor(availableTourData);
		}

		return availableTourData;
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
	public void openTourInEditor(final Long tourId) {

		if (tourId == null) {
			return;
		}

		try {
			PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow()
					.getActivePage()
					.openEditor(new TourEditorInput(tourId), TourEditor.ID, true);

		} catch (final PartInitException e) {
			e.printStackTrace();
		}
	}

	public void removeAllToursFromCache() {

//		final ArrayList<TourData> modifiedTour = new ArrayList<TourData>(fTourDataCache.values());

		fTourDataCache.clear();

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
			fTourEventListeners.remove(listener);
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
		fTourDataCache.remove(tourId);
	}

	public void removeTourSaveListener(final ITourSaveListener listener) {
		if (listener != null) {
			fTourSaveListeners.remove(listener);
		}
	}

	/**
	 * Check tour in tour editor, when the tour is modified and it contains a wrong tourData
	 * instance, show an error, otherwise replace (silently) the tour data in the editor
	 */
	private void replaceTourInTourEditor(final TourData tourDataForEditor) {

		if (tourDataForEditor == null || fTourDataEditorInstance == null) {
			return;
		}

		final TourData tourDataInEditor = fTourDataEditorInstance.getTourData();
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
		if (fTourDataEditorInstance.isDirty()) {

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
					.append("However it isn't very easy to find out, what actions are exactly done, before this error occured. ") //$NON-NLS-1$
					.append(UI.NEW_LINE2)
					.append("These actions must be reproducable otherwise the bug cannot be identified."); //$NON-NLS-1$

			MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error: Out of Synch", sb.toString()); //$NON-NLS-1$

		} else {

			/*
			 * silently replace tour data in editor
			 */
			fTourDataEditorInstance.setTourData(tourDataForEditor);
		}
	}

	/**
	 * Before the application is shut down, the tour save listeners are called to save unsaved data.
	 * 
	 * @return Returns <code>true</code> when the tours have been saved or false when it was
	 *         canceled by the user
	 */
	public boolean saveTours() {

		final Object[] allListeners = fTourSaveListeners.getListeners();
		for (final Object tourSaveListener : allListeners) {
			if (((ITourSaveListener) tourSaveListener).saveTour() == false) {
				return false;
			}
		}

		return true;
	}

	public void setActiveTourChart(final TourChart tourChart) {
		fActiveTourChart = tourChart;
	}

	public void updateTourInCache(final TourData tourData) {

		if (tourData == null) {
			return;
		}

		fTourDataCache.put(tourData.getTourId(), tourData);

		replaceTourInTourEditor(tourData);
	}

}
