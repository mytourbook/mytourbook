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
package net.tourbook.ui.tourChart;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageAppearanceTourChart;
import net.tourbook.ui.ChartOptions_Grid;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Tour chart properties slideout.
 */
public class SlideoutTourChartOptions extends ToolbarSlideout {

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getPrefStore();

	private SelectionAdapter		_defaultSelectionListener;

	private ActionOpenPrefDialog	_actionPrefDialog;
	private Action					_actionRestoreDefaults;

	private ChartOptions_Grid		_gridUI;

	/*
	 * UI controls
	 */
	private TourChart				_tourChart;

	private Button					_chkShowBreaktimeValues;
	private Button					_chkShowSrtmData;
	private Button					_chkShowStartTimeOnXAxis;
	private Button					_chkShowValuePointTooltip;

	/**
	 * @param ownerControl
	 * @param toolBar
	 * @param tourChart
	 * @param gridPrefPrefix
	 */
	public SlideoutTourChartOptions(final Control ownerControl,
									final ToolBar toolBar,
									final TourChart tourChart,
									final String gridPrefPrefix) {

		super(ownerControl, toolBar);

		_tourChart = tourChart;

		_gridUI = new ChartOptions_Grid(gridPrefPrefix);
	}

	private void createActions() {

		/*
		 * Action: Restore default
		 */
		_actionRestoreDefaults = new Action() {
			@Override
			public void run() {
				resetToDefaults();
			}
		};

		_actionRestoreDefaults.setImageDescriptor(//
				TourbookPlugin.getImageDescriptor(Messages.Image__App_RestoreDefault));
		_actionRestoreDefaults.setToolTipText(Messages.App_Action_RestoreDefault_Tooltip);

		_actionPrefDialog = new ActionOpenPrefDialog(
				Messages.Tour_Action_EditChartPreferences,
				PrefPageAppearanceTourChart.ID);
		_actionPrefDialog.closeThisTooltip(this);
		_actionPrefDialog.setShell(_tourChart.getShell());
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		initUI();

		createActions();

		final Composite ui = createUI(parent);

		restoreState();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(shellContainer);
		{
			final Composite container = new Composite(shellContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults()//
					.numColumns(2)
					.applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
			{
				createUI_10_Title(container);
				createUI_12_Actions(container);
				createUI_20_Controls(container);

				_gridUI.createUI(container);
			}
		}

		return shellContainer;
	}

	private void createUI_10_Title(final Composite parent) {

		/*
		 * Label: Slideout title
		 */
		final Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(label);
		label.setText(Messages.Slideout_TourChartOptions_Label_Title);
		label.setFont(JFaceResources.getBannerFont());

		MTFont.setBannerFont(label);
	}

	private void createUI_12_Actions(final Composite parent) {

		final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.align(SWT.END, SWT.BEGINNING)
				.applyTo(toolbar);

		final ToolBarManager tbm = new ToolBarManager(toolbar);

		tbm.add(_actionRestoreDefaults);
		tbm.add(_actionPrefDialog);

		tbm.update(true);
	}

	private void createUI_20_Controls(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.span(2, 1)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Show break time values
				 */
				_chkShowBreaktimeValues = new Button(container, SWT.CHECK);
				_chkShowBreaktimeValues.setText(Messages.Tour_Action_ShowBreaktimeValues);

				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.applyTo(_chkShowBreaktimeValues);

				_chkShowBreaktimeValues.addSelectionListener(_defaultSelectionListener);
			}
			{
				/*
				 * Show start time on x-axis
				 */
				_chkShowStartTimeOnXAxis = new Button(container, SWT.CHECK);
				_chkShowStartTimeOnXAxis.setText(Messages.Tour_Action_show_start_time_on_x_axis);

				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.applyTo(_chkShowStartTimeOnXAxis);

				_chkShowStartTimeOnXAxis.addSelectionListener(_defaultSelectionListener);
			}
			{
				/*
				 * Show SRTM data
				 */
				_chkShowSrtmData = new Button(container, SWT.CHECK);
				_chkShowSrtmData.setText(Messages.tour_action_show_srtm_data);

				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.applyTo(_chkShowSrtmData);

				_chkShowSrtmData.addSelectionListener(_defaultSelectionListener);
			}
			{
				/*
				 * Show value point tooltip
				 */
				_chkShowValuePointTooltip = new Button(container, SWT.CHECK);
				_chkShowValuePointTooltip.setText(Messages.Tour_Action_ValuePointToolTip_IsVisible);

				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.applyTo(_chkShowValuePointTooltip);

				_chkShowValuePointTooltip.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {

						// set in pref store, tooltip is listening pref store modifications
						_prefStore.setValue(
								ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE,
								_chkShowValuePointTooltip.getSelection());

					}
				});
			}
		}
	}

	private void initUI() {

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeUI();
			}
		};
	}

	private void onChangeUI() {

		saveState();

		// update chart with new settings
		_tourChart.updateTourChart();
	}

	private void resetToDefaults() {

		final TourChartConfiguration tcc = _tourChart.getTourChartConfig();

		final boolean isShowBreaktimeValues = _prefStore.getDefaultBoolean(//
				ITourbookPreferences.GRAPH_IS_BREAKTIME_VALUES_VISIBLE);

		final boolean isTourStartTime = _prefStore.getDefaultBoolean(//
				ITourbookPreferences.GRAPH_X_AXIS_STARTTIME);

		final boolean isSrtmDataVisible = _prefStore.getDefaultBoolean(//
				ITourbookPreferences.GRAPH_X_AXIS_STARTTIME);

		final boolean isShowValuePointTooltip = _prefStore.getDefaultBoolean(//
				ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE);

		final X_AXIS_START_TIME xAxisStartTime = isTourStartTime
				? X_AXIS_START_TIME.TOUR_START_TIME
				: X_AXIS_START_TIME.START_WITH_0;

		tcc.isShowBreaktimeValues = isShowBreaktimeValues;
		tcc.isSRTMDataVisible = isSrtmDataVisible;
		tcc.xAxisTime = xAxisStartTime;

		_chkShowBreaktimeValues.setSelection(isShowBreaktimeValues);
		_chkShowSrtmData.setSelection(isSrtmDataVisible);
		_chkShowStartTimeOnXAxis.setSelection(isTourStartTime);
		_chkShowValuePointTooltip.setSelection(isShowValuePointTooltip);

		// this is not set in saveState()
		_prefStore.setValue(ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE, isShowValuePointTooltip);

		_gridUI.resetToDefaults();

		onChangeUI();
	}

	private void restoreState() {

		final TourChartConfiguration tcc = _tourChart.getTourChartConfig();

		if (tcc == null) {
			// this occured when tour chart is empty
			return;
		}

		final boolean canShowTimeOnXAxis = tcc.isShowTimeOnXAxis;
		final boolean canShowSRTMData = tcc.canShowSRTMData;

		_chkShowBreaktimeValues.setSelection(tcc.isShowBreaktimeValues);

		_chkShowSrtmData.setEnabled(canShowSRTMData);
		_chkShowSrtmData.setSelection(tcc.isSRTMDataVisible);

		_chkShowStartTimeOnXAxis.setEnabled(canShowTimeOnXAxis);
		_chkShowStartTimeOnXAxis.setSelection(tcc.xAxisTime == X_AXIS_START_TIME.TOUR_START_TIME);

		_chkShowValuePointTooltip.setSelection(_prefStore.getBoolean(//
				ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE));

		_gridUI.restoreState();
	}

	private void saveState() {

		final TourChartConfiguration tcc = _tourChart.getTourChartConfig();

		final boolean isShowBreaktimeValues = _chkShowBreaktimeValues.getSelection();
		final boolean isTourStartTime = _chkShowStartTimeOnXAxis.getSelection();
		final boolean isSrtmDataVisible = _chkShowSrtmData.getSelection();

		final X_AXIS_START_TIME xAxisStartTime = isTourStartTime
				? X_AXIS_START_TIME.TOUR_START_TIME
				: X_AXIS_START_TIME.START_WITH_0;

		/*
		 * Update pref store
		 */
		_prefStore.setValue(ITourbookPreferences.GRAPH_IS_BREAKTIME_VALUES_VISIBLE, isShowBreaktimeValues);
		_prefStore.setValue(ITourbookPreferences.GRAPH_IS_SRTM_VISIBLE, isSrtmDataVisible);
		_prefStore.setValue(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME, isTourStartTime);

		_gridUI.saveState();

		_tourChart.setupChartConfig();

		/*
		 * Update chart config
		 */
		tcc.isShowBreaktimeValues = isShowBreaktimeValues;
		tcc.isSRTMDataVisible = isSrtmDataVisible;
		tcc.xAxisTime = xAxisStartTime;
	}

}
