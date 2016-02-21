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

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.sdk.IProvSDKHelpContextIds;
import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKMessages;
import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKUIActivator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.Preferences;

/**
 * Preference page for general provisioning preferences.
 * 
 * @since 3.4
 */

@SuppressWarnings("restriction")
public class ProvisioningPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Group	browsingGroup, validateGroup;
	private Button	showLatestRadio, showAllRadio;
	private Button	alwaysShowFailedPlan, neverShowFailedPlan, promptOnFailedPlan;

	@Override
	protected Control createContents(final Composite parent) {

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IProvSDKHelpContextIds.PROVISIONING_PREFERENCE_PAGE);

		GridData gd;

		final Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		container.setLayout(layout);
		{
			// Group for show all versions vs. show latest
			browsingGroup = new Group(container, SWT.NONE);
			browsingGroup.setText(ProvSDKMessages.ProvisioningPreferencePage_BrowsingPrefsGroup);
			layout = new GridLayout();
			layout.numColumns = 3;
			browsingGroup.setLayout(layout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			browsingGroup.setLayoutData(gd);
			{
				showLatestRadio = new Button(browsingGroup, SWT.RADIO);
				showLatestRadio.setText(ProvSDKMessages.ProvisioningPreferencePage_ShowLatestVersions);
				gd = new GridData();
				gd.horizontalSpan = 3;
				showLatestRadio.setLayoutData(gd);

				showAllRadio = new Button(browsingGroup, SWT.RADIO);
				showAllRadio.setText(ProvSDKMessages.ProvisioningPreferencePage_ShowAllVersions);
				gd = new GridData();
				gd.horizontalSpan = 3;
				showAllRadio.setLayoutData(gd);
			}

			//Group for validating a failed plan
			validateGroup = new Group(container, SWT.NONE);
			validateGroup.setText(ProvSDKMessages.ProvisioningPreferencePage_OpenWizardIfInvalid);
			layout = new GridLayout();
			layout.numColumns = 3;
			validateGroup.setLayout(layout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			validateGroup.setLayoutData(gd);
			{
				alwaysShowFailedPlan = new Button(validateGroup, SWT.RADIO);
				alwaysShowFailedPlan.setText(ProvSDKMessages.ProvisioningPreferencePage_AlwaysOpenWizard);
				gd = new GridData();
				gd.horizontalSpan = 3;
				alwaysShowFailedPlan.setLayoutData(gd);

				neverShowFailedPlan = new Button(validateGroup, SWT.RADIO);
				neverShowFailedPlan.setText(ProvSDKMessages.ProvisioningPreferencePage_NeverOpenWizard);
				gd = new GridData();
				gd.horizontalSpan = 3;
				neverShowFailedPlan.setLayoutData(gd);

				promptOnFailedPlan = new Button(validateGroup, SWT.RADIO);
				promptOnFailedPlan.setText(ProvSDKMessages.ProvisioningPreferencePage_PromptToOpenWizard);
				gd = new GridData();
				gd.horizontalSpan = 3;
				promptOnFailedPlan.setLayoutData(gd);
			}

			//Link to installed software page
			//See https://bugs.eclipse.org/bugs/show_bug.cgi?id=313242
			final Link link = new Link(container, SWT.PUSH);
			link.setText(ProvSDKMessages.ProvisioningPreferencePage_UninstallUpdateLink);
			link.addListener(SWT.Selection, new Listener() {
				public void handleEvent(final Event event) {
					ProvUI.openInstallationDialog(event);
				}
			});
		}

		initialize();

		Dialog.applyDialogFont(container);

		return container;
	}

	public void init(final IWorkbench workbench) {
		// Nothing to do
	}

	private void initialize() {

		final IPreferenceStore pref = ProvSDKUIActivator.getDefault().getPreferenceStore();

		showLatestRadio.setSelection(pref.getBoolean(PreferenceConstants.PREF_SHOW_LATEST_VERSION));
		showAllRadio.setSelection(!pref.getBoolean(PreferenceConstants.PREF_SHOW_LATEST_VERSION));

		final String openWizard = pref.getString(PreferenceConstants.PREF_OPEN_WIZARD_ON_ERROR_PLAN);
		alwaysShowFailedPlan.setSelection(openWizard.equals(MessageDialogWithToggle.ALWAYS));
		neverShowFailedPlan.setSelection(openWizard.equals(MessageDialogWithToggle.NEVER));
		promptOnFailedPlan.setSelection(openWizard.equals(MessageDialogWithToggle.PROMPT));
	}

	@Override
	protected void performDefaults() {

		super.performDefaults();
		final Preferences pref = new DefaultScope().getNode(ProvSDKUIActivator.PLUGIN_ID);

		showLatestRadio.setSelection(pref.getBoolean(PreferenceConstants.PREF_SHOW_LATEST_VERSION, false));
		showAllRadio.setSelection(!pref.getBoolean(PreferenceConstants.PREF_SHOW_LATEST_VERSION, false));

		final String openWizard = pref.get(PreferenceConstants.PREF_OPEN_WIZARD_ON_ERROR_PLAN, ""); //$NON-NLS-1$
		alwaysShowFailedPlan.setSelection(openWizard.equals(MessageDialogWithToggle.ALWAYS));
		neverShowFailedPlan.setSelection(openWizard.equals(MessageDialogWithToggle.NEVER));
		promptOnFailedPlan.setSelection(openWizard.equals(MessageDialogWithToggle.PROMPT));
	}

	@Override
	public boolean performOk() {

		final IPreferenceStore pref = ProvSDKUIActivator.getDefault().getPreferenceStore();

		pref.setValue(PreferenceConstants.PREF_SHOW_LATEST_VERSION, showLatestRadio.getSelection());

		if (alwaysShowFailedPlan.getSelection()) {
			pref.setValue(PreferenceConstants.PREF_OPEN_WIZARD_ON_ERROR_PLAN, MessageDialogWithToggle.ALWAYS);
		} else if (neverShowFailedPlan.getSelection()) {
			pref.setValue(PreferenceConstants.PREF_OPEN_WIZARD_ON_ERROR_PLAN, MessageDialogWithToggle.NEVER);
		} else {
			pref.setValue(PreferenceConstants.PREF_OPEN_WIZARD_ON_ERROR_PLAN, MessageDialogWithToggle.PROMPT);
		}

		ProvSDKUIActivator.getDefault().savePreferences();

		return true;
	}

}
