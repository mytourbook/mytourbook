/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.map3.layer.tourtrack;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.map3.view.TVIMap3Root;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;

public class TourTrackConfigManager {

	private static final Bundle	_bundle							= TourbookPlugin.getDefault().getBundle();
	private static final IPath	_stateLocation					= Platform.getStateLocation(_bundle);

	private static final String	MAP3_LAYER_STRUCTURE_FILE_NAME	= "map3-layers.xml";						//$NON-NLS-1$

	/**
	 * This version number is incremented, when structural changes (e.g. new category) are done.
	 * When this happens, the <b>default</b> structure is created.
	 */
	private static final int	MAP3_LAYER_STRUCTURE_VERSION	= 1;

	private static File getLayerXmlFile() {

		final File layerFile = _stateLocation.append(MAP3_LAYER_STRUCTURE_FILE_NAME).toFile();

		return layerFile;
	}

	/**
	 * Read/Create map layers with it's state from a xml file
	 * 
	 * @return
	 */
	private static TVIMap3Root parseLayerXml() {

		final TVIMap3Root tviRoot = new TVIMap3Root();

		InputStreamReader reader = null;

		try {

			XMLMemento xmlRoot = null;

			// try to get layer structure from saved xml file
			final File layerFile = getLayerXmlFile();
			final String absoluteLayerPath = layerFile.getAbsolutePath();

			final File inputFile = new File(absoluteLayerPath);
			if (inputFile.exists()) {

				try {

					reader = new InputStreamReader(new FileInputStream(inputFile), UI.UTF_8);
					xmlRoot = XMLMemento.createReadRoot(reader);

				} catch (final Exception e) {
					// ignore
				}
			}

			Integer layerVersion = null;

			// get current layer version, when available
			if (xmlRoot != null) {
				layerVersion = xmlRoot.getInteger(ATTR_MAP3_LAYER_VERSION);
			}

			if (xmlRoot == null || layerVersion == null || layerVersion < MAP3_LAYER_STRUCTURE_VERSION) {

				// create default layer tree
				xmlRoot = createLayerXml_0_DefaultLayer();
			}

			parseLayerXml_10_Children(xmlRoot, tviRoot);

			_uiVisibleLayers = _uiVisibleLayersFromXml.toArray();
			_uiExpandedCategories = _uiExpandedCategoriesFromXml.toArray();

		} catch (final Exception e) {
			StatusUtil.log(e);
		} finally {

			if (reader != null) {
				try {
					reader.close();
				} catch (final IOException e) {
					StatusUtil.log(e);
				}
			}
		}

		return tviRoot;
	}
}
