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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

public class BuildToolWizardPluginPage extends BuildToolWizardPage {
	
	private static final String EXTERNAL_ECLIPSE_CHECKBOX = "BuildToolWizardPluginPage#fEclipseInstallCheckbox";
	private static final String EXTERNAL_ECLIPSE_LOCATION = "BuildToolWizardPluginPage#fEclipseArchiveLocationText";
	private static final String FILTER_PATTERN = "BuildToolWizardPluginPage#fPluginFilter";
	
	private static final int MARGIN = 12;
	private static final int LABEL_WIDTH = 30;
	private static final int TEXTBOX_WIDTH = 300;
    private static final String[] fEclipseArchiveExtensions = new String[] { "*.zip" }; //$NON-NLS-1$

	private BuildToolModelTable fModelPluginsTable;
	
	private Button fEclipseInstallCheckbox;
	private Button fEclipseArchiveBrowseButton;
	private Text fEclipseArchiveLocationText;
	private Label fEclipseArchiveLocationLabel;
	
	private FilteredListComponent fPluginFilter;
	
	private class TextModifyListener implements ModifyListener {
		public void modifyText(ModifyEvent e) {
			textSelectionChanged();
		}
	}
	
	private class CheckboxSelectionListener implements SelectionListener {
		public void widgetSelected(SelectionEvent e){
			checkboxSelectionChanged();
		}

		public void widgetDefaultSelected(SelectionEvent e){
			
		}
	}
		
	protected BuildToolWizardPluginPage(String pageName, BuildToolModelTable modelPluginsTable) {
		super(pageName);
		this.fModelPluginsTable = modelPluginsTable;
		
		this.setTitle(Messages.getString("BuildToolWizardPluginPage_PageTitle")); //$NON-NLS-1$
		this.setDescription(Messages.getString("BuildToolWizardPluginPage_PageDescription")); //$NON-NLS-1$
	}
	
	@Override
	public void createControl(Composite parent) {		
		// Create filtered list component
		fPluginFilter = new FilteredListComponent(fModelPluginsTable, new PluginLabelProvider(), this);
		Composite container = fPluginFilter.createFilteredListComponent(parent);

		createEclipseInstallArea(container);

		setControl(container);
		Dialog.applyDialogFont(container);
		
		checkboxSelectionChanged();
		
		String pattern = not_null(getDialogSettings().get(FILTER_PATTERN), "");
		fPluginFilter.fFilterText.setText(pattern);
	}
	
	private void createEclipseInstallArea(Composite parent) {				
		Group container = new Group(parent, SWT.NONE);
		FillLayout layout = new FillLayout(SWT.VERTICAL);
		layout.marginHeight = layout.marginWidth = MARGIN;
		container.setLayout(layout);

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		container.setLayoutData(gd);
		
		Composite container1 = new Composite(container, SWT.NONE);
		container1.setLayout(new GridLayout(1, false));
		
		Composite container2 = new Composite(container, SWT.NONE);
		container2.setLayout(new GridLayout(3, false));
		
		createEclipseInstallCheckbox(container1);
		createEclipseInstallBrowse(container2);	
	}

	private void createEclipseInstallCheckbox(Composite container) {
		IDialogSettings settings = getDialogSettings();
		boolean isSelected = settings.getBoolean(EXTERNAL_ECLIPSE_CHECKBOX);
		fEclipseInstallCheckbox = new Button(container, SWT.CHECK);
		fEclipseInstallCheckbox.setText(Messages.getString("BuildToolWizardPluginPage_GenerateFromArchiveLabel")); //$NON-NLS-1$
		fEclipseInstallCheckbox.addSelectionListener(new CheckboxSelectionListener());
		fEclipseInstallCheckbox.setSelection(isSelected);
	}
	
