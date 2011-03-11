/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartXSlider;
import net.tourbook.chart.IChartContextProvider;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.extension.export.ActionExport;
import net.tourbook.tag.TagMenuManager;
import net.tourbook.tour.ActionOpenAdjustAltitudeDialog;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.TourTypeMenuManager;
import net.tourbook.ui.ITourChartViewer;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.tourChart.action.ActionCreateMarker;
import net.tourbook.ui.tourChart.action.ActionCreateRefTour;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

public class TourChartContextProvicer implements IChartContextProvider, ITourProvider {

	private final ITourChartViewer			_tourChartViewer;

	private ActionEditQuick					_actionQuickEdit;
	private ActionEditTour					_actionEditTour;
	private ActionOpenMarkerDialog			_actionOpenMarkerDialog;
	private ActionOpenAdjustAltitudeDialog	_actionOpenAdjustAltitudeDialog;
	private ActionOpenTour					_actionOpenTour;

	private ActionCreateRefTour				_actionCreateRefTour;
	private ActionCreateMarker				_actionCreateMarker;
	private ActionCreateMarker				_actionCreateMarkerLeft;
	private ActionCreateMarker				_actionCreateMarkerRight;
	private ActionExport					_actionExportTour;

	private TagMenuManager					_tagMenuMgr;
	private ActionSetTourTypeMenu			_actionSetTourType;

	private ChartXSlider					_leftSlider;
	private ChartXSlider					_rightSlider;

	/**
	 * Provides a context menu for a tour chart
	 * 
	 * @param tourChartViewer
	 */
	public TourChartContextProvicer(final ITourChartViewer tourChartViewer) {

		_tourChartViewer = tourChartViewer;

		_actionQuickEdit = new ActionEditQuick(_tourChartViewer);
		_actionEditTour = new ActionEditTour(_tourChartViewer);
		_actionOpenTour = new ActionOpenTour(_tourChartViewer);

		final TourChart tourChart = _tourChartViewer.getTourChart();

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

		_actionExportTour = new ActionExport(this);

		_actionOpenMarkerDialog = new ActionOpenMarkerDialog(this, true);
		_actionOpenMarkerDialog.setEnabled(true);

		_actionOpenAdjustAltitudeDialog = new ActionOpenAdjustAltitudeDialog(this);
		_actionOpenAdjustAltitudeDialog.setEnabled(true);

		_actionSetTourType = new ActionSetTourTypeMenu(this);

		_tagMenuMgr = new TagMenuManager(this, true);
	}

	private void enableActions() {

		final TourChart tourChart = _tourChartViewer.getTourChart();
		final TourData tourData = tourChart.getTourData();

		final boolean isDataAvailable = tourData != null && tourData.getTourPerson() != null;
		final Set<TourTag> tourTags = tourData == null ? null : tourData.getTourTags();

		long existingTourTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;
		if (tourData != null) {
			final TourType tourType = tourData.getTourType();
			existingTourTypeId = tourType == null ? TourDatabase.ENTITY_IS_NOT_SAVED : tourType.getTypeId();
		}
		_actionQuickEdit.setEnabled(isDataAvailable);
		_actionEditTour.setEnabled(isDataAvailable);
		_actionOpenMarkerDialog.setEnabled(isDataAvailable);
		_actionOpenAdjustAltitudeDialog.setEnabled(isDataAvailable);
		_actionOpenTour.setEnabled(isDataAvailable);
		_actionExportTour.setEnabled(true);

		_tagMenuMgr.enableTagActions(//
				isDataAvailable,
				isDataAvailable && tourTags.size() > 0,
				tourTags);

		_actionSetTourType.setEnabled(isDataAvailable);
		TourTypeMenuManager.enableRecentTourTypeActions(isDataAvailable, existingTourTypeId);
	}

	public void fillBarChartContextMenu(final IMenuManager menuMgr,
										final int hoveredBarSerieIndex,
										final int hoveredBarValueIndex) {}

	public void fillContextMenu(final IMenuManager menuMgr,
								final int mouseDownDevPositionX,
								final int mouseDownDevPositionY) {

		final TourChart tourChart = _tourChartViewer.getTourChart();

		menuMgr.add(new Separator());
		menuMgr.add(_actionQuickEdit);
		menuMgr.add(_actionEditTour);
		menuMgr.add(_actionOpenMarkerDialog);
		menuMgr.add(_actionOpenAdjustAltitudeDialog);
		menuMgr.add(_actionOpenTour);
		menuMgr.add(_actionExportTour);

		// tour tag actions
		_tagMenuMgr.fillTagMenu(menuMgr);

		// tour type actions
		menuMgr.add(new Separator());
		menuMgr.add(_actionSetTourType);
		TourTypeMenuManager.fillMenuWithRecentTourTypes(menuMgr, this, true);

		// set slider position in export action
		_actionExportTour.setTourRange(tourChart.getLeftSlider().getValuesIndex(), //
				tourChart.getRightSlider().getValuesIndex());

		enableActions();
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

			/*
			 * enable actions
			 */
			final TourData tourData = _tourChartViewer.getTourChart().getTourData();
			final boolean isTourSaved = tourData != null && tourData.getTourPerson() != null;

			final boolean canCreateRefTours = tourData != null
					&& tourData.altitudeSerie != null
					&& tourData.distanceSerie != null
					&& isTourSaved;

			_actionCreateMarker.setEnabled(isTourSaved);
			_actionCreateMarkerLeft.setEnabled(isTourSaved);
			_actionCreateMarkerRight.setEnabled(isTourSaved);

			_actionCreateRefTour.setEnabled(canCreateRefTours);
		}
	}

	public Chart getChart() {
		return _tourChartViewer.getTourChart();
	}

	public ChartXSlider getLeftSlider() {
		return _leftSlider;
	}

	public ChartXSlider getRightSlider() {
		return _rightSlider;
	}

	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> tourList = new ArrayList<TourData>();
		tourList.add(_tourChartViewer.getTourChart().getTourData());

		return tourList;
	}

	@Override
	public void onHideContextMenu(final MenuEvent menuEvent, final Control menuParentControl) {

		_tagMenuMgr.onHideMenu();
	}

	@Override
	public void onShowContextMenu(final MenuEvent menuEvent, final Control menuParentControl) {

		_tagMenuMgr.onShowMenu(//
				menuEvent,
				menuParentControl,
				Display.getCurrent().getCursorLocation());
	}

	public boolean showOnlySliderContextMenu() {
		return true;
	}
}
