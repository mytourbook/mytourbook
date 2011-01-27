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

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class BuildToolWizardConfigurationPage extends BuildToolWizardPage {

	private static final String[] STRING = new String[]{};
	private static final String XML_REPORT = "BuildToolWizardConfigurationPage#fXMLReportGeneration";
	private static final String LONG_REPORT = "BuildToolWizardConfigurationPage#fLongReportGeneration";
	private static final String TRANSLATION_CATALOG_LOCATION = "BuildToolWizardConfigurationPage#fTranslationCatalogueLocationText";
	private static final String WORKING_DIRECTORY = "BuildToolWizardConfigurationPage#fWorkingDirectoryLocationText";
	private static final String IGNORE_LIST = "BuildToolWizardConfigurationPage#IgnoreList";
	private static final String IGNORE_LIST_CHECKED = "BuildToolWizardConfigurationPage#IgnoreListChecked";
	private static final String EXCLUDE_LIST = "BuildToolWizardConfigurationPage#ExcludeList";
	private static final String EXCLUDE_LIST_CHECKED = "BuildToolWizardConfigurationPage#ExcludeListChecked";
	
	
	private static final int VIEWER_WIDTH = 450;
	private static final int VIEWER_HEIGHT = 110;
	private static final int LIST_LABEL_INDENT = 6;
	private static final int BUTTON_WIDTH = 110;
	private static final int LIST_SEPARATOR = 20;
	private static final int TEXT_WIDTH = 325;
	private static final int LABEL_WIDTH = 190;
	
	private BuildToolModelTable fModelIgnoreTable;
	private BuildToolModelTable fModelResourceExclusionTable;
	
	private Button fTranslationCatalogueBrowseButton;
	private Button fWorkingDirectoryBrowseButton;
	
	private Label fWorkingDirectoryLocationLabel;
	private Label fTranslationCatalogueLocationLabel;
	
	private Text fWorkingDirectoryLocationText;
	private Text fTranslationCatalogueLocationText;

	private CheckboxTableViewer fLanguagePackResourceIgnoreViewer;
	private Label fLanguagePackListLabel;
	private Button fAddLanguagePackFilterButton;
	private Button fRemoveLanguagePackFilterButton;
	private String fChosenLanguagePackFilter;
	
	private CheckboxTableViewer fCoverageReportResourceFilterViewer;
	private Label fCoverageReportListLabel;
	private Button fAddCoverageReportFilterButton;
	private Button fRemoveCoverageReportFilterButton;
	private String fChosenCoverageReportFilter;
	
	private LanguagePackFilterDialog fLanguagePackFilterDialog;
	private CoverageReportFilterDialog fCoverageReportFilterDialog;
	private Button fLongReportGeneration;
	private Button fXMLReportGeneration;
	
	private static ToT<Object,String> TO_STRING = new ToT<Object, String>() {
		public String convert(Object from) {
			return from.toString();
		}
	};

	private class TextModifyListener implements ModifyListener {
		public void modifyText(ModifyEvent e) {
			textSelectionChanged();
		}
	}
	
	protected class IgnoreListContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return fModelIgnoreTable.getModels();
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}
	
	protected class ExcludeListContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return fModelResourceExclusionTable.getModels();
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}
	
	protected BuildToolWizardConfigurationPage(String pageName, BuildToolModelTable modelIgnoreTable, BuildToolModelTable modelExcludeTable) {
		super(pageName);
		this.fModelIgnoreTable = modelIgnoreTable;
		this.fModelResourceExclusionTable = modelExcludeTable;
		
		this.setTitle(Messages.getString("BuildToolWizardConfigurationPage_PageTitle")); //$NON-NLS-1$
		this.setDescription(Messages.getString("BuildToolWizardConfigurationPage_PageDescription")); //$NON-NLS-1$
	}
	
	public void createControl(Composite parent) {		
		// Create a scrollable container to hold components
		// This is useful when users are using a lower resolution.
		ScrolledComposite comp = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		Composite container = new Composite(comp, SWT.NONE);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
        
		createConfigurationArea(container);
		
		createLanguagePackFilteredListArea(container);
		
		createCoverageReportFilteredListArea(container);
		
		setControl(container);
		Dialog.applyDialogFont(container);
		setPageComplete(!"".equals(fWorkingDirectoryLocationText.getText()) && !"".equals(fTranslationCatalogueLocationText.getText()));
		
		// Scrollable container properties
		comp.setContent(container);
		comp.setMinHeight(500);
		comp.setExpandHorizontal(true);
		comp.setExpandVertical(true);
	}

	private void createConfigurationArea(Composite parent) {								
		// Create a checkbox allowing for lightweight report generation
		Composite longReportContainer = new Composite(parent, SWT.NONE);
		longReportContainer.setLayout(new GridLayout(1, false));
		longReportContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		createLongReportCheckbox(longReportContainer);
		
		// Create a checkbox allowing for xml report generation
		Composite xmlReportContainer = new Composite(parent, SWT.NONE);
		xmlReportContainer.setLayout(new GridLayout(1, false));
		xmlReportContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		createXMLReportCheckbox(xmlReportContainer);
		
		// Create a place for specifying working directory
		Composite workingDirectoryContainer = new Composite(parent, SWT.NONE);
		workingDirectoryContainer.setLayout(new GridLayout(3, false));
		workingDirectoryContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		createWorkingDirectoryBrowse(workingDirectoryContainer);
		
		// Create a place for specifying translation archive
		Composite translationArchiveContainer = new Composite(parent, SWT.NONE);
		translationArchiveContainer.setLayout(new GridLayout(3, false));
		translationArchiveContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		createTranslationArchiveLocationBrowse(translationArchiveContainer);
	}

	private void createLongReportCheckbox(Composite parent) {
		fLongReportGeneration = new Button(parent, SWT.CHECK);
		fLongReportGeneration.setText(Messages.getString("BuildToolWizardConfigurationPage_GenerateLongReportLabel"));
		fLongReportGeneration.setSelection(getDialogSettings().getBoolean(LONG_REPORT));
	}
	
	private void createXMLReportCheckbox(Composite parent) {
		fXMLReportGeneration = new Button(parent, SWT.CHECK);
		fXMLReportGeneration.setText(Messages.getString("BuildToolWizardConfigurationPage_GenerateXMLReportLabel"));
		fXMLReportGeneration.setSelection(getDialogSettings().getBoolean(XML_REPORT));
	}
	
	public boolean getLongReportSelection() {
		return fLongReportGeneration.getSelection();
	}
	
	public boolean getXmlReportSelection() {
		return fXMLReportGeneration.getSelection();
	}
	
	private void createTranslationArchiveLocationBrowse(Composite parent) {				
		fTranslationCatalogueLocationLabel = new Label(parent, SWT.NULL);
		fTranslationCatalogueLocationLabel.setText(Messages.getString("BuildToolWizardConfigurationPage_TranslationCatalogueLabel")); //$NON-NLS-1$
		GridData gdLabel = new GridData();
		gdLabel.widthHint = LABEL_WIDTH;
		fTranslationCatalogueLocationLabel.setLayoutData(gdLabel);
		
		fTranslationCatalogueLocationText = new Text(parent, SWT.BORDER | SWT.SINGLE);
		String location = not_null(getDialogSettings().get(TRANSLATION_CATALOG_LOCATION), "");
		fTranslationCatalogueLocationText.setText(location);
		fTranslationCatalogueLocationText.addModifyListener(new TextModifyListener());
		GridData gdText = new GridData(GridData.FILL_HORIZONTAL);
		gdText.widthHint = TEXT_WIDTH;
		fTranslationCatalogueLocationText.setLayoutData(gdText);
		
		fTranslationCatalogueBrowseButton = new Button(parent, SWT.PUSH);
		fTranslationCatalogueBrowseButton.setText(Messages.getString("BuildToolWizardConfigurationPage_TranslationCatalogueBrowseButton")); //$NON-NLS-1$

	    Listener listener = new Listener() {
	        public void handleEvent(Event event) {
	    		DirectoryDialog dialog = new DirectoryDialog(getContainer().getShell());
	    		dialog.setText(Messages.getString("BuildToolWizardConfigurationPage_TranslationCatalogueDialogTitle")); //$NON-NLS-1$
	    		dialog.setMessage(Messages.getString("BuildToolWizardConfigurationPage_TranslationCatalogueDialogMessage")); //$NON-NLS-1$
	    		String selectedFileName = dialog.open();
	    		if (selectedFileName != null) {
	    			fTranslationCatalogueLocationText.setText(selectedFileName);
	    		}
	        }
	    };

	    fTranslationCatalogueBrowseButton.addListener(SWT.Selection, listener);
	}		
	
	private void createWorkingDirectoryBrowse(Composite parent) {				
		fWorkingDirectoryLocationLabel = new Label(parent, SWT.NULL);
		fWorkingDirectoryLocationLabel.setText(Messages.getString("BuildToolWizardConfigurationPage_WorkingDirectoryLabel")); //$NON-NLS-1$
		GridData gdLabel = new GridData();
		gdLabel.widthHint = LABEL_WIDTH;
		fWorkingDirectoryLocationLabel.setLayoutData(gdLabel);
		
		fWorkingDirectoryLocationText = new Text(parent, SWT.BORDER | SWT.SINGLE);
		String location = not_null(getDialogSettings().get(WORKING_DIRECTORY), "");
		fWorkingDirectoryLocationText.setText(location);
		fWorkingDirectoryLocationText.addModifyListener(new TextModifyListener());
		GridData gdText = new GridData(GridData.FILL_HORIZONTAL);
		gdText.widthHint = TEXT_WIDTH;
		fWorkingDirectoryLocationText.setLayoutData(gdText);
		
		fWorkingDirectoryBrowseButton = new Button(parent, SWT.PUSH);
		fWorkingDirectoryBrowseButton.setText(Messages.getString("BuildToolWizardConfigurationPage_WorkingDirectoryBrowseButton")); //$NON-NLS-1$

	    Listener listener = new Listener() {
	        public void handleEvent(Event event) {
	    		DirectoryDialog dialog = new DirectoryDialog(getContainer().getShell());
	    		dialog.setText(Messages.getString("BuildToolWizardConfigurationPage_WorkingDirectoryDialogTitle")); //$NON-NLS-1$
	    		dialog.setMessage(Messages.getString("BuildToolWizardConfigurationPage_WorkingDirectoryDialogMessage")); //$NON-NLS-1$
	    		String selectedFileName = dialog.open();
	    		if (selectedFileName != null) {
	    			fWorkingDirectoryLocationText.setText(selectedFileName);
	    		}
	        }
	      };

	    fWorkingDirectoryBrowseButton.addListener(SWT.Selection, listener);
	}
	
	private void createLanguagePackFilteredListArea(Composite parent) {
        fLanguagePackListLabel = new Label(parent, SWT.NONE);
        fLanguagePackListLabel.setText(Messages.getString("BuildToolWizardConfigurationPage_IgnoreListLabel")); //$NON-NLS-1$
        GridData gdLabel = new GridData(GridData.FILL_HORIZONTAL);
		gdLabel.widthHint = LABEL_WIDTH;
		gdLabel.horizontalIndent = LIST_LABEL_INDENT;
		gdLabel.verticalIndent = LIST_SEPARATOR;
		fLanguagePackListLabel.setLayoutData(gdLabel);
		
		Composite listComposite = new Composite(parent, SWT.NONE);
		listComposite.setLayout(new GridLayout(2, false));
		listComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
        Table table = new Table(listComposite, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        table.setLayout(new TableLayout());
        GridData data = new GridData(GridData.FILL_BOTH);
        data.heightHint = VIEWER_HEIGHT;
        data.widthHint = VIEWER_WIDTH;
        table.setLayoutData(data);
        
        fLanguagePackResourceIgnoreViewer = new CheckboxTableViewer(table);
        fLanguagePackResourceIgnoreViewer.setLabelProvider(new LabelProvider());
        fLanguagePackResourceIgnoreViewer.setContentProvider(new IgnoreListContentProvider());
        fLanguagePackResourceIgnoreViewer.setInput(fModelIgnoreTable.getModels());
        fLanguagePackResourceIgnoreViewer.setComparator(new ViewerComparator());
        fLanguagePackResourceIgnoreViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				// Enable button only if there's a selection
				if (event.getSelection().isEmpty()) {
					fRemoveLanguagePackFilterButton.setEnabled(false);
				} else {
					fRemoveLanguagePackFilterButton.setEnabled(true);
				}
			}	
        });
        
        // By default, all pre-defined patterns are checked
        fLanguagePackResourceIgnoreViewer.setAllChecked(true);
        
        createLanguagePackFilteredListControlButtonsArea(listComposite);
	}
	
	private void createLanguagePackFilteredListControlButtonsArea(Composite parent) {
		Composite buttonComposite = new Composite(parent, SWT.NONE);
        buttonComposite.setLayout(new GridLayout(1, false));
        GridData gData = new GridData();
        buttonComposite.setLayoutData(gData);
        
		GridData gdText = new GridData();
		gdText.widthHint = BUTTON_WIDTH;
		
		// Add Pattern Button
        fAddLanguagePackFilterButton = new Button(buttonComposite, SWT.PUSH);
        fAddLanguagePackFilterButton.setText(Messages.getString("BuildToolWizardConfigurationPage_AddLanguagePackFilterButton"));
        fAddLanguagePackFilterButton.setLayoutData(gdText);
        fAddLanguagePackFilterButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				openLanguagePackFilterDialog();
			}
		});
        
        // Remove Pattern Button
        fRemoveLanguagePackFilterButton = new Button(buttonComposite, SWT.PUSH);
        fRemoveLanguagePackFilterButton.setText(Messages.getString("BuildToolWizardConfigurationPage_RemoveLanguagePackFilterButton"));
        fRemoveLanguagePackFilterButton.setLayoutData(gdText);
        fRemoveLanguagePackFilterButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				removeSelectedLanguagePackFilter();
			}
		});
        fRemoveLanguagePackFilterButton.setEnabled(false);
        
        Button selectAll = new Button(buttonComposite, SWT.PUSH);
		selectAll.setText(Messages.getString("BuildToolWizardConfigurationPage_SelectAllLanguagePackFiltersButton"));
		selectAll.setLayoutData(gdText);
		selectAll.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				fLanguagePackResourceIgnoreViewer.setAllChecked(true);
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		Button deselectAll = new Button(buttonComposite, SWT.PUSH);
		deselectAll.setText(Messages.getString("BuildToolWizardConfigurationPage_DeselectAllLanguagePackFiltersButton"));
		deselectAll.setLayoutData(gdText);
		deselectAll.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				fLanguagePackResourceIgnoreViewer.setAllChecked(false);
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		IDialogSettings settings = getDialogSettings();
		for(String filter : not_null(settings.getArray(IGNORE_LIST), new String[]{})){
			addLanguagePackFilter(filter, false);
		}
		
		for(String checked : not_null(settings.getArray(IGNORE_LIST_CHECKED), new String[]{})){
			addLanguagePackFilter(checked, true);
		}
	}
	
	private void createCoverageReportFilteredListArea(Composite parent) {
		fCoverageReportListLabel = new Label(parent, SWT.NONE);
		fCoverageReportListLabel.setText(Messages.getString("BuildToolWizardConfigurationPage_FilterListLabel")); //$NON-NLS-1$
        GridData gdLabel = new GridData(GridData.FILL_HORIZONTAL);
		gdLabel.widthHint = LABEL_WIDTH;
		gdLabel.horizontalIndent = LIST_LABEL_INDENT;
		gdLabel.verticalIndent = LIST_SEPARATOR;
		fCoverageReportListLabel.setLayoutData(gdLabel);
		
		Composite listComposite = new Composite(parent, SWT.NONE);
		listComposite.setLayout(new GridLayout(2, false));
		listComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
        Table table = new Table(listComposite, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        table.setLayout(new TableLayout());
        GridData data = new GridData(GridData.FILL_BOTH);
        data.heightHint = VIEWER_HEIGHT;
        data.widthHint = VIEWER_WIDTH;
        table.setLayoutData(data);
        
        fCoverageReportResourceFilterViewer = new CheckboxTableViewer(table);
        fCoverageReportResourceFilterViewer.setLabelProvider(new LabelProvider());
        fCoverageReportResourceFilterViewer.setContentProvider(new ExcludeListContentProvider());
        fCoverageReportResourceFilterViewer.setInput(fModelResourceExclusionTable.getModels());
        fCoverageReportResourceFilterViewer.setComparator(new ViewerComparator());
        fCoverageReportResourceFilterViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				// Enable button only if there's a selection
				if (event.getSelection().isEmpty()) {
					fRemoveCoverageReportFilterButton.setEnabled(false);
				} else {
					fRemoveCoverageReportFilterButton.setEnabled(true);
				}
			}	
        });
        
        // By default, all pre-defined filters are checked
        fCoverageReportResourceFilterViewer.setAllChecked(true);
        
        // Create an area with buttons for adding/removing resource patterns, and for selecting/deselecting all
        createExcludeListControlButtonArea(listComposite);
        
		IDialogSettings settings = getDialogSettings();
		for(String filter : not_null(settings.getArray(EXCLUDE_LIST), new String[]{})){
			addCoverageReportFilter(filter, false);
		}
		
		for(String checked : not_null(settings.getArray(EXCLUDE_LIST_CHECKED), new String[]{})){
			addCoverageReportFilter(checked, true);
		}
	}
	
	private void createExcludeListControlButtonArea(Composite parent) {
		Composite buttonComposite = new Composite(parent, SWT.NONE);
        buttonComposite.setLayout(new GridLayout(1, false));
        GridData gData = new GridData();
        buttonComposite.setLayoutData(gData);
        
		GridData gdText = new GridData();
		gdText.widthHint = BUTTON_WIDTH;
		
		// Add Pattern Button
        fAddCoverageReportFilterButton = new Button(buttonComposite, SWT.PUSH);
        fAddCoverageReportFilterButton.setText(Messages.getString("BuildToolWizardConfigurationPage_AddCoverageReportFilterButton"));
        fAddCoverageReportFilterButton.setLayoutData(gdText);
        fAddCoverageReportFilterButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				openCoverageReportFilterDialog();
			}
		});
        
        // Remove Pattern Button
        fRemoveCoverageReportFilterButton = new Button(buttonComposite, SWT.PUSH);
        fRemoveCoverageReportFilterButton.setText(Messages.getString("BuildToolWizardConfigurationPage_RemoveCoverageReportFilterButton"));
        fRemoveCoverageReportFilterButton.setLayoutData(gdText);
        fRemoveCoverageReportFilterButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				removeSelectedCoverageReportFilter();
			}
		});
        fRemoveCoverageReportFilterButton.setEnabled(false);
        
        Button selectAll = new Button(buttonComposite, SWT.PUSH);
		selectAll.setText(Messages.getString("BuildToolWizardConfigurationPage_SelectAllCoverageReportFiltersButton"));
		selectAll.setLayoutData(gdText);
		selectAll.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				fCoverageReportResourceFilterViewer.setAllChecked(true);
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		Button deselectAll = new Button(buttonComposite, SWT.PUSH);
		deselectAll.setText(Messages.getString("BuildToolWizardConfigurationPage_DeselectAllCoverageReportFiltersButton"));
		deselectAll.setLayoutData(gdText);
		deselectAll.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				fCoverageReportResourceFilterViewer.setAllChecked(false);
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
	}
	
	public void setCoverageReportFilter(String filter) {
		this.fChosenCoverageReportFilter = filter;
	}
	
	public void setLanguagePackFilter(String filter) {
		this.fChosenLanguagePackFilter = filter;
	}
	
	private void openLanguagePackFilterDialog() {
		Shell shell = new Shell(this.getShell(), SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM);
		
		fLanguagePackFilterDialog = new LanguagePackFilterDialog(shell, this);
		fLanguagePackFilterDialog.getParent().open();
		fLanguagePackFilterDialog.getParent().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				// If a pattern was provided through the dialog, handle.
				if ((fChosenLanguagePackFilter != null) && (fChosenLanguagePackFilter.trim().compareTo("") != 0)) {
					addLanguagePackFilter(fChosenLanguagePackFilter);
				}
				fChosenLanguagePackFilter = null;
			}
		});
	}
	
	private void addCoverageReportFilter(String o){
		addCoverageReportFilter(o, true);
	}
	
	private void addCoverageReportFilter(String o, boolean checked) {
		boolean doesItemExist = false;
		
		// Check if extension is already in the ignore list
		for (TableItem item: fCoverageReportResourceFilterViewer.getTable().getItems()) {
			if (item.getText().compareToIgnoreCase(o) == 0) {
				doesItemExist = true;
			}
		}
		
		// Only add an item if its name does not conflict with an item
		// that is already in the list.
		if (!doesItemExist) {
			fModelResourceExclusionTable.addModel(o);
			fCoverageReportResourceFilterViewer.add(o);
			fCoverageReportResourceFilterViewer.refresh();
		}
		// Ensure new item is checked
		fCoverageReportResourceFilterViewer.setChecked(o, checked);
	}
	
	private void addLanguagePackFilter(String o){
		addLanguagePackFilter(o, true);
	}
	
	private void addLanguagePackFilter(String o, boolean checked) {
		boolean doesItemExist = false;
		
		// Check if extension is already in the ignore list
		for (TableItem item: fLanguagePackResourceIgnoreViewer.getTable().getItems()) {
			if (item.getText().compareToIgnoreCase(o) == 0) {
				doesItemExist = true;
			}
		}
		
		// Only add an item if its name does not conflict with an item
		// that is already in the list.
		if (!doesItemExist) {
			fModelIgnoreTable.addModel(o);
			fLanguagePackResourceIgnoreViewer.add(o);
			fLanguagePackResourceIgnoreViewer.refresh();
		}
		fLanguagePackResourceIgnoreViewer.setChecked(o, checked);
	}
	
	private void removeSelectedLanguagePackFilter() {
		int selectionIndex = fLanguagePackResourceIgnoreViewer.getTable().getSelectionIndex();
		if (selectionIndex != -1) {
			Object o = fLanguagePackResourceIgnoreViewer.getElementAt(selectionIndex);
			
			fModelIgnoreTable.removeModel(o);
			fLanguagePackResourceIgnoreViewer.remove(o);
			
			fLanguagePackResourceIgnoreViewer.refresh();
		}
	}
	
	private void removeSelectedCoverageReportFilter() {
		int selectionIndex = fCoverageReportResourceFilterViewer.getTable().getSelectionIndex();
		if (selectionIndex != -1) {
			Object o = fCoverageReportResourceFilterViewer.getElementAt(selectionIndex);
			
			fModelResourceExclusionTable.removeModel(o);
			fCoverageReportResourceFilterViewer.remove(o);
			
			fCoverageReportResourceFilterViewer.refresh();
		}
	}
	
	private void openCoverageReportFilterDialog() {
		Shell shell = new Shell(this.getShell(), SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM);
		
		// Start up custom AddPatternDialog which requests a custom pattern
		fCoverageReportFilterDialog = new CoverageReportFilterDialog(shell, this);
		fCoverageReportFilterDialog.getParent().open();
		fCoverageReportFilterDialog.getParent().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				// If a pattern was provided through the custom dialog, handle.
				if ((fChosenCoverageReportFilter != null) && (fChosenCoverageReportFilter.trim().compareTo("") != 0)) {
					addCoverageReportFilter(fChosenCoverageReportFilter);
				}
				fChosenCoverageReportFilter = null;
			}
		});
	}
	
	private void textSelectionChanged() {
		setPageComplete((fWorkingDirectoryLocationText.getText().length() > 0) 
				&& (fTranslationCatalogueLocationText.getText().length() > 0));
	}
	
	public String getWorkingDirectoryLocation() {
		return fWorkingDirectoryLocationText.getText();
	}
	
	public String getTranslationCatalogueLocation() {
		return fTranslationCatalogueLocationText.getText();
	}
	
	public Object[] getIgnoreList() {
		return fLanguagePackResourceIgnoreViewer.getCheckedElements();
	}
	
	public Object[] getReportFilterPatterns() {
		return fCoverageReportResourceFilterViewer.getCheckedElements();
	}
	
	public boolean isCurrentPage() {
		return super.isCurrentPage();
	}
	
	public boolean canFlipToNextPage() {
		return false;
	}
	
	private String[] getAllElements(CheckboxTableViewer viewer){
		List<String> elements = new LinkedList<String>();
		Object o;
		int i = 0;
		while((o = viewer.getElementAt(i++)) != null){
			elements.add(o.toString());
		}
		
		return elements.toArray(STRING);
	}
	
	private static interface ToT<F,T>{
		public T convert(F from);
	}
	
	private <F,T> T[] convert(T[] array, ToT<F,T> toT, F... fs){
		List<T> ts = new LinkedList<T>();
		
		for(F f : fs){
			ts.add(toT.convert(f));
		}
		
		return ts.toArray(array);
	}
	
	@Override
	public void storeSettings() {
		IDialogSettings settings = getDialogSettings();
		settings.put(XML_REPORT, fXMLReportGeneration.getSelection());
		settings.put(LONG_REPORT, fLongReportGeneration.getSelection());
		settings.put(TRANSLATION_CATALOG_LOCATION, fTranslationCatalogueLocationText.getText());
		settings.put(WORKING_DIRECTORY, fWorkingDirectoryLocationText.getText());
		settings.put(IGNORE_LIST, getAllElements(fLanguagePackResourceIgnoreViewer));
		settings.put(IGNORE_LIST_CHECKED, convert(STRING, TO_STRING, fLanguagePackResourceIgnoreViewer.getCheckedElements()));
		
		settings.put(EXCLUDE_LIST, getAllElements(fCoverageReportResourceFilterViewer));
		settings.put(EXCLUDE_LIST_CHECKED, convert(STRING, TO_STRING, fCoverageReportResourceFilterViewer.getCheckedElements()));
	}
}
