/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.StringToArrayConverter;
import net.tourbook.statistic.StatisticManager;
import net.tourbook.statistic.TourbookStatistic;

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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageStatistic extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String						ID						= "net.tourbook.preferences.PrefPageStatistic"; //$NON-NLS-1$

	private TableViewer								_statViewer;

	private boolean									_isModified				= false;

	private ArrayList<TourbookStatistic>			_visibleStatistics;

	private Button									_btnMoveDown;
	private Button									_btnMoveUp;
	private Button									_btnSortByData;
	private Button									_btnSortByTime;

//  <attribute name="category-data" use="required">
//  <annotation>
//     <documentation>
//
//     </documentation>
//  </annotation>
//  <simpleType>
//     <restriction base="string">
//        <enumeration value="Other">
//        </enumeration>
//        <enumeration value="Summary">
//        </enumeration>
//        <enumeration value="Altitude">
//        </enumeration>
//        <enumeration value="Distance">
//        </enumeration>
//        <enumeration value="Time">
//        </enumeration>
//        <enumeration value="HR">
//        </enumeration>
//     </restriction>
//  </simpleType>
//	</attribute>

	private static final HashMap<String, Integer>	_sortingByCategoryData	= new HashMap<>();
	{
		_sortingByCategoryData.put("Summary", 1); //$NON-NLS-1$
		_sortingByCategoryData.put("HR", 2); //$NON-NLS-1$
		_sortingByCategoryData.put("Time", 3); //$NON-NLS-1$
		_sortingByCategoryData.put("Altitude", 4); //$NON-NLS-1$
		_sortingByCategoryData.put("Distance", 5); //$NON-NLS-1$
		_sortingByCategoryData.put("Other", 99); //$NON-NLS-1$
	}

