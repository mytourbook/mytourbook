/*******************************************************************************
 * Copyright (C) 2005, 2008 Wolfgang Schramm and Contributors
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

import de.byteholder.geoclipse.map.TileFactory;
import de.byteholder.geoclipse.swt.Map;
import de.byteholder.geoclipse.swt.MapPainter;
import de.byteholder.gpx.GeoPosition;

public class GeoclipseExtensions {

	private static GeoclipseExtensions	fInstance;

	// the position to start with
	private GeoPosition					startPosition	= new GeoPosition(0, 0);

	private int							startZoom		= 0;

	private ArrayList<TileFactory>		fTileFactories;

	public static GeoclipseExtensions getInstance() {

		if (fInstance == null) {
			fInstance = new GeoclipseExtensions();
		}

		return fInstance;
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

	private GeoclipseExtensions() {}

//	/**
//	 * Initializes the map and sets the first available map provider as default
//	 * 
//	 * @param map
//	 * @return Returns a list with all registered map provider plugins
//	 */
//	public List<TileFactory> readExtensions(final Map map) {
//
//		final List<TileFactory> factories = readFactories();
//
//		final TileFactory tf = (factories != null && factories.size() > 0) ? factories.get(0) : null;
//
//		// create the tile factory
//		if (tf != null) {
//			map.setTileFactory(tf);
//			map.setGeoCenterPosition(startPosition);
//			map.setZoom(startZoom);
//		}
//
//		registerOverlays(map);
//
//		return factories;
//	}

	public TileFactory findTileFactory(final String className) {

		final List<TileFactory> factories = readFactories();
		for (final TileFactory factory : factories) {
			if (factory.getClass().getName().equals(className)) {
				return factory;
			}
		}

		return null;
	}

	/**
	 * @return Returns a list with all available map/tile factories
	 */
	public List<TileFactory> readFactories() {

		if (fTileFactories != null) {
			return fTileFactories;
		}

		final IExtensionRegistry registry = RegistryFactory.getRegistry();
		final IExtensionPoint point = registry.getExtensionPoint("de.byteholder.geoclipse.tilefactory"); //$NON-NLS-1$
		final IExtension[] extensions = point.getExtensions();

		fTileFactories = new ArrayList<TileFactory>();

		for (final IExtension extension : extensions) {
			final IConfigurationElement[] elements = extension.getConfigurationElements();

			final IConfigurationElement element = elements[elements.length - 1];

			Object o = null;
			try {
				o = element.createExecutableExtension("class"); //$NON-NLS-1$
			} catch (final CoreException e) {
				e.printStackTrace();
			}

			if (o != null && o instanceof TileFactory) {
				fTileFactories.add((TileFactory) o);
			}
		}

//		/*
//		 * set the base url from the pref store, the base url can be overwritten in the pref page
//		 */
//		final IPreferenceStore prefStore = Activator.getDefault().getPreferenceStore();
//		for (final TileFactory tileFactory : fTileFactories) {
//
//			final String baseURL = prefStore.getString(tileFactory.getPrefStoreBaseUrlName());
//
//			if (baseURL.length() > 0) {
//				tileFactory.getInfo().setBaseURL(baseURL);
//			}
//		}

		return fTileFactories;
	}
}
