/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.prefs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKMessages;
import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKUIActivator;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.ProfileScope;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.ServiceReference;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * @since 3.4
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	public static void migratePreferences() {
		Preferences pref = new ProfileScope(getDefaultAgentLocation(), IProfileRegistry.SELF).getNode(ProvSDKUIActivator.PLUGIN_ID);
		try {
			if (pref.keys().length == 0) {
				// migrate preferences from instance scope to profile scope
				Preferences oldPref = new InstanceScope().getNode(ProvSDKUIActivator.PLUGIN_ID);
				// don't migrate everything.  Some of the preferences moved to
				// another bundle.
				pref.put(PreferenceConstants.PREF_OPEN_WIZARD_ON_ERROR_PLAN, oldPref.get(PreferenceConstants.PREF_OPEN_WIZARD_ON_ERROR_PLAN, MessageDialogWithToggle.PROMPT));
				pref.putBoolean(PreferenceConstants.PREF_SHOW_LATEST_VERSION, oldPref.getBoolean(PreferenceConstants.PREF_SHOW_LATEST_VERSION, true));
				pref.flush();
			}
		} catch (BackingStoreException e) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR, ProvSDKUIActivator.PLUGIN_ID, 0, ProvSDKMessages.PreferenceInitializer_Error, e), StatusManager.LOG);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		Preferences node = new DefaultScope().getNode("org.eclipse.equinox.p2.ui.sdk"); //$NON-NLS-1$
		// default values
		node.putBoolean(PreferenceConstants.PREF_SHOW_LATEST_VERSION, true);
		node.put(PreferenceConstants.PREF_OPEN_WIZARD_ON_ERROR_PLAN, MessageDialogWithToggle.PROMPT);
	}

	private static IAgentLocation getDefaultAgentLocation() {
		ServiceReference<?> reference = ProvSDKUIActivator.getContext().getServiceReference(IAgentLocation.SERVICE_NAME);
		if (reference == null)
			return null;
		IAgentLocation result = (IAgentLocation) ProvSDKUIActivator.getContext().getService(reference);
		ProvSDKUIActivator.getContext().ungetService(reference);
		return result;
	}
}
