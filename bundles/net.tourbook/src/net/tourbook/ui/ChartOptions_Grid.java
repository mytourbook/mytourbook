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
package net.tourbook.ui;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

public class ChartOptions_Grid {

	public static int				GRID_ALL						= 0;
	public static int				GRID_HORIZONTAL_DISTANCE		= 1 << 1;
	public static int				GRID_VERTICAL_DISTANCE			= 1 << 2;
	public static int				GRID_IS_SHOW_HORIZONTAL_LINE	= 1 << 3;
	public static int				GRID_IS_SHOW_VERTICAL_LINE		= 1 << 4;

	private final IPreferenceStore	_prefStore						= TourbookPlugin.getPrefStore();

	private SelectionAdapter		_defaultSelectionListener;
	private MouseWheelListener		_defaultMouseWheelListener;
	private SelectionAdapter		_gridLineListener;

	private String					_prefStorePrefix;

	/*
	 * UI controls
	 */
	private Button					_chkShowGrid_HorizontalLines;
	private Button					_chkShowGrid_VerticalLines;

	private Label					_lblGridHorizontal;
	private Label					_lblGridHorizontal_Unit;
	private Label					_lblGridVertical;
	private Label					_lblGridVertical_Unit;

	private Spinner					_spinnerGridHorizontalDistance;
	private Spinner					_spinnerGridVerticalDistance;

	@SuppressWarnings("unused")
	private ChartOptions_Grid() {}

	/**
	 * @param prefStorePrefix
	 *            Prefix for the grid pref store values.
	 */
	public ChartOptions_Grid(final String prefStorePrefix) {

		_prefStorePrefix = prefStorePrefix;
	}

