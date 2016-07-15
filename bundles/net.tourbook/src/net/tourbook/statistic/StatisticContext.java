/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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

import net.tourbook.data.TourPerson;
import net.tourbook.ui.TourTypeFilter;

/**
 * Contains fields which are use to display the statistics.
 * <p>
 * Field prefixes which are not set in the constructor:
 * <p>
 * <code>in</code> field is set by the statistic container for the statistic implementation<br>
 * <code>out</code> field is set by the statistic implementation for the statistic container<br>
 */
public class StatisticContext {

	/**
	 * Person which is selected in the application.
	 */
	public TourPerson			appPerson;

	/**
	 * Tour type filter which is selected in the application.
	 */
	public TourTypeFilter		appTourTypeFilter;

	/**
	 * First (youngest) year which is selected in the statistic container and where the statistics
	 * start.
	 */
	public int					statFirstYear;

	/**
	 * Number of years which should be displayed in the statistic
	 */
	public int					statNumberOfYears;

	public boolean				isRefreshData				= false;

	/**
	 * Contains the state if bar reordering is supported by the statistic or not, default is
	 * <code>false</code>.
	 */
	public boolean				outIsBarReorderingSupported	= false;

	/**
	 * Is <code>true</code> when bar names in the statistic UI must be updated with data from
	 * {@link #outBarNames} and {@link #outVerticalBarIndex}.
	 */
	public boolean				outIsUpdateBarNames			= false;

	/**
	 * When stacked charts are displayed, the stacked parts can be resorted vertically.
	 * <p>
	 * This contains the names of the bars or <code>null</code> when bars are not available.
	 */
	public String[]				outBarNames					= null;

	/**
	 * Index which bar should be selected in the combo box.
	 */
	public int					outVerticalBarIndex;

	public StatisticView	eventManager;

	/**
	 * @param person
	 *            Active person or <code>null</code> when no person (all people) is selected
	 * @param activeTourTypeFilter
	 *            Tour type filter
	 * @param year
	 *            Year for the statistic, when multiple years are displayed, this is the youngest
	 *            year
	 * @param numberOfYears
	 *            Number of years which should be displayed in the statistic
	 */
	public StatisticContext(final TourPerson activePerson,
							final TourTypeFilter activeTourTypeFilter,
							final int selectedYear,
							final int numberOfYears) {

		this.appPerson = activePerson;
		this.appTourTypeFilter = activeTourTypeFilter;

		this.statFirstYear = selectedYear;
		this.statNumberOfYears = numberOfYears;
	};

	/**
	 * @return Returns <code>true</code> when events can be fired otherwise they cannot be fired
	 *         because the view is already processing events.
	 */
	public boolean canFireEvents() {

		return eventManager == null || eventManager.canFireEvents();
	}

}
