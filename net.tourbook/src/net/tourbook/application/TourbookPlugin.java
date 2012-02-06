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
 *******************************************************************************/
package net.tourbook.application;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import net.tourbook.data.TourPerson;
import net.tourbook.database.TourDatabase;
import net.tourbook.photo.manager.PhotoImageCache;
import net.tourbook.ui.TourTypeFilter;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.framework.internal.core.AbstractBundle;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

/**
 * The main plugin class to be used in the desktop.
 */
@SuppressWarnings("restriction")
public class TourbookPlugin extends AbstractUIPlugin {

	public static final String				PLUGIN_ID								= "net.tourbook";				//$NON-NLS-1$

	public static final String				EXT_POINT_STATISTIC_YEAR				= "statisticYear";				//$NON-NLS-1$
	public static final String				EXT_POINT_EXPORT_TOUR					= "exportTour";				//$NON-NLS-1$
	public static final String				EXT_POINT_PRINT_TOUR					= "printTour";					//$NON-NLS-1$
	public static final String				EXT_POINT_DEVICE_DATA_READER			= "deviceDataReader";			//$NON-NLS-1$
	public static final String				EXT_POINT_EXTERNAL_DEVICE_DATA_READER	= "externalDeviceDataReader";	//$NON-NLS-1$

	// The shared instance.
	private static TourbookPlugin			_instance;

	private static ResourceBundle			_resourceBundle;

	/**
	 * active person (selected in the combobox), is set to <code>null</code> when 'All People' are
	 * selected
	 */
	private static TourPerson				_activePerson;

	private static TourTypeFilter			_activeTourTypeFilter;

	private static MyTourbookSplashHandler	_splashHandler;

	private static BundleContext			_bundleContext;

	private Version							_version;

	/**
	 * The constructor.
	 */
	public TourbookPlugin() {}

	/**
	 * @return Returns the active selected person or <code>null</code> when all people are selected
	 */
	public static TourPerson getActivePerson() {
		return _activePerson;
	}

	/**
	 * @return Returns {@link TourTypeFilter} which is currently selected in the UI
	 */
	public static TourTypeFilter getActiveTourTypeFilter() {
		return _activeTourTypeFilter;
	}

	public static BundleContext getBundleContext() {
		return _bundleContext;
	}

	/**
	 * Returns the shared instance.
	 */
	public static TourbookPlugin getDefault() {
		return _instance;
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
	 * Returns the plugin's resource bundle,
	 */
	private static ResourceBundle getResourceBundle() {
		try {
			if (_resourceBundle == null) {
				_resourceBundle = ResourceBundle.getBundle("net.tourbook.data.TourbookPluginResources"); //$NON-NLS-1$
			}
		} catch (final MissingResourceException x) {
			_resourceBundle = null;
		}
		return _resourceBundle;
	}

	/**
	 * Returns the string from the plugin's resource bundle, or 'key' if not found.
	 */
	public static String getResourceString(final String key) {

		final ResourceBundle bundle = getResourceBundle();

		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (final MissingResourceException e) {
			return key;
		}
	}

	public static MyTourbookSplashHandler getSplashHandler() {
		return _splashHandler;
	}

	public static void setActivePerson(final TourPerson currentPerson) {
		_activePerson = currentPerson;
	}

	public static void setActiveTourTypeFilter(final TourTypeFilter tourTypeFilter) {
		_activeTourTypeFilter = tourTypeFilter;
		TourDatabase.updateActiveTourTypeList(tourTypeFilter);
	}

	public static void setSplashHandler(final MyTourbookSplashHandler splashHandler) {
		_splashHandler = splashHandler;
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

	public Version getVersion() {
		return _version;
	}

	public void log(final String message, final Throwable exception) {
		getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, 0, message, exception));
	}

	@SuppressWarnings("restriction")
	@Override
	public void start(final BundleContext context) throws Exception {

		super.start(context);

		_instance = this;
		_bundleContext = context;

		final Bundle bundle = context.getBundle();
		if (bundle instanceof AbstractBundle) {
			final AbstractBundle abstractBundle = (AbstractBundle) bundle;
			_version = abstractBundle.getVersion();
		}
	}

	@Override
	public void stop(final BundleContext context) throws Exception {

		_instance = null;
		_bundleContext = null;

		PhotoImageCache.getInstance().dispose();

		super.stop(context);
	}

}
