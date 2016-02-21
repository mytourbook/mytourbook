/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genuitec, LLC - added license support
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk;

import java.util.HashSet;
import java.util.StringTokenizer;

import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceConstants;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.ProfileScope;
import org.eclipse.equinox.p2.metadata.ILicense;
import org.eclipse.equinox.p2.ui.LicenseManager;
import org.osgi.service.prefs.Preferences;

/**
 * SimpleLicenseManager is a license manager that keeps track of IInstallableUnit licenses using
 * their UUID. The licenses ids are stored in the profile's preferences.
 * 
 * @since 3.6
 */
public class SimpleLicenseManager extends LicenseManager {
	java.util.Set<String>	accepted	= new HashSet<String>();
	String					profileId;

	public SimpleLicenseManager() {
		this(IProfileRegistry.SELF);
	}

	public SimpleLicenseManager(final String profileId) {
		super();
		this.profileId = profileId;
		initializeFromPreferences();
	}

	@Override
	public boolean accept(final ILicense license) {
		accepted.add(license.getUUID());
		updatePreferences();
		return true;
	}

	private Preferences getPreferences() {
		final IAgentLocation location = (IAgentLocation) ProvSDKUIActivator
				.getDefault()
				.getProvisioningAgent()
				.getService(IAgentLocation.SERVICE_NAME);
		return new ProfileScope(location, profileId).getNode(ProvSDKUIActivator.PLUGIN_ID);
	}

	@Override
	public boolean hasAcceptedLicenses() {
		return !accepted.isEmpty();
	}

	private void initializeFromPreferences() {
		final Preferences pref = getPreferences();
		if (pref != null) {
			final String digestList = pref.get(PreferenceConstants.PREF_LICENSE_DIGESTS, ""); //$NON-NLS-1$
			final StringTokenizer tokenizer = new StringTokenizer(digestList, ","); //$NON-NLS-1$
			while (tokenizer.hasMoreTokens()) {
				accepted.add(tokenizer.nextToken().trim());
			}
		}
	}

	@Override
	public boolean isAccepted(final ILicense license) {
		return accepted.contains(license.getUUID());
	}

	@Override
	public boolean reject(final ILicense license) {
		accepted.remove(license.getUUID());
		updatePreferences();
		return true;
	}

	private void updatePreferences() {
		final Preferences pref = getPreferences();
		final StringBuffer result = new StringBuffer();
		final Object[] indexedList = accepted.toArray();
		for (int i = 0; i < indexedList.length; i++) {
			if (i != 0) {
				result.append(","); //$NON-NLS-1$
			}
			result.append((String) indexedList[i]);
		}
		pref.put(PreferenceConstants.PREF_LICENSE_DIGESTS, result.toString());
	}
}
