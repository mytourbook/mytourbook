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
package net.tourbook.tour;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ComputeChartValue;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.ChangedTags;
import net.tourbook.ui.UI;
import net.tourbook.ui.views.TourChartAnalyzerInfo;
import net.tourbook.util.StringToArrayConverter;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class TourManager {

	/**
	 * properties of the tour chart has been changed
	 */
	public static final int						TOUR_CHART_PROPERTY_IS_MODIFIED			= 10;

	/**
	 * 
	 */
	public static final int						TOUR_PROPERTY_SEGMENT_LAYER_CHANGED		= 20;

	/**
	 * 
	 */
	public static final int						TOUR_PROPERTY_REFERENCE_TOUR_CHANGED	= 30;

	/**
	 * 
	 */
	public static final int						TOUR_PROPERTY_COMPARE_TOUR_CHANGED		= 40;

	/**
	 * The property data contains an {@link ArrayList} with all {@link TourData} which have been
	 * modified
	 */
	public static final int						TOUR_PROPERTIES_CHANGED					= 50;

	/**
	 * Tags for a tour has been modified. The property data contains an object {@link ChangedTags}
	 * which contains the tags and the modified tours
	 */
	public static final int						TOUR_TAGS_CHANGED						= 60;

	/**
	 * structure of the tags changed, this includes add/remove of tags and categories and
	 * tag/category renaming
	 */
	public static final int						TAG_STRUCTURE_CHANGED					= 70;

	/**
	 * Sliders in the tourchart moved. Property data contains {@link SelectionChartInfo} with the
	 * position of the sliders
	 */
	public static final int						SLIDER_POSITION_CHANGED					= 80;

	/**
	 * All computed data for all tours are modified
	 */
	public static final int						ALL_TOURS_ARE_MODIFIED					= 90;

	public static final String					CUSTOM_DATA_TIME						= "time";									//$NON-NLS-1$
	public static final String					CUSTOM_DATA_DISTANCE					= "distance";								//$NON-NLS-1$
	public static final String					CUSTOM_DATA_ALTITUDE					= "altitude";								//$NON-NLS-1$
	public static final String					CUSTOM_DATA_SPEED						= "speed";									//$NON-NLS-1$
	public static final String					CUSTOM_DATA_PACE						= "pace";									//$NON-NLS-1$
	public static final String					CUSTOM_DATA_POWER						= "power";									//$NON-NLS-1$
	public static final String					CUSTOM_DATA_GRADIENT					= "gradient";								//$NON-NLS-1$
	public static final String					CUSTOM_DATA_ALTIMETER					= "altimeter";								//$NON-NLS-1$
	public static final String					CUSTOM_DATA_PULSE						= "pulse";									//$NON-NLS-1$

	public static final String					CUSTOM_DATA_TOUR_DATA					= "tourdata";								//$NON-NLS-1$

	public static final String					ANALYZER_INFO							= "AnalyzerInfo";							//$NON-NLS-1$
	public static final String					X_AXIS_TIME								= "time";									//$NON-NLS-1$
	public static final String					X_AXIS_DISTANCE							= "distance";								//$NON-NLS-1$

	public static final int						GRAPH_ALTITUDE							= 1000;
	public static final int						GRAPH_SPEED								= 1001;
	public static final int						GRAPH_ALTIMETER							= 1002;
	public static final int						GRAPH_PULSE								= 1003;
	public static final int						GRAPH_TEMPERATURE						= 1004;
	public static final int						GRAPH_CADENCE							= 1005;
	public static final int						GRAPH_GRADIENT							= 1006;
	public static final int						GRAPH_POWER								= 1007;
	public static final int						GRAPH_PACE								= 1008;
	public static final int						GRAPH_TOUR_COMPARE						= 2000;

	public static final int[]					allGraphIDs								= new int[] {
			GRAPH_ALTITUDE,
			GRAPH_SPEED,
			GRAPH_ALTIMETER,
			GRAPH_PULSE,
			GRAPH_TEMPERATURE,
			GRAPH_CADENCE,
			GRAPH_GRADIENT,
			GRAPH_POWER,
			GRAPH_PACE,
			GRAPH_TOUR_COMPARE															};

	public static final int						GRADIENT_DIVISOR						= 10;

	private static TourManager					instance;

	private ComputeChartValue					computeAltimeterAvg;
	private ComputeChartValue					computeGradientAvg;
	private ComputeChartValue					computePaceAvg;
	private ComputeChartValue					computePowerAvg;
	private ComputeChartValue					computeSpeedAvg;

	private final LinkedHashMap<Long, TourData>	fTourDataMap							= new LinkedHashMap<Long, TourData>();

	private static final ListenerList			fPropertyListeners						= new ListenerList(ListenerList.IDENTITY);

	/**
	 * tour chart which shows the selected tour
	 */
	private TourChart							fActiveTourChart;

	public static float computeTourSpeed(final TourData tourData, final int startIndex, final int endIndex) {

		final int[] distanceSerie = tourData.getMetricDistanceSerie();
		final int[] timeSerie = tourData.timeSerie;

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

		// set the starttime from the prefs
		chartConfig.isStartTime = prefStore.getBoolean(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME);

		updateZoomOptionsInChartConfig(chartConfig, prefStore);

		return chartConfig;
	}

	public static void firePropertyChange(final int propertyId, final Object propertyData) {
		final Object[] allListeners = fPropertyListeners.getListeners();
		for (final Object allListener : allListeners) {
			final ITourPropertyListener listener = (ITourPropertyListener) allListener;
			listener.propertyChanged(propertyId, propertyData);
		}
	}

	public static TourManager getInstance() {

		if (instance == null) {
			instance = new TourManager();
		}

		return instance;
	}

	/**
	 * @return returns the date of this tour
	 */
	public static String getTourDate(final TourData tourData) {

		final Calendar calendar = GregorianCalendar.getInstance();

		calendar.set(tourData.getStartYear(), tourData.getStartMonth() - 1, tourData.getStartDay());

		return UI.DateFormatterShort.format(calendar.getTime());
	}

	public static String getTourDateFull(final TourData tourData) {

		final Calendar calendar = GregorianCalendar.getInstance();

		calendar.set(tourData.getStartYear(), tourData.getStartMonth() - 1, tourData.getStartDay());

		return UI.DateFormatterFull.format(calendar.getTime());
	}

	/**
	 * @return returns the date of this tour
	 */
	public static String getTourTime(final TourData tourData) {

		final Calendar calendar = GregorianCalendar.getInstance();

		calendar.set(tourData.getStartYear(),
				tourData.getStartMonth() - 1,
				tourData.getStartDay(),
				tourData.getStartHour(),
				tourData.getStartMinute());

		return UI.TimeFormatterShort.format(calendar.getTime());
	}

	/**
	 * @return returns the detailed title of this tour (displayed as chart title)
	 */
	public static String getTourTitleDetailed(final TourData tourData) {

		final String tourTitle = tourData.getTourTitle();

		return getTourDateFull(tourData) + //
				" - " //$NON-NLS-1$
				+ getTourTime(tourData)
				+ ((tourTitle.length() == 0) ? "" : " - " + tourTitle); //$NON-NLS-1$ //$NON-NLS-2$ 
	}

	/**
	 * Check if the selected tours are opened in an editor
	 * 
	 * @param selectedTours
	 *            contains the tours which will be checked
	 * @return Returns <code>true</code> when the tours are saved or <code>false</code> when the
	 *         user prevented to save the editors
	 */
	public static boolean saveTourEditors(final ArrayList<TourData> selectedTours) {

		final ArrayList<IEditorPart> openedEditors = UI.getOpenedEditors();
		final ArrayList<IEditorPart> editorsToBeSaved = new ArrayList<IEditorPart>();

		// check if a tour is opened in an editor
		for (final IEditorPart editorPart : openedEditors) {
			if (editorPart instanceof TourEditor) {

				final IEditorInput editorInput = editorPart.getEditorInput();

				if (editorInput instanceof TourEditorInput) {

					final TourEditor tourEditor = (TourEditor) editorPart;
					final long editorTourId = ((TourEditorInput) editorInput).getTourId();

					for (final TourData tourData : selectedTours) {
						if (editorTourId == tourData.getTourId()) {

							// a tour editor was found containing the current tour

							if (tourEditor.isDirty()) {
								editorsToBeSaved.add(tourEditor);
							}
						}
					}
				}
			}
		}

		// tour editors must be saved when they contain a tour which will be modified
		if (editorsToBeSaved.size() > 0) {

			if (MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), //
					Messages.app_action_dlg_confirm_save_editors_title,
					Messages.app_action_dlg_confirm_save_editors_message)) {

				// save all editors
				for (final IEditorPart editorPart : editorsToBeSaved) {
					editorPart.doSave(null);
				}

				return true;

			} else {
				return false;
			}

		} else {
			return true;
		}

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

	/**
	 * update the zoom options in the chart configuration from the pref store
	 * 
	 * @param chartConfig
	 * @param prefStore
	 */
	static void updateZoomOptionsInChartConfig(	final TourChartConfiguration chartConfig,
												final IPreferenceStore prefStore) {

		chartConfig.autoZoomToSlider = prefStore.getBoolean(ITourbookPreferences.GRAPH_ZOOM_AUTO_ZOOM_TO_SLIDER);
		chartConfig.moveSlidersWhenZoomed = prefStore.getBoolean(ITourbookPreferences.GRAPH_MOVE_SLIDERS_WHEN_ZOOMED);
	}

	private TourManager() {}

	public void addPropertyListener(final ITourPropertyListener listener) {
		fPropertyListeners.add(listener);
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
	 * Remove all {@link TourData} from the cache so they need to be reloaded the next time in
	 * {@link #getTourData} the database
	 */
	public void clearTourDataCache() {
		fTourDataMap.clear();
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

		if (speedSerie == null
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

				final int[] distanceValues = ((ChartDataSerie) (customDataDistance)).getHighValues()[0];
				final int[] timeValues = ((ChartDataSerie) (chartModel.getCustomData(TourManager.CUSTOM_DATA_TIME))).getHighValues()[0];
				final TourData tourData = (TourData) chartModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_DATA);

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

				final int[] distanceValues = ((ChartDataSerie) (customDataDistance)).getHighValues()[0];
				final int[] timeValues = ((ChartDataSerie) (chartModel.getCustomData(TourManager.CUSTOM_DATA_TIME))).getHighValues()[0];
				final TourData tourData = (TourData) chartModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_DATA);

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
				final TourData tourData = (TourData) chartModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_DATA);

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

		if (tourData.timeSerie.length == 0) {
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
			xDataDistance.setLabel(Messages.Tour_Properties_Label_distance);
			xDataDistance.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
			xDataDistance.setValueDivisor(1000);
			xDataDistance.setDefaultRGB(new RGB(0, 0, 0));
		}

		/*
		 * time
		 */
		final ChartDataXSerie xDataTime = new ChartDataXSerie(tourData.timeSerie);
		xDataTime.setLabel(Messages.Tour_Properties_Label_time);
		xDataTime.setUnitLabel(Messages.Tour_Properties_Label_time_unit);
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
		final int[] altitudeSerie = tourData.getAltitudeSerie();
		ChartDataYSerie yDataAltitude = null;
		if (altitudeSerie != null) {

			yDataAltitude = getChartData(altitudeSerie, chartType);

			yDataAltitude.setYTitle(Messages.Graph_Label_Altitude);
			yDataAltitude.setUnitLabel(UI.UNIT_LABEL_ALTITUDE);
			yDataAltitude.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
			yDataAltitude.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_ALTITUDE);
			yDataAltitude.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true));
			yDataAltitude.setShowYSlider(true);

			setGraphColor(prefStore, yDataAltitude, GraphColorProvider.PREF_GRAPH_ALTITUDE);
			adjustMinMax(yDataAltitude);
			chartDataModel.addXyData(yDataAltitude);
		}

		/*
		 * heartbeat
		 */
		final int[] pulseSerie = tourData.pulseSerie;
		ChartDataYSerie yDataPulse = null;
		if (pulseSerie != null) {

			yDataPulse = getChartData(pulseSerie, chartType);

			yDataPulse.setYTitle(Messages.Graph_Label_Heartbeat);
			yDataPulse.setUnitLabel(Messages.Graph_Label_Heartbeat_unit);
			yDataPulse.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
			yDataPulse.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_PULSE);
			yDataPulse.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true));
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

			yDataSpeed = getChartData(speedSerie, chartType);

			yDataSpeed.setYTitle(Messages.Graph_Label_Speed);
			yDataSpeed.setUnitLabel(UI.UNIT_LABEL_SPEED);
			yDataSpeed.setValueDivisor(10);
			yDataSpeed.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
			yDataSpeed.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_SPEED);
			yDataSpeed.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true, true, computeSpeedAvg, 2));
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

			yDataPace = getChartData(paceSerie, chartType);

			yDataPace.setYTitle(Messages.Graph_Label_Pace);
			yDataPace.setUnitLabel(UI.UNIT_LABEL_PACE);
			yDataPace.setValueDivisor(10);
			yDataPace.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
			yDataPace.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_PACE);
			yDataPace.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true, true, computePaceAvg, 1));
			yDataPace.setShowYSlider(true);

			setGraphColor(prefStore, yDataPace, GraphColorProvider.PREF_GRAPH_PACE);
			chartDataModel.addXyData(yDataPace);
		}

		/*
		 * power
		 */
		final int[] powerSerie = tourData.getPowerSerie();
		ChartDataYSerie yDataPower = null;
		if (powerSerie != null) {

			yDataPower = getChartData(powerSerie, chartType);

			yDataPower.setYTitle(Messages.Graph_Label_Power);
			yDataPower.setUnitLabel(Messages.Graph_Label_Power_unit);
			yDataPower.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
			yDataPower.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_POWER);
			yDataPower.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true, false, computePowerAvg, 0));
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

			yDataAltimeter = getChartData(altimeterSerie, chartType);

			yDataAltimeter.setYTitle(Messages.Graph_Label_Altimeter);
			yDataAltimeter.setUnitLabel(UI.UNIT_LABEL_ALTIMETER);
			yDataAltimeter.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_ZERO);
			yDataAltimeter.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_ALTIMETER);
			yDataAltimeter.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true, computeAltimeterAvg));
			yDataAltimeter.setShowYSlider(true);
			setGraphColor(prefStore, yDataAltimeter, GraphColorProvider.PREF_GRAPH_ALTIMETER);
			chartDataModel.addXyData(yDataAltimeter);

			// adjust min altitude when it's defined in the pref store
			if (prefStore.getBoolean(ITourbookPreferences.GRAPH_ALTIMETER_MIN_ENABLED)) {
				yDataAltimeter.setVisibleMinValue(prefStore.getInt(ITourbookPreferences.GRAPH_ALTIMETER_MIN_VALUE));
			}
		}

		/*
		 * gradient
		 */
		final int[] gradientSerie = tourData.gradientSerie;
		ChartDataYSerie yDataGradient = null;
		if (gradientSerie != null) {

			yDataGradient = getChartData(gradientSerie, chartType);

			yDataGradient.setYTitle(Messages.Graph_Label_Gradient);
			yDataGradient.setUnitLabel(Messages.Graph_Label_Gradiend_unit);
			yDataGradient.setValueDivisor(GRADIENT_DIVISOR);
			yDataGradient.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_ZERO);
			yDataGradient.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_GRADIENT);
			yDataGradient.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true, true, computeGradientAvg, 1));
			yDataGradient.setShowYSlider(true);
			setGraphColor(prefStore, yDataGradient, GraphColorProvider.PREF_GRAPH_GRADIENT);
			chartDataModel.addXyData(yDataGradient);

			// adjust min value when defined in the pref store
			if (prefStore.getBoolean(ITourbookPreferences.GRAPH_GRADIENT_MIN_ENABLED)) {
				yDataGradient.setVisibleMinValue(prefStore.getInt(ITourbookPreferences.GRAPH_GRADIENT_MIN_VALUE)
						* GRADIENT_DIVISOR);
			}
		}

		/*
		 * cadence
		 */
		final int[] cadenceSerie = tourData.cadenceSerie;
		ChartDataYSerie yDataCadence = null;
		if (cadenceSerie != null) {

			yDataCadence = getChartData(cadenceSerie, chartType);

			yDataCadence.setYTitle(Messages.Graph_Label_Cadence);
			yDataCadence.setUnitLabel(Messages.Graph_Label_Cadence_unit);
			yDataCadence.setShowYSlider(true);
			yDataCadence.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
			yDataCadence.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_CADENCE);
			yDataCadence.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true));
			setGraphColor(prefStore, yDataCadence, GraphColorProvider.PREF_GRAPH_CADENCE);
			chartDataModel.addXyData(yDataCadence);
		}

		/*
		 * temperature
		 */
		final int[] temperatureSerie = tourData.getTemperatureSerie();
		ChartDataYSerie yDataTemperature = null;
		if (temperatureSerie != null) {

			yDataTemperature = getChartData(temperatureSerie, chartType);

			yDataTemperature.setYTitle(Messages.Graph_Label_Temperature);
			yDataTemperature.setUnitLabel(UI.UNIT_LABEL_TEMPERATURE);
			yDataTemperature.setShowYSlider(true);
			yDataTemperature.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
			yDataTemperature.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_TEMPERATURE);
			yDataTemperature.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true, true));
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

			yDataTourCompare = getChartData(tourCompareSerie, chartType);

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

			case GRAPH_PULSE:
				if (yDataPulse != null) {
					chartDataModel.addYData(yDataPulse);
					chartDataModel.setCustomData(CUSTOM_DATA_PULSE, yDataPulse);
				}
				break;

			case GRAPH_TEMPERATURE:
				if (yDataTemperature != null) {
					chartDataModel.addYData(yDataTemperature);
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
		chartDataModel.setCustomData(CUSTOM_DATA_TOUR_DATA, tourData);

		return chartDataModel;
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

	private ChartDataYSerie getChartData(final int[] dataSerie, final int chartType) {

		ChartDataYSerie chartDataSerie;

		if (chartType == 0 || chartType == ChartDataModel.CHART_TYPE_LINE) {
			chartDataSerie = new ChartDataYSerie(ChartDataModel.CHART_TYPE_LINE, dataSerie);

		} else {
			chartDataSerie = new ChartDataYSerie(ChartDataModel.CHART_TYPE_LINE_WITH_BARS, dataSerie);
		}
		return chartDataSerie;
	}

	/**
	 * Get a tour from the cache, the cache is necessary because getting a tour from the database
	 * creates always a new instance
	 * 
	 * @param tourId
	 * @return Returns the tour data for the tour id or <code>null</code> when the tour is not in
	 *         the database
	 */
	public TourData getTourData(final Long tourId) {

		if (tourId == null) {
			return null;
		}

		final TourData tourDataInMap = fTourDataMap.get(tourId);
		if (tourDataInMap != null) {
			return tourDataInMap;
		}

		final TourData tourData = TourDatabase.getTourFromDb(tourId);

		if (tourData == null) {
			return null;
		}

		// keep the tour data
		updateTourInCache(tourData);

		return tourData;
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
		fTourDataMap.values().clear();
	}

	public void removePropertyListener(final ITourPropertyListener listener) {
		if (listener != null) {
			fPropertyListeners.remove(listener);
		}
	}

	public void removeTourFromCache(final Long tourId) {
		fTourDataMap.remove(tourId);
	}

	public void setActiveTourChart(final TourChart tourChart) {
		fActiveTourChart = tourChart;
	}

	public void updateTourInCache(final TourData tourData) {
		fTourDataMap.put(tourData.getTourId(), tourData);
	}

}
