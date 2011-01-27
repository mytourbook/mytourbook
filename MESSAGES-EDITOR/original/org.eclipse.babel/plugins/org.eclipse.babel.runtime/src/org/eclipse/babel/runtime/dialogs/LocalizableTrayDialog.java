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

package org.eclipse.babel.runtime.dialogs;

import org.eclipse.babel.runtime.Activator;
import org.eclipse.babel.runtime.Messages;
import org.eclipse.babel.runtime.actions.LocalizeDialog;
import org.eclipse.babel.runtime.external.TranslatableNLS;
import org.eclipse.babel.runtime.external.TranslatableSet;
import org.eclipse.babel.runtime.external.TranslatableTextInput;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;


public class LocalizableTrayDialog extends TrayDialog {

	private static ImageDescriptor localizationImageDescriptor = Activator.createImageDescriptor("icons/babel.gif"); //$NON-NLS-1$
	protected TranslatableSet languageSet = new TranslatableSet();
	
	/**
	 * Creates a tray dialog instance. Note that the window will have no visual
	 * representation (no widgets) until it is told to open.
	 * 
	 * @param shell the parent shell, or <code>null</code> to create a top-level shell
	 */
	protected LocalizableTrayDialog(Shell shell) {
		super(shell);
	}
	
	/**
	 * Creates a tray dialog with the given parent.
	 * 
	 * @param parentShell the object that returns the current parent shell
	 */
	protected LocalizableTrayDialog(IShellProvider parentShell) {
		super(parentShell);
	}

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#createButtonBar(org.eclipse.swt.widgets.Composite)
     */
	@Override
	protected Control createButtonBar(Composite parent) {
    	Composite composite = new Composite(parent, SWT.NONE);
    	GridLayout layout = new GridLayout();
    	layout.marginWidth = 0;
    	layout.marginHeight = 0;
    	layout.horizontalSpacing = 0;
    	composite.setLayout(layout);
    	composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    	composite.setFont(parent.getFont());

        // create the localization button
    	Control localizationControl = createLocalizationControl(composite);
    	((GridData) localizationControl.getLayoutData()).horizontalIndent = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
    	
        Control buttonSection = super.createButtonBar(composite);
        ((GridData) buttonSection.getLayoutData()).grabExcessHorizontalSpace = true;
        return composite;
	}

	/**
	 * Creates a new help control that provides access to context help.
	 * <p>
	 * The <code>TrayDialog</code> implementation of this method creates
	 * the control, registers it for selection events including selection,
	 * Note that the parent's layout is assumed to be a <code>GridLayout</code>
	 * and the number of columns in this layout is incremented. Subclasses may
	 * override.
	 * </p>
	 * 
	 * @param parent the parent composite
	 * @return the help control
	 */
    protected Control createLocalizationControl(Composite parent) {
		Image image = localizationImageDescriptor.createImage();
		if (image != null) {
			return createLocalizationImageButton(parent, image);
		}
		return createLocalizationLink(parent);
    }
    
    /*
     * Creates a button with a help image. This is only used if there
     * is an image available.
     */
	private ToolBar createLocalizationImageButton(Composite parent, Image image) {
        ToolBar toolBar = new ToolBar(parent, SWT.FLAT | SWT.NO_FOCUS);
        ((GridLayout) parent.getLayout()).numColumns++;
		toolBar.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
		final Cursor cursor = new Cursor(parent.getDisplay(), SWT.CURSOR_HAND);
		toolBar.setCursor(cursor);
		toolBar.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				cursor.dispose();
			}
		});		

		final ToolItem item = new ToolItem(toolBar, SWT.NONE);
		item.setImage(image);
		
		languageSet.associate(
				"localizationToolTip",  //$NON-NLS-1$
				new TranslatableTextInput(Activator.getLocalizableText("LocalizableTrayDialog.localizationToolTip")) { //$NON-NLS-1$
					@Override
					public void updateControl(String text) {
						item.setToolTipText(text);
					}
				}
		);
		
		item.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
            	localizationPressed();
            }
        });
		return toolBar;
	}

	/*
	 * Creates a help link. This is used when there is no help image
	 * available.
	 */
	private Link createLocalizationLink(Composite parent) {
		Link link = new Link(parent, SWT.WRAP | SWT.NO_FOCUS);
        ((GridLayout) parent.getLayout()).numColumns++;
		link.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
		link.setText("<a>"+IDialogConstants.HELP_LABEL+"</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		link.setToolTipText(IDialogConstants.HELP_LABEL);
		link.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
            	localizationPressed();
            }
        });
		return link;
	}	
	
	/*
	 * Called when the user selects the action to localize this dialog.
	 * This will bring up a dialog that can be used to localize this dialog.
	 */
	protected void localizationPressed() {
		Dialog dialog = new LocalizeDialog(getShell(), TranslatableNLS.bind(Messages.LocalizeDialog_Title_DialogPart, getShell().getText()), languageSet, Activator.getDefault().getMenuTextSet());
		dialog.open();
	}
}
