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

import java.util.ArrayList;

import org.eclipse.babel.editor.plugin.MessagesEditorPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.pde.nls.internal.ui.parser.LocaleUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class ConfigureColumnsDialog extends Dialog {

	private class ColumnField {
		Text text;
		ToolItem clearButton;
	}

	private ArrayList<ColumnField> fields = new ArrayList<ColumnField>();

	private ArrayList<String> result = new ArrayList<String>();
	private String[] initialValues;
	private Color errorColor;

	private Image clearImage;

	public ConfigureColumnsDialog(Shell parentShell, String[] initialValues) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.initialValues = initialValues;
	}

	/*
	 * @see org.eclipse.jface.window.Window#open()
	 */
	@Override
	public int open() {
		return super.open();
	}

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Configure Columns");
	}

	/*
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = (GridLayout) composite.getLayout();
		gridLayout.numColumns = 3;

		Label label = new Label(composite, SWT.NONE);
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		label.setText("Enter \"key\", \"default\" or locale (e.g. \"de\" or \"zh_TW\"):");
		label.setLayoutData(GridDataFactory.fillDefaults().hint(300, SWT.DEFAULT).span(3, 1).create());

		fields.add(createLanguageField(composite, "Column &1:"));
		fields.add(createLanguageField(composite, "Column &2:"));
		fields.add(createLanguageField(composite, "Column &3:"));
		fields.add(createLanguageField(composite, "Column &4:"));

		if (initialValues != null) {
			for (int i = 0; i < 4 && i < initialValues.length; i++) {
				fields.get(i).text.setText(initialValues[i]);
			}
		}
		
		ModifyListener modifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validate();
			}
		};
		for (ColumnField field : fields) {
			field.text.addModifyListener(modifyListener);
		}
		errorColor = new Color(Display.getCurrent(), 0xff, 0x7f, 0x7f);

		return composite;
	}
	
	/*
	 * @see org.eclipse.jface.dialogs.Dialog#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		Control contents = super.createContents(parent);
		validate();
		return contents;
	}

	private ColumnField createLanguageField(Composite parent, String labelText) {
		Label label = new Label(parent, SWT.NONE);
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		label.setText(labelText);

		Text text = new Text(parent, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		if (clearImage == null)
			clearImage = MessagesEditorPlugin.getImageDescriptor("elcl16/clear_co.gif").createImage(); //$NON-NLS-1$
		
		ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
		ToolItem item = new ToolItem(toolbar, SWT.PUSH);
		item.setImage(clearImage);
		item.setToolTipText("Clear");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (ColumnField field : fields) {
					if (field.clearButton == e.widget) {
						field.text.setText(""); //$NON-NLS-1$
					}
				}
			}
		});
		
		ColumnField field = new ColumnField();
		field.text = text;
		field.clearButton = item;
		return field;
	}

	/*
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		for (ColumnField field : fields) {
			String text = field.text.getText().trim();
			if (text.length() > 0) {
				result.add(text);
			}
		}
		super.okPressed();
		errorColor.dispose();
		clearImage.dispose();
	}

	public String[] getResult() {
		return result.toArray(new String[result.size()]);
	}
	
	protected void validate() {
		boolean isValid = true;
		for (ColumnField field : fields) {
			String text = field.text.getText();
			if (text.equals("") || text.equals("key") || text.equals("default")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				field.text.setBackground(null);
			} else {
				try {
					LocaleUtil.parseLocale(text);
					field.text.setBackground(null);
				} catch (IllegalArgumentException e) {
					field.text.setBackground(errorColor);
					isValid = false;
				}
			}
		}
		Button okButton = getButton(IDialogConstants.OK_ID);
		okButton.setEnabled(isValid);
	}

}
