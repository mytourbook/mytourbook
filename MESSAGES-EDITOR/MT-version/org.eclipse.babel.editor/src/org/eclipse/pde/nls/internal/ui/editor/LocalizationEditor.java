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
package org.eclipse.pde.nls.internal.ui.editor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import org.eclipse.babel.editor.plugin.MessagesEditorPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.nls.internal.ui.dialogs.ConfigureColumnsDialog;
import org.eclipse.pde.nls.internal.ui.dialogs.EditResourceBundleEntriesDialog;
import org.eclipse.pde.nls.internal.ui.dialogs.FilterOptions;
import org.eclipse.pde.nls.internal.ui.dialogs.FilterOptionsDialog;
import org.eclipse.pde.nls.internal.ui.model.ResourceBundle;
import org.eclipse.pde.nls.internal.ui.model.ResourceBundleFamily;
import org.eclipse.pde.nls.internal.ui.model.ResourceBundleKey;
import org.eclipse.pde.nls.internal.ui.model.ResourceBundleKeyList;
import org.eclipse.pde.nls.internal.ui.model.ResourceBundleModel;
import org.eclipse.pde.nls.internal.ui.parser.LocaleUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.internal.misc.StringMatcher;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.ShowInContext;

// TODO Fix restriction and remove warning 
@SuppressWarnings("restriction")
public class LocalizationEditor extends EditorPart {

	private final class LocalizationLabelProvider extends ColumnLabelProvider {

		private final Object columnConfig;

		public LocalizationLabelProvider(Object columnConfig) {
			this.columnConfig = columnConfig;
		}

		public String getText(Object element) {
			ResourceBundleKey key = (ResourceBundleKey) element;
			if (columnConfig == KEY)
				return key.getName();
			Locale locale = (Locale) columnConfig;
			String value;
			try {
				value = key.getValue(locale);
			} catch (CoreException e) {
				value = null;
				MessagesEditorPlugin.log(e);
			}
			if (value == null)
				value = "";
			return value;
		}
	}

	private class EditEntryAction extends Action {
		public EditEntryAction() {
			super("&Edit", IAction.AS_PUSH_BUTTON);
		}

		/*
		 * @see org.eclipse.jface.action.Action#run()
		 */
		public void run() {
			ResourceBundleKey key = getSelectedEntry();
			if (key == null) {
				return;
			}
			Shell shell = Display.getCurrent().getActiveShell();
			Locale[] locales = getLocales();
			EditResourceBundleEntriesDialog dialog = new EditResourceBundleEntriesDialog(shell, locales);
			dialog.setResourceBundleKey(key);
			if (dialog.open() == Window.OK) {
				updateLabels();
			}
		}
	}

	private class ConfigureColumnsAction extends Action {
		public ConfigureColumnsAction() {
			super(null, IAction.AS_PUSH_BUTTON); //$NON-NLS-1$
			setImageDescriptor(MessagesEditorPlugin.getImageDescriptor("elcl16/conf_columns.gif"));
			setToolTipText("Configure Columns");
		}

		/*
		 * @see org.eclipse.jface.action.Action#run()
		 */
		public void run() {
			Shell shell = Display.getCurrent().getActiveShell();
			String[] values = new String[columnConfigs.length];
			for (int i = 0; i < columnConfigs.length; i++) {
				String config = columnConfigs[i].toString();
				if (config.equals("")) //$NON-NLS-1$
					config = "default"; //$NON-NLS-1$
				values[i] = config;
			}
			ConfigureColumnsDialog dialog = new ConfigureColumnsDialog(shell, values);
			if (dialog.open() == Window.OK) {
				String[] result = dialog.getResult();
				Object[] newConfigs = new Object[result.length];
				for (int i = 0; i < newConfigs.length; i++) {
					if (result[i].equals("key")) { //$NON-NLS-1$
						newConfigs[i] = KEY;
					} else if (result[i].equals("default")) { //$NON-NLS-1$
						newConfigs[i] = new Locale("");
					} else {
						newConfigs[i] = LocaleUtil.parseLocale(result[i]);
					}
				}
				setColumns(newConfigs);
			}
		}
	}

