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
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageAppearanceTourChart;
import net.tourbook.preferences.PrefPageMap2Appearance;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Map 2D properties slideout.
 */
public class SlideoutMap2_MapOptions extends ToolbarSlideout {

	final static IPreferenceStore	_prefStore	= TourbookPlugin.getPrefStore();

	private SelectionAdapter		_defaultSelectionListener;
	private MouseWheelListener		_defaultMouseWheelListener;
	private FocusListener			_keepOpenListener;
	private SelectionAdapter		_layerSelectionListener;

	private Action					_actionRestoreDefaults;
	private ActionOpenPrefDialog	_actionPrefDialog;

	private PixelConverter			_pc;

	private Map2View				_map2View;

	/*
	 * UI controls
	 */
	private Composite				_parent;

	private Button					_chkTrackOpacity;
	private Spinner					_spinnerTrackOpacity;

	/**
	 * @param ownerControl
	 * @param toolBar
	 * @param map2View
	 */
	public SlideoutMap2_MapOptions(	final Control ownerControl,
									final ToolBar toolBar,
									final Map2View map2View) {

		super(ownerControl, toolBar);

		_map2View = map2View;
	}

	private void createActions() {

		_actionRestoreDefaults = new Action() {
			@Override
			public void run() {
				resetToDefaults();
			}
		};

		_actionRestoreDefaults.setImageDescriptor(TourbookPlugin.getImageDescriptor(
				Messages.Image__App_RestoreDefault));
		_actionRestoreDefaults.setToolTipText(Messages.App_Action_RestoreDefault_Tooltip);

		_actionPrefDialog = new ActionOpenPrefDialog(
				Messages.Tour_Action_EditChartPreferences,
				PrefPageAppearanceTourChart.ID);
		_actionPrefDialog.closeThisTooltip(this);
		_actionPrefDialog.setShell(_parent.getShell());
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		initUI(parent);

		createActions();

		final Composite ui = createUI(parent);

		restoreState();
		enableActions();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(shellContainer);
		{
			createUI_10_Header(shellContainer);
			createUI_20_MapOptions(shellContainer);
		}

		return shellContainer;
	}

	private void createUI_10_Header(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			{
				/*
				 * Slideout title
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Slideout_Map2MapOptions_Label_MapOptions);
				MTFont.setBannerFont(label);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.BEGINNING, SWT.CENTER)
						.applyTo(label);
			}
			{
				/*
				 * Actionbar
				 */
				final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.align(SWT.END, SWT.BEGINNING)
						.applyTo(toolbar);

				final ToolBarManager tbm = new ToolBarManager(toolbar);

				tbm.add(_actionRestoreDefaults);
				tbm.add(_actionPrefDialog);

				tbm.update(true);
			}
		}
	}

	private void createUI_20_MapOptions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Tour track opacity
				 */
				{
					// checkbox
					_chkTrackOpacity = new Button(container, SWT.CHECK);
					_chkTrackOpacity.setText(Messages.Pref_Map2_Label_TrackOpacity);
					_chkTrackOpacity.setToolTipText(Messages.Pref_Map2_Label_TrackOpacity_Tooltip);
					_chkTrackOpacity.addSelectionListener(_defaultSelectionListener);
				}
				{
					// spinner
					_spinnerTrackOpacity = new Spinner(container, SWT.BORDER);
					_spinnerTrackOpacity.setMinimum(PrefPageMap2Appearance.MAP_OPACITY_MINIMUM);
					_spinnerTrackOpacity.setMaximum(100);
					_spinnerTrackOpacity.setIncrement(1);
					_spinnerTrackOpacity.setPageIncrement(10);
					_spinnerTrackOpacity.addSelectionListener(_defaultSelectionListener);
					_spinnerTrackOpacity.addMouseWheelListener(_defaultMouseWheelListener);
				}
			}
		}
	}

	private void enableActions() {

		final boolean isTrackOpacity = _chkTrackOpacity.getSelection();

		_spinnerTrackOpacity.setEnabled(isTrackOpacity);
	}

	private void initUI(final Composite parent) {

		_parent = parent;

		_pc = new PixelConverter(parent);

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeUI();
			}
		};

		_defaultMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onChangeUI();
			}
		};

		_keepOpenListener = new FocusListener() {

			@Override
			public void focusGained(final FocusEvent e) {

				/*
				 * This will fix the problem that when the list of a combobox is displayed, then the
				 * slideout will disappear :-(((
				 */
				setIsAnotherDialogOpened(true);
			}

			@Override
			public void focusLost(final FocusEvent e) {
				setIsAnotherDialogOpened(false);
			}
		};
	}

	private void onChangeUI() {

		saveState();

		enableActions();
	}

	private void resetToDefaults() {

// SET_FORMATTING_OFF

		_chkTrackOpacity.setSelection(		_prefStore.getDefaultBoolean(ITourbookPreferences.MAP2_LAYOUT_IS_TOUR_TRACK_OPACITY));
		_spinnerTrackOpacity.setSelection(	_prefStore.getDefaultInt(ITourbookPreferences.MAP2_LAYOUT_TOUR_TRACK_OPACITY));
		
// SET_FORMATTING_ON

		saveState();
	}

	private void restoreState() {

// SET_FORMATTING_OFF
		
		_chkTrackOpacity.setSelection(			_prefStore.getBoolean(ITourbookPreferences.MAP2_LAYOUT_IS_TOUR_TRACK_OPACITY));
		_spinnerTrackOpacity.setSelection(		_prefStore.getInt(ITourbookPreferences.MAP2_LAYOUT_TOUR_TRACK_OPACITY));
				
// SET_FORMATTING_ON
	}

	private void saveState() {

// SET_FORMATTING_OFF
		
		_prefStore.setValue(ITourbookPreferences.MAP2_LAYOUT_IS_TOUR_TRACK_OPACITY,		_chkTrackOpacity.getSelection());
		_prefStore.setValue(ITourbookPreferences.MAP2_LAYOUT_TOUR_TRACK_OPACITY,		_spinnerTrackOpacity.getSelection());

// SET_FORMATTING_ON

		// force repainting
		TourbookPlugin.getPrefStore().setValue(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED, Math.random());
	}

}
