/*******************************************************************************
 * Copyright (c) 2009 Nigel Westbury
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Nigel Westbury - initial implementation
 ******************************************************************************/
package org.eclipse.babel.editor.refactoring;

import org.eclipse.babel.core.message.tree.KeyTreeNode;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * A wizard for the rename bundle key refactoring.
 */
public class RenameKeyWizard extends RefactoringWizard {

	/**
	 * Creates a {@link RenameKeyWizard}.
	 *
	 * @param resource
	 *             the bundle key to rename
	 * @param refactoring 
	 */
	public RenameKeyWizard(KeyTreeNode resource, RenameKeyProcessor refactoring) {
		super(new RenameRefactoring(refactoring), DIALOG_BASED_USER_INTERFACE);
		setDefaultPageTitle("Rename Resource Bundle Key");
		setWindowTitle("Rename Resource Bundle Key");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.ui.refactoring.RefactoringWizard#addUserInputPages()
	 */
	protected void addUserInputPages() {
		RenameKeyProcessor processor = (RenameKeyProcessor) getRefactoring().getAdapter(RenameKeyProcessor.class);
		addPage(new RenameResourceRefactoringConfigurationPage(processor));
	}

	private static class RenameResourceRefactoringConfigurationPage extends UserInputWizardPage {

		private final RenameKeyProcessor fRefactoringProcessor;
		private Text fNameField;

		public RenameResourceRefactoringConfigurationPage(RenameKeyProcessor processor) {
			super("RenameResourceRefactoringInputPage"); //$NON-NLS-1$
			fRefactoringProcessor= processor;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
		 */
		public void createControl(Composite parent) {
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout(2, false));
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			composite.setFont(parent.getFont());

			Label label = new Label(composite, SWT.NONE);
			label.setText("New name:");
			label.setLayoutData(new GridData());

			fNameField = new Text(composite, SWT.BORDER);
			fNameField.setText(fRefactoringProcessor.getNewResourceName());
			fNameField.setFont(composite.getFont());
			fNameField.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
			fNameField.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					validatePage();
				}
			});

			final Button includeChildKeysCheckbox = new Button(composite, SWT.CHECK);
			if (fRefactoringProcessor.getNewKeyTreeNode().isUsedAsKey()) {
				if (fRefactoringProcessor.getNewKeyTreeNode().getChildren().length == 0) {
					// This is an actual key with no child keys.
					includeChildKeysCheckbox.setSelection(false);
					includeChildKeysCheckbox.setEnabled(false);
				} else {
					// This is both an actual key and it has child keys, so we 
					// let the user choose whether to also rename the child keys.
					includeChildKeysCheckbox.setSelection(fRefactoringProcessor.getRenameChildKeys());
					includeChildKeysCheckbox.setEnabled(true);
				}
			} else {
				// This is no an actual key, just a containing node, so the option
				// to rename child keys must be set (otherwise this rename would not
				// do anything).
				includeChildKeysCheckbox.setSelection(true);
				includeChildKeysCheckbox.setEnabled(false);
			}
			
			includeChildKeysCheckbox.setText("Also rename child keys (other keys with this key as a prefix)");
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan= 2;
			includeChildKeysCheckbox.setLayoutData(gd);
			includeChildKeysCheckbox.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					fRefactoringProcessor.setRenameChildKeys(includeChildKeysCheckbox.getSelection());
				}
			});
			
			fNameField.selectAll();
			setPageComplete(false);
			setControl(composite);
		}

		public void setVisible(boolean visible) {
			if (visible) {
				fNameField.setFocus();
			}
			super.setVisible(visible);
		}

		protected final void validatePage() {
			String text= fNameField.getText();
			RefactoringStatus status= fRefactoringProcessor.validateNewElementName(text);
			setPageComplete(status);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ltk.ui.refactoring.UserInputWizardPage#performFinish()
		 */
		protected boolean performFinish() {
			initializeRefactoring();
			storeSettings();
			return super.performFinish();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ltk.ui.refactoring.UserInputWizardPage#getNextPage()
		 */
		public IWizardPage getNextPage() {
			initializeRefactoring();
			storeSettings();
			return super.getNextPage();
		}

		private void storeSettings() {
		}

		private void initializeRefactoring() {
			fRefactoringProcessor.setNewResourceName(fNameField.getText());
		}
	}
}