	private void createEclipseInstallBrowse(Composite container) {
		IDialogSettings settings = getDialogSettings();
		String archiveLocation = not_null(settings.get(EXTERNAL_ECLIPSE_LOCATION), "");
		
		fEclipseArchiveLocationLabel = new Label(container, SWT.NONE);
		fEclipseArchiveLocationLabel.setText(Messages.getString("BuildToolWizardPluginPage_ExternalArchiveLocationLabel")); //$NON-NLS-1$
		GridData gdLabel = new GridData();
		gdLabel.horizontalIndent = LABEL_WIDTH;
		fEclipseArchiveLocationLabel.setLayoutData(gdLabel);
		
		fEclipseArchiveLocationText = new Text(container, SWT.BORDER | SWT.SINGLE);
		fEclipseArchiveLocationText.setText(archiveLocation);
		fEclipseArchiveLocationText.addModifyListener(new TextModifyListener());
		GridData gdText = new GridData(GridData.FILL_HORIZONTAL);
		gdText.widthHint = TEXTBOX_WIDTH;
		fEclipseArchiveLocationText.setLayoutData(gdText);
		
		
		fEclipseArchiveBrowseButton = new Button(container, SWT.PUSH);
		fEclipseArchiveBrowseButton.setText(Messages.getString("BuildToolWizardPluginPage_BrowseButton")); //$NON-NLS-1$

	    Listener listener = new Listener() {
			public void handleEvent(Event event) {
	    		FileDialog dialog = new FileDialog(getContainer().getShell(), SWT.OPEN);
	    		dialog.setText(Messages.getString("BuildToolWizardPluginPage_SelectEclipseArchiveDialogTitle")); //$NON-NLS-1$
	    		dialog.setFilterExtensions(fEclipseArchiveExtensions ); //$NON-NLS-1$
	    		String selectedFileName = dialog.open();
	    		if (selectedFileName != null) {
	    			fEclipseArchiveLocationText.setText(selectedFileName);
	    		}
	        }
	      };

	    fEclipseArchiveBrowseButton.addListener(SWT.Selection, listener);
	    enableEclipseInstallLocationArea(false);
	}	
	
	private void textSelectionChanged() {
		setPageComplete(fEclipseArchiveLocationText.getText().length() > 0);
	}
	
	private void checkboxSelectionChanged() {
		if(fEclipseInstallCheckbox.getSelection()) {			
			enableEclipseInstallLocationArea(true);
			fPluginFilter.setEnabled(false);
			
			setPageComplete(fEclipseArchiveLocationText.getText().length() > 0);
		}
		else {
			enableEclipseInstallLocationArea(false);
			fPluginFilter.setEnabled(true);
			
			setPageComplete(fPluginFilter.getSelectedViewer().getTable().getItemCount() > 0);
		}
	}
	
	private void enableEclipseInstallLocationArea(boolean enable) {
		fEclipseArchiveLocationText.setEnabled(enable);
		fEclipseArchiveBrowseButton.setEnabled(enable);
	}
	
	public String getEclipseArchiveLocation() {
		return this.fEclipseArchiveLocationText.getText();
	}
	
	public Object[] getSelectedPlugins() {
		return this.fPluginFilter.getSelected();
	}
	
	public boolean isGeneratingFromPlugins() {
		return !fEclipseInstallCheckbox.getSelection();
	}
	
	public boolean isCurrentPage() {
		return super.isCurrentPage();
	}
	
	@Override
	public boolean canFlipToNextPage() {
		if(!fEclipseInstallCheckbox.getSelection() && fPluginFilter.hasSelectedItems()){
			return true;
		}
		else if(fEclipseInstallCheckbox.getSelection() && fEclipseArchiveLocationText.getText().length() > 0) {
			return true;
		}
		return false;
	}
	
	@Override
	public void storeSettings() {
		IDialogSettings settings = getDialogSettings();
		settings.put(EXTERNAL_ECLIPSE_CHECKBOX, fEclipseInstallCheckbox.getSelection());
		settings.put(EXTERNAL_ECLIPSE_LOCATION, fEclipseArchiveLocationText.getText());
		settings.put(FILTER_PATTERN, fPluginFilter.fFilterText.getText());
	}
}