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
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageMap2Appearance;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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
 * Map 2D properties slideout
 */
public class SlideoutMap2_MapOptions extends ToolbarSlideout implements IColorSelectorListener {

// SET_FORMATTING_OFF
	
	private static final String		MAP_ACTION_EDIT2D_MAP_PREFERENCES	= net.tourbook.map2.Messages.Map_Action_Edit2DMapPreferences;
	
// SET_FORMATTING_ON

	final static IPreferenceStore	_prefStore							= TourbookPlugin.getPrefStore();
	final private IDialogSettings	_state;

	private MouseWheelListener		_defaultMouseWheelListener;
	private SelectionAdapter		_defaultSelectionListener;
	private SelectionAdapter		_defaultState_SelectionListener;
	private MouseWheelListener		_defaultState_MouseWheelListener;
	private FocusListener			_keepOpenListener;
	private IPropertyChangeListener	_defaultState_ChangePropertyListener;

	private Action					_actionRestoreDefaults;
	private ActionOpenPrefDialog	_actionPrefDialog;

	private PixelConverter			_pc;

	private Map2View				_map2View;

	/*
	 * UI controls
	 */
	private Composite				_parent;

	private Button					_chkSliderRelation;
	private Button					_chkTrackOpacity;

	private Label					_lblSliderRelationColor;
	private Label					_lblSliderRelationSegments;
	private Label					_lblSliderRelationWidth;

	private Spinner					_spinnerSliderRelationOpacity;
	private Spinner					_spinnerSliderRelationSegments;
	private Spinner					_spinnerSliderRelationLineWidth;
	private Spinner					_spinnerTrackOpacity;

	private ColorSelectorExtended	_colorSliderRelationColor;

	/**
	 * @param ownerControl
	 * @param toolBar
	 * @param map2View
	 * @param state
	 */
	public SlideoutMap2_MapOptions(	final Control ownerControl,
									final ToolBar toolBar,
									final Map2View map2View,
									final IDialogSettings state) {

		super(ownerControl, toolBar);

		_map2View = map2View;
		_state = state;
	}

