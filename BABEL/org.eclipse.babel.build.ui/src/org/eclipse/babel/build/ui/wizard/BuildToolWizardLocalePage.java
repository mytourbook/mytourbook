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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

public class BuildToolWizardLocalePage extends BuildToolWizardPage {

	private final static String FILTER_PATTERN = "BuildToolWizardLocalePage#fLocaleFilter";
	private final static String SELECTED_LOCALES = "BuildToolWizardLocalePage#Locales";
	private final static String ALL_LOCALES = "BuildToolWizardLocalePage#fAllLocalesCheckbox";
	private final static String BY_GROUP = "BuildToolWizardLocalePage#fLocaleGroupCheckbox";
	private final static String SELECTED_GROUP = "BuildToolWizardLocalePage#fLocaleGroupCombo";
	
	private BuildToolModelTable fModelLocalesTable;
	
	private Button fAllLocalesCheckbox;
	private Button fLocaleGroupCheckbox;
	private Label fLocaleGroupLabel;
	private Combo fLocaleGroupCombo;
	
	private FilteredListComponent fLocaleFilter;
	
	private class GroupCheckboxSelectionListener implements SelectionListener {
		public void widgetSelected(SelectionEvent e){
			groupCheckboxSelectionChanged();
		}

		public void widgetDefaultSelected(SelectionEvent e){
		}
	}
	
	private class AllLocalesCheckboxSelectionListener implements SelectionListener {
		public void widgetSelected(SelectionEvent e){
			allLocalesCheckboxSelectionChanged();
		}

		public void widgetDefaultSelected(SelectionEvent e){
		}
	}
	
	protected BuildToolWizardLocalePage(String pageName, BuildToolModelTable modelLocalesTable) {
		super(pageName);
		this.fModelLocalesTable = modelLocalesTable;
		
		this.setTitle(Messages.getString("BuildToolWizardLocalePage_PageTitle")); //$NON-NLS-1$
		this.setDescription(Messages.getString("BuildToolWizardLocalePage_PageDescription")); //$NON-NLS-1$
	}

	@Override
	public void createControl(Composite parent) {		
		// Create filtered list component
		fLocaleFilter = new FilteredListComponent(fModelLocalesTable, new LocaleLabelProvider(), this);
		Composite container = fLocaleFilter.createFilteredListComponent(parent);
		
		createAllLocalesCheckbox(container);
		
		createLocaleGroupArea(container);
		
		setControl(container);
		Dialog.applyDialogFont(container);
		
		allLocalesCheckboxSelectionChanged();
		groupCheckboxSelectionChanged();
		
		try {
			int selectedIndex = getDialogSettings().getInt(SELECTED_GROUP);
			fLocaleGroupCombo.select(selectedIndex);
		} catch(NumberFormatException e) { }
		
		String pattern = not_null(getDialogSettings().get(FILTER_PATTERN), "");
		fLocaleFilter.fFilterText.setText(pattern);
	}

	private void createAllLocalesCheckbox(Composite container) {
		fAllLocalesCheckbox = new Button(container, SWT.CHECK);
		GridData bLayout = new GridData();
		bLayout.horizontalIndent = 6;
		bLayout.horizontalSpan = 3;
		fAllLocalesCheckbox.setLayoutData(bLayout);
		fAllLocalesCheckbox.setText(Messages.getString("BuildToolWizardLocalePage_AllLocaleCheckbox"));
		fAllLocalesCheckbox.addSelectionListener(new AllLocalesCheckboxSelectionListener());
		fAllLocalesCheckbox.setSelection(getDialogSettings().getBoolean(ALL_LOCALES));
	}
	
	private void createLocaleGroupArea(Composite parent) {				
		Group container = new Group(parent, SWT.NONE);
		FillLayout layout = new FillLayout(SWT.VERTICAL);
		layout.marginHeight = layout.marginWidth = 12;
		container.setLayout(layout);

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		container.setLayoutData(gd);
		
		Composite container1 = new Composite(container, SWT.NONE);
		container1.setLayout(new GridLayout(1, false));
		
		Composite container2 = new Composite(container, SWT.NONE);
		container2.setLayout(new GridLayout(3, false));
		
		createLocaleGroupCheckbox(container1);
		createLocaleGroupDropdown(container2);	
	}
	
	private void createLocaleGroupCheckbox(Composite container) {			
		fLocaleGroupCheckbox = new Button(container, SWT.CHECK);
		fLocaleGroupCheckbox.setText(Messages.getString("BuildToolWizardLocalePage_GenerateForLocaleGroupLabel")); //$NON-NLS-1$
		fLocaleGroupCheckbox.addSelectionListener(new GroupCheckboxSelectionListener());
		fLocaleGroupCheckbox.setSelection(getDialogSettings().getBoolean(BY_GROUP));
	}
	
