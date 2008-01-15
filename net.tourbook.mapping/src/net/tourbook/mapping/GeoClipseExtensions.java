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

package net.tourbook.mapping;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import de.byteholder.geoclipse.map.EmptyTileFactory;
import de.byteholder.geoclipse.map.TileFactory;
import de.byteholder.geoclipse.swt.Map;
import de.byteholder.geoclipse.swt.MapPainter;
import de.byteholder.gpx.GeoPosition;

public class GeoClipseExtensions {

	private static GeoClipseExtensions	fInstance;

	// the position to start with
	private GeoPosition					startPosition	= new GeoPosition(0, 0);

	private int							startZoom		= 0;

	private GeoClipseExtensions() {}

	public static GeoClipseExtensions getInstance() {

		if (fInstance == null) {
			fInstance = new GeoClipseExtensions();
		}

		return fInstance;
	}

	private Image getImage(Map map, String imagePath, String messageText) {

		int tileSize = map.getTileFactory().getTileSize();
		Image image = null;

		try {

			image = Activator.getImageDescriptor(imagePath).createImage(false);

		} catch (Exception e) {}

		if (image == null) {

			image = new Image(Display.getCurrent(), tileSize, tileSize);

			GC gc = new GC(image);
			{
				gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
				gc.drawString(messageText, 5, 5);
			}
			gc.dispose();
		}

		return image;
	}

	public void readMapExtensions(Map map) {

		// read all registered Extensions here
		// and TODO instantiate the one, the user configured to use
		IExtensionRegistry registry = RegistryFactory.getRegistry();
		IExtensionPoint point = registry.getExtensionPoint("de.byteholder.geoclipse.tilefactory");
		IExtension[] extensions = point.getExtensions();

		TileFactory tf = null;
		EmptyTileFactory etf = null;

		for (IExtension extension : extensions) {
			IConfigurationElement[] elements = extension.getConfigurationElements();

			IConfigurationElement element = elements[elements.length - 1];

			Object o = null;
			try {
				o = element.createExecutableExtension("class");
			} catch (CoreException e) {
				e.printStackTrace();
			}

			if (o != null && o instanceof TileFactory) {

				// Do not use EmptyTileFactory if there are others
				if (!(o instanceof EmptyTileFactory)) {
					tf = (TileFactory) o;
				} else {
					etf = (EmptyTileFactory) o;
				}
			}
		}

		if (tf == null) {
			tf = etf;
		}

		// create the tile factory
		if (tf != null) {

			map.setTileFactory(tf);
			map.setCenterPosition(startPosition);
			map.setZoom(startZoom);

			map.setFailedImage(getImage(map, "mapviewer/resources/failed.png", "Failed!"));
			map.setLoadingImage(getImage(map, "mapviewer/resources/loading.png", "Loading ..."));
		}

		registerOverlays(map);
	}

	private void registerOverlays(Map map) {

		IExtensionRegistry registry = RegistryFactory.getRegistry();
		IExtensionPoint point = registry.getExtensionPoint("de.byteholder.geoclipse.mapOverlay");
		IExtension[] extensions = point.getExtensions();

		for (IExtension extension : extensions) {
			IConfigurationElement[] elements = extension.getConfigurationElements();

			IConfigurationElement element = elements[elements.length - 1];

			Object o = null;
			try {
				o = element.createExecutableExtension("class");
			} catch (CoreException e) {
				e.printStackTrace();
			}

			if (o != null && o instanceof MapPainter) {
				map.addOverlayPainter((MapPainter) o);
			}
		}
	}
}
