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
package net.tourbook.device.garmin;

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
	public static final String	PLUGIN_ID	= "net.tourbook.device.garmin"; //$NON-NLS-1$

	// The shared instance
	private static Activator	plugin;

	private Version				version;

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * The constructor
	 */
	public Activator() {
		plugin = this;
	}

	public Version getVersion() {
		return version;
	}

	@Override
	public void start(final BundleContext context) throws Exception {
		final Bundle bundle = context.getBundle();
		if (bundle instanceof AbstractBundle) {
			final AbstractBundle abstractBundle = (AbstractBundle) bundle;
			version = abstractBundle.getVersion();
		}
		super.start(context);
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}
}