	private class EditFilterOptionsAction extends Action {
		public EditFilterOptionsAction() {
			super(null, IAction.AS_PUSH_BUTTON); //$NON-NLS-1$
			setImageDescriptor(MessagesEditorPlugin.getImageDescriptor("elcl16/filter_obj.gif"));
			setToolTipText("Edit Filter Options");
		}

		/*
		 * @see org.eclipse.jface.action.Action#run()
		 */
		public void run() {
			Shell shell = Display.getCurrent().getActiveShell();
			FilterOptionsDialog dialog = new FilterOptionsDialog(shell);
			dialog.setInitialFilterOptions(filterOptions);
			if (dialog.open() == Window.OK) {
				filterOptions = dialog.getResult();
				refresh();
				updateFilterLabel();
			}
		}
	}

	private class RefreshAction extends Action {
		public RefreshAction() {
			super(null, IAction.AS_PUSH_BUTTON); //$NON-NLS-1$
			setImageDescriptor(MessagesEditorPlugin.getImageDescriptor("elcl16/refresh.gif"));
			setToolTipText("Refresh");
		}

		/*
		 * @see org.eclipse.jface.action.Action#run()
		 */
		public void run() {
			MessagesEditorPlugin.disposeModel();
			entryList = new ResourceBundleKeyList(new ResourceBundleKey[0]);
			tableViewer.getTable().setItemCount(0);
			updateLabels();
			refresh();
		}
	}

	private class BundleStringComparator implements Comparator<ResourceBundleKey> {
		private final Locale locale;
		public BundleStringComparator(Locale locale) {
			this.locale = locale;
		}
		public int compare(ResourceBundleKey o1, ResourceBundleKey o2) {
			String value1 = null;
			String value2 = null;
			try {
				value1 = o1.getValue(locale);
			} catch (CoreException e) {
				MessagesEditorPlugin.log(e);
			}
			try {
				value2 = o2.getValue(locale);
			} catch (CoreException e) {
				MessagesEditorPlugin.log(e);
			}
			if (value1 == null)
				value1 = ""; //$NON-NLS-1$
			if (value2 == null)
				value2 = ""; //$NON-NLS-1$
			return value1.compareToIgnoreCase(value2);
		}
	}

	private class ExportAction extends Action {
		public ExportAction() {
			super(null, IAction.AS_PUSH_BUTTON); //$NON-NLS-1$
			setImageDescriptor(MessagesEditorPlugin.getImageDescriptor("elcl16/export.gif"));
			setToolTipText("Export Current View to CSV or HTML File");
		}

