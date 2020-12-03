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
package net.tourbook.tour;

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
import net.tourbook.tag.TagMenuManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.action.ActionCreateMarkerFromSlider;
import net.tourbook.ui.tourChart.action.ActionCreateRefTour;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

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
	private ActionCreateMarkerFromSlider				_actionCreateMarker;
	private ActionCreateMarkerFromSlider				_actionCreateMarkerLeft;
	private ActionCreateMarkerFromSlider				_actionCreateMarkerRight;

	private ActionSetTourTypeMenu			_actionSetTourType;
	private TagMenuManager					_tagMenuMgr;

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

		_actionCreateMarker = new ActionCreateMarkerFromSlider(this, //
				Messages.tourCatalog_view_action_create_marker,
				true);

		_actionCreateMarkerLeft = new ActionCreateMarkerFromSlider(
				this,
				Messages.tourCatalog_view_action_create_left_marker,
				true);

		_actionCreateMarkerRight = new ActionCreateMarkerFromSlider(
				this,
				Messages.tourCatalog_view_action_create_right_marker,
				false);

		_actionSetTourType = new ActionSetTourTypeMenu(this);

		_tagMenuMgr = new TagMenuManager(this, true);
	}

	/**
	 * enable actions
	 */
	private void enableActions() {

		final TourData tourData = _tourEditor.getTourData();
		final boolean isDataAvailable = tourData != null && tourData.getTourPerson() != null;
		final Set<TourTag> tourTags = tourData == null ? null : tourData.getTourTags();

		long existingTourTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;
		if (tourData != null) {
			final TourType tourType = tourData.getTourType();
			existingTourTypeId = tourType == null ? TourDatabase.ENTITY_IS_NOT_SAVED : tourType.getTypeId();
		}

		_actionQuickEdit.setEnabled(isDataAvailable);
		_actionEditTour.setEnabled(isDataAvailable);

		_tagMenuMgr.enableTagActions(//
				isDataAvailable,
				isDataAvailable && tourTags.size() > 0,
				tourTags);

		TourTypeMenuManager.enableRecentTourTypeActions(isDataAvailable, existingTourTypeId);
	}

	@Override
	public void fillBarChartContextMenu(final IMenuManager menuMgr,
										final int hoveredBarSerieIndex,
										final int hoveredBarValueIndex) {}

	@Override
	public void fillContextMenu(final IMenuManager menuMgr,
								final int mouseDownDevPositionX,
								final int mouseDownDevPositionY) {

		menuMgr.add(new Separator());
		menuMgr.add(_actionQuickEdit);
		menuMgr.add(_actionEditTour);
		menuMgr.add(_actionOpenMarkerDialog);
		menuMgr.add(_actionAdjustAltitude);

		// tour tag actions
      _tagMenuMgr.fillTagMenu(menuMgr, false);

		// tour type actions
		menuMgr.add(new Separator());
		menuMgr.add(_actionSetTourType);
		TourTypeMenuManager.fillMenuWithRecentTourTypes(menuMgr, this, true);

		enableActions();
	}

	@Override
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

	@Override
	public Chart getChart() {
		return _tourEditor.getTourChart();
	}

	@Override
	public ChartXSlider getLeftSlider() {
		return _leftSlider;
	}

	@Override
	public ChartXSlider getRightSlider() {
		return _rightSlider;
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> tourList = new ArrayList<>();
		tourList.add(_tourEditor.getTourData());

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
				Display.getCurrent().getCursorLocation(),
				null);
	}

	@Override
	public boolean showOnlySliderContextMenu() {
		return false;
	}

}
