/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
/**
 * @author Wolfgang Schramm
 * 
 * created: 06.07.2005
 */
package net.tourbook.ui.tourChart;

import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.ChartYDataMinMaxKeeper;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;

/**
 * Contains the configuration how the tour chart is displayed.
 */
public class TourChartConfiguration {

	private final IPreferenceStore	_prefStore				= TourbookPlugin.getPrefStore();

	/**
	 * true: show time on the x-axis
	 * <p>
	 * false: show distance on the x-axis
	 */
	public boolean					isShowTimeOnXAxis		= false;

	public boolean					isShowTimeOnXAxisBackup	= false;

	/**
	 * is <code>true</code> when the distance is not available and the time must be displayed on the
	 * x-axis
	 */
	public boolean					isForceTimeOnXAxis;

	/**
	 * true: show the start time of the tour
	 * <p>
	 * false: show the tour time which starts at 0
	 */
	public X_AXIS_START_TIME		xAxisTime				= X_AXIS_START_TIME.START_WITH_0;

	/**
	 * contains a list for all graphs which are displayed, the sequence of the list is the sequence
	 * in which the graphs will be displayed
	 */
	private ArrayList<Integer>		_visibleGraphSequence	= new ArrayList<Integer>();

	/**
	 * contains the min/max keeper or <code>null</code> when min/max is not kept
	 */
	private ChartYDataMinMaxKeeper	_minMaxKeeper;

	/**
	 * when <code>true</code> the sliders are moved when the chart is zoomed
	 */
	public boolean					moveSlidersWhenZoomed	= false;

	/**
	 * the graph is automatically zoomed to the slider position when the slider is moved
	 */
	public boolean					autoZoomToSlider		= false;

	/**
	 * when <code>true</code> the action button is displayed to show/hide the tour compare result
	 * graph
	 */
	public boolean					canShowTourCompareGraph	= false;

	/**
	 * is <code>true</code> when the altitude diff scaling in the merge layer is relative
	 */
	public boolean					isRelativeValueDiffScaling;

	/**
	 * when <code>true</code> the SRTM data are visible in the altitude graph
	 */
	public boolean					isSRTMDataVisible		= false;

	/**
	 * when <code>true</code> the SRTM data are visible in the altitude graph
	 */
	public boolean					canShowSRTMData;

	/**
	 * Is <code>true</code> when HR zones are currently displayed in the chart
	 */
	public boolean					isHrZoneDisplayed		= false;

	/**
	 * Is <code>true</code> when HR zones can be displayed, which requires that pulse values are
	 * available and the person has defined HR zones.
	 */
	public boolean					canShowHrZones			= false;

	/**
	 * Is <code>true</code> when tour markers are displayed.
	 */
	public Boolean					isShowTourMarker		= true;

	/**
	 * When <code>true</code>, hidden markers are also visible.
	 */
	public boolean					isShowHiddenMarker;

	/**
	 * When <code>true</code>, all marker label with be drawn with default colors, otherwise they
	 * are drawn with device or hidden color.
	 */
	public boolean					isDrawMarkerWithDefaultColor;

	/**
	 * When <code>true</code> tour marker labels are displayed.
	 */
	public boolean					isShowMarkerLabel;

	public boolean					isShowMarkerTooltip;

	public int						markerHoverSize;
	public int						markerLabelOffset;

	public boolean					isShowLabelTempPos;
	public int						markerLabelTempPos;

	/**
	 * Size of the marker point in DLU (Dialog Units).
	 */
	public int						markerPointSize;

	/**
	 * Color for the tour marker point and label.
	 */
	public RGB						markerColorDefault;

	/**
	 * Color for the tour marker point which is created by the device and not with the marker
	 * editor.
	 */
	public RGB						markerColorDevice;

	/**
	 * Color for tour markers which are hidden, visibility is false.
	 */
	public RGB						markerColorHidden;

	/**
	 * Is <code>true</code> when graph values are displayed when they are recorded when a break time
	 * is detected.
	 */
	public boolean					isShowBreaktimeValues	= true;