		/*
		 * @see org.eclipse.jface.action.Action#run()
		 */
		public void run() {
			Shell shell = Display.getCurrent().getActiveShell();
			FileDialog dialog = new FileDialog(shell);
			dialog.setText("Export File");
			dialog.setFilterExtensions(new String[] {"*.*", "*.htm; *.html", "*.txt; *.csv"});
			dialog.setFilterNames(new String[] {"All Files (*.*)", "HTML File (*.htm; *.html)",
					"Tabulator Separated File (*.txt; *.csv)"});
			final String filename = dialog.open();
			if (filename != null) {
				BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
					public void run() {
						File file = new File(filename);
						try {
							BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
								new FileOutputStream(file),
								"UTF8")); //$NON-NLS-1$
							boolean isHtml = filename.endsWith(".htm") || filename.endsWith(".html"); //$NON-NLS-1$ //$NON-NLS-2$
							if (isHtml) {
								writer.write("" //$NON-NLS-1$
										+ "<html>\r\n" //$NON-NLS-1$
										+ "<head>\r\n" //$NON-NLS-1$
										+ "<meta http-equiv=Content-Type content=\"text/html; charset=UTF-8\">\r\n" //$NON-NLS-1$
										+ "<style>\r\n" //$NON-NLS-1$
										+ "table {width:100%;}\r\n" //$NON-NLS-1$
										+ "td.sep {height:10px;background:#C0C0C0;}\r\n" //$NON-NLS-1$
										+ "</style>\r\n" //$NON-NLS-1$
										+ "</head>\r\n" //$NON-NLS-1$
										+ "<body>\r\n" //$NON-NLS-1$
										+ "<table width=\"100%\" border=\"1\">\r\n"); //$NON-NLS-1$
							}

							int size = entryList.getSize();
							Object[] configs = LocalizationEditor.this.columnConfigs;
							int valueCount = 0;
							int missingCount = 0;
							for (int i = 0; i < size; i++) {
								ResourceBundleKey key = entryList.getKey(i);
								if (isHtml) {
									writer.write("<table border=\"1\">\r\n"); //$NON-NLS-1$
								}
								for (int j = 0; j < configs.length; j++) {
									if (isHtml) {
										writer.write("<tr><td>"); //$NON-NLS-1$
									}
									Object config = configs[j];
									if (!isHtml && j > 0)
										writer.write("\t"); //$NON-NLS-1$
									if (config == KEY) {
										writer.write(key.getName());
									} else {
										Locale locale = (Locale) config;
										String value;
										try {
											value = key.getValue(locale);
										} catch (CoreException e) {
											value = null;
											MessagesEditorPlugin.log(e);
										}
										if (value == null) {
											value = ""; //$NON-NLS-1$
											missingCount++;
										} else {
											valueCount++;
										}
										writer.write(EditResourceBundleEntriesDialog.escape(value));
									}
									if (isHtml) {
										writer.write("</td></tr>\r\n"); //$NON-NLS-1$
									}
								}
								if (isHtml) {
									writer.write("<tr><td class=\"sep\">&nbsp;</td></tr>\r\n"); //$NON-NLS-1$
									writer.write("</table>\r\n"); //$NON-NLS-1$
								} else {
									writer.write("\r\n"); //$NON-NLS-1$
								}
							}
							if (isHtml) {
								writer.write("</body>\r\n" + "</html>\r\n"); //$NON-NLS-1$ //$NON-NLS-2$
							}
							writer.close();
							Shell shell = Display.getCurrent().getActiveShell();
							MessageDialog.openInformation(
									shell,
									"Finished",
									"File written successfully.\n\nNumber of entries written: "
											+ entryList.getSize()
											+ "\nNumber of translations: "
											+ valueCount
											+ " ("
											+ missingCount
											+ " missing)");
						} catch (IOException e) {
							Shell shell = Display.getCurrent().getActiveShell();
							ErrorDialog.openError(shell, "Error", "Error saving file.", new Status(
								IStatus.ERROR,
								MessagesEditorPlugin.PLUGIN_ID,
								e.getMessage(),
								e));
						}
					}
				});
			}
		}
	}

	public static final String ID = "org.eclipse.pde.nls.ui.LocalizationEditor"; //$NON-NLS-1$

	protected static final Object KEY = "key"; // used to indicate the key column

	private static final String PREF_SECTION_NAME = "org.eclipse.pde.nls.ui.LocalizationEditor"; //$NON-NLS-1$
	private static final String PREF_SORT_ORDER = "sortOrder"; //$NON-NLS-1$
	private static final String PREF_COLUMNS = "columns"; //$NON-NLS-1$
	private static final String PREF_FILTER_OPTIONS_FILTER_PLUGINS = "filterOptions_filterPlugins"; //$NON-NLS-1$
	private static final String PREF_FILTER_OPTIONS_PLUGIN_PATTERNS = "filterOptions_pluginPatterns"; //$NON-NLS-1$
	private static final String PREF_FILTER_OPTIONS_MISSING_ONLY = "filterOptions_missingOnly"; //$NON-NLS-1$

	// Actions
	private EditFilterOptionsAction editFiltersAction;
	private ConfigureColumnsAction selectLanguagesAction;
	private RefreshAction refreshAction;
	private ExportAction exportAction;

	// Form
	protected FormToolkit toolkit = new FormToolkit(Display.getCurrent());
	private Form form;
	private Image formImage;

	// Query
	protected Composite queryComposite;
	protected Text queryText;

	// Results 
	private Section resultsSection;
	private Composite tableComposite;
	protected TableViewer tableViewer;
	protected Table table;
	protected ArrayList<TableColumn> columns = new ArrayList<TableColumn>();

	// Data and configuration
	protected LocalizationEditorInput input;
	protected ResourceBundleKeyList entryList;
	protected FilterOptions filterOptions;

	/**
	 * Column configuration. Values may be either <code>KEY</code> or a {@link Locale}.
	 */
	protected Object[] columnConfigs;
	/**
	 * Either <code>KEY</code> or a {@link Locale}.
	 */
	protected Object sortOrder;

	private String lastQuery = "";
	protected Job searchJob;

	private ISchedulingRule mutexRule = new ISchedulingRule() {
		public boolean contains(ISchedulingRule rule) {
			return rule == this;
		}
		public boolean isConflicting(ISchedulingRule rule) {
			return rule == this;
		}
	};

	private Label filteredLabel;

	public LocalizationEditor() {
	}

	public ResourceBundleKey getSelectedEntry() {
		IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
		if (selection.size() == 1) {
			return (ResourceBundleKey) selection.getFirstElement();
		}
		return null;
	}

	public String getQueryText() {
		return queryText.getText();
	}

	@Override
	public void createPartControl(Composite parent) {
		GridData gd;
		GridLayout gridLayout;

		form = toolkit.createForm(parent);
		form.setSeparatorVisible(true);
		form.setText("Localization");

		form.setImage(formImage = MessagesEditorPlugin.getImageDescriptor("obj16/nls_editor.gif").createImage()); //$NON-NLS-1$
		toolkit.adapt(form);
		toolkit.paintBordersFor(form);
		final Composite body = form.getBody();
		gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		body.setLayout(gridLayout);
		toolkit.paintBordersFor(body);
		toolkit.decorateFormHeading(form);

		queryComposite = toolkit.createComposite(body);
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		queryComposite.setLayoutData(gd);
		gridLayout = new GridLayout(5, false);
		gridLayout.marginHeight = 0;
		queryComposite.setLayout(gridLayout);
		toolkit.paintBordersFor(queryComposite);

		// Form toolbar
		editFiltersAction = new EditFilterOptionsAction();
		selectLanguagesAction = new ConfigureColumnsAction();
		refreshAction = new RefreshAction();
		exportAction = new ExportAction();
		IToolBarManager toolBarManager = form.getToolBarManager();
		toolBarManager.add(refreshAction);
		toolBarManager.add(editFiltersAction);
		toolBarManager.add(selectLanguagesAction);
		toolBarManager.add(exportAction);
		form.updateToolBar();

		toolkit.createLabel(queryComposite, "Search:");

		// Query text
		queryText = toolkit.createText(queryComposite, "", SWT.WRAP | SWT.SINGLE); //$NON-NLS-1$
		queryText.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN) {
					table.setFocus();
				} else if (e.keyCode == SWT.ESC) {
					queryText.setText(""); //$NON-NLS-1$
				}
			}
		});
		queryText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				executeQuery();

				Object[] listeners = getListeners();
				for (int i = 0; i < listeners.length; i++) {
					IPropertyListener listener = (IPropertyListener) listeners[i];
					listener.propertyChanged(this, PROP_TITLE);
				}
			}
		});
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		queryText.setLayoutData(gd);
		toolkit.adapt(queryText, true, true);

		ToolBarManager toolBarManager2 = new ToolBarManager(SWT.FLAT);
		toolBarManager2.createControl(queryComposite);
		ToolBar control = toolBarManager2.getControl();
		toolkit.adapt(control);

		// Results section
		resultsSection = toolkit.createSection(body, ExpandableComposite.TITLE_BAR
				| ExpandableComposite.LEFT_TEXT_CLIENT_ALIGNMENT);
		resultsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		resultsSection.setText("Localization Strings");
		toolkit.adapt(resultsSection);

		final Composite resultsComposite = toolkit.createComposite(resultsSection, SWT.NONE);
		toolkit.adapt(resultsComposite);
		final GridLayout gridLayout2 = new GridLayout();
		gridLayout2.marginTop = 1;
		gridLayout2.marginWidth = 1;
		gridLayout2.marginHeight = 1;
		gridLayout2.horizontalSpacing = 0;
		resultsComposite.setLayout(gridLayout2);

		filteredLabel = new Label(resultsSection, SWT.NONE);
		filteredLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		filteredLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		filteredLabel.setText(""); //$NON-NLS-1$

		toolkit.paintBordersFor(resultsComposite);
		resultsSection.setClient(resultsComposite);
		resultsSection.setTextClient(filteredLabel);

		tableComposite = toolkit.createComposite(resultsComposite, SWT.NONE);
		tableComposite.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TREE_BORDER);
		toolkit.adapt(tableComposite);
		tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Table
		createTableViewer();

		registerContextMenu();

		// Set default configuration
		filterOptions = new FilterOptions();
		filterOptions.filterPlugins = false;
		filterOptions.pluginPatterns = new String[0];
		filterOptions.keysWithMissingEntriesOnly = false;
		sortOrder = KEY;
		columnConfigs = new Object[] {KEY, new Locale(""), new Locale("de")}; //$NON-NLS-1$ //$NON-NLS-2$

		// Load configuration
		try {
			loadSettings();
		} catch (Exception e) {
			// Ignore
		}

		updateColumns();
		updateFilterLabel();
		table.setSortDirection(SWT.UP);
	}

	protected void updateFilterLabel() {
		if (filterOptions.filterPlugins || filterOptions.keysWithMissingEntriesOnly) {
			filteredLabel.setText("(filtered)");
		} else {
			filteredLabel.setText(""); //$NON-NLS-1$
		}
		filteredLabel.getParent().layout(true);
	}

	private void loadSettings() {
		// TODO Move this to the preferences?
		IDialogSettings dialogSettings = MessagesEditorPlugin.getDefault().getDialogSettings();
		IDialogSettings section = dialogSettings.getSection(PREF_SECTION_NAME);
		if (section == null)
			return;

		// Sort order
		String sortOrderString = section.get(PREF_SORT_ORDER);
		if (sortOrderString != null) {
			if (sortOrderString.equals(KEY)) {
				sortOrder = KEY;
			} else {
				try {
					sortOrder = LocaleUtil.parseLocale(sortOrderString);
				} catch (IllegalArgumentException e) {
					// Should never happen
				}
			}
		}

		// Columns
		String columns = section.get(PREF_COLUMNS);
		if (columns != null) {
			String[] cols = columns.substring(1, columns.length() - 1).split(","); //$NON-NLS-1$
			columnConfigs = new Object[cols.length];
			for (int i = 0; i < cols.length; i++) {
				String value = cols[i].trim();
				if (value.equals(KEY)) {
					columnConfigs[i] = KEY;
				} else if (value.equals("default")) { //$NON-NLS-1$
					columnConfigs[i] = new Locale(""); //$NON-NLS-1$
				} else {
					try {
						columnConfigs[i] = LocaleUtil.parseLocale(value);
					} catch (IllegalArgumentException e) {
						columnConfigs[i] = null;
					}
				}
			}
		}

		// Filter options
		String filterOptions = section.get(PREF_FILTER_OPTIONS_FILTER_PLUGINS);
		this.filterOptions.filterPlugins = "true".equals(filterOptions); //$NON-NLS-1$
		String patterns = section.get(PREF_FILTER_OPTIONS_PLUGIN_PATTERNS);
		if (patterns != null) {
			String[] split = patterns.substring(1, patterns.length() - 1).split(","); //$NON-NLS-1$
			for (int i = 0; i < split.length; i++) {
				split[i] = split[i].trim();
			}
			this.filterOptions.pluginPatterns = split;
		}
		this.filterOptions.keysWithMissingEntriesOnly = "true".equals(section.get(PREF_FILTER_OPTIONS_MISSING_ONLY)); //$NON-NLS-1$

		// TODO Save column widths 
	}

	private void saveSettings() {
		IDialogSettings dialogSettings = MessagesEditorPlugin.getDefault().getDialogSettings();
		IDialogSettings section = dialogSettings.getSection(PREF_SECTION_NAME);
		if (section == null) {
			section = dialogSettings.addNewSection(PREF_SECTION_NAME);
		}
		// Sort order
		section.put(PREF_SORT_ORDER, sortOrder.toString());

		// Columns
		section.put(PREF_COLUMNS, Arrays.toString(columnConfigs));

		// Filter options
		section.put(PREF_FILTER_OPTIONS_FILTER_PLUGINS, filterOptions.filterPlugins);
		section.put(PREF_FILTER_OPTIONS_PLUGIN_PATTERNS, Arrays.toString(filterOptions.pluginPatterns));
		section.put(PREF_FILTER_OPTIONS_MISSING_ONLY, filterOptions.keysWithMissingEntriesOnly);
	}

	private void createTableViewer() {
		table = new Table(tableComposite, SWT.VIRTUAL | SWT.FULL_SELECTION | SWT.MULTI);
		tableViewer = new TableViewer(table);
		table.setHeaderVisible(true);
		toolkit.adapt(table);
		toolkit.paintBordersFor(table);
		toolkit.adapt(table, true, true);

		tableViewer.setContentProvider(new ILazyContentProvider() {
			public void updateElement(int index) {
				tableViewer.replace(entryList.getKey(index), index);
			}
			public void dispose() {
			}
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});
		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				new EditEntryAction().run();
			}
		});
	}

	private void registerContextMenu() {
		MenuManager menuManager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager menu) {
				fillContextMenu(menu);
			}
		});
		Menu contextMenu = menuManager.createContextMenu(table);
		table.setMenu(contextMenu);
		getSite().registerContextMenu(menuManager, getSite().getSelectionProvider());
	}

	protected void fillContextMenu(IMenuManager menu) {
		int selectionCount = table.getSelectionCount();
		if (selectionCount == 1) {
			menu.add(new EditEntryAction());
			menu.add(new Separator());
		}
		MenuManager showInSubMenu = new MenuManager("&Show In");
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IContributionItem item = ContributionItemFactory.VIEWS_SHOW_IN.create(window);
		showInSubMenu.add(item);
		menu.add(showInSubMenu);
	}

	@Override
	public void setFocus() {
		queryText.setFocus();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		this.input = (LocalizationEditorInput) input;
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	/*
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@Override
	public void dispose() {
		saveSettings();
		if (formImage != null) {
			formImage.dispose();
			formImage = null;
		}
		MessagesEditorPlugin.disposeModel();
	}

	protected void executeQuery() {
		String pattern = queryText.getText();
		lastQuery = pattern;
		executeQuery(pattern);
	}

	protected void executeQuery(final String pattern) {
		if (searchJob != null) {
			searchJob.cancel();
		}
		searchJob = new Job("Localization Editor Search...") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						form.setBusy(true);
					}
				});

				String keyPattern = pattern;
				if (!pattern.endsWith("*")) { //$NON-NLS-1$
					keyPattern = pattern.concat("*"); //$NON-NLS-1$
				}
				String strPattern = keyPattern;
				if (strPattern.length() > 0 && !strPattern.startsWith("*")) { //$NON-NLS-1$
					strPattern = "*".concat(strPattern); //$NON-NLS-1$
				}

				ResourceBundleModel model = MessagesEditorPlugin.getModel(new NullProgressMonitor());
				Locale[] locales = getLocales();

				// Collect keys
				ResourceBundleKey[] keys;
				if (!filterOptions.filterPlugins
						|| filterOptions.pluginPatterns == null
						|| filterOptions.pluginPatterns.length == 0) {

					// Ensure the bundles are loaded
					for (Locale locale : locales) {
						try {
							model.loadBundles(locale);
						} catch (CoreException e) {
							MessagesEditorPlugin.log(e);
						}
					}

					try {
						keys = model.getAllKeys();
					} catch (CoreException e) {
						MessagesEditorPlugin.log(e);
						keys = new ResourceBundleKey[0];
					}
				} else {
					String[] patterns = filterOptions.pluginPatterns;
					StringMatcher[] matchers = new StringMatcher[patterns.length];
					for (int i = 0; i < matchers.length; i++) {
						matchers[i] = new StringMatcher(patterns[i], true, false);
					}

					int size = 0;
					ResourceBundleFamily[] allFamilies = model.getFamilies();
					ArrayList<ResourceBundleFamily> families = new ArrayList<ResourceBundleFamily>();
					for (int i = 0; i < allFamilies.length; i++) {
						ResourceBundleFamily family = allFamilies[i];
						String pluginId = family.getPluginId();
						for (StringMatcher matcher : matchers) {
							if (matcher.match(pluginId)) {
								families.add(family);
								break;
							}
						}

					}
					for (ResourceBundleFamily family : families) {
						size += family.getKeyCount();
					}

					ArrayList<ResourceBundleKey> filteredKeys = new ArrayList<ResourceBundleKey>(size);
					for (ResourceBundleFamily family : families) {
						// Ensure the bundles are loaded
						for (Locale locale : locales) {
							try {
								ResourceBundle bundle = family.getBundle(locale);
								if (bundle != null)
									bundle.load();
							} catch (CoreException e) {
								MessagesEditorPlugin.log(e);
							}
						}

						ResourceBundleKey[] familyKeys = family.getKeys();
						for (ResourceBundleKey key : familyKeys) {
							filteredKeys.add(key);
						}
					}
					keys = filteredKeys.toArray(new ResourceBundleKey[filteredKeys.size()]);
				}

				// Filter keys
				ArrayList<ResourceBundleKey> filtered = new ArrayList<ResourceBundleKey>();

				StringMatcher keyMatcher = new StringMatcher(keyPattern, true, false);
				StringMatcher strMatcher = new StringMatcher(strPattern, true, false);
				for (ResourceBundleKey key : keys) {
					if (monitor.isCanceled())
						return Status.OK_STATUS;

					// Missing entries
					if (filterOptions.keysWithMissingEntriesOnly) {
						boolean hasMissingEntry = false;
						// Check all columns for missing values
						for (Object config : columnConfigs) {
							if (config == KEY)
								continue;
							Locale locale = (Locale) config;
							String value = null;
							try {
								value = key.getValue(locale);
							} catch (CoreException e) {
								MessagesEditorPlugin.log(e);
							}
							if (value == null || value.length() == 0) {
								hasMissingEntry = true;
								break;
							}
						}
						if (!hasMissingEntry)
							continue;
					}

					// Match key
					if (keyMatcher.match(key.getName())) {
						filtered.add(key);
						continue;
					}

					// Match entries
					for (Object config : columnConfigs) {
						if (config == KEY)
							continue;
						Locale locale = (Locale) config;
						String value = null;
						try {
							value = key.getValue(locale);
						} catch (CoreException e) {
							MessagesEditorPlugin.log(e);
						}
						if (strMatcher.match(value)) {
							filtered.add(key);
							break;
						}
					}
				}

				ResourceBundleKey[] array = filtered.toArray(new ResourceBundleKey[filtered.size()]);
				if (sortOrder == KEY) {
					Arrays.sort(array, new Comparator<ResourceBundleKey>() {
						public int compare(ResourceBundleKey o1, ResourceBundleKey o2) {
							return o1.getName().compareToIgnoreCase(o2.getName());
						}
					});
				} else {
					Locale locale = (Locale) sortOrder;
					Arrays.sort(array, new BundleStringComparator(locale));
				}
				entryList = new ResourceBundleKeyList(array);

				if (monitor.isCanceled())
					return Status.OK_STATUS;

				final ResourceBundleKeyList entryList2 = entryList;
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						form.setBusy(false);
						if (entryList2 != null) {
							entryList = entryList2;
						}
						setSearchResult(entryList);
					}
				});
				return Status.OK_STATUS;
			}
		};
		searchJob.setSystem(true);
		searchJob.setRule(mutexRule);
		searchJob.schedule();
	}

	protected void updateTableLayout() {
		table.getParent().layout(true, true);
	}

	protected void setSearchResult(ResourceBundleKeyList entryList) {
		table.removeAll();
		if (entryList != null) {
			table.setItemCount(entryList.getSize());
		} else {
			table.setItemCount(0);
		}
		updateTableLayout();
	}

	public void refresh() {
		executeQuery(lastQuery);
	}

	public void updateLabels() {
		table.redraw();
		table.update();
	}

	/**
	 * @param columnConfigs an array containing <code>KEY</code> and {@link Locale} values 
	 */
	public void setColumns(Object[] columnConfigs) {
		this.columnConfigs = columnConfigs;
		updateColumns();
	}

	public void updateColumns() {
		for (TableColumn column : columns) {
			column.dispose();
		}
		columns.clear();

		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		tableComposite.setLayout(tableColumnLayout);

		HashSet<Locale> localesToUnload = new HashSet<Locale>(4);
		Locale[] currentLocales = getLocales();
		for (Locale locale : currentLocales) {
			localesToUnload.add(locale);
		}

		// Create columns
		for (Object config : columnConfigs) {
			if (config == null)
				continue;

			final TableViewerColumn viewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
			TableColumn column = viewerColumn.getColumn();
			if (config == KEY) {
				column.setText("Key");
			} else {
				Locale locale = (Locale) config;
				if (locale.getLanguage().equals("")) { //$NON-NLS-1$
					column.setText("Default Bundle");
				} else {
					String displayName = locale.getDisplayName();
					if (displayName.equals("")) //$NON-NLS-1$
						displayName = locale.toString();
					column.setText(displayName);
					localesToUnload.remove(locale);
				}
			}

			viewerColumn.setLabelProvider(new LocalizationLabelProvider(config));
			tableColumnLayout.setColumnData(column, new ColumnWeightData(33));
			columns.add(column);
			column.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					int size = columns.size();
					for (int i = 0; i < size; i++) {
						TableColumn column = columns.get(i);
						if (column == e.widget) {
							Object config = columnConfigs[i];
							sortOrder = config;
							table.setSortColumn(column);
							table.setSortDirection(SWT.UP);
							refresh();
							break;
						}
					}
				}
			});
		}

		// Update sort order
		List<Object> configs = Arrays.asList(columnConfigs);
		if (!configs.contains(sortOrder)) {
			sortOrder = KEY; // fall back to default sort order
		}
		int index = configs.indexOf(sortOrder);
		if (index != -1)
			table.setSortColumn(columns.get(index));

		refresh();
	}

	/*
	 * @see org.eclipse.ui.part.WorkbenchPart#getAdapter(java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(Class adapter) {
		if (IShowInSource.class == adapter) {
			return new IShowInSource() {
				public ShowInContext getShowInContext() {
					ResourceBundleKey entry = getSelectedEntry();
					if (entry == null)
						return null;
					ResourceBundle bundle = entry.getParent().getBundle(new Locale(""));
					if (bundle == null)
						return null;
					Object resource = bundle.getUnderlyingResource();
					return new ShowInContext(resource, new StructuredSelection(resource));
				}
			};
		}
		return super.getAdapter(adapter);
	}

	/**
	 * Returns the currently displayed locales.
	 * 
	 * @return the currently displayed locales
	 */
	public Locale[] getLocales() {
		ArrayList<Locale> locales = new ArrayList<Locale>(columnConfigs.length);
		for (Object config : columnConfigs) {
			if (config instanceof Locale) {
				Locale locale = (Locale) config;
				locales.add(locale);
			}
		}
		return locales.toArray(new Locale[locales.size()]);
	}

}
