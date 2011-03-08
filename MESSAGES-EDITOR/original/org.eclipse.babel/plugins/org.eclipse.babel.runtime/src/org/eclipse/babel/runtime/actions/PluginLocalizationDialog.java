/*******************************************************************************
 * Copyright (c) 2008 Nigel Westbury and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Nigel Westbury - initial API and implementation
 *******************************************************************************/

package org.eclipse.babel.runtime.actions;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import org.eclipse.babel.runtime.Messages;
import org.eclipse.babel.runtime.dialogs.LocalizableTrayDialog;
import org.eclipse.babel.runtime.external.TranslatableNLS;
import org.eclipse.babel.runtime.external.ITranslatableSet;
import org.eclipse.babel.runtime.external.ITranslatableText;
import org.eclipse.babel.runtime.external.TranslatableText;
import org.eclipse.babel.runtime.external.TranslatableTextInput;
import org.eclipse.babel.runtime.external.TranslatableResourceBundle;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.internal.about.AboutBundleData;
import org.osgi.framework.Bundle;

public class PluginLocalizationDialog extends LocalizableTrayDialog {

	private Bundle bundle;

	private ITranslatableSet targetLanguageSet;

	private Set<TranslatableResourceBundle> updatedBundles = new HashSet<TranslatableResourceBundle>();

	protected PluginLocalizationDialog(Shell shell, AboutBundleData bundleData, ITranslatableSet targetLanguageSet) {
		super(shell);

		this.bundle = bundleData.getBundle();
		this.targetLanguageSet = targetLanguageSet;
	}

	protected Control createDialogArea(Composite parent) {
		languageSet.associate(getShell(), TranslatableNLS.bind(Messages.LocalizeDialog_Title_PluginPart, bundle.getSymbolicName()));

		Composite container = (Composite)super.createDialogArea(parent);

		final TabFolder tabFolder = new TabFolder(container, SWT.NONE);
		
		// The plugin.properties resource bundle
		addPreActivationResourceTab(tabFolder);

		/*
		 * For each resource bundle in the plug-in, add a tab for that
		 * too.
		 */
		Collection<TranslatableResourceBundle> resourceBundles = TranslatableResourceBundle.getAllResourceBundles().get(bundle);
		if (resourceBundles != null) {
			for (TranslatableResourceBundle resourceBundle: resourceBundles) {
				addPostActivationResourceTab(tabFolder, resourceBundle);
			}
		}

//		createButtonsSection(container);

		Dialog.applyDialogFont(container);
		return container;
	}

	private void addPreActivationResourceTab(TabFolder tabFolder) {
		TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
		languageSet.associate(tabItem, Messages.LocalizeDialog_TabTitle_PluginXml);

		Composite c = new Composite(tabFolder, SWT.NONE);
		tabItem.setControl(c);

		c.setLayout(new GridLayout());

		final Properties p = new Properties();
		Enumeration<?> e = bundle.findEntries("", "plugin.properties", false); //$NON-NLS-1$ //$NON-NLS-2$
		// e will be null if there is no plugin.properties in the plug-in.
		if (e != null) {
			while (e.hasMoreElements()) {
				URL url = (URL)e.nextElement();
				try {
					InputStream is = url.openStream();
					p.load(is);
					is.close();
				} catch (IOException ex) {
					throw new RuntimeException("", ex); //$NON-NLS-1$
				}
				break;
			}
		}

		ITranslatableText[] texts = new ITranslatableText[p.size()];
		int i = 0;
		for (final Object key: p.keySet()) {
			texts[i++] = new ITranslatableText() {

				public String getLocalizedText(Locale locale) {
					return p.getProperty((String)key);
				}

				public String getLocalizedText() {
					return getLocalizedText(Locale.getDefault());
				}

				public void validateLocale(Locale locale) {
				}
			};
		}
		
		Control tv = new TranslatableTreeComposite(c, new TextInputContentProvider(), texts, languageSet, updatedBundles);
		tv.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
	}

	private void addPostActivationResourceTab(TabFolder tabFolder, TranslatableResourceBundle resourceBundle) {
		TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(resourceBundle.getDescription());

		Composite c = new Composite(tabFolder, SWT.NONE);
		tabItem.setControl(c);

		c.setLayout(new GridLayout());

		ArrayList<ITranslatableText> texts = new ArrayList<ITranslatableText>();
		Enumeration<String> e = resourceBundle.getKeys();
		while (e.hasMoreElements()) {
			texts.add(new TranslatableText(resourceBundle, e.nextElement()));
		}

		Control tv = new TranslatableTreeComposite(c, new TextInputContentProvider(), texts.toArray(new ITranslatableText[0]), languageSet, updatedBundles);
		tv.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
	}

	@Override
	protected void okPressed() {
		if (targetLanguageSet != null) {
			for (TranslatableTextInput textInput: targetLanguageSet.getLocalizedTexts()) {
				textInput.updateControl();
			}
		}

		/*
		 * Save all bundles that have had changes.  These have been put
		 * into a set, so it is easy for us to know which ones have been changed.
		 */
		for (TranslatableResourceBundle bundle: updatedBundles) {
			bundle.save();
		}

		super.okPressed();
	}
}
