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

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.progress.WorkbenchJob;

public class FilteredListComponent {

	private Button fAddButton;
	private Button fAddAllButton;
	private Button fRemoveButton;
	private Button fRemoveAllButton;
	private Label fCountLabel;
	
	protected WorkbenchJob fFilterJob;
	protected Text fFilterText;
	protected ListFilter fFilter;
	
	private boolean fBlockSelectionListeners;
	
	protected TableViewer fAvailableViewer;
	protected TableViewer fSelectedViewer;
	
	protected HashMap<Object, Object> fSelected;
	protected Composite fHighLevelContainer;
	
	protected LabelProvider fLabelProvider;
	protected boolean fEnabled;
	
	protected BuildToolWizardPage fParentPage;
	protected BuildToolModelTable fModelTable;
	
	protected class ContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return fModelTable.getModels();
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	protected class SelectedContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return fModelTable.getPreSelected();
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}
	
	protected FilteredListComponent(BuildToolModelTable modelTable, LabelProvider labelProvider, BuildToolWizardPage parentPage) {
		this.fModelTable = modelTable;
		this.fLabelProvider = labelProvider;
		this.fParentPage = parentPage;
		
		fEnabled = true;
		fSelected = new HashMap<Object, Object>();
	}
	
	protected GridLayout getComponentLayout() {
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.makeColumnsEqualWidth = false;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 20;
		return layout;
	}
	
	private GridLayout getViewerLayout() {
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		return layout;
	}
	
	public Composite createFilteredListComponent(Composite parent) {	
		GridLayout gridLayout = getComponentLayout();
		
		fHighLevelContainer = new Composite(parent, SWT.NONE);
		fHighLevelContainer.setLayout(gridLayout);

		// Create filter area component
		createFilterArea(fHighLevelContainer);
		
		// Create available plug-in list component
		createAvailableList(fHighLevelContainer);
		
		// Create control button area component
		createControlButtonArea(fHighLevelContainer);
		
		// Create selected plug-in list component
		createSelectedList(fHighLevelContainer);
		
		// Add viewer listeners
		addViewerListeners();
		
		addFilter();
		
		updateCount();
		
		initialize();
		
		return fHighLevelContainer;
	}
	
	private void addFilter() {
		fFilter = new ListFilter(fSelected, fLabelProvider);
		fAvailableViewer.addFilter(fFilter);
		fFilterJob = new WorkbenchJob("FilterJob") { //$NON-NLS-1$
			public IStatus runInUIThread(IProgressMonitor monitor) {
				handleFilter();
				return Status.OK_STATUS;
			}
		};
		fFilterJob.setSystem(true);
	}
	
	protected Composite createFilterArea(Composite parent) {
		Group container = createFilterContainer(parent);
		fFilterText = createFilterText(container, ""); //$NON-NLS-1$
		return container;
	}
	
	private Group createFilterContainer(Composite parent) {
		Group container = new Group(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = layout.marginHeight = 6;
		container.setLayout(layout);

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		container.setLayoutData(gd);
		container.setText(Messages.getString("FilteredListComponent_FilterAvailableListLabel")); //$NON-NLS-1$
		return container;
	}
	
	private Text createFilterText(Composite parent, String initial) {
		Label filter = new Label(parent, SWT.NONE);
		filter.setText(Messages.getString("FilteredListComponent_FilterDescriptionLabel")); //$NON-NLS-1$
		
		Text text = new Text(parent, SWT.BORDER);
		text.setText(initial);
		
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		text.setLayoutData(gd);
		return text;
	}
	
	protected Composite createAvailableList(Composite parent) {
		Composite container = createViewerContainer(parent, Messages.getString("FilteredListComponent_AvailableListLabel")); //$NON-NLS-1$
		fAvailableViewer = createTableViewer(container, new ContentProvider(), fModelTable.getModels());
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		if(fModelTable.getModels().length > 0) {
			fAvailableViewer.getTable().setSelection(0);
		}
		return container;
	}
	
	/**
	 * Handles changes to the list based on changes to the text field.
	 */
	void handleFilter() {
		boolean changed = false;
		String newFilter;
		if (fFilterText == null || (newFilter = fFilterText.getText().trim()).length() == 0)
			newFilter = "*"; //$NON-NLS-1$
		changed = fFilter.setPattern(newFilter);
		if (changed) {
			fAvailableViewer.getTable().setRedraw(false);
			fAvailableViewer.refresh();
			fAvailableViewer.getTable().setRedraw(true);
			if (fEnabled) {
				updateButtonEnablement(false, false);
				updateCount();
			}
		}
	}
	
	protected Composite createSelectedList(Composite parent) {
		Composite container = createViewerContainer(parent, Messages.getString("FilteredListComponent_SelectedListLabel")); //$NON-NLS-1$
		fSelectedViewer = createTableViewer(container, new SelectedContentProvider(), fModelTable.getPreSelected());
		if(fModelTable.getPreSelected().length > 0) {
			fSelectedViewer.getTable().setSelection(0);
		}
		return container;
	}
	
	private Composite createViewerContainer(Composite parent, String message) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = getViewerLayout();
		container.setLayout(gridLayout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label label = new Label(container, SWT.NONE);
		label.setText(message);
		return container;
	}
	
	private TableViewer createTableViewer(Composite container, IContentProvider provider, Object[] input) {
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 200;
		gd.widthHint = 225;
		
		Table table = new Table(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		table.setLayoutData(gd);

		TableViewer viewer = new TableViewer(table);
		viewer.setLabelProvider(fLabelProvider);
		viewer.setContentProvider(provider);
		viewer.setInput(input);
		viewer.setComparator(new ViewerComparator());
		return viewer;
	}
	
	protected Composite createControlButtonArea(Composite parent) {
		ScrolledComposite comp = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		comp.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		Composite container = new Composite(comp, SWT.NONE);
		layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginTop = 50;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_VERTICAL);
		gd.verticalIndent = 15;
		container.setLayoutData(gd);

		fAddButton = new Button(container, SWT.PUSH);
		fAddButton.setText(Messages.getString("FilteredListComponent_AddButton")); //$NON-NLS-1$
		fAddButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fAddButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAdd();
			}
		});

		fAddAllButton = new Button(container, SWT.PUSH);
		fAddAllButton.setText(Messages.getString("FilteredListComponent_AddAllButton")); //$NON-NLS-1$
		fAddAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fAddAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAddAll();
			}
		});

		fRemoveButton = new Button(container, SWT.PUSH);
		fRemoveButton.setText(Messages.getString("FilteredListComponent_RemoveButton")); //$NON-NLS-1$
		fRemoveButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});

		fRemoveAllButton = new Button(container, SWT.PUSH);
		fRemoveAllButton.setText(Messages.getString("FilteredListComponent_RemoveAllButton")); //$NON-NLS-1$
		fRemoveAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fRemoveAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemoveAll();
			}
		});

		fCountLabel = new Label(container, SWT.NONE);
		fCountLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
		comp.setContent(container);
		comp.setMinHeight(250);
		comp.setExpandHorizontal(true);
		comp.setExpandVertical(true);
		return container;
	}
	
	private void handleAdd() {
		IStructuredSelection ssel = (IStructuredSelection) fAvailableViewer.getSelection();
		if (ssel.size() > 0) {
			Table table = fAvailableViewer.getTable();
			int index = table.getSelectionIndices()[0];
			Object[] selection = ssel.toArray();
			setBlockSelectionListeners(true);
			setRedraw(false);
			for (int i = 0; i < selection.length; i++) {
				doAdd(selection[i]);
			}
			setRedraw(true);
			setBlockSelectionListeners(false);
			table.setSelection(index < table.getItemCount() ? index : table.getItemCount() - 1);
		}
	}

	private void handleAddAll() {
		ArrayList<Object> data = new ArrayList<Object>();
		for(TableItem item: fAvailableViewer.getTable().getItems()) {
			data.add(item.getData());
		}
		
		if (data.size() > 0) {
			Object[] datas = data.toArray();
			setBlockSelectionListeners(true);
			setRedraw(false);
			for (int i = 0; i < datas.length; i++) {
				doAdd(datas[i]);
			}
			setRedraw(true);
			setBlockSelectionListeners(false);
		}
	}

	private void handleRemove() {
		IStructuredSelection ssel = (IStructuredSelection) fSelectedViewer.getSelection();
		if (ssel.size() > 0) {
			Table table = fSelectedViewer.getTable();
			int index = table.getSelectionIndices()[0];
			Object[] selection = ssel.toArray();
			setBlockSelectionListeners(true);
			setRedraw(false);
			for (int i = 0; i < selection.length; i++) {
				doRemove(selection[i]);
			}
			setRedraw(true);
			setBlockSelectionListeners(false);
			table.setSelection(index < table.getItemCount() ? index : table.getItemCount() - 1);
		}
	}

	private void doAdd(Object o) {
		fModelTable.removeModel(o);
		fModelTable.addToPreselected(o);
		fSelectedViewer.add(o);
		fAvailableViewer.remove(o);
		fSelected.put(o, null);
		listChanged(true, false);
	}

	private void doRemove(Object o) {
		fModelTable.addModel(o);
		fModelTable.removeFromPreselected(o);
		fSelected.remove(o);
		fSelectedViewer.remove(o);
		fAvailableViewer.add(o);
		listChanged(false, true);
	}
	
	private void listChanged(boolean doAddEnablement, boolean doRemoveEnablement) {
		updateCount();
		updateButtonEnablement(doAddEnablement, doRemoveEnablement);
		fParentPage.setPageComplete(fSelectedViewer.getTable().getItemCount() > 0);
	}
	
	public boolean hasSelectedItems() {
		if (fSelectedViewer.getTable().getItems().length > 0) {
			return true;
		}
		return false;
	}

	private void handleRemoveAll() {
		ArrayList<Object> data = new ArrayList<Object>();
		for(TableItem item: fSelectedViewer.getTable().getItems()) {
			data.add(item.getData());
		}
		if (data.size() > 0) {
			Object[] datas = data.toArray();
			setBlockSelectionListeners(true);
			setRedraw(false);
			for (int i = 0; i < datas.length; i++) {
				doRemove(datas[i]);
			}
			setRedraw(true);
			setBlockSelectionListeners(false);
		}
	}
	
	private void setBlockSelectionListeners(boolean blockSelectionListeners) {
		fBlockSelectionListeners = blockSelectionListeners;
	}
	
	private void setRedraw(boolean redraw) {
		fAvailableViewer.getTable().setRedraw(redraw);
		fSelectedViewer.getTable().setRedraw(redraw);
	}
	
	protected void updateCount() {
		Integer numAvailable = new Integer(fAvailableViewer.getTable().getItemCount() + fSelectedViewer.getTable().getItemCount());
		Integer numSelected = new Integer(fSelectedViewer.getTable().getItemCount());
		
		fCountLabel.setText(numSelected.toString() + " of " + numAvailable.toString() + " selected"); //$NON-NLS-1$ //$NON-NLS-2$
		fCountLabel.getParent().layout();
	}

	protected void updateButtonEnablement(boolean doAddEnablement, boolean doRemoveEnablement) {
		int availableCount = fAvailableViewer.getTable().getItemCount();
		int selectedCount = fSelectedViewer.getTable().getItemCount();

		fAddAllButton.setEnabled(availableCount > 0);
		fRemoveAllButton.setEnabled(selectedCount > 0);
		
		if(!(availableCount>0) && fAddButton.isEnabled()) {
			fAddButton.setEnabled(false);
		}
		if(!(selectedCount>0) && fRemoveButton.isEnabled()) {
			fRemoveButton.setEnabled(false);
		}
	}
	
	private void updateSelectionBasedEnablement(ISelection theSelection, boolean available) {
		if (available) {
			fAddButton.setEnabled(!theSelection.isEmpty());
		}
		else {
			fRemoveButton.setEnabled(!theSelection.isEmpty());
		}
	}
	
	protected void initialize() {
		if (fEnabled) {
			updateButtonEnablement(true, true);
		}
		//setPageComplete(false);
	}
	
	protected void addViewerListeners() {
		fAvailableViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleAdd();
			}
		});

		fSelectedViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleRemove();
			}
		});

		fAvailableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (!fBlockSelectionListeners)
					updateSelectionBasedEnablement(event.getSelection(), true);
			}
		});

		fSelectedViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (!fBlockSelectionListeners)
					updateSelectionBasedEnablement(event.getSelection(), false);
			}
		});

		fFilterText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fFilterJob.cancel();
				fFilterJob.schedule(0);
			}
		});
	}
	
	public void setEnabled(boolean enabled) {
		fFilterText.setEnabled(enabled);
		fAddButton.setEnabled(enabled);
		fRemoveButton.setEnabled(enabled);
		fAddAllButton.setEnabled(enabled);
		fRemoveAllButton.setEnabled(enabled);
		fAvailableViewer.getControl().setEnabled(enabled);
		fSelectedViewer.getControl().setEnabled(enabled);
		if (enabled) {
			updateButtonEnablement(false, false);
			//fParentPage.setPageComplete(fSelectedViewer.getTable().getItemCount() > 0);
		}
		fEnabled = enabled;
	}
	
	public TableViewer getSelectedViewer() {
		return fSelectedViewer;
	}
	
	public Object[] getSelected() {
		return fModelTable.getPreSelected();
	}
	
	public boolean isEnabled() {
		return fEnabled;
	}
}
