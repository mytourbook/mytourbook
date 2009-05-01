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
/**
 * 
 */
package net.tourbook.statistics;

import java.util.ArrayList;

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartXSlider;
import net.tourbook.chart.IChartContextProvider;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.ActionRemoveAllTags;
import net.tourbook.tag.ActionSetTourTag;
import net.tourbook.tag.TagManager;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionOpenPrefDialog;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.action.ActionSetTourTypeMenu;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;

/**
 * provides the fill menu methods for the chart context menu
 */
class TourChartContextProvider implements IChartContextProvider, ITourProvider {

	/** 
	 * 
	 */
	private final Chart					fChart;
	private final IBarSelectionProvider	fBarSelectionProvider;

	private final ActionEditQuick		fActionEditQuick;
	private final ActionEditTour		fActionEditTour;
	private final ActionOpenTour		fActionOpenTour;

	private final ActionSetTourTypeMenu		fActionSetTourType;
	private final ActionSetTourTag		fActionAddTag;
	private final ActionSetTourTag		fActionRemoveTag;

	private final ActionRemoveAllTags	fActionRemoveAllTags;
	private final ActionOpenPrefDialog	fActionOpenTagPrefs;

	public TourChartContextProvider(final Chart chart, final IBarSelectionProvider barSelectionProvider) {

		fChart = chart;
		fBarSelectionProvider = barSelectionProvider;

		fActionEditQuick = new ActionEditQuick(this);
		fActionEditTour = new ActionEditTour(this);
		fActionOpenTour = new ActionOpenTour(this);

		fActionSetTourType = new ActionSetTourTypeMenu(this);
		fActionAddTag = new ActionSetTourTag(this, true);
		fActionRemoveTag = new ActionSetTourTag(this, false);
		fActionRemoveAllTags = new ActionRemoveAllTags(this);

		fActionOpenTagPrefs = new ActionOpenPrefDialog(net.tourbook.Messages.action_tag_open_tagging_structure,
				ITourbookPreferences.PREF_PAGE_TAGS);
	}

	private void enableActions(final boolean isTourHovered) {

		boolean isTagAvailable = false;
		final Long selectedTourId = fBarSelectionProvider.getSelectedTourId();
		if (selectedTourId != null) {
			final TourData tourData = TourManager.getInstance().getTourData(selectedTourId);
			if (tourData != null) {
				isTagAvailable = tourData.getTourTags().size() > 0;
			}
		}

		fActionEditQuick.setEnabled(isTourHovered);
		fActionEditTour.setEnabled(isTourHovered);
		fActionOpenTour.setEnabled(isTourHovered);

		fActionSetTourType.setEnabled(isTourHovered && TourDatabase.getAllTourTypes().size() > 0);
		fActionAddTag.setEnabled(isTourHovered);
		fActionRemoveTag.setEnabled(isTourHovered && isTagAvailable);
		fActionRemoveAllTags.setEnabled(isTourHovered && isTagAvailable);

		// enable actions for the recent tags
		TagManager.enableRecentTagActions(isTourHovered);
	}

	public void fillBarChartContextMenu(final IMenuManager menuMgr,
										final int hoveredBarSerieIndex,
										final int hoveredBarValueIndex) {

		menuMgr.add(fActionEditQuick);
		menuMgr.add(fActionEditTour);
		menuMgr.add(fActionOpenTour);

		menuMgr.add(new Separator());
		menuMgr.add(fActionSetTourType);
		menuMgr.add(fActionAddTag);
		menuMgr.add(fActionRemoveTag);
		menuMgr.add(fActionRemoveAllTags);
		TagManager.fillRecentTagsIntoMenu(menuMgr, this, true, true);
		menuMgr.add(fActionOpenTagPrefs);

		enableActions(hoveredBarSerieIndex != -1);
	}

	public void fillContextMenu(final IMenuManager menuMgr,
								final int mouseDownDevPositionX,
								final int mouseDownDevPositionY) {}

	public void fillXSliderContextMenu(	final IMenuManager menuMgr,
										final ChartXSlider leftSlider,
										final ChartXSlider rightSlider) {}

	public Chart getChart() {
		return fChart;
	}

	public ChartXSlider getLeftSlider() {
		return null;
	}

	public ChartXSlider getRightSlider() {
		return null;
	}

	public ArrayList<TourData> getSelectedTours() {

		final Long selectedTourId = fBarSelectionProvider.getSelectedTourId();
		if (selectedTourId != null) {

			final TourData tourData = TourManager.getInstance().getTourData(selectedTourId);
			if (tourData != null) {

				final ArrayList<TourData> selectedTourData = new ArrayList<TourData>();
				selectedTourData.add(tourData);

				return selectedTourData;
			}
		}

		return null;
	}
 
	public boolean showOnlySliderContextMenu() {
		return false;
	}
}
