/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.statistic;

import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class TourStatisticsView extends ViewPart implements ITourProvider {

	public static final String		ID			= "net.tourbook.views.StatisticView";						//$NON-NLS-1$

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getDefault().getPreferenceStore();

	private final IDialogSettings	_state		= TourbookPlugin.getDefault()//
														.getDialogSettingsSection("TourStatisticsView");	//$NON-NLS-1$

	private StatContainer			_statContainer;

	private PostSelectionProvider	_postSelectionProvider;
	private IPartListener2			_partListener;
	private IPropertyChangeListener	_prefChangeListener;
	private ITourEventListener		_tourEventListener;
	private ISelectionListener		_postSelectionListener;

	private TourPerson				_activePerson;
	private TourTypeFilter			_activeTourTypeFilter;

	private void addPartListener() {

		// set the part listener
		_partListener = new IPartListener2() {

			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourStatisticsView.this) {
					saveState();
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {}
		};

		// register the part listener
		getSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {

			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				/*
				 * set a new chart configuration when the preferences has changed
				 */

				if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)
						|| property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)
						|| property.equals(ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED)) {

					_activePerson = TourbookPlugin.getActivePerson();
					_activeTourTypeFilter = TourbookPlugin.getActiveTourTypeFilter();

					updateStatistics();

				} else if (property.equals(ITourbookPreferences.STATISTICS_STATISTIC_PROVIDER_IDS)) {

					_statContainer.refreshStatisticProvider();

				} else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					UI.updateUnits();

					updateStatistics();
				}
			}
		};

		// register the listener
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	private void addSelectionListener() {

		// this view part is a selection listener
		_postSelectionListener = new ISelectionListener() {

			@Override
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (selection instanceof SelectionDeletedTours) {
					updateStatistics();
				}
			}
		};

		// register selection listener in the page
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			@Override
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
					updateStatistics();

				} else if (eventId == TourEventId.UPDATE_UI || //
						eventId == TourEventId.ALL_TOURS_ARE_MODIFIED) {
					updateStatistics();
				}
			}
		};
		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	@Override
	public void createPartControl(final Composite parent) {

		// this view is a selection provider, set it before the statistics container is created
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

		_statContainer = new StatContainer(parent, getViewSite(), _postSelectionProvider, SWT.NONE);

		addPartListener();
		addPrefListener();
		addSelectionListener();
		addTourEventListener();

		/*
		 * Start async that the workspace is fully initialized with all data filters
		 */
		parent.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {

				_activePerson = TourbookPlugin.getActivePerson();
				_activeTourTypeFilter = TourbookPlugin.getActiveTourTypeFilter();

				_statContainer.restoreStatistics(_state, _activePerson, _activeTourTypeFilter);
			}
		});
	}

	@Override
	public void dispose() {

		getViewSite().getPage().removePartListener(_partListener);
		getSite().getPage().removePostSelectionListener(_postSelectionListener);
		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		final TourbookStatistic selectedStatistic = _statContainer.getSelectedStatistic();
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

	public void saveState() {
		_statContainer.saveState(_state);
	}

	@Override
	public void setFocus() {}

	private void updateStatistics() {
		_statContainer.updateStatistic(_activePerson, _activeTourTypeFilter);
	}

}
