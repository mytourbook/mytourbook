/*******************************************************************************
 * Copyright (c) 2008 Nigel Westbury and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Nigel Westbury - initial API and implementation
 *    Remy Chi Jian Suen - Bug 226500 'Translate' button throws NPE if nothing is selected in the table
 *******************************************************************************/

package org.eclipse.babel.runtime.actions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.eclipse.babel.runtime.Activator;
import org.eclipse.babel.runtime.MenuAnalyzer;
import org.eclipse.babel.runtime.Messages;
import org.eclipse.babel.runtime.TranslatableMenuItem;
import org.eclipse.babel.runtime.dialogs.LocalizableTrayDialog;
import org.eclipse.babel.runtime.external.TranslatableNLS;
import org.eclipse.babel.runtime.external.ITranslatableSet;
import org.eclipse.babel.runtime.external.ITranslatableText;
import org.eclipse.babel.runtime.external.TranslatableText;
import org.eclipse.babel.runtime.external.TranslatableTextInput;
import org.eclipse.babel.runtime.external.TranslatableResourceBundle;
import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.about.AboutBundleData;
import org.eclipse.ui.internal.about.AboutBundleGroupData;
import org.eclipse.ui.internal.util.BundleUtility;
import org.osgi.framework.Bundle;

public class LocalizeDialog extends LocalizableTrayDialog {

	private ITranslatableText tabTitle;

	private ITranslatableSet targetLanguageSet;

	private ITranslatableSet menuTextSet;
	
	private Set<TranslatableResourceBundle> updatedBundles = new HashSet<TranslatableResourceBundle>();

	private AboutBundleGroupData[] bundleGroupInfos;

	private TableViewer pluginTableViewer;

	public LocalizeDialog(Shell shell, ITranslatableText tabTitle, ITranslatableSet targetLanguageSet, ITranslatableSet menuTextSet) {
		super(shell);
		this.tabTitle = tabTitle;
		this.targetLanguageSet = targetLanguageSet;
		this.menuTextSet = menuTextSet;

		// create a descriptive object for each BundleGroup
		IBundleGroupProvider[] providers = Platform.getBundleGroupProviders();
		LinkedList groups = new LinkedList();
		if (providers != null) {
			for (int i = 0; i < providers.length; ++i) {
				IBundleGroup[] bundleGroups = providers[i].getBundleGroups();
				for (int j = 0; j < bundleGroups.length; ++j) {
					groups.add(new AboutBundleGroupData(bundleGroups[j]));
				}
			}
		}
		bundleGroupInfos = (AboutBundleGroupData[]) groups
		.toArray(new AboutBundleGroupData[0]);
	}

	// On the plug-ins tab
	private Button translatePluginButton;

	protected Control createDialogArea(Composite parent) {
		languageSet.associate(getShell(), Messages.LocalizeDialog_Title);
//		getShell().setText(Messages.LocalizeDialog_Title);

		Composite container = (Composite)super.createDialogArea(parent);

		final TabFolder tabFolder = new TabFolder(container, SWT.NONE);

//		addFeaturesTab(tabFolder);
		if (tabTitle != null) {
			addActivePartTab(tabFolder);
		}
		addMenuTab(tabFolder);
		addPluginsTab(tabFolder);
		
		languageSet.associate(recursiveUseText);
		
		Dialog.applyDialogFont(container);
		return container;
	}

	private void addFeaturesTab(TabFolder tabFolder) {
		final TabItem tabItem = new TabItem(tabFolder, SWT.NONE);

		languageSet.associate(
				"features",  //$NON-NLS-1$
				new TranslatableTextInput(Activator.getLocalizableText("LocalizeDialog.featuresTab")) { //$NON-NLS-1$
					@Override
					public void updateControl(String text) {
						tabItem.setText(text);
					}
				}
		);

		Composite c = new Composite(tabFolder, SWT.NONE);
		tabItem.setControl(c);

//		createFeaturesTable(c, bundleInfos);
	}

