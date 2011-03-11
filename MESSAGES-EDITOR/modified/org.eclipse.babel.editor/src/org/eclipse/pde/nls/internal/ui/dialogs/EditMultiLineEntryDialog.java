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
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class EditMultiLineEntryDialog extends Dialog {

	private Text textWidget;
	private String text;
	private boolean readOnly;
	
	protected EditMultiLineEntryDialog(Shell parentShell, String initialInput, boolean readOnly) {
		super(parentShell);
		this.readOnly = readOnly;
		setShellStyle(getShellStyle() | SWT.RESIZE);
		text = initialInput;
	}
	
	/*
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Edit Resource Bundle Entry");
	}

	public String getValue() {
		return text;
	}
	
	/*
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		
		int readOnly = this.readOnly ? SWT.READ_ONLY : 0;
		Text text = new Text(composite, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | readOnly);
		text.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(350, 150).create());
		text.setText(text == null ? "" : this.text);
		
		textWidget = text;
		
		return composite;
	}
	
	/*
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		text = textWidget.getText();
		super.okPressed();
	}

}
