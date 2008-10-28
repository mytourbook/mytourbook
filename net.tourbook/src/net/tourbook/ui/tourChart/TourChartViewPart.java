/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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

import net.tourbook.data.TourData;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourPropertyListener;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourProperties;
import net.tourbook.ui.UI;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

/**
 * Provides a skeleton for a view which displays a tour chart
 */
public abstract class TourChartViewPart extends ViewPart {

	protected TourChart					fTourChart;
	protected TourData					fTourData;
	protected TourChartConfiguration	fTourChartConfig;

	protected PostSelectionProvider		fPostSelectionProvider;

	private IPropertyChangeListener		fPrefChangeListener;
	private ITourPropertyListener		fTourPropertyListener;
	private ISelectionListener			fPostSelectionListener;

	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				/*
				 * set a new chart configuration when the preferences has changed
				 */
				if (property.equals(ITourbookPreferences.GRAPH_VISIBLE)
						|| property.equals(ITourbookPreferences.GRAPH_X_AXIS)
						|| property.equals(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME)) {

					fTourChartConfig = TourManager.createTourChartConfiguration();

					if (fTourChart != null) {
						fTourChart.updateTourChart(fTourData, fTourChartConfig, false);
					}

				} else if (property.equals(ITourbookPreferences.GRAPH_MOUSE_MODE)) {

					fTourChart.setMouseMode(event.getNewValue());
				}
			}
		};

		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		fPostSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
				onSelectionChanged(part, selection);
			}
		};
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void addTourPropertyListener() {

		fTourPropertyListener = new ITourPropertyListener() {
			public void propertyChanged(final IWorkbenchPart part, final int propertyId, final Object propertyData) {

				if (fTourData == null || part == TourChartViewPart.this) {
					return;
				}

				if (propertyId == TourManager.TOUR_PROPERTY_SEGMENT_LAYER_CHANGED) {
					fTourChart.updateSegmentLayer((Boolean) propertyData);

				} else if (propertyId == TourManager.TOUR_CHART_PROPERTY_IS_MODIFIED) {
					fTourChart.updateTourChart(true, true);

				} else if (propertyId == TourManager.TOUR_PROPERTIES_CHANGED && propertyData instanceof TourProperties) {

					final TourData tourData = UI.getTourPropertyTourData((TourProperties) propertyData, fTourData);
					if (tourData != null) {

						fTourData = tourData;

						updateChart();
					}
				}
			}
		};

		TourManager.getInstance().addPropertyListener(fTourPropertyListener);
	}

	@Override
	public void createPartControl(final Composite parent) {

		addPrefListener();
		addTourPropertyListener();
		addSelectionListener();

		// set this part as selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(fPostSelectionListener);

		TourManager.getInstance().removePropertyListener(fTourPropertyListener);

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);

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
