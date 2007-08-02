/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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

import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;

public abstract class TourbookStatistic {

	public String		fStatisticId;

	public String		fVisibleName;

	private Composite	fContainer;

	private boolean		fIsRefreshData;

	/**
	 * Create the statistic component
	 * 
	 * @param parent
	 * @param actionBars
	 * @param postSelectionProvider
	 */
	public abstract void createControl(	Composite parent,
										IActionBars actionBars,
										IPostSelectionProvider postSelectionProvider);

	public Composite getControl() {
		return fContainer;
	}

	/**
	 * Returns the refresh status and resets the status to false
	 * 
	 * @return current reset status
	 */
	protected boolean isRefreshDataWithReset() {
		boolean isRefresh = fIsRefreshData;
		fIsRefreshData = false;
		return isRefresh;
	}

	/**
	 * Select the day in the statistic, this is used to visualize a selected tour in the statistic
	 * chart. Deselect the day when set to <code>null</code>. <code>date</code> is in the
	 * format milliseconds
	 * 
	 * @param date
	 *        contains the date value in milliseconds
	 * @return <code>true</code> when a day was selected
	 */
	public boolean selectDay(Long date) {
		return false;
	}

	/**
	 * Select the month in the statistic, this is used to visualize a selected tour in the statistic
	 * chart. Deselect the month when set to <code>null</code>
	 * 
	 * @param date
	 *        contains the date value in milliseconds
	 * @return <code>true</code> when a month was selected
	 */
	public boolean selectMonth(Long date) {
		return false;
	}

	/**
	 * Select the tour in the statistic, this is used to visualize a selected tour in the statistic
	 * chart.
	 * 
	 * @param tourId
	 * @return <code>true</code> when a tour was selected
	 */
	public boolean selectTour(long tourId) {
		return false;
	}

	public void setContainer(Composite container) {
		fContainer = container;
	}

	/**
	 * @param isRefresh
	 *        when set to <code>true</code>, the data must be refreshed when the chart is
	 *        displayed the next time, set to <code>false</code> when the data has been refreshed
	 */
	public void setRefreshData(boolean isRefresh) {
		fIsRefreshData = isRefresh;
	}

	/**
	 * Set the state if the scale for the chart is synched for different data (e.g. years)
	 * 
	 * @param isEnabled
	 *        <code>true</code> when the synch is enabled, <code>false</code> when it's disabled
	 */
	public abstract void setSynchScale(boolean isEnabled);

	/**
	 * this method is called before the statistic control will be displayed. When the toolbar
	 * manager is used, this method should put the actions into the toolbar manager
	 * 
	 * @param refreshToolbar
	 *        <code>true</code> will refresh the toolbar
	 */
	public void updateToolBar(boolean refreshToolbar) {}

	/**
	 * reset the selection in the statistic chart
	 */
	public abstract void resetSelection();

}
