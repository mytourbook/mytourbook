/*******************************************************************************
 * Copyright (c) 2001, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.babel.build.ui.wizard;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public abstract class FilterDialog extends Dialog {

	protected static final int DIALOG_POSITION_Y = 350;
	protected static final int DIALOG_POSITION_X = 425;
	protected static final int DIALOG_HEIGHT = 115;
	protected static final int DIALOG_WIDTH = 425;
	
	protected Label fAddLabel;
	protected Text fAddText;
	protected Button fAddButton;
	protected Button fCancelButton;
	
	protected BuildToolWizardConfigurationPage fCallingPage;
	
	protected Label fErrorLabel;
	
	public FilterDialog(final Shell parent, final BuildToolWizardConfigurationPage caller) {
		super(parent);
		
		this.fCallingPage = caller;
		
		parent.setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
		parent.setLocation(DIALOG_POSITION_X, DIALOG_POSITION_Y);
	    
	    // Set layouts
		parent.setLayout(new GridLayout(1, false));
		parent.setLayoutData(new GridData());
		
		// Label describes what is expected
	    createLabelArea(parent);
	    
		// Text allows user to enter a pattern
	    createTextboxArea(parent);
	    
	    // Create a button area for adding
	    createButtonArea(parent);
	}

	protected void createLabelArea(final Shell parent) {
		fAddLabel = new Label(parent, SWT.NONE);
		fAddLabel.setText(Messages.getString("FilterDialog_EnterPatternLabel"));
		
	    GridData gdLabel = new GridData(GridData.FILL_HORIZONTAL);
		gdLabel.widthHint = 400;
		fAddLabel.setLayoutData(gdLabel);
	}

	protected void createTextboxArea(final Shell parent) {
		fAddText = new Text(parent, SWT.BORDER | SWT.SINGLE);
        
		GridData gdText = new GridData(GridData.FILL_HORIZONTAL);
        gdText.widthHint = 400;
		fAddText.setLayoutData(gdText);
	}

	protected void createButtonArea(final Shell parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));
		
		GridData gdLabel = new GridData();
		gdLabel.widthHint = 240;
		
		fErrorLabel = new Label(container, SWT.NONE);
		fErrorLabel.setLayoutData(gdLabel);
		fErrorLabel.setForeground(new Color(null, new RGB(255,0,0)));
		fErrorLabel.setText(Messages.getString("FilterDialog_PatternValidationError"));
		fErrorLabel.setVisible(false);
		
		GridData gdButton = new GridData();
		gdButton.widthHint = 75;
		
		fAddButton = new Button(container, SWT.PUSH);
		fAddButton.setText(Messages.getString("Common_AddButton"));
		fAddButton.setLayoutData(gdButton);
		fAddButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					fErrorLabel.setVisible(false);
					Pattern.compile(fAddText.getText());
					setChosenFilter(fAddText.getText());
					parent.dispose();
				} catch (PatternSyntaxException ex) {
					fErrorLabel.setVisible(true);
				}
			}
		});
		
		fCancelButton = new Button(container, SWT.PUSH);
		fCancelButton.setText(Messages.getString("Common_CancelButton"));
		fCancelButton.setLayoutData(gdButton);
		fCancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				parent.dispose();
			}
		});
		
		parent.setDefaultButton(fAddButton);
	}
	
	protected abstract void setChosenFilter(String filter);
}
