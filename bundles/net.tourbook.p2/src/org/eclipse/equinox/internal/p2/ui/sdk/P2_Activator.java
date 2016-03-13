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

/*
 * Modified for MyTourbook
 */

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.model.ElementUtils;
import org.eclipse.equinox.internal.p2.ui.model.MetadataRepositoryElement;
import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceConstants;
import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceInitializer;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.ProfileScope;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Shell;
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

	public static final String					PLUGIN_ID				= "org.eclipse.equinox.p2.ui.sdk";											//$NON-NLS-1$

	private static final String					UPDATE_SITE_NAME		= "MyTourbook Update Site";												//$NON-NLS-1$

	private static String						UPDATE_SITE_PRODUCTION	= "http://mytourbook.sourceforge.net/updates";								//$NON-NLS-1$
	private static String						UPDATE_SITE_TEST		= "http://mytourbook.sourceforge.net/TEST-updates";						//$NON-NLS-1$
	private static String						UPDATE_SITE_TEST_LOCAL	= "file:/C:/DAT/MT/mytourbook/build/build.update-site/target/repository";	//$NON-NLS-1$

	private static MetadataRepositoryElement[]	DEFAULT_UPDATE_SITES;																				;

	static {

		try {

			/*
			 * http://mytourbook.sourceforge.net/updates
			 */
			final MetadataRepositoryElement repo_PRODUCTION = new MetadataRepositoryElement(null, //
					new URI(UPDATE_SITE_PRODUCTION),
					true);

			repo_PRODUCTION.setNickname(UPDATE_SITE_NAME);

			/*
			 * http://mytourbook.sourceforge.net/TEST-updates
			 */
			final MetadataRepositoryElement repo_TEST_Web = new MetadataRepositoryElement(null,//
					new URI(UPDATE_SITE_TEST),
					true);

			repo_TEST_Web.setNickname(UPDATE_SITE_TEST);

			/*
			 * file:/C:/DAT/MT/mytourbook/build/build.update-site/target/repository
			 */
			final MetadataRepositoryElement repo_TEST_Local = new MetadataRepositoryElement(null, //
					new URI(UPDATE_SITE_TEST_LOCAL),
					true);

			repo_TEST_Local.setNickname(UPDATE_SITE_TEST_LOCAL);

			DEFAULT_UPDATE_SITES = new MetadataRepositoryElement[] {

			//	PRODUCTION	PRODUCTION	PRODUCTION	PRODUCTION

			repo_PRODUCTION,

			//
			// DEBUG	DEBUG	DEBUG	DEBUG	DEBUG	DEBUG
			//

//			repo_TEST_Web,
//			repo_TEST_Local

			//
			};

		} catch (final URISyntaxException e) {
			StatusUtil.handleStatus(e, 0);
		}
	}

	private static P2_Activator					plugin;
	private static BundleContext				context;

	private ScopedPreferenceStore				preferenceStore;
	private IPropertyChangeListener				preferenceListener;

	public P2_Activator() {
		// constructor
	}

	static boolean containsURI(final URI[] locations, final URI url) {
		for (final URI location : locations) {
			if (location.equals(url)) {
				return true;
			}
		}
		return false;
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

	private static void setColocatedRepositoryEnablement(	final ProvisioningUI ui,
															final URI location,
															final boolean enable) {
		ProvUI.getArtifactRepositoryManager(ui.getSession()).setEnabled(location, enable);
		ProvUI.getMetadataRepositoryManager(ui.getSession()).setEnabled(location, enable);
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

// Original
//		ElementUtils.updateRepositoryUsingElements(ui, DEFAULT_UPDATE_SITES, null);

		updateRepositoryUsingElements(ProvisioningUI.getDefaultUI(), DEFAULT_UPDATE_SITES, null);
	}

	/**
	 * Copied from {@link ElementUtils} and disabled the part which removes previous repositories.
	 * 
	 * @param ui
	 * @param newRepos
	 * @param shell
	 */
	private static void updateRepositoryUsingElements(	final ProvisioningUI ui,
														final MetadataRepositoryElement[] newRepos,
														final Shell shell) {
		ui.signalRepositoryOperationStart();

		final IMetadataRepositoryManager metaManager = ProvUI.getMetadataRepositoryManager(ui.getSession());
		final IArtifactRepositoryManager artManager = ProvUI.getArtifactRepositoryManager(ui.getSession());

		try {

			final int visibilityFlags = ui.getRepositoryTracker().getMetadataRepositoryFlags();

			final URI[] currentlyEnabled = metaManager.getKnownRepositories(visibilityFlags);
			final URI[] currentlyDisabled = metaManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED
					| visibilityFlags);

			for (final MetadataRepositoryElement newRepo : newRepos) {

				final URI location = newRepo.getLocation();

				if (newRepo.isEnabled()) {

					if (containsURI(currentlyDisabled, location)) {

						// It should be enabled and is not currently

// disabled this code, that existing disabled repos are not enabled if the user do not want it
//
//						setColocatedRepositoryEnablement(ui, location, true);

					} else if (!containsURI(currentlyEnabled, location)) {

						// It is not known as enabled or disabled.  Add it.
						metaManager.addRepository(location);
						artManager.addRepository(location);
					}

				} else {

					if (containsURI(currentlyEnabled, location)) {

						// It should be disabled, and is currently enabled
						setColocatedRepositoryEnablement(ui, location, false);

					} else if (!containsURI(currentlyDisabled, location)) {

						// It is not known as enabled or disabled.  Add it and then disable it.
						metaManager.addRepository(location);
						artManager.addRepository(location);
						setColocatedRepositoryEnablement(ui, location, false);
					}
				}

				// set repo name

				final String name = newRepo.getName();
				if (name != null && name.length() > 0) {
					metaManager.setRepositoryProperty(location, IRepository.PROP_NICKNAME, name);
					artManager.setRepositoryProperty(location, IRepository.PROP_NICKNAME, name);
				}
			}

//			// Are there any elements that need to be deleted?  Go over the original state
//			// and remove any elements that weren't in the elements we were given
//			final Set<String> nowKnown = new HashSet<String>();
//			for (final MetadataRepositoryElement element : elements) {
//				nowKnown.add(URIUtil.toUnencodedString(element.getLocation()));
//			}
//			for (int i = 0; i < currentlyEnabled.length; i++) {
//				if (!nowKnown.contains(URIUtil.toUnencodedString(currentlyEnabled[i]))) {
//					metaManager.removeRepository(currentlyEnabled[i]);
//					artManager.removeRepository(currentlyEnabled[i]);
//				}
//			}
//			for (int i = 0; i < currentlyDisabled.length; i++) {
//				if (!nowKnown.contains(URIUtil.toUnencodedString(currentlyDisabled[i]))) {
//					metaManager.removeRepository(currentlyDisabled[i]);
//					artManager.removeRepository(currentlyDisabled[i]);
//				}
//			}

		} finally {
			ui.signalRepositoryOperationComplete(null, true);
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
