/*******************************************************************************
 * Copyright (c) 2008 Stefan Mücke and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stefan Mücke - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.nls.internal.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class FilterOptionsDialog extends Dialog {

	private static final String SEPARATOR = ","; //$NON-NLS-1$

	private Button enablePluginPatterns;
	private Button keysWithMissingEntriesOnly;
	private Text pluginPatterns;
	
	private FilterOptions initialOptions;
	private FilterOptions result;

	public FilterOptionsDialog(Shell shell) {
		super(shell);
	}

	public void setInitialFilterOptions(FilterOptions initialfilterOptions) {
		this.initialOptions = initialfilterOptions;
	}

	/*
	 * @see org.eclipse.ui.dialogs.SelectionDialog#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Filters");
	}

	/*
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		// Checkbox
		enablePluginPatterns = new Button(composite, SWT.CHECK);
		enablePluginPatterns.setFocus();
		enablePluginPatterns.setText("Filter by plug-in id patterns (separated by comma):");
		enablePluginPatterns.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pluginPatterns.setEnabled(enablePluginPatterns.getSelection());
			}
		});
		enablePluginPatterns.setSelection(initialOptions.filterPlugins);

		// Pattern	field
		pluginPatterns = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		data.widthHint = convertWidthInCharsToPixels(59);
		pluginPatterns.setLayoutData(data);
		pluginPatterns.setEnabled(initialOptions.filterPlugins);
		if (initialOptions.pluginPatterns != null) {
			StringBuilder builder = new StringBuilder();
			String[] patterns = initialOptions.pluginPatterns;
			for (String pattern : patterns) {
				if (builder.length() > 0) {
					builder.append(SEPARATOR);
					builder.append(" ");
				}
				builder.append(pattern);
			}
			pluginPatterns.setText(builder.toString());
		}
		
		keysWithMissingEntriesOnly = new Button(composite, SWT.CHECK);
		keysWithMissingEntriesOnly.setText("Keys with missing entries only");
		keysWithMissingEntriesOnly.setSelection(initialOptions.keysWithMissingEntriesOnly);

		applyDialogFont(parent);
		return parent;
	}

	protected void okPressed() {
		String patterns = pluginPatterns.getText();
		result = new FilterOptions();
		result.filterPlugins = enablePluginPatterns.getSelection();
		String[] split = patterns.split(SEPARATOR);
		for (int i = 0; i < split.length; i++) {
			split[i] = split[i].trim();
		}
		result.pluginPatterns = split;
		result.keysWithMissingEntriesOnly = keysWithMissingEntriesOnly.getSelection();
		super.okPressed();
	}
	
	public FilterOptions getResult() {
		return result;
	}
	
}
