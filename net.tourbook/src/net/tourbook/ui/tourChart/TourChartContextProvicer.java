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

package net.tourbook.ui.tourChart;

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
import net.tourbook.tour.ActionOpenAdjustAltitudeDialog;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.ui.ITourChartViewer;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionOpenPrefDialog;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.action.ActionSetTourType;
import net.tourbook.ui.tourChart.action.ActionCreateMarker;
import net.tourbook.ui.tourChart.action.ActionCreateRefTour;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;

public class TourChartContextProvicer implements IChartContextProvider, ITourProvider {

	private final ITourChartViewer			fTourChartViewer;

	private ActionEditQuick					fActionQuickEdit;
	private ActionEditTour					fActionEditTour;
	private ActionOpenMarkerDialog			fActionOpenMarkerDialog;
	private ActionOpenAdjustAltitudeDialog	fActionOpenAdjustAltitudeDialog;
	private ActionOpenTour					fActionOpenTour;

	private ActionCreateRefTour				fActionCreateRefTour;
	private ActionCreateMarker				fActionCreateMarker;
	private ActionCreateMarker				fActionCreateMarkerLeft;
	private ActionCreateMarker				fActionCreateMarkerRight;

	private ActionSetTourType				fActionSetTourType;
	private ActionSetTourTag				fActionAddTag;
	private ActionSetTourTag				fActionRemoveTag;
	private ActionRemoveAllTags				fActionRemoveAllTags;
	private ActionOpenPrefDialog			fActionOpenTagPrefDialog;

	private ChartXSlider					fLeftSlider;
	private ChartXSlider					fRightSlider;

	/**
	 * Provides a context menu for a tour chart
	 * 
	 * @param tourChartViewer
	 */
	public TourChartContextProvicer(final ITourChartViewer tourChartViewer) {

		fTourChartViewer = tourChartViewer;

		fActionQuickEdit = new ActionEditQuick(fTourChartViewer);
		fActionEditTour = new ActionEditTour(fTourChartViewer);
		fActionOpenTour = new ActionOpenTour(fTourChartViewer);

		final TourChart tourChart = fTourChartViewer.getTourChart();

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

		fActionOpenMarkerDialog = new ActionOpenMarkerDialog(this, true);
		fActionOpenMarkerDialog.setEnabled(true);

		fActionOpenAdjustAltitudeDialog = new ActionOpenAdjustAltitudeDialog(this, true);
		fActionOpenAdjustAltitudeDialog.setEnabled(true);

		fActionSetTourType = new ActionSetTourType(this);
		fActionAddTag = new ActionSetTourTag(this, true);
		fActionRemoveTag = new ActionSetTourTag(this, false);
		fActionRemoveAllTags = new ActionRemoveAllTags(this);
		fActionOpenTagPrefDialog = new ActionOpenPrefDialog(Messages.action_tag_open_tagging_structure,
				ITourbookPreferences.PREF_PAGE_TAGS);
	}

	public void fillBarChartContextMenu(final IMenuManager menuMgr,
										final int hoveredBarSerieIndex,
										final int hoveredBarValueIndex) {}

	public void fillContextMenu(final IMenuManager menuMgr) {

		final TourData tourData = fTourChartViewer.getTourChart().getTourData();
		final boolean isTourSaved = tourData != null && tourData.getTourPerson() != null;

		menuMgr.add(new Separator());
		menuMgr.add(fActionQuickEdit);
		menuMgr.add(fActionEditTour);
		menuMgr.add(fActionOpenMarkerDialog);
		menuMgr.add(fActionOpenAdjustAltitudeDialog);
		menuMgr.add(fActionOpenTour);

		menuMgr.add(new Separator());
		menuMgr.add(fActionSetTourType);
		menuMgr.add(fActionAddTag);
		menuMgr.add(fActionRemoveTag);
		menuMgr.add(fActionRemoveAllTags);
		if (isTourSaved) {
			TagManager.fillRecentTagsIntoMenu(menuMgr, this, true, true);
		}
		menuMgr.add(fActionOpenTagPrefDialog);

		/*
		 * enable actions
		 */
		final boolean isTagSet = tourData.getTourTags().size() > 0;

		fActionQuickEdit.setEnabled(isTourSaved);
		fActionEditTour.setEnabled(isTourSaved);
		fActionOpenMarkerDialog.setEnabled(isTourSaved);
		fActionOpenAdjustAltitudeDialog.setEnabled(isTourSaved);
		fActionOpenTour.setEnabled(isTourSaved);

		fActionSetTourType.setEnabled(isTourSaved);
		fActionAddTag.setEnabled(isTourSaved);
		fActionRemoveTag.setEnabled(isTourSaved);
		fActionRemoveAllTags.setEnabled(isTourSaved && isTagSet);

		// enable actions for the recent tags
		TagManager.enableRecentTagActions(isTourSaved);

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

			/*
			 * enable actions
			 */
			final TourData tourData = fTourChartViewer.getTourChart().getTourData();
			final boolean isTourSaved = tourData != null && tourData.getTourPerson() != null;

			final boolean canCreateRefTours = tourData.altitudeSerie != null
					&& tourData.distanceSerie != null
					&& isTourSaved;

			fActionCreateMarker.setEnabled(isTourSaved);
			fActionCreateMarkerLeft.setEnabled(isTourSaved);
			fActionCreateMarkerRight.setEnabled(isTourSaved);

			fActionCreateRefTour.setEnabled(canCreateRefTours);
		}

	}

	public Chart getChart() {
		return fTourChartViewer.getTourChart();
	}

	public ChartXSlider getLeftSlider() {
		return fLeftSlider;
	}

	public ChartXSlider getRightSlider() {
		return fRightSlider;
	}

	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> tourList = new ArrayList<TourData>();
		tourList.add(fTourChartViewer.getTourChart().getTourData());

		return tourList;
	}
}
