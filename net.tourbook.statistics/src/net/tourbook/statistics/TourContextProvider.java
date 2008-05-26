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
/**
 * 
 */

package net.tourbook.statistics;

import java.util.ArrayList;

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartXSlider;
import net.tourbook.chart.IChartContextProvider;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.ActionEditQuick;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ActionSetTourTag;
import net.tourbook.ui.ActionSetTourType;
import net.tourbook.ui.ISelectedTours;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;

/**
 * provides the fill menu methods for the chart context menu
 */
class TourContextProvider implements IChartContextProvider, ISelectedTours {

	/** 
	 * 
	 */
	private final IBarSelectionProvider	fBarSelectionProvider;

	private final ActionEditQuick		fActionEditQuick;
	private final ActionEditTour		fActionEditTour;
	private final ActionSetTourType		fActionSetTourType;
	private final ActionSetTourTag		fActionSetTourTag;

	private class ActionEditTour extends Action {

		public ActionEditTour(final String text) {

			super(text);

			setImageDescriptor(TourbookPlugin.getImageDescriptor("write_obj.gif")); //$NON-NLS-1$
			setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor("write_obj_disabled.gif")); //$NON-NLS-1$
		}

		@Override
		public void run() {

			final Long selectedTourId = fBarSelectionProvider.getSelectedTourId();

			if (selectedTourId != null) {

				// select the tour in the chart and open the tour in the editor

				fBarSelectionProvider.selectTour(selectedTourId);

				TourManager.getInstance().openTourInEditor(selectedTourId);
			}
		}
	}

//	private class ActionZoomIntoMonth extends Action {
//
//		public ActionZoomIntoMonth(String text) {
//			super(text);
//		}
//
//		@Override
//		public void run() {
//			fChart.zoomWithParts(12, fBarSelectionProvider.getSelectedMonth() - 1, false);
//		}
//	}

	public TourContextProvider(final Chart chart, final IBarSelectionProvider barSelectionProvider) {

		fBarSelectionProvider = barSelectionProvider;

		fActionEditQuick = new ActionEditQuick(this);
		fActionEditTour = new ActionEditTour(Messages.action_edit_tour);
		fActionSetTourType = new ActionSetTourType(this);
		fActionSetTourTag = new ActionSetTourTag(this);
	}

	public void fillBarChartContextMenu(final IMenuManager menuMgr,
										final int hoveredBarSerieIndex,
										final int hoveredBarValueIndex) {

		final boolean isTourHovered = hoveredBarSerieIndex != -1;

		fActionEditQuick.setEnabled(isTourHovered);
		fActionEditTour.setEnabled(isTourHovered);

		final ArrayList<TourType> tourTypes = TourDatabase.getTourTypes();
		fActionSetTourType.setEnabled(isTourHovered && tourTypes.size() > 0);
		fActionSetTourTag.setEnabled(isTourHovered && tourTypes.size() > 0);

		menuMgr.add(fActionEditQuick);
		menuMgr.add(fActionSetTourType);
		menuMgr.add(fActionSetTourTag);

		menuMgr.add(new Separator());
		menuMgr.add(fActionEditTour);

// disabled because it shows the wrong month		
//		menuMgr.add(new Separator());
//		menuMgr.add(new ActionZoomIntoMonth(Messages.ACTION_ZOOM_INTO_MONTH));
	}

	public void fillContextMenu(final IMenuManager menuMgr) {}

	public void fillXSliderContextMenu(	final IMenuManager menuMgr,
										final ChartXSlider leftSlider,
										final ChartXSlider rightSlider) {}

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

	public boolean isFromTourEditor() {
		return false;
	}

}
