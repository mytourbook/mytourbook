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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.MessageDialog;

/**
 * UpdateHandler invokes the check for updates UI
 * 
 * @since 3.4
 */
public class UpdateHandler extends PreloadingRepositoryHandler {

	boolean			hasNoRepos	= false;

	UpdateOperation	operation;

	@Override
	protected void doExecute(final LoadMetadataRepositoryJob job) {

		final ProvisioningUI provisioningUI = getProvisioningUI();
		final Policy policy = provisioningUI.getPolicy();

		if (hasNoRepos) {
			if (policy.getRepositoriesVisible()) {

				final boolean goToSites = MessageDialog.openQuestion(
						getShell(),
						ProvSDKMessages.UpdateHandler_NoSitesTitle,
						ProvSDKMessages.UpdateHandler_NoSitesMessage);

				if (goToSites) {
					provisioningUI.manipulateRepositories(getShell());
				}
			}
			return;
		}
		// Report any missing repositories.
		job.reportAccumulatedStatus();
		if (policy.continueWorkingWithOperation(operation, getShell())) {
			provisioningUI.openUpdateWizard(false, operation, job);
		}
	}

	@Override
	protected void doPostLoadBackgroundWork(final IProgressMonitor monitor) throws OperationCanceledException {
		operation = getProvisioningUI().getUpdateOperation(null, null);
		// check for updates
		final IStatus resolveStatus = operation.resolveModal(monitor);
		if (resolveStatus.getSeverity() == IStatus.CANCEL) {
			throw new OperationCanceledException();
		}
	}

	@Override
	protected String getProgressTaskName() {
		return ProvSDKMessages.UpdateHandler_ProgressTaskName;
	}

	@Override
	protected boolean preloadRepositories() {

		hasNoRepos = false;
		final RepositoryTracker repoMan = getProvisioningUI().getRepositoryTracker();

		if (repoMan.getKnownRepositories(getProvisioningUI().getSession()).length == 0) {
			hasNoRepos = true;
			return false;
		}

		return super.preloadRepositories();
	}
}