	@Override
	public void colorDialogOpened(final boolean isAnotherDialogOpened) {
		setIsAnotherDialogOpened(isAnotherDialogOpened);
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

		_actionPrefDialog = new ActionOpenPrefDialog(MAP_ACTION_EDIT2D_MAP_PREFERENCES, PrefPageMap2Appearance.ID);
		_actionPrefDialog.closeThisTooltip(this);
		_actionPrefDialog.setShell(_parent.getShell());
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		initUI(parent);

		createActions();

		final Composite ui = createUI(parent);

		restoreState();
		enableControls();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(shellContainer);
		{
			createUI_10_Header(shellContainer);
			createUI_20_MapOptions(shellContainer);
			createUI_30_SliderRelation(shellContainer);
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
				label.setText(Messages.Slideout_Map2_MapOptions_Title);
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
					_chkTrackOpacity.setText(Messages.Pref_Map2_Checkbox_TrackOpacity);
					_chkTrackOpacity.setToolTipText(Messages.Pref_Map2_Checkbox_TrackOpacity_Tooltip);
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

	private void createUI_30_SliderRelation(final Composite parent) {

		{
			/*
			 * Slider relationship
			 */
			// checkbox
			_chkSliderRelation = new Button(parent, SWT.CHECK);
			_chkSliderRelation.setText(Messages.Slideout_Map2_MapOptions_Label_SliderRelation);
			_chkSliderRelation.setToolTipText(Messages.Slideout_Map2_MapOptions_Label_SliderRelation_Tooltip);
			_chkSliderRelation.addSelectionListener(_defaultState_SelectionListener);
		}

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
				.grab(true, false)
				.indent(_pc.convertWidthInCharsToPixels(3), 0)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Segments
				 */

				// label
				_lblSliderRelationSegments = new Label(container, SWT.NONE);
				_lblSliderRelationSegments.setText(Messages.Slideout_Map2_MapOptions_Label_RelationSegemnts);

				// spinner
				_spinnerSliderRelationSegments = new Spinner(container, SWT.BORDER);
				_spinnerSliderRelationSegments.setMinimum(1);
				_spinnerSliderRelationSegments.setMaximum(100);
				_spinnerSliderRelationSegments.setIncrement(1);
				_spinnerSliderRelationSegments.setPageIncrement(10);
				_spinnerSliderRelationSegments.addSelectionListener(_defaultState_SelectionListener);
				_spinnerSliderRelationSegments.addMouseWheelListener(_defaultState_MouseWheelListener);
			}
			{
				/*
				 * Line width
				 */

				// label
				_lblSliderRelationWidth = new Label(container, SWT.NONE);
				_lblSliderRelationWidth.setText(Messages.Slideout_Map2_MapOptions_Label_RelationWidth);

				// spinner
				_spinnerSliderRelationLineWidth = new Spinner(container, SWT.BORDER);
				_spinnerSliderRelationLineWidth.setMinimum(1);
				_spinnerSliderRelationLineWidth.setMaximum(100);
				_spinnerSliderRelationLineWidth.setIncrement(1);
				_spinnerSliderRelationLineWidth.setPageIncrement(10);
				_spinnerSliderRelationLineWidth.addSelectionListener(_defaultState_SelectionListener);
				_spinnerSliderRelationLineWidth.addMouseWheelListener(_defaultState_MouseWheelListener);
			}
			{
				/*
				 * Color + opacity
				 */

				// label
				_lblSliderRelationColor = new Label(container, SWT.NONE);
				_lblSliderRelationColor.setText(Messages.Slideout_Map2_MapOptions_Label_RelationColor);

				final Composite colorContainer = new Composite(container, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(colorContainer);
				GridLayoutFactory.fillDefaults().numColumns(2).applyTo(colorContainer);
				{
					// color
					_colorSliderRelationColor = new ColorSelectorExtended(colorContainer);
					_colorSliderRelationColor.addListener(_defaultState_ChangePropertyListener);
					_colorSliderRelationColor.addOpenListener(this);

					// opacity
					_spinnerSliderRelationOpacity = new Spinner(colorContainer, SWT.BORDER);
					_spinnerSliderRelationOpacity.setMinimum(1);
					_spinnerSliderRelationOpacity.setMaximum(100);
					_spinnerSliderRelationOpacity.setIncrement(1);
					_spinnerSliderRelationOpacity.setPageIncrement(10);
					_spinnerSliderRelationOpacity.addSelectionListener(_defaultState_SelectionListener);
					_spinnerSliderRelationOpacity.addMouseWheelListener(_defaultState_MouseWheelListener);
				}
			}
		}
	}

	private void enableControls() {

		final boolean isUseTrackOpacity = _chkTrackOpacity.getSelection();
		final boolean isShowSliderRelation = _chkSliderRelation.getSelection();

		_spinnerTrackOpacity.setEnabled(isUseTrackOpacity);

		_colorSliderRelationColor.setEnabled(isShowSliderRelation);
		_lblSliderRelationColor.setEnabled(isShowSliderRelation);
		_lblSliderRelationWidth.setEnabled(isShowSliderRelation);
		_lblSliderRelationSegments.setEnabled(isShowSliderRelation);
		_spinnerSliderRelationOpacity.setEnabled(isShowSliderRelation);
		_spinnerSliderRelationSegments.setEnabled(isShowSliderRelation);
		_spinnerSliderRelationLineWidth.setEnabled(isShowSliderRelation);
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

		_defaultState_ChangePropertyListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				onChangeUI_WithState();
			}
		};

