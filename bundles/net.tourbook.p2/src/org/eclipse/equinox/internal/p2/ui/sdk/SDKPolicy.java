/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceConstants;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.query.UserVisibleRootQuery;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * SDKPolicy defines the Eclipse SDK UI policies for the
 * p2 UI.  The policy is declared as an OSGi service in
 * the policy_component.xml file.
 * 
 * @since 3.6
 */
public class SDKPolicy extends Policy {

	public SDKPolicy() {
		// initialize for our values
		setVisibleAvailableIUQuery(QueryUtil.createIUGroupQuery());
		// If this ever changes, we must change AutomaticUpdateSchedule.getProfileQuery()
		setVisibleInstalledIUQuery(new UserVisibleRootQuery());
		setRepositoryPreferencePageId("org.eclipse.equinox.internal.p2.ui.sdk.SitesPreferencePage"); //$NON-NLS-1$
		setRepositoryPreferencePageName(ProvSDKMessages.SDKPolicy_PrefPageName);
		ProvSDKUIActivator.getDefault().updateWithPreferences(this);
	}

	public IStatus getNoProfileChosenStatus() {
		return ProvSDKUIActivator.getNoSelfProfileStatus();
	}

	public boolean continueWorkingOperation(ProfileChangeOperation operation, Shell shell) {
		// don't continue if superclass has already identified problem scenarios
		boolean ok = super.continueWorkingWithOperation(operation, shell);
		if (!ok)
			return false;

		IProvisioningPlan plan = operation.getProvisioningPlan();
		if (plan == null)
			return false;

		// Check the preference to see whether to continue.
		IPreferenceStore prefs = ProvSDKUIActivator.getDefault().getPreferenceStore();
		String openPlan = prefs.getString(PreferenceConstants.PREF_OPEN_WIZARD_ON_ERROR_PLAN);
		if (MessageDialogWithToggle.ALWAYS.equals(openPlan)) {
			return true;
		}
		if (MessageDialogWithToggle.NEVER.equals(openPlan)) {
			StatusManager.getManager().handle(plan.getStatus(), StatusManager.SHOW | StatusManager.LOG);
			return false;
		}
		MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoCancelQuestion(shell, ProvSDKMessages.ProvSDKUIActivator_Question, ProvSDKMessages.ProvSDKUIActivator_OpenWizardAnyway, null, false, prefs, PreferenceConstants.PREF_OPEN_WIZARD_ON_ERROR_PLAN);

		// Any answer but yes will stop the performance of the plan, but NO is interpreted to mean, show me the error.
		if (dialog.getReturnCode() == IDialogConstants.NO_ID)
			StatusManager.getManager().handle(plan.getStatus(), StatusManager.SHOW | StatusManager.LOG);
		return dialog.getReturnCode() == IDialogConstants.YES_ID;
	}
}
