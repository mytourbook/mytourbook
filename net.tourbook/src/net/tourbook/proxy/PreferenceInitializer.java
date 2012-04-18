/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
 ********************************************************************************
 *
 * @author Meinhard Ritscher
 *
 ********************************************************************************

 This class provides the default settings for various preferences

 *******************************************************************************/

package net.tourbook.proxy;

import net.tourbook.application.TourbookPlugin;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		prefStore.setDefault(IPreferences.PROXY_METHOD, IPreferences.NO_PROXY);
		prefStore.setDefault(IPreferences.PROXY_SERVER_ADDRESS, ""); //$NON-NLS-1$
		prefStore.setDefault(IPreferences.PROXY_SERVER_PORT, "8080"); //$NON-NLS-1$
		prefStore.setDefault(IPreferences.PROXY_USER, ""); //$NON-NLS-1$
		prefStore.setDefault(IPreferences.PROXY_PWD, ""); //$NON-NLS-1$
		prefStore.setDefault(IPreferences.SOCKS_PROXY_SERVER_ADDRESS, ""); //$NON-NLS-1$
		prefStore.setDefault(IPreferences.SOCKS_PROXY_SERVER_PORT, "8080"); //$NON-NLS-1$
	}
}
