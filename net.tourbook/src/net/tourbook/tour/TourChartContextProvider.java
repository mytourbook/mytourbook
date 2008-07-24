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
import net.tourbook.chart.ChartMarker;
import net.tourbook.chart.ChartXSlider;
import net.tourbook.chart.IChartContextProvider;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.ActionRemoveAllTags;
import net.tourbook.tag.ActionSetTourTag;
import net.tourbook.tag.TagManager;
import net.tourbook.ui.ActionOpenPrefDialog;
import net.tourbook.ui.ActionSetTourType;
import net.tourbook.ui.ISelectedTours;
import net.tourbook.ui.views.tourCatalog.ReferenceTourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Display;

public class TourChartContextProvider implements IChartContextProvider, ISelectedTours {

	public ChartXSlider				fSlider;

	private MarkerDialog			fMarkerDialog;
	private TourEditor				fTourEditor;

	private ActionSetTourType		fActionSetTourType;
	private ActionEditQuick			fActionQuickEdit;

	private ActionSetTourTag		fActionAddTag;
	private ActionSetTourTag		fActionRemoveTag;
	private ActionRemoveAllTags		fActionRemoveAllTags;

	private ActionOpenPrefDialog	fActionOpenTagPrefs;

	/**
	 * add a new reference tour to all reference tours
	 */
	class ActionAddTourReference extends Action {

		ActionAddTourReference() {
			setText(Messages.tourCatalog_view_action_create_reference_tour);
		}

		@Override
		public void run() {
			ReferenceTourManager.getInstance().addReferenceTour(fTourEditor);
		}
	}

	class SliderAction extends Action {

		SliderAction(final String text, final ChartXSlider slider) {
			super(text);

			fSlider = slider;
		}

		private TourMarker createTourMarker(final TourData tourData) {

			final int serieIndex = fSlider.getValuesIndex();

			// create a new marker
			final TourMarker newTourMarker = new TourMarker(tourData, ChartMarker.MARKER_TYPE_CUSTOM);
			newTourMarker.setSerieIndex(serieIndex);
			newTourMarker.setDistance(tourData.getMetricDistanceSerie()[serieIndex]);
			newTourMarker.setTime(tourData.timeSerie[serieIndex]);
			newTourMarker.setLabel(Messages.TourData_Label_new_marker);
			newTourMarker.setVisualPosition(ChartMarker.VISUAL_HORIZONTAL_ABOVE_GRAPH_CENTERED);

			return newTourMarker;
		}

		@Override
		public void run() {

			if (fTourEditor != null) {

				final TourChart tourChart = fTourEditor.getTourChart();
				final TourData tourData = tourChart.getTourData();

				final TourMarker newTourMarker = createTourMarker(tourData);

				final MarkerDialog markerDialog = new MarkerDialog(Display.getCurrent().getActiveShell(),
						tourData,
						null);

				markerDialog.create();
				markerDialog.addTourMarker(newTourMarker);
				markerDialog.open();

				/*
				 * Currently the dialog works with the markers from the tour editor not with a
				 * backup, so changes in the dialog are made in the tourdata of the tour editor ->
				 * the tour will be dirty when this dialog was opened
				 */

				// force the tour to be saved
				tourChart.setTourDirty(true);

				// update chart
				tourChart.updateMarkerLayer(true);

				// update marker list and other listener
				TourDatabase.getInstance().firePropertyChange(TourDatabase.TOUR_IS_CHANGED);

			} else if (fMarkerDialog != null) {

				final TourMarker newTourMarker = createTourMarker(fMarkerDialog.getTourData());

				fMarkerDialog.addTourMarker(newTourMarker);
			}
		}
	}

	public TourChartContextProvider(final MarkerDialog markerDialog) {
		fMarkerDialog = markerDialog;
	}

