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
import net.tourbook.ui.action.ActionSetTourType;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.action.ActionCreateMarker;
import net.tourbook.ui.tourChart.action.ActionCreateRefTour;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;

/**
 * Chart context provider for the tour viewer (which is currently the TourEditor)
 */
public class TourChartContextProvider implements IChartContextProvider, ITourProvider {

	private TourEditor					fTourEditor;

	private ActionEditQuick				fActionQuickEdit;
	private ActionEditTour				fActionEditTour;
	private ActionOpenMarkerDialog		fActionOpenMarkerDialog;
	private ActionOpenAdjustAltitudeDialog	fActionAdjustAltitude;

	private ActionCreateRefTour			fActionCreateRefTour;
	private ActionCreateMarker			fActionCreateMarker;
	private ActionCreateMarker			fActionCreateMarkerLeft;
	private ActionCreateMarker			fActionCreateMarkerRight;

	private ActionSetTourType			fActionSetTourType;
	private ActionSetTourTag			fActionAddTag;
	private ActionSetTourTag			fActionRemoveTag;
	private ActionRemoveAllTags			fActionRemoveAllTags;
	private ActionOpenPrefDialog		fActionOpenTagPrefs;

	private ChartXSlider				fLeftSlider;
	private ChartXSlider				fRightSlider;

	/**
	 * @param tourEditor
	 * @param tourChartView
	 */
	TourChartContextProvider(final TourEditor tourEditor) {

		fTourEditor = tourEditor;
		final TourChart tourChart = fTourEditor.getTourChart();

		fActionQuickEdit = new ActionEditQuick(this);
		fActionEditTour = new ActionEditTour(this);

		fActionOpenMarkerDialog = new ActionOpenMarkerDialog(this, true);
		fActionOpenMarkerDialog.setEnabled(true);

		fActionAdjustAltitude = new ActionOpenAdjustAltitudeDialog(this, false);
		fActionAdjustAltitude.setEnabled(true);
		

		fActionCreateRefTour = new ActionCreateRefTour(tourChart);

		fActionCreateMarker = new ActionCreateMarker(this, //
				Messages.tourCatalog_view_action_create_marker,
				true);

		fActionCreateMarkerLeft = new ActionCreateMarker(this,
				Messages.tourCatalog_view_action_create_left_marker,
				true);

		fActionCreateMarkerRight = new ActionCreateMarker(this,
				Messages.tourCatalog_view_action_create_right_marker,
				false);

		fActionSetTourType = new ActionSetTourType(this);
		fActionAddTag = new ActionSetTourTag(this, true);
		fActionRemoveTag = new ActionSetTourTag(this, false);
		fActionRemoveAllTags = new ActionRemoveAllTags(this);
		fActionOpenTagPrefs = new ActionOpenPrefDialog(Messages.action_tag_open_tagging_structure,
				ITourbookPreferences.PREF_PAGE_TAGS);
	}

	public void fillBarChartContextMenu(final IMenuManager menuMgr,
										final int hoveredBarSerieIndex,
										final int hoveredBarValueIndex) {}

	public void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(new Separator());
		menuMgr.add(fActionQuickEdit);
		menuMgr.add(fActionEditTour);
		menuMgr.add(fActionOpenMarkerDialog);
		menuMgr.add(fActionAdjustAltitude);

		menuMgr.add(new Separator());
		menuMgr.add(fActionSetTourType);
		menuMgr.add(fActionAddTag);
		menuMgr.add(fActionRemoveTag);
		menuMgr.add(fActionRemoveAllTags);
		TagManager.fillRecentTagsIntoMenu(menuMgr, this, true, true);
		menuMgr.add(fActionOpenTagPrefs);

		/*
		 * enable actions
		 */
		final boolean isDataAvailable = fTourEditor.getTourData() != null
				&& fTourEditor.getTourData().getTourPerson() != null;

		fActionQuickEdit.setEnabled(isDataAvailable);
		fActionEditTour.setEnabled(isDataAvailable);
	}

	public void fillXSliderContextMenu(	final IMenuManager menuMgr,
										final ChartXSlider leftSlider,
										final ChartXSlider rightSlider) {

		fLeftSlider = leftSlider;
		fRightSlider = rightSlider;

		if (leftSlider != null || rightSlider != null) {

			// marker actions
			if (leftSlider != null && rightSlider == null) {
				menuMgr.add(fActionCreateMarker);
			} else {
				menuMgr.add(fActionCreateMarkerLeft);
				menuMgr.add(fActionCreateMarkerRight);
			}

			menuMgr.add(fActionCreateRefTour);
			menuMgr.add(new Separator());

			// action: create reference tour
			final TourData tourData = fTourEditor.getTourChart().getTourData();
			final boolean canCreateRefTours = tourData.altitudeSerie != null && tourData.distanceSerie != null;

			fActionCreateRefTour.setEnabled(canCreateRefTours);

		}

	}

	public Chart getChart() {
		return fTourEditor.getTourChart();
	}

	public ChartXSlider getLeftSlider() {
		return fLeftSlider;
	}

	public ChartXSlider getRightSlider() {
		return fRightSlider;
	}

	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> tourList = new ArrayList<TourData>();
		tourList.add(fTourEditor.getTourData());

		return tourList;
	}

}
