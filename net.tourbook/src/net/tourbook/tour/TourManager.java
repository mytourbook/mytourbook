/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

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
import net.tourbook.ui.views.TourChartAnalyzerInfo;
import net.tourbook.ui.views.tourMap.TourDataNormalizer;
import net.tourbook.util.StringToArrayConverter;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;

public class TourManager {

	public static final String	CUSTOM_DATA_TIME		= "time";			//$NON-NLS-1$
	public static final String	CUSTOM_DATA_DISTANCE	= "distance";		//$NON-NLS-1$
	public static final String	CUSTOM_DATA_ALTITUDE	= "altitude";		//$NON-NLS-1$
	public static final String	CUSTOM_DATA_SPEED		= "speed";			//$NON-NLS-1$
	public static final String	CUSTOM_DATA_GRADIENT	= "gradient";		//$NON-NLS-1$
	public static final String	CUSTOM_DATA_ALTIMETER	= "altimeter";		//$NON-NLS-1$
	public static final String	CUSTOM_DATA_PULSE		= "pulse";			//$NON-NLS-1$

	public static final String	ANALYZER_INFO			= "AnalyzerInfo";	//$NON-NLS-1$
	public static final String	X_AXIS_TIME				= "time";			//$NON-NLS-1$
	public static final String	X_AXIS_DISTANCE			= "distance";		//$NON-NLS-1$

	public static final int		GRAPH_ALTITUDE			= 1000;
	public static final int		GRAPH_SPEED				= 1001;
	public static final int		GRAPH_ALTIMETER			= 1002;
	public static final int		GRAPH_PULSE				= 1003;
	public static final int		GRAPH_TEMPERATURE		= 1004;
	public static final int		GRAPH_CADENCE			= 1005;
	public static final int		GRAPH_GRADIENT			= 1006;
	public static final int		GRAPH_POWER				= 1007;

	private static TourManager	instance;

	private ComputeChartValue	computeSpeedAvg;
	private ComputeChartValue	computeAltimeterAvg;
	private ComputeChartValue	computeGradientAvg;

	/**
	 * local cache for tour editor inputs
	 */
	// private final HashMap<Long, TourEditorInput> tourCache = new
	// HashMap<Long, TourEditorInput>();
	private TourManager() {}

	/**
	 * create the tour chart configuration by reading the settings from the
	 * preferences
	 * 
	 * @return
	 */
	public static TourChartConfiguration createTourChartConfiguration() {

		final TourChartConfiguration chartConfig = new TourChartConfiguration();

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		// convert the graph ids from the preferences into visible graphs in
		// the chart panel configuration
		final String[] prefGraphIds = StringToArrayConverter.convertStringToArray(prefStore
				.getString(ITourbookPreferences.GRAPH_VISIBLE));
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

		// chartConfig.setMinMaxKeeper(true);

		return chartConfig;
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

		return DateFormat.getDateInstance().format(calendar.getTime());
	}

