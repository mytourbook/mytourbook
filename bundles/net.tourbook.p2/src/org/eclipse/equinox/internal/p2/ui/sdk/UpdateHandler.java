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

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.jface.dialogs.MessageDialog;

/**
 * UpdateHandler invokes the check for updates UI
 * 
 * @since 3.4
 */
public class UpdateHandler extends PreloadingRepositoryHandler {

	boolean hasNoRepos = false;
	UpdateOperation operation;

	protected void doExecute(LoadMetadataRepositoryJob job) {
		if (hasNoRepos) {
			if (getProvisioningUI().getPolicy().getRepositoriesVisible()) {
				boolean goToSites = MessageDialog.openQuestion(getShell(), ProvSDKMessages.UpdateHandler_NoSitesTitle, ProvSDKMessages.UpdateHandler_NoSitesMessage);
				if (goToSites) {
					getProvisioningUI().manipulateRepositories(getShell());
				}
			}
			return;
		}
		// Report any missing repositories.
		job.reportAccumulatedStatus();
		if (getProvisioningUI().getPolicy().continueWorkingWithOperation(operation, getShell())) {
			getProvisioningUI().openUpdateWizard(false, operation, job);
		}
	}

	protected void doPostLoadBackgroundWork(IProgressMonitor monitor) throws OperationCanceledException {
		operation = getProvisioningUI().getUpdateOperation(null, null);
		// check for updates
		IStatus resolveStatus = operation.resolveModal(monitor);
		if (resolveStatus.getSeverity() == IStatus.CANCEL)
			throw new OperationCanceledException();
	}

	protected boolean preloadRepositories() {
		hasNoRepos = false;
		RepositoryTracker repoMan = getProvisioningUI().getRepositoryTracker();
		if (repoMan.getKnownRepositories(getProvisioningUI().getSession()).length == 0) {
			hasNoRepos = true;
			return false;
		}
		return super.preloadRepositories();
	}

	@Override
	protected String getProgressTaskName() {
		return ProvSDKMessages.UpdateHandler_ProgressTaskName;
	}
}
