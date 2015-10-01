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
package net.tourbook.ui.views.tourBook;

import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.tour.TourTypeFilterManager;
import net.tourbook.ui.TourTypeFilter;

import org.eclipse.jface.dialogs.IDialogSettings;

class CollateTourManager {

	private static final String				ID								= "net.tourbook.ui.views.tourBook.CollateTourManager";	//$NON-NLS-1$

	private static final String				STATE_SELECTED_COLLATE_FILTER	= "STATE_SELECTED_COLLATE_FILTER";						//$NON-NLS-1$

	private final static IDialogSettings	_state							= TourbookPlugin.getState(ID);

	/**
	 * @return Collate filters contain a subset of all tour type filters, the system filter are
	 *         removed.
	 */
	static ArrayList<TourTypeFilter> getAllCollateFilters() {

		final ArrayList<TourTypeFilter> tourTypeFilters = TourTypeFilterManager.getTourTypeFilters();

		final ArrayList<TourTypeFilter> removedTourTypeFilters = new ArrayList<>();

		// remove not appropriate filters, system filters
		for (final TourTypeFilter tourTypeFilter : tourTypeFilters) {
			if (tourTypeFilter.getFilterType() == TourTypeFilter.FILTER_TYPE_SYSTEM) {
				removedTourTypeFilters.add(tourTypeFilter);
			}
		}

		@SuppressWarnings("unchecked")
		final ArrayList<TourTypeFilter> collateFilter = (ArrayList<TourTypeFilter>) tourTypeFilters.clone();

		collateFilter.removeAll(removedTourTypeFilters);

		return collateFilter;
	}

	/**
	 * @return Returns the selected tour type filter for collated tours or <code>null</code> when
	 *         nothing is selected.
	 */
	static TourTypeFilter getSelectedCollateFilter() {

		final ArrayList<TourTypeFilter> collateTypes = getAllCollateFilters();

		final String lastCollateTypeName = _state.get(STATE_SELECTED_COLLATE_FILTER);

		if (lastCollateTypeName != null) {

			// find the name in the filter list

			for (final TourTypeFilter tourTypeFilter : collateTypes) {
				if (tourTypeFilter.getFilterName().equals(lastCollateTypeName)) {
					return tourTypeFilter;
				}
			}
		}

		return null;
	}

	static void setSelectedCollateFilter(final TourTypeFilter collateFilter) {

		_state.put(STATE_SELECTED_COLLATE_FILTER, collateFilter.getFilterName());
	}
}
