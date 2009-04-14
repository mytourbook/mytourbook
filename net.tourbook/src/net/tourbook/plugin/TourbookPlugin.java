/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
 
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import net.tourbook.application.MyTourbookSplashHandler;
import net.tourbook.data.TourPerson;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.TourTypeFilter;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class TourbookPlugin extends AbstractUIPlugin {

	public static final String		PLUGIN_ID								= "net.tourbook";				//$NON-NLS-1$

	public static final String		EXT_POINT_STATISTIC_YEAR				= "statisticYear";				//$NON-NLS-1$
	public static final String		EXT_POINT_EXPORT_TOUR					= "exportTour";				//$NON-NLS-1$
	public static final String		EXT_POINT_DEVICE_DATA_READER			= "deviceDataReader";			//$NON-NLS-1$
	public static final String		EXT_POINT_EXTERNAL_DEVICE_DATA_READER	= "externalDeviceDataReader";	//$NON-NLS-1$

	// The shared instance.
	private static TourbookPlugin	plugin;

	// Resource bundle.
	private ResourceBundle			resourceBundle;

	/**
	 * active person (selected in the combobox), set to <code>null</code> when 'All People' are
	 * selected
	 */
	private TourPerson				fCurrentPerson;

	private TourTypeFilter			fActiveTourTypeFilter;

	private MyTourbookSplashHandler	fSplashHandler;

	private BundleContext			fContext;

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
	 *            the path
	 * @return the axisImage descriptor
	 */
	public static ImageDescriptor getImageDescriptor(final String path) {
		return imageDescriptorFromPlugin(TourbookPlugin.PLUGIN_ID, "icons/" + path); //$NON-NLS-1$
	}

	/**
	 * Returns the string from the plugin's resource bundle, or 'key' if not found.
	 */
	public static String getResourceString(final String key) {
		final ResourceBundle bundle = TourbookPlugin.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (final MissingResourceException e) {
			return key;
		}
	}

	/**
	 * The constructor.
	 */
	public TourbookPlugin() {
		super();
		plugin = this;
	}

	/**
	 * @return Returns the active selected person or <code>null</code> when no person is selected
	 */
	public TourPerson getActivePerson() {
		return fCurrentPerson;
	}

	public TourTypeFilter getActiveTourTypeFilter() {
		return fActiveTourTypeFilter;
	}

	public BundleContext getBundleContext() {
		return fContext;
	}

	/**
	 * @param sectionName
	 * @return Returns the dialog setting section for the sectionName, a section is always returned
	 *         even when it's empty
	 */
	public IDialogSettings getDialogSettingsSection(final String sectionName) {

		final IDialogSettings dialogSettings = getDialogSettings();
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
		} catch (final MissingResourceException x) {
			resourceBundle = null;
		}
		return resourceBundle;
	}

	public MyTourbookSplashHandler getSplashHandler() {
		return fSplashHandler;
	}

	public void setActivePerson(final TourPerson currentPerson) {
		fCurrentPerson = currentPerson;
	}

	public void setActiveTourTypeFilter(final TourTypeFilter tourTypeFilter) {
		fActiveTourTypeFilter = tourTypeFilter;
		TourDatabase.updateActiveTourTypeList(tourTypeFilter);
	}

//	/**
//	 * This method is called upon plug-in activation
//	 */
//	@Override
//	public void start(final BundleContext context) throws Exception {
//		super.start(context);
//	}
//
//	/**
//	 * This method is called when the plug-in is stopped
//	 */
//	@Override
//	public void stop(final BundleContext context) throws Exception {
//
//		super.stop(context);
//
//		plugin = null;
//		resourceBundle = null;
//
//		// NetworkServerControl server = new NetworkServerControl();
//		// server.shutdown();
//	}

	public void setSplashHandler(final MyTourbookSplashHandler splashHandler) {
		fSplashHandler = splashHandler;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		fContext = context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}
}
