/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * PreloadingRepositoryHandler provides background loading of
 * repositories before executing the provisioning handler.
 * 
 * @since 3.5
 */
abstract class PreloadingRepositoryHandler extends AbstractHandler {

	/**
	 * The constructor.
	 */
	public PreloadingRepositoryHandler() {
		// constructor
	}

	/**
	 * Execute the command.
	 */
	public Object execute(ExecutionEvent event) {
		// Look for a profile.  We may not immediately need it in the
		// handler, but if we don't have one, whatever we are trying to do
		// will ultimately fail in a more subtle/low-level way.  So determine
		// up front if the system is configured properly.
		String profileId = getProvisioningUI().getProfileId();
		IProvisioningAgent agent = getProvisioningUI().getSession().getProvisioningAgent();
		IProfile profile = null;
		if (agent != null) {
			IProfileRegistry registry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
			if (registry != null) {
				profile = registry.getProfile(profileId);
			}
		}
		if (profile == null) {
			// Inform the user nicely
			MessageDialog.openInformation(null, ProvSDKMessages.Handler_SDKUpdateUIMessageTitle, ProvSDKMessages.Handler_CannotLaunchUI);
			// Log the detailed message
			StatusManager.getManager().handle(ProvSDKUIActivator.getNoSelfProfileStatus());
		} else {
			BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
				public void run() {
					doExecuteAndLoad();
				}
			});
		}
		return null;
	}

	void doExecuteAndLoad() {
		if (preloadRepositories()) {
			//cancel any load that is already running
			Job.getJobManager().cancel(LoadMetadataRepositoryJob.LOAD_FAMILY);
			final LoadMetadataRepositoryJob loadJob = new LoadMetadataRepositoryJob(getProvisioningUI()) {

				public IStatus runModal(IProgressMonitor monitor) {
					SubMonitor sub = SubMonitor.convert(monitor, getProgressTaskName(), 1000);
					IStatus status = super.runModal(sub.newChild(500));
					if (status.getSeverity() == IStatus.CANCEL)
						return status;
					try {
						doPostLoadBackgroundWork(sub.newChild(500));
					} catch (OperationCanceledException e) {
						return Status.CANCEL_STATUS;
					}
					if (shouldAccumulateFailures()) {
						// If we are accumulating failures, don't return a combined status here. By returning OK, 
						// we are indicating that the operation should continue with the repositories that
						// did load.
						return Status.OK_STATUS;
					}
					return status;
				}
			};
			setLoadJobProperties(loadJob);
			if (waitForPreload()) {
				loadJob.addJobChangeListener(new JobChangeAdapter() {
					public void done(IJobChangeEvent event) {
						if (PlatformUI.isWorkbenchRunning())
							if (event.getResult().isOK()) {
								PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
									public void run() {
										doExecute(loadJob);
									}
								});
							}
					}
				});
				loadJob.setUser(true);
				loadJob.schedule();

			} else {
				loadJob.setSystem(true);
				loadJob.setUser(false);
				loadJob.schedule();
				doExecute(null);
			}
		} else {
			doExecute(null);
		}
	}

	protected abstract String getProgressTaskName();

	protected abstract void doExecute(LoadMetadataRepositoryJob job);

	protected boolean preloadRepositories() {
		return true;
	}

	protected void doPostLoadBackgroundWork(IProgressMonitor monitor) throws OperationCanceledException {
		// default is to do nothing more.
	}

	protected boolean waitForPreload() {
		return true;
	}

	protected void setLoadJobProperties(Job loadJob) {
		loadJob.setProperty(LoadMetadataRepositoryJob.ACCUMULATE_LOAD_ERRORS, Boolean.toString(true));
	}

	protected ProvisioningUI getProvisioningUI() {
		return ProvisioningUI.getDefaultUI();
	}

	/**
	 * Return a shell appropriate for parenting dialogs of this handler.
	 * @return a Shell
	 */
	protected Shell getShell() {
		return PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
	}
}