	private void addPluginsTab(TabFolder tabFolder) {
		TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
		languageSet.associate(tabItem, Messages.LocalizeDialog_TabTitle_Plugins);

		AboutBundleData[] bundleInfos;



		// create a data object for each bundle, remove duplicates, and include only resolved bundles (bug 65548)
		Map map = new HashMap();
		Bundle[] bundles = WorkbenchPlugin.getDefault().getBundles();
		for (int i = 0; i < bundles.length; ++i) {
			AboutBundleData data = new AboutBundleData(bundles[i]);
			if (BundleUtility.isReady(data.getState()) && !map.containsKey(data.getVersionedId())) {
				map.put(data.getVersionedId(), data);
			}
		}
		bundleInfos = (AboutBundleData[]) map.values().toArray(
				new AboutBundleData[0]);

		Composite c = new Composite(tabFolder, SWT.NONE);
		tabItem.setControl(c);

		createPluginsTab(c, bundleInfos);
	}


	private void addActivePartTab(TabFolder tabFolder) {
		TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
		languageSet.associate(tabItem, tabTitle);

		Composite composite = new Composite(tabFolder, SWT.NONE);
		tabItem.setControl(composite);

		composite.setLayout(new GridLayout());

		if (targetLanguageSet != null) {

			TranslatableTextInput[] textInputs = targetLanguageSet.getLocalizedTexts();
			ITranslatableText[] texts = new ITranslatableText[textInputs.length];
			for (int i = 0; i < textInputs.length; i++) {
				texts[i] = textInputs[i].getLocalizedTextObject();
			}

			Control tv = new TranslatableTreeComposite(composite, new TextInputContentProvider(), texts, languageSet, updatedBundles);
			GridData treeLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
			treeLayoutData.heightHint = 400;
			tv.setLayoutData(treeLayoutData);
			
		} else {
			/*
			 * The active part (editor/view/dialog) has not been instrumented
			 * for translation.
			 */
			Label notTranslatableLabel = new Label(composite, SWT.WRAP);
			languageSet.associate(notTranslatableLabel, Activator.getLocalizableText("LocalizeDialog.notTranslatable")); //$NON-NLS-1$
		}
	}

	private void addMenuTab(TabFolder tabFolder) {
		TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
		languageSet.associate(tabItem, Messages.LocalizeDialog_TabTitle_Menu);

		Composite composite = new Composite(tabFolder, SWT.NONE);
		tabItem.setControl(composite);

		composite.setLayout(new GridLayout());

		TranslatableMenuItem translatableMenu = Activator.getDefault().getTranslatableMenu();
		if (translatableMenu == null) {
			// Not likely this can happen.  Possibly the user was so quick
			// to open this dialog that this initialization has not yet been
			// completed.
			
			try {
				MenuAnalyzer analyser = new MenuAnalyzer();
				translatableMenu = analyser.createTranslatableMenu();
				this.menuTextSet = analyser.getTextSet();
				Activator.getDefault().setTranslatableMenu(translatableMenu, menuTextSet);
			} catch (Exception e) {
				Status status = new Status(IStatus.ERROR,
						Activator.PLUGIN_ID, 0,
						"Translatable menu created.", e);
				Activator.getDefault().getLog().log(status);		
			}
		}

		Control tv = new TranslatableTreeComposite(composite, new MenuContentProvider(), translatableMenu, languageSet, updatedBundles);
		tv.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
	}

	/**
	 * Table height in dialog units (value 200).
	 */
	private static final int TABLE_HEIGHT = 200;

	private TranslatableText recursiveUseText = Activator.getLocalizableText("LocalizeDialog.recursiveUse");