	private void createLocaleGroupDropdown(Composite container) {				
		fLocaleGroupLabel = new Label(container, SWT.NONE);
		fLocaleGroupLabel.setText(Messages.getString("BuildToolWizardLocalePage_LocaleGroupLabel")); //$NON-NLS-1$
		GridData gdLabel = new GridData();
		gdLabel.horizontalIndent = 30;
		fLocaleGroupLabel.setLayoutData(gdLabel);
		
		fLocaleGroupCombo = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
		GridData gdText = new GridData(GridData.FILL_HORIZONTAL);
		gdText.widthHint = 300;
		fLocaleGroupCombo.setLayoutData(gdText);
		
		fLocaleGroupCombo.add("All groups"); //$NON-NLS-1$
		fLocaleGroupCombo.add("Group1: de, es, fr, it, ja, ko, pt_BR, zh, zh_HK, zh_TW"); //$NON-NLS-1$
		fLocaleGroupCombo.add("Group2: cs, hu, pl, ru"); //$NON-NLS-1$
		fLocaleGroupCombo.add("Group2a: da, el, fi, nl, no, pt, sv, tr"); //$NON-NLS-1$
		fLocaleGroupCombo.add("GroupBidi: ar, iw"); //$NON-NLS-1$
		fLocaleGroupCombo.setText("All groups"); //$NON-NLS-1$
		
//	    enableLocaleGroupArea(false);
	}
	
	private void groupCheckboxSelectionChanged() {
		if(fLocaleGroupCheckbox.getSelection()) {			
			enableLocaleGroupArea(true);
			enableLocaleArea(false);
			
			setPageComplete(true);
		}
		else {
			enableLocaleGroupArea(false);
			enableLocaleArea(true);
		}
	}
	
	private void allLocalesCheckboxSelectionChanged() {
		if(fAllLocalesCheckbox.getSelection()) {			
			fLocaleFilter.setEnabled(false);			
			setPageComplete(true);
		}
		else {
			fLocaleFilter.setEnabled(true);			
			setPageComplete(fLocaleFilter.getSelectedViewer().getTable().getItemCount() > 0);
		}
	}
	
	private void enableLocaleGroupArea(boolean enable) {
		fLocaleGroupCombo.setEnabled(enable);
	}
	
	private void enableLocaleArea(boolean enable) {
		if(enable) {
			fAllLocalesCheckbox.setEnabled(true);
			if(fAllLocalesCheckbox.getSelection()) {
				fLocaleFilter.setEnabled(false);
				setPageComplete(true);
			}
			else {
				fLocaleFilter.setEnabled(true);
				setPageComplete(fLocaleFilter.getSelectedViewer().getTable().getItemCount() > 0);
			}
		}
		else {
			fLocaleFilter.setEnabled(false);
			fAllLocalesCheckbox.setEnabled(false);
		}
	}
	
	public boolean isGeneratingFromLocales() {
		return !fLocaleGroupCheckbox.getSelection();
	}
	
	public boolean isGeneratingForAllLocales() {
		return fAllLocalesCheckbox.getSelection();
	}
	
	public Object[] getSelectedLocales() {
		return this.fLocaleFilter.getSelected();
	}
	
	public String getSelectedGroup() {
		return this.fLocaleGroupCombo.getText();
	}
	
	public boolean isCurrentPage() {
		return super.isCurrentPage();
	}
	
	public boolean canFlipToNextPage() {
		if(fAllLocalesCheckbox.getSelection() || fLocaleFilter.hasSelectedItems()){
			return true;
		}
		
		if(fLocaleGroupCheckbox.getSelection()) {
			return true;
		}
		return false;
	}
	
	@Override
	public void storeSettings() {
		Object[] objs = getSelectedLocales();
		String[] locales = new String[objs.length];
		int current = 0;
		for(Object o : objs){
			locales[current++] = o.toString();
		}		
		
		IDialogSettings settings = getDialogSettings();
		settings.put(FILTER_PATTERN, fLocaleFilter.fFilterText.getText());
		settings.put(SELECTED_LOCALES, locales);
		settings.put(ALL_LOCALES, fAllLocalesCheckbox.getSelection());
		settings.put(BY_GROUP, fLocaleGroupCheckbox.getSelection());
		
		int selectionIndex = fLocaleGroupCombo.getSelectionIndex();
		if (selectionIndex != -1) {
			settings.put(SELECTED_GROUP, fLocaleGroupCombo.getSelectionIndex());
		}
	}
}