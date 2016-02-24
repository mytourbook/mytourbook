/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.ui.model.ElementUtils;
import org.eclipse.equinox.internal.p2.ui.model.MetadataRepositoryElement;
import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceConstants;
import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceInitializer;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.ProfileScope;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.internal.misc.StatusUtil;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Activator class for the p2 UI.
 */
@SuppressWarnings("restriction")
public class P2_Activator extends AbstractUIPlugin {

	public static final String			PLUGIN_ID			= "org.eclipse.equinox.p2.ui.sdk";												//$NON-NLS-1$

	private static final String			UPDATE_SITE_NAME	= "MyTourbook Update Site";													//$NON-NLS-1$

//	private static String				UPDATE_SITE			= "http://mytourbook.sourceforge.net/updates";	//$NON-NLS-1$
	private static String				UPDATE_SITE			= "http://mytourbook.sourceforge.net/TEST-updates"; //$NON-NLS-1$
//	private static String				UPDATE_SITE			= "file:/C:/DAT/MT/mytourbook/build/build.update-site.test/target/repository";	//$NON-NLS-1$

	private static P2_Activator	plugin;
	private static BundleContext		context;

	private ScopedPreferenceStore		preferenceStore;
	private IPropertyChangeListener		preferenceListener;

	public P2_Activator() {
		// constructor
	}

	public static BundleContext getContext() {
		return context;
	}

	/**
	 * Returns the singleton plugin instance
	 * 
	 * @return the instance
	 */
	public static P2_Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in relative path
	 * 
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(final String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	static IStatus getNoSelfProfileStatus() {
		return new Status(IStatus.WARNING, PLUGIN_ID, ProvSDKMessages.ProvSDKUIActivator_NoSelfProfile);
	}

	/**
	 * After trying to set addRepository with p2.inf for 2 full days, I ended up to set it
	 * programmatically like others also did, found a solution here: <a href=
	 * "http://coopology.com/2012/08/eclipse-rcp-setting-p2-repositories-update-sites-programmatically-for-when-p2-inf-fails/"
	 * >http://coopology.com/2012/08/eclipse-rcp-setting-p2-repositories-update-sites-
	 * programmatically-for-when-p2-inf-fails/</a>
	 * 
	 * @throws InvocationTargetException
	 */
	public static void setUpdateSites() {

		try {

			final MetadataRepositoryElement repo = new MetadataRepositoryElement(null, new URI(UPDATE_SITE), true);
			repo.setNickname(UPDATE_SITE_NAME);

			ElementUtils.updateRepositoryUsingElements(
					ProvisioningUI.getDefaultUI(),
					new MetadataRepositoryElement[] { repo },
					null);

		} catch (final URISyntaxException e) {
			StatusUtil.handleStatus(e, 0);
		}
	}

	private IAgentLocation getAgentLocation() {
		final ServiceReference<?> ref = getContext().getServiceReference(IAgentLocation.SERVICE_NAME);
		if (ref == null) {
			return null;
		}
		final IAgentLocation location = (IAgentLocation) getContext().getService(ref);
		getContext().ungetService(ref);
		return location;
	}

	Policy getPolicy() {
		return getProvisioningUI().getPolicy();
	}

	private IPropertyChangeListener getPreferenceListener() {
		if (preferenceListener == null) {
			preferenceListener = new IPropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent event) {
					updateWithPreferences(getPolicy());
				}
			};
		}
		return preferenceListener;
	}

	/*
	 * Overridden to use a profile scoped preference store. (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#getPreferenceStore()
	 */
	@Override
	public IPreferenceStore getPreferenceStore() {
		// Create the preference store lazily.
		if (preferenceStore == null) {
			final IAgentLocation agentLocation = getAgentLocation();
			if (agentLocation == null) {
				return super.getPreferenceStore();
			}
			preferenceStore = new ScopedPreferenceStore(
					new ProfileScope(agentLocation, IProfileRegistry.SELF),
					PLUGIN_ID);
		}
		return preferenceStore;
	}

	public IProvisioningAgent getProvisioningAgent() {
		return getProvisioningUI().getSession().getProvisioningAgent();
	}

	public ProvisioningUI getProvisioningUI() {
		return ProvisioningUI.getDefaultUI();
	}

	public void savePreferences() {

		if (preferenceStore != null) {

			try {
				preferenceStore.save();
			} catch (final IOException e) {

				final Status status = new Status(
						IStatus.ERROR,
						PLUGIN_ID,
						0,
						ProvSDKMessages.ProvSDKUIActivator_ErrorSavingPrefs,
						e);

				StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.SHOW);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(final BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		plugin = this;
		P2_Activator.context = bundleContext;
		PreferenceInitializer.migratePreferences();
		getPreferenceStore().addPropertyChangeListener(getPreferenceListener());
	}

	@Override
	public void stop(final BundleContext bundleContext) throws Exception {
		plugin = null;
		getPreferenceStore().removePropertyChangeListener(preferenceListener);
		super.stop(bundleContext);
	}

	void updateWithPreferences(final Policy policy) {
		policy.setShowLatestVersionsOnly(getPreferenceStore().getBoolean(PreferenceConstants.PREF_SHOW_LATEST_VERSION));
	}
}
