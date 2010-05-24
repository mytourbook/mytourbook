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
package net.tourbook.ui.views;

import java.util.ArrayList;

import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.statistic.StatisticContainer;
import net.tourbook.statistic.TourbookStatistic;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class TourStatisticsView extends ViewPart implements ITourProvider {

	public static final String		ID			= "net.tourbook.views.StatisticView";				//$NON-NLS-1$

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getDefault().getPreferenceStore();

	private final IDialogSettings	_state		= TourbookPlugin.getDefault().getDialogSettingsSection(
														"TourStatisticsView");						//$NON-NLS-1$

	private StatisticContainer		_statisticContainer;

	private PostSelectionProvider	_postSelectionProvider;
	private IPartListener2			_partListener;
	private IPropertyChangeListener	_prefChangeListener;
	private ITourEventListener		_tourEventListener;
	private ISelectionListener		_postSelectionListener;

	private TourPerson				_activePerson;
	private TourTypeFilter			_activeTourTypeFilter;

	private RGB						_rgbYearFg	= new RGB(255, 255, 255);
	private RGB						_rgbMonthFg	= new RGB(128, 64, 0);
	private RGB						_rgbTourFg	= new RGB(0, 0, 128);

	private RGB						_rgbYearBg	= new RGB(111, 130, 197);
	private RGB						_rgbMonthBg	= new RGB(220, 220, 255);
	private RGB						_rgbTourBg	= new RGB(240, 240, 255);

	private Color					_colorYearFg;
	private Color					_colorMonthFg;
	private Color					_colorTourFg;

	private Color					_colorYearBg;
	private Color					_colorMonthBg;
	private Color					_colorTourBg;

	public Font						_fontNormal;
	public Font						_fontBold;

	private void addPartListener() {

		// set the part listener
		_partListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourStatisticsView.this) {
					_statisticContainer.activateActions(getSite());
				}
			}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourStatisticsView.this) {
					saveState();
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourStatisticsView.this) {
					_statisticContainer.deactivateActions(getSite());
				}
			}

			public void partHidden(final IWorkbenchPartReference partRef) {}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {}
		};

		// register the part listener
		getSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {

			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				/*
				 * set a new chart configuration when the preferences has changed
				 */

				if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

					_activePerson = TourbookPlugin.getActivePerson();
					_activeTourTypeFilter = TourbookPlugin.getActiveTourTypeFilter();

					refreshStatistics();

				} else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					// update statistics
					refreshStatistics();

				} else if (property.equals(ITourbookPreferences.STATISTICS_STATISTIC_PROVIDER_IDS)) {

					_statisticContainer.refreshStatisticProvider();

				} else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					UI.updateUnits();

					refreshStatistics();
				}
			}
		};

		// register the listener
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	private void addSelectionListener() {

		// this view part is a selection listener
		_postSelectionListener = new ISelectionListener() {

			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (selection instanceof SelectionDeletedTours) {
					refreshStatistics();
				}
			}
		};

		// register selection listener in the page
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object propertyData) {

				if (eventId == TourEventId.TOUR_CHANGED && propertyData instanceof TourEvent) {

					if (part == TourStatisticsView.this) {
						return;
					}

					if (((TourEvent) propertyData).isTourModified) {
						/*
						 * ignore edit changes because the statistics show data only from saved data
						 */
						return;
					}

					// update statistics
					refreshStatistics();

				} else if (eventId == TourEventId.UPDATE_UI || //
						eventId == TourEventId.ALL_TOURS_ARE_MODIFIED) {
					refreshStatistics();
				}
			}
		};
		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	@Override
	public void createPartControl(final Composite parent) {

		createResources();

		// this view is a selection provider, set it before the statistics container is created
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider());

		_statisticContainer = new StatisticContainer(getViewSite(), _postSelectionProvider, parent, SWT.NONE);

		addPartListener();
		addPrefListener();
		addSelectionListener();
		addTourEventListener();

		_activePerson = TourbookPlugin.getActivePerson();
		_activeTourTypeFilter = TourbookPlugin.getActiveTourTypeFilter();

		_statisticContainer.restoreStatistics(_state, _activePerson, _activeTourTypeFilter);
	}

	private void createResources() {

		final Display display = Display.getCurrent();

		_colorYearFg = new Color(display, _rgbYearFg);
		_colorYearBg = new Color(display, _rgbYearBg);
		_colorMonthFg = new Color(display, _rgbMonthFg);
		_colorMonthBg = new Color(display, _rgbMonthBg);
		_colorTourFg = new Color(display, _rgbTourFg);
		_colorTourBg = new Color(display, _rgbTourBg);

		_fontNormal = JFaceResources.getFontRegistry().get(JFaceResources.DIALOG_FONT);
		_fontBold = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
	}

	@Override
	public void dispose() {

		getViewSite().getPage().removePartListener(_partListener);
		getSite().getPage().removePostSelectionListener(_postSelectionListener);
		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		_colorYearFg.dispose();
		_colorYearBg.dispose();
		_colorMonthFg.dispose();
		_colorMonthBg.dispose();
		_colorTourFg.dispose();
		_colorTourBg.dispose();

		super.dispose();
	}

	public ArrayList<TourData> getSelectedTours() {

		final TourbookStatistic selectedStatistic = _statisticContainer.getSelectedStatistic();
		if (selectedStatistic == null) {
			return null;
		}

		final Long selectedTourId = selectedStatistic.getSelectedTour();
		if (selectedTourId == null) {
			return null;
		}

		final TourData selectedTourData = TourManager.getInstance().getTourData(selectedTourId);
		if (selectedTourData == null) {
			return null;
		} else {
			final ArrayList<TourData> selectedTours = new ArrayList<TourData>();
			selectedTours.add(selectedTourData);
			return selectedTours;
		}
	}

	private void refreshStatistics() {
		_statisticContainer.refreshStatistic(_activePerson, _activeTourTypeFilter);
	}

	public void saveState() {
		_statisticContainer.saveState(_state);
	}

	@Override
	public void setFocus() {}

}
