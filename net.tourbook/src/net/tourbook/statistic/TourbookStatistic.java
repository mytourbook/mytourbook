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
package net.tourbook.statistic;

import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;

/**
 * Plugin interface for statistics in MyTourbook
 */
public abstract class TourbookStatistic {

	protected static final String	NEW_LINE	= "\n"; //$NON-NLS-1$

	public String					fStatisticId;

	public String					fVisibleName;

	private Composite				fContainer;

	private boolean					fIsDataDirty;

	/**
	 * Activates the actions in the statistic
	 * 
	 * @param partSite
	 */
	public abstract void activateActions(IWorkbenchPartSite partSite);

	public boolean canSelectDay() {
		return false;
	}

	public boolean canSelectMonth() {
		return false;
	}

	public boolean canSelectTour() {
		return false;
	}

	/**
	 * Create the statistic component
	 * 
	 * @param parent
	 * @param viewSite
	 * @param actionBars
	 * @param postSelectionProvider
	 */
	public abstract void createControl(	Composite parent,
										IViewSite viewSite,
										IPostSelectionProvider postSelectionProvider);

	/**
	 * Deactivates the actions in the statistic
	 * 
	 * @param partSite
	 */
	public abstract void deactivateActions(IWorkbenchPartSite partSite);

	/**
	 * Disposes of the statistic
	 */
	public void dispose() {}

	public Composite getControl() {
		return fContainer;
	}

	/**
	 * @return returns the status if the tour data for the statistic is dirty and resets the status
	 *         to <code>false</code>
	 */
	protected boolean isDataDirtyWithReset() {
		final boolean isDataDirty = fIsDataDirty;
		fIsDataDirty = false;
		return isDataDirty;
	}

	/**
	 * reset the selection in the statistic chart
	 */
	public abstract void resetSelection();

	/**
	 * Restores the state from a memento (e.g. select previous selection), default does nothing
	 * 
	 * @param memento
	 */
	public abstract void restoreState(final IMemento memento);

	/**
	 * Saves the state of the statistic into the memento, default does nothing
	 * 
	 * @param memento
	 */
	public abstract void saveState(final IMemento memento);

	/**
	 * Select the day in the statistic, this is used to visualize a selected tour in the statistic
	 * chart. Deselect the day when set to <code>null</code>. <code>date</code> is in the format
	 * milliseconds
	 * 
	 * @param date
	 *            contains the date value in milliseconds
	 * @return <code>true</code> when a day was selected
	 */
	public boolean selectDay(final Long date) {
		return false;
	}

	/**
	 * Select the month in the statistic, this is used to visualize a selected tour in the statistic
	 * chart. Deselect the month when set to <code>null</code>
	 * 
	 * @param date
	 *            contains the date value in milliseconds
	 * @return <code>true</code> when a month was selected
	 */
	public boolean selectMonth(final Long date) {
		return false;
	}

	/**
	 * Select a tour in the statistic to visualize a selected tour.
	 * <p>
	 * This can be overwritten to select a tour in the statistic
	 * 
	 * @param tourId
	 * @return <code>true</code> when a tour was selected
	 */
	public boolean selectTour(final Long tourId) {
		return false;
	}

	public void setContainer(final Composite container) {
		fContainer = container;
	}

	/**
	 * sets the statistic data dirty, they must be refreshed when the chart is displayed the next
	 * time
	 */
	public void setDataDirty() {
		fIsDataDirty = true;
	}

	/**
	 * Set the state if the scale for the chart is synched for different data (e.g. years)
	 * 
	 * @param isEnabled
	 *            <code>true</code> when the synch is enabled, <code>false</code> when it's disabled
	 */
	public abstract void setSynchScale(boolean isEnabled);

	/**
	 * this method is called before the statistic control will be displayed. When the toolbar
	 * manager is used, this method should put the actions into the toolbar manager
	 * 
	 * @param refreshToolbar
	 *            <code>true</code> will refresh the toolbar
	 */
	public abstract void updateToolBar(boolean refreshToolbar);

}
