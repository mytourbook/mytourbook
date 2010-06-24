/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package de.byteholder.geoclipse.tileinfo;

import net.tourbook.application.TourbookPlugin;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import de.byteholder.geoclipse.preferences.IMappingPreferences;

public class TrimPropertyTester extends PropertyTester {

	private static IPreferenceStore	_prefStore;

	// set default-default value
	private static boolean			_isShowTileInfo	= true;
 
	public TrimPropertyTester() {

		if (_prefStore == null) {

			_prefStore = TourbookPlugin.getDefault().getPreferenceStore();

			// get current value
			if (_prefStore.contains(IMappingPreferences.SHOW_MAP_TILE_INFO)) {
				_isShowTileInfo = _prefStore.getBoolean(IMappingPreferences.SHOW_MAP_TILE_INFO);
			}

			_prefStore.addPropertyChangeListener(new IPropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent event) {
					if (event.getProperty().equals(IMappingPreferences.SHOW_MAP_TILE_INFO)) {
						final Object isShowInfo = event.getNewValue();
						if (isShowInfo instanceof Boolean) {
							_isShowTileInfo = (Boolean) isShowInfo;
						} else {
							_isShowTileInfo = true;
						}
					}
				}
			});
		}
	}

	public boolean test(final Object receiver, final String property, final Object[] args, final Object expectedValue) {
		return _isShowTileInfo;
	}

}
