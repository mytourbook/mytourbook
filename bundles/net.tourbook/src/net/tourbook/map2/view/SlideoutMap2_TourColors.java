/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
package net.tourbook.map2.view;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.color.MapGraphId;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Slideout for the map2 tour colors
 */
public class SlideoutMap2_TourColors extends ToolbarSlideout {

	private IDialogSettings		_state;

	private Action				_actionRestoreDefaults;

	private SelectionAdapter	_defaultSelectionListener;

	/*
	 * UI controls
	 */
	private Map2View			_map2View;

	private Button				_chkShowInChartToolbar_Altitude;
	private Button				_chkShowInChartToolbar_Gradient;
	private Button				_chkShowInChartToolbar_Pace;
	private Button				_chkShowInChartToolbar_Pulse;
	private Button				_chkShowInChartToolbar_Speed;
	private Button				_chkShowInChartToolbar_RunDyn_StepLength;
	private Button				_chkShowInChartToolbar_HrZone;

	public SlideoutMap2_TourColors(	final Control ownerControl,
									final ToolBar toolBar,
									final Map2View map2View,
									final IDialogSettings state) {

		super(ownerControl, toolBar);

		_map2View = map2View;
		_state = state;
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
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		initUI(parent);

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
			GridLayoutFactory
					.fillDefaults()//
					.numColumns(2)
					.applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
			{
				createUI_10_Title(container);
				createUI_12_Actions(container);
			}
			createUI_20_Graphs(shellContainer);
		}