	/**
	 * @return returns the detailed title of this tour (displayed as chart
	 *         title)
	 */
	public static String getTourTitleDetailed(final TourData tourData) {

		return getTourDate(tourData)
				+ ((tourData.getTourTitle() != null) ? " - " + tourData.getTourTitle() : ""); //$NON-NLS-1$ //$NON-NLS-2$
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
		boolean scrollZoomedGraph = prefStore
				.getBoolean(ITourbookPreferences.GRAPH_ZOOM_SCROLL_ZOOMED_GRAPH);
		boolean autoZoomToSlider = prefStore
				.getBoolean(ITourbookPreferences.GRAPH_ZOOM_AUTO_ZOOM_TO_SLIDER);

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
	private int compareTour(final TourData compareTourData) {

		final TourDataNormalizer compareTourNormalizer = new TourDataNormalizer();
		final int[] compareTourDataDistance = compareTourData.distanceSerie;

		// normalize the tour which will be compared
		compareTourNormalizer.normalizeAltitude(
				compareTourData,
				0,
				compareTourDataDistance.length - 1);

		final int[] normCompDistances = compareTourNormalizer.getNormalizedDistance();
		final int[] normCompAltitudes = compareTourNormalizer.getNormalizedAltitude();
		final int[] normCompAltiDiff = new int[normCompAltitudes.length];
		final int[] normCompTour = new int[normCompAltitudes.length];

		/*
		 * reference tour
		 */

		Long refTourId;
		int refMeasureStartIndex;
		int refMeasureEndIndex;

		// Maur - Pfannenstiel
		// refTourId = 2005102416228826L;
		// refMeasureStartIndex = 50;
		// refMeasureEndIndex = 153;

		// Dübendorf - Greifensee
		// refTourId = 20059301618311L;
		// refMeasureStartIndex = 23;
		// refMeasureEndIndex = 132;

		// Egg - Pfannenstiel
		// refTourId = 2005102416228826L;
		// refMeasureStartIndex = 121;
		// refMeasureEndIndex = 167;

		// Maur - Forch
		refTourId = 200592616168216L;
		refMeasureStartIndex = 49;
		refMeasureEndIndex = 101;

		// get the reference tour
		final TourData refTourData = TourDatabase.getTourDataByTourId(refTourId);
		if (refTourData == null) {
			return -1;
		}

		// normalize the reference tour
		final TourDataNormalizer refTourNormalizer = new TourDataNormalizer();
		refTourNormalizer.normalizeAltitude(refTourData, refMeasureStartIndex, refMeasureEndIndex);

		final int[] refAltitudes = refTourNormalizer.getNormalizedAltitude();
		int minAltiDiff = Integer.MAX_VALUE;

		// start index of the reference tour in the compare tour
		int compareIndexStart = -1;

		final int compareLastIndex = normCompAltitudes.length;

		for (int compareIndex = 0; compareIndex < normCompAltitudes.length; compareIndex++) {

			int altitudeDiff = -1;

			// loop: all data in the reference tour
			for (int refIndex = 0; refIndex < refAltitudes.length; refIndex++) {

				final int compareRefIndex = compareIndex + refIndex;

				// make sure the ref index is not bigger than the compare index,
				// this can happen when the reference data exeed the compare
				// data
				if (compareRefIndex == compareLastIndex) {
					altitudeDiff = -1;
					break;
				}

				// get the altitude difference between the reference and the
				// measured value
				final int diffCompareRef = Math.abs(refAltitudes[refIndex]
						- normCompAltitudes[compareRefIndex]);

				altitudeDiff += diffCompareRef;
			}

			// save the altitude difference in the pulse data
			normCompAltiDiff[compareIndex] = altitudeDiff;

			// find the lowest altitude, this will be the start point for the
			// reference tour
			if (altitudeDiff < minAltiDiff && altitudeDiff != -1) {
				minAltiDiff = altitudeDiff;
				compareIndexStart = compareIndex;
			}
		}

		// show the reference tour in the temperature serie
		for (int refIndex = 0; refIndex < refAltitudes.length; refIndex++) {

			final int compareIndex = compareIndexStart + refIndex;

			// prevent out of bounds error
			if (compareIndex >= normCompTour.length) {
				break;
			}

			normCompTour[compareIndex] = normCompAltitudes[compareIndex];
		}

		// distance for the reference tour
		final int refDistance = refTourData.distanceSerie[refMeasureEndIndex]
				- refTourData.distanceSerie[refMeasureStartIndex];

		// get the start point in the compare tour
		final int distanceStart = normCompDistances[compareIndexStart];

		// find the start distance in the measure data
		int compareIndex = 0;
		for (compareIndex = 0; compareIndex < compareTourDataDistance.length; compareIndex++) {
			if (compareTourDataDistance[compareIndex] >= distanceStart) {
				break;
			}
		}
		final int compareDistanceStart = compareTourDataDistance[compareIndex];

		// overwrite the changed data series
		compareTourData.distanceSerie = compareTourNormalizer.getNormalizedDistance();
		compareTourData.altitudeSerie = compareTourNormalizer.getNormalizedAltitude();
		compareTourData.pulseSerie = normCompAltiDiff;
		compareTourData.temperatureSerie = normCompTour;

		// overwrite all data series, otherwise the chart will not be drawn
		compareTourData.timeSerie = compareTourNormalizer.getNormalizedTime();
		compareTourData.speedSerie = compareTourNormalizer.getNormalizedTime();
		compareTourData.cadenceSerie = compareTourNormalizer.getNormalizedTime();

		return compareIndexStart;
	}

	/**
	 * create the callbacks which compute the average
	 */
	private void createChartAvgCallbacks() {

		computeSpeedAvg = new ComputeChartValue() {

			/*
			 * Compute the average distance speed between the two sliders
			 */
			public float compute() {

				final int[] distanceValues = ((ChartDataSerie) (chartModel
						.getCustomData(TourManager.CUSTOM_DATA_DISTANCE))).getHighValues()[0];

				final int[] timeValues = ((ChartDataSerie) (chartModel
						.getCustomData(TourManager.CUSTOM_DATA_TIME))).getHighValues()[0];

				final int leftDistance = distanceValues[valuesIndexLeft];
				final int rightDistance = distanceValues[valuesIndexRight];
				final int leftTime = timeValues[valuesIndexLeft];
				final int rightTime = timeValues[valuesIndexRight];

				if (leftTime == rightTime) {
					// left and right slider are at the same position
					return 0;

				} else {

					int timeSlice = timeValues[1] - timeValues[0];
					final float time = rightTime
							- leftTime
							- (getIgnoreTimeSlices(
									distanceValues,
									valuesIndexLeft,
									valuesIndexRight,
									10 / timeSlice) * timeSlice);

					final float distance = rightDistance - leftDistance;

					float speed = distance / time * 3.6f;

					return speed;
				}

			}
		};

		computeAltimeterAvg = new ComputeChartValue() {

			/*
			 * Compute the average altimeter speed between the two sliders
			 */
			public float compute() {

				final int[] altitudeValues = ((ChartDataSerie) (chartModel
						.getCustomData(TourManager.CUSTOM_DATA_ALTITUDE))).getHighValues()[0];

				final int[] timeValues = ((ChartDataSerie) (chartModel
						.getCustomData(TourManager.CUSTOM_DATA_TIME))).getHighValues()[0];

				final int leftAltitude = altitudeValues[valuesIndexLeft];
				final int rightAltitude = altitudeValues[valuesIndexRight];
				final int leftTime = timeValues[valuesIndexLeft];
				final int rightTime = timeValues[valuesIndexRight];

				if (leftTime == rightTime) {
					// left and right slider are at the same position
					return 0;
				} else {

					final int[] distanceValues = ((ChartDataSerie) (chartModel
							.getCustomData(TourManager.CUSTOM_DATA_DISTANCE))).getHighValues()[0];

					int timeSlice = timeValues[1] - timeValues[0];
					final float time = rightTime
							- leftTime
							- (getIgnoreTimeSlices(
									distanceValues,
									valuesIndexLeft,
									valuesIndexRight,
									10 / timeSlice) * timeSlice);

					return (((float) (rightAltitude - leftAltitude) / time) * 3600);
				}
			}
		};

		computeGradientAvg = new ComputeChartValue() {

			/*
			 * Compute the average altimeter speed between the two sliders
			 */
			public float compute() {

				final int[] altitudeValues = ((ChartDataSerie) (chartModel
						.getCustomData(TourManager.CUSTOM_DATA_ALTITUDE))).getHighValues()[0];

				final int[] distanceValues = ((ChartDataSerie) (chartModel
						.getCustomData(TourManager.CUSTOM_DATA_DISTANCE))).getHighValues()[0];

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
	 * @param fTourChartConfig
	 * @return
	 */
	public ChartDataModel createChartDataModel(	final TourData tourData,
												final TourChartConfiguration chartConfig) {

		if (computeSpeedAvg == null) {
			createChartAvgCallbacks();
		}

		final ChartDataModel chartDataModel = new ChartDataModel(ChartDataModel.CHART_TYPE_LINE);

		if (tourData.timeSerie.length == 0) {
			return chartDataModel;
		}

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		createAltiGradiSerie(tourData);
		createSpeedSerie(tourData);

		/*
		 * distance
		 */
		final ChartDataXSerie xDataDistance = new ChartDataXSerie(tourData.distanceSerie);
		xDataDistance.setLabel(Messages.Tour_Label_distance);
		xDataDistance.setUnitLabel(Messages.Tour_Label_distance_unit);
		xDataDistance.setValueDivisor(1000);
		xDataDistance.setRgbLine(new RGB[] { new RGB(0, 0, 0) });

		/*
		 * time
		 */
		final ChartDataXSerie xDataTime = new ChartDataXSerie(tourData.timeSerie);
		xDataTime.setLabel(Messages.Tour_Label_time);
		xDataTime.setUnitLabel(Messages.Tour_Label_time_unit);
		xDataTime.setRgbLine(new RGB[] { new RGB(0, 0, 0) });
		xDataTime.setAxisUnit(ChartDataXSerie.AXIS_UNIT_HOUR_MINUTE_SECOND);

		/*
		 * show the distance axis only when a distance is available
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
			 * when time is displayed, the x-axis can show the start time
			 * starting from 0 or from the current time of the day
			 */
			xDataTime.setStartValue(chartConfig.isStartTime ? (tourData.getStartHour() * 3600)
					+ (tourData.getStartMinute() * 60) : 0);
		} else {
			// distance is displayed on the x axis
			chartDataModel.setXData(xDataDistance);
			chartDataModel.setXData2nd(xDataTime);
			chartDataModel.addXyData(xDataDistance);
			chartDataModel.addXyData(xDataTime);
		}

		/*
		 * altitude
		 */
		final ChartDataYSerie yDataAltitude = new ChartDataYSerie(
				ChartDataModel.CHART_TYPE_LINE,
				tourData.altitudeSerie);
		yDataAltitude.setYTitle(Messages.Graph_Label_Altitude);
		yDataAltitude.setUnitLabel(Messages.Graph_Label_Altitude_unit);
		yDataAltitude.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
		yDataAltitude.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_ALTITUDE);
		yDataAltitude.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true));
		yDataAltitude.setShowYSlider(true);
		setChartColors(prefStore, yDataAltitude, GraphColors.PREF_GRAPH_ALTITUDE);
		adjustMinMax(yDataAltitude);
		chartDataModel.addXyData(yDataAltitude);

		/*
		 * speed
		 */
		final ChartDataYSerie yDataSpeed = new ChartDataYSerie(
				ChartDataModel.CHART_TYPE_LINE,
				tourData.speedSerie);
		yDataSpeed.setYTitle(Messages.Graph_Label_Speed);
		yDataSpeed.setUnitLabel(Messages.Graph_Label_Speed_unit);
		yDataSpeed.setValueDivisor(10);
		yDataSpeed.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
		yDataSpeed.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_SPEED);
		yDataSpeed.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(
				true,
				true,
				computeSpeedAvg,
				2));
		yDataSpeed.setShowYSlider(true);
		setChartColors(prefStore, yDataSpeed, GraphColors.PREF_GRAPH_SPEED);
		chartDataModel.addXyData(yDataSpeed);

		/*
		 * heartbeat
		 */
		final ChartDataYSerie yDataPulse = new ChartDataYSerie(
				ChartDataModel.CHART_TYPE_LINE,
				tourData.pulseSerie);
		yDataPulse.setYTitle(Messages.Graph_Label_Heartbeat);
		yDataPulse.setUnitLabel(Messages.Graph_Label_Heartbeat_unit);
		yDataPulse.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
		yDataPulse.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_PULSE);
		yDataPulse.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true));
		yDataPulse.setShowYSlider(true);
		setChartColors(prefStore, yDataPulse, GraphColors.PREF_GRAPH_HEARTBEAT);
		chartDataModel.addXyData(yDataPulse);

		/*
		 * altimeter
		 */
		final ChartDataYSerie yDataAltimeter = new ChartDataYSerie(
				ChartDataModel.CHART_TYPE_LINE,
				tourData.altimeterSerie);
		yDataAltimeter.setYTitle(Messages.Graph_Label_Altimeter);
		yDataAltimeter.setUnitLabel(Messages.Graph_Label_Altimeter_unit);
		yDataAltimeter.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_ZERO);
		yDataAltimeter.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_ALTIMETER);
		yDataAltimeter.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(
				true,
				computeAltimeterAvg));
		yDataAltimeter.setShowYSlider(true);
		setChartColors(prefStore, yDataAltimeter, GraphColors.PREF_GRAPH_ALTIMETER);
		chartDataModel.addXyData(yDataAltimeter);

		// adjust min altitude when it's defined in the pref store
		if (prefStore.getBoolean(ITourbookPreferences.GRAPH_ALTIMETER_MIN_ENABLED)) {
			yDataAltimeter.setSavedMinValue(prefStore
					.getInt(ITourbookPreferences.GRAPH_ALTIMETER_MIN_VALUE));
		}

		/*
		 * gradient
		 */
		final ChartDataYSerie yDataGradient = new ChartDataYSerie(
				ChartDataModel.CHART_TYPE_LINE,
				tourData.gradientSerie);
		yDataGradient.setYTitle(Messages.Graph_Label_Gradiend);
		yDataGradient.setUnitLabel(Messages.Graph_Label_Gradiend_unit);
		yDataGradient.setValueDivisor(10);
		yDataGradient.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_ZERO);
		yDataGradient.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_GRADIENT);
		yDataGradient.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(
				true,
				true,
				computeGradientAvg,
				1));
		yDataGradient.setShowYSlider(true);
		setChartColors(prefStore, yDataGradient, GraphColors.PREF_GRAPH_GRADIEND);
		chartDataModel.addXyData(yDataGradient);

		// adjust min value when defined in the pref store
		if (prefStore.getBoolean(ITourbookPreferences.GRAPH_GRADIENT_MIN_ENABLED)) {
			yDataGradient.setSavedMinValue(prefStore
					.getInt(ITourbookPreferences.GRAPH_GRADIENT_MIN_VALUE));
		}

		/*
		 * cadence
		 */
		final ChartDataYSerie yDataCadence = new ChartDataYSerie(
				ChartDataModel.CHART_TYPE_LINE,
				tourData.cadenceSerie);
		yDataCadence.setYTitle(Messages.Graph_Label_Cadence);
		yDataCadence.setUnitLabel(Messages.Graph_Label_Cadence_unit);
		yDataCadence.setShowYSlider(true);
		yDataCadence.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
		yDataCadence.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_CADENCE);
		yDataCadence.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true));
		setChartColors(prefStore, yDataCadence, GraphColors.PREF_GRAPH_CADENCE);
		chartDataModel.addXyData(yDataCadence);

		/*
		 * temperature
		 */
		final ChartDataYSerie yDataTemperature = new ChartDataYSerie(
				ChartDataModel.CHART_TYPE_LINE,
				tourData.temperatureSerie);
		yDataTemperature.setYTitle(Messages.Graph_Label_Temperature);
		yDataTemperature.setUnitLabel(Messages.Graph_Label_Temperature_unit);
		yDataTemperature.setShowYSlider(true);
		yDataTemperature.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
		yDataTemperature.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_TEMPERATURE);
		yDataTemperature.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true, true));
		setChartColors(prefStore, yDataTemperature, GraphColors.PREF_GRAPH_TEMPTERATURE);
		adjustMinMax(yDataTemperature);
		chartDataModel.addXyData(yDataTemperature);

		/*
		 * all visible graphs are added as y-data to the chart data fDataModel
		 * in the sequence as they were activated
		 */
		for (final int actionId : chartConfig.getVisibleGraphs()) {

			switch (actionId) {
			case GRAPH_ALTITUDE:
				chartDataModel.addYData(yDataAltitude);
				break;

			case GRAPH_SPEED:
				chartDataModel.addYData(yDataSpeed);
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
	 * adjust the min/max values to make them more visible and not at the same
	 * position as the x-axis or the top of the chart
	 */
	private void adjustMinMax(final ChartDataYSerie yData) {

		yData.setMaxValue(yData.getMaxValue() + 1);

		// if (yData.getMinValue() > 0) {
		yData.setMinValue(yData.getMinValue() - 1);
		// }
	}

	private void createAltiGradiSerieOLD(TourData tourData) {

		if (tourData.altimeterSerie != null) {
			return;
		}

		final int[] timeSerie = tourData.timeSerie;
		final int[] distanceSerie = tourData.distanceSerie;
		final int[] altitudeSerie = tourData.altitudeSerie;

		final int altimeterSerie[] = tourData.altimeterSerie = new int[timeSerie.length];
		final int gradientSerie[] = tourData.gradientSerie = new int[timeSerie.length];

		final int altimeterAvg[] = { 0, 0, 0 };
		final int sliceAvg[] = { 0, 0, 0 };

		final int distanceSum[] = { 0, 0, 0 };
		final int altitudeSum[] = { 0, 0, 0 };

		int index = 0;
		int altitudeLast = altitudeSerie[index];
		int distanceLast = distanceSerie[index];
		int timeAbs = altitudeSerie[index];

		// convert data from the tour format into an interger[]
		for (final int timeSlice : timeSerie) {

			// first value is the absolute altitude
			final int altitudeDiff = altitudeSerie[index] - altitudeLast;
			final int distanceDiff = distanceSerie[index] - distanceLast;
			final int timeDiff = timeSlice - timeAbs;

			// compute altimeter as an average of multiple values
			for (int avgIndex = altimeterAvg.length - 1; avgIndex >= 0; avgIndex--) {
				if (avgIndex > 0) {
					altimeterAvg[avgIndex] = altimeterAvg[avgIndex - 1] + altitudeDiff;
					sliceAvg[avgIndex] = sliceAvg[avgIndex - 1] + timeDiff;
				} else {
					// set current value
					altimeterAvg[avgIndex] = altitudeDiff;
					sliceAvg[avgIndex] = timeDiff;
				}
			}

			// compute gradient as an average of multiple values
			for (int sumIndex = distanceSum.length - 1; sumIndex >= 0; sumIndex--) {
				if (sumIndex > 0) {
					distanceSum[sumIndex] = distanceSum[sumIndex - 1] + distanceDiff;
					altitudeSum[sumIndex] = altitudeSum[sumIndex - 1] + altitudeDiff;
				} else {
					// set current value
					distanceSum[sumIndex] = distanceDiff;
					altitudeSum[sumIndex] = altitudeDiff;
				}
			}

			// keep altimeter data
			final float slicePerHour = 3600f / (sliceAvg[sliceAvg.length - 1] / sliceAvg.length);
			altimeterSerie[index] = (int) (((float) altimeterAvg[altimeterAvg.length - 1]
					/ altimeterAvg.length * slicePerHour));

			// keep gradient data
			final int distanceSumTotal = distanceSum[distanceSum.length - 1];
			final int altitudeSumTotal = altitudeSum[altitudeSum.length - 1];
			gradientSerie[index] = distanceSumTotal == 0
					? 0
					: (altitudeSumTotal * 1000 / distanceSumTotal);

			// prepare next iteration
			altitudeLast = altitudeSerie[index];
			distanceLast = distanceSerie[index];
			timeAbs = timeSlice;

			index++;
		}
	}

	/**
	 * the speed must be interpolated for low time intervals because the
	 * smallest distance is 10 m
	 * 
	 * @param tourData
	 */
	private void createAltiGradiSerie(TourData tourData) {

		// if (tourData.altimeterSerie != null) {
		// return;
		// }

		final int serieLength = tourData.timeSerie.length;

		final int altimeterSerie[] = tourData.altimeterSerie = new int[serieLength];
		final int gradientSerie[] = tourData.gradientSerie = new int[serieLength];

		int deviceTimeInterval = tourData.getDeviceTimeInterval();

		int indexLowAdjustment;
		int indexHighAdjustment;

		if (deviceTimeInterval <= 2) {
			indexLowAdjustment = 15;
			indexHighAdjustment = 15;

		} else if (deviceTimeInterval <= 5) {
			indexLowAdjustment = 5;
			indexHighAdjustment = 6;

		} else if (deviceTimeInterval <= 10) {
			indexLowAdjustment = 2;
			indexHighAdjustment = 3;
		} else {
			indexLowAdjustment = 1;
			indexHighAdjustment = 2;
		}

		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			// adjust index to the array size
			int indexLow = Math.min(Math.max(0, serieIndex - indexLowAdjustment), serieLength - 1);
			int indexHigh = Math
					.max(0, Math.min(serieIndex + indexHighAdjustment, serieLength - 1));

			final int distance = tourData.distanceSerie[indexHigh]
					- tourData.distanceSerie[indexLow];

			final int altitude = tourData.altitudeSerie[indexHigh]
					- tourData.altitudeSerie[indexLow];

			final float timeInterval = deviceTimeInterval * (indexHigh - indexLow);

			// keep altimeter data
			altimeterSerie[serieIndex] = (int) (3600F * altitude / timeInterval);

			// keep gradient data
			gradientSerie[serieIndex] = distance == 0 ? 0 : altitude * 1000 / distance;

			// System.out.println(""
			// + (timeInterval + "\t")
			// + (distance + "\t")
			// + (altitude + "\t")
			// + (tourData.distanceSerie[serieIndex] + "\t")
			// + (tourData.altitudeSerie[serieIndex] + "\t")
			// + (tourData.gradientSerie[serieIndex] + "\t")
			// + "");
		}
	}

	/**
	 * the speed must be interpolated for low time intervals because the
	 * smallest distance is 10 m
	 * 
	 * @param tourData
	 */
	private void createSpeedSerie(TourData tourData) {

		if (tourData.speedSerie != null) {
			return;
		}

		final int serieLength = tourData.timeSerie.length;

		final int speedSerie[] = tourData.speedSerie = new int[serieLength];

		int deviceTimeInterval = tourData.getDeviceTimeInterval();

		int lowIndexAdjustmentDefault = 0;
		int highIndexAdjustmentDefault = 0;
		if (deviceTimeInterval <= 2) {
			lowIndexAdjustmentDefault = 2;
			highIndexAdjustmentDefault = 3;

		} else if (deviceTimeInterval <= 5) {
			lowIndexAdjustmentDefault = 1;
			highIndexAdjustmentDefault = 1;

		} else if (deviceTimeInterval <= 10) {
			lowIndexAdjustmentDefault = 0;
			highIndexAdjustmentDefault = 1;
		} else {
			lowIndexAdjustmentDefault = 0;
			highIndexAdjustmentDefault = 1;
		}

		for (int speedIndex = 0; speedIndex < serieLength; speedIndex++) {

			// adjust index to the array size
			int distIndexLow = Math.min(
					Math.max(0, speedIndex - lowIndexAdjustmentDefault),
					serieLength - 1);

			int distIndexHigh = Math.max(0, Math.min(
					speedIndex + highIndexAdjustmentDefault,
					serieLength - 1));

			int distanceDefault = tourData.distanceSerie[distIndexHigh]
					- tourData.distanceSerie[distIndexLow];

			// adjust the accuracy for the distance
			int lowIndexAdjustment = lowIndexAdjustmentDefault;
			int highIndexAdjustment = highIndexAdjustmentDefault;
			if (distanceDefault < 30) {
				lowIndexAdjustment = lowIndexAdjustmentDefault + 3;
				highIndexAdjustment = highIndexAdjustmentDefault + 3;
			} else if (distanceDefault < 50) {
				lowIndexAdjustment = lowIndexAdjustmentDefault + 2;
				highIndexAdjustment = highIndexAdjustmentDefault + 2;
			} else if (distanceDefault < 100) {
				lowIndexAdjustment = lowIndexAdjustmentDefault + 1;
				highIndexAdjustment = highIndexAdjustmentDefault + 1;
			}

			// adjust index to the array size
			distIndexLow = Math.min(Math.max(0, speedIndex - lowIndexAdjustment), serieLength - 1);

			distIndexHigh = Math
					.max(0, Math.min(speedIndex + highIndexAdjustment, serieLength - 1));

			final int distance = tourData.distanceSerie[distIndexHigh]
					- tourData.distanceSerie[distIndexLow];
			final float timeInterval = deviceTimeInterval * (distIndexHigh - distIndexLow);

			final int speed = (int) (((float) distance * 36F) / timeInterval);

			// System.out.println(""
			// + (timeInterval + "\t")
			// + (distanceDefault + "\t")
			// + (distance + "\t")
			// + speed);

			speedSerie[speedIndex] = speed;
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
	 * @param useNormalizedData
	 */
	public void createTour(TourData tourData, final boolean useNormalizedData) {

		if (tourData.getTourPerson() != null) {
			// load tour from database
			tourData = TourDatabase.getTourDataByTourId(tourData.getTourId());
		}

		if (useNormalizedData) {
			compareTour(tourData);
		}

		// openTourEditor(createTourEditorInput(tourData));
	}

	// /**
	// * Creates a new tour context for a given tour data object
	// *
	// * @param tourData
	// * @return Returns the newly created tour context
	// */
	// private TourEditorInput createTourEditorInput(final TourData tourData) {
	//
	// final TourChartConfiguration chartConfiguration =
	// createTourChartConfiguration();
	//
	// // chartConfiguration.setKeepMinMaxValues(true);
	//
	// // create the tour editor input
	// final TourEditorInput editorInput = new TourEditorInput(tourData,
	// chartConfiguration);
	//
	// // keep the tour in a cache
	// tourCache.put(tourData.getTourId(), editorInput);
	//
	// return editorInput;
	// }
	//
	// /**
	// * Opens the tour editor for the the given editor input
	// *
	// * @param editorInput
	// */
	// private void openTourEditor(final TourEditorInput editorInput) {
	//
	// try {
	// PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(
	// editorInput,
	// TourEditorPart.ID,
	// true);
	//
	// } catch (final PartInitException e) {
	// e.printStackTrace();
	// }
	// }
	//
	// /**
	// * Opens the tour for the given tour id
	// *
	// * @param tourId
	// */
	// public void openTourInEditor(final long tourId) {
	//
	// TourEditorInput editorInput;
	//
	// if (tourCache.containsKey(tourId)) {
	// // tour is already in the cache
	// editorInput = tourCache.get(tourId);
	// } else {
	// // load tour from database
	// final TourData tourData = TourDatabase.getTourDataByTourId(tourId);
	//
	// editorInput = createTourEditorInput(tourData);
	// }
	// // openTourEditor(editorInput);
	// }

	/**
	 * set the graph colors from the pref store
	 * 
	 * @param prefStore
	 * @param yDataSerie
	 * @param graphName
	 */
	public static void setChartColors(	final IPreferenceStore prefStore,
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
	 * calculate the driving time, ignore the time when the distance is 0 within
	 * a time period which is defined by <code>sliceMin</code>
	 * 
	 * @param distanceValues
	 * @param indexLeft
	 * @param indexRight
	 * @param sliceMin
	 * @return Returns the number of slices which can be ignored
	 */
	public int getIgnoreTimeSlices(	final int[] distanceValues,
									int indexLeft,
									int indexRight,
									int sliceMin) {
		int ignoreTimeCounter = 0;
		int oldDistance = 0;
		sliceMin = Math.max(sliceMin, 1);

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

}
