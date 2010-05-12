/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartXSlider;
import net.tourbook.chart.IChartContextProvider;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.ActionRemoveAllTags;
import net.tourbook.tag.ActionSetTourTag;
import net.tourbook.tag.TagManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionOpenPrefDialog;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.action.ActionCreateMarker;
import net.tourbook.ui.tourChart.action.ActionCreateRefTour;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;

/**
 * Chart context provider for the tour viewer (which is currently the TourEditor)
 */
public class TourChartContextProvider implements IChartContextProvider, ITourProvider {

	private TourEditor						_tourEditor;

	private ActionEditQuick					_actionQuickEdit;
	private ActionEditTour					_actionEditTour;
	private ActionOpenMarkerDialog			_actionOpenMarkerDialog;
	private ActionOpenAdjustAltitudeDialog	_actionAdjustAltitude;

	private ActionCreateRefTour				_actionCreateRefTour;
	private ActionCreateMarker				_actionCreateMarker;
	private ActionCreateMarker				_actionCreateMarkerLeft;
	private ActionCreateMarker				_actionCreateMarkerRight;

	private ActionSetTourTypeMenu			_actionSetTourType;
	private ActionSetTourTag				_actionAddTag;
	private ActionSetTourTag				_actionRemoveTag;
	private ActionRemoveAllTags				_actionRemoveAllTags;
	private ActionOpenPrefDialog			_actionOpenTagPrefs;

	private ChartXSlider					_leftSlider;
	private ChartXSlider					_rightSlider;

	/**
	 * @param tourEditor
	 * @param tourChartView
	 */
	TourChartContextProvider(final TourEditor tourEditor) {

		_tourEditor = tourEditor;
		final TourChart tourChart = _tourEditor.getTourChart();

		_actionQuickEdit = new ActionEditQuick(this);
		_actionEditTour = new ActionEditTour(this);

		_actionOpenMarkerDialog = new ActionOpenMarkerDialog(this, true);
		_actionOpenMarkerDialog.setEnabled(true);

		_actionAdjustAltitude = new ActionOpenAdjustAltitudeDialog(this);
		_actionAdjustAltitude.setEnabled(true);

		_actionCreateRefTour = new ActionCreateRefTour(tourChart);

		_actionCreateMarker = new ActionCreateMarker(this, //
				Messages.tourCatalog_view_action_create_marker,
				true);

		_actionCreateMarkerLeft = new ActionCreateMarker(
				this,
				Messages.tourCatalog_view_action_create_left_marker,
				true);

		_actionCreateMarkerRight = new ActionCreateMarker(
				this,
				Messages.tourCatalog_view_action_create_right_marker,
				false);

		_actionSetTourType = new ActionSetTourTypeMenu(this);
		_actionAddTag = new ActionSetTourTag(this, true);
		_actionRemoveTag = new ActionSetTourTag(this, false);
		_actionRemoveAllTags = new ActionRemoveAllTags(this);
		_actionOpenTagPrefs = new ActionOpenPrefDialog(
				Messages.action_tag_open_tagging_structure,
				ITourbookPreferences.PREF_PAGE_TAGS);
	}

	public void fillBarChartContextMenu(final IMenuManager menuMgr,
										final int hoveredBarSerieIndex,
										final int hoveredBarValueIndex) {}

	public void fillContextMenu(final IMenuManager menuMgr,
								final int mouseDownDevPositionX,
								final int mouseDownDevPositionY) {

		menuMgr.add(new Separator());
		menuMgr.add(_actionQuickEdit);
		menuMgr.add(_actionEditTour);
		menuMgr.add(_actionOpenMarkerDialog);
		menuMgr.add(_actionAdjustAltitude);

		menuMgr.add(new Separator());
		menuMgr.add(_actionSetTourType);
		menuMgr.add(_actionAddTag);
		menuMgr.add(_actionRemoveTag);
		menuMgr.add(_actionRemoveAllTags);
		TagManager.fillRecentTagsIntoMenu(menuMgr, this, true, true);
		menuMgr.add(_actionOpenTagPrefs);

		/*
		 * enable actions
		 */
		final boolean isDataAvailable = _tourEditor.getTourData() != null
				&& _tourEditor.getTourData().getTourPerson() != null;

		_actionQuickEdit.setEnabled(isDataAvailable);
		_actionEditTour.setEnabled(isDataAvailable);
	}

	public void fillXSliderContextMenu(	final IMenuManager menuMgr,
										final ChartXSlider leftSlider,
										final ChartXSlider rightSlider) {

		_leftSlider = leftSlider;
		_rightSlider = rightSlider;

		if (leftSlider != null || rightSlider != null) {

			// marker actions
			if (leftSlider != null && rightSlider == null) {
				menuMgr.add(_actionCreateMarker);
			} else {
				menuMgr.add(_actionCreateMarkerLeft);
				menuMgr.add(_actionCreateMarkerRight);
			}

			menuMgr.add(_actionCreateRefTour);
			menuMgr.add(new Separator());

			// action: create reference tour
			final TourData tourData = _tourEditor.getTourChart().getTourData();
			final boolean canCreateRefTours = tourData.altitudeSerie != null && tourData.distanceSerie != null;

			_actionCreateRefTour.setEnabled(canCreateRefTours);

		}

	}

	public Chart getChart() {
		return _tourEditor.getTourChart();
	}

	public ChartXSlider getLeftSlider() {
		return _leftSlider;
	}

	public ChartXSlider getRightSlider() {
		return _rightSlider;
	}

	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> tourList = new ArrayList<TourData>();
		tourList.add(_tourEditor.getTourData());

		return tourList;
	}

	public boolean showOnlySliderContextMenu() {
		return false;
	}

}