	public class BundleTableLabelProvider extends LabelProvider implements ITableLabelProvider  {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
		 */
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
		 */
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof AboutBundleData) {
				AboutBundleData data = (AboutBundleData) element;
				switch (columnIndex) {
				case 0:
					return data.getProviderName();
				case 1:
					return data.getName();
				case 2:
					return data.getVersion();
				case 3:
					return data.getId();
				}
			}
			return ""; //$NON-NLS-1$
		}		
	}


	/**
	 * Create the table part of the dialog.
	 * 
	 * @param parent
	 *            the parent composite to contain the dialog area
	 */
	protected void createPluginsTab(Composite parent, AboutBundleData[] bundleInfos) {
		parent.setLayout(new GridLayout(1,false));

		GridData sgd = new GridData(SWT.FILL, SWT.FILL, true, true);
		sgd.heightHint = 200;
//		pluginTableViewer.getTable().setLayoutData(sgd);

//		createPluginsTable(parent, bundleInfos).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createPluginsTable(parent, bundleInfos).setLayoutData(sgd);
		createPluginsTabButtons(parent).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	}	

	protected Control createPluginsTabButtons(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new RowLayout(SWT.HORIZONTAL));

		translatePluginButton = new Button(container, SWT.PUSH);
		languageSet.associate(translatePluginButton, Messages.LocalizeDialog_Command_Translate);
		translatePluginButton.setEnabled(false);
		translatePluginButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection = (IStructuredSelection)pluginTableViewer.getSelection();
				AboutBundleData selected = (AboutBundleData) selection.getFirstElement(); 
				PluginLocalizationDialog dialog = new PluginLocalizationDialog(getShell(), selected, targetLanguageSet);
				dialog.open();
			}
		});
		return container;
	}

	/**
	 * Create the table part of the dialog.
	 * 
	 * @param parent
	 *            the parent composite to contain the dialog area
	 */
	protected Control createPluginsTable(Composite parent, AboutBundleData[] bundleInfos) {
		ITranslatableText columnTitles[] = {
				Messages.AboutPluginsDialog_provider,
				Messages.AboutPluginsDialog_pluginName,
				Messages.AboutPluginsDialog_version, 
				Messages.AboutPluginsDialog_pluginId,
		};




		pluginTableViewer = new TableViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.SINGLE
				| SWT.FULL_SELECTION | SWT.BORDER);
		pluginTableViewer.setUseHashlookup(true);
		pluginTableViewer.getTable().setHeaderVisible(true);
		pluginTableViewer.getTable().setLinesVisible(true);
		pluginTableViewer.getTable().setFont(parent.getFont());
		pluginTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				// enable if there is an item selected and that
				// item has additional info
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				if (selection.getFirstElement() instanceof AboutBundleData) {
					AboutBundleData selected = (AboutBundleData) selection.getFirstElement(); 
					translatePluginButton.setEnabled(true);
				}
			}
		});

		final TableComparator comparator = new TableComparator();
		comparator.setSortColumn(1); // sort on name initially

		int[] columnWidths = {
				convertHorizontalDLUsToPixels(120),
				convertHorizontalDLUsToPixels(120),
				convertHorizontalDLUsToPixels(70),
				convertHorizontalDLUsToPixels(130),
		};


		// create table headers
		for (int i = 0; i < columnTitles.length; i++) {
			TableColumn column = new TableColumn(pluginTableViewer.getTable(), SWT.NULL);
			column.setWidth(columnWidths[i]);
			languageSet.associate(column, columnTitles[i]);
			final int columnIndex = i;
			column.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (columnIndex == comparator.getSortColumn()) {
						comparator.setAscending(!comparator.isAscending());
					}
					comparator.setSortColumn(columnIndex);
					pluginTableViewer.refresh();
				}
			});
		}

		pluginTableViewer.setComparator(comparator);
		pluginTableViewer.setContentProvider(new ArrayContentProvider());        
		pluginTableViewer.setLabelProvider(new BundleTableLabelProvider());

		GridData gridData = new GridData(GridData.FILL, GridData.FILL, true,
				true);
		gridData.heightHint = convertVerticalDLUsToPixels(TABLE_HEIGHT);
		pluginTableViewer.getTable().setLayoutData(gridData);

		pluginTableViewer.setInput(bundleInfos);

		return pluginTableViewer.getControl();
	}

	class TableComparator extends ViewerComparator {

		private int sortColumn = 0;
		private boolean ascending = true;

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (viewer instanceof TableViewer) {
				TableViewer tableViewer = (TableViewer) viewer;
				IBaseLabelProvider baseLabel = tableViewer.getLabelProvider();
				if (baseLabel instanceof ITableLabelProvider) {
					ITableLabelProvider tableProvider = (ITableLabelProvider) baseLabel;
					String e1p = tableProvider.getColumnText(e1, sortColumn);
					String e2p = tableProvider.getColumnText(e2, sortColumn);
					int result = getComparator().compare(e1p, e2p);
					return ascending ?  result : (-1) * result;
				}
			}

			return super.compare(viewer, e1, e2);
		}

		/**
		 * @return Returns the sortColumn.
		 */
		public int getSortColumn() {
			return sortColumn;
		}

		/**
		 * @param sortColumn The sortColumn to set.
		 */
		public void setSortColumn(int sortColumn) {
			this.sortColumn = sortColumn;
		}

		/**
		 * @return Returns the ascending.
		 */
		public boolean isAscending() {
			return ascending;
		}

		/**
		 * @param ascending The ascending to set.
		 */
		public void setAscending(boolean ascending) {
			this.ascending = ascending;
		}
	}


	/**
	 * Create the table part of the dialog.
	 *
	 * @param parent  the parent composite to contain the dialog area
	 */
	protected void createFeaturesTree(Composite parent) {
		TreeViewer table = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.SINGLE
				| SWT.FULL_SELECTION | SWT.BORDER);

		GridData gridData = new GridData(GridData.FILL, GridData.FILL, true,
				true);
		gridData.heightHint = convertVerticalDLUsToPixels(TABLE_HEIGHT);
		table.getTree().setLayoutData(gridData);
		table.getTree().setHeaderVisible(true);

