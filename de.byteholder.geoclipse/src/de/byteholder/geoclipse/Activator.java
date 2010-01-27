/* *****************************************************************************
 *  Copyright (C) 2008 Michael Kanis and others
 *  
 *  This file is part of Geoclipse.
 *
 *  Geoclipse is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Geoclipse is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Geoclipse.  If not, see <http://www.gnu.org/licenses/>. 
 *******************************************************************************/

package de.byteholder.geoclipse;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.osgi.framework.internal.core.AbstractBundle;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

/**
 * The activator class controls the plug-in life cycle
 */
@SuppressWarnings("restriction")
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String	PLUGIN_ID		= "de.byteholder.geoclipse";	//$NON-NLS-1$

	// The shared instance
	private static Activator	plugin;

	private Version				fVersion;

	public static final String	IMG_KEY_ANCHOR	= "anchor";					//$NON-NLS-1$
	public static final String	IMG_KEY_CAR		= "car";						//$NON-NLS-1$
	public static final String	IMG_KEY_CART	= "cart";						//$NON-NLS-1$
	public static final String	IMG_KEY_FLAG	= "flag";						//$NON-NLS-1$
	public static final String	IMG_KEY_HOUSE	= "house";						//$NON-NLS-1$
	public static final String	IMG_KEY_SOCCER	= "soccer";					//$NON-NLS-1$
	public static final String	IMG_KEY_STAR	= "star";						//$NON-NLS-1$

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for images in the plug-in icons path.
	 * 
	 * @param path
	 *            relative path to the <i>icons/</i> directory
	 * @return Returns the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(final String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, "icons/" + path); //$NON-NLS-1$
	}

	/**
	 * The constructor
	 */
	public Activator() {
		plugin = this;
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
		return fVersion;
	}

	@Override
	public void start(final BundleContext context) throws Exception {

		// get bundle version
		final Bundle bundle = context.getBundle();
		if (bundle instanceof AbstractBundle) {
			final AbstractBundle abstractBundle = (AbstractBundle) bundle;
			fVersion = abstractBundle.getVersion();
		}

		final ImageRegistry imageRegistry = getImageRegistry();

		imageRegistry.put(IMG_KEY_ANCHOR, getImageDescriptor(Messages.Image_POI_Anchor));
		imageRegistry.put(IMG_KEY_CAR, getImageDescriptor(Messages.Image_POI_Car));
		imageRegistry.put(IMG_KEY_CART, getImageDescriptor(Messages.Image_POI_Cart));
		imageRegistry.put(IMG_KEY_FLAG, getImageDescriptor(Messages.Image_POI_Flag));
		imageRegistry.put(IMG_KEY_HOUSE, getImageDescriptor(Messages.Image_POI_House));
		imageRegistry.put(IMG_KEY_SOCCER, getImageDescriptor(Messages.Image_POI_Soccer));
		imageRegistry.put(IMG_KEY_STAR, getImageDescriptor(Messages.Image_POI_Star));

		super.start(context);
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}
}
