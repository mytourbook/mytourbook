/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
 *   
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software 
 * Foundation version 2 of the License.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA    
 *******************************************************************************/
package net.tourbook.preferences;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.statistic.StatisticContainer;
import net.tourbook.statistic.TourbookStatistic;
import net.tourbook.util.StringToArrayConverter;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageStatistic extends PreferencePage implements IWorkbenchPreferencePage {

	private TableViewer						fStatViewer;

	private boolean							fIsModified	= false;

	private ArrayList<TourbookStatistic>	fVisibleStatistics;

	private Button							fBtnUp;

	private Button							fBtnDown;

	private class StatContentProvicer implements IStructuredContentProvider {

		public void dispose() {}

		public Object[] getElements(final Object inputElement) {
			return fVisibleStatistics.toArray();
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	public PrefPageStatistic() {}

	public PrefPageStatistic(final String title) {
		super(title);
	}

	public PrefPageStatistic(final String title, final ImageDescriptor image) {
		super(title, image);
	}

	private void createButtons(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
		final GridLayout gl = new GridLayout();
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		container.setLayout(gl);

		// button: up
		fBtnUp = new Button(container, SWT.NONE);
		fBtnUp.setText(Messages.app_action_button_up);
		setButtonLayoutData(fBtnUp);
		fBtnUp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onMoveUp();
			}
		});

		// button: down
		fBtnDown = new Button(container, SWT.NONE);
		fBtnDown.setText(Messages.app_action_button_down);
		setButtonLayoutData(fBtnDown);
		fBtnDown.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onMoveDown();
			}
		});

	}

	@Override
	protected Control createContents(final Composite parent) {

		final Composite uiContainer = createUI(parent);

		fVisibleStatistics = StatisticContainer.getStatisticProviders();

		// load viewer
		fStatViewer.setInput(new Object());

		// select first statistic provider
		fStatViewer.setSelection(new StructuredSelection(fVisibleStatistics.get(0)));

		return uiContainer;
	}

	private Composite createUI(final Composite parent) {

		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.pref_statistic_lbl_info);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);

		createViewer(container);
		createButtons(container);

		return container;
	}

	private void createViewer(final Composite parent) {

		final TableColumnLayout tableLayout = new TableColumnLayout();

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		layoutContainer.setLayout(tableLayout);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.applyTo(layoutContainer);

		/*
		 * create table
		 */
		final Table table = new Table(layoutContainer, SWT.FULL_SELECTION | SWT.BORDER);

		table.setLayout(new TableLayout());
		table.setHeaderVisible(false);
		table.setLinesVisible(false);

		fStatViewer = new TableViewer(table);

		/*
		 * create columns
		 */
		TableViewerColumn tvc;

		// column: map provider
		tvc = new TableViewerColumn(fStatViewer, SWT.LEAD);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourbookStatistic statistic = (TourbookStatistic) cell.getElement();

				cell.setText(statistic.fVisibleName);
			}
		});
		tableLayout.setColumnData(tvc.getColumn(), new ColumnWeightData(4, true));

		/*
		 * create table viewer
		 */
		fStatViewer.setContentProvider(new StatContentProvicer());
		fStatViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				enableActions();
			}
		});
	}

	private void enableActions() {

		final IStructuredSelection selection = (IStructuredSelection) fStatViewer.getSelection();

		final Object selectedItem = selection.getFirstElement();
		final Table filterTable = fStatViewer.getTable();

		fBtnUp.setEnabled(selectedItem != null && filterTable.getSelectionIndex() > 0);

		fBtnDown.setEnabled(selectedItem != null && filterTable.getSelectionIndex() < filterTable.getItemCount() - 1);
	}

	public void init(final IWorkbench workbench) {}

	private void onMoveDown() {

		final Object selectedItem = ((IStructuredSelection) fStatViewer.getSelection()).getFirstElement();
		if (selectedItem == null) {
			return;
		}

		final Table viewerTable = fStatViewer.getTable();
		final int selectionIndex = viewerTable.getSelectionIndex();

		if (selectionIndex < viewerTable.getItemCount() - 1) {

			fStatViewer.remove(selectedItem);
			fStatViewer.insert(selectedItem, selectionIndex + 1);

			// reselect moved item
			fStatViewer.setSelection(new StructuredSelection(selectedItem));

			if (viewerTable.getSelectionIndex() == viewerTable.getItemCount() - 1) {
				fBtnUp.setFocus();
			} else {
				fBtnDown.setFocus();
			}

			fIsModified = true;
		}
	}

	private void onMoveUp() {

		final Object selectedItem = ((IStructuredSelection) fStatViewer.getSelection()).getFirstElement();
		if (selectedItem == null) {
			return;
		}

		final Table viewerTable = fStatViewer.getTable();

		final int selectionIndex = viewerTable.getSelectionIndex();
		if (selectionIndex > 0) {
			fStatViewer.remove(selectedItem);
			fStatViewer.insert(selectedItem, selectionIndex - 1);

			// reselect moved item
			fStatViewer.setSelection(new StructuredSelection(selectedItem));

			if (viewerTable.getSelectionIndex() == 0) {
				fBtnDown.setFocus();
			} else {
				fBtnUp.setFocus();
			}

			fIsModified = true;
		}
	}

	@Override
	protected void performDefaults() {

		super.performDefaults();

		fIsModified = true;

		fVisibleStatistics = StatisticContainer.getStatisticExtensionPoints();

		fStatViewer.setInput(new Object());

		// select first statistic provider
		fStatViewer.setSelection(new StructuredSelection(fVisibleStatistics.get(0)));
	}

	@Override
	public boolean performOk() {

		final boolean isOK = super.performOk();

		if (isOK && fIsModified) {
			saveSettings();
		}

		return isOK;
	}

	private void saveSettings() {

		/*
		 * save order of all statistic providers in the pref store
		 */
		final TableItem[] items = fStatViewer.getTable().getItems();
		final String[] statisticIds = new String[items.length];

		for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
			statisticIds[itemIndex] = ((TourbookStatistic) items[itemIndex].getData()).fStatisticId;
		}

		// set new value and fire event
		TourbookPlugin.getDefault()
				.getPreferenceStore()
				.setValue(ITourbookPreferences.STATISTICS_STATISTIC_PROVIDER_IDS,
						StringToArrayConverter.convertArrayToString(statisticIds));
		
		
	}

}