//		ViewContentProvider contentProvider;

//table.setContentProvider(contentProvider);
//		table.setLabelProvider(labelProvider);

		table.getTree().setLinesVisible(true);
		table.getTree().setFont(parent.getFont());
//		table.addSelectionListener(new SelectionAdapter() {
//		public void widgetSelected(SelectionEvent e) {
//		AboutBundleGroupData info = (AboutBundleGroupData) e.item
//		.getData();
//		updateButtons(info);
//		}
//		});

		int[] columnWidths = { convertHorizontalDLUsToPixels(120),
				convertHorizontalDLUsToPixels(120),
				convertHorizontalDLUsToPixels(70),
				convertHorizontalDLUsToPixels(130) 
		};

//		for (int i = 0; i < columnTitles.length; i++) {
//		TreeColumn tableColumn = new TreeColumn(table, SWT.NULL);
//		tableColumn.setWidth(columnWidths[i]);
//		tableColumn.setText(columnTitles[i]);
//		final int columnIndex = i;
//		tableColumn.addSelectionListener(new SelectionAdapter() {
//		public void widgetSelected(SelectionEvent e) {
//		sort(columnIndex);
//		}
//		});
//		}

//		// create a table row for each bundle group
//		String selId = lastSelection == null ? null : lastSelection.getId();
//		int sel = 0;
//		for (int i = 0; i < bundleGroupInfos.length; i++) {
//		if (bundleGroupInfos[i].getId().equals(selId)) {
//		sel = i;
//		}

//		TableItem item = new TableItem(table, SWT.NULL);
//		item.setText(createRow(bundleGroupInfos[i]));
//		item.setData(bundleGroupInfos[i]);
//		}

		// if an item was specified during construction, it should be
		// selected when the table is created
//		if (bundleGroupInfos.length > 0) {
//		table.setSelection(sel);
//		table.showSelection();
//		}
	}




	protected void createButtonsForButtonBar(Composite parent) {
		createButton(
				parent,
				IDialogConstants.OK_ID,
				IDialogConstants.OK_LABEL,
				true);
		createButton(
				parent,
				IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL,
				false);
	}

	protected void okPressed() {
		// Update part messages, if any
		if (targetLanguageSet != null) {
			for (TranslatableTextInput textInput: targetLanguageSet.getLocalizedTexts()) {
				textInput.updateControl();
			}
			targetLanguageSet.layout();
		}

		// Update menu messages
		for (TranslatableTextInput textInput: menuTextSet.getLocalizedTexts()) {
			textInput.updateControl();
		}
		menuTextSet.layout();  // Never does anything, actually

		/*
		 * Save all bundles that have had changes.  These have been put
		 * into a set, so it is easy for us to know which ones have been changed.
		 */
		for (TranslatableResourceBundle bundle: updatedBundles) {
			bundle.save();
		}

		super.okPressed();
	}

	/*
	 * Called when the user selects the action to localize this dialog.
	 * This will bring up a dialog that can be used to localize this dialog.
	 */
	@Override
	protected void localizationPressed() {
		/*
		 * This dialog may itself be the dialog that does the localization.  The
		 * user can localize the localization dialog itself.  We don't want to
		 * allow the user to recursively bring up the localization dialog, so check
		 * that we are not already localizing the localization dialog.
		 * 
		 * It's a little kludgy, but we detect recursive use by testing the title
		 * used for the active dialog tab.
		 */
		if (tabTitle.getLocalizedText().equals(TranslatableNLS.bind(Messages.LocalizeDialog_TabTitle_Dialog, getShell().getText()).getLocalizedText())) {
			MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
			messageBox.setMessage(recursiveUseText.getLocalizedText());
			messageBox.open();
		}

		super.localizationPressed();
	}
}
