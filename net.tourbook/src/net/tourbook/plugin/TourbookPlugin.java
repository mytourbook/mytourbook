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
package net.tourbook.plugin;

import java.util.ArrayList;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import net.tourbook.application.MyTourbookSplashHandler;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class TourbookPlugin extends AbstractUIPlugin {

	public static final String		PLUGIN_ID						= "net.tourbook";		//$NON-NLS-1$

	public static final String		EXT_POINT_STATISTIC_YEAR		= "statisticYear";		//$NON-NLS-1$
	public static final String		EXT_POINT_DEVICE_DATA_READER	= "deviceDataReader";	//$NON-NLS-1$

	// The shared instance.
	private static TourbookPlugin	plugin;

	// Resource bundle.
	private ResourceBundle			resourceBundle;

	/**
	 * active person (selected in the combobox), set to <code>null</code> when 'All People' are
	 * selected
	 */
	private TourPerson				fCurrentPerson;

	private TourType				fActiveTourType;
	private ArrayList<TourType>		fTourTypes;

	private MyTourbookSplashHandler	fSplashHandler;

	private ArrayList<TourType>		fDbTourTypes;

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
		return imageDescriptorFromPlugin(TourbookPlugin.PLUGIN_ID, "icons/" + path); //$NON-NLS-1$
	}

	/**
	 * Returns the string from the plugin's resource bundle, or 'key' if not found.
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
	 * @return Returns the tour types which are stored in the database
	 */
	public ArrayList<TourType> getDbTourTypes() {
		return fDbTourTypes;
	}

	/**
	 * @param sectionName
	 * @return Returns the dialog setting section for the sectionName
	 */
	public IDialogSettings getDialogSettingsSection(String sectionName) {
		IDialogSettings dialogSettings = getDialogSettings();
		IDialogSettings section = dialogSettings.getSection(sectionName);
		if (section == null) {
			section = dialogSettings.addNewSection(sectionName);
		}
		return section;
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		try {
			if (resourceBundle == null) {
				resourceBundle = ResourceBundle.getBundle("net.tourbook.data.TourbookPluginResources"); //$NON-NLS-1$
			}
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
		return resourceBundle;
	}

	public MyTourbookSplashHandler getSplashHandler() {
		return fSplashHandler;
	}

	/**
	 * @return Returns all tour types which are displayed in the tour type combobox
	 */
	public ArrayList<TourType> getAllTourTypes() {
		return fTourTypes;
	}

	/**
	 * @return Returns all tour types which are displayed in the tour type combobox
	 */
	public TourType[] getAllTourTypesArray() {
		return fTourTypes.toArray(new TourType[fTourTypes.size()]);
	}

	public void setActivePerson(TourPerson currentPerson) {
		fCurrentPerson = currentPerson;
	}

	public void setActiveTourType(TourType tourType) {
		fActiveTourType = tourType;
	}

	/**
	 * Set the tour types which are displayed in the tour type combobox
	 * 
	 * @param tourTypes
	 */
	public void setAllTourTypes(ArrayList<TourType> tourTypes) {
		fTourTypes = tourTypes;
	}

	/**
	 * Set the tour types which are stored in the database
	 * 
	 * @param dbTourTypes
	 */
	public void setDbTourTypes(ArrayList<TourType> dbTourTypes) {
		fDbTourTypes = dbTourTypes;
	}

	public void setSplashHandler(MyTourbookSplashHandler splashHandler) {
		fSplashHandler = splashHandler;
	}

	/**
	 * This method is called upon plug-in activation
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
		resourceBundle = null;

		// NetworkServerControl server = new NetworkServerControl();
		// server.shutdown();
	}
}