		return shellContainer;
	}

	private void createUI_10_Title(final Composite parent) {

		/*
		 * Label: Slideout title
		 */
		final Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(label);
		label.setText(Messages.Slideout_Map_TourColors_Label_Title);
		MTFont.setBannerFont(label);
	}

	private void createUI_12_Actions(final Composite parent) {

		final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.align(SWT.END, SWT.BEGINNING)
				.applyTo(toolbar);

		final ToolBarManager tbm = new ToolBarManager(toolbar);

		tbm.add(_actionRestoreDefaults);

		tbm.update(true);
	}

	private void createUI_20_Graphs(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(7).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{

			{
				/*
				 * Actions: chart graphs
				 */
				{
					createUI_GraphAction(container, MapGraphId.Altitude);
					createUI_GraphAction(container, MapGraphId.Pulse);
					createUI_GraphAction(container, MapGraphId.Speed);
					createUI_GraphAction(container, MapGraphId.Pace);
					createUI_GraphAction(container, MapGraphId.Gradient);
					createUI_GraphAction(container, MapGraphId.RunDyn_StepLength);
					createUI_GraphAction(container, MapGraphId.HrZone);
				}
				{
					/*
					 * Checkbox: show in chart toolbar
					 */
					_chkShowInChartToolbar_Altitude = createUI_GraphCheckbox(container);
					_chkShowInChartToolbar_Pulse = createUI_GraphCheckbox(container);
					_chkShowInChartToolbar_Speed = createUI_GraphCheckbox(container);
					_chkShowInChartToolbar_Pace = createUI_GraphCheckbox(container);
					_chkShowInChartToolbar_Gradient = createUI_GraphCheckbox(container);
					_chkShowInChartToolbar_RunDyn_StepLength = createUI_GraphCheckbox(container);
					_chkShowInChartToolbar_HrZone = createUI_GraphCheckbox(container);
				}
			}
		}
	}

	private void createUI_GraphAction(final Composite parent, final MapGraphId graphId) {

		final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
		final ToolBarManager tbm = new ToolBarManager(toolbar);

		tbm.add(_map2View.getTourColorAction(graphId));

		tbm.update(true);
	}

	private Button createUI_GraphCheckbox(final Composite parent) {

		final Button checkbox = new Button(parent, SWT.CHECK);

		checkbox.setToolTipText(Messages.Slideout_Map_TourColors_Checkbox_ShowInChartToolbar_Tooltip);
		checkbox.addSelectionListener(_defaultSelectionListener);

		GridDataFactory
				.fillDefaults()
				.grab(true, false)
				.align(SWT.CENTER, SWT.FILL)
				.applyTo(checkbox);

		return checkbox;
	}

	private void initUI(final Composite parent) {

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeUI();
			}
		};
	}

	@Override
	protected boolean isCenterHorizontal() {
		return true;
	}

	private void onChangeUI() {

		saveState();

		// update chart toolbar
		_map2View.updateTourColorsInToolbar();
	}

	@Override
	protected void onDispose() {

	}

	private void resetToDefaults() {

// SET_FORMATTING_OFF
		
		_state.put(Map2View.STATE_IS_SHOW_IN_TOOLBAR_ALTITUDE, 		Map2View.STATE_IS_SHOW_IN_TOOLBAR_ALTITUDE_DEFAULT);
		_state.put(Map2View.STATE_IS_SHOW_IN_TOOLBAR_GRADIENT, 		Map2View.STATE_IS_SHOW_IN_TOOLBAR_GRADIENT_DEFAULT);
		_state.put(Map2View.STATE_IS_SHOW_IN_TOOLBAR_HR_ZONE,		Map2View.STATE_IS_SHOW_IN_TOOLBAR_HR_ZONE_DEFAULT);
		_state.put(Map2View.STATE_IS_SHOW_IN_TOOLBAR_PACE, 			Map2View.STATE_IS_SHOW_IN_TOOLBAR_PACE_DEFAULT);
		_state.put(Map2View.STATE_IS_SHOW_IN_TOOLBAR_PULSE, 		Map2View.STATE_IS_SHOW_IN_TOOLBAR_PULSE_DEFAULT);
		_state.put(Map2View.STATE_IS_SHOW_IN_TOOLBAR_SPEED, 		Map2View.STATE_IS_SHOW_IN_TOOLBAR_SPEED_DEFAULT);
		
// SET_FORMATTING_ON

		// update UI
		restoreState();

		// update chart toolbar
		_map2View.updateTourColorsInToolbar();
	}

	private void restoreState() {

// SET_FORMATTING_OFF
		
		_chkShowInChartToolbar_Altitude.setSelection(			Util.getStateBoolean(_state, Map2View.STATE_IS_SHOW_IN_TOOLBAR_ALTITUDE,			Map2View.STATE_IS_SHOW_IN_TOOLBAR_ALTITUDE_DEFAULT));
		_chkShowInChartToolbar_Gradient.setSelection(			Util.getStateBoolean(_state, Map2View.STATE_IS_SHOW_IN_TOOLBAR_GRADIENT,			Map2View.STATE_IS_SHOW_IN_TOOLBAR_GRADIENT_DEFAULT));
		_chkShowInChartToolbar_HrZone.setSelection(				Util.getStateBoolean(_state, Map2View.STATE_IS_SHOW_IN_TOOLBAR_HR_ZONE,				Map2View.STATE_IS_SHOW_IN_TOOLBAR_HR_ZONE_DEFAULT));
		_chkShowInChartToolbar_Pace.setSelection(				Util.getStateBoolean(_state, Map2View.STATE_IS_SHOW_IN_TOOLBAR_PACE,				Map2View.STATE_IS_SHOW_IN_TOOLBAR_PACE_DEFAULT));
		_chkShowInChartToolbar_Pulse.setSelection(				Util.getStateBoolean(_state, Map2View.STATE_IS_SHOW_IN_TOOLBAR_PULSE,				Map2View.STATE_IS_SHOW_IN_TOOLBAR_PULSE_DEFAULT));
		_chkShowInChartToolbar_RunDyn_StepLength.setSelection(	Util.getStateBoolean(_state, Map2View.STATE_IS_SHOW_IN_TOOLBAR_RUN_DYN_STEP_LENGTH,	Map2View.STATE_IS_SHOW_IN_TOOLBAR_RUN_DYN_STEP_LENGTH_DEFAULT));
		_chkShowInChartToolbar_Speed.setSelection(				Util.getStateBoolean(_state, Map2View.STATE_IS_SHOW_IN_TOOLBAR_SPEED,				Map2View.STATE_IS_SHOW_IN_TOOLBAR_SPEED_DEFAULT));
		
// SET_FORMATTING_ON
	}

	private void saveState() {

// SET_FORMATTING_OFF
		
		_state.put(Map2View.STATE_IS_SHOW_IN_TOOLBAR_ALTITUDE, 				_chkShowInChartToolbar_Altitude.getSelection());
		_state.put(Map2View.STATE_IS_SHOW_IN_TOOLBAR_GRADIENT, 				_chkShowInChartToolbar_Gradient.getSelection());
		_state.put(Map2View.STATE_IS_SHOW_IN_TOOLBAR_HR_ZONE,				_chkShowInChartToolbar_HrZone.getSelection());
		_state.put(Map2View.STATE_IS_SHOW_IN_TOOLBAR_PACE, 					_chkShowInChartToolbar_Pace.getSelection());
		_state.put(Map2View.STATE_IS_SHOW_IN_TOOLBAR_PULSE, 				_chkShowInChartToolbar_Pulse.getSelection());
		_state.put(Map2View.STATE_IS_SHOW_IN_TOOLBAR_RUN_DYN_STEP_LENGTH,	_chkShowInChartToolbar_RunDyn_StepLength.getSelection());
		_state.put(Map2View.STATE_IS_SHOW_IN_TOOLBAR_SPEED, 				_chkShowInChartToolbar_Speed.getSelection());
		
// SET_FORMATTING_ON
	}
}
