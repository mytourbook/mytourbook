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
package net.tourbook.statistics;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;

import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.statistic.IYearStatistic;
import net.tourbook.statistic.TourbookStatistic;

public abstract class YearStatistic extends TourbookStatistic implements IYearStatistic {

	private IPropertyChangeListener	fPrefChangeListener;

	/**
	 * call super.createControl to initialize the color change listener
	 * 
	 * @see net.tourbook.statistic.TourbookStatistic#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		addPrefListener(parent);
	}

	/**
	 * Add the pref listener which is called when the color was changed
	 * 
	 * @param container
	 */
	private void addPrefListener(Composite container) {

		// create pref listener
		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(Preferences.PropertyChangeEvent event) {
				final String property = event.getProperty();

				// test if the color or statistic data have changed
				if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)) {

					// update chart
					prefColorChanged();
				}
			}
		};

		// add pref listener
		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(
				fPrefChangeListener);

		// remove pre listener
		container.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				TourbookPlugin
						.getDefault()
						.getPluginPreferences()
						.removePropertyChangeListener(fPrefChangeListener);
			}
		});
	}
}
