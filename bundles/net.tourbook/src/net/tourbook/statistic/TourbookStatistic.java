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

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;

/**
 * Plugin interface for statistics in MyTourbook
 */
public abstract class TourbookStatistic {

	public String		statisticId;

	public String		visibleName;

	private Composite	_container;

	private boolean		_isDataDirty;

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
	 * Disposes of the statistic
	 */
	public void dispose() {
		if (_container != null && _container.isDisposed() == false) {

			_container.dispose();

			// null is checked outside of this class
			_container = null;
		}
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TourbookStatistic)) {
			return false;
		}
		final TourbookStatistic other = (TourbookStatistic) obj;
		if (statisticId == null) {
			if (other.statisticId != null) {
				return false;
			}
		} else if (!statisticId.equals(other.statisticId)) {
			return false;
		}
		return true;
	}

	public Composite getControl() {
		return _container;
	}

	/**
	 * @return When a tour can be selected in the statistic, this will return the tour Id of the
	 *         selected tour or <code>null</code> otherwise.
	 */
	public Long getSelectedTour() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((statisticId == null) ? 0 : statisticId.hashCode());
		return result;
	}

	/**
	 * @return Returns the status if the tour data for the statistic is dirty and resets the status
	 *         to <code>false</code>
	 */
	protected boolean isDataDirtyWithReset() {

		final boolean isDataDirty = _isDataDirty;
		_isDataDirty = false;

		return isDataDirty;
	}

	/**
	 * reset the selection in the statistic chart
	 */
	public abstract void resetSelection();

	/**
	 * Restores the state from a memento (e.g. select previous selection), default does nothing
	 * 
	 * @param state
	 */
	public abstract void restoreState(final IDialogSettings state);

	/**
	 * Restore state after the controls is created.
	 * 
	 * @param state
	 */
	public void restoreStateEarly(final IDialogSettings state) {
		// do nothing
	}

	/**
	 * Saves the state of the statistic into the memento, default does nothing
	 * 
	 * @param viewState
	 */
	public abstract void saveState(final IDialogSettings viewState);

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

	/**
	 * Bar vertical order was in the UI.
	 * 
	 * @param selectedIndex
	 *            Combobox selection index
	 */
	public void setBarVerticalOrder(final int selectedIndex) {
		// do nothing
	}

	public void setContainer(final Composite container) {
		_container = container;
	}

	/**
	 * Set statistic data dirty that they must be reloaded when the chart is displayed the next
	 * time.
	 */
	public void setDataDirty() {
		_isDataDirty = true;
	}

	/**
	 * Set the state if the scale for the chart is synched for different data (e.g. years)
	 * 
	 * @param isEnabled
	 *            <code>true</code> when the synch is enabled, <code>false</code> when it's disabled
	 */
	public abstract void setSynchScale(boolean isEnabled);

	/**
	 * This method is called before the statistic control will be displayed. When the toolbar
	 * manager is used, this method should put the actions into the toolbar manager
	 * 
	 * @param refreshToolbar
	 *            <code>true</code> will refresh the toolbar
	 */
	public abstract void updateToolBar(boolean refreshToolbar);

}
