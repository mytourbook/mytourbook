/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

/**
 * Provides a skeleton for a view which displays a tour chart
 */
public abstract class TourChartViewPart extends ViewPart {

	private static final String			ID			= "net.tourbook.ui.tourChart.TourChartViewPart";	//$NON-NLS-1$

	private final IPreferenceStore		_prefStore	= TourbookPlugin.getDefault().getPreferenceStore();

	public TourData						_tourData;

	protected TourChart					_tourChart;
	protected TourChartConfiguration	_tourChartConfig;

	public PostSelectionProvider		_postSelectionProvider;

	private IPropertyChangeListener		_prefChangeListener;
	private ITourEventListener			_tourEventListener;
	private ISelectionListener			_postSelectionListener;
	private IPartListener2				_partListener;

	/**
	 * set the part listener to save the view settings, the listeners are called before the controls
	 * are disposed
	 */
	private void addPartListeners() {

		_partListener = new IPartListener2() {

			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourChartViewPart.this) {}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourChartViewPart.this) {
					_tourChart.partIsHidden();
				}
			}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourChartViewPart.this) {
					_tourChart.partIsVisible();
				}
			}
		};

		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				/*
				 * set a new chart configuration when the preferences has changed
				 */
				if (property.equals(ITourbookPreferences.GRAPH_VISIBLE)
						|| property.equals(ITourbookPreferences.GRAPH_X_AXIS)
						|| property.equals(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME)) {

					_tourChartConfig = TourManager.createDefaultTourChartConfig();

					if (_tourChart != null) {
						_tourChart.updateTourChart(_tourData, _tourChartConfig, false);
					}

				} else if (property.equals(ITourbookPreferences.GRAPH_MOUSE_MODE)) {

					_tourChart.setMouseMode(event.getNewValue());
				}
			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		_postSelectionListener = new ISelectionListener() {
			@Override
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
				onSelectionChanged(part, selection);
			}
		};
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (_tourData == null || part == TourChartViewPart.this) {
					return;
				}

				if (eventId == TourEventId.SEGMENT_LAYER_CHANGED) {
					_tourChart.updateLayerSegment();

				} else if (eventId == TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED) {
					_tourChart.updateTourChart(true, true);

				} else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

					_tourData = null;
					_postSelectionProvider.clearSelection();

					updateChart();

				} else if (eventId == TourEventId.TOUR_CHANGED && eventData instanceof TourEvent) {

					final TourData tourData = UI.getTourPropertyTourData((TourEvent) eventData, _tourData);
					if (tourData != null) {

						_tourData = tourData;

						updateChart();
					}
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	@Override
	public void createPartControl(final Composite parent) {

		addPrefListener();
		addTourEventListener();
		addSelectionListener();
		addPartListeners();

		// set this part as selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(_postSelectionListener);
		getSite().getPage().removePartListener(_partListener);

		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	/**
	 * A post selection event was received by the selection listener
	 * 
	 * @param part
	 * @param selection
	 */
	protected abstract void onSelectionChanged(IWorkbenchPart part, ISelection selection);

	/**
	 * Update the chart after the tour data was modified
	 */
	protected abstract void updateChart();

}