	public TourChartContextProvider(final TourEditor tourEditor) {

		fTourEditor = tourEditor;

		fActionQuickEdit = new ActionEditQuick(this);
		fActionSetTourType = new ActionSetTourType(this);

		fActionAddTag = new ActionSetTourTag(this, true);
		fActionRemoveTag = new ActionSetTourTag(this, false);
		fActionRemoveAllTags = new ActionRemoveAllTags(this);

		fActionOpenTagPrefs = new ActionOpenPrefDialog(Messages.action_tag_open_tagging_structure,
				ITourbookPreferences.PREF_PAGE_TAGS);
	}

	private void createMarkerMenu(	final IMenuManager menuMgr,
									final ChartXSlider leftSlider,
									final ChartXSlider rightSlider) {

		if (leftSlider != null || rightSlider != null) {

			// marker menu
			if (leftSlider != null && rightSlider == null) {
				menuMgr.add(new SliderAction(Messages.tourCatalog_view_action_create_marker, leftSlider));
			} else {
				menuMgr.add(new SliderAction(Messages.tourCatalog_view_action_create_left_marker, leftSlider));
				menuMgr.add(new SliderAction(Messages.tourCatalog_view_action_create_right_marker, rightSlider));
			}
		}
	}

	/**
	 * enable actions for the tour editor
	 */
	private void enableActions() {
		
		final TourChart tourChart = fTourEditor.getTourChart();
		final TourData tourData = tourChart.getTourData();

		final boolean isDataAvailable = tourData != null && tourData.getTourPerson() != null;

		boolean isTagAvailable = false;
		if (isDataAvailable) {
			isTagAvailable = tourData.getTourTags().size() > 0;
		}

		fActionQuickEdit.setEnabled(isDataAvailable);
		fActionSetTourType.setEnabled(TourDatabase.getTourTypes().size() > 0);

		fActionAddTag.setEnabled(isDataAvailable);
		fActionRemoveTag.setEnabled(isDataAvailable && isTagAvailable);
		fActionRemoveAllTags.setEnabled(isDataAvailable && isTagAvailable);

		fActionSetTourType.setEnabled(isDataAvailable);

		// enable actions for the recent tags
		TagManager.enableRecentTagActions(isDataAvailable);
	}

	public void fillBarChartContextMenu(final IMenuManager menuMgr,
										final int hoveredBarSerieIndex,
										final int hoveredBarValueIndex) {}

	public void fillContextMenu(final IMenuManager menuMgr) {

		if (fTourEditor != null) {

			menuMgr.add(fActionQuickEdit);
			menuMgr.add(fActionSetTourType);

			menuMgr.add(new Separator());
			menuMgr.add(fActionAddTag);
			menuMgr.add(fActionRemoveTag);
			menuMgr.add(fActionRemoveAllTags);

			TagManager.fillRecentTagsIntoMenu(menuMgr, this, true);

			menuMgr.add(new Separator());
			menuMgr.add(fActionOpenTagPrefs);

			enableActions();
		}
	}

	public void fillXSliderContextMenu(	final IMenuManager menuMgr,
										final ChartXSlider leftSlider,
										final ChartXSlider rightSlider) {

		if (fTourEditor != null) {

			// context dialog for the tour editor

			if (leftSlider != null || rightSlider != null) {

				createMarkerMenu(menuMgr, leftSlider, rightSlider);

				// action: create chart reference
				final TourData tourData = fTourEditor.getTourChart().getTourData();
				final boolean isEnabled = tourData.altitudeSerie != null && tourData.distanceSerie != null;

				final ActionAddTourReference actionAddTourReference = new ActionAddTourReference();
				actionAddTourReference.setEnabled(isEnabled);

				menuMgr.add(actionAddTourReference);
			}

		} else if (fMarkerDialog != null) {

			// context menu for the marker dialog

			createMarkerMenu(menuMgr, leftSlider, rightSlider);
		}

	}

	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> selectedTourData = new ArrayList<TourData>();
		selectedTourData.add(fTourEditor.getTourChart().getTourData());
		return selectedTourData;

	}

	public boolean isFromTourEditor() {
		return true;
	}

}