		_defaultState_MouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onChangeUI_WithState();
			}
		};
		_defaultState_SelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeUI_WithState();
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

		enableControls();
	}

	private void onChangeUI_WithState() {

		onChangeUI();

		_map2View.restoreState_Map2Options(true);
	}

	private void resetToDefaults() {

// SET_FORMATTING_OFF

		// slider relation
		_chkSliderRelation.setSelection(				Map2View.STATE_IS_SHOW_SLIDER_RELATION_DEFAULT);
		_spinnerSliderRelationOpacity.setSelection(		Map2View.STATE_SLIDER_RELATION_OPACITY_DEFAULT);
		_spinnerSliderRelationSegments.setSelection(	Map2View.STATE_SLIDER_RELATION_SEGMENTS_DEFAULT);
		_spinnerSliderRelationLineWidth.setSelection(	Map2View.STATE_SLIDER_RELATION_LINE_WIDTH_DEFAULT);
		_colorSliderRelationColor.setColorValue(		Map2View.STATE_SLIDER_RELATION_COLOR_DEFAULT);
		
		// track opacity
		_chkTrackOpacity.setSelection(		_prefStore.getDefaultBoolean(ITourbookPreferences.MAP2_LAYOUT_IS_TOUR_TRACK_OPACITY));
		_spinnerTrackOpacity.setSelection(	_prefStore.getDefaultInt(ITourbookPreferences.MAP2_LAYOUT_TOUR_TRACK_OPACITY));
		
// SET_FORMATTING_ON

		saveState();
	}

	private void restoreState() {

// SET_FORMATTING_OFF

		// slider relation
		_chkSliderRelation.setSelection(			Util.getStateBoolean(_state, Map2View.STATE_IS_SHOW_SLIDER_RELATION, 	Map2View.STATE_IS_SHOW_SLIDER_RELATION_DEFAULT));
		_spinnerSliderRelationOpacity.setSelection(		Util.getStateInt(_state, Map2View.STATE_SLIDER_RELATION_OPACITY,	Map2View.STATE_SLIDER_RELATION_OPACITY_DEFAULT));
		_spinnerSliderRelationSegments.setSelection(	Util.getStateInt(_state, Map2View.STATE_SLIDER_RELATION_SEGMENTS,	Map2View.STATE_SLIDER_RELATION_SEGMENTS_DEFAULT));
		_spinnerSliderRelationLineWidth.setSelection(	Util.getStateInt(_state, Map2View.STATE_SLIDER_RELATION_LINE_WIDTH, Map2View.STATE_SLIDER_RELATION_LINE_WIDTH_DEFAULT));
		_colorSliderRelationColor.setColorValue(		Util.getStateRGB(_state, Map2View.STATE_SLIDER_RELATION_COLOR, 		Map2View.STATE_SLIDER_RELATION_COLOR_DEFAULT));
		
		// track opacity
		_chkTrackOpacity.setSelection(			_prefStore.getBoolean(ITourbookPreferences.MAP2_LAYOUT_IS_TOUR_TRACK_OPACITY));
		_spinnerTrackOpacity.setSelection(		_prefStore.getInt(ITourbookPreferences.MAP2_LAYOUT_TOUR_TRACK_OPACITY));
				
// SET_FORMATTING_ON
	}

	private void saveState() {

// SET_FORMATTING_OFF
		
		// slider relation
		_state.put(Map2View.STATE_IS_SHOW_SLIDER_RELATION, _chkSliderRelation.getSelection());
		_state.put(Map2View.STATE_SLIDER_RELATION_OPACITY, _spinnerSliderRelationOpacity.getSelection());
		_state.put(Map2View.STATE_SLIDER_RELATION_SEGMENTS, _spinnerSliderRelationSegments.getSelection());
		_state.put(Map2View.STATE_SLIDER_RELATION_LINE_WIDTH, _spinnerSliderRelationLineWidth.getSelection());
		Util.setState(_state, Map2View.STATE_SLIDER_RELATION_COLOR, _colorSliderRelationColor.getColorValue());

		// track opacity
		_prefStore.setValue(ITourbookPreferences.MAP2_LAYOUT_IS_TOUR_TRACK_OPACITY,		_chkTrackOpacity.getSelection());
		_prefStore.setValue(ITourbookPreferences.MAP2_LAYOUT_TOUR_TRACK_OPACITY,		_spinnerTrackOpacity.getSelection());

// SET_FORMATTING_ON

	}

}
