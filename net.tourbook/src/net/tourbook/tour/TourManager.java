/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
import java.util.HashMap;

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ComputeChartValue;
import net.tourbook.colors.GraphColors;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.UI;
import net.tourbook.ui.views.TourChartAnalyzerInfo;
import net.tourbook.util.StringToArrayConverter;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class TourManager {

	/**
	 * tour was changed and saved in the database
	 */
	public static final int					TOUR_PROPERTY_CHART_IS_MODIFIED				= 10;
	public static final int					TOUR_PROPERTY_SEGMENT_LAYER_CHANGED			= 20;
	public static final int					TOUR_PROPERTY_REFERENCE_TOUR_CHANGED		= 30;
	public static final int					TOUR_PROPERTY_COMPARE_TOUR_CHANGED			= 40;

	/**
	 * content of the property data is an {@link ArrayList} with {@link TourData}
	 */
	public static final int					TOUR_PROPERTY_TOUR_TYPE_CHANGED				= 50;
	public static final int					TOUR_PROPERTY_TOUR_TYPE_CHANGED_IN_EDITOR	= 60;

	public static final String				CUSTOM_DATA_TIME							= "time";									//$NON-NLS-1$
	public static final String				CUSTOM_DATA_DISTANCE						= "distance";								//$NON-NLS-1$
	public static final String				CUSTOM_DATA_ALTITUDE						= "altitude";								//$NON-NLS-1$
	public static final String				CUSTOM_DATA_SPEED							= "speed";									//$NON-NLS-1$
	public static final String				CUSTOM_DATA_GRADIENT						= "gradient";								//$NON-NLS-1$
	public static final String				CUSTOM_DATA_ALTIMETER						= "altimeter";								//$NON-NLS-1$
	public static final String				CUSTOM_DATA_PULSE							= "pulse";									//$NON-NLS-1$

	public static final String				ANALYZER_INFO								= "AnalyzerInfo";							//$NON-NLS-1$
	public static final String				X_AXIS_TIME									= "time";									//$NON-NLS-1$
	public static final String				X_AXIS_DISTANCE								= "distance";								//$NON-NLS-1$

	public static final int					GRAPH_ALTITUDE								= 1000;
	public static final int					GRAPH_SPEED									= 1001;
	public static final int					GRAPH_ALTIMETER								= 1002;
	public static final int					GRAPH_PULSE									= 1003;
	public static final int					GRAPH_TEMPERATURE							= 1004;
	public static final int					GRAPH_CADENCE								= 1005;
	public static final int					GRAPH_GRADIENT								= 1006;
	public static final int					GRAPH_POWER									= 1007;
	public static final int					GRAPH_PACE									= 1008;
	public static final int					GRAPH_TOUR_COMPARE							= 2000;

	public static final int					GRADIENT_DIVISOR							= 10;

	private static TourManager				instance;

	private ComputeChartValue				computeSpeedAvg;
	private ComputeChartValue				computePaceAvg;
	private ComputeChartValue				computeAltimeterAvg;
	private ComputeChartValue				computeGradientAvg;

	private final HashMap<Long, TourData>	fTourDataMap								= new HashMap<Long, TourData>();

	private final ListenerList				fPropertyListeners							= new ListenerList(ListenerList.IDENTITY);

	private TourManager() {}

	/**
	 * Compute the speed between start and end index
	 * 
	 * @param chartDataModel
	 * @param startIndex
	 * @param endIndex
	 * @return Returns the speed between start and end index
	 */
	public static float computeTourSpeed(	final ChartDataModel chartDataModel,
											final int startIndex,
											final int endIndex) {

		final int[] distanceValues = ((ChartDataXSerie) chartDataModel.getCustomData(TourManager.CUSTOM_DATA_DISTANCE)).getHighValues()[0];
		final int[] timeValues = ((ChartDataXSerie) chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TIME)).getHighValues()[0];

		return computeTourSpeed(distanceValues, timeValues, startIndex, endIndex);
	}

	public static float computeTourSpeed(	final int[] distanceSerie,
											final int[] timeSerie,
											final int startIndex,
											final int endIndex) {

		final int distance = distanceSerie[endIndex] - distanceSerie[startIndex];
		int time = timeSerie[endIndex] - timeSerie[startIndex];

		int timeInterval = timeSerie[1] - timeSerie[0];
		if (timeInterval == 0 && timeSerie.length > 1) {
			timeInterval = timeSerie[2] - timeSerie[1];
		}
		// remove breaks from the time
		final int ignoreTimeSlices = timeInterval == 0 ? 0 : getIgnoreTimeSlices(timeSerie,
				startIndex,
				endIndex,
				10 / timeInterval);

		time = time - (ignoreTimeSlices * timeInterval);

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

	/**
	 * calculate the driving time, ignore the time when the distance is 0 within a time period which
	 * is defined by <code>sliceMin</code>
	 * 
	 * @param distanceValues
	 * @param indexLeft
	 * @param indexRight
	 * @param sliceMin
	 * @return Returns the number of slices which can be ignored
	 */
	public static int getIgnoreTimeSlices(	final int[] distanceValues,
											final int indexLeft,
											int indexRight,
											int sliceMin) {
		int ignoreTimeCounter = 0;
		int oldDistance = 0;
		sliceMin = Math.max(sliceMin, 1);
		indexRight = Math.min(indexRight, distanceValues.length - 1);

		for (int valueIndex = indexLeft; valueIndex <= indexRight; valueIndex++) {

			if (distanceValues[valueIndex] == oldDistance) {
				ignoreTimeCounter++;
			}

			int oldIndex = valueIndex - sliceMin;
			if (oldIndex < 0) {
				oldIndex = 0;
			}
			oldDistance = distanceValues[oldIndex];
		}
		return ignoreTimeCounter;
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

		return UI.DateFormatter.format(calendar.getTime());
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

		return UI.TimeFormatter.format(calendar.getTime());
	}

	/**
	 * @return returns the detailed title of this tour (displayed as chart title)
	 */
	public static String getTourTitleDetailed(final TourData tourData) {

		final String tourTitle = tourData.getTourTitle();

		return getTourDate(tourData)
				+ " - " + getTourTime(tourData) + ((tourTitle.length() == 0) ? "" : " - " + tourTitle); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * set the graph colors from the pref store
	 * 
	 * @param prefStore
	 * @param yDataSerie
	 * @param graphName
	 */
	public static void setGraphColor(	final IPreferenceStore prefStore,
										final ChartDataYSerie yDataSerie,
										final String graphName) {

		final String prefGraphName = ITourbookPreferences.GRAPH_COLORS + graphName + "."; //$NON-NLS-1$

		yDataSerie.setRgbLine(new RGB[] { PreferenceConverter.getColor(prefStore, prefGraphName
				+ GraphColors.PREF_COLOR_LINE) });

		yDataSerie.setRgbDark(new RGB[] { PreferenceConverter.getColor(prefStore, prefGraphName
				+ GraphColors.PREF_COLOR_DARK) });

		yDataSerie.setRgbBright(new RGB[] { PreferenceConverter.getColor(prefStore, prefGraphName
				+ GraphColors.PREF_COLOR_BRIGHT) });
	}

	/**
	 * update the zoom options in the chart configuration from the pref store
	 * 
	 * @param chartConfig
	 * @param prefStore
	 */
	static void updateZoomOptionsInChartConfig(	final TourChartConfiguration chartConfig,
												final IPreferenceStore prefStore) {

		// get pref store settings
		boolean scrollZoomedGraph = prefStore.getBoolean(ITourbookPreferences.GRAPH_ZOOM_SCROLL_ZOOMED_GRAPH);
		boolean autoZoomToSlider = prefStore.getBoolean(ITourbookPreferences.GRAPH_ZOOM_AUTO_ZOOM_TO_SLIDER);

		// prevent setting both zoom options to true
		if (scrollZoomedGraph) {
			autoZoomToSlider = false;
		}
		if (autoZoomToSlider) {
			scrollZoomedGraph = false;
		}

		chartConfig.scrollZoomedGraph = scrollZoomedGraph;
		chartConfig.autoZoomToSlider = autoZoomToSlider;
	}

	public void addPropertyListener(final ITourPropertyListener listener) {
		fPropertyListeners.add(listener);
	}

	/**
	 * adjust the min/max values to make them more visible and not at the same position as the
	 * x-axis or the top of the chart
	 */
	private void adjustMinMax(final ChartDataYSerie yData) {

		yData.setVisibleMaxValue(yData.getVisibleMaxValue() + 1);

		if (yData.getVisibleMinValue() > 0) {
			yData.setVisibleMinValue(yData.getVisibleMinValue() - 1);
		}
	}

	/**
	 * Clip values when a minimum distance is fallen short of
	 * 
	 * @param tourData
	 */
	private void computeValueClipping(final TourData tourData) {

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

		final int[] timeSerie = tourData.timeSerie;
		final int[] gradientSerie = tourData.gradientSerie;

		final int[] speedSerie = tourData.getSpeedSerie();
		final int[] paceSerie = tourData.getPaceSerie();
		final int[] altimeterSerie = tourData.getAltimeterSerie();
		final int[] distanceSerie = tourData.getDistanceSerie();

		final int serieLength = timeSerie.length;

		int deviceTimeInterval = tourData.getDeviceTimeInterval();
		if (deviceTimeInterval > 0) {

			/*
			 * clipping for constanct time intervals
			 */

			final int slices = Math.max(1, clippingTime / deviceTimeInterval);

			for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

				// adjust index to the array size
				int sliceIndex = serieIndex + slices;
				sliceIndex = Math.min(Math.max(0, sliceIndex), serieLength - 1);

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
				int lowIndex = Math.max(0, serieIndex - 1);

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
	private void createChartAvgCallbacks() {

		computeSpeedAvg = new ComputeChartValue() {

			/*
			 * Compute the average distance speed between the two sliders
			 */
			@Override
			public float compute() {

				final int[] distanceValues = ((ChartDataSerie) (chartModel.getCustomData(TourManager.CUSTOM_DATA_DISTANCE))).getHighValues()[0];
				final int[] timeValues = ((ChartDataSerie) (chartModel.getCustomData(TourManager.CUSTOM_DATA_TIME))).getHighValues()[0];

				final int leftDistance = distanceValues[valuesIndexLeft];
				final int rightDistance = distanceValues[valuesIndexRight];
				final int leftTime = timeValues[valuesIndexLeft];
				final int rightTime = timeValues[valuesIndexRight];

				if (leftTime == rightTime) {
					// left and right slider are at the same position
					return 0;

				} else {

					int timeSlice = timeValues[1] - timeValues[0];

					if (timeSlice == 0 && timeValues.length > 1) {
						timeSlice = timeValues[2] - timeValues[1];
					}

					if (timeSlice > 0) {

						final int ignoreTimeSlices = timeSlice == 0
								? 0
								: getIgnoreTimeSlices(distanceValues,
										valuesIndexLeft,
										valuesIndexRight,
										10 / timeSlice);
						final float time = rightTime - leftTime - (ignoreTimeSlices * timeSlice);

						final float distance = rightDistance - leftDistance;
						final float speed = distance / time * 3.6f;

						return speed;
					}

					return 0;
				}

			}
		};

		computePaceAvg = new ComputeChartValue() {

			/*
			 * Compute the average pace between two sliders
			 */
			@Override
			public float compute() {

				final int[] distanceValues = ((ChartDataSerie) (chartModel.getCustomData(TourManager.CUSTOM_DATA_DISTANCE))).getHighValues()[0];
				final int[] timeValues = ((ChartDataSerie) (chartModel.getCustomData(TourManager.CUSTOM_DATA_TIME))).getHighValues()[0];

				final int leftDistance = distanceValues[valuesIndexLeft];
				final int rightDistance = distanceValues[valuesIndexRight];
				final int leftTime = timeValues[valuesIndexLeft];
				final int rightTime = timeValues[valuesIndexRight];

				if (leftTime == rightTime) {
					// left and right slider are at the same position
					return 0;

				} else {

					int timeSlice = timeValues[1] - timeValues[0];

					if (timeSlice == 0 && timeValues.length > 1) {
						timeSlice = timeValues[2] - timeValues[1];
					}

					if (timeSlice > 0) {

						final int ignoreTimeSlices = timeSlice == 0
								? 0
								: getIgnoreTimeSlices(distanceValues,
										valuesIndexLeft,
										valuesIndexRight,
										10 / timeSlice);
						final float time = rightTime - leftTime - (ignoreTimeSlices * timeSlice);

						final float distance = rightDistance - leftDistance;
						final float speed = distance / time * 3.6f;

						return speed;
					}

					return 0;
				}

			}
		};

		computeAltimeterAvg = new ComputeChartValue() {

			/*
			 * Compute the average altimeter speed between the two sliders
			 */
			@Override
			public float compute() {

				final int[] altitudeValues = ((ChartDataSerie) (chartModel.getCustomData(TourManager.CUSTOM_DATA_ALTITUDE))).getHighValues()[0];

				final int[] timeValues = ((ChartDataSerie) (chartModel.getCustomData(TourManager.CUSTOM_DATA_TIME))).getHighValues()[0];

				final int leftAltitude = altitudeValues[valuesIndexLeft];
				final int rightAltitude = altitudeValues[valuesIndexRight];
				final int leftTime = timeValues[valuesIndexLeft];
				final int rightTime = timeValues[valuesIndexRight];

				if (leftTime == rightTime) {
					// left and right slider are at the same position
					return 0;
				} else {

					final int[] distanceValues = ((ChartDataSerie) (chartModel.getCustomData(TourManager.CUSTOM_DATA_DISTANCE))).getHighValues()[0];

					int timeSlice = timeValues[1] - timeValues[0];

					if (timeSlice == 0 && timeValues.length > 1) {
						timeSlice = timeValues[2] - timeValues[1];
					}

					if (timeSlice > 0) {
						final int ignoreTimeSlices = timeSlice == 0
								? 0
								: getIgnoreTimeSlices(distanceValues,
										valuesIndexLeft,
										valuesIndexRight,
										10 / timeSlice);
						final float time = rightTime - leftTime - (ignoreTimeSlices * timeSlice);

						return (((rightAltitude - leftAltitude) / time) * 3600);
					}

					return 0;
				}
			}
		};

		computeGradientAvg = new ComputeChartValue() {

			/*
			 * Compute the average altimeter speed between the two sliders
			 */
			@Override
			public float compute() {

				final int[] altitudeValues = ((ChartDataSerie) (chartModel.getCustomData(TourManager.CUSTOM_DATA_ALTITUDE))).getHighValues()[0];

				final int[] distanceValues = ((ChartDataSerie) (chartModel.getCustomData(TourManager.CUSTOM_DATA_DISTANCE))).getHighValues()[0];

				final int leftAltitude = altitudeValues[valuesIndexLeft];
				final int rightAltitude = altitudeValues[valuesIndexRight];
				final int leftDistance = distanceValues[valuesIndexLeft];
				final int rightDistance = distanceValues[valuesIndexRight];

				if (leftDistance == rightDistance) {
					// left and right slider are at the same position
					return 0;
				} else {
					return (float) ((rightAltitude - leftAltitude))
							/ (rightDistance - leftDistance)
							* 100;
				}
			}
		};
	}

	/**
	 * Creates a chart data fDataModel from the tour data
	 * 
	 * @param tourData
	 *        data which contains the tour data
	 * @param tourChartProperty
	 * @param fTourChartConfig
	 * @return
	 */
	public ChartDataModel createChartDataModel(	final TourData tourData,
												final TourChartConfiguration chartConfig) {

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
//		long startTime = System.currentTimeMillis();

//		long endTime = System.currentTimeMillis();
//		System.out.println("Execution time : " + (endTime - startTime) + " ms");

		// check if the callbacks are created
		if (computeSpeedAvg == null) {
			createChartAvgCallbacks();
		}

		final ChartDataModel chartDataModel = new ChartDataModel(ChartDataModel.CHART_TYPE_LINE);

		if (tourData.timeSerie.length == 0) {
			return chartDataModel;
		}

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		if (hasPropertyChanged) {
			tourData.cleanComputedSeries();
		}

		tourData.computeAltimeterGradientSerie();
		tourData.computeSpeedSerie();

		computeValueClipping(tourData);

		/*
		 * distance
		 */

		final ChartDataXSerie xDataDistance = new ChartDataXSerie(tourData.getDistanceSerie());
		xDataDistance.setLabel(Messages.Tour_Properties_Label_distance);
		xDataDistance.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
		xDataDistance.setValueDivisor(1000);
		xDataDistance.setRgbLine(new RGB[] { new RGB(0, 0, 0) });

		/*
		 * time
		 */
		final ChartDataXSerie xDataTime = new ChartDataXSerie(tourData.timeSerie);
		xDataTime.setLabel(Messages.Tour_Properties_Label_time);
		xDataTime.setUnitLabel(Messages.Tour_Properties_Label_time_unit);
		xDataTime.setRgbLine(new RGB[] { new RGB(0, 0, 0) });
		xDataTime.setAxisUnit(ChartDataXSerie.AXIS_UNIT_HOUR_MINUTE_SECOND);

		/*
		 * show the distance on the x-axis when a distance is available, otherwise the time is
		 * displayed
		 */
		boolean showTimeOnXAxis;
		if (tourData.getTourDistance() == 0) {
			showTimeOnXAxis = true;

		} else {
			showTimeOnXAxis = chartConfig.showTimeOnXAxisBackup;
		}
		chartConfig.showTimeOnXAxis = showTimeOnXAxis;

		if (showTimeOnXAxis) {

			// time is displayed on the X axis

			chartDataModel.setXData(xDataTime);
			chartDataModel.setXData2nd(xDataDistance);

			chartDataModel.addXyData(xDataTime);
			chartDataModel.addXyData(xDataDistance);

			/*
			 * when time is displayed, the x-axis can show the start time starting from 0 or from
			 * the current time of the day
			 */
			final int startTime = chartConfig.isStartTime ? (tourData.getStartHour() * 3600)
					+ (tourData.getStartMinute() * 60) : 0;
			xDataTime.setStartValue(startTime);

		} else {

			// distance is displayed on the x axis

			chartDataModel.setXData(xDataDistance);
			chartDataModel.setXData2nd(xDataTime);

			chartDataModel.addXyData(xDataDistance);
			chartDataModel.addXyData(xDataTime);
		}

		final int chartType = prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_CHARTTYPE);

		/*
		 * altitude
		 */

		int[] altitudeSerie = tourData.altitudeSerie;
		final float unitValueAltitude = UI.UNIT_VALUE_ALTITUDE;

		if (unitValueAltitude != 1) {

			// use imperial system

			int[] altitudeSerieImperial = tourData.altitudeSerieImperial;

			if (altitudeSerieImperial == null) {

				// compute imperial data

				tourData.altitudeSerieImperial = altitudeSerieImperial = new int[altitudeSerie.length];

				for (int valueIndex = 0; valueIndex < altitudeSerie.length; valueIndex++) {
					altitudeSerieImperial[valueIndex] = (int) (altitudeSerie[valueIndex] / unitValueAltitude);
				}
			}
			altitudeSerie = altitudeSerieImperial;
		}

		final ChartDataYSerie yDataAltitude = getChartData(altitudeSerie, chartType);

		yDataAltitude.setYTitle(Messages.Graph_Label_Altitude);
		yDataAltitude.setUnitLabel(UI.UNIT_LABEL_ALTITUDE);
		yDataAltitude.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
		yDataAltitude.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_ALTITUDE);
		yDataAltitude.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true));
		yDataAltitude.setShowYSlider(true);
		setGraphColor(prefStore, yDataAltitude, GraphColors.PREF_GRAPH_ALTITUDE);
		adjustMinMax(yDataAltitude);
		chartDataModel.addXyData(yDataAltitude);

		/*
		 * speed
		 */
		final ChartDataYSerie yDataSpeed = getChartData(tourData.getSpeedSerie(), chartType);

		yDataSpeed.setYTitle(Messages.Graph_Label_Speed);
		yDataSpeed.setUnitLabel(UI.UNIT_LABEL_SPEED);
		yDataSpeed.setValueDivisor(10);
		yDataSpeed.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
		yDataSpeed.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_SPEED);
		yDataSpeed.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true,
				true,
				computeSpeedAvg,
				2));
		yDataSpeed.setShowYSlider(true);
		setGraphColor(prefStore, yDataSpeed, GraphColors.PREF_GRAPH_SPEED);
		chartDataModel.addXyData(yDataSpeed);

		/*
		 * pace
		 */
		final ChartDataYSerie yDataPace = getChartData(tourData.getPaceSerie(), chartType);

		yDataPace.setYTitle(Messages.Graph_Label_Pace);
		yDataPace.setUnitLabel(UI.UNIT_LABEL_PACE);
		yDataPace.setValueDivisor(10);
		yDataPace.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
		yDataPace.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_PACE);
		yDataPace.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true,
				true,
				computePaceAvg,
				2));
		yDataPace.setShowYSlider(true);
		setGraphColor(prefStore, yDataPace, GraphColors.PREF_GRAPH_PACE);
		chartDataModel.addXyData(yDataPace);

		/*
		 * heartbeat
		 */
		final ChartDataYSerie yDataPulse = getChartData(tourData.pulseSerie, chartType);
		yDataPulse.setYTitle(Messages.Graph_Label_Heartbeat);
		yDataPulse.setUnitLabel(Messages.Graph_Label_Heartbeat_unit);
		yDataPulse.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
		yDataPulse.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_PULSE);
		yDataPulse.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true));
		yDataPulse.setShowYSlider(true);
		setGraphColor(prefStore, yDataPulse, GraphColors.PREF_GRAPH_HEARTBEAT);
		chartDataModel.addXyData(yDataPulse);

		/*
		 * altimeter
		 */
		final ChartDataYSerie yDataAltimeter = getChartData(tourData.getAltimeterSerie(), chartType);
		yDataAltimeter.setYTitle(Messages.Graph_Label_Altimeter);
		yDataAltimeter.setUnitLabel(UI.UNIT_LABEL_ALTIMETER);
		yDataAltimeter.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_ZERO);
		yDataAltimeter.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_ALTIMETER);
		yDataAltimeter.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true,
				computeAltimeterAvg));
		yDataAltimeter.setShowYSlider(true);
		setGraphColor(prefStore, yDataAltimeter, GraphColors.PREF_GRAPH_ALTIMETER);
		chartDataModel.addXyData(yDataAltimeter);

		// adjust min altitude when it's defined in the pref store
		if (prefStore.getBoolean(ITourbookPreferences.GRAPH_ALTIMETER_MIN_ENABLED)) {
			yDataAltimeter.setVisibleMinValue(prefStore.getInt(ITourbookPreferences.GRAPH_ALTIMETER_MIN_VALUE));
		}

		/*
		 * gradient
		 */
		final ChartDataYSerie yDataGradient = getChartData(tourData.gradientSerie, chartType);
		yDataGradient.setYTitle(Messages.Graph_Label_Gradiend);
		yDataGradient.setUnitLabel(Messages.Graph_Label_Gradiend_unit);
		yDataGradient.setValueDivisor(GRADIENT_DIVISOR);
		yDataGradient.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_ZERO);
		yDataGradient.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_GRADIENT);
		yDataGradient.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true,
				true,
				computeGradientAvg,
				1));
		yDataGradient.setShowYSlider(true);
		setGraphColor(prefStore, yDataGradient, GraphColors.PREF_GRAPH_GRADIEND);
		chartDataModel.addXyData(yDataGradient);

		// adjust min value when defined in the pref store
		if (prefStore.getBoolean(ITourbookPreferences.GRAPH_GRADIENT_MIN_ENABLED)) {
			yDataGradient.setVisibleMinValue(prefStore.getInt(ITourbookPreferences.GRAPH_GRADIENT_MIN_VALUE)
					* GRADIENT_DIVISOR);
		}

		/*
		 * cadence
		 */
		final ChartDataYSerie yDataCadence = getChartData(tourData.cadenceSerie, chartType);
		yDataCadence.setYTitle(Messages.Graph_Label_Cadence);
		yDataCadence.setUnitLabel(Messages.Graph_Label_Cadence_unit);
		yDataCadence.setShowYSlider(true);
		yDataCadence.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
		yDataCadence.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_CADENCE);
		yDataCadence.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true));
		setGraphColor(prefStore, yDataCadence, GraphColors.PREF_GRAPH_CADENCE);
		chartDataModel.addXyData(yDataCadence);

		/*
		 * temperature
		 */
		final ChartDataYSerie yDataTemperature = getChartData(tourData.getTemperatureSerie(),
				chartType);
		yDataTemperature.setYTitle(Messages.Graph_Label_Temperature);
		yDataTemperature.setUnitLabel(UI.UNIT_LABEL_TEMPERATURE);
		yDataTemperature.setShowYSlider(true);
		yDataTemperature.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
		yDataTemperature.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_TEMPERATURE);
		yDataTemperature.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true, true));
		setGraphColor(prefStore, yDataTemperature, GraphColors.PREF_GRAPH_TEMPTERATURE);
		adjustMinMax(yDataTemperature);
		chartDataModel.addXyData(yDataTemperature);

		/*
		 * tour compare altitude difference
		 */
		ChartDataYSerie yDataTourCompare = null;
		if (tourData.tourCompareSerie != null && tourData.tourCompareSerie.length > 0) {
			yDataTourCompare = getChartData(tourData.tourCompareSerie, chartType);
			yDataTourCompare.setYTitle(Messages.Graph_Label_Tour_Compare);
			yDataTourCompare.setUnitLabel(Messages.Graph_Label_Tour_Compare_unit);
			yDataTourCompare.setShowYSlider(true);
			yDataTourCompare.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
			yDataTourCompare.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_TOUR_COMPARE);
			setGraphColor(prefStore, yDataTourCompare, GraphColors.PREF_GRAPH_TOUR_COMPARE);
			chartDataModel.addXyData(yDataTourCompare);
		}

		/*
		 * all visible graphs are added as y-data to the chart data model in the sequence as they
		 * were activated
		 */
		for (final int actionId : chartConfig.getVisibleGraphs()) {

			switch (actionId) {
			case GRAPH_ALTITUDE:
				chartDataModel.addYData(yDataAltitude);
				break;

			case GRAPH_SPEED:
				chartDataModel.addYData(yDataSpeed);
				break;

			case GRAPH_PACE:
				chartDataModel.addYData(yDataPace);
				break;

			case GRAPH_ALTIMETER:
				chartDataModel.addYData(yDataAltimeter);
				break;

			case GRAPH_GRADIENT:
				chartDataModel.addYData(yDataGradient);
				break;

			case GRAPH_CADENCE:
				chartDataModel.addYData(yDataCadence);
				break;

			case GRAPH_PULSE:
				chartDataModel.addYData(yDataPulse);
				break;

			case GRAPH_TEMPERATURE:
				chartDataModel.addYData(yDataTemperature);
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

		chartDataModel.setCustomData(CUSTOM_DATA_DISTANCE, xDataDistance);
		chartDataModel.setCustomData(CUSTOM_DATA_TIME, xDataTime);
		chartDataModel.setCustomData(CUSTOM_DATA_ALTITUDE, yDataAltitude);
		chartDataModel.setCustomData(CUSTOM_DATA_SPEED, yDataSpeed);
		chartDataModel.setCustomData(CUSTOM_DATA_GRADIENT, yDataGradient);
		chartDataModel.setCustomData(CUSTOM_DATA_ALTIMETER, yDataAltimeter);
		chartDataModel.setCustomData(CUSTOM_DATA_PULSE, yDataPulse);

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

	public void firePropertyChange(final int propertyId, final Object propertyData) {
		final Object[] allListeners = fPropertyListeners.getListeners();
		for (int i = 0; i < allListeners.length; i++) {
			final ITourPropertyListener listener = (ITourPropertyListener) allListeners[i];
			listener.propertyChanged(propertyId, propertyData);
		}
	}

	private ChartDataYSerie getChartData(final int[] dataSerie, final int chartType) {

		ChartDataYSerie chartDataSerie;

		if (chartType == 0 || chartType == ChartDataModel.CHART_TYPE_LINE) {
			chartDataSerie = new ChartDataYSerie(ChartDataModel.CHART_TYPE_LINE, dataSerie);

		} else {
			chartDataSerie = new ChartDataYSerie(ChartDataModel.CHART_TYPE_LINE_WITH_BARS,
					dataSerie);
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

		if (fTourDataMap.containsKey(tourId)) {
			return fTourDataMap.get(tourId);
		}

		final TourData tourData = TourDatabase.getTourData(tourId);

		// keep the tour data
		if (tourData != null) {
			fTourDataMap.put(tourId, tourData);
			return tourData;
		}

		return null;
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

	public void removePropertyListener(final ITourPropertyListener listener) {
		fPropertyListeners.remove(listener);
	}

	public void removeTourFromCache(final Long tourId) {
		fTourDataMap.remove(tourId);
	}

	public void removeAllToursFromCache() {
		fTourDataMap.values().clear();
	}

}
