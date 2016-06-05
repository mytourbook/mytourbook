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

import net.tourbook.application.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;

/**
 * Plugin interface for statistics in MyTourbook
 */
public abstract class TourbookStatistic {

	protected static final String	MEMENTO_SELECTED_TOUR_ID	= "statistic.selected.tourId";

	/** ID from plugin.xml */
	public String					statisticId;

	/** Name from plugin.xml */
	public String					visibleName;

	private boolean					_isDataDirty;

	private final IPreferenceStore	_prefStore					= TourbookPlugin.getPrefStore();

	private IPropertyChangeListener	_prefChangeListener;

	/*
	 * UI controls
	 */
	private Composite				_container;

	/**
	 * Add the pref listener which is called when the color was changed
	 */
	private void addPrefListener() {

		// create pref listener
		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				final String property = event.getProperty();

				// test if the color or statistic data have changed
				if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)
				//
						|| property.equals(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE)
						|| property.equals(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE)
						|| property.equals(ITourbookPreferences.GRAPH_GRID_IS_SHOW_HORIZONTAL_GRIDLINES)
						|| property.equals(ITourbookPreferences.GRAPH_GRID_IS_SHOW_VERTICAL_GRIDLINES)
						|| property.equals(ITourbookPreferences.GRAPH_IS_SEGMENT_ALTERNATE_COLOR)
						|| property.equals(ITourbookPreferences.GRAPH_SEGMENT_ALTERNATE_COLOR)
				//
				) {

					// update chart
					preferencesHasChanged();
				}
			}
		};

		// add pref listener
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	/**
	 * Create the statistic component
	 * 
	 * @param parent
	 * @param viewSite
	 * @param postSelectionProvider
	 */
	protected abstract void createStatisticUI(	Composite parent,
												IViewSite viewSite,
												IPostSelectionProvider postSelectionProvider);

	public void createUI(	final Composite parent,
							final IViewSite viewSite,
							final IPostSelectionProvider postSelectionProvider) {

		_container = parent;

		createStatisticUI(parent, viewSite, postSelectionProvider);

		addPrefListener();
	}

	/**
	 * Disposes of the statistic
	 */
	public void dispose() {

		if (_prefChangeListener != null) {

			_prefStore.removePropertyChangeListener(_prefChangeListener);
		}

		if (_container != null && _container.isDisposed() == false) {

			_container.dispose();

			// !!! null is checked outside of this class !!!
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

	/**
	 * @return When a tour can be selected in the statistic, this will return the tour Id of the
	 *         selected tour or <code>null</code> otherwise.
	 */
	public Long getSelectedTour() {
		return null;
	}

	public Composite getUIControl() {
		return _container;
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
	 * Preferences for the statistics has changed.
	 */
	public abstract void preferencesHasChanged();

	/**
	 * Restores the state from a memento (e.g. select previous selection), default does nothing
	 * 
	 * @param state
	 */
	public void restoreState(final IDialogSettings state) {
		// do nothing
	}

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
	public void saveState(final IDialogSettings viewState) {
		// do nothing
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

	@Override
	public String toString() {
		return "TourbookStatistic ["//
				+ ("statisticId=" + statisticId + ", ")//
				+ ("visibleName=" + visibleName)
				+ "]";
	}

	/**
	 * Update statistic with the data in the context.
	 * 
	 * @param statContext
	 */
	public abstract void updateStatistic(StatisticContext statContext);

	/**
	 * This method is called before the statistic control will be displayed. When the toolbar
	 * manager is used, this method should put the actions into the toolbar manager
	 */
	public abstract void updateToolBar();

}
