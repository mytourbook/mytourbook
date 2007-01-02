/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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
package net.tourbook.plugin;

import java.util.ArrayList;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class TourbookPlugin extends AbstractUIPlugin {

	public static final String		PLUGIN_ID						= "net.tourbook";

	public static final String		EXT_POINT_STATISTIC_YEAR		= "statisticYear";
	public static final String		EXT_POINT_DEVICE_DATA_READER	= "deviceDataReader";

	// The shared instance.
	private static TourbookPlugin	plugin;

	// Resource bundle.
	private ResourceBundle			resourceBundle;

	/**
	 * active person (selected in the combobox), set to <code>null</code> when
	 * 'All People' are selected
	 */
	private TourPerson				fCurrentPerson;

	private TourType				fActiveTourType;
	private ArrayList<TourType>		fTourTypes;

	/**
	 * The constructor.
	 */
	public TourbookPlugin() {
		super();
		plugin = this;
	}

	/**
	 * Returns the shared instance.
	 */
	public static TourbookPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for images in the plug-in path.
	 * 
	 * @param path
	 *        the path
	 * @return the axisImage descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(TourbookPlugin.PLUGIN_ID, "icons/" + path);
	}

	/**
	 * Returns the string from the plugin's resource bundle, or 'key' if not
	 * found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = TourbookPlugin.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}

	public TourPerson getActivePerson() {
		return fCurrentPerson;
	}

	public TourType getActiveTourType() {
		return fActiveTourType;
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		try {
			if (resourceBundle == null)
				resourceBundle = ResourceBundle
						.getBundle("net.tourbook.data.TourbookPluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
		return resourceBundle;
	}

	public ArrayList<TourType> getTourTypes() {
		return fTourTypes;
	}

	public TourType[] getTourTypesArray() {
		return fTourTypes.toArray(new TourType[fTourTypes.size()]);
	}

	public void setActivePerson(TourPerson currentPerson) {
		fCurrentPerson = currentPerson;
	}

	public void setActiveTourType(TourType tourType) {
		fActiveTourType = tourType;
	}

	public void setTourTypes(ArrayList<TourType> tourTypes) {
		fTourTypes = tourTypes;
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
		resourceBundle = null;

		// NetworkServerControl server = new NetworkServerControl();
		// server.shutdown();
	}

}