	public void createUI(final Composite parent) {

		initUI(parent);

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Pref_Graphs_Group_Grid);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.span(2, 1)
				.applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
//		group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			{
				/*
				 * label: grid distance
				 */
				final Label label = new Label(group, SWT.NONE);
				label.setText(Messages.Pref_Graphs_grid_distance);
				GridDataFactory.fillDefaults()//
						.span(3, 1)
						.applyTo(label);
			}

			{
				/*
				 * label: horizontal grid
				 */
				_lblGridHorizontal = new Label(group, SWT.NONE);
				_lblGridHorizontal.setText(Messages.Pref_Graphs_grid_horizontal_distance);
				GridDataFactory.fillDefaults()//
//						.indent(16, 0)
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_lblGridHorizontal);

				/*
				 * spinner: horizontal grid
				 */
				_spinnerGridHorizontalDistance = new Spinner(group, SWT.BORDER);
				_spinnerGridHorizontalDistance.setMinimum(10);
				_spinnerGridHorizontalDistance.setMaximum(1000);
				_spinnerGridHorizontalDistance.addMouseWheelListener(_defaultMouseWheelListener);
				_spinnerGridHorizontalDistance.addSelectionListener(_defaultSelectionListener);
				GridDataFactory.fillDefaults() //
						.align(SWT.BEGINNING, SWT.FILL)
						.applyTo(_spinnerGridHorizontalDistance);

				/*
				 * Label: px
				 */
				_lblGridHorizontal_Unit = new Label(group, SWT.NONE);
				_lblGridHorizontal_Unit.setText(Messages.App_Unit_Px);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_lblGridHorizontal_Unit);
			}

			{
				/*
				 * label: vertical grid
				 */
				_lblGridVertical = new Label(group, SWT.NONE);
				_lblGridVertical.setText(Messages.Pref_Graphs_grid_vertical_distance);
				GridDataFactory.fillDefaults()//
//						.indent(16, 0)
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_lblGridVertical);

				/*
				 * spinner: vertical grid
				 */
				_spinnerGridVerticalDistance = new Spinner(group, SWT.BORDER);
				_spinnerGridVerticalDistance.setMinimum(10);
				_spinnerGridVerticalDistance.setMaximum(1000);
				_spinnerGridVerticalDistance.addMouseWheelListener(_defaultMouseWheelListener);
				_spinnerGridVerticalDistance.addSelectionListener(_defaultSelectionListener);
				GridDataFactory.fillDefaults() //
						.align(SWT.BEGINNING, SWT.FILL)
						.applyTo(_spinnerGridVerticalDistance);
				/*
				 * Label: px
				 */
				_lblGridVertical_Unit = new Label(group, SWT.NONE);
				_lblGridVertical_Unit.setText(Messages.App_Unit_Px);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_lblGridVertical_Unit);
			}
			{
				/*
				 * checkbox: show horizontal grid
				 */
				_chkShowGrid_HorizontalLines = new Button(group, SWT.CHECK);
				_chkShowGrid_HorizontalLines.setText(Messages.Pref_Graphs_Checkbox_ShowHorizontalGrid);
				_chkShowGrid_HorizontalLines.setToolTipText(Messages.Pref_Graphs_Dialog_GridLine_Warning_Message);
				_chkShowGrid_HorizontalLines.addSelectionListener(_gridLineListener);
				GridDataFactory.fillDefaults()//
//						.indent(0, 15)
						.span(3, 1)
						.applyTo(_chkShowGrid_HorizontalLines);
			}
			{
				/*
				 * checkbox: show vertical grid
				 */
				_chkShowGrid_VerticalLines = new Button(group, SWT.CHECK);
				_chkShowGrid_VerticalLines.setText(Messages.Pref_Graphs_Checkbox_ShowVerticalGrid);
				_chkShowGrid_VerticalLines.setToolTipText(Messages.Pref_Graphs_Dialog_GridLine_Warning_Message);
				_chkShowGrid_VerticalLines.addSelectionListener(_gridLineListener);
				GridDataFactory.fillDefaults()//
						.span(3, 1)
						.applyTo(_chkShowGrid_VerticalLines);
			}
		}
	}

	public void enableGridOptions(final int gridOptions) {

		final boolean isShowAll = gridOptions == GRID_ALL;

		final boolean isGridHorizontalDistance = isShowAll || (gridOptions & GRID_HORIZONTAL_DISTANCE) != 0;
		final boolean isGridVerticalDistance = isShowAll || (gridOptions & GRID_VERTICAL_DISTANCE) != 0;
		final boolean isShowGridHorizontal = isShowAll || (gridOptions & GRID_IS_SHOW_HORIZONTAL_LINE) != 0;
		final boolean isShowGridVertical = isShowAll || (gridOptions & GRID_IS_SHOW_VERTICAL_LINE) != 0;

		_lblGridHorizontal.setEnabled(isGridHorizontalDistance);
		_lblGridHorizontal_Unit.setEnabled(isGridHorizontalDistance);
		_spinnerGridHorizontalDistance.setEnabled(isGridHorizontalDistance);

		_lblGridVertical.setEnabled(isGridVerticalDistance);
		_lblGridVertical_Unit.setEnabled(isGridVerticalDistance);
		_spinnerGridVerticalDistance.setEnabled(isGridVerticalDistance);

		_chkShowGrid_HorizontalLines.setEnabled(isShowGridHorizontal);
		_chkShowGrid_VerticalLines.setEnabled(isShowGridVertical);
	}

	private void initUI(final Composite parent) {

		_gridLineListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectGridLine();
			}
		};

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeUI();
			}
		};

		_defaultMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				net.tourbook.common.UI.adjustSpinnerValueOnMouseScroll(event);
				onChangeUI();
			}
		};
	}

	private void onChangeUI() {

		saveState();
	}

	private void onSelectGridLine() {

		// run async otherwise the update of the dialog box UI is slooooow
//		_parent.getDisplay().asyncExec(new Runnable() {
//			@Override
//			public void run() {
//				onChangeUI();
//			}
//		});

		onChangeUI();
	}

	public void resetToDefaults() {

		_spinnerGridHorizontalDistance.setSelection(//
				_prefStore.getDefaultInt(ITourbookPreferences.CHART_GRID_HORIZONTAL_DISTANCE));
		_spinnerGridVerticalDistance.setSelection(//
				_prefStore.getDefaultInt(ITourbookPreferences.CHART_GRID_VERTICAL_DISTANCE));

		_chkShowGrid_HorizontalLines.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.CHART_GRID_IS_SHOW_HORIZONTAL_GRIDLINES));
		_chkShowGrid_VerticalLines.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.CHART_GRID_IS_SHOW_VERTICAL_GRIDLINES));

		onChangeUI();
	}

	public void restoreState() {

		_spinnerGridHorizontalDistance.setSelection(//
				Util
						.getPrefixPrefInt(
								_prefStore,
								_prefStorePrefix,
								ITourbookPreferences.CHART_GRID_HORIZONTAL_DISTANCE));
		_spinnerGridVerticalDistance.setSelection(//
				Util.getPrefixPrefInt(_prefStore, _prefStorePrefix, ITourbookPreferences.CHART_GRID_VERTICAL_DISTANCE));

		_chkShowGrid_HorizontalLines.setSelection(//
				Util.getPrefixPrefBoolean(
						_prefStore,
						_prefStorePrefix,
						ITourbookPreferences.CHART_GRID_IS_SHOW_HORIZONTAL_GRIDLINES));
		_chkShowGrid_VerticalLines.setSelection(//
				Util.getPrefixPrefBoolean(
						_prefStore,
						_prefStorePrefix,
						ITourbookPreferences.CHART_GRID_IS_SHOW_VERTICAL_GRIDLINES));
	}

	public void saveState() {

		_prefStore.setValue(_prefStorePrefix + ITourbookPreferences.CHART_GRID_HORIZONTAL_DISTANCE, //
				_spinnerGridHorizontalDistance.getSelection());
		_prefStore.setValue(_prefStorePrefix + ITourbookPreferences.CHART_GRID_VERTICAL_DISTANCE, //
				_spinnerGridVerticalDistance.getSelection());

		_prefStore.setValue(_prefStorePrefix + ITourbookPreferences.CHART_GRID_IS_SHOW_HORIZONTAL_GRIDLINES, //
				_chkShowGrid_HorizontalLines.getSelection());
		_prefStore.setValue(_prefStorePrefix + ITourbookPreferences.CHART_GRID_IS_SHOW_VERTICAL_GRIDLINES, //
				_chkShowGrid_VerticalLines.getSelection());
	}
}