//    <attribute name="category-time" use="required">
//    <annotation>
//       <documentation>
//
//       </documentation>
//    </annotation>
//    <simpleType>
//       <restriction base="string">
//          <enumeration value="Other">
//          </enumeration>
//          <enumeration value="Day">
//          </enumeration>
//          <enumeration value="Week">
//          </enumeration>
//          <enumeration value="Month">
//          </enumeration>
//          <enumeration value="Year">
//          </enumeration>
//       </restriction>
//    </simpleType>
//    </attribute>

	private static final HashMap<String, Integer>	_sortingByCategoryTime	= new HashMap<>();
	{
		_sortingByCategoryTime.put("Day", 1); //$NON-NLS-1$
		_sortingByCategoryTime.put("Week", 2); //$NON-NLS-1$
		_sortingByCategoryTime.put("Month", 3); //$NON-NLS-1$
		_sortingByCategoryTime.put("Year", 4); //$NON-NLS-1$
		_sortingByCategoryTime.put("Other", 99); //$NON-NLS-1$
	}

	private class StatContentProvicer implements IStructuredContentProvider {

		@Override
		public void dispose() {}

		@Override
		public Object[] getElements(final Object inputElement) {
			return _visibleStatistics.toArray();
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	public PrefPageStatistic() {}

	public PrefPageStatistic(final String title) {
		super(title);
	}

	public PrefPageStatistic(final String title, final ImageDescriptor image) {
		super(title, image);
	}

	@Override
	protected Control createContents(final Composite parent) {

		final Composite uiContainer = createUI(parent);

		_visibleStatistics = StatisticManager.getStatisticProviders();

		// load viewer
		_statViewer.setInput(new Object());

		// select first statistic provider
		_statViewer.setSelection(new StructuredSelection(_visibleStatistics.get(0)));

		return uiContainer;
	}

	private Composite createUI(final Composite parent) {

		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.pref_statistic_lbl_info);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			createUI_10_List(container);
			createUI_20_Actions(container);
		}

		return container;
	}

	private void createUI_10_List(final Composite parent) {

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

		_statViewer = new TableViewer(table);

		/*
		 * create columns
		 */
		TableViewerColumn tvc;

		// column: map provider
		tvc = new TableViewerColumn(_statViewer, SWT.LEAD);
		tvc.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourbookStatistic statistic = (TourbookStatistic) cell.getElement();

				final String statisticName = statistic.plugin_VisibleName

// THIS IS FOR DEBUGGING
//						+ UI.SPACE3
//						+ UI.SYMBOL_BRACKET_LEFT
//						+ statistic.plugin_Category_Data
//						+ UI.DASH_WITH_SPACE
//						+ statistic.plugin_Category_Time
//						+ UI.SYMBOL_BRACKET_RIGHT
				;

				cell.setText(statisticName);
			}
		});
		tableLayout.setColumnData(tvc.getColumn(), new ColumnWeightData(4, true));

		/*
		 * create table viewer
		 */
		_statViewer.setContentProvider(new StatContentProvicer());
		_statViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				enableActions();
			}
		});
	}

	private void createUI_20_Actions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			{
				// Button: move up
				_btnMoveUp = new Button(container, SWT.NONE);
				_btnMoveUp.setText(Messages.app_action_button_up);
				setButtonLayoutData(_btnMoveUp);
				_btnMoveUp.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onMoveUp();
					}
				});
			}

			{
				// Button: move down
				_btnMoveDown = new Button(container, SWT.NONE);
				_btnMoveDown.setText(Messages.app_action_button_down);
				setButtonLayoutData(_btnMoveDown);
				_btnMoveDown.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onMoveDown();
					}
				});
			}

			{
				// Button: Sort by data
				_btnSortByData = new Button(container, SWT.NONE);
				_btnSortByData.setText(Messages.Pref_Statistic_Action_SortByData);
				setButtonLayoutData(_btnSortByData);
				_btnSortByData.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSortByData();
					}
				});
			}

			{
				// Button: Sort by time
				_btnSortByTime = new Button(container, SWT.NONE);
				_btnSortByTime.setText(Messages.Pref_Statistic_Action_SortByTime);
				setButtonLayoutData(_btnSortByTime);
				_btnSortByTime.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSortByTime();
					}
				});
			}
		}
	}

	private void enableActions() {

		final IStructuredSelection selection = (IStructuredSelection) _statViewer.getSelection();

		final Object selectedItem = selection.getFirstElement();
		final Table filterTable = _statViewer.getTable();

		_btnMoveUp.setEnabled(selectedItem != null && filterTable.getSelectionIndex() > 0);
		_btnMoveDown.setEnabled(selectedItem != null
				&& filterTable.getSelectionIndex() < filterTable.getItemCount() - 1);
	}

	@Override
	public void init(final IWorkbench workbench) {}

	private void onMoveDown() {

		final Object selectedItem = ((IStructuredSelection) _statViewer.getSelection()).getFirstElement();
		if (selectedItem == null) {
			return;
		}

		final Table viewerTable = _statViewer.getTable();
		final int selectionIndex = viewerTable.getSelectionIndex();

		if (selectionIndex < viewerTable.getItemCount() - 1) {

			_statViewer.remove(selectedItem);
			_statViewer.insert(selectedItem, selectionIndex + 1);

			// reselect moved item
			_statViewer.setSelection(new StructuredSelection(selectedItem));

			if (viewerTable.getSelectionIndex() == viewerTable.getItemCount() - 1) {
				_btnMoveUp.setFocus();
			} else {
				_btnMoveDown.setFocus();
			}

			_isModified = true;
		}
	}

	private void onMoveUp() {

		final Object selectedItem = ((IStructuredSelection) _statViewer.getSelection()).getFirstElement();
		if (selectedItem == null) {
			return;
		}

		final Table viewerTable = _statViewer.getTable();

		final int selectionIndex = viewerTable.getSelectionIndex();
		if (selectionIndex > 0) {
			_statViewer.remove(selectedItem);
			_statViewer.insert(selectedItem, selectionIndex - 1);

			// reselect moved item
			_statViewer.setSelection(new StructuredSelection(selectedItem));

			if (viewerTable.getSelectionIndex() == 0) {
				_btnMoveDown.setFocus();
			} else {
				_btnMoveUp.setFocus();
			}

			_isModified = true;
		}
	}

	private void onSortByData() {

		Collections.sort(_visibleStatistics, new Comparator<TourbookStatistic>() {

			@Override
			public int compare(final TourbookStatistic stat1, final TourbookStatistic stat2) {

				final int stat1Sorting = _sortingByCategoryData.get(stat1.plugin_Category_Data);
				final int stat2Sorting = _sortingByCategoryData.get(stat2.plugin_Category_Data);

				return stat1Sorting - stat2Sorting;
			}
		});

		updateUI_WithReselection();
	}

	private void onSortByTime() {

		Collections.sort(_visibleStatistics, new Comparator<TourbookStatistic>() {

			@Override
			public int compare(final TourbookStatistic stat1, final TourbookStatistic stat2) {

				final int stat1Sorting = _sortingByCategoryTime.get(stat1.plugin_Category_Time);
				final int stat2Sorting = _sortingByCategoryTime.get(stat2.plugin_Category_Time);

				return stat1Sorting - stat2Sorting;
			}
		});

		updateUI_WithReselection();
	}

	@Override
	protected void performDefaults() {

		super.performDefaults();

		// use default sorting
		_visibleStatistics = StatisticManager.getStatisticExtensionPoints();

		updateUI_WithReselection();
	}

	@Override
	public boolean performOk() {

		final boolean isOK = super.performOk();

		if (isOK && _isModified) {
			saveState();
		}

		return isOK;
	}

	private void saveState() {

		/*
		 * save order of all statistic providers in the pref store
		 */
		final TableItem[] items = _statViewer.getTable().getItems();
		final String[] statisticIds = new String[items.length];

		for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
			statisticIds[itemIndex] = ((TourbookStatistic) items[itemIndex].getData()).plugin_StatisticId;
		}

		// set new value and fire event
		TourbookPlugin
				.getDefault()
				.getPreferenceStore()
				.setValue(
						ITourbookPreferences.STATISTICS_STATISTIC_PROVIDER_IDS,
						StringToArrayConverter.convertArrayToString(statisticIds));

	}

	private void updateUI_WithReselection() {

		final Object selectedItem = ((IStructuredSelection) _statViewer.getSelection()).getFirstElement();

		_isModified = true;

		_statViewer.setInput(new Object());

		// select first statistic provider
		_statViewer.setSelection(new StructuredSelection(selectedItem == null
				? _visibleStatistics.get(0)
				: selectedItem));
	}

}
