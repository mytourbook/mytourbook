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
/**
 * 
 */
package net.tourbook.statistics;

import java.util.ArrayList;
import java.util.Set;

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartXSlider;
import net.tourbook.chart.IChartContextProvider;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.ActionRemoveAllTags;
import net.tourbook.tag.ActionSetTourTag;
import net.tourbook.tag.TagManager;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourTypeMenuManager;
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

	private final Chart					_chart;
	private final IBarSelectionProvider	_barSelectionProvider;

	private final ActionEditQuick		_actionEditQuick;
	private final ActionEditTour		_actionEditTour;
	private final ActionOpenTour		_actionOpenTour;

	private final ActionSetTourTypeMenu	_actionSetTourType;
	private final ActionSetTourTag		_actionAddTag;
	private final ActionSetTourTag		_actionRemoveTag;

	private final ActionRemoveAllTags	_actionRemoveAllTags;
	private final ActionOpenPrefDialog	_actionOpenTagPrefs;


	public TourChartContextProvider(final Chart chart, final IBarSelectionProvider barSelectionProvider) {

		_chart = chart;
		_barSelectionProvider = barSelectionProvider;

		_actionEditQuick = new ActionEditQuick(this);
		_actionEditTour = new ActionEditTour(this);
		_actionOpenTour = new ActionOpenTour(this);

		_actionSetTourType = new ActionSetTourTypeMenu(this);
		_actionAddTag = new ActionSetTourTag(this, true);
		_actionRemoveTag = new ActionSetTourTag(this, false);
		_actionRemoveAllTags = new ActionRemoveAllTags(this);

		_actionOpenTagPrefs = new ActionOpenPrefDialog(
				net.tourbook.Messages.action_tag_open_tagging_structure,
				ITourbookPreferences.PREF_PAGE_TAGS);
	}

	private void enableActions(final boolean isTourHovered) {

		boolean isTagAvailable = false;
		Set<TourTag> allExistingTags = null;
		long existingTourTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;

		final Long selectedTourId = _barSelectionProvider.getSelectedTourId();
		if (selectedTourId != null) {

			final TourData tourData = TourManager.getInstance().getTourData(selectedTourId);

			if (tourData != null) {

				allExistingTags = tourData.getTourTags();
				isTagAvailable = allExistingTags.size() > 0;

				final TourType tourType = tourData.getTourType();
				existingTourTypeId = tourType == null ? TourDatabase.ENTITY_IS_NOT_SAVED : tourType.getTypeId();
			}
		}

		_actionEditQuick.setEnabled(isTourHovered);
		_actionEditTour.setEnabled(isTourHovered);
		_actionOpenTour.setEnabled(isTourHovered);

		_actionSetTourType.setEnabled(isTourHovered && TourDatabase.getAllTourTypes().size() > 0);
		_actionAddTag.setEnabled(isTourHovered);
		_actionRemoveTag.setEnabled(isTourHovered && isTagAvailable);
		_actionRemoveAllTags.setEnabled(isTourHovered && isTagAvailable);

		// enable/disable actions for tags/tour types
		TagManager.enableRecentTagActions(isTourHovered, allExistingTags);
		TourTypeMenuManager.enableRecentTourTypeActions(isTourHovered, existingTourTypeId);
	}

	public void fillBarChartContextMenu(final IMenuManager menuMgr,
										final int hoveredBarSerieIndex,
										final int hoveredBarValueIndex) {

		menuMgr.add(_actionEditQuick);
		menuMgr.add(_actionEditTour);
		menuMgr.add(_actionOpenTour);

		// tour type action
		menuMgr.add(new Separator());
		menuMgr.add(_actionSetTourType);
		TourTypeMenuManager.fillMenuRecentTourTypes(menuMgr, this, true);

		// tour tag actions
		menuMgr.add(new Separator());
		menuMgr.add(_actionAddTag);
		TagManager.fillMenuRecentTags(menuMgr, this, true, true);
		menuMgr.add(_actionRemoveTag);
		menuMgr.add(_actionRemoveAllTags);
		menuMgr.add(_actionOpenTagPrefs);

		enableActions(hoveredBarSerieIndex != -1);
	}

	public void fillContextMenu(final IMenuManager menuMgr,
								final int mouseDownDevPositionX,
								final int mouseDownDevPositionY) {}

	public void fillXSliderContextMenu(	final IMenuManager menuMgr,
										final ChartXSlider leftSlider,
										final ChartXSlider rightSlider) {}

	public Chart getChart() {
		return _chart;
	}

	public ChartXSlider getLeftSlider() {
		return null;
	}

	public ChartXSlider getRightSlider() {
		return null;
	}

	public ArrayList<TourData> getSelectedTours() {

		final Long selectedTourId = _barSelectionProvider.getSelectedTourId();
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
