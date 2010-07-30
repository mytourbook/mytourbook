/*******************************************************************************
 * Copyright (C) 2005, 2010 Wolfgang Schramm and Contributors
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package de.byteholder.geoclipse;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;

import de.byteholder.geoclipse.map.Map;
import de.byteholder.geoclipse.map.MapPainter;
import de.byteholder.geoclipse.mapprovider.MPPlugin;

public class GeoclipseExtensions {

	private static GeoclipseExtensions	_instance;

	private ArrayList<MPPlugin>			_tileFactories;

	private GeoclipseExtensions() {}

	public static GeoclipseExtensions getInstance() {

		if (_instance == null) {
			_instance = new GeoclipseExtensions();
		}

		return _instance;
	}

	public static void registerOverlays(final Map map) {

		final IExtensionRegistry registry = RegistryFactory.getRegistry();
		final IExtensionPoint point = registry.getExtensionPoint("de.byteholder.geoclipse.mapOverlay"); //$NON-NLS-1$
		final IExtension[] extensions = point.getExtensions();

		for (final IExtension extension : extensions) {
			final IConfigurationElement[] elements = extension.getConfigurationElements();

			final IConfigurationElement element = elements[elements.length - 1];

			Object o = null;
			try {
				o = element.createExecutableExtension("class"); //$NON-NLS-1$
			} catch (final CoreException e) {
				e.printStackTrace();
			}

			if (o != null && o instanceof MapPainter) {
				map.addOverlayPainter((MapPainter) o);
			}
		}
	}

	/**
	 * @return Returns a list with all available map/tile factories
	 */
	public List<MPPlugin> readFactories() {

		if (_tileFactories != null) {
			return _tileFactories;
		}

		final IExtensionRegistry registry = RegistryFactory.getRegistry();
		final IExtensionPoint point = registry.getExtensionPoint("de.byteholder.geoclipse.tilefactory"); //$NON-NLS-1$
		final IExtension[] extensions = point.getExtensions();

		_tileFactories = new ArrayList<MPPlugin>();

		for (final IExtension extension : extensions) {
			final IConfigurationElement[] elements = extension.getConfigurationElements();

			final IConfigurationElement element = elements[elements.length - 1];

			Object o = null;
			try {
				o = element.createExecutableExtension("class"); //$NON-NLS-1$
			} catch (final CoreException e) {
				e.printStackTrace();
			}

			if (o != null && o instanceof MPPlugin) {
				_tileFactories.add((MPPlugin) o);
			}
		}

		return _tileFactories;
	}
}
