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
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageAppearanceTourChart;
import net.tourbook.ui.ChartOptions_GridUI;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Tour chart marker properties slideout.
 */
public class SlideoutTourChartOptions extends AnimatedToolTipShell {

	private final IPreferenceStore	_prefStore			= TourbookPlugin.getPrefStore();

	// initialize with default values which are (should) never be used
	private Rectangle				_toolTipItemBounds	= new Rectangle(0, 0, 50, 50);

	private final WaitTimer			_waitTimer			= new WaitTimer();

	private boolean					_isWaitTimerStarted;
	private boolean					_canOpenToolTip;
	private boolean					_isAnotherDialogOpened;

	private SelectionAdapter		_defaultSelectionListener;


	private ActionOpenPrefDialog	_actionPrefDialog;
	private Action					_actionRestoreDefaults;

	private ChartOptions_GridUI		_gridUI				= new ChartOptions_GridUI();

	/*
	 * UI controls
	 */
	private Composite				_shellContainer;
	private TourChart				_tourChart;

	private Button					_chkShowBreaktimeValues;
	private Button					_chkShowSrtmData;
	private Button					_chkShowStartTimeOnXAxis;
	private Button					_chkShowValuePointTooltip;

	private final class WaitTimer implements Runnable {
		@Override
		public void run() {
			open_Runnable();
		}
	}

	public SlideoutTourChartOptions(final Control ownerControl, final ToolBar toolBar, final TourChart tourChart) {

		super(ownerControl);

		_tourChart = tourChart;

		addListener(ownerControl, toolBar);

		setToolTipCreateStyle(AnimatedToolTipShell.TOOLTIP_STYLE_KEEP_CONTENT);
		setBehaviourOnMouseOver(AnimatedToolTipShell.MOUSE_OVER_BEHAVIOUR_IGNORE_OWNER);
		setIsKeepShellOpenWhenMoved(false);

		setFadeInSteps(1);
		setFadeOutSteps(10);
		setFadeOutDelaySteps(1);
	}

	private void addListener(final Control ownerControl, final ToolBar toolBar) {

		toolBar.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseExit(final MouseEvent e) {

				// prevent to open the tooltip
				_canOpenToolTip = false;
			}
		});
	}

	@Override
	protected void beforeHideToolTip() {

	}

	@Override
	protected boolean canCloseToolTip() {

		/*
		 * Do not hide this dialog when the color selector dialog or other dialogs are opened
		 * because it will lock the UI completely !!!
		 */

		final boolean isCanClose = _isAnotherDialogOpened == false;

		return isCanClose;
	}

	@Override
	protected boolean canShowToolTip() {
		return true;
	}

	@Override
	protected boolean closeShellAfterHidden() {

		/*
		 * Close the tooltip that the state is saved.
		 */

		return true;
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

		/*
		 * Set shell to the parent otherwise the pref dialog is closed when the slideout is closed.
		 */
		_actionPrefDialog.setShell(_tourChart.getShell());
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		initUI();

		createActions();

		final Composite ui = createUI(parent);

		restoreState();

		enableControls();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		_shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(_shellContainer);
		{
			final Composite container = new Composite(_shellContainer, SWT.NONE);
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

		return _shellContainer;
	}

	private void createUI_10_Title(final Composite parent) {

		/*
		 * Label: Slideout title
		 */
		final Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(label);
		label.setText(Messages.Slideout_TourChartOptions_Label_Title);
		label.setFont(JFaceResources.getBannerFont());
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

//		addItem(tourChartActions.get(TourChart.ACTION_ID_IS_SHOW_BREAKTIME_VALUES));
//		addItem(tourChartActions.get(TourChart.ACTION_ID_IS_SHOW_START_TIME));
//		addItem(tourChartActions.get(TourChart.ACTION_ID_IS_SHOW_SRTM_DATA));
//		addItem(tourChartActions.get(TourChart.ACTION_ID_IS_SHOW_VALUEPOINT_TOOLTIP));
//
//		(new Separator()).fill(_menu, -1);
//		addItem(tourChartActions.get(TourChart.ACTION_ID_CAN_AUTO_ZOOM_TO_SLIDER));
//		addItem(tourChartActions.get(TourChart.ACTION_ID_CAN_MOVE_SLIDERS_WHEN_ZOOMED));
//
//		(new Separator()).fill(_menu, -1);
//		addItem(tourChartActions.get(TourChart.ACTION_ID_EDIT_CHART_PREFERENCES));

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

	private void enableControls() {

//		final boolean isShowInfoTooltip = _chkShowInfoTooltip.getSelection();
//
//		_lblTooltipDelay.setEnabled(isShowInfoTooltip);
//		_spinTooltipDelay.setEnabled(isShowInfoTooltip);
	}

	public Shell getShell() {

		if (_shellContainer == null) {
			return null;
		}

		return _shellContainer.getShell();
	}

	@Override
	public Point getToolTipLocation(final Point tipSize) {

//		final int tipWidth = tipSize.x;
		final int tipHeight = tipSize.y;

//		final int itemWidth = _toolTipItemBounds.width;
		final int itemHeight = _toolTipItemBounds.height;

		// center horizontally
		final int devX = _toolTipItemBounds.x;// + itemWidth / 2 - tipWidth / 2;
		int devY = _toolTipItemBounds.y + itemHeight + 0;

		final Rectangle displayBounds = this.getShell().getDisplay().getBounds();

		if (devY + tipHeight > displayBounds.height) {

			// slideout is below bottom, show it above the action button

			devY = _toolTipItemBounds.y - tipHeight;
		}

		return new Point(devX, devY);

	}

	private void initUI() {
		
		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeUI();
			}
		};
	}

	@Override
	protected Rectangle noHideOnMouseMove() {

		return _toolTipItemBounds;
	}

	private void onChangeUI() {

		saveState();

		// update chart with new settings
		_tourChart.updateTourChart();

		enableControls();
	}

	/**
	 * @param toolTipItemBounds
	 * @param isOpenDelayed
	 */
	public void open(final Rectangle toolTipItemBounds, final boolean isOpenDelayed) {

		if (isToolTipVisible()) {
			return;
		}

		if (isOpenDelayed == false) {

			if (toolTipItemBounds != null) {

				_toolTipItemBounds = toolTipItemBounds;

				showToolTip();
			}

		} else {

			if (toolTipItemBounds == null) {

				// item is not hovered any more

				_canOpenToolTip = false;

				return;
			}

			_toolTipItemBounds = toolTipItemBounds;
			_canOpenToolTip = true;

			if (_isWaitTimerStarted == false) {

				_isWaitTimerStarted = true;

				Display.getCurrent().timerExec(50, _waitTimer);
			}
		}
	}

	private void open_Runnable() {

		_isWaitTimerStarted = false;

		if (_canOpenToolTip) {
			showToolTip();
		}
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