	/**
	 * Graph type which is used to draw the HR zone background.
	 */
	public String					hrZoneStyle				= TourChart.ACTION_ID_HR_ZONE_STYLE_GRAPH_TOP;

	public boolean					isShowTourPhotos		= true;											;
	public boolean					isShowTourPhotoTooltip	= true;

	/**
	 * @param keepMinMaxValues
	 *            set <code>true</code> to keep min/max values when tour data will change
	 */
	public TourChartConfiguration(final boolean keepMinMaxValues) {

		if (keepMinMaxValues) {
			setMinMaxKeeper(true);
		}

		/*
		 * Initialize tour marker settings from the pref store
		 */
		markerHoverSize = _prefStore.getInt(ITourbookPreferences.GRAPH_MARKER_HOVER_SIZE);
		markerLabelOffset = _prefStore.getInt(ITourbookPreferences.GRAPH_MARKER_LABEL_OFFSET);
		markerPointSize = _prefStore.getInt(ITourbookPreferences.GRAPH_MARKER_POINT_SIZE);
		markerLabelTempPos = _prefStore.getInt(ITourbookPreferences.GRAPH_MARKER_LABEL_TEMP_POSITION);

		isDrawMarkerWithDefaultColor = _prefStore.getBoolean(//
				ITourbookPreferences.GRAPH_MARKER_IS_DRAW_WITH_DEFAULT_COLOR);
		isShowHiddenMarker = _prefStore.getBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_HIDDEN_MARKER);
		isShowMarkerLabel = _prefStore.getBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_LABEL);
		isShowMarkerTooltip = _prefStore.getBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_TOOLTIP);
		isShowLabelTempPos = _prefStore.getBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_LABEL_TEMP_POSITION);

		markerColorDefault = PreferenceConverter.getColor(_prefStore,//
				ITourbookPreferences.GRAPH_MARKER_COLOR_DEFAULT);
		markerColorDevice = PreferenceConverter.getColor(_prefStore,//
				ITourbookPreferences.GRAPH_MARKER_COLOR_DEVICE);
		markerColorHidden = PreferenceConverter.getColor(_prefStore,//
				ITourbookPreferences.GRAPH_MARKER_COLOR_HIDDEN);

	}

	public void addVisibleGraph(final int visibleGraph) {
		_visibleGraphSequence.add(visibleGraph);
	}

	/**
	 * @return Returns the min/max keeper of the chart configuration or <code>null</code> when a
	 *         min/max keeper is not set
	 */
	ChartYDataMinMaxKeeper getMinMaxKeeper() {
		return _minMaxKeeper;
	}

	/**
	 * @return Returns all graph id's which are displayed in the chart, the list is in the sequence
	 *         in which the graphs are displayed
	 */
	public ArrayList<Integer> getVisibleGraphs() {
		return _visibleGraphSequence;
	}

	public void removeVisibleGraph(final int selectedGraphId) {

		int graphIndex = 0;

		for (final Integer graphId : _visibleGraphSequence) {
			if (graphId == selectedGraphId) {
				_visibleGraphSequence.remove(graphIndex);
				break;
			}
			graphIndex++;
		}
	}

	public void setIsShowTimeOnXAxis(final boolean isShowTimeOnXAxis) {
		this.isShowTimeOnXAxis = isShowTimeOnXAxisBackup = isShowTimeOnXAxis;
	}

	/**
	 * <code>true</code> indicates to keep the min/max values in the chart configuration when the
	 * data model was changed, this has the higher priority than keeping the min/max values in the
	 * chart widget
	 * 
	 * @param keepMinMaxValues
	 *            the keepMinMaxValues to set
	 */
	public void setMinMaxKeeper(final boolean keepMinMaxValues) {
		if (keepMinMaxValues) {
			_minMaxKeeper = new ChartYDataMinMaxKeeper();
		} else {
			_minMaxKeeper = null;
		}
	}

}
